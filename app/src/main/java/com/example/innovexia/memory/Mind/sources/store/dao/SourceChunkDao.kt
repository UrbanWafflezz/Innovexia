package com.example.innovexia.memory.Mind.sources.store.dao

import androidx.room.*
import com.example.innovexia.memory.Mind.sources.store.entities.SourceChunkEntity

/**
 * DAO for source chunks with embeddings
 */
@Dao
interface SourceChunkDao {

    @Query("SELECT * FROM source_chunks WHERE sourceId = :sourceId ORDER BY pageStart, pageEnd")
    suspend fun getBySource(sourceId: String): List<SourceChunkEntity>

    @Query("SELECT * FROM source_chunks WHERE personaId = :personaId")
    suspend fun getByPersona(personaId: String): List<SourceChunkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: SourceChunkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<SourceChunkEntity>)

    @Query("DELETE FROM source_chunks WHERE sourceId = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    @Query("DELETE FROM source_chunks WHERE personaId = :personaId")
    suspend fun deleteByPersona(personaId: String)

    @Query("SELECT COUNT(*) FROM source_chunks WHERE sourceId = :sourceId")
    suspend fun getChunkCount(sourceId: String): Int

    @Query("SELECT COUNT(*) FROM source_chunks WHERE personaId = :personaId")
    suspend fun getTotalChunks(personaId: String): Int

    /**
     * Search chunks by similarity (for future retrieval)
     * For now, just get all chunks for a persona
     */
    @Query("SELECT * FROM source_chunks WHERE personaId = :personaId LIMIT :limit")
    suspend fun searchByPersona(personaId: String, limit: Int = 20): List<SourceChunkEntity>
}
