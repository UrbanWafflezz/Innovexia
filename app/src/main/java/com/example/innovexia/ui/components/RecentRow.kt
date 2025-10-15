package com.example.innovexia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.models.ChatListItem
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.InnovexiaDesign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

/**
 * Individual chat row in the drawer list
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentRow(
    item: ChatListItem,
    onClick: (String) -> Unit,
    onLongPress: (ChatListItem) -> Unit,
    modifier: Modifier = Modifier,
    isPinned: Boolean = false,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    multiSelectMode: Boolean = false,
    isSelected: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }

    val surfaceColor = when {
        isSelected -> InnovexiaColors.BlueAccent.copy(alpha = 0.15f)
        darkTheme -> {
            if (isPinned) InnovexiaColors.DarkSurfaceElevated.copy(alpha = 0.8f)
            else InnovexiaColors.DarkSurfaceElevated
        }
        else -> {
            if (isPinned) InnovexiaColors.LightSurfaceElevated.copy(alpha = 0.8f)
            else InnovexiaColors.LightSurfaceElevated
        }
    }

    val borderColor = when {
        isSelected -> InnovexiaColors.BlueAccent
        darkTheme -> {
            if (isPinned) InnovexiaColors.GoldDim.copy(alpha = 0.3f)
            else InnovexiaColors.DarkBorder.copy(alpha = 0.6f)
        }
        else -> {
            if (isPinned) InnovexiaColors.Gold.copy(alpha = 0.3f)
            else InnovexiaColors.LightBorder.copy(alpha = 0.6f)
        }
    }

    Surface(
        shape = RoundedCornerShape(InnovexiaDesign.Radius.Large),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = { onClick(item.chatId) },
                onLongClick = { onLongPress(item) }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Multi-select checkbox (replaces persona stack in multi-select mode)
            if (multiSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) InnovexiaColors.BlueAccent
                    else if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
            } else {
                // Persona stack
                PersonaStack(initials = item.personaInitials)
                Spacer(Modifier.width(12.dp))
            }

            // Title and message preview
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Pinned",
                            modifier = Modifier.size(14.dp),
                            tint = InnovexiaColors.GoldDim
                        )
                    }
                    Text(
                        text = item.title,
                        color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.lastMessage,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right side: cloud indicator and time
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Cloud sync indicator (small, minimal space)
                // Shows actual sync status based on cloudId
                if (item.isIncognito) {
                    // Incognito = Local only (will NEVER sync to cloud)
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "Local only (Incognito)",
                        modifier = Modifier.size(12.dp),
                        tint = if (darkTheme) InnovexiaColors.DarkTextSecondary.copy(alpha = 0.5f)
                               else InnovexiaColors.LightTextSecondary.copy(alpha = 0.5f)
                    )
                } else if (item.isSyncedToCloud) {
                    // Has been synced to cloud (has cloudId)
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = "Synced to cloud",
                        modifier = Modifier.size(12.dp),
                        tint = if (darkTheme) InnovexiaColors.BlueAccent.copy(alpha = 0.6f)
                               else InnovexiaColors.BlueAccent.copy(alpha = 0.5f)
                    )
                } else {
                    // Not synced yet (local only for now)
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "Local (not yet synced)",
                        modifier = Modifier.size(12.dp),
                        tint = if (darkTheme) InnovexiaColors.DarkTextSecondary.copy(alpha = 0.4f)
                               else InnovexiaColors.LightTextSecondary.copy(alpha = 0.4f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = formatTime(item.updatedAt),
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Format timestamp to show time (for today/yesterday) or date
 */
private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp) / 1000

    return when {
        // Less than 1 minute - show "Just now"
        diff < 60 -> "Just now"
        // Less than 1 hour - show minutes
        diff < 3600 -> "${diff / 60}m"
        // Less than 24 hours (today) - show time
        diff < 86_400 -> {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            timeFormat.format(Date(timestamp))
        }
        // Older - show date
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
