package com.example.innovexia.core.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.innovexia.MainActivity
import com.example.innovexia.R

/**
 * Background worker to periodically check for app updates
 */
class UpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val updateChecker = UpdateChecker(context)

    companion object {
        private const val TAG = "UpdateWorker"
        const val WORK_NAME = "update_checker_work"
        private const val NOTIFICATION_CHANNEL_ID = "app_updates"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Background update check started")

            // Create notification channel (required for Android 8.0+)
            createNotificationChannel()

            // Check for updates
            val result = updateChecker.checkForUpdates()

            when (result) {
                is UpdateChecker.UpdateCheckResult.UpdateAvailable -> {
                    val updateInfo = result.updateInfo
                    Log.d(TAG, "Update available: ${updateInfo.latestVersion}")

                    // Show notification about available update
                    showUpdateNotification(updateInfo)

                    Result.success()
                }
                is UpdateChecker.UpdateCheckResult.NoUpdateAvailable -> {
                    Log.d(TAG, "No update available")
                    Result.success()
                }
                is UpdateChecker.UpdateCheckResult.Error -> {
                    Log.e(TAG, "Update check failed: ${result.message}", result.exception)
                    // Retry on error (WorkManager will handle backoff)
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in background update check", e)
            Result.retry()
        }
    }

    /**
     * Create notification channel for update notifications (Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new app versions"
                enableVibration(true)
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show notification when update is available
     */
    private fun showUpdateNotification(updateInfo: UpdateInfo) {
        try {
            // Intent to open MainActivity when notification is tapped
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification
            val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app icon
                .setContentTitle("Update Available")
                .setContentText("Version ${updateInfo.latestVersion} is now available")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Version ${updateInfo.latestVersion} is now available.\n\nTap to download and install the update."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            // Show notification
            val notificationManager = NotificationManagerCompat.from(applicationContext)
            notificationManager.notify(NOTIFICATION_ID, notification)

            Log.d(TAG, "Update notification shown for version ${updateInfo.latestVersion}")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing update notification", e)
        }
    }
}
