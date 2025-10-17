package com.example.innovexia.memory.Mind

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.innovexia.memory.Mind.api.*
import com.example.innovexia.memory.Mind.ingest.Ingestor
import com.example.innovexia.memory.Mind.retrieve.ContextBuilder
import com.example.innovexia.memory.Mind.retrieve.Retriever
import com.example.innovexia.memory.Mind.store.MemoryDatabase
import com.example.innovexia.memory.Mind.store.dao.KindCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore extension for memory preferences
 */
private val Context.memoryPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "memory_prefs")

/**
 * Implementation of MemoryEngine
 */
class MemoryEngineImpl(
    private val context: Context,
    private val database: MemoryDatabase,
    private val ingestor: Ingestor,
    private val retriever: Retriever,
    private val contextBuilder: ContextBuilder
) : MemoryEngine {

    private val memoryDao = database.memoryDao()

    override suspend fun enable(personaId: String, enabled: Boolean) {
        val key = booleanPreferencesKey("memory_enabled_$personaId")
        context.memoryPrefsDataStore.edit { prefs ->
            prefs[key] = enabled
        }
    }

    override suspend fun isEnabled(personaId: String): Boolean {
        val key = booleanPreferencesKey("memory_enabled_$personaId")
        return context.memoryPrefsDataStore.data.map { prefs ->
            prefs[key] ?: true // default enabled
        }.first()
    }

    override suspend fun ingest(turn: ChatTurn, personaId: String, incognito: Boolean) {
        val enabled = isEnabled(personaId)
        android.util.Log.d("MemoryEngineImpl", "ingest() called - personaId=$personaId, enabled=$enabled, incognito=$incognito")
        if (!enabled) {
            android.util.Log.w("MemoryEngineImpl", "Memory is DISABLED for persona $personaId - skipping ingestion")
            return
        }
        ingestor.ingest(turn, personaId, incognito)
    }

    override suspend fun contextFor(message: String, personaId: String, chatId: String): ContextBundle {
        if (!isEnabled(personaId)) {
            return ContextBundle(emptyList(), emptyList(), 0)
        }

        // Get userId from FirebaseAuth
        val userId = com.example.innovexia.core.auth.FirebaseAuthManager.currentUser()?.uid
            ?: com.example.innovexia.core.auth.ProfileId.GUEST_OWNER_ID

        return contextBuilder.contextFor(message, personaId, userId, chatId)
    }

    override fun observeCounts(personaId: String, userId: String): Flow<List<CategoryCount>> {
        return memoryDao.observeCountsByKind(personaId, userId).map { kindCounts ->
            kindCounts.map { kc ->
                CategoryCount(
                    kind = MemoryKind.valueOf(kc.kind),
                    count = kc.count
                )
            }
        }
    }

    override fun feed(personaId: String, userId: String, kind: MemoryKind?, query: String?): Flow<List<MemoryHit>> {
        // For now, return recent memories as Flow
        // TODO: Implement proper search with query
        val flow = if (kind != null) {
            memoryDao.observeByKind(personaId, userId, kind.name)
        } else {
            memoryDao.observeAll(personaId, userId)
        }

        return flow.map { entities ->
            entities
                .filter { entity ->
                    query == null || entity.text.contains(query, ignoreCase = true)
                }
                .map { entity ->
                    MemoryHit(
                        memory = entity.toMemory(),
                        score = 1.0,
                        fromChatTitle = null
                    )
                }
        }
    }

    override suspend fun delete(memoryId: String) {
        memoryDao.deleteById(memoryId)
    }

    override suspend fun deleteAll(personaId: String) {
        memoryDao.deleteAll(personaId)
    }

    override suspend fun getCount(personaId: String, userId: String): Int {
        val count = memoryDao.getCount(personaId, userId)
        android.util.Log.d("MemoryEngineImpl", "getCount for personaId=$personaId, userId=$userId: $count")
        return count
    }

    /**
     * Clear all memory preferences for a specific owner/user.
     * Used when signing out to reset memory state.
     * Note: This clears memory enable/disable preferences, not the actual memories.
     */
    suspend fun clearAllPreferencesForOwner(ownerId: String) {
        // Memory preferences are keyed by personaId, so we need to clear all memory-related keys
        // This is a broad clear - removes all memory enable/disable flags
        // The actual memory data in the database should be cleared separately if needed
        context.memoryPrefsDataStore.edit { prefs ->
            // Get all keys that contain memory_enabled prefix
            val keysToRemove = prefs.asMap().keys.filter {
                it.name.startsWith("memory_enabled_")
            }
            keysToRemove.forEach { key ->
                prefs.remove(key)
            }
        }
    }

    /**
     * Clear all memories from the database for a specific owner/user.
     * Used when signing out to ensure memories don't persist across different accounts.
     * This deletes the actual memory data from Room database.
     */
    suspend fun clearAllMemoriesForOwner(ownerId: String) {
        memoryDao.deleteAllForUser(ownerId)
        android.util.Log.d("MemoryEngineImpl", "Cleared all memories for user: $ownerId")
    }

    /**
     * Clear ALL memories from the database (all users).
     * This is a nuclear option for debugging or manual cleanup.
     * Clears all memory-related tables.
     */
    suspend fun clearAllMemories() {
        val totalCount = memoryDao.getTotalCount()
        android.util.Log.d("MemoryEngineImpl", "Clearing all memories (total: $totalCount)")

        // Clear all tables in the memory database
        database.clearAllTables()

        android.util.Log.d("MemoryEngineImpl", "All memories cleared from database")
    }

    /**
     * Delete all memories that don't belong to the specified userId.
     * This is useful for cleaning up memories from other users that may be
     * incorrectly visible due to missing or incorrect userId filtering.
     */
    suspend fun deleteMemoriesNotOwnedBy(userId: String): Int {
        val totalBefore = memoryDao.getTotalCount()
        android.util.Log.d("MemoryEngineImpl", "Cleaning memories not owned by user: $userId (total before: $totalBefore)")

        val deletedCount = memoryDao.deleteAllNotForUser(userId)

        val totalAfter = memoryDao.getTotalCount()
        android.util.Log.d("MemoryEngineImpl", "Cleanup complete: deleted $deletedCount memories, remaining: $totalAfter")

        return deletedCount
    }

    /**
     * Debug function to log all memories in the database with their userIds.
     * Useful for troubleshooting memory isolation issues.
     */
    suspend fun debugLogAllMemories() {
        val allMemories = memoryDao.getAllMemories()
        android.util.Log.d("MemoryEngineImpl", "=== DEBUG: All Memories in Database (total: ${allMemories.size}) ===")
        allMemories.forEachIndexed { index, memory ->
            android.util.Log.d("MemoryEngineImpl", "Memory[$index]: id=${memory.id}, userId=${memory.userId}, personaId=${memory.personaId}, text=${memory.text.take(50)}")
        }
        android.util.Log.d("MemoryEngineImpl", "=== END DEBUG ===")
    }

    /**
     * Convert entity to API model
     */
    private fun com.example.innovexia.memory.Mind.store.entities.MemoryEntity.toMemory() = Memory(
        id = id,
        personaId = personaId,
        userId = userId,
        chatId = chatId,
        role = role,
        text = text,
        kind = MemoryKind.valueOf(kind),
        emotion = emotion?.let { EmotionType.valueOf(it) },
        importance = importance,
        createdAt = createdAt,
        lastAccessed = lastAccessed
    )
}
