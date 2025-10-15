package com.example.innovexia.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.chat.TitleNamer
import com.example.innovexia.data.ai.GeminiException
import com.example.innovexia.data.ai.GeminiService
import com.example.innovexia.data.ai.RateLimitException
import com.example.innovexia.data.local.entities.ChatEntity
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.data.local.entities.MsgStatus
import com.example.innovexia.data.repository.ChatRepository
import com.example.innovexia.memory.Mind.api.ChatTurn
import com.example.innovexia.memory.Mind.api.MemoryEngine
import com.example.innovexia.memory.Mind.di.MindModule
import com.example.innovexia.ui.models.Persona
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for individual chat view.
 * Manages messages, streaming, reply, edit, delete, and retry operations.
 */
class ChatViewModel(
    private val application: Application,
    private val chatRepository: ChatRepository,
    private val geminiService: GeminiService,
    private val chatId: String,
    private val userPreferences: com.example.innovexia.data.preferences.UserPreferences
) : ViewModel() {

    // Memory engine for persona memory - initialized on background thread
    private var memoryEngine: MemoryEngine? = null

    // Chat entity
    private val _chat = MutableStateFlow<ChatEntity?>(null)
    val chat: StateFlow<ChatEntity?> = _chat.asStateFlow()

    // Chat title
    val title: StateFlow<String> = _chat.map { it?.title ?: "New chat" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "New chat")

    // Messages
    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    // New chat state (true when no messages yet)
    val isNewChat: StateFlow<Boolean> = _messages.map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Incognito mode state
    val isIncognito: StateFlow<Boolean> = _chat.map { it?.isIncognito == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Streaming state
    private val _streamingMessageId = MutableStateFlow<String?>(null)
    val streamingMessageId: StateFlow<String?> = _streamingMessageId.asStateFlow()

    // Error messages
    private val _errorMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val errorMessageIds: StateFlow<Set<String>> = _errorMessageIds.asStateFlow()

    // Truncated messages (hit token limit)
    private val _truncatedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val truncatedMessageIds: StateFlow<Set<String>> = _truncatedMessageIds.asStateFlow()

    // Reply target
    private val _replyTarget = MutableStateFlow<MessageEntity?>(null)
    val replyTarget: StateFlow<MessageEntity?> = _replyTarget.asStateFlow()

    // Error
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Rate limit countdown
    private val _rateLimitSecondsRemaining = MutableStateFlow<Int?>(null)
    val rateLimitSecondsRemaining: StateFlow<Int?> = _rateLimitSecondsRemaining.asStateFlow()

    // Crash-safe streaming scope with SupervisorJob so one stream failing doesn't cancel siblings
    private val streamScope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate)

    // Streaming job
    private var streamingJob: Job? = null
    private var countdownJob: Job? = null

    // Regeneration jobs (messageId -> Job)
    private val regenJobs = mutableMapOf<String, Job>()

    // Prevent double-send
    private var isSending = false

    // Regenerate throttle
    private var lastRegenTime = 0L
    private val REGEN_THROTTLE_MS = 1500L

    // Edit mode
    private val _editingMessageId = MutableStateFlow<String?>(null)
    val editingMessageId: StateFlow<String?> = _editingMessageId.asStateFlow()

    // Quote reply text
    private val _quoteReplyText = MutableStateFlow<String?>(null)
    val quoteReplyText: StateFlow<String?> = _quoteReplyText.asStateFlow()

    // Selected persona
    private val _selectedPersona = MutableStateFlow<Persona?>(null)
    val selectedPersona: StateFlow<Persona?> = _selectedPersona.asStateFlow()

    // Track if last response was truncated (MAX_TOKENS)
    private val _truncatedMessageId = MutableStateFlow<String?>(null)
    val truncatedMessageId: StateFlow<String?> = _truncatedMessageId.asStateFlow()

    // Grounding with Google Search
    private val _groundingEnabled = MutableStateFlow(false)
    val groundingEnabled: StateFlow<Boolean> = _groundingEnabled.asStateFlow()

    // Grounding metadata map (messageId -> GroundingMetadata)
    private val _groundingDataMap = MutableStateFlow<Map<String, com.example.innovexia.data.ai.GroundingMetadata>>(emptyMap())
    val groundingDataMap: StateFlow<Map<String, com.example.innovexia.data.ai.GroundingMetadata>> = _groundingDataMap.asStateFlow()

    // Grounding status map (messageId -> GroundingStatus)
    private val _groundingStatusMap = MutableStateFlow<Map<String, com.example.innovexia.data.ai.GroundingStatus>>(emptyMap())
    val groundingStatusMap: StateFlow<Map<String, com.example.innovexia.data.ai.GroundingStatus>> = _groundingStatusMap.asStateFlow()

    // Voice input state
    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _voiceTranscription = MutableStateFlow<String?>(null)
    val voiceTranscription: StateFlow<String?> = _voiceTranscription.asStateFlow()

    init {
        // Initialize memory engine on background thread to avoid blocking UI
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            memoryEngine = MindModule.provideMemoryEngine(application)
        }

        // Initialize grounding state from UserPreferences
        viewModelScope.launch {
            userPreferences.groundingEnabled.collect { enabled ->
                _groundingEnabled.value = enabled
                android.util.Log.d("ChatViewModel", "Grounding preference updated: $enabled")
            }
        }

        loadChat()
    }

    /**
     * Set the selected persona for this chat
     */
    fun setSelectedPersona(persona: Persona?) {
        _selectedPersona.value = persona
    }

    /**
     * Enable/disable Google Search grounding
     */
    fun setGroundingEnabled(enabled: Boolean) {
        android.util.Log.d("ChatViewModel", "setGroundingEnabled called with: $enabled")
        // Update local state immediately for instant UI response
        _groundingEnabled.value = enabled
        android.util.Log.d("ChatViewModel", "Local state updated to: ${_groundingEnabled.value}")

        // Save to preferences asynchronously
        viewModelScope.launch {
            userPreferences.setGroundingEnabled(enabled)
            android.util.Log.d("ChatViewModel", "Grounding ${if (enabled) "enabled" else "disabled"} and saved to preferences")
        }
    }

    /**
     * Start voice input recording
     */
    fun startVoiceInput() {
        android.util.Log.d("ChatViewModel", "Starting voice input")
        _isRecordingVoice.value = true
        _voiceTranscription.value = null
    }

    /**
     * Stop voice input recording
     */
    fun stopVoiceInput() {
        android.util.Log.d("ChatViewModel", "Stopping voice input")
        _isRecordingVoice.value = false
    }

    /**
     * Handle voice transcription result
     */
    fun onVoiceTranscription(text: String) {
        android.util.Log.d("ChatViewModel", "Voice transcription received: $text")
        _voiceTranscription.value = text
        _isRecordingVoice.value = false
    }

    /**
     * Clear voice transcription
     */
    fun clearVoiceTranscription() {
        _voiceTranscription.value = null
    }

    private fun loadChat() {
        viewModelScope.launch {
            // Load chat entity
            launch {
                chatRepository.observeChatById(chatId).collect { chat ->
                    _chat.value = chat

                    // Log memory-related state for debugging
                    if (chat != null) {
                        android.util.Log.d("ChatViewModel", "Chat loaded: chatId=${chat.id}, incognito=${chat.isIncognito}, personaId=${_selectedPersona.value?.id}")

                        // Log memory state if persona is set
                        val persona = _selectedPersona.value
                        if (persona != null && memoryEngine != null) {
                            viewModelScope.launch {
                                try {
                                    val memoryEnabled = memoryEngine?.isEnabled(persona.id) ?: false
                                    android.util.Log.d("ChatViewModel", "Memory state: personaId=${persona.id}, name=${persona.name}, memoryEnabled=$memoryEnabled")
                                } catch (e: Exception) {
                                    android.util.Log.e("ChatViewModel", "Failed to check memory state: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }

            // Load messages
            chatRepository.messagesForChat(chatId).collect { msgs ->
                _messages.value = msgs

                // Load grounding metadata and status from database for all messages
                val groundingMap = mutableMapOf<String, com.example.innovexia.data.ai.GroundingMetadata>()
                val groundingStatusMap = mutableMapOf<String, com.example.innovexia.data.ai.GroundingStatus>()

                android.util.Log.d("ChatViewModel", "=== Loading grounding data from database for ${msgs.size} messages ===")

                msgs.forEach { message ->
                    // Log raw database values for debugging
                    if (message.groundingJson != null || message.groundingStatus != "NONE") {
                        android.util.Log.d("ChatViewModel", "Message ${message.id}: groundingJson=${message.groundingJson?.take(100)}..., groundingStatus=${message.groundingStatus}")
                    }

                    message.groundingMetadata()?.let { metadata ->
                        groundingMap[message.id] = metadata
                        android.util.Log.d("ChatViewModel", "✓ Loaded grounding metadata for message ${message.id}: ${com.example.innovexia.data.ai.GroundingService.getGroundingSummary(metadata)}")
                    }
                    val status = message.getGroundingStatusEnum()
                    if (status != com.example.innovexia.data.ai.GroundingStatus.NONE) {
                        groundingStatusMap[message.id] = status
                        android.util.Log.d("ChatViewModel", "✓ Loaded grounding status for message ${message.id}: $status")
                    }
                }

                if (groundingMap.isNotEmpty()) {
                    _groundingDataMap.value = groundingMap
                    android.util.Log.d("ChatViewModel", "📦 Loaded ${groundingMap.size} grounding metadata entries from database")
                } else {
                    android.util.Log.w("ChatViewModel", "⚠️ NO grounding metadata found in database")
                }

                if (groundingStatusMap.isNotEmpty()) {
                    _groundingStatusMap.value = groundingStatusMap
                    android.util.Log.d("ChatViewModel", "📦 Loaded ${groundingStatusMap.size} grounding status entries from database")
                } else {
                    android.util.Log.w("ChatViewModel", "⚠️ NO grounding status found in database")
                }

                // Trigger AI-powered auto-naming based on message count
                // The repository will check if it's the right time to update
                if (msgs.size >= 2) {
                    // Launch in background - don't block message loading
                    launch {
                        try {
                            chatRepository.generateAndUpdateAITitle(chatId)
                        } catch (e: Exception) {
                            // Ignore title generation errors
                        }
                    }
                }
            }
        }
    }

    /**
     * Send a message in this chat.
     */
    fun sendMessage(text: String, persona: Persona? = null) {
        if (text.isBlank()) return
        if (isSending) {
            android.util.Log.w("ChatViewModel", "sendMessage called while already sending - blocked")
            return
        }

        isSending = true
        android.util.Log.d("ChatViewModel", "sendMessage - text='$text'")

        viewModelScope.launch {
            try {
                _errorMessage.value = null

                // Add user message
                chatRepository.appendUserMessage(chatId, text)

                // Create model message placeholder
                val modelMsgId = java.util.UUID.randomUUID().toString()
                _streamingMessageId.value = modelMsgId

                // Stream response with crash-safe handling
                streamingJob = streamScope.launch {
                    val collector = StreamCollector(
                        onUpdate = { partial ->
                            // Update message in DB during streaming
                            viewModelScope.launch {
                                chatRepository.appendModelToken(
                                    chatId = chatId,
                                    messageId = modelMsgId,
                                    token = "",  // Don't re-append, just update streamed flag
                                    isFinal = false
                                )
                            }
                        },
                        onDone = { full ->
                            // Final flush
                            viewModelScope.launch {
                                chatRepository.appendModelToken(
                                    chatId = chatId,
                                    messageId = modelMsgId,
                                    token = "",
                                    isFinal = true
                                )
                            }
                        }
                    )

                    var finalInputTokens = 0
                    var finalOutputTokens = 0
                    var finalGroundingMetadata: com.example.innovexia.data.ai.GroundingMetadata? = null

                    try {
                        val persona = _selectedPersona.value?.toCorePersona()
                        android.util.Log.d("ChatViewModel", "Sending message with persona: id=${persona?.id}, name=${persona?.name}")

                        // Set initial grounding status if grounding is enabled
                        if (_groundingEnabled.value) {
                            android.util.Log.d("ChatViewModel", "🔍 Grounding ENABLED for message $modelMsgId")
                            _groundingStatusMap.value = _groundingStatusMap.value + (modelMsgId to com.example.innovexia.data.ai.GroundingStatus.SEARCHING)
                        } else {
                            android.util.Log.d("ChatViewModel", "❌ Grounding DISABLED for message $modelMsgId")
                        }

                        android.util.Log.d("ChatViewModel", "Sending message with groundingEnabled=${_groundingEnabled.value}")

                        geminiService.generateReplyWithTokens(
                            chatId = chatId,
                            userText = text,
                            persona = persona,
                            enableThinking = false,
                            groundingEnabled = _groundingEnabled.value
                        ).collect { chunk ->
                            collector.onToken(chunk.text)

                            // Capture token counts from API
                            if (chunk.inputTokens > 0 || chunk.outputTokens > 0) {
                                finalInputTokens = chunk.inputTokens
                                finalOutputTokens = chunk.outputTokens
                            }

                            // Update grounding status from chunk
                            if (chunk.groundingStatus != com.example.innovexia.data.ai.GroundingStatus.NONE) {
                                _groundingStatusMap.value = _groundingStatusMap.value + (modelMsgId to chunk.groundingStatus)
                            }

                            // Store grounding metadata if present (both in map and for DB)
                            if (chunk.groundingMetadata != null) {
                                finalGroundingMetadata = chunk.groundingMetadata
                                _groundingDataMap.value = _groundingDataMap.value + (modelMsgId to chunk.groundingMetadata)
                                android.util.Log.d("ChatViewModel", "🔍 Grounding data captured from API for message $modelMsgId: ${com.example.innovexia.data.ai.GroundingService.getGroundingSummary(chunk.groundingMetadata)}")
                            }

                            // Update DB with each token
                            chatRepository.appendModelToken(
                                chatId = chatId,
                                messageId = modelMsgId,
                                token = chunk.text,
                                isFinal = false,
                                groundingStatus = chunk.groundingStatus.takeIf { it != com.example.innovexia.data.ai.GroundingStatus.NONE }
                            )
                        }

                        // Successfully completed streaming
                        collector.complete()
                        val streamedText = collector.current()

                        // Log final token counts and grounding data
                        android.util.Log.d("ChatViewModel", "Streaming completed - Final tokens: Input=$finalInputTokens, Output=$finalOutputTokens")
                        android.util.Log.d("ChatViewModel", "==== FINAL GROUNDING DATA CHECK BEFORE SAVING ====")
                        android.util.Log.d("ChatViewModel", "finalGroundingMetadata is null? ${finalGroundingMetadata == null}")

                        // Update grounding status to SUCCESS if grounding was enabled (even if no sources found)
                        if (_groundingEnabled.value && _groundingStatusMap.value[modelMsgId] == com.example.innovexia.data.ai.GroundingStatus.SEARCHING) {
                            _groundingStatusMap.value = _groundingStatusMap.value + (modelMsgId to com.example.innovexia.data.ai.GroundingStatus.SUCCESS)
                            android.util.Log.d("ChatViewModel", "✅ Updated grounding status from SEARCHING to SUCCESS for completed message")
                        }

                        if (finalGroundingMetadata != null) {
                            android.util.Log.d("ChatViewModel", "✅ Will save grounding metadata to DB for message $modelMsgId: ${com.example.innovexia.data.ai.GroundingService.getGroundingSummary(finalGroundingMetadata)}")
                            android.util.Log.d("ChatViewModel", "Grounding status from map: ${_groundingStatusMap.value[modelMsgId]}")
                        } else {
                            android.util.Log.w("ChatViewModel", "❌ NO grounding metadata to save for message $modelMsgId (finalGroundingMetadata is NULL)")
                            android.util.Log.w("ChatViewModel", "Grounding status from map: ${_groundingStatusMap.value[modelMsgId]}")
                        }

                        // Mark complete in DB with grounding metadata and final status
                        chatRepository.appendModelToken(
                            chatId = chatId,
                            messageId = modelMsgId,
                            token = "",
                            isFinal = true,
                            groundingMetadata = finalGroundingMetadata,
                            groundingStatus = _groundingStatusMap.value[modelMsgId]
                        )

                        android.util.Log.d("ChatViewModel", "✅ appendModelToken called - database write should be complete")

                        // Ingest into memory if persona is selected
                        val currentPersona = _selectedPersona.value
                        val currentChat = _chat.value
                        if (currentPersona != null && currentChat != null && memoryEngine != null) {
                            try {
                                val turn = ChatTurn(
                                    userId = currentChat.ownerId,
                                    chatId = chatId,
                                    userMessage = text,
                                    assistantMessage = streamedText,
                                    timestamp = System.currentTimeMillis()
                                )
                                memoryEngine?.ingest(
                                    turn = turn,
                                    personaId = currentPersona.id,
                                    incognito = currentChat.isIncognito
                                )
                                android.util.Log.d("ChatViewModel", "Memory ingested for persona ${currentPersona.name}")
                            } catch (e: Exception) {
                                android.util.Log.e("ChatViewModel", "Failed to ingest memory: ${e.message}")
                            }
                        }

                        _streamingMessageId.value = null
                        isSending = false

                    } catch (e: com.google.ai.client.generativeai.type.ResponseStoppedException) {
                        // Gemini stopped early (MAX_TOKENS / SAFETY / OTHER)
                        val reason = e.message?.substringAfter("Reason: ")?.trim() ?: "MAX_TOKENS"
                        android.util.Log.w("ChatViewModel", "Response stopped: $reason")

                        collector.complete()  // Ensure final flush
                        finishWithTruncation(modelMsgId, reason, finalGroundingMetadata)

                        _streamingMessageId.value = null
                        isSending = false

                    } catch (e: RateLimitException) {
                        _streamingMessageId.value = null
                        _errorMessageIds.value = _errorMessageIds.value + modelMsgId
                        startCountdown(e.retryAfterSeconds)
                        isSending = false

                        // Update the message with rate limit error
                        chatRepository.appendModelToken(
                            chatId = chatId,
                            messageId = modelMsgId,
                            token = "⚠️ Rate limit exceeded. Please wait ${e.retryAfterSeconds} seconds before trying again.",
                            isFinal = true
                        )
                    } catch (e: CancellationException) {
                        // User canceled / nav away - just exit cleanly
                        android.util.Log.d("ChatViewModel", "Stream canceled by user")
                        _streamingMessageId.value = null
                        _groundingStatusMap.value = _groundingStatusMap.value - modelMsgId
                        _groundingDataMap.value = _groundingDataMap.value - modelMsgId
                        isSending = false
                        throw e  // Re-throw to respect coroutine cancellation

                    } catch (e: GeminiException) {
                        _streamingMessageId.value = null
                        _errorMessageIds.value = _errorMessageIds.value + modelMsgId
                        _groundingStatusMap.value = _groundingStatusMap.value - modelMsgId
                        _groundingDataMap.value = _groundingDataMap.value - modelMsgId
                        _errorMessage.value = "Couldn't reach model. Retry?"
                        isSending = false

                    } catch (e: Exception) {
                        _streamingMessageId.value = null
                        _errorMessageIds.value = _errorMessageIds.value + modelMsgId
                        _groundingStatusMap.value = _groundingStatusMap.value - modelMsgId
                        _groundingDataMap.value = _groundingDataMap.value - modelMsgId
                        _errorMessage.value = "Error: ${e.message}"
                        isSending = false
                    }
                }

            } catch (e: Exception) {
                _errorMessage.value = "Failed to send: ${e.message}"
                isSending = false
            }
        }
    }

    /**
     * Edit a user message (UI-only for now).
     * TODO: Implement resend if needed.
     */
    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            val message = _messages.value.find { it.id == messageId }
            if (message != null && message.role == "user") {
                // Update in database
                val updated = message.copy(text = newText)
                // TODO: Add update method to MessageDao
                // For now, this is a placeholder
            }
        }
    }

    /**
     * Delete a message (soft delete, UI-only).
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            // TODO: Add soft delete to MessageDao
            // For now, remove from error set if present
            _errorMessageIds.value = _errorMessageIds.value - messageId
        }
    }

    /**
     * Retry a failed model message.
     */
    fun retryMessage(messageId: String) {
        viewModelScope.launch {
            // Find the user message before this model message
            val messages = _messages.value
            val failedIndex = messages.indexOfFirst { it.id == messageId }
            if (failedIndex > 0) {
                val previousUserMsg = messages
                    .take(failedIndex)
                    .lastOrNull { it.role == "user" }

                if (previousUserMsg != null) {
                    // Clear error
                    _errorMessageIds.value = _errorMessageIds.value - messageId
                    // Resend
                    sendMessage(previousUserMsg.text)
                }
            }
        }
    }

    /**
     * Set reply target.
     */
    fun setReplyTarget(messageId: String) {
        val message = _messages.value.find { it.id == messageId }
        _replyTarget.value = message
    }

    /**
     * Clear reply target.
     */
    fun clearReplyTarget() {
        _replyTarget.value = null
    }

    /**
     * Update chat title.
     */
    fun updateTitle(newTitle: String) {
        viewModelScope.launch {
            chatRepository.updateTitleIfNeeded(chatId, newTitle)
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Stop streaming.
     */
    fun stopStreaming() {
        streamingJob?.cancel()
        _streamingMessageId.value = null
    }

    /**
     * Start rate limit countdown timer.
     */
    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        _rateLimitSecondsRemaining.value = seconds

        countdownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000)
                remaining--
                _rateLimitSecondsRemaining.value = remaining
            }
            _rateLimitSecondsRemaining.value = null
        }
    }

    /**
     * Copy message text to clipboard (handled by UI)
     */
    fun copyMessage(message: MessageEntity) {
        // Clipboard handling is done in the UI layer
    }

    /**
     * Start editing a message
     */
    fun startEditingMessage(messageId: String) {
        android.util.Log.d("ChatViewModel", "Starting edit for message: $messageId")
        _editingMessageId.value = messageId
        android.util.Log.d("ChatViewModel", "Editing message ID now: ${_editingMessageId.value}")
    }

    /**
     * Cancel editing
     */
    fun cancelEditing() {
        _editingMessageId.value = null
    }

    /**
     * Resend edited message (creates new message, doesn't mutate original)
     */
    fun resendEdited(original: MessageEntity, newText: String) {
        if (newText.isBlank()) return

        viewModelScope.launch {
            try {
                _errorMessage.value = null
                _editingMessageId.value = null

                val now = System.currentTimeMillis()
                val editedMsgId = UUID.randomUUID().toString()

                // Create new user message with supersedes reference
                val editedMsg = original.copy(
                    id = editedMsgId,
                    text = newText,
                    createdAt = now,
                    updatedAt = now,
                    status = MsgStatus.SENDING.name,
                    editedAt = now,
                    supersedesMessageId = original.id
                )

                chatRepository.insertMessage(editedMsg)

                // Generate AI response with crash-safe streaming
                val modelMsgId = UUID.randomUUID().toString()
                _streamingMessageId.value = modelMsgId

                streamingJob = streamScope.launch {
                    val collector = StreamCollector(
                        onUpdate = { partial -> /* DB updates happen in collect */ },
                        onDone = { full -> /* Final flush in complete() */ }
                    )

                    var finalGroundingMetadata: com.example.innovexia.data.ai.GroundingMetadata? = null

                    try {
                        // Set initial grounding status if grounding is enabled
                        if (_groundingEnabled.value) {
                            _groundingStatusMap.value = _groundingStatusMap.value + (modelMsgId to com.example.innovexia.data.ai.GroundingStatus.SEARCHING)
                        }

                        geminiService.generateReplyWithTokens(
                            chatId = chatId,
                            userText = newText,
                            persona = null,
                            enableThinking = false,
                            groundingEnabled = _groundingEnabled.value
                        ).collect { chunk ->
                            collector.onToken(chunk.text)

                            // Update grounding status from chunk
                            if (chunk.groundingStatus != com.example.innovexia.data.ai.GroundingStatus.NONE) {
                                _groundingStatusMap.value = _groundingStatusMap.value + (modelMsgId to chunk.groundingStatus)
                            }

                            // Store grounding metadata if present
                            if (chunk.groundingMetadata != null) {
                                finalGroundingMetadata = chunk.groundingMetadata
                                _groundingDataMap.value = _groundingDataMap.value + (modelMsgId to chunk.groundingMetadata)
                            }

                            chatRepository.appendModelToken(
                                chatId = chatId,
                                messageId = modelMsgId,
                                token = chunk.text,
                                isFinal = false,
                                groundingStatus = chunk.groundingStatus.takeIf { it != com.example.innovexia.data.ai.GroundingStatus.NONE }
                            )
                        }

                        // Successfully completed
                        collector.complete()
                        val streamedText = collector.current()

                        // Mark complete with grounding data
                        chatRepository.appendModelToken(
                            chatId = chatId,
                            messageId = modelMsgId,
                            token = "",
                            isFinal = true,
                            groundingMetadata = finalGroundingMetadata,
                            groundingStatus = _groundingStatusMap.value[modelMsgId]
                        )

                        // Ingest into memory if persona is selected
                        val currentPersona = _selectedPersona.value
                        val currentChat = _chat.value
                        if (currentPersona != null && currentChat != null && !currentChat.isIncognito && memoryEngine != null) {
                            try {
                                val turn = ChatTurn(
                                    userId = currentChat.ownerId,
                                    chatId = chatId,
                                    userMessage = newText,
                                    assistantMessage = streamedText,
                                    timestamp = System.currentTimeMillis()
                                )
                                memoryEngine?.ingest(
                                    turn = turn,
                                    personaId = currentPersona.id,
                                    incognito = currentChat.isIncognito
                                )
                                android.util.Log.d("ChatViewModel", "Memory ingested for edited message - persona ${currentPersona.name}")
                            } catch (e: Exception) {
                                android.util.Log.e("ChatViewModel", "Failed to ingest memory for edited message: ${e.message}")
                            }
                        }

                        // Update edited message status to SENT
                        chatRepository.updateMessageStatus(editedMsgId, MsgStatus.SENT)
                        _streamingMessageId.value = null

                    } catch (e: com.google.ai.client.generativeai.type.ResponseStoppedException) {
                        // Handle truncation
                        val reason = e.message?.substringAfter("Reason: ")?.trim() ?: "MAX_TOKENS"
                        android.util.Log.w("ChatViewModel", "Edited response stopped: $reason")

                        collector.complete()
                        finishWithTruncation(modelMsgId, reason, finalGroundingMetadata)

                        chatRepository.updateMessageStatus(editedMsgId, MsgStatus.SENT)
                        _streamingMessageId.value = null

                    } catch (e: CancellationException) {
                        android.util.Log.d("ChatViewModel", "Edited stream canceled")
                        _streamingMessageId.value = null
                        _groundingStatusMap.value = _groundingStatusMap.value - modelMsgId
                        _groundingDataMap.value = _groundingDataMap.value - modelMsgId
                        throw e

                    } catch (e: Exception) {
                        _streamingMessageId.value = null
                        _errorMessageIds.value = _errorMessageIds.value + modelMsgId
                        _groundingStatusMap.value = _groundingStatusMap.value - modelMsgId
                        _groundingDataMap.value = _groundingDataMap.value - modelMsgId
                        chatRepository.updateMessageStatus(editedMsgId, MsgStatus.FAILED)
                        _errorMessage.value = "Failed to send edited message"
                    }
                }

            } catch (e: Exception) {
                _errorMessage.value = "Failed to resend: ${e.message}"
            }
        }
    }

    /**
     * Quote a message (adds it to composer)
     */
    fun quoteMessage(message: MessageEntity) {
        _quoteReplyText.value = "> ${message.text}"
    }

    /**
     * Clear quote reply
     */
    fun clearQuoteReply() {
        _quoteReplyText.value = null
    }

    /**
     * Retry failed user message
     */
    fun retryUserMessage(message: MessageEntity) {
        if (message.role != "user") return

        viewModelScope.launch {
            try {
                // Update status to SENDING
                chatRepository.updateMessageStatus(message.id, MsgStatus.SENDING)
                _errorMessageIds.value = _errorMessageIds.value - message.id

                // Retry sending
                sendMessage(message.text)

            } catch (e: Exception) {
                chatRepository.updateMessageStatus(message.id, MsgStatus.FAILED)
                _errorMessage.value = "Retry failed: ${e.message}"
            }
        }
    }

    /**
     * Delete message (soft delete)
     */
    fun deleteUserMessage(messageId: String) {
        viewModelScope.launch {
            chatRepository.softDeleteMessage(messageId)
        }
    }

    /**
     * Toggle incognito mode for this chat
     */
    fun toggleIncognito(enabled: Boolean) {
        viewModelScope.launch {
            chatRepository.toggleIncognito(chatId, enabled)
        }
    }

    /**
     * Move incognito chat to cloud
     */
    fun moveChatToCloud(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            chatRepository.moveChatToCloud(chatId)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Failed to move chat to cloud") }
        }
    }

    /**
     * Regenerate assistant message in-place (no new message created)
     */
    fun regenerateAssistant(messageId: String) {
        // Throttle to prevent spam clicks
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastRegenTime < REGEN_THROTTLE_MS) {
            android.util.Log.d("ChatViewModel", "Regenerate throttled - too soon")
            return
        }
        lastRegenTime = now

        val assistantMsg = _messages.value.firstOrNull {
            it.id == messageId && it.role == "model"
        } ?: return

        // Don't allow regenerate if already streaming
        if (assistantMsg.getStreamStateEnum() == com.example.innovexia.data.local.entities.StreamState.STREAMING) {
            android.util.Log.d("ChatViewModel", "Regenerate blocked - already streaming")
            return
        }

        // Find the user message that this assistant reply answered
        val userTurn = findUserTurnFor(assistantMsg) ?: return

        // Cancel any previous stream on this bubble
        regenJobs[messageId]?.cancel()

        streamScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val collector = StreamCollector(
                onUpdate = { partial -> /* Updates happen in collect */ },
                onDone = { full -> /* Final flush in complete() */ }
            )

            var finalGroundingMetadata: com.example.innovexia.data.ai.GroundingMetadata? = null

            try {
                // Increment regen count and set streaming state
                chatRepository.bumpRegenCount(messageId, now)
                chatRepository.updateMessageStreamState(messageId, com.example.innovexia.data.local.entities.StreamState.STREAMING.name, now)

                // Clear the bubble text
                chatRepository.overwriteMessageText(messageId, "", now)

                // Set initial grounding status if grounding is enabled
                if (_groundingEnabled.value) {
                    _groundingStatusMap.value = _groundingStatusMap.value + (messageId to com.example.innovexia.data.ai.GroundingStatus.SEARCHING)
                }

                // Stream the new response into the same row with crash-safe handling
                geminiService.generateReplyWithTokens(
                    chatId = chatId,
                    userText = userTurn.text,
                    persona = null,
                    enableThinking = false,
                    groundingEnabled = _groundingEnabled.value
                ).collect { chunk ->
                    collector.onToken(chunk.text)

                    // Update grounding status from chunk
                    if (chunk.groundingStatus != com.example.innovexia.data.ai.GroundingStatus.NONE) {
                        _groundingStatusMap.value = _groundingStatusMap.value + (messageId to chunk.groundingStatus)
                    }

                    // Store NEW grounding metadata (replace old data on regeneration)
                    if (chunk.groundingMetadata != null) {
                        finalGroundingMetadata = chunk.groundingMetadata
                        _groundingDataMap.value = _groundingDataMap.value + (messageId to chunk.groundingMetadata)
                    }

                    chatRepository.overwriteMessageText(messageId, collector.current(), System.currentTimeMillis())
                }

                // Successfully completed
                collector.complete()
                val streamedText = collector.current()

                // Mark complete and save grounding data to database
                val message = chatRepository.getMessageById(messageId)
                if (message != null) {
                    val updated = message.copy(
                        streamState = com.example.innovexia.data.local.entities.StreamState.IDLE.name,
                        updatedAt = System.currentTimeMillis(),
                        groundingJson = finalGroundingMetadata?.let {
                            com.example.innovexia.data.models.GroundingMetadataSerializer.toJson(it)
                        },
                        groundingStatus = _groundingStatusMap.value[messageId]?.name ?: message.groundingStatus
                    )
                    chatRepository.updateMessage(updated)
                } else {
                    // Fallback if message not found
                    chatRepository.updateMessageStreamState(messageId, com.example.innovexia.data.local.entities.StreamState.IDLE.name, System.currentTimeMillis())
                }

                // Ingest regenerated response into memory
                val currentPersona = _selectedPersona.value
                val currentChat = _chat.value
                if (currentPersona != null && currentChat != null && !currentChat.isIncognito && memoryEngine != null) {
                    try {
                        val turn = ChatTurn(
                            userId = currentChat.ownerId,
                            chatId = chatId,
                            userMessage = userTurn.text,
                            assistantMessage = streamedText,
                            timestamp = System.currentTimeMillis()
                        )
                        memoryEngine?.ingest(
                            turn = turn,
                            personaId = currentPersona.id,
                            incognito = currentChat.isIncognito
                        )
                        android.util.Log.d("ChatViewModel", "Memory ingested for regenerated response - persona ${currentPersona.name}")
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "Failed to ingest memory for regenerated response: ${e.message}")
                    }
                }

            } catch (e: com.google.ai.client.generativeai.type.ResponseStoppedException) {
                // Handle truncation in regeneration
                val reason = e.message?.substringAfter("Reason: ")?.trim() ?: "MAX_TOKENS"
                android.util.Log.w("ChatViewModel", "Regenerated response stopped: $reason")

                collector.complete()  // Ensure final flush

                // Mark complete but truncated - save grounding data
                val message = chatRepository.getMessageById(messageId)
                if (message != null) {
                    val updated = message.copy(
                        streamState = com.example.innovexia.data.local.entities.StreamState.IDLE.name,
                        updatedAt = System.currentTimeMillis(),
                        groundingJson = finalGroundingMetadata?.let {
                            com.example.innovexia.data.models.GroundingMetadataSerializer.toJson(it)
                        },
                        groundingStatus = _groundingStatusMap.value[messageId]?.name ?: message.groundingStatus
                    )
                    chatRepository.updateMessage(updated)
                } else {
                    // Fallback if message not found
                    chatRepository.updateMessageStreamState(messageId, com.example.innovexia.data.local.entities.StreamState.IDLE.name, System.currentTimeMillis())
                }
                _truncatedMessageIds.value = _truncatedMessageIds.value + messageId

            } catch (e: CancellationException) {
                android.util.Log.d("ChatViewModel", "Regeneration canceled for $messageId")
                _groundingStatusMap.value = _groundingStatusMap.value - messageId
                _groundingDataMap.value = _groundingDataMap.value - messageId
                throw e

            } catch (t: Throwable) {
                _groundingStatusMap.value = _groundingStatusMap.value - messageId
                _groundingDataMap.value = _groundingDataMap.value - messageId
                chatRepository.markMessageError(
                    messageId,
                    t.message ?: "Generation failed",
                    com.example.innovexia.data.local.entities.StreamState.ERROR.name,
                    System.currentTimeMillis()
                )
            }
        }.also { regenJobs[messageId] = it }
    }

    /**
     * Find the user message that this assistant reply answered (previous user msg)
     */
    private fun findUserTurnFor(assistantMsg: MessageEntity): MessageEntity? {
        val idx = _messages.value.indexOfFirst { it.id == assistantMsg.id }
        if (idx == -1) return null
        return _messages.value.take(idx).lastOrNull { it.role == "user" }
    }

    /**
     * Handle suggestion card clicks from SmartGreetingScreen
     * Creates full contextual messages based on the suggestion type and content
     */
    fun handleSuggestionClick(suggestion: com.example.innovexia.ui.chat.newchat.suggestions.SuggestionCardUi, composerCallback: (String) -> Unit) {
        val messageText = when (suggestion.kind) {
            com.example.innovexia.ui.chat.newchat.suggestions.SuggestionKind.CONTINUE_TASK -> {
                // Create a contextual message asking to continue the task
                if (suggestion.subtitle.isNullOrBlank()) {
                    "I'd like to continue working on: ${suggestion.title}"
                } else {
                    "I'd like to continue working on ${suggestion.title.lowercase()}. ${suggestion.subtitle}"
                }
            }
            com.example.innovexia.ui.chat.newchat.suggestions.SuggestionKind.PICK_UP_TOPIC -> {
                // Create a contextual question about the topic
                if (suggestion.subtitle.isNullOrBlank()) {
                    "Tell me more about ${suggestion.title.lowercase()}"
                } else {
                    "I want to discuss ${suggestion.title.lowercase()}. ${suggestion.subtitle}"
                }
            }
            com.example.innovexia.ui.chat.newchat.suggestions.SuggestionKind.RECAP_FILE -> {
                // Ask about the file/source
                if (suggestion.subtitle.isNullOrBlank()) {
                    "Can you summarize ${suggestion.title}?"
                } else {
                    "Tell me about ${suggestion.title} - ${suggestion.subtitle.lowercase()}"
                }
            }
            com.example.innovexia.ui.chat.newchat.suggestions.SuggestionKind.QUICK_ACTION -> {
                // For quick actions, make it a request
                if (suggestion.subtitle.isNullOrBlank()) {
                    "Help me with: ${suggestion.title.lowercase()}"
                } else {
                    "${suggestion.title} - ${suggestion.subtitle.lowercase()}"
                }
            }
        }

        // Send the contextual message directly
        sendMessage(messageText, _selectedPersona.value)
    }

    /**
     * Continue a truncated response that hit the token limit.
     * Appends to the existing message rather than creating a new one.
     */
    fun continueResponse(messageId: String) {
        val message = _messages.value.firstOrNull { it.id == messageId && it.role == "model" } ?: return

        // Remove from truncated set
        _truncatedMessageIds.value = _truncatedMessageIds.value - messageId

        // Find the user message that prompted this response
        val userTurn = findUserTurnFor(message) ?: return

        streamScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existingText = message.text
            val collector = StreamCollector(
                onUpdate = { partial -> /* Updates happen in collect */ },
                onDone = { full -> /* Final flush in complete() */ }
            )

            var finalGroundingMetadata: com.example.innovexia.data.ai.GroundingMetadata? = null

            try {
                // Set streaming state
                chatRepository.updateMessageStreamState(messageId, com.example.innovexia.data.local.entities.StreamState.STREAMING.name, now)

                // Get the last sentence to use as context
                val lastSentence = existingText.split(". ").lastOrNull() ?: ""
                val continuePrompt = "Continue from: \"$lastSentence\""

                // Stream the continuation with crash-safe handling
                val sb = StringBuilder(existingText) // Start with existing text

                // Set searching status if grounding enabled
                if (_groundingEnabled.value) {
                    _groundingStatusMap.value = _groundingStatusMap.value + (messageId to com.example.innovexia.data.ai.GroundingStatus.SEARCHING)
                }

                geminiService.generateReplyWithTokens(
                    chatId = chatId,
                    userText = continuePrompt,
                    persona = null,
                    enableThinking = false,
                    groundingEnabled = _groundingEnabled.value
                ).collect { chunk ->
                    collector.onToken(chunk.text)

                    // Update grounding status from chunk
                    if (chunk.groundingStatus != com.example.innovexia.data.ai.GroundingStatus.NONE) {
                        _groundingStatusMap.value = _groundingStatusMap.value + (messageId to chunk.groundingStatus)
                    }

                    // Store NEW grounding metadata (replace old on continuation)
                    if (chunk.groundingMetadata != null) {
                        finalGroundingMetadata = chunk.groundingMetadata
                        _groundingDataMap.value = _groundingDataMap.value + (messageId to chunk.groundingMetadata)
                    }

                    sb.append(chunk.text)
                    chatRepository.overwriteMessageText(messageId, sb.toString(), System.currentTimeMillis())
                }

                // Successfully completed continuation
                collector.complete()
                val finalText = sb.toString()

                // Mark complete and save grounding data to database
                val msg = chatRepository.getMessageById(messageId)
                if (msg != null) {
                    val updated = msg.copy(
                        streamState = com.example.innovexia.data.local.entities.StreamState.IDLE.name,
                        updatedAt = System.currentTimeMillis(),
                        groundingJson = finalGroundingMetadata?.let {
                            com.example.innovexia.data.models.GroundingMetadataSerializer.toJson(it)
                        },
                        groundingStatus = _groundingStatusMap.value[messageId]?.name ?: msg.groundingStatus
                    )
                    chatRepository.updateMessage(updated)
                } else {
                    // Fallback if message not found
                    chatRepository.updateMessageStreamState(messageId, com.example.innovexia.data.local.entities.StreamState.IDLE.name, System.currentTimeMillis())
                }

                // Ingest continued response into memory
                val currentPersona = _selectedPersona.value
                val currentChat = _chat.value
                if (currentPersona != null && currentChat != null && !currentChat.isIncognito && memoryEngine != null) {
                    try {
                        val turn = ChatTurn(
                            userId = currentChat.ownerId,
                            chatId = chatId,
                            userMessage = userTurn.text,
                            assistantMessage = finalText,
                            timestamp = System.currentTimeMillis()
                        )
                        memoryEngine?.ingest(
                            turn = turn,
                            personaId = currentPersona.id,
                            incognito = currentChat.isIncognito
                        )
                        android.util.Log.d("ChatViewModel", "Memory ingested for continued response - persona ${currentPersona.name}")
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "Failed to ingest memory for continued response: ${e.message}")
                    }
                }

            } catch (e: com.google.ai.client.generativeai.type.ResponseStoppedException) {
                // Hit limit again during continuation
                val reason = e.message?.substringAfter("Reason: ")?.trim() ?: "MAX_TOKENS"
                android.util.Log.w("ChatViewModel", "Continuation stopped again: $reason")

                collector.complete()

                // Mark complete but still truncated - save grounding data
                val msg = chatRepository.getMessageById(messageId)
                if (msg != null) {
                    val updated = msg.copy(
                        streamState = com.example.innovexia.data.local.entities.StreamState.IDLE.name,
                        updatedAt = System.currentTimeMillis(),
                        groundingJson = finalGroundingMetadata?.let {
                            com.example.innovexia.data.models.GroundingMetadataSerializer.toJson(it)
                        },
                        groundingStatus = _groundingStatusMap.value[messageId]?.name ?: msg.groundingStatus
                    )
                    chatRepository.updateMessage(updated)
                } else {
                    // Fallback if message not found
                    chatRepository.updateMessageStreamState(messageId, com.example.innovexia.data.local.entities.StreamState.IDLE.name, System.currentTimeMillis())
                }
                _truncatedMessageIds.value = _truncatedMessageIds.value + messageId  // Keep truncated

            } catch (e: CancellationException) {
                android.util.Log.d("ChatViewModel", "Continuation canceled")
                _groundingStatusMap.value = _groundingStatusMap.value - messageId
                _groundingDataMap.value = _groundingDataMap.value - messageId
                throw e

            } catch (t: Throwable) {
                _groundingStatusMap.value = _groundingStatusMap.value - messageId
                _groundingDataMap.value = _groundingDataMap.value - messageId
                chatRepository.markMessageError(
                    messageId,
                    t.message ?: "Continuation failed",
                    com.example.innovexia.data.local.entities.StreamState.ERROR.name,
                    System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * Mark a message as truncated (hit token limit)
     */
    fun markMessageTruncated(messageId: String) {
        _truncatedMessageIds.value = _truncatedMessageIds.value + messageId
    }

    /**
     * Finish streaming with truncation indicator.
     * Called when Gemini stops early (MAX_TOKENS, SAFETY, etc.)
     */
    private fun finishWithTruncation(messageId: String, reason: String, groundingMetadata: com.example.innovexia.data.ai.GroundingMetadata? = null) {
        viewModelScope.launch {
            try {
                // Mark as truncated so UI shows "Continue" button
                _truncatedMessageIds.value = _truncatedMessageIds.value + messageId

                // Mark message as complete (streaming done, even if truncated)
                // Include grounding metadata if available
                chatRepository.appendModelToken(
                    chatId = chatId,
                    messageId = messageId,
                    token = "",
                    isFinal = true,
                    groundingMetadata = groundingMetadata,
                    groundingStatus = _groundingStatusMap.value[messageId]
                )

                android.util.Log.i("ChatViewModel", "Message $messageId truncated ($reason) - continue available")
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Failed to mark truncation: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
        countdownJob?.cancel()
        regenJobs.values.forEach { it.cancel() }
        regenJobs.clear()
    }
}
