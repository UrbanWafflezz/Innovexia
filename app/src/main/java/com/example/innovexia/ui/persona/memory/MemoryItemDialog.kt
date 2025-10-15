package com.example.innovexia.ui.persona.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Dialog for viewing and editing memory details
 * UI placeholder for Phase 1 (no edit functionality yet)
 */
@Composable
fun MemoryItemDialog(
    memory: MemoryItem?,
    onDismiss: () -> Unit,
    onDelete: (MemoryItem) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    if (memory == null) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (darkTheme) Color(0xFF141A22) else Color.White,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (darkTheme) Color(0xFF253041).copy(alpha = 0.6f) else Color(0xFFE7EDF5).copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Memory Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary
                        )
                    }
                }

                HorizontalDivider(
                    color = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                )

                // Content (scrollable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category and time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = memory.category.emoji,
                                fontSize = 24.sp
                            )
                            Text(
                                text = memory.category.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
                            )
                        }
                        Text(
                            text = memory.relativeTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary
                        )
                    }

                    // Memory text
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (darkTheme) Color(0xFF1E2530) else Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = memory.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Metadata section
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Metadata",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
                        )

                        // Emotion
                        memory.emotion?.let { emotion ->
                            MetadataRow(
                                label = "Emotion",
                                value = "${emotion.emoji} ${emotion.displayName}",
                                darkTheme = darkTheme
                            )
                        }

                        // Importance
                        MetadataRow(
                            label = "Importance",
                            value = memory.importance.displayName,
                            darkTheme = darkTheme
                        )

                        // Chat title
                        memory.chatTitle?.let { title ->
                            MetadataRow(
                                label = "From Chat",
                                value = title,
                                darkTheme = darkTheme
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                )

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Delete button
                    OutlinedButton(
                        onClick = {
                            onDelete(memory)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (darkTheme) Color(0xFFEF4444) else Color(0xFFDC2626)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (darkTheme) Color(0xFFEF4444) else Color(0xFFDC2626)
                        )
                    ) {
                        Text("Delete")
                    }

                    // Close button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                            contentColor = if (darkTheme) Color(0xFF0F172A) else Color.White
                        )
                    ) {
                        Text("Close")
                    }
                }

                // Edit placeholder note (Phase 1)
                Text(
                    text = "✏️ Editing will be available in a future update",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
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
    darkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
            fontSize = 12.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
