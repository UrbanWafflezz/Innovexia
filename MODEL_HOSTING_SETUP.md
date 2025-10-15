# Model Hosting Setup

The app now supports **automatic model downloads**! When users select a local model, it will download automatically.

## Current Status

✅ Download UI implemented
✅ Progress tracking working
✅ Model loading from downloaded files
⏳ **Need to add download URLs**

## Where to Update URLs

File: `app/src/main/java/com/example/innovexia/local/ModelDownloadManager.kt`

Look for this section (lines ~62-90):

```kotlin
val availableModels = mapOf(
    "tinyllama-1.1b-chat-q8" to DownloadableModel(
        id = "tinyllama-1.1b-chat-q8",
        displayName = "TinyLlama 1.1B Chat",
        files = listOf(
            ModelFile(
                filename = "tinyllama_1.1b_chat_q8.tflite",
                url = "https://example.com/models/tinyllama_1.1b_chat_q8.tflite", // ← REPLACE THIS
                sizeMB = 1100f
            ),
            ModelFile(
                filename = "tinyllama_tokenizer.model",
                url = "https://example.com/models/tinyllama_tokenizer.model", // ← REPLACE THIS
                sizeMB = 2f
            )
        ),
        totalSizeMB = 1102f
    )
)
```

## Hosting Options

### Option 1: Firebase Storage (Recommended)
**Best for**: Easy setup, free tier available, built-in CDN

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to Storage
4. Upload your model files
5. Make files publicly accessible
6. Copy the download URLs

**Pros:**
- Already using Firebase for auth/firestore
- Free up to 1GB/day bandwidth
- Automatic CDN
- Simple URL management

**Cons:**
- Bandwidth limits on free tier
- May need to upgrade for many users

### Option 2: GitHub Releases
**Best for**: Open source projects, version control

1. Create a new release in your GitHub repo
2. Attach model files as release assets
3. Copy the download URLs from the release page

**Example URL:**
```
https://github.com/username/innovexia/releases/download/models-v1.0/tinyllama_1.1b_chat_q8.tflite
```

**Pros:**
- Free
- Version control
- Easy to update

**Cons:**
- 100MB file size limit (won't work for TinyLlama 1.1GB)
- Slower download speeds

### Option 3: Hugging Face Hub
**Best for**: ML models, community sharing

1. Create account at [huggingface.co](https://huggingface.co/)
2. Create a new model repository
3. Upload TFLite files using Git LFS
4. Get direct download links

**Example URL:**
```
https://huggingface.co/username/tinyllama-tflite/resolve/main/tinyllama_1.1b_chat_q8.tflite
```

**Pros:**
- Built for AI models
- Unlimited storage for public models
- Fast CDN
- Community visibility

**Cons:**
- Requires LFS for large files
- Learning curve for Git LFS

### Option 4: Google Cloud Storage
**Best for**: High bandwidth, reliability

1. Create Google Cloud Storage bucket
2. Upload model files
3. Make bucket/files public
4. Get download URLs

**Pros:**
- Very fast
- Reliable
- Scalable

**Cons:**
- Costs money (after free tier)
- More complex setup

### Option 5: Self-Hosted CDN
**Best for**: Full control, existing infrastructure

Use Cloudflare, AWS S3 + CloudFront, or any CDN service.

## Recommended Setup

For Innovexia, I recommend:

1. **Firebase Storage** for initial testing
2. **Hugging Face** for public release (free, built for models)

## Step-by-Step: Firebase Storage

### 1. Upload Models

```bash
# Using Firebase CLI
firebase storage:upload tinyllama_1.1b_chat_q8.tflite /models/
firebase storage:upload tinyllama_tokenizer.model /models/
```

Or use the web console.

### 2. Get Download URLs

In Firebase Console → Storage:
1. Click on the file
2. Click "Copy download URL"
3. Or get programmatic URL:

```
https://firebasestorage.googleapis.com/v0/b/YOUR_PROJECT.appspot.com/o/models%2Ftinyllama_1.1b_chat_q8.tflite?alt=media&token=YOUR_TOKEN
```

### 3. Update ModelDownloadManager.kt

Replace the URLs in the code with your actual URLs.

### 4. Test Download

Build and run the app, select a local model, and verify download works!

## Step-by-Step: Hugging Face

### 1. Create Repository

```bash
# Install Git LFS
git lfs install

# Create new repo
git clone https://huggingface.co/YOUR_USERNAME/tinyllama-tflite
cd tinyllama-tflite

# Track large files
git lfs track "*.tflite"
git lfs track "*.model"
```

### 2. Upload Files

```bash
# Add files
cp /path/to/tinyllama_1.1b_chat_q8.tflite .
cp /path/to/tinyllama_tokenizer.model .

# Commit and push
git add .
git commit -m "Add TinyLlama TFLite model"
git push
```

### 3. Get Direct Links

Format: `https://huggingface.co/{username}/{repo}/resolve/main/{filename}`

Example:
```
https://huggingface.co/innovexia/tinyllama-tflite/resolve/main/tinyllama_1.1b_chat_q8.tflite
```

### 4. Update Code

Replace URLs in `ModelDownloadManager.kt`.

## Model File Checklist

Before uploading, verify:

- ✅ Files are INT8 quantized
- ✅ TFLite files end with `.tflite`
- ✅ Tokenizer files end with `.model`
- ✅ File sizes match expected:
  - TinyLlama model: ~1.1GB
  - TinyLlama tokenizer: ~2MB
  - FLAN-T5 model: ~80MB
  - FLAN-T5 tokenizer: ~2MB

## Security Considerations

### Public vs Private

**Public models** (recommended):
- Anyone can download
- Good for open-source project
- No auth required
- Faster, simpler

**Private models**:
- Require Firebase Auth or signed URLs
- More complex
- Slower download (auth overhead)
- Better for proprietary models

### Bandwidth Costs

Estimate:
- Each model download: 1.1GB (TinyLlama)
- 1000 users = 1.1TB bandwidth
- Firebase free tier: 1GB/day = ~30GB/month
- **You'll need paid tier for > ~30 users/month**

Solutions:
- Use Hugging Face (unlimited)
- Add Firebase Blaze plan
- Use P2P distribution (advanced)

## Testing

After uploading:

1. **Test URL directly:**
```bash
curl -I https://your-storage-url/tinyllama_1.1b_chat_q8.tflite
```

Should return `200 OK`.

2. **Test in app:**
- Select "Innovexia Local (TinyLlama)" in model dropdown
- Download should start automatically
- Monitor LogCat for progress

3. **Verify download:**
```bash
adb shell ls -lh /data/data/com.example.innovexia/files/local_models/
```

## Troubleshooting

### Download fails with 403
- Files not public
- Check Firebase Storage rules
- Or add authentication to download

### Download fails with 404
- Wrong URL
- File not uploaded
- Check file path

### Download stalls
- Network issue
- Increase connection timeout in `ModelDownloadManager.kt`

### App crashes after download
- Model corrupted during download
- Verify file size matches
- Check device has enough storage

## Next Steps

1. ✅ Convert/obtain model files (see conversion guide)
2. ⏳ Choose hosting option (Firebase recommended)
3. ⏳ Upload files
4. ⏳ Update URLs in `ModelDownloadManager.kt`
5. ⏳ Test download in app
6. ✅ Ship to users!

## Alternative: Skip Hosting

If you don't want to host files, users can:

1. Manually place files in `app/src/main/assets/local_models/`
2. Rebuild the app themselves
3. Or use manual file transfer to device storage

The app will work either way!
