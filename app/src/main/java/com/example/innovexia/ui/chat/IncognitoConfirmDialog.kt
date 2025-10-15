package com.example.innovexia.ui.chat

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Dialog shown when incognito mode is first enabled for a chat.
 * Explains that the chat will stay on device with no cloud sync.
 */
@Composable
fun IncognitoConfirmDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Incognito active") },
        text = {
            Text(
                "This chat stays on your device. No cloud sync, no backups, no analytics. " +
                "You can move it to Cloud later from the chat menu."
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
