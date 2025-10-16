package com.example.innovexia.subscriptions.mock

import kotlinx.coroutines.flow.Flow

/**
 * Interface for usage tracking implementations
 * Allows switching between local-only and Firebase-backed tracking
 */
interface UsageTrackerInterface {
    /**
     * Flow of current usage window
     */
    val usageWindowFlow: Flow<UsageWindow>

    /**
     * Get current usage window (suspend)
     */
    suspend fun getCurrentWindow(): UsageWindow

    /**
     * Track a message with token usage
     * Automatically starts a new window if needed or resets if expired
     */
    suspend fun trackMessage(tokensIn: Long, tokensOut: Long)

    /**
     * Update token counts for current message WITHOUT incrementing message count
     * Used when actual token counts arrive from API after message was already tracked
     */
    suspend fun updateTokens(tokensIn: Long, tokensOut: Long)

    /**
     * Check if user can send a message (hasn't hit limits)
     */
    suspend fun canSendMessage(plan: PlanId): Pair<Boolean, String?>

    /**
     * Get usage status message
     */
    suspend fun getStatusMessage(plan: PlanId): String

    /**
     * Reset usage (for testing or manual reset)
     */
    suspend fun reset()

    /**
     * Clear all data (on logout)
     */
    suspend fun clear()
}
