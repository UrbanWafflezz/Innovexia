package com.example.innovexia.ui.glass

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.InnovexiaDesign

/**
 * Design tokens for the liquid glass design system.
 * Now integrated with InnovexiaDesign system tokens.
 */
object GlassTokens {
    object Light {
        val tint = InnovexiaColors.White.copy(alpha = InnovexiaDesign.Glass.SurfaceAlphaLight)
        val borderGradient = Brush.linearGradient(
            colors = listOf(
                InnovexiaColors.White.copy(alpha = 0.8f),
                InnovexiaColors.LightGradientStart
            )
        )
        val highlightColor = InnovexiaColors.BlueAccent.copy(alpha = 0.3f)
    }

    object Dark {
        val tint = InnovexiaColors.White.copy(alpha = InnovexiaDesign.Glass.SurfaceAlphaDark)
        val borderGradient = Brush.sweepGradient(
            colors = listOf(
                InnovexiaColors.MagentaAccent,
                InnovexiaColors.BlueAccent,
                InnovexiaColors.CyanAccent,
                InnovexiaColors.MagentaAccent
            )
        )
        val highlightCyan = InnovexiaColors.CyanAccent
        val highlightPink = InnovexiaColors.MagentaAccent
    }

    // Glass system constants (from InnovexiaDesign)
    val blurRadius = InnovexiaDesign.Glass.BlurRadius
    val borderWidth = InnovexiaDesign.Glass.BorderWidth
    val cornerRadius = InnovexiaDesign.Glass.CornerRadius
}
