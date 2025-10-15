package com.example.innovexia.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaTheme
import kotlinx.coroutines.delay

/**
 * Design tokens for the Claude-style composer
 */
object ComposerTokens {
    object Color {
        val ContainerDark = androidx.compose.ui.graphics.Color(0xFF161A1F)       // main bg (dark)
        val ContainerLight = androidx.compose.ui.graphics.Color(0xFFF6F7F9)      // main bg (light)
        val BorderDark = androidx.compose.ui.graphics.Color(0xFF2A323B)
        val BorderLight = androidx.compose.ui.graphics.Color(0xFFE1E5EA)
        val PlaceholderDark = androidx.compose.ui.graphics.Color(0xFF9AA6B2)
        val PlaceholderLight = androidx.compose.ui.graphics.Color(0xFF8C95A3)
        val TextDark = androidx.compose.ui.graphics.Color(0xFFECEFF4)
        val TextLight = androidx.compose.ui.graphics.Color(0xFF0F141A)
        val GlassBgDark = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f)
        val GlassBgLight = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.06f)
        val GlassStrokeDark = androidx.compose.ui.graphics.Color(0x332A323B)
        val GlassStrokeLight = androidx.compose.ui.graphics.Color(0x33AEB6C2)
        val FocusRingDark = androidx.compose.ui.graphics.Color(0x33FFFFFF)
        val FocusRingLight = androidx.compose.ui.graphics.Color(0x33000000)
        val AccentGold = androidx.compose.ui.graphics.Color(0xFFDBB461)
    }

    object Dimen {
        val Height = 80.dp          // increased from 64dp
        val Radius = 28.dp          // increased from 24dp
        val IconFrame = 44.dp       // increased from 36dp
        val IconSize = 22.dp        // increased from 18dp
        val PadH = 16.dp            // increased from 14dp
        val PadV = 14.dp            // increased from 12dp
        val MaxWidth = 860.dp       // tablets/desktops
    }
}

/**
 * Glass-styled icon frame with subtle background and stroke
 */
@Composable
fun GlassIconFrame(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconTint: Color? = null,
    modifier: Modifier = Modifier
) {
    val colors = ComposerTokens.Color
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) colors.GlassBgDark else colors.GlassBgLight
    val stroke = if (isDark) colors.GlassStrokeDark else colors.GlassStrokeLight
    val defaultIconTint = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.Black

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, stroke),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.size(ComposerTokens.Dimen.IconFrame)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = iconTint ?: defaultIconTint,
                modifier = Modifier.size(ComposerTokens.Dimen.IconSize)
            )
        }
    }
}

/**
 * Persona chip displayed inside the field (left side)
 * Beautiful gradient background with colorful border
 */
@Composable
fun PersonaChip(
    initials: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // Gradient colors for the background
    val gradientColors = if (isDark) {
        listOf(
            androidx.compose.ui.graphics.Color(0xFF8B5CF6), // Purple
            androidx.compose.ui.graphics.Color(0xFF3B82F6)  // Blue
        )
    } else {
        listOf(
            androidx.compose.ui.graphics.Color(0xFFA78BFA), // Lighter purple
            androidx.compose.ui.graphics.Color(0xFF60A5FA)  // Lighter blue
        )
    }

    // Border color with nice accent
    val borderColor = if (isDark) {
        androidx.compose.ui.graphics.Color(0xFF8B5CF6).copy(alpha = 0.6f)
    } else {
        androidx.compose.ui.graphics.Color(0xFF8B5CF6).copy(alpha = 0.8f)
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier.size(32.dp) // Increased from 28dp
    ) {
        Box(
            Modifier.background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = gradientColors
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}

/**
 * Cycling placeholder that rotates through messages every ~3.5s
 */
@Composable
fun CyclingPlaceholder(): String {
    val items = remember {
        listOf(
            "Message Innovexia…",
            "Ask to summarize a PDF…",
            "Brainstorm ideas…",
            "Draft an email…",
            "Create a plan…"
        )
    }
    var idx by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            idx = (idx + 1) % items.size
        }
    }
    return items[idx]
}

/**
 * ChatComposerV3 - Claude-style large input composer
 *
 * Features:
 * - Persona chip on the left, inside the field
 * - Tools button removed
 * - Rounded, elevated, glass icon frames for +, mic, and send
 * - Animated send icon that appears when user types
 * - Red stop button during streaming
 * - Animated/cycling placeholder that never wraps
 * - Bigger input height; consistent across devices
 * - Anchored to keyboard with IME padding
 * - Smooth focus states without bright blue highlights
 * - Web search mode with blue accent and search icon
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
    val isDark = isSystemInDarkTheme()
    val c = ComposerTokens.Color
    val d = ComposerTokens.Dimen

    // Debug logging for grounding mode
    LaunchedEffect(groundingMode) {
        android.util.Log.d("ChatComposerV3", "Grounding mode changed: $groundingMode")
    }

    // Web search mode colors (blue accent theme)
    val searchAccentColor = androidx.compose.ui.graphics.Color(0xFF4A90E2)

    val containerColor = if (groundingMode) {
        if (isDark) searchAccentColor.copy(alpha = 0.08f) else searchAccentColor.copy(alpha = 0.05f)
    } else {
        if (isDark) c.ContainerDark else c.ContainerLight
    }

    val borderColor = if (groundingMode) {
        searchAccentColor.copy(alpha = 0.5f)
    } else {
        if (isDark) c.BorderDark else c.BorderLight
    }

    val textColor = if (isDark) c.TextDark else c.TextLight
    val placeholderCol = if (isDark) c.PlaceholderDark else c.PlaceholderLight

    Surface(
        shape = RoundedCornerShape(d.Radius),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .heightIn(min = d.Height)
            .imePadding()
            .navigationBarsPadding()
            .widthIn(max = d.MaxWidth)
    ) {
        Row(
            Modifier.padding(horizontal = d.PadH, vertical = d.PadV),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Search icon in grounding mode, otherwise attach button
            if (groundingMode) {
                // Search icon for web search mode
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Web search",
                    tint = searchAccentColor,
                    modifier = Modifier.size(d.IconSize)
                )
            } else {
                // Regular attach button
                GlassIconFrame(
                    icon = Icons.Rounded.Add,
                    contentDesc = "Attach files",
                    onClick = onAttach
                )
            }
            Spacer(Modifier.width(10.dp))

            // Persona chip inside field (hidden for guest users OR in grounding mode)
            if (!isGuest && !groundingMode) {
                PersonaChip(initials = persona?.initial ?: "K", onClick = onPersona)
                Spacer(Modifier.width(10.dp))
            }

            // Text area (single composable controls height)
            Box(Modifier.weight(1f)) {
                // Use web search placeholder in grounding mode, otherwise cycling placeholder
                val ph = if (groundingMode) "Search the web…" else CyclingPlaceholder()

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(if (groundingMode) searchAccentColor else textColor),
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            // one-line, never wrapping
                            Text(
                                text = ph,
                                color = if (groundingMode) searchAccentColor.copy(alpha = 0.6f) else placeholderCol,
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

            // Right side: Mic OR Send OR Stop button with animation
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
                    fadeIn(animationSpec = tween(200)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(200)
                    ) togetherWith fadeOut(animationSpec = tween(150)) + scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(150)
                    )
                },
                label = "action_button_animation"
            ) { targetState ->
                when (targetState) {
                    "stop" -> {
                        // Red stop button during streaming
                        Surface(
                            onClick = onStopStreaming,
                            shape = CircleShape,
                            color = androidx.compose.ui.graphics.Color(0xFFDC2626),
                            modifier = Modifier.size(d.IconFrame)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(d.IconSize - 4.dp)
                                        .background(
                                            androidx.compose.ui.graphics.Color.White,
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                    }
                    "send" -> {
                        // Send button with scale animation
                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(200),
                            label = "send_scale"
                        )

                        GlassIconFrame(
                            icon = Icons.Rounded.NorthEast,
                            contentDesc = "Send",
                            onClick = onSend,
                            enabled = true,
                            iconTint = if (isDark) androidx.compose.ui.graphics.Color.White
                                      else androidx.compose.ui.graphics.Color.Black,
                            modifier = Modifier.scale(scale)
                        )
                    }
                    "recording" -> {
                        // Recording button with pulsing red animation
                        var targetScale by remember { mutableStateOf(1.15f) }
                        val pulseScale by animateFloatAsState(
                            targetValue = targetScale,
                            animationSpec = tween(durationMillis = 600),
                            label = "pulse_scale"
                        )

                        // Toggle between 1f and 1.15f infinitely
                        LaunchedEffect(Unit) {
                            while (true) {
                                targetScale = 1.15f
                                kotlinx.coroutines.delay(600)
                                targetScale = 1f
                                kotlinx.coroutines.delay(600)
                            }
                        }

                        Surface(
                            onClick = onMic, // Tap to stop recording
                            shape = CircleShape,
                            color = androidx.compose.ui.graphics.Color(0xFFDC2626),
                            modifier = Modifier
                                .size(d.IconFrame)
                                .scale(pulseScale)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = "Recording (tap to stop)",
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(d.IconSize)
                                )
                            }
                        }
                    }
                    "none" -> {
                        // Empty space for guests when no text
                        Spacer(modifier = Modifier.size(d.IconFrame))
                    }
                    else -> {
                        // Mic button
                        GlassIconFrame(
                            icon = Icons.Rounded.Mic,
                            contentDesc = "Voice",
                            onClick = onMic,
                            iconTint = if (isDark) androidx.compose.ui.graphics.Color.White
                                      else androidx.compose.ui.graphics.Color.Black
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
