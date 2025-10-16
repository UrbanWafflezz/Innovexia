package com.example.innovexia.core.persona

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Firestore DTO for persona documents (backward compatible)
 */
data class PersonaDto(
    val name: String = "",
    val initial: String = "",
    val color: Long = 0xFF60A5FA,
    val summary: String = "",
    val tags: List<String> = emptyList(),
    val system: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isDefault: Boolean = false
) {
    fun toMap(createdAt: Any, updatedAt: Any): Map<String, Any?> = mapOf(
        "name" to name,
        "initial" to initial,
        "color" to color,
        "summary" to summary,
        "tags" to tags,
        "system" to system,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDefault" to isDefault
    )
}

/**
 * Domain model for personas
 */
data class Persona(
    val id: String,
    val name: String,
    val initial: String,
    val color: Long,
    val summary: String,
    val tags: List<String>,
    val system: String?,
    val isDefault: Boolean,
    val updatedAt: Timestamp? = null,
    val extendedSettings: String? = null, // JSON string of Persona 2.0 extended settings
    // Parsed extended settings for easy access
    val behavior: PersonaBehavior? = null,
    val systemConfig: PersonaSystem? = null,
    val memory: PersonaMemory? = null,
    val sources: PersonaSources? = null
) {
    /**
     * Parse extended settings from JSON if available
     */
    fun parseExtendedSettings(): Persona {
        if (extendedSettings == null) return this

        return try {
            val gson = com.google.gson.Gson()
            val map = gson.fromJson<Map<String, Any>>(
                extendedSettings,
                object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
            )

            this.copy(
                behavior = map["behavior"]?.let { gson.fromJson(gson.toJson(it), PersonaBehavior::class.java) },
                systemConfig = map["system"]?.let { gson.fromJson(gson.toJson(it), PersonaSystem::class.java) },
                memory = map["memory"]?.let { gson.fromJson(gson.toJson(it), PersonaMemory::class.java) },
                sources = map["sources"]?.let { gson.fromJson(gson.toJson(it), PersonaSources::class.java) }
            )
        } catch (e: Exception) {
            android.util.Log.e("Persona", "Failed to parse extended settings: ${e.message}")
            this
        }
    }
}

// ============================================================
// PERSONA 2.0 EXTENDED DATA MODELS
// ============================================================

/**
 * Behavior configuration for persona
 */
data class PersonaBehavior(
    val conciseness: Float = 0.6f,
    val formality: Float = 0.5f,
    val empathy: Float = 0.4f,
    val creativityTemp: Float = 0.7f,
    val topP: Float = 0.9f,
    val thinkingDepth: String = "balanced", // "off" | "balanced" | "deep"
    val proactivity: String = "ask_when_unclear",
    val safetyLevel: String = "standard",
    val hallucinationGuard: String = "prefer_idk",
    val selfCheck: SelfCheckConfig = SelfCheckConfig(),
    val citationPolicy: String = "when_uncertain",
    val formatting: FormattingConfig = FormattingConfig()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "conciseness" to conciseness,
        "formality" to formality,
        "empathy" to empathy,
        "creativityTemp" to creativityTemp,
        "topP" to topP,
        "thinkingDepth" to thinkingDepth,
        "proactivity" to proactivity,
        "safetyLevel" to safetyLevel,
        "hallucinationGuard" to hallucinationGuard,
        "selfCheck" to selfCheck.toMap(),
        "citationPolicy" to citationPolicy,
        "formatting" to formatting.toMap()
    )
}

data class SelfCheckConfig(
    val enabled: Boolean = true,
    val maxMs: Int = 500
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "enabled" to enabled,
        "maxMs" to maxMs
    )
}

data class FormattingConfig(
    val markdown: Boolean = true,
    val emoji: String = "light" // "off" | "light" | "full"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "markdown" to markdown,
        "emoji" to emoji
    )
}

/**
 * System prompt configuration
 */
data class PersonaSystem(
    val instructions: String = "",
    val rules: List<SystemRule> = emptyList(),
    val variables: List<String> = listOf("{user_name}", "{today}", "{timezone}", "{app_name}"),
    val version: Int = 1
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "instructions" to instructions,
        "rules" to rules.map { it.toMap() },
        "variables" to variables,
        "version" to version
    )
}

data class SystemRule(
    val `when`: String = "always", // "always" | "on_request" | conditional
    val `do`: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "when" to `when`,
        "do" to `do`
    )
}

/**
 * Memory configuration - Simplified to enable/disable
 */
data class PersonaMemory(
    val enabled: Boolean = true
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "enabled" to enabled
    )
}

/**
 * Sources configuration - Simplified to enable/disable
 */
data class PersonaSources(
    val enabled: Boolean = true
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "enabled" to enabled
    )
}

/**
 * Tools configuration
 */
data class PersonaTools(
    val web: Boolean = true,
    val code: Boolean = false,
    val vision: Boolean = true,
    val audio: Boolean = false,
    val functions: List<FunctionConfig> = emptyList(),
    val modelRouting: ModelRoutingConfig = ModelRoutingConfig()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "web" to web,
        "code" to code,
        "vision" to vision,
        "audio" to audio,
        "functions" to functions.map { it.toMap() },
        "modelRouting" to modelRouting.toMap()
    )
}

data class FunctionConfig(
    val name: String = "",
    val allowed: Boolean = true
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "allowed" to allowed
    )
}

data class ModelRoutingConfig(
    val preferred: String = "fast", // "fast" | "thinking" | "creative"
    val fallbacks: List<String> = listOf("thinking")
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "preferred" to preferred,
        "fallbacks" to fallbacks
    )
}

/**
 * Limits configuration
 */
data class PersonaLimits(
    val maxOutputTokens: Int = 1200,
    val maxContextTokens: Int = 48000,
    val timeBudgetMs: Int = 15000,
    val rateWeight: Float = 1.0f,
    val concurrency: Int = 2
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "maxOutputTokens" to maxOutputTokens,
        "maxContextTokens" to maxContextTokens,
        "timeBudgetMs" to timeBudgetMs,
        "rateWeight" to rateWeight,
        "concurrency" to concurrency
    )
}

/**
 * Testing configuration
 */
data class PersonaTesting(
    val scenarios: List<TestScenario> = emptyList()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "scenarios" to scenarios.map { it.toMap() }
    )
}

data class TestScenario(
    val prompt: String = "",
    val expectedBehavior: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "prompt" to prompt,
        "expectedBehavior" to expectedBehavior
    )
}

/**
 * Author metadata
 */
data class AuthorMeta(
    val uid: String = "",
    val name: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "name" to name
    )
}

/**
 * Extended Persona DTO (Persona 2.0) for Firestore
 */
data class PersonaDraftDto(
    val name: String = "",
    val initial: String = "",
    val color: Long = 0xFF60A5FA,
    val bio: String = "",
    val tags: List<String> = emptyList(),
    val defaultLanguage: String = "en-US",
    val greeting: String = "",
    val isDefault: Boolean = false,
    val visibility: String = "private", // "private" | "team" | "public"
    val status: String = "draft", // "draft" | "published"
    val behavior: PersonaBehavior = PersonaBehavior(),
    val system: PersonaSystem = PersonaSystem(),
    val memory: PersonaMemory = PersonaMemory(),
    val sources: PersonaSources = PersonaSources(),
    val tools: PersonaTools = PersonaTools(),
    val limits: PersonaLimits = PersonaLimits(),
    val testing: PersonaTesting = PersonaTesting(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val author: AuthorMeta? = null
) {
    fun toMap(createdAt: Any, updatedAt: Any): Map<String, Any?> = mapOf(
        "name" to name,
        "initial" to initial,
        "color" to color,
        "bio" to bio,
        "tags" to tags,
        "defaultLanguage" to defaultLanguage,
        "greeting" to greeting,
        "isDefault" to isDefault,
        "visibility" to visibility,
        "status" to status,
        "behavior" to behavior.toMap(),
        "system" to system.toMap(),
        "memory" to memory.toMap(),
        "sources" to sources.toMap(),
        "tools" to tools.toMap(),
        "limits" to limits.toMap(),
        "testing" to testing.toMap(),
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "author" to author?.toMap()
    )
}

/**
 * Extension to convert Firestore document to Persona
 */
fun DocumentSnapshot.toPersona(): Persona? {
    return try {
        val extendedSettingsJson = try {
            // Try to get extendedSettings field
            getString("extendedSettings")
        } catch (e: Exception) {
            null
        }

        Persona(
            id = id,
            name = getString("name") ?: return null,
            initial = getString("initial") ?: "",
            color = getLong("color") ?: 0xFF60A5FA,
            summary = getString("summary") ?: getString("bio") ?: "",
            tags = get("tags") as? List<String> ?: emptyList(),
            system = getString("system") ?: (get("system") as? Map<*, *>)?.get("instructions") as? String,
            isDefault = getBoolean("isDefault") ?: false,
            updatedAt = getTimestamp("updatedAt"),
            extendedSettings = extendedSettingsJson
        ).parseExtendedSettings() // Automatically parse extended settings
    } catch (e: Exception) {
        null
    }
}
