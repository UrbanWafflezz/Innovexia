package com.example.innovexia.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.ui.chat.bubbles.BubbleGroupPosition
import com.example.innovexia.ui.chat.bubbles.GroundingSearchBubble
import com.example.innovexia.ui.chat.bubbles.MessageBubble
import com.example.innovexia.ui.chat.bubbles.ResponseBubbleV2
import com.example.innovexia.ui.chat.bubbles.ResponseBubbleSkeleton
import com.example.innovexia.ui.chat.bubbles.UserBubbleV2
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch
import com.example.innovexia.ui.chat.newchat.SmartGreetingScreen
import com.example.innovexia.ui.chat.newchat.suggestions.SuggestionCardUi
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Message list with grouping, date dividers, scroll-to-bottom, and empty state greeting.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageList(
    messages: List<MessageEntity>,
    onLongPress: (MessageEntity) -> Unit,
    onSwipeReply: (MessageEntity) -> Unit,
    onRetry: (MessageEntity) -> Unit,
    streamingMessageId: String? = null,
    errorMessageIds: Set<String> = emptySet(),
    editingMessageId: String? = null,
    truncatedMessageIds: Set<String> = emptySet(),
    onCopyMessage: (MessageEntity) -> Unit = {},
    onQuoteMessage: (MessageEntity) -> Unit = {},
    onDeleteMessage: (MessageEntity) -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onSaveEdit: (String, MessageEntity) -> Unit = { _, _ -> },
    onRegenerateAssistant: (String) -> Unit = {},
    onContinueResponse: (String) -> Unit = {},
    currentModelName: String = "Innovexia",
    ownerId: String = "guest",
    selectedPersona: com.example.innovexia.core.persona.Persona? = null,
    onSuggestionClicked: (SuggestionCardUi) -> Unit = {},
    groundingDataMap: Map<String, com.example.innovexia.data.ai.GroundingMetadata> = emptyMap(),
    groundingStatusMap: Map<String, com.example.innovexia.data.ai.GroundingStatus> = emptyMap(),
    groundingEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show greeting when no messages
    if (messages.isEmpty()) {
        SmartGreetingScreen(
            persona = selectedPersona,
            onSuggestionClicked = onSuggestionClicked,
            modifier = modifier
        )
        return
    }

    // Show scroll to bottom FAB when not at bottom
    val showScrollToBottom by remember {
        derivedStateOf {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            firstVisibleIndex > 3
        }
    }

    // Group messages and add date dividers
    val groupedMessages = remember(messages) {
        groupMessagesWithDividers(messages)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 200.dp, start = 0.dp, end = 0.dp),
            reverseLayout = false
        ) {
            groupedMessages.forEach { item ->
                when (item) {
                    is MessageListItem.DateDivider -> {
                        item(key = "divider_${item.date}") {
                            DateDivider(
                                text = item.text,
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                    is MessageListItem.MessageGroup -> {
                        items(
                            items = item.messages,
                            key = { it.message.id }
                        ) { groupedMessage ->
                            var swipeOffset by remember { mutableStateOf(0f) }

                            Box(
                                modifier = Modifier
                                    .animateItemPlacement()
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                if (swipeOffset > 100f) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onSwipeReply(groupedMessage.message)
                                                }
                                                swipeOffset = 0f
                                            },
                                            onHorizontalDrag = { _, dragAmount ->
                                                if (dragAmount > 0) {
                                                    swipeOffset += dragAmount
                                                }
                                            }
                                        )
                                    }
                            ) {
                                Column {
                                    val isStreaming = groupedMessage.message.id == streamingMessageId
                                    val hasError = groupedMessage.message.id in errorMessageIds
                                    val isEditing = groupedMessage.message.id == editingMessageId

                                    if (isEditing) {
                                        android.util.Log.d("MessageList", "Message ${groupedMessage.message.id} is in edit mode")
                                    }

                                    // Debug logging for grounding
                                    if (groupedMessage.message.role == "model") {
                                        android.util.Log.d("MessageList", "🔍 Model message ${groupedMessage.message.id}:")
                                        android.util.Log.d("MessageList", "  - DB groundingStatus: ${groupedMessage.message.groundingStatus}")
                                        android.util.Log.d("MessageList", "  - DB groundingJson: ${groupedMessage.message.groundingJson?.take(50)}")
                                        android.util.Log.d("MessageList", "  - getGroundingStatusEnum(): ${groupedMessage.message.getGroundingStatusEnum()}")
                                        android.util.Log.d("MessageList", "  - groundingStatusMap[id]: ${groundingStatusMap[groupedMessage.message.id]}")
                                        android.util.Log.d("MessageList", "  - groundingDataMap[id]: ${groundingDataMap[groupedMessage.message.id]}")
                                        android.util.Log.d("MessageList", "  - groundingEnabled: $groundingEnabled")
                                    }

                                    when {
                                        // Use GroundingSearchBubble for messages sent with grounding enabled
                                        // Check multiple sources: database status, in-memory status map, OR grounding metadata presence
                                        groupedMessage.message.role == "model" && (
                                            groupedMessage.message.getGroundingStatusEnum() != com.example.innovexia.data.ai.GroundingStatus.NONE ||
                                            groundingStatusMap[groupedMessage.message.id] != null ||
                                            groundingDataMap[groupedMessage.message.id] != null
                                        ) -> {
                                            android.util.Log.d("MessageList", "✅ Using GroundingSearchBubble for message ${groupedMessage.message.id}")
                                            val status = groundingStatusMap[groupedMessage.message.id] ?: groupedMessage.message.getGroundingStatusEnum()

                                            if (isStreaming && groupedMessage.message.text.isEmpty()) {
                                                ResponseBubbleSkeleton()
                                            } else {
                                                GroundingSearchBubble(
                                                    message = groupedMessage.message,
                                                    isStreaming = isStreaming,
                                                    groundingMetadata = groundingDataMap[groupedMessage.message.id],
                                                    groundingStatus = status,
                                                    modelName = currentModelName,
                                                    onRegenerate = { messageId ->
                                                        android.util.Log.d("MessageList", "Regenerate callback (grounding) - messageId=$messageId")
                                                        onRegenerateAssistant(messageId)
                                                    },
                                                    onCopy = { text ->
                                                        android.util.Log.d("MessageList", "Copy callback (grounding) - text length=${text.length}")
                                                        clipboardManager.setText(AnnotatedString(text))
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                message = "Copied",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        // Use ResponseBubbleV2 for regular model messages (no grounding)
                                        groupedMessage.message.role == "model" -> {
                                            android.util.Log.d("MessageList", "❌ Using ResponseBubbleV2 for message ${groupedMessage.message.id}")
                                            if (isStreaming && groupedMessage.message.text.isEmpty()) {
                                                ResponseBubbleSkeleton()
                                            } else {
                                                ResponseBubbleV2(
                                                    message = groupedMessage.message,
                                                    isStreaming = isStreaming,
                                                    modelName = currentModelName,
                                                    isTruncated = groupedMessage.message.id in truncatedMessageIds,
                                                    onRegenerate = { messageId ->
                                                        android.util.Log.d("MessageList", "Regenerate callback - messageId=$messageId")
                                                        onRegenerateAssistant(messageId)
                                                    },
                                                    onCopy = { text ->
                                                        android.util.Log.d("MessageList", "Copy callback - text length=${text.length}")
                                                        // Copy markdown to clipboard
                                                        clipboardManager.setText(AnnotatedString(text))
                                                        // Haptic feedback
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        // Show "Copied" snackbar
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                message = "Copied",
                                                                duration = SnackbarDuration.Short
                                                            )
                                                        }
                                                    },
                                                    onContinue = onContinueResponse
                                                )
                                            }
                                        }

                                        // Use UserBubbleV2 for user messages
                                        groupedMessage.message.role == "user" -> {
                                            UserBubbleV2(
                                                msg = groupedMessage.message,
                                                onCopy = { msg ->
                                                    clipboardManager.setText(AnnotatedString(msg.text))
                                                    onCopyMessage(msg)
                                                },
                                                onRetry = onRetry,
                                                onQuote = onQuoteMessage,
                                                onDelete = onDeleteMessage
                                            )
                                        }

                                        // Use original MessageBubble for system messages
                                        else -> {
                                            MessageBubble(
                                                message = groupedMessage.message,
                                                groupPosition = groupedMessage.position,
                                                isStreaming = isStreaming,
                                                hasError = hasError,
                                                onLongPress = { onLongPress(groupedMessage.message) },
                                                onRetry = { onRetry(groupedMessage.message) }
                                            )
                                        }
                                    }

                                    // Show timestamp for last message in group (not in edit mode)
                                    if (groupedMessage.showTimestamp && !isEditing) {
                                        MessageTimestamp(
                                            timestamp = groupedMessage.message.createdAt,
                                            isUser = groupedMessage.message.role == "user"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scroll to bottom FAB
        AnimatedVisibility(
            visible = showScrollToBottom,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val totalItems = groupedMessages.sumOf {
                            when (it) {
                                is MessageListItem.DateDivider -> 1
                                is MessageListItem.MessageGroup -> it.messages.size
                            }
                        }
                        if (totalItems > 0) {
                            // Scroll to the last item with offset to ensure it's fully visible
                            listState.scrollToItem(totalItems - 1, scrollOffset = 0)
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom"
                )
            }
        }

        // Snackbar for copy feedback
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // Auto-scroll to bottom on new messages or during streaming
    LaunchedEffect(messages.size, streamingMessageId, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) {
            // Calculate total item count including dividers
            val totalItems = groupedMessages.sumOf {
                when (it) {
                    is MessageListItem.DateDivider -> 1
                    is MessageListItem.MessageGroup -> it.messages.size
                }
            }

            // Always scroll to bottom when streaming (to show growing content)
            // or when not scrolled up
            if (streamingMessageId != null || !showScrollToBottom) {
                if (totalItems > 0) {
                    // Use scrollToItem with offset 0 to ensure we reach the very bottom
                    listState.scrollToItem(totalItems - 1, scrollOffset = 0)
                }
            }
        }
    }
}

@Composable
private fun DateDivider(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MessageTimestamp(
    timestamp: Long,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val timeText = remember(timestamp) {
        val date = Date(timestamp)
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// Data structures for grouping
private sealed class MessageListItem {
    data class DateDivider(val date: Long, val text: String) : MessageListItem()
    data class MessageGroup(val messages: List<GroupedMessage>) : MessageListItem()
}

private data class GroupedMessage(
    val message: MessageEntity,
    val position: BubbleGroupPosition,
    val showTimestamp: Boolean
)

private fun groupMessagesWithDividers(messages: List<MessageEntity>): List<MessageListItem> {
    if (messages.isEmpty()) return emptyList()

    val result = mutableListOf<MessageListItem>()
    val sortedMessages = messages.sortedBy { it.createdAt }

    var currentDate: Long? = null
    val currentGroup = mutableListOf<MessageEntity>()

    sortedMessages.forEach { message ->
        val messageDate = getDayTimestamp(message.createdAt)

        // Add date divider if date changed
        if (currentDate != messageDate) {
            // Flush current group
            if (currentGroup.isNotEmpty()) {
                result.add(MessageListItem.MessageGroup(groupMessages(currentGroup)))
                currentGroup.clear()
            }

            // Add date divider
            result.add(
                MessageListItem.DateDivider(
                    date = messageDate,
                    text = formatDateDivider(messageDate)
                )
            )
            currentDate = messageDate
        }

        // Check if should start new group
        if (currentGroup.isNotEmpty()) {
            val lastMessage = currentGroup.last()
            val timeDiff = message.createdAt - lastMessage.createdAt
            val differentAuthor = message.role != lastMessage.role

            if (differentAuthor || timeDiff > TimeUnit.MINUTES.toMillis(3)) {
                result.add(MessageListItem.MessageGroup(groupMessages(currentGroup)))
                currentGroup.clear()
            }
        }

        currentGroup.add(message)
    }

    // Flush remaining group
    if (currentGroup.isNotEmpty()) {
        result.add(MessageListItem.MessageGroup(groupMessages(currentGroup)))
    }

    return result
}

private fun groupMessages(messages: List<MessageEntity>): List<GroupedMessage> {
    return messages.mapIndexed { index, message ->
        val position = when {
            messages.size == 1 -> BubbleGroupPosition.Single
            index == 0 -> BubbleGroupPosition.First
            index == messages.lastIndex -> BubbleGroupPosition.Last
            else -> BubbleGroupPosition.Middle
        }

        GroupedMessage(
            message = message,
            position = position,
            showTimestamp = index == messages.lastIndex
        )
    }
}

private fun getDayTimestamp(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

private fun formatDateDivider(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    calendar.timeInMillis = timestamp

    val daysDiff = TimeUnit.MILLISECONDS.toDays(today.timeInMillis - calendar.timeInMillis)

    return when {
        daysDiff == 0L -> "Today"
        daysDiff == 1L -> "Yesterday"
        daysDiff < 7 -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

// Preview
@Preview(name = "Message List")
@Composable
private fun MessageListPreview() {
    val now = System.currentTimeMillis()
    val messages = listOf(
        MessageEntity(
            id = "1",
            ownerId = "guest",
            chatId = "chat1",
            role = "user",
            text = "Hello!",
            createdAt = now - TimeUnit.HOURS.toMillis(25)
        ),
        MessageEntity(
            id = "2",
            ownerId = "guest",
            chatId = "chat1",
            role = "model",
            text = "Hi there! How can I help you today?",
            createdAt = now - TimeUnit.HOURS.toMillis(25) + 1000
        ),
        MessageEntity(
            id = "3",
            ownerId = "guest",
            chatId = "chat1",
            role = "user",
            text = "I need help with Kotlin",
            createdAt = now - 10000
        ),
        MessageEntity(
            id = "4",
            ownerId = "guest",
            chatId = "chat1",
            role = "user",
            text = "Specifically coroutines",
            createdAt = now - 9000
        ),
        MessageEntity(
            id = "5",
            ownerId = "guest",
            chatId = "chat1",
            role = "model",
            text = "I'd be happy to help with Kotlin coroutines!",
            createdAt = now - 5000
        )
    )

    MaterialTheme {
        Surface {
            MessageList(
                messages = messages,
                onLongPress = {},
                onSwipeReply = {},
                onRetry = {},
                streamingMessageId = "5"
            )
        }
    }
}
