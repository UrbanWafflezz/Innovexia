package com.example.innovexia.ui.persona.memory

/**
 * Memory category types for organizing persona memories
 */
enum class MemoryCategory {
    All,
    Facts,
    Events,
    Preferences,
    Emotions,
    Projects,
    Knowledge;

    val displayName: String
        get() = when (this) {
            All -> "All"
            Facts -> "Facts"
            Events -> "Events"
            Preferences -> "Preferences"
            Emotions -> "Emotions"
            Projects -> "Projects"
            Knowledge -> "Knowledge"
        }

    val emoji: String
        get() = when (this) {
            All -> "\uD83D\uDCDA" // 📚
            Facts -> "\uD83D\uDCD8" // 📘
            Events -> "\uD83D\uDCC5" // 📅
            Preferences -> "⚙\uFE0F" // ⚙️
            Emotions -> "\uD83D\uDCAD" // 💭
            Projects -> "\uD83D\uDCBC" // 💼
            Knowledge -> "\uD83E\uDDE0" // 🧠
        }
}

/**
 * Emotion type for memory items
 */
enum class EmotionType {
    Positive,
    Neutral,
    Negative,
    Excited,
    Curious;

    val displayName: String
        get() = when (this) {
            Positive -> "Positive"
            Neutral -> "Neutral"
            Negative -> "Negative"
            Excited -> "Excited"
            Curious -> "Curious"
        }

    val emoji: String
        get() = when (this) {
            Positive -> "\uD83D\uDE0A" // 😊
            Neutral -> "\uD83D\uDE10" // 😐
            Negative -> "☹\uFE0F" // ☹️
            Excited -> "\uD83E\uDD29" // 🤩
            Curious -> "\uD83E\uDD14" // 🤔
        }
}

/**
 * Importance level for memory items
 */
enum class ImportanceLevel {
    Low,
    Medium,
    High;

    val displayName: String
        get() = when (this) {
            Low -> "Low"
            Medium -> "Medium"
            High -> "High"
        }
}

/**
 * Individual memory item model
 */
data class MemoryItem(
    val id: String,
    val category: MemoryCategory,
    val text: String,
    val relativeTime: String,
    val emotion: EmotionType?,
    val importance: ImportanceLevel,
    val chatTitle: String?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Category summary for overview cards
 */
data class CategorySummary(
    val category: MemoryCategory,
    val count: Int
)

/**
 * Memory UI state
 */
data class MemoryUiState(
    val isMemoryEnabled: Boolean = true,
    val selectedCategory: MemoryCategory = MemoryCategory.All,
    val searchQuery: String = "",
    val memories: List<MemoryItem> = emptyList(),
    val categorySummaries: List<CategorySummary> = emptyList()
)

/**
 * Generate mock memory data for a persona
 */
fun generateMockMemories(): List<MemoryItem> = listOf(
    MemoryItem(
        id = "mem_1",
        category = MemoryCategory.Events,
        text = "User mentioned mowing the lawn and saw a grasshopper.",
        relativeTime = "3 days ago",
        emotion = EmotionType.Positive,
        importance = ImportanceLevel.Medium,
        chatTitle = "Weekend Activities"
    ),
    MemoryItem(
        id = "mem_2",
        category = MemoryCategory.Preferences,
        text = "User prefers dark mode and minimal UI designs with high contrast.",
        relativeTime = "1 week ago",
        emotion = EmotionType.Neutral,
        importance = ImportanceLevel.High,
        chatTitle = "Design Preferences"
    ),
    MemoryItem(
        id = "mem_3",
        category = MemoryCategory.Facts,
        text = "User is working on an Android app using Jetpack Compose and Kotlin.",
        relativeTime = "2 days ago",
        emotion = null,
        importance = ImportanceLevel.High,
        chatTitle = "Development Setup"
    ),
    MemoryItem(
        id = "mem_4",
        category = MemoryCategory.Emotions,
        text = "User expressed excitement about implementing new persona memory feature.",
        relativeTime = "5 hours ago",
        emotion = EmotionType.Excited,
        importance = ImportanceLevel.Medium,
        chatTitle = "Feature Development"
    ),
    MemoryItem(
        id = "mem_5",
        category = MemoryCategory.Projects,
        text = "Currently working on Innovexia app - a multi-persona AI chat application.",
        relativeTime = "1 week ago",
        emotion = null,
        importance = ImportanceLevel.High,
        chatTitle = "Project Overview"
    ),
    MemoryItem(
        id = "mem_6",
        category = MemoryCategory.Knowledge,
        text = "User learned about Material 3 design system and glass morphism effects.",
        relativeTime = "4 days ago",
        emotion = EmotionType.Curious,
        importance = ImportanceLevel.Medium,
        chatTitle = "Design Learning"
    ),
    MemoryItem(
        id = "mem_7",
        category = MemoryCategory.Facts,
        text = "User's typical work hours are 9 AM to 6 PM EST.",
        relativeTime = "2 weeks ago",
        emotion = null,
        importance = ImportanceLevel.Low,
        chatTitle = "Schedule Info"
    ),
    MemoryItem(
        id = "mem_8",
        category = MemoryCategory.Events,
        text = "User attended a tech meetup about Kotlin multiplatform development.",
        relativeTime = "1 week ago",
        emotion = EmotionType.Excited,
        importance = ImportanceLevel.Medium,
        chatTitle = "Events & Meetups"
    ),
    MemoryItem(
        id = "mem_9",
        category = MemoryCategory.Preferences,
        text = "User prefers concise code explanations over verbose documentation.",
        relativeTime = "3 days ago",
        emotion = null,
        importance = ImportanceLevel.Medium,
        chatTitle = "Communication Style"
    ),
    MemoryItem(
        id = "mem_10",
        category = MemoryCategory.Emotions,
        text = "User felt frustrated when dealing with complex state management in Compose.",
        relativeTime = "6 days ago",
        emotion = EmotionType.Negative,
        importance = ImportanceLevel.Low,
        chatTitle = "Development Challenges"
    ),
    MemoryItem(
        id = "mem_11",
        category = MemoryCategory.Projects,
        text = "Planning to add multi-modal AI support with image and voice input.",
        relativeTime = "2 days ago",
        emotion = EmotionType.Excited,
        importance = ImportanceLevel.High,
        chatTitle = "Future Features"
    ),
    MemoryItem(
        id = "mem_12",
        category = MemoryCategory.Knowledge,
        text = "User discovered best practices for accessibility in Android apps.",
        relativeTime = "1 week ago",
        emotion = EmotionType.Curious,
        importance = ImportanceLevel.High,
        chatTitle = "Accessibility Learning"
    ),
    MemoryItem(
        id = "mem_13",
        category = MemoryCategory.Facts,
        text = "User has 5+ years of experience with Android development.",
        relativeTime = "3 weeks ago",
        emotion = null,
        importance = ImportanceLevel.Medium,
        chatTitle = "Professional Background"
    ),
    MemoryItem(
        id = "mem_14",
        category = MemoryCategory.Events,
        text = "User completed a successful app deployment to Google Play Store.",
        relativeTime = "10 days ago",
        emotion = EmotionType.Excited,
        importance = ImportanceLevel.High,
        chatTitle = "Milestones"
    ),
    MemoryItem(
        id = "mem_15",
        category = MemoryCategory.Preferences,
        text = "User likes to test features on real devices rather than emulators.",
        relativeTime = "1 week ago",
        emotion = null,
        importance = ImportanceLevel.Low,
        chatTitle = "Testing Preferences"
    ),
    MemoryItem(
        id = "mem_16",
        category = MemoryCategory.Emotions,
        text = "User was happy with the improved app performance after optimization.",
        relativeTime = "5 days ago",
        emotion = EmotionType.Positive,
        importance = ImportanceLevel.Medium,
        chatTitle = "Performance Work"
    ),
    MemoryItem(
        id = "mem_17",
        category = MemoryCategory.Knowledge,
        text = "User learned about coroutines and flow for asynchronous programming.",
        relativeTime = "2 weeks ago",
        emotion = EmotionType.Curious,
        importance = ImportanceLevel.High,
        chatTitle = "Kotlin Learning"
    )
)

/**
 * Generate category summaries from memories
 */
fun generateCategorySummaries(memories: List<MemoryItem>): List<CategorySummary> {
    val categories = MemoryCategory.values().filter { it != MemoryCategory.All }
    return categories.map { category ->
        CategorySummary(
            category = category,
            count = memories.count { it.category == category }
        )
    }.filter { it.count > 0 }
}
