# Personas v2 - Local-First with Cloud Backup

## Overview

Personas v2 implements a **local-first architecture** (same as chats) where personas are stored primarily in Room database with optional Firebase cloud backup. Users can create, manage, and select personas that work offline and sync when signed in.

## Key Features

- **My Personas**: Local Room storage (works for guest + signed-in)
- **Cloud Backup**: Firebase sync for signed-in users only
- **Public Personas**: Firebase-only (Innovexia) unless imported
- **Create New Persona**: Full CRUD modal with validation
- **Persona Selection**: Persisted via DataStore per ownerId
- **Guest Support**: Full CRUD in local storage, no sign-in required
- **Offline-First**: All operations work offline, sync happens in background

## Architecture

### Local Storage (Room Database)

```kotlin
@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey val id: String,              // UUID
    val ownerId: String,                     // "guest" or Firebase UID
    val name: String,
    val initial: String,                     // 1 char uppercase
    val color: Long,                         // ARGB
    val summary: String,
    val tags: List<String>,
    val system: String?,
    val createdAt: Long,                     // Timestamp millis
    val updatedAt: Long,
    val isDefault: Boolean,
    // Cloud sync metadata
    val cloudId: String?,                    // Firestore doc ID
    val lastSyncedAt: Long?                  // Last sync timestamp
)
```

### Cloud Backup (Firebase)

```
/users/{uid}/personas/{cloudId}
  - name: string
  - initial: string
  - color: long
  - summary: string
  - tags: string[]
  - system: string?
  - isDefault: boolean
  - createdAt: timestamp
  - updatedAt: timestamp

/public/personas (single document)
  - name: "Innovexia"
  - initial: "I"
  - color: <brand color>
  - summary: string
  - tags: string[]
  - system: string?
  - createdAt: timestamp
  - updatedAt: timestamp
```

### Data Flow

1. **Create/Update**: Write to Room first, sync to Firebase in background
2. **Read**: Always from Room (instant, works offline)
3. **Public Personas**: Fetched from Firebase, optionally imported to Room
4. **Sync**: CloudSyncEngine handles background Firebase sync for signed-in users

### Core Components

1. **Data Layer**
   - `data/local/entities/PersonaEntity.kt` - Room entity
   - `data/local/dao/PersonaDao.kt` - Room DAO
   - `core/persona/PersonaModels.kt` - Domain models & DTOs
   - `core/persona/PersonaRepository.kt` - Local-first operations + cloud sync
   - `core/persona/PersonaPreferences.kt` - DataStore for active persona

2. **ViewModel** (`ui/persona/`)
   - `MyPersonasViewModel.kt` - State management with Flows
   - Uses `ownerId` (guest or uid) for scoped operations
   - Methods: `start()`, `create()`, `delete()`, `importPublic()`, `rename()`, `toggleDefault()`

3. **UI Layer** (`ui/sheets/personas/`)
   - `PersonasSheetHost.kt` - Main container with ViewModel integration
   - `PersonasTabMy.kt` - My Personas tab (works for guest + signed-in)
   - `PersonasTabPublic.kt` - Public personas (Innovexia from Firebase)
   - `CreatePersonaDialog.kt` - Create persona modal with validation

## Usage

### Creating a Persona

1. Works for both guest and signed-in users
2. Click "New" button in My Personas tab
3. Fill in:
   - Name (required, 2-30 chars)
   - Initial (auto-generated, editable, 1 char)
   - Color (picker with 8 swatches)
   - Summary (0-140 chars)
   - Tags (up to 6)
4. Click "Create"
5. Saved to local Room database immediately
6. Synced to Firebase in background (signed-in only)

### Importing Public Persona

1. Navigate to Public tab
2. Click "Import" on Innovexia card
3. Persona copied to local Room database
4. Works offline, syncs later if signed in

### Selecting a Persona

1. Click "Select" on any persona card
2. Active persona ID saved to DataStore (keyed by ownerId)
3. Persona chip updates with selected persona
4. Sheet closes automatically

### Guest vs Signed-In

**Guest Users:**
- Full CRUD in local Room database
- Data scoped to `ownerId = "guest"`
- No cloud backup
- Can import public personas
- Data persists until app uninstall

**Signed-In Users:**
- Full CRUD in local Room database
- Data scoped to `ownerId = uid`
- Automatic cloud backup via CloudSyncEngine
- Can restore from cloud on new device
- Guest data can be migrated after sign-in

## State Management

### ViewModel Flows

- `my: StateFlow<List<Persona>>` - User's personas from Room (guest or signed-in)
- `public: StateFlow<List<Persona>>` - Public personas from Firebase
- `busy: StateFlow<Boolean>` - Loading state
- `error: SharedFlow<String>` - Error messages

### Persistence

- **Room**: Primary storage for all my personas (offline-first)
- **Firebase**: Cloud backup for signed-in users + public personas
- **DataStore**: Active persona ID per ownerId (guest vs uid)

## Cloud Sync

### Background Sync (CloudSyncEngine)

- Syncs unsynced personas to Firebase for signed-in users
- Tracks sync status via `cloudId` and `lastSyncedAt`
- Handles create, update, delete operations
- Runs in background, doesn't block UI

### Pull from Cloud (Restore)

- Called when user signs in on new device
- Fetches all cloud personas for uid
- Merges with local Room database
- Preserves local changes, avoids duplicates

## Validation

### Create Persona

- Name: 2-30 characters, required
- Initial: 1 uppercase character (auto-generated)
- Color: Valid ARGB long
- Summary: 0-140 characters
- Tags: Max 6 tags

### Security

- Local Room: Scoped by ownerId (guest or uid)
- Firebase: Read/write only for owner's personas
- Public personas: Read-only, write restricted to admin

## Future Enhancements

1. **Sources Tab**: Attach URLs/files to personas
2. **Memory Tab**: Persona-scoped memory items
3. **Rename Dialog**: Edit existing persona names (✓ implemented)
4. **Duplicate**: Clone existing personas
5. **System Prompts**: Custom instructions per persona
6. **Default Persona**: Auto-select in new chats (✓ implemented)
7. **Guest Data Migration**: Transfer guest personas to signed-in account

## Files Created/Modified

### Created
- `data/local/entities/PersonaEntity.kt` - Room entity
- `data/local/dao/PersonaDao.kt` - Room DAO
- `core/persona/PersonaModels.kt` - Domain models & DTOs
- `core/persona/PersonaRepository.kt` - Local-first repository
- `core/persona/PersonaPreferences.kt` - DataStore preferences
- `ui/persona/MyPersonasViewModel.kt` - ViewModel with ownerId support
- `ui/sheets/personas/CreatePersonaDialog.kt` - Create dialog
- `firestore-personas.rules` - Security rules

### Modified
- `ui/persona/PersonaModels.kt` - Added UI conversion helpers
- `ui/sheets/personas/PersonasSheetHost.kt` - ViewModel integration
- `ui/sheets/personas/PersonasTabMy.kt` - Works for guest + signed-in
- `ui/sheets/personas/PersonasTabPublic.kt` - Already compatible

## Testing Checklist

- [ ] Create persona as guest
- [ ] Create persona as signed-in user
- [ ] Create persona validation (name, initial, color, tags)
- [ ] Import Innovexia from Public tab (guest + signed-in)
- [ ] Delete persona
- [ ] Rename persona
- [ ] Toggle default persona
- [ ] Guest has full CRUD access (local only)
- [ ] Signed-in user personas sync to Firebase
- [ ] Persona selection persists per ownerId
- [ ] Offline mode: all CRUD works without network
- [ ] Sign-out → Sign-in: personas scoped correctly
- [ ] Pull from cloud restores personas
- [ ] Sources/Memory tabs show "Coming soon"

## Migration Notes

### Database Migration Required

Add `PersonaEntity` table to AppDatabase:

```kotlin
@Database(
    entities = [
        // ... existing entities
        PersonaEntity::class
    ],
    version = 9, // Increment version
    // ... rest
)
```

Create migration from version 8 to 9 to add personas table.

### Dependency Injection

Ensure `PersonaDao` is provided via Dagger/Hilt in DatabaseModule:

```kotlin
@Provides
fun providePersonaDao(db: AppDatabase): PersonaDao = db.personaDao()
```
