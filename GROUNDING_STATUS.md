# Grounding Implementation Status

## ✅ COMPLETED - Backend Infrastructure (100%)

### 1. Data Models
- ✅ **GroundingModels.kt** - Complete data class hierarchy
  - GroundingMetadata
  - SearchEntryPoint
  - GroundingChunk / WebSource
  - GroundingSupport / GroundingSegment
  - CitationSource

### 2. Core Service Logic
- ✅ **GroundingService.kt** - All utility functions implemented
  - `parseGroundingMetadata()` - Extract from API responses
  - `formatCitationsInline()` - Convert to markdown citations
  - `extractSources()` - Generate UI-friendly source list
  - `hasGroundingMetadata()` - Check availability
  - `getGroundingSummary()` - Debug logging

### 3. Gemini Integration
- ✅ **GeminiService.kt** - Full Tool integration
  - Imports: `Tool`, `GoogleSearch`
  - `StreamChunk` updated with `groundingMetadata` field
  - `generateReplyEnhanced()` has `groundingEnabled` parameter
  - Tool configuration:
    ```kotlin
    val tools = if (groundingEnabled) {
        listOf(Tool(googleSearch = GoogleSearch()))
    } else {
        emptyList()
    }
    ```
  - Grounding metadata parsing in stream response
  - Logging for debugging

### 4. UI Models
- ✅ **ModelSwitcher.kt** - UI toggle ready
  - `UiAiPrefs.groundingEnabled` field exists
  - Tools section with toggle switch UI
  - Professional description: "Search the web for real-time information and cite sources"

## 🚧 TODO - UI Integration & Wiring

### 1. ChatViewModel Wiring (15 min)
**File:** `ChatViewModel.kt`

**What to do:**
```kotlin
// Add state for grounding preference
private val _groundingEnabled = MutableStateFlow(false)
val groundingEnabled: StateFlow<Boolean> = _groundingEnabled.asStateFlow()

// Add setter
fun setGroundingEnabled(enabled: Boolean) {
    _groundingEnabled.value = enabled
}

// Update line 238 in sendMessage():
geminiService.generateReplyWithTokens(
    chatId = chatId,
    userText = text,
    persona = persona,
    enableThinking = false,
    groundingEnabled = _groundingEnabled.value  // ADD THIS
).collect { chunk ->
    // ... existing code

    // Store grounding metadata if present
    if (chunk.groundingMetadata != null) {
        // TODO: Optionally save to database or state
        android.util.Log.d("ChatViewModel", "Grounding data: ${GroundingService.getGroundingSummary(chunk.groundingMetadata)}")
    }
}
```

**Also update:** All other calls to `generateReplyWithTokens`:
- Line 293: `resendEdited()`
- Line 486: `regenerateAssistant()`
- Line 636: `continueResponse()`

### 2. Wire ModelSwitcher to ChatViewModel (10 min)
**File:** `ChatScreen.kt` or wherever ModelSwitcher is shown

**What to do:**
```kotlin
// In ChatScreen composable:
val groundingEnabled by viewModel.groundingEnabled.collectAsState()

// Pass to ModelSwitcherPanel when shown:
ModelSwitcherPanel(
    prefs = currentPrefs.copy(groundingEnabled = groundingEnabled),
    onPrefsChange = { newPrefs ->
        viewModel.setGroundingEnabled(newPrefs.groundingEnabled)
        // ... other preference updates
    },
    // ... other params
)
```

### 3. Citation Display UI (30-45 min)
**File:** `ResponseBubbleV2.kt`

**What to do:**
```kotlin
@Composable
fun ResponseBubbleV2(
    message: MessageEntity,
    groundingMetadata: GroundingMetadata? = null,  // ADD THIS
    // ... other params
) {
    Column {
        // Existing markdown text rendering
        // The citations [1](url) will be automatically clickable if using MarkdownText

        // Add sources section at bottom
        if (groundingMetadata != null && GroundingService.hasGroundingMetadata(groundingMetadata)) {
            Spacer(Modifier.height(12.dp))

            SourcesSection(
                sources = GroundingService.extractSources(groundingMetadata)
            )
        }
    }
}

@Composable
private fun SourcesSection(sources: List<CitationSource>) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_link),  // Use appropriate icon
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Sources (${sources.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                sources.forEach { source ->
                    SourceItem(source = source)
                }
            }
        }
    }
}

@Composable
private fun SourceItem(source: CitationSource) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = source.clickable) {
                try {
                    uriHandler.openUri(source.uri)
                } catch (e: Exception) {
                    android.util.Log.e("SourceItem", "Failed to open URI: ${e.message}")
                }
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${source.index}.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Text(
            source.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (source.clickable) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open link",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
```

### 4. Pass Grounding Data to UI (5 min)
**File:** `ChatViewModel.kt` or `MessageList.kt`

**What to do:**
- Store `groundingMetadata` from StreamChunk somewhere accessible to UI
- Options:
  1. Add `groundingMetadata: String?` (JSON) to MessageEntity in database
  2. Keep in ViewModel state: `Map<String, GroundingMetadata>`
  3. Pass directly from ChatViewModel to MessageList composable

**Recommended:** Option 2 (ViewModel state) for MVP, Option 1 for persistence

```kotlin
// In ChatViewModel:
private val _groundingDataMap = MutableStateFlow<Map<String, GroundingMetadata>>(emptyMap())
val groundingDataMap: StateFlow<Map<String, GroundingMetadata>> = _groundingDataMap.asStateFlow()

// In streaming collection (line 243):
if (chunk.groundingMetadata != null) {
    _groundingDataMap.value = _groundingDataMap.value + (modelMsgId to chunk.groundingMetadata)
}
```

```kotlin
// In MessageList:
val groundingData = groundingDataMap[message.id]
ResponseBubbleV2(
    message = message,
    groundingMetadata = groundingData,  // Pass here
    // ... other params
)
```

## 📋 Testing Checklist

1. **Enable Grounding**
   - [ ] Open ModelSwitcher dialog (click header title)
   - [ ] Toggle "Grounding with Search" ON
   - [ ] Verify toggle state is saved

2. **Test Real-Time Query**
   - [ ] Ask: "Who won Euro 2024?"
   - [ ] Expected: Response with inline citations like `[1](url), [2](url)`
   - [ ] Expected: "Sources (3)" section at bottom
   - [ ] Verify: Citations are clickable links
   - [ ] Verify: Sources list shows website titles

3. **Test Non-Grounded Query**
   - [ ] Toggle grounding OFF
   - [ ] Ask: "What is 2+2?"
   - [ ] Expected: Normal response, no citations, no sources section

4. **Verify Logging**
   - [ ] Check Logcat for "Grounding enabled - adding google_search tool"
   - [ ] Check for "Grounding: X queries, Y sources, Z citations"
   - [ ] Verify no errors in parsing

5. **Edge Cases**
   - [ ] Query that doesn't need grounding (model decides)
   - [ ] Query with many sources (5+)
   - [ ] Expand/collapse sources section
   - [ ] Click individual source links

## 📊 Estimated Time to Complete

- **ChatViewModel wiring:** 15 minutes
- **ModelSwitcher connection:** 10 minutes
- **Citation UI (ResponseBubbleV2):** 30-45 minutes
- **Testing & polish:** 20 minutes

**Total:** ~1.5-2 hours

## 🎯 Current Status Summary

**Backend:** 100% Complete ✅
- Data models ready
- Parsing logic working
- Tool integration done
- API calls configured

**UI Integration:** 0% Complete 🚧
- Need to wire ChatViewModel
- Need to connect ModelSwitcher
- Need to render citations
- Need to display sources

**Next Step:** Start with ChatViewModel wiring (15 min task) to enable end-to-end testing.
