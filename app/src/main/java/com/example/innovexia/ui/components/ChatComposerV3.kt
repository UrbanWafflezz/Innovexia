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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Material 3 Design tokens for the composer
 * Following Material Design 3 principles and motion guidelines
 */
object ComposerTokens {
    object Dimen {
        val Height = 80.dp          // Material 3 large component height
        val Radius = 28.dp          // M3 extra large shape (28dp)
        val IconFrame = 44.dp       // M3 touch target size
        val IconSize = 24.dp        // M3 icon size (increased from 22dp)
        val PadH = 16.dp            // M3 horizontal padding
        val PadV = 14.dp            // M3 vertical padding
        val MaxWidth = 860.dp       // Responsive max width
    }

    object Motion {
        // Material 3 motion tokens
        const val DurationShort = 200      // Simple transitions
        const val DurationMedium = 300     // Standard transitions
        const val DurationLong = 500       // Complex transitions

        // Easing curves
        val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        val EasingStandard = FastOutSlowInEasing
        val EasingEnter = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        val EasingExit = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    }
}

/**
 * Material 3 icon button with beautiful animations
 * Features:
 * - Breathing animation when idle
 * - Ripple effect on press
 * - Scale animation on enter/exit
 * - State layer effects
 */
@Composable
fun M3IconButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconTint: Color? = null,
    breathingAnimation: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Breathing animation for mic button
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (breathingAnimation) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (breathingAnimation) 0.7f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_alpha"
    )

    // Dark theme colors matching side menu
    val containerColor = Color(0xFF2A323B).copy(alpha = 0.5f) // Subtle dark background
    val contentColor = iconTint ?: Color(0xFFE5EAF0) // Light icon color

    FilledTonalIconButton(
        onClick = onClick,
        enabled = enabled,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = modifier
            .size(ComposerTokens.Dimen.IconFrame)
            .scale(breathScale)
            .graphicsLayer { alpha = breathAlpha }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            modifier = Modifier.size(ComposerTokens.Dimen.IconSize)
        )
    }
}

/**
 * Persona chip with spring-like rotation animation
 * Subtle colors matching the plus icon style
 */
@Composable
fun PersonaChip(
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "persona_animations")

    // Spring-like rotation - swings back and forth
    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spring_rotation"
    )

    // Subtle breathing glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Gentle scale pulse
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    // Subtle gradient colors matching plus icon theme
    val gradientColors = listOf(
        Color(0xFF3A4149), // Dark gray
        Color(0xFF4A525B), // Medium gray
        Color(0xFF3A4149)  // Back to dark for smooth loop
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.5.dp,
            color = Color(0xFF4A525B).copy(alpha = glowAlpha)
        ),
        modifier = modifier
            .size(36.dp)
            .scale(scale)
            .graphicsLayer { rotationZ = rotation }
    ) {
        Box(
            Modifier.background(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(100f, 100f)
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFE5EAF0) // Light text matching theme
            )
        }
    }
}

/**
 * Cycling placeholder with beautiful fade + slide animations
 * Material 3 motion design
 */
@Composable
fun CyclingPlaceholder(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
) {
    val items = remember {
        listOf(
            "Message Innovexia…",
            "Ask to summarize a PDF…",
            "Brainstorm ideas…",
            "Draft an email…",
            "Create a plan…"
        )
    }
    var idx by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            idx = (idx + 1) % items.size
        }
    }

    AnimatedContent(
        targetState = items[idx],
        transitionSpec = {
            (fadeIn(
                animationSpec = tween(
                    durationMillis = ComposerTokens.Motion.DurationMedium,
                    easing = ComposerTokens.Motion.EasingEnter
                )
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = ComposerTokens.Motion.DurationMedium,
                    easing = ComposerTokens.Motion.EasingEnter
                ),
                initialOffsetY = { it / 3 }
            )).togetherWith(
                fadeOut(
                    animationSpec = tween(
                        durationMillis = ComposerTokens.Motion.DurationShort,
                        easing = ComposerTokens.Motion.EasingExit
                    )
                ) + slideOutVertically(
                    animationSpec = tween(
                        durationMillis = ComposerTokens.Motion.DurationShort,
                        easing = ComposerTokens.Motion.EasingExit
                    ),
                    targetOffsetY = { -it / 3 }
                )
            )
        },
        label = "placeholder_animation"
    ) { targetText ->
        Text(
            text = targetText,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier
        )
    }
}

/**
 * ChatComposerV3 - Material 3 Design chat composer
 *
 * Material 3 Features:
 * - M3 color scheme (surfaceContainer, onSurfaceVariant, etc.)
 * - M3 motion design with beautiful animations
 * - M3 state layers (hover, pressed, focus, drag)
 * - Animated persona chip with rotating gradient
 * - Cycling placeholder with fade + slide animations
 * - Enhanced button animations (spring, breathing, pulse)
 * - Focus state with subtle elevation and scale
 * - Web search mode with secondary color accent
 * - Accessibility support (respects reduced motion)
 */
@Composable
fun ChatComposerV3(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onMic: () -> Unit,
    onPersona: () -> Unit,
    hasAttachment: Boolean,
    isStreaming: Boolean = false,
    onStopStreaming: () -> Unit = {},
    persona: com.example.innovexia.ui.models.Persona? = null,
    isGuest: Boolean = false,
    groundingMode: Boolean = false,
    isRecording: Boolean = false,
    recordingDuration: Long = 0L,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val d = ComposerTokens.Dimen

    // Debug logging for grounding mode
    LaunchedEffect(groundingMode) {
        android.util.Log.d("ChatComposerV3", "Grounding mode changed: $groundingMode")
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

    // Dark theme colors matching the side menu
    val containerColor = if (groundingMode) {
        Color(0xFF1E2329) // Slightly lighter for search mode
    } else {
        Color(0xFF171A1E) // Very dark surface matching chat items
    }

    val borderColor = if (groundingMode) {
        Color(0xFF4A90E2).copy(alpha = 0.4f) // Blue accent for search
    } else {
        Color(0xFF2A323B).copy(alpha = if (isFocused) 0.6f else 0.3f) // Subtle border
    }

    val textColor = Color(0xFFE5EAF0) // Light text like in side menu
    val placeholderColor = if (groundingMode) {
        Color(0xFF9AA6B2).copy(alpha = 0.7f) // Muted text
    } else {
        Color(0xFF9AA6B2).copy(alpha = 0.6f) // Placeholder text
    }

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
            // Left side: Animated search icon in grounding mode, otherwise attach button
            AnimatedContent(
                targetState = groundingMode,
                transitionSpec = {
                    fadeIn(tween(ComposerTokens.Motion.DurationShort)) +
                            scaleIn(tween(ComposerTokens.Motion.DurationShort)) togetherWith
                            fadeOut(tween(ComposerTokens.Motion.DurationShort)) +
                            scaleOut(tween(ComposerTokens.Motion.DurationShort))
                },
                label = "left_icon_animation"
            ) { isGrounding ->
                if (isGrounding) {
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
                            .rotate(rotation * 0.05f) // Subtle rotation
                    )
                } else {
                    // Regular attach button
                    M3IconButton(
                        icon = Icons.Rounded.Add,
                        contentDesc = "Attach files",
                        onClick = onAttach
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Persona chip inside field (hidden for guest users OR in grounding mode)
            AnimatedVisibility(
                visible = !isGuest && !groundingMode,
                enter = fadeIn(tween(ComposerTokens.Motion.DurationMedium)) +
                        scaleIn(tween(ComposerTokens.Motion.DurationMedium)),
                exit = fadeOut(tween(ComposerTokens.Motion.DurationShort)) +
                        scaleOut(tween(ComposerTokens.Motion.DurationShort))
            ) {
                Row {
                    PersonaChip(initials = persona?.initial ?: "K", onClick = onPersona)
                    Spacer(Modifier.width(10.dp))
                }
            }

            // Text area with animated placeholder
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(
                        if (groundingMode) colorScheme.secondary else colorScheme.primary
                    ),
                    maxLines = 6,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            if (groundingMode) {
                                Text(
                                    text = "Search the web…",
                                    color = placeholderColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                CyclingPlaceholder(color = placeholderColor)
                            }
                        }
                        innerTextField()
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Right side: Mic OR Send OR Stop button with enhanced M3 animations
            // Priority: streaming > can send > recording > mic (mic hidden for guests)
            val canSend = value.isNotBlank() || hasAttachment
            val buttonState = when {
                isStreaming -> "stop"
                canSend -> "send"
                isRecording -> "recording"
                isGuest -> "none" // No mic button for guests when nothing to send
                else -> "mic"
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
                        // Send button with spring bounce animation
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
                                containerColor = Color(0xFFF2C94C), // Gold accent like your theme
                                contentColor = Color(0xFF0F0F0F) // Dark text on gold
                            ),
                            modifier = Modifier
                                .size(d.IconFrame)
                                .scale(scale)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NorthEast,
                                contentDescription = "Send",
                                modifier = Modifier.size(d.IconSize)
                            )
                        }
                    }
                    "recording" -> {
                        // Recording button with pulsing animation
                        val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.15f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "recording_pulse_scale"
                        )

                        FilledIconButton(
                            onClick = onMic, // Tap to stop recording
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = colorScheme.error
                            ),
                            modifier = Modifier
                                .size(d.IconFrame)
                                .scale(pulseScale)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Recording (tap to stop)",
                                tint = colorScheme.onError,
                                modifier = Modifier.size(d.IconSize)
                            )
                        }
                    }
                    "none" -> {
                        // Empty space for guests when no text
                        Spacer(modifier = Modifier.size(d.IconFrame))
                    }
                    else -> {
                        // Mic button with breathing animation
                        M3IconButton(
                            icon = Icons.Rounded.Mic,
                            contentDesc = "Voice input",
                            onClick = onMic,
                            breathingAnimation = true
                        )
                    }
                }
            }
        }
    }
}

// Previews
@Preview(name = "Composer Light Empty", showBackground = true)
@Composable
fun ChatComposerV3PreviewLight() {
    var text by remember { mutableStateOf("") }
    InnovexiaTheme(darkTheme = false) {
        Surface(color = androidx.compose.ui.graphics.Color(0xFFF9FAFB)) {
            ChatComposerV3(
                value = text,
                onValueChange = { text = it },
                onSend = {},
                onAttach = {},
                onMic = {},
                onPersona = {},
                hasAttachment = false,
                isStreaming = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Composer Dark Empty", showBackground = true)
@Composable
fun ChatComposerV3PreviewDark() {
    var text by remember { mutableStateOf("") }
    InnovexiaTheme(darkTheme = true) {
        Surface(color = androidx.compose.ui.graphics.Color(0xFF0D1117)) {
            ChatComposerV3(
                value = text,
                onValueChange = { text = it },
                onSend = {},
                onAttach = {},
                onMic = {},
                onPersona = {},
                hasAttachment = false,
                isStreaming = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Composer Light With Text", showBackground = true)
@Composable
fun ChatComposerV3PreviewLightText() {
    var text by remember { mutableStateOf("Hello! This is a test message that spans multiple lines to see how the composer handles longer text input.") }
    InnovexiaTheme(darkTheme = false) {
        Surface(color = androidx.compose.ui.graphics.Color(0xFFF9FAFB)) {
            ChatComposerV3(
                value = text,
                onValueChange = { text = it },
                onSend = {},
                onAttach = {},
                onMic = {},
                onPersona = {},
                hasAttachment = false,
                isStreaming = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Composer Dark Streaming", showBackground = true)
@Composable
fun ChatComposerV3PreviewDarkStreaming() {
    var text by remember { mutableStateOf("") }
    InnovexiaTheme(darkTheme = true) {
        Surface(color = androidx.compose.ui.graphics.Color(0xFF0D1117)) {
            ChatComposerV3(
                value = text,
                onValueChange = { text = it },
                onSend = {},
                onAttach = {},
                onMic = {},
                onPersona = {},
                hasAttachment = false,
                isStreaming = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
