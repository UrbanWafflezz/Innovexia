package com.example.innovexia.subscriptions.mock

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firebase-backed usage tracker with 5-hour reset windows
 * - Primary: Firestore /users/{uid}/usage/current
 * - Fallback: Local DataStore for offline support
 * - Prevents bypass via app data deletion or reinstall
 * - Syncs across all devices logged into same account
 */
class FirebaseUsageTracker(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UsageTrackerInterface {

    companion object {
        private val Context.usageDataStore: DataStore<Preferences> by preferencesDataStore(
            name = "usage_tracker_local"
        )

        private val WINDOW_START_TIME = longPreferencesKey("window_start_time")
        private val MESSAGE_COUNT = intPreferencesKey("message_count")
        private val TOKENS_IN = longPreferencesKey("tokens_in")
        private val TOKENS_OUT = longPreferencesKey("tokens_out")
    }

    /**
     * Flow of current usage window
     * Listens to real-time Firebase updates for cross-device sync
     * Falls back to local DataStore when not authenticated
     */
    override val usageWindowFlow: Flow<UsageWindow> = flow {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Listen to real-time Firebase updates
            try {
                val usageRef = firestore.collection("users").document(userId)
                    .collection("usage").document("current")

                // Convert Firestore snapshot listener to Flow
                callbackFlow {
                    val listenerRegistration = usageRef.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("FirebaseUsageTracker", "Error listening to Firebase updates", error)
                            close(error)
                            return@addSnapshotListener
                        }

                        val window = if (snapshot != null && snapshot.exists()) {
                            val data = snapshot.data ?: emptyMap()
                            val windowStart = (data["windowStartTime"] as? Timestamp)?.toDate()?.time ?: 0L
                            val messageCount = (data["messageCount"] as? Long)?.toInt() ?: 0
                            val tokensIn = data["tokensIn"] as? Long ?: 0L
                            val tokensOut = data["tokensOut"] as? Long ?: 0L

                            val window = UsageWindow.fromData(windowStart, messageCount, tokensIn, tokensOut)

                            // Check if window expired
                            if (window.hasExpired()) {
                                UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)
                            } else {
                                window
                            }
                        } else {
                            // No data yet
                            UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)
                        }

                        // Update local cache
                        launch {
                            context.usageDataStore.edit { prefs ->
                                prefs[WINDOW_START_TIME] = window.windowStartTime
                                prefs[MESSAGE_COUNT] = window.messageCount
                                prefs[TOKENS_IN] = window.tokensIn
                                prefs[TOKENS_OUT] = window.tokensOut
                            }
                        }

                        trySend(window)
                    }

                    awaitClose {
                        listenerRegistration.remove()
                    }
                }.collect { emit(it) }
            } catch (e: Exception) {
                Log.e("FirebaseUsageTracker", "Error setting up Firebase listener, using local", e)
                // Fall back to local DataStore
                context.usageDataStore.data.map { prefs ->
                    getLocalUsageWindow(prefs)
                }.collect { emit(it) }
            }
        } else {
            // Not authenticated - use local DataStore
            context.usageDataStore.data.map { prefs ->
                getLocalUsageWindow(prefs)
            }.collect { emit(it) }
        }
    }

    /**
     * Get current usage window (suspend)
     */
    override suspend fun getCurrentWindow(): UsageWindow {
        val userId = auth.currentUser?.uid

        return if (userId != null) {
            try {
                // Fetch from Firebase as source of truth
                fetchUsageWindowFromFirebase(userId)
            } catch (e: Exception) {
                Log.e("FirebaseUsageTracker", "Error fetching from Firebase, using local", e)
                // Fallback to local
                usageWindowFlow.first()
            }
        } else {
            // Not authenticated - use local only
            usageWindowFlow.first()
        }
    }

    /**
     * Track a message with token usage
     * Automatically starts a new window if needed or resets if expired
     * Updates both Firebase (source of truth) and local (fallback)
     */
    override suspend fun trackMessage(tokensIn: Long, tokensOut: Long) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            // Not authenticated - track locally only
            trackMessageLocal(tokensIn, tokensOut)
            Log.w("FirebaseUsageTracker", "No user ID - tracking locally only")
            return
        }

        try {
            // Track in Firebase (source of truth)
            trackMessageFirebase(userId, tokensIn, tokensOut)

            // Also update local for offline fallback
            trackMessageLocal(tokensIn, tokensOut)
        } catch (e: Exception) {
            Log.e("FirebaseUsageTracker", "Error tracking message in Firebase", e)
            // Fallback to local tracking
            trackMessageLocal(tokensIn, tokensOut)
        }
    }

    /**
     * Update token counts for current message WITHOUT incrementing message count
     * Used when actual token counts arrive from API after message was already tracked
     */
    override suspend fun updateTokens(tokensIn: Long, tokensOut: Long) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            // Not authenticated - update locally only
            updateTokensLocal(tokensIn, tokensOut)
            Log.w("FirebaseUsageTracker", "No user ID - updating tokens locally only")
            return
        }

        try {
            // Update in Firebase (source of truth)
            updateTokensFirebase(userId, tokensIn, tokensOut)

            // Also update local for offline fallback
            updateTokensLocal(tokensIn, tokensOut)
        } catch (e: Exception) {
            Log.e("FirebaseUsageTracker", "Error updating tokens in Firebase", e)
            // Fallback to local update
            updateTokensLocal(tokensIn, tokensOut)
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
        val userId = auth.currentUser?.uid

        // Clear local
        context.usageDataStore.edit { prefs ->
            prefs.clear()
        }

        // Clear Firebase if authenticated
        if (userId != null) {
            try {
                firestore.collection("users").document(userId)
                    .collection("usage").document("current")
                    .delete()
                    .await()
            } catch (e: Exception) {
                Log.e("FirebaseUsageTracker", "Error resetting Firebase usage", e)
            }
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
     * Reset usage tracker on auth state change (login/logout)
     * Clears local DataStore to ensure fresh state for the new auth context
     * Call this when user logs in or logs out to prevent stale data from affecting new session
     */
    suspend fun resetOnAuthChange() {
        // Clear local DataStore to remove any stale data
        context.usageDataStore.edit { prefs ->
            prefs.clear()
        }
        Log.d("FirebaseUsageTracker", "Usage tracker reset on auth state change")
    }

    // ========== PRIVATE HELPERS ==========

    /**
     * Fetch usage window from Firebase
     */
    private suspend fun fetchUsageWindowFromFirebase(userId: String): UsageWindow {
        val usageRef = firestore.collection("users").document(userId)
            .collection("usage").document("current")

        val snapshot = usageRef.get().await()

        if (!snapshot.exists()) {
            // No usage data yet - return empty window
            return UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)
        }

        val data = snapshot.data ?: return UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)

        val windowStart = (data["windowStartTime"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
        val messageCount = (data["messageCount"] as? Long)?.toInt() ?: 0
        val tokensIn = data["tokensIn"] as? Long ?: 0L
        val tokensOut = data["tokensOut"] as? Long ?: 0L

        val window = UsageWindow.fromData(windowStart, messageCount, tokensIn, tokensOut)

        // Check if window expired
        if (window.hasExpired()) {
            // Window expired, return fresh window
            return UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)
        }

        return window
    }

    /**
     * Get usage window from local DataStore
     */
    private fun getLocalUsageWindow(prefs: Preferences): UsageWindow {
        val windowStart = prefs[WINDOW_START_TIME] ?: 0L
        val messageCount = prefs[MESSAGE_COUNT] ?: 0
        val tokensIn = prefs[TOKENS_IN] ?: 0L
        val tokensOut = prefs[TOKENS_OUT] ?: 0L

        // Check if window has expired
        if (windowStart == 0L) {
            // No window yet, return empty
            return UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)
        } else {
            val window = UsageWindow.fromData(windowStart, messageCount, tokensIn, tokensOut)
            if (window.hasExpired()) {
                // Window expired, return fresh window
                return UsageWindow.create().copy(messageCount = 0, tokensUsed = 0L, tokensIn = 0L, tokensOut = 0L)
            } else {
                return window
            }
        }
    }

    /**
     * Track message in Firebase
     */
    private suspend fun trackMessageFirebase(userId: String, tokensIn: Long, tokensOut: Long) {
        val now = Timestamp.now()
        val usageRef = firestore.collection("users").document(userId)
            .collection("usage").document("current")

        Log.d("FirebaseUsageTracker", "Tracking message for user: $userId")

        val snapshot = usageRef.get().await()

        if (!snapshot.exists()) {
            // Create new usage window
            Log.d("FirebaseUsageTracker", "Creating new usage window")
            usageRef.set(
                mapOf(
                    "windowStartTime" to now,
                    "messageCount" to 1,
                    "tokensIn" to tokensIn,
                    "tokensOut" to tokensOut
                )
            ).await()
            Log.d("FirebaseUsageTracker", "Usage window created. Count: 1")
        } else {
            val data = snapshot.data ?: emptyMap()
            val windowStart = (data["windowStartTime"] as? Timestamp)?.seconds ?: 0L
            val inSameWindow = (now.seconds - windowStart) < UsageLimits.WINDOW_DURATION_MS / 1000

            if (inSameWindow) {
                // Increment in same window
                Log.d("FirebaseUsageTracker", "Incrementing count in same window")
                usageRef.update(
                    mapOf(
                        "messageCount" to FieldValue.increment(1),
                        "tokensIn" to FieldValue.increment(tokensIn),
                        "tokensOut" to FieldValue.increment(tokensOut)
                    )
                ).await()
                Log.d("FirebaseUsageTracker", "Usage incremented")
            } else {
                // New window - reset
                Log.d("FirebaseUsageTracker", "New window - resetting usage")
                usageRef.set(
                    mapOf(
                        "windowStartTime" to now,
                        "messageCount" to 1,
                        "tokensIn" to tokensIn,
                        "tokensOut" to tokensOut
                    )
                ).await()
                Log.d("FirebaseUsageTracker", "New window created. Count: 1")
            }
        }
    }

    /**
     * Track message locally
     */
    private suspend fun trackMessageLocal(tokensIn: Long, tokensOut: Long) {
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
     * Update tokens in Firebase
     */
    private suspend fun updateTokensFirebase(userId: String, tokensIn: Long, tokensOut: Long) {
        val usageRef = firestore.collection("users").document(userId)
            .collection("usage").document("current")

        Log.d("FirebaseUsageTracker", "Updating tokens for user: $userId - tokensIn=$tokensIn, tokensOut=$tokensOut")

        val snapshot = usageRef.get().await()

        if (snapshot.exists()) {
            // Update existing document
            usageRef.update(
                mapOf(
                    "tokensIn" to FieldValue.increment(tokensIn),
                    "tokensOut" to FieldValue.increment(tokensOut)
                )
            ).await()
            Log.d("FirebaseUsageTracker", "Tokens updated in Firebase")
        } else {
            // No usage document yet - this shouldn't happen, but handle gracefully
            Log.w("FirebaseUsageTracker", "No usage document found for token update")
        }
    }

    /**
     * Update tokens locally
     */
    private suspend fun updateTokensLocal(tokensIn: Long, tokensOut: Long) {
        context.usageDataStore.edit { prefs ->
            val currentTokensIn = prefs[TOKENS_IN] ?: 0L
            val currentTokensOut = prefs[TOKENS_OUT] ?: 0L

            // Add to existing token counts
            prefs[TOKENS_IN] = currentTokensIn + tokensIn
            prefs[TOKENS_OUT] = currentTokensOut + tokensOut
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
