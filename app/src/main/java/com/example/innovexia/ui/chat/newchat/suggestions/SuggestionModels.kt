package com.example.innovexia.ui.chat.newchat.suggestions

/**
 * Types of suggestion cards
 */
enum class SuggestionKind {
    CONTINUE_TASK,    // Resume incomplete task/project
    RECAP_FILE,       // Use an indexed source (PDF/URL)
    PICK_UP_TOPIC,    // Explore a topic from preferences/knowledge
    QUICK_ACTION      // Generic quick actions
}

/**
 * Card UI model for smart suggestions
 */
data class SuggestionCardUi(
    val id: String,
    val kind: SuggestionKind,
    val title: String,                      // "Pick up AI résumé draft"
    val subtitle: String?,                  // "From 'ResumeDraft.pdf', p. 2"
    val icon: Int,                          // drawable res id
    val personaId: String,
    val payload: Map<String, String> = emptyMap() // memoryId/sourceId/etc.
)
