package com.example.innovexia.ui.chat.bubbles

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed interface representing different markdown block types
 * for advanced rendering in ResponseBubbleV2
 */
sealed interface MarkdownBlock {

    /**
     * Regular paragraph text
     */
    data class Paragraph(
        val text: String
    ) : MarkdownBlock

    /**
     * Heading with level (1-6)
     */
    data class Heading(
        val text: String,
        val level: Int
    ) : MarkdownBlock

    /**
     * Unordered or ordered list
     */
    data class List(
        val items: kotlin.collections.List<String>,
        val ordered: Boolean = false,
        val isTaskList: Boolean = false,
        val checkedStates: kotlin.collections.List<Boolean> = emptyList(),
        val nestedLists: kotlin.collections.List<kotlin.collections.List<String>> = emptyList()
    ) : MarkdownBlock

    /**
     * Block quote
     */
    data class Quote(
        val text: String
    ) : MarkdownBlock

    /**
     * Code block with optional language
     */
    data class Code(
        val code: String,
        val language: String? = null
    ) : MarkdownBlock

    /**
     * Table with headers and rows
     */
    data class Table(
        val headers: kotlin.collections.List<String>,
        val rows: kotlin.collections.List<kotlin.collections.List<String>>
    ) : MarkdownBlock

    /**
     * Callout/Alert box with icon
     */
    data class Callout(
        val text: String,
        val type: CalloutType = CalloutType.INFO
    ) : MarkdownBlock {
        enum class CalloutType(val icon: ImageVector, val tintProvider: (Color, Color, Color) -> Color) {
            INFO(Icons.Outlined.Info, { info, _, _ -> info }),
            WARNING(Icons.Outlined.Warning, { _, warning, _ -> warning }),
            TIP(Icons.Outlined.Info, { _, _, success -> success })
        }
    }

    /**
     * Collapsible section
     */
    data class Collapsible(
        val title: String,
        val content: String
    ) : MarkdownBlock

    /**
     * Image with optional alt text
     */
    data class Image(
        val url: String,
        val altText: String? = null
    ) : MarkdownBlock

    /**
     * Horizontal rule / divider
     */
    object Divider : MarkdownBlock
}
