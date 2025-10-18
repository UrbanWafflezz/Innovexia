package com.example.innovexia.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.innovexia.core.location.LocationCacheManager
import com.example.innovexia.core.permissions.PermissionHelper

/**
 * Background worker that periodically updates the location cache.
 * Runs every 15 minutes when app is active to ensure fresh location data.
 *
 * Benefits:
 * - Always have recent location available for AI context/grounding
 * - No GPS wait time when sending messages
 * - Battery efficient (balanced power mode)
 * - Respects user privacy (only runs if permission granted)
 */
class LocationUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        android.util.Log.d("LocationUpdateWorker", "Starting background location update")

        return try {
            // Check if location permission is granted
            if (!PermissionHelper.hasLocationPermission(context)) {
                android.util.Log.w("LocationUpdateWorker", "Location permission not granted - skipping update")
                return Result.success()
            }

            // Fetch fresh location from GPS
            val location = PermissionHelper.getCurrentLocation(context)

            if (location != null) {
                // Update cache with fresh location
                LocationCacheManager.updateLocation(context, location)
                android.util.Log.d("LocationUpdateWorker", "✓ Background location update successful: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")
                Result.success()
            } else {
                // GPS failed to get location (indoors, signal blocked, etc.)
                android.util.Log.w("LocationUpdateWorker", "Failed to get location from GPS - keeping existing cache")
                // Don't fail the job - just retry later
                Result.success()
            }

        } catch (e: Exception) {
            android.util.Log.e("LocationUpdateWorker", "Location update failed: ${e.message}", e)
            // Retry with exponential backoff
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "location_update_worker"
    }
}
