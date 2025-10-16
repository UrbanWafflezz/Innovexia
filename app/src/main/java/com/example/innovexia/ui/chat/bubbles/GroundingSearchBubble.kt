package com.example.innovexia.ui.chat.bubbles

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.data.ai.GroundingMetadata
import com.example.innovexia.data.ai.GroundingStatus
import com.example.innovexia.data.local.entities.MessageEntity
import com.example.innovexia.ui.theme.InnovexiaColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Grounding Search Bubble - Dedicated response bubble for web-grounded responses
 *
 * Features:
 * - Blue accent theme matching WebSearchComposer
 * - Shows search process and queries
 * - Displays sources prominently
 * - Distinct visual identity from regular responses
 */
@Composable
fun GroundingSearchBubble(
    message: MessageEntity,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    groundingMetadata: GroundingMetadata? = null,
    groundingStatus: GroundingStatus = GroundingStatus.NONE,
    modelName: String = "Gemini",
    onRegenerate: (String) -> Unit = {},
    onCopy: (String) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    // Blue accent theme
    val accentColor = Color(0xFF4A90E2)
    val bubbleColor = if (isDark) Color(0xFF1A2332) else Color(0xFFF0F7FF)
    val borderColor = if (isDark) accentColor.copy(alpha = 0.3f) else accentColor.copy(alpha = 0.2f)
    val textPrimary = if (isDark) Color(0xFFECEFF4) else Color(0xFF1C1C1E)
    val textSecondary = if (isDark) Color(0xFFB7C0CC) else Color(0xFF86868B)
    val dividerColor = if (isDark) Color(0xFF2A3B4D).copy(alpha = 0.4f) else Color(0xFFD1E3F5)

    // Check if sending (streaming but no text yet)
    val isSending = isStreaming && message.text.isBlank()

    Surface(
        color = bubbleColor,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 150,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
    ) {
        Box {
            Column {
                // Message content with padding
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = 100,
                                easing = androidx.compose.animation.core.LinearOutSlowInEasing
                            )
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header: Web Search indicator
                    GroundingHeader(
                        status = groundingStatus,
                        accentColor = accentColor,
                        textSecondary = textSecondary,
                        isStreaming = isStreaming,
                        isSending = isSending
                    )

                    // Show search queries if available
                    if (groundingMetadata?.webSearchQueries?.isNotEmpty() == true && !isSending) {
                        SearchQueriesSection(
                            queries = groundingMetadata.webSearchQueries,
                            accentColor = accentColor,
                            textSecondary = textSecondary
                        )
                    }

                    // Content: Show sending indicator or actual content
                    if (isSending) {
                        SendingIndicator(textSecondary = textSecondary)
                    } else if (isStreaming && message.text.isBlank()) {
                        GroundingBubbleSkeleton(textSecondary)
                    } else if (message.text.isNotEmpty()) {
                        // Simple text rendering (markdown parsing happens in ResponseBubbleV2)
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 22.sp,
                                fontSize = 14.sp,
                                color = textPrimary,
                                letterSpacing = 0.2.sp
                            )
                        )

                        // Streaming indicator
                        if (isStreaming) {
                            StreamingIndicator(textSecondary, accentColor)
                        }
                    }
                }

                // Footer with sources, timestamp, and actions
                if (!isSending && !isStreaming) {
                    GroundingFooter(
                        message = message,
                        modelName = modelName,
                        groundingMetadata = groundingMetadata,
                        textSecondary = textSecondary,
                        dividerColor = dividerColor,
                        accentColor = accentColor,
                        onCopy = onCopy,
                        onRegenerate = onRegenerate
                    )
                }
            }

            // Sources count badge at top-right (when complete)
            if (groundingMetadata != null && !isSending && !isStreaming) {
                val sources = extractSources(groundingMetadata)
                if (sources.isNotEmpty()) {
                    SourcesCountBadge(
                        count = sources.size,
                        sources = sources,
                        accentColor = accentColor,
                        textSecondary = textSecondary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Header section showing grounding status
 */
@Composable
private fun GroundingHeader(
    status: GroundingStatus,
    accentColor: Color,
    textSecondary: Color,
    isStreaming: Boolean,
    isSending: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Public,
            contentDescription = "Web search",
            tint = accentColor,
            modifier = Modifier.size(16.dp)
        )

        val statusText = when {
            isSending -> "Preparing search"
            status == GroundingStatus.SEARCHING -> "Searching the web"
            status == GroundingStatus.SUCCESS -> "Web search results"
            status == GroundingStatus.FAILED -> "Web search unavailable"
            else -> "Web search"
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accentColor
            )
        )

        // Animated dots during search
        if (status == GroundingStatus.SEARCHING || isSending) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { index ->
                    val alpha by animateFloatAsState(
                        targetValue = if ((System.currentTimeMillis() / 400 % 3).toInt() == index) 1f else 0.3f,
                        animationSpec = tween(400),
                        label = "header_dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(
                                color = accentColor.copy(alpha = alpha),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Search queries section
 */
@Composable
private fun SearchQueriesSection(
    queries: List<String>,
    accentColor: Color,
    textSecondary: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Search queries",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Search ${if (queries.size == 1) "query" else "queries"}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                )
            }

            queries.forEach { query ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(accentColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                    )
                    Text(
                        text = query,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = textSecondary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Footer with sources and actions
 */
@Composable
private fun GroundingFooter(
    message: MessageEntity,
    modelName: String,
    groundingMetadata: GroundingMetadata?,
    textSecondary: Color,
    dividerColor: Color,
    accentColor: Color,
    onCopy: (String) -> Unit,
    onRegenerate: (String) -> Unit
) {
    val context = LocalContext.current

    // Format timestamp
    val timeStr = remember(message.createdAt) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.createdAt))
    }

    Column {
        // Divider
        HorizontalDivider(color = dividerColor, thickness = 1.dp)

        // Sources list (if available)
        if (groundingMetadata != null) {
            val sources = extractSources(groundingMetadata)
            if (sources.isNotEmpty()) {
                SourcesList(
                    sources = sources,
                    accentColor = accentColor,
                    textSecondary = textSecondary
                )
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
            }
        }

        // Footer info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Model name
            Text(
                text = modelName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary
                )
            )

            // Right: Timestamp
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = textSecondary.copy(alpha = 0.7f)
                )
            )
        }
    }
}

/**
 * Sources list in footer
 */
@Composable
private fun SourcesList(
    sources: List<SourceItem>,
    accentColor: Color,
    textSecondary: Color
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = "Sources",
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Sources (${sources.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
            )
        }

        // Source items (show max 5)
        sources.take(5).forEach { source ->
            Surface(
                onClick = {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                        intent.data = android.net.Uri.parse(source.url)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle error silently
                    }
                },
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.05f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = source.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = source.domain,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = textSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // "More sources" indicator
        if (sources.size > 5) {
            Text(
                text = "+ ${sources.size - 5} more ${if (sources.size - 5 == 1) "source" else "sources"}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic,
                    color = textSecondary
                ),
                modifier = Modifier.padding(start = 26.dp)
            )
        }
    }
}

/**
 * Sources count badge at top-right with dropdown
 */
@Composable
private fun SourcesCountBadge(
    count: Int,
    sources: List<SourceItem>,
    accentColor: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = accentColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
            modifier = Modifier.clickable { showMenu = !showMenu }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = "Sources",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                )
            }
        }

        // Dropdown menu
        androidx.compose.material3.DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Public,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Sources",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp)

            // Source items
            sources.take(8).forEach { source ->
                androidx.compose.material3.DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = source.title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = source.domain,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = textSecondary
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse(source.url)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Handle error silently
                        }
                        showMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = accentColor
                        )
                    },
                    modifier = Modifier.height(56.dp)
                )
            }

            if (sources.size > 8) {
                HorizontalDivider(thickness = 1.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+ ${sources.size - 8} more ${if (sources.size - 8 == 1) "source" else "sources"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontStyle = FontStyle.Italic,
                            color = textSecondary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Sending indicator
 */
@Composable
private fun SendingIndicator(textSecondary: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        repeat(3) { index ->
            val alpha by animateFloatAsState(
                targetValue = if ((System.currentTimeMillis() / 400 % 3).toInt() == index) 1f else 0.3f,
                animationSpec = tween(400),
                label = "sending_dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = textSecondary.copy(alpha = alpha),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        Text(
            text = "Sending",
            fontSize = 13.sp,
            color = textSecondary,
            fontStyle = FontStyle.Italic
        )
    }
}

/**
 * Streaming indicator with accent color
 */
@Composable
private fun StreamingIndicator(textSecondary: Color, accentColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by animateFloatAsState(
                targetValue = if ((System.currentTimeMillis() / 400 % 3).toInt() == index) 1f else 0.3f,
                animationSpec = tween(400),
                label = "dot_$index"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = accentColor.copy(alpha = alpha),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

/**
 * Skeleton loading state
 */
@Composable
private fun GroundingBubbleSkeleton(textSecondary: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (it == 2) 0.6f else 1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(textSecondary.copy(alpha = 0.1f))
            )
        }
    }
}

// Helper data class for sources
private data class SourceItem(
    val title: String,
    val url: String,
    val domain: String
)

// Helper function to extract sources
private fun extractSources(metadata: GroundingMetadata): List<SourceItem> {
    return if (metadata.groundingChunks.isNotEmpty()) {
        metadata.groundingChunks.map { chunk ->
            SourceItem(
                title = chunk.web.title,
                url = chunk.web.uri,
                domain = try {
                    android.net.Uri.parse(chunk.web.uri).host ?: chunk.web.uri
                } catch (e: Exception) {
                    chunk.web.uri
                }
            )
        }
    } else {
        metadata.searchResultUrls.map { url ->
            val domain = try {
                android.net.Uri.parse(url).host ?: url
            } catch (e: Exception) {
                url
            }
            SourceItem(
                title = domain,
                url = url,
                domain = domain
            )
        }
    }
}
