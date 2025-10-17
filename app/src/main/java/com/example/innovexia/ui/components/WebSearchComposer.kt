package com.example.innovexia.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * WebSearchComposer - Material 3 web search input
 *
 * Material 3 Features:
 * - M3 secondary color scheme for search mode
 * - Animated rotating search icon
 * - Animated grounding chip with pulse effect
 * - Enhanced focus states and animations
 * - Spring bounce for send button
 * - Pulsing stop button during streaming
 */
@Composable
fun WebSearchComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isStreaming: Boolean = false,
    onStopStreaming: () -> Unit = {},
    onDisableGrounding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }

    // Auto-focus when composer appears
    LaunchedEffect(Unit) {
        delay(100) // Small delay to ensure layout is ready
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Silently ignore focus request failures
        }
    }

    // Focus state for elevation animation
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Focus elevation animation
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) 1.01f else 1f,
        animationSpec = tween(
            durationMillis = ComposerTokens.Motion.DurationMedium,
            easing = ComposerTokens.Motion.EasingStandard
        ),
        label = "focus_scale"
    )

    // Dark theme colors matching side menu - blue accent for search mode
    val containerColor = Color(0xFF1E2329) // Slightly lighter dark surface for search
    val borderColor = Color(0xFF4A90E2).copy(alpha = if (isFocused) 0.6f else 0.4f) // Blue accent border
    val textColor = Color(0xFFE5EAF0) // Light text
    val placeholderColor = Color(0xFF9AA6B2).copy(alpha = 0.7f) // Muted placeholder

    // Use same dimensions as ChatComposerV3
    val d = ComposerTokens.Dimen

    Surface(
        shape = RoundedCornerShape(d.Radius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isFocused) 2.dp else 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .heightIn(min = d.Height)
            .imePadding()
            .navigationBarsPadding()
            .widthIn(max = d.MaxWidth)
            .scale(focusScale)
    ) {
        Row(
            Modifier.padding(horizontal = d.PadH, vertical = d.PadV),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated search icon with rotation
            val infiniteTransition = rememberInfiniteTransition(label = "search_rotation")
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "search_rotate"
            )

            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Web search",
                tint = Color(0xFF4A90E2), // Blue accent
                modifier = Modifier
                    .size(d.IconSize)
                    .rotate(rotation * 0.05f) // Subtle rotation effect
            )
            Spacer(Modifier.width(8.dp))

            // Simple X button to exit grounding mode
            FilledTonalIconButton(
                onClick = onDisableGrounding,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFF2A323B).copy(alpha = 0.5f),
                    contentColor = Color(0xFF9AA6B2)
                ),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Exit web search mode",
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Text input area
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF4A90E2)), // Blue accent cursor
                    maxLines = 6,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .focusRequester(focusRequester)
                ) { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Search the web…",
                                color = placeholderColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        innerTextField()
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Right side: Send OR Stop button with M3 animations
            val canSend = value.isNotBlank()
            val buttonState = when {
                isStreaming -> "stop"
                canSend -> "send"
                else -> "none"
            }

            AnimatedContent(
                targetState = buttonState,
                transitionSpec = {
                    (fadeIn(
                        animationSpec = tween(
                            durationMillis = ComposerTokens.Motion.DurationMedium,
                            easing = ComposerTokens.Motion.EasingEnter
                        )
                    ) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )).togetherWith(
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = ComposerTokens.Motion.DurationShort,
                                easing = ComposerTokens.Motion.EasingExit
                            )
                        ) + scaleOut(
                            targetScale = 0.7f,
                            animationSpec = tween(
                                durationMillis = ComposerTokens.Motion.DurationShort,
                                easing = ComposerTokens.Motion.EasingExit
                            )
                        )
                    )
                },
                label = "action_button_animation"
            ) { targetState ->
                when (targetState) {
                    "stop" -> {
                        // Pulsing stop button with M3 error color
                        val infiniteTransition = rememberInfiniteTransition(label = "stop_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "stop_pulse_scale"
                        )

                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.8f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "stop_pulse_alpha"
                        )

                        FilledIconButton(
                            onClick = onStopStreaming,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colorScheme.error
                            ),
                            modifier = Modifier
                                .size(d.IconFrame)
                                .scale(pulseScale)
                                .graphicsLayer { alpha = pulseAlpha }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "Stop streaming",
                                tint = colorScheme.onError,
                                modifier = Modifier.size(d.IconSize)
                            )
                        }
                    }
                    "send" -> {
                        // Search button with spring bounce animation
                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "send_scale"
                        )

                        FilledIconButton(
                            onClick = onSend,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF4A90E2), // Blue accent
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(d.IconFrame)
                                .scale(scale)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(d.IconSize)
                            )
                        }
                    }
                    else -> {
                        // Empty space when no text
                        Spacer(modifier = Modifier.size(d.IconFrame))
                    }
                }
            }
        }
    }
}
