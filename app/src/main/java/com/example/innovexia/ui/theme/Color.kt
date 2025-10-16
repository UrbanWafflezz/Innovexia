package com.example.innovexia.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Legacy color objects for backward compatibility
 * DEPRECATED: Use InnovexiaColors from ColorTokens.kt instead
 *
 * These are maintained for existing code that hasn't been migrated yet.
 * All new code should use InnovexiaColors directly.
 */

// Light theme colors
@Deprecated("Use InnovexiaColors instead", ReplaceWith("InnovexiaColors"))
object LightColors {
    val PrimaryText = InnovexiaColors.LightTextPrimary
    val SecondaryText = InnovexiaColors.LightTextSecondary
    val SurfaceElevated = InnovexiaColors.LightSurfaceElevated
    val AccentBlue = InnovexiaColors.BlueAccent

    // Gradient stops
    val GradientTop = InnovexiaColors.LightGradientStart
    val GradientCenter = InnovexiaColors.LightGradientMid
    val GradientBottom = InnovexiaColors.LightGradientEnd
}

// Dark theme colors - dark gray with fall accents
@Deprecated("Use InnovexiaColors instead", ReplaceWith("InnovexiaColors"))
object DarkColors {
    val PrimaryText = InnovexiaColors.DarkTextPrimary
    val SecondaryText = InnovexiaColors.DarkTextSecondary
    val SurfaceElevated = InnovexiaColors.DarkSurfaceElevated
    val AccentBlue = InnovexiaColors.BlueAccent

    // Fall color accents
    val AccentRed = InnovexiaColors.ErrorAlt
    val AccentGreen = InnovexiaColors.Success
    val AccentYellow = InnovexiaColors.WarningAlt

    // Gradient stops - dark grays
    val GradientTop = InnovexiaColors.DarkGradientStart
    val GradientCenter = InnovexiaColors.DarkGradientMid
    val GradientBottom = InnovexiaColors.DarkGradientEnd
}

// Gold Accent (dim, premium) for selected/active states
@Deprecated("Use InnovexiaColors instead", ReplaceWith("InnovexiaColors"))
object GoldAccent {
    val GoldDim = InnovexiaColors.Gold
    val GoldDimDark = InnovexiaColors.GoldDim
    val OnGold = InnovexiaColors.OnGold
}

// Persona Card Tokens
@Deprecated("Use ExtendedColors from ColorTokens.kt instead")
object PersonaCardTokens {
    // Dark mode
    val CardBgDark = InnovexiaColors.DarkSurface
    val CardBorderDark = InnovexiaColors.DarkBorder
    val MutedTextDark = InnovexiaColors.DarkTextMuted
    val SearchBgDark = InnovexiaColors.DarkSurface
    val SearchBorderDark = InnovexiaColors.DarkBorder

    // Light mode
    val CardBgLight = InnovexiaColors.LightSurface
    val CardBorderLight = InnovexiaColors.LightBorderLight
    val MutedTextLight = InnovexiaColors.LightTextMuted
    val SearchBgLight = InnovexiaColors.LightSurface
    val SearchBorderLight = InnovexiaColors.LightBorder
}
