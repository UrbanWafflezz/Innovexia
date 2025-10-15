package com.example.innovexia.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.models.ChatListItem
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.InnovexiaDesign
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * List of recent chats with day grouping and sticky headers
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentList(
    items: List<ChatListItem>,
    onClick: (String) -> Unit,
    onLongPress: (ChatListItem) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    multiSelectMode: Boolean = false,
    selectedChatIds: Set<String> = emptySet()
) {
    val listState = rememberLazyListState()

    // Separate pinned and unpinned items
    val (pinnedItems, unpinnedItems) = remember(items) {
        items.partition { it.pinned }
    }

    val grouped = remember(unpinnedItems) {
        unpinnedItems.sortedByDescending { it.updatedAt }.groupByDay()
    }

    if (items.isEmpty()) {
        // Empty state
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No chats yet",
                color = if (darkTheme) InnovexiaColors.DarkTextSecondary.copy(alpha = 0.6f)
                       else InnovexiaColors.LightTextSecondary.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = InnovexiaDesign.Spacing.LG,
                vertical = InnovexiaDesign.Spacing.MD
            )
        ) {
            // Pinned section
            if (pinnedItems.isNotEmpty()) {
                stickyHeader(key = "header_pinned") {
                    Text(
                        text = "Pinned",
                        color = if (darkTheme) InnovexiaColors.GoldDim.copy(alpha = 0.8f)
                               else InnovexiaColors.Gold.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }

                items(
                    count = pinnedItems.size,
                    key = { index -> "pinned_${pinnedItems[index].chatId}" }
                ) { index ->
                    val item = pinnedItems.sortedByDescending { it.updatedAt }[index]
                    RecentRow(
                        item = item,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        isPinned = true,
                        darkTheme = darkTheme,
                        multiSelectMode = multiSelectMode,
                        isSelected = item.chatId in selectedChatIds,
                        modifier = Modifier.animateItemPlacement(
                            animationSpec = tween(durationMillis = 300)
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                }

                // Spacer between pinned and regular
                item {
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Regular chats grouped by day
            grouped.forEach { (header, rows) ->
                stickyHeader(key = "header_$header") {
                    Text(
                        text = header,
                        color = if (darkTheme) InnovexiaColors.DarkTextSecondary.copy(alpha = 0.7f)
                               else InnovexiaColors.LightTextSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }

                items(
                    count = rows.size,
                    key = { index -> rows[index].chatId }
                ) { index ->
                    val item = rows[index]
                    RecentRow(
                        item = item,
                        onClick = onClick,
                        onLongPress = onLongPress,
                        darkTheme = darkTheme,
                        multiSelectMode = multiSelectMode,
                        isSelected = item.chatId in selectedChatIds,
                        modifier = Modifier.animateItemPlacement(
                            animationSpec = tween(durationMillis = 300)
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

/**
 * Group chat items by day
 */
private fun List<ChatListItem>.groupByDay(): Map<String, List<ChatListItem>> {
    val zone = ZoneId.systemDefault()
    val now = LocalDate.now(zone)

    return groupBy { item ->
        val date = Instant.ofEpochMilli(item.updatedAt)
            .atZone(zone)
            .toLocalDate()

        when (date) {
            now -> "Today"
            now.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    }
}
