package com.example.innovexia.ui.persona.sources

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for managing Sources tab state (UI only, persona-scoped)
 */
class SourcesViewModel(
    private val personaId: String,
    private val savedStateHandle: SavedStateHandle? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SourcesUiState(
            items = mockSources,
            storageUsedBytes = calculateStorageUsed(mockSources)
        )
    )
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    /**
     * Enable or disable sources for this persona
     */
    fun setEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enabled = enabled) }
    }

    /**
     * Add a URL source (UI only)
     */
    fun addUrl(url: String, title: String? = null) {
        val domain = try {
            java.net.URL(url).host
        } catch (e: Exception) {
            url
        }

        val newItem = SourceItemUi(
            id = UUID.randomUUID().toString(),
            type = SourceType.URL,
            title = title ?: domain,
            subtitle = domain,
            uri = url,
            status = SourceStatus.NOT_INDEXED
        )

        _uiState.update { state ->
            state.copy(items = state.items + newItem)
        }
    }

    /**
     * Add a mock file source (UI only)
     */
    fun addMockFile(fileName: String, sizeBytes: Long = 1024000, pageCount: Int? = null) {
        val newItem = SourceItemUi(
            id = UUID.randomUUID().toString(),
            type = SourceType.FILE,
            title = fileName.substringBeforeLast('.'),
            subtitle = fileName,
            uri = "content://documents/${UUID.randomUUID()}",
            sizeBytes = sizeBytes,
            pageCount = pageCount,
            status = SourceStatus.NOT_INDEXED
        )

        _uiState.update { state ->
            state.copy(
                items = state.items + newItem,
                storageUsedBytes = state.storageUsedBytes + sizeBytes
            )
        }
    }

    /**
     * Add a mock image source (UI only)
     */
    fun addMockImage(imageName: String, sizeBytes: Long = 512000) {
        val newItem = SourceItemUi(
            id = UUID.randomUUID().toString(),
            type = SourceType.IMAGE,
            title = imageName.substringBeforeLast('.'),
            subtitle = imageName,
            uri = "content://media/images/${UUID.randomUUID()}",
            sizeBytes = sizeBytes,
            status = SourceStatus.NOT_INDEXED
        )

        _uiState.update { state ->
            state.copy(
                items = state.items + newItem,
                storageUsedBytes = state.storageUsedBytes + sizeBytes
            )
        }
    }

    /**
     * Toggle selection mode
     */
    fun toggleSelectMode() {
        _uiState.update { state ->
            state.copy(
                selecting = !state.selecting,
                selectedIds = if (state.selecting) emptySet() else state.selectedIds
            )
        }
    }

    /**
     * Toggle item selection
     */
    fun toggleItemSelection(id: String) {
        _uiState.update { state ->
            val newSelectedIds = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = newSelectedIds)
        }
    }

    /**
     * Clear all selections
     */
    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    /**
     * Apply filter
     */
    fun applyFilter(filter: SourceType?) {
        _uiState.update { it.copy(filter = filter) }
    }

    /**
     * Update search query
     */
    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    /**
     * Apply sort
     */
    fun applySort(sort: String) {
        _uiState.update { state ->
            val sortedItems = when (sort) {
                "A→Z" -> state.items.sortedBy { it.title }
                "Size" -> state.items.sortedByDescending { it.sizeBytes ?: 0 }
                "Type" -> state.items.sortedBy { it.type }
                else -> state.items.sortedByDescending { it.lastUpdated } // Recent
            }
            state.copy(sort = sort, items = sortedItems)
        }
    }

    /**
     * Pin/unpin an item
     */
    fun togglePin(id: String) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == id) item.copy(pinned = !item.pinned) else item
                }
            )
        }
    }

    /**
     * Pin/unpin selected items
     */
    fun pinSelected(pin: Boolean) {
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id in state.selectedIds) item.copy(pinned = pin) else item
                }
            )
        }
    }

    /**
     * Remove items by IDs
     */
    fun remove(ids: Set<String>) {
        _uiState.update { state ->
            val removedItems = state.items.filter { it.id in ids }
            val freedBytes = removedItems.sumOf { it.sizeBytes ?: 0 }

            state.copy(
                items = state.items.filterNot { it.id in ids },
                selectedIds = state.selectedIds - ids,
                storageUsedBytes = (state.storageUsedBytes - freedBytes).coerceAtLeast(0)
            )
        }
    }

    /**
     * Reindex items (UI only - simulates transition)
     */
    fun reindex(ids: Set<String>) {
        // Set to INDEXING immediately
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id in ids) {
                        item.copy(status = SourceStatus.INDEXING, errorMsg = null)
                    } else {
                        item
                    }
                }
            )
        }

        // Simulate async indexing - transition to READY after delay
        // In real app, this would be handled by a background worker
        viewModelScope.launch {
            delay(2000)
            _uiState.update { state ->
                state.copy(
                    items = state.items.map { item ->
                        if (item.id in ids && item.status == SourceStatus.INDEXING) {
                            item.copy(status = SourceStatus.READY)
                        } else {
                            item
                        }
                    }
                )
            }
        }
    }

    /**
     * Get filtered and searched items
     */
    fun getFilteredItems(): List<SourceItemUi> {
        val state = _uiState.value
        return state.items
            .filter { item ->
                // Apply type filter
                (state.filter == null || item.type == state.filter)
            }
            .filter { item ->
                // Apply search query
                if (state.query.isBlank()) {
                    true
                } else {
                    item.title.contains(state.query, ignoreCase = true) ||
                            item.subtitle?.contains(state.query, ignoreCase = true) == true ||
                            item.tags.any { tag -> tag.contains(state.query, ignoreCase = true) }
                }
            }
    }

    private fun calculateStorageUsed(items: List<SourceItemUi>): Long {
        return items.sumOf { it.sizeBytes ?: 0 }
    }
}
