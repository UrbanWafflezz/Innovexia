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
 * Rate limit countdown composer - Material 3 Design
 *
 * Material 3 Features:
 * - M3 color scheme with warning color
 * - Enhanced pulsing clock icon with color shift
 * - Animated countdown with flip transitions
 * - M3 elevated button for upgrade
 * - Smooth entrance animation
 */
@Composable
fun RateLimitComposer(
    usageState: UsageState,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    isGuest: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme

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

    // Pulsing animation for the clock icon with color shift
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ComposerTokens.Dimen.Height)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(ComposerTokens.Dimen.Radius),
        color = Color(0xFF1E2329), // Dark surface matching theme
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)), // Red error border
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Animated icon + Message info
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Enhanced pulsing clock icon with color shift
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "Rate limit",
                    tint = Color(0xFFEF4444), // Red error color
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                            alpha = pulseAlpha
                        }
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Rate limit reached",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE5EAF0) // Light text
                    )

                    // Animated countdown with flip effect
                    AnimatedContent(
                        targetState = timeRemaining,
                        transitionSpec = {
                            (fadeIn(
                                animationSpec = tween(
                                    durationMillis = 200,
                                    easing = LinearEasing
                                )
                            ) + slideInVertically(
                                animationSpec = tween(200),
                                initialOffsetY = { it / 2 }
                            )).togetherWith(
                                fadeOut(
                                    animationSpec = tween(100)
                                ) + slideOutVertically(
                                    animationSpec = tween(100),
                                    targetOffsetY = { -it / 2 }
                                )
                            )
                        },
                        label = "countdown_animation"
                    ) { time ->
                        Text(
                            text = "Resets in $time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9AA6B2), // Muted text
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // Right side: M3 Elevated button (hidden for guest users)
            AnimatedVisibility(
                visible = !isGuest,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = ComposerTokens.Motion.DurationMedium,
                        easing = ComposerTokens.Motion.EasingEnter
                    )
                ) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = ComposerTokens.Motion.DurationShort,
                        easing = ComposerTokens.Motion.EasingExit
                    )
                ) + scaleOut(
                    animationSpec = tween(
                        durationMillis = ComposerTokens.Motion.DurationShort,
                        easing = ComposerTokens.Motion.EasingExit
                    )
                )
            ) {
                ElevatedButton(
                    onClick = onUpgrade,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFFF2C94C), // Gold accent
                        contentColor = Color(0xFF0F0F0F) // Dark text on gold
                    ),
                    elevation = ButtonDefaults.elevatedButtonElevation(
                        defaultElevation = 2.dp,
                        pressedElevation = 4.dp
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Upgrade,
                            contentDescription = "Upgrade",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Upgrade",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
