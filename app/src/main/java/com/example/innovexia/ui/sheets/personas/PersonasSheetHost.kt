package com.example.innovexia.ui.sheets.personas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.core.persona.PersonaRepository
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.ui.persona.*
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Personas Dialog with proper scrolling behavior
 * - Dialog has bounded height
 * - Header + search + tabs are fixed
 * - Only tab body scrolls (LazyVerticalGrid)
 */
@Composable
fun PersonasDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    initialState: PersonaUiState = PersonaUiState(),
    onPersonaSelected: (Persona) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val focusManager = LocalFocusManager.current

    // Real ViewModel with Room + Firebase integration
    val context = LocalContext.current
    val viewModel: MyPersonasViewModel = viewModel {
        val database = AppDatabase.getInstance(context)
        val repo = PersonaRepository(database.personaDao())
        val prefs = com.example.innovexia.core.persona.PersonaPreferences(context)
        MyPersonasViewModel(repo, prefs)
    }

    // Collect ViewModel state
    val myPersonas by viewModel.my.collectAsState()
    val publicPersonas by viewModel.public.collectAsState()
    val activePersonaId by viewModel.activePersonaId.collectAsState()

    // Start observing when sheet becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            viewModel.start()
        }
    }

    // Listen for error/success messages and show Toast
    LaunchedEffect(visible) {
        if (visible) {
            viewModel.error.collect { message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // Local UI state for tab/query/dialogs
    var activeTab by rememberSaveable { mutableStateOf(PersonaTab.My) }
    var query by rememberSaveable { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPersona by remember { mutableStateOf<com.example.innovexia.core.persona.Persona?>(null) }

    val coroutineScope = rememberCoroutineScope()

    if (visible) {
        Dialog(
            onDismissRequest = {}, // Only X button can close
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            // Outer surface (rounded card) - matches chat page background
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (darkTheme) InnovexiaColors.DarkBackground else InnovexiaColors.LightBackground,
                tonalElevation = 0.dp,
                border = BorderStroke(
                    1.dp,
                    if (darkTheme) InnovexiaColors.DarkBorder.copy(alpha = 0.6f) else InnovexiaColors.LightBorder.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f) // Bounded height
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // Layout: header + search + tabs fixed; body scrolls
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                ) {
                    // ---- Compact Header ----
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            "Personas",
                            color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        IconButton(
                            onClick = {
                                focusManager.clearFocus()
                                onDismiss()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // ---- Search + New Button Row ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SearchField(
                            value = query,
                            onValueChange = { query = it },
                            darkTheme = darkTheme,
                            modifier = Modifier.weight(1f)
                        )

                        // New Persona Button
                        Surface(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Create new persona",
                                    tint = if (darkTheme) InnovexiaColors.OnGold else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "New",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (darkTheme) InnovexiaColors.OnGold else Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // ---- Tabs (fixed) ----
                    PersonasTabs(
                        selected = activeTab,
                        onSelect = { activeTab = it },
                        darkTheme = darkTheme
                    )

                    HorizontalDivider(
                        Modifier.padding(top = 6.dp, bottom = 6.dp),
                        color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f)
                        else LightColors.SecondaryText.copy(alpha = 0.2f)
                    )

                    // ---- Scrollable body (the ONLY scroller) ----
                    Box(Modifier.weight(1f)) {
                        when (activeTab) {
                            PersonaTab.My -> PersonasTabMy(
                                query = query,
                                personas = myPersonas,
                                selectedPersonaId = activePersonaId,
                                isSignedIn = viewModel.isSignedIn,
                                onPersonaSelected = { persona ->
                                    viewModel.setActivePersona(persona.id)
                                    onPersonaSelected(persona)
                                    focusManager.clearFocus()
                                    onDismiss()
                                },
                                onNewPersona = { showCreateDialog = true },
                                onStar = { persona ->
                                    viewModel.toggleDefault(persona.id)
                                },
                                onDuplicate = { /* TODO */ },
                                onRename = { /* TODO: Show rename dialog */ },
                                onDelete = { persona ->
                                    viewModel.delete(persona.id)
                                },
                                onEdit = { persona ->
                                    // Fetch core persona for editing
                                    coroutineScope.launch {
                                        val corePersona = viewModel.getCorePersona(persona.id)
                                        editingPersona = corePersona
                                    }
                                },
                                onMakePublic = { persona ->
                                    viewModel.makePublic(persona.id)
                                }
                            )

                            PersonaTab.Public -> PersonasTabPublic(
                                query = query,
                                personas = publicPersonas,
                                onImport = { persona ->
                                    viewModel.importPublic(persona)
                                },
                                onStar = { /* No-op for public */ }
                            )

                            PersonaTab.Sources -> {
                                // Sources tab - Full backend integration with PDF support
                                val selectedPersona = myPersonas.find { it.id == activePersonaId }
                                    ?: myPersonas.firstOrNull()

                                android.util.Log.d("PersonasSheetHost", "Sources tab - activePersonaId: $activePersonaId, selectedPersona: ${selectedPersona?.id} (${selectedPersona?.name})")

                                if (selectedPersona != null) {
                                    com.example.innovexia.ui.persona.sources.SourcesTabWithBackend(
                                        personaId = selectedPersona.id,
                                        personaName = selectedPersona.name,
                                        personaColor = Color(selectedPersona.color),
                                        darkTheme = darkTheme
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Select a persona to view sources",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                                        )
                                    }
                                }
                            }

                            PersonaTab.Memory -> {
                                // Memory tab - Backend connected
                                val selectedPersona = myPersonas.find { it.id == activePersonaId }
                                    ?: myPersonas.firstOrNull()

                                if (selectedPersona != null) {
                                    com.example.innovexia.ui.persona.memory.MemoryTabConnected(
                                        persona = selectedPersona,
                                        darkTheme = darkTheme
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Select a persona to view memories",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Persona Dialog (Persona 2.0)
    CreatePersonaDialog(
        visible = showCreateDialog,
        onDismiss = { showCreateDialog = false },
        onCreate = { name, color, summary, tags ->
            // Backward compatible: still uses the simple create for now
            // The dialog internally saves the full PersonaDraftDto
            viewModel.create(name, color, summary, tags)
            showCreateDialog = false
        },
        editPersonaId = null,
        editUid = if (viewModel.isSignedIn) viewModel.uid else null
    )

    // Edit Persona Dialog
    editingPersona?.let { persona ->
        CreatePersonaDialog(
            visible = true,
            onDismiss = { editingPersona = null },
            onCreate = { name, color, summary, tags ->
                // Update existing persona
                viewModel.create(name, color, summary, tags)
                editingPersona = null
            },
            editPersona = persona,
            editPersonaId = persona.id,
            editUid = if (viewModel.isSignedIn) viewModel.uid else null
        )
    }
}

/**
 * Modern tabs row with enhanced styling
 */
@Composable
private fun PersonasTabs(
    selected: PersonaTab,
    onSelect: (PersonaTab) -> Unit,
    darkTheme: Boolean
) {
    val tabs = PersonaTab.values().toList()

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0),
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        indicator = { tabPositions ->
            val idx = tabs.indexOf(selected)
            if (idx >= 0) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[idx]),
                    color = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                    height = 3.dp
                )
            }
        },
        divider = {}
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        when (tab) {
                            PersonaTab.My -> "My Personas"
                            PersonaTab.Public -> "Public"
                            PersonaTab.Sources -> "Sources"
                            PersonaTab.Memory -> "Memory"
                        },
                        color = if (tab == selected) {
                            if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                        } else {
                            if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.8f)
                            else LightColors.SecondaryText.copy(alpha = 0.7f)
                        },
                        fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (tab == selected) 14.sp else 13.sp
                    )
                },
                modifier = Modifier.height(40.dp)
            )
        }
    }
}

/**
 * Search field component
 */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val searchBg = if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F7FA)
    val searchBorder = if (darkTheme) Color(0xFF2A3441).copy(alpha = 0.5f) else Color(0xFFE0E5EB).copy(alpha = 0.7f)

    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = searchBg,
        border = BorderStroke(1.dp, searchBorder),
        shadowElevation = if (value.isNotEmpty()) 1.dp else 0.dp
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Search personas" },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold),
            singleLine = true,
            decorationBox = @Composable { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.8f)
                               else LightColors.SecondaryText.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                text = "Search...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.7f)
                                       else LightColors.SecondaryText.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                    if (value.isNotEmpty()) {
                        IconButton(
                            onClick = { onValueChange("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}

// Backward compatibility: Keep old PersonasSheetHost name
@Deprecated(
    "Use PersonasDialog instead",
    ReplaceWith("PersonasDialog(visible, onDismiss, initialState, onPersonaSelected, modifier)")
)
@Composable
fun PersonasSheetHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    initialState: PersonaUiState = PersonaUiState(),
    onPersonaSelected: (Persona) -> Unit,
    modifier: Modifier = Modifier
) {
    PersonasDialog(
        visible = visible,
        onDismiss = onDismiss,
        initialState = initialState,
        onPersonaSelected = onPersonaSelected,
        modifier = modifier
    )
}
