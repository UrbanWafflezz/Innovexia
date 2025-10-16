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

        // Check for updates at most once per hour
        private const val CHECK_INTERVAL_MS = 60 * 60 * 1000L // 1 hour

        // "Remind Later" delays check for 24 hours
        private const val REMIND_LATER_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
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
     * Check for updates from GitHub Releases API
     * Returns UpdateInfo if update is available, null otherwise
     */
    suspend fun checkForUpdates(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for updates from GitHub...")

            val response = api.getLatestRelease(OWNER, REPO)

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to fetch releases: ${response.code()}")
                return@withContext null
            }

            val release = response.body() ?: run {
                Log.e(TAG, "Empty response body")
                return@withContext null
            }

            // Update last checked timestamp
            prefs.edit().putLong(KEY_LAST_CHECKED, System.currentTimeMillis()).apply()

            val currentVersion = BuildConfig.VERSION_NAME
            val latestVersion = release.tagName.removePrefix("v")

            Log.d(TAG, "Current version: $currentVersion, Latest version: $latestVersion")

            // Check if user already dismissed this version
            val dismissedVersion = prefs.getString(KEY_DISMISSED_VERSION, null)
            if (dismissedVersion == latestVersion) {
                Log.d(TAG, "User already dismissed this version")
                return@withContext null
            }

            // Find APK asset in release
            val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
            val downloadUrl = apkAsset?.downloadUrl ?: release.htmlUrl

            val updateInfo = UpdateInfo(
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                downloadUrl = downloadUrl,
                releaseNotes = release.body,
                releasePageUrl = release.htmlUrl,
                apkSize = apkAsset?.size
            )

            if (updateInfo.isUpdateAvailable) {
                Log.d(TAG, "Update available: $latestVersion")
                return@withContext updateInfo
            } else {
                Log.d(TAG, "Already on latest version")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            return@withContext null
        }
    }

    /**
     * User clicked "Remind Later" - don't show update dialog for 24 hours
     */
    fun setRemindLater() {
        val remindLaterUntil = System.currentTimeMillis() + REMIND_LATER_INTERVAL_MS
        prefs.edit().putLong(KEY_REMIND_LATER, remindLaterUntil).apply()
        Log.d(TAG, "Remind later set for 24 hours")
    }

    /**
     * User dismissed a specific version - don't show for this version again
     */
    fun dismissVersion(version: String) {
        prefs.edit().putString(KEY_DISMISSED_VERSION, version).apply()
        Log.d(TAG, "Dismissed version: $version")
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
