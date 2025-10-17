package com.example.innovexia.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * AttachmentToolbar - Material 3 Design
 *
 * Material 3 Features:
 * - Staggered cascade animation for toolbar items
 * - M3 surface container colors
 * - Enhanced ripple effects
 * - Active state with M3 tertiary color
 * - Smooth slide-in from bottom
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
    val colorScheme = MaterialTheme.colorScheme

    // Capture the callback to prevent stale closures
    val currentOnGroundingToggle = rememberUpdatedState(onGroundingToggle)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            initialOffsetY = { it }
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = ComposerTokens.Motion.DurationMedium,
                easing = ComposerTokens.Motion.EasingEnter
            )
        ),
        exit = slideOutVertically(
            animationSpec = tween(
                durationMillis = ComposerTokens.Motion.DurationShort,
                easing = ComposerTokens.Motion.EasingExit
            ),
            targetOffsetY = { it }
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = ComposerTokens.Motion.DurationShort,
                easing = ComposerTokens.Motion.EasingExit
            )
        ),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1E2329), // Dark surface matching side menu
            tonalElevation = 3.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, Color(0xFF2A323B).copy(alpha = 0.6f)), // Subtle border
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Staggered animation for each item
                val items = remember {
                    listOf(
                        Triple(Icons.Rounded.PhotoLibrary, "Photos", onPickPhotos),
                        Triple(Icons.Rounded.Description, "Files", onPickFiles),
                        Triple(Icons.Rounded.Public, "Grounding") {
                            android.util.Log.d("AttachmentToolbar", "Grounding clicked! Current: $groundingEnabled, New: ${!groundingEnabled}")
                            currentOnGroundingToggle.value(!groundingEnabled)
                        },
                        Triple(Icons.Rounded.PhotoCamera, "Camera", onCapture),
                        Triple(Icons.Rounded.PictureAsPdf, "Scan", onScanPdf)
                    )
                }

                items.forEachIndexed { index, (icon, label, onClick) ->
                    ToolbarItem(
                        icon = icon,
                        label = label,
                        onClick = onClick,
                        isActive = label == "Grounding" && groundingEnabled,
                        delay = index * 50L // Staggered delay
                    )
                }
            }
        }
    }
}

/**
 * Toolbar item with staggered animation and Material 3 styling
 */
@Composable
private fun RowScope.ToolbarItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    delay: Long = 0L
) {
    // Staggered entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delay)
        visible = true
    }

    // Entrance animation values
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "item_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ComposerTokens.Motion.DurationMedium,
            easing = ComposerTokens.Motion.EasingEnter
        ),
        label = "item_alpha"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (isActive) {
                    Color(0xFF8B5CF6).copy(alpha = 0.2f) // Purple accent for active
                } else {
                    Color(0xFF2A323B).copy(alpha = 0.5f) // Dark subtle background
                },
                contentColor = if (isActive) {
                    Color(0xFFA855F7) // Bright purple for active icon
                } else {
                    Color(0xFFE5EAF0) // Light gray for inactive icon
                }
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = label,
            color = if (isActive) {
                Color(0xFFA855F7) // Purple accent text when active
            } else {
                Color(0xFF9AA6B2) // Muted text when inactive
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) {
                androidx.compose.ui.text.font.FontWeight.SemiBold
            } else {
                androidx.compose.ui.text.font.FontWeight.Normal
            }
        )
    }
}
