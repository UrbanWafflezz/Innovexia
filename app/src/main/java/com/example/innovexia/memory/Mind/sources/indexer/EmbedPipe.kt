package com.example.innovexia.memory.Mind.sources.indexer

import com.example.innovexia.memory.Mind.embed.Embedder
import com.example.innovexia.memory.Mind.embed.Quantizer
import com.example.innovexia.memory.Mind.sources.store.entities.SourceChunkEntity
import java.util.UUID

/**
 * Pipeline for embedding and quantizing text chunks
 */
class EmbedPipe(
    private val embedder: Embedder
) {

    /**
     * Embed and quantize a list of text chunks
     * @return List of SourceChunkEntity ready for database insertion
     */
    suspend fun embedChunks(
        sourceId: String,
        personaId: String,
        chunks: List<TextChunk>
    ): List<SourceChunkEntity> {
        val entities = mutableListOf<SourceChunkEntity>()

        for (chunk in chunks) {
            try {
                // Embed the text
                val embedding = embedder.embed(chunk.text)

                // Quantize the embedding
                val (q8, scale) = Quantizer.quantize(embedding)

                // Create entity
                val entity = SourceChunkEntity(
                    id = UUID.randomUUID().toString(),
                    sourceId = sourceId,
                    personaId = personaId,
                    pageStart = chunk.pageStart,
                    pageEnd = chunk.pageEnd,
                    text = chunk.text,
                    dim = embedding.size,
                    q8 = q8,
                    scale = scale
                )

                entities.add(entity)
            } catch (e: Exception) {
                // Log and skip failed chunks
                android.util.Log.e("EmbedPipe", "Failed to embed chunk: ${e.message}")
            }
        }

        return entities
    }

    /**
     * Embed chunks in batches for better performance
     */
    suspend fun embedChunksBatch(
        sourceId: String,
        personaId: String,
        chunks: List<TextChunk>,
        batchSize: Int = 10
    ): List<SourceChunkEntity> {
        val entities = mutableListOf<SourceChunkEntity>()

        // Process in batches
        chunks.chunked(batchSize).forEach { batch ->
            try {
                // Batch embed
                val texts = batch.map { it.text }
                val embeddings = embedder.embedBatch(texts)

                // Quantize and create entities
                batch.forEachIndexed { index, chunk ->
                    val embedding = embeddings.getOrNull(index) ?: return@forEachIndexed
                    val (q8, scale) = Quantizer.quantize(embedding)

                    val entity = SourceChunkEntity(
                        id = UUID.randomUUID().toString(),
                        sourceId = sourceId,
                        personaId = personaId,
                        pageStart = chunk.pageStart,
                        pageEnd = chunk.pageEnd,
                        text = chunk.text,
                        dim = embedding.size,
                        q8 = q8,
                        scale = scale
                    )

                    entities.add(entity)
                }
            } catch (e: Exception) {
                android.util.Log.e("EmbedPipe", "Failed to embed batch: ${e.message}")
            }
        }

        return entities
    }
}
