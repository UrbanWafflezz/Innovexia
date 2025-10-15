package com.example.innovexia.subscriptions.mock

/**
 * Usage limits and rate limits for each plan tier
 * Follows Claude Code model: 5-hour windows with reset timer from first message
 */
data class UsageLimits(
    val messagesPerWindow: Int,        // Messages allowed per 5-hour window
    val tokensPerWindow: Long,         // Token limit per 5-hour window
    val contextLength: String,         // Max context window
    val windowDurationHours: Int = 5   // Window duration in hours
) {
    companion object {
        /**
         * Get usage limits for a plan
         */
        fun forPlan(plan: PlanId): UsageLimits {
            return when (plan) {
                PlanId.FREE -> UsageLimits(
                    messagesPerWindow = 25,
                    tokensPerWindow = 100_000L,
                    contextLength = "32K"
                )
                PlanId.PLUS -> UsageLimits(
                    messagesPerWindow = 100,
                    tokensPerWindow = 500_000L,
                    contextLength = "128K"
                )
                PlanId.PRO -> UsageLimits(
                    messagesPerWindow = 250,
                    tokensPerWindow = 1_500_000L,
                    contextLength = "256K"
                )
                PlanId.MASTER -> UsageLimits(
                    messagesPerWindow = 1000,
                    tokensPerWindow = 5_000_000L,
                    contextLength = "512K"
                )
            }
        }

        /**
         * Window duration in milliseconds
         */
        const val WINDOW_DURATION_MS = 5 * 60 * 60 * 1000L // 5 hours
    }
}

/**
 * Current usage window state
 */
data class UsageWindow(
    val windowStartTime: Long,         // Timestamp when window started (first message)
    val messageCount: Int = 0,         // Messages sent in this window
    val tokensUsed: Long = 0L,         // Tokens used in this window
    val tokensIn: Long = 0L,           // Input tokens
    val tokensOut: Long = 0L           // Output tokens
) {
    /**
     * Time remaining in window (milliseconds)
     */
    fun timeRemainingMs(): Long {
        val elapsed = System.currentTimeMillis() - windowStartTime
        val remaining = UsageLimits.WINDOW_DURATION_MS - elapsed
        return remaining.coerceAtLeast(0L)
    }

    /**
     * Check if window has expired
     */
    fun hasExpired(): Boolean {
        return timeRemainingMs() == 0L
    }

    /**
     * Time until reset in human readable format
     */
    fun timeUntilReset(): String {
        val ms = timeRemainingMs()
        val hours = ms / (60 * 60 * 1000)
        val minutes = (ms % (60 * 60 * 1000)) / (60 * 1000)

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "Now"
        }
    }

    /**
     * Progress percentage (0-100)
     */
    fun progressPercent(limit: UsageLimits): Float {
        val messagePercent = (messageCount.toFloat() / limit.messagesPerWindow.toFloat() * 100f)
        val tokenPercent = (tokensUsed.toFloat() / limit.tokensPerWindow.toFloat() * 100f)
        return maxOf(messagePercent, tokenPercent).coerceIn(0f, 100f)
    }

    /**
     * Check if limit is reached
     */
    fun isLimitReached(limit: UsageLimits): Boolean {
        return messageCount >= limit.messagesPerWindow || tokensUsed >= limit.tokensPerWindow
    }

    /**
     * Check if approaching limit (>= 90%)
     */
    fun isApproachingLimit(limit: UsageLimits): Boolean {
        return progressPercent(limit) >= 90f
    }

    companion object {
        /**
         * Create a new window starting now
         */
        fun create(): UsageWindow {
            return UsageWindow(
                windowStartTime = System.currentTimeMillis()
            )
        }

        /**
         * Create window from stored data
         */
        fun fromData(
            windowStartTime: Long,
            messageCount: Int,
            tokensIn: Long,
            tokensOut: Long
        ): UsageWindow {
            return UsageWindow(
                windowStartTime = windowStartTime,
                messageCount = messageCount,
                tokensUsed = tokensIn + tokensOut,
                tokensIn = tokensIn,
                tokensOut = tokensOut
            )
        }
    }
}
