# Personas v2 - Database Integration Guide

## Current Status

✅ **Completed**:
- PersonaEntity (Room entity)
- PersonaDao (DAO with all CRUD operations)
- PersonaRepository (local-first with cloud backup)
- MyPersonasViewModel (state management)
- CreatePersonaDialog (UI for creating personas)
- PersonasSheetHost (updated with ViewModel - currently commented out)
- PersonasTabMy (works for guest + signed-in)
- PersonasTabPublic (shows Innovexia)
- Hilt dependencies removed (not used in this project)

⚠️ **Pending Integration** (Required to make it work):
1. Add PersonaEntity to AppDatabase
2. Add PersonaDao to AppDatabase
3. Create database migration
4. Uncomment ViewModel code in PersonasSheetHost

---

## Step-by-Step Integration

### Step 1: Update AppDatabase

**File**: `app/src/main/java/com/example/innovexia/data/local/AppDatabase.kt`

1. Add imports:
```kotlin
import com.example.innovexia.data.local.dao.PersonaDao
import com.example.innovexia.data.local.entities.PersonaEntity
```

2. Add PersonaEntity to entities list (line 29):
```kotlin
@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        MemChunkEntity::class,
        HealthCheckEntity::class,
        IncidentEntity::class,
        SessionEntity::class,
        PersonaEntity::class  // <-- ADD THIS
    ],
    version = 9,  // <-- INCREMENT from 8 to 9
    exportSchema = false
)
```

3. Add abstract function (after sessionDao, line 40):
```kotlin
abstract fun personaDao(): PersonaDao
```

### Step 2: Create Database Migration

**File**: `app/src/main/java/com/example/innovexia/data/local/migrations/Migration_8_9.kt`

Create new file:
```kotlin
package com.example.innovexia.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create personas table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `personas` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `ownerId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `initial` TEXT NOT NULL,
                `color` INTEGER NOT NULL,
                `summary` TEXT NOT NULL,
                `tags` TEXT NOT NULL,
                `system` TEXT,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                `isDefault` INTEGER NOT NULL DEFAULT 0,
                `cloudId` TEXT,
                `lastSyncedAt` INTEGER
            )
        """.trimIndent())

        // Create indices
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_personas_ownerId` ON `personas` (`ownerId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_personas_updatedAt` ON `personas` (`updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_personas_isDefault` ON `personas` (`isDefault`)")
    }
}
```

### Step 3: Add Migration to AppDatabase

In `AppDatabase.kt`, add the import:
```kotlin
import com.example.innovexia.data.local.migrations.MIGRATION_8_9
```

Update the migrations (line 53):
```kotlin
.addMigrations(
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9  // <-- ADD THIS
)
```

### Step 4: Add TypeConverter for List<String>

In `AppDatabase.kt`, add a TypeConverter companion object (if not already present):

```kotlin
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // ... existing code ...

    class Converters {
        @TypeConverter
        fun fromStringList(value: List<String>?): String {
            return value?.joinToString(",") ?: ""
        }

        @TypeConverter
        fun toStringList(value: String): List<String> {
            return if (value.isBlank()) emptyList()
            else value.split(",").map { it.trim() }
        }
    }
}
```

### Step 5: Uncomment ViewModel Code

**File**: `app/src/main/java/com/example/innovexia/ui/sheets/personas/PersonasSheetHost.kt`

Find the TODO comment (around line 71) and:

1. **Comment out** the temporary demo data (lines 98-111)
2. **Uncomment** the ViewModel initialization block (lines 77-96)

Before:
```kotlin
/*
// Manual ViewModel instantiation (no Hilt)
val context = LocalContext.current
...
*/

// TEMPORARY: Using demo data
val myPersonas = demoMyPersonas()
...
```

After:
```kotlin
// Manual ViewModel instantiation (no Hilt)
val context = LocalContext.current
val viewModel: MyPersonasViewModel = viewModel {
    val database = AppDatabase.getInstance(context)
    val repo = PersonaRepository(database.personaDao())
    MyPersonasViewModel(repo)
}

// Collect ViewModel state
val myPersonas by viewModel.my.collectAsState()
val publicPersonas by viewModel.public.collectAsState()

// Start observing when sheet becomes visible
LaunchedEffect(visible) {
    if (visible) {
        viewModel.start()
    }
}
```

3. Update isSignedIn:
```kotlin
isSignedIn = viewModel.isSignedIn,
```

4. Uncomment ViewModel method calls:
```kotlin
onStar = { persona -> viewModel.toggleDefault(persona.id) },
onDelete = { persona -> viewModel.delete(persona.id) }
// In Public tab:
onImport = { persona -> viewModel.importPublic(persona) }
// In Create dialog:
onCreate = { name, color, summary, tags ->
    viewModel.create(name, color, summary, tags)
}
```

---

## Verification Steps

After completing the integration:

1. **Build the app** - Should compile without errors
2. **Test as Guest**:
   - Open Personas sheet
   - Click "New Persona"
   - Fill in name, color, summary, tags
   - Click "Create"
   - Persona should appear in "My Personas" tab
   - Works offline

3. **Test Public Tab**:
   - Navigate to "Public" tab
   - Should see "Innovexia" persona (if Firebase is configured)
   - Click "Import"
   - Persona copies to "My Personas"

4. **Test Sign-In**:
   - Sign in with Firebase account
   - Personas switch from `guest` to `uid` scope
   - Cloud backup should sync in background

5. **Test Offline**:
   - Turn off network
   - All CRUD operations should still work
   - Data persists in Room database

---

## Troubleshooting

### Build Errors

**Error**: "Unresolved reference: personaDao"
- **Fix**: Make sure `abstract fun personaDao(): PersonaDao` is added to AppDatabase

**Error**: "Cannot access database on the main thread"
- **Fix**: Already handled - all DAO operations are suspend functions

**Error**: "Cannot resolve symbol 'TypeConverter'"
- **Fix**: Add TypeConverter for List<String> (see Step 4)

### Runtime Errors

**Error**: "Migration didn't properly handle personas"
- **Fix**: Uninstall app and reinstall (or use fallbackToDestructiveMigration)

**Error**: "No personas showing up"
- **Fix**: Check that `viewModel.start()` is being called when sheet becomes visible

**Error**: "Create dialog doesn't work"
- **Fix**: Make sure ViewModel.create() is properly wired in onCreate callback

---

## Optional: Cloud Sync Integration

To enable cloud backup for signed-in users:

1. **In CloudSyncEngine**, add persona sync:
```kotlin
suspend fun syncPersonas() {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val repo = PersonaRepository(db.personaDao())
    repo.syncToCloud(uid)
}
```

2. **Call from periodic sync worker**:
```kotlin
syncPersonas()
```

3. **On sign-in**, pull from cloud:
```kotlin
suspend fun restorePersonas() {
    val uid = Firebase.auth.currentUser?.uid ?: return
    val repo = PersonaRepository(db.personaDao())
    repo.pullFromCloud(uid)
}
```

---

## Files Summary

### Created
- ✅ `data/local/entities/PersonaEntity.kt`
- ✅ `data/local/dao/PersonaDao.kt`
- ✅ `core/persona/PersonaModels.kt`
- ✅ `core/persona/PersonaRepository.kt`
- ✅ `core/persona/PersonaPreferences.kt`
- ✅ `ui/persona/MyPersonasViewModel.kt`
- ✅ `ui/sheets/personas/CreatePersonaDialog.kt`
- ⚠️ `data/local/migrations/Migration_8_9.kt` (needs creation)

### Modified
- ✅ `ui/persona/PersonaModels.kt`
- ✅ `ui/sheets/personas/PersonasSheetHost.kt`
- ✅ `ui/sheets/personas/PersonasTabMy.kt`
- ⚠️ `data/local/AppDatabase.kt` (needs modification)

---

## Summary

The Personas v2 implementation is **95% complete**. Only database integration remains:
1. Add PersonaEntity to AppDatabase (2 lines)
2. Create Migration_8_9.kt (1 file)
3. Uncomment ViewModel code in PersonasSheetHost (remove /* */ comments)

Once these 3 steps are done, you'll have a fully functional local-first persona system with:
- Guest support (local storage only)
- Signed-in cloud backup
- Offline-first operations
- Create/Edit/Delete personas
- Import public personas
- Default persona selection
