package com.example.innovexia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.innovexia.data.models.AttachmentKind
import com.example.innovexia.data.models.AttachmentMeta
import com.example.innovexia.data.models.AttachmentStatus

/**
 * Design tokens for AttachmentChips
 */
private object AttachmentChipTokens {
    object Color {
        val SurfaceDarkHi = androidx.compose.ui.graphics.Color(0xFF1E2329)
        val SurfaceLightHi = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        val BorderDark = androidx.compose.ui.graphics.Color(0xFF2A323B)
        val BorderLight = androidx.compose.ui.graphics.Color(0xFFE1E5EA)
        val TextPri = androidx.compose.ui.graphics.Color(0xFFECEFF4)
        val TextPriLight = androidx.compose.ui.graphics.Color(0xFF0F141A)
        val TextSec = androidx.compose.ui.graphics.Color(0xFF9AA6B2)
        val TextSecLight = androidx.compose.ui.graphics.Color(0xFF6B7280)
    }
}

/**
 * Horizontal strip of attachment chips
 *
 * @param items List of attachments to display
 * @param onRemove Callback when user removes an attachment
 */
@Composable
fun AttachmentStrip(
    items: List<AttachmentMeta>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        items(items, key = { it.id }) { attachment ->
            AttachmentChip(
                attachment = attachment,
                onRemove = onRemove
            )
        }
    }
}

/**
 * Individual attachment chip with thumbnail, name, size, status, and remove button
 */
@Composable
private fun AttachmentChip(
    attachment: AttachmentMeta,
    onRemove: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val colors = AttachmentChipTokens.Color

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) colors.SurfaceDarkHi else colors.SurfaceLightHi,
        border = BorderStroke(
            1.dp,
            if (isDark) colors.BorderDark.copy(alpha = 0.5f) else colors.BorderLight
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            // Thumbnail or icon
            if (attachment.kind == AttachmentKind.PHOTO) {
                AsyncImage(
                    model = attachment.localUri,
                    contentDescription = attachment.displayName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = if (isDark) colors.TextPri else colors.TextPriLight,
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Name and size
            Column(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .widthIn(max = 140.dp)
            ) {
                Text(
                    text = attachment.displayName,
                    color = if (isDark) colors.TextPri else colors.TextPriLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
                Text(
                    text = formatFileSize(attachment.sizeBytes),
                    color = if (isDark) colors.TextSec else colors.TextSecLight,
                    fontSize = 11.sp
                )
            }

            // Status indicator
            when (attachment.status) {
                AttachmentStatus.PREPPING, AttachmentStatus.UPLOADING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = if (isDark) colors.TextPri else colors.TextPriLight
                    )
                }
                AttachmentStatus.FAILED -> {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = "Failed",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> {}
            }

            Spacer(Modifier.width(4.dp))

            // Remove button
            IconButton(
                onClick = { onRemove(attachment.id) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove",
                    tint = if (isDark) colors.TextSec else colors.TextSecLight,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Format file size in human-readable format
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
