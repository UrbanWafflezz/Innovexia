package com.example.innovexia.ui.glass

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors

/**
 * iOS-style segmented control with liquid glass styling.
 */
@Composable
fun GlassSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val shape = RoundedCornerShape(20.dp)
    val itemWidth = 100.dp

    // Animated offset for selection indicator
    val indicatorOffset by animateDpAsState(
        targetValue = itemWidth * selectedIndex,
        animationSpec = tween(durationMillis = 250),
        label = "segmented_indicator"
    )

    LiquidGlassSurface(
        shape = shape,
        modifier = modifier.height(40.dp),
        darkTheme = darkTheme
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            // Selection indicator
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (darkTheme) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF60A5FA).copy(alpha = 0.6f),
                                    Color(0xFF38E8E1).copy(alpha = 0.5f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.2f),
                                    Color(0xFF60A5FA).copy(alpha = 0.15f)
                                )
                            )
                        }
                    )
            )

            // Options
            Row {
                options.forEachIndexed { index, option ->
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(32.dp)
                            .clickable(
                                onClick = { onSelected(index) },
                                role = Role.Tab
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (index == selectedIndex) {
                                if (darkTheme) Color.White else LightColors.PrimaryText
                            } else {
                                if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
