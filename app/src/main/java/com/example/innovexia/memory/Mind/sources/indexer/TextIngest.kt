package com.example.innovexia.memory.Mind.sources.indexer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.innovexia.memory.Mind.sources.SourcesConfig
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Handles text-based file ingestion: .kt, .md, .json, .xml, .kts, .txt, etc.
 */
class TextIngest(
    private val context: Context,
    private val config: SourcesConfig
) {

    companion object {
        private const val TAG = "TextIngest"

        // Supported text file types with their MIME types
        val SUPPORTED_TYPES = mapOf(
            "kt" to "text/x-kotlin",
            "kts" to "text/x-kotlin-script",
            "md" to "text/markdown",
            "json" to "application/json",
            "xml" to "text/xml",
            "txt" to "text/plain",
            "java" to "text/x-java",
            "py" to "text/x-python",
            "js" to "text/javascript",
            "ts" to "text/typescript",
            "css" to "text/css",
            "html" to "text/html",
            "yaml" to "text/yaml",
            "yml" to "text/yaml",
            "gradle" to "text/x-gradle",
            "properties" to "text/x-java-properties",
            "sh" to "text/x-shellscript",
            "sql" to "text/x-sql",
            "csv" to "text/csv",
            "log" to "text/plain"
        )

        const val MAX_TEXT_FILE_MB = 10 // Max 10MB for text files
    }

    /**
     * Ingest a text file from URI
     * @return SourceEntity with initial metadata
     */
    suspend fun ingestTextFile(
        personaId: String,
        uri: Uri,
        displayName: String
    ): Result<SourceEntity> = withContext(Dispatchers.IO) {
        try {
            // Get file extension
            val extension = displayName.substringAfterLast('.', "").lowercase()

            if (!SUPPORTED_TYPES.containsKey(extension)) {
                return@withContext Result.failure(
                    Exception("Unsupported file type: .$extension")
                )
            }

            val sourceId = UUID.randomUUID().toString()
            val mimeType = SUPPORTED_TYPES[extension] ?: "text/plain"

            // Create storage directories
            val sourceDir = getPersonaSourcesDir(personaId)
            sourceDir.mkdirs()

            val textFile = File(sourceDir, "$sourceId.$extension")

            // Copy file to internal storage and read content
            var fileSize = 0L
            var lineCount = 0

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(textFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        fileSize += read

                        // Count newlines for line estimation
                        lineCount += buffer.take(read).count { it == '\n'.code.toByte() }
                    }
                }
            } ?: return@withContext Result.failure(Exception("Failed to open file"))

            // Check file size
            val maxBytes = MAX_TEXT_FILE_MB * 1024 * 1024L
            if (fileSize > maxBytes) {
                textFile.delete()
                return@withContext Result.failure(
                    Exception("File too large: ${fileSize / 1024 / 1024}MB (max: ${MAX_TEXT_FILE_MB}MB)")
                )
            }

            // Verify it's actually a text file by reading a sample
            try {
                textFile.bufferedReader().use { reader ->
                    val sample = CharArray(1024)
                    val charsRead = reader.read(sample)

                    // Check for binary content (null bytes or too many non-printable chars)
                    val nonPrintable = sample.take(charsRead).count {
                        it.code < 32 && it != '\n' && it != '\r' && it != '\t'
                    }

                    if (nonPrintable > charsRead / 10) { // More than 10% non-printable
                        textFile.delete()
                        return@withContext Result.failure(
                            Exception("File appears to be binary, not text")
                        )
                    }
                }
            } catch (e: Exception) {
                textFile.delete()
                return@withContext Result.failure(
                    Exception("Failed to read file as text: ${e.message}")
                )
            }

            // Create SourceEntity
            val entity = SourceEntity(
                id = sourceId,
                personaId = personaId,
                type = "TEXT",
                displayName = displayName.substringBeforeLast('.'),
                fileName = "$sourceId.$extension",
                mime = mimeType,
                bytes = fileSize,
                pageCount = maxOf(1, (lineCount / 50)), // Estimate "pages" as groups of ~50 lines
                addedAt = System.currentTimeMillis(),
                lastIndexedAt = null,
                status = "NOT_INDEXED",
                errorMsg = null,
                storagePath = textFile.absolutePath,
                thumbPath = null // No thumbnail for text files
            )

            Result.success(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ingest text file", e)
            Result.failure(e)
        }
    }

    /**
     * Extract text from a text file
     */
    suspend fun extractText(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val text = File(filePath).readText()
            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from file", e)
            Result.failure(e)
        }
    }

    /**
     * Get persona-specific sources directory
     */
    private fun getPersonaSourcesDir(personaId: String): File {
        return File(context.filesDir, "sources/$personaId")
    }

    /**
     * Delete source files
     */
    suspend fun deleteSourceFiles(source: SourceEntity) = withContext(Dispatchers.IO) {
        try {
            File(source.storagePath).delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting source files", e)
        }
    }
}
