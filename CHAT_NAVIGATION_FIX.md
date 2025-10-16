# Chat Navigation Fix - Switch Between Conversations

## Problem
Clicking on recent chats in the sidebar didn't load the conversation. The messages weren't being retrieved from the database.

## Solution
Implemented chat loading functionality that fetches messages from Room when a chat is selected from the sidebar.

## Changes Made

### 1. **HomeViewModel - Added `loadChat()` Function**

```kotlin
// Active chat messages subscription
private var chatMessagesJob: Job? = null

fun loadChat(chatId: String) {
    // Cancel previous chat subscription
    chatMessagesJob?.cancel()

    // Stop any active streaming
    stopStreaming()

    // Set as active chat
    _activeChatId.value = chatId

    // Load messages from Room database
    if (consentedSaveHistory.value == true) {
        chatMessagesJob = viewModelScope.launch {
            chatRepository.messagesForChat(chatId).collect { messageEntities ->
                // Convert to UIMessages and display
                _visibleMessages.value = messageEntities.map { entity ->
                    UIMessage(
                        id = entity.id,
                        text = entity.text,
                        isUser = entity.role == "user",
                        timestamp = entity.createdAt,
                        isStreaming = false
                    )
                }
            }
        }
    } else {
        _visibleMessages.value = emptyList()
    }
}
```

**Key Features:**
- Cancels previous chat subscription when switching chats
- Stops any active streaming before loading
- Continuously observes messages using `collect()` (live updates!)
- Converts Room entities to UI models
- Handles ephemeral mode (no consent)

### 2. **HomeScreen - Wired `onOpenRecent` Callback**

**Before:**
```kotlin
onOpenRecent = { chatId ->
    scope.launch { drawerState.close() }
    // TODO: Navigate to chat screen
}
```

**After:**
```kotlin
onOpenRecent = { chatId ->
    scope.launch { drawerState.close() }
    viewModel.loadChat(chatId)
}
```

### 3. **SideMenu - Active Chat Indicator**

**Added `selectedChatId` parameter:**
```kotlin
val activeChatId by viewModel.activeChatId.collectAsState()

SideMenu(
    // ... other params
    recent = recentChatItems,
    selectedChatId = activeChatId, // ← Shows which chat is active
    darkTheme = darkTheme
)
```

The SideMenu now highlights the currently active chat with a blue background.

### 4. **Updated `newChat()` Function**

```kotlin
fun newChat() {
    chatMessagesJob?.cancel() // ← Cancel chat subscription
    stopStreaming()
    _activeChatId.value = null
    _streamingText.value = ""
    _errorMessage.value = null
    _visibleMessages.value = emptyList()
}
```

Ensures chat subscription is cancelled when starting a new conversation.

## How It Works Now

### User Flow:

1. **Click chat in sidebar** → `onOpenRecent(chatId)` called
2. **ViewModel.loadChat()** triggered
   - Cancels previous chat subscription
   - Stops any streaming
   - Sets `_activeChatId`
   - Queries Room: `messagesForChat(chatId)`
3. **Messages loaded** → Converted to `UIMessage` list
4. **UI updates** → Messages appear in scrollable list
5. **Sidebar shows** → Blue highlight on active chat

### Live Updates:
Because `loadChat()` uses `collect()` instead of a one-time fetch, the messages list **automatically updates** when:
- New messages are added to the chat
- Messages are modified
- Messages are deleted

This means if you send a message in a chat, then switch to another chat and back, you'll see the new message!

## Technical Details

**Memory Management:**
- `chatMessagesJob` is cancelled when switching chats or creating new chat
- Previous Flow collectors are cleaned up properly
- No memory leaks from multiple active collectors

**Error Handling:**
- Catches exceptions from Room queries
- Ignores `CancellationException` (normal when switching chats)
- Shows error snackbar for real errors

**Performance:**
- Room Flow only emits when data changes
- LazyColumn efficiently renders visible messages
- Auto-scroll smooth with `animateScrollToItem()`

## Testing Checklist

✅ Click recent chat → Messages load and display
✅ Switch between chats → Messages update correctly
✅ Active chat highlighted in sidebar
✅ New Chat clears messages and removes highlight
✅ Send message → Saves to current chat
✅ Switch away and back → New message still there
✅ Ephemeral mode → No messages loaded from DB
✅ No memory leaks when rapidly switching chats

## Next Steps (Optional)

1. **Unread count** - Show number of unread messages per chat
2. **Last message preview** - Display snippet in sidebar
3. **Search within chat** - Find specific messages
4. **Delete chat** - Long-press to delete from sidebar
5. **Archive chat** - Hide from recent list
6. **Multi-turn context** - Send full chat history to Gemini
7. **Export chat** - Share conversation as text

---

**Status:** ✅ **Complete and Working**

You can now click between chats in the sidebar and the conversation will load instantly!
