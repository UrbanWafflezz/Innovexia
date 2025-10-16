package com.example.innovexia.ui.chat.context

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.innovexia.data.local.entities.MessageEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Context menu actions for messages
 */
sealed class MessageAction {
    object Copy : MessageAction()
    object Reply : MessageAction()
    object Edit : MessageAction()
    object Delete : MessageAction()
    object Retry : MessageAction()
    object Select : MessageAction()
}

/**
 * Long-press context menu for messages
 */
@Composable
fun MessageContextMenu(
    message: MessageEntity,
    onDismiss: () -> Unit,
    onAction: (MessageAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .widthIn(min = 200.dp, max = 280.dp)
                .clip(RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                // Header with timestamp
                MenuHeader(message = message)

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Menu items
                val isUser = message.role == "user"
                val isModel = message.role == "model"

                MenuItem(
                    icon = Icons.Default.ContentCopy,
                    text = "Copy",
                    onClick = {
                        onAction(MessageAction.Copy)
                        onDismiss()
                    }
                )

                MenuItem(
                    icon = Icons.Default.Reply,
                    text = "Reply",
                    onClick = {
                        onAction(MessageAction.Reply)
                        onDismiss()
                    }
                )

                if (isUser) {
                    MenuItem(
                        icon = Icons.Default.Edit,
                        text = "Edit",
                        onClick = {
                            onAction(MessageAction.Edit)
                            onDismiss()
                        }
                    )
                }

                MenuItem(
                    icon = Icons.Default.Delete,
                    text = "Delete",
                    onClick = {
                        onAction(MessageAction.Delete)
                        onDismiss()
                    },
                    textColor = MaterialTheme.colorScheme.error
                )

                if (isModel) {
                    MenuItem(
                        icon = Icons.Default.Refresh,
                        text = "Retry",
                        onClick = {
                            onAction(MessageAction.Retry)
                            onDismiss()
                        }
                    )
                }

                MenuItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Select",
                    onClick = {
                        onAction(MessageAction.Select)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun MenuHeader(message: MessageEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = when (message.role) {
                "user" -> "You"
                "model" -> "Innovexia"
                else -> "System"
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        val timestamp = remember(message.createdAt) {
            val date = Date(message.createdAt)
            SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(date)
        }

        Text(
            text = timestamp,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = text
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .heightIn(min = 44.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}

@Preview(name = "Context Menu - User Message")
@Composable
private fun UserMessageContextMenuPreview() {
    MaterialTheme {
        MessageContextMenu(
            message = MessageEntity(
                id = "1",
                ownerId = "guest",
                chatId = "chat1",
                role = "user",
                text = "Hello, how are you?",
                createdAt = System.currentTimeMillis()
            ),
            onDismiss = {},
            onAction = {}
        )
    }
}

@Preview(name = "Context Menu - Model Message")
@Composable
private fun ModelMessageContextMenuPreview() {
    MaterialTheme {
        MessageContextMenu(
            message = MessageEntity(
                id = "2",
                ownerId = "guest",
                chatId = "chat1",
                role = "model",
                text = "I'm doing great, thanks for asking!",
                createdAt = System.currentTimeMillis()
            ),
            onDismiss = {},
            onAction = {}
        )
    }
}

@Preview(name = "Context Menu - Dark")
@Composable
private fun DarkContextMenuPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        MessageContextMenu(
            message = MessageEntity(
                id = "3",
                ownerId = "guest",
                chatId = "chat1",
                role = "user",
                text = "Test message",
                createdAt = System.currentTimeMillis()
            ),
            onDismiss = {},
            onAction = {}
        )
    }
}
