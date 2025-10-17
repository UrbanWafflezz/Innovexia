package com.example.innovexia.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.models.ChatListItem
import com.example.innovexia.ui.models.ChatState
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.InnovexiaDesign

/**
 * New side menu with tabs, fixed header/footer, and scrolling chat list
 */
@Composable
fun SideMenuNew(
    items: List<ChatListItem>,
    onNewChat: () -> Unit,
    onOpenChat: (String) -> Unit,
    onAboutClick: () -> Unit,
    onItemLongPress: (ChatListItem) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenUsage: () -> Unit,
    onOpenSubscription: () -> Unit,
    onManageSubscription: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Cloud restore parameters
    showCloudRestoreButton: Boolean = false,
    cloudChatCount: Int = 0,
    onRestoreFromCloud: () -> Unit = {},
    onHideCloudRestoreButton: () -> Unit = {},
    // Multi-select delete parameters
    onDeleteChats: (List<String>) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
    // Archive/Unarchive parameters
    onArchiveChats: (List<String>) -> Unit = {},
    onUnarchiveChats: (List<String>) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(ChatState.ACTIVE) }
    var showQuickPanel by remember { mutableStateOf(false) }
    var multiSelectMode by remember { mutableStateOf(false) }
    var selectedChatIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val focusManager = LocalFocusManager.current

    // Filter items based on selected tab and search query
    val filteredItems = remember(items, selectedTab, searchQuery) {
        items.filter { it.state == selectedTab }
            .filter { item ->
                if (searchQuery.isBlank()) true
                else item.title.contains(searchQuery, ignoreCase = true) ||
                     item.lastMessage.contains(searchQuery, ignoreCase = true)
            }
    }

    ModalDrawerSheet(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(max = 340.dp),
        drawerContainerColor = if (darkTheme) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Spacer(Modifier.height(20.dp))

                // Search bar with embedded New button
                DrawerSearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 20.dp),
                    darkTheme = darkTheme,
                    onNewChat = onNewChat
                )

                Spacer(Modifier.height(16.dp))

                // Cloud Restore Button (if visible)
                if (showCloudRestoreButton) {
                    CloudRestoreButtonCompact(
                        chatCount = cloudChatCount,
                        onClick = onRestoreFromCloud,
                        onHide = onHideCloudRestoreButton,
                        onOpenSettings = onOpenSettings,
                        darkTheme = darkTheme,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // Fixed tabs
                DrawerTabs(
                    tab = selectedTab,
                    onTabChange = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = 0.dp)
                )

                Spacer(Modifier.height(12.dp))

                // Multi-select action bar
                if (multiSelectMode) {
                    MultiSelectActionBar(
                        selectedCount = selectedChatIds.size,
                        currentTab = selectedTab,
                        onCancel = {
                            multiSelectMode = false
                            selectedChatIds = emptySet()
                        },
                        onSelectAll = {
                            selectedChatIds = filteredItems.map { it.chatId }.toSet()
                        },
                        onDelete = {
                            onDeleteChats(selectedChatIds.toList())
                            multiSelectMode = false
                            selectedChatIds = emptySet()
                        },
                        onEmptyTrash = {
                            onEmptyTrash()
                            multiSelectMode = false
                            selectedChatIds = emptySet()
                        },
                        onArchive = {
                            onArchiveChats(selectedChatIds.toList())
                            multiSelectMode = false
                            selectedChatIds = emptySet()
                        },
                        onUnarchive = {
                            onUnarchiveChats(selectedChatIds.toList())
                            multiSelectMode = false
                            selectedChatIds = emptySet()
                        },
                        darkTheme = darkTheme,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Scrolling list area
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    RecentList(
                        items = filteredItems,
                        onClick = if (multiSelectMode) { chatId ->
                            selectedChatIds = if (chatId in selectedChatIds) {
                                selectedChatIds - chatId
                            } else {
                                selectedChatIds + chatId
                            }
                        } else onOpenChat,
                        onLongPress = { item ->
                            if (!multiSelectMode) {
                                multiSelectMode = true
                                selectedChatIds = setOf(item.chatId)
                            } else {
                                onItemLongPress(item)
                            }
                        },
                        darkTheme = darkTheme,
                        multiSelectMode = multiSelectMode,
                        selectedChatIds = selectedChatIds
                    )
                }

                // Fixed footer - Profile card
                DrawerFooterProfile(
                    onOpenQuickPanel = { showQuickPanel = true }
                )
            }
        }
    }

    // Account Quick Panel
    if (showQuickPanel) {
        AccountQuickPanel(
            onDismiss = { showQuickPanel = false },
            onProfile = { onOpenProfile() },
            onUsage = { onOpenUsage() },
            onSubscription = { onOpenSubscription() },
            onManageSubscription = { onManageSubscription() },
            onSettings = { onOpenSettings() },
            onLogout = { onLogout() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CloudRestoreButtonCompact(
    chatCount: Int,
    onClick: () -> Unit,
    onHide: () -> Unit,
    onOpenSettings: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showMenu = true
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (darkTheme) Color(0xFF1E3A8A).copy(alpha = 0.2f)
                else Color(0xFFDBEAFE).copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (darkTheme) Color(0xFF4A9EFF).copy(alpha = 0.4f)
                    else Color(0xFF3B82F6).copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cloud icon
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = "Restore from Cloud",
                modifier = Modifier.size(20.dp),
                tint = if (darkTheme) Color(0xFF60A5FA)
                       else Color(0xFF2563EB)
            )

            // Text
            Text(
                text = if (chatCount > 0) "Restore from Cloud ($chatCount)" else "Restore from Cloud",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = if (darkTheme) Color(0xFF60A5FA)
                        else Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )
        }

        // Context menu for long press
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier
                .background(
                    if (darkTheme) Color(0xFF1F1F1F) else Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (darkTheme) Color(0xFF404040) else Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Hide this button",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    )
                },
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    showMenu = false
                    onHide()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Cloud Settings",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    )
                },
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    showMenu = false
                    onOpenSettings()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}

/**
 * Multi-select action bar for batch operations
 * Material 3 design with filled tonal buttons and proper elevation
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiSelectActionBar(
    selectedCount: Int,
    currentTab: ChatState,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onEmptyTrash: () -> Unit,
    onArchive: () -> Unit,
    onUnarchive: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(InnovexiaDesign.Radius.Large),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Cancel button with count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onCancel,
                    shape = RoundedCornerShape(InnovexiaDesign.Radius.Medium),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel selection",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Select All
                Surface(
                    onClick = onSelectAll,
                    shape = RoundedCornerShape(InnovexiaDesign.Radius.Medium),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 1.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Select all",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Archive button (only show in Active tab)
                if (currentTab == ChatState.ACTIVE) {
                    Surface(
                        onClick = onArchive,
                        enabled = selectedCount > 0,
                        shape = RoundedCornerShape(InnovexiaDesign.Radius.Medium),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        tonalElevation = 1.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Archive,
                                contentDescription = "Archive selected",
                                tint = if (selectedCount > 0)
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                else
                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.38f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Unarchive button (only show in Archived tab)
                if (currentTab == ChatState.ARCHIVED) {
                    Surface(
                        onClick = onUnarchive,
                        enabled = selectedCount > 0,
                        shape = RoundedCornerShape(InnovexiaDesign.Radius.Medium),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        tonalElevation = 1.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Unarchive,
                                contentDescription = "Unarchive selected",
                                tint = if (selectedCount > 0)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Empty Trash (only show in trash tab)
                if (currentTab == ChatState.TRASH) {
                    Surface(
                        onClick = onEmptyTrash,
                        shape = RoundedCornerShape(InnovexiaDesign.Radius.Medium),
                        color = MaterialTheme.colorScheme.errorContainer,
                        tonalElevation = 1.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Empty trash",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Delete Selected
                Surface(
                    onClick = onDelete,
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(InnovexiaDesign.Radius.Medium),
                    color = MaterialTheme.colorScheme.errorContainer,
                    tonalElevation = 1.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete selected",
                            tint = if (selectedCount > 0)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
