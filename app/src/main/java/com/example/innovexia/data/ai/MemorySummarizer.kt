package com.example.innovexia.data.ai

import com.example.innovexia.BuildConfig
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.local.entities.MemChunkEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.fold
import java.util.*

/**
 * Generates and refreshes rolling summaries of chat history.
 * Triggered when older messages accumulate beyond threshold.
 *
 * Optimized for Gemini 2.5 Flash's massive input window:
 * - Summary limit increased to 4K tokens (was 800) for richer context
 * - Refresh threshold raised to 20K tokens (was 1.8K)
 * - Keeps last 50 messages unsummarized (was 12)
 * - Even with these increases, uses <3% of 1M input token budget
 */
class MemorySummarizer(
    private val database: AppDatabase,
    private val memoryAssembler: MemoryAssembler
) {

    companion object {
        private const val MODEL_NAME = "gemini-2.0-flash-exp"
        private const val SUMMARIZATION_TEMPERATURE = 0.3f

        // Increased from 800 to 4000 tokens - still tiny vs. 1M input window
        // Richer summaries preserve more context without sacrificing space
        private const val MAX_SUMMARY_TOKENS = 4000
    }

    /**
     * Conditionally refresh the rolling summary for a chat.
     *
     * @param chatId The chat to potentially summarize
     * @param thresholdNewTokens Trigger summarization when older content exceeds this (default 20K)
     * @param keepRecentTurns Number of recent messages to exclude from summarization (default 50)
     */
    suspend fun maybeRefreshSummary(
        chatId: String,
        thresholdNewTokens: Int = 20000,  // Increased from 1800 to 20K tokens
        keepRecentTurns: Int = 50         // Increased from 12 to 50 messages (matches MemoryAssembler)
    ) {
        val chatDao = database.chatDao()
        val messageDao = database.messageDao()
        val memChunkDao = database.memChunkDao()

        val chat = chatDao.getById(chatId) ?: return
        if (!chat.memoryEnabled) return

        // Get all messages
        val allMessages = messageDao.forChatSync(chatId)
        if (allMessages.size <= keepRecentTurns) return // Not enough to summarize

        // Split: older messages vs recent window
        val olderMessages = allMessages.dropLast(keepRecentTurns)
        val olderTokens = memoryAssembler.estimateTokens(olderMessages)

        // Check if threshold exceeded
        if (olderTokens < thresholdNewTokens) return

        // Generate summary
        val summaryText = generateSummary(olderMessages.map { it.text })

        // Save summary to chat
        val timestamp = System.currentTimeMillis()
        chatDao.updateSummary(chatId, summaryText, timestamp)

        // Optional: Save as chunks for future semantic retrieval
        saveAsChunks(chatId, summaryText, 0, olderMessages.size - 1, memChunkDao)
    }

    /**
     * Force refresh the summary (e.g., from user action in Memory Viewer).
     */
    suspend fun forceRefreshSummary(
        chatId: String,
        keepRecentTurns: Int = 12
    ): String {
        val messageDao = database.messageDao()
        val chatDao = database.chatDao()

        val allMessages = messageDao.forChatSync(chatId)
        if (allMessages.isEmpty()) return ""

        val messagesToSummarize = if (allMessages.size > keepRecentTurns) {
            allMessages.dropLast(keepRecentTurns)
        } else {
            allMessages
        }

        val summaryText = generateSummary(messagesToSummarize.map { it.text })
        val timestamp = System.currentTimeMillis()
        chatDao.updateSummary(chatId, summaryText, timestamp)

        return summaryText
    }

    /**
     * Generate a summary using Gemini.
     */
    private suspend fun generateSummary(messages: List<String>): String {
        if (!BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            throw IllegalStateException("Gemini API key not configured")
        }

        val combinedText = messages.joinToString("\n\n---\n\n")
        val prompt = """
            Summarize the following chat portion compactly for future context.
            Keep facts, decisions, user preferences, tasks, and entities.
            Omit small talk.
            Use bullet-ready prose within 400–800 tokens.
            Maintain neutrality and include dates/times if present.

            Chat content:
            $combinedText
        """.trimIndent()

        val model = GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = SUMMARIZATION_TEMPERATURE
                maxOutputTokens = MAX_SUMMARY_TOKENS
            }
        )

        return try {
            model.generateContentStream(prompt)
                .catch { exception ->
                    android.util.Log.e("MemorySummarizer", "Stream error: ${exception.message}", exception)

                    // Check if it's a SerializationException (missing 'parts' field, etc.)
                    if (exception.message?.contains("MissingFieldException") == true ||
                        exception.message?.contains("deserialize") == true) {
                        throw GeminiException("Failed to parse API response. The model may have returned an empty or blocked response.", exception)
                    } else {
                        throw GeminiException("Failed to generate summary: ${exception.message}", exception)
                    }
                }
                .fold("") { acc, response ->
                    acc + (response.text ?: "")
                }
        } catch (e: Exception) {
            throw GeminiException("Summarization failed: ${e.message}", e)
        }
    }

    /**
     * Save summary as memory chunks for future retrieval.
     */
    private suspend fun saveAsChunks(
        chatId: String,
        summaryText: String,
        startIndex: Int,
        endIndex: Int,
        memChunkDao: com.example.innovexia.data.local.dao.MemChunkDao
    ) {
        // For now, save as single chunk
        // Future: split into semantic segments with embeddings
        val chunk = MemChunkEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            text = summaryText,
            startIndex = startIndex,
            endIndex = endIndex
        )
        memChunkDao.insert(chunk)
    }

    /**
     * Clear memory summary for a chat.
     */
    suspend fun clearMemory(chatId: String) {
        val chatDao = database.chatDao()
        val memChunkDao = database.memChunkDao()

        chatDao.updateSummary(chatId, "", 0)
        memChunkDao.deleteForChat(chatId)
    }
}
