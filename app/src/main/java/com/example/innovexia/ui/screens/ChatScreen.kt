package com.example.innovexia.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.chat.*
import com.example.innovexia.ui.chat.context.MessageAction
import com.example.innovexia.ui.chat.context.MessageContextMenu
import com.example.innovexia.ui.chat.reply.ReplyPreview
import com.example.innovexia.ui.components.AttachmentToolbar
import com.example.innovexia.ui.components.ChatComposerV3
import com.example.innovexia.ui.components.WebSearchComposer
import com.example.innovexia.ui.components.ModelSwitcherPanel
import com.example.innovexia.ui.components.UiAiPrefs
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.example.innovexia.ui.theme.InnovexiaTheme
import com.example.innovexia.ui.theme.InnovexiaTokens
import com.example.innovexia.ui.viewmodels.ChatViewModel
import com.example.innovexia.data.local.entities.MessageEntity
import com.google.firebase.auth.FirebaseAuth
import com.example.innovexia.core.permissions.PermissionHelper
import com.example.innovexia.core.permissions.rememberAudioPermissionLauncher
import com.example.innovexia.core.voice.VoiceInputManager
import android.Manifest

/**
 * Chat screen with message list, composer, and header.
 * Integrates all chat UI components.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    usageVM: com.example.innovexia.subscriptions.mock.UsageVM? = null,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val title by viewModel.title.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isNewChat by viewModel.isNewChat.collectAsState()
    val isIncognito by viewModel.isIncognito.collectAsState()
    val streamingMessageId by viewModel.streamingMessageId.collectAsState()
    val errorMessageIds by viewModel.errorMessageIds.collectAsState()
    val truncatedMessageIds by viewModel.truncatedMessageIds.collectAsState()
    val replyTarget by viewModel.replyTarget.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val rateLimitSecondsRemaining by viewModel.rateLimitSecondsRemaining.collectAsState()
    val editingMessageId by viewModel.editingMessageId.collectAsState()
    val quoteReplyText by viewModel.quoteReplyText.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()

    // Usage state (if provided)
    val usageState by usageVM?.usageState?.collectAsState() ?: remember { mutableStateOf(com.example.innovexia.subscriptions.mock.UsageState.empty()) }

    var composerText by remember { mutableStateOf("") }
    var selectedMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var justSent by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var showIncognitoDialog by remember { mutableStateOf(false) }
    var showAttachmentToolbar by remember { mutableStateOf(false) }
    var showModelSwitcher by remember { mutableStateOf(false) }

    // Grounding state from ViewModel
    val groundingEnabled by viewModel.groundingEnabled.collectAsState()
    val groundingDataMap by viewModel.groundingDataMap.collectAsState()
    val groundingStatusMap by viewModel.groundingStatusMap.collectAsState()

    // Voice input state from ViewModel
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val voiceTranscription by viewModel.voiceTranscription.collectAsState()

    // Debug: Log grounding state
    LaunchedEffect(groundingEnabled, groundingDataMap, groundingStatusMap) {
        android.util.Log.d("ChatScreen", "=== GROUNDING STATE ===")
        android.util.Log.d("ChatScreen", "groundingEnabled=$groundingEnabled")
        android.util.Log.d("ChatScreen", "groundingDataMap.size=${groundingDataMap.size}")
        android.util.Log.d("ChatScreen", "groundingStatusMap.size=${groundingStatusMap.size}")
        if (groundingDataMap.isNotEmpty()) {
            groundingDataMap.forEach { (messageId, metadata) ->
                android.util.Log.d("ChatScreen", "  Message $messageId: ${com.example.innovexia.data.ai.GroundingService.getGroundingSummary(metadata)}")
            }
        } else {
            android.util.Log.w("ChatScreen", "  ⚠️ groundingDataMap is EMPTY!")
        }
    }

    val clipboardManager = LocalClipboardManager.current
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current

    // Check if user is guest (not logged in)
    val isGuest = remember { FirebaseAuth.getInstance().currentUser == null }

    // Match ChatHeader background exactly
    val chatBackground = Color(0xFF171A1E)

    // Voice input manager (must be after context)
    val voiceInputManager = remember { VoiceInputManager(context) }
    val voiceState by voiceInputManager.state.collectAsState()
    val voiceFinalResult by voiceInputManager.finalResult.collectAsState()
    val voiceError by voiceInputManager.error.collectAsState()
    val voiceElapsedTime by voiceInputManager.elapsedTime.collectAsState()

    // Permission launcher for audio recording (must be after context)
    val audioPermissionLauncher = rememberAudioPermissionLauncher { granted ->
        if (granted) {
            android.util.Log.d("ChatScreen", "Audio permission granted, starting voice input")
            viewModel.startVoiceInput()
            voiceInputManager.startListening()
        } else {
            android.util.Log.w("ChatScreen", "Audio permission denied")
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_LONG).show()
        }
    }

    // Apply quote reply to composer
    LaunchedEffect(quoteReplyText) {
        if (quoteReplyText != null) {
            composerText = quoteReplyText + "\n\n"
            viewModel.clearQuoteReply()
        }
    }

    // Handle voice transcription result
    LaunchedEffect(voiceFinalResult) {
        if (voiceFinalResult != null && voiceFinalResult!!.isNotBlank()) {
            android.util.Log.d("ChatScreen", "Voice transcription completed: $voiceFinalResult")
            // Insert transcribed text into composer
            composerText = if (composerText.isBlank()) {
                voiceFinalResult!!
            } else {
                "$composerText ${voiceFinalResult}"
            }
            viewModel.onVoiceTranscription(voiceFinalResult!!)
            viewModel.stopVoiceInput()
        }
    }

    // Handle voice input errors
    LaunchedEffect(voiceError) {
        voiceError?.let { error ->
            val errorMessage = when (error) {
                is com.example.innovexia.core.voice.VoiceInputError.NoSpeech -> "No speech detected. Please try again."
                is com.example.innovexia.core.voice.VoiceInputError.NetworkError -> "Network error. Please check your connection."
                is com.example.innovexia.core.voice.VoiceInputError.AudioError -> "Audio error. Please check microphone permissions."
                is com.example.innovexia.core.voice.VoiceInputError.RecognizerBusy -> "Speech recognizer is busy. Please try again."
                is com.example.innovexia.core.voice.VoiceInputError.PermissionDenied -> "Microphone permission denied."
                is com.example.innovexia.core.voice.VoiceInputError.Unknown -> "Voice input error. Please try again."
            }
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.stopVoiceInput()
            voiceInputManager.clearError()
        }
    }

    // Cleanup voice input manager on dispose
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("ChatScreen", "Disposing voice input manager")
            voiceInputManager.destroy()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(chatBackground)
    ) {
        // New header with isNewChat state
        ChatHeader(
            title = title,
            isNewChat = isNewChat,
            isIncognitoActive = isIncognito,
            onNewChat = onNavigateHome,
            onIncognito = {
                viewModel.toggleIncognito(true)
                showIncognitoDialog = true
            },
            onIncognitoOff = {
                viewModel.toggleIncognito(false)
            },
            onOpenMenu = onBack,
            onTitleClick = if (!isGuest) {
                { showModelSwitcher = true }
            } else null,
            isGuest = isGuest
        )

        // Incognito confirmation dialog
        if (showIncognitoDialog) {
            IncognitoConfirmDialog(
                onDismiss = { showIncognitoDialog = false }
            )
        }

        // Model Switcher dialog
        if (showModelSwitcher) {
            ModelSwitcherPanel(
                prefs = UiAiPrefs(
                    groundingEnabled = groundingEnabled
                    // TODO: Add other AI prefs (model, temperature, etc.) when needed
                ),
                onPrefsChange = { newPrefs ->
                    viewModel.setGroundingEnabled(newPrefs.groundingEnabled)
                    // TODO: Handle other pref changes when needed
                },
                onOpenFullSettings = {
                    showModelSwitcher = false
                    Toast.makeText(context, "Full AI settings - coming soon", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showModelSwitcher = false },
                darkTheme = darkTheme
            )
        }

        // Divider
        HorizontalDivider(color = InnovexiaTokens.Color.GrayStroke)

        // Content area
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Message list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                MessageList(
                    messages = messages,
                    onLongPress = { message ->
                        selectedMessage = message
                        showMenu = true
                    },
                    onSwipeReply = { message ->
                        viewModel.setReplyTarget(message.id)
                    },
                    onRetry = { message ->
                        if (message.role == "user") {
                            viewModel.retryUserMessage(message)
                        } else {
                            viewModel.retryMessage(message.id)
                        }
                    },
                    streamingMessageId = streamingMessageId,
                    errorMessageIds = errorMessageIds,
                    truncatedMessageIds = truncatedMessageIds,
                    editingMessageId = editingMessageId,
                    onCopyMessage = { message ->
                        viewModel.copyMessage(message)
                    },
                    onQuoteMessage = { message ->
                        viewModel.quoteMessage(message)
                    },
                    onDeleteMessage = { message ->
                        viewModel.deleteUserMessage(message.id)
                    },
                    onCancelEdit = {
                        viewModel.cancelEditing()
                    },
                    onSaveEdit = { newText, original ->
                        viewModel.resendEdited(original, newText)
                    },
                    onRegenerateAssistant = { messageId ->
                        viewModel.regenerateAssistant(messageId)
                    },
                    onContinueResponse = { messageId ->
                        viewModel.continueResponse(messageId)
                    },
                    ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest",
                    selectedPersona = selectedPersona?.toCorePersona(),
                    groundingDataMap = groundingDataMap,
                    groundingStatusMap = groundingStatusMap,
                    groundingEnabled = groundingEnabled,
                    onSuggestionClicked = { suggestion ->
                        viewModel.handleSuggestionClick(suggestion) { prefillText ->
                            composerText = prefillText
                        }
                    }
                )
            }

            // Reply preview
            if (replyTarget != null) {
                ReplyPreview(
                    replyToMessage = replyTarget!!,
                    onClear = {
                        viewModel.clearReplyTarget()
                    }
                )
            }

            // Usage limit banner (new system)
            if (usageVM != null) {
                com.example.innovexia.ui.chat.UsageLimitBanner(usageState = usageState)
            }

            // Rate limit banner (legacy - shown if usageVM not available)
            if (usageVM == null && rateLimitSecondsRemaining != null) {
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                ) {
                    Text("⚠️ Rate limit exceeded. Resets in ${rateLimitSecondsRemaining}s")
                }
            }

            // Error banner
            if (errorMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMessage!!)
                }
            }

            // Guest upgrade banner (only for guest users)
            if (isGuest) {
                GuestUpgradeBanner(
                    onSignUp = {
                        // TODO: Navigate to sign up screen
                        Toast.makeText(context, "Sign up screen - coming soon", Toast.LENGTH_SHORT).show()
                    },
                    onSignIn = {
                        // TODO: Navigate to sign in screen
                        Toast.makeText(context, "Sign in screen - coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Attachment toolbar (slides in above composer)
            AttachmentToolbar(
                visible = showAttachmentToolbar,
                onPickPhotos = {
                    showAttachmentToolbar = false
                    Toast.makeText(context, "Photo picker - wire to launcher", Toast.LENGTH_SHORT).show()
                },
                onPickFiles = {
                    showAttachmentToolbar = false
                    Toast.makeText(context, "File picker - wire to launcher", Toast.LENGTH_SHORT).show()
                },
                onCapture = {
                    showAttachmentToolbar = false
                    Toast.makeText(context, "Camera - wire to launcher", Toast.LENGTH_SHORT).show()
                },
                groundingEnabled = groundingEnabled,
                onGroundingToggle = { enabled ->
                    Toast.makeText(context, "Grounding toggle: $enabled", Toast.LENGTH_SHORT).show()
                    android.util.Log.d("ChatScreen", "Grounding toggle clicked! New value: $enabled")
                    android.util.Log.d("ChatScreen", "Current groundingEnabled: $groundingEnabled")
                    showAttachmentToolbar = false
                    viewModel.setGroundingEnabled(enabled)
                    android.util.Log.d("ChatScreen", "Grounding toggle complete")
                }
            )

            // Composer - switches between WebSearchComposer and ChatComposerV3 based on grounding mode
            // Reset justSent flag when streaming completes
            LaunchedEffect(streamingMessageId) {
                if (streamingMessageId == null) {
                    justSent = false
                    isSending = false
                }
            }

            // Animated transition between composers
            AnimatedContent(
                targetState = groundingEnabled,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "composer_transition"
            ) { isGroundingMode ->
                if (isGroundingMode) {
                    // Web Search Composer (grounding mode)
                    WebSearchComposer(
                        value = composerText,
                        onValueChange = { composerText = it },
                        onSend = {
                            val textToSend = composerText.trim()
                            if (textToSend.isNotBlank() && !isSending) {
                                // Check usage limits before sending
                                if (usageState.isLimitReached) {
                                    Toast.makeText(
                                        context,
                                        "Usage limit reached. Resets in ${usageState.timeUntilReset}.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    android.util.Log.d("ChatScreen", "SEND BLOCKED - usage limit reached")
                                    return@WebSearchComposer
                                }

                                android.util.Log.d("ChatScreen", "WEB SEARCH SEND clicked - text='$textToSend'")
                                isSending = true
                                justSent = true
                                composerText = ""
                                viewModel.sendMessage(textToSend)
                                viewModel.clearReplyTarget()

                                // Track usage
                                usageVM?.trackMessage(
                                    tokensIn = (textToSend.length / 4).toLong(),
                                    tokensOut = 0L
                                )
                            }
                        },
                        isStreaming = streamingMessageId != null || justSent,
                        onStopStreaming = {
                            viewModel.stopStreaming()
                            justSent = false
                        },
                        onDisableGrounding = {
                            viewModel.setGroundingEnabled(false)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Regular Chat Composer
                    ChatComposerV3(
                        value = composerText,
                        onValueChange = { composerText = it },
                        onSend = {
                            val textToSend = composerText.trim()
                            if (textToSend.isNotBlank() && !isSending) {
                                // Check usage limits before sending
                                if (usageState.isLimitReached) {
                                    Toast.makeText(
                                        context,
                                        "Usage limit reached. Resets in ${usageState.timeUntilReset}.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    android.util.Log.d("ChatScreen", "SEND BLOCKED - usage limit reached")
                                    return@ChatComposerV3
                                }

                                android.util.Log.d("ChatScreen", "SEND clicked - text='$textToSend', isSending=$isSending")
                                isSending = true
                                justSent = true
                                composerText = ""
                                viewModel.sendMessage(textToSend)
                                viewModel.clearReplyTarget()

                                // Track usage (approximate - will be updated with actual tokens later)
                                usageVM?.trackMessage(
                                    tokensIn = (textToSend.length / 4).toLong(), // Rough estimate: ~4 chars per token
                                    tokensOut = 0L // Will be updated when response completes
                                )

                                android.util.Log.d("ChatScreen", "After send - justSent=$justSent, isSending=$isSending")
                            } else {
                                android.util.Log.d("ChatScreen", "SEND BLOCKED - isSending=$isSending, text='$textToSend'")
                            }
                        },
                        onAttach = {
                            android.util.Log.d("ChatScreen", "Plus button clicked! Current state: $showAttachmentToolbar")
                            showAttachmentToolbar = !showAttachmentToolbar
                            android.util.Log.d("ChatScreen", "New state: $showAttachmentToolbar")
                        },
                        onMic = {
                            if (isRecordingVoice) {
                                // Stop recording
                                android.util.Log.d("ChatScreen", "Stopping voice recording")
                                voiceInputManager.stopListening()
                                viewModel.stopVoiceInput()
                            } else {
                                // Check permission and start recording
                                android.util.Log.d("ChatScreen", "Mic button clicked")
                                if (PermissionHelper.hasAudioPermission(context)) {
                                    android.util.Log.d("ChatScreen", "Audio permission already granted, starting voice input")
                                    viewModel.startVoiceInput()
                                    voiceInputManager.startListening()
                                } else {
                                    android.util.Log.d("ChatScreen", "Requesting audio permission")
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        onPersona = {
                            // TODO: Implement persona selection sheet
                            Toast.makeText(context, "Persona selection (coming soon)", Toast.LENGTH_SHORT).show()
                        },
                        hasAttachment = false,
                        isStreaming = streamingMessageId != null || justSent,
                        onStopStreaming = {
                            viewModel.stopStreaming()
                            justSent = false
                        },
                        persona = selectedPersona,
                        isGuest = isGuest,
                        isRecording = isRecordingVoice,
                        recordingDuration = voiceElapsedTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Context menu
    if (showMenu && selectedMessage != null) {
        MessageContextMenu(
            message = selectedMessage!!,
            onDismiss = {
                showMenu = false
                selectedMessage = null
            },
            onAction = { action ->
                when (action) {
                    MessageAction.Copy -> {
                        clipboardManager.setText(AnnotatedString(selectedMessage!!.text))
                    }
                    MessageAction.Reply -> {
                        viewModel.setReplyTarget(selectedMessage!!.id)
                    }
                    MessageAction.Edit -> {
                        // TODO: Implement inline edit
                        // For now, just copy to composer
                        if (selectedMessage!!.role == "user") {
                            composerText = selectedMessage!!.text
                        }
                    }
                    MessageAction.Delete -> {
                        viewModel.deleteMessage(selectedMessage!!.id)
                    }
                    MessageAction.Retry -> {
                        viewModel.retryMessage(selectedMessage!!.id)
                    }
                    MessageAction.Select -> {
                        // TODO: Implement multi-select mode
                    }
                }
                showMenu = false
                selectedMessage = null
            }
        )
    }
}

// Preview with mock data
@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    // Note: This preview won't work without a real ViewModel
    // In production, use dependency injection to provide ViewModel
    InnovexiaTheme {
        Surface {
            // Placeholder - actual implementation requires ViewModel
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "ChatScreen Preview\n(Requires ViewModel)",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
