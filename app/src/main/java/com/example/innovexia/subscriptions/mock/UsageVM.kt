package com.example.innovexia.subscriptions.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for usage tracking and limits
 * Combines entitlements with usage tracking
 * Supports both local-only and Firebase-backed trackers
 */
class UsageVM(
    private val usageTracker: UsageTrackerInterface,
    private val entitlementsVM: EntitlementsVM,
    private val usageDataRepository: UsageDataRepository? = null
) : ViewModel() {

    /**
     * Current usage window
     */
    val usageWindow: StateFlow<UsageWindow> = usageTracker.usageWindowFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)
        )

    /**
     * Current plan limits
     */
    val usageLimits: StateFlow<UsageLimits> = entitlementsVM.entitlement
        .map { entitlement ->
            UsageLimits.forPlan(entitlement.planId())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UsageLimits.forPlan(PlanId.FREE)
        )

    /**
     * Combined usage state for UI
     */
    val usageState: StateFlow<UsageState> = if (usageDataRepository != null) {
        // With real usage data
        combine(
            usageWindow,
            usageLimits,
            entitlementsVM.entitlement,
            usageDataRepository.observeUsageData()
        ) { window, limits, entitlement, usageData ->
            UsageState(
                window = window,
                limits = limits,
                plan = entitlement.planId(),
                messagesUsed = window.messageCount,
                messagesLimit = limits.messagesPerWindow,
                tokensUsed = window.tokensUsed,
                tokensLimit = limits.tokensPerWindow,
                tokensIn = window.tokensIn,
                tokensOut = window.tokensOut,
                timeUntilReset = window.timeUntilReset(),
                progressPercent = window.progressPercent(limits),
                isLimitReached = window.isLimitReached(limits),
                isApproachingLimit = window.isApproachingLimit(limits),
                // Real counts from local databases
                memoryEntriesCount = usageData.memoryEntriesCount,
                sourcesCount = usageData.sourcesCount
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UsageState.empty()
        )
    } else {
        // Without real usage data (fallback)
        combine(
            usageWindow,
            usageLimits,
            entitlementsVM.entitlement
        ) { window, limits, entitlement ->
            UsageState(
                window = window,
                limits = limits,
                plan = entitlement.planId(),
                messagesUsed = window.messageCount,
                messagesLimit = limits.messagesPerWindow,
                tokensUsed = window.tokensUsed,
                tokensLimit = limits.tokensPerWindow,
                tokensIn = window.tokensIn,
                tokensOut = window.tokensOut,
                timeUntilReset = window.timeUntilReset(),
                progressPercent = window.progressPercent(limits),
                isLimitReached = window.isLimitReached(limits),
                isApproachingLimit = window.isApproachingLimit(limits)
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UsageState.empty()
        )
    }

    /**
     * Track a message with token usage
     */
    fun trackMessage(tokensIn: Long, tokensOut: Long) {
        viewModelScope.launch {
            usageTracker.trackMessage(tokensIn, tokensOut)
        }
    }

    /**
     * Update token counts without incrementing message count
     */
    fun updateTokens(tokensIn: Long, tokensOut: Long) {
        viewModelScope.launch {
            usageTracker.updateTokens(tokensIn, tokensOut)
        }
    }

    /**
     * Check if user can send a message
     */
    suspend fun canSendMessage(): Pair<Boolean, String?> {
        val plan = entitlementsVM.entitlement.value.planId()
        return usageTracker.canSendMessage(plan)
    }

    /**
     * Get status message
     */
    suspend fun getStatusMessage(): String {
        val plan = entitlementsVM.entitlement.value.planId()
        return usageTracker.getStatusMessage(plan)
    }

    /**
     * Reset usage (for testing)
     */
    fun resetUsage() {
        viewModelScope.launch {
            usageTracker.reset()
        }
    }

    /**
     * Clear all data (on logout)
     */
    fun clear() {
        viewModelScope.launch {
            usageTracker.clear()
        }
    }
}

/**
 * Complete usage state for UI
 */
data class UsageState(
    val window: UsageWindow,
    val limits: UsageLimits,
    val plan: PlanId,
    val messagesUsed: Int,
    val messagesLimit: Int,
    val tokensUsed: Long,
    val tokensLimit: Long,
    val tokensIn: Long,
    val tokensOut: Long,
    val timeUntilReset: String,
    val progressPercent: Float,
    val isLimitReached: Boolean,
    val isApproachingLimit: Boolean,
    // Plan limits
    val memoryLimit: Int? = limits.memoryEntryLimit,
    val sourcesLimit: Int = limits.maxSources,
    val uploadLimitMB: Int = limits.maxUploadMB,
    // Real usage counts from local databases
    val memoryEntriesCount: Int = 0,
    val sourcesCount: Int = 0
) {
    /**
     * Messages remaining
     */
    val messagesRemaining: Int
        get() = (messagesLimit - messagesUsed).coerceAtLeast(0)

    /**
     * Tokens remaining
     */
    val tokensRemaining: Long
        get() = (tokensLimit - tokensUsed).coerceAtLeast(0L)

    /**
     * Format tokens for display
     */
    fun formatTokensUsed(): String = formatTokens(tokensUsed)
    fun formatTokensLimit(): String = formatTokens(tokensLimit)
    fun formatTokensRemaining(): String = formatTokens(tokensRemaining)
    fun formatTokensIn(): String = formatTokens(tokensIn)
    fun formatTokensOut(): String = formatTokens(tokensOut)

    /**
     * Get usage summary text
     */
    fun getSummaryText(): String {
        return "$messagesUsed/$messagesLimit messages • ${formatTokensUsed()}/${formatTokensLimit()} tokens"
    }

    /**
     * Get reset info text
     */
    fun getResetText(): String {
        return "Resets in $timeUntilReset"
    }

    /**
     * Get warning message if applicable
     */
    fun getWarningMessage(): String? {
        return when {
            isLimitReached -> "Usage limit reached. Resets in $timeUntilReset"
            isApproachingLimit -> "Approaching usage limit. Resets in $timeUntilReset"
            else -> null
        }
    }

    companion object {
        /**
         * Empty state
         */
        fun empty(): UsageState {
            val limits = UsageLimits.forPlan(PlanId.FREE)
            val window = UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)

            return UsageState(
                window = window,
                limits = limits,
                plan = PlanId.FREE,
                messagesUsed = 0,
                messagesLimit = limits.messagesPerWindow,
                tokensUsed = 0L,
                tokensLimit = limits.tokensPerWindow,
                tokensIn = 0L,
                tokensOut = 0L,
                timeUntilReset = "5h 0m",
                progressPercent = 0f,
                isLimitReached = false,
                isApproachingLimit = false
            )
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
}
