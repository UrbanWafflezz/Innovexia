package com.example.innovexia.core.ai

/**
 * Model identifiers for Gemini 2.5 family
 */
object ModelIds {
    const val GEM_PRO        = "gemini-2.5-pro"
    const val GEM_FLASH      = "gemini-2.5-flash"
    const val GEM_FLASH_LITE = "gemini-2.5-flash-lite"

    val all = listOf(GEM_PRO, GEM_FLASH, GEM_FLASH_LITE)

    /**
     * Check if a model ID is valid Gemini 2.5 model
     */
    fun isValid(id: String): Boolean = id in all
}

/**
 * Model metadata for display purposes
 */
data class ModelInfo(
    val id: String,
    val label: String,
    val description: String
)

/**
 * Available Gemini 2.5 models with official descriptions
 */
val GeminiModels = listOf(
    ModelInfo(
        ModelIds.GEM_PRO,
        "Gemini 2.5 Pro",
        "Most advanced thinking model for complex reasoning, large datasets, and analysis"
    ),
    ModelInfo(
        ModelIds.GEM_FLASH,
        "Gemini 2.5 Flash",
        "Best price-performance for high-volume tasks and agentic use cases"
    ),
    ModelInfo(
        ModelIds.GEM_FLASH_LITE,
        "Gemini 2.5 Flash Lite",
        "Fastest model optimized for cost-efficiency and high throughput"
    )
)

/**
 * Get ModelInfo by ID
 */
fun getModelInfo(id: String): ModelInfo? {
    return GeminiModels.find { it.id == id }
}

/**
 * Get display label for a model ID
 */
fun getModelLabel(id: String): String {
    return when (id) {
        ModelIds.GEM_PRO -> "Gemini 2.5 Pro"
        ModelIds.GEM_FLASH -> "Gemini 2.5 Flash"
        ModelIds.GEM_FLASH_LITE -> "Gemini 2.5 Flash Lite"
        else -> id
    }
}
