package com.example.innovexia.memory.Mind.store.dao

import androidx.room.*
import com.example.innovexia.memory.Mind.store.entities.MemoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for memory CRUD operations
 */
@Dao
interface MemoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(memories: List<MemoryEntity>)

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getById(id: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE personaId = :personaId AND userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(personaId: String, userId: String, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE personaId = :personaId AND userId = :userId AND chatId = :chatId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentForChat(personaId: String, userId: String, chatId: String, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE personaId = :personaId AND userId = :userId ORDER BY createdAt DESC")
    fun observeAll(personaId: String, userId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE personaId = :personaId AND userId = :userId AND kind = :kind ORDER BY createdAt DESC")
    fun observeByKind(personaId: String, userId: String, kind: String): Flow<List<MemoryEntity>>

    @Query("SELECT kind, COUNT(*) as count FROM memories WHERE personaId = :personaId AND userId = :userId GROUP BY kind")
    fun observeCountsByKind(personaId: String, userId: String): Flow<List<KindCount>>

    @Query("SELECT COUNT(*) FROM memories WHERE personaId = :personaId AND userId = :userId")
    suspend fun getCount(personaId: String, userId: String): Int

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM memories")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM memories WHERE userId = :userId")
    suspend fun getTotalCountForUser(userId: String): Int

    @Query("SELECT COUNT(*) FROM memories WHERE userId = :userId")
    fun observeTotalCountForUser(userId: String): Flow<Int>

    /**
     * Get memories within a specific time range, ordered by creation time (chronological)
     * Useful for temporal queries like "what did I say yesterday" or "show me memories from last week"
     */
    @Query("SELECT * FROM memories WHERE personaId = :personaId AND userId = :userId AND createdAt >= :startTimeMs AND createdAt <= :endTimeMs ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getMemoriesBetweenTimes(
        personaId: String,
        userId: String,
        startTimeMs: Long,
        endTimeMs: Long,
        limit: Int
    ): List<MemoryEntity>

    /**
     * Get memories for a specific day (24-hour period)
     * Convenience wrapper around getMemoriesBetweenTimes
     */
    @Query("SELECT * FROM memories WHERE personaId = :personaId AND userId = :userId AND createdAt >= :dayStartMs AND createdAt < :dayEndMs ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getMemoriesForDay(
        personaId: String,
        userId: String,
        dayStartMs: Long,
        dayEndMs: Long,
        limit: Int
    ): List<MemoryEntity>

    @Query("UPDATE memories SET lastAccessed = :timestamp WHERE id IN (:ids)")
    suspend fun updateLastAccessed(ids: List<String>, timestamp: Long)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM memories WHERE personaId = :personaId")
    suspend fun deleteAll(personaId: String)

    @Query("DELETE FROM memories WHERE personaId = :personaId AND createdAt < :beforeTimestamp AND importance < :minImportance")
    suspend fun pruneLowImportance(personaId: String, beforeTimestamp: Long, minImportance: Double): Int

    @Query("DELETE FROM memories WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("DELETE FROM memories WHERE userId != :userId")
    suspend fun deleteAllNotForUser(userId: String): Int

    @Query("SELECT * FROM memories")
    suspend fun getAllMemories(): List<MemoryEntity>
}

/**
 * Result type for count queries
 */
data class KindCount(
    val kind: String,
    val count: Int
)
