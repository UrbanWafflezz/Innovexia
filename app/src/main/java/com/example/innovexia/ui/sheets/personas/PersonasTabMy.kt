package com.example.innovexia.ui.sheets.personas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.persona.Persona
import com.example.innovexia.ui.persona.PersonaCard
import com.example.innovexia.ui.persona.demoMyPersonas
import com.example.innovexia.ui.theme.InnovexiaTheme

/**
 * My Personas tab with responsive grid, modern filter chips, and sorting.
 * Features: Filter by all/active/favorites/recent, Sort by recent/name/created/lastUsed
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PersonasTabMy(
    query: String,
    personas: List<Persona>,
    selectedPersonaId: String?,
    isSignedIn: Boolean,
    onPersonaSelected: (Persona) -> Unit,
    onNewPersona: () -> Unit,
    onStar: (Persona) -> Unit,
    onDuplicate: (Persona) -> Unit,
    onRename: (Persona) -> Unit,
    onDelete: (Persona) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: ((Persona) -> Unit)? = null,
    onMakePublic: ((Persona) -> Unit)? = null
) {
    // Filter & Sort state
    var filterType by rememberSaveable { mutableStateOf(PersonaFilter.ALL) }
    var sortType by rememberSaveable { mutableStateOf(PersonaSort.RECENT) }

    val filteredPersonas = personas
        .filter {
            // Search filter
            it.name.contains(query, ignoreCase = true) ||
                    it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
        .filter { persona ->
            // Type filter
            when (filterType) {
                PersonaFilter.ALL -> true
                PersonaFilter.ACTIVE -> persona.id == selectedPersonaId
                PersonaFilter.FAVORITES -> persona.starred
            }
        }
        .let { list ->
            // Sort
            when (sortType) {
                PersonaSort.RECENT -> list.sortedByDescending { it.updatedAt }
                PersonaSort.NAME -> list.sortedBy { it.name }
                PersonaSort.CREATED -> list.sortedBy { it.createdAtFormatted }
                PersonaSort.LAST_USED -> list.sortedByDescending { it.lastUsedFormatted ?: "" }
            }
        }

    // Responsive columns
    val configuration = LocalConfiguration.current
    val maxWidth = configuration.screenWidthDp.dp
    val columns = when {
        maxWidth < 360.dp -> 1
        maxWidth < 520.dp -> 2
        maxWidth < 720.dp -> 3
        else -> 4
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
    ) {
        // Compact filter bar
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimal filter buttons
                PersonaFilter.values().forEach { filter ->
                    val isSelected = filterType == filter
                    Surface(
                        onClick = { filterType = filter },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) {
                            Color(0xFFE6B84A).copy(alpha = 0.2f)
                        } else {
                            Color(0xFF2A2A2A).copy(alpha = 0.6f)
                        },
                        border = if (isSelected) {
                            BorderStroke(1.dp, Color(0xFFE6B84A).copy(alpha = 0.6f))
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = filter.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) {
                                    Color(0xFFE6B84A)
                                } else {
                                    Color(0xFF94A3B8)
                                }
                            )
                            Text(
                                text = filter.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) {
                                    Color(0xFFE6B84A)
                                } else {
                                    Color(0xFF94A3B8)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Persona cards
        items(filteredPersonas, key = { it.id }) { persona ->
            PersonaCard(
                persona = persona,
                isActive = persona.id == selectedPersonaId,
                onSelect = onPersonaSelected,
                onStar = onStar,
                onDuplicate = onDuplicate,
                onRename = onRename,
                onDelete = onDelete,
                onEdit = onEdit,
                onMakePublic = onMakePublic,
                modifier = Modifier.animateItemPlacement()
            )
        }

        // Empty state
        if (filteredPersonas.isEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                EmptyState(
                    icon = Icons.Default.Person,
                    message = when {
                        !isSignedIn -> "Sign in to create and use personas"
                        query.isEmpty() -> "No personas yet.\nCreate your first one!"
                        else -> "No personas match \"$query\""
                    }
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Filter & Sort Enums
// ═════════════════════════════════════════════════════════════════════════════

enum class PersonaFilter(val label: String, val shortLabel: String, val icon: ImageVector) {
    ALL("All", "All", Icons.Default.Person),
    ACTIVE("Active", "Active", Icons.Default.CheckCircle),
    FAVORITES("Favorites", "Starred", Icons.Default.Star)
}

enum class PersonaSort(val label: String) {
    RECENT("Recently Updated"),
    NAME("Name A-Z"),
    CREATED("Date Created"),
    LAST_USED("Last Used")
}

@Composable
internal fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF475569),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFA89968),
            textAlign = TextAlign.Center
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Previews
// ═════════════════════════════════════════════════════════════════════════════

@Preview(name = "My Tab - Compact Phone", showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 360)
@Composable
private fun PersonasTabMyPreview_Compact() {
    InnovexiaTheme(darkTheme = true) {
        PersonasTabMy(
            query = "",
            personas = demoMyPersonas(),
            selectedPersonaId = "1",
            isSignedIn = true,
            onPersonaSelected = {},
            onNewPersona = {},
            onStar = {},
            onDuplicate = {},
            onRename = {},
            onDelete = {}
        )
    }
}

@Preview(name = "My Tab - Narrow Phone 1 Col", showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 320)
@Composable
private fun PersonasTabMyPreview_Narrow() {
    InnovexiaTheme(darkTheme = true) {
        PersonasTabMy(
            query = "",
            personas = demoMyPersonas().take(3),
            selectedPersonaId = "1",
            isSignedIn = true,
            onPersonaSelected = {},
            onNewPersona = {},
            onStar = {},
            onDuplicate = {},
            onRename = {},
            onDelete = {}
        )
    }
}

@Preview(name = "My Tab - Tablet 3 Cols", showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 650)
@Composable
private fun PersonasTabMyPreview_Tablet() {
    InnovexiaTheme(darkTheme = true) {
        PersonasTabMy(
            query = "",
            personas = demoMyPersonas(),
            selectedPersonaId = "2",
            isSignedIn = true,
            onPersonaSelected = {},
            onNewPersona = {},
            onStar = {},
            onDuplicate = {},
            onRename = {},
            onDelete = {}
        )
    }
}
