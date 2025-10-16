package com.example.innovexia.ui.chat.bubbles

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Skeleton loader for ResponseBubbleV2 during streaming
 * Shows shimmer effect for text and code blocks
 */
@Composable
fun ResponseBubbleSkeleton(
    modifier: Modifier = Modifier
) {
    val bubbleColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    // Shimmer animation
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        borderColor.copy(alpha = 0.05f),
        borderColor.copy(alpha = 0.15f),
        borderColor.copy(alpha = 0.05f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 1000f, translateAnim - 1000f),
        end = Offset(translateAnim, translateAnim)
    )

    Surface(
        color = bubbleColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header skeleton
            ShimmerBox(
                brush = brush,
                width = 120.dp,
                height = 14.dp,
                cornerRadius = 7.dp
            )

            Spacer(Modifier.height(4.dp))

            // Text line skeletons
            ShimmerBox(
                brush = brush,
                width = 280.dp,
                height = 16.dp,
                cornerRadius = 4.dp
            )
            ShimmerBox(
                brush = brush,
                width = 240.dp,
                height = 16.dp,
                cornerRadius = 4.dp
            )
            ShimmerBox(
                brush = brush,
                width = 260.dp,
                height = 16.dp,
                cornerRadius = 4.dp
            )

            Spacer(Modifier.height(4.dp))

            // Code block skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(borderColor.copy(alpha = 0.1f))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(
                        brush = brush,
                        width = 80.dp,
                        height = 12.dp,
                        cornerRadius = 4.dp
                    )
                    ShimmerBox(
                        brush = brush,
                        width = 200.dp,
                        height = 14.dp,
                        cornerRadius = 4.dp
                    )
                    ShimmerBox(
                        brush = brush,
                        width = 180.dp,
                        height = 14.dp,
                        cornerRadius = 4.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ShimmerBox(
    brush: Brush,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}
