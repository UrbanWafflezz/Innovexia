package com.example.innovexia.ui.chat.bubbles

/**
 * Markdown parser that converts raw markdown text into structured blocks
 * for advanced rendering in ResponseBubbleV2
 */
object MarkdownParser {

    /**
     * Parse markdown text into a list of MarkdownBlock objects
     */
    fun parse(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            when {
                // Code block with better language detection
                line.trim().startsWith("```") -> {
                    val languageInfo = line.trim().removePrefix("```").trim()
                    // Extract just the language name (first word), normalize common variants
                    val language = if (languageInfo.isNotEmpty()) {
                        val lang = languageInfo.split("\\s+".toRegex()).firstOrNull()?.lowercase()
                        // Normalize common language names Gemini uses
                        when (lang) {
                            "py", "python3" -> "python"
                            "js", "javascript" -> "javascript"
                            "ts" -> "typescript"
                            "kt" -> "kotlin"
                            "java" -> "java"
                            "cpp", "c++" -> "c++"
                            "cs", "csharp" -> "c#"
                            "rb" -> "ruby"
                            "go", "golang" -> "go"
                            "rs" -> "rust"
                            "swift" -> "swift"
                            "php" -> "php"
                            "sh", "bash", "shell" -> "bash"
                            "sql" -> "sql"
                            "html" -> "html"
                            "css" -> "css"
                            "json" -> "json"
                            "xml" -> "xml"
                            "yaml", "yml" -> "yaml"
                            "markdown", "md" -> "markdown"
                            else -> lang
                        }
                    } else null

                    i++
                    val codeLines = mutableListOf<String>()
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    if (codeLines.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Code(codeLines.joinToString("\n"), language))
                    }
                    i++
                }

                // Table (starts with | and contains | separators)
                line.trim().startsWith("|") && line.trim().endsWith("|") -> {
                    val tableLines = mutableListOf(line)
                    i++
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        tableLines.add(lines[i])
                        i++
                    }
                    parseTable(tableLines)?.let { blocks.add(it) }
                }

                // Heading
                line.trim().startsWith("#") -> {
                    val level = line.takeWhile { it == '#' }.length
                    val text = line.drop(level).trim()
                    if (text.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Heading(text, level))
                    }
                    i++
                }

                // Quote
                line.trim().startsWith(">") -> {
                    val quoteLines = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().startsWith(">")) {
                        quoteLines.add(lines[i].trim().removePrefix(">").trim())
                        i++
                    }
                    if (quoteLines.isNotEmpty()) {
                        // Preserve line breaks in quotes by joining with newline
                        blocks.add(MarkdownBlock.Quote(quoteLines.joinToString("\n")))
                    }
                }

                // Callout (special syntax: :::info, :::warning, :::tip)
                line.trim().startsWith(":::") -> {
                    val type = line.trim().removePrefix(":::").trim().lowercase()
                    i++
                    val contentLines = mutableListOf<String>()
                    while (i < lines.size && !lines[i].trim().startsWith(":::")) {
                        contentLines.add(lines[i])
                        i++
                    }
                    if (contentLines.isNotEmpty()) {
                        val calloutType = when (type) {
                            "warning", "warn" -> MarkdownBlock.Callout.CalloutType.WARNING
                            "tip", "success" -> MarkdownBlock.Callout.CalloutType.TIP
                            else -> MarkdownBlock.Callout.CalloutType.INFO
                        }
                        blocks.add(MarkdownBlock.Callout(contentLines.joinToString("\n"), calloutType))
                    }
                    i++
                }

                // Collapsible (special syntax: <details> or +++title)
                line.trim().startsWith("<details>") -> {
                    val title = lines.getOrNull(i + 1)?.trim()
                        ?.removePrefix("<summary>")?.removeSuffix("</summary>")
                        ?: "Details"
                    i += 2
                    val contentLines = mutableListOf<String>()
                    while (i < lines.size && !lines[i].trim().startsWith("</details>")) {
                        contentLines.add(lines[i])
                        i++
                    }
                    if (contentLines.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Collapsible(title, contentLines.joinToString("\n")))
                    }
                    i++
                }

                line.trim().startsWith("+++") -> {
                    val title = line.trim().removePrefix("+++").trim()
                    i++
                    val contentLines = mutableListOf<String>()
                    while (i < lines.size && !lines[i].trim().startsWith("+++")) {
                        contentLines.add(lines[i])
                        i++
                    }
                    if (contentLines.isNotEmpty()) {
                        blocks.add(MarkdownBlock.Collapsible(title, contentLines.joinToString("\n")))
                    }
                    i++
                }

                // Horizontal rule
                line.trim() == "---" || line.trim() == "***" || line.trim() == "___" -> {
                    blocks.add(MarkdownBlock.Divider)
                    i++
                }

                // Unordered list (including task lists)
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    val listItems = mutableListOf<String>()
                    val checkedStates = mutableListOf<Boolean>()
                    var isTaskList = false

                    while (i < lines.size &&
                           (lines[i].trim().startsWith("- ") || lines[i].trim().startsWith("* "))) {
                        val itemText = lines[i].trim().drop(2)

                        // Check if it's a task list item: - [ ] or - [x]
                        when {
                            itemText.startsWith("[ ] ") -> {
                                isTaskList = true
                                checkedStates.add(false)
                                listItems.add(itemText.drop(4))
                            }
                            itemText.startsWith("[x] ") || itemText.startsWith("[X] ") -> {
                                isTaskList = true
                                checkedStates.add(true)
                                listItems.add(itemText.drop(4))
                            }
                            else -> {
                                if (isTaskList) checkedStates.add(false)
                                listItems.add(itemText)
                            }
                        }
                        i++
                    }
                    blocks.add(MarkdownBlock.List(listItems, ordered = false, isTaskList = isTaskList, checkedStates = checkedStates))
                }

                // Ordered list
                line.trim().matches(Regex("^\\d+\\.\\s.*")) -> {
                    val listItems = mutableListOf<String>()
                    val nestedLists = mutableListOf<List<String>>()

                    while (i < lines.size) {
                        val currentLine = lines[i]

                        // Check if it's a numbered item
                        if (currentLine.trim().matches(Regex("^\\d+\\.\\s.*"))) {
                            listItems.add(currentLine.trim().substringAfter(". "))
                            i++

                            // Check for nested bullets
                            val nestedItems = mutableListOf<String>()
                            while (i < lines.size &&
                                   (lines[i].startsWith("   ") || lines[i].startsWith("\t")) &&
                                   (lines[i].trim().startsWith("- ") || lines[i].trim().startsWith("* "))) {
                                nestedItems.add(lines[i].trim().drop(2))
                                i++
                            }
                            nestedLists.add(nestedItems)
                        } else {
                            break
                        }
                    }
                    blocks.add(MarkdownBlock.List(listItems, ordered = true, nestedLists = nestedLists))
                }

                // Image
                line.trim().matches(Regex("^!\\[.*]\\(.*\\)$")) -> {
                    val altText = line.substringAfter("[").substringBefore("]")
                    val url = line.substringAfter("(").substringBefore(")")
                    blocks.add(MarkdownBlock.Image(url, altText.ifEmpty { null }))
                    i++
                }

                // Regular paragraph
                line.isNotBlank() -> {
                    val paragraphLines = mutableListOf(line)
                    i++
                    while (i < lines.size && lines[i].isNotBlank() && !isSpecialLine(lines[i])) {
                        // Preserve line breaks for better formatting
                        paragraphLines.add(lines[i])
                        i++
                    }
                    // Join with space but preserve intentional line breaks
                    blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ").trim()))
                }

                // Empty line
                else -> i++
            }
        }

        return blocks
    }

    /**
     * Parse table from lines
     */
    private fun parseTable(tableLines: List<String>): MarkdownBlock.Table? {
        if (tableLines.size < 2) return null

        val headers = tableLines[0]
            .trim()
            .removeSurrounding("|")
            .split("|")
            .map { it.trim() }

        // Skip separator line (index 1)
        val rows = tableLines.drop(2).map { line ->
            line.trim()
                .removeSurrounding("|")
                .split("|")
                .map { it.trim() }
        }

        return MarkdownBlock.Table(headers, rows)
    }

    /**
     * Check if a line starts a special block
     */
    private fun isSpecialLine(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.startsWith("#") ||
               trimmed.startsWith("```") ||
               trimmed.startsWith(">") ||
               trimmed.startsWith("- ") ||
               trimmed.startsWith("* ") ||
               trimmed.startsWith(":::") ||
               trimmed.startsWith("<details>") ||
               trimmed.startsWith("+++") ||
               trimmed.startsWith("|") ||
               trimmed.matches(Regex("^\\d+\\.\\s.*")) ||
               trimmed.matches(Regex("^!\\[.*]\\(.*\\)$")) ||
               trimmed in listOf("---", "***", "___")
    }
}
