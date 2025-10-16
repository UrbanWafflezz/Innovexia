package com.example.innovexia.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.subscriptions.mock.UsageState
import kotlinx.coroutines.delay

/**
 * Rate limit countdown composer
 * Replaces the normal composer when rate limit is hit
 * Shows real-time countdown until reset
 */
@Composable
fun RateLimitComposer(
    usageState: UsageState,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    isGuest: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val containerColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF5F5F7)
    val borderColor = if (isDark) Color(0xFF38383A) else Color(0xFFE0E0E2)
    val textColor = if (isDark) Color(0xFFE5E5E7) else Color(0xFF1C1C1E)
    val accentColor = if (isDark) Color(0xFFE8A84D) else Color(0xFFD89438) // Muted gold
    val subtleColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6C6C70)
    val buttonBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE8E8EA)
    val buttonText = if (isDark) Color(0xFFE8A84D) else Color(0xFFD89438)

    // Real-time countdown
    var timeRemaining by remember { mutableStateOf(usageState.timeUntilReset) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Update every second
            // Recalculate time remaining
            val ms = usageState.window.timeRemainingMs()
            val hours = ms / (60 * 60 * 1000)
            val minutes = (ms % (60 * 60 * 1000)) / (60 * 1000)
            val seconds = (ms % (60 * 1000)) / 1000

            timeRemaining = when {
                hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                seconds > 0 -> "${seconds}s"
                else -> "Resetting..."
            }
        }
    }

    // Pulsing animation for the clock icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ComposerTokens.Dimen.Height)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(ComposerTokens.Dimen.Radius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Icon + Message info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Pulsing clock icon
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "Rate limit",
                    tint = accentColor,
                    modifier = Modifier
                        .size(28.dp)
                        .alpha(0.9f)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Rate limit reached",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        text = "Resets in $timeRemaining",
                        fontSize = 13.sp,
                        color = subtleColor,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Right side: Upgrade button (hidden for guest users)
            if (!isGuest) {
                Button(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = buttonText
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Upgrade,
                            contentDescription = "Upgrade",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Upgrade",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
