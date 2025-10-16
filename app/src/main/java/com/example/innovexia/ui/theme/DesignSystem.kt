package com.example.innovexia.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Innovexia Global Design System
 *
 * Centralized design tokens for consistent spacing, typography, corners,
 * elevation, and motion across the entire application.
 */
object InnovexiaDesign {

    /**
     * Font Size Scale
     * Standardized text sizes for all components
     */
    object FontSize {
        val Display = 24.sp
        val Title = 20.sp
        val Body = 16.sp
        val BodySmall = 14.sp
        val Label = 13.sp
        val Caption = 11.sp
    }

    /**
     * Corner Radius System
     * Consistent rounded corners hierarchy
     */
    object Radius {
        val None = 0.dp
        val XSmall = 4.dp
        val Small = 8.dp
        val Medium = 12.dp
        val Large = 16.dp
        val XLarge = 20.dp
        val XXLarge = 24.dp
        val Sheet = 28.dp
        val Button = 24.dp
        val Input = 30.dp
        val Card = 14.dp
        val Circle = 100.dp
    }

    /**
     * Spacing Grid
     * 4dp-based spacing system for consistent layouts
     */
    object Spacing {
        val XXS = 2.dp
        val XS = 4.dp
        val SM = 8.dp
        val MD = 12.dp
        val LG = 16.dp
        val XL = 20.dp
        val XXL = 24.dp
        val XXXL = 32.dp
    }

    /**
     * Elevation System
     * Shadow depths for layering
     */
    object Elevation {
        val Flat = 0.dp
        val Low = 2.dp
        val Card = 4.dp
        val Sheet = 6.dp
        val Dialog = 8.dp
        val Floating = 12.dp
    }

    /**
     * Motion Timings
     * Animation durations in milliseconds
     */
    object Motion {
        const val Instant = 0
        const val Fast = 120
        const val Normal = 200
        const val Moderate = 250
        const val Slow = 300
        const val VerySlow = 500
    }

    /**
     * Border Widths
     * Consistent stroke weights
     */
    object Border {
        val Thin = 0.5.dp
        val Default = 1.dp
        val Medium = 1.25.dp
        val Thick = 2.dp
        val ExtraThick = 2.5.dp
    }

    /**
     * Component Sizes
     * Standard dimensions for common elements
     */
    object Size {
        // Touch targets (minimum 44dp for accessibility)
        val TouchTargetMin = 44.dp
        val TouchTargetMedium = 48.dp

        // Icons
        val IconSmall = 16.dp
        val IconMedium = 20.dp
        val IconLarge = 24.dp
        val IconXLarge = 28.dp

        // Avatars
        val AvatarSmall = 24.dp
        val AvatarMedium = 28.dp
        val AvatarLarge = 40.dp
        val AvatarXLarge = 64.dp

        // Chips
        val ChipHeight = 32.dp
        val ChipTouchTarget = 40.dp

        // Input fields
        val InputHeight = 60.dp
        val InputHeightCompact = 48.dp

        // Bottom bar
        val BottomBarHeight = 64.dp

        // Cards
        val PersonaCardWidth = 164.dp
        val PersonaCardHeight = 132.dp
    }

    /**
     * Glass System Constants
     * Unified glass design parameters
     */
    object Glass {
        val BlurRadius = 32.dp
        val BorderWidth = Border.Medium
        val CornerRadius = Radius.Sheet
        val SurfaceAlphaLight = 0.72f
        val SurfaceAlphaDark = 0.14f
    }

    /**
     * Layout Constants
     * Standard layout parameters
     */
    object Layout {
        val ScreenPaddingHorizontal = Spacing.LG
        val ScreenPaddingVertical = Spacing.LG
        val SectionSpacing = Spacing.LG
        val CardPadding = Spacing.MD
        val SheetPadding = Spacing.XL
        val ListItemPadding = Spacing.LG
        val GridSpacing = Spacing.MD
    }
}
