package com.example.innovexia.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.models.ChatListItem
import com.example.innovexia.ui.models.ChatState
import com.example.innovexia.ui.theme.InnovexiaColors

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
                InnovexiaColors.DarkSurfaceElevated,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = InnovexiaColors.DarkBorder.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        when (item.state) {
            ChatState.ACTIVE -> {
                // Pin/Unpin
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (item.pinned) "Unpin" else "Pin",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = InnovexiaColors.DarkTextPrimary
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
                            tint = InnovexiaColors.GoldDim
                        )
                    }
                )

                // Archive
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Archive",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = InnovexiaColors.DarkTextPrimary
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
                            tint = InnovexiaColors.DarkTextSecondary
                        )
                    }
                )

                // Move to Trash
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Move to Trash",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = InnovexiaColors.DarkTextPrimary
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
                            tint = InnovexiaColors.DarkTextSecondary
                        )
                    }
                )

                // Delete Permanently
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete Permanently",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = InnovexiaColors.Error
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
                            tint = InnovexiaColors.Error
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
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = InnovexiaColors.DarkTextPrimary
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
                            tint = InnovexiaColors.GoldDim
                        )
                    }
                )

                // Move to Trash
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Move to Trash",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = InnovexiaColors.DarkTextPrimary
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
                            tint = InnovexiaColors.DarkTextSecondary
                        )
                    }
                )

                // Delete Permanently
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete Permanently",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = InnovexiaColors.Error
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
                            tint = InnovexiaColors.Error
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
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = InnovexiaColors.DarkTextPrimary
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
                            tint = InnovexiaColors.GoldDim
                        )
                    }
                )

                // Delete Permanently
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Delete Permanently",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = InnovexiaColors.Error
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
                            tint = InnovexiaColors.Error
                        )
                    }
                )
            }
        }
    }
}
