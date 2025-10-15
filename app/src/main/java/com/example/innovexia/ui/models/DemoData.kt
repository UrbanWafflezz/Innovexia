package com.example.innovexia.ui.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlinx.parcelize.Parcelize

/**
 * Represents a recent chat item in the side menu
 */
@Parcelize
@Stable
data class RecentChat(
    val id: String,
    val title: String,
    val emoji: String?,
    val timestamp: String,
    val unread: Boolean = false,
    val pinned: Boolean = false,
    val isIncognito: Boolean = false
) : Parcelable

/**
 * Represents a persona that can be selected
 */
@Parcelize
@Stable
data class Persona(
    val id: String,
    val name: String,
    val initial: String,
    val colorHex: Long // Store as Long for Parcelable compatibility
) : Parcelable {
    val color: Color
        get() = Color(colorHex)

    /**
     * Convert UI Persona to core Persona for backend operations
     */
    fun toCorePersona(): com.example.innovexia.core.persona.Persona {
        return com.example.innovexia.core.persona.Persona(
            id = id,
            name = name,
            initial = initial,
            color = colorHex,
            summary = "",
            tags = emptyList(),
            system = null,
            isDefault = false,
            updatedAt = null,
            extendedSettings = null
        )
    }
}

/**
 * Demo recent chats for UI previews and initial state
 */
fun demoRecentChats(): List<RecentChat> = listOf(
    RecentChat(
        id = "1",
        title = "Morning standup notes",
        emoji = "☀️",
        timestamp = "2m ago",
        unread = true
    ),
    RecentChat(
        id = "2",
        title = "Q4 Planning Discussion",
        emoji = "📊",
        timestamp = "1h ago",
        unread = false
    ),
    RecentChat(
        id = "3",
        title = "Code review feedback",
        emoji = "💻",
        timestamp = "3h ago",
        unread = true
    ),
    RecentChat(
        id = "4",
        title = "Design system updates",
        emoji = "🎨",
        timestamp = "Yesterday",
        unread = false
    ),
    RecentChat(
        id = "5",
        title = "API integration help",
        emoji = "🔌",
        timestamp = "Yesterday",
        unread = false
    ),
    RecentChat(
        id = "6",
        title = "Team building ideas",
        emoji = "🎉",
        timestamp = "2 days ago",
        unread = false
    ),
    RecentChat(
        id = "7",
        title = "Bug triage session",
        emoji = "🐛",
        timestamp = "3 days ago",
        unread = false
    ),
    RecentChat(
        id = "8",
        title = "Performance optimization",
        emoji = "⚡",
        timestamp = "1 week ago",
        unread = false
    )
)

/**
 * Demo personas for UI previews and initial state
 */
fun demoPersonas(): List<Persona> = listOf(
    Persona(
        id = "nova",
        name = "Nova",
        initial = "N",
        colorHex = 0xFF3B82F6 // Blue
    ),
    Persona(
        id = "atlas",
        name = "Atlas",
        initial = "A",
        colorHex = 0xFF8B5CF6 // Purple
    ),
    Persona(
        id = "muse",
        name = "Muse",
        initial = "M",
        colorHex = 0xFFEC4899 // Pink
    ),
    Persona(
        id = "orion",
        name = "Orion",
        initial = "O",
        colorHex = 0xFF10B981 // Green
    ),
    Persona(
        id = "echo",
        name = "Echo",
        initial = "E",
        colorHex = 0xFFF59E0B // Amber
    )
)
