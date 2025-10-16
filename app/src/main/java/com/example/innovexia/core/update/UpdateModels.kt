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
 * Returns:
 * - negative if version1 < version2
 * - 0 if version1 == version2
 * - positive if version1 > version2
 */
fun compareVersions(version1: String, version2: String): Int {
    val v1Parts = version1.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
    val v2Parts = version2.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }

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
}
