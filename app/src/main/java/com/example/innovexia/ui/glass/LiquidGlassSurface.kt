package com.example.innovexia.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid glass surface with blur, gradients, noise, and specular highlights.
 * Auto-adapts to light/dark theme with vibrant colors in dark mode.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color? = null,
    borderGradient: Brush? = null,
    blurRadius: Dp = 32.dp,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    val actualTint = tint ?: if (darkTheme) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.88f)
    }

    val actualBorder = borderGradient ?: if (darkTheme) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFDC2626).copy(alpha = 0.12f),  // Subtle red
                Color(0xFFFBBF24).copy(alpha = 0.10f),  // Subtle yellow
                Color(0xFF10B981).copy(alpha = 0.08f)   // Subtle green
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.8f),
                Color(0xFFBFE8FF)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.25.dp, actualBorder, shape)
            .background(actualTint)
            .drawWithCache {
                onDrawBehind {
                    // Top-left specular highlight
                    val highlightTopLeft = Brush.radialGradient(
                        colors = if (darkTheme) {
                            listOf(
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        },
                        center = Offset(size.width * 0.2f, size.height * 0.2f),
                        radius = size.width * 0.5f
                    )

                    // Bottom-right specular highlight
                    val highlightBottomRight = Brush.radialGradient(
                        colors = if (darkTheme) {
                            listOf(
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFF60A5FA).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        },
                        center = Offset(size.width * 0.8f, size.height * 0.8f),
                        radius = size.width * 0.6f
                    )

                    drawRect(highlightTopLeft)
                    drawRect(highlightBottomRight)
                }
            }
    ) {
        content()
    }
}

