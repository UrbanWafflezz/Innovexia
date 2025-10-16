# Database Migration Fix

## Problem
The app crashes on startup with error:
```
Migration didn't properly handle: usage(com.example.innovexia.data.local.entities.UsageEntity)
```

This happens because a previous incomplete migration created tables with the wrong schema.

## Solution

You have 3 options:

### Option 1: Clear App Data (Recommended)
1. Go to **Settings** → **Apps** → **Innovexia**
2. Tap **Storage**
3. Tap **Clear Data** (or **Clear Storage**)
4. Relaunch the app

⚠️ **Warning**: This will delete all local data (chats, settings)

### Option 2: Uninstall and Reinstall
1. Uninstall Innovexia
2. Reinstall from Android Studio or APK
3. Launch the app

⚠️ **Warning**: This will delete all local data (chats, settings)

### Option 3: Rebuild and Force Clean Install
```bash
cd "c:\Users\Kobes Work Account\Documents\Innovexia"
./gradlew clean
./gradlew installDebug
# Then manually clear app data as in Option 1
```

## What Was Fixed

The migration file [Migration_9_10.kt](app/src/main/java/com/example/innovexia/data/local/migrations/Migration_9_10.kt) has been updated to:

1. **Drop existing tables** before creating new ones (prevents schema conflicts)
2. **Remove DEFAULT values** from column definitions (Room doesn't expect them)
3. **Use correct schema** matching Room entity definitions

## Changes Made:

**Before:**
```sql
CREATE TABLE IF NOT EXISTS usage (
    ...
    tokensIn INTEGER NOT NULL DEFAULT 0,  -- Wrong: Room doesn't expect DEFAULT
    ...
)
```

**After:**
```sql
DROP TABLE IF EXISTS usage;  -- Clean slate
CREATE TABLE usage (
    ...
    tokensIn INTEGER NOT NULL,  -- Correct: No DEFAULT value
    ...
)
```

## Future Migrations

Going forward, ensure migrations:
- Match the exact schema Room generates
- Don't include `DEFAULT` values in columns
- Use `DROP TABLE IF EXISTS` before `CREATE TABLE` to avoid conflicts

## Testing

After clearing app data:
1. Launch app
2. Sign in
3. Go to Profile → Usage tab
4. Verify no crashes
5. Check that subscription data loads

The database should now be at **version 10** with all subscription and usage tables properly created.
