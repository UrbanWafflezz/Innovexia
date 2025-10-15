package com.example.innovexia.data.ai

import com.example.innovexia.core.persona.Persona
import com.example.innovexia.data.models.AttachmentMeta
import com.example.innovexia.memory.Mind.api.ContextBundle
import com.example.innovexia.memory.Mind.api.MemoryKind
import kotlin.math.min

/**
 * Intelligent context optimizer for dynamic token allocation.
 *
 * Analyzes query characteristics and allocates tokens optimally across:
 * - System instructions (10-20%)
 * - Conversation history (30-50%)
 * - Persona memories (20-40%)
 * - PDF sources (10-30%)
 * - Attachments (10-20%)
 *
 * Goal: Maximize useful context within Gemini's 1M token window
 * while keeping response quality high and token costs reasonable.
 */
class ContextOptimizer {

    companion object {
        // Maximum safe input tokens (leaving room for output and safety margin)
        private const val MAX_SAFE_INPUT_TOKENS = 800_000  // 80% of 1M window

        // Target ranges for different query complexities
        private const val SIMPLE_QUERY_TOKENS = 20_000     // 2% of window
        private const val MODERATE_QUERY_TOKENS = 50_000   // 5% of window
        private const val COMPLEX_QUERY_TOKENS = 100_000   // 10% of window
        private const val RESEARCH_QUERY_TOKENS = 200_000  // 20% of window
    }

    /**
     * Context allocation strategy
     */
    data class AllocationStrategy(
        val totalBudget: Int,
        val systemInstructions: Int,
        val conversationHistory: Int,
        val personaMemories: Int,
        val pdfSources: Int,
        val attachments: Int,
        val reserve: Int  // Safety margin
    ) {
        fun validate(): Boolean {
            val sum = systemInstructions + conversationHistory + personaMemories +
                     pdfSources + attachments + reserve
            return sum <= totalBudget
        }
    }

    /**
     * Query analysis results
     */
    data class QueryAnalysis(
        val complexity: PromptBuilder.QueryComplexity,
        val reasoningMode: PromptBuilder.ReasoningMode,
        val requiresMemory: Boolean,
        val requiresSources: Boolean,
        val hasAttachments: Boolean,
        val isMultiTurn: Boolean,
        val estimatedResponseTokens: Int
    )

    /**
     * Analyze query and determine optimal context allocation.
     *
     * @param query The user's query text
     * @param persona The active persona
     * @param conversationLength Number of messages in conversation
     * @param availableMemories Number of persona memories available
     * @param availableSources Number of PDF source chunks available
     * @param attachments Attachments in current message
     * @return Optimal allocation strategy
     */
    fun optimizeAllocation(
        query: String,
        persona: Persona?,
        conversationLength: Int,
        availableMemories: Int,
        availableSources: Int,
        attachments: List<AttachmentMeta> = emptyList()
    ): AllocationStrategy {
        // Analyze query characteristics
        val analysis = analyzeQuery(query, conversationLength, availableMemories,
                                    availableSources, attachments)

        // Determine base budget from complexity
        val baseBudget = when (analysis.complexity) {
            PromptBuilder.QueryComplexity.SIMPLE -> SIMPLE_QUERY_TOKENS
            PromptBuilder.QueryComplexity.MODERATE -> MODERATE_QUERY_TOKENS
            PromptBuilder.QueryComplexity.COMPLEX -> COMPLEX_QUERY_TOKENS
            PromptBuilder.QueryComplexity.RESEARCH -> RESEARCH_QUERY_TOKENS
        }

        // Adjust budget based on available context
        val adjustedBudget = adjustBudgetForAvailableContext(
            baseBudget = baseBudget,
            hasMemories = availableMemories > 0,
            hasSources = availableSources > 0,
            hasAttachments = attachments.isNotEmpty(),
            isLongConversation = conversationLength > 50
        )

        // Allocate tokens across components
        return allocateTokens(adjustedBudget, analysis)
    }

    /**
     * Analyze query to understand requirements
     */
    private fun analyzeQuery(
        query: String,
        conversationLength: Int,
        availableMemories: Int,
        availableSources: Int,
        attachments: List<AttachmentMeta>
    ): QueryAnalysis {
        val promptBuilder = PromptBuilder()
        val complexity = promptBuilder.analyzeQueryComplexity(query)
        val reasoningMode = promptBuilder.detectReasoningMode(query, null)

        // Detect if query references memory
        val lowerQuery = query.lowercase()
        val requiresMemory = availableMemories > 0 && (
            lowerQuery.contains("remember") ||
            lowerQuery.contains("you know") ||
            lowerQuery.contains("we discussed") ||
            lowerQuery.contains("last time") ||
            lowerQuery.contains("before") ||
            lowerQuery.contains("what do you know about me")
        )

        // Detect if query references sources
        val requiresSources = availableSources > 0 && (
            lowerQuery.contains("according to") ||
            lowerQuery.contains("based on") ||
            lowerQuery.contains("in the document") ||
            lowerQuery.contains("the file") ||
            lowerQuery.contains("pdf") ||
            lowerQuery.contains("source")
        )

        // Estimate response length based on query type
        val estimatedResponseTokens = when (complexity) {
            PromptBuilder.QueryComplexity.SIMPLE -> 100
            PromptBuilder.QueryComplexity.MODERATE -> 500
            PromptBuilder.QueryComplexity.COMPLEX -> 2000
            PromptBuilder.QueryComplexity.RESEARCH -> 4000
        }

        return QueryAnalysis(
            complexity = complexity,
            reasoningMode = reasoningMode,
            requiresMemory = requiresMemory,
            requiresSources = requiresSources,
            hasAttachments = attachments.isNotEmpty(),
            isMultiTurn = conversationLength > 1,
            estimatedResponseTokens = estimatedResponseTokens
        )
    }

    /**
     * Adjust budget based on what context is actually available
     */
    private fun adjustBudgetForAvailableContext(
        baseBudget: Int,
        hasMemories: Boolean,
        hasSources: Boolean,
        hasAttachments: Boolean,
        isLongConversation: Boolean
    ): Int {
        var budget = baseBudget

        // Increase budget if we have rich context to utilize
        if (hasMemories) budget = (budget * 1.3).toInt()
        if (hasSources) budget = (budget * 1.4).toInt()
        if (hasAttachments) budget = (budget * 1.2).toInt()
        if (isLongConversation) budget = (budget * 1.2).toInt()

        // Cap at safe maximum
        return min(budget, MAX_SAFE_INPUT_TOKENS)
    }

    /**
     * Allocate tokens intelligently across context components
     */
    private fun allocateTokens(totalBudget: Int, analysis: QueryAnalysis): AllocationStrategy {
        // Reserve 10% for safety margin
        val reserve = (totalBudget * 0.10).toInt()
        val usableBudget = totalBudget - reserve

        // Base allocations (percentages of usable budget)
        val baseAllocations = when (analysis.reasoningMode) {
            PromptBuilder.ReasoningMode.ANALYTICAL -> mapOf(
                "system" to 0.15,
                "history" to 0.35,
                "memories" to 0.25,
                "sources" to 0.20,
                "attachments" to 0.05
            )
            PromptBuilder.ReasoningMode.CREATIVE -> mapOf(
                "system" to 0.20,  // More prompting guidance
                "history" to 0.40,
                "memories" to 0.20,
                "sources" to 0.10,
                "attachments" to 0.10
            )
            PromptBuilder.ReasoningMode.FACTUAL -> mapOf(
                "system" to 0.10,
                "history" to 0.20,
                "memories" to 0.30,  // Emphasize memories
                "sources" to 0.35,   // Emphasize sources
                "attachments" to 0.05
            )
            PromptBuilder.ReasoningMode.EMOTIONAL -> mapOf(
                "system" to 0.15,
                "history" to 0.45,  // Recent context matters
                "memories" to 0.30,  // Personal history matters
                "sources" to 0.05,
                "attachments" to 0.05
            )
            PromptBuilder.ReasoningMode.TECHNICAL -> mapOf(
                "system" to 0.15,
                "history" to 0.35,
                "memories" to 0.15,
                "sources" to 0.25,
                "attachments" to 0.10
            )
            PromptBuilder.ReasoningMode.CONVERSATIONAL -> mapOf(
                "system" to 0.15,
                "history" to 0.50,  // Emphasize flow
                "memories" to 0.20,
                "sources" to 0.10,
                "attachments" to 0.05
            )
        }

        // Calculate initial allocations
        var systemTokens = (usableBudget * baseAllocations["system"]!!).toInt()
        var historyTokens = (usableBudget * baseAllocations["history"]!!).toInt()
        var memoryTokens = (usableBudget * baseAllocations["memories"]!!).toInt()
        var sourcesTokens = (usableBudget * baseAllocations["sources"]!!).toInt()
        var attachmentTokens = (usableBudget * baseAllocations["attachments"]!!).toInt()

        // Reallocate if certain context isn't needed
        if (!analysis.requiresMemory) {
            // Redistribute memory tokens to history and sources
            historyTokens += memoryTokens / 2
            sourcesTokens += memoryTokens / 2
            memoryTokens = 0
        }

        if (!analysis.requiresSources) {
            // Redistribute source tokens to history and memory
            historyTokens += sourcesTokens / 2
            memoryTokens += sourcesTokens / 2
            sourcesTokens = 0
        }

        if (!analysis.hasAttachments) {
            // Redistribute attachment tokens to history
            historyTokens += attachmentTokens
            attachmentTokens = 0
        }

        // Boost system instructions for complex queries
        if (analysis.complexity in setOf(
                PromptBuilder.QueryComplexity.COMPLEX,
                PromptBuilder.QueryComplexity.RESEARCH
            )) {
            val boost = (usableBudget * 0.05).toInt()
            systemTokens += boost
            historyTokens -= boost / 2
            memoryTokens -= boost / 2
        }

        return AllocationStrategy(
            totalBudget = totalBudget,
            systemInstructions = systemTokens,
            conversationHistory = historyTokens,
            personaMemories = memoryTokens,
            pdfSources = sourcesTokens,
            attachments = attachmentTokens,
            reserve = reserve
        ).also {
            android.util.Log.d("ContextOptimizer",
                "Allocation for ${analysis.complexity}/${analysis.reasoningMode}: " +
                "total=${it.totalBudget}, system=${it.systemInstructions}, " +
                "history=${it.conversationHistory}, memories=${it.personaMemories}, " +
                "sources=${it.pdfSources}, attachments=${it.attachments}, " +
                "reserve=${it.reserve}, valid=${it.validate()}")
        }
    }

    /**
     * Calculate how many memories to retrieve given token budget
     */
    fun calculateMemoryLimit(tokenBudget: Int, avgMemoryTokens: Int = 100): Int {
        return min(tokenBudget / avgMemoryTokens, 100) // Cap at 100 memories
    }

    /**
     * Calculate how many PDF chunks to retrieve given token budget
     */
    fun calculateSourceChunkLimit(tokenBudget: Int, avgChunkTokens: Int = 500): Int {
        return min(tokenBudget / avgChunkTokens, 30) // Cap at 30 chunks
    }

    /**
     * Calculate how many conversation messages to include given token budget
     */
    fun calculateMessageLimit(tokenBudget: Int, avgMessageTokens: Int = 100): Int {
        return min(tokenBudget / avgMessageTokens, 200) // Cap at 200 messages
    }

    /**
     * Estimate if we're approaching token limits and should compress context
     */
    fun shouldCompressContext(currentTokens: Int): Boolean {
        return currentTokens > (MAX_SAFE_INPUT_TOKENS * 0.8) // 80% threshold
    }

    /**
     * Get compression recommendations when approaching limits
     */
    fun getCompressionRecommendations(
        currentAllocation: AllocationStrategy,
        targetReduction: Int
    ): Map<String, Int> {
        // Prioritize compressing less critical context
        // Order: attachments > sources > memories > history > system
        val recommendations = mutableMapOf<String, Int>()

        var remaining = targetReduction

        // 1. Reduce attachments first (keep minimal)
        if (currentAllocation.attachments > 0 && remaining > 0) {
            val reduction = min(remaining, (currentAllocation.attachments * 0.5).toInt())
            recommendations["attachments"] = currentAllocation.attachments - reduction
            remaining -= reduction
        }

        // 2. Reduce sources (keep top results only)
        if (currentAllocation.pdfSources > 0 && remaining > 0) {
            val reduction = min(remaining, (currentAllocation.pdfSources * 0.3).toInt())
            recommendations["sources"] = currentAllocation.pdfSources - reduction
            remaining -= reduction
        }

        // 3. Reduce memories (keep highest relevance)
        if (currentAllocation.personaMemories > 0 && remaining > 0) {
            val reduction = min(remaining, (currentAllocation.personaMemories * 0.3).toInt())
            recommendations["memories"] = currentAllocation.personaMemories - reduction
            remaining -= reduction
        }

        // 4. Reduce older history (keep recent messages)
        if (currentAllocation.conversationHistory > 0 && remaining > 0) {
            val reduction = min(remaining, (currentAllocation.conversationHistory * 0.3).toInt())
            recommendations["history"] = currentAllocation.conversationHistory - reduction
            remaining -= reduction
        }

        // 5. Compress system instructions (remove examples)
        if (currentAllocation.systemInstructions > 0 && remaining > 0) {
            val reduction = min(remaining, (currentAllocation.systemInstructions * 0.2).toInt())
            recommendations["system"] = currentAllocation.systemInstructions - reduction
        }

        return recommendations
    }
}
