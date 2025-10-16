package com.example.innovexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.innovexia.core.health.HealthCheck
import com.example.innovexia.core.health.HealthState
import com.example.innovexia.data.local.entities.IncidentEntity
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * System Health monitoring tab UI
 */
@Composable
fun SystemHealthTab(
    checks: List<HealthCheck>,
    overallState: HealthState,
    openIncidents: List<IncidentEntity>,
    isRefreshing: Boolean,
    lastRefresh: Long?,
    isConnected: Boolean,
    onRefresh: () -> Unit,
    onCheckService: (String) -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with overall status
        OverallStatusCard(
                checks = checks,
                overallState = overallState,
                isRefreshing = isRefreshing,
                lastRefresh = lastRefresh,
                isConnected = isConnected,
                onRefresh = onRefresh,
                darkTheme = darkTheme
            )

        // Connectivity warning
        if (!isConnected) {
            ConnectivityWarningBanner(darkTheme)
        }

        // Open incidents section
        if (openIncidents.isNotEmpty()) {
            Text(
                    text = "Open Incidents",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                )

            openIncidents.forEach { incident ->
                IncidentCard(incident, darkTheme)
            }
        }

        // Services section
        Text(
                text = "Services",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                modifier = Modifier.padding(top = 8.dp)
            )

        checks.forEach { check ->
            ServiceCard(
                check = check,
                onCheck = { onCheckService(check.id) },
                darkTheme = darkTheme
            )
        }
    }
}

@Composable
private fun OverallStatusCard(
    checks: List<HealthCheck>,
    overallState: HealthState,
    isRefreshing: Boolean,
    lastRefresh: Long?,
    isConnected: Boolean,
    onRefresh: () -> Unit,
    darkTheme: Boolean
) {
    // Calculate metrics
    val totalServices = checks.size
    val onlineServices = checks.count { it.status == HealthState.ONLINE }
    val degradedServices = checks.count { it.status == HealthState.DEGRADED }
    val offlineServices = checks.count { it.status == HealthState.OFFLINE }
    val avgLatency = checks.mapNotNull { it.latencyMs }.average().takeIf { !it.isNaN() }?.toLong()
    val uptimePercentage = if (totalServices > 0) (onlineServices.toFloat() / totalServices * 100).toInt() else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HealthStateIndicator(overallState, size = 16.dp)
                    Text(
                        text = "System Health",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing && isConnected
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh",
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                }
            }

            // Status text with inline uptime badge
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = overallState.displayName(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = overallState.color()
                    )

                    // Small uptime badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(overallState.color().copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$uptimePercentage%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = overallState.color()
                        )
                    }
                }

                if (lastRefresh != null) {
                    Text(
                        text = formatTimestamp(lastRefresh),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                }
            }

            // Visual service status bar
            ServiceStatusBar(
                online = onlineServices,
                degraded = degradedServices,
                offline = offlineServices,
                darkTheme = darkTheme
            )

            // Compact metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricPill(
                    label = "Services",
                    value = "$onlineServices/$totalServices",
                    color = InnovexiaColors.Success,
                    darkTheme = darkTheme
                )

                if (avgLatency != null) {
                    MetricPill(
                        label = "Avg Latency",
                        value = "${avgLatency}ms",
                        color = InnovexiaColors.BlueAccent,
                        darkTheme = darkTheme
                    )
                }

                if (degradedServices > 0) {
                    MetricPill(
                        label = "Degraded",
                        value = degradedServices.toString(),
                        color = InnovexiaColors.WarningAlt,
                        darkTheme = darkTheme
                    )
                }

                if (offlineServices > 0) {
                    MetricPill(
                        label = "Offline",
                        value = offlineServices.toString(),
                        color = InnovexiaColors.ErrorAlt,
                        darkTheme = darkTheme
                    )
                }
            }

            // Loading indicator
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = InnovexiaColors.BlueAccent
                )
            }
        }
    }
}

@Composable
private fun ServiceStatusBar(
    online: Int,
    degraded: Int,
    offline: Int,
    darkTheme: Boolean
) {
    val total = online + degraded + offline
    if (total == 0) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (darkTheme) Color(0xFF2A3441) else Color(0xFFE0E0E0))
    ) {
        if (online > 0) {
            Box(
                modifier = Modifier
                    .weight(online.toFloat())
                    .fillMaxHeight()
                    .background(InnovexiaColors.Success)
            )
        }
        if (degraded > 0) {
            Box(
                modifier = Modifier
                    .weight(degraded.toFloat())
                    .fillMaxHeight()
                    .background(InnovexiaColors.WarningAlt)
            )
        }
        if (offline > 0) {
            Box(
                modifier = Modifier
                    .weight(offline.toFloat())
                    .fillMaxHeight()
                    .background(InnovexiaColors.ErrorAlt)
            )
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color,
    darkTheme: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
        )
    }
}

@Composable
private fun ConnectivityWarningBanner(darkTheme: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = InnovexiaColors.WarningAlt.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = InnovexiaColors.WarningAlt
            )
            Text(
                text = "No Internet Connection",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun IncidentCard(incident: IncidentEntity, darkTheme: Boolean) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = incident.serviceId,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                    )
                    Text(
                        text = formatDuration(System.currentTimeMillis() - incident.startedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                }

                StatusPill(incident.status, darkTheme)
            }

            if (expanded) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = incident.impact,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(
    check: HealthCheck,
    onCheck: () -> Unit,
    darkTheme: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Status indicator with larger size
                    HealthStateIndicator(check.status, size = 14.dp)

                    Column(modifier = Modifier.weight(1f)) {
                        // Service name
                        Text(
                            text = check.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                        )

                        // Status details row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Latency badge
                            if (check.latencyMs != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(InnovexiaColors.BlueAccent.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${check.latencyMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = InnovexiaColors.BlueAccent
                                    )
                                }
                            }

                            // Status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(check.status.color().copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = check.status.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = check.status.color()
                                )
                            }

                            // Notes preview
                            if (!check.notes.isNullOrBlank() && !expanded) {
                                Text(
                                    text = check.notes.take(20) + if (check.notes.length > 20) "..." else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Test button
                IconButton(onClick = onCheck) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Run check",
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                }
            }

            // Expanded details
            if (expanded) {
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = if (darkTheme) Color(0xFF2A3441) else Color(0xFFE0E0E0)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Service ID
                    DetailRow(
                        label = "Service ID",
                        value = check.id,
                        icon = "🔑",
                        darkTheme = darkTheme
                    )

                    // Status
                    DetailRow(
                        label = "Status",
                        value = check.status.name,
                        icon = when (check.status) {
                            HealthState.ONLINE -> "✅"
                            HealthState.DEGRADED -> "⚠️"
                            HealthState.OFFLINE -> "❌"
                            HealthState.UNKNOWN -> "❓"
                        },
                        darkTheme = darkTheme
                    )

                    // Latency
                    if (check.latencyMs != null) {
                        DetailRow(
                            label = "Response Time",
                            value = "${check.latencyMs}ms",
                            icon = "⚡",
                            darkTheme = darkTheme
                        )
                    }

                    // Version
                    if (check.version != null) {
                        DetailRow(
                            label = "Version",
                            value = check.version,
                            icon = "📦",
                            darkTheme = darkTheme
                        )
                    }

                    // Notes
                    if (!check.notes.isNullOrBlank()) {
                        DetailRow(
                            label = "Details",
                            value = check.notes,
                            icon = "📝",
                            darkTheme = darkTheme
                        )
                    }

                    // Last checked
                    DetailRow(
                        label = "Last Checked",
                        value = formatTimestamp(check.lastCheckedAt),
                        icon = "🕒",
                        darkTheme = darkTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: String,
    darkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (darkTheme) Color(0xFF2A3441) else Color(0xFFEEEEEE))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyMedium
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HealthStateIndicator(state: HealthState, size: androidx.compose.ui.unit.Dp = 12.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(state.color())
    )
}

@Composable
private fun StatusPill(status: String, darkTheme: Boolean) {
    val backgroundColor = when (status) {
        "Open" -> InnovexiaColors.ErrorAlt.copy(alpha = 0.2f)
        "Monitoring" -> InnovexiaColors.WarningAlt.copy(alpha = 0.2f)
        "Resolved" -> InnovexiaColors.Success.copy(alpha = 0.2f)
        else -> if (darkTheme) Color(0xFF2A3441) else Color(0xFFE0E0E0)
    }

    val textColor = when (status) {
        "Open" -> InnovexiaColors.ErrorAlt
        "Monitoring" -> InnovexiaColors.WarningAlt
        "Resolved" -> InnovexiaColors.Success
        else -> if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, darkTheme: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper functions
private fun HealthState.displayName() = when (this) {
    HealthState.ONLINE -> "All Systems Operational"
    HealthState.DEGRADED -> "Partial Outage"
    HealthState.OFFLINE -> "Service Outage"
    HealthState.UNKNOWN -> "Status Unknown"
}

private fun HealthState.color() = when (this) {
    HealthState.ONLINE -> InnovexiaColors.Success
    HealthState.DEGRADED -> InnovexiaColors.WarningAlt
    HealthState.OFFLINE -> InnovexiaColors.ErrorAlt
    HealthState.UNKNOWN -> Color.Gray
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun formatDuration(durationMs: Long): String {
    val minutes = durationMs / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h ago"
        hours > 0 -> "${hours}h ${minutes % 60}m ago"
        minutes > 0 -> "${minutes}m ago"
        else -> "Just now"
    }
}
