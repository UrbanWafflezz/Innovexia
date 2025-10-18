package com.example.innovexia.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.data.ai.GroundingMetadata

/**
 * Shared utilities for handling grounding sources across different bubble types
 */

/**
 * Data class representing a source item for UI display
 */
data class SourceItem(
    val title: String,
    val url: String,
    val domain: String
)

/**
 * Extract sources from grounding metadata
 */
fun extractSources(metadata: GroundingMetadata): List<SourceItem> {
    android.util.Log.d("SourceUtils", "🔍 Extracting sources from metadata:")
    android.util.Log.d("SourceUtils", "  - groundingChunks.size: ${metadata.groundingChunks.size}")
    android.util.Log.d("SourceUtils", "  - searchResultUrls.size: ${metadata.searchResultUrls.size}")

    // Always prefer groundingChunks as they have both title and URI
    val sources = if (metadata.groundingChunks.isNotEmpty()) {
        android.util.Log.d("SourceUtils", "  ✓ Using groundingChunks")
        metadata.groundingChunks.map { chunk ->
            SourceItem(
                title = chunk.web.title,
                url = chunk.web.uri,
                domain = try {
                    Uri.parse(chunk.web.uri).host ?: chunk.web.uri
                } catch (e: Exception) {
                    chunk.web.uri
                }
            )
        }
    } else if (metadata.searchResultUrls.isNotEmpty()) {
        android.util.Log.d("SourceUtils", "  ✓ Using searchResultUrls")
        metadata.searchResultUrls.map { url ->
            val domain = try {
                Uri.parse(url).host ?: url
            } catch (e: Exception) {
                url
            }
            SourceItem(
                title = domain,
                url = url,
                domain = domain
            )
        }
    } else {
        android.util.Log.w("SourceUtils", "  ⚠️ NO sources found in metadata!")
        emptyList()
    }

    android.util.Log.d("SourceUtils", "  → Extracted ${sources.size} sources")
    return sources
}

/**
 * Open a URL in the in-app browser
 * @param onOpenWebView Callback to show the in-app WebView browser
 */
fun openUrlInApp(url: String, onOpenWebView: (String) -> Unit) {
    try {
        onOpenWebView(url)
    } catch (e: Exception) {
        android.util.Log.e("SourceUtils", "Failed to open URL in-app: $url", e)
    }
}

/**
 * Open a URL in external browser (fallback/legacy)
 */
fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error silently - could add toast here if desired
        android.util.Log.e("SourceUtils", "Failed to open URL: $url", e)
    }
}

/**
 * Sources count badge with dropdown menu - reusable component
 */
@Composable
fun SourcesCountBadge(
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
        DropdownMenu(
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

            // Source items (show max 8)
            sources.take(8).forEach { source ->
                DropdownMenuItem(
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
                        openUrl(context, source.url)
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

            // Show remaining sources count
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
 * Sources list for footer display - reusable component
 */
@Composable
fun SourcesList(
    sources: List<SourceItem>,
    accentColor: Color,
    textSecondary: Color,
    modifier: Modifier = Modifier,
    maxDisplay: Int = 5
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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

        // Source items
        sources.take(maxDisplay).forEach { source ->
            Surface(
                onClick = { openUrl(context, source.url) },
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
        if (sources.size > maxDisplay) {
            Text(
                text = "+ ${sources.size - maxDisplay} more ${if (sources.size - maxDisplay == 1) "source" else "sources"}",
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
