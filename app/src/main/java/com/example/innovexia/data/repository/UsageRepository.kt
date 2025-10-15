package com.example.innovexia.data.repository

import com.example.innovexia.data.local.dao.UsageDao
import com.example.innovexia.data.local.entities.DailyUsageEntity
import com.example.innovexia.data.local.entities.UsageEntity
import com.example.innovexia.data.models.DailyUsage
import com.example.innovexia.data.models.UsageData
import com.example.innovexia.data.models.UsageSnapshot
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository for tracking and syncing usage data
 * Tracks usage for both guests (local only) and authenticated users (synced to Firestore)
 */
class UsageRepository(
    private val usageDao: UsageDao,
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
     * Get current month's usage (Flow)
     * Works for both guests and authenticated users
     * Reactive to auth state changes - switches between guest and user usage
     */
    fun getCurrentUsageFlow(): Flow<UsageData?> {
        val periodId = UsageData.getCurrentPeriodId()

        return authUserIdFlow.flatMapLatest { userId ->
            usageDao.getUsageFlow(userId, periodId).map { it?.toUsageData() }
        }
    }

    /**
     * Get current month's usage (one-time)
     * Works for both guests (local only) and authenticated users (synced)
     */
    suspend fun getCurrentUsage(): UsageData {
        val userId = auth.currentUser?.uid ?: "guest"
        val periodId = UsageData.getCurrentPeriodId()

        // Try local first
        val local = usageDao.getUsage(userId, periodId)
        if (local != null && (userId == "guest" || isFresh(local.lastSynced))) {
            return local.toUsageData()
        }

        // Guests: return local or empty (no Firestore)
        if (userId == "guest") {
            return local?.toUsageData() ?: createEmptyUsage()
        }

        // Authenticated users: fetch from Firestore
        return fetchFromFirestore(userId, periodId)
    }

    /**
     * Get today's usage (Flow)
     * Works for both guests and authenticated users
     * Reactive to auth state changes - switches between guest and user usage
     */
    fun getTodayUsageFlow(): Flow<DailyUsage?> {
        val today = DailyUsage.getTodayDate()

        return authUserIdFlow.flatMapLatest { userId ->
            usageDao.getDailyUsageFlow(userId, today).map { entity ->
                entity?.let {
                    DailyUsage(
                        date = it.date,
                        tokensIn = it.tokensIn,
                        tokensOut = it.tokensOut,
                        requests = it.requests
                    )
                }
            }
        }
    }

    /**
     * Get today's usage (one-time)
     * Works for both guests and authenticated users
     */
    suspend fun getTodayUsage(): DailyUsage {
        val userId = auth.currentUser?.uid ?: "guest"
        val today = DailyUsage.getTodayDate()

        val entity = usageDao.getDailyUsage(userId, today)
        return if (entity != null) {
            DailyUsage(
                date = entity.date,
                tokensIn = entity.tokensIn,
                tokensOut = entity.tokensOut,
                requests = entity.requests
            )
        } else {
            DailyUsage(today)
        }
    }

    /**
     * Track usage from a model response
     * Updates both monthly and daily usage
     * Works for both guests (local only) and authenticated users (synced to Firestore)
     */
    suspend fun trackUsage(
        tokensIn: Long,
        tokensOut: Long,
        attachmentsBytes: Long = 0L
    ) {
        val userId = auth.currentUser?.uid ?: "guest"  // Track guest usage locally
        val isGuest = userId == "guest"
        val periodId = UsageData.getCurrentPeriodId()
        val today = DailyUsage.getTodayDate()
        val now = System.currentTimeMillis()

        // Update monthly usage locally
        val monthlyId = UsageEntity.createId(userId, periodId)
        val existing = usageDao.getUsage(userId, periodId)

        if (existing != null) {
            usageDao.incrementUsage(
                id = monthlyId,
                tokensIn = tokensIn,
                tokensOut = tokensOut,
                requests = 1L,
                attachmentsBytes = attachmentsBytes,
                timestamp = now
            )
        } else {
            usageDao.upsert(
                UsageEntity(
                    id = monthlyId,
                    userId = userId,
                    periodId = periodId,
                    tokensIn = tokensIn,
                    tokensOut = tokensOut,
                    requests = 1L,
                    attachmentsBytes = attachmentsBytes,
                    lastUpdated = now,
                    lastSynced = now
                )
            )
        }

        // Update daily usage locally
        val dailyId = DailyUsageEntity.createId(userId, today)
        val existingDaily = usageDao.getDailyUsage(userId, today)

        if (existingDaily != null) {
            usageDao.incrementDailyUsage(
                id = dailyId,
                tokensIn = tokensIn,
                tokensOut = tokensOut,
                requests = 1L,
                timestamp = now
            )
        } else {
            usageDao.upsertDaily(
                DailyUsageEntity(
                    id = dailyId,
                    userId = userId,
                    date = today,
                    tokensIn = tokensIn,
                    tokensOut = tokensOut,
                    requests = 1L,
                    lastUpdated = now
                )
            )
        }

        // Sync to Firestore (fire-and-forget) - skip for guests
        if (!isGuest) {
            syncToFirestore(userId, periodId, tokensIn, tokensOut, attachmentsBytes)
        }
    }

    /**
     * Update usage from server snapshot
     * Called after receiving response from /v1/generate
     * Only applicable for authenticated users
     */
    suspend fun updateFromSnapshot(snapshot: UsageSnapshot) {
        val userId = auth.currentUser?.uid ?: return  // Skip for guests

        // Update local cache
        val entity = UsageEntity(
            id = UsageEntity.createId(userId, snapshot.monthId),
            userId = userId,
            periodId = snapshot.monthId,
            tokensIn = snapshot.tokensIn,
            tokensOut = snapshot.tokensOut,
            requests = snapshot.requests,
            attachmentsBytes = snapshot.attachmentsBytes,
            lastUpdated = System.currentTimeMillis(),
            lastSynced = System.currentTimeMillis()
        )

        usageDao.upsert(entity)
    }

    /**
     * Fetch usage from Firestore
     */
    private suspend fun fetchFromFirestore(userId: String, periodId: String): UsageData {
        return try {
            val docRef = firestore.collection("users").document(userId)
                .collection("usage").document(periodId)

            val snapshot = docRef.get().await()

            val usage = if (snapshot.exists()) {
                val data = snapshot.data ?: emptyMap()
                UsageData.fromMap(periodId, data)
            } else {
                UsageData(periodId)
            }

            // Cache locally
            val entity = UsageEntity.fromUsageData(userId, usage)
            usageDao.upsert(entity)

            usage
        } catch (e: Exception) {
            // Fallback to local or empty
            usageDao.getUsage(userId, periodId)?.toUsageData()
                ?: UsageData(periodId)
        }
    }

    /**
     * Sync usage to Firestore (increment counters)
     */
    private fun syncToFirestore(
        userId: String,
        periodId: String,
        tokensIn: Long,
        tokensOut: Long,
        attachmentsBytes: Long
    ) {
        scope.launch {
            try {
                val docRef = firestore.collection("users").document(userId)
                    .collection("usage").document(periodId)

                docRef.update(
                    mapOf(
                        "tokensIn" to FieldValue.increment(tokensIn),
                        "tokensOut" to FieldValue.increment(tokensOut),
                        "requests" to FieldValue.increment(1),
                        "attachmentsBytes" to FieldValue.increment(attachmentsBytes),
                        "lastUpdated" to Timestamp.now()
                    )
                ).await()
            } catch (e: Exception) {
                // If document doesn't exist, create it
                try {
                    val docRef = firestore.collection("users").document(userId)
                        .collection("usage").document(periodId)

                    docRef.set(
                        mapOf(
                            "tokensIn" to tokensIn,
                            "tokensOut" to tokensOut,
                            "requests" to 1L,
                            "attachmentsBytes" to attachmentsBytes,
                            "lastUpdated" to Timestamp.now()
                        )
                    ).await()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }

    /**
     * Check if local data is fresh (< 1 minute old for usage)
     */
    private fun isFresh(lastSynced: Long): Boolean {
        val oneMinute = 60 * 1000L
        return (System.currentTimeMillis() - lastSynced) < oneMinute
    }

    /**
     * Create empty usage
     */
    private fun createEmptyUsage(): UsageData {
        return UsageData(UsageData.getCurrentPeriodId())
    }

    /**
     * Clear usage data (on logout)
     */
    suspend fun clear() {
        usageDao.deleteAll()
        usageDao.deleteAllDaily()
    }

    /**
     * Cleanup old daily usage (keep last 90 days)
     */
    suspend fun cleanupOldDailyUsage() {
        val userId = auth.currentUser?.uid ?: return
        val cutoffDate = java.time.LocalDate.now().minusDays(90).toString()
        usageDao.deleteOldDailyUsage(userId, cutoffDate)
    }
}
