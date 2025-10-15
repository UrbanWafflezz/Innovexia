package com.example.innovexia.core.health

import com.example.innovexia.BuildConfig

/**
 * Service descriptor for health monitoring
 */
data class ServiceDescriptor(
    val id: String,
    val name: String,
    val baseUrl: String,
    val healthPath: String,
    val wsUrl: String? = null
)

/**
 * Service catalog for health monitoring
 *
 * Configure endpoints via BuildConfig or remote config.
 *
 * To set per-environment:
 * 1. Add to app/build.gradle.kts:
 *    buildConfigField("String", "CONTEXT_BASE", "\"https://api.innovexia.dev/context\"")
 *    buildConfigField("String", "CONTEXT_WS", "\"wss://api.innovexia.dev/context/ws\"")
 * 2. Repeat for other services
 * 3. Use different values per buildType (debug/release) or productFlavor (dev/stage/prod)
 */
object ServiceCatalog {

    /**
     * Load service catalog
     * Includes all active services with real-time health checks
     */
    fun load(): List<ServiceDescriptor> = buildList {
        // Local Database (Room database health check)
        add(ServiceDescriptor(
            id = "database",
            name = "Local Database",
            baseUrl = "",
            healthPath = ""
        ))

        // Local Cache (DataStore and in-memory caching)
        add(ServiceDescriptor(
            id = "cache",
            name = "Local Cache",
            baseUrl = "",
            healthPath = ""
        ))

        // Memory System (persona memory and embeddings)
        add(ServiceDescriptor(
            id = "memory-system",
            name = "Context Memory Engine",
            baseUrl = "",
            healthPath = ""
        ))

        // Gemini Bridge (Google Gemini API connectivity test)
        add(ServiceDescriptor(
            id = "gemini-bridge",
            name = "Gemini Bridge",
            baseUrl = "https://generativelanguage.googleapis.com",
            healthPath = "/v1beta/models"
        ))

        // Persona Service (Firebase Firestore connectivity)
        add(ServiceDescriptor(
            id = "persona-service",
            name = "Persona Service",
            baseUrl = "https://firestore.googleapis.com",
            healthPath = ""
        ))

        // Rolling Summarizer (local chat summarization)
        add(ServiceDescriptor(
            id = "rolling-summarizer",
            name = "Rolling Summarizer",
            baseUrl = "",
            healthPath = ""
        ))
    }

    /**
     * Get BuildConfig field via reflection (safe fallback)
     */
    private fun getConfigString(fieldName: String, default: String?): String {
        return try {
            val field = BuildConfig::class.java.getDeclaredField(fieldName)
            field.get(null) as? String ?: default.orEmpty()
        } catch (e: Exception) {
            default.orEmpty()
        }
    }
}
