package com.example.innovexia.ui.persona.sources

/**
 * Type of source content
 */
enum class SourceType {
    URL,
    FILE,
    IMAGE
}

/**
 * Processing status of a source
 */
enum class SourceStatus {
    NOT_INDEXED,
    INDEXING,
    READY,
    ERROR
}

/**
 * UI model for a single source item
 */
data class SourceItemUi(
    val id: String,
    val type: SourceType,
    val title: String,
    val subtitle: String? = null,           // domain or file name
    val uri: String,                         // http(s) or content://
    val sizeBytes: Long? = null,            // files/images only
    val pageCount: Int? = null,             // pdf hint
    val thumbnail: Any? = null,             // coil model or placeholder
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val status: SourceStatus = SourceStatus.NOT_INDEXED,
    val lastUpdated: Long = System.currentTimeMillis(),
    val errorMsg: String? = null
)

/**
 * UI state for the Sources tab
 */
data class SourcesUiState(
    val enabled: Boolean = true,                 // persona-level switch
    val filter: SourceType? = null,              // null = All
    val query: String = "",
    val sort: String = "Recent",                 // Recent | A→Z | Size | Type
    val selecting: Boolean = false,              // batch mode
    val selectedIds: Set<String> = emptySet(),
    val items: List<SourceItemUi> = emptyList(),
    val storageUsedBytes: Long = 0L
)

/**
 * Mock data for UI development
 */
val mockSources = listOf(
    SourceItemUi(
        id = "1",
        type = SourceType.URL,
        title = "Android Developer Documentation",
        subtitle = "developer.android.com",
        uri = "https://developer.android.com",
        pinned = true,
        status = SourceStatus.READY,
        tags = listOf("docs", "android")
    ),
    SourceItemUi(
        id = "2",
        type = SourceType.URL,
        title = "Jetpack Compose Guidelines",
        subtitle = "developer.android.com",
        uri = "https://developer.android.com/jetpack/compose",
        pinned = true,
        status = SourceStatus.READY,
        tags = listOf("compose", "ui")
    ),
    SourceItemUi(
        id = "3",
        type = SourceType.FILE,
        title = "Project Requirements",
        subtitle = "requirements.pdf",
        uri = "content://documents/12345",
        sizeBytes = 2_485_760,
        pageCount = 24,
        status = SourceStatus.READY,
        tags = listOf("specs")
    ),
    SourceItemUi(
        id = "4",
        type = SourceType.FILE,
        title = "Architecture Diagram",
        subtitle = "architecture.pdf",
        uri = "content://documents/67890",
        sizeBytes = 1_024_000,
        pageCount = 8,
        status = SourceStatus.INDEXING,
        tags = listOf("design")
    ),
    SourceItemUi(
        id = "5",
        type = SourceType.IMAGE,
        title = "UI Mockup - Home Screen",
        subtitle = "home_mockup.png",
        uri = "content://media/images/11111",
        sizeBytes = 524_288,
        status = SourceStatus.READY,
        tags = listOf("design", "ui")
    ),
    SourceItemUi(
        id = "6",
        type = SourceType.IMAGE,
        title = "User Flow Diagram",
        subtitle = "user_flow.jpg",
        uri = "content://media/images/22222",
        sizeBytes = 768_000,
        status = SourceStatus.NOT_INDEXED,
        tags = listOf("design")
    ),
    SourceItemUi(
        id = "7",
        type = SourceType.URL,
        title = "Material Design 3",
        subtitle = "m3.material.io",
        uri = "https://m3.material.io",
        status = SourceStatus.READY,
        tags = listOf("design", "material")
    ),
    SourceItemUi(
        id = "8",
        type = SourceType.FILE,
        title = "API Documentation",
        subtitle = "api_docs.pdf",
        uri = "content://documents/33333",
        sizeBytes = 3_145_728,
        pageCount = 45,
        status = SourceStatus.ERROR,
        errorMsg = "Failed to parse PDF: Unsupported encryption",
        tags = listOf("api", "docs")
    ),
    SourceItemUi(
        id = "9",
        type = SourceType.URL,
        title = "Kotlin Documentation",
        subtitle = "kotlinlang.org",
        uri = "https://kotlinlang.org/docs",
        status = SourceStatus.READY,
        tags = listOf("kotlin", "docs")
    ),
    SourceItemUi(
        id = "10",
        type = SourceType.IMAGE,
        title = "Color Palette Reference",
        subtitle = "colors.png",
        uri = "content://media/images/44444",
        sizeBytes = 156_672,
        status = SourceStatus.READY,
        tags = listOf("design", "colors")
    ),
    SourceItemUi(
        id = "11",
        type = SourceType.FILE,
        title = "Meeting Notes Q1",
        subtitle = "notes_q1.pdf",
        uri = "content://documents/55555",
        sizeBytes = 987_654,
        pageCount = 12,
        status = SourceStatus.READY,
        tags = listOf("notes")
    ),
    SourceItemUi(
        id = "12",
        type = SourceType.URL,
        title = "Firebase Documentation",
        subtitle = "firebase.google.com",
        uri = "https://firebase.google.com/docs",
        status = SourceStatus.INDEXING,
        tags = listOf("firebase", "backend")
    ),
    SourceItemUi(
        id = "13",
        type = SourceType.IMAGE,
        title = "Logo Assets",
        subtitle = "logo_variants.png",
        uri = "content://media/images/66666",
        sizeBytes = 412_000,
        status = SourceStatus.READY,
        tags = listOf("branding")
    ),
    SourceItemUi(
        id = "14",
        type = SourceType.FILE,
        title = "User Research Report",
        subtitle = "research_2024.pdf",
        uri = "content://documents/77777",
        sizeBytes = 5_242_880,
        pageCount = 67,
        status = SourceStatus.NOT_INDEXED,
        tags = listOf("research", "ux")
    ),
    SourceItemUi(
        id = "15",
        type = SourceType.URL,
        title = "Figma Design System",
        subtitle = "figma.com",
        uri = "https://www.figma.com/design-systems",
        status = SourceStatus.READY,
        tags = listOf("design", "figma")
    )
)
