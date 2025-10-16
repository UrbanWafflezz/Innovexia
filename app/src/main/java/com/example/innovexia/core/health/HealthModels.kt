package com.example.innovexia.core.health

/**
 * Health states for services
 */
enum class HealthState {
    ONLINE,
    DEGRADED,
    OFFLINE,
    UNKNOWN
}

/**
 * Health check result for a single service
 */
data class HealthCheck(
    val id: String,
    val name: String,
    val status: HealthState,
    val latencyMs: Long?,
    val version: String?,
    val lastCheckedAt: Long,
    val notes: String?
)

/**
 * Overall health summary
 */
data class HealthSummary(
    val overall: HealthState,
    val checks: List<HealthCheck>,
    val openIncidents: List<com.example.innovexia.data.local.entities.IncidentEntity>
)

/**
 * Map remote status string to HealthState
 */
fun mapRemoteStatus(s: String?): HealthState = when (s?.lowercase()) {
    "online" -> HealthState.ONLINE
    "degraded" -> HealthState.DEGRADED
    "offline" -> HealthState.OFFLINE
    else -> HealthState.UNKNOWN
}
