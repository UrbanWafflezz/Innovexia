package com.example.innovexia.core.update

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.innovexia.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service to check for app updates from GitHub Releases
 */
class UpdateChecker(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "update_preferences",
        Context.MODE_PRIVATE
    )

    private val api: GitHubApi = GitHubApi.create()

    companion object {
        private const val TAG = "UpdateChecker"
        private const val OWNER = "UrbanWafflezz"
        private const val REPO = "Innovexia"

        private const val KEY_LAST_CHECKED = "last_checked_timestamp"
        private const val KEY_REMIND_LATER = "remind_later_timestamp"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val KEY_FIRST_DETECTED = "first_detected_timestamp"
        private const val KEY_DETECTED_VERSION = "detected_version"

        // Check for updates at most once per 15 minutes
        private const val CHECK_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        // Force update after 7 days of first detection
        private const val FORCE_UPDATE_AFTER_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    /**
     * Check if we should check for updates now
     * (respects rate limiting and "remind later" preference)
     */
    fun shouldCheckForUpdates(): Boolean {
        val now = System.currentTimeMillis()
        val lastChecked = prefs.getLong(KEY_LAST_CHECKED, 0L)
        val remindLaterUntil = prefs.getLong(KEY_REMIND_LATER, 0L)

        // If user clicked "Remind Later", don't check until interval passes
        if (remindLaterUntil > now) {
            Log.d(TAG, "User requested remind later, skipping check")
            return false
        }

        // Rate limit: only check once per hour
        if (now - lastChecked < CHECK_INTERVAL_MS) {
            Log.d(TAG, "Already checked recently, skipping")
            return false
        }

        return true
    }

    /**
     * Result of an update check
     */
    sealed class UpdateCheckResult {
        data class UpdateAvailable(val updateInfo: UpdateInfo) : UpdateCheckResult()
        object NoUpdateAvailable : UpdateCheckResult()
        data class Error(val message: String, val exception: Throwable? = null) : UpdateCheckResult()
    }

    /**
     * Check for updates from GitHub Releases API
     * Returns UpdateCheckResult with detailed status
     */
    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for updates from GitHub...")

            val response = api.getLatestRelease(OWNER, REPO)

            if (!response.isSuccessful) {
                val errorMsg = when (response.code()) {
                    403 -> "GitHub API rate limit exceeded. Try again later."
                    404 -> "No releases found for this app."
                    429 -> "Too many requests. Please wait before checking again."
                    in 500..599 -> "GitHub server error. Try again later."
                    else -> "Failed to check for updates (HTTP ${response.code()})"
                }
                Log.e(TAG, "Failed to fetch releases: ${response.code()}")
                return@withContext UpdateCheckResult.Error(errorMsg)
            }

            val release = response.body() ?: run {
                Log.e(TAG, "Empty response body")
                return@withContext UpdateCheckResult.Error("Empty response from GitHub")
            }

            // Update last checked timestamp
            prefs.edit().putLong(KEY_LAST_CHECKED, System.currentTimeMillis()).apply()

            val currentVersion = BuildConfig.VERSION_NAME
            val latestVersion = release.tagName.removePrefix("v")

            Log.d(TAG, "Current version: $currentVersion, Latest version: $latestVersion")

            // Find APK asset in release
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }

            if (apkAsset == null) {
                Log.w(TAG, "No APK file found in release assets")
                return@withContext UpdateCheckResult.Error("No APK file available for download")
            }

            val downloadUrl = apkAsset.downloadUrl

            val updateInfo = UpdateInfo(
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                releaseNotes = release.body,
                releasePageUrl = release.htmlUrl,
                apkSize = apkAsset.size
            )

            if (updateInfo.isUpdateAvailable) {
                Log.d(TAG, "Update available: $latestVersion")

                // Track first detection time for this version
                val detectedVersion = prefs.getString(KEY_DETECTED_VERSION, null)
                if (detectedVersion != latestVersion) {
                    // New version detected - reset tracking
                    prefs.edit()
                        .putString(KEY_DETECTED_VERSION, latestVersion)
                        .putLong(KEY_FIRST_DETECTED, System.currentTimeMillis())
                        .remove(KEY_REMIND_LATER)
                        .apply()
                    Log.d(TAG, "First time detecting version $latestVersion")
                }

                return@withContext UpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                Log.d(TAG, "Already on latest version")
                return@withContext UpdateCheckResult.NoUpdateAvailable
            }

        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("Unable to resolve host") == true ->
                    "No internet connection. Please check your network."
                e.message?.contains("timeout") == true ->
                    "Connection timeout. Please try again."
                else -> "Error checking for updates: ${e.message ?: "Unknown error"}"
            }
            Log.e(TAG, "Error checking for updates", e)
            return@withContext UpdateCheckResult.Error(errorMsg, e)
        }
    }

    /**
     * Check if update should be forced (7 days have passed since first detection)
     */
    fun shouldForceUpdate(): Boolean {
        val firstDetected = prefs.getLong(KEY_FIRST_DETECTED, 0L)
        if (firstDetected == 0L) return false

        val now = System.currentTimeMillis()
        val daysPassed = (now - firstDetected) / (24 * 60 * 60 * 1000L)

        return daysPassed >= 7
    }

    /**
     * Set remind later with custom duration in milliseconds
     */
    fun setRemindLater(durationMs: Long) {
        val remindLaterUntil = System.currentTimeMillis() + durationMs
        prefs.edit().putLong(KEY_REMIND_LATER, remindLaterUntil).apply()
        val hours = durationMs / (60 * 60 * 1000L)
        Log.d(TAG, "Remind later set for $hours hours")
    }

    /**
     * Clear "remind later" preference (for testing or manual override)
     */
    fun clearRemindLater() {
        prefs.edit().remove(KEY_REMIND_LATER).apply()
    }

    /**
     * Clear all update preferences (for testing)
     */
    fun clearAllPreferences() {
        prefs.edit().clear().apply()
    }
}
