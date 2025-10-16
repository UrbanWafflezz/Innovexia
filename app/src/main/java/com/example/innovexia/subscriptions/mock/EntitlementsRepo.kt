package com.example.innovexia.subscriptions.mock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository for entitlements persistence and business logic
 * Uses DataStore for local storage and syncs with Firestore for cross-device support
 */
class EntitlementsRepo(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var firestoreListener: ListenerRegistration? = null

    companion object {
        private val Context.entitlementsDataStore: DataStore<Preferences> by preferencesDataStore(
            name = "entitlements"
        )
        private val ENTITLEMENT_KEY = stringPreferencesKey("current_entitlement")
        private val HISTORY_KEY = stringPreferencesKey("entitlement_history")
    }

    private val gson = Gson()

    init {
        // Start listening to Firestore changes on init
        startFirestoreSync()
    }

    /**
     * Flow that emits whenever Firebase auth state changes
     * Emits userId or null for guest
     */
    private val authUserIdFlow: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val userId = auth.currentUser?.uid
            trySend(userId)

            // Clear entitlements when logging out
            if (userId == null) {
                scope.launch {
                    saveLocal(Entitlement.free())
                }
            }
        }
        auth.addAuthStateListener(listener)

        // Emit initial state
        trySend(auth.currentUser?.uid)

        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Flow of current entitlement (synced with Firestore in real-time)
     * Reactive to auth state changes - switches immediately between guest and user entitlements
     */
    val entitlementFlow: Flow<Entitlement> = authUserIdFlow.flatMapLatest { userId ->
        // If no user, return free entitlement immediately
        if (userId == null) {
            return@flatMapLatest flowOf(Entitlement.free())
        }

        // For authenticated users, listen to Firestore in real-time
        callbackFlow {
            val docRef = firestore.collection("users").document(userId)
                .collection("entitlements").document("current")

            // Emit cached value immediately for instant UI response
            try {
                val cached = getLocalEntitlement()
                trySend(cached)
            } catch (e: Exception) {
                trySend(Entitlement.free())
            }

            // Listen to Firestore for real-time updates
            val listener = docRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // On error, emit cached value
                    scope.launch {
                        try {
                            val cached = getLocalEntitlement()
                            trySend(cached)
                        } catch (e: Exception) {
                            trySend(Entitlement.free())
                        }
                    }
                    return@addSnapshotListener
                }

                val entitlement = if (snapshot?.exists() == true) {
                    try {
                        val data = snapshot.data ?: emptyMap<String, Any>()
                        Entitlement.fromMap(data)
                    } catch (e: Exception) {
                        Entitlement.free()
                    }
                } else {
                    Entitlement.free()
                }

                // Cache locally
                scope.launch {
                    saveLocal(entitlement)
                }

                trySend(entitlement)
            }

            awaitClose { listener.remove() }
        }
    }

    /**
     * Flow of feature capabilities based on current plan
     */
    val capsFlow: Flow<FeatureCaps> = entitlementFlow.map { entitlement ->
        FeatureCaps.forPlan(entitlement.planId())
    }

    /**
     * Get current entitlement (suspend)
     */
    suspend fun getCurrent(): Entitlement {
        return entitlementFlow.first()
    }

    /**
     * Get current feature capabilities (suspend)
     */
    suspend fun getCaps(): FeatureCaps {
        return capsFlow.first()
    }

    /**
     * Save entitlement (syncs to both local and Firestore)
     */
    suspend fun save(entitlement: Entitlement) {
        // Save locally
        saveLocal(entitlement)

        // Sync to Firestore
        syncToFirestore(entitlement)
    }

    /**
     * Save entitlement locally only
     */
    private suspend fun saveLocal(entitlement: Entitlement) {
        context.entitlementsDataStore.edit { prefs ->
            val jsonString = gson.toJson(entitlement)
            prefs[ENTITLEMENT_KEY] = jsonString

            // Optionally add to history
            appendToHistory(prefs, entitlement)
        }
    }

    /**
     * Get local entitlement
     */
    private suspend fun getLocalEntitlement(): Entitlement {
        val prefs = context.entitlementsDataStore.data.first()
        val jsonString = prefs[ENTITLEMENT_KEY]
        return if (jsonString != null) {
            try {
                gson.fromJson(jsonString, Entitlement::class.java)
            } catch (e: Exception) {
                Entitlement.free()
            }
        } else {
            Entitlement.free()
        }
    }

    /**
     * Sync entitlement to Firestore
     */
    private suspend fun syncToFirestore(entitlement: Entitlement) {
        val userId = auth.currentUser?.uid ?: return

        try {
            firestore.collection("users").document(userId)
                .collection("entitlements").document("current")
                .set(entitlement.toMap())
                .await()
        } catch (e: Exception) {
            // Log error but don't throw - local save succeeded
            e.printStackTrace()
        }
    }

    /**
     * Start Firestore sync listener
     */
    private fun startFirestoreSync() {
        val userId = auth.currentUser?.uid ?: return

        scope.launch {
            try {
                // Fetch initial state from Firestore
                val docRef = firestore.collection("users").document(userId)
                    .collection("entitlements").document("current")

                val snapshot = docRef.get().await()

                if (snapshot.exists()) {
                    val data = snapshot.data ?: emptyMap<String, Any>()
                    val entitlement = Entitlement.fromMap(data)
                    saveLocal(entitlement)
                }
            } catch (e: Exception) {
                // Silent fail - will use local data
            }
        }
    }

    /**
     * Clear entitlement (reset to free)
     */
    suspend fun clear() {
        save(Entitlement.free())
    }

    /**
     * Set plan directly (for dev/testing)
     */
    suspend fun setDirect(plan: PlanId, period: Period = Period.MONTHLY) {
        val now = TimeUtils.now()
        val renewalDays = when (period) {
            Period.MONTHLY -> 30
            Period.YEARLY -> 365
        }

        val entitlement = Entitlement(
            plan = plan.name,
            period = period.name,
            status = SubStatus.ACTIVE.name,
            startedAt = now,
            renewsAt = if (plan == PlanId.FREE) null else TimeUtils.daysFromNow(renewalDays),
            trialEndsAt = null,
            source = "dev-direct",
            orderId = "DEV-${System.currentTimeMillis()}"
        )

        save(entitlement)
    }

    /**
     * Append to history (optional, for audit trail)
     */
    private fun appendToHistory(prefs: MutablePreferences, entitlement: Entitlement) {
        try {
            val historyJson = prefs[HISTORY_KEY] ?: "[]"
            val type = object : TypeToken<MutableList<Entitlement>>() {}.type
            val history: MutableList<Entitlement> = gson.fromJson(historyJson, type) ?: mutableListOf()

            // Keep last 20 entries
            if (history.size >= 20) {
                history.removeAt(0)
            }

            history.add(entitlement)
            prefs[HISTORY_KEY] = gson.toJson(history)
        } catch (e: Exception) {
            // Ignore history errors
        }
    }

    /**
     * Get entitlement history
     */
    suspend fun getHistory(): List<Entitlement> {
        val prefs = context.entitlementsDataStore.data.first()
        val historyJson = prefs[HISTORY_KEY] ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Entitlement>>() {}.type
            gson.fromJson(historyJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Manually trigger sync from Firestore (useful after login)
     */
    suspend fun forceSync() {
        val userId = auth.currentUser?.uid ?: return

        try {
            val docRef = firestore.collection("users").document(userId)
                .collection("entitlements").document("current")

            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val data = snapshot.data ?: emptyMap<String, Any>()
                val entitlement = Entitlement.fromMap(data)
                save(entitlement)  // This will save locally and sync back
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stop Firestore listener (on logout)
     */
    fun stopSync() {
        firestoreListener?.remove()
        firestoreListener = null
    }
}

/**
 * Feature gating helper
 * Used throughout the app to check capabilities
 */
object Gate {
    /**
     * Check if user has access to a specific model
     */
    fun hasModel(model: String, caps: FeatureCaps): Boolean {
        return model.lowercase() in caps.models.map { it.lowercase() }
    }

    /**
     * Check if user can add more sources
     */
    fun canAddSource(currentCount: Int, caps: FeatureCaps): Boolean {
        return currentCount < caps.maxSources
    }

    /**
     * Get maximum number of sources allowed
     */
    fun maxSources(caps: FeatureCaps): Int = caps.maxSources

    /**
     * Get maximum upload size in bytes
     */
    fun maxUploadBytes(caps: FeatureCaps): Long {
        return caps.maxUploadMb * 1024L * 1024L
    }

    /**
     * Get memory entry limit (null = unlimited)
     */
    fun memoryLimit(caps: FeatureCaps): Int? = caps.memoryEntries

    /**
     * Check if memory is within limit
     */
    fun canAddMemory(currentCount: Int, caps: FeatureCaps): Boolean {
        val limit = caps.memoryEntries ?: return true
        return currentCount < limit
    }

    /**
     * Check if cloud backup is available
     */
    fun hasCloudBackup(caps: FeatureCaps): Boolean = caps.cloudBackup

    /**
     * Check if team spaces are available
     */
    fun hasTeamSpaces(caps: FeatureCaps): Boolean = caps.teamSpaces > 0

    /**
     * Get maximum team size
     */
    fun maxTeamSize(caps: FeatureCaps): Int = caps.teamSpaces

    /**
     * Get priority class for rate limiting
     */
    fun priorityClass(caps: FeatureCaps): Int = caps.priorityClass

    /**
     * Format upgrade message for gated feature
     */
    fun upgradeMessage(feature: String, requiredPlan: PlanId): String {
        return "$feature is available on ${requiredPlan.name.lowercase().replaceFirstChar { it.uppercase() }} and above. Upgrade to unlock."
    }
}
