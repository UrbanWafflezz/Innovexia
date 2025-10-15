package com.example.innovexia.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * InnovexiaTokens - High-end design tokens optimized for 60/90/120 Hz displays
 *
 * Refined dark-mode gray palette with P3-safe colors, consistent spacing,
 * and premium shapes for chat header and UI components.
 */
object InnovexiaTokens {

    /**
     * Color System - Premium dark mode with refined grays
     * All colors are P3 wide-gamut safe
     */
    object Color {
        // ═══════════════════════════════════════════════════════════════════
        // High-End Dark Surfaces
        // ═══════════════════════════════════════════════════════════════════

        /** Main page background - deep charcoal */
        val GraySurface = androidx.compose.ui.graphics.Color(0xFF171A1E)

        /** Header/elevated surfaces - slightly lighter */
        val GrayElevated = androidx.compose.ui.graphics.Color(0xFF1E2329)

        /** Subtle borders and strokes */
        val GrayStroke = androidx.compose.ui.graphics.Color(0xFF2A323B)

        /** Input fields, cards */
        val GrayCard = androidx.compose.ui.graphics.Color(0xFF252C35)

        // ═══════════════════════════════════════════════════════════════════
        // Text Colors
        // ═══════════════════════════════════════════════════════════════════

        /** Primary text - high contrast white */
        val TextPrimary = androidx.compose.ui.graphics.Color(0xFFECEFF4)

        /** Secondary text - muted gray */
        val TextSecondary = androidx.compose.ui.graphics.Color(0xFFB7C0CC)

        /** Tertiary/hint text */
        val TextTertiary = androidx.compose.ui.graphics.Color(0xFF8A96A6)

        // ═══════════════════════════════════════════════════════════════════
        // Accent Colors
        // ═══════════════════════════════════════════════════════════════════

        /** Subtle gold accent - dimmed for dark mode */
        val AccentGoldDim = androidx.compose.ui.graphics.Color(0xFFDBB461)

        /** Soft blue for links/actions */
        val AccentBlueSoft = androidx.compose.ui.graphics.Color(0xFF4B78C7)

        /** Teal highlight */
        val AccentTeal = androidx.compose.ui.graphics.Color(0xFF3DBAA2)

        /** Error/warning red */
        val AccentRed = androidx.compose.ui.graphics.Color(0xFFE85D5D)

        // ═══════════════════════════════════════════════════════════════════
        // Interactive States
        // ═══════════════════════════════════════════════════════════════════

        /** Overlay for press/hover states */
        val PressOverlay = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f)

        /** Ripple effect color */
        val RippleColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f)

        /** Disabled state overlay */
        val DisabledOverlay = androidx.compose.ui.graphics.Color(0x40000000)

        // ═══════════════════════════════════════════════════════════════════
        // Transparency Variants
        // ═══════════════════════════════════════════════════════════════════

        val WhiteAlpha06 = androidx.compose.ui.graphics.Color(0x0FFFFFFF)
        val WhiteAlpha10 = androidx.compose.ui.graphics.Color(0x1AFFFFFF)
        val WhiteAlpha14 = androidx.compose.ui.graphics.Color(0x24FFFFFF)
        val WhiteAlpha20 = androidx.compose.ui.graphics.Color(0x33FFFFFF)
        val BlackAlpha40 = androidx.compose.ui.graphics.Color(0x66000000)
    }

    /**
     * Shape System - Rounded corners for premium feel
     */
    object Shape {
        /** Small radius for cards */
        val RadiusS = 12.dp

        /** Medium radius for dialogs */
        val RadiusM = 14.dp

        /** Large radius for sheets */
        val RadiusL = 16.dp

        /** Extra large radius */
        val RadiusXL = 20.dp

        /** Capsule shape for buttons */
        val Capsule = RoundedCornerShape(28.dp)

        /** Rounded corners for chat bubbles */
        val Bubble = RoundedCornerShape(18.dp)

        /** Input field shape */
        val Input = RoundedCornerShape(30.dp)

        /** Card shape */
        val Card = RoundedCornerShape(14.dp)
    }

    /**
     * Spacing System - 4dp-based grid
     */
    object Space {
        val XXS = 4.dp
        val XS = 6.dp
        val S = 8.dp
        val M = 12.dp
        val L = 16.dp
        val XL = 20.dp
        val XXL = 24.dp
        val XXXL = 32.dp
    }

    /**
     * Motion System - Optimized for high refresh rates
     * Timings in milliseconds
     */
    object Motion {
        /** Ultra-fast micro-interactions (120 Hz optimized) */
        const val Instant = 100

        /** Quick transitions (60/90/120 Hz friendly) */
        const val Fast = 160

        /** Standard animations */
        const val Normal = 220

        /** Moderate pace for complex animations */
        const val Moderate = 280

        /** Slow, emphasis animations */
        const val Slow = 350
    }

    /**
     * Size System - Component dimensions
     */
    object Size {
        /** Minimum touch target (accessibility) */
        val TouchMin = 44.dp

        /** Icon sizes */
        val IconS = 20.dp
        val IconM = 24.dp
        val IconL = 28.dp

        /** Button heights */
        val ButtonS = 36.dp
        val ButtonM = 44.dp
        val ButtonL = 52.dp

        /** Avatar sizes */
        val AvatarS = 28.dp
        val AvatarM = 40.dp
        val AvatarL = 64.dp
    }

    /**
     * Border System - Consistent stroke widths
     */
    object Border {
        val Thin = 0.5.dp
        val Default = 1.dp
        val Medium = 1.25.dp
        val Thick = 2.dp
    }
}
