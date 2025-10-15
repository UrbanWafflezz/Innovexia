package com.example.innovexia.data.local.dao

import androidx.room.*
import com.example.innovexia.data.local.entities.DailyUsageEntity
import com.example.innovexia.data.local.entities.UsageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for usage data
 */
@Dao
interface UsageDao {
    /**
     * Get usage for a specific period (Flow)
     */
    @Query("SELECT * FROM usage WHERE userId = :userId AND periodId = :periodId LIMIT 1")
    fun getUsageFlow(userId: String, periodId: String): Flow<UsageEntity?>

    /**
     * Get usage for a specific period (one-time)
     */
    @Query("SELECT * FROM usage WHERE userId = :userId AND periodId = :periodId LIMIT 1")
    suspend fun getUsage(userId: String, periodId: String): UsageEntity?

    /**
     * Get all usage records for a user
     */
    @Query("SELECT * FROM usage WHERE userId = :userId ORDER BY periodId DESC")
    suspend fun getAllUsage(userId: String): List<UsageEntity>

    /**
     * Insert or update usage
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: UsageEntity)

    /**
     * Increment usage counters atomically
     */
    @Query("""
        UPDATE usage
        SET tokensIn = tokensIn + :tokensIn,
            tokensOut = tokensOut + :tokensOut,
            requests = requests + :requests,
            attachmentsBytes = attachmentsBytes + :attachmentsBytes,
            lastUpdated = :timestamp
        WHERE id = :id
    """)
    suspend fun incrementUsage(
        id: String,
        tokensIn: Long,
        tokensOut: Long,
        requests: Long,
        attachmentsBytes: Long,
        timestamp: Long
    )

    /**
     * Delete usage for a specific period
     */
    @Query("DELETE FROM usage WHERE userId = :userId AND periodId = :periodId")
    suspend fun delete(userId: String, periodId: String)

    /**
     * Delete all usage for a user
     */
    @Query("DELETE FROM usage WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    /**
     * Delete all usage
     */
    @Query("DELETE FROM usage")
    suspend fun deleteAll()

    // ==================== Daily Usage ====================

    /**
     * Get daily usage (Flow)
     */
    @Query("SELECT * FROM daily_usage WHERE userId = :userId AND date = :date LIMIT 1")
    fun getDailyUsageFlow(userId: String, date: String): Flow<DailyUsageEntity?>

    /**
     * Get daily usage (one-time)
     */
    @Query("SELECT * FROM daily_usage WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getDailyUsage(userId: String, date: String): DailyUsageEntity?

    /**
     * Get recent daily usage (last N days)
     */
    @Query("SELECT * FROM daily_usage WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDailyUsage(userId: String, limit: Int = 30): List<DailyUsageEntity>

    /**
     * Insert or update daily usage
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDaily(usage: DailyUsageEntity)

    /**
     * Increment daily usage counters
     */
    @Query("""
        UPDATE daily_usage
        SET tokensIn = tokensIn + :tokensIn,
            tokensOut = tokensOut + :tokensOut,
            requests = requests + :requests,
            lastUpdated = :timestamp
        WHERE id = :id
    """)
    suspend fun incrementDailyUsage(
        id: String,
        tokensIn: Long,
        tokensOut: Long,
        requests: Long,
        timestamp: Long
    )

    /**
     * Delete old daily usage records (cleanup)
     */
    @Query("DELETE FROM daily_usage WHERE userId = :userId AND date < :beforeDate")
    suspend fun deleteOldDailyUsage(userId: String, beforeDate: String)

    /**
     * Delete all daily usage
     */
    @Query("DELETE FROM daily_usage")
    suspend fun deleteAllDaily()
}
