# Memory Engine Integration Instructions

## Quick Start: Enable the Memory Backend

### Step 1: Update PersonasSheetHost.kt

Replace the Memory tab case in `PersonasSheetHost.kt` (line ~255-276):

**OLD CODE:**
```kotlin
PersonaTab.Memory -> {
    // Memory tab - Phase 1 UI
    val selectedPersona = myPersonas.find { it.id == activePersonaId }
        ?: myPersonas.firstOrNull()

    if (selectedPersona != null) {
        com.example.innovexia.ui.persona.memory.MemoryTab(
            persona = selectedPersona,
            darkTheme = darkTheme
        )
    } else {
        Box(/* ... */) { Text("Select a persona to view memories") }
    }
}
```

**NEW CODE:**
```kotlin
PersonaTab.Memory -> {
    // Memory tab - Connected to backend
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
```

### Step 2: Test the Memory Tab

1. Run the app
2. Open Persona Sheet → Memory tab
3. You should see:
   - Empty state (no memories yet)
   - Category tabs working
   - Search bar functional
   - Enable/disable toggle persisted

### Step 3: Add Test Data (Optional)

To test with sample data, add this code somewhere in your app (e.g., in a debug menu):

```kotlin
import com.example.innovexia.memory.Mind.api.ChatTurn
import com.example.innovexia.memory.Mind.di.MindModule

// Get engine
val memoryEngine = MindModule.provideMemoryEngine(context)

// Add test memories
scope.launch {
    memoryEngine.ingest(
        turn = ChatTurn(
            chatId = "test_chat",
            userId = "user_001",
            userMessage = "I love hiking in the mountains on weekends",
            assistantMessage = "That sounds wonderful! Hiking is great exercise.",
            timestamp = System.currentTimeMillis()
        ),
        personaId = currentPersonaId,
        incognito = false
    )

    memoryEngine.ingest(
        turn = ChatTurn(
            chatId = "test_chat",
            userId = "user_001",
            userMessage = "I'm working on an Android app using Jetpack Compose",
            assistantMessage = "Great choice! Compose makes UI development much easier.",
            timestamp = System.currentTimeMillis()
        ),
        personaId = currentPersonaId,
        incognito = false
    )
}
```

## Future: Hook Into Chat Pipeline

### Add Ingestion After Each Turn

In your chat message handler (where you save messages to Room):

```kotlin
// After saving message to database
val memoryEngine = MindModule.provideMemoryEngine(context)

scope.launch {
    memoryEngine.ingest(
        turn = ChatTurn(
            chatId = chatId,
            userId = userId,
            userMessage = userMessageText,
            assistantMessage = assistantResponseText,
            timestamp = System.currentTimeMillis()
        ),
        personaId = currentPersonaId,
        incognito = chat.isIncognito
    )
}
```

### Add Context Retrieval Before LLM Call

When building the prompt for Gemini:

```kotlin
// Before calling Gemini
val memoryEngine = MindModule.provideMemoryEngine(context)
val contextBundle = memoryEngine.contextFor(
    message = userMessage,
    personaId = currentPersonaId,
    chatId = chatId
)

// Add to system prompt
val systemPrompt = buildString {
    append("You are ${persona.name}. ")

    // Add relevant memories
    if (contextBundle.longTerm.isNotEmpty()) {
        append("\n\nRelevant context from past conversations:\n")
        contextBundle.longTerm.take(5).forEach { hit ->
            append("- ${hit.memory.text}\n")
        }
    }

    // Rest of prompt...
}
```

## Troubleshooting

### Issue: "No memories found"
- Check that memory is enabled for the persona
- Verify data was ingested (check Room Inspector)
- Look for errors in logcat

### Issue: FTS search not working
- FTS4 requires text to be tokenized
- Try exact word matches first
- Check FTS index was created

### Issue: App crashes on Memory tab
- Verify all imports are correct
- Check MemoryDatabase is initialized
- Look for missing dependencies in build.gradle

### Issue: Counts not updating
- Memories are stored but counts Flow not emitting
- Check Room DAO @Query returns Flow
- Verify collectAsState in UI

## Performance Tips

1. **Batch Ingestion**: If processing many messages, use `insertAll()`
2. **Limit Results**: Default k=12 is good, don't fetch 100s at once
3. **Debounce Search**: Add delay before search query triggers
4. **Background Thread**: All Room operations already on IO dispatcher

## Database Inspection

Use Android Studio Database Inspector to view:
- `memories` table - all stored memories
- `memories_fts` table - FTS index
- `memory_vectors` table - quantized embeddings

## Next Features to Implement

1. **Chat Title Resolution**: Fetch actual chat titles for "From Chat" chips
2. **Edit Memory**: Allow users to edit memory text
3. **Export/Import**: Backup memories to JSON
4. **Analytics**: Show memory growth over time
5. **Smart Insights**: "You've mentioned X 5 times this month"

---

**Questions?** Check MEMORY_ENGINE_IMPLEMENTATION.md for architecture details.
