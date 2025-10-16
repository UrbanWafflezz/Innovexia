package com.example.innovexia.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.ratelimit.FirebaseRateLimiter
import com.example.innovexia.data.models.*
import com.example.innovexia.data.repository.SubscriptionRepository
import com.example.innovexia.data.repository.UsageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing subscription and usage state
 */
class SubscriptionViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val usageRepository: UsageRepository,
    private val firebaseRateLimiter: FirebaseRateLimiter = FirebaseRateLimiter(
        FirebaseFirestore.getInstance(),
        FirebaseAuth.getInstance()
    )
) : ViewModel() {

    // ==================== Subscription State ====================

    private val _subscription = MutableStateFlow<UserSubscription>(UserSubscription.default())
    val subscription: StateFlow<UserSubscription> = _subscription.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ==================== Usage State ====================

    private val _currentUsage = MutableStateFlow<UsageData?>(null)
    val currentUsage: StateFlow<UsageData?> = _currentUsage.asStateFlow()

    private val _todayUsage = MutableStateFlow<DailyUsage?>(null)
    val todayUsage: StateFlow<DailyUsage?> = _todayUsage.asStateFlow()

    private val _usageSnapshot = MutableStateFlow<UsageSnapshot?>(null)
    val usageSnapshot: StateFlow<UsageSnapshot?> = _usageSnapshot.asStateFlow()

    // ==================== Computed State ====================

    /**
     * Current plan limits
     */
    val planLimits: StateFlow<PlanLimits> = subscription.map { it.getLimits() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlanLimits.getLimits(SubscriptionPlan.FREE))

    /**
     * Usage percentage (0-100)
     */
    val usagePercent: StateFlow<Float> = combine(currentUsage, planLimits) { usage, limits ->
        if (usage == null || limits.tokensPerWindow == 0L) return@combine 0f
        (usage.totalTokens.toFloat() / limits.tokensPerWindow.toFloat() * 100f).coerceIn(0f, 100f)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0f)

    /**
     * Is approaching limit (>= 90%)
     */
    val isApproachingLimit: StateFlow<Boolean> = usagePercent.map { it >= 90f }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Has exceeded limit
     */
    val hasExceededLimit: StateFlow<Boolean> = combine(currentUsage, planLimits) { usage, limits ->
        usage != null && usage.totalTokens >= limits.tokensPerWindow
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ==================== Rate Limit State ====================

    /**
     * Current burst rate limit count (from Firebase)
     */
    val burstCount: StateFlow<Int> = firebaseRateLimiter.currentCount

    /**
     * Is rate limited (from Firebase)
     */
    val isRateLimited: StateFlow<Boolean> = firebaseRateLimiter.isLimited

    /**
     * Cooldown seconds until next request allowed
     */
    val rateLimitCooldown: StateFlow<Int> = firebaseRateLimiter.cooldownSeconds

    /**
     * Rate limit status message
     */
    val rateLimitMessage: StateFlow<String> = combine(subscription, burstCount, isRateLimited, rateLimitCooldown) { sub, count, limited, cooldown ->
        firebaseRateLimiter.getStatusMessage(sub.plan, count)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init {
        loadSubscriptionAndUsage()

        // Listen to subscription changes and reset rate limiter when auth state changes
        viewModelScope.launch {
            var previousUserId: String? = null
            var isFirstEmission = true

            subscription.collect { sub ->
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

                // Skip first emission to establish baseline auth state
                if (isFirstEmission) {
                    previousUserId = currentUserId
                    isFirstEmission = false
                    android.util.Log.d("SubscriptionViewModel", "Initial auth state captured: $currentUserId")
                    return@collect
                }

                // Detect auth state change (login or logout)
                if (previousUserId != currentUserId) {
                    // Auth state changed - reset rate limiter
                    firebaseRateLimiter.resetOnAuthChange()
                    android.util.Log.d("SubscriptionViewModel", "Auth state changed. Resetting rate limiter. Previous: $previousUserId, Current: $currentUserId")
                }
                previousUserId = currentUserId
            }
        }

        // Periodic refresh of rate limit count (every 5 seconds)
        viewModelScope.launch {
            while (true) {
                try {
                    val plan = _subscription.value.plan
                    firebaseRateLimiter.getCurrentCount(plan)
                } catch (e: Exception) {
                    // Silent fail - will use local fallback
                }
                kotlinx.coroutines.delay(5000) // 5 seconds
            }
        }
    }

    /**
     * Load subscription and usage data
     */
    private fun loadSubscriptionAndUsage() {
        viewModelScope.launch {
            _isLoading.value = true

            // Load subscription
            subscriptionRepository.getSubscriptionFlow()
                .catch { e ->
                    _error.value = "Failed to load subscription: ${e.message}"
                }
                .collect { sub ->
                    _subscription.value = sub
                }
        }

        viewModelScope.launch {
            // Load current month usage
            usageRepository.getCurrentUsageFlow()
                .catch { e ->
                    _error.value = "Failed to load usage: ${e.message}"
                }
                .collect { usage ->
                    _currentUsage.value = usage
                    _isLoading.value = false
                }
        }

        viewModelScope.launch {
            // Load today's usage
            usageRepository.getTodayUsageFlow()
                .catch { e ->
                    _error.value = "Failed to load daily usage: ${e.message}"
                }
                .collect { daily ->
                    _todayUsage.value = daily
                }
        }
    }

    /**
     * Refresh subscription and usage from server
     */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sub = subscriptionRepository.getSubscription()
                _subscription.value = sub

                val usage = usageRepository.getCurrentUsage()
                _currentUsage.value = usage

                val daily = usageRepository.getTodayUsage()
                _todayUsage.value = daily

                // Refresh rate limit count
                firebaseRateLimiter.getCurrentCount(sub.plan)

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to refresh: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update usage from server snapshot
     * Called after each model generation
     */
    fun updateUsageSnapshot(snapshot: UsageSnapshot) {
        viewModelScope.launch {
            _usageSnapshot.value = snapshot
            usageRepository.updateFromSnapshot(snapshot)
        }
    }

    /**
     * Track local usage (fallback if server doesn't return snapshot)
     */
    fun trackUsage(tokensIn: Long, tokensOut: Long, attachmentsBytes: Long = 0L) {
        viewModelScope.launch {
            usageRepository.trackUsage(tokensIn, tokensOut, attachmentsBytes)
        }
    }

    // ==================== Rate Limit Methods ====================

    /**
     * Check if a request can be made (rate limit check)
     * @return Pair<Boolean, String> - (canMakeRequest, errorMessage)
     */
    suspend fun canMakeRequest(): Pair<Boolean, String> {
        val plan = _subscription.value.plan
        val (canMake, secondsUntil) = firebaseRateLimiter.canMakeRequest(plan)

        return if (canMake) {
            Pair(true, "")
        } else {
            val message = "Rate limit exceeded. Wait $secondsUntil second${if (secondsUntil != 1) "s" else ""} before sending another message."
            Pair(false, message)
        }
    }

    /**
     * Record that a request was made
     */
    suspend fun recordRequest() {
        firebaseRateLimiter.recordRequest()
    }

    /**
     * Reset rate limiter (for testing)
     */
    suspend fun resetRateLimiter() {
        firebaseRateLimiter.reset()
    }

    /**
     * Upgrade to a plan (creates Stripe checkout session)
     */
    fun upgradeToPlan(plan: SubscriptionPlan, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val checkoutUrl = subscriptionRepository.createCheckoutSession(plan)
                _error.value = null
                onSuccess(checkoutUrl)
            } catch (e: Exception) {
                _error.value = "Failed to start checkout: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Open billing portal (Stripe customer portal)
     */
    fun openBillingPortal(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val portalUrl = subscriptionRepository.createPortalSession()
                _error.value = null
                onSuccess(portalUrl)
            } catch (e: Exception) {
                _error.value = "Failed to open portal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear all data (on logout)
     */
    fun clear() {
        viewModelScope.launch {
            subscriptionRepository.clear()
            usageRepository.clear()
            _subscription.value = UserSubscription.default()
            _currentUsage.value = null
            _todayUsage.value = null
            _usageSnapshot.value = null
            _error.value = null

            // Reset rate limiter to guest defaults
            firebaseRateLimiter.resetOnAuthChange()
        }
    }
}
