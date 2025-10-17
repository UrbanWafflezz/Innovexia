package com.example.innovexia

import android.app.Application
import androidx.work.*
import com.example.innovexia.core.health.HealthRetrofitApi
import com.example.innovexia.core.health.RealHealthApi
import com.example.innovexia.core.health.ServiceCatalog
import com.example.innovexia.core.net.Connectivity
import com.example.innovexia.core.ratelimit.FirebaseRateLimiter
import com.example.innovexia.data.ai.GeminiService
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.preferences.UserPreferences
import com.example.innovexia.data.repository.ChatRepository
import com.example.innovexia.data.repository.SubscriptionRepository
import com.example.innovexia.data.repository.UsageRepository
import com.example.innovexia.memory.Mind.api.*
import com.example.innovexia.memory.Mind.sources.api.SourcesEngine
import com.example.innovexia.memory.Mind.sources.store.entities.SourceChunkEntity
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity
import com.example.innovexia.subscriptions.mock.*
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.example.innovexia.subscriptions.stripe.BillingRetrofitClient
import com.example.innovexia.subscriptions.stripe.StripeBillingProvider
import com.example.innovexia.workers.HealthWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stripe.android.PaymentConfiguration
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Simple stub implementation of MemoryEngine for local models
 * Returns empty results - memory features not used for now
 */
private class StubMemoryEngine : MemoryEngine {
    override suspend fun enable(personaId: String, enabled: Boolean) {}
    override suspend fun isEnabled(personaId: String): Boolean = false
    override suspend fun ingest(turn: ChatTurn, personaId: String, incognito: Boolean) {}
    override suspend fun contextFor(message: String, personaId: String, chatId: String): ContextBundle {
        return ContextBundle(
            shortTerm = emptyList(),
            longTerm = emptyList(),
            totalTokens = 0
        )
    }
    override fun observeCounts(personaId: String, userId: String): Flow<List<CategoryCount>> = flowOf(emptyList())
    override fun feed(personaId: String, userId: String, kind: MemoryKind?, query: String?): Flow<List<MemoryHit>> = flowOf(emptyList())
    override suspend fun delete(memoryId: String) {}
    override suspend fun deleteAll(personaId: String) {}
    override suspend fun getCount(personaId: String, userId: String): Int = 0
}

/**
 * Simple stub implementation of SourcesEngine for local models
 * Returns empty results - RAG features not used for now
 */
private class StubSourcesEngine : SourcesEngine {
    override suspend fun addPdfFromUri(personaId: String, uri: Uri): Result<String> = Result.success("")
    override suspend fun addFileFromUri(personaId: String, uri: Uri): Result<String> = Result.success("")
    override suspend fun addUrlSource(personaId: String, url: String, maxDepth: Int, maxPages: Int): Result<String> = Result.success("")
    override fun observeSource(personaId: String, sourceId: String): Flow<SourceEntity?> = flowOf(null)
    override fun observeSources(personaId: String): Flow<List<SourceEntity>> = flowOf(emptyList())
    override suspend fun listChunks(sourceId: String): List<SourceChunkEntity> = emptyList()
    override suspend fun reindex(sourceId: String): Result<Unit> = Result.success(Unit)
    override suspend fun removeSource(sourceId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getStorageUsed(personaId: String): Long = 0L
    override suspend fun getSourceCount(personaId: String): Int = 0
    override suspend fun searchChunks(personaId: String, query: String, limit: Int): List<SourceChunkEntity> = emptyList()
    override suspend fun getContextForQuery(personaId: String, query: String, limit: Int): String = ""
}

/**
 * Application class for Innovexia.
 * Initializes singletons for database, repository, and services.
 */
@HiltAndroidApp
class InnovexiaApplication : Application() {

    // Lazy-initialized singletons
    val database by lazy { AppDatabase.getInstance(this) }
    val userPreferences by lazy { UserPreferences(this) }
    val chatRepository by lazy {
        ChatRepository(
            chatDao = database.chatDao(),
            messageDao = database.messageDao(),
            database = database,
            context = this,
            userPreferences = userPreferences
        )
    }
    val geminiService by lazy { GeminiService(database, this, userPreferences) }

    // Persona repository for managing personas
    val personaRepository by lazy {
        com.example.innovexia.core.persona.PersonaRepository(
            personaDao = database.personaDao()
        )
    }

    // Subscription and usage repositories
    val subscriptionRepository by lazy {
        SubscriptionRepository(
            subscriptionDao = database.subscriptionDao(),
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance()
        )
    }

    val usageRepository by lazy {
        UsageRepository(
            usageDao = database.usageDao(),
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance()
        )
    }

    // Rate limiter (separate tracking for guest vs logged-in users)
    val firebaseRateLimiter by lazy {
        FirebaseRateLimiter(
            firestore = FirebaseFirestore.getInstance(),
            auth = FirebaseAuth.getInstance()
        )
    }

    // Health monitoring
    val connectivity by lazy { Connectivity(this) }

    private val healthOkHttp by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }

    val healthApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://placeholder.local/")
            .client(healthOkHttp)
            .build()

        RealHealthApi(
            http = retrofit.create(HealthRetrofitApi::class.java),
            catalog = { ServiceCatalog.load() },
            db = database,
            context = this
        )
    }

    // Entitlements and billing providers
    val entitlementsRepo by lazy {
        EntitlementsRepo(this)
    }

    // Mock billing provider (for testing without Stripe)
    val mockBillingProvider by lazy {
        MockBillingProvider(entitlementsRepo)
    }

    // Stripe billing provider (for real subscriptions)
    val stripeBillingProvider by lazy {
        StripeBillingProvider(
            api = BillingRetrofitClient.api,
            entitlementsRepo = entitlementsRepo
        )
    }

    // Active billing provider (switch between mock and stripe)
    // Set USE_STRIPE = true to enable Stripe payments
    val billingProvider: BillingProvider by lazy {
        if (USE_STRIPE) stripeBillingProvider else mockBillingProvider
    }

    companion object {
        // Toggle to switch between mock and Stripe billing
        const val USE_STRIPE = true  // Set to true to use Stripe

        // Stripe publishable key (test mode)
        private const val STRIPE_PUBLISHABLE_KEY = "pk_test_51QTE5NRutIy9oqiFymSauDhoWiKkRuQLau9AdG2YdGe7UV0R5vhBQH5mZsTy54bwbNQ8q51fBhIE14J4zJBIfEUu003sBu9vBb"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Stripe SDK if using Stripe billing
        if (USE_STRIPE) {
            PaymentConfiguration.init(
                applicationContext,
                STRIPE_PUBLISHABLE_KEY
            )
        }

        scheduleHealthMonitoring()
        scheduleEntitlementChecks()
        scheduleUpdateChecks()
        seedInnoPersona()
    }

    /**
     * Seed Inno default persona for all users on first launch.
     * This runs in the background and creates Inno if it doesn't exist.
     * Also deduplicates any existing Inno personas to prevent duplicates.
     */
    private fun seedInnoPersona() {
        // Run in background thread to avoid blocking app startup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get current owner ID (guest or Firebase UID)
                val ownerId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: com.example.innovexia.core.auth.ProfileId.GUEST_OWNER_ID

                // IMPORTANT: Deduplicate any existing Inno personas before checking
                personaRepository.deduplicateInnoPersonas(ownerId)

                // Check if Inno already exists
                val hasInno = personaRepository.hasInnoPersona(ownerId)

                if (!hasInno) {
                    // Create Inno as default persona
                    val inno = personaRepository.ensureInnoIsDefault(ownerId)
                    android.util.Log.d("InnovexiaApplication", "Seeded Inno persona for owner $ownerId: ${inno.name}")
                } else {
                    android.util.Log.d("InnovexiaApplication", "Inno persona already exists for owner $ownerId")
                }
            } catch (e: Exception) {
                android.util.Log.e("InnovexiaApplication", "Failed to seed Inno persona", e)
                // Don't crash app - Inno will be created lazily when needed
            }
        }
    }

    /**
     * Schedule periodic health monitoring with WorkManager
     */
    private fun scheduleHealthMonitoring() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val healthWorkRequest = PeriodicWorkRequestBuilder<HealthWorker>(
            15, TimeUnit.MINUTES // Minimum interval for periodic work
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            HealthWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            healthWorkRequest
        )
    }

    /**
     * Schedule periodic entitlement state checks
     */
    private fun scheduleEntitlementChecks() {
        val workRequest = PeriodicWorkRequestBuilder<com.example.innovexia.workers.EntitlementCheckWorker>(
            6, TimeUnit.HOURS // Check every 6 hours
        )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "entitlement_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Schedule periodic update checks (every 12 hours)
     */
    private fun scheduleUpdateChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateWorkRequest = PeriodicWorkRequestBuilder<com.example.innovexia.core.update.UpdateWorker>(
            12, TimeUnit.HOURS // Check for updates every 12 hours
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            com.example.innovexia.core.update.UpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            updateWorkRequest
        )

        android.util.Log.d("InnovexiaApplication", "Scheduled background update checks (every 12 hours)")
    }
}
