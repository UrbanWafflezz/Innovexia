package com.example.innovexia.ui.persona.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Virtualized list of source items with sections
 */
@Composable
fun SourceList(
    items: List<SourceItemUi>,
    selectedIds: Set<String>,
    selecting: Boolean,
    onItemClick: (SourceItemUi) -> Unit,
    onItemLongPress: (SourceItemUi) -> Unit,
    onTogglePin: (String) -> Unit,
    onReindex: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpenDetails: (SourceItemUi) -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    // Separate pinned and unpinned items
    val pinnedItems = items.filter { it.pinned }
    val unpinnedItems = items.filterNot { it.pinned }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Pinned section
        if (pinnedItems.isNotEmpty()) {
            item(key = "section_pinned") {
                SectionHeader(
                    title = "Pinned",
                    darkTheme = darkTheme
                )
            }

            items(
                items = pinnedItems,
                key = { it.id }
            ) { item ->
                SourceCard(
                    item = item,
                    selected = item.id in selectedIds,
                    selecting = selecting,
                    onCardClick = { onItemClick(item) },
                    onLongPress = { onItemLongPress(item) },
                    onTogglePin = { onTogglePin(item.id) },
                    onReindex = { onReindex(item.id) },
                    onRemove = { onRemove(item.id) },
                    onOpenDetails = { onOpenDetails(item) },
                    darkTheme = darkTheme
                )
            }

            item(key = "spacer_pinned") {
                Spacer(Modifier.height(8.dp))
            }
        }

        // Recent section
        if (unpinnedItems.isNotEmpty()) {
            item(key = "section_recent") {
                SectionHeader(
                    title = "Recent",
                    darkTheme = darkTheme
                )
            }

            items(
                items = unpinnedItems,
                key = { it.id }
            ) { item ->
                SourceCard(
                    item = item,
                    selected = item.id in selectedIds,
                    selecting = selecting,
                    onCardClick = { onItemClick(item) },
                    onLongPress = { onItemLongPress(item) },
                    onTogglePin = { onTogglePin(item.id) },
                    onReindex = { onReindex(item.id) },
                    onRemove = { onRemove(item.id) },
                    onOpenDetails = { onOpenDetails(item) },
                    darkTheme = darkTheme
                )
            }
        }

        // Bottom spacer for batch action bar
        if (selecting && selectedIds.isNotEmpty()) {
            item(key = "bottom_spacer") {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Section header
 */
@Composable
private fun SectionHeader(
    title: String,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

/**
 * Batch action bar (appears at bottom when items are selected)
 */
@Composable
fun BatchActionBar(
    selectedCount: Int,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onReindex: () -> Unit,
    onRemove: () -> Unit,
    onClearSelection: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (darkTheme) Color(0xFF1E2530) else Color.White,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Count badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (darkTheme) InnovexiaColors.GoldDim.copy(alpha = 0.2f)
                                else InnovexiaColors.Gold.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$selectedCount",
                            color = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "selected",
                        color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                        fontSize = 14.sp
                    )
                }

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPin,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Pin",
                            tint = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onReindex,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reindex",
                            tint = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Remove",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    VerticalDivider(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(horizontal = 4.dp),
                        color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.3f)
                               else LightColors.SecondaryText.copy(alpha = 0.3f)
                    )
                    TextButton(
                        onClick = onClearSelection,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        )
                    ) {
                        Text("Clear", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * Footer hint with storage usage
 */
@Composable
fun SourcesFooter(
    storageUsedBytes: Long,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${formatStorageSize(storageUsedBytes)} of local storage used",
            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
            fontSize = 12.sp
        )
    }
}

private fun formatStorageSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
