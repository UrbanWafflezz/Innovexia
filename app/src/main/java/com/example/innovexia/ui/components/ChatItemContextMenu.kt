package com.example.innovexia.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.models.ChatListItem
import com.example.innovexia.ui.models.ChatState

/**
 * Actions available for chat items
 */
sealed class ChatItemAction {
    object Pin : ChatItemAction()
    object Unpin : ChatItemAction()
    object Archive : ChatItemAction()
    object MoveToTrash : ChatItemAction()
    object Restore : ChatItemAction()
    object DeletePermanently : ChatItemAction()
}

/**
 * Context menu for chat items (triggered by long-press)
 * Material 3 design with proper container and text styles
 */
@Composable
fun ChatItemContextMenu(
    item: ChatListItem,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAction: (ChatItemAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        when (item.state) {
            ChatState.ACTIVE -> {
                // Pin/Unpin
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (item.pinned) "Unpin" else "Pin",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAction(if (item.pinned) ChatItemAction.Unpin else ChatItemAction.Pin)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (item.pinned) Icons.Rounded.StarBorder else Icons.Rounded.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )

                // Archive
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Archive",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAction(ChatItemAction.Archive)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Archive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )

                // Move to Trash
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Move to Trash",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAction(ChatItemAction.MoveToTrash)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )

                // Delete Permanently
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete Permanently",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAction(ChatItemAction.DeletePermanently)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
            }

            ChatState.ARCHIVED -> {
                // Restore from Archive
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Unarchive",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAction(ChatItemAction.Restore)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )

                // Move to Trash
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Move to Trash",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAction(ChatItemAction.MoveToTrash)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )

                // Delete Permanently
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete Permanently",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAction(ChatItemAction.DeletePermanently)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
            }

            ChatState.TRASH -> {
                // Restore from Trash
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Restore",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAction(ChatItemAction.Restore)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )

                // Delete Permanently
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete Permanently",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onAction(ChatItemAction.DeletePermanently)
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                )
            }
        }
    }
}
