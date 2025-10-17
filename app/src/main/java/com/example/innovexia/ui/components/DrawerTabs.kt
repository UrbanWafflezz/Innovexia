package com.example.innovexia.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.models.ChatState

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

    // Animate indicator height
    val selectedIndex = tabs.indexOfFirst { it.first == tab }
    val indicatorHeight by animateDpAsState(
        targetValue = 3.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "indicatorHeight"
    )

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        indicator = { tabPositions ->
            if (selectedIndex >= 0) {
                Box(
                    modifier = Modifier
                        .tabIndicatorOffset(tabPositions[selectedIndex])
                        .height(indicatorHeight)
                        .padding(horizontal = 12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
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
                        fontSize = 14.sp
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
