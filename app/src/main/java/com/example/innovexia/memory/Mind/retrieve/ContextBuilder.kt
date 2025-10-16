package com.example.innovexia.memory.Mind.retrieve

import com.example.innovexia.memory.Mind.api.ContextBundle
import com.example.innovexia.memory.Mind.api.Memory
import com.example.innovexia.memory.Mind.store.dao.MemoryDao

/**
 * Builds context bundles for LLM prompting
 */
class ContextBuilder(
    private val memoryDao: MemoryDao,
    private val retriever: Retriever
) {

    /**
     * Build context for a user message
     */
    suspend fun contextFor(
        message: String,
        personaId: String,
        chatId: String,
        maxTokens: Int = 2000
    ): ContextBundle {
        // Get short-term: ALL recent turns from THIS specific chat
        // INCREASED: From 10 to 100 to ensure comprehensive context
        val shortTerm = memoryDao.getRecentForChat(personaId, chatId, limit = 100)
            .map { it.toMemory() }

        android.util.Log.d("ContextBuilder", "Retrieved ${shortTerm.size} short-term memories for persona=$personaId, chat=$chatId")

        // Get long-term: retrieved relevant memories across ALL chats
        // INCREASED: From 12 to 50 to capture more context
        val longTerm = retriever.recall(personaId, message, k = 50)

        android.util.Log.d("ContextBuilder", "Retrieved ${longTerm.size} long-term memories via hybrid search")

        // Estimate tokens (rough: 1 token ≈ 4 chars)
        val estimatedTokens = (shortTerm.sumOf { it.text.length } +
                               longTerm.sumOf { it.memory.text.length }) / 4

        android.util.Log.d("ContextBuilder", "Total estimated tokens: $estimatedTokens")

        return ContextBundle(
            shortTerm = shortTerm,
            longTerm = longTerm,
            totalTokens = estimatedTokens
        )
    }

    /**
     * Convert MemoryEntity to Memory
     */
    private fun com.example.innovexia.memory.Mind.store.entities.MemoryEntity.toMemory() =
        Memory(
            id = id,
            personaId = personaId,
            userId = userId,
            chatId = chatId,
            role = role,
            text = text,
            kind = com.example.innovexia.memory.Mind.api.MemoryKind.valueOf(kind),
            emotion = emotion?.let { com.example.innovexia.memory.Mind.api.EmotionType.valueOf(it) },
            importance = importance,
            createdAt = createdAt,
            lastAccessed = lastAccessed
        )
}
