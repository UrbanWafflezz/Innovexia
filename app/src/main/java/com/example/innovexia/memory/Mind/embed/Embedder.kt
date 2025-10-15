package com.example.innovexia.memory.Mind.embed

/**
 * Interface for text embedding
 */
interface Embedder {
    /**
     * Embed a single text string
     * @return float array of dim size
     */
    suspend fun embed(text: String): FloatArray

    /**
     * Embed multiple texts in batch
     */
    suspend fun embedBatch(texts: List<String>): List<FloatArray>

    /**
     * Get embedding dimension
     */
    fun getDimension(): Int
}
