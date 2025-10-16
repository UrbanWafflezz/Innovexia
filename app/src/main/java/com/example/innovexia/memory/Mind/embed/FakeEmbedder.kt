package com.example.innovexia.memory.Mind.embed

import kotlin.math.abs
import kotlin.math.sin

/**
 * Fake embedder for testing - generates deterministic vectors from text hash
 */
class FakeEmbedder(private val dim: Int = 256) : Embedder {

    override suspend fun embed(text: String): FloatArray {
        val hash = text.hashCode()
        return FloatArray(dim) { i ->
            // Deterministic pseudo-random values based on text hash
            val seed = hash + i
            sin(seed.toDouble()).toFloat()
        }
    }

    override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
        return texts.map { embed(it) }
    }

    override fun getDimension(): Int = dim
}
