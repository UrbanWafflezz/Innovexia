# Attachment Integration Guide

This guide provides step-by-step instructions for integrating the file & photo upload feature into ChatScreen.

## ✅ Completed Components

### 1. Dependencies Added
- Coil 2.6.0 (image loading)
- ExifInterface (EXIF stripping)
- JitPack repository (for future extensions)
- Note: PDF viewer library is optional (commented out) - only needed for PDF preview UI, not required for sending PDFs to Gemini

### 2. Data Models
- `AttachmentKind` enum (PHOTO, FILE, PDF)
- `AttachmentStatus` enum (PENDING, PREPPING, UPLOADING, READY, FAILED)
- `AttachmentMeta` data class (extended existing model)

### 3. UI Components
- `AttachmentToolbar` - Slide-in toolbar with Photos/Files/Camera/Scan options
- `AttachmentStrip` & `AttachmentChip` - Preview chips with thumbnails and remove buttons

### 4. Processing Utilities
- `ImageProcessor` - Downscales, converts HEIC→JPEG, strips EXIF
- `FileProcessor` - Copies PDFs to cache with size validation

### 5. Backend Integration
- `GeminiService` - Updated to accept attachments and send to Gemini 2.5 Flash
- `AttachmentUploader` - Uploads to Firebase Storage (respects incognito mode)
- `AttachmentManager` - Manages attachment state and processing

## 🔧 Integration Steps

### Step 1: Add Activity Result Launchers to ChatScreen

Add these inside `ChatScreen` composable:

```kotlin
// Photo picker (Android 13+)
val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 6)
) { uris ->
    if (uris.isNotEmpty()) {
        viewModel.processAttachmentUris(uris, context.contentResolver)
    }
}

// File picker (PDFs and images)
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
) { uris ->
    if (uris.isNotEmpty()) {
        viewModel.processAttachmentUris(uris, context.contentResolver)
    }
}

// Camera
val cameraUri = remember { mutableStateOf<Uri?>(null) }
val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
) { success ->
    if (success && cameraUri.value != null) {
        viewModel.processAttachmentUris(listOf(cameraUri.value!!), context.contentResolver)
    }
}

// Camera permission
val cameraPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) {
        // Create temp file for camera
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraUri.value = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        cameraLauncher.launch(cameraUri.value!!)
    } else {
        Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }
}
```

### Step 2: Add State for Attachment Toolbar

```kotlin
var showAttachmentToolbar by remember { mutableStateOf(false) }
val attachments by viewModel.attachments.collectAsState()
```

### Step 3: Add AttachmentToolbar Before Composer

Replace the TODO comment at line 216 with:

```kotlin
// Attachment toolbar (slides in above composer)
AttachmentToolbar(
    visible = showAttachmentToolbar,
    onPickPhotos = {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
        showAttachmentToolbar = false
    },
    onPickFiles = {
        filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
        showAttachmentToolbar = false
    },
    onCapture = {
        // Check camera permission
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                cameraUri.value = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                cameraLauncher.launch(cameraUri.value!!)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        showAttachmentToolbar = false
    },
    onScanPdf = {
        // For now, reuse camera (PDF stitching later)
        Toast.makeText(context, "PDF scan coming soon - using camera", Toast.LENGTH_SHORT).show()
        showAttachmentToolbar = false
    }
)

// Attachment preview chips
if (attachments.isNotEmpty()) {
    AttachmentStrip(
        items = attachments,
        onRemove = { attachmentId ->
            viewModel.removeAttachment(attachmentId)
        }
    )
}
```

### Step 4: Update ChatComposerV3 Calls

Update the onAttach handler:

```kotlin
onAttach = {
    showAttachmentToolbar = !showAttachmentToolbar
},
```

Update hasAttachment:

```kotlin
hasAttachment = attachments.isNotEmpty(),
```

Update onSend to include attachments:

```kotlin
onSend = {
    val textToSend = composerText.trim()
    val hasContent = textToSend.isNotBlank() || attachments.isNotEmpty()

    if (hasContent && !isSending) {
        isSending = true
        justSent = true
        composerText = ""
        viewModel.sendMessageWithAttachments(textToSend)
        viewModel.clearReplyTarget()
    }
},
```

### Step 5: Extend ChatViewModel

Add to ChatViewModel:

```kotlin
// Attachment manager
private lateinit var attachmentManager: AttachmentManager

// In init or factory
fun initAttachments(context: Context) {
    attachmentManager = AttachmentManager(
        context = context,
        chatId = chatId,
        isIncognito = { isIncognito.value },
        hasCloudBackup = { /* get from settings */ true }
    )
}

// Expose attachments
val attachments: StateFlow<List<AttachmentMeta>> = attachmentManager.attachments

// Process URIs
fun processAttachmentUris(uris: List<Uri>, resolver: ContentResolver) {
    viewModelScope.launch {
        attachmentManager.processUris(uris, resolver)
    }
}

// Remove attachment
fun removeAttachment(attachmentId: String) {
    attachmentManager.removeAttachment(attachmentId)
}

// Send with attachments
fun sendMessageWithAttachments(text: String) {
    if (isSending) return
    isSending = true

    viewModelScope.launch {
        try {
            val readyAttachments = attachmentManager.getReadyAttachments()

            // Add user message with attachments
            chatRepository.appendUserMessage(chatId, text, attachments = readyAttachments)

            // Create model message placeholder
            val modelMsgId = UUID.randomUUID().toString()
            _streamingMessageId.value = modelMsgId

            // Stream response with attachments
            var streamedText = ""
            streamingJob = launch {
                try {
                    geminiService.generateReply(
                        chatId = chatId,
                        userText = text,
                        persona = null,
                        enableThinking = false,
                        attachments = readyAttachments
                    ).collect { token ->
                        streamedText += token
                        chatRepository.appendModelToken(
                            chatId = chatId,
                            messageId = modelMsgId,
                            token = token,
                            isFinal = false
                        )
                    }

                    // Mark complete and clear attachments
                    chatRepository.appendModelToken(
                        chatId = chatId,
                        messageId = modelMsgId,
                        token = "",
                        isFinal = true
                    )

                    _streamingMessageId.value = null
                    attachmentManager.clearAttachments()
                    isSending = false

                } catch (e: Exception) {
                    _streamingMessageId.value = null
                    _errorMessage.value = "Error: ${e.message}"
                    isSending = false
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to send: ${e.message}"
            isSending = false
        }
    }
}
```

### Step 6: Add FileProvider Configuration

Add to `AndroidManifest.xml` inside `<application>`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

Create `res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="cache" path="." />
</paths>
```

### Step 7: Add Required Imports to ChatScreen

```kotlin
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.innovexia.ui.components.AttachmentStrip
import com.example.innovexia.ui.components.AttachmentToolbar
import java.io.File
```

## 📋 Checklist

- [ ] Add activity result launchers to ChatScreen
- [ ] Add attachment toolbar state
- [ ] Insert AttachmentToolbar UI before composer
- [ ] Update ChatComposerV3 callbacks (onAttach, hasAttachment, onSend)
- [ ] Extend ChatViewModel with attachment methods
- [ ] Add FileProvider to AndroidManifest.xml
- [ ] Create file_paths.xml
- [ ] Test photo picker
- [ ] Test file picker (PDFs)
- [ ] Test camera capture
- [ ] Test incognito mode (no Firebase upload)
- [ ] Test cloud backup mode (Firebase upload)
- [ ] Test attachment removal
- [ ] Test sending messages with attachments

## 🎨 UX Flows

### Photo Attachment
1. User taps `+` button → Attachment toolbar slides in
2. User taps "Photos" → System photo picker opens
3. User selects images → Images process (downscale, EXIF strip)
4. Thumbnails appear as chips above composer
5. User taps send → Images sent to Gemini inline

### PDF Attachment
1. User taps `+` → Attachment toolbar slides in
2. User taps "Files" → File picker opens with PDF/image filter
3. User selects PDF → File copies to cache, validates size
4. PDF chip appears with icon and size
5. User taps send → PDF sent to Gemini

### Camera Capture
1. User taps `+` → Attachment toolbar slides in
2. User taps "Camera" → Permission check
3. If granted → Camera opens
4. User captures photo → Processes like photo attachment
5. Thumbnail appears, user can send

## 🔐 Privacy & Incognito

- **Incognito mode ON**: Attachments stay local only, no Firebase upload
- **Incognito mode OFF + Cloud backup ON**: Attachments upload to Firebase Storage in background
- **EXIF stripping**: All images have location/metadata stripped automatically

## ⚙️ Configuration

### Size Limits (tunable in processors)
- Images: Max 2048px longest side, JPEG 85% quality
- PDFs: Max 30MB (enforced in FileProcessor)
- Per message: Max 6 attachments, ~40MB total

### Supported Formats
- Images: JPEG, PNG, HEIC (auto-converts to JPEG)
- Documents: PDF

## 🐛 Error Handling

- Unsupported file type → Toast notification
- File too large → Failed status with error chip
- Upload failure → Retry available
- Processing error → Failed status, can remove and retry

## 📦 Dependencies Summary

```kotlin
// Already added to build.gradle.kts
implementation("io.coil-kt:coil-compose:2.6.0")
implementation("com.github.barteksc:android-pdf-viewer:3.2.0-beta.1")
implementation("androidx.exifinterface:exifinterface:1.3.7")
```

## 🚀 Next Steps (Optional Enhancements)

1. **File Manager API**: For very large PDFs, use Gemini Files API instead of inline
2. **PDF Scan**: Implement ML Kit document scanning to stitch multi-page PDFs
3. **Gallery Ribbon**: Quick access to last 12 photos in toolbar
4. **Preview Modal**: Full-screen attachment preview before sending
5. **Gemini File ID caching**: Reuse uploaded files on retry to avoid re-upload
6. **Paste from clipboard**: Detect clipboard images/text and add as attachment
