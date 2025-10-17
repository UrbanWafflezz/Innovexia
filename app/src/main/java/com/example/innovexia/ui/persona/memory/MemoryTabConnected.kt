package com.example.innovexia.ui.persona.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.ui.persona.Persona
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Memory Tab connected to real backend
 * Replaces mock data with actual Room database
 * Now supports external search query from top search bar
 */
@Composable
fun MemoryTabConnected(
    persona: Persona,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    searchQuery: String = "", // External search query from parent
    viewModel: MemoryViewModel = viewModel()
) {
    // State
    var memoryEnabled by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf(MemoryCategory.All) }
    var selectedMemory by remember { mutableStateOf<MemoryItem?>(null) }
    var currentPage by remember { mutableStateOf(0) }

    val itemsPerPage = 15

    // Load memory enabled state
    LaunchedEffect(persona.id) {
        memoryEnabled = viewModel.isMemoryEnabled(persona.id)
    }

    // Observe category counts
    val categorySummaries by viewModel.observeCounts(persona.id)
        .collectAsState(initial = emptyList())

    // Observe memory feed
    val allMemories by viewModel.observeFeed(persona.id, selectedCategory, searchQuery)
        .collectAsState(initial = emptyList())

    // Reset to page 0 when filters or search query changes
    LaunchedEffect(selectedCategory, searchQuery) {
        currentPage = 0
    }

    // Calculate pagination
    val totalPages = (allMemories.size + itemsPerPage - 1) / itemsPerPage
    val startIndex = currentPage * itemsPerPage
    val endIndex = minOf(startIndex + itemsPerPage, allMemories.size)
    val memories = allMemories.subList(
        startIndex.coerceIn(0, allMemories.size),
        endIndex.coerceIn(0, allMemories.size)
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (!memoryEnabled) {
            // Memory disabled state
            MemoryDisabledState(
                onEnableMemory = {
                    viewModel.setMemoryEnabled(persona.id, true)
                    memoryEnabled = true
                },
                darkTheme = darkTheme
            )
        } else {
            // Memory enabled - show full UI
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category tabs
                item {
                    MemoryCategoryTabs(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            selectedCategory = category
                        },
                        darkTheme = darkTheme
                    )
                }

                // Section header for feed with Material 3 design
                if (memories.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (darkTheme) Color(0xFF1E2530).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        searchQuery.isNotBlank() -> "Search Results"
                                        selectedCategory == MemoryCategory.All -> "All Memories"
                                        else -> selectedCategory.displayName
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (darkTheme) InnovexiaColors.GoldDim.copy(alpha = 0.2f) else InnovexiaColors.Gold.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${allMemories.size} ${if (allMemories.size == 1) "memory" else "memories"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Memory items
                if (memories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp),
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
                                    text = "No memories found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary
                                )
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        "Try a different search"
                                    } else {
                                        "Start chatting to help this persona learn about you"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(memories, key = { it.id }) { memory ->
                        MemoryItemCard(
                            memory = memory,
                            onClick = { selectedMemory = memory },
                            onDelete = {
                                viewModel.deleteMemory(memory.id)
                            },
                            darkTheme = darkTheme
                        )
                    }

                    // Pagination controls
                    if (totalPages > 1) {
                        item {
                            PaginationControls(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                onPreviousPage = {
                                    if (currentPage > 0) currentPage--
                                },
                                onNextPage = {
                                    if (currentPage < totalPages - 1) currentPage++
                                },
                                onPageSelected = { page ->
                                    currentPage = page
                                },
                                darkTheme = darkTheme
                            )
                        }
                    }
                }
            }
        }
    }

    // Memory detail dialog
    MemoryItemDialog(
        memory = selectedMemory,
        onDismiss = { selectedMemory = null },
        onDelete = { memory ->
            viewModel.deleteMemory(memory.id)
        },
        darkTheme = darkTheme
    )
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
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
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
                    // Chips row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        memory.emotion?.let { emotion ->
                            MetaChip(
                                text = emotion.emoji,
                                backgroundColor = getEmotionColor(emotion, darkTheme).copy(alpha = 0.15f),
                                textColor = getEmotionColor(emotion, darkTheme)
                            )
                        }

                        MetaChip(
                            text = memory.importance.displayName,
                            backgroundColor = getImportanceColor(memory.importance, darkTheme).copy(alpha = 0.15f),
                            textColor = getImportanceColor(memory.importance, darkTheme)
                        )
                    }

                    // Delete button
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

@Composable
private fun MetaChip(
    text: String,
    backgroundColor: Color,
    textColor: Color
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

private fun getEmotionColor(emotion: EmotionType, darkTheme: Boolean): Color {
    return when (emotion) {
        EmotionType.Positive -> if (darkTheme) Color(0xFF34D399) else Color(0xFF10B981)
        EmotionType.Neutral -> if (darkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
        EmotionType.Negative -> if (darkTheme) Color(0xFFF87171) else Color(0xFFEF4444)
        EmotionType.Excited -> if (darkTheme) Color(0xFFFBBF24) else Color(0xFFF59E0B)
        EmotionType.Curious -> if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
    }
}

private fun getImportanceColor(importance: ImportanceLevel, darkTheme: Boolean): Color {
    return when (importance) {
        ImportanceLevel.Low -> if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
        ImportanceLevel.Medium -> if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
        ImportanceLevel.High -> if (darkTheme) Color(0xFFF472B6) else Color(0xFFEC4899)
    }
}

@Composable
private fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageSelected: (Int) -> Unit,
    darkTheme: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Page info
        Text(
            text = "Page ${currentPage + 1} of $totalPages",
            style = MaterialTheme.typography.bodySmall,
            color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
            fontSize = 12.sp
        )

        // Navigation buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            Button(
                onClick = onPreviousPage,
                enabled = currentPage > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                    contentColor = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary,
                    disabledContainerColor = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    disabledContentColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Previous", fontSize = 13.sp)
            }

            // Page numbers (show up to 5 pages)
            val pagesToShow = when {
                totalPages <= 5 -> (0 until totalPages).toList()
                currentPage < 3 -> (0..4).toList()
                currentPage > totalPages - 4 -> (totalPages - 5 until totalPages).toList()
                else -> listOf(currentPage - 2, currentPage - 1, currentPage, currentPage + 1, currentPage + 2)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pagesToShow.forEach { page ->
                    val isCurrentPage = page == currentPage
                    Surface(
                        onClick = { onPageSelected(page) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCurrentPage) {
                            if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold
                        } else {
                            if (darkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                        },
                        border = if (!isCurrentPage) {
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                            )
                        } else null,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = (page + 1).toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isCurrentPage) {
                                    if (darkTheme) Color(0xFF0F172A) else Color.White
                                } else {
                                    if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary
                                },
                                fontSize = 13.sp,
                                fontWeight = if (isCurrentPage) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Next button
            Button(
                onClick = onNextPage,
                enabled = currentPage < totalPages - 1,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                    contentColor = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary,
                    disabledContainerColor = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    disabledContentColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Next", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MemoryDisabledState(
    onEnableMemory: () -> Unit,
    darkTheme: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "\uD83D\uDEAB",
                fontSize = 64.sp
            )
            Text(
                text = "Memory is off for this persona",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
            )
            Text(
                text = "Enable it to allow this persona to remember and learn from your conversations.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onEnableMemory,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                    contentColor = if (darkTheme) Color(0xFF0F172A) else Color.White
                )
            ) {
                Text("Enable Memory")
            }
        }
    }
}
