package com.example.innovexia.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MergeType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog shown on first sign-in when guest chats exist.
 * Offers the user the option to merge guest data into their account.
 */
@Composable
fun MergeGuestChatsDialog(
    guestChatCount: Int,
    onMerge: () -> Unit,
    onKeepSeparate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
        icon = {
            Icon(
                imageVector = Icons.Outlined.MergeType,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Import Guest Chats?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "You have $guestChatCount chat${if (guestChatCount > 1) "s" else ""} from Guest mode on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Would you like to import them into your account?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "• Merge now: Guest chats will sync with your account",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• Keep separate: Guest chats stay local and hidden while signed in",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onMerge()
                    onDismiss()
                }
            ) {
                Text("Merge Now")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onKeepSeparate()
                    onDismiss()
                }
            ) {
                Text("Keep Separate")
            }
        }
    )
}

/**
 * Simplified version for Settings "Import Guest chats" feature.
 */
@Composable
fun ImportGuestChatsDialog(
    guestChatCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        icon = {
            Icon(
                imageVector = Icons.Outlined.MergeType,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Import Guest Chats?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Found $guestChatCount guest chat${if (guestChatCount > 1) "s" else ""} on this device.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Import them into your account? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
