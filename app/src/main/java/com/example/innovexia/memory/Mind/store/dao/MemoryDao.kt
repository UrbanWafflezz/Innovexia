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

    @Query("SELECT * FROM memories WHERE personaId = :personaId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(personaId: String, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE personaId = :personaId AND chatId = :chatId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentForChat(personaId: String, chatId: String, limit: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE personaId = :personaId ORDER BY createdAt DESC")
    fun observeAll(personaId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE personaId = :personaId AND kind = :kind ORDER BY createdAt DESC")
    fun observeByKind(personaId: String, kind: String): Flow<List<MemoryEntity>>

    @Query("SELECT kind, COUNT(*) as count FROM memories WHERE personaId = :personaId GROUP BY kind")
    fun observeCountsByKind(personaId: String): Flow<List<KindCount>>

    @Query("SELECT COUNT(*) FROM memories WHERE personaId = :personaId")
    suspend fun getCount(personaId: String): Int

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM memories")
    fun observeTotalCount(): Flow<Int>

    @Query("UPDATE memories SET lastAccessed = :timestamp WHERE id IN (:ids)")
    suspend fun updateLastAccessed(ids: List<String>, timestamp: Long)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM memories WHERE personaId = :personaId")
    suspend fun deleteAll(personaId: String)

    @Query("DELETE FROM memories WHERE personaId = :personaId AND createdAt < :beforeTimestamp AND importance < :minImportance")
    suspend fun pruneLowImportance(personaId: String, beforeTimestamp: Long, minImportance: Double): Int
}

/**
 * Result type for count queries
 */
data class KindCount(
    val kind: String,
    val count: Int
)
