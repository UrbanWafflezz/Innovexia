package com.example.innovexia.ui.chat.newchat.suggestions

import com.example.innovexia.core.persona.Persona
import com.example.innovexia.data.ai.GeminiService
import com.example.innovexia.memory.Mind.api.MemoryHit
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity
import kotlinx.coroutines.flow.fold
import org.json.JSONArray
import org.json.JSONObject

/**
 * Optional Gemini-powered suggestion generation and reranking
 */
object GeminiReranker {

    /**
     * Generate smart, actionable suggestions from memories and sources using Gemini
     */
    suspend fun generateSuggestions(
        gemini: GeminiService,
        memories: List<MemoryHit>,
        sources: List<SourceEntity>,
        persona: Persona
    ): List<SuggestionCardUi> {
        if (memories.isEmpty() && sources.isEmpty()) {
            return emptyList()
        }

        val prompt = buildString {
            appendLine("Generate EXACTLY 4 smart conversation starters. Analyze the user's memories and extract KEY TOPICS to create specific, actionable suggestions.")
            appendLine()
            appendLine("RULES:")
            appendLine("1. Title: 2-4 words MAX (e.g., 'Python coding tips', 'Milo's training', 'Red color theory')")
            appendLine("2. Subtitle: Brief context (5-8 words)")
            appendLine("3. Extract SPECIFIC topics: programming languages, pet names, hobbies, projects, interests")
            appendLine("4. Focus on NOUNS and CONCRETE topics (Python, JavaScript, dogs, fitness, etc.)")
            appendLine("5. If user discussed coding/programming → suggest specific languages or concepts")
            appendLine("6. If user mentioned pets → use their actual names")
            appendLine("7. If user talked about hobbies → reference the specific hobby")
            appendLine("8. NO generic phrases - every suggestion MUST be memory-specific")
            appendLine()
            appendLine("USER'S MEMORIES (extract key topics from these):")
            if (memories.isNotEmpty()) {
                memories.take(10).forEach { memory ->
                    // Extract clean user queries/statements, keeping important keywords
                    val cleanText = memory.memory.text
                        .replace(Regex("Thanks for sharing.*"), "")
                        .replace(Regex("I'll remember.*"), "")
                        .replace(Regex("Hello there!.*"), "")
                        .replace(Regex("I'm Inno.*"), "")
                        .trim()
                    if (cleanText.isNotBlank() && cleanText.length > 5) {
                        appendLine("• ${cleanText.take(150)}")
                    }
                }
            }
            if (sources.isNotEmpty()) {
                appendLine()
                appendLine("AVAILABLE FILES:")
                sources.take(3).forEach { source ->
                    appendLine("• ${source.displayName}")
                }
            }
            appendLine()
            appendLine("EXAMPLES:")
            appendLine("If memories mention 'Python programming': {\"title\": \"Python coding help\", \"subtitle\": \"Continue where you left off\"}")
            appendLine("If memories mention 'dog named Milo': {\"title\": \"Talk about Milo\", \"subtitle\": \"Your dog's activities\"}")
            appendLine("If memories mention 'fitness goals': {\"title\": \"Fitness planning\", \"subtitle\": \"Your workout routine\"}")
            appendLine("If memories mention 'JavaScript project': {\"title\": \"JavaScript tips\", \"subtitle\": \"For your project\"}")
            appendLine()
            appendLine("Now analyze the memories and generate EXACTLY 4 specific suggestions as a JSON array:")
            appendLine("[{\"title\": \"...\", \"subtitle\": \"...\"}, {\"title\": \"...\", \"subtitle\": \"...\"}, {\"title\": \"...\", \"subtitle\": \"...\"}, {\"title\": \"...\", \"subtitle\": \"...\"}]")
            appendLine()
            appendLine("Return ONLY the JSON array with 4 items. NO markdown formatting, NO explanation.")
        }

        return runCatching {
            android.util.Log.d("GeminiReranker", "🚀 Generating suggestions from ${memories.size} memories and ${sources.size} sources")
            android.util.Log.d("GeminiReranker", "📝 Sample memories:")
            memories.take(3).forEach { memory ->
                android.util.Log.d("GeminiReranker", "  - ${memory.memory.text.take(80)}")
            }
            android.util.Log.d("GeminiReranker", "📤 Sending prompt to Gemini...")

            // Collect all chunks from the streaming response
            val response = gemini.streamText(prompt, persona = persona, enableThinking = false)
                .fold("") { acc, chunk -> acc + chunk }

            if (response.isEmpty()) {
                android.util.Log.w("GeminiReranker", "⚠️ Gemini returned empty response")
                return emptyList()
            }

            android.util.Log.d("GeminiReranker", "✅ Got Gemini response: ${response.take(200)}...")
            android.util.Log.d("GeminiReranker", "📄 Full response: $response")

            // Parse JSON array
            val suggestionsData = parseJsonSuggestions(response)
            android.util.Log.d("GeminiReranker", "📋 Parsed ${suggestionsData.size} suggestions from JSON")

            if (suggestionsData.isEmpty()) {
                android.util.Log.w("GeminiReranker", "⚠️ No suggestions parsed from response")
                return emptyList()
            }

            // Convert to SuggestionCardUi
            suggestionsData.mapIndexed { index, data ->
                SuggestionCardUi(
                    id = "gemini-$index",
                    kind = when {
                        data.title.contains("continue", ignoreCase = true) ||
                        data.title.contains("finish", ignoreCase = true) ||
                        data.title.contains("resume", ignoreCase = true) -> SuggestionKind.CONTINUE_TASK
                        data.title.contains("discuss", ignoreCase = true) ||
                        data.title.contains("explore", ignoreCase = true) ||
                        data.title.contains("talk about", ignoreCase = true) ||
                        data.title.contains("learn", ignoreCase = true) -> SuggestionKind.PICK_UP_TOPIC
                        sources.any { it.displayName.contains(data.title, ignoreCase = true) } -> SuggestionKind.RECAP_FILE
                        else -> SuggestionKind.QUICK_ACTION
                    },
                    title = data.title.take(35).trim(),
                    subtitle = data.subtitle.take(40).trim().ifBlank { "Based on your memories" },
                    icon = com.example.innovexia.R.drawable.ic_sparkles,
                    personaId = persona.id,
                    payload = emptyMap()
                )
            }
        }.getOrElse { error ->
            android.util.Log.w("GeminiReranker", "Failed to generate suggestions: ${error.message}")
            emptyList()
        }
    }

    /**
     * Re-title and optionally fill gaps in suggestions using Gemini (legacy)
     */
    suspend fun retitleAndFill(
        gemini: GeminiService,
        base: List<SuggestionCardUi>,
        memories: List<MemoryHit>,
        sources: List<SourceEntity>,
        persona: Persona
    ): List<SuggestionCardUi> {
        if (base.isEmpty()) return base

        val prompt = buildString {
            appendLine("You are generating smart, contextual suggestion cards for a chat interface.")
            appendLine("These suggestions must be:")
            appendLine("- Very concise (2-5 words max)")
            appendLine("- Action-oriented and immediately clickable")
            appendLine("- Specific to the user's context")
            appendLine()
            appendLine("User context:")
            appendLine("- Recent memories: ${memories.take(5).joinToString(", ") { it.memory.text.take(60) }}")
            appendLine("- Available sources: ${sources.take(3).joinToString(", ") { it.displayName }}")
            appendLine()
            appendLine("Current suggestions to improve:")
            base.forEachIndexed { i, s ->
                appendLine("${i + 1}. ${s.title}${if (!s.subtitle.isNullOrBlank()) " (${s.subtitle})" else ""}")
            }
            appendLine()
            appendLine("Rewrite these as concise, engaging titles (like 'Brainstorm ideas', 'Research anything', 'Summarize a PDF').")
            appendLine("Return ONLY a JSON array of strings:")
            appendLine("[\"Title 1\", \"Title 2\", ...]")
        }

        return runCatching {
            // Use Gemini to rewrite titles - collect all chunks from the streaming response
            val response = gemini.streamText(prompt, persona = persona, enableThinking = false)
                .fold("") { acc, chunk -> acc + chunk }

            if (response.isEmpty()) return base

            // Parse JSON array
            val titles = parseJsonArray(response)
            if (titles.isEmpty()) return base

            // Zip titles with base cards (keep to 40 chars max for UI)
            base.zip(titles) { card, newTitle ->
                card.copy(title = newTitle.take(40).trim())
            }
        }.getOrElse {
            // On error, return original suggestions
            android.util.Log.w("GeminiReranker", "Failed to retitle suggestions: ${it.message}")
            base
        }
    }

    /**
     * Parse JSON array from response
     */
    private fun parseJsonArray(jsonString: String): List<String> {
        return try {
            // Clean markdown code blocks if present
            val cleaned = jsonString
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleaned)
            List(jsonArray.length()) { i ->
                jsonArray.getString(i)
            }
        } catch (e: Exception) {
            android.util.Log.w("GeminiReranker", "Failed to parse JSON: ${e.message}")
            emptyList()
        }
    }

    /**
     * Parse JSON suggestions with title and subtitle
     */
    private fun parseJsonSuggestions(jsonString: String): List<SuggestionData> {
        return try {
            // Clean markdown code blocks if present
            val cleaned = jsonString
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleaned)
            List(jsonArray.length()) { i ->
                val obj = jsonArray.getJSONObject(i)
                SuggestionData(
                    title = obj.optString("title", ""),
                    subtitle = obj.optString("subtitle", "")
                )
            }.filter { it.title.isNotBlank() }
        } catch (e: Exception) {
            android.util.Log.w("GeminiReranker", "Failed to parse JSON suggestions: ${e.message}")
            emptyList()
        }
    }

    /**
     * Data class for parsed suggestions
     */
    private data class SuggestionData(
        val title: String,
        val subtitle: String
    )
}
