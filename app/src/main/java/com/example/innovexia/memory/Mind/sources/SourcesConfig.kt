package com.example.innovexia.memory.Mind.sources

/**
 * Configuration for sources indexing and processing
 */
data class SourcesConfig(
    val chunkChars: Int = 1200,
    val chunkOverlap: Int = 150,
    val maxPdfMB: Int = 30,
    val maxPages: Int = 2000,
    val dim: Int = 768,  // Gemini embedding dimension (change to 256 for custom models)
    val thumbnailWidth: Int = 512
) {
    companion object {
        val DEFAULT = SourcesConfig()
    }
}
