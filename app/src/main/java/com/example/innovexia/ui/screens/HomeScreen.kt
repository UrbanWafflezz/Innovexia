package com.example.innovexia.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.draw.blur
import com.example.innovexia.data.local.AppDatabase
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.data.local.entities.ChatEntity
import com.example.innovexia.ui.components.AttachmentStrip
import com.example.innovexia.ui.components.AttachmentToolbar
import com.example.innovexia.ui.components.ChatComposerV3
import com.example.innovexia.ui.components.WebSearchComposer
import com.example.innovexia.ui.components.ChevronButton
import com.example.innovexia.ui.components.EmptyChatGreeting
import com.example.innovexia.ui.chat.newchat.SmartGreetingScreen
import com.example.innovexia.ui.chat.ChatHeader
import com.example.innovexia.ui.theme.InnovexiaTokens
import com.example.innovexia.ui.components.GradientScaffold
import com.example.innovexia.ui.components.SideMenu
import com.example.innovexia.ui.models.Persona
import com.example.innovexia.ui.models.RecentChat
import com.example.innovexia.ui.models.SettingsPrefs
import com.example.innovexia.ui.models.ThemeMode
import com.example.innovexia.ui.models.demoPersonas
import com.example.innovexia.ui.sheets.HistoryConsentSheet
import com.example.innovexia.ui.sheets.PersonaSheet
import com.example.innovexia.ui.dialogs.SubscriptionDialog
import com.example.innovexia.ui.dialogs.UsageDialog
import com.example.innovexia.ui.sheets.ProfileSheet
import com.example.innovexia.ui.sheets.SettingsSheet
import com.example.innovexia.ui.sheets.ToolsPanelSheet
import com.example.innovexia.ui.subscriptions.SubscriptionsScreen
import com.example.innovexia.ui.subscriptions.BillingPeriod
import com.example.innovexia.subscriptions.mock.*
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors
import com.example.innovexia.ui.viewmodels.HomeViewModel
import com.example.innovexia.ui.viewmodels.HomeViewModelFactory
import com.example.innovexia.ui.auth.MergeGuestChatsDialog
import com.example.innovexia.ui.viewmodels.AuthViewModel
import com.example.innovexia.ui.sheets.profile.ProfileViewModel
import com.example.innovexia.ui.sheets.CloudRestoreSheet
import com.example.innovexia.data.preferences.UserPreferences
import com.example.innovexia.core.sync.CloudSyncDetector
import com.example.innovexia.core.auth.FirebaseAuthManager
import com.example.innovexia.core.permissions.PermissionHelper
import com.example.innovexia.core.permissions.rememberAudioPermissionLauncher
import com.example.innovexia.core.voice.VoiceInputManager
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as InnovexiaApplication
    val database = AppDatabase.getInstance(context)
    val personaRepository = com.example.innovexia.core.persona.PersonaRepository(database.personaDao())

    val subscriptionViewModel: com.example.innovexia.ui.viewmodels.SubscriptionViewModel = viewModel(
        factory = com.example.innovexia.ui.viewmodels.SubscriptionViewModelFactory(
            subscriptionRepository = app.subscriptionRepository,
            usageRepository = app.usageRepository
        )
    )

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            chatRepository = app.chatRepository,
            userPreferences = app.userPreferences,
            geminiService = app.geminiService,
            personaRepository = personaRepository,
            subscriptionViewModel = subscriptionViewModel
        )
    )

    // Set context for reading attachment URIs
    viewModel.setContext(context)

    val authViewModel: AuthViewModel = viewModel { AuthViewModel(app) }
    val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel(context) }
    val currentUser by profileViewModel.user.collectAsState()
    val signedIn by authViewModel.signedIn.collectAsState()
    val showMergeDialog by authViewModel.showMergeDialog.collectAsState()
    val guestChatCount by authViewModel.guestChatCount.collectAsState()

    // Entitlements ViewModel
    val entitlementsVM: EntitlementsVM = viewModel(
        factory = EntitlementsVMFactory(
            billingProvider = app.billingProvider,
            repo = app.entitlementsRepo
        )
    )
    val entitlement by entitlementsVM.entitlement.collectAsState()
    val caps by entitlementsVM.caps.collectAsState()

    // Usage ViewModel - Use Firebase-backed tracker for cross-device sync
    val firestore = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    val firebaseAuth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val usageTracker = remember { FirebaseUsageTracker(context, firestore, firebaseAuth) }

    // Usage Data Repository - Aggregates real usage counts from local databases
    val currentUserId = firebaseAuth.currentUser?.uid ?: "guest"
    val usageDataRepository = remember(currentUserId) {
        UsageDataRepository(context, currentUserId)
    }

    val usageVM = remember(usageTracker, entitlementsVM, usageDataRepository) {
        UsageVM(usageTracker, entitlementsVM, usageDataRepository)
    }

    // State management
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Force drawer to properly close on initial composition
    LaunchedEffect(Unit) {
        if (drawerState.currentValue == DrawerValue.Closed) {
            drawerState.snapTo(DrawerValue.Closed)
        }
    }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showUsage by rememberSaveable { mutableStateOf(false) }
    var showSubscription by rememberSaveable { mutableStateOf(false) }
    var showManageSubscription by rememberSaveable { mutableStateOf(false) }
    var showToolsPanel by rememberSaveable { mutableStateOf(false) }
    var showUpgradeSuccess by rememberSaveable { mutableStateOf(false) }
    var upgradedPlanName by rememberSaveable { mutableStateOf("") }
    var showPersonaSelector by rememberSaveable { mutableStateOf(false) }
    var showAttachmentToolbar by rememberSaveable { mutableStateOf(false) }
    var showConsentSheet by rememberSaveable { mutableStateOf(false) }
    var showCloudRestoreSheet by rememberSaveable { mutableStateOf(false) }
    var showRestorePrompt by rememberSaveable { mutableStateOf(false) }

    // Theme state (in-memory, no persistence)
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
    var settingsPrefs by rememberSaveable { mutableStateOf(SettingsPrefs()) }

    // Composer state
    var composerText by rememberSaveable { mutableStateOf("") }
    var selectedPersona by rememberSaveable { mutableStateOf<Persona?>(null) }
    var isIncognitoEnabled by rememberSaveable { mutableStateOf(false) }
    var showIncognitoDialog by rememberSaveable { mutableStateOf(false) }

    // Model switcher state (UI only)
    var showModelPanel by rememberSaveable { mutableStateOf(false) }

    // ViewModel state
    val consentedSaveHistory by viewModel.consentedSaveHistory.collectAsState()
    val recentChats by viewModel.recentChats.collectAsState()
    val archivedChats by viewModel.archivedChats.collectAsState()
    val deletedChats by viewModel.deletedChats.collectAsState()
    val activeChatId by viewModel.activeChatId.collectAsState()

    // AI preferences - sync with ViewModel's persisted preferences
    val selectedModelFromVM by viewModel.selectedModel.collectAsState()
    val temperatureFromVM by viewModel.temperature.collectAsState()
    val maxOutputTokensFromVM by viewModel.maxOutputTokens.collectAsState()
    val safetyLevelFromVM by viewModel.safetyLevel.collectAsState()
    val groundingEnabledFromVM by viewModel.groundingEnabled.collectAsState()
    val groundingDataMap by viewModel.groundingDataMap.collectAsState()
    val groundingStatusMap by viewModel.groundingStatusMap.collectAsState()

    // Create UiAiPrefs from ViewModel state (always reflects persisted preferences)
    val aiPrefs = com.example.innovexia.ui.components.UiAiPrefs(
        model = selectedModelFromVM,
        creativity = temperatureFromVM,
        maxOutputTokens = maxOutputTokensFromVM,
        safety = safetyLevelFromVM,
        groundingEnabled = groundingEnabledFromVM
    )

    val activeChatTitle by viewModel.displayTitle.collectAsState() // Use displayTitle for streaming animation
    val isStreaming by viewModel.isStreaming.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val visibleMessages by viewModel.visibleMessages.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val attachments by viewModel.attachments.collectAsState()

    // Cloud restore state - use singleton from Application
    val userPrefs = app.userPreferences
    val hideCloudRestoreButton by userPrefs.hideCloudRestoreButton.collectAsState(initial = false)
    val alwaysShowRestoreButton by userPrefs.alwaysShowRestoreButton.collectAsState(initial = false)
    val hasSeenRestorePrompt by userPrefs.hasSeenRestorePrompt.collectAsState(initial = false)
    var cloudChatCount by remember { mutableStateOf(0) }

    // Determine if cloud restore button should show
    val showCloudRestoreButton = signedIn && !hideCloudRestoreButton && (cloudChatCount > 0 || alwaysShowRestoreButton)

    // Photo picker (Android 13+)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.processAttachmentUris(uris, context.contentResolver)
        }
        showAttachmentToolbar = false
    }

    // File picker (PDFs and images)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.processAttachmentUris(uris, context.contentResolver)
        }
        showAttachmentToolbar = false
    }

    // Snackbar for errors
    val snackbarHostState = remember { SnackbarHostState() }

    // Load selected persona from preferences on startup
    LaunchedEffect(Unit) {
        val prefs = com.example.innovexia.core.persona.PersonaPreferences(context)
        val ownerId = com.example.innovexia.core.auth.ProfileId.current().toOwnerId()
        prefs.getActivePersonaId(ownerId).collect { personaId ->
            if (personaId != null) {
                val persona = personaRepository.getPersonaById(personaId)
                if (persona != null) {
                    // Convert core persona to UI persona
                    selectedPersona = Persona(
                        id = persona.id,
                        name = persona.name,
                        initial = persona.initial,
                        colorHex = persona.color
                    )
                } else {
                    // Persona was deleted, clear selection
                    selectedPersona = null
                }
            } else {
                // No active persona selected (user turned it off or never selected one)
                selectedPersona = null
            }
        }
    }

    // Show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Listen for successful subscription upgrades
    LaunchedEffect(Unit) {
        entitlementsVM.success.collect { message ->
            // Check if it's an upgrade/plan change message
            if (message.contains("updated", ignoreCase = true) ||
                message.contains("activated", ignoreCase = true)) {
                // Extract plan name from entitlement
                val currentPlan = entitlement.plan
                upgradedPlanName = when (currentPlan) {
                    "PLUS" -> "Plus"
                    "PRO" -> "Pro"
                    "MASTER" -> "Master"
                    else -> currentPlan
                }
                showUpgradeSuccess = true
            }
        }
    }

    // Track actual token counts from API
    val lastTokenCounts by viewModel.lastTokenCounts.collectAsState()
    var lastTrackedTokens by remember { mutableStateOf(0 to 0) }

    LaunchedEffect(lastTokenCounts) {
        // When we receive new token counts from API, update ONLY tokens (message already tracked on send)
        val (inputTokens, outputTokens) = lastTokenCounts
        val (lastInput, lastOutput) = lastTrackedTokens

        // Only update if we have new counts (both > 0) and they're different from last
        if (inputTokens > 0 && outputTokens > 0 &&
            (inputTokens != lastInput || outputTokens != lastOutput)) {

            android.util.Log.d("HomeScreen", "Updating API tokens - Input: $inputTokens, Output: $outputTokens")

            // Use updateTokens to add tokens WITHOUT incrementing message count
            usageVM.updateTokens(
                tokensIn = inputTokens.toLong(),
                tokensOut = outputTokens.toLong()
            )

            lastTrackedTokens = inputTokens to outputTokens
        }
    }

    // Focus manager for tap-to-dismiss
    val focusManager = LocalFocusManager.current

    // Derive actual dark theme from mode
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> systemDarkTheme
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    // Auto-close profile dialog after successful sign-in
    // Clear persona and memory when logging out
    LaunchedEffect(signedIn) {
        if (signedIn && showProfile) {
            // User just signed in while profile dialog was open for auth
            // Close it to return to chat page
            showProfile = false
        } else if (!signedIn) {
            // User logged out - clear persona immediately
            android.util.Log.d("HomeScreen", "🚫 User logged out - clearing persona and resetting to guest mode")
            selectedPersona = null
        }
    }

    // Reset usage tracker on auth state change (separate effect to track previous state)
    var previousSignedIn by rememberSaveable { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(signedIn) {
        // Only reset if auth state actually changed (not on first load)
        if (previousSignedIn != null && previousSignedIn != signedIn) {
            android.util.Log.d("HomeScreen", "Auth state changed from $previousSignedIn to $signedIn - resetting usage tracker")
            usageTracker.resetOnAuthChange()
        }
        previousSignedIn = signedIn
    }

    // Clear focus when drawer closes
    LaunchedEffect(drawerState.isClosed) {
        if (drawerState.isClosed) {
            focusManager.clearFocus()
        }
    }

    // Check for cloud restore prompt and fetch cloud chat count
    LaunchedEffect(signedIn) {
        if (signedIn) {
            val user = FirebaseAuthManager.currentUser()
            if (user != null) {
                // Fetch cloud chat count
                try {
                    cloudChatCount = CloudSyncDetector.getCloudChatCount(user.uid)
                } catch (e: Exception) {
                    android.util.Log.e("HomeScreen", "Failed to fetch cloud chat count", e)
                }

                // Check if should show restore prompt (fresh install scenario)
                if (!hasSeenRestorePrompt) {
                    try {
                        val shouldPrompt = CloudSyncDetector.shouldPromptRestore(context, userPrefs)
                        if (shouldPrompt) {
                            showRestorePrompt = true
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeScreen", "Failed to check restore prompt", e)
                    }
                }
            }
        }
    }

    // State to hold chat items with last messages
    var allChatItems by remember { mutableStateOf<List<com.example.innovexia.ui.models.ChatListItem>>(emptyList()) }

    // Load last messages for all chats
    LaunchedEffect(recentChats, archivedChats, deletedChats) {
        val items = mutableListOf<com.example.innovexia.ui.models.ChatListItem>()

        recentChats.forEach { chat ->
            val lastMsg = viewModel.getLastMessagePreview(chat.id)
            items.add(
                com.example.innovexia.ui.models.ChatListItem(
                    chatId = chat.id,
                    title = chat.title,
                    lastMessage = lastMsg ?: "No messages yet",
                    updatedAt = chat.updatedAt,
                    personaInitials = listOfNotNull(chat.personaInitial),
                    state = com.example.innovexia.ui.models.ChatState.ACTIVE,
                    pinned = chat.pinned,
                    isIncognito = chat.isIncognito,
                    isSyncedToCloud = chat.cloudId != null // Has been synced if cloudId exists
                )
            )
        }

        archivedChats.forEach { chat ->
            val lastMsg = viewModel.getLastMessagePreview(chat.id)
            items.add(
                com.example.innovexia.ui.models.ChatListItem(
                    chatId = chat.id,
                    title = chat.title,
                    lastMessage = lastMsg ?: "No messages yet",
                    updatedAt = chat.updatedAt,
                    personaInitials = listOfNotNull(chat.personaInitial),
                    state = com.example.innovexia.ui.models.ChatState.ARCHIVED,
                    pinned = false,
                    isIncognito = chat.isIncognito,
                    isSyncedToCloud = chat.cloudId != null
                )
            )
        }

        deletedChats.forEach { chat ->
            val lastMsg = viewModel.getLastMessagePreview(chat.id)
            items.add(
                com.example.innovexia.ui.models.ChatListItem(
                    chatId = chat.id,
                    title = chat.title,
                    lastMessage = lastMsg ?: "No messages yet",
                    updatedAt = chat.updatedAt,
                    personaInitials = listOfNotNull(chat.personaInitial),
                    state = com.example.innovexia.ui.models.ChatState.TRASH,
                    pinned = false,
                    isIncognito = chat.isIncognito,
                    isSyncedToCloud = chat.cloudId != null
                )
            )
        }

        allChatItems = items
    }

    // State for context menu
    var showContextMenu by remember { mutableStateOf(false) }
    var selectedChatItem by remember { mutableStateOf<com.example.innovexia.ui.models.ChatListItem?>(null) }

    com.example.innovexia.ui.theme.InnovexiaTheme(darkTheme = darkTheme) {
    Box(modifier = Modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true, // Enable swipe to open menu
            drawerContent = {
                com.example.innovexia.ui.components.SideMenuNew(
                    items = allChatItems,
                    onNewChat = {
                        scope.launch { drawerState.close() }
                        viewModel.newChat()
                        composerText = ""
                    },
                    onOpenChat = { chatId ->
                        scope.launch {
                            drawerState.close()
                            // Small delay to let drawer animation complete before loading
                            kotlinx.coroutines.delay(150)
                            viewModel.loadChat(chatId)
                        }
                    },
                    onAboutClick = {
                        scope.launch { drawerState.close() }
                        // TODO: Show about dialog
                    },
                    onItemLongPress = { item ->
                        selectedChatItem = item
                        showContextMenu = true
                    },
                    onOpenProfile = {
                        scope.launch { drawerState.close() }
                        showProfile = true
                    },
                    onOpenUsage = {
                        scope.launch { drawerState.close() }
                        showUsage = true
                    },
                    onOpenSubscription = {
                        scope.launch { drawerState.close() }
                        showSubscription = true
                    },
                    onManageSubscription = {
                        scope.launch { drawerState.close() }
                        showManageSubscription = true
                    },
                    onOpenSettings = {
                        scope.launch { drawerState.close() }
                        showSettings = true
                    },
                    onLogout = {
                        scope.launch { drawerState.close() }
                        authViewModel.signOut()
                    },
                    darkTheme = darkTheme,
                    // Cloud restore parameters
                    showCloudRestoreButton = showCloudRestoreButton,
                    cloudChatCount = cloudChatCount,
                    onRestoreFromCloud = {
                        scope.launch { drawerState.close() }
                        showCloudRestoreSheet = true
                    },
                    onHideCloudRestoreButton = {
                        scope.launch {
                            userPrefs.setHideCloudRestoreButton(true)
                        }
                    },
                    // Multi-select delete parameters
                    onDeleteChats = { chatIds ->
                        // Don't close drawer - keep it open during multi-select
                        // Check which tab we're in to determine delete behavior
                        val chatsToDelete = allChatItems.filter { it.chatId in chatIds }
                        chatsToDelete.forEach { chat ->
                            when (chat.state) {
                                com.example.innovexia.ui.models.ChatState.TRASH -> {
                                    // Already in trash - delete forever
                                    viewModel.deleteForever(chat.chatId)
                                }
                                else -> {
                                    // Active or Archived - move to trash
                                    viewModel.moveToTrash(chat.chatId)
                                }
                            }
                        }
                    },
                    onEmptyTrash = {
                        // Don't close drawer - keep it open
                        // Delete all chats in trash
                        deletedChats.forEach { chat ->
                            viewModel.deleteForever(chat.id)
                        }
                    },
                    // Archive/Unarchive parameters
                    onArchiveChats = { chatIds ->
                        // Don't close drawer - keep it open
                        chatIds.forEach { chatId ->
                            viewModel.archiveChat(chatId)
                        }
                    },
                    onUnarchiveChats = { chatIds ->
                        // Don't close drawer - keep it open
                        chatIds.forEach { chatId ->
                            viewModel.restoreFromArchive(chatId)
                        }
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Get display name for current model
                val currentModelDisplayName = remember(aiPrefs.model) {
                    com.example.innovexia.core.ai.getModelLabel(aiPrefs.model)
                }

                HomeContent(
                    modifier = modifier,
                    darkTheme = darkTheme,
                    isDrawerOpen = drawerState.isOpen,
                chatTitle = activeChatTitle,
                onMenuClick = {
                    scope.launch {
                        if (drawerState.isClosed) {
                            drawerState.open()
                        } else {
                            drawerState.close()
                        }
                    }
                },
                onNewChatClick = {
                    viewModel.newChat()
                    composerText = ""
                    isIncognitoEnabled = false // Reset incognito on new chat
                },
                onTitleClick = if (!signedIn) null else { { showModelPanel = true } }, // Guests cannot change model
                currentModelName = currentModelDisplayName,
                composerText = composerText,
                onComposerTextChange = { composerText = it },
                onSend = onSend@{
                    // Check usage limits first
                    val usageState = usageVM.usageState.value

                    // Block if at/over limit (UI will show rate limit countdown)
                    if (usageState.messagesUsed >= usageState.messagesLimit ||
                        usageState.tokensUsed >= usageState.tokensLimit) {
                        return@onSend
                    }

                    // Check consent
                    when (consentedSaveHistory) {
                        null -> showConsentSheet = true
                        else -> {
                            // Track message IMMEDIATELY to prevent double-send during async response
                            // Token counts will be updated when response completes
                            usageVM.trackMessage(tokensIn = 0L, tokensOut = 0L)

                            viewModel.sendMessage(composerText, selectedPersona, isIncognito = isIncognitoEnabled)
                            composerText = ""
                            isIncognitoEnabled = false // Reset after sending
                        }
                    }
                },
                onAttachClick = {
                    showAttachmentToolbar = !showAttachmentToolbar
                },
                onToolsClick = { showToolsPanel = true },
                selectedPersona = selectedPersona,
                onPersonaClick = { showPersonaSelector = true },
                isStreaming = isStreaming,
                messages = visibleMessages,
                onStopStreaming = { viewModel.stopStreaming() },
                apiKeyMissing = !viewModel.isApiKeyConfigured(),
                isIncognitoEnabled = isIncognitoEnabled,
                onIncognitoToggle = {
                    if (!isIncognitoEnabled) { // Only allow enabling, not disabling
                        isIncognitoEnabled = true
                        showIncognitoDialog = true
                    }
                },
                showAttachmentToolbar = showAttachmentToolbar,
                onPickPhotos = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickFiles = {
                    filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                },
                attachments = attachments,
                onRemoveAttachment = { attachmentId ->
                    viewModel.removeAttachment(attachmentId)
                },
                usageVM = usageVM,
                onUpgrade = { showSubscription = true },
                isGuest = !signedIn,
                onShowProfile = { showProfile = true },
                groundingEnabled = groundingEnabledFromVM,
                onGroundingToggle = { enabled ->
                    showAttachmentToolbar = false
                    viewModel.setGroundingEnabled(enabled)
                },
                groundingDataMap = groundingDataMap,
                groundingStatusMap = groundingStatusMap
            )

                // Blur/scrim overlay when drawer is open
                if (drawerState.isOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    scope.launch { drawerState.close() }
                                }
                            }
                    )
                }
            }
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )

        // Consent sheet
        if (showConsentSheet) {
            HistoryConsentSheet(
                onAllow = {
                    viewModel.setConsent(true)
                    showConsentSheet = false

                    // Track message immediately
                    usageVM.trackMessage(tokensIn = 0L, tokensOut = 0L)

                    viewModel.sendMessage(composerText, selectedPersona)
                    composerText = ""
                },
                onDecline = {
                    viewModel.setConsent(false)
                    showConsentSheet = false

                    // Track message immediately
                    usageVM.trackMessage(tokensIn = 0L, tokensOut = 0L)

                    viewModel.sendMessage(composerText, selectedPersona)
                    composerText = ""
                },
                darkTheme = darkTheme
            )
        }

        // Settings sheet
        if (showSettings) {
            SettingsSheet(
                onDismiss = { showSettings = false },
                themeMode = themeMode,
                onThemeChange = { themeMode = it },
                prefs = settingsPrefs,
                onPrefsChange = { settingsPrefs = it },
                darkTheme = darkTheme,
                onDeleteHistory = {
                    viewModel.deleteAllHistory()
                },
                consentedSaveHistory = consentedSaveHistory
            )
        }

        // Profile sheet
        if (showProfile) {
            ProfileSheet(
                onDismiss = { showProfile = false },
                darkTheme = darkTheme,
                authViewModel = authViewModel,
                homeViewModel = viewModel
            )
        }

        // Usage dialog
        if (showUsage) {
            UsageDialog(
                onDismiss = { showUsage = false },
                usageVM = usageVM,
                darkTheme = darkTheme
            )
        }

        // Subscription screen
        if (showSubscription) {
            SubscriptionsScreen(
                currentPlanId = entitlement.plan.lowercase(),
                entitlementsVM = entitlementsVM,
                usageVM = usageVM,
                onOpenUsage = {
                    showSubscription = false
                    showUsage = true
                },
                onBack = {
                    showSubscription = false
                }
            )
        }

        // Manage subscription screen
        if (showManageSubscription) {
            com.example.innovexia.ui.subscriptions.ManageSubscriptionScreen(
                entitlement = entitlement,
                onUpgrade = {
                    showManageSubscription = false
                    showSubscription = true
                },
                onDowngrade = {
                    // If on Plus tier, downgrading to Free means canceling
                    if (entitlement.plan == "PLUS") {
                        entitlementsVM.cancel()
                    } else {
                        // For Master/Pro, open subscription screen to choose tier
                        showManageSubscription = false
                        showSubscription = true
                    }
                },
                onCancel = {
                    entitlementsVM.cancel()
                },
                onResume = {
                    entitlementsVM.resume()
                },
                onBack = {
                    showManageSubscription = false
                }
            )
        }

        // Tools panel sheet
        if (showToolsPanel) {
            ToolsPanelSheet(
                onDismiss = { showToolsPanel = false },
                darkTheme = darkTheme,
                thinkingEnabled = viewModel.thinkingEnabled.collectAsState().value,
                onThinkingChanged = { viewModel.setThinkingEnabled(it) }
            )
        }

        // Persona sheet
        if (showPersonaSelector) {
            PersonaSheet(
                visible = showPersonaSelector,
                onDismiss = { showPersonaSelector = false },
                onPersonaSelected = { persona ->
                    // Convert from ui.persona.Persona to ui.models.Persona
                    selectedPersona = Persona(
                        id = persona.id,
                        name = persona.name,
                        initial = persona.initial,
                        colorHex = persona.color
                    )

                    // Save selection to preferences
                    scope.launch {
                        val prefs = com.example.innovexia.core.persona.PersonaPreferences(context)
                        val ownerId = com.example.innovexia.core.auth.ProfileId.current().toOwnerId()
                        prefs.setActivePersonaId(ownerId, persona.id)
                    }
                }
            )
        }

        // Merge guest chats dialog (shown on first sign-in if guest data exists)
        if (showMergeDialog && guestChatCount > 0) {
            MergeGuestChatsDialog(
                guestChatCount = guestChatCount,
                onMerge = { authViewModel.mergeGuestChats() },
                onKeepSeparate = { authViewModel.dismissMergeDialog() },
                onDismiss = { authViewModel.dismissMergeDialog() }
            )
        }

        // Incognito confirmation dialog
        if (showIncognitoDialog) {
            com.example.innovexia.ui.chat.IncognitoConfirmDialog(
                onDismiss = { showIncognitoDialog = false }
            )
        }

        // Model switcher panel
        if (showModelPanel) {
            com.example.innovexia.ui.components.ModelSwitcherPanel(
                prefs = aiPrefs,
                onPrefsChange = { newPrefs ->
                    // Update ViewModel with all AI preferences (this will automatically update aiPrefs via StateFlows)
                    viewModel.setSelectedModel(newPrefs.model)
                    viewModel.setAiPreferences(
                        temperature = newPrefs.creativity,
                        maxTokens = newPrefs.maxOutputTokens,
                        safety = newPrefs.safety
                    )
                    viewModel.setGroundingEnabled(newPrefs.groundingEnabled)
                },
                onOpenFullSettings = {
                    showModelPanel = false
                    showSettings = true
                },
                onDismiss = { showModelPanel = false },
                darkTheme = darkTheme
            )
        }

        // Context menu for chat items
        if (showContextMenu && selectedChatItem != null) {
            com.example.innovexia.ui.components.ChatItemContextMenu(
                item = selectedChatItem!!,
                expanded = showContextMenu,
                onDismiss = {
                    showContextMenu = false
                    selectedChatItem = null
                },
                onAction = { action ->
                    when (action) {
                        com.example.innovexia.ui.components.ChatItemAction.Pin ->
                            viewModel.togglePin(selectedChatItem!!.chatId)
                        com.example.innovexia.ui.components.ChatItemAction.Unpin ->
                            viewModel.togglePin(selectedChatItem!!.chatId)
                        com.example.innovexia.ui.components.ChatItemAction.Archive ->
                            viewModel.archiveChat(selectedChatItem!!.chatId)
                        com.example.innovexia.ui.components.ChatItemAction.MoveToTrash ->
                            viewModel.moveToTrash(selectedChatItem!!.chatId)
                        com.example.innovexia.ui.components.ChatItemAction.Restore -> {
                            when (selectedChatItem!!.state) {
                                com.example.innovexia.ui.models.ChatState.ARCHIVED ->
                                    viewModel.restoreFromArchive(selectedChatItem!!.chatId)
                                com.example.innovexia.ui.models.ChatState.TRASH ->
                                    viewModel.restoreFromTrash(selectedChatItem!!.chatId)
                                else -> {}
                            }
                        }
                        com.example.innovexia.ui.components.ChatItemAction.DeletePermanently ->
                            viewModel.deleteForever(selectedChatItem!!.chatId)
                    }
                    showContextMenu = false
                    selectedChatItem = null
                }
            )
        }

        // Upgrade success dialog
        if (showUpgradeSuccess) {
            com.example.innovexia.ui.dialogs.UpgradeSuccessDialog(
                planName = upgradedPlanName,
                onDismiss = {
                    showUpgradeSuccess = false
                    upgradedPlanName = ""
                }
            )
        }

        // Cloud restore sheet
        if (showCloudRestoreSheet) {
            @OptIn(ExperimentalMaterial3Api::class)
            CloudRestoreSheet(
                onDismiss = { showCloudRestoreSheet = false },
                onRestoreComplete = {
                    // Chat list will auto-refresh via Room Flow
                },
                onCountChanged = { newCount ->
                    // Update cloud chat count in real-time
                    cloudChatCount = newCount
                },
                darkTheme = darkTheme
            )
        }

        // Auto-restore prompt dialog (on fresh install)
        if (showRestorePrompt) {
            AutoRestorePromptDialog(
                cloudChatCount = cloudChatCount,
                onRestoreAll = {
                    showRestorePrompt = false
                    showCloudRestoreSheet = true
                    scope.launch {
                        userPrefs.setHasSeenRestorePrompt(true)
                    }
                },
                onChoose = {
                    showRestorePrompt = false
                    showCloudRestoreSheet = true
                    scope.launch {
                        userPrefs.setHasSeenRestorePrompt(true)
                    }
                },
                onDismiss = { hideButton ->
                    showRestorePrompt = false
                    scope.launch {
                        userPrefs.setHasSeenRestorePrompt(true)
                        if (hideButton) {
                            userPrefs.setHideCloudRestoreButton(true)
                        }
                    }
                },
                darkTheme = darkTheme
            )
        }
    }
    } // InnovexiaTheme
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = false,
    isDrawerOpen: Boolean = false,
    chatTitle: String = "",
    onMenuClick: () -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onTitleClick: (() -> Unit)? = null,
    composerText: String = "",
    onComposerTextChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    onAttachClick: () -> Unit = {},
    onToolsClick: () -> Unit = {},
    selectedPersona: Persona? = null,
    onPersonaClick: () -> Unit = {},
    isStreaming: Boolean = false,
    messages: List<com.example.innovexia.ui.models.UIMessage> = emptyList(),
    onStopStreaming: () -> Unit = {},
    apiKeyMissing: Boolean = false,
    isIncognitoEnabled: Boolean = false,
    onIncognitoToggle: () -> Unit = {},
    showAttachmentToolbar: Boolean = false,
    onPickPhotos: () -> Unit = {},
    onPickFiles: () -> Unit = {},
    attachments: List<com.example.innovexia.data.models.AttachmentMeta> = emptyList(),
    onRemoveAttachment: (String) -> Unit = {},
    currentModelName: String = "Gemini 2.5 Flash",
    usageVM: com.example.innovexia.subscriptions.mock.UsageVM,
    onUpgrade: () -> Unit = {},
    isGuest: Boolean = false,
    onShowProfile: () -> Unit = {},
    groundingEnabled: Boolean = false,
    onGroundingToggle: (Boolean) -> Unit = {},
    groundingDataMap: Map<String, com.example.innovexia.data.ai.GroundingMetadata> = emptyMap(),
    groundingStatusMap: Map<String, com.example.innovexia.data.ai.GroundingStatus> = emptyMap()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    // Voice input manager and state
    val voiceInputManager = remember { VoiceInputManager(context) }
    val voiceState by voiceInputManager.state.collectAsState()
    val voiceFinalResult by voiceInputManager.finalResult.collectAsState()
    val voicePartialResult by voiceInputManager.partialResult.collectAsState()
    val voiceError by voiceInputManager.error.collectAsState()
    val voiceElapsedTime by voiceInputManager.elapsedTime.collectAsState()

    var isRecordingVoice by remember { mutableStateOf(false) }
    var triggerBlur by remember { mutableStateOf(0) }

    // Blur animation with haptic feedback for no speech detected
    LaunchedEffect(triggerBlur) {
        if (triggerBlur > 0) {
            // Perform 3 rapid haptic pulses
            repeat(3) { index ->
                kotlinx.coroutines.delay(index * 50L)
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            // Reset after animation completes
            kotlinx.coroutines.delay(200)
            triggerBlur = 0
        }
    }

    val blurRadius by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (triggerBlur > 0) 8f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 80,
            easing = androidx.compose.animation.core.LinearEasing
        ),
        label = "blur_animation"
    )

    // Permission launcher for audio recording
    val audioPermissionLauncher = rememberAudioPermissionLauncher { granted ->
        if (granted) {
            android.util.Log.d("HomeContent", "Audio permission granted, starting voice input")
            isRecordingVoice = true
            voiceInputManager.startListening()
        } else {
            android.util.Log.w("HomeContent", "Audio permission denied")
            android.widget.Toast.makeText(context, "Microphone permission is required for voice input", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Handle real-time partial results (show text as user speaks)
    LaunchedEffect(voicePartialResult) {
        if (voicePartialResult.isNotBlank() && isRecordingVoice) {
            // Update composer with partial results in real-time
            onComposerTextChange(voicePartialResult)
        }
    }

    // Handle final voice transcription result
    LaunchedEffect(voiceFinalResult) {
        if (voiceFinalResult != null && voiceFinalResult!!.isNotBlank()) {
            android.util.Log.d("HomeContent", "Voice transcription completed: $voiceFinalResult")
            // Final result is already in composer from partial updates
            isRecordingVoice = false
        }
    }

    // Handle voice input errors
    LaunchedEffect(voiceError) {
        voiceError?.let { error ->
            when (error) {
                is com.example.innovexia.core.voice.VoiceInputError.NoSpeech -> {
                    // Trigger blur animation with haptic feedback
                    triggerBlur = triggerBlur + 1 // Increment to trigger animation
                }
                else -> {
                    // Single haptic feedback for other errors
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                    // Show toast for other errors
                    val errorMessage = when (error) {
                        is com.example.innovexia.core.voice.VoiceInputError.NetworkError -> "Network error. Please check your connection."
                        is com.example.innovexia.core.voice.VoiceInputError.AudioError -> "Audio error. Please check microphone permissions."
                        is com.example.innovexia.core.voice.VoiceInputError.RecognizerBusy -> "Speech recognizer is busy. Please try again."
                        is com.example.innovexia.core.voice.VoiceInputError.PermissionDenied -> "Microphone permission denied."
                        is com.example.innovexia.core.voice.VoiceInputError.Unknown -> "Voice input error. Please try again."
                        else -> "Voice input error. Please try again."
                    }
                    android.widget.Toast.makeText(context, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            isRecordingVoice = false
            voiceInputManager.clearError()
        }
    }

    // Cleanup voice input manager on dispose
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("HomeContent", "Disposing voice input manager")
            voiceInputManager.destroy()
        }
    }

    GradientScaffold(
        modifier = modifier,
        darkTheme = darkTheme,
        applySystemBarsPadding = false // We handle insets manually for proper keyboard anchoring
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                // Remove status bar padding to make it full-screen
        ) {
            // API key missing banner
            if (apiKeyMissing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f)
                            else LightColors.SecondaryText.copy(alpha = 0.15f)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Warning",
                            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        )
                        Text(
                            text = "API key missing (dev). Add GEMINI_API_KEY to local.properties",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        )
                    }
                }
            }

            // Messages list (scrollable) or empty chat greeting
            if (messages.isNotEmpty()) {
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()

                // Auto-scroll to bottom when new message added or chat loaded
                androidx.compose.runtime.LaunchedEffect(messages.size, chatTitle) {
                    if (messages.isNotEmpty()) {
                        // Smooth scroll to bottom
                        kotlinx.coroutines.delay(100) // Small delay to ensure layout is ready
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                // Calculate top padding: status bar + top bar + extra spacing
                val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val topBarHeight = 44.dp // Slimmer header
                val extraSpacing = 16.dp // More breathing room below header
                val totalTopPadding = statusBarHeight + topBarHeight + extraSpacing + if (apiKeyMissing) 8.dp else 0.dp

                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = totalTopPadding,
                        bottom = 120.dp // Space for composer
                    ),
                    reverseLayout = false
                ) {
                    items(messages.size) { index ->
                        val message = messages[index]

                        // Convert UIMessage to MessageEntity (preserve real message ID for grounding lookup)
                        val messageEntity = com.example.innovexia.data.local.entities.MessageEntity(
                            id = message.id, // Use real message ID to match grounding data
                            ownerId = "guest",
                            chatId = "current",
                            role = if (message.isUser) "user" else "model",
                            text = message.text,
                            createdAt = message.timestamp,
                            attachmentsJson = if (message.attachments.isNotEmpty()) {
                                com.example.innovexia.data.models.AttachmentMetaSerializer.toJson(message.attachments)
                            } else null
                        )

                        // Use UserBubbleV2 for user messages, ResponseBubbleV2 for AI
                        if (message.isUser) {
                            com.example.innovexia.ui.chat.bubbles.UserBubbleV2(
                                msg = messageEntity,
                                onCopy = { /* TODO: Implement copy */ },

                                onRetry = { /* Retry disabled in HomeScreen */ },
                                onQuote = { /* Quote disabled in HomeScreen */ },
                                onDelete = { /* Delete disabled in HomeScreen */ }
                            )
                        } else {
                            // Use ResponseBubbleV2 for regular responses
                            // Note: HomeScreen uses transient UIMessages, not persisted MessageEntities
                            // For proper grounding bubble support, messages should be loaded from database (like in ChatScreen)
                            com.example.innovexia.ui.chat.bubbles.ResponseBubbleV2(
                                message = messageEntity,
                                isStreaming = message.isStreaming,
                                messageStatus = message.status,
                                modelName = message.modelName ?: currentModelName,
                                onRegenerate = {
                                    // TODO: Implement regenerate functionality in HomeViewModel
                                }
                            )
                        }
                    }
                }
            } else {
                // Show smart greeting with memory-aware suggestions
                val corePersona = selectedPersona?.toCorePersona()

                SmartGreetingScreen(
                    persona = corePersona,
                    onSuggestionClicked = { suggestion ->
                        // Prefill composer with suggestion text
                        onComposerTextChange(suggestion.title)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = if (apiKeyMissing) 60.dp else 56.dp,
                            bottom = 120.dp
                        )
                )
            }

            // New refined header with isNewChat state
            ChatHeader(
                title = if (chatTitle.isNotEmpty()) chatTitle else "New chat",
                isNewChat = messages.isEmpty(),
                isIncognitoActive = isIncognitoEnabled,
                onNewChat = onNewChatClick,
                onIncognito = onIncognitoToggle,
                onIncognitoOff = onIncognitoToggle,
                onOpenMenu = onMenuClick,
                onTitleClick = onTitleClick,
                isGuest = isGuest,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )

            // Bottom section: BottomBar and Input field
            val isKeyboardVisible = WindowInsets.isImeVisible

            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.ime)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Attachment chips (show selected files/photos)
                if (attachments.isNotEmpty()) {
                    AttachmentStrip(
                        items = attachments,
                        onRemove = onRemoveAttachment
                    )
                }

                // Attachment toolbar (slides in above composer)
                AttachmentToolbar(
                    visible = showAttachmentToolbar,
                    onPickPhotos = onPickPhotos,
                    onPickFiles = onPickFiles,
                    onCapture = {
                        android.widget.Toast.makeText(context, "Camera - wire to launcher", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onScanPdf = {
                        android.widget.Toast.makeText(context, "Scan PDF - wire to launcher", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    groundingEnabled = groundingEnabled,
                    onGroundingToggle = onGroundingToggle
                )

                // Guest upgrade banner (only for guest users)
                if (isGuest) {
                    com.example.innovexia.ui.chat.GuestUpgradeBanner(
                        onSignUp = {
                            // Open profile sheet in sign up mode
                            onShowProfile()
                        },
                        onSignIn = {
                            // Open profile sheet in sign in mode
                            onShowProfile()
                        }
                    )
                }

                // Show rate limit composer or normal composer
                val usageState = usageVM.usageState.collectAsState().value

                androidx.compose.animation.AnimatedContent(
                    targetState = usageState.isLimitReached,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
                    },
                    label = "composerTransition"
                ) { isLimitReached ->
                    if (isLimitReached) {
                        // Rate limit countdown composer
                        com.example.innovexia.ui.components.RateLimitComposer(
                            usageState = usageState,
                            onUpgrade = onUpgrade,
                            modifier = Modifier.fillMaxWidth(),
                            isGuest = isGuest
                        )
                    } else {
                        // Switch between web search and normal composer based on grounding
                        androidx.compose.animation.AnimatedContent(
                            targetState = groundingEnabled,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(200)) togetherWith
                                fadeOut(animationSpec = tween(200))
                            },
                            label = "groundingComposerSwitch"
                        ) { isGroundingMode ->
                            if (isGroundingMode) {
                                // Web search composer when grounding enabled
                                WebSearchComposer(
                                    value = composerText,
                                    onValueChange = onComposerTextChange,
                                    onSend = {
                                        focusManager.clearFocus()
                                        onSend()
                                    },
                                    isStreaming = isStreaming,
                                    onStopStreaming = onStopStreaming,
                                    onDisableGrounding = { onGroundingToggle(false) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                // Normal chat composer
                                ChatComposerV3(
                                    value = composerText,
                                    onValueChange = onComposerTextChange,
                                    onSend = {
                                        focusManager.clearFocus() // Close keyboard and clear focus
                                        onSend()
                                    },
                                    onAttach = onAttachClick,
                                    onMic = {
                                        if (isRecordingVoice) {
                                            // Stop recording
                                            android.util.Log.d("HomeContent", "Stopping voice recording")
                                            voiceInputManager.stopListening()
                                            isRecordingVoice = false
                                        } else {
                                            // Check permission and start recording
                                            android.util.Log.d("HomeContent", "Mic button clicked")
                                            if (PermissionHelper.hasAudioPermission(context)) {
                                                android.util.Log.d("HomeContent", "Audio permission already granted, starting voice input")
                                                isRecordingVoice = true
                                                voiceInputManager.startListening()
                                            } else {
                                                android.util.Log.d("HomeContent", "Requesting audio permission")
                                                audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    },
                                    onPersona = onPersonaClick,
                                    hasAttachment = false,
                                    isStreaming = isStreaming,
                                    onStopStreaming = onStopStreaming,
                                    persona = selectedPersona,
                                    isGuest = isGuest,
                                    isRecording = isRecordingVoice,
                                    recordingDuration = voiceElapsedTime,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .blur(radius = blurRadius.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoRestorePromptDialog(
    cloudChatCount: Int,
    onRestoreAll: () -> Unit,
    onChoose: () -> Unit,
    onDismiss: (hideButton: Boolean) -> Unit,
    darkTheme: Boolean
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { onDismiss(dontShowAgain) },
        containerColor = if (darkTheme) Color(0xFF1F1F1F) else Color.White,
        icon = {
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = "Cloud Restore",
                tint = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Restore from Cloud?",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = if (darkTheme) Color(0xFFE5E7EB) else Color(0xFF1F2937)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "We found $cloudChatCount chat${if (cloudChatCount != 1) "s" else ""} in your cloud backup. Would you like to restore them?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { dontShowAgain = !dontShowAgain }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = dontShowAgain,
                        onCheckedChange = { dontShowAgain = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (darkTheme) Color(0xFF3B82F6) else Color(0xFF2563EB),
                            uncheckedColor = if (darkTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Don't show this again",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = if (darkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onRestoreAll,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
                )
            ) {
                Text("Restore All")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = { onDismiss(dontShowAgain) },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (darkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)
                    )
                ) {
                    Text("Not Now")
                }
                TextButton(
                    onClick = onChoose,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF2563EB)
                    )
                ) {
                    Text("Choose")
                }
            }
        }
    )
}

// Helper functions
private fun getEmojiForInitial(initial: String): String {
    return when (initial.uppercase()) {
        "N" -> "🌟"
        "A" -> "🗺️"
        "M" -> "🎨"
        "O" -> "⭐"
        "E" -> "🔊"
        else -> "💬"
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days} days ago"
        else -> "${days / 7} weeks ago"
    }
}
