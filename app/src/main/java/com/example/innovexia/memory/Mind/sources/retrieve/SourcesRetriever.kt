package com.example.innovexia.memory.Mind.sources.retrieve

import com.example.innovexia.memory.Mind.embed.Embedder
import com.example.innovexia.memory.Mind.embed.Quantizer
import com.example.innovexia.memory.Mind.sources.store.dao.SourceChunkDao
import com.example.innovexia.memory.Mind.sources.store.entities.SourceChunkEntity

/**
 * Retriever for searching PDF chunks by semantic similarity
 */
class SourcesRetriever(
    private val chunkDao: SourceChunkDao,
    private val embedder: Embedder
) {

    /**
     * Search for relevant chunks based on query
     * @param personaId Persona to search within
     * @param query User's query text
     * @param limit Maximum number of chunks to return
     * @return List of chunks ranked by similarity
     */
    suspend fun search(
        personaId: String,
        query: String,
        limit: Int = 5
    ): List<SourceChunkEntity> {
        android.util.Log.d("SourcesRetriever", "search called - personaId: $personaId, query: '$query', limit: $limit")

        if (query.isBlank()) {
            android.util.Log.d("SourcesRetriever", "Query is blank, returning empty list")
            return emptyList()
        }

        try {
            // 1. Embed the query
            android.util.Log.d("SourcesRetriever", "Embedding query...")
            val queryEmbedding = embedder.embed(query)
            val (q8, scale) = Quantizer.quantize(queryEmbedding)
            android.util.Log.d("SourcesRetriever", "Query embedded successfully")

            // 2. Get all chunks for this persona
            android.util.Log.d("SourcesRetriever", "Fetching chunks for persona: $personaId")
            val chunks = chunkDao.getByPersona(personaId)
            android.util.Log.d("SourcesRetriever", "Found ${chunks.size} chunks for persona")

            if (chunks.isEmpty()) {
                android.util.Log.d("SourcesRetriever", "No chunks found for persona")
                return emptyList()
            }

            // 3. Rank by cosine similarity
            val ranked = chunks.map { chunk ->
                val similarity = Quantizer.cosineSimilarity(
                    q8, scale,
                    chunk.q8, chunk.scale
                )
                android.util.Log.d("SourcesRetriever", "Chunk ${chunk.id} similarity: $similarity")
                chunk to similarity
            }
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first }

            android.util.Log.d("SourcesRetriever", "Returning ${ranked.size} ranked chunks")
            return ranked
        } catch (e: Exception) {
            android.util.Log.e("SourcesRetriever", "Error searching chunks: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Get context string from chunks
     * @param chunks List of source chunks
     * @return Formatted context string
     */
    fun formatContext(chunks: List<SourceChunkEntity>): String {
        if (chunks.isEmpty()) {
            return ""
        }

        val formattedChunks = chunks.joinToString("\n\n") { chunk ->
            "[Source: ${chunk.sourceId}, Pages ${chunk.pageStart}-${chunk.pageEnd}]\n${chunk.text}"
        }

        return """
Relevant information from uploaded PDFs:

$formattedChunks

---
""".trimIndent()
    }
}
