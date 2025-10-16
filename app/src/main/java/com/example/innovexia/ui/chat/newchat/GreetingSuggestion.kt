package com.example.innovexia.ui.chat.newchat

import com.example.innovexia.ui.persona.MemoryItem
import com.example.innovexia.ui.persona.SourceItem

/**
 * Represents an action that can be triggered by a greeting suggestion
 */
sealed class SuggestionAction {
    data class FromMemory(val memory: MemoryItem) : SuggestionAction()
    data class FromSource(val source: SourceItem) : SuggestionAction()
    data class Chat(val prefillText: String) : SuggestionAction()
}

/**
 * A suggestion chip displayed in the greeting screen
 */
data class GreetingSuggestion(
    val text: String,
    val action: SuggestionAction
)

/**
 * UI state for the smart greeting screen
 */
data class GreetingUiState(
    val greeting: String = "Let's get started — what's on your mind today?",
    val suggestions: List<GreetingSuggestion> = emptyList(),
    val persona: com.example.innovexia.core.persona.Persona? = null,
    val isLoading: Boolean = false
)
