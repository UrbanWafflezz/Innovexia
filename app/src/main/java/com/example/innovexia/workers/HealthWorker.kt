package com.example.innovexia.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.innovexia.R
import com.example.innovexia.core.health.HealthState
import com.example.innovexia.core.health.RealHealthApi
import com.example.innovexia.core.health.ServiceCatalog
import com.example.innovexia.core.health.HealthRetrofitApi
import com.example.innovexia.data.local.AppDatabase
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Background worker for periodic health monitoring
 * Runs via WorkManager when app is backgrounded
 */
class HealthWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "health_monitoring"
        const val NOTIFICATION_CHANNEL_ID = "system_health"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        return try {
            // Initialize health API
            val db = AppDatabase.getInstance(applicationContext)
            val okHttp = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .writeTimeout(3, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://placeholder.local/") // Base URL not used with @Url
                .client(okHttp)
                .build()

            val healthApi = RealHealthApi(
                http = retrofit.create(HealthRetrofitApi::class.java),
                catalog = { ServiceCatalog.load() },
                db = db,
                context = applicationContext
            )

            // Get previous state
            val previousChecks = db.healthCheckDao().observe().firstOrNull() ?: emptyList()
            val previousStates = previousChecks.associate { it.serviceId to it.status }

            // Run health check
            val summary = healthApi.checkAll()

            // Detect state transitions
            val transitions = summary.checks.filter { check ->
                val previous = previousStates[check.id]
                previous != null && previous != check.status.name
            }

            // Post notifications for transitions
            if (transitions.isNotEmpty()) {
                postHealthNotifications(transitions.map { "${it.name}: ${it.status}" })
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * Post notification for health state transitions
     */
    private fun postHealthNotifications(changes: List<String>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // Create notification channel (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "System Health",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for system health status changes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification
        val inboxStyle = NotificationCompat.InboxStyle()
        changes.forEach { inboxStyle.addLine(it) }

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use appropriate icon
            .setContentTitle("System Health Alert")
            .setContentText("${changes.size} service(s) changed status")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
