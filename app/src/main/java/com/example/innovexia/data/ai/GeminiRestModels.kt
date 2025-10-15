package com.example.innovexia.data.ai

import com.google.gson.annotations.SerializedName

/**
 * REST API models for Gemini API with Google Search Grounding support.
 *
 * Documentation: https://ai.google.dev/gemini-api/docs/grounding
 */

// ==================== REQUEST MODELS ====================

/**
 * Complete request body for Gemini API generateContent endpoint.
 */
data class GeminiRequest(
    @SerializedName("contents")
    val contents: List<Content>,

    @SerializedName("generationConfig")
    val generationConfig: GenerationConfig? = null,

    @SerializedName("safetySettings")
    val safetySettings: List<SafetySetting>? = null,

    @SerializedName("systemInstruction")
    val systemInstruction: Content? = null,

    @SerializedName("tools")
    val tools: List<Tool>? = null
)

data class Content(
    @SerializedName("role")
    val role: String, // "user" or "model"

    @SerializedName("parts")
    val parts: List<Part>
)

data class Part(
    @SerializedName("text")
    val text: String? = null,

    @SerializedName("inlineData")
    val inlineData: InlineData? = null
)

data class InlineData(
    @SerializedName("mimeType")
    val mimeType: String,

    @SerializedName("data")
    val data: String // Base64 encoded
)

data class GenerationConfig(
    @SerializedName("temperature")
    val temperature: Float? = null,

    @SerializedName("topK")
    val topK: Int? = null,

    @SerializedName("topP")
    val topP: Float? = null,

    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int? = null,

    @SerializedName("candidateCount")
    val candidateCount: Int? = null
)

data class SafetySetting(
    @SerializedName("category")
    val category: String,

    @SerializedName("threshold")
    val threshold: String
)

data class Tool(
    @SerializedName("googleSearch")
    val googleSearch: GoogleSearchRetrieval? = null
)

/**
 * Google Search retrieval tool (empty object).
 * The API only needs the presence of this object to enable grounding.
 */
data class GoogleSearchRetrieval(
    // Empty - just needs to be present to enable Google Search grounding
    @SerializedName("_placeholder")
    private val placeholder: String? = null
)

// ==================== RESPONSE MODELS ====================

/**
 * Response from Gemini API generateContent endpoint.
 */
data class GeminiResponse(
    @SerializedName("candidates")
    val candidates: List<Candidate>? = null,

    @SerializedName("usageMetadata")
    val usageMetadata: UsageMetadata? = null,

    @SerializedName("promptFeedback")
    val promptFeedback: PromptFeedback? = null
)

data class Candidate(
    @SerializedName("content")
    val content: Content? = null,

    @SerializedName("finishReason")
    val finishReason: String? = null,

    @SerializedName("safetyRatings")
    val safetyRatings: List<SafetyRating>? = null,

    @SerializedName("groundingMetadata")
    val groundingMetadata: GroundingMetadataResponse? = null
)

data class SafetyRating(
    @SerializedName("category")
    val category: String,

    @SerializedName("probability")
    val probability: String
)

data class UsageMetadata(
    @SerializedName("promptTokenCount")
    val promptTokenCount: Int? = null,

    @SerializedName("candidatesTokenCount")
    val candidatesTokenCount: Int? = null,

    @SerializedName("totalTokenCount")
    val totalTokenCount: Int? = null
)

data class PromptFeedback(
    @SerializedName("blockReason")
    val blockReason: String? = null,

    @SerializedName("safetyRatings")
    val safetyRatings: List<SafetyRating>? = null
)

/**
 * Grounding metadata from API response.
 */
data class GroundingMetadataResponse(
    @SerializedName("webSearchQueries")
    val webSearchQueries: List<String>? = null,

    @SerializedName("searchEntryPoint")
    val searchEntryPoint: SearchEntryPointResponse? = null,

    @SerializedName("groundingChunks")
    val groundingChunks: List<GroundingChunkResponse>? = null,

    @SerializedName("groundingSupports")
    val groundingSupports: List<GroundingSupportResponse>? = null
)

data class SearchEntryPointResponse(
    @SerializedName("renderedContent")
    val renderedContent: String? = null
)

data class GroundingChunkResponse(
    @SerializedName("web")
    val web: WebSourceResponse? = null
)

data class WebSourceResponse(
    @SerializedName("uri")
    val uri: String? = null,

    @SerializedName("title")
    val title: String? = null
)

data class GroundingSupportResponse(
    @SerializedName("segment")
    val segment: GroundingSegmentResponse? = null,

    @SerializedName("groundingChunkIndices")
    val groundingChunkIndices: List<Int>? = null,

    @SerializedName("confidenceScores")
    val confidenceScores: List<Float>? = null
)

data class GroundingSegmentResponse(
    @SerializedName("startIndex")
    val startIndex: Int? = null,

    @SerializedName("endIndex")
    val endIndex: Int? = null,

    @SerializedName("text")
    val text: String? = null
)
