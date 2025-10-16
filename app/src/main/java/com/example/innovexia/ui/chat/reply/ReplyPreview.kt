package com.example.innovexia.ui.chat.reply

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.innovexia.data.local.entities.MessageEntity

/**
 * Reply preview shown above the composer when replying to a message.
 */
@Composable
fun ReplyPreview(
    replyToMessage: MessageEntity,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Reply icon
            Icon(
                imageVector = Icons.Default.Reply,
                contentDescription = "Replying to",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            // Message preview
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (replyToMessage.role == "user") "You" else "Innovexia",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = replyToMessage.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close button
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear reply",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Preview(name = "Reply Preview Light")
@Composable
private fun ReplyPreviewLightPreview() {
    MaterialTheme {
        Surface {
            ReplyPreview(
                replyToMessage = MessageEntity(
                    id = "1",
                    ownerId = "guest",
                    chatId = "chat1",
                    role = "model",
                    text = "This is a longer message that will be truncated to fit within the preview area. It demonstrates how long messages are handled.",
                    createdAt = System.currentTimeMillis()
                ),
                onClear = {}
            )
        }
    }
}

@Preview(name = "Reply Preview Dark")
@Composable
private fun ReplyPreviewDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            ReplyPreview(
                replyToMessage = MessageEntity(
                    id = "2",
                    ownerId = "guest",
                    chatId = "chat1",
                    role = "user",
                    text = "How do I use this feature?",
                    createdAt = System.currentTimeMillis()
                ),
                onClear = {}
            )
        }
    }
}
