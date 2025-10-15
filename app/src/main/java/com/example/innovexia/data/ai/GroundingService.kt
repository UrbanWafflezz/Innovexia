package com.example.innovexia.data.ai

import com.google.ai.client.generativeai.type.GenerateContentResponse

/**
 * Service for handling Grounding with Google Search functionality.
 *
 * Provides utilities for:
 * - Configuring the google_search tool
 * - Parsing grounding metadata from API responses
 * - Formatting inline citations
 * - Extracting source lists for UI display
 *
 * Note: Google Search grounding requires Gemini SDK 0.9.1+ or direct API access.
 * This version provides stub functionality for SDK 0.9.0 compatibility.
 */
object GroundingService {

    /**
     * Parse grounding metadata from a Gemini API response.
     *
     * The API returns grounding data in the response metadata when google_search tool is used.
     * This method extracts and converts it to our GroundingMetadata model.
     *
     * @param response The API response from GenerativeModel
     * @return GroundingMetadata if available, null otherwise
     */
    fun parseGroundingMetadata(response: GenerateContentResponse): GroundingMetadata? {
        return try {
            // Access grounding metadata from the first candidate
            val candidate = response.candidates?.firstOrNull() ?: return null

            // Try to access groundingMetadata via reflection (not available in SDK 0.9.0)
            val groundingData = try {
                val field = candidate.javaClass.getDeclaredField("groundingMetadata")
                field.isAccessible = true
                field.get(candidate)
            } catch (e: Exception) {
                android.util.Log.d("GroundingService", "Grounding metadata not available in this SDK version")
                return null
            }

            if (groundingData == null) return null

            // Extract web search queries (list of strings)
            val queries = try {
                @Suppress("UNCHECKED_CAST")
                val field = groundingData.javaClass.getDeclaredField("webSearchQueries")
                field.isAccessible = true
                field.get(groundingData) as? List<String>
            } catch (e: Exception) {
                null
            } ?: emptyList()

            // Extract grounding chunks to get source URLs
            val chunks: List<*> = try {
                val field = groundingData.javaClass.getDeclaredField("groundingChunks")
                field.isAccessible = true
                (field.get(groundingData) as? List<*>) ?: emptyList<Any>()
            } catch (e: Exception) {
                emptyList<Any>()
            }

            // Extract URIs from chunks
            val uris = chunks.mapNotNull { chunk ->
                try {
                    val webField = chunk?.javaClass?.getDeclaredField("web")
                    webField?.isAccessible = true
                    val web = webField?.get(chunk)

                    val uriField = web?.javaClass?.getDeclaredField("uri")
                    uriField?.isAccessible = true
                    uriField?.get(web) as? String
                } catch (e: Exception) {
                    null
                }
            }

            if (queries.isEmpty() && uris.isEmpty()) {
                return null
            }

            GroundingMetadata(
                webSearchQueries = queries,
                searchResultUrls = uris
            )
        } catch (e: Exception) {
            android.util.Log.e("GroundingService", "Failed to parse grounding metadata: ${e.message}", e)
            null
        }
    }

    /**
     * Check if a response has grounding metadata.
     */
    fun hasGroundingMetadata(metadata: GroundingMetadata?): Boolean {
        return metadata != null &&
               (metadata.webSearchQueries?.isNotEmpty() == true ||
                metadata.searchResultUrls.isNotEmpty())
    }

    /**
     * Get a summary of grounding usage for logging/debugging.
     */
    fun getGroundingSummary(metadata: GroundingMetadata?): String {
        if (metadata == null) return "No grounding data"

        return buildString {
            append("Grounding: ")
            append("${metadata.webSearchQueries?.size ?: 0} queries, ")
            append("${metadata.searchResultUrls.size} sources")
            if (!metadata.webSearchQueries.isNullOrEmpty()) {
                append(" (queries: ${metadata.webSearchQueries.joinToString(", ")})")
            }
        }
    }
}
