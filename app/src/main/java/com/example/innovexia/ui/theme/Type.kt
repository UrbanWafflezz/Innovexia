package com.example.innovexia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.innovexia.R

/**
 * Legacy Typography for backward compatibility
 * DEPRECATED: Use InnovexiaTypography from Typography.kt instead
 *
 * This is maintained for existing code that hasn't been migrated yet.
 * All new code should use InnovexiaTypography directly.
 */

val InterFontFamily = FontFamily(
    Font(R.font.inter_font, FontWeight.Normal),
    Font(R.font.inter_font, FontWeight.Medium),
    Font(R.font.inter_font, FontWeight.SemiBold),
    Font(R.font.inter_font, FontWeight.Bold)
)

@Deprecated("Use InnovexiaTypography instead", ReplaceWith("InnovexiaTypography"))
val Typography = Typography(
    // Title for headers and section titles
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    // Subtitle and body text
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    // Button text and labels
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    // Default body text
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Small labels (section headers, timestamps, etc.)
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
