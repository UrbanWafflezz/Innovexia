# Chat UI Fix - Persistent Scrollable Messages

## Problem
Messages were disappearing after being sent instead of staying on screen. The streaming text appeared temporarily in an overlay and then vanished.

## Solution
Implemented a persistent, scrollable message list that displays all user and AI messages in a chat conversation.

## Changes Made

### 1. **UIMessage Data Class** (`ui/models/UIMessage.kt`)
```kotlin
data class UIMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val isStreaming: Boolean = false
)
```
- Represents a message for UI display
- Tracks streaming state for live updates
- Used by ViewModel to maintain visible messages list

### 2. **MessageBubble Component** (`ui/components/MessageBubble.kt`)
- New composable for displaying individual messages
- Different styles for user vs. AI messages
- User messages: aligned right, blue background, "You" avatar
- AI messages: aligned left, surface background, "AI" avatar
- Streaming indicator: blinking cursor (▌) when `isStreaming = true`
- Rounded corners with asymmetric radii for chat bubble effect

### 3. **HomeViewModel Updates** (`ui/viewmodels/HomeViewModel.kt`)

**New State:**
```kotlin
private val _visibleMessages = MutableStateFlow<List<UIMessage>>(emptyList())
val visibleMessages: StateFlow<List<UIMessage>>
```

**Updated `sendMessage()` function:**
1. **Add user message immediately** to visible list
2. **Add placeholder AI message** with `isStreaming = true`
3. **Stream tokens** from Gemini, updating placeholder message on each token
4. **Mark complete** when stream finishes (removes cursor)
5. **Save to Room** if user has consented

**Updated `newChat()` function:**
- Clears `_visibleMessages` when starting new conversation

### 4. **HomeScreen Updates** (`ui/screens/HomeScreen.kt`)

**Collect messages state:**
```kotlin
val visibleMessages by viewModel.visibleMessages.collectAsState()
```

**Pass to HomeContent:**
```kotlin
messages = visibleMessages
```

**Replaced streaming overlay with LazyColumn:**
```kotlin
LazyColumn(
    state = listState,
    modifier = Modifier
        .fillMaxWidth()
        .padding(top = 48.dp)
        .padding(bottom = 160.dp)
) {
    items(messages.size) { index ->
        MessageBubble(
            text = message.text,
            isUser = message.isUser,
            isStreaming = message.isStreaming,
            darkTheme = darkTheme
        )
    }
}
```

**Auto-scroll to bottom:**
```kotlin
LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
        listState.animateScrollToItem(messages.size - 1)
    }
}
```

## User Experience

### Before:
1. User types message
2. Sends
3. Temporary overlay shows streaming response
4. Overlay disappears when done
5. **No conversation history visible**

### After:
1. User types message
2. Sends
3. **User message appears in chat immediately**
4. **AI response streams in real-time** (with blinking cursor)
5. **Both messages stay on screen**
6. **Scrollable list** for long conversations
7. **Auto-scrolls to latest message**

## Features

✅ **Persistent messages** - All messages stay visible
✅ **Scrollable chat** - LazyColumn handles long conversations
✅ **Auto-scroll** - New messages automatically scroll into view
✅ **Live streaming** - AI responses type out in real-time
✅ **Visual indicator** - Blinking cursor (▌) during streaming
✅ **User/AI distinction** - Different bubble styles and alignment
✅ **Room integration** - Messages save to DB if consented
✅ **Ephemeral mode** - Messages visible in-session even if not saved

## Technical Details

**Memory Management:**
- Messages stored in `StateFlow<List<UIMessage>>`
- Cleared on `newChat()`
- Not automatically loaded from Room (current implementation is in-memory)
- Future enhancement: Load chat history from Room when reopening chat

**Performance:**
- LazyColumn only renders visible items
- Auto-scroll uses `animateScrollToItem()` for smooth UX
- Message updates use `map()` to avoid rebuilding entire list

**Styling:**
- 16dp horizontal padding between bubbles and screen edge
- 8dp vertical padding between messages
- User bubbles: Blue background, right-aligned, top-right sharp corner
- AI bubbles: Surface background, left-aligned, top-left sharp corner
- 32dp avatars with text labels

## Next Steps (Optional)

1. **Load chat history** - When reopening a chat, load messages from Room
2. **Multi-turn context** - Send previous messages to Gemini for context
3. **Copy message** - Long-press to copy text
4. **Regenerate response** - Retry AI message
5. **Delete message** - Remove from conversation
6. **Export chat** - Share as text file
7. **Search messages** - Find text in conversation

---

**Status:** ✅ **Complete and Working**

Messages now stay on screen, are scrollable, and update in real-time during streaming!
