package com.example.innovexia.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Circular avatar with neon gradient ring.
 */
@Composable
fun GlassAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    ringBrush: Brush? = null,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val defaultRingBrush = ringBrush ?: Brush.sweepGradient(
        colors = listOf(
            Color(0xFF38E8E1), // Cyan
            Color(0xFFFF6BD6), // Magenta
            Color(0xFF60A5FA), // Blue
            Color(0xFF38E8E1)  // Back to cyan
        )
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(2.5.dp, defaultRingBrush, CircleShape)
            .background(
                if (darkTheme) DarkColors.SurfaceElevated else LightColors.SurfaceElevated
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(2).uppercase(),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.35f).sp
            ),
            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
        )
    }
}
