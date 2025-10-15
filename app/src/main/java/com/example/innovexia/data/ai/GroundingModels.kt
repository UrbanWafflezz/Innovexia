package com.example.innovexia.data.ai

/**
 * Data models for Gemini Grounding with Google Search.
 *
 * Grounding connects Gemini to real-time web content, enabling:
 * - Increased factual accuracy (reduces hallucinations)
 * - Access to recent events and information
 * - Verifiable source citations
 *
 * These models match the API response structure from the Gemini API.
 */

/**
 * Complete grounding metadata returned by the API when google_search tool is used.
 */
data class GroundingMetadata(
    val webSearchQueries: List<String>? = null,
    val searchEntryPoint: SearchEntryPoint? = null,
    val groundingChunks: List<GroundingChunk> = emptyList(),
    val groundingSupports: List<GroundingSupport> = emptyList(),
    val searchResultUrls: List<String> = emptyList()
)

/**
 * HTML/CSS widget for displaying "Search with Google" entry point.
 * Required by Terms of Service when using grounding.
 */
data class SearchEntryPoint(
    val renderedContent: String
)

/**
 * A web source chunk containing a URI and title.
 */
data class GroundingChunk(
    val web: WebSource
)

/**
 * Web source information.
 */
data class WebSource(
    val uri: String,
    val title: String
)

/**
 * Links a text segment to one or more grounding chunks (sources).
 * This is the key to building inline citations.
 */
data class GroundingSupport(
    val segment: GroundingSegment,
    val groundingChunkIndices: List<Int> = emptyList()
)

/**
 * A text segment defined by start and end indices in the response.
 */
data class GroundingSegment(
    val startIndex: Int,
    val endIndex: Int,
    val text: String
)

/**
 * Simplified source information for UI display.
 */
data class CitationSource(
    val index: Int,         // 1-based index for display
    val title: String,      // Website title or domain
    val uri: String,        // Full URL
    val clickable: Boolean = true
)

/**
 * Grounding status for web search
 */
enum class GroundingStatus {
    NONE,           // No grounding requested
    SEARCHING,      // Web search in progress
    SUCCESS,        // Search completed with results
    FAILED          // Search failed or returned no results
}

/**
 * Streaming response chunk with optional grounding data and token counts.
 */
data class StreamChunk(
    val text: String,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val groundingMetadata: GroundingMetadata? = null,
    val groundingStatus: GroundingStatus = GroundingStatus.NONE
)
