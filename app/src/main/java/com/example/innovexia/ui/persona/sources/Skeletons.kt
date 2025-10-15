package com.example.innovexia.ui.persona.sources

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Skeleton loading state for sources list
 */
@Composable
fun SourcesSkeletons(
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(8) { index ->
            SourceCardSkeleton(darkTheme = darkTheme)
        }
    }
}

/**
 * Skeleton for a single source card
 */
@Composable
private fun SourceCardSkeleton(
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val shimmerColors = listOf(
        if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5),
        if (darkTheme) Color(0xFF253041) else Color(0xFFE7EDF5),
        if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5)
    )

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5))
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Preview skeleton
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmer(shimmerColors, translateAnim)
            )

            // Content skeleton
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmer(shimmerColors, translateAnim)
                )

                // Subtitle skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .shimmer(shimmerColors, translateAnim)
                )

                // Metadata chips skeleton
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .shimmer(shimmerColors, translateAnim)
                        )
                    }
                }

                // Status pill skeleton
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmer(shimmerColors, translateAnim)
                )
            }

            // Menu button skeleton
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .shimmer(shimmerColors, translateAnim)
            )
        }
    }
}

/**
 * Shimmer effect modifier
 */
private fun Modifier.shimmer(colors: List<Color>, translateAnim: Float): Modifier {
    return this.background(
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = colors,
            start = androidx.compose.ui.geometry.Offset(translateAnim - 1000f, 0f),
            end = androidx.compose.ui.geometry.Offset(translateAnim, 0f)
        )
    )
}
