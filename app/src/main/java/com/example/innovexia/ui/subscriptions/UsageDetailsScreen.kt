package com.example.innovexia.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.innovexia.subscriptions.mock.UsageVM

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageDetailsScreen(
    usageVM: UsageVM,
    onBack: () -> Unit
) {
    val usageState by usageVM.usageState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Usage & Limits",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TierTokens.Surface
                )
            )
        },
        containerColor = TierTokens.Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plan badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TierTokens.Card,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current Plan",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = usageState.plan.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TierTokens.getTierColor(usageState.plan)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = TierTokens.getTierColor(usageState.plan),
                        modifier = Modifier.size(12.dp)
                    ) {}
                }
            }

            // Current window stats
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TierTokens.Card,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "This Window",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TierTokens.Master.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "5-hour window",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = TierTokens.Master
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress circle/bar
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Large percentage
                            Text(
                                text = "${usageState.progressPercent.toInt()}%",
                                style = MaterialTheme.typography.displayMedium.copy(
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
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { usageState.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = when {
                            usageState.isLimitReached -> Color(0xFFFF6B6B)
                            usageState.isApproachingLimit -> Color(0xFFFFB020)
                            else -> TierTokens.Plus
                        },
                        trackColor = TierTokens.Surface,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Reset info
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TierTokens.Surface.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱️ Resets in",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = usageState.timeUntilReset,
                                style = MaterialTheme.typography.titleSmall.copy(
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
                color = TierTokens.Card,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Detailed Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(
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
                    HorizontalDivider(color = TierTokens.Border)
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
                    HorizontalDivider(color = TierTokens.Border)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Input tokens
                    UsageMetricRow(
                        icon = "📥",
                        label = "Input Tokens",
                        current = usageState.formatTokensIn(),
                        limit = null,
                        remaining = null
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = TierTokens.Border)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Output tokens
                    UsageMetricRow(
                        icon = "📤",
                        label = "Output Tokens",
                        current = usageState.formatTokensOut(),
                        limit = null,
                        remaining = null
                    )
                }
            }

            // Info card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = TierTokens.Plus.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, TierTokens.Plus.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ About Usage Windows",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TierTokens.Plus
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Usage limits reset every 5 hours from your first message in each window. This gives you flexibility while preventing abuse.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = icon,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            if (limit != null) {
                Text(
                    text = "$current / $limit",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            } else {
                Text(
                    text = current,
                    style = MaterialTheme.typography.titleSmall.copy(
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
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
