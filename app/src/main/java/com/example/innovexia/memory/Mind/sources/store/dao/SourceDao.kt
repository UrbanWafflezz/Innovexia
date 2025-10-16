package com.example.innovexia.memory.Mind.sources.store.dao

import androidx.room.*
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for source documents
 */
@Dao
interface SourceDao {

    @Query("SELECT * FROM sources WHERE personaId = :personaId ORDER BY addedAt DESC")
    fun observeByPersona(personaId: String): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE personaId = :personaId AND id = :sourceId")
    fun observe(personaId: String, sourceId: String): Flow<SourceEntity?>

    @Query("SELECT * FROM sources WHERE id = :sourceId")
    suspend fun getById(sourceId: String): SourceEntity?

    @Query("SELECT * FROM sources WHERE personaId = :personaId ORDER BY addedAt DESC")
    suspend fun getByPersona(personaId: String): List<SourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: SourceEntity)

    @Update
    suspend fun update(source: SourceEntity)

    @Query("UPDATE sources SET status = :status, errorMsg = :errorMsg WHERE id = :sourceId")
    suspend fun updateStatus(sourceId: String, status: String, errorMsg: String?)

    @Query("UPDATE sources SET status = :status, lastIndexedAt = :lastIndexedAt WHERE id = :sourceId")
    suspend fun updateIndexed(sourceId: String, status: String, lastIndexedAt: Long)

    @Query("DELETE FROM sources WHERE id = :sourceId")
    suspend fun deleteById(sourceId: String)

    @Query("DELETE FROM sources WHERE personaId = :personaId")
    suspend fun deleteByPersona(personaId: String)

    @Query("SELECT SUM(bytes) FROM sources WHERE personaId = :personaId")
    suspend fun getTotalBytes(personaId: String): Long?

    @Query("SELECT COUNT(*) FROM sources WHERE personaId = :personaId")
    suspend fun getCount(personaId: String): Int

    @Query("SELECT COUNT(*) FROM sources")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM sources")
    fun observeTotalCount(): Flow<Int>
}
