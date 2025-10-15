package com.example.innovexia.data.ai

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Result of file processing
 */
data class ProcessedFile(
    val uri: Uri,
    val displayName: String,
    val size: Long
)

/**
 * Utility for processing files (PDFs, etc.) for Gemini
 * - Copies to app cache
 * - Enforces size limits
 */
class FileProcessor(
    private val cacheDir: File
) {

    companion object {
        private const val MAX_SIZE_MB = 30L
        private const val MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024
    }

    /**
     * Copy file to cache directory and validate size
     *
     * @param resolver ContentResolver for accessing the file
     * @param uri URI of the file to process
     * @param enforceMaxMB Optional custom max size in MB (defaults to 30MB)
     * @return ProcessedFile with cached URI and metadata
     * @throws IOException if file is too large or cannot be processed
     */
    fun copyToCache(
        resolver: ContentResolver,
        uri: Uri,
        enforceMaxMB: Long = MAX_SIZE_MB
    ): ProcessedFile {
        val maxBytes = enforceMaxMB * 1024 * 1024

        // Get file metadata
        val (displayName, size) = getFileMetadata(resolver, uri)

        // Check size limit
        if (size > maxBytes) {
            throw IOException("File too large: ${formatSize(size)}. Maximum is ${enforceMaxMB}MB")
        }

        // Copy to cache
        val safeName = sanitizeFileName(displayName)
        val outputFile = File(cacheDir, "attachment_${System.currentTimeMillis()}_$safeName")

        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Cannot open input stream for $uri")

        return ProcessedFile(
            uri = Uri.fromFile(outputFile),
            displayName = displayName,
            size = outputFile.length()
        )
    }

    /**
     * Get file metadata (name and size)
     */
    private fun getFileMetadata(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var displayName = "file_${System.currentTimeMillis()}"
        var size = 0L

        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)

                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }

                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        return displayName to size
    }

    /**
     * Sanitize filename to prevent path traversal and invalid characters
     */
    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(100) // Limit length
    }

    /**
     * Format file size for display
     */
    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return "%.1f MB".format(mb)
    }
}
