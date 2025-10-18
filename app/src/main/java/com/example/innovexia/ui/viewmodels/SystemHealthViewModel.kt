package com.example.innovexia.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.health.HealthCheck
import com.example.innovexia.core.health.HealthState
import com.example.innovexia.core.health.RealHealthApi
import com.example.innovexia.core.health.toEntity
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.local.entities.IncidentEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for System Health monitoring UI
 */
class SystemHealthViewModel(
    private val healthApi: RealHealthApi,
    private val db: AppDatabase
) : ViewModel() {

    // Health checks from database
    private val _checks = db.healthCheckDao().observe()
        .map { entities ->
            entities.map { entity ->
                HealthCheck(
                    id = entity.serviceId,
                    name = entity.name,
                    status = HealthState.valueOf(entity.status),
                    latencyMs = entity.latencyMs,
                    version = entity.version,
                    lastCheckedAt = entity.lastCheckedAt,
                    notes = entity.notes
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checks: StateFlow<List<HealthCheck>> = _checks

    // Open incidents from database
    val openIncidents: StateFlow<List<IncidentEntity>> = db.incidentDao().observeOpen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total incident count (last 30 days)
    val totalIncidents: StateFlow<Int> = flow {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        emit(db.incidentDao().observeRecent().first().count { it.startedAt >= thirtyDaysAgo })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // System uptime since last major outage
    val uptimeSince: StateFlow<Long?> = db.incidentDao().observeRecent()
        .map { incidents ->
            // Find last resolved major incident (OFFLINE status)
            val lastMajorIncident = incidents
                .filter { it.status == "Resolved" && it.impact.contains("OFFLINE", ignoreCase = true) }
                .maxByOrNull { it.endedAt ?: it.startedAt }

            lastMajorIncident?.endedAt ?: lastMajorIncident?.startedAt
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Overall health state
    val overallState: StateFlow<HealthState> = _checks.map { checkList ->
        checkList.fold(HealthState.ONLINE) { acc, next ->
            when {
                next.status == HealthState.OFFLINE -> HealthState.OFFLINE
                acc != HealthState.OFFLINE && next.status == HealthState.DEGRADED -> HealthState.DEGRADED
                next.status == HealthState.UNKNOWN && acc == HealthState.ONLINE -> HealthState.UNKNOWN
                else -> acc
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HealthState.UNKNOWN)

    // Loading state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Last refresh timestamp
    private val _lastRefresh = MutableStateFlow<Long?>(null)
    val lastRefresh: StateFlow<Long?> = _lastRefresh.asStateFlow()

    // Foreground monitor job
    private var monitorJob: Job? = null

    init {
        // Clean up old/deprecated service entries on init
        viewModelScope.launch {
            try {
                val oldServices = listOf(
                    "db-echo",           // Old test service
                    "embeddings",        // Not implemented separately
                    "files",             // Not implemented
                    "file-service",      // Not implemented
                    "context-engine",    // Old name, replaced by memory-system
                    "summarizer",        // Old name, replaced by rolling-summarizer
                    "persona"            // Old name without suffix, replaced by persona-service
                )

                oldServices.forEach { serviceId ->
                    try {
                        db.healthCheckDao().deleteByServiceId(serviceId)
                    } catch (e: Exception) {
                        // Ignore if service doesn't exist
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Refresh all health checks
     */
    fun refreshAll() {
        if (_isRefreshing.value) return // Debounce

        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val summary = healthApi.checkAll()
                _lastRefresh.value = System.currentTimeMillis()

                // Persist results
                summary.checks.forEach { check ->
                    db.healthCheckDao().upsert(check.toEntity())
                }
            } catch (e: Exception) {
                // Log error (in production, show error state)
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Check a single service
     */
    fun checkService(serviceId: String) {
        viewModelScope.launch {
            try {
                val check = healthApi.check(serviceId)
                db.healthCheckDao().upsert(check.toEntity())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Start foreground monitoring (when UI is visible)
     */
    fun startForegroundMonitoring(intervalMs: Long = 20_000) {
        stopForegroundMonitoring() // Stop existing job

        monitorJob = viewModelScope.launch {
            while (true) {
                refreshAll()
                delay(intervalMs)
            }
        }
    }

    /**
     * Stop foreground monitoring
     */
    fun stopForegroundMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopForegroundMonitoring()
    }
}
