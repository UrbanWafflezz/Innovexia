package com.example.innovexia.data.ai

import com.example.innovexia.BuildConfig
import com.example.innovexia.core.chat.TitleNamer
import com.example.innovexia.data.local.entities.MessageEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.withTimeout

/**
 * AI-powered chat naming service using Gemini.
 * Generates concise, contextual chat titles based on conversation content.
 */
class ChatNamingService {

    companion object {
        private const val MODEL_NAME = "gemini-2.0-flash-exp"
        private const val TEMPERATURE = 0.3f // Lower for more consistent, focused titles
        private const val MAX_OUTPUT_TOKENS = 30 // Concise titles (25 chars)
        private const val TIMEOUT_MS = 10000L // 10 second timeout
    }

    /**
     * Generate a chat title using AI based on conversation messages.
     *
     * @param messages List of messages in the conversation (usually first 4-8 messages)
     * @return AI-generated title, guaranteed to be ≤10 characters
     */
    suspend fun generateChatTitle(messages: List<MessageEntity>): Result<String> {
        return try {
            if (!isApiKeyConfigured()) {
                // Fallback to simple naming if no API key
                return Result.success(fallbackNaming(messages))
            }

            // Build conversation context
            val conversationContext = buildConversationContext(messages)

            // Create prompt for title generation
            val prompt = """
                Analyze this conversation and generate a concise title (maximum 25 characters).

                Rules:
                - Maximum 25 characters total
                - Be concise but clear - capture the essence
                - Use natural language, avoid abbreviations
                - Be specific and meaningful
                - No punctuation at the end
                - Use title case

                Examples of good titles:
                - "Weather Explanation"
                - "Python API Guide"
                - "React Debugging"
                - "Machine Learning Intro"
                - "Climate Discussion"

                Conversation:
                $conversationContext

                Generate title (25 chars max):
            """.trimIndent()

            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = BuildConfig.GEMINI_API_KEY,
                generationConfig = generationConfig {
                    temperature = TEMPERATURE
                    maxOutputTokens = MAX_OUTPUT_TOKENS
                }
            )

            // Generate with timeout
            val response = try {
                withTimeout(TIMEOUT_MS) {
                    model.generateContent(prompt)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatNamingService", "Failed to generate title: ${e.message}", e)
                // Return fallback on API errors
                return Result.success(fallbackNaming(messages))
            }

            val rawTitle = response.text?.trim() ?: ""

            // Apply TitleNamer to ensure ≤25 chars (concise, marquee handles overflow)
            val finalTitle = if (rawTitle.isNotEmpty()) {
                TitleNamer.from(rawTitle, null)
            } else {
                fallbackNaming(messages)
            }

            Result.success(finalTitle)

        } catch (e: Exception) {
            // Return fallback on any error
            Result.success(fallbackNaming(messages))
        }
    }

    /**
     * Check if API key is configured.
     */
    private fun isApiKeyConfigured(): Boolean {
        return BuildConfig.GEMINI_API_KEY.isNotBlank()
    }

    /**
     * Build conversation context from messages.
     * Takes first few messages to understand topic.
     */
    private fun buildConversationContext(messages: List<MessageEntity>): String {
        // Take first 4-6 messages or up to 500 chars
        return messages
            .take(6)
            .joinToString("\n") { msg ->
                val role = if (msg.role == "user") "User" else "AI"
                "$role: ${msg.text.take(150)}"
            }
            .take(500)
    }

    /**
     * Fallback to simple naming if AI is unavailable.
     */
    private fun fallbackNaming(messages: List<MessageEntity>): String {
        val firstUser = messages.firstOrNull { it.role == "user" }?.text
        val firstModel = messages.firstOrNull { it.role == "model" }?.text
        return TitleNamer.from(firstUser, firstModel)
    }
}

/**
 * Determine if chat should be re-named based on message count.
 * Re-name after: first response (2 msgs), then every 6-8 messages.
 */
fun shouldUpdateChatTitle(messageCount: Int): Boolean {
    return when {
        messageCount == 2 -> true  // After first AI response
        messageCount == 8 -> true  // After ~4 exchanges
        messageCount == 16 -> true // After ~8 exchanges
        messageCount % 20 == 0 -> true // Every 10 exchanges after
        else -> false
    }
}
