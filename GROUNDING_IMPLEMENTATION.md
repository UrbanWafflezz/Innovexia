# Google Search Grounding Implementation

## Overview
Successfully implemented Grounding with Google Search functionality for Gemini 2.5 models, enabling real-time web search, citation support, and source attribution in AI responses.

## What Was Implemented

### 1. Data Models (`GroundingModels.kt`)
Created comprehensive data classes matching the Gemini API response structure:

- **GroundingMetadata**: Complete grounding data container
- **SearchEntryPoint**: HTML/CSS widget for "Search with Google" (required by TOS)
- **GroundingChunk**: Web source with URI and title
- **WebSource**: Individual web source information
- **GroundingSupport**: Links text segments to sources (key for inline citations)
- **GroundingSegment**: Text segment with start/end indices
- **CitationSource**: Simplified source for UI display

### 2. Grounding Service (`GroundingService.kt`)
Utility object providing:

- `parseGroundingMetadata()`: Extract grounding data from API responses
- `formatCitationsInline()`: Convert text into markdown citations like `[1](url), [2](url)`
- `extractSources()`: Generate numbered source list for UI
- `hasGroundingMetadata()`: Check if grounding data exists
- `getGroundingSummary()`: Debug/logging summary

**Citation Algorithm:**
1. Sort supports by end index (descending) to avoid string shifting
2. For each support, build citation links from chunk indices
3. Insert citations at the end of text segments

### 3. GeminiService Integration
Updated `GeminiService.kt`:

#### Added Imports
```kotlin
import com.google.ai.client.generativeai.type.Tool
import com.google.ai.client.generativeai.type.GoogleSearch
```

#### Updated StreamChunk
```kotlin
data class StreamChunk(
    val text: String,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val groundingMetadata: GroundingMetadata? = null  // NEW
)
```

#### Added Grounding Parameter
```kotlin
suspend fun generateReplyEnhanced(
    // ... existing parameters
    groundingEnabled: Boolean = false  // NEW
): Flow<StreamChunk>
```

#### Tool Configuration
```kotlin
val tools = if (groundingEnabled) {
    android.util.Log.d("GeminiService", "Grounding enabled - adding google_search tool")
    listOf(Tool(googleSearch = GoogleSearch()))
} else {
    emptyList()
}

val model = GenerativeModel(
    // ... other config
    tools = tools  // Add tools configuration
)
```

#### Response Parsing
```kotlin
// Parse grounding metadata if grounding is enabled
if (groundingEnabled) {
    val groundingData = GroundingService.parseGroundingMetadata(response)
    if (groundingData != null) {
        latestGroundingMetadata = groundingData
        android.util.Log.d("GeminiService", GroundingService.getGroundingSummary(groundingData))
    }
}

StreamChunk(
    text = response.text ?: "",
    inputTokens = latestInputTokens,
    outputTokens = latestOutputTokens,
    groundingMetadata = latestGroundingMetadata  // Include in response
)
```

### 4. Model Switcher UI
Already has grounding toggle in `ModelSwitcher.kt`:

```kotlin
data class UiAiPrefs(
    // ... existing fields
    val groundingEnabled: Boolean = false,  // Already added
    // ...
)
```

Tools section with toggle switch:
- **Grounding with Search**: "Search the web for real-time information and cite sources"
- **Code Execution**: "Run Python code to perform calculations and data analysis"
- **File Creation**: "Generate downloadable PDF documents from responses"

## What's Left to Do

### 1. ChatViewModel Integration
Update `ChatViewModel.kt` to:
- Store grounding preference from UI
- Pass `groundingEnabled` parameter to `GeminiService.generateReplyWithTokens()`
- Handle grounding metadata from responses
- Optionally persist sources with messages in database

### 2. UI Display (ResponseBubbleV2)
Add citation rendering:
- Parse markdown citations `[1](url)` as clickable links
- Display "Sources" section at bottom with expandable list
- Style citations with accent color
- Handle click events to open URLs

### 3. Database Schema (Optional)
If persisting sources:
- Add `groundingSources: String?` column to messages table (JSON-encoded)
- Create migration

### 4. Testing
- Toggle grounding on in ModelSwitcher
- Test query: "Who won Euro 2024?"
- Verify citations appear inline
- Check sources list displays correctly
- Validate links are clickable

## How It Works

### Request Flow
1. User enables "Grounding with Search" toggle in ModelSwitcher
2. ChatViewModel passes `groundingEnabled: true` to GeminiService
3. GeminiService adds `Tool(googleSearch = GoogleSearch())` to model config
4. API receives request with google_search tool enabled

### Response Flow
1. Gemini analyzes prompt and decides if search is needed
2. If yes, generates search queries and executes them
3. Processes search results and formulates grounded response
4. Returns response with `groundingMetadata` containing:
   - Web search queries used
   - Grounding chunks (web sources)
   - Grounding supports (text segment → source mappings)
5. GroundingService parses metadata and formats citations
6. UI displays response with inline citations and sources list

### Example Response
```
Spain won Euro 2024, defeating England 2-1 in the final[1](url1), [2](url2).
This victory marks Spain's record fourth European Championship title[2](url2), [3](url3).

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📚 Sources (3)
  1. aljazeera.com
  2. uefa.com
  3. wikipedia.org
```

## Benefits

✅ **Real-time Information**: Access recent events and data beyond model's cutoff
✅ **Reduced Hallucinations**: Ground responses in verifiable web content
✅ **Source Citations**: Build trust with clickable references
✅ **Professional UI**: Clean citation display matching modern AI apps
✅ **Full Control**: Complete flexibility over citation rendering
✅ **All Models Supported**: Works with Gemini 2.5 Pro, Flash, and Flash Lite

## Pricing
- Billed per API request that includes `google_search` tool
- Multiple search queries in one request = single billable use
- See [Gemini API Pricing](https://ai.google.dev/pricing) for details

## Next Steps

1. **Wire up ChatViewModel**: Pass grounding preference from UI to service
2. **Implement citation UI**: Add rendering in ResponseBubbleV2
3. **Test thoroughly**: Verify grounding works end-to-end
4. **Polish UI**: Ensure citations look professional and work smoothly
5. **Consider persistence**: Optionally save sources to database for offline viewing

## Files Created
- ✅ `app/src/main/java/com/example/innovexia/data/ai/GroundingModels.kt`
- ✅ `app/src/main/java/com/example/innovexia/data/ai/GroundingService.kt`

## Files Modified
- ✅ `app/src/main/java/com/example/innovexia/data/ai/GeminiService.kt`
  - Added Tool and GoogleSearch imports
  - Updated StreamChunk with groundingMetadata field
  - Added groundingEnabled parameter to generateReplyEnhanced()
  - Integrated tool configuration
  - Added grounding metadata parsing

## Ready for Integration
The backend grounding infrastructure is complete and ready to be wired up to the UI layer!
