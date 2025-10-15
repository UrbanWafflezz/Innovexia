package com.example.innovexia.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.glass.LiquidGlassSurface
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaTheme
import com.example.innovexia.ui.theme.LightColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bubble grouping position for consecutive messages
 */
enum class BubblePosition {
    Single,    // Standalone message
    First,     // First in a group
    Middle,    // Middle of a group
    Last       // Last in a group
}

/**
 * Message bubble for chat display.
 * Supports user and model messages with different styling and grouping.
 */
@Composable
fun MessageBubble(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isStreaming: Boolean = false,
    timestamp: Long = System.currentTimeMillis(),
    groupPosition: BubblePosition = BubblePosition.Single,
    showTimestamp: Boolean = true,
    onRegenerate: () -> Unit = {},
    onEdit: () -> Unit = {}
) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(timestamp))

    // Determine bubble shape based on group position
    val bubbleShape = when (groupPosition) {
        BubblePosition.Single -> RoundedCornerShape(18.dp)
        BubblePosition.First -> if (isUser) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
        }
        BubblePosition.Middle -> if (isUser) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        } else {
            RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
        }
        BubblePosition.Last -> if (isUser) {
            RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        } else {
            RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        }
    }

    // Adjust vertical padding based on grouping
    val verticalPadding = when (groupPosition) {
        BubblePosition.Single -> 6.dp
        BubblePosition.First -> 6.dp
        BubblePosition.Middle -> 1.dp
        BubblePosition.Last -> 1.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = verticalPadding),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        if (isUser) {
            // User message - blue glass bubble with timestamp and edit icon
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .clip(bubbleShape)
                    .background(
                        if (darkTheme) DarkColors.AccentBlue else LightColors.AccentBlue
                    )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color.White
                    )

                    // Only show timestamp and actions if showTimestamp is true (last in group)
                    if (showTimestamp) {
                        // Divider
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 1.dp,
                            color = Color.White.copy(alpha = 0.2f)
                        )

                        // Timestamp and edit icon row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.7f)
                            )

                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit message",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // AI message - liquid glass surface with actions inside
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = RoundedCornerShape(18.dp),
                darkTheme = darkTheme
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Message content - formatted plain text
                    Text(
                        text = formatText(text) + if (isStreaming) "▌" else "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                            fontSize = 14.sp
                        ),
                        color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                    )

                    // Actions row (timestamp, copy, regenerate)
                    if (!isStreaming) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            thickness = 1.dp,
                            color = if (darkTheme)
                                DarkColors.SecondaryText.copy(alpha = 0.15f)
                            else
                                LightColors.SecondaryText.copy(alpha = 0.15f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Timestamp
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (darkTheme)
                                    DarkColors.SecondaryText.copy(alpha = 0.5f)
                                else
                                    LightColors.SecondaryText.copy(alpha = 0.5f)
                            )

                            // Action buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Copy button
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("message", text)
                                        clipboard.setPrimaryClip(clip)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy message",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (darkTheme)
                                            DarkColors.SecondaryText.copy(alpha = 0.6f)
                                        else
                                            LightColors.SecondaryText.copy(alpha = 0.6f)
                                    )
                                }

                                // Regenerate button
                                IconButton(
                                    onClick = onRegenerate,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Regenerate",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (darkTheme)
                                            DarkColors.SecondaryText.copy(alpha = 0.6f)
                                        else
                                            LightColors.SecondaryText.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format text by removing markdown symbols and adding proper indentation
 */
private fun formatText(text: String): String {
    return try {
        text
            .replace(Regex("^#+\\s+", RegexOption.MULTILINE), "") // Remove # headers
            .replace(Regex("^[*-]\\s+", RegexOption.MULTILINE), "  • ") // Convert * or - to bullets with indent
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1") // Remove **bold**
            .replace(Regex("\\*(.+?)\\*"), "$1") // Remove *italic*
            .replace(Regex("```[\\s\\S]*?```"), "") // Remove code blocks for now
            .replace(Regex("`(.+?)`"), "$1") // Remove inline code backticks
    } catch (e: Exception) {
        text // Return original text if formatting fails
    }
}

@Preview(name = "User Message Light", showBackground = true)
@Composable
fun MessageBubbleUserPreview() {
    InnovexiaTheme(darkTheme = false) {
        MessageBubble(
            text = "Hello, can you help me with my project?",
            isUser = true,
            darkTheme = false
        )
    }
}

@Preview(name = "Model Message Light", showBackground = true)
@Composable
fun MessageBubbleModelPreview() {
    InnovexiaTheme(darkTheme = false) {
        MessageBubble(
            text = "Of course! I'd be happy to help you with your project. What do you need assistance with?",
            isUser = false,
            darkTheme = false
        )
    }
}

@Preview(name = "Streaming Message", showBackground = true)
@Composable
fun MessageBubbleStreamingPreview() {
    InnovexiaTheme(darkTheme = true) {
        MessageBubble(
            text = "This is a streaming response that's being typed",
            isUser = false,
            darkTheme = true,
            isStreaming = true
        )
    }
}
