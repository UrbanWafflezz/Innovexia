package com.example.innovexia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.innovexia.data.local.entities.HealthCheckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthCheckDao {

    @Query("SELECT * FROM health_checks ORDER BY serviceId")
    fun observe(): Flow<List<HealthCheckEntity>>

    @Query("SELECT * FROM health_checks WHERE serviceId = :serviceId")
    suspend fun getById(serviceId: String): HealthCheckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: HealthCheckEntity)

    @Query("DELETE FROM health_checks WHERE serviceId = :serviceId")
    suspend fun deleteByServiceId(serviceId: String)

    @Query("DELETE FROM health_checks")
    suspend fun clearAll()
}
