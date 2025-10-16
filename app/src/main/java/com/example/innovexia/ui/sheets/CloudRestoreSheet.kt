package com.example.innovexia.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.core.sync.CloudChatItem
import com.example.innovexia.ui.glass.GlassButton
import com.example.innovexia.ui.glass.GlassButtonStyle
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.InnovexiaTokens
import com.example.innovexia.ui.viewmodels.CloudRestoreViewModel
import com.example.innovexia.ui.viewmodels.FilterDeletedMode
import com.example.innovexia.ui.viewmodels.RestoreState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cloud Restore Sheet - allows users to restore chats from Firebase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudRestoreSheet(
    onDismiss: () -> Unit,
    onRestoreComplete: () -> Unit = {},
    onCountChanged: (Int) -> Unit = {}, // Callback when chat count changes
    darkTheme: Boolean = isSystemInDarkTheme(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val viewModel: CloudRestoreViewModel = viewModel { CloudRestoreViewModel(context) }

    val state by viewModel.state.collectAsState()
    val query by viewModel.query.collectAsState()
    val filterDeleted by viewModel.filterDeleted.collectAsState()
    val includeDeletedMessages by viewModel.includeDeletedMessages.collectAsState()
    val forceOverwrite by viewModel.forceOverwrite.collectAsState()
    val reviveInCloud by viewModel.reviveInCloud.collectAsState()
    val selectedChats by viewModel.selectedChats.collectAsState()

    var showOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var showResultDialog by rememberSaveable { mutableStateOf(false) }

    // Load chats on first launch
    LaunchedEffect(Unit) {
        viewModel.loadCloudChats()
    }

    // Show result dialog when done and trigger callback
    LaunchedEffect(state) {
        if (state is RestoreState.Done) {
            showResultDialog = true
            onRestoreComplete() // Notify parent that restore completed
        }
    }

    // Notify parent when chat count changes
    LaunchedEffect(state) {
        val currentState = state
        if (currentState is RestoreState.Ready) {
            onCountChanged(currentState.chats.size)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (darkTheme) InnovexiaTokens.Color.GraySurface else InnovexiaColors.LightSurface,
        contentColor = if (darkTheme) InnovexiaTokens.Color.TextPrimary else InnovexiaColors.LightTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header - Compact
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = "Restore",
                        tint = InnovexiaColors.BlueAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Restore from Cloud",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (darkTheme) InnovexiaTokens.Color.TextPrimary
                            else InnovexiaColors.LightTextPrimary
                        )
                        Text(
                            text = "Select chats to restore",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (darkTheme) InnovexiaTokens.Color.TextSecondary
                            else InnovexiaColors.LightTextSecondary
                        )
                    }
                }
            }

            // Search & Filter
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Search field
                TextField(
                    value = query,
                    onValueChange = { viewModel.setQuery(it) },
                    placeholder = {
                        Text(
                            "Search cloud chats...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = InnovexiaColors.BlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = if (darkTheme) InnovexiaTokens.Color.GrayCard
                        else InnovexiaColors.LightSurfaceElevated,
                        unfocusedContainerColor = if (darkTheme) InnovexiaTokens.Color.GrayCard
                        else InnovexiaColors.LightSurfaceElevated,
                        focusedIndicatorColor = InnovexiaColors.BlueAccent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = if (darkTheme) InnovexiaTokens.Color.TextPrimary else InnovexiaColors.LightTextPrimary,
                        unfocusedTextColor = if (darkTheme) InnovexiaTokens.Color.TextPrimary else InnovexiaColors.LightTextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                // Filter button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (darkTheme) InnovexiaTokens.Color.GrayCard
                            else InnovexiaColors.LightSurfaceElevated
                        )
                        .clickable { /* Cycle filter modes */
                            val nextMode = when (filterDeleted) {
                                FilterDeletedMode.ALL -> FilterDeletedMode.ONLY_DELETED
                                FilterDeletedMode.ONLY_DELETED -> FilterDeletedMode.ONLY_ACTIVE
                                FilterDeletedMode.ONLY_ACTIVE -> FilterDeletedMode.ALL
                            }
                            viewModel.setFilterDeleted(nextMode)
                        }
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = "Filter",
                        tint = InnovexiaColors.BlueAccent
                    )
                }
            }

            // Filter indicator
            if (filterDeleted != FilterDeletedMode.ALL) {
                Text(
                    text = when (filterDeleted) {
                        FilterDeletedMode.ONLY_DELETED -> "Showing only deleted chats"
                        FilterDeletedMode.ONLY_ACTIVE -> "Showing only active chats"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = InnovexiaColors.BlueAccent,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // State-based content
            when (val currentState = state) {
                is RestoreState.Idle, is RestoreState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = InnovexiaColors.BlueAccent
                            )
                            Text(
                                text = "Loading cloud chats...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (darkTheme) InnovexiaTokens.Color.TextSecondary
                                else InnovexiaColors.LightTextSecondary
                            )
                        }
                    }
                }

                is RestoreState.Ready -> {
                    val filteredChats = currentState.chats
                        .filter { chat ->
                            when (filterDeleted) {
                                FilterDeletedMode.ALL -> true
                                FilterDeletedMode.ONLY_DELETED -> chat.deletedAt != null
                                FilterDeletedMode.ONLY_ACTIVE -> chat.deletedAt == null
                            }
                        }
                        .filter { chat ->
                            if (query.isBlank()) true
                            else chat.title.contains(query, ignoreCase = true)
                        }

                    if (filteredChats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (query.isNotBlank()) "No chats match your search"
                                else "No chats found in cloud",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (darkTheme) InnovexiaTokens.Color.TextSecondary
                                else InnovexiaColors.LightTextSecondary
                            )
                        }
                    } else {
                        // Chat list
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredChats) { chat ->
                                CloudChatCard(
                                    chat = chat,
                                    selected = chat.id in selectedChats,
                                    onToggle = { viewModel.toggleChatSelection(chat.id) },
                                    darkTheme = darkTheme,
                                    onDelete = { chatId -> viewModel.deleteSingleChat(chatId) }
                                )
                            }
                        }

                        // Compact selection controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Select All - Compact
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (darkTheme) InnovexiaTokens.Color.GrayCard
                                        else InnovexiaColors.LightSurfaceElevated
                                    )
                                    .clickable { viewModel.selectAll(filteredChats.map { it.id }) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select All",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (darkTheme) InnovexiaTokens.Color.TextPrimary
                                    else InnovexiaColors.LightTextPrimary
                                )
                            }

                            // Clear - Compact
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (darkTheme) InnovexiaTokens.Color.GrayCard
                                        else InnovexiaColors.LightSurfaceElevated
                                    )
                                    .clickable(enabled = selectedChats.isNotEmpty()) {
                                        viewModel.clearSelection()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (selectedChats.isNotEmpty()) {
                                        if (darkTheme) InnovexiaTokens.Color.TextPrimary
                                        else InnovexiaColors.LightTextPrimary
                                    } else {
                                        if (darkTheme) InnovexiaTokens.Color.TextTertiary
                                        else InnovexiaColors.LightTextMuted
                                    }
                                )
                            }

                            // Options - Compact
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (darkTheme) InnovexiaTokens.Color.GrayCard
                                        else InnovexiaColors.LightSurfaceElevated
                                    )
                                    .clickable { showOptionsDialog = true }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = "Options",
                                    tint = if (darkTheme) InnovexiaTokens.Color.TextPrimary
                                    else InnovexiaColors.LightTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Action buttons row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Delete Selected
                            if (selectedChats.isNotEmpty()) {
                                var showDeleteConfirm by remember { mutableStateOf(false) }
                                GlassButton(
                                    text = "Delete (${selectedChats.size})",
                                    onClick = { showDeleteConfirm = true },
                                    style = GlassButtonStyle.Secondary,
                                    modifier = Modifier.weight(1f),
                                    darkTheme = darkTheme
                                )
                                if (showDeleteConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteConfirm = false },
                                        title = { Text("Delete Chats") },
                                        text = { Text("Delete ${selectedChats.size} selected chats from cloud? This cannot be undone.") },
                                        confirmButton = {
                                            GlassButton(
                                                text = "Delete",
                                                onClick = {
                                                    viewModel.deleteSelectedChats()
                                                    showDeleteConfirm = false
                                                },
                                                style = GlassButtonStyle.Primary,
                                                darkTheme = darkTheme
                                            )
                                        },
                                        dismissButton = {
                                            GlassButton(
                                                text = "Cancel",
                                                onClick = { showDeleteConfirm = false },
                                                style = GlassButtonStyle.Secondary,
                                                darkTheme = darkTheme
                                            )
                                        }
                                    )
                                }
                            }

                            // Restore Selected
                            GlassButton(
                                text = "Restore (${selectedChats.size})",
                                onClick = { viewModel.restoreSelected() },
                                style = GlassButtonStyle.Primary,
                                modifier = Modifier.weight(1f),
                                darkTheme = darkTheme,
                                enabled = selectedChats.isNotEmpty()
                            )
                        }

                        // Delete All button
                        var showDeleteAllConfirm by rememberSaveable { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(InnovexiaColors.Error.copy(alpha = 0.15f))
                                .clickable { showDeleteAllConfirm = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete All",
                                    tint = InnovexiaColors.Error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Delete All Cloud Chats",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = InnovexiaColors.Error
                                )
                            }
                        }

                        if (showDeleteAllConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteAllConfirm = false },
                                title = { Text("Delete All Chats") },
                                text = { Text("Delete ALL chats from cloud? This will permanently delete all your cloud data and cannot be undone.") },
                                confirmButton = {
                                    GlassButton(
                                        text = "Delete All",
                                        onClick = {
                                            viewModel.deleteAllChats()
                                            showDeleteAllConfirm = false
                                        },
                                        style = GlassButtonStyle.Primary,
                                        darkTheme = darkTheme
                                    )
                                },
                                dismissButton = {
                                    GlassButton(
                                        text = "Cancel",
                                        onClick = { showDeleteAllConfirm = false },
                                        style = GlassButtonStyle.Secondary,
                                        darkTheme = darkTheme
                                    )
                                }
                            )
                        }
                    }
                }

                is RestoreState.Restoring -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = InnovexiaColors.BlueAccent
                            )
                            Text(
                                text = "Restoring chats...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (darkTheme) InnovexiaTokens.Color.TextPrimary
                                else InnovexiaColors.LightTextPrimary
                            )
                            Text(
                                text = "${currentState.progress} / ${currentState.total}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (darkTheme) InnovexiaTokens.Color.TextSecondary
                                else InnovexiaColors.LightTextSecondary
                            )
                            LinearProgressIndicator(
                                progress = if (currentState.total > 0) {
                                    currentState.progress.toFloat() / currentState.total.toFloat()
                                } else 0f,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = InnovexiaColors.BlueAccent
                            )
                        }
                    }
                }

                is RestoreState.Deleting -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = InnovexiaColors.Error
                            )
                            Text(
                                text = "Deleting chats...",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (darkTheme) InnovexiaTokens.Color.TextPrimary
                                else InnovexiaColors.LightTextPrimary
                            )
                            Text(
                                text = "This may take a moment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (darkTheme) InnovexiaTokens.Color.TextSecondary
                                else InnovexiaColors.LightTextSecondary
                            )
                        }
                    }
                }

                is RestoreState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = InnovexiaColors.Error
                            )
                            Text(
                                text = currentState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (darkTheme) InnovexiaTokens.Color.TextSecondary
                                else InnovexiaColors.LightTextSecondary
                            )
                            GlassButton(
                                text = "Retry",
                                onClick = { viewModel.loadCloudChats() },
                                style = GlassButtonStyle.Primary,
                                darkTheme = darkTheme
                            )
                        }
                    }
                }

                is RestoreState.Done -> {
                    // Will be handled by dialog
                }
            }
        }
    }

    // Options Dialog
    if (showOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Restore Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OptionRow(
                        label = "Include deleted messages",
                        checked = includeDeletedMessages,
                        onCheckedChange = { viewModel.setIncludeDeletedMessages(it) }
                    )
                    OptionRow(
                        label = "Force overwrite local chats",
                        checked = forceOverwrite,
                        onCheckedChange = { viewModel.setForceOverwrite(it) }
                    )
                    OptionRow(
                        label = "Revive deleted chats in cloud",
                        checked = reviveInCloud,
                        onCheckedChange = { viewModel.setReviveInCloud(it) }
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Done",
                    onClick = { showOptionsDialog = false },
                    style = GlassButtonStyle.Primary,
                    darkTheme = darkTheme
                )
            }
        )
    }

    // Result Dialog
    if (showResultDialog && state is RestoreState.Done) {
        val result = (state as RestoreState.Done).result
        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                viewModel.resetState()
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Success",
                    tint = InnovexiaColors.BlueAccent
                )
            },
            title = { Text("Restore Complete") },
            text = {
                Text(
                    "${result.restoredCount} restored, ${result.skippedCount} skipped, ${result.errorCount} errors"
                )
            },
            confirmButton = {
                GlassButton(
                    text = "OK",
                    onClick = {
                        showResultDialog = false
                        viewModel.resetState()
                        onDismiss()
                    },
                    style = GlassButtonStyle.Primary,
                    darkTheme = darkTheme
                )
            }
        )
    }
}

@Composable
private fun CloudChatCard(
    chat: CloudChatItem,
    selected: Boolean,
    onToggle: () -> Unit,
    darkTheme: Boolean,
    onDelete: ((String) -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) InnovexiaColors.BlueAccent.copy(alpha = 0.12f)
                else if (darkTheme) InnovexiaTokens.Color.GrayCard
                else InnovexiaColors.LightSurfaceElevated
            )
            .border(
                1.dp,
                if (selected) InnovexiaColors.BlueAccent
                else if (darkTheme) InnovexiaTokens.Color.GrayStroke
                else InnovexiaColors.LightBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggle)
            .padding(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Selection indicator
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (selected) "Selected" else "Not selected",
                tint = if (selected) InnovexiaColors.BlueAccent
                else if (darkTheme) InnovexiaTokens.Color.TextSecondary
                else InnovexiaColors.LightTextSecondary,
                modifier = Modifier.size(22.dp)
            )

            // Chat info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (darkTheme) InnovexiaTokens.Color.TextPrimary
                        else InnovexiaColors.LightTextPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1
                    )
                    if (chat.deletedAt != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(InnovexiaColors.Error.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "DELETED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = InnovexiaColors.Error
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${chat.msgCount} messages",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) InnovexiaTokens.Color.TextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) InnovexiaTokens.Color.TextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                    Text(
                        text = formatDate(chat.lastMsgAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) InnovexiaTokens.Color.TextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                }
            }

            // Delete icon button
            if (onDelete != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(InnovexiaColors.Error.copy(alpha = 0.15f))
                        .clickable(onClick = { showDeleteConfirm = true })
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete chat",
                        tint = InnovexiaColors.Error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Chat") },
            text = { Text("Delete \"${chat.title}\" from cloud? This cannot be undone.") },
            confirmButton = {
                GlassButton(
                    text = "Delete",
                    onClick = {
                        onDelete(chat.id)
                        showDeleteConfirm = false
                    },
                    style = GlassButtonStyle.Primary,
                    darkTheme = darkTheme
                )
            },
            dismissButton = {
                GlassButton(
                    text = "Cancel",
                    onClick = { showDeleteConfirm = false },
                    style = GlassButtonStyle.Secondary,
                    darkTheme = darkTheme
                )
            }
        )
    }
}

@Composable
private fun OptionRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = InnovexiaColors.BlueAccent,
                checkedTrackColor = InnovexiaColors.BlueAccent.copy(alpha = 0.5f)
            )
        )
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown"
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return format.format(Date(timestamp))
}
