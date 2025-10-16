package com.example.innovexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple markdown renderer for AI responses.
 * Supports: bold, italic, code, code blocks, lists, headers
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    val lines = remember(markdown) { markdown.lines() }
    var i = 0

    Column(modifier = modifier) {
        while (i < lines.size) {
            val line = lines[i]

            when {
                // Code block
                line.trim().startsWith("```") -> {
                    i++
                    val codeLines = mutableListOf<String>()
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    CodeBlock(code = codeLines.joinToString("\n"), color = color)
                    i++
                }

                // Header
                line.startsWith("#") -> {
                    val level = line.takeWhile { it == '#' }.length
                    val text = line.drop(level).trim()
                    Header(text = text, level = level, color = color)
                    i++
                }

                // List item
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    val text = line.trim().drop(2)
                    ListItem(text = text, color = color)
                    i++
                }

                // Numbered list
                line.trim().matches(Regex("^\\d+\\.\\s.*")) -> {
                    val text = line.trim().substringAfter(". ")
                    ListItem(text = text, color = color, numbered = true)
                    i++
                }

                // Regular paragraph
                line.isNotBlank() -> {
                    Paragraph(text = line, color = color)
                    i++
                }

                else -> i++
            }
        }
    }
}

@Composable
private fun Header(text: String, level: Int, color: Color) {
    Text(
        text = parseInlineMarkdown(text),
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = when (level) {
                1 -> 16.sp
                2 -> 15.sp
                3 -> 14.sp
                else -> 14.sp
            },
            fontWeight = FontWeight.Bold,
            lineHeight = when (level) {
                1 -> 22.sp
                2 -> 21.sp
                3 -> 20.sp
                else -> 20.sp
            }
        ),
        color = color,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun Paragraph(text: String, color: Color) {
    Text(
        text = parseInlineMarkdown(text),
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 20.sp,
            fontSize = 14.sp
        ),
        color = color,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ListItem(text: String, color: Color, numbered: Boolean = false) {
    Text(
        text = buildAnnotatedString {
            append(if (numbered) "    • " else "    • ")
            append(parseInlineMarkdown(text))
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 20.sp,
            fontSize = 14.sp
        ),
        color = color,
        modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
    )
}

@Composable
private fun CodeBlock(code: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .horizontalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 19.sp
            ),
            color = color
        )
    }
}

/**
 * Parse inline markdown: **bold**, *italic*, `code`
 */
private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold: **text**
                text.substring(i).startsWith("**") -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }

                // Italic: *text* or _text_
                text.substring(i).startsWith("*") && !text.substring(i).startsWith("**") -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1 && end < text.length - 1 && text[end + 1] != '*') {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }

                text.substring(i).startsWith("_") -> {
                    val end = text.indexOf("_", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }

                // Inline code: `text`
                text.substring(i).startsWith("`") -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color.Gray.copy(alpha = 0.2f)
                            )
                        ) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }

                // Regular text
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
