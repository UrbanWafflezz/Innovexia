package com.example.innovexia.ui.chat.newchat.suggestions

import com.example.innovexia.R
import com.example.innovexia.memory.Mind.api.MemoryHit
import com.example.innovexia.memory.Mind.api.MemoryKind
import com.example.innovexia.memory.Mind.sources.store.entities.SourceEntity

/**
 * Heuristics-based suggestion generator from memories and sources
 */
object MemorySuggest {

    /**
     * Generate suggestions from memories and sources
     */
    fun from(
        memories: List<MemoryHit>,
        sources: List<SourceEntity>,
        personaId: String
    ): List<SuggestionCardUi> {
        val out = mutableListOf<SuggestionCardUi>()

        // A) Projects/Events
        memories
            .filter { it.memory.kind in listOf(MemoryKind.PROJECT, MemoryKind.EVENT) }
            .take(2)
            .forEach { memoryHit ->
                out += SuggestionCardUi(
                    id = "m-${memoryHit.memory.id}",
                    kind = SuggestionKind.CONTINUE_TASK,
                    title = titleize(memoryHit.memory.text),
                    subtitle = "From your recent work",
                    icon = R.drawable.ic_play,
                    personaId = personaId,
                    payload = mapOf("memoryId" to memoryHit.memory.id)
                )
            }

        // B) Sources
        sources.take(2).forEach { source ->
            val label = source.displayName.ifBlank { source.domain ?: "Source" }
            out += SuggestionCardUi(
                id = "s-${source.id}",
                kind = SuggestionKind.RECAP_FILE,
                title = label,
                subtitle = when (source.type) {
                    "PDF" -> "${source.pageCount ?: 0} pages"
                    "URL" -> source.domain ?: "Website"
                    else -> "Attached source"
                },
                icon = when (source.type) {
                    "PDF" -> R.drawable.ic_pdf
                    "URL" -> R.drawable.ic_link
                    else -> R.drawable.ic_doc
                },
                personaId = personaId,
                payload = mapOf("sourceId" to source.id)
            )
        }

        // C) Topics/Interests
        memories
            .filter { it.memory.kind in listOf(MemoryKind.PREFERENCE, MemoryKind.KNOWLEDGE) }
            .take(2)
            .forEach { memoryHit ->
                out += SuggestionCardUi(
                    id = "t-${memoryHit.memory.id}",
                    kind = SuggestionKind.PICK_UP_TOPIC,
                    title = titleize(memoryHit.memory.text),
                    subtitle = "Based on your interests",
                    icon = R.drawable.ic_compass,
                    personaId = personaId,
                    payload = mapOf("memoryId" to memoryHit.memory.id)
                )
            }

        return out
    }

    /**
     * Truncate and clean text for titles
     * Allows up to ~90 chars to fill 2 lines in the card (45 chars per line avg)
     */
    private fun titleize(text: String): String {
        val cleaned = text
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()

        if (cleaned.length <= 90) {
            return cleaned.trimEnd('.', ' ')
        }

        // Truncate at word boundary
        val truncated = cleaned.take(90)
        val lastSpace = truncated.lastIndexOf(' ')

        return if (lastSpace > 60) { // Only break at word if we're past 60 chars
            truncated.substring(0, lastSpace).trimEnd('.', ' ') + "…"
        } else {
            truncated.trimEnd('.', ' ') + "…"
        }
    }
}
