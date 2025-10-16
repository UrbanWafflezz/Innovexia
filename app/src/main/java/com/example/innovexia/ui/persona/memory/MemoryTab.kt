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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.persona.Persona
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Main Memory Tab composable
 * Displays persona memory overview, categories, and feed
 * Phase 1: UI only with mock data
 */
@Composable
fun MemoryTab(
    persona: Persona,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    // UI state
    var memoryState by remember {
        mutableStateOf(
            MemoryUiState(
                isMemoryEnabled = true,
                selectedCategory = MemoryCategory.All,
                searchQuery = "",
                memories = generateMockMemories(),
                categorySummaries = emptyList()
            )
        )
    }

    // Generate category summaries from memories
    LaunchedEffect(memoryState.memories) {
        memoryState = memoryState.copy(
            categorySummaries = generateCategorySummaries(memoryState.memories)
        )
    }

    // Filter memories based on category and search
    val filteredMemories = remember(
        memoryState.selectedCategory,
        memoryState.searchQuery,
        memoryState.memories
    ) {
        memoryState.memories
            .filter { memory ->
                // Filter by category
                val categoryMatch = memoryState.selectedCategory == MemoryCategory.All ||
                        memory.category == memoryState.selectedCategory

                // Filter by search query
                val searchMatch = memoryState.searchQuery.isBlank() ||
                        memory.text.contains(memoryState.searchQuery, ignoreCase = true) ||
                        memory.category.displayName.contains(memoryState.searchQuery, ignoreCase = true) ||
                        memory.chatTitle?.contains(memoryState.searchQuery, ignoreCase = true) == true

                categoryMatch && searchMatch
            }
            .sortedByDescending { it.timestamp }
    }

    // Selected memory for detail dialog
    var selectedMemory by remember { mutableStateOf<MemoryItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        if (!memoryState.isMemoryEnabled) {
            // Memory disabled state
            MemoryDisabledState(
                onEnableMemory = {
                    memoryState = memoryState.copy(isMemoryEnabled = true)
                },
                darkTheme = darkTheme
            )
        } else {
            // Memory enabled - use LazyColumn for entire content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with toggle and info
                item {
                    MemoryHeader(
                        persona = persona,
                        isMemoryEnabled = memoryState.isMemoryEnabled,
                        onMemoryToggleChange = { enabled ->
                            memoryState = memoryState.copy(isMemoryEnabled = enabled)
                        },
                        darkTheme = darkTheme
                    )
                }

                // Search bar
                item {
                    MemorySearchBar(
                        searchQuery = memoryState.searchQuery,
                        onSearchQueryChange = { query ->
                            memoryState = memoryState.copy(searchQuery = query)
                        },
                        darkTheme = darkTheme
                    )
                }

                // Category tabs
                item {
                    MemoryCategoryTabs(
                        selectedCategory = memoryState.selectedCategory,
                        onCategorySelected = { category ->
                            memoryState = memoryState.copy(selectedCategory = category)
                        },
                        darkTheme = darkTheme
                    )
                }

                // Overview cards (only show when All category is selected)
                if (memoryState.selectedCategory == MemoryCategory.All && memoryState.searchQuery.isBlank()) {
                    item {
                        MemoryOverviewCards(
                            categorySummaries = memoryState.categorySummaries,
                            onCategoryClick = { category ->
                                memoryState = memoryState.copy(selectedCategory = category)
                            },
                            darkTheme = darkTheme
                        )
                    }
                }

                // Section header for feed
                if (filteredMemories.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    memoryState.searchQuery.isNotBlank() -> "Search Results"
                                    memoryState.selectedCategory == MemoryCategory.All -> "All Memories"
                                    else -> memoryState.selectedCategory.displayName
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
                            )
                            Text(
                                text = "${filteredMemories.size} ${if (filteredMemories.size == 1) "memory" else "memories"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Memory items
                if (filteredMemories.isEmpty()) {
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
                                    text = if (memoryState.searchQuery.isNotBlank()) {
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
                    items(filteredMemories, key = { it.id }) { memory ->
                        MemoryItemCard(
                            memory = memory,
                            onClick = { selectedMemory = memory },
                            onDelete = {
                                memoryState = memoryState.copy(
                                    memories = memoryState.memories.filter { it.id != memory.id }
                                )
                            },
                            darkTheme = darkTheme
                        )
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
            memoryState = memoryState.copy(
                memories = memoryState.memories.filter { it.id != memory.id }
            )
        },
        darkTheme = darkTheme
    )
}

/**
 * Individual memory item card (extracted from MemoryFeed)
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
                                textColor = getEmotionColor(emotion, darkTheme)
                            )
                        }

                        // Importance chip
                        MetaChip(
                            text = memory.importance.displayName,
                            backgroundColor = getImportanceColor(memory.importance, darkTheme).copy(alpha = 0.15f),
                            textColor = getImportanceColor(memory.importance, darkTheme)
                        )

                        // Chat title chip (if available)
                        memory.chatTitle?.let { title ->
                            if (title.isNotBlank()) {
                                MetaChip(
                                    text = title,
                                    backgroundColor = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                    textColor = if (darkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
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

/**
 * Empty state when memory is disabled
 */
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
