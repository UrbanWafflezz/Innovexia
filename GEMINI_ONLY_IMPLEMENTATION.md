# Gemini 2.5 Only Implementation - Progress Report

## Completed Tasks ✓

### 1. Model Registry Created
- **File**: `app/src/main/java/com/example/innovexia/core/ai/ModelIds.kt`
- Created object with three Gemini 2.5 model IDs (Pro, Flash, Flash Lite)
- Added `GeminiModels` list with display metadata
- Helper functions for model label lookup

### 2. Database Schema Updated
- **ChatEntity**: Added `currentModel: String = "gemini-2.5-flash"` field
- **MessageEntity**: Added `modelUsed: String = "gemini-2.5-flash"` field
- Both entities already had these fields in the codebase

### 3. Database Migration Created
- **File**: `app/src/main/java/com/example/innovexia/data/local/migrations/MIGRATION_15_16.kt`
- Adds `currentModel` column to `chats` table
- Adds `modelUsed` column to `messages` table
- Both default to "gemini-2.5-flash"
- Includes logging for debugging

### 4. AppDatabase Updated
- Version bumped from 15 to 16
- MIGRATION_15_16 added to imports and migration list
- Version comment updated

## Remaining Tasks

### 5. Delete Local Model Files
Files to remove:
- `app/src/main/java/com/example/innovexia/local/LocalModelRegistry.kt`
- `app/src/main/java/com/example/innovexia/local/LocalTFLiteEngine.kt`
- `app/src/main/java/com/example/innovexia/local/ModelDownloadManager.kt`
- `app/src/main/java/com/example/innovexia/local/generation/` directory
- `app/src/main/java/com/example/innovexia/local/nn/` directory
- `app/src/main/java/com/example/innovexia/local/di/` directory
- `app/src/main/java/com/example/innovexia/ui/components/ModelDownloadDialog.kt`
- Any `local_models/` assets

### 6. Update ModelSwitcher Component
Current file: `app/src/main/java/com/example/innovexia/ui/components/ModelSwitcher.kt`

Changes needed:
- Remove LOCAL, OPENAI, CLAUDE, PERPLEXITY providers
- Keep only GOOGLE provider with three Gemini 2.5 models
- Remove download functionality
- Remove ModelDownloadDialog references
- Simplify UI to show only available Gemini models

### 7. Update SettingsSheet (AI Settings)
File: `app/src/main/java/com/example/innovexia/ui/sheets/SettingsSheet.kt`

Changes needed:
- Remove local model selection UI
- Remove "Download models" section
- Remove delegate/quantization options
- Keep only: Default Model (Gemini 2.5 radio list), Temperature, Max tokens

### 8. Create GeminiEngine Router
New approach for `GeminiService`:
- Add `modelName` parameter to existing methods
- Update DI to provide single flexible GeminiService instead of three separate clients
- OR: Keep current implementation since it already accepts `modelName` parameter

### 9. Update ChatViewModel
File: `app/src/main/java/com/example/innovexia/ui/viewmodels/ChatViewModel.kt`

Changes needed:
- Add `currentModel` state from chat entity
- When sending message, capture `chat.currentModel`
- Pass model ID to `geminiService.generateReply(modelName = modelSnapshot, ...)`
- Save `modelUsed` field when creating assistant message
- Add `onModelChange` function to update `chat.currentModel`

### 10. Update Response Bubble Footer
File: Find ResponseBubbleV2 component

Changes needed:
- Add model provenance label to footer
- Display `message.modelUsed` using `getModelLabel()`
- Format: "Gemini 2.5 Flash • 4:35 PM"

### 11. Update ChatHeader to show model picker
File: `app/src/main/java/com/example/innovexia/ui/chat/ChatHeader.kt`

Changes needed:
- Add current model display/dropdown
- Call ModelSwitcherPanel when clicked
- Update chat's currentModel when changed

### 12. Preferences/Settings Update
Create or update `AiPreferences` data class:
```kotlin
data class AiPreferences(
    val defaultModel: String = ModelIds.GEM_FLASH,
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 8192,
    val safetyLevel: String = "Standard"
)
```

Store in DataStore or SharedPreferences.

## Next Steps

1. Delete local model infrastructure files
2. Create simplified ModelSwitcher (Gemini only)
3. Update SettingsSheet AI tab
4. Update ChatViewModel for per-message model tracking
5. Update response bubble footer
6. Test model switching and provenance display

## Notes

- The existing GeminiService already accepts `modelName` parameter in `generateReply()`
- Database entities and migration are ready
- Main work is UI cleanup and ViewModel integration
