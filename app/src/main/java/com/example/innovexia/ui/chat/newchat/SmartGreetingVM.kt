package com.example.innovexia.ui.chat.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.persona.Persona
import com.example.innovexia.core.persona.PersonaRepository
import com.example.innovexia.data.repository.MemoryRepository
import com.example.innovexia.data.repository.SourcesRepository
import com.example.innovexia.ui.persona.MemoryItem
import com.example.innovexia.ui.persona.SourceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for the Smart Greeting screen.
 * Generates personalized greetings and smart suggestions based on:
 * - Current persona
 * - Recent memory chunks
 * - Available sources (Gemini integration)
 * - Time of day
 */
@HiltViewModel
class SmartGreetingVM @Inject constructor(
    private val personaRepo: PersonaRepository,
    private val memoryRepo: MemoryRepository,
    private val sourcesRepo: SourcesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GreetingUiState())
    val uiState: StateFlow<GreetingUiState> = _uiState

    /**
     * Load greeting data for the given owner (legacy)
     */
    fun loadGreeting(ownerId: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        try {
            // Get current/default persona
            val persona = personaRepo.getDefaultPersona(ownerId)

            // Get recent memories (simulated for now - integrate with actual memory system)
            val memories = fetchRecentMemories(limit = 5)

            // Get recent sources (placeholder - integrate with Gemini sources)
            val sources = fetchRecentSources(limit = 3)

            // Build greeting text
            val greeting = buildGreeting(persona, memories)

            // Build smart suggestions
            val suggestions = buildSuggestions(persona, memories, sources)

            _uiState.value = GreetingUiState(
                greeting = greeting,
                suggestions = suggestions,
                persona = persona,
                isLoading = false
            )
        } catch (e: Exception) {
            // Graceful fallback
            _uiState.value = GreetingUiState(
                greeting = "Let's get started — what's on your mind today?",
                suggestions = getDefaultSuggestions(),
                isLoading = false
            )
        }
    }

    /**
     * Load greeting data with provided persona directly
     */
    fun loadGreetingWithPersona(persona: Persona?) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)

        try {
            android.util.Log.d("SmartGreetingVM", "Loading greeting for persona: ${persona?.name ?: "guest"}")

            // Get recent memories (simulated for now - integrate with actual memory system)
            val memories = fetchRecentMemories(limit = 5)

            // Get recent sources (placeholder - integrate with Gemini sources)
            val sources = fetchRecentSources(limit = 3)

            // Build greeting text
            val greeting = buildGreeting(persona, memories)

            // Build smart suggestions
            val suggestions = buildSuggestions(persona, memories, sources)

            _uiState.value = GreetingUiState(
                greeting = greeting,
                suggestions = suggestions,
                persona = persona,
                isLoading = false
            )
        } catch (e: Exception) {
            // Graceful fallback
            android.util.Log.w("SmartGreetingVM", "Failed to load greeting: ${e.message}")
            val defaultGreeting = if (persona != null) {
                "How can ${persona.name} help you today?"
            } else {
                "Let's get started — what's on your mind today?"
            }

            _uiState.value = GreetingUiState(
                greeting = defaultGreeting,
                suggestions = getDefaultSuggestions(),
                persona = persona,
                isLoading = false
            )
        }
    }

    /**
     * Build personalized greeting based on persona and context
     */
    private fun buildGreeting(persona: Persona?, memories: List<MemoryItem>): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val partOfDay = when (hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }

        // Emotional/contextual hint based on memory categories
        val memoryHint = when {
            memories.any { it.text.contains("work", ignoreCase = true) } ->
                "ready to tackle your projects"
            memories.any { it.text.contains("fitness", ignoreCase = true) || it.text.contains("workout", ignoreCase = true) } ->
                "feeling motivated"
            memories.any { it.text.contains("learn", ignoreCase = true) || it.text.contains("study", ignoreCase = true) } ->
                "eager to learn more"
            memories.any { it.text.contains("creative", ignoreCase = true) || it.text.contains("design", ignoreCase = true) } ->
                "ready to create"
            else -> "ready to dive in"
        }

        val personaName = persona?.name ?: "Innovexia"
        return "How can $personaName help you this $partOfDay, $memoryHint?"
    }

    /**
     * Build smart suggestions from memories, sources, and defaults
     */
    private fun buildSuggestions(
        persona: Persona?,
        memories: List<MemoryItem>,
        sources: List<SourceItem>
    ): List<GreetingSuggestion> {
        val suggestions = mutableListOf<GreetingSuggestion>()

        // Suggestions from memories (top 2)
        memories.take(2).forEach { memory ->
            val shortText = memory.text.take(40).trim()
            suggestions.add(
                GreetingSuggestion(
                    text = "Continue: $shortText...",
                    action = SuggestionAction.FromMemory(memory)
                )
            )
        }

        // Suggestions from sources (top 2)
        sources.take(2).forEach { source ->
            val shortName = source.label.take(25)
            suggestions.add(
                GreetingSuggestion(
                    text = "Use $shortName",
                    action = SuggestionAction.FromSource(source)
                )
            )
        }

        // Add default/generic suggestions
        suggestions.addAll(getDefaultSuggestions())

        // Return distinct suggestions, limited to 6
        return suggestions.distinctBy { it.text }.take(6)
    }

    /**
     * Get default suggestion chips
     */
    private fun getDefaultSuggestions(): List<GreetingSuggestion> {
        return listOf(
            GreetingSuggestion(
                text = "Brainstorm ideas",
                action = SuggestionAction.Chat("Help me brainstorm ideas")
            ),
            GreetingSuggestion(
                text = "Research",
                action = SuggestionAction.Chat("Help me research a topic")
            ),
            GreetingSuggestion(
                text = "Summarize a PDF",
                action = SuggestionAction.Chat("Summarize a document for me")
            ),
            GreetingSuggestion(
                text = "Draft an email",
                action = SuggestionAction.Chat("Help me draft a professional email")
            ),
            GreetingSuggestion(
                text = "Create a plan",
                action = SuggestionAction.Chat("Help me create a step-by-step plan")
            )
        )
    }

    /**
     * Fetch recent memory highlights from repository
     */
    private suspend fun fetchRecentMemories(limit: Int): List<MemoryItem> {
        return memoryRepo.fetchRecentHighlights(limit)
    }

    /**
     * Fetch recent sources from repository
     */
    private suspend fun fetchRecentSources(limit: Int): List<SourceItem> {
        return sourcesRepo.listRecent(limit)
    }
}
