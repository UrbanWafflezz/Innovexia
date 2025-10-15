package com.example.innovexia.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.innovexia.InnovexiaApplication

/**
 * Background worker to check and update entitlement state
 * Runs periodically to handle renewals, expirations, trial ends
 */
class EntitlementCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as InnovexiaApplication
            val billingProvider = app.billingProvider

            // Check and update state
            billingProvider.checkAndUpdateState()

            Result.success()
        } catch (e: Exception) {
            // Retry on failure
            Result.retry()
        }
    }
}
