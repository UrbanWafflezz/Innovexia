package com.example.innovexia.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.theme.InnovexiaTokens

/**
 * GlassIconButton - Premium matte button with subtle highlight
 *
 * Optimized for 60/90/120 Hz displays with smooth interactions.
 * Features:
 * - Matte glass surface with subtle border
 * - Optional accent dot indicator
 * - Capsule shape with proper touch target (44dp min)
 * - High contrast on dark backgrounds
 */
@Composable
fun GlassIconButton(
    icon: Painter,
    contentDesc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAccentDot: Boolean = true
) {
    Surface(
        onClick = onClick,
        shape = InnovexiaTokens.Shape.Capsule,
        color = InnovexiaTokens.Color.GraySurface.copy(alpha = 0.55f),
        border = BorderStroke(
            width = InnovexiaTokens.Border.Default,
            color = InnovexiaTokens.Color.GrayStroke
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDesc,
                tint = InnovexiaTokens.Color.TextPrimary,
                modifier = Modifier.size(18.dp)
            )

            // Optional accent dot to imply action/state
            if (showAccentDot) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(
                            color = InnovexiaTokens.Color.AccentGoldDim,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
