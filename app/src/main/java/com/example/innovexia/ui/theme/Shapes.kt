package com.example.innovexia.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Innovexia Shape System
 * Consistent rounded corner hierarchy for all components
 */
val InnovexiaShapes = Shapes(
    extraSmall = RoundedCornerShape(InnovexiaDesign.Radius.Small),
    small = RoundedCornerShape(InnovexiaDesign.Radius.Medium),
    medium = RoundedCornerShape(InnovexiaDesign.Radius.Large),
    large = RoundedCornerShape(InnovexiaDesign.Radius.XLarge),
    extraLarge = RoundedCornerShape(InnovexiaDesign.Radius.XXLarge)
)

/**
 * Additional shape tokens for specific components
 */
object InnovexiaComponentShapes {
    // Cards
    val Card = RoundedCornerShape(InnovexiaDesign.Radius.Card)
    val PersonaCard = RoundedCornerShape(InnovexiaDesign.Radius.Card)

    // Buttons
    val Button = RoundedCornerShape(InnovexiaDesign.Radius.Button)
    val ButtonCompact = RoundedCornerShape(InnovexiaDesign.Radius.Large)

    // Input fields
    val Input = RoundedCornerShape(InnovexiaDesign.Radius.Input)
    val InputCompact = RoundedCornerShape(InnovexiaDesign.Radius.XLarge)

    // Sheets and modals
    val Sheet = RoundedCornerShape(
        topStart = InnovexiaDesign.Radius.Sheet,
        topEnd = InnovexiaDesign.Radius.Sheet
    )
    val Dialog = RoundedCornerShape(InnovexiaDesign.Radius.XLarge)

    // Chips and tags
    val Chip = RoundedCornerShape(InnovexiaDesign.Radius.Large)
    val ChipCircle = CircleShape

    // Glass surfaces
    val Glass = RoundedCornerShape(InnovexiaDesign.Radius.Sheet)
    val GlassCard = RoundedCornerShape(InnovexiaDesign.Radius.Large)

    // Message bubbles
    val MessageBubble = RoundedCornerShape(InnovexiaDesign.Radius.Large)
    val MessageBubbleUser = RoundedCornerShape(
        topStart = InnovexiaDesign.Radius.Large,
        topEnd = InnovexiaDesign.Radius.Large,
        bottomStart = InnovexiaDesign.Radius.Large,
        bottomEnd = InnovexiaDesign.Radius.Small
    )
    val MessageBubbleAI = RoundedCornerShape(
        topStart = InnovexiaDesign.Radius.Large,
        topEnd = InnovexiaDesign.Radius.Large,
        bottomStart = InnovexiaDesign.Radius.Small,
        bottomEnd = InnovexiaDesign.Radius.Large
    )

    // Avatars
    val Avatar = CircleShape

    // None (sharp corners)
    val None = RoundedCornerShape(InnovexiaDesign.Radius.None)
}
