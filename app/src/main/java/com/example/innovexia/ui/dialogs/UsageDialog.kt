package com.example.innovexia.ui.dialogs

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
import androidx.compose.ui.Modifier
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
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        // Current window stats
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F1117),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = "This Window",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0C76A).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "5-hour window",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF0C76A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large percentage
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${usageState.progressPercent.toInt()}%",
                            style = androidx.compose.material3.MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = when {
                                usageState.isLimitReached -> Color(0xFFFF6B6B)
                                usageState.isApproachingLimit -> Color(0xFFFFB020)
                                else -> Color.White
                            }
                        )
                        Text(
                            text = "of limit used",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress bar
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { usageState.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when {
                        usageState.isLimitReached -> Color(0xFFFF6B6B)
                        usageState.isApproachingLimit -> Color(0xFFFFB020)
                        else -> Color(0xFF6EA8FF)
                    },
                    trackColor = Color(0xFF1A1D23),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Reset info
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A1D23).copy(alpha = 0.5f)
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
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Detailed breakdown
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F1117),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Detailed Breakdown",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Messages
                UsageMetricRow(
                    icon = "💬",
                    label = "Messages",
                    current = usageState.messagesUsed.toString(),
                    limit = usageState.messagesLimit.toString(),
                    remaining = usageState.messagesRemaining.toString()
                )

                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0xFF2D3139).copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                // Total tokens
                UsageMetricRow(
                    icon = "🔢",
                    label = "Total Tokens",
                    current = usageState.formatTokensUsed(),
                    limit = usageState.formatTokensLimit(),
                    remaining = usageState.formatTokensRemaining()
                )

                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(color = Color(0xFF2D3139).copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(12.dp))

                // Plan info
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
                            text = "📊",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Current Plan",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Text(
                        text = usageState.plan.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = com.example.innovexia.ui.subscriptions.TierTokens.getTierColor(usageState.plan)
                    )
                }
            }
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
