package com.example.innovexia.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens for AttachmentToolbar
 */
private object AttachmentToolbarTokens {
    object Color {
        val SurfaceDarkHi = androidx.compose.ui.graphics.Color(0xFF1E2329)
        val SurfaceLightHi = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
        val BorderDark = androidx.compose.ui.graphics.Color(0xFF2A323B)
        val BorderLight = androidx.compose.ui.graphics.Color(0xFFE1E5EA)
        val TextPri = androidx.compose.ui.graphics.Color(0xFFECEFF4)
        val TextPriLight = androidx.compose.ui.graphics.Color(0xFF0F141A)
        val TextSec = androidx.compose.ui.graphics.Color(0xFF9AA6B2)
        val TextSecLight = androidx.compose.ui.graphics.Color(0xFF6B7280)
        val ItemBg = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f)
        val ItemBgLight = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.04f)
        val ItemBorder = androidx.compose.ui.graphics.Color(0xFF2A323B).copy(alpha = 0.4f)
        val ItemBorderLight = androidx.compose.ui.graphics.Color(0xFFE1E5EA).copy(alpha = 0.6f)
    }
}

/**
 * AttachmentToolbar - Slide-in toolbar with options for Photos, Files, Camera, Scan, and Grounding
 *
 * @param visible Whether the toolbar is visible
 * @param onPickPhotos Callback for picking photos
 * @param onPickFiles Callback for picking files
 * @param onCapture Callback for capturing with camera
 * @param onScanPdf Callback for scanning to PDF (placeholder/camera)
 * @param groundingEnabled Whether grounding mode is active
 * @param onGroundingToggle Callback for toggling grounding mode
 */
@Composable
fun AttachmentToolbar(
    visible: Boolean,
    onPickPhotos: () -> Unit,
    onPickFiles: () -> Unit,
    onCapture: () -> Unit,
    onScanPdf: () -> Unit,
    groundingEnabled: Boolean = false,
    onGroundingToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val colors = AttachmentToolbarTokens.Color

    // Capture the callback to prevent stale closures
    val currentOnGroundingToggle = androidx.compose.runtime.rememberUpdatedState(onGroundingToggle)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) colors.SurfaceDarkHi else colors.SurfaceLightHi)
                .border(
                    1.dp,
                    if (isDark) colors.BorderDark.copy(alpha = 0.6f) else colors.BorderLight,
                    RoundedCornerShape(16.dp)
                )
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarItem(
                icon = Icons.Rounded.PhotoLibrary,
                label = "Photos",
                onClick = onPickPhotos,
                isDark = isDark,
                isActive = false
            )
            ToolbarItem(
                icon = Icons.Rounded.Description,
                label = "Files",
                onClick = onPickFiles,
                isDark = isDark,
                isActive = false
            )
            ToolbarItem(
                icon = Icons.Rounded.Public,
                label = "Grounding",
                onClick = {
                    android.util.Log.d("AttachmentToolbar", "Grounding clicked! Current: $groundingEnabled, New: ${!groundingEnabled}")
                    android.util.Log.d("AttachmentToolbar", "About to call onGroundingToggle...")
                    currentOnGroundingToggle.value(!groundingEnabled)
                    android.util.Log.d("AttachmentToolbar", "onGroundingToggle called successfully")
                },
                isDark = isDark,
                isActive = groundingEnabled
            )
            ToolbarItem(
                icon = Icons.Rounded.PhotoCamera,
                label = "Camera",
                onClick = onCapture,
                isDark = isDark,
                isActive = false
            )
            ToolbarItem(
                icon = Icons.Rounded.PictureAsPdf,
                label = "Scan",
                onClick = onScanPdf,
                isDark = isDark,
                isActive = false
            )
        }
    }
}

@Composable
private fun ToolbarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDark: Boolean,
    isActive: Boolean = false
) {
    val colors = AttachmentToolbarTokens.Color

    // Blue accent color for active grounding state
    val activeColor = androidx.compose.ui.graphics.Color(0xFF4A90E2)

    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) activeColor.copy(alpha = 0.15f)
                   else if (isDark) colors.ItemBg else colors.ItemBgLight,
            border = BorderStroke(
                1.5.dp,
                if (isActive) activeColor
                else if (isDark) colors.ItemBorder else colors.ItemBorderLight
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) activeColor
                       else if (isDark) colors.TextPri else colors.TextPriLight,
                modifier = Modifier
                    .size(36.dp)
                    .padding(8.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isActive) activeColor
                    else if (isDark) colors.TextSec else colors.TextSecLight,
            fontSize = 12.sp,
            fontWeight = if (isActive) androidx.compose.ui.text.font.FontWeight.Medium
                        else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}
