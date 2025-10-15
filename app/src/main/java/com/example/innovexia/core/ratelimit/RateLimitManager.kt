package com.example.innovexia.core.ratelimit

import com.example.innovexia.data.models.PlanLimits
import com.example.innovexia.data.models.SubscriptionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.ceil

/**
 * Client-side rate limit manager
 * Enforces burst rate limits (requests per minute) for each plan tier
 */
class RateLimitManager {

    private val requestTimestamps = ConcurrentLinkedQueue<Long>()

    private val _currentCount = MutableStateFlow(0)
    val currentCount: StateFlow<Int> = _currentCount.asStateFlow()

    private val _isLimited = MutableStateFlow(false)
    val isLimited: StateFlow<Boolean> = _isLimited.asStateFlow()

    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds.asStateFlow()

    /**
     * Check if a request can be made under the current rate limit
     * @param plan The user's subscription plan
     * @return Pair<Boolean, Int> - (canMakeRequest, secondsUntilAllowed)
     */
    fun canMakeRequest(plan: SubscriptionPlan): Pair<Boolean, Int> {
        val limits = PlanLimits.getLimits(plan)
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000L

        // Clean up old timestamps (older than 1 minute)
        cleanupOldTimestamps(oneMinuteAgo)

        val count = requestTimestamps.size
        _currentCount.value = count

        // Check if we've hit the limit
        if (count >= limits.burstRequestsPerMinute) {
            // Calculate when the oldest request will expire
            val oldestTimestamp = requestTimestamps.peek()
            if (oldestTimestamp != null) {
                val secondsUntilExpiry = ceil((oldestTimestamp + 60_000L - now) / 1000.0).toInt()
                _isLimited.value = true
                _cooldownSeconds.value = secondsUntilExpiry.coerceAtLeast(1)
                return Pair(false, secondsUntilExpiry)
            }
        }

        _isLimited.value = false
        _cooldownSeconds.value = 0
        return Pair(true, 0)
    }

    /**
     * Record a request made at the current time
     */
    fun recordRequest() {
        val now = System.currentTimeMillis()
        requestTimestamps.add(now)
        _currentCount.value = requestTimestamps.size

        // Clean up old timestamps
        val oneMinuteAgo = now - 60_000L
        cleanupOldTimestamps(oneMinuteAgo)
    }

    /**
     * Get the current request count in the last minute
     */
    fun getCurrentCount(plan: SubscriptionPlan): Pair<Int, Int> {
        val limits = PlanLimits.getLimits(plan)
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000L

        cleanupOldTimestamps(oneMinuteAgo)

        val count = requestTimestamps.size
        _currentCount.value = count

        return Pair(count, limits.burstRequestsPerMinute)
    }

    /**
     * Remove timestamps older than the cutoff
     */
    private fun cleanupOldTimestamps(cutoffTime: Long) {
        while (requestTimestamps.peek()?.let { it < cutoffTime } == true) {
            requestTimestamps.poll()
        }
        _currentCount.value = requestTimestamps.size
    }

    /**
     * Reset the rate limiter (for testing or manual reset)
     */
    fun reset() {
        requestTimestamps.clear()
        _currentCount.value = 0
        _isLimited.value = false
        _cooldownSeconds.value = 0
    }

    /**
     * Get a human-readable status message
     */
    fun getStatusMessage(plan: SubscriptionPlan): String {
        val (count, limit) = getCurrentCount(plan)
        val (canMake, secondsUntil) = canMakeRequest(plan)

        return when {
            !canMake && secondsUntil > 0 -> {
                "Rate limited. Wait $secondsUntil seconds before sending another message."
            }
            count >= limit * 0.8 -> {
                "Approaching rate limit: $count/$limit requests this minute"
            }
            else -> {
                "$count/$limit requests this minute"
            }
        }
    }

    companion object {
        private var instance: RateLimitManager? = null

        /**
         * Get singleton instance
         */
        fun getInstance(): RateLimitManager {
            return instance ?: synchronized(this) {
                instance ?: RateLimitManager().also { instance = it }
            }
        }
    }
}
