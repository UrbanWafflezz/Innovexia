package com.example.innovexia.memory.Mind.sources.indexer

import android.util.Log
import com.example.innovexia.memory.Mind.sources.SourcesConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Extracts text content from text-based files
 */
class TextExtractor(
    private val config: SourcesConfig
) {

    companion object {
        private const val TAG = "TextExtractor"
    }

    /**
     * Extract text from a text file
     * @param filePath Path to the text file
     * @return Text content
     */
    suspend fun extractText(filePath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(Exception("File not found: $filePath"))
            }

            val text = file.readText()
            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from $filePath", e)
            Result.failure(e)
        }
    }

    /**
     * Extract text with line metadata (useful for code files)
     * Returns map of line number to line content
     */
    suspend fun extractTextWithLines(filePath: String): Result<Map<Int, String>> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("File not found: $filePath"))
                }

                val lines = file.readLines()
                    .mapIndexed { index, line -> (index + 1) to line }
                    .toMap()

                Result.success(lines)
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting text with lines from $filePath", e)
                Result.failure(e)
            }
        }
}
