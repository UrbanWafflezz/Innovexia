package com.example.innovexia.data.ai

import android.util.Log
import com.example.innovexia.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

/**
 * REST API client for Gemini API with Google Search Grounding support.
 *
 * Uses direct HTTP calls to access features not available in the Android SDK,
 * specifically the `tools` parameter for Google Search grounding.
 *
 * API Documentation: https://ai.google.dev/gemini-api/docs/grounding
 */
class GeminiRestClient {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS) // Long timeout for streaming
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiRestClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com"
        private const val API_VERSION = "v1beta"

        // JSON media type
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Generate content with streaming using Gemini REST API.
     *
     * @param modelName The model name (e.g., "gemini-2.0-flash-exp")
     * @param request The complete request body with contents, tools, etc.
     * @return Flow of response text chunks
     */
    fun generateContentStream(
        modelName: String,
        request: GeminiRequest
    ): Flow<StreamChunk> = flow {
        val apiKey = BuildConfig.GEMINI_API_KEY

        // Build streaming endpoint URL
        val url = "$BASE_URL/$API_VERSION/models/$modelName:streamGenerateContent?key=$apiKey&alt=sse"

        Log.d(TAG, "Streaming request to: $url")
        Log.d(TAG, "Request has ${request.tools?.size ?: 0} tools configured")
        val groundingEnabled = request.tools?.any { it.googleSearch != null } == true
        if (groundingEnabled) {
            Log.d(TAG, "Google Search grounding ENABLED")
        }

        // Serialize request to JSON
        val requestJson = gson.toJson(request)
        Log.d(TAG, "Request JSON: $requestJson")

        val requestBody = requestJson.toRequestBody(JSON_MEDIA_TYPE)

        val httpRequest = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            val response = client.newCall(httpRequest).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "API Error: ${response.code} - $errorBody")

                // Handle rate limiting (429)
                if (response.code == 429) {
                    val retrySeconds = extractRetrySeconds(errorBody)
                    throw RateLimitException(
                        message = "Rate limit exceeded",
                        retryAfterSeconds = retrySeconds
                    )
                }

                // Handle quota exceeded (400 with specific message)
                if (response.code == 400 && errorBody.contains("quota", ignoreCase = true)) {
                    val retrySeconds = extractRetrySeconds(errorBody)
                    throw RateLimitException(
                        message = "Quota exceeded",
                        retryAfterSeconds = retrySeconds
                    )
                }

                throw GeminiException("API request failed: ${response.code} - $errorBody")
            }

            // Parse Server-Sent Events (SSE) stream
            response.body?.byteStream()?.bufferedReader()?.use { reader ->
                var accumulatedText = ""
                var latestInputTokens = 0
                var latestOutputTokens = 0
                var groundingMetadata: GroundingMetadata? = null
                var groundingSearchPerformed = false
                var finishReason: String? = null

                // Read lines manually to use emit() in the flow scope
                var line = reader.readLine()
                while (line != null) {
                    // SSE format: "data: {json}"
                    if (line.startsWith("data: ")) {
                        val jsonData = line.substring(6) // Remove "data: " prefix

                        try {
                            val geminiResponse = gson.fromJson(jsonData, GeminiResponse::class.java)

                            // Extract text from candidates
                            val textChunk = geminiResponse.candidates
                                ?.firstOrNull()
                                ?.content
                                ?.parts
                                ?.firstOrNull()
                                ?.text
                                ?: ""

                            // Extract finish reason (for MAX_TOKENS detection)
                            geminiResponse.candidates?.firstOrNull()?.finishReason?.let {
                                finishReason = it
                            }

                            // Extract token counts
                            geminiResponse.usageMetadata?.let { usage ->
                                usage.promptTokenCount?.let { latestInputTokens = it }
                                usage.candidatesTokenCount?.let { latestOutputTokens = it }
                            }

                            // Extract grounding metadata (usually in final chunk)
                            val currentGroundingStatus = if (groundingEnabled) {
                                geminiResponse.candidates
                                    ?.firstOrNull()
                                    ?.groundingMetadata?.let { gm ->
                                        groundingSearchPerformed = true
                                        groundingMetadata = convertGroundingMetadata(gm)
                                        Log.d(TAG, "✓ Grounding metadata found: ${groundingMetadata?.searchResultUrls?.size ?: 0} sources")

                                        // Log search queries that were performed
                                        gm.webSearchQueries?.let { queries ->
                                            Log.d(TAG, "Web search queries: ${queries.joinToString(", ")}")
                                        }

                                        // Determine status based on results
                                        if (groundingMetadata?.searchResultUrls?.isNotEmpty() == true) {
                                            GroundingStatus.SUCCESS
                                        } else {
                                            GroundingStatus.FAILED
                                        }
                                    } ?: GroundingStatus.SEARCHING  // Still searching if no metadata yet
                            } else {
                                GroundingStatus.NONE
                            }

                            // Emit chunk if we have text OR grounding metadata
                            // (Grounding metadata may arrive in chunks without text)
                            if (textChunk.isNotEmpty() || (groundingEnabled && groundingMetadata != null)) {
                                accumulatedText += textChunk

                                emit(StreamChunk(
                                    text = textChunk,
                                    inputTokens = latestInputTokens,
                                    outputTokens = latestOutputTokens,
                                    groundingMetadata = groundingMetadata,
                                    groundingStatus = currentGroundingStatus
                                ))
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse SSE chunk: ${e.message}", e)
                        }
                    }
                    line = reader.readLine()
                }

                Log.d(TAG, "=== Stream complete ===")
                Log.d(TAG, "Total text length: ${accumulatedText.length}")
                Log.d(TAG, "Final tokens - Input: $latestInputTokens, Output: $latestOutputTokens")

                // Log grounding status
                if (groundingEnabled) {
                    if (groundingSearchPerformed) {
                        Log.d(TAG, "✓ Google Search grounding completed successfully")
                    } else {
                        Log.w(TAG, "⚠ Google Search was enabled but no grounding metadata received")
                    }
                }

                // Check for finish reason MAX_TOKENS
                if (finishReason == "MAX_TOKENS") {
                    Log.w(TAG, "Response stopped: MAX_TOKENS reached")
                    // Don't throw - let the response complete normally
                    // The ViewModel will detect truncation via finishReason in the future
                }
            }

        } catch (e: GeminiException) {
            // Re-throw GeminiException (including RateLimitException)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Stream error: ${e.message}", e)
            throw GeminiException("Failed to stream response: ${e.message}", e)
        }
    }.flowOn(Dispatchers.IO)
    .catch { exception ->
        // Unified error handling - matches SDK path behavior
        Log.e(TAG, "Flow catch: ${exception.message}", exception)

        // Handle serialization errors
        if (exception.message?.contains("MissingFieldException") == true ||
            exception.message?.contains("deserialize") == true) {
            throw GeminiException("Failed to parse API response", exception)
        } else {
            throw exception
        }
    }
    .onCompletion { cause ->
        // Unified completion handling - matches SDK path behavior
        if (cause != null) {
            when (cause) {
                is RateLimitException -> {
                    // Already properly typed - just re-throw
                    throw cause
                }
                is GeminiException -> {
                    // Already a GeminiException - re-throw
                    throw cause
                }
                else -> {
                    throw GeminiException("Failed to generate response: ${cause.message}", cause)
                }
            }
        }
    }

    /**
     * Convert API grounding metadata to internal model.
     * Logs warnings when chunks are dropped due to missing fields.
     */
    private fun convertGroundingMetadata(
        response: GroundingMetadataResponse
    ): GroundingMetadata {
        var droppedChunks = 0
        val chunks = response.groundingChunks?.mapNotNull { chunk ->
            chunk.web?.let { web ->
                if (web.uri != null && web.title != null) {
                    GroundingChunk(
                        web = WebSource(
                            uri = web.uri,
                            title = web.title
                        )
                    )
                } else {
                    droppedChunks++
                    Log.w(TAG, "⚠ Dropped grounding chunk - missing uri or title: uri=${web.uri}, title=${web.title}")
                    null
                }
            }
        } ?: emptyList()

        if (droppedChunks > 0) {
            Log.w(TAG, "⚠ Total dropped grounding chunks: $droppedChunks (API response format may have changed)")
        }

        var droppedSupports = 0
        val supports = response.groundingSupports?.mapNotNull { support ->
            val segment = support.segment
            if (segment?.startIndex != null && segment.endIndex != null && segment.text != null) {
                GroundingSupport(
                    segment = GroundingSegment(
                        startIndex = segment.startIndex,
                        endIndex = segment.endIndex,
                        text = segment.text
                    ),
                    groundingChunkIndices = support.groundingChunkIndices ?: emptyList()
                )
            } else {
                droppedSupports++
                Log.w(TAG, "⚠ Dropped grounding support - missing segment fields")
                null
            }
        } ?: emptyList()

        if (droppedSupports > 0) {
            Log.w(TAG, "⚠ Total dropped grounding supports: $droppedSupports")
        }

        return GroundingMetadata(
            webSearchQueries = response.webSearchQueries,
            searchEntryPoint = response.searchEntryPoint?.let {
                SearchEntryPoint(renderedContent = it.renderedContent ?: "")
            },
            groundingChunks = chunks,
            groundingSupports = supports,
            searchResultUrls = chunks.map { it.web.uri }
        )
    }

    /**
     * Extract retry seconds from error message or response headers.
     */
    private fun extractRetrySeconds(message: String?): Int {
        if (message == null) return 60
        // Try to extract seconds from message like "Please retry in 10.364492042s." or "retry after 60 seconds"
        val patterns = listOf(
            """retry in ([\d.]+)s""".toRegex(RegexOption.IGNORE_CASE),
            """retry after ([\d.]+) seconds""".toRegex(RegexOption.IGNORE_CASE),
            """wait ([\d.]+) seconds""".toRegex(RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(message)
            if (match != null) {
                return match.groupValues[1].toDoubleOrNull()?.toInt() ?: 60
            }
        }
        return 60
    }

    /**
     * Build a simple text-only request.
     */
    @Suppress("unused")
    fun buildTextRequest(
        text: String,
        temperature: Float = 0.7f,
        maxOutputTokens: Int = 8192,
        enableGrounding: Boolean = false,
        systemInstruction: String? = null
    ): GeminiRequest {
        return GeminiRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = text))
                )
            ),
            generationConfig = GenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxOutputTokens,
                topK = 40,
                topP = 0.95f
            ),
            systemInstruction = systemInstruction?.let {
                Content(
                    role = "user",
                    parts = listOf(Part(text = it))
                )
            },
            tools = if (enableGrounding) {
                listOf(
                    Tool(
                        googleSearch = GoogleSearchRetrieval()
                    )
                )
            } else null,
            safetySettings = listOf(
                SafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
                SafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
                SafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
                SafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
            )
        )
    }
}
