package com.example.innovexia.ui.persona.sources

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Card component for displaying a source item
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceCard(
    item: SourceItemUi,
    selected: Boolean,
    selecting: Boolean,
    onCardClick: () -> Unit,
    onLongPress: () -> Unit,
    onTogglePin: () -> Unit,
    onReindex: () -> Unit,
    onRemove: () -> Unit,
    onOpenDetails: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 0.98f else 1f,
        label = "card_scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            if (darkTheme) InnovexiaColors.GoldDim.copy(alpha = 0.1f) else InnovexiaColors.Gold.copy(alpha = 0.1f)
        } else {
            if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)
        },
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox (when selecting) or Preview
            if (selecting) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                        uncheckedColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                )
            } else {
                SourcePreview(
                    type = item.type,
                    subtitle = item.subtitle,
                    darkTheme = darkTheme
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title
                Text(
                    text = item.title,
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle
                if (item.subtitle != null) {
                    Text(
                        text = item.subtitle,
                        color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Metadata chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type badge
                    MetadataChip(
                        text = item.type.name,
                        darkTheme = darkTheme
                    )

                    // Size/pages
                    when {
                        item.pageCount != null -> MetadataChip(
                            text = "${item.pageCount} pages",
                            darkTheme = darkTheme
                        )
                        item.sizeBytes != null -> MetadataChip(
                            text = formatBytes(item.sizeBytes),
                            darkTheme = darkTheme
                        )
                    }

                    // Tags
                    item.tags.take(2).forEach { tag ->
                        MetadataChip(
                            text = tag,
                            darkTheme = darkTheme
                        )
                    }
                }

                // Status pill
                StatusPill(
                    status = item.status,
                    errorMsg = item.errorMsg,
                    darkTheme = darkTheme
                )
            }

            // Overflow menu (when not selecting)
            if (!selecting) {
                var showMenu by remember { mutableStateOf(false) }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(
                            if (darkTheme) Color(0xFF1E2530) else Color.White,
                            RoundedCornerShape(12.dp)
                        )
                    ) {
                        DropdownMenuItem(
                            text = { Text("Preview", fontSize = 14.sp) },
                            onClick = {
                                onOpenDetails()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open", fontSize = 14.sp) },
                            onClick = {
                                // TODO: Open in browser/file
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (item.pinned) "Unpin" else "Pin", fontSize = 14.sp) },
                            onClick = {
                                onTogglePin()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (item.pinned) Icons.Rounded.PushPin else Icons.Rounded.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reindex", fontSize = 14.sp) },
                            onClick = {
                                onReindex()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f) else LightColors.SecondaryText.copy(alpha = 0.2f)
                        )
                        DropdownMenuItem(
                            text = { Text("Remove", fontSize = 14.sp, color = Color.Red) },
                            onClick = {
                                onRemove()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview icon based on source type
 */
@Composable
private fun SourcePreview(
    type: SourceType,
    subtitle: String?,
    darkTheme: Boolean
) {
    val (icon, color) = when (type) {
        SourceType.URL -> Icons.Rounded.Language to (if (darkTheme) Color(0xFF4A9EFF) else Color(0xFF2196F3))
        SourceType.FILE -> Icons.Rounded.Description to (if (darkTheme) Color(0xFFFF7043) else Color(0xFFFF5722))
        SourceType.IMAGE -> Icons.Rounded.Image to (if (darkTheme) Color(0xFF66BB6A) else Color(0xFF4CAF50))
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Small metadata chip
 */
@Composable
private fun MetadataChip(
    text: String,
    darkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (darkTheme) Color(0xFF0F1419) else Color(0xFFE7EDF5)
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Status pill with color coding
 */
@Composable
private fun StatusPill(
    status: SourceStatus,
    errorMsg: String?,
    darkTheme: Boolean
) {
    val (text, color) = when (status) {
        SourceStatus.NOT_INDEXED -> "Not indexed" to (if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText)
        SourceStatus.INDEXING -> "Indexing" to (if (darkTheme) Color(0xFF4A9EFF) else Color(0xFF2196F3))
        SourceStatus.READY -> "Ready" to (if (darkTheme) Color(0xFF66BB6A) else Color(0xFF4CAF50))
        SourceStatus.ERROR -> "Error" to Color(0xFFEF5350)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (status == SourceStatus.INDEXING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = color,
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Error tooltip hint
        if (status == SourceStatus.ERROR && errorMsg != null) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = errorMsg,
                tint = Color(0xFFEF5350),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Format bytes to human-readable format
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
