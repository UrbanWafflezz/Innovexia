package com.example.innovexia.ui.chat

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Chat options menu shown from the overflow menu button.
 * Provides actions like "Move to Cloud", "Export", etc.
 */
@Composable
fun ChatOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isIncognito: Boolean,
    onMoveToCloud: () -> Unit,
    onExportChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        // Move to Cloud option (only visible when incognito is active)
        if (isIncognito) {
            DropdownMenuItem(
                text = { Text("Move to Cloud...") },
                onClick = {
                    onMoveToCloud()
                    onDismiss()
                }
            )
        }

        // Export chat option
        DropdownMenuItem(
            text = { Text("Export chat") },
            onClick = {
                onExportChat()
                onDismiss()
            }
        )
    }
}
