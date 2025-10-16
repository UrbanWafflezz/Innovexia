package com.example.innovexia.ui.persona

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.sheets.PersonaSheet
import com.example.innovexia.ui.theme.InnovexiaTheme

/**
 * Integrated PersonaChip that manages both the chip display and the PersonaSheet.
 * Use this composable to get full persona selection functionality.
 *
 * Example usage:
 * ```
 * PersonaChipWithSheet(
 *     selectedPersona = currentPersona,
 *     onPersonaSelected = { newPersona ->
 *         // Update your app state and Gemini configuration
 *         viewModel.setActivePersona(newPersona)
 *     }
 * )
 * ```
 */
@Composable
fun PersonaChipWithSheet(
    selectedPersona: Persona?,
    onPersonaSelected: (Persona) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val haptic = LocalHapticFeedback.current
    var showSheet by rememberSaveable { mutableStateOf(false) }

    // ─── Chip ───
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSheet = true
                },
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selectedPersona != null) {
            // Persona chip with colored ring and initial
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = Color(selectedPersona.color),
                        shape = CircleShape
                    )
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedPersona.initial,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(selectedPersona.color)
                )
            }
        } else {
            // No persona - show neutral user icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "No persona selected",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }

    // ─── Sheet ───
    PersonaSheet(
        visible = showSheet,
        onDismiss = { showSheet = false },
        initialState = PersonaUiState(
            selectedPersonaId = selectedPersona?.id,
            my = demoMyPersonas(),
            publicList = demoPublicPersonas(),
            sources = SourcesState(items = demoSources()),
            memory = MemoryState(items = demoMemories())
        ),
        onPersonaSelected = { persona ->
            onPersonaSelected(persona)
            // Sheet will close automatically after selection
        }
    )
}

/**
 * Example integration showing how to use PersonaChipWithSheet in your screen.
 * This demonstrates state management and callback handling.
 */
@Composable
fun ExampleIntegrationScreen() {
    // Your app's persona state (typically in a ViewModel)
    var activePersona by rememberSaveable(stateSaver = PersonaSaver) {
        mutableStateOf<Persona?>(null)
    }

    // Example layout with the integrated chip
    Box(
        modifier = Modifier.size(400.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        PersonaChipWithSheet(
            selectedPersona = activePersona,
            onPersonaSelected = { newPersona ->
                // 1. Update local state
                activePersona = newPersona

                // 2. Update your Gemini system instruction
                // Example: viewModel.updateSystemInstruction(newPersona.systemPrompt)

                // 3. Optionally save to preferences
                // Example: preferencesRepo.saveSelectedPersona(newPersona.id)
            },
            modifier = Modifier
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State Saver for Persona
// ─────────────────────────────────────────────────────────────────────────────
private val PersonaSaver = androidx.compose.runtime.saveable.Saver<Persona?, Map<String, Any>>(
    save = { persona ->
        persona?.let {
            mapOf(
                "id" to it.id,
                "name" to it.name,
                "initial" to it.initial,
                "color" to it.color,
                "summary" to it.summary
            )
        }
    },
    restore = { map ->
        map?.let {
            Persona(
                id = it["id"] as String,
                name = it["name"] as String,
                initial = it["initial"] as String,
                color = it["color"] as Long,
                summary = it["summary"] as String
            )
        }
    }
)

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────
@Preview(name = "Chip with Sheet - Light", showBackground = true)
@Composable
private fun PersonaChipWithSheetPreview_Light() {
    InnovexiaTheme(darkTheme = false) {
        ExampleIntegrationScreen()
    }
}

@Preview(name = "Chip with Sheet - Dark", showBackground = true)
@Composable
private fun PersonaChipWithSheetPreview_Dark() {
    InnovexiaTheme(darkTheme = true) {
        ExampleIntegrationScreen()
    }
}
