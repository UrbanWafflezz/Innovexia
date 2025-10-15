package com.example.innovexia.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Innovexia Color Palette
 *
 * Unified color system for light and dark modes.
 * All components should reference these tokens instead of hardcoded colors.
 */
object InnovexiaColors {

    // ═══════════════════════════════════════════════════════════════════
    // Brand & Accent Colors
    // ═══════════════════════════════════════════════════════════════════

    val Gold = Color(0xFFF2C94C)
    val GoldDim = Color(0xFFE6B84A)
    val OnGold = Color(0xFF0F0F0F)

    val BlueAccent = Color(0xFF3B82F6)
    val BlueBright = Color(0xFF2563EB)
    val TealAccent = Color(0xFF34D399)
    val CyanAccent = Color(0xFF38E8E1)
    val MagentaAccent = Color(0xFFFF6BD6)

    // ═══════════════════════════════════════════════════════════════════
    // Light Mode Palette
    // ═══════════════════════════════════════════════════════════════════

    val LightBackground = Color(0xFFF9FAFB)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFE5E7EB)
    val LightBorderLight = Color(0xFFE7EDF5)
    val LightDivider = Color(0xFFE5E7EB)

    val LightTextPrimary = Color(0xFF111827)
    val LightTextSecondary = Color(0xFF6B7280)
    val LightTextMuted = Color(0xFF64748B)

    // Gradients
    val LightGradientStart = Color(0xFFBFE8FF)
    val LightGradientMid = Color(0xFFEAF6FF)
    val LightGradientEnd = Color(0xFFFFFFFF)

    // ═══════════════════════════════════════════════════════════════════
    // Dark Mode Palette
    // ═══════════════════════════════════════════════════════════════════

    val DarkBackground = Color(0xFF0B121A)
    val DarkBackgroundAlt = Color(0xFF0F0F0F)
    val DarkSurface = Color(0xFF141A22)
    val DarkSurfaceElevated = Color(0xFF1F2937)
    val DarkSurfaceDrawer = Color(0xFF2A2A2A)
    val DarkBorder = Color(0xFF253041)
    val DarkDivider = Color(0xFF374151)

    val DarkTextPrimary = Color(0xFFE5EAF0)
    val DarkTextSecondary = Color(0xFF9CA3AF)
    val DarkTextMuted = Color(0xFF8FA0B3)

    // Gradients - Updated to high-end refined gray
    val DarkGradientStart = Color(0xFF171A1E) // InnovexiaTokens GraySurface
    val DarkGradientMid = Color(0xFF1E2329)   // InnovexiaTokens GrayElevated
    val DarkGradientEnd = Color(0xFF171A1E)   // InnovexiaTokens GraySurface

    // ═══════════════════════════════════════════════════════════════════
    // Semantic Colors (Light/Dark Agnostic)
    // ═══════════════════════════════════════════════════════════════════

    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val WarningAlt = Color(0xFFFBBF24)
    val Error = Color(0xFFEF4444)
    val ErrorRed = Color(0xFFEF4444) // Alias for Error
    val ErrorAlt = Color(0xFFDC2626)
    val Info = Color(0xFF3B82F6)

    // ═══════════════════════════════════════════════════════════════════
    // Component-Specific Colors
    // ═══════════════════════════════════════════════════════════════════

    // Chat bubbles
    val UserMessageBg = BlueAccent
    val UserMessageText = Color.White
    val CoralSend = Color(0xFFFF6B6B) // Send button coral/orange

    // Glass highlights
    val GlassHighlightCyan = CyanAccent
    val GlassHighlightPink = MagentaAccent

    // Persona colors (for avatar rings)
    val PersonaBlue = Color(0xFF3B82F6)
    val PersonaTeal = Color(0xFF14B8A6)
    val PersonaPurple = Color(0xFFA855F7)
    val PurpleAccent = Color(0xFFA855F7) // Alias for PersonaPurple
    val PersonaPink = Color(0xFFEC4899)
    val PersonaOrange = Color(0xFFF97316)
    val PersonaGreen = Color(0xFF10B981)
    val PersonaIndigo = Color(0xFF6366F1)
    val PersonaRed = Color(0xFFEF4444)
    val PersonaYellow = Color(0xFFFBBF24)
    val PersonaCyan = Color(0xFF06B6D4)

    // ═══════════════════════════════════════════════════════════════════
    // Transparency Variants
    // ═══════════════════════════════════════════════════════════════════

    val Transparent = Color.Transparent
    val Black = Color.Black
    val White = Color.White

    val WhiteAlpha10 = Color(0x1AFFFFFF)
    val WhiteAlpha14 = Color(0x24FFFFFF)
    val WhiteAlpha20 = Color(0x33FFFFFF)
    val WhiteAlpha72 = Color(0xB8FFFFFF)

    val BlackAlpha10 = Color(0x1A000000)
    val BlackAlpha20 = Color(0x33000000)
    val BlackAlpha50 = Color(0x80000000)

    /**
     * Get a persona color by index (for consistent avatar coloring)
     */
    fun getPersonaColor(index: Int): Color {
        val colors = listOf(
            PersonaBlue,
            PersonaTeal,
            PersonaPurple,
            PersonaPink,
            PersonaOrange,
            PersonaGreen,
            PersonaIndigo,
            PersonaRed,
            PersonaYellow,
            PersonaCyan
        )
        return colors[index % colors.size]
    }

    /**
     * Get a persona color by name (for deterministic coloring)
     */
    fun getPersonaColorByName(name: String): Color {
        val index = name.hashCode().let { if (it < 0) -it else it }
        return getPersonaColor(index)
    }
}

/**
 * Extended color properties for InnovexiaTheme
 */
data class ExtendedColors(
    val goldDim: Color,
    val onGold: Color,
    val personaCardBg: Color,
    val personaCardBorder: Color,
    val personaMutedText: Color,
    val searchBg: Color,
    val searchBorder: Color,
    val coral: Color,
    val glassCyan: Color,
    val glassPink: Color
)

/**
 * Light mode extended colors
 */
val LightExtendedColors = ExtendedColors(
    goldDim = InnovexiaColors.Gold,
    onGold = InnovexiaColors.OnGold,
    personaCardBg = InnovexiaColors.LightSurface,
    personaCardBorder = InnovexiaColors.LightBorderLight,
    personaMutedText = InnovexiaColors.LightTextMuted,
    searchBg = InnovexiaColors.LightSurface,
    searchBorder = InnovexiaColors.LightBorder,
    coral = InnovexiaColors.CoralSend,
    glassCyan = InnovexiaColors.CyanAccent,
    glassPink = InnovexiaColors.MagentaAccent
)

/**
 * Dark mode extended colors
 */
val DarkExtendedColors = ExtendedColors(
    goldDim = InnovexiaColors.GoldDim,
    onGold = InnovexiaColors.OnGold,
    personaCardBg = InnovexiaColors.DarkSurface, // Keep cards slightly elevated from background
    personaCardBorder = InnovexiaColors.DarkBorder,
    personaMutedText = InnovexiaColors.DarkTextMuted,
    searchBg = InnovexiaColors.DarkBackground, // Match chat background
    searchBorder = InnovexiaColors.DarkBorder,
    coral = InnovexiaColors.CoralSend,
    glassCyan = InnovexiaColors.CyanAccent,
    glassPink = InnovexiaColors.MagentaAccent
)
