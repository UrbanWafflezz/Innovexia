package com.example.innovexia.ui.sheets

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.innovexia.ui.persona.*
import com.example.innovexia.ui.sheets.personas.PersonasSheetHost

/**
 * Premium Persona Sheet with My/Public/Sources/Memory tabs.
 * Opens from PersonaChip, allows selection, management, and exploration.
 *
 * REFACTORED: Now delegates to PersonasSheetHost in ui/sheets/personas/.
 * The implementation has been split into 4 organized files:
 * - PersonasSheetHost.kt: Container, header, search, tabs
 * - PersonasTabMy.kt: My personas grid with responsive layout
 * - PersonasTabPublic.kt: Public personas with import
 * - PersonasTabSourcesMemory.kt: Sources and memory management
 */
@Composable
fun PersonaSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    initialState: PersonaUiState = PersonaUiState(),
    onPersonaSelected: (Persona) -> Unit,
    modifier: Modifier = Modifier
) {
    PersonasSheetHost(
        visible = visible,
        onDismiss = onDismiss,
        initialState = initialState,
        onPersonaSelected = onPersonaSelected,
        modifier = modifier
    )
}
