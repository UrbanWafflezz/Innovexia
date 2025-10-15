package com.example.innovexia.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors
import kotlinx.coroutines.launch

/**
 * Memory control chips for chat header.
 * Provides quick access to memory on/off toggle and memory viewer.
 */
@Composable
fun MemoryChips(
    chatId: String?,
    onViewMemory: () -> Unit,
    darkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()

    var memoryEnabled by remember { mutableStateOf(true) }

    // Load memory state
    LaunchedEffect(chatId) {
        if (chatId != null) {
            val chat = database.chatDao().getById(chatId)
            memoryEnabled = chat?.memoryEnabled ?: true
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Memory On/Off Chip
        FilterChip(
            selected = memoryEnabled,
            onClick = {
                scope.launch {
                    if (chatId != null) {
                        val newState = !memoryEnabled
                        database.chatDao().setMemoryEnabled(chatId, newState)
                        memoryEnabled = newState
                    }
                }
            },
            label = {
                Text(
                    text = if (memoryEnabled) "Memory: On" else "Memory: Off",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (memoryEnabled) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = if (darkTheme) DarkColors.AccentBlue.copy(alpha = 0.2f) else LightColors.AccentBlue.copy(alpha = 0.2f),
                selectedLabelColor = if (darkTheme) DarkColors.AccentBlue else LightColors.AccentBlue,
                selectedLeadingIconColor = if (darkTheme) DarkColors.AccentBlue else LightColors.AccentBlue
            ),
            enabled = chatId != null
        )

        // View Memory Chip
        AssistChip(
            onClick = onViewMemory,
            label = {
                Text(
                    text = "View Memory",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (darkTheme) DarkColors.SurfaceElevated else LightColors.SurfaceElevated,
                labelColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
            ),
            enabled = chatId != null
        )
    }
}
