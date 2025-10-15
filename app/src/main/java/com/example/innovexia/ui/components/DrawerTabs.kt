package com.example.innovexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.models.ChatState
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Tabs for switching between Chats, Archived, and Trash
 */
@Composable
fun DrawerTabs(
    tab: ChatState,
    onTabChange: (ChatState) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        ChatState.ACTIVE to "Chats",
        ChatState.ARCHIVED to "Archived",
        ChatState.TRASH to "Trash"
    )

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOfFirst { it.first == tab },
        modifier = modifier.fillMaxWidth(),
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        contentColor = InnovexiaColors.DarkTextPrimary,
        indicator = { tabPositions ->
            if (tabs.indexOfFirst { it.first == tab } >= 0) {
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[tabs.indexOfFirst { it.first == tab }])
                        .height(3.dp)
                        .padding(horizontal = 12.dp)
                        .background(
                            color = InnovexiaColors.GoldDim,
                            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                        )
                )
            }
        },
        divider = {}
    ) {
        tabs.forEach { (key, label) ->
            Tab(
                selected = (key == tab),
                onClick = { onTabChange(key) },
                text = {
                    Text(
                        text = label,
                        fontWeight = if (key == tab) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (key == tab) InnovexiaColors.DarkTextPrimary
                               else InnovexiaColors.DarkTextSecondary,
                        fontSize = 14.sp
                    )
                },
                selectedContentColor = InnovexiaColors.DarkTextPrimary,
                unselectedContentColor = InnovexiaColors.DarkTextSecondary
            )
        }
    }
}
