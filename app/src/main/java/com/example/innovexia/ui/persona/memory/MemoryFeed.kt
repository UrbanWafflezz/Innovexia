package com.example.innovexia.ui.persona.memory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Chronological feed of memory items
 * Shows memory bubbles with metadata, chips, and actions
 */
@Composable
fun MemoryFeed(
    memories: List<MemoryItem>,
    onMemoryClick: (MemoryItem) -> Unit,
    onMemoryDelete: (MemoryItem) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    if (memories.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "\uD83D\uDCED",
                    fontSize = 48.sp
                )
                Text(
                    text = "No memories yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary
                )
                Text(
                    text = "Start chatting to help this persona learn about you",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(memories, key = { it.id }) { memory ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                MemoryItemCard(
                    memory = memory,
                    onClick = { onMemoryClick(memory) },
                    onDelete = { onMemoryDelete(memory) },
                    darkTheme = darkTheme
                )
            }
        }
    }
}

/**
 * Individual memory item card
 */
@Composable
private fun MemoryItemCard(
    memory: MemoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    darkTheme: Boolean
) {
    val cardGradient = if (darkTheme) {
        listOf(
            Color(0xFF1E2530).copy(alpha = 0.6f),
            Color(0xFF141A22).copy(alpha = 0.4f)
        )
    } else {
        listOf(
            Color.White.copy(alpha = 0.95f),
            Color(0xFFF8FAFC).copy(alpha = 0.8f)
        )
    }

    val borderColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(cardGradient))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: category icon + relative time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = memory.category.emoji,
                            fontSize = 16.sp
                        )
                        Text(
                            text = memory.category.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = memory.relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Memory text
                Text(
                    text = memory.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) InnovexiaColors.DarkTextPrimary.copy(alpha = 0.9f)
                            else InnovexiaColors.LightTextPrimary.copy(alpha = 0.9f),
                    lineHeight = 20.sp,
                    fontSize = 14.sp
                )

                // Meta chips and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chips: emotion + importance + chat
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Emotion chip
                        memory.emotion?.let { emotion ->
                            MetaChip(
                                text = emotion.emoji,
                                backgroundColor = getEmotionColor(emotion, darkTheme).copy(alpha = 0.15f),
                                textColor = getEmotionColor(emotion, darkTheme),
                                darkTheme = darkTheme
                            )
                        }

                        // Importance chip
                        MetaChip(
                            text = memory.importance.displayName,
                            backgroundColor = getImportanceColor(memory.importance, darkTheme).copy(alpha = 0.15f),
                            textColor = getImportanceColor(memory.importance, darkTheme),
                            darkTheme = darkTheme
                        )

                        // Chat title chip (if available)
                        memory.chatTitle?.let { title ->
                            if (title.isNotBlank()) {
                                MetaChip(
                                    text = title,
                                    backgroundColor = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    textColor = if (darkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                                    darkTheme = darkTheme
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete memory",
                                tint = if (darkTheme) Color(0xFFEF4444) else Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Meta chip component for displaying metadata
 */
@Composable
private fun MetaChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    darkTheme: Boolean
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        modifier = Modifier.height(22.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * Get color for emotion type
 */
private fun getEmotionColor(emotion: EmotionType, darkTheme: Boolean): Color {
    return when (emotion) {
        EmotionType.Positive -> if (darkTheme) Color(0xFF34D399) else Color(0xFF10B981)
        EmotionType.Neutral -> if (darkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
        EmotionType.Negative -> if (darkTheme) Color(0xFFF87171) else Color(0xFFEF4444)
        EmotionType.Excited -> if (darkTheme) Color(0xFFFBBF24) else Color(0xFFF59E0B)
        EmotionType.Curious -> if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
    }
}

/**
 * Get color for importance level
 */
private fun getImportanceColor(importance: ImportanceLevel, darkTheme: Boolean): Color {
    return when (importance) {
        ImportanceLevel.Low -> if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
        ImportanceLevel.Medium -> if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
        ImportanceLevel.High -> if (darkTheme) Color(0xFFF472B6) else Color(0xFFEC4899)
    }
}
