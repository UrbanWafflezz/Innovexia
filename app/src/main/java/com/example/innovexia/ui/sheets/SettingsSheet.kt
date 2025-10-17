package com.example.innovexia.ui.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.ui.auth.ImportGuestChatsDialog
import com.example.innovexia.ui.components.SystemHealthTab
import com.example.innovexia.ui.models.SettingsPrefs
import com.example.innovexia.ui.models.ThemeMode
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors
import com.example.innovexia.ui.theme.getBackgroundGradient
import androidx.compose.ui.draw.clip
import com.example.innovexia.ui.viewmodels.AuthViewModel
import com.example.innovexia.ui.viewmodels.SettingsTab
import com.example.innovexia.ui.viewmodels.SettingsViewModel
import com.example.innovexia.ui.viewmodels.SystemHealthViewModel
import com.example.innovexia.ui.viewmodels.SystemHealthViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Settings Dialog with proper scrolling behavior
 * - Dialog has bounded height
 * - Header + tabs are fixed
 * - Only tab body scrolls (LazyColumn)
 */
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    prefs: SettingsPrefs,
    onPrefsChange: (SettingsPrefs) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    onDeleteHistory: () -> Unit = {},
    consentedSaveHistory: Boolean? = null,
    viewModel: SettingsViewModel = viewModel()
) {
    // Check if in guest mode
    val user = FirebaseAuthManager.currentUser()
    val isGuest = user == null

    // Filter tabs based on guest mode
    val allTabs = if (isGuest) {
        listOf(SettingsTab.Account)
    } else {
        listOf(
            SettingsTab.Account,
            SettingsTab.Privacy,
            SettingsTab.Notifications,
            SettingsTab.SystemHealth
        )
    }

    Dialog(
        onDismissRequest = {}, // Only X button can close
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // Outer box with gradient background (matches side menu)
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.88f) // Bounded height
                .clip(RoundedCornerShape(20.dp))
                .background(brush = getBackgroundGradient(darkTheme))
                .border(
                    1.dp,
                    if (darkTheme) Color(0xFF253041).copy(alpha = 0.6f) else Color(0xFFE7EDF5).copy(alpha = 0.6f),
                    RoundedCornerShape(20.dp)
                )
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Layout: header + tabs fixed; body scrolls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ---- Header (fixed) ----
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Settings",
                        color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ---- Tabs (fixed) ----
                SettingsTabs(
                    tabs = allTabs,
                    selected = viewModel.tab.collectAsState().value,
                    onSelect = viewModel::setTab,
                    darkTheme = darkTheme
                )

                HorizontalDivider(
                    Modifier.padding(top = 8.dp, bottom = 8.dp),
                    color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f)
                    else LightColors.SecondaryText.copy(alpha = 0.2f)
                )

                // ---- Scrollable body (the ONLY scroller) ----
                Box(Modifier.weight(1f)) {
                    when (viewModel.tab.collectAsState().value) {
                        SettingsTab.Account -> {
                            val context = LocalContext.current
                            val app = context.applicationContext as InnovexiaApplication
                            val authViewModel: AuthViewModel = viewModel { AuthViewModel(app) }
                            AccountTabBody(
                                darkTheme,
                                onDeleteHistory,
                                consentedSaveHistory,
                                themeMode,
                                onThemeChange,
                                authViewModel
                            )
                        }
                        SettingsTab.Privacy -> PrivacyTabBody(darkTheme, prefs, onPrefsChange)
                        SettingsTab.Notifications -> NotificationsTabBody(darkTheme)
                        SettingsTab.SystemHealth -> SystemHealthTabWrapper(darkTheme)
                        else -> {} // Handle any remaining tabs
                    }
                }
            }
        }
    }
}

/**
 * Tabs row (fixed at top)
 */
@Composable
private fun SettingsTabs(
    tabs: List<SettingsTab>,
    selected: SettingsTab,
    onSelect: (SettingsTab) -> Unit,
    darkTheme: Boolean
) {
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
                        tab.title,
                        color = if (tab == selected) {
                            if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                        } else {
                            if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        },
                        fontWeight = if (tab == selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════
// TAB BODIES (Each uses LazyColumn for scrolling)
// ═════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTabBody(
    darkTheme: Boolean,
    onDeleteHistory: () -> Unit,
    consentedSaveHistory: Boolean?,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    authViewModel: AuthViewModel
) {
    val user = FirebaseAuthManager.currentUser()
    val context = LocalContext.current
    val app = context.applicationContext as InnovexiaApplication
    val scope = rememberCoroutineScope()

    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var guestChatCount by rememberSaveable { mutableIntStateOf(0) }
    var showRestoreSheet by rememberSaveable { mutableStateOf(false) }
    var localHistoryEnabled by rememberSaveable { mutableStateOf(consentedSaveHistory ?: false) }

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (user != null) {
            item {
                SettingsItem(
                    title = "Display Name",
                    subtitle = user.displayName ?: "Not set",
                    darkTheme = darkTheme,
                    onClick = { /* TODO: Edit name */ }
                )
            }

            item {
                SettingsItem(
                    title = "Email",
                    subtitle = user.email ?: "Not set",
                    darkTheme = darkTheme,
                    onClick = { /* TODO: Edit email */ }
                )
            }

            // Import Guest chats option
            item {
                LaunchedEffect(Unit) {
                    val count = app.database.chatDao().countGuestChats()
                    guestChatCount = count
                }
            }

            if (guestChatCount > 0) {
                item {
                    SettingsItem(
                        title = "Import Guest chats",
                        subtitle = "$guestChatCount chat${if (guestChatCount > 1) "s" else ""} available",
                        darkTheme = darkTheme,
                        onClick = { showImportDialog = true }
                    )
                }
            }

            item {
                SettingsItem(
                    title = "Restore from Cloud",
                    subtitle = "Bring back deleted or synced chats",
                    darkTheme = darkTheme,
                    onClick = { showRestoreSheet = true }
                )
            }
        } else {
            item {
                SettingsItem(
                    title = "Account",
                    subtitle = "Guest mode · Local only",
                    darkTheme = darkTheme,
                    onClick = { /* TODO: Navigate to sign in */ }
                )
            }

            // Guest mode info
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (darkTheme)
                                DarkColors.AccentBlue.copy(alpha = 0.1f)
                            else
                                LightColors.AccentBlue.copy(alpha = 0.1f)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Sign in to unlock more features",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (darkTheme) DarkColors.AccentBlue else LightColors.AccentBlue
                        )
                        Text(
                            text = "• Cloud Sync\n• AI Settings\n• Privacy Controls\n• System Health",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        )
                    }
                }
            }
        }

        // Theme section
        item {
            SettingsItem(
                title = "Theme",
                subtitle = when (themeMode) {
                    ThemeMode.Light -> "Light"
                    ThemeMode.Dark -> "Dark"
                    ThemeMode.System -> "Auto"
                },
                darkTheme = darkTheme,
                onClick = {
                    onThemeChange(
                        when (themeMode) {
                            ThemeMode.Light -> ThemeMode.Dark
                            ThemeMode.Dark -> ThemeMode.System
                            ThemeMode.System -> ThemeMode.Light
                        }
                    )
                }
            )
        }

        item {
            SettingsSwitchItem(
                title = "Allow saving on this device",
                subtitle = "Local history storage",
                checked = localHistoryEnabled,
                onCheckedChange = { localHistoryEnabled = it },
                darkTheme = darkTheme
            )
        }

        // Delete history button (danger)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (darkTheme) Color(0xFF141A22) else Color.White)
                    .border(
                        1.dp,
                        Color(0xFFEF4444).copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(role = Role.Button, onClick = onDeleteHistory)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Delete all local history",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = Color(0xFFEF4444)
                    )
                    Text(
                        text = "This cannot be undone",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                }
            }
        }
    }

    // Restore from Cloud sheet
    if (showRestoreSheet) {
        CloudRestoreSheet(
            onDismiss = { showRestoreSheet = false },
            darkTheme = darkTheme
        )
    }

    // Import Guest chats dialog
    if (showImportDialog && guestChatCount > 0) {
        ImportGuestChatsDialog(
            guestChatCount = guestChatCount,
            onConfirm = {
                scope.launch {
                    authViewModel.mergeGuestChats()
                    guestChatCount = 0
                }
            },
            onDismiss = { showImportDialog = false }
        )
    }
}

@Composable
private fun AiTabBody(darkTheme: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext as InnovexiaApplication

    val selectedModelId by app.userPreferences.selectedModel.collectAsState(initial = "gemini-2.5-flash")
    val temperature by app.userPreferences.temperature.collectAsState(initial = 0.7f)
    val maxOutputTokens by app.userPreferences.maxOutputTokens.collectAsState(initial = 2048)
    val safetyLevel by app.userPreferences.safetyLevel.collectAsState(initial = "Standard")

    var reasoningMode by rememberSaveable { mutableStateOf(false) }
    var webTools by rememberSaveable { mutableStateOf(false) }
    var memoryEnabledByDefault by rememberSaveable { mutableStateOf(true) }
    var autoSummarize by rememberSaveable { mutableStateOf(true) }
    var summarizeThreshold by rememberSaveable { mutableFloatStateOf(1800f) }
    var keepRecentTurns by rememberSaveable { mutableFloatStateOf(12f) }

    // Available models list
    val availableModels = listOf(
        "gemini-2.5-flash" to "Gemini 2.5 Flash",
        "gemini-2.0-flash-exp" to "Gemini 2.0 Flash",
        "gemini-1.5-pro" to "Gemini 1.5 Pro",
        "gemini-1.5-flash" to "Gemini 1.5 Flash",
        "gemma3-1b-int4" to "Gemma 3 1B (INT4)",
        "gemma3-1b-int8" to "Gemma 3 1B (INT8)",
        "tinyllama-1.1b-chat-q8" to "TinyLlama 1.1B",
        "flan-t5-small-int8" to "FLAN-T5 Small"
    )

    val currentModelIndex = availableModels.indexOfFirst { it.first == selectedModelId }.coerceAtLeast(0)
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsItem(
                title = "Default model",
                subtitle = availableModels[currentModelIndex].second,
                darkTheme = darkTheme,
                onClick = {
                    val nextIndex = (currentModelIndex + 1) % availableModels.size
                    scope.launch {
                        app.userPreferences.setSelectedModel(availableModels[nextIndex].first)
                    }
                }
            )
        }

        item {
            SettingsSwitchItem(
                title = "Reasoning/Thinking mode",
                subtitle = "Extended chain-of-thought",
                checked = reasoningMode,
                onCheckedChange = { reasoningMode = it },
                darkTheme = darkTheme
            )
        }

        item {
            SettingsSwitchItem(
                title = "Web tools / grounding",
                subtitle = "Access to real-time data",
                checked = webTools,
                onCheckedChange = { webTools = it },
                darkTheme = darkTheme
            )
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f)
                else LightColors.SecondaryText.copy(alpha = 0.2f)
            )
        }

        // Memory section
        item {
            Text(
                text = "Contextual Memory",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
            )
        }

        item {
            SettingsSwitchItem(
                title = "Memory On by default",
                subtitle = "Enable memory for new chats",
                checked = memoryEnabledByDefault,
                onCheckedChange = { memoryEnabledByDefault = it },
                darkTheme = darkTheme
            )
        }

        item {
            SettingsSwitchItem(
                title = "Auto-summarize older messages",
                subtitle = "Compress history to save tokens",
                checked = autoSummarize,
                onCheckedChange = { autoSummarize = it },
                darkTheme = darkTheme
            )
        }

        // Summarize threshold slider
        item {
            Column {
                Text(
                    text = "Summarize threshold: ${summarizeThreshold.toInt()} tokens",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                )
                Text(
                    text = "Trigger summary when older content exceeds this",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = summarizeThreshold,
                    onValueChange = { summarizeThreshold = it },
                    valueRange = 500f..4000f,
                    steps = 6,
                    enabled = autoSummarize,
                    colors = SliderDefaults.colors(
                        thumbColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                        activeTrackColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                        inactiveTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent,
                        disabledThumbColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                        disabledActiveTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                        disabledInactiveTrackColor = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    )
                )
            }
        }

        // Keep recent turns slider
        item {
            Column {
                Text(
                    text = "Keep last ${keepRecentTurns.toInt()} turns in prompt",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                )
                Text(
                    text = "Recent messages always included verbatim",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = keepRecentTurns,
                    onValueChange = { keepRecentTurns = it },
                    valueRange = 6f..24f,
                    steps = 5,
                    colors = SliderDefaults.colors(
                        thumbColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                        activeTrackColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                        inactiveTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
            }
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f)
                else LightColors.SecondaryText.copy(alpha = 0.2f)
            )
        }

        // Max output tokens slider
        item {
            Column {
                Text(
                    text = "Max output tokens: $maxOutputTokens",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = maxOutputTokens.toFloat(),
                    onValueChange = {
                        scope.launch {
                            app.userPreferences.setMaxOutputTokens(it.toInt())
                        }
                    },
                    valueRange = 512f..8192f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                        activeTrackColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                        inactiveTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
private fun PrivacyTabBody(
    darkTheme: Boolean,
    prefs: SettingsPrefs,
    onPrefsChange: (SettingsPrefs) -> Unit
) {
    var crashReports by rememberSaveable { mutableStateOf(true) }

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSwitchItem(
                title = "Hide sensitive previews",
                subtitle = "Blur message previews",
                checked = prefs.hideSensitivePreviews,
                onCheckedChange = { onPrefsChange(prefs.copy(hideSensitivePreviews = it)) },
                darkTheme = darkTheme
            )
        }

        item {
            SettingsSwitchItem(
                title = "Typing indicator",
                subtitle = "Show when you're typing",
                checked = prefs.typingIndicator,
                onCheckedChange = { onPrefsChange(prefs.copy(typingIndicator = it)) },
                darkTheme = darkTheme
            )
        }

        item {
            SettingsSwitchItem(
                title = "Share crash reports",
                subtitle = "Help improve Innovexia",
                checked = crashReports,
                onCheckedChange = { crashReports = it },
                darkTheme = darkTheme
            )
        }
    }
}

@Composable
private fun NotificationsTabBody(darkTheme: Boolean) {
    var newMessage by rememberSaveable { mutableStateOf(true) }
    var reminders by rememberSaveable { mutableStateOf(false) }
    var quietHours by rememberSaveable { mutableStateOf("Off") }

    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsSwitchItem(
                title = "New message",
                subtitle = "Notify on new messages",
                checked = newMessage,
                onCheckedChange = { newMessage = it },
                darkTheme = darkTheme
            )
        }

        item {
            SettingsSwitchItem(
                title = "Reminders",
                subtitle = "Scheduled reminders",
                checked = reminders,
                onCheckedChange = { reminders = it },
                darkTheme = darkTheme
            )
        }

        item {
            SettingsItem(
                title = "Quiet hours",
                subtitle = quietHours,
                darkTheme = darkTheme,
                onClick = { quietHours = if (quietHours == "Off") "10pm - 8am" else "Off" }
            )
        }
    }
}

@Composable
private fun HelpTabBody(darkTheme: Boolean) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SettingsItem(
                title = "Frequently asked questions",
                subtitle = "Common questions & answers",
                darkTheme = darkTheme,
                onClick = { /* TODO: Open FAQs */ }
            )
        }

        item {
            SettingsItem(
                title = "Contact support",
                subtitle = "Get help from our team",
                darkTheme = darkTheme,
                onClick = { /* TODO: Contact */ },
                enabled = false
            )
        }

        item {
            SettingsItem(
                title = "About Innovexia",
                subtitle = "v1.0",
                darkTheme = darkTheme,
                onClick = { /* TODO: About dialog */ }
            )
        }
    }
}

@Composable
private fun SystemHealthTabWrapper(darkTheme: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext as InnovexiaApplication

    val viewModel: SystemHealthViewModel = viewModel(
        factory = SystemHealthViewModelFactory(
            healthApi = app.healthApi,
            database = app.database
        )
    )

    val checks by viewModel.checks.collectAsState()
    val overallState by viewModel.overallState.collectAsState()
    val openIncidents by viewModel.openIncidents.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lastRefresh by viewModel.lastRefresh.collectAsState()
    val isConnected by app.connectivity.isConnected.collectAsState()

    // Start foreground monitoring
    LaunchedEffect(Unit) {
        viewModel.startForegroundMonitoring()
    }

    // Perform initial refresh
    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    // SystemHealthTab already uses verticalScroll internally, so we keep it as-is
    // (It's not ideal, but changing it would require modifying SystemHealthTab.kt)
    SystemHealthTab(
        checks = checks,
        overallState = overallState,
        openIncidents = openIncidents,
        isRefreshing = isRefreshing,
        lastRefresh = lastRefresh,
        isConnected = isConnected,
        onRefresh = { viewModel.refreshAll() },
        onCheckService = { serviceId -> viewModel.checkService(serviceId) },
        darkTheme = darkTheme
    )
}

// ═════════════════════════════════════════════════════════════
// SHARED COMPONENTS
// ═════════════════════════════════════════════════════════════

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    darkTheme: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (darkTheme) Color(0xFF141A22) else Color.White)
            .border(
                1.dp,
                if (darkTheme) Color(0xFF253041) else Color(0xFFE7EDF5),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (enabled) {
                    if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                } else {
                    if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    darkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (darkTheme) Color(0xFF141A22) else Color.White)
            .border(
                1.dp,
                if (darkTheme) Color(0xFF253041) else Color(0xFFE7EDF5),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    checkedTrackColor = if (darkTheme) Color(0xFF60A5FA).copy(alpha = 0.5f) else Color(0xFF3B82F6).copy(alpha = 0.5f),
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                    uncheckedTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                    uncheckedBorderColor = Color.Transparent,
                    disabledCheckedThumbColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                    disabledCheckedTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                    disabledUncheckedThumbColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                    disabledUncheckedTrackColor = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                )
            )
        }
    }
}

// Backward compatibility: Keep old SettingsSheet name
@Deprecated(
    "Use SettingsDialog instead",
    ReplaceWith("SettingsDialog(onDismiss, themeMode, onThemeChange, prefs, onPrefsChange, modifier, darkTheme, onDeleteHistory, consentedSaveHistory)")
)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    prefs: SettingsPrefs,
    onPrefsChange: (SettingsPrefs) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    onDeleteHistory: () -> Unit = {},
    consentedSaveHistory: Boolean? = null
) {
    SettingsDialog(
        onDismiss = onDismiss,
        themeMode = themeMode,
        onThemeChange = onThemeChange,
        prefs = prefs,
        onPrefsChange = onPrefsChange,
        modifier = modifier,
        darkTheme = darkTheme,
        onDeleteHistory = onDeleteHistory,
        consentedSaveHistory = consentedSaveHistory
    )
}
