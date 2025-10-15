package com.example.innovexia.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * WebSearchComposer - Dedicated web search input with blue accent theme
 *
 * Features:
 * - Blue accent colors throughout
 * - Search icon on left
 * - Grounding toggle chip to disable web search mode
 * - "Search the web…" placeholder
 * - Send button appears when typing
 * - Red stop button during streaming
 * - Clean, focused web search experience
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
    val isDark = isSystemInDarkTheme()
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

    // Use same colors as ChatComposerV3
    val c = ComposerTokens.Color

    // Colors matching ChatComposerV3
    val containerColor = if (isDark) c.ContainerDark else c.ContainerLight
    val borderColor = if (isDark) c.BorderDark else c.BorderLight
    val textColor = if (isDark) c.TextDark else c.TextLight
    val placeholderColor = if (isDark) c.PlaceholderDark else c.PlaceholderLight

    // Subtle accent for grounding chip
    val chipAccentColor = Color(0xFF4A90E2)

    // Use same dimensions as ChatComposerV3
    val d = ComposerTokens.Dimen

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
            // Search icon (left)
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Web search",
                tint = textColor,
                modifier = Modifier.size(d.IconSize)
            )
            Spacer(Modifier.width(12.dp))

            // Grounding toggle chip (compact)
            Surface(
                onClick = onDisableGrounding,
                shape = RoundedCornerShape(10.dp),
                color = chipAccentColor,
                modifier = Modifier.height(22.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = "Grounding enabled",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Grounding",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Disable grounding",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Text input area
            Box(Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = textColor,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(textColor),
                    maxLines = 6,
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

            // Right side: Send OR Stop button with animation
            val canSend = value.isNotBlank()
            val buttonState = when {
                isStreaming -> "stop"
                canSend -> "send"
                else -> "none"
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
                            color = Color(0xFFDC2626),
                            modifier = Modifier.size(d.IconFrame)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(d.IconSize - 4.dp)
                                        .background(
                                            Color.White,
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                    }
                    "send" -> {
                        // Search button with scale animation (glass style like ChatComposerV3)
                        val scale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(200),
                            label = "send_scale"
                        )

                        GlassIconFrame(
                            icon = Icons.Rounded.Search,
                            contentDesc = "Search",
                            onClick = onSend,
                            enabled = true,
                            iconTint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.scale(scale)
                        )
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
