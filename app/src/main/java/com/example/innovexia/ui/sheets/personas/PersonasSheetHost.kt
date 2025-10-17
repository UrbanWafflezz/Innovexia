package com.example.innovexia.ui.sheets.personas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
            // Outer surface (rounded card) - matches side menu Material 3 design with gradient
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f) // Bounded height
                    .imePadding()
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (darkTheme) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    InnovexiaColors.DarkGradientStart,
                                    InnovexiaColors.DarkGradientMid,
                                    InnovexiaColors.DarkGradientEnd
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    InnovexiaColors.LightGradientStart,
                                    InnovexiaColors.LightGradientMid,
                                    InnovexiaColors.LightGradientEnd
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (darkTheme) Color(0xFF404040).copy(alpha = 0.5f) else InnovexiaColors.LightBorder.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(24.dp)
                    )
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
                            color = if (darkTheme) Color(0xFFD4AF37) else LightColors.PrimaryText,
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
                                tint = if (darkTheme) Color(0xFFA89968) else LightColors.SecondaryText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ---- Search + New Button Row (Integrated like side menu) ----
                    SearchBarWithNewButton(
                        value = query,
                        onValueChange = { query = it },
                        onNewClick = { showCreateDialog = true },
                        darkTheme = darkTheme,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(6.dp))

                    // ---- Tabs (fixed) ----
                    PersonasTabs(
                        selected = activeTab,
                        onSelect = { activeTab = it },
                        darkTheme = darkTheme
                    )

                    HorizontalDivider(
                        Modifier.padding(top = 6.dp, bottom = 6.dp),
                        color = if (darkTheme) Color(0xFF3A3A3A)
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
                                    // Toggle: if clicking active persona, deselect it (turn off)
                                    if (persona.id == activePersonaId) {
                                        viewModel.setActivePersona(null) // Turn off persona
                                        // Note: Don't call onPersonaSelected when turning off
                                        // The HomeScreen will observe the activePersonaId change
                                    } else {
                                        viewModel.setActivePersona(persona.id)
                                        onPersonaSelected(persona)
                                    }
                                    focusManager.clearFocus()
                                    // Don't auto-close - let user close manually
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
                                // Sources tab - Simplified with tab-specific search
                                val selectedPersona = myPersonas.find { it.id == activePersonaId }
                                    ?: myPersonas.firstOrNull()

                                if (selectedPersona != null) {
                                    com.example.innovexia.ui.persona.sources.SourcesTabWithBackend(
                                        personaId = selectedPersona.id,
                                        personaName = selectedPersona.name,
                                        personaColor = Color(selectedPersona.color),
                                        darkTheme = darkTheme,
                                        searchQuery = query // Tab-specific search
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
                                // Memory tab - Backend connected with search
                                val selectedPersona = myPersonas.find { it.id == activePersonaId }
                                    ?: myPersonas.firstOrNull()

                                if (selectedPersona != null) {
                                    com.example.innovexia.ui.persona.memory.MemoryTabConnected(
                                        persona = selectedPersona,
                                        darkTheme = darkTheme,
                                        searchQuery = query // Pass top search bar query
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
 * Material 3 tabs row - compact and properly scaled
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
        contentColor = if (darkTheme) Color(0xFFD4AF37) else LightColors.PrimaryText,
        indicator = { tabPositions ->
            val idx = tabs.indexOf(selected)
            if (idx >= 0) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[idx]),
                    color = if (darkTheme) Color(0xFFE6B84A) else InnovexiaColors.Gold,
                    height = 2.dp
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
                        text = when (tab) {
                            PersonaTab.My -> "Personas"
                            PersonaTab.Public -> "Public"
                            PersonaTab.Sources -> "Sources"
                            PersonaTab.Memory -> "Memory"
                        },
                        color = if (tab == selected) {
                            if (darkTheme) Color(0xFFD4AF37) else LightColors.PrimaryText
                        } else {
                            if (darkTheme) Color(0xFFA89968).copy(alpha = 0.8f)
                            else LightColors.SecondaryText.copy(alpha = 0.7f)
                        },
                        fontWeight = if (tab == selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 80.dp)
            )
        }
    }
}

/**
 * Integrated search bar with New button embedded inside (like side menu design)
 */
@Composable
private fun SearchBarWithNewButton(
    value: String,
    onValueChange: (String) -> Unit,
    onNewClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val searchBg = if (darkTheme) Color(0xFF1E2329) else Color(0xFFF5F7FA)
    val searchBorder = if (darkTheme) Color(0xFF2A323B).copy(alpha = 0.6f) else Color(0xFFE0E5EB).copy(alpha = 0.7f)

    Surface(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(21.dp),
        color = searchBg,
        border = BorderStroke(1.dp, searchBorder),
        shadowElevation = if (value.isNotEmpty()) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search icon
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (darkTheme) Color(0xFFA89968).copy(alpha = 0.8f)
                       else LightColors.SecondaryText.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )

            // Text field
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Search personas" },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = if (darkTheme) Color(0xFFD4AF37) else LightColors.PrimaryText,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(if (darkTheme) Color(0xFFE6B84A) else InnovexiaColors.Gold),
                singleLine = true,
                decorationBox = @Composable { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = "Search...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                color = if (darkTheme) Color(0xFFA89968).copy(alpha = 0.7f)
                                       else LightColors.SecondaryText.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Clear button (when text is present)
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = if (darkTheme) Color(0xFFA89968) else LightColors.SecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // New button (embedded inside search bar)
            Surface(
                onClick = onNewClick,
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(15.dp),
                color = if (darkTheme) Color(0xFFE6B84A).copy(alpha = 0.15f) else InnovexiaColors.Gold.copy(alpha = 0.15f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create new persona",
                        tint = if (darkTheme) Color(0xFFE6B84A) else InnovexiaColors.Gold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
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
