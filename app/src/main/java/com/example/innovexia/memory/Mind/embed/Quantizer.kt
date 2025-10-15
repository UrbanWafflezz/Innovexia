package com.example.innovexia.memory.Mind.embed

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Quantizer for converting float vectors to int8
 */
object Quantizer {

    /**
     * Quantize float vector to int8
     * @return Pair of (quantized bytes, scale factor)
     */
    fun quantize(vector: FloatArray): Pair<ByteArray, Float> {
        if (vector.isEmpty()) {
            return Pair(ByteArray(0), 1f)
        }

        // Find max absolute value
        val maxAbs = vector.maxOf { abs(it) }
        val scale = if (maxAbs > 0f) maxAbs / 127f else 1f

        // Quantize each element
        val q8 = ByteArray(vector.size) { i ->
            (vector[i] / scale).roundToInt().toByte()
        }

        return Pair(q8, scale)
    }

    /**
     * Dequantize int8 back to float
     */
    fun dequantize(q8: ByteArray, scale: Float): FloatArray {
        return FloatArray(q8.size) { i ->
            q8[i].toInt() * scale
        }
    }

    /**
     * Compute cosine similarity between two quantized vectors
     */
    fun cosineSimilarity(
        q8a: ByteArray,
        scaleA: Float,
        q8b: ByteArray,
        scaleB: Float
    ): Float {
        if (q8a.size != q8b.size) return 0f

        // Dot product in int8 space
        var dot = 0
        var normA = 0
        var normB = 0

        for (i in q8a.indices) {
            val a = q8a[i].toInt()
            val b = q8b[i].toInt()
            dot += a * b
            normA += a * a
            normB += b * b
        }

        if (normA == 0 || normB == 0) return 0f

        // Scale back to float space
        val scaledDot = dot * scaleA * scaleB
        val scaledNormA = sqrt(normA.toFloat()) * scaleA
        val scaledNormB = sqrt(normB.toFloat()) * scaleB

        return scaledDot / (scaledNormA * scaledNormB)
    }

    /**
     * Compute dot product (faster approximation for ranking)
     */
    fun dotProduct(
        q8a: ByteArray,
        scaleA: Float,
        q8b: ByteArray,
        scaleB: Float
    ): Float {
        if (q8a.size != q8b.size) return 0f

        var dot = 0
        for (i in q8a.indices) {
            dot += q8a[i].toInt() * q8b[i].toInt()
        }

        return dot * scaleA * scaleB
    }
}
