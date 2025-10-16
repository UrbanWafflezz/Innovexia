package com.example.innovexia.memory.Mind.api

/**
 * Configuration for memory engine
 * INCREASED: All retrieval limits significantly to ensure comprehensive memory access
 */
data class MemoryConfig(
    val dim: Int = 768,  // Gemini text-embedding-004 uses 768 dimensions
    val maxPerPersona: Int = 100_000,
    val kFts: Int = 200,        // INCREASED: from 50 to 200 (FTS candidates)
    val kVec: Int = 200,        // INCREASED: from 50 to 200 (Vector candidates)
    val kReturn: Int = 50,      // INCREASED: from 12 to 50 (memories returned)
    val importanceFloor: Double = 0.05,
    val pruneLowImportanceAfterDays: Int = 365,
    val dedupeCosine: Float = 0.97f,
    // Ranking weights
    val w1Bm25: Float = 0.4f,
    val w2Cosine: Float = 0.3f,
    val w3Recency: Float = 0.2f,
    val w4Importance: Float = 0.1f
) {
    companion object {
        val DEFAULT = MemoryConfig()
    }
}
