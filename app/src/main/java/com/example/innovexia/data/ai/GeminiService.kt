package com.example.innovexia.data.ai

import android.content.Context
import com.example.innovexia.BuildConfig
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.core.persona.Persona
import com.example.innovexia.data.models.AttachmentKind
import com.example.innovexia.data.models.AttachmentMeta
import com.example.innovexia.data.models.SubscriptionGate
import com.example.innovexia.data.models.SubscriptionPlan
import com.example.innovexia.data.preferences.UserPreferences
import com.example.innovexia.memory.Mind.api.MemoryEngine
import com.example.innovexia.memory.Mind.sources.api.SourcesEngine
import com.example.innovexia.memory.Mind.di.MindModule
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
// import com.google.ai.client.generativeai.type.Tool  // Not available in SDK 0.9.0
// import com.google.ai.client.generativeai.type.googleSearch  // Not available in SDK 0.9.0
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/**
 * Service for interacting with Google's Gemini 2.5 models.
 *
 * All Gemini 2.5 Models (Pro, Flash, Flash Lite) Share These Limits:
 * - Input context window: 1,048,576 tokens (~800K words)
 * - Max output tokens: 65,536 tokens (~50K words)
 * - Supports: thinking, function calling, search grounding, structured outputs,
 *   caching, URL context, code execution
 *
 * Our Configuration:
 * - Default output: 8,192 tokens (12.5% of max) - balances quality vs. truncation
 * - User adjustable: 1,024 to 65,536 tokens via model switcher
 * - Input: Uses full 1M token window for context/memory/sources
 * - Truncation handled gracefully with "Continue" button
 *
 * Model Differences:
 * - Pro: Best for complex reasoning, analysis, large datasets
 * - Flash: Best price-performance, high-volume, agentic tasks
 * - Flash Lite: Fastest, most cost-efficient, high throughput
 *
 * API Key: Set GEMINI_API_KEY in local.properties
 * Get your key at: https://aistudio.google.com/app/apikey
 */
class GeminiService(
    private val database: AppDatabase? = null,
    private val context: Context? = null,
    private val userPreferences: UserPreferences? = null
) {

    private val memoryAssembler by lazy { database?.let { MemoryAssembler(it) } }
    private val memorySummarizer by lazy {
        database?.let { db -> memoryAssembler?.let { MemorySummarizer(db, it) } }
    }
    private val memoryEngine: MemoryEngine? by lazy {
        context?.let { MindModule.provideMemoryEngine(it) }
    }
    private val sourcesEngine: SourcesEngine? by lazy {
        context?.let { MindModule.provideSourcesEngine(it) }
    }

    // Advanced prompting components (v2.0)
    private val promptBuilder by lazy { PromptBuilder() }
    private val contextOptimizer by lazy { ContextOptimizer() }

    // REST API client for grounding support
    private val restClient by lazy { GeminiRestClient() }

    companion object {
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val TEMPERATURE = 0.7f
        private const val TOP_K = 40
        private const val TOP_P = 0.95f

        // Gemini 2.5 Flash supports up to 65,536 output tokens
        // Using 8,192 as default (12.5% of max) for optimal balance:
        // - Handles 95% of responses without truncation
        // - Faster response times
        // - Lower token costs
        // - Graceful "Continue" button for longer responses
        private const val MAX_OUTPUT_TOKENS = 8192

        // Can be increased for specific use cases:
        const val MAX_OUTPUT_TOKENS_EXTENDED = 16384  // For long-form content
        const val MAX_OUTPUT_TOKENS_FULL = 65536      // Maximum supported by Flash
    }

    /**
     * Check if the API key is configured.
     */
    fun isApiKeyConfigured(): Boolean {
        return BuildConfig.GEMINI_API_KEY.isNotBlank()
    }

    /**
     * Check if user has access to the specified model based on their subscription
     * @param modelId The model ID to check
     * @param plan The user's subscription plan
     * @return Pair<Boolean, String> - (hasAccess, errorMessage)
     */
    fun checkModelAccess(modelId: String, plan: SubscriptionPlan): Pair<Boolean, String> {
        val hasAccess = SubscriptionGate.hasModelAccess(plan, modelId)

        if (!hasAccess) {
            // Determine minimum required plan
            val requiredPlan = when {
                modelId.contains("gpt-5", ignoreCase = true) ||
                modelId.contains("claude", ignoreCase = true) ||
                modelId.contains("perplexity", ignoreCase = true) -> {
                    if (modelId.contains("perplexity-pro", ignoreCase = true)) {
                        SubscriptionPlan.MASTER
                    } else {
                        SubscriptionPlan.PRO
                    }
                }
                modelId.contains("pro", ignoreCase = true) -> SubscriptionPlan.PLUS
                else -> SubscriptionPlan.FREE
            }

            val message = SubscriptionGate.upgradeMessage(modelId, requiredPlan)
            return Pair(false, message)
        }

        return Pair(true, "")
    }

    /**
     * Stream text generation from Gemini.
     *
     * @param userText The user's input message
     * @param persona Optional persona to influence response style
     * @param enableThinking If true, adds reasoning instruction to system prompt
     * @return Flow of text tokens as they stream in
     */
    suspend fun streamText(
        userText: String,
        persona: Persona? = null,
        enableThinking: Boolean = false
    ): Flow<String> {
        if (!isApiKeyConfigured()) {
            throw IllegalStateException("Gemini API key not configured. Add GEMINI_API_KEY to local.properties")
        }

        val systemInstruction = buildSystemInstruction(persona, enableThinking)

        // Apply behavior settings from persona
        val actualTemperature = persona?.behavior?.creativityTemp ?: TEMPERATURE
        val actualTopP = persona?.behavior?.topP ?: TOP_P

        val model = GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = actualTemperature
                topK = TOP_K
                topP = actualTopP
                maxOutputTokens = MAX_OUTPUT_TOKENS
            },
            systemInstruction = systemInstruction.takeIf { it.isNotBlank() }?.let {
                com.google.ai.client.generativeai.type.content { text(it) }
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
            )
        )

        return model.generateContentStream(userText)
            .map { response ->
                response.text ?: ""
            }
            .catch { exception ->
                // Handle deserialization errors from malformed API responses
                android.util.Log.e("GeminiService", "Stream error: ${exception.message}", exception)

                // Check if it's a SerializationException (missing 'parts' field, etc.)
                if (exception.message?.contains("MissingFieldException") == true ||
                    exception.message?.contains("deserialize") == true) {
                    throw GeminiException("Failed to parse API response. The model may have returned an empty or blocked response.", exception)
                } else {
                    throw exception
                }
            }
            .onCompletion { cause ->
                // Handle completion with error
                if (cause != null) {
                    when (cause) {
                        is QuotaExceededException -> {
                            val retrySeconds = extractRetrySeconds(cause.message)
                            throw RateLimitException(
                                message = "Rate limit exceeded",
                                retryAfterSeconds = retrySeconds,
                                cause = cause
                            )
                        }
                        is ResponseStoppedException -> {
                            // MAX_TOKENS / SAFETY / OTHER - throw to allow VM to handle truncation gracefully
                            val reason = cause.message?.substringAfter("Reason: ")?.trim() ?: "MAX_TOKENS"
                            android.util.Log.w("GeminiService", "Response stopped: $reason - partial response delivered")
                            // Re-throw so ChatViewModel can handle gracefully and show "Continue" button
                            throw cause
                        }
                        else -> {
                            throw GeminiException("Failed to generate response: ${cause.message}", cause)
                        }
                    }
                }
            }
    }

    /**
     * Build system instruction based on persona and thinking mode.
     * Now includes behavior settings, personality traits, and rules.
     */
    private fun buildSystemInstruction(
        persona: Persona?,
        enableThinking: Boolean,
        userName: String? = null
    ): String {
        val parts = mutableListOf<String>()

        // 1. Base persona instruction with variable substitution
        if (!persona?.system.isNullOrBlank()) {
            parts.add(substituteVariables(persona!!.system!!, userName))
        } else if (!persona?.systemConfig?.instructions.isNullOrBlank()) {
            parts.add(substituteVariables(persona!!.systemConfig!!.instructions, userName))
        } else if (persona != null) {
            // Fallback: Generate a basic instruction from persona name and summary
            if (persona.summary.isNotBlank()) {
                parts.add("You are an AI assistant named ${persona.name}. ${persona.summary} Respond directly without prefixing your name.")
            } else {
                parts.add("You are an AI assistant named ${persona.name}. Respond directly without prefixing your name.")
            }
        }

        // 2. Apply personality traits from behavior settings
        persona?.behavior?.let { behavior ->
            val traits = mutableListOf<String>()

            // Conciseness: 0 = verbose, 1 = extremely concise
            when {
                behavior.conciseness >= 0.8f -> traits.add("Be extremely concise and brief in your responses")
                behavior.conciseness >= 0.6f -> traits.add("Keep your responses concise and to the point")
                behavior.conciseness <= 0.3f -> traits.add("Provide detailed and thorough explanations")
            }

            // Formality: 0 = casual, 1 = very formal
            when {
                behavior.formality >= 0.8f -> traits.add("Use very formal and professional language")
                behavior.formality >= 0.6f -> traits.add("Maintain a professional tone")
                behavior.formality <= 0.3f -> traits.add("Use casual and friendly language")
            }

            // Empathy: 0 = matter-of-fact, 1 = highly empathetic
            when {
                behavior.empathy >= 0.8f -> traits.add("Show warmth, understanding, and emotional support")
                behavior.empathy >= 0.6f -> traits.add("Be supportive and considerate")
                behavior.empathy <= 0.3f -> traits.add("Focus on facts and direct information")
            }

            // Citation policy
            when (behavior.citationPolicy) {
                "always" -> traits.add("Always cite sources and provide references")
                "when_uncertain" -> traits.add("Cite sources when uncertain or making factual claims")
                "never" -> {} // Don't add citation instruction
            }

            // Proactivity
            when (behavior.proactivity) {
                "proactive" -> traits.add("Proactively offer suggestions and additional information")
                "ask_when_unclear" -> traits.add("Ask for clarification when the request is unclear")
                "reactive" -> traits.add("Respond directly to what is asked without additional suggestions")
            }

            if (traits.isNotEmpty()) {
                parts.add("Communication style: ${traits.joinToString(". ")}.")
            }
        }

        // 3. System rules from persona config
        persona?.systemConfig?.rules?.let { rules ->
            rules.forEach { rule ->
                val shouldApply = when (rule.`when`) {
                    "always" -> true
                    "on_request" -> false // Only apply if user explicitly asks
                    else -> true // Default to applying the rule
                }

                if (shouldApply && rule.`do`.isNotBlank()) {
                    parts.add(substituteVariables(rule.`do`, userName))
                }
            }
        }

        // 4. Thinking mode (from behavior or explicit flag)
        val thinkingEnabled = enableThinking || (persona?.behavior?.thinkingDepth?.let { it != "off" } ?: false)
        if (thinkingEnabled) {
            val depth = persona?.behavior?.thinkingDepth ?: "balanced"
            when (depth) {
                "deep" -> parts.add("Think through problems step-by-step with detailed reasoning before providing your answer.")
                "balanced" -> parts.add("Reason through your response when needed, but keep your final answer clear and concise.")
                else -> {} // "off" - no thinking instruction
            }
        }

        return parts.joinToString("\n\n")
    }

    /**
     * Substitute variables in system instructions
     * Supports: {user_name}, {today}, {timezone}, {app_name}
     */
    private fun substituteVariables(text: String, userName: String?): String {
        var result = text

        // {user_name}
        result = result.replace("{user_name}", userName ?: "there")

        // {today}
        val today = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        result = result.replace("{today}", today)

        // {timezone}
        val timezone = java.util.TimeZone.getDefault().displayName
        result = result.replace("{timezone}", timezone)

        // {app_name}
        result = result.replace("{app_name}", "Innovexia")

        return result
    }

    /**
     * Generate a reply with contextual memory and optional attachments.
     * Assembles memory context, streams the response, and triggers summary refresh.
     * Returns both text and actual token counts from the API.
     *
     * **ENHANCED v2.0**: Now includes advanced prompting by default!
     * - Query complexity analysis
     * - Reasoning mode detection
     * - Dynamic context optimization (5-10% of 1M window)
     * - Multi-shot examples
     * - Chain-of-thought frameworks
     * - Expanded memory/sources retrieval
     *
     * Set `useAdvancedPrompting = false` to use the original basic prompting.
     *
     * @param chatId The chat ID
     * @param userText The user's latest message
     * @param persona Optional persona
     * @param enableThinking Enable step-by-step reasoning
     * @param attachments Optional list of attachments (images/PDFs)
     * @param context Android Context for reading URIs
     * @param useAdvancedPrompting Enable advanced prompting system (default: true)
     * @return Flow of StreamChunk containing text and token metadata
     */
    suspend fun generateReplyWithTokens(
        chatId: String,
        userText: String,
        persona: Persona? = null,
        enableThinking: Boolean = false,
        attachments: List<AttachmentMeta> = emptyList(),
        context: Context? = null,
        modelName: String = MODEL_NAME,
        temperature: Float = TEMPERATURE,
        maxOutputTokens: Int = MAX_OUTPUT_TOKENS,
        safetyLevel: String = "Standard",
        useAdvancedPrompting: Boolean = true,  // NEW: Enable advanced prompting by default
        groundingEnabled: Boolean = false  // NEW: Enable Google Search grounding
    ): Flow<StreamChunk> {
        if (!isApiKeyConfigured()) {
            throw IllegalStateException("Gemini API key not configured. Add GEMINI_API_KEY to local.properties")
        }

        // ============ ROUTE TO ADVANCED PROMPTING IF ENABLED ============
        if (useAdvancedPrompting) {
            return generateReplyEnhanced(
                chatId = chatId,
                userText = userText,
                persona = persona,
                enableThinking = enableThinking,
                attachments = attachments,
                context = context,
                modelName = modelName,
                temperature = temperature,
                maxOutputTokens = maxOutputTokens,
                safetyLevel = safetyLevel,
                useAdvancedPrompting = true,
                groundingEnabled = groundingEnabled
            )
        }

        // ============ ORIGINAL BASIC PROMPTING (Fallback) ============
        // Phase 2: Retrieve persona memory context BEFORE assembling (to pass to MemoryAssembler)
        // IMPORTANT: Guest users should NEVER have access to persona memory (only conversation history)
        val isGuest = Firebase.auth.currentUser == null
        val personaMemoryContext = if (persona != null && !isGuest) {
            android.util.Log.d("GeminiService", "Phase 2: Retrieving persona memory to include in system instructions")
            val engine = memoryEngine
            if (engine != null) {
                try {
                    val ctx = engine.contextFor(userText, persona.id, chatId)
                    android.util.Log.d("GeminiService", "Retrieved memory context: longTerm=${ctx.longTerm.size}, shortTerm=${ctx.shortTerm.size}, totalTokens=${ctx.totalTokens}")
                    if (ctx.longTerm.isNotEmpty()) {
                        android.util.Log.d("GeminiService", "Memory preview: ${ctx.longTerm.take(3).joinToString("; ") { it.memory.text }}")
                    } else {
                        android.util.Log.w("GeminiService", "No memories found for persona ${persona.id}!")
                    }
                    ctx
                } catch (e: Exception) {
                    android.util.Log.e("GeminiService", "Failed to retrieve persona memory: ${e.message}", e)
                    null
                }
            } else {
                android.util.Log.e("GeminiService", "MemoryEngine is null!")
                null
            }
        } else {
            if (isGuest) {
                android.util.Log.d("GeminiService", "Guest user: skipping persona memory (only conversation history available)")
            } else {
                android.util.Log.d("GeminiService", "Persona is null, skipping memory context")
            }
            null
        }

        // Phase 2: Retrieve PDF sources context BEFORE assembling (to pass to MemoryAssembler)
        // IMPORTANT: Guest users should NEVER have access to PDF sources (only conversation history)
        val pdfSourcesContext = if (persona != null && !isGuest) {
            android.util.Log.d("GeminiService", "Phase 2: Retrieving PDF sources to include in system instructions")
            val engine = sourcesEngine
            if (engine != null) {
                try {
                    val ctx = engine.getContextForQuery(persona.id, userText, limit = 3)
                    android.util.Log.d("GeminiService", "Retrieved sources context: ${ctx.length} chars")
                    android.util.Log.d("GeminiService", "Context preview: ${ctx.take(200)}")
                    ctx
                } catch (e: Exception) {
                    android.util.Log.e("GeminiService", "Failed to retrieve PDF sources: ${e.message}", e)
                    ""
                }
            } else {
                android.util.Log.e("GeminiService", "SourcesEngine is null!")
                ""
            }
        } else {
            if (isGuest) {
                android.util.Log.d("GeminiService", "Guest user: skipping PDF sources (only conversation history available)")
            } else {
                android.util.Log.d("GeminiService", "Persona is null, skipping sources context")
            }
            ""
        }

        // Phase 2: Assemble context WITH persona memory and PDF sources (will be in system instructions)
        // Skip memory assembler for guests - they only get conversation history, no persona context
        val memoryContext = if (!isGuest) {
            memoryAssembler?.assembleContext(
                chatId = chatId,
                persona = persona,
                enableThinking = enableThinking,
                personaMemoryContext = personaMemoryContext,
                pdfSourcesContext = pdfSourcesContext
            )
        } else {
            null
        }

        // Phase 2: Build clean prompt with ONLY conversation history and current message
        // (Memory context now in system instructions via MemoryAssembler)
        val promptParts = mutableListOf<String>()

        android.util.Log.d("GeminiService", "Phase 2: Building prompt with clean conversation history (memory in system instructions)")

        // Add conversation summary if present (kept in prompt for continuity)
        if (!memoryContext?.memorySummary.isNullOrBlank()) {
            promptParts.add("Conversation summary:\n${memoryContext?.memorySummary}")
        }

        // Add recent messages
        memoryContext?.recentWindow?.forEach { msg ->
            val roleLabel = if (msg.role == "user") "User" else "Assistant"
            promptParts.add("$roleLabel: ${msg.text}")
        }

        // Add current user message
        promptParts.add("User: $userText")

        val fullPrompt = promptParts.joinToString("\n\n")

        // Log the final prompt for debugging
        android.util.Log.d("GeminiService", "Final prompt length: ${fullPrompt.length} chars")
        android.util.Log.d("GeminiService", "Prompt preview (first 500 chars):\n${fullPrompt.take(500)}")

        // Build content with text and attachments
        val requestContent = content {
            text(fullPrompt)

            // Add attachment parts
            if (context != null) {
                attachments.forEach { attachment ->
                    try {
                        attachment.localUri?.let { uri ->
                            // Read bytes from URI using ContentResolver
                            val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                inputStream.readBytes()
                            }

                            if (bytes != null && bytes.isNotEmpty()) {
                                blob(attachment.mime, bytes)
                            }
                        }
                    } catch (e: Exception) {
                        // Log error but continue processing other attachments
                        android.util.Log.e("GeminiService", "Failed to read attachment: ${e.message}")
                    }
                }
            }
        }

        // Build system instruction
        val systemInstruction = memoryContext?.systemPreamble ?: buildSystemInstruction(persona, enableThinking)

        // Safety settings - use BLOCK_NONE to allow all content including medical advice
        // The app handles appropriate disclaimers via UI
        val safetyThreshold = when (safetyLevel) {
            "Standard" -> BlockThreshold.NONE  // Allow all content, show disclaimers in UI
            "Strict" -> BlockThreshold.MEDIUM_AND_ABOVE  // More restrictive if needed
            else -> BlockThreshold.NONE
        }

        val model = GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                this.temperature = temperature
                topK = TOP_K
                topP = TOP_P
                this.maxOutputTokens = maxOutputTokens
            },
            systemInstruction = systemInstruction.takeIf { it.isNotBlank() }?.let {
                content { text(it) }
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, safetyThreshold),
                SafetySetting(HarmCategory.HATE_SPEECH, safetyThreshold),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, safetyThreshold),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, safetyThreshold)
            )
        )

        // Trigger background summary refresh after response completes
        CoroutineScope(Dispatchers.IO).launch {
            try {
                memorySummarizer?.maybeRefreshSummary(chatId)
            } catch (e: Exception) {
                // Silent fail - memory refresh is non-critical
            }
        }

        // Calculate user message tokens ONLY (not including memories/system prompts)
        val userMessageTokenCount = com.example.innovexia.subscriptions.mock.TokenCounter.estimateInputTokens(userText).toInt()
        android.util.Log.d("GeminiService", "User message token estimate: $userMessageTokenCount tokens")

        var latestInputTokens = 0
        var latestOutputTokens = 0

        return model.generateContentStream(requestContent)
            .map { response ->
                // Capture token usage from response metadata
                response.usageMetadata?.let { metadata ->
                    latestInputTokens = metadata.promptTokenCount ?: 0
                    latestOutputTokens = metadata.candidatesTokenCount ?: 0
                }

                StreamChunk(
                    text = response.text ?: "",
                    inputTokens = latestInputTokens,
                    outputTokens = latestOutputTokens,
                    userMessageTokens = userMessageTokenCount
                )
            }
            .catch { exception ->
                // Handle deserialization errors from malformed API responses
                android.util.Log.e("GeminiService", "Stream error: ${exception.message}", exception)

                // Check if it's a SerializationException (missing 'parts' field, etc.)
                if (exception.message?.contains("MissingFieldException") == true ||
                    exception.message?.contains("deserialize") == true) {
                    // Emit empty response and let onCompletion handle as generic error
                    throw GeminiException("Failed to parse API response. The model may have returned an empty or blocked response.", exception)
                } else {
                    // Re-throw other exceptions
                    throw exception
                }
            }
            .onCompletion { cause ->
                // Log final token counts
                if (cause == null) {
                    android.util.Log.d("GeminiService", "Stream completed - Final tokens: Input=$latestInputTokens, Output=$latestOutputTokens")
                }

                // Handle completion with error
                if (cause != null) {
                    when (cause) {
                        is QuotaExceededException -> {
                            // Extract retry time from message
                            val retrySeconds = extractRetrySeconds(cause.message)
                            throw RateLimitException(
                                message = "Rate limit exceeded",
                                retryAfterSeconds = retrySeconds,
                                cause = cause
                            )
                        }
                        is ResponseStoppedException -> {
                            // MAX_TOKENS / SAFETY / OTHER - throw to allow VM to handle truncation gracefully
                            val reason = cause.message?.substringAfter("Reason: ")?.trim() ?: "MAX_TOKENS"
                            android.util.Log.w("GeminiService", "Response stopped: $reason - partial response delivered")
                            // Re-throw so ChatViewModel can handle gracefully and show "Continue" button
                            throw cause
                        }
                        else -> {
                            throw GeminiException("Failed to generate response: ${cause.message}", cause)
                        }
                    }
                }
            }
    }

    /**
     * Generate a reply with contextual memory and optional attachments.
     * Assembles memory context, streams the response, and triggers summary refresh.
     *
     * @param chatId The chat ID
     * @param userText The user's latest message
     * @param persona Optional persona
     * @param enableThinking Enable step-by-step reasoning
     * @param attachments Optional list of attachments (images/PDFs)
     * @param context Android Context for reading URIs
     * @return Flow of text tokens
     */
    suspend fun generateReply(
        chatId: String,
        userText: String,
        persona: Persona? = null,
        enableThinking: Boolean = false,
        attachments: List<AttachmentMeta> = emptyList(),
        context: Context? = null,
        modelName: String = MODEL_NAME,
        temperature: Float = TEMPERATURE,
        maxOutputTokens: Int = MAX_OUTPUT_TOKENS,
        safetyLevel: String = "Standard"
    ): Flow<String> {
        if (!isApiKeyConfigured()) {
            throw IllegalStateException("Gemini API key not configured. Add GEMINI_API_KEY to local.properties")
        }

        // Phase 2: Retrieve persona memory context BEFORE assembling (to pass to MemoryAssembler)
        // IMPORTANT: Guest users should NEVER have access to persona memory (only conversation history)
        val isGuest = Firebase.auth.currentUser == null
        val personaMemoryContext = if (persona != null && !isGuest) {
            android.util.Log.d("GeminiService", "Phase 2: Retrieving persona memory to include in system instructions")
            val engine = memoryEngine
            if (engine != null) {
                try {
                    val ctx = engine.contextFor(userText, persona.id, chatId)
                    android.util.Log.d("GeminiService", "Retrieved memory context: longTerm=${ctx.longTerm.size}, shortTerm=${ctx.shortTerm.size}, totalTokens=${ctx.totalTokens}")
                    if (ctx.longTerm.isNotEmpty()) {
                        android.util.Log.d("GeminiService", "Memory preview: ${ctx.longTerm.take(3).joinToString("; ") { it.memory.text }}")
                    } else {
                        android.util.Log.w("GeminiService", "No memories found for persona ${persona.id}!")
                    }
                    ctx
                } catch (e: Exception) {
                    android.util.Log.e("GeminiService", "Failed to retrieve persona memory: ${e.message}", e)
                    null
                }
            } else {
                android.util.Log.e("GeminiService", "MemoryEngine is null!")
                null
            }
        } else {
            if (isGuest) {
                android.util.Log.d("GeminiService", "Guest user: skipping persona memory (only conversation history available)")
            } else {
                android.util.Log.d("GeminiService", "Persona is null, skipping memory context")
            }
            null
        }

        // Phase 2: Retrieve PDF sources context BEFORE assembling (to pass to MemoryAssembler)
        // IMPORTANT: Guest users should NEVER have access to PDF sources (only conversation history)
        val pdfSourcesContext = if (persona != null && !isGuest) {
            android.util.Log.d("GeminiService", "Phase 2: Retrieving PDF sources to include in system instructions")
            val engine = sourcesEngine
            if (engine != null) {
                try {
                    val ctx = engine.getContextForQuery(persona.id, userText, limit = 3)
                    android.util.Log.d("GeminiService", "Retrieved sources context: ${ctx.length} chars")
                    android.util.Log.d("GeminiService", "Context preview: ${ctx.take(200)}")
                    ctx
                } catch (e: Exception) {
                    android.util.Log.e("GeminiService", "Failed to retrieve PDF sources: ${e.message}", e)
                    ""
                }
            } else {
                android.util.Log.e("GeminiService", "SourcesEngine is null!")
                ""
            }
        } else {
            if (isGuest) {
                android.util.Log.d("GeminiService", "Guest user: skipping PDF sources (only conversation history available)")
            } else {
                android.util.Log.d("GeminiService", "Persona is null, skipping sources context")
            }
            ""
        }

        // Phase 2: Assemble context WITH persona memory and PDF sources (will be in system instructions)
        // Skip memory assembler for guests - they only get conversation history, no persona context
        val memoryContext = if (!isGuest) {
            memoryAssembler?.assembleContext(
                chatId = chatId,
                persona = persona,
                enableThinking = enableThinking,
                personaMemoryContext = personaMemoryContext,
                pdfSourcesContext = pdfSourcesContext
            )
        } else {
            null
        }

        // Phase 2: Build clean prompt with ONLY conversation history and current message
        // (Memory context now in system instructions via MemoryAssembler)
        val promptParts = mutableListOf<String>()

        android.util.Log.d("GeminiService", "Phase 2: Building prompt with clean conversation history (memory in system instructions)")

        // Add conversation summary if present (kept in prompt for continuity)
        if (!memoryContext?.memorySummary.isNullOrBlank()) {
            promptParts.add("Conversation summary:\n${memoryContext?.memorySummary}")
        }

        // Add recent messages
        memoryContext?.recentWindow?.forEach { msg ->
            val roleLabel = if (msg.role == "user") "User" else "Assistant"
            promptParts.add("$roleLabel: ${msg.text}")
        }

        // Add current user message
        promptParts.add("User: $userText")

        val fullPrompt = promptParts.joinToString("\n\n")

        // Log the final prompt for debugging
        android.util.Log.d("GeminiService", "Final prompt length: ${fullPrompt.length} chars")
        android.util.Log.d("GeminiService", "Prompt preview (first 500 chars):\n${fullPrompt.take(500)}")

        // Build content with text and attachments
        val requestContent = content {
            text(fullPrompt)

            // Add attachment parts
            if (context != null) {
                attachments.forEach { attachment ->
                    try {
                        attachment.localUri?.let { uri ->
                            // Read bytes from URI using ContentResolver
                            val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                inputStream.readBytes()
                            }

                            if (bytes != null && bytes.isNotEmpty()) {
                                blob(attachment.mime, bytes)
                            }
                        }
                    } catch (e: Exception) {
                        // Log error but continue processing other attachments
                        android.util.Log.e("GeminiService", "Failed to read attachment: ${e.message}")
                    }
                }
            }
        }

        // Build system instruction
        val systemInstruction = memoryContext?.systemPreamble ?: buildSystemInstruction(persona, enableThinking)

        // Safety settings - use BLOCK_NONE to allow all content including medical advice
        // The app handles appropriate disclaimers via UI
        val safetyThreshold = when (safetyLevel) {
            "Standard" -> BlockThreshold.NONE  // Allow all content, show disclaimers in UI
            "Strict" -> BlockThreshold.MEDIUM_AND_ABOVE  // More restrictive if needed
            else -> BlockThreshold.NONE
        }

        val model = GenerativeModel(
            modelName = modelName,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                this.temperature = temperature
                topK = TOP_K
                topP = TOP_P
                this.maxOutputTokens = maxOutputTokens
            },
            systemInstruction = systemInstruction.takeIf { it.isNotBlank() }?.let {
                content { text(it) }
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, safetyThreshold),
                SafetySetting(HarmCategory.HATE_SPEECH, safetyThreshold),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, safetyThreshold),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, safetyThreshold)
            )
        )

        // Trigger background summary refresh after response completes
        CoroutineScope(Dispatchers.IO).launch {
            try {
                memorySummarizer?.maybeRefreshSummary(chatId)
            } catch (e: Exception) {
                // Silent fail - memory refresh is non-critical
            }
        }

        var totalInputTokens = 0
        var totalOutputTokens = 0

        return model.generateContentStream(requestContent)
            .map { response ->
                // Capture token usage from response metadata
                response.usageMetadata?.let { metadata ->
                    totalInputTokens = metadata.promptTokenCount ?: 0
                    totalOutputTokens = metadata.candidatesTokenCount ?: 0

                    // Log actual token counts from API
                    android.util.Log.d("GeminiService", "API Tokens - Input: $totalInputTokens, Output: $totalOutputTokens, Total: ${metadata.totalTokenCount}")
                }

                response.text ?: ""
            }
            .catch { exception ->
                // Handle deserialization errors from malformed API responses
                android.util.Log.e("GeminiService", "Stream error: ${exception.message}", exception)

                // Check if it's a SerializationException (missing 'parts' field, etc.)
                if (exception.message?.contains("MissingFieldException") == true ||
                    exception.message?.contains("deserialize") == true) {
                    // Emit empty response and let onCompletion handle as generic error
                    throw GeminiException("Failed to parse API response. The model may have returned an empty or blocked response.", exception)
                } else {
                    // Re-throw other exceptions
                    throw exception
                }
            }
            .onCompletion { cause ->
                // Handle completion with error
                if (cause != null) {
                    when (cause) {
                        is QuotaExceededException -> {
                            // Extract retry time from message
                            val retrySeconds = extractRetrySeconds(cause.message)
                            throw RateLimitException(
                                message = "Rate limit exceeded",
                                retryAfterSeconds = retrySeconds,
                                cause = cause
                            )
                        }
                        is ResponseStoppedException -> {
                            // MAX_TOKENS / SAFETY / OTHER - throw to allow VM to handle truncation gracefully
                            val reason = cause.message?.substringAfter("Reason: ")?.trim() ?: "MAX_TOKENS"
                            android.util.Log.w("GeminiService", "Response stopped: $reason - partial response delivered")
                            // Re-throw so ChatViewModel can handle gracefully and show "Continue" button
                            throw cause
                        }
                        else -> {
                            throw GeminiException("Failed to generate response: ${cause.message}", cause)
                        }
                    }
                }
            }
    }

    private fun extractRetrySeconds(message: String?): Int {
        if (message == null) return 60
        // Extract seconds from message like "Please retry in 10.364492042s."
        val regex = """retry in ([\d.]+)s""".toRegex()
        val match = regex.find(message)
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.toInt() ?: 60
    }

    /**
     * ============================================================================
     * ADVANCED PROMPTING v2.0 - Enhanced Reply Generation
     * ============================================================================
     *
     * This method uses the new advanced prompting system with:
     * - Query complexity analysis
     * - Reasoning mode detection
     * - Dynamic context optimization (5-10% of 1M window vs 0.7%)
     * - Multi-shot examples
     * - Chain-of-thought frameworks
     * - Expanded memory/sources retrieval
     * - Meta-cognitive instructions
     *
     * Usage: Call this instead of generateReplyWithTokens() for better responses
     */
    suspend fun generateReplyEnhanced(
        chatId: String,
        userText: String,
        persona: Persona? = null,
        enableThinking: Boolean = false,
        attachments: List<AttachmentMeta> = emptyList(),
        context: Context? = null,
        modelName: String = MODEL_NAME,
        temperature: Float = TEMPERATURE,
        maxOutputTokens: Int = MAX_OUTPUT_TOKENS,
        safetyLevel: String = "Standard",
        useAdvancedPrompting: Boolean = true,  // Toggle to compare with baseline
        groundingEnabled: Boolean = false  // NEW: Enable Google Search grounding
    ): Flow<StreamChunk> {
        if (!isApiKeyConfigured()) {
            throw IllegalStateException("Gemini API key not configured. Add GEMINI_API_KEY to local.properties")
        }

        // If advanced prompting is disabled, fall back to standard method
        if (!useAdvancedPrompting) {
            return generateReplyWithTokens(chatId, userText, persona, enableThinking,
                attachments, context, modelName, temperature, maxOutputTokens, safetyLevel)
        }

        // ============ STEP 1: Query Analysis ============
        val queryComplexity = promptBuilder.analyzeQueryComplexity(userText)
        val reasoningMode = promptBuilder.detectReasoningMode(userText, persona)

        android.util.Log.d("GeminiService", "=== Advanced Prompting v2.0 ===")
        android.util.Log.d("GeminiService", "Query complexity: $queryComplexity")
        android.util.Log.d("GeminiService", "Reasoning mode: $reasoningMode")

        // ============ STEP 2: Context Optimization ============
        val isGuest = Firebase.auth.currentUser == null
        val messageCount = database?.messageDao()?.lastN(chatId, 500)?.size ?: 0
        val memoryEngineLocal = memoryEngine
        val userId = Firebase.auth.currentUser?.uid ?: com.example.innovexia.core.auth.ProfileId.GUEST_OWNER_ID
        val availableMemoryCount = if (persona != null && !isGuest && memoryEngineLocal != null) {
            try {
                memoryEngineLocal.getCount(persona.id, userId)
            } catch (e: Exception) {
                0
            }
        } else 0

        // ============ STEP 3: Enhanced Memory Retrieval ============
        val personaMemoryContext = if (persona != null && !isGuest && memoryEngineLocal != null) {
            try {
                val ctx = memoryEngineLocal.contextFor(userText, persona.id, chatId)
                android.util.Log.d("GeminiService", "Retrieved ${ctx.longTerm.size} long-term + ${ctx.shortTerm.size} short-term memories")
                ctx
            } catch (e: Exception) {
                android.util.Log.e("GeminiService", "Memory retrieval failed: ${e.message}")
                null
            }
        } else null

        // ============ STEP 4: Enhanced PDF Sources Retrieval ============
        val sourcesEngineLocal = sourcesEngine
        val pdfSourcesContext = if (persona != null && !isGuest && sourcesEngineLocal != null) {
            try {
                // Adaptive chunk limit based on query complexity
                val chunkLimit = when (queryComplexity) {
                    PromptBuilder.QueryComplexity.SIMPLE -> 3
                    PromptBuilder.QueryComplexity.MODERATE -> 10
                    PromptBuilder.QueryComplexity.COMPLEX -> 15
                    PromptBuilder.QueryComplexity.RESEARCH -> 20
                }
                val ctx = sourcesEngineLocal.getContextForQuery(persona.id, userText, limit = chunkLimit)
                android.util.Log.d("GeminiService", "Retrieved $chunkLimit PDF chunks (${ctx.length} chars)")
                ctx
            } catch (e: Exception) {
                android.util.Log.e("GeminiService", "Sources retrieval failed: ${e.message}")
                ""
            }
        } else ""

        // ============ STEP 5: Optimize Token Allocation ============
        val allocation = contextOptimizer.optimizeAllocation(
            query = userText,
            persona = persona,
            conversationLength = messageCount,
            availableMemories = availableMemoryCount,
            availableSources = pdfSourcesContext?.length ?: 0,
            attachments = attachments
        )

        android.util.Log.d("GeminiService", "Token allocation: total=${allocation.totalBudget}, " +
                "system=${allocation.systemInstructions}, history=${allocation.conversationHistory}, " +
                "memories=${allocation.personaMemories}, sources=${allocation.pdfSources}")

        // ============ STEP 6: Assemble Enhanced Context ============
        val memoryAssemblerLocal = memoryAssembler
        val memoryContext = if (!isGuest && memoryAssemblerLocal != null) {
            memoryAssemblerLocal.assembleContext(
                chatId = chatId,
                persona = persona,
                enableThinking = enableThinking,
                personaMemoryContext = personaMemoryContext,
                pdfSourcesContext = pdfSourcesContext,
                targetMaxTokens = allocation.totalBudget,
                keepRecentTurns = contextOptimizer.calculateMessageLimit(allocation.conversationHistory),
                includeTokenBreakdown = true
            )
        } else null

        // Log actual token usage
        memoryContext?.tokenBreakdown?.let { breakdown ->
            val utilizationPct = (breakdown.total.toDouble() / 1_048_576 * 100)
            android.util.Log.d("GeminiService", "Actual tokens: ${breakdown.total} (${String.format("%.1f", utilizationPct)}% of window)")
            android.util.Log.d("GeminiService", "Breakdown: system=${breakdown.systemInstruction}, " +
                    "history=${breakdown.conversationHistory}, memories=${breakdown.personaMemories}, " +
                    "sources=${breakdown.pdfSources}")
        }

        // ============ STEP 7: Get Current Location (if available) ============
        val currentLocation = if (context != null) {
            try {
                val hasPermission = com.example.innovexia.core.permissions.PermissionHelper.hasLocationPermission(context)
                android.util.Log.d("GeminiService", "Location permission granted: $hasPermission")

                if (hasPermission) {
                    // Try cached location first (instant retrieval, no GPS wait)
                    val cachedLoc = com.example.innovexia.core.location.LocationCacheManager.getLastKnownLocation(context)

                    if (cachedLoc != null) {
                        android.util.Log.d("GeminiService", "✓ Using cached location: ${cachedLoc.latitude}, ${cachedLoc.longitude}, accuracy: ${cachedLoc.accuracy}m")
                        cachedLoc
                    } else {
                        // Cache miss or stale - fetch fresh location from GPS
                        android.util.Log.d("GeminiService", "Cache miss/stale - fetching fresh GPS location")
                        val freshLoc = com.example.innovexia.core.permissions.PermissionHelper.getCurrentLocation(context)

                        if (freshLoc != null) {
                            // Update cache with fresh location for next time
                            com.example.innovexia.core.location.LocationCacheManager.updateLocation(context, freshLoc)
                            android.util.Log.d("GeminiService", "✓ Fresh location retrieved and cached: ${freshLoc.latitude}, ${freshLoc.longitude}, accuracy: ${freshLoc.accuracy}m")
                            freshLoc
                        } else {
                            // GPS failed - try to use stale cached location as fallback
                            android.util.Log.w("GeminiService", "GPS failed - checking for stale cached location")
                            val staleLoc = com.example.innovexia.core.location.LocationCacheManager.getLastKnownLocationAnyAge(context)
                            if (staleLoc != null) {
                                android.util.Log.w("GeminiService", "Using stale cached location as fallback (age: ${(System.currentTimeMillis() - staleLoc.time) / 1000}s)")
                            } else {
                                android.util.Log.w("GeminiService", "No location available (GPS failed and no cache)")
                            }
                            staleLoc
                        }
                    }
                } else {
                    android.util.Log.w("GeminiService", "Location permission not granted - location context will not be available")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("GeminiService", "Failed to get location: ${e.message}", e)
                null
            }
        } else {
            android.util.Log.w("GeminiService", "Context is null - cannot retrieve location")
            null
        }

        // ============ STEP 7.5: Get User's Timezone (saved preference) ============
        val userTimezoneId = userPreferences?.userTimezoneId?.first()
        if (userTimezoneId != null) {
            android.util.Log.d("GeminiService", "Using saved timezone: $userTimezoneId")
        } else {
            android.util.Log.d("GeminiService", "No saved timezone, will use system default")
        }

        // ============ STEP 8: Build Advanced System Instructions ============
        val systemInstruction = promptBuilder.buildSystemInstruction(
            persona = persona,
            enableThinking = enableThinking,
            personaMemoryContext = personaMemoryContext,
            pdfSourcesContext = pdfSourcesContext,
            conversationSummary = memoryContext?.memorySummary,
            firstMessage = memoryContext?.firstMessage,
            attachments = attachments,
            queryComplexity = queryComplexity,
            reasoningMode = reasoningMode,
            location = currentLocation,
            timezoneId = userTimezoneId
        )

        val systemTokens = promptBuilder.estimateTokens(systemInstruction)
        android.util.Log.d("GeminiService", "Advanced system instruction: $systemTokens tokens")

        // ============ STEP 9: Build User Prompt ============
        val promptParts = mutableListOf<String>()

        if (!memoryContext?.memorySummary.isNullOrBlank()) {
            promptParts.add("Conversation summary:\n${memoryContext?.memorySummary}")
        }

        memoryContext?.recentWindow?.forEach { msg ->
            val roleLabel = if (msg.role == "user") "User" else "Assistant"
            promptParts.add("$roleLabel: ${msg.text}")
        }

        promptParts.add("User: $userText")

        val fullPrompt = promptParts.joinToString("\n\n")
        val promptTokens = promptBuilder.estimateTokens(fullPrompt)
        android.util.Log.d("GeminiService", "User prompt: $promptTokens tokens")

        // ============ STEP 10: Build Content with Attachments ============
        val requestContent = content {
            text(fullPrompt)

            if (context != null) {
                attachments.forEach { attachment ->
                    try {
                        attachment.localUri?.let { uri ->
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            if (bytes != null && bytes.isNotEmpty()) {
                                blob(attachment.mime, bytes)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GeminiService", "Failed to read attachment: ${e.message}")
                    }
                }
            }
        }

        // ============ STEP 11: Configure Model ============
        val safetyThreshold = when (safetyLevel) {
            "Standard" -> BlockThreshold.NONE
            "Strict" -> BlockThreshold.MEDIUM_AND_ABOVE
            else -> BlockThreshold.NONE
        }

        // ============ STEP 12: Choose SDK or REST API Based on Grounding ============

        // Trigger background summary refresh
        CoroutineScope(Dispatchers.IO).launch {
            try {
                memorySummarizer?.maybeRefreshSummary(chatId)
            } catch (e: Exception) {
                // Silent fail
            }
        }

        // Use REST API if grounding is enabled, otherwise use SDK
        if (groundingEnabled) {
            android.util.Log.d("GeminiService", "Using REST API for Google Search grounding")

            // Calculate user message tokens ONLY (not including memories/system prompts)
            val userMessageTokenCount = com.example.innovexia.subscriptions.mock.TokenCounter.estimateInputTokens(userText).toInt()
            android.util.Log.d("GeminiService", "User message token estimate: $userMessageTokenCount tokens")

            // Build REST API request with full conversation history
            val contents = mutableListOf<Content>()

            // Add conversation history
            memoryContext?.recentWindow?.forEach { msg ->
                contents.add(
                    Content(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                )
            }

            // Add current user message
            contents.add(
                Content(
                    role = "user",
                    parts = listOf(Part(text = userText))
                )
            )

            val request = GeminiRequest(
                contents = contents,
                generationConfig = GenerationConfig(
                    temperature = temperature,
                    topK = TOP_K,
                    topP = TOP_P,
                    maxOutputTokens = maxOutputTokens
                ),
                systemInstruction = Content(
                    role = "user",
                    parts = listOf(Part(text = systemInstruction))
                ),
                tools = listOf(
                    Tool(
                        googleSearch = GoogleSearchRetrieval()
                    )
                ),
                safetySettings = listOf(
                    com.example.innovexia.data.ai.SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
                    com.example.innovexia.data.ai.SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
                    com.example.innovexia.data.ai.SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
                    com.example.innovexia.data.ai.SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
                )
            )

            // Wrap the REST client stream to add userMessageTokens to each chunk
            return restClient.generateContentStream(modelName, request)
                .map { chunk ->
                    chunk.copy(userMessageTokens = userMessageTokenCount)
                }
        } else {
            // Use SDK for non-grounding requests
            android.util.Log.d("GeminiService", "Using SDK (grounding disabled)")

            val model = GenerativeModel(
                modelName = modelName,
                apiKey = BuildConfig.GEMINI_API_KEY,
                generationConfig = generationConfig {
                    this.temperature = temperature
                    topK = TOP_K
                    topP = TOP_P
                    this.maxOutputTokens = maxOutputTokens
                },
                systemInstruction = content { text(systemInstruction) },
                safetySettings = listOf(
                    SafetySetting(HarmCategory.HARASSMENT, safetyThreshold),
                    SafetySetting(HarmCategory.HATE_SPEECH, safetyThreshold),
                    SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, safetyThreshold),
                    SafetySetting(HarmCategory.DANGEROUS_CONTENT, safetyThreshold)
                )
            )

            // Calculate user message tokens ONLY (not including memories/system prompts)
            val userMessageTokenCount = com.example.innovexia.subscriptions.mock.TokenCounter.estimateInputTokens(userText).toInt()
            android.util.Log.d("GeminiService", "User message token estimate: $userMessageTokenCount tokens")

            var latestInputTokens = 0
            var latestOutputTokens = 0

            return model.generateContentStream(requestContent)
                .map { response ->
                    response.usageMetadata?.let { metadata ->
                        latestInputTokens = metadata.promptTokenCount ?: 0
                        latestOutputTokens = metadata.candidatesTokenCount ?: 0
                    }

                    StreamChunk(
                        text = response.text ?: "",
                        inputTokens = latestInputTokens,
                        outputTokens = latestOutputTokens,
                        userMessageTokens = userMessageTokenCount,
                        groundingMetadata = null
                    )
                }
                .catch { exception ->
                    android.util.Log.e("GeminiService", "Stream error: ${exception.message}", exception)

                    if (exception.message?.contains("MissingFieldException") == true ||
                        exception.message?.contains("deserialize") == true) {
                        throw GeminiException("Failed to parse API response", exception)
                    } else {
                        throw exception
                    }
                }
                .onCompletion { cause ->
                    if (cause == null) {
                        val utilizationPct = (latestInputTokens.toDouble() / 1_048_576 * 100)
                        android.util.Log.d("GeminiService", "=== Stream completed ===")
                        android.util.Log.d("GeminiService", "Final input tokens: $latestInputTokens (${String.format("%.2f", utilizationPct)}%)")
                        android.util.Log.d("GeminiService", "Final output tokens: $latestOutputTokens")
                    }

                    if (cause != null) {
                        when (cause) {
                            is QuotaExceededException -> {
                                throw RateLimitException("Rate limit exceeded", extractRetrySeconds(cause.message), cause)
                            }
                            is ResponseStoppedException -> {
                                val reason = cause.message?.substringAfter("Reason: ")?.trim() ?: "MAX_TOKENS"
                                android.util.Log.w("GeminiService", "Response stopped: $reason")
                                throw cause
                            }
                            else -> {
                                throw GeminiException("Failed to generate response: ${cause.message}", cause)
                            }
                        }
                    }
                }
        }
    }
}

/**
 * Exception thrown when Gemini service fails.
 */
open class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when rate limit is exceeded.
 */
class RateLimitException(
    message: String,
    val retryAfterSeconds: Int,
    cause: Throwable? = null
) : GeminiException(message, cause)

/**
 * Exception thrown when response generation is stopped (e.g., MAX_TOKENS).
 */
class MaxTokensException(
    message: String,
    val stopReason: String,
    cause: Throwable? = null
) : GeminiException(message, cause)
