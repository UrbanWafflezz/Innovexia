package com.example.innovexia.ui.chat.bubbles

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.data.local.entities.MsgStatus
import com.example.innovexia.data.models.AttachmentMeta
import java.text.SimpleDateFormat
import java.util.*

/**
 * Design tokens for User Bubble V2
 * Premium dark-mode and light-mode adaptive styling
 */
object UserBubbleTokens {
    @Composable
    fun bg(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) Color(0xFF1C2B3B) else Color(0xFFE3F2FD) // Light blue for light mode
    }

    @Composable
    fun border(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) Color(0xFF2F4155) else Color(0xFFBBDEFB)
    }

    @Composable
    fun textPri(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) Color(0xFFE8F1FF) else Color(0xFF1C1C1E)
    }

    @Composable
    fun textSec(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) Color(0xFFB6C6DA) else Color(0xFF757575)
    }

    @Composable
    fun codeBg(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFF90CAF9).copy(alpha = 0.3f)
    }

    @Composable
    fun codeBorder(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF64B5F6)
    }

    @Composable
    fun link(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) Color(0xFF7FB4FF) else Color(0xFF1976D2)
    }

    // Dimensions
    val Radius = 16.dp
    val PadH = 14.dp
    val PadV = 10.dp
}

/**
 * User Bubble V2
 * Right-aligned bubble for user messages with:
 * - Header (time, status, actions)
 * - Markdown-lite body
 * - Footer (edited badge, quote reply anchor)
 * - Support for attachments, copy, edit, retry, quote
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserBubbleV2(
    msg: MessageEntity,
    modifier: Modifier = Modifier,
    onCopy: (MessageEntity) -> Unit,
    onRetry: (MessageEntity) -> Unit,
    onQuote: (MessageEntity) -> Unit,
    onDelete: (MessageEntity) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    var showContextMenu by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Copy feedback
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }

    // Dynamic sizing based on message length - MUCH more compact
    val messageLength = msg.text.length
    val dynamicMinWidth = when {
        messageLength < 5 -> 60.dp    // "hi" → super compact
        messageLength < 15 -> 80.dp   // short phrases
        messageLength < 40 -> 120.dp
        else -> 160.dp
    }
    val dynamicMaxWidth = (screenWidth * 0.75f).coerceAtMost(480.dp)  // Reduced from 80% to 75%

    // More compact padding
    val verticalPadding = when {
        messageLength < 10 -> 6.dp
        messageLength < 30 -> 8.dp
        else -> 10.dp
    }
    val horizontalPadding = when {
        messageLength < 10 -> 10.dp
        else -> UserBubbleTokens.PadH
    }

    // Right-aligned row wrapper
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = UserBubbleTokens.bg(),
            shape = RoundedCornerShape(UserBubbleTokens.Radius),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, UserBubbleTokens.border()),
            modifier = Modifier
                .widthIn(min = dynamicMinWidth, max = dynamicMaxWidth)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showContextMenu = true
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                )
            ) {
                UserHeaderRow(msg, copied, onCopyClick = {
                    clipboardManager.setText(AnnotatedString(msg.text))
                    copied = true
                    onCopy(msg)
                }, onRetry)
                Spacer(Modifier.height(4.dp))
                UserBody(msg)
            }
        }
    }

    // Edited badge below bubble (right-aligned)
    if (msg.editedAt != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                "edited",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = UserBubbleTokens.textSec(),
                    fontSize = 10.sp
                ),
                modifier = Modifier.padding(top = 2.dp, end = 4.dp)
            )
        }
    }

    // Context menu
    if (showContextMenu) {
        ContextMenu(
            msg = msg,
            onDismiss = { showContextMenu = false },
            onCopy = { onCopy(msg); showContextMenu = false },
            onQuote = { onQuote(msg); showContextMenu = false },
            onRetry = { onRetry(msg); showContextMenu = false },
            onDelete = { onDelete(msg); showContextMenu = false }
        )
    }
}

/**
 * Header row: timestamp, status, action icons
 */
@Composable
private fun UserHeaderRow(
    msg: MessageEntity,
    copied: Boolean,
    onCopyClick: () -> Unit,
    onRetry: (MessageEntity) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = remember(msg.createdAt) { timeAgo(msg.createdAt) },
                style = MaterialTheme.typography.labelSmall.copy(
                    color = UserBubbleTokens.textSec(),
                    fontSize = 11.sp
                )
            )
            when (msg.getStatusEnum()) {
                MsgStatus.SENDING -> DotPill("sending")
                MsgStatus.SENT -> Unit
                MsgStatus.FAILED -> DotPill("failed")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onCopyClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    contentDescription = "Copy message",
                    tint = if (copied) Color(0xFF4CAF50) else UserBubbleTokens.textSec(),
                    modifier = Modifier.size(16.dp)
                )
            }
            if (msg.getStatusEnum() == MsgStatus.FAILED) {
                IconButton(
                    onClick = { onRetry(msg) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Retry send",
                        tint = UserBubbleTokens.textSec(),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Status pill for sending/failed
 */
@Composable
private fun DotPill(text: String) {
    Surface(
        color = UserBubbleTokens.codeBg(),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, UserBubbleTokens.codeBorder())
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(color = UserBubbleTokens.textSec())
        )
    }
}

/**
 * Markdown-lite body with attachments
 */
@Composable
private fun UserBody(msg: MessageEntity) {
    val blocks = remember(msg.id, msg.text) {
        MarkdownLite.parse(msg.text)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdLite.Paragraph -> TextBlock(block.text)
                is MdLite.InlineCode -> InlineCodeBlock(block.code)
                is MdLite.CodeBlock -> CodeBlock(block)
                is MdLite.ListBlock -> ListBlock(block)
                is MdLite.Link -> LinkBlock(block)
            }
        }

        val attachments = msg.attachments()
        if (attachments.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            AttachmentGrid(attachments)
        }
    }
}

/**
 * Simple text paragraph
 */
@Composable
private fun TextBlock(text: String) {
    SelectionContainer {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = UserBubbleTokens.textPri(),
                lineHeight = 20.sp
            )
        )
    }
}

/**
 * Inline code snippet
 */
@Composable
private fun InlineCodeBlock(code: String) {
    Text(
        code,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = UserBubbleTokens.textPri(),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(UserBubbleTokens.codeBg())
            .border(1.dp, UserBubbleTokens.codeBorder(), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * Code block with language and copy button
 */
@Composable
private fun CodeBlock(block: MdLite.CodeBlock) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(UserBubbleTokens.codeBg())
            .border(1.dp, UserBubbleTokens.codeBorder(), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = block.lang?.uppercase() ?: "CODE",
                style = MaterialTheme.typography.labelSmall.copy(color = UserBubbleTokens.textSec())
            )
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(block.code))
                    copied = true
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                    contentDescription = "Copy code",
                    tint = UserBubbleTokens.textSec(),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        SelectionContainer {
            Text(
                block.code,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
                fontSize = 13.sp,
                color = UserBubbleTokens.textPri(),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
    }
}

/**
 * List block (bullets/numbered)
 */
@Composable
private fun ListBlock(block: MdLite.ListBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        block.items.forEachIndexed { index, item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (block.ordered) "${index + 1}." else "•",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = UserBubbleTokens.textPri().copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = UserBubbleTokens.textPri(),
                        lineHeight = 20.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Link block
 */
@Composable
private fun LinkBlock(block: MdLite.Link) {
    Text(
        text = block.text,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = UserBubbleTokens.link(),
            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
        )
    )
}

/**
 * Attachment grid
 */
@Composable
private fun AttachmentGrid(attachments: List<AttachmentMeta>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        attachments.forEach { attachment ->
            Surface(
                color = UserBubbleTokens.codeBg(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, UserBubbleTokens.codeBorder())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.AttachFile,
                        contentDescription = null,
                        tint = UserBubbleTokens.textSec(),
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            attachment.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = UserBubbleTokens.textPri(),
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            attachment.mime,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = UserBubbleTokens.textSec()
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Context menu for long-press
 */
@Composable
private fun ContextMenu(
    msg: MessageEntity,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onQuote: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Message Options") },
        text = {
            Column {
                TextButton(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
                    Text("Copy")
                }
                TextButton(onClick = onQuote, modifier = Modifier.fillMaxWidth()) {
                    Text("Quote Reply")
                }
                if (msg.getStatusEnum() == MsgStatus.FAILED) {
                    TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
                    }
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Simple time ago formatter
 */
private fun timeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Simplified markdown parser for user messages
 * Supports: paragraphs, lists, inline code, fenced code, links
 */
object MarkdownLite {
    fun parse(text: String): List<MdLite> {
        val blocks = mutableListOf<MdLite>()
        val lines = text.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            when {
                // Code block
                line.trim().startsWith("```") -> {
                    val lang = line.trim().removePrefix("```").trim().ifEmpty { null }
                    i++
                    val codeLines = mutableListOf<String>()
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    if (codeLines.isNotEmpty()) {
                        blocks.add(MdLite.CodeBlock(codeLines.joinToString("\n"), lang))
                    }
                    i++
                }

                // Unordered list
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    val items = mutableListOf<String>()
                    while (i < lines.size && (lines[i].trim().startsWith("- ") || lines[i].trim().startsWith("* "))) {
                        items.add(lines[i].trim().drop(2))
                        i++
                    }
                    blocks.add(MdLite.ListBlock(items, ordered = false))
                }

                // Ordered list
                line.trim().matches(Regex("^\\d+\\.\\s.*")) -> {
                    val items = mutableListOf<String>()
                    while (i < lines.size && lines[i].trim().matches(Regex("^\\d+\\.\\s.*"))) {
                        items.add(lines[i].trim().substringAfter(". "))
                        i++
                    }
                    blocks.add(MdLite.ListBlock(items, ordered = true))
                }

                // Inline code (simple detection)
                line.contains("`") && !line.trim().startsWith("```") -> {
                    val codeMatch = Regex("`([^`]+)`").find(line)
                    if (codeMatch != null) {
                        blocks.add(MdLite.InlineCode(codeMatch.groupValues[1]))
                    } else {
                        blocks.add(MdLite.Paragraph(line))
                    }
                    i++
                }

                // Paragraph
                line.isNotBlank() -> {
                    blocks.add(MdLite.Paragraph(line))
                    i++
                }

                else -> i++
            }
        }

        return blocks
    }
}

/**
 * Markdown lite block types
 */
sealed class MdLite {
    data class Paragraph(val text: String) : MdLite()
    data class InlineCode(val code: String) : MdLite()
    data class CodeBlock(val code: String, val lang: String?) : MdLite()
    data class ListBlock(val items: List<String>, val ordered: Boolean) : MdLite()
    data class Link(val text: String, val url: String) : MdLite()
}
