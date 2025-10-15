# Grounding Persistence Fix - Installation Instructions

## Problem
The grounding indicators and sources chip disappear when switching chats because:
1. Database migration 20→21 hasn't run yet (adds `groundingStatus` column)
2. App needs to be reinstalled or data cleared to run the migration

## What Was Fixed

### Code Changes (Already Complete)
✅ Added `groundingStatus` field to MessageEntity
✅ Created database migration 20→21
✅ Updated all streaming functions to save grounding data
✅ Fixed stale data issues on regeneration
✅ Added error cleanup for grounding state
✅ Fixed UI indicators for all grounding states

### Files Modified
1. **MessageEntity.kt** - Added groundingStatus field
2. **MIGRATION_20_21.kt** - Database migration (NEW)
3. **AppDatabase.kt** - Bumped version to 21, added migration
4. **ChatViewModel.kt** - Save/load grounding status, error cleanup
5. **ChatRepository.kt** - Added getMessageById(), updateMessage()
6. **ResponseBubbleV2.kt** - Fixed indicator visibility
7. **MessageList.kt** - Pass groundingStatusMap to UI
8. **ChatScreen.kt** - Collect groundingStatusMap from ViewModel

## Installation Steps

### Option 1: Clear App Data (Keeps App Installed)
```bash
# Via ADB
adb shell pm clear com.example.innovexia

# Then reopen the app - migration will run automatically
```

### Option 2: Reinstall App (Recommended for Testing)
```bash
# Uninstall
adb uninstall com.example.innovexia

# Rebuild and install
./gradlew installDebug

# Or from Android Studio: Run -> Clean and Rebuild Project, then Run
```

### Option 3: Manual Migration (If You Want to Keep Data)
Run this SQL command via Android Studio Database Inspector:
```sql
ALTER TABLE messages ADD COLUMN groundingStatus TEXT NOT NULL DEFAULT 'NONE';
```

## Verification Steps

After installation:

1. **Test New Message with Grounding:**
   - Enable grounding toggle
   - Send a message
   - You should see "Searching the web..." with animated dots
   - After completion: "Searched the web" indicator
   - Top-right: Sources chip with number

2. **Test Persistence:**
   - Navigate away from chat (go to home)
   - Come back to the same chat
   - ✅ Sources chip should still be visible
   - ✅ "Searched the web" indicator should still show

3. **Test Regeneration:**
   - Regenerate a message with grounding
   - Old sources should be REPLACED with new ones
   - No stale data

4. **Test Error Handling:**
   - Turn off internet mid-stream
   - Status should clear, no stuck "Searching..." indicator

## Expected Behavior After Fix

### During Streaming:
- "Sending..." appears first
- Once tokens start: "Searching the web..." with animated dots
- Top-right: Animated searching skeleton (3 dots pulsing)

### After Completion:
- "Searched the web" indicator (static)
- Top-right: Sources chip showing count (e.g., "18 sources")
- Click chip to see dropdown with source list

### After Switching Chats:
- ALL indicators persist
- Data loaded from database
- No loss of information

## Troubleshooting

### Issue: Still no indicators showing
**Cause:** Migration didn't run
**Fix:** Option 1 or 2 above (clear data or reinstall)

### Issue: Indicators show during streaming but disappear after
**Cause:** Database save might be failing
**Check Logs:**
```bash
adb logcat | grep -E "(ChatViewModel|groundingStatus|MIGRATION)"
```
Look for:
- "Loaded X grounding status entries from database"
- "MIGRATION_20_21: Migration completed successfully"

### Issue: Sources chip shows wrong data after regeneration
**Cause:** Shouldn't happen anymore (fixed stale data bug)
**Verify:** Check that regeneration replaces old grounding metadata

## Database Schema

After migration, the `messages` table will have:
```sql
CREATE TABLE messages (
    id TEXT PRIMARY KEY,
    ownerId TEXT NOT NULL,
    chatId TEXT NOT NULL,
    role TEXT NOT NULL,
    text TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    -- ... other fields ...
    groundingJson TEXT,              -- Existing (v20)
    groundingStatus TEXT NOT NULL DEFAULT 'NONE',  -- NEW (v21)
    -- ... other fields ...
)
```

Valid values for `groundingStatus`:
- `"NONE"` - Default, no grounding
- `"SEARCHING"` - Web search in progress
- `"SUCCESS"` - Web search completed successfully
- `"FAILED"` - Web search failed

## Testing Checklist

After installation, verify:
- [ ] "Searching the web..." shows during streaming
- [ ] Animated dots appear in indicator
- [ ] Searching skeleton shows in top-right corner
- [ ] "Searched the web" shows after completion
- [ ] Sources chip appears with count
- [ ] Clicking chip shows sources dropdown
- [ ] Indicators persist after switching chats
- [ ] Regeneration replaces old sources
- [ ] Error clears grounding indicators
- [ ] Database logs show migration success

## Need Help?

If issues persist:
1. Check logcat for errors
2. Verify database version is 21:
   ```bash
   adb shell "sqlite3 /data/data/com.example.innovexia/databases/innovexia_db 'PRAGMA user_version;'"
   ```
3. Check if groundingStatus column exists:
   ```bash
   adb shell "sqlite3 /data/data/com.example.innovexia/databases/innovexia_db '.schema messages'" | grep groundingStatus
   ```
