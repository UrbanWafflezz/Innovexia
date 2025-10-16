package com.example.innovexia.ui.sheets.personas

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.persona.Persona
import com.example.innovexia.ui.persona.PersonaCard
import com.example.innovexia.ui.persona.demoPublicPersonas
import com.example.innovexia.ui.theme.InnovexiaTheme

/**
 * Public personas tab with category filters and Import button.
 * Import button is fully visible with proper padding and heightIn.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PersonasTabPublic(
    query: String,
    personas: List<Persona>,
    onImport: (Persona) -> Unit,
    onStar: (Persona) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = listOf("Coding", "Research", "Writing", "Tutor")

    val filteredPersonas = personas.filter {
        val matchesSearch = it.name.contains(query, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        val matchesCategory = selectedCategory == null ||
                it.tags.any { tag -> tag.equals(selectedCategory, ignoreCase = true) }
        matchesSearch && matchesCategory
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
        // Category filters
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val selected = category == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedCategory = if (selected) null else category
                        },
                        label = { Text(category) }
                    )
                }
            }
        }

        // Public persona cards with Import
        items(filteredPersonas, key = { it.id }) { persona ->
            Box(modifier = Modifier.animateItemPlacement()) {
                PersonaCard(
                    persona = persona,
                    isActive = false,
                    onSelect = { /* Not used for public */ },
                    onStar = onStar,
                    onDuplicate = { /* Not available for public */ },
                    onRename = { /* Not available for public */ },
                    onDelete = { /* Not available for public */ },
                    showImport = true,
                    onImport = onImport
                )
                // Public ribbon
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF1E40AF),
                    contentColor = Color.White
                ) {
                    Text(
                        text = "Public",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Empty state
        if (filteredPersonas.isEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                EmptyState(
                    icon = Icons.Default.Public,
                    message = "No public personas match your filters"
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Previews
// ═════════════════════════════════════════════════════════════════════════════

@Preview(name = "Public Tab - 2 Cols", showBackground = true, backgroundColor = 0xFF0F172A, widthDp = 360)
@Composable
private fun PersonasTabPublicPreview() {
    InnovexiaTheme(darkTheme = true) {
        PersonasTabPublic(
            query = "",
            personas = demoPublicPersonas(),
            onImport = {},
            onStar = {}
        )
    }
}

@Preview(name = "Public Tab - Light Mode", showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 400)
@Composable
private fun PersonasTabPublicPreview_Light() {
    InnovexiaTheme(darkTheme = false) {
        PersonasTabPublic(
            query = "",
            personas = demoPublicPersonas().take(4),
            onImport = {},
            onStar = {}
        )
    }
}
