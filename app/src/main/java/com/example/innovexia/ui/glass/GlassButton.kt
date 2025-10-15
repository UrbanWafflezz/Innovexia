package com.example.innovexia.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors

enum class GlassButtonStyle {
    Primary,
    Secondary,
    Ghost,
    Danger
}

/**
 * Glass-styled button with Primary, Secondary, and Ghost variants.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.Primary,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val shape = RoundedCornerShape(24.dp)

    when (style) {
        GlassButtonStyle.Primary -> {
            val gradient = if (darkTheme) {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF60A5FA).copy(alpha = 0.9f),
                        Color(0xFF38E8E1).copy(alpha = 0.8f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF3B82F6),
                        Color(0xFF60A5FA)
                    )
                )
            }

            Box(
                modifier = modifier
                    .defaultMinSize(minHeight = 56.dp)
                    .clip(shape)
                    .background(gradient)
                    .clickable(
                        enabled = enabled,
                        onClick = onClick,
                        role = Role.Button
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }
        }

        GlassButtonStyle.Secondary -> {
            LiquidGlassSurface(
                shape = shape,
                modifier = modifier.defaultMinSize(minHeight = 52.dp),
                darkTheme = darkTheme
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = enabled,
                            onClick = onClick,
                            role = Role.Button
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (leading != null) {
                            leading()
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                        )
                    }
                }
            }
        }

        GlassButtonStyle.Ghost -> {
            Box(
                modifier = modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clickable(
                        enabled = enabled,
                        onClick = onClick,
                        role = Role.Button
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (darkTheme) {
                            Color(0xFF60A5FA)
                        } else {
                            Color(0xFF3B82F6)
                        }
                    )
                }
            }
        }
        GlassButtonStyle.Danger -> {
            val gradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFEF4444),
                    Color(0xFFDC2626)
                )
            )

            Box(
                modifier = modifier
                    .defaultMinSize(minHeight = 56.dp)
                    .clip(shape)
                    .background(gradient)
                    .clickable(
                        enabled = enabled,
                        onClick = onClick,
                        role = Role.Button
                    )
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leading != null) {
                        leading()
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}
