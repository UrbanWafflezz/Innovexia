package com.example.innovexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.core.persona.InnoPersonaDefaults
import com.example.innovexia.ui.models.Persona
import com.example.innovexia.ui.models.demoPersonas
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaTheme
import com.example.innovexia.ui.theme.LightColors

/**
 * A circular chip displaying a persona's initial with a colored ring.
 * Replaces the model selector in the UI.
 */
@Composable
fun PersonaChip(
    persona: Persona?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(40.dp) // 40dp touch target
            .clip(CircleShape)
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        if (persona != null) {
            // Check if this is Inno (the default persona)
            val isInno = persona.id == InnoPersonaDefaults.INNO_PERSONA_ID

            // Persona chip with colored ring and initial
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isInno) 2.5.dp else 2.dp, // Slightly thicker border for Inno
                        color = persona.color,
                        shape = CircleShape
                    )
                    .background(
                        if (darkTheme) DarkColors.SurfaceElevated else LightColors.SurfaceElevated
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Show star icon overlay for Inno
                if (isInno) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Default persona",
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd),
                        tint = persona.color
                    )
                }

                Text(
                    text = persona.initial,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = if (isInno) FontWeight.Bold else FontWeight.SemiBold // Bold for Inno
                    ),
                    color = persona.color
                )
            }
        } else {
            // No persona - show neutral user icon with dashed ring
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.4f)
                                else LightColors.SecondaryText.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .background(
                        if (darkTheme) DarkColors.SurfaceElevated else LightColors.SurfaceElevated
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "No persona",
                    modifier = Modifier.size(18.dp),
                    tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                )
            }
        }
    }
}

@Preview(name = "PersonaChip with Persona", showBackground = true)
@Composable
fun PersonaChipPreview() {
    InnovexiaTheme(darkTheme = false) {
        PersonaChip(
            persona = demoPersonas().first(),
            onClick = {},
            darkTheme = false
        )
    }
}

@Preview(name = "PersonaChip No Persona", showBackground = true)
@Composable
fun PersonaChipNoPersonaPreview() {
    InnovexiaTheme(darkTheme = false) {
        PersonaChip(
            persona = null,
            onClick = {},
            darkTheme = false
        )
    }
}

@Preview(name = "PersonaChip Dark", showBackground = true)
@Composable
fun PersonaChipDarkPreview() {
    InnovexiaTheme(darkTheme = true) {
        PersonaChip(
            persona = demoPersonas()[1],
            onClick = {},
            darkTheme = true
        )
    }
}
