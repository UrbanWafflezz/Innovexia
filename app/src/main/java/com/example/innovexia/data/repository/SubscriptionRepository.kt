package com.example.innovexia.data.repository

import com.example.innovexia.data.local.dao.SubscriptionDao
import com.example.innovexia.data.local.entities.SubscriptionEntity
import com.example.innovexia.data.models.SubscriptionPlan
import com.example.innovexia.data.models.UserSubscription
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing user subscriptions
 * Syncs between Firestore and local Room database
 * Reactive to authentication state changes (guest vs authenticated user)
 */
class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Flow that emits whenever Firebase auth state changes
     * Emits userId or "guest"
     */
    private val authUserIdFlow: Flow<String> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val userId = auth.currentUser?.uid ?: "guest"
            trySend(userId)
        }
        auth.addAuthStateListener(listener)

        // Emit initial state
        trySend(auth.currentUser?.uid ?: "guest")

        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Get subscription for current user (Flow with real-time Firestore sync)
     * Reactive to auth state changes - switches immediately between guest and user subscriptions
     * For authenticated users, listens to Firestore in real-time to ensure correct tier on login
     */
    fun getSubscriptionFlow(): Flow<UserSubscription> {
        return authUserIdFlow.flatMapLatest { userId ->
            if (userId == "guest") {
                // Guest users always get FREE tier
                flowOf(UserSubscription.default())
            } else {
                // Authenticated users: listen to Firestore in real-time
                // This ensures immediate correct tier display when logging in
                callbackFlow {
                    val docRef = firestore.collection("users").document(userId)
                        .collection("subscription").document("current")

                    val listener: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("SubscriptionRepository", "Error listening to subscription: ${error.message}")
                            // On error, try local cache as fallback
                            scope.launch {
                                val local = subscriptionDao.getSubscription(userId)
                                trySend(local?.toUserSubscription() ?: UserSubscription.default())
                            }
                            return@addSnapshotListener
                        }

                        val subscription = if (snapshot?.exists() == true) {
                            val data = snapshot.data ?: emptyMap()
                            UserSubscription.fromMap(data)
                        } else {
                            UserSubscription.default()
                        }

                        // Cache locally for offline access
                        val entity = SubscriptionEntity.fromUserSubscription(userId, subscription)
                        scope.launch {
                            subscriptionDao.upsert(entity)
                        }

                        trySend(subscription)
                    }

                    awaitClose { listener.remove() }
                }
            }
        }
    }

    /**
     * Get subscription (one-time)
     */
    suspend fun getSubscription(): UserSubscription {
        val userId = auth.currentUser?.uid ?: "guest"

        if (userId == "guest") {
            return UserSubscription.default()
        }

        // Try local first
        val local = subscriptionDao.getSubscription(userId)
        if (local != null && isFresh(local.lastSynced)) {
            return local.toUserSubscription()
        }

        // Fetch from Firestore
        return fetchFromFirestore(userId)
    }

    /**
     * Listen to Firestore subscription changes in real-time
     */
    fun listenToSubscription(): Flow<UserSubscription> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(UserSubscription.default())
            close()
            return@callbackFlow
        }

        val docRef = firestore.collection("users").document(userId)
            .collection("subscription").document("current")

        val listener: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val subscription = if (snapshot?.exists() == true) {
                val data = snapshot.data ?: emptyMap()
                UserSubscription.fromMap(data)
            } else {
                UserSubscription.default()
            }

            // Cache locally
            val entity = SubscriptionEntity.fromUserSubscription(userId, subscription)
            scope.launch {
                subscriptionDao.upsert(entity)
            }

            trySend(subscription)
        }

        awaitClose { listener.remove() }
    }

    /**
     * Fetch subscription from Firestore
     */
    private suspend fun fetchFromFirestore(userId: String): UserSubscription {
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("subscription").document("current")

            val snapshot = docRef.get().await()

            val subscription = if (snapshot.exists()) {
                val data = snapshot.data ?: emptyMap()
                UserSubscription.fromMap(data)
            } else {
                // Create default subscription if doesn't exist
                val default = UserSubscription.default()
                docRef.set(default.toMap()).await()
                default
            }

            // Cache locally
            val entity = SubscriptionEntity.fromUserSubscription(userId, subscription)
            subscriptionDao.upsert(entity)

            subscription
        } catch (e: Exception) {
            // Fallback to local or default
            subscriptionDao.getSubscription(userId)?.toUserSubscription()
                ?: UserSubscription.default()
        }
    }

    /**
     * Start syncing from Firestore (fire-and-forget)
     */
    private fun syncFromFirestore(userId: String) {
        scope.launch {
            fetchFromFirestore(userId)
        }
    }

    /**
     * Update subscription (typically called by webhooks/backend)
     */
    suspend fun updateSubscription(subscription: UserSubscription) {
        val userId = auth.currentUser?.uid ?: return

        // Update Firestore
        try {
            firestore.collection("users").document(userId)
                .collection("subscription").document("current")
                .set(subscription.toMap())
                .await()

            // Update local cache
            val entity = SubscriptionEntity.fromUserSubscription(userId, subscription)
            subscriptionDao.upsert(entity)
        } catch (e: Exception) {
            // Log error but don't throw - let local cache remain
            e.printStackTrace()
        }
    }

    /**
     * Upgrade/downgrade plan (initiates Stripe checkout)
     * Returns Stripe checkout session URL
     */
    suspend fun createCheckoutSession(plan: SubscriptionPlan): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("No ID token")

        // TODO: Call Cloud Function to create Stripe checkout session
        // For now, return placeholder
        return "https://checkout.stripe.com/session_placeholder"
    }

    /**
     * Create Stripe customer portal session
     * Returns portal URL
     */
    suspend fun createPortalSession(): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")
        val idToken = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: throw IllegalStateException("No ID token")

        // TODO: Call Cloud Function to create portal session
        // For now, return placeholder
        return "https://billing.stripe.com/portal_placeholder"
    }

    /**
     * Check if local data is fresh (< 5 minutes old)
     */
    private fun isFresh(lastSynced: Long): Boolean {
        val fiveMinutes = 5 * 60 * 1000L
        return (System.currentTimeMillis() - lastSynced) < fiveMinutes
    }

    /**
     * Clear subscription data (on logout)
     */
    suspend fun clear() {
        subscriptionDao.deleteAll()
    }
}
