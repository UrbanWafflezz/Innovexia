package com.example.innovexia.memory.Mind.embed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Embedder using Gemini REST API for generating embeddings
 * Uses the embedContent REST endpoint directly since Android SDK doesn't support embeddings
 */
class GeminiEmbedder(
    private val apiKey: String,
    private val modelName: String = "text-embedding-004"
) : Embedder {

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:embedContent"

    override suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        android.util.Log.d("GeminiEmbedder", "Embedding text: '${text.take(100)}...' (length=${text.length})")
        try {
            val url = URL("$baseUrl?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Create request body using JSONObject
            val requestJson = JSONObject().apply {
                put("content", JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", text)
                        })
                    })
                })
            }

            // Send request
            connection.outputStream.use {
                it.write(requestJson.toString().toByteArray())
            }

            // Read response
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val responseJson = JSONObject(responseText)

                // Parse embedding values
                val embeddingArray = responseJson.getJSONObject("embedding").getJSONArray("values")
                val result = FloatArray(embeddingArray.length()) { i ->
                    embeddingArray.getDouble(i).toFloat()
                }
                android.util.Log.d("GeminiEmbedder", "Successfully embedded, dimension=${result.size}")
                result
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }
                android.util.Log.e("GeminiEmbedder", "HTTP $responseCode: $errorText")
                FloatArray(768) // Return zero vector on error
            }
        } catch (e: Exception) {
            android.util.Log.e("GeminiEmbedder", "Error embedding text: ${e.message}", e)
            FloatArray(768) // Return zero vector on error
        }
    }

    override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        // Process sequentially (could be optimized with batch endpoint)
        return texts.map { embed(it) }
    }

    override fun getDimension(): Int = 768
}
