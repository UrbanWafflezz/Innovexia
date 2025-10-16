package com.example.innovexia.ui.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.innovexia.subscriptions.mock.UsageState

@Composable
fun UsagePreview(
    usageState: UsageState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(TierTokens.Radius),
        colors = CardDefaults.cardColors(
            containerColor = TierTokens.Card
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, TierTokens.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Usage",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TierTokens.Master.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "This period",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TierTokens.Master
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Usage stats
            UsageStat(label = "Messages", value = "${usageState.messagesUsed}/${usageState.messagesLimit}")
            Spacer(modifier = Modifier.height(8.dp))
            UsageStat(label = "Tokens", value = "${usageState.formatTokensUsed()}/${usageState.formatTokensLimit()}")
            Spacer(modifier = Modifier.height(8.dp))
            UsageStat(label = "Resets in", value = usageState.timeUntilReset)

            // Progress bar
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { usageState.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = when {
                    usageState.isLimitReached -> Color(0xFFFF6B6B)
                    usageState.isApproachingLimit -> Color(0xFFFFB020)
                    else -> TierTokens.Plus
                },
                trackColor = TierTokens.Surface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CTA Button
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TierTokens.Surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Open Usage",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun UsageStat(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color.White.copy(alpha = 0.7f)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )
    }
}
