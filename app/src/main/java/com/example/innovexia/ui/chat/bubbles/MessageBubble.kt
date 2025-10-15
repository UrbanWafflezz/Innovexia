package com.example.innovexia.ui.chat.bubbles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.core.chat.Formatting
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.ui.chat.TypingDots

/**
 * Position of bubble in a group
 */
enum class BubbleGroupPosition {
    Single,
    First,
    Middle,
    Last
}

/**
 * Message bubble for user, model, or system messages.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    groupPosition: BubbleGroupPosition = BubbleGroupPosition.Single,
    isStreaming: Boolean = false,
    hasError: Boolean = false,
    onLongPress: () -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isUser = message.role == "user"
    val isModel = message.role == "model"
    val isSystem = message.role == "system"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        contentAlignment = when {
            isUser -> Alignment.CenterEnd
            isSystem -> Alignment.Center
            else -> Alignment.CenterStart
        }
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
        ) {
            when {
                isUser -> UserBubble(
                    message = message,
                    groupPosition = groupPosition
                )
                isModel -> ModelBubble(
                    message = message,
                    groupPosition = groupPosition,
                    isStreaming = isStreaming,
                    hasError = hasError,
                    onRetry = onRetry
                )
                isSystem -> SystemBubble(message = message)
            }
        }
    }
}

@Composable
private fun UserBubble(
    message: MessageEntity,
    groupPosition: BubbleGroupPosition
) {
    val shape = bubbleShape(groupPosition, isUser = true)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary)
            .padding(12.dp)
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun ModelBubble(
    message: MessageEntity,
    groupPosition: BubbleGroupPosition,
    isStreaming: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit
) {
    val shape = bubbleShape(groupPosition, isUser = false)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = shape
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message.text.isNotEmpty()) {
                    val (cleanText, codeBlocks) = Formatting.extractCodeBlocks(message.text)

                    if (cleanText.isNotEmpty()) {
                        FormattedText(text = cleanText)
                    }

                    codeBlocks.forEachIndexed { index, block ->
                        CodeBlock(
                            language = block.language,
                            code = block.code
                        )
                    }
                }

                if (isStreaming) {
                    TypingDots()
                }

                if (hasError) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Failed to generate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        FilledTonalButton(
                            onClick = onRetry,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Retry", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemBubble(message: MessageEntity) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FormattedText(text: String) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotatedString = remember(text) {
        Formatting.parseMarkdown(text, linkColor)
    }
    val uriHandler = LocalUriHandler.current

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    uriHandler.openUri(annotation.item)
                }
        }
    )
}

@Composable
private fun CodeBlock(
    language: String,
    code: String
) {
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(code)) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = code.trim(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun bubbleShape(
    groupPosition: BubbleGroupPosition,
    isUser: Boolean
): RoundedCornerShape {
    val cornerRadius = 18.dp
    val connectedRadius = 4.dp

    return when (groupPosition) {
        BubbleGroupPosition.Single -> RoundedCornerShape(cornerRadius)
        BubbleGroupPosition.First -> if (isUser) {
            RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = cornerRadius,
                bottomStart = cornerRadius,
                bottomEnd = connectedRadius
            )
        } else {
            RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = cornerRadius,
                bottomStart = connectedRadius,
                bottomEnd = cornerRadius
            )
        }
        BubbleGroupPosition.Middle -> if (isUser) {
            RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = connectedRadius,
                bottomStart = cornerRadius,
                bottomEnd = connectedRadius
            )
        } else {
            RoundedCornerShape(
                topStart = connectedRadius,
                topEnd = cornerRadius,
                bottomStart = connectedRadius,
                bottomEnd = cornerRadius
            )
        }
        BubbleGroupPosition.Last -> if (isUser) {
            RoundedCornerShape(
                topStart = cornerRadius,
                topEnd = connectedRadius,
                bottomStart = cornerRadius,
                bottomEnd = cornerRadius
            )
        } else {
            RoundedCornerShape(
                topStart = connectedRadius,
                topEnd = cornerRadius,
                bottomStart = cornerRadius,
                bottomEnd = cornerRadius
            )
        }
    }
}

// Previews
@Preview(name = "User Bubble")
@Composable
private fun UserBubblePreview() {
    MaterialTheme {
        Surface {
            MessageBubble(
                message = MessageEntity(
                    id = "1",
                    ownerId = "guest",
                    chatId = "chat1",
                    role = "user",
                    text = "Hello! How can you help me today?",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}

@Preview(name = "Model Bubble")
@Composable
private fun ModelBubblePreview() {
    MaterialTheme {
        Surface {
            MessageBubble(
                message = MessageEntity(
                    id = "2",
                    ownerId = "guest",
                    chatId = "chat1",
                    role = "model",
                    text = "I can help you with **many things**! Try asking me about `code` or use [links](https://example.com).",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}

@Preview(name = "Model with Code")
@Composable
private fun ModelWithCodePreview() {
    MaterialTheme {
        Surface {
            MessageBubble(
                message = MessageEntity(
                    id = "3",
                    ownerId = "guest",
                    chatId = "chat1",
                    role = "model",
                    text = "Here's an example:\n```kotlin\nfun greet() {\n    println(\"Hello World\")\n}\n```",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}

@Preview(name = "System Bubble")
@Composable
private fun SystemBubblePreview() {
    MaterialTheme {
        Surface {
            MessageBubble(
                message = MessageEntity(
                    id = "4",
                    ownerId = "guest",
                    chatId = "chat1",
                    role = "system",
                    text = "Memory saved",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }
}

@Preview(name = "Streaming")
@Composable
private fun StreamingBubblePreview() {
    MaterialTheme {
        Surface {
            MessageBubble(
                message = MessageEntity(
                    id = "5",
                    ownerId = "guest",
                    chatId = "chat1",
                    role = "model",
                    text = "Generating response",
                    createdAt = System.currentTimeMillis(),
                    streamed = false
                ),
                isStreaming = true
            )
        }
    }
}

@Preview(name = "Error")
@Composable
private fun ErrorBubblePreview() {
    MaterialTheme {
        Surface {
            MessageBubble(
                message = MessageEntity(
                    id = "6",
                    ownerId = "guest",
                    chatId = "chat1",
                    role = "model",
                    text = "",
                    createdAt = System.currentTimeMillis()
                ),
                hasError = true
            )
        }
    }
}
