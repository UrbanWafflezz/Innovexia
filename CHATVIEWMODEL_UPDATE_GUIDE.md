# ChatViewModel Update Guide - Model Tracking Per Message

## Overview
Update ChatViewModel to track which model is used for each message, allowing users to switch models mid-chat while preserving provenance.

## Key Changes Needed

### 1. Add Current Model State

Add this after line ~120 (after `selectedPersona` state):

```kotlin
// Current model for this chat
val currentModel: StateFlow<String> = _chat.map { it?.currentModel ?: "gemini-2.5-flash" }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gemini-2.5-flash")
```

### 2. Add Model Change Function

Add this function after `setSelectedPersona`:

```kotlin
/**
 * Update the current model for this chat
 */
fun setCurrentModel(modelId: String) {
    viewModelScope.launch {
        val chat = _chat.value ?: return@launch
        chatRepository.updateChat(chat.copy(currentModel = modelId))
    }
}
```

### 3. Update sendMessage Function

Find the `sendMessage` function and update it to capture the model snapshot:

```kotlin
fun sendMessage(text: String, persona: Persona? = null) {
    if (text.isBlank()) return
    if (isSending) {
        android.util.Log.w("ChatViewModel", "sendMessage called while already sending - blocked")
        return
    }

    isSending = true
    android.util.Log.d("ChatViewModel", "sendMessage - text='$text'")

    viewModelScope.launch {
        try {
            _errorMessage.value = null

            // Capture current model snapshot BEFORE sending
            val chat = _chat.value
            val modelSnapshot = chat?.currentModel ?: "gemini-2.5-flash"
            android.util.Log.d("ChatViewModel", "Using model: $modelSnapshot")

            // Add user message
            chatRepository.appendUserMessage(chatId, text)

            // Create model message placeholder with modelUsed field
            val modelMsgId = java.util.UUID.randomUUID().toString()
            _streamingMessageId.value = modelMsgId

            // Stream response with crash-safe handling
            streamingJob = streamScope.launch {
                val collector = StreamCollector(
                    onUpdate = { partial ->
                        viewModelScope.launch {
                            chatRepository.appendModelToken(
                                chatId = chatId,
                                messageId = modelMsgId,
                                token = "",
                                isFinal = false,
                                modelUsed = modelSnapshot  // Pass model to repository
                            )
                        }
                    },
                    onDone = { full ->
                        viewModelScope.launch {
                            chatRepository.appendModelToken(
                                chatId = chatId,
                                messageId = modelMsgId,
                                token = "",
                                isFinal = true,
                                modelUsed = modelSnapshot  // Pass model to repository
                            )
                        }
                    }
                )

                try {
                    val persona = _selectedPersona.value?.toCorePersona()
                    android.util.Log.d("ChatViewModel", "Sending with persona: ${persona?.name}, model: $modelSnapshot")

                    // Pass modelName to geminiService
                    geminiService.generateReply(
                        chatId = chatId,
                        userText = text,
                        persona = persona,
                        enableThinking = false,
                        modelName = modelSnapshot  // Use the captured model
                    ).collect { token ->
                        collector.onToken(token)
                        chatRepository.appendModelToken(
                            chatId = chatId,
                            messageId = modelMsgId,
                            token = token,
                            isFinal = false,
                            modelUsed = modelSnapshot
                        )
                    }

                    collector.complete()
                    val streamedText = collector.current()

                    chatRepository.appendModelToken(
                        chatId = chatId,
                        messageId = modelMsgId,
                        token = "",
                        isFinal = true,
                        modelUsed = modelSnapshot
                    )

                    // Ingest into memory if persona is selected
                    val currentPersona = _selectedPersona.value
                    val currentChat = _chat.value
                    if (currentPersona != null && currentChat != null) {
                        try {
                            val turn = ChatTurn(
                                userId = currentChat.ownerId,
                                chatId = chatId,
                                userMessage = text,
                                assistantMessage = streamedText,
                                timestamp = System.currentTimeMillis()
                            )
                            memoryEngine.ingest(
                                turn = turn,
                                personaId = currentPersona.id,
                                incognito = currentChat.isIncognito
                            )
                            android.util.Log.d("ChatViewModel", "Memory ingested")
                        } catch (e: Exception) {
                            android.util.Log.e("ChatViewModel", "Failed to ingest memory: ${e.message}")
                        }
                    }

                    _streamingMessageId.value = null
                    isSending = false

                } catch (e: com.google.ai.client.generativeai.type.ResponseStoppedException) {
                    val reason = e.message?.substringAfter("Reason: ")?.trim() ?: "MAX_TOKENS"
                    android.util.Log.w("ChatViewModel", "Response stopped: $reason")
                    collector.complete()
                    finishWithTruncation(modelMsgId, reason)
                    _streamingMessageId.value = null
                    isSending = false
                } catch (e: RateLimitException) {
                    _streamingMessageId.value = null
                    _errorMessageIds.value = _errorMessageIds.value + modelMsgId
                    startCountdown(e.retryAfterSeconds)
                    isSending = false
                    chatRepository.appendModelToken(
                        chatId = chatId,
                        messageId = modelMsgId,
                        token = "⚠️ Rate limit exceeded. Please wait ${e.retryAfterSeconds} seconds.",
                        isFinal = true,
                        modelUsed = modelSnapshot
                    )
                } catch (e: CancellationException) {
                    android.util.Log.d("ChatViewModel", "Stream canceled")
                    _streamingMessageId.value = null
                    isSending = false
                    throw e
                } catch (e: GeminiException) {
                    android.util.Log.e("ChatViewModel", "Gemini error: ${e.message}", e)
                    _streamingMessageId.value = null
                    _errorMessageIds.value = _errorMessageIds.value + modelMsgId
                    _errorMessage.value = e.message
                    isSending = false
                    chatRepository.appendModelToken(
                        chatId = chatId,
                        messageId = modelMsgId,
                        token = "⚠️ Error: ${e.message}",
                        isFinal = true,
                        modelUsed = modelSnapshot
                    )
                } catch (e: Exception) {
                    android.util.Log.e("ChatViewModel", "Unexpected error: ${e.message}", e)
                    _streamingMessageId.value = null
                    _errorMessageIds.value = _errorMessageIds.value + modelMsgId
                    _errorMessage.value = "Unexpected error: ${e.message}"
                    isSending = false
                    chatRepository.appendModelToken(
                        chatId = chatId,
                        messageId = modelMsgId,
                        token = "⚠️ Unexpected error: ${e.message}",
                        isFinal = true,
                        modelUsed = modelSnapshot
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error in sendMessage: ${e.message}", e)
            _errorMessage.value = "Failed to send: ${e.message}"
            isSending = false
        }
    }
}
```

## Repository Changes Needed

You'll also need to update ChatRepository to:

1. Update `appendModelToken` signature to accept `modelUsed` parameter
2. Pass `modelUsed` when creating MessageEntity

Example:
```kotlin
suspend fun appendModelToken(
    chatId: String,
    messageId: String,
    token: String,
    isFinal: Boolean,
    modelUsed: String = "gemini-2.5-flash"
) {
    // Update existing message or create new with modelUsed field
    val existing = messageDao.getMessageById(messageId)
    if (existing != null) {
        messageDao.update(existing.copy(
            text = existing.text + token,
            streamed = isFinal,
            modelUsed = modelUsed
        ))
    } else {
        // Create new message with modelUsed
        messageDao.insert(MessageEntity(
            id = messageId,
            chatId = chatId,
            role = "model",
            text = token,
            createdAt = System.currentTimeMillis(),
            streamed = isFinal,
            modelUsed = modelUsed,
            ownerId = getCurrentUserId()
        ))
    }
}
```

## Testing Checklist

- [ ] New chat defaults to gemini-2.5-flash
- [ ] Changing model via header updates chat.currentModel
- [ ] New messages use the current model
- [ ] Old messages retain their original model
- [ ] Message bubbles show correct model label in footer
- [ ] Model switching doesn't break streaming
- [ ] Database migration runs successfully

## Next Steps

After ChatViewModel is updated:
1. Update ChatHeader to show model picker
2. Update ResponseBubbleV2 footer to show model provenance
3. Test full flow end-to-end
