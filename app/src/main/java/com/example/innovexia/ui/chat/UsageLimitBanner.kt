package com.example.innovexia.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.innovexia.subscriptions.mock.UsageState

/**
 * Banner that displays usage limit warnings and errors
 */
@Composable
fun UsageLimitBanner(
    usageState: UsageState,
    modifier: Modifier = Modifier
) {
    if (usageState.isLimitReached) {
        // Limit reached - blocking state
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFF6B6B),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Usage Limit Reached",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append("You've used ")
                            append("${usageState.messagesUsed}/${usageState.messagesLimit} messages")
                            append(" or ")
                            append("${usageState.formatTokensUsed()}/${usageState.formatTokensLimit()} tokens")
                            append(". Resets in ${usageState.timeUntilReset}.")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.95f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.4
                    )
                }
            }
        }
    } else if (usageState.isApproachingLimit) {
        // Approaching limit - warning state
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFB020),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Approaching Limit",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${usageState.messagesRemaining} messages, ${usageState.formatTokensRemaining()} tokens left. Resets in ${usageState.timeUntilReset}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }
        }
    }
}

/**
 * Compact usage info for chat header/status bar
 */
@Composable
fun UsageStatusChip(
    usageState: UsageState,
    modifier: Modifier = Modifier
) {
    val color = when {
        usageState.isLimitReached -> Color(0xFFFF6B6B)
        usageState.isApproachingLimit -> Color(0xFFFFB020)
        else -> Color(0xFF6EA8FF)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = when {
                    usageState.isLimitReached -> "⏸️"
                    usageState.isApproachingLimit -> "⚠️"
                    else -> "📊"
                },
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "${usageState.messagesUsed}/${usageState.messagesLimit}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            )
        }
    }
}
