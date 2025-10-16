package com.example.innovexia.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Innovexia Typography System
 * Unified text styles using design system tokens
 */
val InnovexiaTypography = Typography(
    // Display - Large headings and hero text
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = InnovexiaDesign.FontSize.Display,
        lineHeight = (InnovexiaDesign.FontSize.Display.value * 1.3).sp,
        letterSpacing = 0.sp
    ),

    displayMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = InnovexiaDesign.FontSize.Title,
        lineHeight = (InnovexiaDesign.FontSize.Title.value * 1.3).sp,
        letterSpacing = 0.sp
    ),

    // Title - Section headers and sheet titles
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = InnovexiaDesign.FontSize.Title,
        lineHeight = (InnovexiaDesign.FontSize.Title.value * 1.3).sp,
        letterSpacing = 0.sp
    ),

    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = InnovexiaDesign.FontSize.Body,
        lineHeight = (InnovexiaDesign.FontSize.Body.value * 1.5).sp,
        letterSpacing = 0.sp
    ),

    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = InnovexiaDesign.FontSize.BodySmall,
        lineHeight = (InnovexiaDesign.FontSize.BodySmall.value * 1.4).sp,
        letterSpacing = 0.sp
    ),

    // Body - Main content text
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = InnovexiaDesign.FontSize.Body,
        lineHeight = (InnovexiaDesign.FontSize.Body.value * 1.5).sp,
        letterSpacing = 0.5.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = InnovexiaDesign.FontSize.BodySmall,
        lineHeight = (InnovexiaDesign.FontSize.BodySmall.value * 1.4).sp,
        letterSpacing = 0.25.sp
    ),

    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = InnovexiaDesign.FontSize.Label,
        lineHeight = (InnovexiaDesign.FontSize.Label.value * 1.4).sp,
        letterSpacing = 0.4.sp
    ),

    // Label - Buttons, tabs, and UI elements
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = InnovexiaDesign.FontSize.Body,
        lineHeight = (InnovexiaDesign.FontSize.Body.value * 1.5).sp,
        letterSpacing = 0.sp
    ),

    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = InnovexiaDesign.FontSize.BodySmall,
        lineHeight = (InnovexiaDesign.FontSize.BodySmall.value * 1.4).sp,
        letterSpacing = 0.5.sp
    ),

    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = InnovexiaDesign.FontSize.Caption,
        lineHeight = (InnovexiaDesign.FontSize.Caption.value * 1.4).sp,
        letterSpacing = 0.5.sp
    )
)
