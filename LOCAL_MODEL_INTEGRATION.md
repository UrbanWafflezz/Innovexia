# Local TFLite Model Integration Guide

## 1. Update ModelSwitcher.kt

### Add LOCAL provider to enum (line 84):
```kotlin
enum class ModelProvider {
    GOOGLE, OPENAI, CLAUDE, PERPLEXITY, LOCAL
}
```

### Add local model to AvailableModels.all list (after line 127):
```kotlin
// Add after Gemini 2.5 Pro entry
ModelInfo(
    id = "innovexia-local-tflite",
    displayName = "Innovexia Local (TFLite)",
    provider = ModelProvider.LOCAL,
    badge = "On-Device",
    description = "Private, offline AI powered by Pixel NPU",
    isAvailable = true
),
```

### Add LOCAL provider section in PanelContent (after line 338):
```kotlin
// Add after Perplexity section
// Local TFLite Section
ProviderSection(
    provider = ModelProvider.LOCAL,
    prefs = prefs,
    onPrefsChange = onPrefsChange,
    darkTheme = darkTheme,
    borderColor = borderColor
)
```

## 2. Update ChatViewModel.kt

### Import LocalTFLiteEngine
```kotlin
import com.example.innovexia.local.LocalTFLiteEngine
import com.example.innovexia.local.LocalModelRegistry
import com.example.innovexia.local.PersonaProfile
import javax.inject.Inject
```

### Add LocalTFLiteEngine to constructor or inject it
```kotlin
class ChatViewModel @Inject constructor(
    private val application: Application,
    private val chatRepository: ChatRepository,
    private val geminiService: GeminiService,
    private val localTFLiteEngine: LocalTFLiteEngine, // ADD THIS
    private val chatId: String
) : ViewModel() {
```

### Add model loading state
```kotlin
// Add to ViewModel class
private val _localModelLoading = MutableStateFlow(false)
val localModelLoading: StateFlow<Boolean> = _localModelLoading.asStateFlow()

private val _localModelError = MutableStateFlow<String?>(null)
val localModelError: StateFlow<String?> = _localModelError.asStateFlow()
```

### Update sendMessage() to handle local model
Find the sendMessage() function and add local model routing:

```kotlin
fun sendMessage(text: String, persona: Persona? = null) {
    if (text.isBlank()) return
    if (isSending) return
    isSending = true

    viewModelScope.launch {
        try {
            // Create user message
            val userMsgId = UUID.randomUUID().toString()
            val userMsg = MessageEntity(
                id = userMsgId,
                chatId = chatId,
                role = "user",
                text = text,
                status = MsgStatus.SENT,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.insertMessage(userMsg)

            // Create assistant message placeholder
            val assistantMsgId = UUID.randomUUID().toString()
            val assistantMsg = MessageEntity(
                id = assistantMsgId,
                chatId = chatId,
                role = "model",
                text = "",
                status = MsgStatus.PENDING,
                timestamp = System.currentTimeMillis()
            )
            chatRepository.insertMessage(assistantMsg)
            _streamingMessageId.value = assistantMsgId

            // Get current model from preferences
            val currentModel = prefs.model // Assuming you have prefs state

            // Route to appropriate model
            when (currentModel) {
                "innovexia-local-tflite",
                LocalModelRegistry.MODEL_LOCAL_TFLITE -> {
                    // LOCAL MODEL PATH
                    handleLocalModelGeneration(
                        chatId = chatId,
                        userMessage = text,
                        persona = persona,
                        assistantMsgId = assistantMsgId
                    )
                }
                else -> {
                    // EXISTING GEMINI PATH
                    geminiService.streamText(text, persona?.toDomain()).collect { token ->
                        // ... existing streaming logic
                    }
                }
            }

        } catch (e: Exception) {
            // ... existing error handling
        } finally {
            isSending = false
            _streamingMessageId.value = null
        }
    }
}
```

### Add local model generation handler
```kotlin
private suspend fun handleLocalModelGeneration(
    chatId: String,
    userMessage: String,
    persona: Persona?,
    assistantMsgId: String
) {
    try {
        // Load model on first use
        if (!localTFLiteEngine.isReady()) {
            _localModelLoading.value = true

            val loadResult = localTFLiteEngine.load(
                config = LocalModelRegistry.FlanT5Small,
                delegatePref = null // Auto-select NNAPI -> GPU -> CPU
            )

            if (loadResult.isFailure) {
                throw loadResult.exceptionOrNull()
                    ?: Exception("Failed to load local model")
            }

            _localModelLoading.value = false
        }

        // Create PersonaProfile from Persona
        val personaProfile = object : PersonaProfile {
            override val id = persona?.id ?: "default"
            override val systemPrompt = persona?.system ?: "You are a helpful AI assistant."
            override val maxTokens = persona?.extendedSettings?.let {
                // Parse JSON if needed, or default
                128
            } ?: 128
            override val temperature = 0.7f
            override val topP = 0.9f
        }

        // Stream generation
        val fullText = StringBuilder()
        val result = localTFLiteEngine.generate(
            chatId = chatId,
            userMessage = userMessage,
            persona = personaProfile,
            onToken = { token ->
                fullText.append(token)
                // Update message in DB
                viewModelScope.launch {
                    chatRepository.updateMessageText(assistantMsgId, fullText.toString())
                }
            }
        )

        // Mark as complete
        if (result.isSuccess) {
            chatRepository.updateMessageStatus(assistantMsgId, MsgStatus.SENT)

            // Ingest into memory if not incognito
            if (_chat.value?.isIncognito != true) {
                memoryEngine.ingest(
                    turn = ChatTurn(
                        chatId = chatId,
                        userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        userMessage = userMessage,
                        assistantMessage = fullText.toString(),
                        timestamp = System.currentTimeMillis()
                    ),
                    personaId = personaProfile.id,
                    incognito = false
                )
            }
        } else {
            chatRepository.updateMessageStatus(assistantMsgId, MsgStatus.ERROR)
            _localModelError.value = result.exceptionOrNull()?.message
                ?: "Local model generation failed"
        }

    } catch (e: Exception) {
        Log.e("ChatViewModel", "Local model error", e)
        chatRepository.updateMessageStatus(assistantMsgId, MsgStatus.ERROR)
        _localModelError.value = e.message ?: "Unknown error"
    } finally {
        _localModelLoading.value = false
    }
}
```

## 3. Update ChatHeader.kt

### Add loading indicator for local model
```kotlin
// In ChatHeader composable, add:
val localModelLoading by viewModel.localModelLoading.collectAsState()

// Show a pill/chip when loading local model
if (localModelLoading) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Loading local model...",
                fontSize = 11.sp
            )
        }
    }
}
```

## 4. Show delegate type chip

Add a chip showing which delegate is active (NNAPI/GPU/CPU):

```kotlin
// When local model is selected, show delegate
if (currentModel == "innovexia-local-tflite" && localTFLiteEngine.isReady()) {
    val delegate = localTFLiteEngine.getActiveDelegate()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when (delegate) {
            "NNAPI" -> Color(0xFF4CAF50).copy(alpha = 0.2f) // Green for NPU
            "GPU" -> Color(0xFF2196F3).copy(alpha = 0.2f)   // Blue for GPU
            else -> Color(0xFFFF9800).copy(alpha = 0.2f)    // Orange for CPU
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Memory, // or appropriate icon
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                delegate,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
```

## 5. Add Model Assets Directory

Create the assets directory structure:
```
app/src/main/assets/
  local_models/
    flan_t5_small_int8.tflite          (TODO: Add actual model)
    flan_t5_small_tokenizer.model       (TODO: Add actual tokenizer)
    tinyllama_1.1b_chat_q8.tflite      (TODO: Optional, larger model)
    tinyllama_tokenizer.model           (TODO: Optional)
```

Create placeholder README:
```bash
mkdir -p app/src/main/assets/local_models
echo "# Local Models\n\nTODO: Add TFLite model files here" > app/src/main/assets/local_models/README.md
```

## 6. Settings UI (Optional - Phase 2)

Add local model settings screen at `ui/settings/LocalModelSettings.kt`:

```kotlin
@Composable
fun LocalModelSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    // Settings:
    // - Model file selection (Flan-T5 / TinyLlama)
    // - Delegate preference (Auto / NNAPI / GPU / CPU)
    // - Max output tokens slider
    // - Temperature slider
    // - Top-k / Top-p controls
    // - Performance stats (tokens/sec)
    // - Storage used
}
```

## Testing Checklist

- [ ] Add TFLite dependencies to build.gradle.kts
- [ ] Add Hilt plugin to build.gradle.kts
- [ ] Sync Gradle and rebuild
- [ ] Add LOCAL provider to ModelProvider enum
- [ ] Add local model to AvailableModels list
- [ ] Add LOCAL ProviderSection to PanelContent
- [ ] Inject LocalTFLiteEngine into ChatViewModel
- [ ] Add local model routing in sendMessage()
- [ ] Test model loading on first use
- [ ] Verify NNAPI delegate on Pixel 7
- [ ] Test streaming token generation
- [ ] Verify memory ingestion works
- [ ] Test airplane mode (should work offline)
- [ ] Add model assets (when available)
- [ ] Test with actual TFLite models

## Model Asset TODOs

1. Convert FLAN-T5-Small to TFLite int8:
   - Use TensorFlow's converter
   - Quantize to int8 for better performance
   - Test input/output tensor shapes

2. Convert TinyLlama-1.1B to TFLite:
   - Quantize to int8
   - Optimize for mobile
   - Add as optional download

3. Extract/convert SentencePiece tokenizers:
   - Export .model files from HuggingFace
   - Test encoding/decoding

4. Update tensor I/O in LocalTFLiteEngine.infer():
   - Log actual shapes
   - Adjust array dimensions
   - Handle batch size correctly
