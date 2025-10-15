package com.example.innovexia.ui.sheets.personas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.core.persona.*
import com.example.innovexia.ui.theme.InnovexiaTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persona 2.0 Create/Edit Dialog with tabs:
 * Identity, Behavior, System, Memory, Sources, Tools, Limits, Testing, Share
 */
@Composable
fun CreatePersonaDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreate: (name: String, color: Long, summary: String, tags: List<String>) -> Unit,
    modifier: Modifier = Modifier,
    editPersona: com.example.innovexia.core.persona.Persona? = null,
    editPersonaId: String? = null,
    editUid: String? = null,
    viewModel: CreatePersonaViewModel = viewModel(key = "create_persona_${editPersona?.id ?: editPersonaId ?: "new"}")
) {
    if (!visible) return

    val draft by viewModel.draft.collectAsState()
    val errors by viewModel.errors.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showUnsavedDialog by remember { mutableStateOf(false) }

    // Load for edit if persona provided, otherwise reset
    LaunchedEffect(editPersona) {
        if (editPersona != null) {
            // Deserialize extended settings if present
            val extendedMap = try {
                editPersona.extendedSettings?.let { json ->
                    Gson().fromJson<Map<String, Any>>(json, object : TypeToken<Map<String, Any>>() {}.type)
                }
            } catch (e: Exception) {
                null
            }

            // Convert Persona to PersonaDraftDto with extended settings
            val draftFromPersona = PersonaDraftDto(
                name = editPersona.name,
                initial = editPersona.initial,
                color = editPersona.color,
                bio = editPersona.summary,
                tags = editPersona.tags,
                isDefault = editPersona.isDefault,
                system = PersonaSystem(
                    instructions = editPersona.system ?: ""
                ),
                // Deserialize extended settings or use defaults
                behavior = extendedMap?.get("behavior")?.let { Gson().fromJson(Gson().toJson(it), PersonaBehavior::class.java) } ?: PersonaBehavior(),
                memory = extendedMap?.get("memory")?.let { Gson().fromJson(Gson().toJson(it), PersonaMemory::class.java) } ?: PersonaMemory(),
                sources = extendedMap?.get("sources")?.let { Gson().fromJson(Gson().toJson(it), PersonaSources::class.java) } ?: PersonaSources(),
                tools = extendedMap?.get("tools")?.let { Gson().fromJson(Gson().toJson(it), PersonaTools::class.java) } ?: PersonaTools(),
                limits = extendedMap?.get("limits")?.let { Gson().fromJson(Gson().toJson(it), PersonaLimits::class.java) } ?: PersonaLimits(),
                testing = extendedMap?.get("testing")?.let { Gson().fromJson(Gson().toJson(it), PersonaTesting::class.java) } ?: PersonaTesting(),
                defaultLanguage = extendedMap?.get("defaultLanguage") as? String ?: "en-US",
                greeting = extendedMap?.get("greeting") as? String ?: "",
                visibility = extendedMap?.get("visibility") as? String ?: "private",
                status = extendedMap?.get("status") as? String ?: "draft"
            )
            // Load the draft into the ViewModel
            viewModel.loadFromPersona(editPersona.id, draftFromPersona)
        } else if (editPersonaId != null && editUid != null) {
            viewModel.loadForEdit(editUid, editPersonaId)
        } else {
            viewModel.reset()
        }
    }

    // Unsaved changes dialog
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.discardChanges()
                    showUnsavedDialog = false
                    onDismiss()
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = {
            if (hasChanges) {
                showUnsavedDialog = true
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !hasChanges,
            dismissOnClickOutside = !hasChanges
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = InnovexiaTheme.colors.personaCardBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                DialogHeader(
                    title = if (editPersonaId != null) "Edit Persona" else "Create Persona",
                    onClose = {
                        if (hasChanges) {
                            showUnsavedDialog = true
                        } else {
                            onDismiss()
                        }
                    }
                )

                // Simple layout with tabs
                Column(modifier = Modifier.weight(1f)) {
                    TabRow(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                    TabContent(
                        selectedTab = selectedTab,
                        draft = draft,
                        errors = errors,
                        onUpdate = { viewModel.updateDraft(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Footer actions
                DialogFooter(
                    busy = busy,
                    hasChanges = hasChanges,
                    isValid = errors.isEmpty() && draft.name.isNotBlank(),
                    onDiscard = {
                        if (hasChanges) {
                            showUnsavedDialog = true
                        } else {
                            onDismiss()
                        }
                    },
                    onSaveDraft = {
                        viewModel.saveDraft()
                        // Keep dialog open for further editing
                    },
                    onPublish = {
                        viewModel.publish()
                        // Don't call onCreate - publish() already saves the persona
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun DialogHeader(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun TabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        "Identity", "Behavior", "System", "Memory", "Sources"
    )

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 16.dp,
        containerColor = InnovexiaTheme.colors.personaCardBg,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {
            HorizontalDivider(
                color = InnovexiaTheme.colors.personaMutedText.copy(alpha = 0.1f)
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = InnovexiaTheme.colors.personaMutedText,
                text = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: Int,
    draft: PersonaDraftDto,
    errors: Map<String, String>,
    onUpdate: ((PersonaDraftDto) -> PersonaDraftDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> IdentityTab(draft, errors, onUpdate)
            1 -> BehaviorTab(draft, onUpdate)
            2 -> SystemTab(draft, onUpdate)
            3 -> MemoryTab(draft, onUpdate)
            4 -> SourcesTab(draft, onUpdate)
        }
    }
}

// ============================================================
// TAB 1: IDENTITY
// ============================================================

@Composable
private fun IdentityTab(
    draft: PersonaDraftDto,
    errors: Map<String, String>,
    onUpdate: ((PersonaDraftDto) -> PersonaDraftDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { if (it.length <= 40) onUpdate { d -> d.copy(name = it) } },
                label = { Text("Name *") },
                supportingText = {
                    Text(
                        if (errors.containsKey("name")) errors["name"]!!
                        else "${draft.name.length}/40"
                    )
                },
                isError = errors.containsKey("name"),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = draft.initial,
                    onValueChange = { if (it.length <= 1) onUpdate { d -> d.copy(initial = it.uppercase()) } },
                    label = { Text("Initial") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Color",
                        style = MaterialTheme.typography.labelMedium,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(colorSwatches) { color ->
                            ColorSwatch(
                                color = color,
                                selected = color == draft.color,
                                onClick = { onUpdate { d -> d.copy(color = color) } }
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = draft.bio,
                onValueChange = { if (it.length <= 140) onUpdate { d -> d.copy(bio = it) } },
                label = { Text("Bio") },
                supportingText = { Text("${draft.bio.length}/140") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 3
            )
        }

        item {
            OutlinedTextField(
                value = draft.greeting,
                onValueChange = { onUpdate { d -> d.copy(greeting = it) } },
                label = { Text("Greeting") },
                placeholder = { Text("How can I help you today?") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            var tagInput by remember { mutableStateOf("") }

            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                label = { Text("Tags (press Enter to add)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (tagInput.isNotBlank() && draft.tags.size < 6) {
                        TextButton(onClick = {
                            onUpdate { d -> d.copy(tags = d.tags + tagInput.trim()) }
                            tagInput = ""
                        }) {
                            Text("Add")
                        }
                    }
                }
            )

            if (draft.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(draft.tags) { tag ->
                        TagChip(
                            tag = tag,
                            onRemove = { onUpdate { d -> d.copy(tags = d.tags - tag) } }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set as default for new chats")
                Switch(
                    checked = draft.isDefault,
                    onCheckedChange = { onUpdate { d -> d.copy(isDefault = it) } }
                )
            }
        }
    }
}

// ============================================================
// TAB 2: BEHAVIOR
// ============================================================

@Composable
private fun BehaviorTab(
    draft: PersonaDraftDto,
    onUpdate: ((PersonaDraftDto) -> PersonaDraftDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "About Behavior Settings",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "These settings control how your persona communicates and generates responses. Adjust them to match the desired personality and output style.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                }
            }
        }

        item {
            Text(
                text = "Personality Traits",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            SliderFieldWithHelp(
                label = "Conciseness",
                value = draft.behavior.conciseness,
                onValueChange = { onUpdate { d -> d.copy(behavior = d.behavior.copy(conciseness = it)) } },
                valueRange = 0f..1f,
                helpText = "Controls response length. Low = detailed explanations, High = brief answers.",
                exampleLow = "Let me explain in detail...",
                exampleHigh = "TL;DR: The answer is X."
            )
        }

        item {
            SliderFieldWithHelp(
                label = "Formality",
                value = draft.behavior.formality,
                onValueChange = { onUpdate { d -> d.copy(behavior = d.behavior.copy(formality = it)) } },
                valueRange = 0f..1f,
                helpText = "Controls language style. Low = casual/friendly, High = formal/professional.",
                exampleLow = "Hey! Here's what I found...",
                exampleHigh = "I have identified the following information..."
            )
        }

        item {
            SliderFieldWithHelp(
                label = "Empathy",
                value = draft.behavior.empathy,
                onValueChange = { onUpdate { d -> d.copy(behavior = d.behavior.copy(empathy = it)) } },
                valueRange = 0f..1f,
                helpText = "Controls emotional tone. Low = factual, High = warm and supportive.",
                exampleLow = "The data shows X.",
                exampleHigh = "I understand this might be frustrating. Let me help..."
            )
        }

        item {
            Divider()
        }

        item {
            Text(
                text = "Model Parameters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            SliderFieldWithHelp(
                label = "Creativity (Temperature)",
                value = draft.behavior.creativityTemp,
                onValueChange = { onUpdate { d -> d.copy(behavior = d.behavior.copy(creativityTemp = it)) } },
                valueRange = 0f..1f,
                helpText = "Controls response randomness. Low = predictable/factual, High = creative/varied.",
                exampleLow = "Standard approach",
                exampleHigh = "Novel and creative approach"
            )
        }

        item {
            SliderFieldWithHelp(
                label = "Top-p (Nucleus Sampling)",
                value = draft.behavior.topP,
                onValueChange = { onUpdate { d -> d.copy(behavior = d.behavior.copy(topP = it)) } },
                valueRange = 0f..1f,
                helpText = "Controls word choice diversity. Low = focused vocabulary, High = varied vocabulary.",
                exampleLow = "0.5 = Top 50% of likely words",
                exampleHigh = "0.95 = Top 95% of likely words"
            )
        }

        item {
            Divider()
        }

        item {
            DropdownFieldWithHelp(
                label = "Thinking Depth",
                value = draft.behavior.thinkingDepth,
                options = listOf("off", "balanced", "deep"),
                onValueChange = { onUpdate { d -> d.copy(behavior = d.behavior.copy(thinkingDepth = it)) } },
                helpText = mapOf(
                    "off" to "No step-by-step reasoning",
                    "balanced" to "Show reasoning when helpful",
                    "deep" to "Always show detailed reasoning"
                )
            )
        }

        item {
            DropdownFieldWithHelp(
                label = "Proactivity",
                value = draft.behavior.proactivity,
                options = listOf("ask_when_unclear", "proactive", "reactive"),
                onValueChange = { onUpdate { d -> d.copy(behavior = d.behavior.copy(proactivity = it)) } },
                helpText = mapOf(
                    "ask_when_unclear" to "Ask for clarification when needed",
                    "proactive" to "Offer suggestions and additional info",
                    "reactive" to "Only answer what's asked"
                )
            )
        }

        item {
            DropdownFieldWithHelp(
                label = "Citation Policy",
                value = draft.behavior.citationPolicy,
                options = listOf("when_uncertain", "always", "never"),
                onValueChange = { onUpdate { d -> d.copy(behavior = d.behavior.copy(citationPolicy = it)) } },
                helpText = mapOf(
                    "when_uncertain" to "Cite sources for uncertain claims",
                    "always" to "Always provide source references",
                    "never" to "Don't include citations"
                )
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Self-Check",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Review output before sending for accuracy",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                }
                Switch(
                    checked = draft.behavior.selfCheck.enabled,
                    onCheckedChange = {
                        onUpdate { d ->
                            d.copy(behavior = d.behavior.copy(selfCheck = d.behavior.selfCheck.copy(enabled = it)))
                        }
                    }
                )
            }
        }
    }
}

// ============================================================
// TAB 3: SYSTEM PROMPT
// ============================================================

@Composable
private fun SystemTab(
    draft: PersonaDraftDto,
    onUpdate: ((PersonaDraftDto) -> PersonaDraftDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "System Instructions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            OutlinedTextField(
                value = draft.system.instructions,
                onValueChange = { onUpdate { d -> d.copy(system = d.system.copy(instructions = it)) } },
                label = { Text("Instructions") },
                placeholder = { Text("You are a helpful assistant who...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Available Variables",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Use these variables in your instructions - they'll be replaced with actual values:",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(draft.system.variables) { variable ->
                            VariableChip(variable)
                        }
                    }
                }
            }
        }

        item {
            Divider()
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rules",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = {
                        onUpdate { d ->
                            d.copy(system = d.system.copy(rules = d.system.rules + SystemRule()))
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Rule")
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Rules are conditional behaviors. Example: 'When: always, Do: Respond in bullet points'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        items(draft.system.rules.size) { index ->
            RuleCard(
                rule = draft.system.rules[index],
                onUpdate = { updated ->
                    onUpdate { d ->
                        val newRules = d.system.rules.toMutableList()
                        newRules[index] = updated
                        d.copy(system = d.system.copy(rules = newRules))
                    }
                },
                onRemove = {
                    onUpdate { d ->
                        d.copy(system = d.system.copy(rules = d.system.rules - draft.system.rules[index]))
                    }
                }
            )
        }
    }
}

@Composable
private fun RuleCard(
    rule: SystemRule,
    onUpdate: (SystemRule) -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rule",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                }
            }

            OutlinedTextField(
                value = rule.`when`,
                onValueChange = { onUpdate(rule.copy(`when` = it)) },
                label = { Text("When") },
                placeholder = { Text("always, on_request, etc.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = rule.`do`,
                onValueChange = { onUpdate(rule.copy(`do` = it)) },
                label = { Text("Do") },
                placeholder = { Text("Cite sources") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }
}

// ============================================================
// TAB 4: MEMORY
// ============================================================

@Composable
private fun MemoryTab(
    draft: PersonaDraftDto,
    onUpdate: ((PersonaDraftDto) -> PersonaDraftDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Memory System",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "About Memory",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "When enabled, this persona will remember past conversations and context across chats. This helps maintain continuity and personalization.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Enable Memory",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Remember conversations and user preferences",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                }
                Switch(
                    checked = draft.memory.enabled,
                    onCheckedChange = {
                        onUpdate { d -> d.copy(memory = d.memory.copy(enabled = it)) }
                    }
                )
            }
        }
    }
}

// ============================================================
// TAB 5: SOURCES
// ============================================================

@Composable
private fun SourcesTab(
    draft: PersonaDraftDto,
    onUpdate: ((PersonaDraftDto) -> PersonaDraftDto) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Sources System",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "About Sources",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "When enabled, this persona can access uploaded documents (PDFs, text files) to answer questions based on your content. Sources are retrieved using semantic search.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Enable Sources",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Access uploaded documents for context",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaTheme.colors.personaMutedText
                    )
                }
                Switch(
                    checked = draft.sources.enabled,
                    onCheckedChange = {
                        onUpdate { d -> d.copy(sources = d.sources.copy(enabled = it)) }
                    }
                )
            }
        }
    }
}

// ============================================================
// FOOTER ACTIONS
// ============================================================

@Composable
private fun DialogFooter(
    busy: Boolean,
    hasChanges: Boolean,
    isValid: Boolean,
    onDiscard: () -> Unit,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = InnovexiaTheme.colors.personaCardBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
            TextButton(
                onClick = onDiscard,
                enabled = !busy,
                modifier = Modifier.height(40.dp)
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Draft button
            OutlinedButton(
                onClick = onSaveDraft,
                enabled = isValid && !busy,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    "Draft",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // Save button
            Button(
                onClick = onPublish,
                enabled = isValid && !busy,
                modifier = Modifier.height(40.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ============================================================
// HELPER COMPONENTS
// ============================================================

@Composable
private fun ColorSwatch(
    color: Long,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(color))
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier.border(1.dp, Color(0xFF334155), CircleShape)
                }
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun TagChip(
    tag: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun VariableChip(variable: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    ) {
        Text(
            text = variable,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SliderField(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.labelSmall,
                color = InnovexiaTheme.colors.personaMutedText
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun SliderFieldWithHelp(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    helpText: String,
    exampleLow: String,
    exampleHigh: String,
    modifier: Modifier = Modifier
) {
    var showHelp by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, style = MaterialTheme.typography.labelMedium)
                IconButton(
                    onClick = { showHelp = !showHelp },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Help",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.labelSmall,
                color = InnovexiaTheme.colors.personaMutedText
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )

        if (showHelp) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = helpText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Low: $exampleLow",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "High: $exampleHigh",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DropdownFieldWithHelp(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    helpText: Map<String, String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                label = { Text(label) },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Show help text for current selection
        helpText[value]?.let { help ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = help,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// Color swatches
private val colorSwatches = listOf(
    0xFF60A5FA, // Blue
    0xFF34D399, // Green
    0xFFF472B6, // Pink
    0xFFFBBF24, // Yellow
    0xFFA78BFA, // Purple
    0xFFEF4444, // Red
    0xFF06B6D4, // Cyan
    0xFFF59E0B  // Orange
)
