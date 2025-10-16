package com.example.innovexia.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Health check state for a service
 */
@Entity(tableName = "health_checks")
data class HealthCheckEntity(
    @PrimaryKey
    val serviceId: String,
    val name: String,
    val status: String,       // ONLINE/DEGRADED/OFFLINE/UNKNOWN
    val latencyMs: Long?,
    val version: String?,
    val lastCheckedAt: Long,
    val notes: String?
)

/**
 * Incident record for service degradation/outage
 */
@Entity(
    tableName = "incidents",
    indices = [Index("serviceId")]
)
data class IncidentEntity(
    @PrimaryKey
    val id: String,
    val serviceId: String,
    val status: String,       // Open/Monitoring/Resolved
    val impact: String,
    val startedAt: Long,
    val endedAt: Long? = null
)
