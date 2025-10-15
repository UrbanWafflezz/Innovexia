package com.example.innovexia.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import com.example.innovexia.core.sync.CloudSyncSettings
import com.example.innovexia.workers.PeriodicSyncScheduler
import androidx.work.WorkManager
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.repository.CloudSyncRepository
import com.example.innovexia.workers.CloudSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel for cloud sync settings and operations.
 */
class CloudSyncViewModel(private val context: Context) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)
    // FirebaseAuthManager is an object, not a class
    private val database = AppDatabase.getInstance(context)

    private val syncRepo = CloudSyncRepository(
        context = context,
        chatDao = database.chatDao(),
        messageDao = database.messageDao()
    )
    private val settings = CloudSyncSettings(context)

    private val _syncEnabled = MutableStateFlow(false)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    private val _syncInProgress = MutableStateFlow(false)
    val syncInProgress: StateFlow<Boolean> = _syncInProgress.asStateFlow()

    private val _syncProgress = MutableStateFlow(0 to 0) // (current, total)
    val syncProgress: StateFlow<Pair<Int, Int>> = _syncProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSyncStatus()
    }

    /**
     * Load current sync status.
     */
    private fun loadSyncStatus() {
        viewModelScope.launch {
            try {
                _syncEnabled.value = syncRepo.isSyncEnabled()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sync status", e)
            }
        }
    }

    /**
     * Toggle cloud sync on/off.
     */
    fun toggleSync(enabled: Boolean) {
        viewModelScope.launch {
            try {
                if (enabled) {
                    val success = syncRepo.enableSync()
                    if (success) {
                        _syncEnabled.value = true
                        // Trigger initial upload in background
                        scheduleInitialUpload()
                        // Schedule periodic sync every hour
                        PeriodicSyncScheduler.schedule(context)
                    } else {
                        _error.value = "Cannot enable sync: not signed in"
                    }
                } else {
                    syncRepo.disableSync()
                    // Cancel periodic sync
                    PeriodicSyncScheduler.cancel(context)
                    _syncEnabled.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle sync", e)
                _error.value = "Failed to toggle sync: ${e.message}"
            }
        }
    }

    /**
     * Schedule initial upload using WorkManager.
     */
    private fun scheduleInitialUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadWork = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "cloud_sync_initial_upload",
            ExistingWorkPolicy.REPLACE,
            uploadWork
        )

        // Observe work progress
        workManager.getWorkInfoByIdLiveData(uploadWork.id).observeForever { workInfo ->
            when (workInfo?.state) {
                WorkInfo.State.RUNNING -> {
                    _syncInProgress.value = true
                    val progress = workInfo.progress.getInt(CloudSyncWorker.KEY_PROGRESS, 0)
                    val total = workInfo.progress.getInt(CloudSyncWorker.KEY_TOTAL, 0)
                    _syncProgress.value = progress to total
                }
                WorkInfo.State.SUCCEEDED -> {
                    _syncInProgress.value = false
                    _syncProgress.value = 0 to 0
                    Log.d(TAG, "Initial upload completed")
                }
                WorkInfo.State.FAILED -> {
                    _syncInProgress.value = false
                    _syncProgress.value = 0 to 0
                    val errorMsg = workInfo.outputData.getString(CloudSyncWorker.KEY_ERROR)
                    _error.value = "Sync failed: $errorMsg"
                    Log.e(TAG, "Initial upload failed: $errorMsg")
                }
                else -> {
                    // Handle other states (ENQUEUED, BLOCKED, CANCELLED)
                }
            }
        }
    }

    /**
     * Delete all cloud data for the current user.
     */
    fun deleteCloudData() {
        viewModelScope.launch {
            try {
                _syncInProgress.value = true
                syncRepo.deleteCloudData()
                _syncInProgress.value = false
                Log.d(TAG, "Cloud data deleted successfully")
            } catch (e: Exception) {
                _syncInProgress.value = false
                _error.value = "Failed to delete cloud data: ${e.message}"
                Log.e(TAG, "Failed to delete cloud data", e)
            }
        }
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Manually trigger a sync now.
     */
    fun manualSync() {
        viewModelScope.launch {
            try {
                _syncInProgress.value = true
                syncRepo.performInitialUpload()
                _syncInProgress.value = false
                Log.d(TAG, "Manual sync completed")
            } catch (e: Exception) {
                _syncInProgress.value = false
                _error.value = "Manual sync failed: ${e.message}"
                Log.e(TAG, "Manual sync failed", e)
            }
        }
    }

    /**
     * Get last sync time as StateFlow.
     */
    val lastSyncTime: StateFlow<Long> = settings.lastSyncTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0L)

    /**
     * Get last sync chat count.
     */
    val lastSyncChatCount: StateFlow<Int> = settings.lastSyncChatCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    /**
     * Get last sync message count.
     */
    val lastSyncMessageCount: StateFlow<Int> = settings.lastSyncMessageCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    companion object {
        private const val TAG = "CloudSyncViewModel"
    }
}
