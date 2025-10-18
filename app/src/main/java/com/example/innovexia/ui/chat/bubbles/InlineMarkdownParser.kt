package com.example.innovexia.ui.chat.bubbles

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Optimized inline markdown parser using compiled regex patterns
 * Significantly faster than character-by-character parsing
 */
private val INLINE_PATTERNS = listOf(
    // Escape sequences: \* \_ \` \[ etc.
    "\\\\([*_`\\[\\]()\\\\~])" to { match: MatchResult, builder: AnnotatedString.Builder, _: Color ->
        builder.append(match.groupValues[1])
    },
    // Bold + Italic: ***text***
    "\\*\\*\\*(.+?)\\*\\*\\*" to { match: MatchResult, builder: AnnotatedString.Builder, _: Color ->
        builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
            append(match.groupValues[1])
        }
    },
    // Bold: **text**
    "\\*\\*(.+?)\\*\\*" to { match: MatchResult, builder: AnnotatedString.Builder, _: Color ->
        builder.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(match.groupValues[1])
        }
    },
    // Strikethrough: ~~text~~
    "~~(.+?)~~" to { match: MatchResult, builder: AnnotatedString.Builder, _: Color ->
        builder.withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) {
            append(match.groupValues[1])
        }
    },
    // Italic: *text* (but not **)
    "(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)" to { match: MatchResult, builder: AnnotatedString.Builder, _: Color ->
        builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(match.groupValues[1])
        }
    },
    // Italic: _text_ (but not at word boundaries for snake_case)
    "(?<!\\w)_(.+?)_(?!\\w)" to { match: MatchResult, builder: AnnotatedString.Builder, _: Color ->
        builder.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(match.groupValues[1])
        }
    },
    // Inline code: `text` (but not ```)
    "(?<!`)(`(?!`)(.+?)`)(?!`)" to { match: MatchResult, builder: AnnotatedString.Builder, baseColor: Color ->
        builder.append(" ")
        builder.withStyle(
            SpanStyle(
                fontFamily = FontFamily.Monospace,
                background = baseColor.copy(alpha = 0.15f),
                fontSize = 13.sp,
                color = InnovexiaColors.Info,
                fontWeight = FontWeight.Medium
            )
        ) {
            append(" ${match.groupValues[2]} ")
        }
        builder.append(" ")
    },
    // Link: [text](url)
    "\\[(.+?)\\]\\((.+?)\\)" to { match: MatchResult, builder: AnnotatedString.Builder, _: Color ->
        val linkText = match.groupValues[1]
        val url = match.groupValues[2]
        builder.pushStringAnnotation(tag = "URL", annotation = url)
        builder.withStyle(
            SpanStyle(
                color = InnovexiaColors.BlueAccent,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                fontWeight = FontWeight.Medium
            )
        ) {
            append(linkText)
        }
        builder.pop()
    }
)

/**
 * Parse inline markdown (bold, italic, strikethrough, code, links)
 * Optimized version using regex patterns for better performance
 */
fun parseInlineMarkdown(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var currentText = text
        var lastIndex = 0
        val matches = mutableListOf<Triple<Int, Int, Pair<String, (MatchResult, AnnotatedString.Builder, Color) -> Unit>>>()

        // Find all matches from all patterns
        for ((pattern, handler) in INLINE_PATTERNS) {
            val regex = Regex(pattern)
            regex.findAll(currentText).forEach { match ->
                matches.add(Triple(match.range.first, match.range.last, pattern to handler))
            }
        }

        // Sort matches by start position
        matches.sortBy { it.first }

        // Process matches, avoiding overlaps
        val processedRanges = mutableListOf<IntRange>()

        for ((start, end, patternHandler) in matches) {
            val range = start..end

            // Check if this range overlaps with any processed range
            if (processedRanges.any { it.intersect(range).isNotEmpty() }) {
                continue
            }

            // Add plain text before this match
            if (start > lastIndex) {
                append(currentText.substring(lastIndex, start))
            }

            // Apply the pattern handler
            val (pattern, handler) = patternHandler
            val regex = Regex(pattern)
            val match = regex.find(currentText, start)
            if (match != null && match.range.first == start) {
                handler(match, this, baseColor)
                processedRanges.add(range)
                lastIndex = end + 1
            }
        }

        // Add remaining plain text
        if (lastIndex < currentText.length) {
            append(currentText.substring(lastIndex))
        }
    }
}
