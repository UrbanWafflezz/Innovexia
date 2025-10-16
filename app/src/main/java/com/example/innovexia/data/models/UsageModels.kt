package com.example.innovexia.data.models

import com.google.firebase.Timestamp
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * Usage data for a specific period (stored in Firestore /users/{uid}/usage/{periodId})
 */
data class UsageData(
    val periodId: String, // Format: "2025-10" (YYYY-MM)
    val tokensIn: Long = 0L,
    val tokensOut: Long = 0L,
    val requests: Long = 0L,
    val attachmentsBytes: Long = 0L,
    val lastUpdated: Timestamp = Timestamp.now()
) {
    /**
     * Total tokens (input + output)
     */
    val totalTokens: Long
        get() = tokensIn + tokensOut

    /**
     * Convert to Firestore map
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "tokensIn" to tokensIn,
            "tokensOut" to tokensOut,
            "requests" to requests,
            "attachmentsBytes" to attachmentsBytes,
            "lastUpdated" to lastUpdated
        )
    }

    companion object {
        /**
         * Parse from Firestore document
         */
        fun fromMap(periodId: String, map: Map<String, Any>): UsageData {
            return UsageData(
                periodId = periodId,
                tokensIn = (map["tokensIn"] as? Number)?.toLong() ?: 0L,
                tokensOut = (map["tokensOut"] as? Number)?.toLong() ?: 0L,
                requests = (map["requests"] as? Number)?.toLong() ?: 0L,
                attachmentsBytes = (map["attachmentsBytes"] as? Number)?.toLong() ?: 0L,
                lastUpdated = map["lastUpdated"] as? Timestamp ?: Timestamp.now()
            )
        }

        /**
         * Get current period ID (YYYY-MM format)
         */
        fun getCurrentPeriodId(): String {
            val now = YearMonth.now()
            return String.format("%04d-%02d", now.year, now.monthValue)
        }

        /**
         * Get period ID for a specific timestamp
         */
        fun getPeriodId(timestamp: Long): String {
            val instant = Instant.ofEpochMilli(timestamp)
            val yearMonth = YearMonth.from(instant.atZone(ZoneId.systemDefault()))
            return String.format("%04d-%02d", yearMonth.year, yearMonth.monthValue)
        }
    }
}

/**
 * Rate limiting data (stored in Firestore /users/{uid}/rate/now)
 */
data class RateLimitData(
    val minuteWindowStart: Timestamp = Timestamp.now(),
    val requestsThisMinute: Int = 0
) {
    /**
     * Check if within the same minute window
     */
    fun isInCurrentWindow(): Boolean {
        val now = Timestamp.now().seconds
        val windowStart = minuteWindowStart.seconds
        return (now - windowStart) < 60
    }

    /**
     * Convert to Firestore map
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "minuteWindowStart" to minuteWindowStart,
            "requestsThisMinute" to requestsThisMinute
        )
    }

    companion object {
        /**
         * Parse from Firestore document
         */
        fun fromMap(map: Map<String, Any>): RateLimitData {
            return RateLimitData(
                minuteWindowStart = map["minuteWindowStart"] as? Timestamp ?: Timestamp.now(),
                requestsThisMinute = (map["requestsThisMinute"] as? Number)?.toInt() ?: 0
            )
        }
    }
}

/**
 * Real-time usage snapshot returned by the API
 */
data class UsageSnapshot(
    val monthId: String,
    val tokensIn: Long,
    val tokensOut: Long,
    val requests: Long,
    val attachmentsBytes: Long,
    val minuteCount: Int,
    val burstLimit: Int,
    val monthlyLimit: Long,
    val dailyLimit: Long,
    val periodEnd: Long // Timestamp in seconds
) {
    /**
     * Total tokens used this month
     */
    val totalTokens: Long
        get() = tokensIn + tokensOut

    /**
     * Percentage of monthly limit used (0-100)
     */
    fun monthlyUsagePercent(): Float {
        if (monthlyLimit == 0L) return 0f
        return (totalTokens.toFloat() / monthlyLimit.toFloat() * 100f).coerceIn(0f, 100f)
    }

    /**
     * Percentage of burst limit used (0-100)
     */
    fun burstUsagePercent(): Float {
        if (burstLimit == 0) return 0f
        return (minuteCount.toFloat() / burstLimit.toFloat() * 100f).coerceIn(0f, 100f)
    }

    /**
     * Check if user is approaching monthly limit (>= 90%)
     */
    fun isApproachingLimit(): Boolean {
        return monthlyUsagePercent() >= 90f
    }

    /**
     * Check if user has exceeded monthly limit
     */
    fun hasExceededLimit(): Boolean {
        return totalTokens >= monthlyLimit
    }

    /**
     * Check if burst rate is exceeded
     */
    fun isBurstLimitExceeded(): Boolean {
        return minuteCount >= burstLimit
    }

    companion object {
        /**
         * Create empty snapshot
         */
        fun empty(plan: SubscriptionPlan): UsageSnapshot {
            val limits = PlanLimits.getLimits(plan)
            val now = Timestamp.now()
            val oneMonthLater = now.seconds + 30L * 24 * 60 * 60

            return UsageSnapshot(
                monthId = UsageData.getCurrentPeriodId(),
                tokensIn = 0L,
                tokensOut = 0L,
                requests = 0L,
                attachmentsBytes = 0L,
                minuteCount = 0,
                burstLimit = limits.burstRequestsPerMinute,
                monthlyLimit = limits.monthlyTokens,
                dailyLimit = limits.dailyTokens,
                periodEnd = oneMonthLater
            )
        }
    }
}

/**
 * Daily usage summary (for "Today" stats)
 */
data class DailyUsage(
    val date: String, // Format: "2025-10-07"
    val tokensIn: Long = 0L,
    val tokensOut: Long = 0L,
    val requests: Long = 0L
) {
    val totalTokens: Long
        get() = tokensIn + tokensOut

    companion object {
        /**
         * Get today's date string
         */
        fun getTodayDate(): String {
            val now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
            return String.format("%04d-%02d-%02d", now.year, now.monthValue, now.dayOfMonth)
        }
    }
}
