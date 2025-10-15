package com.example.innovexia.core.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Simple markdown and code formatting utilities for message bubbles.
 */
object Formatting {

    /**
     * Parse basic markdown:
     * - **bold**
     * - `inline code`
     * - [link](url)
     * - # Heading
     */
    fun parseMarkdown(text: String, linkColor: Color = Color.Blue): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            val length = text.length

            while (currentIndex < length) {
                when {
                    // Bold: **text**
                    text.startsWith("**", currentIndex) -> {
                        val endIndex = text.indexOf("**", currentIndex + 2)
                        if (endIndex != -1) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(text.substring(currentIndex + 2, endIndex))
                            }
                            currentIndex = endIndex + 2
                        } else {
                            append(text[currentIndex])
                            currentIndex++
                        }
                    }

                    // Inline code: `text`
                    text.startsWith("`", currentIndex) && !text.startsWith("```", currentIndex) -> {
                        val endIndex = text.indexOf("`", currentIndex + 1)
                        if (endIndex != -1) {
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = Color.Gray.copy(alpha = 0.2f)
                                )
                            ) {
                                append(text.substring(currentIndex + 1, endIndex))
                            }
                            currentIndex = endIndex + 1
                        } else {
                            append(text[currentIndex])
                            currentIndex++
                        }
                    }

                    // Link: [text](url)
                    text.startsWith("[", currentIndex) -> {
                        val textEndIndex = text.indexOf("](", currentIndex)
                        val urlEndIndex = text.indexOf(")", textEndIndex + 2)
                        if (textEndIndex != -1 && urlEndIndex != -1) {
                            val linkText = text.substring(currentIndex + 1, textEndIndex)
                            val url = text.substring(textEndIndex + 2, urlEndIndex)
                            pushStringAnnotation(tag = "URL", annotation = url)
                            withStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(linkText)
                            }
                            pop()
                            currentIndex = urlEndIndex + 1
                        } else {
                            append(text[currentIndex])
                            currentIndex++
                        }
                    }

                    // Heading: # text (at start of line)
                    text.startsWith("# ", currentIndex) && (currentIndex == 0 || text[currentIndex - 1] == '\n') -> {
                        val endIndex = text.indexOf('\n', currentIndex).let { if (it == -1) length else it }
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                            append(text.substring(currentIndex + 2, endIndex))
                        }
                        currentIndex = endIndex
                    }

                    else -> {
                        append(text[currentIndex])
                        currentIndex++
                    }
                }
            }
        }
    }

    /**
     * Extract code blocks from text.
     * Returns list of (language, code) pairs and the text with code blocks removed.
     */
    fun extractCodeBlocks(text: String): Pair<String, List<CodeBlock>> {
        val codeBlocks = mutableListOf<CodeBlock>()
        val pattern = Regex("```(\\w*)\\n([\\s\\S]*?)```")
        var result = text

        pattern.findAll(text).forEach { match ->
            val language = match.groupValues[1].ifEmpty { "text" }
            val code = match.groupValues[2]
            codeBlocks.add(CodeBlock(language, code))
            result = result.replaceFirst(match.value, "[CODE_BLOCK_${codeBlocks.size - 1}]")
        }

        return Pair(result, codeBlocks)
    }

    data class CodeBlock(
        val language: String,
        val code: String
    )
}
