package com.example.innovexia.ui.chat.newchat.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.R
import com.example.innovexia.core.persona.Persona
import com.example.innovexia.core.persona.PersonaRepository
import com.example.innovexia.data.ai.GeminiService
import com.example.innovexia.memory.Mind.api.MemoryEngine
import com.example.innovexia.memory.Mind.sources.api.SourcesEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for smart suggestions with memory + Gemini integration
 */
@HiltViewModel
class SuggestionsVM @Inject constructor(
    private val memoryEngine: MemoryEngine,
    private val sourcesEngine: SourcesEngine,
    private val personaRepo: PersonaRepository,
    private val geminiService: GeminiService? // Optional
) : ViewModel() {

    private val _ui = MutableStateFlow<List<SuggestionCardUi>>(emptyList())
    val ui: StateFlow<List<SuggestionCardUi>> = _ui

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentOwnerId: String? = null
    private var autoRefreshJob: kotlinx.coroutines.Job? = null

    // Track if suggestions have been loaded in this session
    private var hasLoadedOnce = false

    /**
     * Load suggestions for the given owner ID (legacy)
     */
    fun load(ownerId: String) = viewModelScope.launch(Dispatchers.IO) {
        currentOwnerId = ownerId
        loadSuggestions(ownerId)
        startAutoRefresh(ownerId)
    }

    /**
     * Load suggestions for the given persona directly (preferred)
     * Loads on first time OR when persona changes significantly (different owner/user)
     */
    fun loadWithPersona(persona: Persona?, forceReload: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        val personaChanged = currentPersona?.id != persona?.id

        // Load if: first time, persona changed, or force reload
        if (!hasLoadedOnce || personaChanged || forceReload) {
            if (personaChanged) {
                android.util.Log.d("SuggestionsVM", "🔄 Persona changed - reloading suggestions (old: ${currentPersona?.name}, new: ${persona?.name})")
            } else if (forceReload) {
                android.util.Log.d("SuggestionsVM", "🔄 Force reload - loading suggestions")
            } else {
                android.util.Log.d("SuggestionsVM", "🆕 First load - loading suggestions")
            }

            currentOwnerId = persona?.id // Track by persona ID for refresh
            loadSuggestionsWithPersona(persona)
            hasLoadedOnce = true

            if (persona != null) {
                startAutoRefreshWithPersona(persona)
            }
        } else {
            android.util.Log.d("SuggestionsVM", "⏭️ Skipping load - suggestions already loaded for same persona")
            // Just update the current persona for refresh button
            currentPersona = persona
        }
    }

    /**
     * Manually refresh suggestions
     */
    fun refresh() {
        currentOwnerId?.let { ownerId ->
            viewModelScope.launch(Dispatchers.IO) {
                loadSuggestions(ownerId)
            }
        }
    }

    private var currentPersona: Persona? = null

    /**
     * Update current persona without reloading suggestions
     * Used when persona changes but we want to keep existing suggestions
     */
    fun updateCurrentPersona(persona: Persona?) {
        currentPersona = persona
        android.util.Log.d("SuggestionsVM", "Updated current persona to: ${persona?.name}")
    }

    /**
     * Manually refresh suggestions with persona
     * This is triggered by the refresh button - always reloads
     */
    fun refreshWithPersona() {
        currentPersona?.let { persona ->
            android.util.Log.d("SuggestionsVM", "🔄 Manual refresh triggered")
            viewModelScope.launch(Dispatchers.IO) {
                loadSuggestionsWithPersona(persona)
            }
        } ?: run {
            android.util.Log.w("SuggestionsVM", "⚠️ Cannot refresh - no persona available")
        }
    }

    /**
     * Clear all suggestions and reset to guest state
     * Called when user logs out
     */
    fun clearSuggestions() {
        android.util.Log.d("SuggestionsVM", "🧹 Clearing suggestions (user logged out)")
        currentPersona = null
        currentOwnerId = null
        hasLoadedOnce = false
        autoRefreshJob?.cancel()
        _ui.value = defaultGuestSuggestions()
    }

    /**
     * Start auto-refresh timer (every 5 minutes)
     */
    private fun startAutoRefresh(ownerId: String) {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5 * 60 * 1000L) // 5 minutes
            loadSuggestions(ownerId)
            startAutoRefresh(ownerId) // Restart timer
        }
    }

    /**
     * Start auto-refresh timer with persona (every 5 minutes)
     * DISABLED: We only want manual refresh now
     */
    private fun startAutoRefreshWithPersona(persona: Persona) {
        // Auto-refresh disabled - suggestions only refresh on app launch or manual button press
        android.util.Log.d("SuggestionsVM", "ℹ️ Auto-refresh disabled - use refresh button for updates")
        autoRefreshJob?.cancel()
    }

    /**
     * Internal function to load suggestions
     */
    private suspend fun loadSuggestions(ownerId: String) {
        _isLoading.value = true
        try {
            // Get current persona
            val persona = personaRepo.getDefaultPersona(ownerId)
            if (persona == null) {
                // Guest mode - show default suggestions
                _ui.value = defaultGuestSuggestions()
                return
            }

            // 1) Pull high-signal memories from last 14 days
            val userId = com.example.innovexia.core.auth.ProfileId.current().toOwnerId()
            val memories = memoryEngine.feed(
                personaId = persona.id,
                userId = userId,
                kind = null,
                query = null
            )
                .first()
                .filter { it.score >= 0.25 } // Tune threshold
                .sortedByDescending { it.score }
                .take(30)

            android.util.Log.d("SuggestionsVM", "Memory feed returned ${memories.size} memories for persona ${persona.id}")

            // 2) Pull recently indexed sources (ready only)
            val sources = sourcesEngine.observeSources(persona.id)
                .first()
                .filter { it.status == "READY" }
                .sortedByDescending { it.lastIndexedAt ?: it.addedAt }
                .take(10)

            // 3) Heuristic suggestions from memories + sources
            var suggestions = MemorySuggest.from(memories, sources, persona.id)
            android.util.Log.d("SuggestionsVM", "Generated ${suggestions.size} suggestions from memories and sources")

            // 4) Optionally ask Gemini to re-title/cluster (only if we have < 6 or want nicer phrasings)
            suggestions = if (geminiService != null && geminiService.isApiKeyConfigured() && suggestions.size < 6) {
                try {
                    GeminiReranker.retitleAndFill(geminiService, suggestions, memories, sources, persona)
                } catch (e: Exception) {
                    android.util.Log.w("SuggestionsVM", "Gemini retitle failed, using heuristics: ${e.message}")
                    suggestions
                }
            } else {
                suggestions
            }

            // 5) Cap and publish
            _ui.value = suggestions.distinctBy { it.title }.take(6)

        } catch (e: Exception) {
            android.util.Log.e("SuggestionsVM", "Failed to load suggestions: ${e.message}", e)
            // Fallback to guest suggestions on error
            _ui.value = defaultGuestSuggestions()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Load suggestions with provided persona directly (bypasses default persona lookup)
     */
    private suspend fun loadSuggestionsWithPersona(persona: Persona?) {
        currentPersona = persona
        _isLoading.value = true

        try {
            // Check if user is in guest mode - guests should NEVER see memory-based suggestions
            val currentProfileId = com.example.innovexia.core.auth.ProfileId.current()
            if (currentProfileId is com.example.innovexia.core.auth.ProfileId.Guest) {
                android.util.Log.d("SuggestionsVM", "�� Guest mode detected - showing default suggestions only (no memory/personas)")
                _ui.value = defaultGuestSuggestions()
                return
            }

            // If persona is null, try to get the default persona for the current user
            val effectivePersona = persona ?: run {
                android.util.Log.w("SuggestionsVM", "⚠️ No persona provided - attempting to load default persona")
                val ownerId = com.example.innovexia.core.auth.ProfileId.current().toOwnerId()
                personaRepo.getDefaultPersona(ownerId) ?: run {
                    android.util.Log.w("SuggestionsVM", "⚠️ No default persona found - showing guest suggestions")
                    _ui.value = defaultGuestSuggestions()
                    return
                }
            }

            android.util.Log.d("SuggestionsVM", "📋 Loading suggestions for persona: ${effectivePersona.name} (${effectivePersona.id})")

            // 1) Pull high-signal memories from last 14 days
            android.util.Log.d("SuggestionsVM", "🔍 Fetching memories for persona ${effectivePersona.id}...")
            val userId = com.example.innovexia.core.auth.ProfileId.current().toOwnerId()
            val memories = memoryEngine.feed(
                personaId = effectivePersona.id,
                userId = userId,
                kind = null,
                query = null
            )
                .first()
                .filter { it.score >= 0.25 } // Tune threshold
                .sortedByDescending { it.score }
                .take(30)

            android.util.Log.d("SuggestionsVM", "💾 Memory feed returned ${memories.size} memories for persona ${effectivePersona.id}")
            if (memories.isNotEmpty()) {
                android.util.Log.d("SuggestionsVM", "📝 Sample memories:")
                memories.take(3).forEach { memory ->
                    android.util.Log.d("SuggestionsVM", "  - [${memory.memory.kind}] ${memory.memory.text.take(60)}... (score: ${memory.score})")
                }
            } else {
                android.util.Log.w("SuggestionsVM", "⚠️ No memories found for this persona!")
            }

            // 2) Pull recently indexed sources (ready only)
            android.util.Log.d("SuggestionsVM", "🔍 Fetching sources for persona ${effectivePersona.id}...")
            val sources = sourcesEngine.observeSources(effectivePersona.id)
                .first()
                .filter { it.status == "READY" }
                .sortedByDescending { it.lastIndexedAt ?: it.addedAt }
                .take(10)

            android.util.Log.d("SuggestionsVM", "📚 Found ${sources.size} ready sources for persona ${effectivePersona.id}")
            if (sources.isNotEmpty()) {
                android.util.Log.d("SuggestionsVM", "📄 Sample sources:")
                sources.take(3).forEach { source ->
                    android.util.Log.d("SuggestionsVM", "  - ${source.displayName} (${source.type})")
                }
            }

            // 3) Check if we have any context to work with
            if (memories.isEmpty() && sources.isEmpty()) {
                android.util.Log.w("SuggestionsVM", "⚠️ No memories or sources available - showing default suggestions")
                _ui.value = defaultGuestSuggestions()
                return
            }

            // 4) Generate smart suggestions using Gemini (preferred) or heuristics (fallback)
            var suggestions: List<SuggestionCardUi>

            val geminiAvailable = geminiService != null && geminiService.isApiKeyConfigured()
            android.util.Log.d("SuggestionsVM", "🤖 Gemini available: $geminiAvailable")

            if (geminiAvailable) {
                // Use Gemini to generate contextual, actionable suggestions
                android.util.Log.d("SuggestionsVM", "🚀 Using Gemini to generate smart suggestions")
                try {
                    suggestions = GeminiReranker.generateSuggestions(geminiService!!, memories, sources, effectivePersona)
                    android.util.Log.d("SuggestionsVM", "✅ Gemini generated ${suggestions.size} suggestions")

                    if (suggestions.isNotEmpty()) {
                        android.util.Log.d("SuggestionsVM", "📋 Sample Gemini suggestions:")
                        suggestions.take(3).forEach { sugg ->
                            android.util.Log.d("SuggestionsVM", "  - ${sugg.title} (${sugg.subtitle})")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SuggestionsVM", "❌ Gemini failed: ${e.message}", e)
                    // Fallback to heuristic suggestions
                    suggestions = MemorySuggest.from(memories, sources, effectivePersona.id)
                    android.util.Log.d("SuggestionsVM", "🔄 Fallback: Generated ${suggestions.size} heuristic suggestions")
                }
            } else {
                // Fallback: Use heuristic suggestions
                android.util.Log.d("SuggestionsVM", "🔄 Gemini not available, using heuristic suggestions")
                suggestions = MemorySuggest.from(memories, sources, effectivePersona.id)
                android.util.Log.d("SuggestionsVM", "📋 Generated ${suggestions.size} heuristic suggestions")
            }

            // 5) If we have fewer than 4 suggestions, add default quick actions
            if (suggestions.size < 4) {
                android.util.Log.d("SuggestionsVM", "➕ Adding default quick actions (current: ${suggestions.size})")
                val quickActions = listOf(
                    SuggestionCardUi(
                        id = "qa-brainstorm",
                        kind = SuggestionKind.QUICK_ACTION,
                        title = "Brainstorm ideas",
                        subtitle = "Creative problem solving",
                        icon = R.drawable.ic_lightbulb,
                        personaId = effectivePersona.id,
                        payload = emptyMap()
                    ),
                    SuggestionCardUi(
                        id = "qa-research",
                        kind = SuggestionKind.QUICK_ACTION,
                        title = "Research a topic",
                        subtitle = "Fast, sourced answers",
                        icon = R.drawable.ic_search,
                        personaId = effectivePersona.id,
                        payload = emptyMap()
                    )
                )
                suggestions = (suggestions + quickActions).take(4)
            }

            // 6) Cap to 4 and publish
            _ui.value = suggestions.distinctBy { it.title }.take(4)
            android.util.Log.d("SuggestionsVM", "✅ Published ${_ui.value.size} final suggestions")

            if (_ui.value.isNotEmpty()) {
                android.util.Log.d("SuggestionsVM", "📋 Final suggestions list:")
                _ui.value.forEach { sugg ->
                    android.util.Log.d("SuggestionsVM", "  - [${sugg.kind}] ${sugg.title}")
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("SuggestionsVM", "Failed to load suggestions with persona: ${e.message}", e)
            // Fallback to guest suggestions on error
            _ui.value = defaultGuestSuggestions()
        } finally {
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }

    /**
     * Default suggestions for guest users
     */
    private fun defaultGuestSuggestions(): List<SuggestionCardUi> {
        return listOf(
            SuggestionCardUi(
                id = "guest1",
                kind = SuggestionKind.QUICK_ACTION,
                title = "Brainstorm ideas",
                subtitle = "Start a new project",
                icon = R.drawable.ic_lightbulb,
                personaId = "",
                payload = emptyMap()
            ),
            SuggestionCardUi(
                id = "guest2",
                kind = SuggestionKind.QUICK_ACTION,
                title = "Research anything",
                subtitle = "Fast, sourced answers",
                icon = R.drawable.ic_search,
                personaId = "",
                payload = emptyMap()
            ),
            SuggestionCardUi(
                id = "guest3",
                kind = SuggestionKind.QUICK_ACTION,
                title = "Summarize a PDF",
                subtitle = "Upload and analyze",
                icon = R.drawable.ic_doc,
                personaId = "",
                payload = emptyMap()
            ),
            SuggestionCardUi(
                id = "guest4",
                kind = SuggestionKind.QUICK_ACTION,
                title = "Draft an email",
                subtitle = "Professional writing help",
                icon = R.drawable.ic_email,
                personaId = "",
                payload = emptyMap()
            )
        )
    }
}
