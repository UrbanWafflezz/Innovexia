package com.example.innovexia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Composition local for extended Innovexia colors
 */
private val LocalExtendedColors = staticCompositionLocalOf {
    LightExtendedColors
}

/**
 * InnovexiaTheme accessor object for extended colors
 */
object InnovexiaTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}

/**
 * Light color scheme using unified color tokens
 */
private val LightColorScheme = lightColorScheme(
    primary = InnovexiaColors.Gold,
    onPrimary = InnovexiaColors.OnGold,
    primaryContainer = InnovexiaColors.Gold.copy(alpha = 0.1f),
    onPrimaryContainer = InnovexiaColors.LightTextPrimary,
    secondary = InnovexiaColors.BlueAccent,
    onSecondary = Color.White,
    surface = InnovexiaColors.LightSurface,
    onSurface = InnovexiaColors.LightTextPrimary,
    surfaceVariant = InnovexiaColors.LightSurfaceElevated,
    onSurfaceVariant = InnovexiaColors.LightTextSecondary,
    background = InnovexiaColors.LightBackground,
    onBackground = InnovexiaColors.LightTextPrimary,
    outline = InnovexiaColors.LightBorder,
    error = InnovexiaColors.Error,
    onError = Color.White
)

/**
 * Dark color scheme using unified color tokens
 */
private val DarkColorScheme = darkColorScheme(
    primary = InnovexiaColors.GoldDim,
    onPrimary = InnovexiaColors.OnGold,
    primaryContainer = InnovexiaColors.GoldDim.copy(alpha = 0.2f),
    onPrimaryContainer = InnovexiaColors.DarkTextPrimary,
    secondary = InnovexiaColors.BlueAccent,
    onSecondary = Color.White,
    surface = InnovexiaColors.DarkSurface,
    onSurface = InnovexiaColors.DarkTextPrimary,
    surfaceVariant = InnovexiaColors.DarkSurfaceElevated,
    onSurfaceVariant = InnovexiaColors.DarkTextSecondary,
    background = InnovexiaColors.DarkBackground,
    onBackground = InnovexiaColors.DarkTextPrimary,
    outline = InnovexiaColors.DarkBorder,
    error = InnovexiaColors.Error,
    onError = Color.White
)

/**
 * Innovexia Material Theme
 * Provides unified design system tokens throughout the app
 */
@Composable
fun InnovexiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = InnovexiaTypography,
            shapes = InnovexiaShapes,
            content = content
        )
    }
}

/**
 * Get the background gradient for the app
 */
@Composable
fun getBackgroundGradient(darkTheme: Boolean = isSystemInDarkTheme()): Brush {
    return if (darkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                InnovexiaColors.DarkGradientStart,
                InnovexiaColors.DarkGradientMid,
                InnovexiaColors.DarkGradientEnd
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                InnovexiaColors.LightGradientStart,
                InnovexiaColors.LightGradientMid,
                InnovexiaColors.LightGradientEnd
            )
        )
    }
}
