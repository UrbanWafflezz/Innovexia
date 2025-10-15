package com.example.innovexia.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.core.sync.CloudChatItem
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.repository.CloudSyncRepository
import com.example.innovexia.data.repository.RestoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for cloud restore operations.
 */
class CloudRestoreViewModel(private val context: Context) : ViewModel() {

    private val database = AppDatabase.getInstance(context)
    private val syncRepo = CloudSyncRepository(
        context = context,
        chatDao = database.chatDao(),
        messageDao = database.messageDao()
    )

    private val _state = MutableStateFlow<RestoreState>(RestoreState.Idle)
    val state: StateFlow<RestoreState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filterDeleted = MutableStateFlow(FilterDeletedMode.ALL)
    val filterDeleted: StateFlow<FilterDeletedMode> = _filterDeleted.asStateFlow()

    private val _includeDeletedMessages = MutableStateFlow(false)
    val includeDeletedMessages: StateFlow<Boolean> = _includeDeletedMessages.asStateFlow()

    private val _forceOverwrite = MutableStateFlow(false)
    val forceOverwrite: StateFlow<Boolean> = _forceOverwrite.asStateFlow()

    private val _reviveInCloud = MutableStateFlow(false)
    val reviveInCloud: StateFlow<Boolean> = _reviveInCloud.asStateFlow()

    private val _selectedChats = MutableStateFlow<Set<String>>(emptySet())
    val selectedChats: StateFlow<Set<String>> = _selectedChats.asStateFlow()

    /**
     * Load cloud chats for restore.
     */
    fun loadCloudChats() {
        viewModelScope.launch {
            try {
                val uid = FirebaseAuthManager.currentUser()?.uid
                if (uid == null) {
                    _state.value = RestoreState.Error("User not signed in")
                    return@launch
                }

                _state.value = RestoreState.Loading

                // Fetch all chats (we'll filter in UI based on filterDeleted)
                val allChats = syncRepo.listCloudChats(includeDeleted = true)
                _state.value = RestoreState.Ready(allChats)

                Log.d(TAG, "Loaded ${allChats.size} cloud chats")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cloud chats", e)
                _state.value = RestoreState.Error("Failed to load chats: ${e.message}")
            }
        }
    }

    /**
     * Update search query.
     */
    fun setQuery(q: String) {
        _query.value = q
    }

    /**
     * Update filter mode.
     */
    fun setFilterDeleted(mode: FilterDeletedMode) {
        _filterDeleted.value = mode
    }

    /**
     * Toggle include deleted messages option.
     */
    fun setIncludeDeletedMessages(include: Boolean) {
        _includeDeletedMessages.value = include
    }

    /**
     * Toggle force overwrite option.
     */
    fun setForceOverwrite(force: Boolean) {
        _forceOverwrite.value = force
    }

    /**
     * Toggle revive in cloud option.
     */
    fun setReviveInCloud(revive: Boolean) {
        _reviveInCloud.value = revive
    }

    /**
     * Toggle chat selection.
     */
    fun toggleChatSelection(chatId: String) {
        _selectedChats.value = if (chatId in _selectedChats.value) {
            _selectedChats.value - chatId
        } else {
            _selectedChats.value + chatId
        }
    }

    /**
     * Select all visible chats.
     */
    fun selectAll(chatIds: List<String>) {
        _selectedChats.value = chatIds.toSet()
    }

    /**
     * Clear all selections.
     */
    fun clearSelection() {
        _selectedChats.value = emptySet()
    }

    /**
     * Restore selected chats.
     */
    fun restoreSelected() {
        viewModelScope.launch {
            try {
                val chatIds = _selectedChats.value.toList()
                if (chatIds.isEmpty()) {
                    _state.value = RestoreState.Error("No chats selected")
                    return@launch
                }

                _state.value = RestoreState.Restoring(progress = 0, total = chatIds.size)

                val result = syncRepo.restoreChatsFromCloud(
                    chatIds = chatIds,
                    includeDeletedMessages = _includeDeletedMessages.value,
                    forceOverwrite = _forceOverwrite.value,
                    reviveInCloud = _reviveInCloud.value,
                    onProgress = { current, total ->
                        _state.value = RestoreState.Restoring(progress = current, total = total)
                    }
                )

                _state.value = RestoreState.Done(result)
                _selectedChats.value = emptySet() // Clear selections

                Log.d(TAG, "Restore complete: ${result.restoredCount} restored, ${result.skippedCount} skipped, ${result.errorCount} errors")
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                _state.value = RestoreState.Error("Restore failed: ${e.message}")
            }
        }
    }

    /**
     * Reset state to idle.
     */
    fun resetState() {
        _state.value = RestoreState.Idle
        _selectedChats.value = emptySet()
    }

    /**
     * Delete a single chat from cloud.
     */
    fun deleteSingleChat(chatId: String) {
        viewModelScope.launch {
            try {
                syncRepo.permanentlyDeleteChatFromCloud(chatId)

                // Remove from current list
                val currentState = _state.value
                if (currentState is RestoreState.Ready) {
                    val updatedChats = currentState.chats.filter { it.id != chatId }
                    _state.value = RestoreState.Ready(updatedChats)
                }

                // Remove from selections if present
                _selectedChats.value = _selectedChats.value - chatId

                Log.d(TAG, "Deleted chat $chatId from cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete chat $chatId", e)
                _state.value = RestoreState.Error("Failed to delete chat: ${e.message}")
            }
        }
    }

    /**
     * Delete all chats from cloud.
     */
    fun deleteAllChats() {
        viewModelScope.launch {
            try {
                _state.value = RestoreState.Loading
                syncRepo.deleteCloudData()

                // Clear state
                _state.value = RestoreState.Ready(emptyList())
                _selectedChats.value = emptySet()

                Log.d(TAG, "Deleted all cloud data")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete all chats", e)
                _state.value = RestoreState.Error("Failed to delete all chats: ${e.message}")
            }
        }
    }

    /**
     * Delete selected chats from cloud.
     */
    fun deleteSelectedChats() {
        viewModelScope.launch {
            try {
                val chatIds = _selectedChats.value.toList()
                if (chatIds.isEmpty()) {
                    _state.value = RestoreState.Error("No chats selected")
                    return@launch
                }

                _state.value = RestoreState.Deleting(progress = 0, total = chatIds.size)

                syncRepo.batchDeleteChatsFromCloud(chatIds)

                // Remove from current list
                val currentState = _state.value
                if (currentState is RestoreState.Deleting) {
                    // Reload to get fresh data
                    loadCloudChats()
                }

                _selectedChats.value = emptySet()

                Log.d(TAG, "Deleted ${chatIds.size} chats from cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete selected chats", e)
                _state.value = RestoreState.Error("Failed to delete chats: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "CloudRestoreViewModel"
    }
}

/**
 * State for restore operations.
 */
sealed class RestoreState {
    data object Idle : RestoreState()
    data object Loading : RestoreState()
    data class Ready(val chats: List<CloudChatItem>) : RestoreState()
    data class Restoring(val progress: Int, val total: Int) : RestoreState()
    data class Deleting(val progress: Int, val total: Int) : RestoreState()
    data class Done(val result: RestoreResult) : RestoreState()
    data class Error(val message: String) : RestoreState()
}

/**
 * Filter mode for deleted chats.
 */
enum class FilterDeletedMode {
    ALL,
    ONLY_DELETED,
    ONLY_ACTIVE
}
