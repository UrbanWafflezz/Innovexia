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
        private const val KEY_PENDING_UPDATE_VERSION = "pending_update_version"
        private const val KEY_PENDING_UPDATE_DOWNLOAD_URL = "pending_update_download_url"
        private const val KEY_PENDING_UPDATE_NOTES = "pending_update_release_notes"
        private const val KEY_PENDING_UPDATE_PAGE_URL = "pending_update_page_url"
        private const val KEY_PENDING_UPDATE_APK_SIZE = "pending_update_apk_size"

        // Check for updates on every app launch (no rate limiting for showing updates)
        // Only rate limit the actual API call to GitHub
        private const val API_CALL_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        // Force update after 7 days of first detection
        private const val FORCE_UPDATE_AFTER_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    /**
     * Check if we should make a new API call to GitHub
     * (respects rate limiting for API calls only)
     */
    private fun shouldMakeApiCall(): Boolean {
        val now = System.currentTimeMillis()
        val lastChecked = prefs.getLong(KEY_LAST_CHECKED, 0L)

        // Rate limit API calls only (not update notifications)
        if (now - lastChecked < API_CALL_INTERVAL_MS) {
            Log.d(TAG, "API call rate limited, using cached update info")
            return false
        }

        return true
    }

    /**
     * Check if we should show update notification
     * (respects "remind later" preference only)
     */
    fun shouldShowUpdateNotification(): Boolean {
        val now = System.currentTimeMillis()
        val remindLaterUntil = prefs.getLong(KEY_REMIND_LATER, 0L)

        // If user clicked "Remind Later", don't show until interval passes
        if (remindLaterUntil > now) {
            val hoursLeft = (remindLaterUntil - now) / (60 * 60 * 1000L)
            Log.d(TAG, "User requested remind later ($hoursLeft hours remaining)")
            return false
        }

        return true
    }

    /**
     * Save pending update info to preferences (persists across app restarts)
     */
    private fun savePendingUpdate(updateInfo: UpdateInfo) {
        prefs.edit()
            .putString(KEY_PENDING_UPDATE_VERSION, updateInfo.latestVersion)
            .putString(KEY_PENDING_UPDATE_DOWNLOAD_URL, updateInfo.downloadUrl)
            .putString(KEY_PENDING_UPDATE_NOTES, updateInfo.releaseNotes)
            .putString(KEY_PENDING_UPDATE_PAGE_URL, updateInfo.releasePageUrl)
            .putLong(KEY_PENDING_UPDATE_APK_SIZE, updateInfo.apkSize ?: 0L)
            .apply()
        Log.d(TAG, "Saved pending update: ${updateInfo.latestVersion}")
    }

    /**
     * Get pending update info from preferences (returns null if none)
     */
    fun getPendingUpdate(): UpdateInfo? {
        val version = prefs.getString(KEY_PENDING_UPDATE_VERSION, null) ?: return null
        val downloadUrl = prefs.getString(KEY_PENDING_UPDATE_DOWNLOAD_URL, null) ?: return null
        val notes = prefs.getString(KEY_PENDING_UPDATE_NOTES, null)
        val pageUrl = prefs.getString(KEY_PENDING_UPDATE_PAGE_URL, null) ?: return null
        val apkSize = prefs.getLong(KEY_PENDING_UPDATE_APK_SIZE, 0L).takeIf { it > 0 }

        return UpdateInfo(
            currentVersion = BuildConfig.VERSION_NAME,
            latestVersion = version,
            downloadUrl = downloadUrl,
            releaseNotes = notes,
            releasePageUrl = pageUrl,
            apkSize = apkSize
        )
    }

    /**
     * Clear pending update (called when user completes update)
     */
    fun clearPendingUpdate() {
        prefs.edit()
            .remove(KEY_PENDING_UPDATE_VERSION)
            .remove(KEY_PENDING_UPDATE_DOWNLOAD_URL)
            .remove(KEY_PENDING_UPDATE_NOTES)
            .remove(KEY_PENDING_UPDATE_PAGE_URL)
            .remove(KEY_PENDING_UPDATE_APK_SIZE)
            .remove(KEY_DETECTED_VERSION)
            .remove(KEY_FIRST_DETECTED)
            .remove(KEY_REMIND_LATER)
            .apply()
        Log.d(TAG, "Cleared pending update")
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
            // Check if we should make a new API call or use cached data
            if (!shouldMakeApiCall()) {
                // Return cached pending update if available
                val pendingUpdate = getPendingUpdate()
                if (pendingUpdate != null) {
                    Log.d(TAG, "Returning cached pending update: ${pendingUpdate.latestVersion}")
                    return@withContext UpdateCheckResult.UpdateAvailable(pendingUpdate)
                }
            }

            Log.d(TAG, "Checking for updates from GitHub...")

            // Fetch ALL releases to handle beta/stable properly
            val response = api.getAllReleases(OWNER, REPO)

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

            val releases = response.body() ?: run {
                Log.e(TAG, "Empty response body")
                return@withContext UpdateCheckResult.Error("Empty response from GitHub")
            }

            if (releases.isEmpty()) {
                Log.e(TAG, "No releases available")
                return@withContext UpdateCheckResult.Error("No releases found for this app.")
            }

            // Update last checked timestamp
            prefs.edit().putLong(KEY_LAST_CHECKED, System.currentTimeMillis()).apply()

            val currentVersion = BuildConfig.VERSION_NAME
            val currentIsBeta = isBetaVersion(currentVersion)

            Log.d(TAG, "Current version: $currentVersion (beta: $currentIsBeta)")
            Log.d(TAG, "Found ${releases.size} releases")

            // Filter releases that have APK assets
            val validReleases = releases.filter { release ->
                release.assets.any { it.name.endsWith(".apk") }
            }

            if (validReleases.isEmpty()) {
                Log.w(TAG, "No releases with APK files found")
                return@withContext UpdateCheckResult.Error("No APK files available for download")
            }

            // Find the best update candidate
            // Priority:
            // 1. If user is on beta, prefer same version stable release OR newer versions
            // 2. If user is on stable, only show newer stable releases
            val bestRelease = validReleases
                .filter { release ->
                    val releaseVersion = release.tagName.removePrefix("v")
                    val shouldShow = shouldShowUpdate(currentVersion, releaseVersion, release.prerelease)

                    if (shouldShow) {
                        Log.d(TAG, "Release ${releaseVersion} is a valid update (prerelease: ${release.prerelease})")
                    }

                    shouldShow
                }
                .maxByOrNull { release ->
                    // Sort by version, preferring stable over beta for same version
                    val releaseVersion = release.tagName.removePrefix("v")
                    val versionParts = releaseVersion.split(".").mapNotNull { it.toIntOrNull() }
                    val versionScore = versionParts.getOrNull(0)?.let { major ->
                        major * 1000000 + (versionParts.getOrNull(1) ?: 0) * 1000 + (versionParts.getOrNull(2) ?: 0)
                    } ?: 0

                    // Add bonus for stable releases
                    val stabilityBonus = if (!release.prerelease) 100 else 0

                    versionScore + stabilityBonus
                }

            if (bestRelease == null) {
                Log.d(TAG, "No valid updates found - already on latest version")
                return@withContext UpdateCheckResult.NoUpdateAvailable
            }

            val latestVersion = bestRelease.tagName.removePrefix("v")
            val apkAsset = bestRelease.assets.first { it.name.endsWith(".apk") }

            Log.d(TAG, "Update available: $latestVersion (prerelease: ${bestRelease.prerelease})")

            val updateInfo = UpdateInfo(
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                downloadUrl = apkAsset.downloadUrl,
                releaseNotes = bestRelease.body,
                releasePageUrl = bestRelease.htmlUrl,
                apkSize = apkAsset.size
            )

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

            // Save pending update to persist across app restarts
            savePendingUpdate(updateInfo)

            return@withContext UpdateCheckResult.UpdateAvailable(updateInfo)

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
