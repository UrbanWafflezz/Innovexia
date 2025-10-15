package com.example.innovexia.ui.persona.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Read-only details dialog for a source item
 */
@Composable
fun SourceDetailsDialog(
    item: SourceItemUi,
    onDismiss: () -> Unit,
    onReindex: () -> Unit,
    onRemove: () -> Unit,
    darkTheme: Boolean
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = if (darkTheme) Color(0xFF141A22) else Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Source Details",
                        color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Icon + Title
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val (icon, color) = when (item.type) {
                            SourceType.URL -> Icons.Rounded.Language to (if (darkTheme) Color(0xFF4A9EFF) else Color(0xFF2196F3))
                            SourceType.FILE -> Icons.Rounded.Description to (if (darkTheme) Color(0xFFFF7043) else Color(0xFFFF5722))
                            SourceType.IMAGE -> Icons.Rounded.Image to (if (darkTheme) Color(0xFF66BB6A) else Color(0xFF4CAF50))
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Column {
                            Text(
                                text = item.title,
                                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (item.subtitle != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.subtitle,
                                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f)
                               else LightColors.SecondaryText.copy(alpha = 0.2f)
                    )

                    // Metadata grid
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Metadata",
                            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        MetadataRow(
                            label = "Type",
                            value = item.type.name,
                            darkTheme = darkTheme
                        )

                        item.sizeBytes?.let { bytes ->
                            MetadataRow(
                                label = "Size",
                                value = formatBytes(bytes),
                                darkTheme = darkTheme
                            )
                        }

                        item.pageCount?.let { pages ->
                            MetadataRow(
                                label = "Pages",
                                value = "$pages pages",
                                darkTheme = darkTheme
                            )
                        }

                        MetadataRow(
                            label = "Status",
                            value = item.status.name.replace("_", " "),
                            darkTheme = darkTheme,
                            valueColor = when (item.status) {
                                SourceStatus.READY -> if (darkTheme) Color(0xFF66BB6A) else Color(0xFF4CAF50)
                                SourceStatus.ERROR -> Color(0xFFEF5350)
                                SourceStatus.INDEXING -> if (darkTheme) Color(0xFF4A9EFF) else Color(0xFF2196F3)
                                else -> null
                            }
                        )

                        MetadataRow(
                            label = "Added",
                            value = formatDate(item.lastUpdated),
                            darkTheme = darkTheme
                        )

                        if (item.tags.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Tags",
                                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item.tags.forEach { tag ->
                                        Box(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)
                                                )
                                                .padding(horizontal = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tag,
                                                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Error message (if any)
                    if (item.status == SourceStatus.ERROR && item.errorMsg != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Error Details",
                                color = Color(0xFFEF5350),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEF5350).copy(alpha = 0.1f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = item.errorMsg,
                                    color = Color(0xFFEF5350),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Description placeholder
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Description",
                            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "No description yet.",
                            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                            fontSize = 14.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Preview */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Preview", fontSize = 14.sp)
                    }

                    OutlinedButton(
                        onClick = { /* TODO: Open */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                        )
                    ) {
                        Icon(
                            Icons.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Open", fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onReindex()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Reindex", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            onRemove()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Remove", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * Metadata row component
 */
@Composable
private fun MetadataRow(
    label: String,
    value: String,
    darkTheme: Boolean,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            color = valueColor ?: (if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText),
            fontSize = 14.sp,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
