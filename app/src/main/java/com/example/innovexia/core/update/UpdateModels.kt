package com.example.innovexia.core.update

import com.google.gson.annotations.SerializedName

/**
 * GitHub Release API response
 */
data class GitHubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("body")
    val body: String?,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("assets")
    val assets: List<ReleaseAsset>,
    @SerializedName("published_at")
    val publishedAt: String
)

data class ReleaseAsset(
    @SerializedName("name")
    val name: String,
    @SerializedName("browser_download_url")
    val downloadUrl: String,
    @SerializedName("size")
    val size: Long
)

/**
 * Update info for UI
 */
data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String?,
    val releasePageUrl: String,
    val apkSize: Long? = null
) {
    val isUpdateAvailable: Boolean
        get() = compareVersions(currentVersion, latestVersion) < 0
}

/**
 * Update preferences
 */
data class UpdatePreferences(
    val lastCheckedTimestamp: Long = 0L,
    val remindLaterTimestamp: Long = 0L,
    val dismissedVersion: String? = null
)

/**
 * Compare two semantic versions (e.g., "1.0.2" vs "1.0.3")
 * Supports versions with optional 'v' prefix and handles pre-release suffixes
 * Returns:
 * - negative if version1 < version2
 * - 0 if version1 == version2
 * - positive if version1 > version2
 */
fun compareVersions(version1: String, version2: String): Int {
    try {
        // Remove 'v' prefix if present
        val v1Clean = version1.removePrefix("v").trim()
        val v2Clean = version2.removePrefix("v").trim()

        // Split by '.' and extract numeric parts (ignore suffixes like -beta, -rc1, etc.)
        val v1Parts = v1Clean
            .split(".", "-", "_")
            .take(3) // Only take major.minor.patch
            .mapNotNull { it.toIntOrNull() }

        val v2Parts = v2Clean
            .split(".", "-", "_")
            .take(3) // Only take major.minor.patch
            .mapNotNull { it.toIntOrNull() }

        // Ensure we have at least some version parts
        if (v1Parts.isEmpty() && v2Parts.isEmpty()) {
            return 0
        }
        if (v1Parts.isEmpty()) {
            return -1 // v1 is invalid, consider it older
        }
        if (v2Parts.isEmpty()) {
            return 1 // v2 is invalid, consider v1 newer
        }

        val maxLength = maxOf(v1Parts.size, v2Parts.size)

        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrElse(i) { 0 }
            val v2Part = v2Parts.getOrElse(i) { 0 }

            when {
                v1Part < v2Part -> return -1
                v1Part > v2Part -> return 1
            }
        }

        return 0
    } catch (e: Exception) {
        // Fallback to string comparison if parsing fails
        android.util.Log.w("UpdateModels", "Failed to parse versions: $version1 vs $version2", e)
        return version1.compareTo(version2)
    }
}
