# Grounding Search Bubble Implementation

## Overview
Implemented a dedicated response bubble (`GroundingSearchBubble`) for web-grounded AI responses, matching the dedicated composer approach used by `WebSearchComposer`.

## Architecture

### Component Hierarchy
```
ChatScreen / HomeScreen
    ↓
MessageList
    ↓
    ├── GroundingSearchBubble (for messages with groundingStatus)
    └── ResponseBubbleV2 (for regular AI responses)
```

### Decision Logic
MessageList now checks for grounding status and routes messages accordingly:

```kotlin
when {
    // Grounded responses → GroundingSearchBubble
    message.role == "model" && groundingStatusMap[messageId] != null -> {
        GroundingSearchBubble(...)
    }

    // Regular responses → ResponseBubbleV2
    message.role == "model" -> {
        ResponseBubbleV2(...)
    }
}
```

## Component Features

### GroundingSearchBubble
**Location**: `app/src/main/java/com/example/innovexia/ui/chat/bubbles/GroundingSearchBubble.kt`

**Visual Design**:
- Blue accent theme matching `WebSearchComposer` (Color: `#4A90E2`)
- Distinct bubble background (blue tint vs. gray for regular responses)
- 1.5dp border with blue accent
- Dedicated grounding header with status

**Key Features**:

1. **Header Section**
   - Shows grounding status with icon
   - Animated dots during search
   - Status messages:
     - "Preparing search" - When sending
     - "Searching the web..." - During search
     - "Web search results" - Success
     - "Web search unavailable" - Failed

2. **Search Queries Section**
   - Displays the actual search queries used by Gemini
   - Shown in a dedicated card with search icon
   - Only visible when `webSearchQueries` is available

3. **Sources Display**
   - **Top-right badge**: Shows source count, clickable dropdown
   - **Footer sources list**: Shows first 5 sources with clickable links
   - Each source shows:
     - Title (from grounding metadata)
     - Domain extracted from URL
     - "Open in new" icon
   - Tappable to open in browser

4. **Content Rendering**
   - Full markdown support (reuses `MarkdownBody` from ResponseBubbleV2)
   - Streaming indicator with blue accent dots
   - Sending indicator with animated dots

5. **Footer**
   - Sources list (collapsible)
   - Model name display
   - Timestamp (12-hour format)

## Data Flow

### Grounding Status Enum
```kotlin
enum class GroundingStatus {
    NONE,           // No grounding requested
    SEARCHING,      // Web search in progress
    SUCCESS,        // Search completed with results
    FAILED          // Search failed or returned no results
}
```

### Grounding Metadata
```kotlin
data class GroundingMetadata(
    val webSearchQueries: List<String>?,        // Queries used
    val searchEntryPoint: SearchEntryPoint?,    // "Search with Google" widget
    val groundingChunks: List<GroundingChunk>,  // Source chunks with titles
    val searchResultUrls: List<String>          // Fallback URLs
)
```

### Source Extraction Priority
1. **Primary**: `groundingChunks` (has titles + URIs)
2. **Fallback**: `searchResultUrls` (URIs only, domain used as title)

## Files Modified

### 1. Created: `GroundingSearchBubble.kt`
New component with all grounding-specific UI logic.

### 2. Updated: `MessageList.kt`
- Added import for `GroundingSearchBubble`
- Added conditional logic to route messages:
  - Messages with `groundingStatus` → `GroundingSearchBubble`
  - Regular messages → `ResponseBubbleV2`
- No changes to function signature (already had grounding params)

### 3. No Changes Required:
- **ChatScreen.kt**: Already passes `groundingDataMap` and `groundingStatusMap`
- **HomeScreen.kt**: Already passes grounding data to MessageList
- **ViewModel layer**: Already provides grounding state flows

## Visual Comparison

### WebSearchComposer (Input)
- Blue accent theme
- Globe icon
- "Grounding" chip
- "Search the web..." placeholder

### GroundingSearchBubble (Output)
- Blue accent theme
- Globe icon in header
- "Web search results" status
- Search queries section
- Prominent sources display

## Usage Example

When a user enables grounding and sends a message:

1. **Composer**: `WebSearchComposer` appears with blue theme
2. **User sends message**: "What are the latest AI developments?"
3. **Bubble shows**:
   - "Searching the web..." with animated dots
   - Blue-tinted bubble
4. **As response streams**:
   - Content appears with markdown formatting
   - Blue streaming dots
5. **When complete**:
   - Shows search queries: ["latest AI developments 2025"]
   - Displays sources in footer (e.g., TechCrunch, ArXiv, etc.)
   - Source count badge at top-right
   - Clickable source links to open in browser

## Benefits

1. **Visual Consistency**: Matches WebSearchComposer's blue theme
2. **Clear Distinction**: Users immediately know when web search was used
3. **Source Transparency**: Sources are prominent and accessible
4. **Reduced Confusion**: Regular AI responses don't show grounding UI
5. **Extensibility**: Easy to add more grounding-specific features later

## Testing Checklist

- [ ] Enable grounding toggle in AttachmentToolbar
- [ ] Send a message requiring web search
- [ ] Verify GroundingSearchBubble appears with blue theme
- [ ] Check "Searching the web..." indicator during streaming
- [ ] Verify search queries section appears
- [ ] Click sources badge and test dropdown
- [ ] Click source links to open in browser
- [ ] Disable grounding, verify ResponseBubbleV2 used for regular responses
- [ ] Test in both dark and light themes

## Future Enhancements

Potential improvements:
1. Inline citations (link text segments to specific sources)
2. "Search with Google" entry point widget (required by ToS)
3. Source preview on hover/long-press
4. Copy source citations feature
5. Source reliability indicators
6. Search query refinement feedback
