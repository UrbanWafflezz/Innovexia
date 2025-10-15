package com.example.innovexia.ui.sheets.personas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.persona.*
import com.example.innovexia.ui.theme.InnovexiaTheme

/**
 * Sources & Memory tab with responsive layout.
 * On narrow screens: stacked vertically.
 * On wider screens: side-by-side with BoxWithConstraints.
 */
@Composable
fun PersonasTabSourcesMemory(
    sources: SourcesState,
    memory: MemoryState,
    query: String,
    onSourcesChange: (SourcesState) -> Unit,
    onMemoryChange: (MemoryState) -> Unit,
    modifier: Modifier = Modifier,
    startWithMemory: Boolean = false
) {
    val configuration = LocalConfiguration.current
    val maxWidth = configuration.screenWidthDp.dp

    // Responsive: stack vertically on narrow, side-by-side on wide
    if (maxWidth < 600.dp) {
        // Narrow: show Sources or Memory based on tab
        // Use single LazyColumn for the entire tab content
        if (startWithMemory) {
            MemorySection(
                memory = memory,
                query = query,
                onMemoryChange = onMemoryChange,
                modifier = modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(horizontal = 16.dp)
            )
        } else {
            SourcesSection(
                sources = sources,
                onSourcesChange = onSourcesChange,
                modifier = modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .imePadding()
                    .padding(horizontal = 16.dp)
            )
        }
    } else {
        // Wide: side-by-side
        Row(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SourcesSection(
                sources = sources,
                onSourcesChange = onSourcesChange,
                modifier = Modifier.weight(1f)
            )
            MemorySection(
                memory = memory,
                query = query,
                onMemoryChange = onMemoryChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Sources Section
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourcesSection(
    sources: SourcesState,
    onSourcesChange: (SourcesState) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingUrl by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Tip banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = InnovexiaTheme.colors.searchBg,
                border = BorderStroke(1.dp, InnovexiaTheme.colors.searchBorder)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Attach web links or files to enrich this persona. (Demo)",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                }
            }
        }

        // Add URL row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = pendingUrl,
                        onValueChange = { pendingUrl = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(InnovexiaTheme.colors.searchBg),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        decorationBox = @Composable { innerTextField ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (pendingUrl.isEmpty()) {
                                    Text(
                                        text = "Paste URL…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InnovexiaTheme.colors.personaMutedText
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Button(
                        onClick = {
                            if (pendingUrl.startsWith("http")) {
                                val newSource = SourceItem(
                                    id = "src_${System.currentTimeMillis()}",
                                    kind = SourceKind.Url,
                                    label = pendingUrl.substringAfter("//").substringBefore("/"),
                                    detail = pendingUrl.substringAfter("//").substringBefore("/")
                                )
                                onSourcesChange(sources.copy(items = sources.items + newSource))
                                pendingUrl = ""
                            }
                        },
                        enabled = pendingUrl.startsWith("http"),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Add")
                    }
                }

                OutlinedButton(
                    onClick = { /* TODO: show toast */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = InnovexiaTheme.colors.personaMutedText
                    ),
                    border = BorderStroke(1.dp, InnovexiaTheme.colors.searchBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add file…")
                }
            }
        }

        // Source list
        items(sources.items, key = { it.id }) { source ->
            SourceItemCard(
                source = source,
                onToggle = { /* TODO */ },
                onRemove = {
                    onSourcesChange(
                        sources.copy(items = sources.items.filter { it.id != source.id })
                    )
                },
                modifier = Modifier.animateItemPlacement()
            )
        }

        // Empty state
        if (sources.items.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Source,
                    message = "No sources attached yet"
                )
            }
        }
    }
}

@Composable
private fun SourceItemCard(
    source: SourceItem,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enabled by rememberSaveable { mutableStateOf(true) }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InnovexiaTheme.colors.personaCardBg),
        border = BorderStroke(1.dp, InnovexiaTheme.colors.personaCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (source.kind) {
                    SourceKind.Url -> Icons.Default.Language
                    SourceKind.File -> Icons.Default.AttachFile
                },
                contentDescription = null,
                tint = if (enabled) Color(0xFF60A5FA) else Color(0xFF475569),
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else InnovexiaTheme.colors.personaMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = InnovexiaTheme.colors.personaMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    onToggle()
                }
            )

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = InnovexiaTheme.colors.personaMutedText
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Memory Section
// ═════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemorySection(
    memory: MemoryState,
    query: String,
    onMemoryChange: (MemoryState) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddMemory by rememberSaveable { mutableStateOf(false) }
    var newMemoryText by rememberSaveable { mutableStateOf("") }

    val filteredMemories = memory.items.filter {
        val matchesFilter = memory.filter == null || it.scope == memory.filter
        val matchesSearch = it.text.contains(query, ignoreCase = true)
        matchesFilter && matchesSearch
    }.sortedWith(compareByDescending<MemoryItem> { it.pinned }.thenByDescending { it.createdAt })

    LazyColumn(
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Filter row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InnovexiaTheme.colors.searchBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf<MemoryScope?>(null, MemoryScope.Global, MemoryScope.Chat, MemoryScope.Persona).forEach { scope ->
                    val selected = memory.filter == scope
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) Color(0xFF334155) else Color.Transparent)
                            .clickable {
                                onMemoryChange(memory.copy(filter = scope))
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = scope?.name ?: "All",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else InnovexiaTheme.colors.personaMutedText
                        )
                    }
                }
            }
        }

        // Add memory button
        item {
            Button(
                onClick = { showAddMemory = !showAddMemory },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InnovexiaTheme.colors.searchBg,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Memory")
            }
        }

        // Add memory editor
        if (showAddMemory) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = InnovexiaTheme.colors.personaCardBg),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BasicTextField(
                            value = newMemoryText,
                            onValueChange = { newMemoryText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(InnovexiaTheme.colors.searchBg)
                                .padding(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = @Composable { innerTextField ->
                                if (newMemoryText.isEmpty()) {
                                    Text(
                                        text = "Enter memory text…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InnovexiaTheme.colors.personaMutedText
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    showAddMemory = false
                                    newMemoryText = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    if (newMemoryText.isNotBlank()) {
                                        val newMemory = MemoryItem(
                                            id = "mem_${System.currentTimeMillis()}",
                                            scope = MemoryScope.Persona,
                                            text = newMemoryText,
                                            createdAt = "Just now",
                                            pinned = false
                                        )
                                        onMemoryChange(memory.copy(items = memory.items + newMemory))
                                        showAddMemory = false
                                        newMemoryText = ""
                                    }
                                },
                                enabled = newMemoryText.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }

        // Memory items
        items(filteredMemories, key = { it.id }) { memoryItem ->
            MemoryItemCard(
                memory = memoryItem,
                onPin = {
                    val updated = memory.items.map { m ->
                        if (m.id == memoryItem.id) m.copy(pinned = !m.pinned) else m
                    }
                    onMemoryChange(memory.copy(items = updated))
                },
                onEdit = { /* TODO */ },
                onDelete = {
                    onMemoryChange(
                        memory.copy(items = memory.items.filter { it.id != memoryItem.id })
                    )
                },
                modifier = Modifier.animateItemPlacement()
            )
        }

        // Empty state
        if (filteredMemories.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Memory,
                    message = if (query.isEmpty()) {
                        "No memories stored yet"
                    } else {
                        "No memories match your search"
                    }
                )
            }
        }
    }
}

@Composable
private fun MemoryItemCard(
    memory: MemoryItem,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InnovexiaTheme.colors.personaCardBg),
        border = BorderStroke(
            1.dp,
            if (memory.pinned) MaterialTheme.colorScheme.primary else InnovexiaTheme.colors.personaCardBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (memory.scope) {
                            MemoryScope.Global -> Color(0xFF1E40AF)
                            MemoryScope.Chat -> Color(0xFF047857)
                            MemoryScope.Persona -> Color(0xFF7C3AED)
                        },
                        contentColor = Color.White
                    ) {
                        Text(
                            text = memory.scope.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = memory.createdAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                }

                Row {
                    IconButton(
                        onClick = onPin,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (memory.pinned) Icons.Filled.PushPin else Icons.Default.PushPin,
                            contentDescription = if (memory.pinned) "Unpin" else "Pin",
                            tint = if (memory.pinned) MaterialTheme.colorScheme.primary else InnovexiaTheme.colors.personaMutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = InnovexiaTheme.colors.personaMutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Body
            Text(
                text = memory.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis
            )

            if (memory.text.length > 200 && !expanded) {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Show more", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Previews
// ═════════════════════════════════════════════════════════════════════════════

@Preview(name = "Sources & Memory - Narrow", showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 360)
@Composable
private fun PersonasTabSourcesMemoryPreview_Narrow() {
    InnovexiaTheme(darkTheme = true) {
        PersonasTabSourcesMemory(
            sources = SourcesState(items = demoSources()),
            memory = MemoryState(items = demoMemories()),
            query = "",
            onSourcesChange = {},
            onMemoryChange = {}
        )
    }
}

@Preview(name = "Sources & Memory - Wide", showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 700)
@Composable
private fun PersonasTabSourcesMemoryPreview_Wide() {
    InnovexiaTheme(darkTheme = true) {
        PersonasTabSourcesMemory(
            sources = SourcesState(items = demoSources()),
            memory = MemoryState(items = demoMemories()),
            query = "",
            onSourcesChange = {},
            onMemoryChange = {}
        )
    }
}
