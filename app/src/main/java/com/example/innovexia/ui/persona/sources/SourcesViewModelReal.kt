package com.example.innovexia.ui.persona.sources

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.memory.Mind.di.MindModule
import com.example.innovexia.memory.Mind.sources.api.SourcesEngine
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Sources tab with real backend (persona-scoped)
 */
class SourcesViewModelReal(
    private val context: Context,
    private val personaId: String
) : ViewModel() {

    private val sourcesEngine: SourcesEngine = MindModule.provideSourcesEngine(context)

    // UI state for filters and selection
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // Observe sources from database
    val sources: StateFlow<List<SourceItemUi>> = sourcesEngine
        .observeSources(personaId)
        .map { entities -> entities.map { it.toUi() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Storage usage
    val storageUsed: StateFlow<Long> = sources
        .map { items -> items.sumOf { it.sizeBytes ?: 0L } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    /**
     * Add PDF from URI
     */
    fun addPdfFromUri(uri: Uri, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = sourcesEngine.addPdfFromUri(personaId, uri)
            onResult(result)
        }
    }

    /**
     * Add URL source
     */
    fun addUrl(url: String, onResult: (Result<String>) -> Unit) {
        android.util.Log.d("SourcesViewModelReal", "addUrl called - personaId: $personaId, url: $url")
        viewModelScope.launch {
            val result = sourcesEngine.addUrlSource(
                personaId = personaId,
                url = url,
                maxDepth = 2,
                maxPages = 10
            )
            android.util.Log.d("SourcesViewModelReal", "addUrl result - success: ${result.isSuccess}, sourceId: ${result.getOrNull()}")
            onResult(result)
        }
    }

    /**
     * Add any file from URI (auto-detects type)
     */
    fun addFileFromUri(uri: Uri, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = sourcesEngine.addFileFromUri(personaId, uri)
            onResult(result)
        }
    }

    /**
     * Apply filter
     */
    fun applyFilter(filter: SourceType?) {
        _filterState.update { it.copy(filter = filter) }
    }

    /**
     * Update search query
     */
    fun updateQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    /**
     * Apply sort
     */
    fun applySort(sort: String) {
        _filterState.update { it.copy(sort = sort) }
    }

    /**
     * Toggle selection mode
     */
    fun toggleSelectMode() {
        _filterState.update {
            it.copy(
                selecting = !it.selecting,
                selectedIds = if (it.selecting) emptySet() else it.selectedIds
            )
        }
    }

    /**
     * Toggle item selection
     */
    fun toggleItemSelection(id: String) {
        _filterState.update { state ->
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
        _filterState.update { it.copy(selectedIds = emptySet()) }
    }

    /**
     * Reindex source
     */
    fun reindex(sourceId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = sourcesEngine.reindex(sourceId)
            onResult(result)
        }
    }

    /**
     * Remove source
     */
    fun removeSource(sourceId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = sourcesEngine.removeSource(sourceId)
            onResult(result)
        }
    }

    /**
     * Remove multiple sources
     */
    fun removeSources(sourceIds: Set<String>, onResult: (Int, Int) -> Unit) {
        viewModelScope.launch {
            var successCount = 0
            var errorCount = 0

            sourceIds.forEach { id ->
                val result = sourcesEngine.removeSource(id)
                if (result.isSuccess) successCount++ else errorCount++
            }

            onResult(successCount, errorCount)
        }
    }

    /**
     * Get filtered items based on current filter state
     */
    fun getFilteredItems(): StateFlow<List<SourceItemUi>> {
        return combine(sources, filterState) { items, filter ->
            items
                .filter { item ->
                    // Apply type filter
                    (filter.filter == null || item.type == filter.filter)
                }
                .filter { item ->
                    // Apply search query
                    if (filter.query.isBlank()) {
                        true
                    } else {
                        item.title.contains(filter.query, ignoreCase = true) ||
                                item.subtitle?.contains(filter.query, ignoreCase = true) == true ||
                                item.tags.any { tag -> tag.contains(filter.query, ignoreCase = true) }
                    }
                }
                .let { filtered ->
                    // Apply sort
                    when (filter.sort) {
                        "A→Z" -> filtered.sortedBy { it.title }
                        "Size" -> filtered.sortedByDescending { it.sizeBytes ?: 0 }
                        "Type" -> filtered.sortedBy { it.type }
                        else -> filtered.sortedByDescending { it.lastUpdated } // Recent
                    }
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }
}

/**
 * Filter state for UI
 */
data class FilterState(
    val filter: SourceType? = null,
    val query: String = "",
    val sort: String = "Recent",
    val selecting: Boolean = false,
    val selectedIds: Set<String> = emptySet()
)

/**
 * Map SourceEntity to SourceItemUi
 */
private fun SourceEntity.toUi(): SourceItemUi {
    return SourceItemUi(
        id = id,
        type = when (type) {
            "PDF" -> SourceType.FILE
            "URL" -> SourceType.URL
            "IMAGE" -> SourceType.IMAGE
            "TEXT" -> SourceType.FILE
            "DOCUMENT" -> SourceType.FILE
            else -> SourceType.FILE
        },
        title = when (type) {
            "URL" -> metaTitle ?: displayName
            else -> displayName
        },
        subtitle = when (type) {
            "URL" -> domain ?: fileName
            else -> fileName
        },
        uri = storagePath,
        sizeBytes = bytes,
        pageCount = when (type) {
            "URL" -> pagesIndexed
            else -> pageCount
        },
        thumbnail = thumbPath,
        tags = emptyList(), // TODO: Add tags support
        pinned = false, // TODO: Add pinned support
        status = when (status) {
            "NOT_INDEXED" -> SourceStatus.NOT_INDEXED
            "INDEXING" -> SourceStatus.INDEXING
            "READY" -> SourceStatus.READY
            "ERROR" -> SourceStatus.ERROR
            else -> SourceStatus.NOT_INDEXED
        },
        lastUpdated = addedAt,
        errorMsg = errorMsg
    )
}
