package com.example.innovexia.memory.Mind.store.dao

import androidx.room.*
import com.example.innovexia.memory.Mind.store.entities.MemoryVectorEntity

/**
 * DAO for vector operations
 */
@Dao
interface VectorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vector: MemoryVectorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vectors: List<MemoryVectorEntity>)

    @Query("SELECT * FROM memory_vectors WHERE memoryId = :memoryId")
    suspend fun getByMemoryId(memoryId: String): MemoryVectorEntity?

    @Query("""
        SELECT mv.* FROM memory_vectors mv
        JOIN memories m ON mv.memoryId = m.id
        WHERE m.personaId = :personaId
    """)
    suspend fun getAllByPersona(personaId: String): List<MemoryVectorEntity>

    @Query("DELETE FROM memory_vectors WHERE memoryId = :memoryId")
    suspend fun deleteByMemoryId(memoryId: String)

    @Query("""
        DELETE FROM memory_vectors WHERE memoryId IN (
            SELECT id FROM memories WHERE personaId = :personaId
        )
    """)
    suspend fun deleteAllByPersona(personaId: String)
}
