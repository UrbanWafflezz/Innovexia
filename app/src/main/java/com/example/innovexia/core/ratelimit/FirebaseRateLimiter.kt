package com.example.innovexia.core.ratelimit

import android.util.Log
import com.example.innovexia.data.models.PlanLimits
import com.example.innovexia.data.models.RateLimitData
import com.example.innovexia.data.models.SubscriptionPlan
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.ceil

/**
 * Firebase-backed rate limiter with local fallback
 * - Primary: Firestore /users/{uid}/rate/now
 * - Fallback: Local RateLimitManager for offline support
 * - Separate tracking for guest vs logged-in users
 */
class FirebaseRateLimiter(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Separate rate limiters for logged-in users and guest users
    private val loggedInLimiter = RateLimitManager()
    private val guestLimiter = RateLimitManager()

    private val _currentCount = MutableStateFlow(0)
    val currentCount: StateFlow<Int> = _currentCount.asStateFlow()

    private val _isLimited = MutableStateFlow(false)
    val isLimited: StateFlow<Boolean> = _isLimited.asStateFlow()

    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds: StateFlow<Int> = _cooldownSeconds.asStateFlow()

    /**
     * Get the appropriate rate limiter for the current user state
     */
    private fun getLocalLimiter(): RateLimitManager {
        return if (auth.currentUser != null) {
            loggedInLimiter
        } else {
            guestLimiter
        }
    }

    /**
     * Check if a request can be made
     * Uses Firebase as source of truth, falls back to local
     */
    suspend fun canMakeRequest(plan: SubscriptionPlan): Pair<Boolean, Int> {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            // Not authenticated - use guest limiter only
            return guestLimiter.canMakeRequest(plan)
        }

        return try {
            // Try Firebase first
            canMakeRequestFirebase(userId, plan)
        } catch (e: Exception) {
            // Fallback to logged-in limiter on error
            loggedInLimiter.canMakeRequest(plan)
        }
    }

    /**
     * Check rate limit from Firebase
     */
    private suspend fun canMakeRequestFirebase(userId: String, plan: SubscriptionPlan): Pair<Boolean, Int> {
        val limits = PlanLimits.getLimits(plan)
        val now = Timestamp.now()
        val rateRef = firestore.collection("users").document(userId)
            .collection("rate").document("now")

        val snapshot = rateRef.get().await()

        if (!snapshot.exists()) {
            // No rate data yet - allowed
            _currentCount.value = 0
            _isLimited.value = false
            _cooldownSeconds.value = 0
            return Pair(true, 0)
        }

        val rateData = RateLimitData.fromMap(snapshot.data ?: emptyMap())
        val windowStart = rateData.minuteWindowStart.seconds
        val currentCount = rateData.requestsThisMinute

        // Check if still in same window
        val inSameWindow = (now.seconds - windowStart) < 60

        if (inSameWindow && currentCount >= limits.burstRequestsPerMinute) {
            // Rate limited
            val secondsUntilExpiry = ceil((windowStart + 60 - now.seconds).toDouble()).toInt()
            _currentCount.value = currentCount
            _isLimited.value = true
            _cooldownSeconds.value = secondsUntilExpiry.coerceAtLeast(1)
            return Pair(false, secondsUntilExpiry)
        }

        // Allowed
        _currentCount.value = if (inSameWindow) currentCount else 0
        _isLimited.value = false
        _cooldownSeconds.value = 0
        return Pair(true, 0)
    }

    /**
     * Record a request in Firebase (and local backup)
     */
    suspend fun recordRequest() {
        val userId = auth.currentUser?.uid

        Log.d("FirebaseRateLimiter", "recordRequest() called. userId: $userId")

        // Always record locally for fallback - use appropriate limiter
        val limiter = getLocalLimiter()
        limiter.recordRequest()

        if (userId == null) {
            Log.d("FirebaseRateLimiter", "No user ID - using guest limiter only")
            return
        }

        try {
            recordRequestFirebase(userId)
        } catch (e: Exception) {
            // Silent fail - local limiter is the backup
            Log.e("FirebaseRateLimiter", "Error recording request to Firebase", e)
            e.printStackTrace()
        }
    }

    /**
     * Record request in Firebase
     */
    private suspend fun recordRequestFirebase(userId: String) {
        val now = Timestamp.now()
        val rateRef = firestore.collection("users").document(userId)
            .collection("rate").document("now")

        Log.d("FirebaseRateLimiter", "Recording request for user: $userId")

        val snapshot = rateRef.get().await()

        if (!snapshot.exists()) {
            // Create new rate limit document
            Log.d("FirebaseRateLimiter", "Creating new rate limit document")
            rateRef.set(
                mapOf(
                    "minuteWindowStart" to now,
                    "requestsThisMinute" to 1
                )
            ).await()
            _currentCount.value = 1
            Log.d("FirebaseRateLimiter", "Rate limit document created. Count: 1")
        } else {
            val rateData = RateLimitData.fromMap(snapshot.data ?: emptyMap())
            val windowStart = rateData.minuteWindowStart.seconds
            val inSameWindow = (now.seconds - windowStart) < 60

            if (inSameWindow) {
                // Increment in same window
                Log.d("FirebaseRateLimiter", "Incrementing count in same window")
                rateRef.update(
                    "requestsThisMinute", FieldValue.increment(1)
                ).await()
                _currentCount.value = rateData.requestsThisMinute + 1
                Log.d("FirebaseRateLimiter", "Count incremented to: ${_currentCount.value}")
            } else {
                // New window - reset
                Log.d("FirebaseRateLimiter", "New window - resetting count")
                rateRef.set(
                    mapOf(
                        "minuteWindowStart" to now,
                        "requestsThisMinute" to 1
                    )
                ).await()
                _currentCount.value = 1
                _isLimited.value = false
                _cooldownSeconds.value = 0
                Log.d("FirebaseRateLimiter", "New window created. Count: 1")
            }
        }
    }

    /**
     * Get current count and limit
     */
    suspend fun getCurrentCount(plan: SubscriptionPlan): Pair<Int, Int> {
        val userId = auth.currentUser?.uid
        val limits = PlanLimits.getLimits(plan)

        if (userId == null) {
            return guestLimiter.getCurrentCount(plan)
        }

        return try {
            val rateRef = firestore.collection("users").document(userId)
                .collection("rate").document("now")

            val snapshot = rateRef.get().await()

            if (!snapshot.exists()) {
                _currentCount.value = 0
                Pair(0, limits.burstRequestsPerMinute)
            } else {
                val rateData = RateLimitData.fromMap(snapshot.data ?: emptyMap())
                val now = Timestamp.now()
                val inSameWindow = (now.seconds - rateData.minuteWindowStart.seconds) < 60

                val count = if (inSameWindow) rateData.requestsThisMinute else 0
                _currentCount.value = count
                Pair(count, limits.burstRequestsPerMinute)
            }
        } catch (e: Exception) {
            loggedInLimiter.getCurrentCount(plan)
        }
    }

    /**
     * Reset rate limiter (for testing)
     */
    suspend fun reset() {
        // Reset the appropriate limiter based on auth state
        getLocalLimiter().reset()

        val userId = auth.currentUser?.uid ?: return

        try {
            firestore.collection("users").document(userId)
                .collection("rate").document("now")
                .delete()
                .await()

            _currentCount.value = 0
            _isLimited.value = false
            _cooldownSeconds.value = 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Get status message
     */
    fun getStatusMessage(plan: SubscriptionPlan, count: Int): String {
        val limits = PlanLimits.getLimits(plan)
        val (_, secondsUntil) = if (_isLimited.value) {
            Pair(false, _cooldownSeconds.value)
        } else {
            Pair(true, 0)
        }

        return when {
            _isLimited.value && secondsUntil > 0 -> {
                "Rate limited. Wait $secondsUntil seconds before sending another message."
            }
            count >= limits.burstRequestsPerMinute * 0.8 -> {
                "Approaching rate limit: $count/${limits.burstRequestsPerMinute} requests this minute"
            }
            else -> {
                "$count/${limits.burstRequestsPerMinute} requests this minute"
            }
        }
    }

    /**
     * Reset rate limiter on auth state change (login/logout)
     * Call this when user logs in or logs out to ensure fresh rate limit state
     * Note: This does NOT reset the guest limiter when logging in, or the logged-in limiter when logging out
     */
    suspend fun resetOnAuthChange() {
        // Only reset the limiter for the NEW state (not the one we're leaving)
        // This ensures guest and logged-in rate limits remain independent
        getLocalLimiter().reset()

        // Reset Firebase state flows
        _currentCount.value = 0
        _isLimited.value = false
        _cooldownSeconds.value = 0

        Log.d("FirebaseRateLimiter", "Rate limiter reset on auth state change")
    }
}
