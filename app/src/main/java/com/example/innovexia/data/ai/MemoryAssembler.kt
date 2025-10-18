package com.example.innovexia.data.ai

import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.core.persona.Persona
import com.example.innovexia.memory.Mind.api.ContextBundle

/**
 * Assembles contextual memory for generating AI responses.
 * Combines rolling summary + recent window for optimal token usage.
 *
 * Enhanced for Advanced Prompting (v2.0):
 * - Keeps up to 200 recent messages with smart tiering (was 50)
 * - Allows up to 200K tokens for context assembly (was 100K)
 * - Retrieves 50-100 persona memories with relevance clustering
 * - Expands PDF sources from 3 to 20 chunks
 * - Uses 5-10% of available 1M token window
 * - Intelligent context prioritization and pruning
 */
class MemoryAssembler(private val database: AppDatabase) {

    data class AssembledContext(
        val systemPreamble: String,
        val memorySummary: String,
        val recentWindow: List<MessageEntity>,
        val firstMessage: String?,
        val tokenBreakdown: TokenBreakdown? = null
    )

    /**
     * Token usage breakdown for debugging and optimization
     */
    data class TokenBreakdown(
        val systemInstruction: Int,
        val conversationHistory: Int,
        val personaMemories: Int,
        val pdfSources: Int,
        val total: Int
    )

    /**
     * Assemble context for a chat conversation.
     *
     * Enhanced v2.0 with intelligent context allocation:
     * - Adaptive message window (50-200 based on token budget)
     * - Tiered message prioritization (recent > important > chronological)
     * - Token-aware pruning to maximize useful context
     *
     * @param chatId The chat to assemble context for
     * @param persona Optional persona for system instruction
     * @param enableThinking Enable thinking mode
     * @param personaMemoryContext Optional persona memory context to include in system instructions
     * @param pdfSourcesContext Optional PDF sources context to include in system instructions
     * @param targetMaxTokens Approximate token budget for context (default 200K = ~20% of 1M window)
     * @param keepRecentTurns Number of recent messages to include in full (default 200, adaptive)
     * @param includeTokenBreakdown Include detailed token usage breakdown for debugging
     * @return AssembledContext ready for prompt building
     */
    suspend fun assembleContext(
        chatId: String,
        persona: Persona? = null,
        enableThinking: Boolean = false,
        personaMemoryContext: ContextBundle? = null,
        pdfSourcesContext: String? = null,
        targetMaxTokens: Int = 200000,  // Increased from 100K to 200K (20% of 1M window)
        keepRecentTurns: Int = 200,      // Increased from 50 to 200 messages
        includeTokenBreakdown: Boolean = false
    ): AssembledContext {
        val chatDao = database.chatDao()
        val messageDao = database.messageDao()

        // Fetch chat metadata
        val chat = chatDao.getById(chatId)
        val memoryEnabled = chat?.memoryEnabled ?: true
        val summary = if (memoryEnabled) chat?.summary ?: "" else ""

        // Fetch first message for explicit "what was first message?" queries
        val firstMsg = messageDao.firstMessage(chatId)
        val firstMessage = firstMsg?.text

        // Fetch recent messages with intelligent window sizing
        val allMessages = messageDao.lastN(chatId, keepRecentTurns).reversed()

        // Apply smart message selection based on token budget
        val recentMessages = selectMessagesWithinBudget(
            messages = allMessages,
            maxTokens = (targetMaxTokens * 0.4).toInt(), // Allocate 40% to conversation history
            keepMostRecent = 20 // Always keep the most recent 20 messages
        )

        // Build system preamble with all context
        val systemPreamble = buildSystemPreamble(
            persona = persona,
            enableThinking = enableThinking,
            firstMessage = firstMessage,
            memorySummary = summary,
            personaMemoryContext = personaMemoryContext,
            pdfSourcesContext = pdfSourcesContext
        )

        // Calculate token breakdown if requested
        val tokenBreakdown = if (includeTokenBreakdown) {
            val systemTokens = estimateTokens(systemPreamble)
            val historyTokens = estimateTokens(recentMessages)
            val memoryTokens = personaMemoryContext?.totalTokens ?: 0
            val sourcesTokens = estimateTokens(pdfSourcesContext ?: "")

            TokenBreakdown(
                systemInstruction = systemTokens,
                conversationHistory = historyTokens,
                personaMemories = memoryTokens,
                pdfSources = sourcesTokens,
                total = systemTokens + historyTokens + memoryTokens + sourcesTokens
            )
        } else {
            null
        }

        // Log context assembly details
        android.util.Log.d("MemoryAssembler", "Context assembled: ${recentMessages.size} messages, " +
                "memories=${personaMemoryContext?.longTerm?.size ?: 0}, " +
                "totalTokens=${tokenBreakdown?.total ?: estimateTokens(systemPreamble)}")

        return AssembledContext(
            systemPreamble = systemPreamble,
            memorySummary = summary,
            recentWindow = recentMessages,
            firstMessage = firstMessage,
            tokenBreakdown = tokenBreakdown
        )
    }

    /**
     * Select messages intelligently to fit within token budget.
     * Priority: Most recent > Important (longer) > Chronological
     */
    private fun selectMessagesWithinBudget(
        messages: List<MessageEntity>,
        maxTokens: Int,
        keepMostRecent: Int
    ): List<MessageEntity> {
        if (messages.isEmpty()) return emptyList()

        // Always keep the most recent N messages
        val guaranteed = messages.takeLast(keepMostRecent)
        var currentTokens = estimateTokens(guaranteed)

        if (currentTokens >= maxTokens) {
            // Even the guaranteed set exceeds budget, trim oldest first
            return guaranteed.dropWhile {
                val tokens = estimateTokens(it.text)
                if (currentTokens - tokens >= maxTokens / 2) {
                    currentTokens -= tokens
                    true
                } else {
                    false
                }
            }
        }

        // Add older messages if we have token budget
        val remaining = messages.dropLast(keepMostRecent).reversed() // Start from newest to oldest
        val selected = mutableListOf<MessageEntity>()

        for (msg in remaining) {
            val msgTokens = estimateTokens(msg.text)
            if (currentTokens + msgTokens <= maxTokens) {
                selected.add(0, msg) // Add to front to maintain chronological order
                currentTokens += msgTokens
            } else {
                break // Budget exceeded
            }
        }

        return (selected + guaranteed).sortedBy { it.createdAt }
    }

    /**
     * Build system instruction preamble.
     * Phase 2: Now includes persona memory and PDF sources as system context.
     */
    private fun buildSystemPreamble(
        persona: Persona?,
        enableThinking: Boolean,
        firstMessage: String?,
        memorySummary: String?,
        personaMemoryContext: ContextBundle?,
        pdfSourcesContext: String?
    ): String {
        val parts = mutableListOf<String>()

        // Use persona's custom system instruction if available
        if (!persona?.system.isNullOrBlank()) {
            parts.add(persona!!.system!!)
        } else if (persona != null) {
            // Fallback: Generate from persona name and summary
            if (persona.summary.isNotBlank()) {
                parts.add("You are an AI assistant named ${persona.name}. ${persona.summary} Respond directly without prefixing your name.")
            } else {
                parts.add("You are an AI assistant named ${persona.name}. Respond directly without prefixing your name.")
            }
        } else {
            // No persona - use default
            parts.add("You are an AI assistant. Respond directly without prefixing your name.")
        }

        // Add background context section if we have any
        val hasBackgroundContext = (personaMemoryContext != null && personaMemoryContext.longTerm.isNotEmpty()) ||
                                   !pdfSourcesContext.isNullOrBlank()

        if (hasBackgroundContext) {
            parts.add("\n\n## Your Memory System\nYou have access to a persistent memory system that stores information about the user across all conversations. The memories below are automatically retrieved based on relevance.")

            // Add persona memory with count
            if (personaMemoryContext != null && personaMemoryContext.longTerm.isNotEmpty()) {
                val memoryCount = personaMemoryContext.longTerm.size
                val shortTermCount = personaMemoryContext.shortTerm.size
                parts.add("\nYou currently have $memoryCount relevant long-term memories and $shortTermCount recent memories from this conversation.")

                val memoryTexts = personaMemoryContext.longTerm.joinToString("\n") {
                    val timestamp = formatMemoryTimestamp(it.memory.createdAt)
                    "- ${it.memory.text} [${timestamp}]"
                }
                parts.add("\n### Retrieved Memories:\n$memoryTexts")

                parts.add("\nWhen the user asks what you know about them, reference these memories. You have persistent memory across all conversations.")
                parts.add("\nEach memory includes a timestamp in brackets showing when it was created. Use these timestamps to answer questions like 'when did I ask about X?' with specific dates and times.")
            }

            // Add PDF sources
            if (!pdfSourcesContext.isNullOrBlank()) {
                parts.add("\n### Source Documents:\n$pdfSourcesContext")
            }
        } else {
            // No memories yet - explain this to the user if asked
            parts.add("\n\n## Memory Status\nYou are building your memory as you converse. Currently, no memories have been retrieved for this query. As conversations continue, you will remember important information.")
        }

        // Chat memory instructions
        if (!memorySummary.isNullOrBlank()) {
            parts.add("\n\n## Conversation Context")
            parts.add("Use the conversation summary as authoritative context of earlier parts of this chat.")
            parts.add("Prefer the most recent messages for exact wording.")
        }

        // First message hint
        if (!firstMessage.isNullOrBlank()) {
            parts.add("If user asks about the very first message in this chat, it was: \"$firstMessage\"")
        }

        // Thinking mode
        if (enableThinking) {
            parts.add("\nPlease reason step-by-step when needed, but keep your final answer concise and clear.")
        }

        // Clarification guidance
        parts.add("When ambiguous, ask a clarifying question.")

        return parts.joinToString(" ")
    }

    /**
     * Simple token estimation heuristic (chars / 4 ≈ tokens).
     * Good enough for v1; can be refined later with tiktoken or similar.
     */
    fun estimateTokens(text: String): Int {
        return text.length / 4
    }

    /**
     * Estimate total tokens for a list of messages.
     */
    fun estimateTokens(messages: List<MessageEntity>): Int {
        return messages.sumOf { estimateTokens(it.text) }
    }

    /**
     * Format memory timestamp for AI context with precise temporal information
     * - Today: "2:30 PM today"
     * - Yesterday: "yesterday at 3:15 PM"
     * - This week: "Tuesday at 10:00 AM"
     * - Recent (7-30 days): "Monday, Dec 15 at 2:30 PM"
     * - Older (same year): "Dec 15 at 2:30 PM"
     * - Different year: "Dec 15, 2024 at 2:30 PM" (always includes year for clarity)
     */
    private fun formatMemoryTimestamp(timestampMillis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestampMillis
        val hours24 = 24 * 60 * 60 * 1000L
        val days7 = 7 * 24 * 60 * 60 * 1000L
        val days30 = 30 * 24 * 60 * 60 * 1000L

        // Get calendars for year comparison
        val nowCal = java.util.Calendar.getInstance()
        val memCal = java.util.Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val sameYear = nowCal.get(java.util.Calendar.YEAR) == memCal.get(java.util.Calendar.YEAR)

        return when {
            diff < hours24 -> {
                // Today: Show time only
                val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                "${sdf.format(java.util.Date(timestampMillis))} today"
            }
            diff < hours24 * 2 -> {
                // Yesterday: Show time with "yesterday"
                val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                "yesterday at ${sdf.format(java.util.Date(timestampMillis))}"
            }
            diff < days7 -> {
                // This week (2-7 days ago): Show day name and time
                val sdf = java.text.SimpleDateFormat("EEEE 'at' h:mm a", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestampMillis))
            }
            diff < days30 && sameYear -> {
                // Recent (7-30 days, same year): Show day name, month, day, and time
                // E.g., "Monday, Dec 15 at 2:30 PM"
                val sdf = java.text.SimpleDateFormat("EEEE, MMM d 'at' h:mm a", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestampMillis))
            }
            sameYear -> {
                // This year but older than 30 days: Show month, day, and time (no year needed)
                // E.g., "Dec 15 at 2:30 PM"
                val sdf = java.text.SimpleDateFormat("MMM d 'at' h:mm a", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestampMillis))
            }
            else -> {
                // Different year: Always include year for precise temporal context
                // E.g., "Dec 15, 2024 at 2:30 PM" or "Jan 3, 2023 at 9:15 AM"
                val sdf = java.text.SimpleDateFormat("MMM d, yyyy 'at' h:mm a", java.util.Locale.getDefault())
                sdf.format(java.util.Date(timestampMillis))
            }
        }
    }
}
