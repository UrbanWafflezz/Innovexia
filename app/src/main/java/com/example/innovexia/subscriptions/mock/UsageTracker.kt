package com.example.innovexia.subscriptions.mock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Usage tracker with 5-hour reset windows
 * Tracks messages and tokens, resets automatically when window expires
 * LOCAL-ONLY VERSION - Use FirebaseUsageTracker for production
 */
class UsageTracker(private val context: Context) : UsageTrackerInterface {

    companion object {
        private val Context.usageDataStore: DataStore<Preferences> by preferencesDataStore(
            name = "usage_tracker"
        )

        private val WINDOW_START_TIME = longPreferencesKey("window_start_time")
        private val MESSAGE_COUNT = intPreferencesKey("message_count")
        private val TOKENS_IN = longPreferencesKey("tokens_in")
        private val TOKENS_OUT = longPreferencesKey("tokens_out")
    }

    /**
     * Flow of current usage window
     */
    override val usageWindowFlow: Flow<UsageWindow> = context.usageDataStore.data.map { prefs ->
        val windowStart = prefs[WINDOW_START_TIME] ?: 0L
        val messageCount = prefs[MESSAGE_COUNT] ?: 0
        val tokensIn = prefs[TOKENS_IN] ?: 0L
        val tokensOut = prefs[TOKENS_OUT] ?: 0L

        // Check if window has expired
        if (windowStart == 0L) {
            // No window yet, return empty
            UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)
        } else {
            val window = UsageWindow.fromData(windowStart, messageCount, tokensIn, tokensOut)
            if (window.hasExpired()) {
                // Window expired, return fresh window
                UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)
            } else {
                window
            }
        }
    }

    /**
     * Get current usage window (suspend)
     */
    override suspend fun getCurrentWindow(): UsageWindow {
        return usageWindowFlow.first()
    }

    /**
     * Track a message with token usage
     * Automatically starts a new window if needed or resets if expired
     */
    override suspend fun trackMessage(tokensIn: Long, tokensOut: Long) {
        context.usageDataStore.edit { prefs ->
            val currentWindowStart = prefs[WINDOW_START_TIME] ?: 0L
            val currentMessageCount = prefs[MESSAGE_COUNT] ?: 0
            val currentTokensIn = prefs[TOKENS_IN] ?: 0L
            val currentTokensOut = prefs[TOKENS_OUT] ?: 0L

            // Check if we need to start a new window
            val now = System.currentTimeMillis()
            val shouldReset = if (currentWindowStart == 0L) {
                // No window yet
                true
            } else {
                // Check if window expired
                val elapsed = now - currentWindowStart
                elapsed >= UsageLimits.WINDOW_DURATION_MS
            }

            if (shouldReset) {
                // Start new window
                prefs[WINDOW_START_TIME] = now
                prefs[MESSAGE_COUNT] = 1
                prefs[TOKENS_IN] = tokensIn
                prefs[TOKENS_OUT] = tokensOut
            } else {
                // Increment existing window
                prefs[MESSAGE_COUNT] = currentMessageCount + 1
                prefs[TOKENS_IN] = currentTokensIn + tokensIn
                prefs[TOKENS_OUT] = currentTokensOut + tokensOut
            }
        }
    }

    /**
     * Update token counts for current message WITHOUT incrementing message count
     * Used when actual token counts arrive from API after message was already tracked
     */
    override suspend fun updateTokens(tokensIn: Long, tokensOut: Long) {
        context.usageDataStore.edit { prefs ->
            val currentTokensIn = prefs[TOKENS_IN] ?: 0L
            val currentTokensOut = prefs[TOKENS_OUT] ?: 0L

            // Add to existing token counts
            prefs[TOKENS_IN] = currentTokensIn + tokensIn
            prefs[TOKENS_OUT] = currentTokensOut + tokensOut
        }
    }

    /**
     * Check if user can send a message (hasn't hit limits)
     */
    override suspend fun canSendMessage(plan: PlanId): Pair<Boolean, String?> {
        val window = getCurrentWindow()
        val limits = UsageLimits.forPlan(plan)

        // Check if window expired (should reset)
        if (window.hasExpired() && window.messageCount > 0) {
            // Window expired, allow message (will reset on track)
            return Pair(true, null)
        }

        // Check message limit
        if (window.messageCount >= limits.messagesPerWindow) {
            val resetTime = window.timeUntilReset()
            return Pair(false, "Message limit reached. Resets in $resetTime")
        }

        // Check token limit
        if (window.tokensUsed >= limits.tokensPerWindow) {
            val resetTime = window.timeUntilReset()
            return Pair(false, "Token limit reached. Resets in $resetTime")
        }

        // Check if approaching limit
        if (window.isApproachingLimit(limits)) {
            val resetTime = window.timeUntilReset()
            return Pair(true, "Approaching limit. Resets in $resetTime")
        }

        return Pair(true, null)
    }

    /**
     * Get usage status message
     */
    override suspend fun getStatusMessage(plan: PlanId): String {
        val window = getCurrentWindow()
        val limits = UsageLimits.forPlan(plan)

        if (window.messageCount == 0) {
            return "No messages sent this window"
        }

        val resetTime = window.timeUntilReset()
        val messagesLeft = (limits.messagesPerWindow - window.messageCount).coerceAtLeast(0)
        val tokensLeft = (limits.tokensPerWindow - window.tokensUsed).coerceAtLeast(0L)

        return "$messagesLeft messages, ${formatTokens(tokensLeft)} tokens left. Resets in $resetTime"
    }

    /**
     * Reset usage (for testing or manual reset)
     */
    override suspend fun reset() {
        context.usageDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    /**
     * Clear all data (on logout)
     */
    override suspend fun clear() {
        context.usageDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    /**
     * Format tokens for display
     */
    private fun formatTokens(tokens: Long): String {
        return when {
            tokens >= 1_000_000 -> String.format("%.1fM", tokens / 1_000_000.0)
            tokens >= 1_000 -> String.format("%.1fK", tokens / 1_000.0)
            else -> tokens.toString()
        }
    }
}
