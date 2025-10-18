package com.example.innovexia.ui.persona.memory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.memory.Mind.api.MemoryEngine
import com.example.innovexia.memory.Mind.api.MemoryKind
import com.example.innovexia.memory.Mind.di.MindModule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Memory Tab UI
 */
class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val memoryEngine: MemoryEngine = MindModule.provideMemoryEngine(application)

    private val _personaId = MutableStateFlow<String?>(null)
    private val _selectedCategory = MutableStateFlow(MemoryCategory.All)
    private val _searchQuery = MutableStateFlow("")

    /**
     * Get current user ID from Firebase Auth
     */
    private fun getCurrentUserId(): String {
        return FirebaseAuthManager.currentUser()?.uid
            ?: com.example.innovexia.core.auth.ProfileId.GUEST_OWNER_ID
    }

    /**
     * Set current persona
     */
    fun setPersona(personaId: String) {
        _personaId.value = personaId
    }

    /**
     * Set selected category
     */
    fun setCategory(category: MemoryCategory) {
        _selectedCategory.value = category
    }

    /**
     * Set search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Observe category counts
     */
    fun observeCounts(personaId: String): Flow<List<CategorySummary>> {
        val userId = getCurrentUserId()
        return memoryEngine.observeCounts(personaId, userId).map { counts ->
            counts.map { count ->
                CategorySummary(
                    category = count.kind.toUiCategory(),
                    count = count.count
                )
            }
        }
    }

    /**
     * Observe memory feed
     */
    fun observeFeed(
        personaId: String,
        category: MemoryCategory,
        searchQuery: String
    ): Flow<List<MemoryItem>> {
        val userId = getCurrentUserId()
        val kind = if (category == MemoryCategory.All) null else category.toApiKind()
        val query = searchQuery.ifBlank { null }

        return memoryEngine.feed(personaId, userId, kind, query).map { hits ->
            hits.map { hit ->
                MemoryItem(
                    id = hit.memory.id,
                    category = hit.memory.kind.toUiCategory(),
                    text = hit.memory.text,
                    relativeTime = formatRelativeTime(hit.memory.createdAt),
                    emotion = hit.memory.emotion?.toUiEmotion(),
                    importance = hit.memory.importance.toImportanceLevel(),
                    chatTitle = hit.fromChatTitle,
                    timestamp = hit.memory.createdAt
                )
            }
        }
    }

    /**
     * Delete a memory
     */
    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            memoryEngine.delete(memoryId)
        }
    }

    /**
     * Toggle memory enabled for persona
     */
    fun setMemoryEnabled(personaId: String, enabled: Boolean) {
        viewModelScope.launch {
            memoryEngine.enable(personaId, enabled)
        }
    }

    /**
     * Check if memory is enabled
     */
    suspend fun isMemoryEnabled(personaId: String): Boolean {
        return memoryEngine.isEnabled(personaId)
    }

    /**
     * Convert API MemoryKind to UI MemoryCategory
     */
    private fun MemoryKind.toUiCategory(): MemoryCategory = when (this) {
        MemoryKind.FACT -> MemoryCategory.Facts
        MemoryKind.EVENT -> MemoryCategory.Events
        MemoryKind.PREFERENCE -> MemoryCategory.Preferences
        MemoryKind.EMOTION -> MemoryCategory.Emotions
        MemoryKind.PROJECT -> MemoryCategory.Projects
        MemoryKind.KNOWLEDGE -> MemoryCategory.Knowledge
        MemoryKind.OTHER -> MemoryCategory.All
    }

    /**
     * Convert UI MemoryCategory to API MemoryKind
     */
    private fun MemoryCategory.toApiKind(): MemoryKind = when (this) {
        MemoryCategory.Facts -> MemoryKind.FACT
        MemoryCategory.Events -> MemoryKind.EVENT
        MemoryCategory.Preferences -> MemoryKind.PREFERENCE
        MemoryCategory.Emotions -> MemoryKind.EMOTION
        MemoryCategory.Projects -> MemoryKind.PROJECT
        MemoryCategory.Knowledge -> MemoryKind.KNOWLEDGE
        MemoryCategory.All -> MemoryKind.OTHER // shouldn't be used
    }

    /**
     * Convert API EmotionType to UI EmotionType
     */
    private fun com.example.innovexia.memory.Mind.api.EmotionType.toUiEmotion(): EmotionType = when (this) {
        com.example.innovexia.memory.Mind.api.EmotionType.HAPPY -> EmotionType.Positive
        com.example.innovexia.memory.Mind.api.EmotionType.SAD -> EmotionType.Negative
        com.example.innovexia.memory.Mind.api.EmotionType.EXCITED -> EmotionType.Excited
        com.example.innovexia.memory.Mind.api.EmotionType.CURIOUS -> EmotionType.Curious
        com.example.innovexia.memory.Mind.api.EmotionType.FRUSTRATED -> EmotionType.Negative
        com.example.innovexia.memory.Mind.api.EmotionType.NEUTRAL -> EmotionType.Neutral
        com.example.innovexia.memory.Mind.api.EmotionType.ANXIOUS -> EmotionType.Negative
        com.example.innovexia.memory.Mind.api.EmotionType.CONFIDENT -> EmotionType.Positive
    }

    /**
     * Convert importance to UI level
     */
    private fun Double.toImportanceLevel(): ImportanceLevel = when {
        this >= 0.7 -> ImportanceLevel.High
        this >= 0.4 -> ImportanceLevel.Medium
        else -> ImportanceLevel.Low
    }

    /**
     * Format timestamp to absolute time with date always visible
     * - Today: "Dec 18, 8:04 AM"
     * - This year: "Dec 15, 8:04 AM"
     * - Older: "Jan 15, 2024, 8:04 AM"
     */
    private fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val year = 365L * 24 * 60 * 60 * 1000L

        val dateFormat = when {
            diff < year -> {
                // This year: Show date + time (including today)
                java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
            }
            else -> {
                // Older than a year: Show full date with year
                java.text.SimpleDateFormat("MMM d, yyyy, h:mm a", java.util.Locale.getDefault())
            }
        }

        return dateFormat.format(java.util.Date(timestamp))
    }
}
