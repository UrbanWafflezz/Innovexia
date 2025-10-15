package com.example.innovexia.ui.persona

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// Persona Tab Navigation
// ─────────────────────────────────────────────────────────────────────────────
enum class PersonaTab {
    My, Public, Sources, Memory
}

// ─────────────────────────────────────────────────────────────────────────────
// Persona Core Model (UI)
// ─────────────────────────────────────────────────────────────────────────────
data class Persona(
    val id: String,
    val name: String,
    val initial: String,
    val color: Long,
    val summary: String,
    val tags: List<String> = emptyList(),
    val starred: Boolean = false,
    val updatedAt: String = "2d",
    val isDefault: Boolean = false,
    val createdAtFormatted: String = "",
    val lastUsedFormatted: String? = null
)

/**
 * Convert core Persona to UI Persona
 */
fun com.example.innovexia.core.persona.Persona.toUiPersona(): Persona {
    return Persona(
        id = id,
        name = name,
        initial = initial,
        color = color,
        summary = summary,
        tags = tags,
        starred = false, // UI-only state
        updatedAt = updatedAt?.toRelativeTime() ?: "New",
        isDefault = isDefault,
        createdAtFormatted = "", // Will be populated from entity
        lastUsedFormatted = null // Will be populated from entity
    )
}

/**
 * Convert PersonaEntity to UI Persona (includes timestamps from entity)
 */
fun com.example.innovexia.data.local.entities.PersonaEntity.toUiPersona(): Persona {
    return Persona(
        id = id,
        name = name,
        initial = initial,
        color = color,
        summary = summary,
        tags = tags,
        starred = isDefault, // Use isDefault as starred for now
        updatedAt = updatedAt.toRelativeTime(),
        isDefault = isDefault,
        createdAtFormatted = createdAt.toFormattedDate(),
        lastUsedFormatted = lastUsedAt?.toRelativeTime()
    )
}

/**
 * Convert Timestamp to relative time string
 */
private fun Timestamp.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val time = seconds * 1000
    val diff = now - time

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 604800_000 -> "${diff / 86400_000}d"
        diff < 2592000_000 -> "${diff / 604800_000}w"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(time))
    }
}

/**
 * Convert Long timestamp millis to relative time string
 */
private fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        diff < 2592000_000 -> "${diff / 604800_000}w ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(this))
    }
}

/**
 * Convert Long timestamp millis to formatted date string
 */
private fun Long.toFormattedDate(): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(this))
}

// ─────────────────────────────────────────────────────────────────────────────
// Sources
// ─────────────────────────────────────────────────────────────────────────────
enum class SourceKind { Url, File }

data class SourceItem(
    val id: String,
    val kind: SourceKind,
    val label: String,
    val detail: String
)

data class SourcesState(
    val items: List<SourceItem> = emptyList(),
    val pendingUrl: String = ""
)

// ─────────────────────────────────────────────────────────────────────────────
// Memory
// ─────────────────────────────────────────────────────────────────────────────
enum class MemoryScope { Global, Chat, Persona }

data class MemoryItem(
    val id: String,
    val scope: MemoryScope,
    val text: String,
    val createdAt: String,
    val pinned: Boolean = false
)

data class MemoryState(
    val items: List<MemoryItem> = emptyList(),
    val filter: MemoryScope? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Overall Sheet State
// ─────────────────────────────────────────────────────────────────────────────
data class PersonaUiState(
    val activeTab: PersonaTab = PersonaTab.My,
    val query: String = "",
    val selectedPersonaId: String? = null,
    val my: List<Persona> = emptyList(),
    val publicList: List<Persona> = emptyList(),
    val sources: SourcesState = SourcesState(),
    val memory: MemoryState = MemoryState()
)

// ─────────────────────────────────────────────────────────────────────────────
// Demo Seeders
// ─────────────────────────────────────────────────────────────────────────────
fun demoMyPersonas() = listOf(
    Persona(
        id = "1",
        name = "Code Assistant",
        initial = "C",
        color = 0xFF60A5FA,
        summary = "Expert in Kotlin, Jetpack Compose, and Android development. Provides clean, idiomatic code.",
        tags = listOf("Coding", "Android"),
        starred = true,
        updatedAt = "1h"
    ),
    Persona(
        id = "2",
        name = "Research Analyst",
        initial = "R",
        color = 0xFF34D399,
        summary = "Deep researcher with systematic approach to complex topics and data synthesis.",
        tags = listOf("Research", "Analysis"),
        starred = false,
        updatedAt = "2d"
    ),
    Persona(
        id = "3",
        name = "Creative Writer",
        initial = "W",
        color = 0xFFF472B6,
        summary = "Crafts engaging narratives, blog posts, and marketing copy with unique voice.",
        tags = listOf("Writing", "Creative"),
        starred = true,
        updatedAt = "5d"
    ),
    Persona(
        id = "4",
        name = "Math Tutor",
        initial = "M",
        color = 0xFFFBBF24,
        summary = "Patient educator specializing in calculus, linear algebra, and problem-solving.",
        tags = listOf("Tutor", "Math"),
        starred = false,
        updatedAt = "1w"
    ),
    Persona(
        id = "5",
        name = "UI/UX Designer",
        initial = "U",
        color = 0xFFA78BFA,
        summary = "Focus on accessibility, design systems, and user-centered design patterns.",
        tags = listOf("Design", "UX"),
        starred = false,
        updatedAt = "3d"
    ),
    Persona(
        id = "6",
        name = "DevOps Engineer",
        initial = "D",
        color = 0xFFEF4444,
        summary = "Cloud infrastructure, CI/CD pipelines, Kubernetes, and automation specialist.",
        tags = listOf("DevOps", "Cloud"),
        starred = false,
        updatedAt = "4d"
    )
)

fun demoPublicPersonas() = listOf(
    Persona(
        id = "p1",
        name = "Socratic Tutor",
        initial = "S",
        color = 0xFF06B6D4,
        summary = "Guides learning through thoughtful questions instead of direct answers.",
        tags = listOf("Tutor", "Education"),
        starred = false,
        updatedAt = "Popular"
    ),
    Persona(
        id = "p2",
        name = "Code Reviewer",
        initial = "C",
        color = 0xFF8B5CF6,
        summary = "Provides constructive feedback on code quality, patterns, and best practices.",
        tags = listOf("Coding", "Review"),
        starred = false,
        updatedAt = "Trending"
    ),
    Persona(
        id = "p3",
        name = "Technical Writer",
        initial = "T",
        color = 0xFF10B981,
        summary = "Creates clear documentation, API guides, and technical tutorials.",
        tags = listOf("Writing", "Documentation"),
        starred = false,
        updatedAt = "Popular"
    ),
    Persona(
        id = "p4",
        name = "Data Scientist",
        initial = "D",
        color = 0xFFF59E0B,
        summary = "Statistical analysis, machine learning, and data visualization expert.",
        tags = listOf("Research", "Data"),
        starred = false,
        updatedAt = "New"
    ),
    Persona(
        id = "p5",
        name = "Business Strategist",
        initial = "B",
        color = 0xFFEC4899,
        summary = "Market analysis, competitive strategy, and growth planning specialist.",
        tags = listOf("Business", "Strategy"),
        starred = false,
        updatedAt = "Popular"
    ),
    Persona(
        id = "p6",
        name = "Language Coach",
        initial = "L",
        color = 0xFF6366F1,
        summary = "Helps improve writing style, grammar, and communication clarity.",
        tags = listOf("Writing", "Language"),
        starred = false,
        updatedAt = "Trending"
    ),
    Persona(
        id = "p7",
        name = "Product Manager",
        initial = "P",
        color = 0xFF14B8A6,
        summary = "Feature prioritization, user stories, and roadmap planning expertise.",
        tags = listOf("Product", "Strategy"),
        starred = false,
        updatedAt = "New"
    ),
    Persona(
        id = "p8",
        name = "Debate Partner",
        initial = "D",
        color = 0xFFF97316,
        summary = "Challenges ideas constructively and explores multiple perspectives.",
        tags = listOf("Critical Thinking"),
        starred = false,
        updatedAt = "Popular"
    )
)

fun demoSources() = listOf(
    SourceItem(
        id = "s1",
        kind = SourceKind.Url,
        label = "Compose Material 3 Guidelines",
        detail = "m3.material.io"
    ),
    SourceItem(
        id = "s2",
        kind = SourceKind.File,
        label = "Design System Tokens",
        detail = "tokens.json"
    ),
    SourceItem(
        id = "s3",
        kind = SourceKind.Url,
        label = "Kotlin Coding Conventions",
        detail = "kotlinlang.org"
    ),
    SourceItem(
        id = "s4",
        kind = SourceKind.File,
        label = "API Specification",
        detail = "api-spec.yaml"
    ),
    SourceItem(
        id = "s5",
        kind = SourceKind.Url,
        label = "Android Accessibility Guide",
        detail = "developer.android.com"
    )
)

fun demoMemories() = listOf(
    MemoryItem(
        id = "m1",
        scope = MemoryScope.Persona,
        text = "User prefers functional programming patterns and immutable data structures in Kotlin.",
        createdAt = "Mar 3",
        pinned = true
    ),
    MemoryItem(
        id = "m2",
        scope = MemoryScope.Global,
        text = "Project uses Jetpack Compose with Material 3 design system. Target Android API 26+.",
        createdAt = "Mar 2",
        pinned = true
    ),
    MemoryItem(
        id = "m3",
        scope = MemoryScope.Chat,
        text = "Working on implementing a bottom sheet persona selector with tabbed interface.",
        createdAt = "10:12a",
        pinned = false
    ),
    MemoryItem(
        id = "m4",
        scope = MemoryScope.Persona,
        text = "User values accessibility and asks for semantic content descriptions on all interactive elements.",
        createdAt = "Mar 1",
        pinned = false
    ),
    MemoryItem(
        id = "m5",
        scope = MemoryScope.Global,
        text = "Spacing grid: 20dp outer padding, 16dp section gaps, 12dp inner card padding.",
        createdAt = "Feb 28",
        pinned = false
    ),
    MemoryItem(
        id = "m6",
        scope = MemoryScope.Chat,
        text = "Discussed implementing glass-morphism effects for premium feel on interactive surfaces.",
        createdAt = "Feb 27",
        pinned = false
    ),
    MemoryItem(
        id = "m7",
        scope = MemoryScope.Persona,
        text = "Code should include inline documentation for complex logic and public APIs.",
        createdAt = "Feb 25",
        pinned = false
    ),
    MemoryItem(
        id = "m8",
        scope = MemoryScope.Global,
        text = "Dark theme primary: #0F172A, surface: #141A22, border: #253041, text: #E5EAF0.",
        createdAt = "Feb 24",
        pinned = true
    ),
    MemoryItem(
        id = "m9",
        scope = MemoryScope.Chat,
        text = "User requested bottom sheet height up to 90% screen with content-based sizing.",
        createdAt = "9:45a",
        pinned = false
    ),
    MemoryItem(
        id = "m10",
        scope = MemoryScope.Persona,
        text = "Prefer LazyColumn/Grid with proper content padding for IME and navigation bars.",
        createdAt = "Feb 23",
        pinned = false
    ),
    MemoryItem(
        id = "m11",
        scope = MemoryScope.Global,
        text = "All touch targets must be minimum 44dp for accessibility compliance.",
        createdAt = "Feb 22",
        pinned = false
    ),
    MemoryItem(
        id = "m12",
        scope = MemoryScope.Chat,
        text = "Animations should use 180ms duration with easing for premium feel.",
        createdAt = "Feb 20",
        pinned = false
    )
)
