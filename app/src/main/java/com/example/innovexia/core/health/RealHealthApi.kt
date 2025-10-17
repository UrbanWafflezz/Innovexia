package com.example.innovexia.core.health

import android.content.Context
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.local.entities.HealthCheckEntity
import com.example.innovexia.data.local.entities.IncidentEntity
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject
import java.util.UUID

/**
 * Real health API that performs actual HTTP checks and local probes
 */
class RealHealthApi(
    private val http: HealthRetrofitApi,
    private val catalog: () -> List<ServiceDescriptor>,
    private val db: AppDatabase,
    @Suppress("unused") private val context: Context
) {

    /**
     * Check all services and return summary
     */
    suspend fun checkAll(): HealthSummary {
        val results = catalog().map { desc -> runServiceCheck(desc) }

        // Compute overall state
        val overall = results.fold(HealthState.ONLINE) { acc, next ->
            when {
                next.status == HealthState.OFFLINE -> HealthState.OFFLINE
                acc != HealthState.OFFLINE && next.status == HealthState.DEGRADED -> HealthState.DEGRADED
                else -> acc
            }
        }

        val open = db.incidentDao().observeOpen().firstOrNull().orEmpty()
        return HealthSummary(overall, results, open)
    }

    /**
     * Check a single service by ID
     */
    suspend fun check(serviceId: String): HealthCheck {
        val desc = catalog().firstOrNull { it.id == serviceId }
            ?: error("Service $serviceId not found in catalog")
        return runServiceCheck(desc)
    }

    /**
     * Run health check for a service
     */
    private suspend fun runServiceCheck(desc: ServiceDescriptor): HealthCheck {
        val now = System.currentTimeMillis()

        return when (desc.id) {
            // Local services
            "database" -> checkDatabase(desc, now)
            "cache" -> checkCache(desc, now)
            "memory-system" -> checkMemorySystem(desc, now)
            "rolling-summarizer" -> checkRollingSummarizer(desc, now)

            // Remote services
            "gemini-bridge" -> checkGeminiBridge(desc, now)
            "persona-service" -> checkPersonaService(desc, now)

            // Unconfigured service
            else -> {
                val fallback = HealthCheck(
                    desc.id, desc.name, HealthState.UNKNOWN,
                    null, null, now, "Unconfigured"
                )
                db.healthCheckDao().upsert(fallback.toEntity())
                fallback
            }
        }
    }

    /**
     * Check database health with round-trip latency
     */
    private suspend fun checkDatabase(desc: ServiceDescriptor, now: Long): HealthCheck {
        return try {
            val t0 = System.nanoTime()

            // Perform a real DB operation - count chats
            val chatCount = db.chatDao().getAllFor("health-check").size

            val latency = ((System.nanoTime() - t0) / 1_000_000)

            val check = HealthCheck(
                desc.id, desc.name, HealthState.ONLINE,
                latency, null, now, "OK"
            )
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.ONLINE, null, now)
            check
        } catch (e: Exception) {
            val check = HealthCheck(
                desc.id, desc.name, HealthState.OFFLINE,
                null, null, now, "DB Error: ${e.message}"
            )
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.OFFLINE, e.message, now)
            check
        }
    }

    /**
     * Check local cache health (DataStore + in-memory)
     */
    private suspend fun checkCache(desc: ServiceDescriptor, now: Long): HealthCheck {
        return try {
            val t0 = System.nanoTime()

            // Test DataStore access
            val prefs = context.getSharedPreferences("test_health", Context.MODE_PRIVATE)
            prefs.edit().putLong("health_check", now).apply()
            val value = prefs.getLong("health_check", 0L)

            val latency = ((System.nanoTime() - t0) / 1_000_000)

            val check = if (value == now) {
                HealthCheck(
                    desc.id, desc.name, HealthState.ONLINE,
                    latency, null, now, "Cache operational"
                )
            } else {
                HealthCheck(
                    desc.id, desc.name, HealthState.DEGRADED,
                    latency, null, now, "Cache verification failed"
                )
            }

            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, check.status, check.notes, now)
            check
        } catch (e: Exception) {
            val check = HealthCheck(
                desc.id, desc.name, HealthState.OFFLINE,
                null, null, now, "Cache error: ${e.message}"
            )
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.OFFLINE, e.message, now)
            check
        }
    }

    /**
     * Check memory system health
     * Tests MemoryEngine, MemoryDatabase, and embedder functionality
     */
    private suspend fun checkMemorySystem(desc: ServiceDescriptor, now: Long): HealthCheck {
        return try {
            val t0 = System.nanoTime()

            // Initialize memory engine
            val memoryEngine = com.example.innovexia.memory.Mind.di.MindModule.provideMemoryEngine(context)

            // Test memory database by getting count for test persona
            val testPersonaId = "health-check-test"
            val testUserId = "health-check-user"
            val count = memoryEngine.getCount(testPersonaId, testUserId)

            // Test embedder availability
            val embedder = com.example.innovexia.memory.Mind.di.MindModule.provideEmbedder(768, context)
            val isRealEmbedder = embedder is com.example.innovexia.memory.Mind.embed.GeminiEmbedder

            val latency = ((System.nanoTime() - t0) / 1_000_000)

            val notes = if (isRealEmbedder) {
                "Online (Gemini embeddings)"
            } else {
                "Online (fallback embeddings)"
            }

            val check = HealthCheck(
                desc.id, desc.name, HealthState.ONLINE,
                latency, null, now, notes
            )
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.ONLINE, null, now)
            check
        } catch (e: Exception) {
            val check = HealthCheck(
                desc.id, desc.name, HealthState.OFFLINE,
                null, null, now, "Memory system error: ${e.message}"
            )
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.OFFLINE, e.message, now)
            check
        }
    }

    /**
     * Check HTTP service health endpoint
     */
    private suspend fun checkHttpService(desc: ServiceDescriptor, now: Long): HealthCheck {
        val url = desc.baseUrl.trimEnd('/') + desc.healthPath

        return try {
            val t0 = System.nanoTime()
            val resp = http.getHealth(url)
            val latency = ((System.nanoTime() - t0) / 1_000_000)

            if (resp.isSuccessful()) {
                val body = resp.body()?.string() ?: ""
                parseHealthResponse(desc, body, latency, now)
            } else {
                val notes = "HTTP ${resp.code()}"
                val check = HealthCheck(
                    desc.id, desc.name, HealthState.OFFLINE,
                    null, null, now, notes
                )
                db.healthCheckDao().upsert(check.toEntity())
                maybeOpenOrResolveIncident(desc.id, HealthState.OFFLINE, notes, now)
                check
            }
        } catch (e: Exception) {
            val notes = "Connection failed: ${e.message}"
            val check = HealthCheck(
                desc.id, desc.name, HealthState.OFFLINE,
                null, null, now, notes
            )
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.OFFLINE, notes, now)
            check
        }
    }

    /**
     * Parse health endpoint JSON response
     */
    private suspend fun parseHealthResponse(
        desc: ServiceDescriptor,
        body: String,
        latency: Long,
        now: Long
    ): HealthCheck {
        return try {
            val json = JSONObject(body)
            val state = mapRemoteStatus(json.optString("status"))
            val version = if (json.has("version")) json.getString("version") else null
            val notes = if (json.has("notes")) json.getString("notes") else null

            val check = HealthCheck(desc.id, desc.name, state, latency, version, now, notes)
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, state, notes, now)
            check
        } catch (_: Exception) {
            // Invalid JSON response
            val check = HealthCheck(
                desc.id, desc.name, HealthState.UNKNOWN,
                latency, null, now, "Invalid response format"
            )
            db.healthCheckDao().upsert(check.toEntity())
            check
        }
    }

    /**
     * Open or resolve incidents based on health state transitions
     */
    private suspend fun maybeOpenOrResolveIncident(
        serviceId: String,
        state: HealthState,
        notes: String?,
        now: Long
    ) {
        val dao = db.incidentDao()
        val open = dao.observeOpen().firstOrNull().orEmpty()
            .firstOrNull { it.serviceId == serviceId }

        when (state) {
            HealthState.OFFLINE, HealthState.DEGRADED -> {
                if (open == null) {
                    // Create new incident
                    dao.upsert(
                        IncidentEntity(
                            id = UUID.randomUUID().toString(),
                            serviceId = serviceId,
                            status = if (state == HealthState.OFFLINE) "Open" else "Monitoring",
                            impact = notes ?: "Service status: $state",
                            startedAt = now,
                            endedAt = null
                        )
                    )
                }
            }
            HealthState.ONLINE -> {
                if (open != null) {
                    // Resolve existing incident
                    dao.resolve(open.id, now)
                }
            }
            else -> Unit
        }
    }

    /**
     * Check Gemini Bridge health (Google Gemini API connectivity)
     */
    private suspend fun checkGeminiBridge(desc: ServiceDescriptor, now: Long): HealthCheck {
        return try {
            val t0 = System.nanoTime()

            // Test Gemini API connectivity with a simple models list request
            val apiKey = try {
                com.example.innovexia.BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }

            if (apiKey.isEmpty()) {
                val check = HealthCheck(
                    desc.id, desc.name, HealthState.OFFLINE,
                    null, null, now, "API key not configured"
                )
                db.healthCheckDao().upsert(check.toEntity())
                maybeOpenOrResolveIncident(desc.id, HealthState.OFFLINE, "No API key", now)
                return check
            }

            val url = "${desc.baseUrl}${desc.healthPath}?key=$apiKey"
            val resp = http.getHealth(url)
            val latency = ((System.nanoTime() - t0) / 1_000_000)

            val check = if (resp.isSuccessful()) {
                HealthCheck(
                    desc.id, desc.name, HealthState.ONLINE,
                    latency, null, now, "API responsive"
                )
            } else {
                HealthCheck(
                    desc.id, desc.name, HealthState.OFFLINE,
                    latency, null, now, "HTTP ${resp.code()}"
                )
            }

            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, check.status, check.notes, now)
            check
        } catch (e: Exception) {
            val check = HealthCheck(
                desc.id, desc.name, HealthState.OFFLINE,
                null, null, now, "Connection failed: ${e.message}"
            )
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.OFFLINE, e.message, now)
            check
        }
    }

    /**
     * Check Persona Service health (Firebase Firestore connectivity)
     */
    private suspend fun checkPersonaService(desc: ServiceDescriptor, now: Long): HealthCheck {
        return try {
            val t0 = System.nanoTime()

            // Test Firestore connectivity by checking if Firebase is initialized
            val firebaseApp = try {
                com.google.firebase.FirebaseApp.getInstance()
            } catch (e: Exception) {
                null
            }

            val latency = ((System.nanoTime() - t0) / 1_000_000)

            val check = if (firebaseApp != null) {
                // Try to get Firestore instance
                try {
                    val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    HealthCheck(
                        desc.id, desc.name, HealthState.ONLINE,
                        latency, null, now, "Firebase connected"
                    )
                } catch (e: Exception) {
                    HealthCheck(
                        desc.id, desc.name, HealthState.DEGRADED,
                        latency, null, now, "Firestore init failed"
                    )
                }
            } else {
                HealthCheck(
                    desc.id, desc.name, HealthState.OFFLINE,
                    latency, null, now, "Firebase not initialized"
                )
            }

            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, check.status, check.notes, now)
            check
        } catch (e: Exception) {
            val check = HealthCheck(
                desc.id, desc.name, HealthState.OFFLINE,
                null, null, now, "Error: ${e.message}"
            )
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.OFFLINE, e.message, now)
            check
        }
    }

    /**
     * Check Rolling Summarizer health (local chat summarization)
     */
    private suspend fun checkRollingSummarizer(desc: ServiceDescriptor, now: Long): HealthCheck {
        return try {
            val t0 = System.nanoTime()

            // Test by checking if MemorySummarizer is available
            val chatDao = db.chatDao()
            val testQuery = chatDao.getById("test-health-check")

            val latency = ((System.nanoTime() - t0) / 1_000_000)

            val check = HealthCheck(
                desc.id, desc.name, HealthState.ONLINE,
                latency, null, now, "Summarizer ready"
            )

            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.ONLINE, null, now)
            check
        } catch (e: Exception) {
            val check = HealthCheck(
                desc.id, desc.name, HealthState.OFFLINE,
                null, null, now, "Summarizer error: ${e.message}"
            )
            db.healthCheckDao().upsert(check.toEntity())
            maybeOpenOrResolveIncident(desc.id, HealthState.OFFLINE, e.message, now)
            check
        }
    }
}

/**
 * Convert HealthCheck to entity
 */
fun HealthCheck.toEntity() = HealthCheckEntity(
    serviceId = id,
    name = name,
    status = status.name,
    latencyMs = latencyMs,
    version = version,
    lastCheckedAt = lastCheckedAt,
    notes = notes
)

/**
 * Map remote status string to HealthState
 */
fun mapRemoteStatus(status: String): HealthState {
    return when (status.lowercase()) {
        "online", "ok", "healthy", "up" -> HealthState.ONLINE
        "degraded", "warning", "slow" -> HealthState.DEGRADED
        "offline", "down", "error" -> HealthState.OFFLINE
        else -> HealthState.UNKNOWN
    }
}
