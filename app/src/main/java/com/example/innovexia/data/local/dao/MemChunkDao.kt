package com.example.innovexia.data.local.dao

import androidx.room.*
import com.example.innovexia.data.local.entities.MemChunkEntity

/**
 * Data Access Object for memory chunk operations.
 * Future: Add embedding-based retrieval.
 */
@Dao
interface MemChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: MemChunkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<MemChunkEntity>)

    @Query("SELECT * FROM mem_chunks WHERE chatId = :chatId ORDER BY startIndex ASC")
    suspend fun forChat(chatId: String): List<MemChunkEntity>

    @Query("SELECT * FROM mem_chunks WHERE chatId = :chatId ORDER BY startIndex DESC LIMIT :limit")
    suspend fun lastN(chatId: String, limit: Int): List<MemChunkEntity>

    @Query("DELETE FROM mem_chunks WHERE chatId = :chatId")
    suspend fun deleteForChat(chatId: String)

    @Query("DELETE FROM mem_chunks")
    suspend fun deleteAll()
}
