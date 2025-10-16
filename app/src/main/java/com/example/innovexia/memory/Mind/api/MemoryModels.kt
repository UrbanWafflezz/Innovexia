package com.example.innovexia.memory.Mind.api

/**
 * Memory category/kind classification
 */
enum class MemoryKind {
    FACT,        // Factual information about the user
    EVENT,       // Events, activities, experiences
    PREFERENCE,  // User preferences, likes/dislikes
    PROJECT,     // Ongoing projects, goals
    KNOWLEDGE,   // Learned information, skills
    EMOTION,     // Emotional context, feelings
    OTHER        // Uncategorized
}

/**
 * Emotion classification for memories
 */
enum class EmotionType {
    HAPPY,
    SAD,
    EXCITED,
    CURIOUS,
    FRUSTRATED,
    NEUTRAL,
    ANXIOUS,
    CONFIDENT
}

/**
 * Core memory data model
 */
data class Memory(
    val id: String,
    val personaId: String,
    val userId: String,
    val chatId: String?,
    val role: String, // "user" | "model"
    val text: String,
    val kind: MemoryKind,
    val emotion: EmotionType?,
    val importance: Double, // 0.0 to 1.0
    val createdAt: Long,
    val lastAccessed: Long
)

/**
 * Search/retrieval result with score
 */
data class MemoryHit(
    val memory: Memory,
    val score: Double,
    val fromChatTitle: String?
)

/**
 * Context bundle for LLM prompting
 */
data class ContextBundle(
    val shortTerm: List<Memory>,  // Recent chat turns
    val longTerm: List<MemoryHit>, // Retrieved relevant memories
    val totalTokens: Int
)

/**
 * Category count for UI overview
 */
data class CategoryCount(
    val kind: MemoryKind,
    val count: Int
)

/**
 * Chat turn for ingestion
 */
data class ChatTurn(
    val chatId: String,
    val userId: String,
    val userMessage: String,
    val assistantMessage: String?,
    val timestamp: Long
)

/**
 * Memory search filters
 */
data class MemoryFilters(
    val kind: MemoryKind? = null,
    val minImportance: Double? = null,
    val afterTimestamp: Long? = null,
    val beforeTimestamp: Long? = null
)
