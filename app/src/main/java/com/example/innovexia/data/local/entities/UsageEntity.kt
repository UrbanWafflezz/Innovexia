package com.example.innovexia.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.innovexia.data.models.UsageData
import com.google.firebase.Timestamp

/**
 * Local cache of usage data for a specific period
 */
@Entity(tableName = "usage")
data class UsageEntity(
    @PrimaryKey
    val id: String, // Format: "{userId}_{periodId}" e.g., "user123_2025-10"
    val userId: String,
    val periodId: String, // "2025-10"
    val tokensIn: Long = 0L,
    val tokensOut: Long = 0L,
    val requests: Long = 0L,
    val attachmentsBytes: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis(),
    val lastSynced: Long = System.currentTimeMillis()
) {
    /**
     * Total tokens
     */
    val totalTokens: Long
        get() = tokensIn + tokensOut

    /**
     * Convert to domain model
     */
    fun toUsageData(): UsageData {
        return UsageData(
            periodId = periodId,
            tokensIn = tokensIn,
            tokensOut = tokensOut,
            requests = requests,
            attachmentsBytes = attachmentsBytes,
            lastUpdated = Timestamp(lastUpdated / 1000, ((lastUpdated % 1000) * 1_000_000).toInt())
        )
    }

    companion object {
        /**
         * Create entity ID
         */
        fun createId(userId: String, periodId: String): String {
            return "${userId}_${periodId}"
        }

        /**
         * Create from domain model
         */
        fun fromUsageData(userId: String, usage: UsageData): UsageEntity {
            return UsageEntity(
                id = createId(userId, usage.periodId),
                userId = userId,
                periodId = usage.periodId,
                tokensIn = usage.tokensIn,
                tokensOut = usage.tokensOut,
                requests = usage.requests,
                attachmentsBytes = usage.attachmentsBytes,
                lastUpdated = usage.lastUpdated.seconds * 1000 + usage.lastUpdated.nanoseconds / 1_000_000
            )
        }
    }
}

/**
 * Daily usage tracking (local only, aggregated from messages)
 */
@Entity(tableName = "daily_usage")
data class DailyUsageEntity(
    @PrimaryKey
    val id: String, // Format: "{userId}_{date}" e.g., "user123_2025-10-07"
    val userId: String,
    val date: String, // "2025-10-07"
    val tokensIn: Long = 0L,
    val tokensOut: Long = 0L,
    val requests: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val totalTokens: Long
        get() = tokensIn + tokensOut

    companion object {
        /**
         * Create entity ID
         */
        fun createId(userId: String, date: String): String {
            return "${userId}_${date}"
        }
    }
}
