package com.example.innovexia.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.repository.CloudSyncRepository

/**
 * Background worker for cloud sync operations.
 *
 * Use cases:
 * - Initial upload when sync is first enabled
 * - Periodic sync to keep cloud up to date
 * - Retry failed sync operations
 */
class CloudSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "CloudSyncWorker"
        const val KEY_PROGRESS = "progress"
        const val KEY_TOTAL = "total"
        const val KEY_ERROR = "error"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting cloud sync worker")

        return try {
            // Get app dependencies
            val app = context.applicationContext as? InnovexiaApplication
                ?: return Result.failure(workDataOf(KEY_ERROR to "App not available"))

            val database = AppDatabase.getInstance(context)

            val syncRepo = CloudSyncRepository(
                context = context,
                chatDao = database.chatDao(),
                messageDao = database.messageDao()
            )

            // Check if sync is enabled
            if (!syncRepo.isSyncEnabled()) {
                Log.d(TAG, "Cloud sync not enabled, skipping")
                return Result.success()
            }

            // Perform initial upload
            // Note: Progress tracking removed as setProgress cannot be called from callback
            syncRepo.performInitialUpload()

            Log.d(TAG, "Cloud sync worker completed successfully")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Cloud sync worker failed", e)
            Result.failure(workDataOf(KEY_ERROR to e.message))
        }
    }
}
