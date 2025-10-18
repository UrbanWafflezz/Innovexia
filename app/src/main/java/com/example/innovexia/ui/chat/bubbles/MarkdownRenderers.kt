package com.example.innovexia.ui.chat.bubbles

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.utils.openUrl

/**
 * Markdown body with block-based rendering
 */
@Composable
fun MarkdownBody(
    blocks: List<MarkdownBlock>,
    textPrimary: Color,
    textSecondary: Color,
    bubbleBorder: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> TextBlock(block, textPrimary)
                is MarkdownBlock.Heading -> HeadingBlock(block, textPrimary)
                is MarkdownBlock.List -> ListBlock(block, textPrimary)
                is MarkdownBlock.Quote -> QuoteBlock(block, textPrimary, bubbleBorder)
                is MarkdownBlock.Code -> CodeBlockV2(block, textPrimary, textSecondary, bubbleBorder)
                is MarkdownBlock.Table -> TableBlock(block, textPrimary, textSecondary, bubbleBorder)
                is MarkdownBlock.Callout -> CalloutBlock(block, textPrimary)
                is MarkdownBlock.Collapsible -> CollapsibleBlock(block, textPrimary, textSecondary, bubbleBorder)
                is MarkdownBlock.Image -> ImageBlock(block)
                is MarkdownBlock.Divider -> Divider(color = bubbleBorder, thickness = 1.dp)
            }
        }
    }
}

/**
 * Text/Paragraph block
 */
@Composable
private fun TextBlock(block: MarkdownBlock.Paragraph, textPrimary: Color) {
    val context = LocalContext.current
    val annotatedString = parseInlineMarkdown(block.text, textPrimary)

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 22.sp,
            fontSize = 14.sp,
            color = textPrimary,
            letterSpacing = 0.2.sp
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    openUrl(context, annotation.item)
                }
        }
    )
}

/**
 * Heading block
 */
@Composable
private fun HeadingBlock(block: MarkdownBlock.Heading, textPrimary: Color) {
    val context = LocalContext.current
    val (size, topPadding, bottomPadding) = when (block.level) {
        1 -> Triple(20.sp, 8.dp, 4.dp)
        2 -> Triple(18.sp, 6.dp, 3.dp)
        3 -> Triple(16.sp, 4.dp, 2.dp)
        else -> Triple(15.sp, 2.dp, 1.dp)
    }
    val annotatedString = parseInlineMarkdown(block.text, textPrimary)

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = size,
            fontWeight = FontWeight.Bold,
            lineHeight = (size.value * 1.3).sp,
            color = textPrimary
        ),
        modifier = Modifier.padding(top = topPadding, bottom = bottomPadding),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    openUrl(context, annotation.item)
                }
        }
    )
}

/**
 * List block (ordered/unordered)
 */
@Composable
private fun ListBlock(block: MarkdownBlock.List, textPrimary: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        block.items.forEachIndexed { index, item ->
            // Main list item
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Render checkbox for task lists
                    if (block.isTaskList) {
                        val isChecked = block.checkedStates.getOrNull(index) ?: false
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (isChecked) InnovexiaColors.Success else textPrimary.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .background(
                                    if (isChecked) InnovexiaColors.Success.copy(alpha = 0.1f) else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Text(
                                    text = "✓",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InnovexiaColors.Success
                                )
                            }
                        }
                    } else {
                        Text(
                            text = if (block.ordered) "${index + 1}." else "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = textPrimary.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.width(24.dp)
                        )
                    }

                    // List item text with clickable links
                    val context = LocalContext.current
                    val itemAnnotatedString = parseInlineMarkdown(item, textPrimary)
                    ClickableText(
                        text = itemAnnotatedString,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            fontSize = 14.sp,
                            color = textPrimary,
                            textDecoration = if (block.isTaskList && block.checkedStates.getOrNull(index) == true) {
                                androidx.compose.ui.text.style.TextDecoration.LineThrough
                            } else null
                        ),
                        modifier = Modifier.weight(1f),
                        onClick = { offset ->
                            itemAnnotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    openUrl(context, annotation.item)
                                }
                        }
                    )
                }

                // Render nested list items if present
                val nestedItems = block.nestedLists.getOrNull(index) ?: emptyList()
                if (nestedItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 34.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        nestedItems.forEach { nestedItem ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "◦",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = textPrimary.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.width(16.dp)
                                )
                                // Nested item with clickable links
                                val context = LocalContext.current
                                val nestedAnnotatedString = parseInlineMarkdown(nestedItem, textPrimary)
                                ClickableText(
                                    text = nestedAnnotatedString,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        lineHeight = 20.sp,
                                        fontSize = 13.sp,
                                        color = textPrimary
                                    ),
                                    modifier = Modifier.weight(1f),
                                    onClick = { offset ->
                                        nestedAnnotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                openUrl(context, annotation.item)
                                            }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quote block
 */
@Composable
private fun QuoteBlock(block: MarkdownBlock.Quote, textPrimary: Color, borderColor: Color) {
    val context = LocalContext.current
    val annotatedString = parseInlineMarkdown(block.text, textPrimary)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(borderColor.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
            .border(
                width = 3.dp,
                color = borderColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 10.dp)
    ) {
        ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontStyle = FontStyle.Italic,
                color = textPrimary.copy(alpha = 0.85f),
                letterSpacing = 0.15.sp
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        openUrl(context, annotation.item)
                    }
            }
        )
    }
}

/**
 * Code block with toolbar
 */
@Composable
internal fun CodeBlockV2(
    block: MarkdownBlock.Code,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    var showFullscreen by remember { mutableStateOf(false) }
    var showLineNumbers by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        // Toolbar
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = (block.language?.uppercase() ?: "CODE"),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Line numbers toggle
                IconButton(
                    onClick = { showLineNumbers = !showLineNumbers },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FormatListNumbered,
                        contentDescription = if (showLineNumbers) "Hide line numbers" else "Show line numbers",
                        tint = if (showLineNumbers) InnovexiaColors.BlueAccent else textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Copy button
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(block.code))
                        copied = true
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) InnovexiaColors.Success else textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Fullscreen button
                IconButton(
                    onClick = { showFullscreen = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Code content with optional line numbers
        if (showLineNumbers) {
            CodeWithLineNumbers(block.code, textPrimary, textSecondary)
        } else {
            Text(
                text = block.code.trim(),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = textPrimary,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
    }

    // Fullscreen code dialog
    if (showFullscreen) {
        FullscreenCodeDialog(
            code = block.code,
            language = block.language,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onDismiss = { showFullscreen = false }
        )
    }
}

/**
 * Code display with line numbers
 */
@Composable
private fun CodeWithLineNumbers(
    code: String,
    textPrimary: Color,
    textSecondary: Color
) {
    val lines = code.trim().lines()
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier.horizontalScroll(scrollState)
    ) {
        // Line numbers column
        Column(
            modifier = Modifier.padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            lines.forEachIndexed { index, _ ->
                Text(
                    text = "${index + 1}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End
                )
            }
        }

        // Code column
        Column {
            lines.forEach { line ->
                Text(
                    text = line.ifEmpty { " " }, // Preserve empty lines
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = textPrimary
                )
            }
        }
    }
}

/**
 * Fullscreen code dialog with copy functionality
 */
@Composable
private fun FullscreenCodeDialog(
    code: String,
    language: String?,
    textPrimary: Color,
    textSecondary: Color,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF1E2329) else Color(0xFFF5F5F7),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header with language and actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Code,
                            contentDescription = null,
                            tint = InnovexiaColors.BlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = language?.uppercase() ?: "CODE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Copy button
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(code))
                                copied = true
                            }
                        ) {
                            Icon(
                                imageVector = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                contentDescription = "Copy code",
                                tint = if (copied) InnovexiaColors.Success else textSecondary
                            )
                        }

                        // Close button
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = textSecondary
                            )
                        }
                    }
                }

                HorizontalDivider(
                    color = if (isDark) Color(0xFF2A323B) else Color(0xFFE0E0E3)
                )

                // Scrollable code content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = code.trim(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = textPrimary
                    )
                }
            }
        }
    }
}

/**
 * Callout/Alert block
 */
@Composable
private fun CalloutBlock(block: MarkdownBlock.Callout, textPrimary: Color) {
    val context = LocalContext.current
    val tint = block.type.tintProvider(
        InnovexiaColors.Info,
        InnovexiaColors.Warning,
        InnovexiaColors.Success
    )
    val annotatedString = parseInlineMarkdown(block.text, textPrimary)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.1f))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = block.type.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                color = textPrimary
            ),
            modifier = Modifier.weight(1f),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        openUrl(context, annotation.item)
                    }
            }
        )
    }
}

/**
 * Collapsible section
 */
@Composable
private fun CollapsibleBlock(
    block: MarkdownBlock.Collapsible,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "arrow_rotation"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(borderColor.copy(alpha = 0.06f))
            .animateContentSize()
            .clickable { expanded = !expanded }
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = textSecondary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation }
            )
            Text(
                text = block.title,
                color = textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            val context = LocalContext.current
            val contentAnnotatedString = parseInlineMarkdown(block.content, textPrimary)
            ClickableText(
                text = contentAnnotatedString,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                onClick = { offset ->
                    contentAnnotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            openUrl(context, annotation.item)
                        }
                }
            )
        }
    }
}

/**
 * Table block with horizontal scroll
 */
@Composable
private fun TableBlock(
    block: MarkdownBlock.Table,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        // Header row
        Row(
            modifier = Modifier
                .background(borderColor.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            block.headers.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        letterSpacing = 0.3.sp
                    ),
                    modifier = Modifier
                        .width(130.dp)
                        .padding(horizontal = 8.dp)
                )
            }
        }

        HorizontalDivider(color = borderColor.copy(alpha = 0.4f), thickness = 1.dp)

        // Data rows
        block.rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .background(
                        if (rowIndex % 2 == 0) Color.Transparent
                        else borderColor.copy(alpha = 0.04f)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                row.forEach { cell ->
                    Text(
                        text = cell,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = textPrimary
                        ),
                        modifier = Modifier
                            .width(130.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Image block with zoom support and fullscreen dialog
 */
@Composable
private fun ImageBlock(block: MarkdownBlock.Image) {
    val context = LocalContext.current
    var showFullscreen by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
    ) {
        // Show error placeholder manually when load fails
        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BrokenImage,
                        contentDescription = "Failed to load image",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Failed to load image",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (block.altText != null) {
                        Text(
                            text = block.altText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(block.url)
                    .crossfade(true)
                    .listener(
                        onError = { _, _ -> isError = true },
                        onSuccess = { _, _ -> isError = false }
                    )
                    .build(),
                contentDescription = block.altText ?: "Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showFullscreen = true },
                contentScale = ContentScale.Fit
            )
        }
    }

    // Fullscreen image dialog
    if (showFullscreen && !isError) {
        Dialog(onDismissRequest = { showFullscreen = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .clickable { showFullscreen = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(block.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = block.altText ?: "Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Close button at top-right
                IconButton(
                    onClick = { showFullscreen = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Alt text at bottom
                if (block.altText != null) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = block.altText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
