package com.example.innovexia.ui.sheets.profile.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.data.models.PlanLimits
import com.example.innovexia.data.models.SubscriptionPlan
import com.example.innovexia.data.models.UsageData
import com.example.innovexia.data.models.DailyUsage
import com.example.innovexia.ui.glass.GlassButton
import com.example.innovexia.ui.glass.GlassButtonStyle
import com.example.innovexia.ui.theme.InnovexiaColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Usage tab showing real-time usage statistics
 */
@Composable
fun UsageTab(
    currentUsage: UsageData?,
    todayUsage: DailyUsage?,
    plan: SubscriptionPlan,
    planLimits: PlanLimits,
    usagePercent: Float,
    burstCount: Int = 0,
    onRefresh: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val textPrimary = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
    val textSecondary = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary
    val surface = if (darkTheme) InnovexiaColors.DarkSurface else InnovexiaColors.LightSurface

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Usage & Limits",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = textPrimary
            )

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh usage",
                    tint = InnovexiaColors.BlueAccent
                )
            }
        }

        // Current plan badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Plan:",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary
            )
            Surface(
                color = InnovexiaColors.BlueAccent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = plan.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = InnovexiaColors.BlueAccent
                )
            }
        }

        // Monthly usage donut chart
        UsageDonutCard(
            usagePercent = usagePercent,
            tokensUsed = currentUsage?.totalTokens ?: 0L,
            tokensLimit = planLimits.tokensPerWindow,
            periodId = currentUsage?.periodId ?: "",
            surface = surface,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        // Today's usage
        TodayUsageCard(
            todayUsage = todayUsage,
            dailyLimit = planLimits.tokensPerWindow / 30,
            surface = surface,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        // Burst rate limit
        BurstLimitCard(
            burstCount = burstCount,
            burstLimit = planLimits.burstRequestsPerMinute,
            surface = surface,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        // Details section
        UsageDetailsCard(
            currentUsage = currentUsage,
            planLimits = planLimits,
            surface = surface,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        // Upgrade CTA if approaching limit
        if (usagePercent >= 90f) {
            UpgradePrompt(
                usagePercent = usagePercent,
                plan = plan,
                onUpgrade = onUpgrade,
                darkTheme = darkTheme
            )
        }
    }
}

/**
 * Donut chart showing monthly usage percentage
 */
@Composable
private fun UsageDonutCard(
    usagePercent: Float,
    tokensUsed: Long,
    tokensLimit: Long,
    periodId: String,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Monthly Usage",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textPrimary
            )

            // Donut chart
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                DonutChart(
                    percentage = usagePercent,
                    modifier = Modifier.size(180.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${usagePercent.toInt()}%",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                    Text(
                        text = "used",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }

            // Token counts
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${formatNumber(tokensUsed)} / ${formatNumber(tokensLimit)} tokens",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = textPrimary
                )
                Text(
                    text = "Resets ${formatPeriodEnd(periodId)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondary
                )
            }
        }
    }
}

/**
 * Donut chart visualization
 */
@Composable
private fun DonutChart(
    percentage: Float,
    modifier: Modifier = Modifier
) {
    val colorStops = arrayOf(
        0.0f to InnovexiaColors.BlueAccent,
        0.5f to InnovexiaColors.PurpleAccent,
        1.0f to InnovexiaColors.GoldDim
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 20.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val topLeft = Offset(
            x = (size.width - radius * 2) / 2,
            y = (size.height - radius * 2) / 2
        )

        // Background arc
        drawArc(
            color = Color.Gray.copy(alpha = 0.2f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress arc with gradient
        val sweepAngle = (percentage / 100f) * 360f
        drawArc(
            brush = Brush.sweepGradient(colorStops = colorStops),
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Today's usage card
 */
@Composable
private fun TodayUsageCard(
    todayUsage: DailyUsage?,
    dailyLimit: Long,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textPrimary
            )

            val used = todayUsage?.totalTokens ?: 0L
            val percent = if (dailyLimit > 0) (used.toFloat() / dailyLimit * 100f).coerceIn(0f, 100f) else 0f

            UsageProgressBar(
                label = "Tokens",
                used = used,
                limit = dailyLimit,
                percent = percent,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Requests", "${todayUsage?.requests ?: 0}", textPrimary, textSecondary)
                StatItem("Input", formatNumber(todayUsage?.tokensIn ?: 0L), textPrimary, textSecondary)
                StatItem("Output", formatNumber(todayUsage?.tokensOut ?: 0L), textPrimary, textSecondary)
            }
        }
    }
}

/**
 * Burst rate limit card
 */
@Composable
private fun BurstLimitCard(
    burstCount: Int,
    burstLimit: Int,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Burst Rate (per minute)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = textPrimary
                )
                Text(
                    text = "$burstCount / $burstLimit",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (burstCount >= burstLimit * 0.9f) InnovexiaColors.ErrorRed else textPrimary
                )
            }

            val percent = if (burstLimit > 0) (burstCount.toFloat() / burstLimit * 100f).coerceIn(0f, 100f) else 0f
            LinearProgressIndicator(
                progress = percent / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (percent >= 90f) InnovexiaColors.ErrorRed else InnovexiaColors.BlueAccent,
                trackColor = Color.Gray.copy(alpha = 0.2f)
            )

            if (burstCount >= burstLimit) {
                Text(
                    text = "⏱️ Burst limit reached. Wait ~60 seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InnovexiaColors.ErrorRed
                )
            }
        }
    }
}

/**
 * Usage details card
 */
@Composable
private fun UsageDetailsCard(
    currentUsage: UsageData?,
    planLimits: PlanLimits,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textPrimary
            )

            DetailRow("Input tokens", formatNumber(currentUsage?.tokensIn ?: 0L), textPrimary, textSecondary)
            DetailRow("Output tokens", formatNumber(currentUsage?.tokensOut ?: 0L), textPrimary, textSecondary)
            DetailRow("Total requests", "${currentUsage?.requests ?: 0}", textPrimary, textSecondary)
            DetailRow("Attachments", formatBytes(currentUsage?.attachmentsBytes ?: 0L), textPrimary, textSecondary)
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            DetailRow("Window limit", formatNumber(planLimits.tokensPerWindow) + " tokens/${planLimits.windowDurationHours}hr", textPrimary, textSecondary)
            DetailRow("Burst limit", "${planLimits.burstRequestsPerMinute} req/min", textPrimary, textSecondary)
            DetailRow("Max upload", "${planLimits.maxUploadMB} MB", textPrimary, textSecondary)
        }
    }
}

/**
 * Upgrade prompt when approaching limit
 */
@Composable
private fun UpgradePrompt(
    usagePercent: Float,
    plan: SubscriptionPlan,
    onUpgrade: () -> Unit,
    darkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(InnovexiaColors.ErrorRed.copy(alpha = 0.1f))
            .padding(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (usagePercent >= 100f) "⚠️ Limit Exceeded" else "⚠️ Approaching Limit",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = InnovexiaColors.ErrorRed
            )

            Text(
                text = "You've used ${usagePercent.toInt()}% of your monthly quota. Upgrade to continue uninterrupted.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary
            )

            if (plan != SubscriptionPlan.MASTER) {
                GlassButton(
                    text = "Upgrade Now",
                    onClick = onUpgrade,
                    style = GlassButtonStyle.Primary,
                    modifier = Modifier.fillMaxWidth(),
                    darkTheme = darkTheme
                )
            }
        }
    }
}

// ==================== Helper Composables ====================

@Composable
private fun UsageProgressBar(
    label: String,
    used: Long,
    limit: Long,
    percent: Float,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary
            )
            Text(
                text = "${formatNumber(used)} / ${formatNumber(limit)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textPrimary
            )
        }

        LinearProgressIndicator(
            progress = percent / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (percent >= 90f) InnovexiaColors.ErrorRed else InnovexiaColors.BlueAccent,
            trackColor = Color.Gray.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = textPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = textSecondary
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = textPrimary
        )
    }
}

// ==================== Helper Functions ====================

private fun formatNumber(num: Long): String {
    return when {
        num >= 1_000_000 -> String.format("%.1fM", num / 1_000_000.0)
        num >= 1_000 -> String.format("%.1fK", num / 1_000.0)
        else -> num.toString()
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format("%.2f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}

private fun formatPeriodEnd(periodId: String): String {
    return try {
        val parts = periodId.split("-")
        if (parts.size != 2) return "soon"
        val year = parts[0].toInt()
        val month = parts[1].toInt()

        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        calendar.add(Calendar.MONTH, 1)

        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        sdf.format(calendar.time)
    } catch (e: Exception) {
        "soon"
    }
}
