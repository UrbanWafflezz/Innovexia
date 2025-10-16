# Quick Fix: Enable + Button for Attachments

The `+` button currently shows "coming soon" toast. Here's the minimal fix to enable the attachment toolbar.

## Option 1: Simple Toggle (No Full Integration)

This gets the `+` button working to show/hide the attachment toolbar without full backend integration.

### Step 1: Add State Variable

In `ChatScreen.kt`, add this state variable near the top (around line 50):

```kotlin
var showAttachmentToolbar by remember { mutableStateOf(false) }
```

### Step 2: Update onAttach Handler

Replace lines 215-218 with:

```kotlin
onAttach = {
    showAttachmentToolbar = !showAttachmentToolbar
},
```

### Step 3: Add AttachmentToolbar Component

Add this BEFORE the `ChatComposerV3` component (around line 197):

```kotlin
// Attachment toolbar
AttachmentToolbar(
    visible = showAttachmentToolbar,
    onPickPhotos = {
        showAttachmentToolbar = false
        Toast.makeText(context, "Photo picker - wire to launcher", Toast.LENGTH_SHORT).show()
    },
    onPickFiles = {
        showAttachmentToolbar = false
        Toast.makeText(context, "File picker - wire to launcher", Toast.LENGTH_SHORT).show()
    },
    onCapture = {
        showAttachmentToolbar = false
        Toast.makeText(context, "Camera - wire to launcher", Toast.LENGTH_SHORT).show()
    },
    onScanPdf = {
        showAttachmentToolbar = false
        Toast.makeText(context, "Scan PDF - wire to launcher", Toast.LENGTH_SHORT).show()
    }
)
```

### Step 4: Add Import

Add to imports at top of file:

```kotlin
import com.example.innovexia.ui.components.AttachmentToolbar
```

## Result

After these changes:
- ✅ Tapping `+` will show/hide the attachment toolbar with slide animation
- ✅ Toolbar displays with Photos, Files, Camera, and Scan options
- ⏸️ Each option shows a placeholder toast (ready for launcher wiring)

## Option 2: Full Integration

For complete functionality with file pickers and processing, follow the full guide in [ATTACHMENT_INTEGRATION_GUIDE.md](ATTACHMENT_INTEGRATION_GUIDE.md).

This requires:
1. Activity result launchers
2. ViewModel attachment manager
3. Permission handling
4. FileProvider setup

## Visual Result

After fix, tapping `+` will show:

```
┌─────────────────────────────────────┐
│  📷      📄       📸        📋     │
│ Photos  Files  Camera    Scan      │
└─────────────────────────────────────┘
        ▲
    [Composer with + button]
```

## Testing

1. Tap `+` button → toolbar slides up ✅
2. Tap `+` again → toolbar slides down ✅
3. Tap any toolbar item → shows placeholder toast ✅
4. Toolbar animates smoothly at 60/90/120 Hz ✅

## Next Steps

Once the toolbar is visible and working, proceed with:
1. Wiring activity result launchers (see integration guide)
2. Connecting to ViewModel
3. Processing selected files
4. Displaying attachment chips
