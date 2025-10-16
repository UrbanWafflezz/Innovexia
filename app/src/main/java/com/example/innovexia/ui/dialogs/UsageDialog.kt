package com.example.innovexia.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.innovexia.subscriptions.mock.UsageVM
import com.example.innovexia.subscriptions.mock.EntitlementsVM
import com.example.innovexia.ui.subscriptions.UsageDetailsScreen
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Standalone Usage Dialog
 * Shows usage statistics and limits with new usage tracking system
 */
@Composable
fun UsageDialog(
    onDismiss: () -> Unit,
    usageVM: UsageVM,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val usageState by usageVM.usageState.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (darkTheme) Color(0xFF1A1D23) else Color.White,
            tonalElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                if (darkTheme) Color(0xFF2D3139).copy(alpha = 0.6f) else Color(0xFFE7EDF5).copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with close button
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Usage & Limit",
                        color = if (darkTheme) Color.White else Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = if (darkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                        )
                    }
                }

                // Use the new UsageDetailsScreen content (without scaffold)
                UsageDetailsContent(
                    usageState = usageState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

/**
 * Content from UsageDetailsScreen without Scaffold wrapper
 * This allows embedding in a dialog
 */
@Composable
private fun UsageDetailsContent(
    usageState: com.example.innovexia.subscriptions.mock.UsageState,
    modifier: Modifier = Modifier
) {
    var isWindowExpanded by remember { mutableStateOf(false) }

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        // Compact "This Window" Card with expand option
        item {
            androidx.compose.material3.Card(
                onClick = { isWindowExpanded = !isWindowExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = Color(0xFF0F1117)
                ),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    // Compact header
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "This Window",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            androidx.compose.material3.AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = "5h",
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFF0C76A).copy(alpha = 0.15f),
                                    labelColor = Color(0xFFF0C76A)
                                ),
                                border = null,
                                modifier = Modifier.height(24.dp)
                            )
                        }

                        Text(
                            text = "${usageState.progressPercent.toInt()}%",
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = when {
                                usageState.isLimitReached -> Color(0xFFFF6B6B)
                                usageState.isApproachingLimit -> Color(0xFFFFB020)
                                else -> Color(0xFF6EA8FF)
                            }
                        )
                    }

                    // Compact progress bar
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { usageState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            usageState.isLimitReached -> Color(0xFFFF6B6B)
                            usageState.isApproachingLimit -> Color(0xFFFFB020)
                            else -> Color(0xFF6EA8FF)
                        },
                        trackColor = Color(0xFF1A1D23),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    // Expandable details with animation
                    AnimatedVisibility(
                        visible = isWindowExpanded,
                        enter = expandVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1A1D23).copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏱️ Resets in",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = usageState.timeUntilReset,
                                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color(0xFF6EA8FF)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Detailed breakdown - More Compact
        item {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = Color(0xFF0F1117)
                ),
                elevation = androidx.compose.material3.CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Detailed Breakdown",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    // Messages metric
                    MetricCard(
                        icon = "💬",
                        label = "Messages",
                        current = usageState.messagesUsed.toString(),
                        limit = usageState.messagesLimit.toString(),
                        remaining = usageState.messagesRemaining.toString(),
                        usageState = usageState
                    )

                    // Total tokens metric
                    MetricCard(
                        icon = "🔢",
                        label = "Total Tokens",
                        current = usageState.formatTokensUsed(),
                        limit = usageState.formatTokensLimit(),
                        remaining = usageState.formatTokensRemaining(),
                        usageState = usageState
                    )

                    // Current Plan - Inline display
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = com.example.innovexia.ui.subscriptions.TierTokens.getTierColor(usageState.plan).copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            com.example.innovexia.ui.subscriptions.TierTokens.getTierColor(usageState.plan).copy(alpha = 0.25f)
                        )
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Row(
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Current Plan",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }

                            androidx.compose.material3.AssistChip(
                                onClick = { },
                                label = {
                                    Text(
                                        text = usageState.plan.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                },
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = com.example.innovexia.ui.subscriptions.TierTokens.getTierColor(usageState.plan).copy(alpha = 0.2f),
                                    labelColor = com.example.innovexia.ui.subscriptions.TierTokens.getTierColor(usageState.plan)
                                ),
                                border = null,
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // Plan Limits - Unified Seamless Section with Animation
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Plan Limits",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Memory Entries
                val memoryProgress = if (usageState.memoryLimit != null && usageState.memoryLimit > 0) {
                    (usageState.memoryEntriesCount.toFloat() / usageState.memoryLimit.toFloat())
                } else {
                    0f
                }

                LimitRow(
                    icon = "🧠",
                    iconColor = Color(0xFF8B5CF6),
                    label = "Memory Entries",
                    value = if (usageState.memoryLimit == null) "Unlimited (${usageState.memoryEntriesCount})"
                           else "${usageState.memoryEntriesCount} / ${usageState.memoryLimit}",
                    showProgress = usageState.memoryLimit != null,
                    progress = memoryProgress,
                    progressColor = Color(0xFF8B5CF6)
                )

                // Sources
                val sourcesProgress = if (usageState.sourcesLimit > 0) {
                    (usageState.sourcesCount.toFloat() / usageState.sourcesLimit.toFloat())
                } else {
                    0f
                }

                LimitRow(
                    icon = "📁",
                    iconColor = Color(0xFF6EA8FF),
                    label = "Sources",
                    value = "${usageState.sourcesCount} / ${usageState.sourcesLimit}",
                    showProgress = true,
                    progress = sourcesProgress,
                    progressColor = Color(0xFF6EA8FF)
                )

                // Max Upload Size
                LimitRow(
                    icon = "📤",
                    iconColor = Color(0xFFF0C76A),
                    label = "Max Upload Size",
                    value = "${usageState.uploadLimitMB}MB per file",
                    showProgress = false,
                    progress = 0f,
                    progressColor = Color(0xFFF0C76A)
                )
            }
        }
    }
}

/**
 * Seamless Limit Row with Optional Progress Bar
 */
@Composable
private fun LimitRow(
    icon: String,
    iconColor: Color,
    label: String,
    value: String,
    showProgress: Boolean,
    progress: Float,
    progressColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                // Icon with colored background
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = iconColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(0.dp)
                ) {
                    Text(
                        text = icon,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(6.dp)
                    )
                }

                Text(
                    text = label,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Text(
                text = value,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (value.startsWith("Unlimited")) Color(0xFFF0C76A) else Color.White.copy(alpha = 0.85f)
            )
        }

        // Optional progress bar
        if (showProgress) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    progress >= 0.9f -> Color(0xFFFF6B6B)
                    progress >= 0.75f -> Color(0xFFFFB020)
                    else -> progressColor
                },
                trackColor = progressColor.copy(alpha = 0.15f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/**
 * Material Design 3 Metric Card with Progress Bar
 */
@Composable
private fun MetricCard(
    icon: String,
    label: String,
    current: String,
    limit: String,
    remaining: String,
    usageState: com.example.innovexia.subscriptions.mock.UsageState
) {
    // Calculate progress percentage based on label
    val progress = when (label) {
        "Messages" -> {
            val used = usageState.messagesUsed.toFloat()
            val max = usageState.messagesLimit.toFloat()
            if (max > 0) (used / max) else 0f
        }
        "Total Tokens" -> {
            val used = usageState.tokensUsed.toFloat()
            val max = usageState.tokensLimit.toFloat()
            if (max > 0) (used / max) else 0f
        }
        else -> 0f
    }

    val progressPercent = (progress * 100f).coerceIn(0f, 100f)

    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1D23).copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = icon,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = label,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Text(
                    text = "$current / $limit",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            // Progress bar
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    progressPercent >= 90f -> Color(0xFFFF6B6B)
                    progressPercent >= 75f -> Color(0xFFFFB020)
                    else -> Color(0xFF6EA8FF)
                },
                trackColor = Color(0xFF0F1117),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )

            Text(
                text = "$remaining remaining",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = Color(0xFF6EA8FF).copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun UsageMetricRow(
    icon: String,
    label: String,
    current: String,
    limit: String?,
    remaining: String?
) {
    Column {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = icon,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                )
                Text(
                    text = label,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            if (limit != null) {
                Text(
                    text = "$current / $limit",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            } else {
                Text(
                    text = current,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }

        if (remaining != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$remaining remaining",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
