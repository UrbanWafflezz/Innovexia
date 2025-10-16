package com.example.innovexia.core.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Handles downloading APK files from GitHub releases
 */
class ApkDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    companion object {
        private const val TAG = "ApkDownloader"
        private const val APK_DIR = "apk"
        private const val APK_FILENAME = "innovexia-update.apk"
    }

    /**
     * Download state for progress tracking
     */
    sealed class DownloadState {
        data object Idle : DownloadState()
        data class Downloading(val progress: Int, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
        data class Completed(val file: File) : DownloadState()
        data class Error(val message: String, val exception: Exception? = null) : DownloadState()
    }

    /**
     * Download APK from URL with progress tracking
     * Returns a Flow that emits download progress states
     */
    fun downloadApk(url: String): Flow<DownloadState> = flow {
        emit(DownloadState.Idle)

        try {
            Log.d(TAG, "Starting APK download from: $url")

            // Create cache directory for APK
            val cacheDir = File(context.cacheDir, APK_DIR)
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            // Delete old APK if it exists
            val apkFile = File(cacheDir, APK_FILENAME)
            if (apkFile.exists()) {
                apkFile.delete()
            }

            // Create HTTP request
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.android.package-archive")
                .build()

            // Execute request
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(DownloadState.Error("Download failed: HTTP ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadState.Error("Empty response body"))
                return@flow
            }

            val contentLength = body.contentLength()
            Log.d(TAG, "APK size: $contentLength bytes")

            // Download file with progress tracking
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(apkFile)

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L
            var lastProgressUpdate = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead

                // Calculate progress percentage
                val progress = if (contentLength > 0) {
                    ((totalBytesRead * 100) / contentLength).toInt()
                } else {
                    -1 // Unknown size
                }

                // Emit progress updates (throttled to every 5%)
                if (progress - lastProgressUpdate >= 5 || progress == 100) {
                    emit(DownloadState.Downloading(progress, totalBytesRead, contentLength))
                    lastProgressUpdate = progress
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Log.d(TAG, "APK downloaded successfully: ${apkFile.absolutePath}")
            emit(DownloadState.Completed(apkFile))

        } catch (e: IOException) {
            Log.e(TAG, "Error downloading APK", e)
            emit(DownloadState.Error("Download failed: ${e.message}", e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error downloading APK", e)
            emit(DownloadState.Error("Unexpected error: ${e.message}", e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Get the downloaded APK file (if it exists)
     */
    fun getDownloadedApk(): File? {
        val cacheDir = File(context.cacheDir, APK_DIR)
        val apkFile = File(cacheDir, APK_FILENAME)
        return if (apkFile.exists()) apkFile else null
    }

    /**
     * Delete downloaded APK file
     */
    fun clearDownloadedApk() {
        val apkFile = getDownloadedApk()
        apkFile?.delete()
    }
}
