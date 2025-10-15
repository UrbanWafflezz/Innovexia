package com.example.innovexia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.innovexia.data.local.entities.IncidentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    @Query("SELECT * FROM incidents WHERE endedAt IS NULL ORDER BY startedAt DESC")
    fun observeOpen(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents ORDER BY startedAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM incidents WHERE serviceId = :serviceId ORDER BY startedAt DESC LIMIT 10")
    suspend fun getForService(serviceId: String): List<IncidentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: IncidentEntity)

    @Query("UPDATE incidents SET endedAt = :end, status = 'Resolved' WHERE id = :id")
    suspend fun resolve(id: String, end: Long)

    @Query("DELETE FROM incidents WHERE endedAt IS NOT NULL AND startedAt < :before")
    suspend fun cleanupOld(before: Long)
}
