# Persona Edit Functionality - Fix Summary

## Problem
When clicking "Edit" on a persona, the dialog opened but showed empty fields with "Untitled" instead of loading the existing persona data (e.g., "Innovexia").

## Root Causes

### 1. **Shared ViewModel Instance**
The create and edit dialogs were using the same ViewModel instance, causing state pollution.

### 2. **Missing Data Conversion**
The `toPersonaDraftDto()` function needed to handle backward compatibility with old simple persona format.

### 3. **No Reset on Create**
When opening the dialog for creating a new persona, it wasn't resetting the previous state.

## Fixes Applied

### 1. **Unique ViewModel Keys** ✅
[CreatePersonaDialog.kt:43](file:///c:/Users/Kobes%20Work%20Account/Documents/Innovexia/app/src/main/java/com/example/innovexia/ui/sheets/personas/CreatePersonaDialog.kt#L43)

```kotlin
viewModel: CreatePersonaViewModel = viewModel(key = "create_persona_${editPersonaId ?: "new"}")
```

Each edit session now gets its own ViewModel instance.

### 2. **Improved Data Loading** ✅
[CreatePersonaViewModel.kt:262-293](file:///c:/Users/Kobes%20Work%20Account/Documents/Innovexia/app/src/main/java/com/example/innovexia/ui/sheets/personas/CreatePersonaViewModel.kt#L262-L293)

```kotlin
private fun DocumentSnapshot.toPersonaDraftDto(): PersonaDraftDto {
    // Handle old simple persona format (backward compatibility)
    val name = getString("name") ?: ""
    val initial = getString("initial") ?: name.firstOrNull()?.uppercase()?.toString() ?: ""
    val summary = getString("summary") // Old field name
    val bio = getString("bio") ?: summary ?: "" // Try bio first, fallback to summary

    // ... handles both old and new format
}
```

**Backward Compatibility:**
- `summary` field (old) → `bio` field (new)
- `system` as String (old) → `system` as Map (new)
- Auto-generate `initial` from `name` if missing

### 3. **Smart Reset Logic** ✅
[CreatePersonaDialog.kt:56-63](file:///c:/Users/Kobes%20Work%20Account/Documents/Innovexia/app/src/main/java/com/example/innovexia/ui/sheets/personas/CreatePersonaDialog.kt#L56-L63)

```kotlin
LaunchedEffect(editPersonaId, editUid) {
    if (editPersonaId != null && editUid != null) {
        viewModel.loadForEdit(editUid, editPersonaId)  // Load existing
    } else {
        viewModel.reset()  // Start fresh
    }
}
```

### 4. **Enhanced Logging** ✅
[CreatePersonaViewModel.kt:43-75](file:///c:/Users/Kobes%20Work%20Account/Documents/Innovexia/app/src/main/java/com/example/innovexia/ui/sheets/personas/CreatePersonaViewModel.kt#L43-L75)

```kotlin
android.util.Log.d("CreatePersonaVM", "Loading persona for edit: uid=$uid, id=$personaId")
android.util.Log.d("CreatePersonaVM", "Document exists: ${doc.exists()}")
android.util.Log.d("CreatePersonaVM", "Loaded persona: name=${loadedDraft.name}, initial=${loadedDraft.initial}")
```

Check LogCat for these messages to debug loading issues.

## Testing

### Test Edit Flow:
1. Open Personas sheet
2. Tap ⋮ on any persona card
3. Tap "Edit"
4. **Verify**: All fields are populated with existing data
5. Make changes
6. Tap "Save Draft"
7. **Verify**: Changes are persisted

### Test Create Flow:
1. Open Personas sheet
2. Tap "+ New"
3. **Verify**: All fields are empty/default
4. Fill in data
5. Tap "Save Draft"
6. **Verify**: New persona is created

### Check Logs:
```bash
adb logcat | grep CreatePersonaVM
```

Should show:
```
D/CreatePersonaVM: Loading persona for edit: uid=pR2ESVPZf1WgUDoeemG8pSXDcZA2, id=xyz
D/CreatePersonaVM: Document exists: true
D/CreatePersonaVM: Loaded persona: name=Innovexia, initial=I
```

## Known Compatibility

### Old Format (Created before this update):
```json
{
  "name": "Innovexia",
  "initial": "I",
  "color": 4284377971,
  "summary": "AI assistant",
  "tags": ["helper"],
  "system": "You are Innovexia..."
}
```

### New Format (Persona 2.0):
```json
{
  "name": "Innovexia",
  "initial": "I",
  "color": 4284377971,
  "bio": "AI assistant",
  "tags": ["helper"],
  "system": {
    "instructions": "You are Innovexia...",
    "version": 1
  },
  "behavior": { ... },
  "limits": { ... }
}
```

**Both formats now work!** The conversion function handles the migration automatically.

## Troubleshooting

### If edit still shows empty:

1. **Check Firestore path:**
   ```
   /users/{your-uid}/personas/{persona-id}
   ```

2. **Check LogCat:**
   ```bash
   adb logcat | grep "CreatePersonaVM"
   ```

3. **Verify persona exists in Firestore Console**

4. **Check user is signed in:**
   The edit requires `editUid` to be provided.

### If "Document does not exist" error:

The persona might be stored in Room (local DB) only. Check:
- PersonaRepository sync status
- Whether the persona has a `cloudId`

## Future Improvements

1. **Add loading indicator** during persona fetch
2. **Show error toast** if load fails
3. **Auto-sync local personas** to Firestore on first edit
4. **Add "Retry" button** if network fails
5. **Cache loaded data** to prevent re-fetching on rotate

## Related Files

- [CreatePersonaDialog.kt](file:///c:/Users/Kobes%20Work%20Account/Documents/Innovexia/app/src/main/java/com/example/innovexia/ui/sheets/personas/CreatePersonaDialog.kt)
- [CreatePersonaViewModel.kt](file:///c:/Users/Kobes%20Work%20Account/Documents/Innovexia/app/src/main/java/com/example/innovexia/ui/sheets/personas/CreatePersonaViewModel.kt)
- [PersonaCard.kt](file:///c:/Users/Kobes%20Work%20Account/Documents/Innovexia/app/src/main/java/com/example/innovexia/ui/persona/PersonaCard.kt) (Edit button)
- [PersonasSheetHost.kt](file:///c:/Users/Kobes%20Work%20Account/Documents/Innovexia/app/src/main/java/com/example/innovexia/ui/sheets/personas/PersonasSheetHost.kt) (Edit wiring)
