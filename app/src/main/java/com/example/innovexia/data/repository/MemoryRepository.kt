package com.example.innovexia.data.repository

import com.example.innovexia.data.local.dao.MemChunkDao
import com.example.innovexia.ui.persona.MemoryItem
import com.example.innovexia.ui.persona.MemoryScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for accessing memory chunks and highlights.
 * Provides recent memories for smart greeting suggestions.
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val memChunkDao: MemChunkDao
) {
    /**
     * Fetch recent memory highlights (last N chunks across all chats)
     * Used for smart greeting suggestions
     */
    suspend fun fetchRecentHighlights(limit: Int = 5): List<MemoryItem> = withContext(Dispatchers.IO) {
        // TODO: Once we have a proper memory table with global scope, query it
        // For now, return empty list as placeholder
        // Future implementation:
        // - Query mem_chunks table for recent entries
        // - Group by category/topic
        // - Return most relevant memories
        emptyList()
    }

    /**
     * Get memories for a specific chat
     */
    suspend fun getMemoriesForChat(chatId: String, limit: Int = 10): List<MemoryItem> = withContext(Dispatchers.IO) {
        val chunks = memChunkDao.lastN(chatId, limit)
        chunks.map { chunk ->
            MemoryItem(
                id = chunk.id,
                scope = MemoryScope.Chat,
                text = chunk.text,
                createdAt = formatTimestamp(System.currentTimeMillis()),
                pinned = false
            )
        }
    }

    /**
     * Get all memory chunks for a chat
     */
    suspend fun getAllMemoriesForChat(chatId: String): List<MemoryItem> = withContext(Dispatchers.IO) {
        val chunks = memChunkDao.forChat(chatId)
        chunks.map { chunk ->
            MemoryItem(
                id = chunk.id,
                scope = MemoryScope.Chat,
                text = chunk.text,
                createdAt = formatTimestamp(System.currentTimeMillis()),
                pinned = false
            )
        }
    }

    /**
     * Format timestamp to relative time
     */
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "Over a week ago"
        }
    }
}
