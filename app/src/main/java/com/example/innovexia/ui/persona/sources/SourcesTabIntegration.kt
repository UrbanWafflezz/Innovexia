package com.example.innovexia.ui.persona.sources

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Sources tab with real PDF backend integration - Simplified Material 3 design
 */
@Composable
fun SourcesTabWithBackend(
    personaId: String,
    personaName: String,
    personaColor: Color,
    darkTheme: Boolean = isSystemInDarkTheme(),
    modifier: Modifier = Modifier,
    searchQuery: String = "" // Tab-specific search from main search bar
) {
    val context = LocalContext.current

    // Use real ViewModel
    val viewModel = remember(personaId) {
        SourcesViewModelReal(context, personaId)
    }

    val sources by viewModel.sources.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val storageUsed by viewModel.storageUsed.collectAsState()
    val filteredItems by viewModel.getFilteredItems().collectAsState()

    // Update query from main search bar
    LaunchedEffect(searchQuery) {
        viewModel.updateQuery(searchQuery)
    }

    // File picker launcher
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Request persistent permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Permission not needed for some URIs
            }

            // Add PDF
            viewModel.addPdfFromUri(it) { result ->
                if (result.isSuccess) {
                    Toast.makeText(context, "PDF added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Error: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Dialog states
    var detailsItem by remember { mutableStateOf<SourceItemUi?>(null) }
    var removeConfirmIds by remember { mutableStateOf<Set<String>?>(null) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Action buttons - URL, File, Image (at top)
        SourceAddBar(
            onAddUrl = { url ->
                // Add URL source (real backend)
                viewModel.addUrl(url) { result ->
                    if (result.isSuccess) {
                        Toast.makeText(context, "URL added - indexing started", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Error: ${result.exceptionOrNull()?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onAddFile = {
                // Launch PDF picker
                pdfPicker.launch(arrayOf("application/pdf"))
            },
            onAddImage = {
                // TODO: Image support in future phase
                Toast.makeText(context, "Image support coming soon", Toast.LENGTH_SHORT).show()
            },
            darkTheme = darkTheme
        )

        // Filter chips - All, URLs, Files, Images (below action buttons)
        SourcesFilters(
            filter = filterState.filter,
            query = filterState.query,
            sort = filterState.sort,
            selecting = filterState.selecting,
            onFilterChange = { viewModel.applyFilter(it) },
            onQueryChange = { viewModel.updateQuery(it) },
            onSortChange = { viewModel.applySort(it) },
            onToggleSelect = { viewModel.toggleSelectMode() },
            darkTheme = darkTheme,
            hideSearchAndControls = true // Hide search sources, Recent, Select
        )

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                filteredItems.isEmpty() && filterState.query.isBlank() && filterState.filter == null -> {
                    EmptyState(
                        type = EmptyStateType.NO_ITEMS,
                        onAddFile = { pdfPicker.launch(arrayOf("application/pdf")) },
                        darkTheme = darkTheme
                    )
                }
                filteredItems.isEmpty() && filterState.filter != null && filterState.query.isBlank() -> {
                    val emptyType = when (filterState.filter) {
                        SourceType.FILE -> EmptyStateType.FILTER_EMPTY_FILES
                        else -> EmptyStateType.NO_ITEMS
                    }
                    EmptyState(
                        type = emptyType,
                        onAddFile = { pdfPicker.launch(arrayOf("application/pdf")) },
                        darkTheme = darkTheme
                    )
                }
                filteredItems.isEmpty() && filterState.query.isNotBlank() -> {
                    EmptyState(
                        type = EmptyStateType.SEARCH_EMPTY(filterState.query),
                        darkTheme = darkTheme
                    )
                }
                else -> {
                    SourceList(
                        items = filteredItems,
                        selectedIds = filterState.selectedIds,
                        selecting = filterState.selecting,
                        onItemClick = { item ->
                            if (filterState.selecting) {
                                viewModel.toggleItemSelection(item.id)
                            } else {
                                detailsItem = item
                            }
                        },
                        onItemLongPress = { item ->
                            if (!filterState.selecting) {
                                viewModel.toggleSelectMode()
                            }
                            viewModel.toggleItemSelection(item.id)
                        },
                        onTogglePin = { id ->
                            // TODO: Pin support
                        },
                        onReindex = { id ->
                            viewModel.reindex(id) { result ->
                                val msg = if (result.isSuccess) {
                                    "Reindexing started"
                                } else {
                                    "Error: ${result.exceptionOrNull()?.message}"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
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

            // Batch action bar
            if (filterState.selecting && filterState.selectedIds.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 0.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    BatchActionBar(
                        selectedCount = filterState.selectedIds.size,
                        onPin = { /* TODO */ },
                        onUnpin = { /* TODO */ },
                        onReindex = {
                            filterState.selectedIds.forEach { id ->
                                viewModel.reindex(id) { }
                            }
                            Toast.makeText(
                                context,
                                "Reindexing ${filterState.selectedIds.size} items...",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onRemove = {
                            removeConfirmIds = filterState.selectedIds
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
        if (filteredItems.isNotEmpty() && !filterState.selecting) {
            SourcesFooter(
                storageUsedBytes = storageUsed,
                darkTheme = darkTheme
            )
        }
    }

    // Details dialog
    detailsItem?.let { item ->
        SourceDetailsDialog(
            item = item,
            onDismiss = { detailsItem = null },
            onReindex = {
                viewModel.reindex(item.id) { result ->
                    val msg = if (result.isSuccess) {
                        "Reindexing started"
                    } else {
                        "Error: ${result.exceptionOrNull()?.message}"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onRemove = {
                viewModel.removeSource(item.id) { result ->
                    val msg = if (result.isSuccess) {
                        "Source removed"
                    } else {
                        "Error: ${result.exceptionOrNull()?.message}"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            darkTheme = darkTheme
        )
    }

    // Remove confirmation dialog
    removeConfirmIds?.let { ids ->
        RemoveConfirmDialog(
            count = ids.size,
            onConfirm = {
                viewModel.removeSources(ids) { success, errors ->
                    if (filterState.selecting && ids == filterState.selectedIds) {
                        viewModel.toggleSelectMode()
                    }
                    val msg = if (errors == 0) {
                        "Removed $success ${if (success == 1) "source" else "sources"}"
                    } else {
                        "Removed $success, failed $errors"
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { removeConfirmIds = null },
            darkTheme = darkTheme
        )
    }
}
