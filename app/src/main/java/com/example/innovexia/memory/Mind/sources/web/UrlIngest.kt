package com.example.innovexia.memory.Mind.sources.web

import android.content.Context
import android.util.Log
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID

/**
 * Ingests URLs and creates SourceEntity records
 */
class UrlIngest(private val context: Context) {
    companion object {
        private const val TAG = "UrlIngest"
    }

    /**
     * Ingest a URL and create a SourceEntity
     * @param personaId Persona ID to associate with
     * @param rawUrl Raw URL from user input
     * @return Result with SourceEntity on success
     */
    suspend fun ingestUrl(personaId: String, rawUrl: String): Result<SourceEntity> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Ingesting URL: $rawUrl for persona: $personaId")

                // Normalize URL
                val normalizedUrl = normalizeUrl(rawUrl)
                Log.d(TAG, "Normalized URL: $normalizedUrl")

                // Check robots.txt
                if (!RobotsPolicy.isAllowed(normalizedUrl)) {
                    Log.w(TAG, "URL disallowed by robots.txt: $normalizedUrl")
                    return@withContext Result.failure(
                        Exception("Disallowed by robots.txt")
                    )
                }

                // Parse URL for metadata
                val parsedUrl = URL(normalizedUrl)
                val domain = parsedUrl.host
                val displayName = extractDisplayName(normalizedUrl)

                // Create source entity
                val sourceId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()

                val sourceEntity = SourceEntity(
                    id = sourceId,
                    personaId = personaId,
                    type = "URL",
                    displayName = displayName,
                    fileName = "", // URLs don't have local files initially
                    mime = "text/html",
                    bytes = 0L, // Will be updated after indexing
                    pageCount = 0, // Will be updated after indexing
                    addedAt = now,
                    lastIndexedAt = null,
                    status = "NOT_INDEXED",
                    errorMsg = null,
                    storagePath = normalizedUrl, // Store the URL in storagePath
                    thumbPath = null,
                    // Web-specific fields
                    metaTitle = null, // Will be extracted during indexing
                    metaDesc = null,  // Will be extracted during indexing
                    domain = domain,
                    depth = 2, // Default depth
                    contentType = "text/html",
                    pagesIndexed = 0
                )

                Log.d(TAG, "Created SourceEntity: $sourceId for URL: $normalizedUrl")
                Result.success(sourceEntity)

            } catch (e: Exception) {
                Log.e(TAG, "Error ingesting URL: $rawUrl", e)
                Result.failure(e)
            }
        }

    /**
     * Normalize a URL
     * - Add https:// if no protocol
     * - Remove trailing slashes
     * - Remove fragments
     */
    private fun normalizeUrl(url: String): String {
        var normalized = url.trim()

        // Add protocol if missing
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            normalized = "https://$normalized"
        }

        // Parse and reconstruct URL to normalize
        val parsed = URL(normalized)
        normalized = URL(parsed.protocol, parsed.host, parsed.port, parsed.path).toString()

        // Remove trailing slash (unless it's the root)
        if (normalized.endsWith("/") && normalized.length > parsed.protocol.length + 3) {
            normalized = normalized.dropLast(1)
        }

        return normalized
    }

    /**
     * Extract a display name from URL
     * e.g., "https://example.com/docs/guide" -> "example.com - guide"
     */
    private fun extractDisplayName(url: String): String {
        return try {
            val parsed = URL(url)
            val domain = parsed.host.removePrefix("www.")
            val path = parsed.path

            if (path.isEmpty() || path == "/") {
                domain
            } else {
                val segments = path.split("/").filter { it.isNotEmpty() }
                val lastSegment = segments.lastOrNull()?.replace("-", " ")?.replace("_", " ")
                    ?: domain

                "$domain - $lastSegment"
            }
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Validate that a URL is accessible and returns HTML
     * @return Result with error message if validation fails
     */
    suspend fun validateUrl(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalizedUrl = normalizeUrl(url)

            // Check robots.txt
            if (!RobotsPolicy.isAllowed(normalizedUrl)) {
                return@withContext Result.failure(Exception("Disallowed by robots.txt"))
            }

            // Try to fetch the URL
            val fetcher = HtmlFetcher()
            fetcher.fetch(normalizedUrl)

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "URL validation failed: $url", e)
            Result.failure(e)
        }
    }
}
