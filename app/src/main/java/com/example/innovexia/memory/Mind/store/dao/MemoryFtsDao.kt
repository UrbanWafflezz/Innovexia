package com.example.innovexia.memory.Mind.store.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.innovexia.memory.Mind.store.entities.MemoryFtsEntity

/**
 * DAO for FTS operations
 */
@Dao
interface MemoryFtsDao {

    /**
     * Insert FTS entry
     */
    @Insert
    suspend fun insert(ftsEntity: MemoryFtsEntity)

    /**
     * Delete FTS entry by id
     */
    @Query("DELETE FROM memories_fts WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * Full-text search across memories
     * Returns memory IDs matching the query
     */
    @Query("""
        SELECT memories.id
        FROM memories_fts
        JOIN memories ON memories.id = memories_fts.id
        WHERE memories_fts MATCH :query
        AND memories.personaId = :personaId
        AND memories.userId = :userId
        LIMIT :limit
    """)
    suspend fun search(personaId: String, userId: String, query: String, limit: Int): List<String>
}
