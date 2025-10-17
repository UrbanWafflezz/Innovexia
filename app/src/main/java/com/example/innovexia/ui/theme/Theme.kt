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
 * Full Material 3 color system implementation
 */
private val LightColorScheme = lightColorScheme(
    primary = InnovexiaColors.Gold,
    onPrimary = InnovexiaColors.OnGold,
    primaryContainer = InnovexiaColors.Gold.copy(alpha = 0.12f),
    onPrimaryContainer = InnovexiaColors.LightTextPrimary,
    inversePrimary = InnovexiaColors.GoldDim,

    secondary = InnovexiaColors.BlueAccent,
    onSecondary = Color.White,
    secondaryContainer = InnovexiaColors.BlueAccent.copy(alpha = 0.12f),
    onSecondaryContainer = InnovexiaColors.BlueBright,

    tertiary = InnovexiaColors.TealAccent,
    onTertiary = Color.White,
    tertiaryContainer = InnovexiaColors.TealAccent.copy(alpha = 0.12f),
    onTertiaryContainer = InnovexiaColors.TealAccent,

    surface = InnovexiaColors.LightSurface,
    onSurface = InnovexiaColors.LightTextPrimary,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = InnovexiaColors.LightTextSecondary,
    surfaceTint = InnovexiaColors.Gold,
    inverseSurface = InnovexiaColors.DarkSurface,
    inverseOnSurface = InnovexiaColors.DarkTextPrimary,

    background = InnovexiaColors.LightBackground,
    onBackground = InnovexiaColors.LightTextPrimary,

    error = InnovexiaColors.Error,
    onError = Color.White,
    errorContainer = InnovexiaColors.Error.copy(alpha = 0.12f),
    onErrorContainer = InnovexiaColors.ErrorAlt,

    outline = InnovexiaColors.LightBorder,
    outlineVariant = InnovexiaColors.LightBorderLight,
    scrim = Color.Black.copy(alpha = 0.32f)
)

/**
 * Dark color scheme using unified color tokens
 * Full Material 3 color system implementation
 */
private val DarkColorScheme = darkColorScheme(
    primary = InnovexiaColors.GoldDim,
    onPrimary = InnovexiaColors.OnGold,
    primaryContainer = InnovexiaColors.GoldDim.copy(alpha = 0.16f),
    onPrimaryContainer = InnovexiaColors.DarkTextPrimary,
    inversePrimary = InnovexiaColors.Gold,

    secondary = InnovexiaColors.BlueAccent,
    onSecondary = Color.White,
    secondaryContainer = InnovexiaColors.BlueAccent.copy(alpha = 0.16f),
    onSecondaryContainer = InnovexiaColors.BlueAccent,

    tertiary = InnovexiaColors.TealAccent,
    onTertiary = Color.White,
    tertiaryContainer = InnovexiaColors.TealAccent.copy(alpha = 0.16f),
    onTertiaryContainer = InnovexiaColors.TealAccent,

    surface = InnovexiaColors.DarkSurface,
    onSurface = InnovexiaColors.DarkTextPrimary,
    surfaceVariant = Color(0xFF1C2128),
    onSurfaceVariant = InnovexiaColors.DarkTextSecondary,
    surfaceTint = InnovexiaColors.GoldDim,
    inverseSurface = InnovexiaColors.LightSurface,
    inverseOnSurface = InnovexiaColors.LightTextPrimary,

    background = InnovexiaColors.DarkBackground,
    onBackground = InnovexiaColors.DarkTextPrimary,

    error = InnovexiaColors.Error,
    onError = Color.White,
    errorContainer = InnovexiaColors.Error.copy(alpha = 0.16f),
    onErrorContainer = InnovexiaColors.Error,

    outline = InnovexiaColors.DarkBorder,
    outlineVariant = InnovexiaColors.DarkBorder.copy(alpha = 0.5f),
    scrim = Color.Black.copy(alpha = 0.5f)
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
