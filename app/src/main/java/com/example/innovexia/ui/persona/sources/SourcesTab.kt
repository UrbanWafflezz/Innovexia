package com.example.innovexia.ui.persona.sources

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Main Sources tab component integrating all sub-components
 */
@Composable
fun SourcesTab(
    personaId: String,
    personaName: String,
    personaColor: Color,
    darkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SourcesViewModel = viewModel(
        key = "sources_$personaId"
    ) {
        SourcesViewModel(personaId = personaId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val filteredItems = viewModel.getFilteredItems()

    // Dialog states
    var detailsItem by remember { mutableStateOf<SourceItemUi?>(null) }
    var removeConfirmIds by remember { mutableStateOf<Set<String>?>(null) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with persona info and enable toggle
        SourcesHeader(
            personaName = personaName,
            personaColor = personaColor,
            enabled = uiState.enabled,
            onEnabledChange = { enabled ->
                viewModel.setEnabled(enabled)
            },
            darkTheme = darkTheme
        )

        // Show content only if enabled
        if (uiState.enabled) {
            // Add bar
            SourceAddBar(
                onAddUrl = { url ->
                    viewModel.addUrl(url)
                    Toast.makeText(context, "URL added (UI only)", Toast.LENGTH_SHORT).show()
                },
                onAddFile = {
                    // Simulate file picker
                    viewModel.addMockFile("document.pdf", sizeBytes = 1_500_000, pageCount = 20)
                    Toast.makeText(context, "File picker (UI only)", Toast.LENGTH_SHORT).show()
                },
                onAddImage = {
                    // Simulate image picker
                    viewModel.addMockImage("screenshot.png", sizeBytes = 800_000)
                    Toast.makeText(context, "Image picker (UI only)", Toast.LENGTH_SHORT).show()
                },
                darkTheme = darkTheme
            )

            // Filters
            SourcesFilters(
                filter = uiState.filter,
                query = uiState.query,
                sort = uiState.sort,
                selecting = uiState.selecting,
                onFilterChange = { filter ->
                    viewModel.applyFilter(filter)
                },
                onQueryChange = { query ->
                    viewModel.updateQuery(query)
                },
                onSortChange = { sort ->
                    viewModel.applySort(sort)
                },
                onToggleSelect = {
                    viewModel.toggleSelectMode()
                },
                darkTheme = darkTheme
            )

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    // Empty state: no items
                    filteredItems.isEmpty() && uiState.query.isBlank() && uiState.filter == null -> {
                        EmptyState(
                            type = EmptyStateType.NO_ITEMS,
                            onAddUrl = {
                                // Trigger add URL action
                            },
                            darkTheme = darkTheme
                        )
                    }
                    // Empty state: filter empty
                    filteredItems.isEmpty() && uiState.filter != null && uiState.query.isBlank() -> {
                        val emptyType = when (uiState.filter) {
                            SourceType.URL -> EmptyStateType.FILTER_EMPTY_URLS
                            SourceType.FILE -> EmptyStateType.FILTER_EMPTY_FILES
                            SourceType.IMAGE -> EmptyStateType.FILTER_EMPTY_IMAGES
                            else -> EmptyStateType.NO_ITEMS
                        }
                        EmptyState(
                            type = emptyType,
                            onAddUrl = {
                                viewModel.addUrl("https://example.com")
                                Toast.makeText(context, "URL added (UI only)", Toast.LENGTH_SHORT).show()
                            },
                            onAddFile = {
                                viewModel.addMockFile("document.pdf")
                                Toast.makeText(context, "File added (UI only)", Toast.LENGTH_SHORT).show()
                            },
                            onAddImage = {
                                viewModel.addMockImage("image.png")
                                Toast.makeText(context, "Image added (UI only)", Toast.LENGTH_SHORT).show()
                            },
                            darkTheme = darkTheme
                        )
                    }
                    // Empty state: search empty
                    filteredItems.isEmpty() && uiState.query.isNotBlank() -> {
                        EmptyState(
                            type = EmptyStateType.SEARCH_EMPTY(uiState.query),
                            darkTheme = darkTheme
                        )
                    }
                    // Show list
                    else -> {
                        SourceList(
                            items = filteredItems,
                            selectedIds = uiState.selectedIds,
                            selecting = uiState.selecting,
                            onItemClick = { item ->
                                if (uiState.selecting) {
                                    viewModel.toggleItemSelection(item.id)
                                } else {
                                    detailsItem = item
                                }
                            },
                            onItemLongPress = { item ->
                                if (!uiState.selecting) {
                                    viewModel.toggleSelectMode()
                                }
                                viewModel.toggleItemSelection(item.id)
                            },
                            onTogglePin = { id ->
                                viewModel.togglePin(id)
                            },
                            onReindex = { id ->
                                viewModel.reindex(setOf(id))
                            },
                            onRemove = { id ->
                                removeConfirmIds = setOf(id)
                            },
                            onOpenDetails = { item ->
                                detailsItem = item
                            },
                            darkTheme = darkTheme
                        )
                    }
                }

                // Batch action bar (overlays at bottom)
                if (uiState.selecting && uiState.selectedIds.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 0.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        BatchActionBar(
                            selectedCount = uiState.selectedIds.size,
                            onPin = {
                                viewModel.pinSelected(true)
                            },
                            onUnpin = {
                                viewModel.pinSelected(false)
                            },
                            onReindex = {
                                viewModel.reindex(uiState.selectedIds)
                                Toast.makeText(context, "Reindexing ${uiState.selectedIds.size} items...", Toast.LENGTH_SHORT).show()
                            },
                            onRemove = {
                                removeConfirmIds = uiState.selectedIds
                            },
                            onClearSelection = {
                                viewModel.clearSelection()
                            },
                            darkTheme = darkTheme
                        )
                    }
                }
            }

            // Footer
            if (filteredItems.isNotEmpty() && !uiState.selecting) {
                SourcesFooter(
                    storageUsedBytes = uiState.storageUsedBytes,
                    darkTheme = darkTheme
                )
            }
        } else {
            // Disabled state
            EmptyState(
                type = EmptyStateType.DISABLED,
                onEnableSources = {
                    viewModel.setEnabled(true)
                },
                darkTheme = darkTheme,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Details dialog
    detailsItem?.let { item ->
        SourceDetailsDialog(
            item = item,
            onDismiss = { detailsItem = null },
            onReindex = {
                viewModel.reindex(setOf(item.id))
            },
            onRemove = {
                viewModel.remove(setOf(item.id))
            },
            darkTheme = darkTheme
        )
    }

    // Remove confirmation dialog
    removeConfirmIds?.let { ids ->
        RemoveConfirmDialog(
            count = ids.size,
            onConfirm = {
                viewModel.remove(ids)
                if (uiState.selecting && ids == uiState.selectedIds) {
                    viewModel.toggleSelectMode()
                }
                Toast.makeText(
                    context,
                    "Removed ${ids.size} ${if (ids.size == 1) "source" else "sources"}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { removeConfirmIds = null },
            darkTheme = darkTheme
        )
    }
}
