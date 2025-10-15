package com.example.innovexia.ui.chat.bubbles

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.innovexia.data.local.entities.MessageEntity

/**
 * Example usage of ResponseBubbleV2 in your chat screen
 *
 * Integration Steps:
 *
 * 1. In your ChatScreen or MessageList, replace the old MessageBubble for model responses:
 *
 *    OLD:
 *    if (message.role == "model") {
 *        MessageBubble(message, isStreaming = isLastMessage && isStreaming)
 *    }
 *
 *    NEW:
 *    if (message.role == "model") {
 *        ResponseBubbleV2(message, isStreaming = isLastMessage && isStreaming)
 *    }
 *
 * 2. For skeleton loading during initial streaming:
 *
 *    if (isStreaming && message.text.isEmpty()) {
 *        ResponseBubbleSkeleton()
 *    } else if (message.role == "model") {
 *        ResponseBubbleV2(message, isStreaming = isStreaming)
 *    }
 *
 * 3. Keep user messages with existing UserBubble or create ResponseBubbleV2User if needed
 *
 * Features included:
 * - Advanced markdown with headings, lists, quotes, code blocks
 * - Tables with horizontal scrolling
 * - Collapsible sections
 * - Callouts (info/warning/tip)
 * - Image support with zoom
 * - Copy buttons on code blocks
 * - Streaming indicator
 * - Skeleton loader
 */

@Composable
fun ChatScreenExample(
    messages: List<MessageEntity>,
    isStreaming: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(messages) { message ->
            when (message.role) {
                "user" -> {
                    // Keep existing UserBubble or use MessageBubble
                    MessageBubble(message)
                }
                "model" -> {
                    val isLastMessage = message.id == messages.lastOrNull()?.id
                    if (isStreaming && isLastMessage && message.text.isEmpty()) {
                        // Show skeleton while waiting for first chunk
                        ResponseBubbleSkeleton()
                    } else {
                        // Show advanced markdown bubble
                        ResponseBubbleV2(
                            message = message,
                            isStreaming = isStreaming && isLastMessage,
                            modelName = "Gemini 2.5 Flash"
                        )
                    }
                }
                "system" -> {
                    // Keep existing system bubble
                    MessageBubble(message)
                }
            }
        }
    }
}

/**
 * Preview demonstrating ResponseBubbleV2 with various markdown elements
 */
@Preview(name = "Advanced Markdown Response")
@Composable
private fun ResponseBubbleV2Preview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ResponseBubbleV2(
                    message = MessageEntity(
                        id = "preview-1",
                        ownerId = "guest",
                        chatId = "chat1",
                        role = "model",
                        text = """
                            # Welcome to ResponseBubbleV2

                            This is an advanced markdown renderer with **bold**, *italic*, and `code` support.

                            ## Features

                            - Beautiful typography
                            - Code blocks with syntax highlighting
                            - Collapsible sections
                            - Tables and images

                            ```kotlin
                            fun greet() {
                                println("Hello World")
                            }
                            ```

                            > This is a quote with proper styling

                            :::info
                            This is an info callout!
                            :::
                        """.trimIndent(),
                        createdAt = System.currentTimeMillis()
                    ),
                    modelName = "Gemini 2.5 Pro"
                )
            }
        }
    }
}

@Preview(name = "Streaming State")
@Composable
private fun StreamingPreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ResponseBubbleV2(
                    message = MessageEntity(
                        id = "preview-2",
                        ownerId = "guest",
                        chatId = "chat1",
                        role = "model",
                        text = "Generating response...",
                        createdAt = System.currentTimeMillis()
                    ),
                    isStreaming = true,
                    modelName = "Gemini 2.5 Flash Lite"
                )
            }
        }
    }
}

@Preview(name = "Skeleton Loader")
@Composable
private fun SkeletonPreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                ResponseBubbleSkeleton()
            }
        }
    }
}

/**
 * Markdown Syntax Guide for ResponseBubbleV2
 *
 * HEADINGS:
 * # Heading 1
 * ## Heading 2
 * ### Heading 3
 *
 * TEXT FORMATTING:
 * **bold text**
 * *italic text*
 * `inline code`
 * [link text](https://example.com)
 *
 * LISTS:
 * - Unordered item 1
 * - Unordered item 2
 *
 * 1. Ordered item 1
 * 2. Ordered item 2
 *
 * QUOTES:
 * > This is a quote
 * > Multiple lines work too
 *
 * CODE BLOCKS:
 * ```kotlin
 * fun example() {
 *     println("Hello")
 * }
 * ```
 *
 * TABLES:
 * | Header 1 | Header 2 | Header 3 |
 * |----------|----------|----------|
 * | Cell 1   | Cell 2   | Cell 3   |
 * | Cell 4   | Cell 5   | Cell 6   |
 *
 * CALLOUTS:
 * :::info
 * This is an info callout
 * :::
 *
 * :::warning
 * This is a warning callout
 * :::
 *
 * :::tip
 * This is a tip callout
 * :::
 *
 * COLLAPSIBLE:
 * +++Click to expand
 * Hidden content here
 * +++
 *
 * OR:
 * <details>
 * <summary>Click to expand</summary>
 * Hidden content here
 * </details>
 *
 * IMAGES:
 * ![Alt text](https://example.com/image.jpg)
 *
 * DIVIDERS:
 * ---
 */
