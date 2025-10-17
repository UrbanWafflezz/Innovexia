package com.example.innovexia.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
 * Material 3 design with improved states and elevation
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
    val isPressed by interaction.collectIsPressedAsState()

    // Animate scale when pressed
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )

    // Surface colors that blend with side menu background
    val surfaceColor = when {
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.secondary
        isPinned -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Surface(
        shape = RoundedCornerShape(InnovexiaDesign.Radius.Large),
        color = surfaceColor,
        border = BorderStroke(0.5.dp, borderColor),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = { onClick(item.chatId) },
                onLongClick = { onLongPress(item) }
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Multi-select checkbox (replaces persona stack in multi-select mode)
            if (multiSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Not selected",
                    tint = if (isSelected) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(14.dp))
            } else {
                // Persona stack
                PersonaStack(initials = item.personaInitials)
                Spacer(Modifier.width(14.dp))
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
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.lastMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right side: cloud indicator and time
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
            ) {
                // Cloud sync indicator (small, minimal space)
                // Shows actual sync status based on cloudId
                if (item.isIncognito) {
                    // Incognito = Local only (will NEVER sync to cloud)
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "Local only (Incognito)",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                } else if (item.isSyncedToCloud) {
                    // Has been synced to cloud (has cloudId)
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = "Synced to cloud",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                } else {
                    // Not synced yet (local only for now)
                    Icon(
                        imageVector = Icons.Rounded.CloudOff,
                        contentDescription = "Local (not yet synced)",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }

                Text(
                    text = formatTime(item.updatedAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
