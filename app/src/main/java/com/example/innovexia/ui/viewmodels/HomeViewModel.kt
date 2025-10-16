package com.example.innovexia.ui.viewmodels

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.auth.ProfileId
import com.example.innovexia.data.ai.GeminiException
import com.example.innovexia.data.ai.GeminiService
import com.example.innovexia.data.local.entities.ChatEntity
import com.example.innovexia.data.preferences.UserPreferences
import com.example.innovexia.data.repository.ChatRepository
import com.example.innovexia.memory.Mind.api.ChatTurn
import com.example.innovexia.memory.Mind.api.MemoryEngine
import com.example.innovexia.memory.Mind.di.MindModule
import com.example.innovexia.ui.models.Persona
import com.example.innovexia.core.persona.PersonaRepository
import com.example.innovexia.data.models.AttachmentMeta
import com.example.innovexia.data.models.AttachmentKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Home screen.
 * Manages chat state, streaming, consent, and recent chats.
 */
class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val userPreferences: UserPreferences,
    private val geminiService: GeminiService,
    private val personaRepository: PersonaRepository,
    private val subscriptionViewModel: SubscriptionViewModel,
) : ViewModel() {

    // Track current profile to detect switches
    private var currentProfileId: ProfileId = ProfileId.current()

    // Context for reading attachment URIs
    private var appContext: Context? = null

    // Memory engine for persona memory
    private var memoryEngine: MemoryEngine? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
        memoryEngine = MindModule.provideMemoryEngine(context.applicationContext)
    }

    // AI preferences - persisted via DataStore
    val selectedModel: StateFlow<String> = userPreferences.selectedModel
        .stateIn(viewModelScope, SharingStarted.Eagerly, "gemini-2.5-flash")

    val temperature: StateFlow<Float> = userPreferences.temperature
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.7f)

    val maxOutputTokens: StateFlow<Int> = userPreferences.maxOutputTokens
        .stateIn(viewModelScope, SharingStarted.Eagerly, 2048)

    val safetyLevel: StateFlow<String> = userPreferences.safetyLevel
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Standard")

    val groundingEnabled: StateFlow<Boolean> = userPreferences.groundingEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _temperature get() = temperature
    private val _maxOutputTokens get() = maxOutputTokens
    private val _safetyLevel get() = safetyLevel
    private val _groundingEnabled get() = groundingEnabled

    fun setSelectedModel(modelId: String) {
        viewModelScope.launch {
            userPreferences.setSelectedModel(modelId)
        }
    }

    fun setAiPreferences(temperature: Float, maxTokens: Int, safety: String) {
        viewModelScope.launch {
            userPreferences.setTemperature(temperature)
            userPreferences.setMaxOutputTokens(maxTokens)
            userPreferences.setSafetyLevel(safety)
        }
    }

    fun setGroundingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setGroundingEnabled(enabled)
            android.util.Log.d("HomeViewModel", "Grounding ${if (enabled) "enabled" else "disabled"}")
        }
    }

    init {
        // Observe profile changes and clear active chat when profile switches
        viewModelScope.launch {
            chatRepository.getProfileRepository()?.profile?.collect { newProfile ->
                if (newProfile != currentProfileId) {
                    // Profile switched - clear active chat
                    currentProfileId = newProfile
                    newChat()
                }
            }
        }
    }

    // Consent status (null = not asked, true = allowed, false = declined)
    val consentedSaveHistory: StateFlow<Boolean?> = userPreferences.consentedSaveHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Recent chats from database (empty if consent = false)
    // Debounced to avoid excessive updates during streaming (reduces 8-10/sec to ~3/sec)
    val recentChats: StateFlow<List<ChatEntity>> = consentedSaveHistory
        .flatMapLatest { consented ->
            if (consented == true) {
                chatRepository.observeRecentChats()
                    .debounce(300)  // Debounce by 300ms to reduce DB load during streaming
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Archived chats from database
    val archivedChats: StateFlow<List<ChatEntity>> = consentedSaveHistory
        .flatMapLatest { consented ->
            if (consented == true) {
                chatRepository.observeArchivedChats()
                    .debounce(300)  // Debounce for consistency
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deleted (trash) chats from database
    val deletedChats: StateFlow<List<ChatEntity>> = consentedSaveHistory
        .flatMapLatest { consented ->
            if (consented == true) {
                chatRepository.observeDeletedChats()
                    .debounce(300)  // Debounce for consistency
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current active chat ID (null if no active chat)
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    // Current chat title (empty if no active chat)
    val activeChatTitle: StateFlow<String> = _activeChatId.flatMapLatest { chatId ->
        if (chatId != null && consentedSaveHistory.value == true) {
            chatRepository.observeAllRecentChats().map { chats ->
                chats.find { it.id == chatId }?.title ?: "New chat"
            }
        } else {
            flowOf("")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Attachment state
    private val _attachments = MutableStateFlow<List<AttachmentMeta>>(emptyList())
    val attachments: StateFlow<List<AttachmentMeta>> = _attachments.asStateFlow()

    // Streaming state
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    // Current streaming text (accumulated tokens)
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    // Streaming title state (for real-time title generation)
    private val _streamingTitle = MutableStateFlow<String?>(null)
    val streamingTitle: StateFlow<String?> = _streamingTitle.asStateFlow()

    // Combined title (streaming title takes precedence over static title)
    val displayTitle: StateFlow<String> = combine(
        activeChatTitle,
        _streamingTitle
    ) { staticTitle, streaming ->
        streaming ?: staticTitle
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "New chat")

    // Visible messages in current chat (for UI display)
    private val _visibleMessages = MutableStateFlow<List<com.example.innovexia.ui.models.UIMessage>>(emptyList())
    val visibleMessages: StateFlow<List<com.example.innovexia.ui.models.UIMessage>> = _visibleMessages.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Streaming job (for cancellation)
    private var streamingJob: Job? = null

    // Thinking mode (from tools panel)
    private val _thinkingEnabled = MutableStateFlow(false)
    val thinkingEnabled: StateFlow<Boolean> = _thinkingEnabled.asStateFlow()

    // Token counts from last API response
    private val _lastTokenCounts = MutableStateFlow<Pair<Int, Int>>(0 to 0) // input to output
    val lastTokenCounts: StateFlow<Pair<Int, Int>> = _lastTokenCounts.asStateFlow()

    // Grounding metadata map (messageId -> GroundingMetadata)
    private val _groundingDataMap = MutableStateFlow<Map<String, com.example.innovexia.data.ai.GroundingMetadata>>(emptyMap())
    val groundingDataMap: StateFlow<Map<String, com.example.innovexia.data.ai.GroundingMetadata>> = _groundingDataMap.asStateFlow()

    // Grounding status map (messageId -> GroundingStatus)
    private val _groundingStatusMap = MutableStateFlow<Map<String, com.example.innovexia.data.ai.GroundingStatus>>(emptyMap())
    val groundingStatusMap: StateFlow<Map<String, com.example.innovexia.data.ai.GroundingStatus>> = _groundingStatusMap.asStateFlow()

    /**
     * Check if API key is configured.
     */
    fun isApiKeyConfigured(): Boolean {
        return geminiService.isApiKeyConfigured()
    }

    /**
     * Set user consent for saving chat history.
     */
    fun setConsent(consented: Boolean) {
        viewModelScope.launch {
            userPreferences.setConsentSaveHistory(consented)
        }
    }

    /**
     * Enable or disable thinking mode.
     */
    fun setThinkingEnabled(enabled: Boolean) {
        _thinkingEnabled.value = enabled
    }

    /**
     * Send a message and stream the response.
     * If consent is true, saves to database. Otherwise, ephemeral.
     *
     * @param userMessage The user's message
     * @param persona Optional persona for style
     * @param isIncognito Whether to create the chat in incognito mode (local-only)
     */
    fun sendMessage(userMessage: String, persona: Persona?, isIncognito: Boolean = false) {
        if (userMessage.isBlank()) return

        viewModelScope.launch {
            try {
                // ============ RATE LIMITING CHECK ============
                android.util.Log.d("HomeViewModel", "=== RATE LIMIT CHECK START ===")
                android.util.Log.d("HomeViewModel", "User: ${com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "GUEST"}")
                android.util.Log.d("HomeViewModel", "Message: ${userMessage.take(50)}")

                val (canSend, rateLimitError) = subscriptionViewModel.canMakeRequest()
                android.util.Log.d("HomeViewModel", "Rate limit result: canSend=$canSend, error='$rateLimitError'")

                if (!canSend) {
                    android.util.Log.w("HomeViewModel", "❌ RATE LIMITED - blocking send")
                    _errorMessage.value = rateLimitError
                    return@launch
                }
                android.util.Log.d("HomeViewModel", "✅ Rate limit check PASSED - proceeding with send")

                _errorMessage.value = null
                _isStreaming.value = true
                _streamingText.value = ""

                // Capture attachments before clearing
                val attachmentsToSend = _attachments.value

                // Add user message to visible list immediately
                val userMsgId = java.util.UUID.randomUUID().toString()
                val userMsg = com.example.innovexia.ui.models.UIMessage(
                    id = userMsgId,
                    text = userMessage,
                    isUser = true,
                    attachments = attachmentsToSend
                )
                _visibleMessages.value = _visibleMessages.value + userMsg

                // Clear attachments immediately after capturing for sending
                _attachments.value = emptyList()

                val consented = consentedSaveHistory.value == true
                android.util.Log.d("HomeViewModel", "sendMessage - consented=$consented, consentedSaveHistory=${consentedSaveHistory.value}")

                // Capture original chat ID BEFORE updating it
                val previousChatId = _activeChatId.value

                // Create or get active chat
                val chatId = if (consented) {
                    _activeChatId.value ?: chatRepository.startChat(userMessage, persona, isIncognito, attachmentsToSend).also {
                        _activeChatId.value = it
                        // Start streaming title immediately for new chats (concise, 25 chars)
                        _streamingTitle.value = userMessage.take(25)
                    }
                } else {
                    // Ephemeral: just use a temp ID, don't save
                    _activeChatId.value ?: "ephemeral-${System.currentTimeMillis()}".also {
                        _activeChatId.value = it
                        // Show user message as title for ephemeral chats too (concise, 25 chars)
                        _streamingTitle.value = userMessage.take(25)
                    }
                }

                // If not first message, append user message to DB
                // (startChat already saved first message, so only append if chat existed before)
                val isExistingChat = previousChatId != null
                if (consented && isExistingChat) {
                    chatRepository.appendUserMessage(chatId, userMessage, attachmentsToSend)
                }

                // Capture the model name that will be used for this response
                val modelNameForResponse = selectedModel.value
                val modelDisplayName = com.example.innovexia.core.ai.getModelLabel(modelNameForResponse)

                // Add streaming model message placeholder with SENDING status
                val modelMsgId = java.util.UUID.randomUUID().toString()
                val modelMsg = com.example.innovexia.ui.models.UIMessage(
                    id = modelMsgId,
                    text = "",
                    isUser = false,
                    isStreaming = true,
                    status = com.example.innovexia.ui.models.MessageStatus.SENDING,
                    modelName = modelDisplayName // Store the model name with the message
                )
                _visibleMessages.value = _visibleMessages.value + modelMsg

                // Load full persona from repository if persona ID provided
                val fullPersona = if (persona != null) {
                    personaRepository.getPersonaById(persona.id)
                } else {
                    null
                }
                android.util.Log.d("HomeViewModel", "About to call generateReply - persona=${persona?.name}, fullPersona=${fullPersona?.name}, chatId=$chatId")

                // ============ RECORD REQUEST FOR RATE LIMITING ============
                android.util.Log.d("HomeViewModel", "Recording request for rate limiting...")
                subscriptionViewModel.recordRequest()
                android.util.Log.d("HomeViewModel", "✅ Request recorded successfully")

                // Stream Gemini response
                var dbMessageId: String? = null
                var hasReceivedFirstToken = false
                var dbTokenBuffer = StringBuilder() // Separate buffer for database saves
                var lastUpdateTime = 0L
                val updateIntervalMs = 50L // Update UI every 50ms for smooth streaming

                var finalInputTokens = 0
                var finalOutputTokens = 0
                var finalGroundingMetadata: com.example.innovexia.data.ai.GroundingMetadata? = null

                streamingJob = launch {
                    try {
                        // Get current AI preferences for this request
                        val currentModel = selectedModel.value
                        val currentTemp = temperature.value
                        val currentMaxTokens = maxOutputTokens.value
                        val currentSafety = safetyLevel.value
                        val currentGroundingEnabled = groundingEnabled.value

                        android.util.Log.d("HomeViewModel", "Starting generateReplyWithTokens stream - chatId=$chatId, model=$currentModel, temp=$currentTemp, maxTokens=$currentMaxTokens, safety=$currentSafety, grounding=$currentGroundingEnabled")

                        // Set initial grounding status if grounding is enabled
                        if (currentGroundingEnabled) {
                            _groundingStatusMap.value = _groundingStatusMap.value + (modelMsgId to com.example.innovexia.data.ai.GroundingStatus.SEARCHING)
                        }

                        // Stream response from Gemini with memory context and user-selected model
                        geminiService.generateReplyWithTokens(
                            chatId = chatId,
                            userText = userMessage,
                            persona = fullPersona,
                            enableThinking = _thinkingEnabled.value,
                            attachments = attachmentsToSend,
                            context = appContext,
                            modelName = currentModel,
                            temperature = currentTemp,
                            maxOutputTokens = currentMaxTokens,
                            safetyLevel = currentSafety,
                            groundingEnabled = currentGroundingEnabled
                        ).collect { chunk ->
                            // Update UI state
                            _streamingText.value += chunk.text

                            // Capture token counts from API
                            if (chunk.inputTokens > 0 || chunk.outputTokens > 0) {
                                finalInputTokens = chunk.inputTokens
                                finalOutputTokens = chunk.outputTokens
                            }

                            // Update grounding status from chunk
                            if (chunk.groundingStatus != com.example.innovexia.data.ai.GroundingStatus.NONE) {
                                _groundingStatusMap.value = _groundingStatusMap.value + (modelMsgId to chunk.groundingStatus)
                            }

                            // Store grounding metadata if present AND contains actual data
                            if (chunk.groundingMetadata != null &&
                                (chunk.groundingMetadata.webSearchQueries?.isNotEmpty() == true ||
                                 chunk.groundingMetadata.groundingChunks.isNotEmpty() ||
                                 chunk.groundingMetadata.searchResultUrls.isNotEmpty())) {
                                finalGroundingMetadata = chunk.groundingMetadata
                                _groundingDataMap.value = _groundingDataMap.value + (modelMsgId to chunk.groundingMetadata)
                                android.util.Log.d("HomeViewModel", "🔍 Grounding data captured for message $modelMsgId: ${com.example.innovexia.data.ai.GroundingService.getGroundingSummary(chunk.groundingMetadata)}")
                            }

                            // Add to database buffer
                            dbTokenBuffer.append(chunk.text)

                            // Update visible message
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime >= updateIntervalMs || !hasReceivedFirstToken) {
                                hasReceivedFirstToken = true
                                lastUpdateTime = currentTime

                                // Update the streaming message in visible list
                                _visibleMessages.value = _visibleMessages.value.map { msg ->
                                    if (msg.id == modelMsgId) {
                                        msg.copy(
                                            text = _streamingText.value,
                                            status = com.example.innovexia.ui.models.MessageStatus.STREAMING
                                        )
                                    } else msg
                                }

                                // Save to database periodically
                                if (consented && dbTokenBuffer.length >= 100) {
                                    if (dbMessageId == null) {
                                        // Create first DB message
                                        dbMessageId = chatRepository.appendModelToken(
                                            chatId = chatId,
                                            messageId = null,
                                            token = dbTokenBuffer.toString(),
                                            isFinal = false
                                        )
                                    } else {
                                        // Update existing DB message
                                        chatRepository.appendModelToken(
                                            chatId = chatId,
                                            messageId = dbMessageId,
                                            token = dbTokenBuffer.toString(),
                                            isFinal = false
                                        )
                                    }
                                    dbTokenBuffer.clear()
                                }
                            }
                        }

                        // Streaming completed successfully - save final state WITH GROUNDING DATA
                        android.util.Log.d("HomeViewModel", "==== SAVING TO DATABASE ====")
                        android.util.Log.d("HomeViewModel", "finalGroundingMetadata is null? ${finalGroundingMetadata == null}")
                        if (finalGroundingMetadata != null) {
                            android.util.Log.d("HomeViewModel", "✅ Will save grounding metadata: ${com.example.innovexia.data.ai.GroundingService.getGroundingSummary(finalGroundingMetadata)}")
                        } else {
                            android.util.Log.w("HomeViewModel", "❌ No grounding metadata to save")
                        }

                        if (consented && dbTokenBuffer.isNotEmpty()) {
                            if (dbMessageId == null) {
                                dbMessageId = chatRepository.appendModelToken(
                                    chatId = chatId,
                                    messageId = null,
                                    token = dbTokenBuffer.toString(),
                                    isFinal = true,
                                    groundingMetadata = finalGroundingMetadata,
                                    groundingStatus = _groundingStatusMap.value[modelMsgId]
                                )
                            } else {
                                chatRepository.appendModelToken(
                                    chatId = chatId,
                                    messageId = dbMessageId,
                                    token = dbTokenBuffer.toString(),
                                    isFinal = true,
                                    groundingMetadata = finalGroundingMetadata,
                                    groundingStatus = _groundingStatusMap.value[modelMsgId]
                                )
                                android.util.Log.d("HomeViewModel", "✅ appendModelToken completed for dbMessageId=$dbMessageId")
                            }
                        }

                        // Remap grounding data from UI ID to database ID
                        if (dbMessageId != null && dbMessageId != modelMsgId) {
                            if (finalGroundingMetadata != null) {
                                _groundingDataMap.value = _groundingDataMap.value - modelMsgId + (dbMessageId to finalGroundingMetadata)
                                android.util.Log.d("HomeViewModel", "Remapped grounding data from $modelMsgId to $dbMessageId")
                            }
                            _groundingStatusMap.value[modelMsgId]?.let { status ->
                                _groundingStatusMap.value = _groundingStatusMap.value - modelMsgId + (dbMessageId to status)
                                android.util.Log.d("HomeViewModel", "Remapped grounding status from $modelMsgId to $dbMessageId")
                            }
                        }

                        // Database save completed - grounding data is now persisted

                        // Update final visible message
                        _visibleMessages.value = _visibleMessages.value.map { msg ->
                            if (msg.id == modelMsgId) {
                                msg.copy(
                                    text = _streamingText.value,
                                    isStreaming = false,
                                    status = com.example.innovexia.ui.models.MessageStatus.COMPLETE
                                )
                            } else msg
                        }

                        // Ingest conversation turn to memory system
                        if (!isIncognito && persona != null) {
                            try {
                                val turn = ChatTurn(
                                    userId = currentProfileId.toOwnerId(),
                                    chatId = chatId,
                                    userMessage = userMessage,
                                    assistantMessage = _streamingText.value,
                                    timestamp = System.currentTimeMillis()
                                )
                                memoryEngine?.ingest(
                                    turn = turn,
                                    personaId = persona.id,
                                    incognito = isIncognito
                                )
                                android.util.Log.d("HomeViewModel", "Memory ingested for persona ${persona.id}: user='${userMessage.take(50)}' assistant='${_streamingText.value.take(50)}'")
                            } catch (e: Exception) {
                                android.util.Log.e("HomeViewModel", "Failed to ingest memory", e)
                            }
                        }

                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Streaming error", e)
                        // Update message with error status - use COMPLETE with error text
                        _visibleMessages.value = _visibleMessages.value.map { msg ->
                            if (msg.id == modelMsgId) {
                                msg.copy(
                                    text = if (_streamingText.value.isNotEmpty()) _streamingText.value else "Error: ${e.message}",
                                    isStreaming = false,
                                    status = com.example.innovexia.ui.models.MessageStatus.COMPLETE
                                )
                            } else msg
                        }
                        // Don't re-throw - just let the error be handled gracefully
                        _errorMessage.value = when (e) {
                            is GeminiException -> "Gemini API error: ${e.message}"
                            else -> "An error occurred: ${e.message}"
                        }
                        // Reset streaming state
                        _isStreaming.value = false
                        _streamingText.value = ""
                    } finally {
                        // Always ensure streaming state is reset
                        _isStreaming.value = false
                    }
                }

                streamingJob?.join()
                _streamingText.value = ""

                // Update token counts from API response
                _lastTokenCounts.value = finalInputTokens to finalOutputTokens
                android.util.Log.d("HomeViewModel", "Final API tokens - Input: $finalInputTokens, Output: $finalOutputTokens")

                // After streaming completes, generate AI title with streaming animation
                if (consented && chatId.isNotEmpty()) {
                    // Launch in background - don't block on title generation
                    launch {
                        try {
                            // Stream title generation character by character for visual effect
                            val success = chatRepository.generateAndUpdateAITitle(chatId)

                            if (success) {
                                // Get the newly generated title from active chat flow
                                val newTitle = activeChatTitle.value

                                if (newTitle != null && newTitle != _streamingTitle.value) {
                                    // Animate title streaming in
                                    var currentLength = 0
                                    while (currentLength < newTitle.length) {
                                        currentLength = minOf(currentLength + 2, newTitle.length)
                                        _streamingTitle.value = newTitle.take(currentLength)
                                        kotlinx.coroutines.delay(30) // 30ms per 2 characters
                                    }

                                    // After animation, clear streaming state (static title takes over)
                                    kotlinx.coroutines.delay(500)
                                    _streamingTitle.value = null
                                }
                            } else {
                                // Title generation failed/skipped, clear streaming title
                                kotlinx.coroutines.delay(500)
                                _streamingTitle.value = null
                            }
                        } catch (e: Exception) {
                            // Ignore title generation errors, clear streaming title
                            _streamingTitle.value = null
                        }
                    }
                }

            } catch (e: com.example.innovexia.data.ai.RateLimitException) {
                _isStreaming.value = false
                _errorMessage.value = "Rate limit reached. Wait ${e.retryAfterSeconds}s and try again."
                _streamingText.value = ""
            } catch (e: GeminiException) {
                _isStreaming.value = false
                _errorMessage.value = "Couldn't reach Gemini. Try again."
                _streamingText.value = ""
            } catch (e: Exception) {
                _isStreaming.value = false
                _errorMessage.value = "An error occurred: ${e.message}"
                _streamingText.value = ""
            }
        }
    }

    /**
     * Handle local model generation with download support
     */

    /**
     * Process attachment URIs from file picker.
     */
    fun processAttachmentUris(uris: List<Uri>, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newAttachments = uris.mapNotNull { uri ->
                    try {
                        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                        val fileName = getFileNameFromUri(uri, contentResolver)
                        val fileSize = getFileSizeFromUri(uri, contentResolver)

                        AttachmentMeta(
                            id = java.util.UUID.randomUUID().toString(),
                            name = fileName,
                            mime = mimeType,
                            bytes = fileSize,
                            localUri = uri,
                            kind = when {
                                mimeType.startsWith("image/") -> AttachmentKind.PHOTO
                                mimeType == "application/pdf" -> AttachmentKind.PDF
                                else -> AttachmentKind.FILE
                            }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "Failed to process URI: $uri", e)
                        null
                    }
                }

                withContext(Dispatchers.Main) {
                    _attachments.value = _attachments.value + newAttachments
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to process attachments", e)
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri, contentResolver: ContentResolver): String {
        var name = "Unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun getFileSizeFromUri(uri: Uri, contentResolver: ContentResolver): Long {
        var size = 0L
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = it.getLong(sizeIndex)
                }
            }
        }
        return size
    }

    /**
     * Remove an attachment by ID.
     */
    fun removeAttachment(attachmentId: String) {
        _attachments.value = _attachments.value.filter { it.id != attachmentId }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Get last message preview for a chat.
     */
    suspend fun getLastMessagePreview(chatId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                chatRepository.getLastMessages(chatId, 1).firstOrNull()?.text?.take(100)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Start a new chat.
     */
    fun newChat() {
        _activeChatId.value = null
        _visibleMessages.value = emptyList()
        _attachments.value = emptyList()
        _streamingText.value = ""
        _streamingTitle.value = null
        _errorMessage.value = null
        _isStreaming.value = false
        _groundingDataMap.value = emptyMap()
        _groundingStatusMap.value = emptyMap()
        streamingJob?.cancel()
    }

    /**
     * Load an existing chat by ID.
     */
    fun loadChat(chatId: String) {
        viewModelScope.launch {
            try {
                _activeChatId.value = chatId
                _visibleMessages.value = emptyList()
                _attachments.value = emptyList()
                _streamingText.value = ""
                _streamingTitle.value = null
                _errorMessage.value = null
                _isStreaming.value = false
                streamingJob?.cancel()

                // Load messages from database by collecting the flow
                val messages = mutableListOf<com.example.innovexia.data.local.entities.MessageEntity>()
                chatRepository.messagesForChat(chatId).first().let { messages.addAll(it) }
                _visibleMessages.value = messages.map { msg ->
                    com.example.innovexia.ui.models.UIMessage(
                        id = msg.id,
                        text = msg.text,
                        isUser = msg.role == "user",
                        timestamp = msg.createdAt,
                        attachments = msg.attachments()
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load chat: ${e.message}"
            }
        }
    }

    /**
     * Stop streaming response.
     */
    fun stopStreaming() {
        streamingJob?.cancel()
        _isStreaming.value = false
        _streamingText.value = ""
    }

    /**
     * Delete all chat history.
     */
    fun deleteAllHistory() {
        viewModelScope.launch {
            try {
                chatRepository.deleteAllHistory()
                newChat()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete history: ${e.message}"
            }
        }
    }

    /**
     * Toggle pin status of a chat.
     */
    fun togglePin(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.togglePin(chatId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle pin: ${e.message}"
            }
        }
    }

    /**
     * Archive a chat.
     */
    fun archiveChat(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.archiveChat(chatId)
                if (_activeChatId.value == chatId) {
                    newChat()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to archive chat: ${e.message}"
            }
        }
    }

    /**
     * Move a chat to trash.
     */
    fun moveToTrash(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.moveToTrash(chatId)
                if (_activeChatId.value == chatId) {
                    newChat()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to move to trash: ${e.message}"
            }
        }
    }

    /**
     * Restore a chat from archive.
     */
    fun restoreFromArchive(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.restoreFromArchive(chatId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore from archive: ${e.message}"
            }
        }
    }

    /**
     * Restore a chat from trash.
     */
    fun restoreFromTrash(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.restoreFromTrash(chatId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to restore from trash: ${e.message}"
            }
        }
    }

    /**
     * Delete a chat permanently.
     */
    fun deleteForever(chatId: String) {
        viewModelScope.launch {
            try {
                chatRepository.deleteForever(chatId)
                if (_activeChatId.value == chatId) {
                    newChat()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete permanently: ${e.message}"
            }
        }
    }

    /**
     * Empty trash - delete all chats in trash permanently.
     */
    fun emptyTrash() {
        viewModelScope.launch {
            try {
                val trashedChats = deletedChats.value
                trashedChats.forEach { chat ->
                    chatRepository.deleteForever(chat.id)
                }
                // If active chat was in trash, start new chat
                if (trashedChats.any { it.id == _activeChatId.value }) {
                    newChat()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to empty trash: ${e.message}"
            }
        }
    }

    /**
     * Clear all chat state (used when signing out or switching accounts).
     * This resets the active chat and clears all in-memory state.
     */
    fun clearAllChatState() {
        _activeChatId.value = null
        _visibleMessages.value = emptyList()
        _attachments.value = emptyList()
        _streamingText.value = ""
        _streamingTitle.value = null
        _errorMessage.value = null
        _isStreaming.value = false
        _lastTokenCounts.value = 0 to 0
        streamingJob?.cancel()
        streamingJob = null
    }
}
