# How to Add Local On-Device AI Models

This guide explains how to add TensorFlow Lite models to Innovexia for completely offline, on-device AI inference.

## Overview

Innovexia supports local AI models that run entirely on your device using:
- **NPU acceleration** on Pixel 7+ (Tensor G2/G3 chip)
- **GPU acceleration** on other Android devices
- **CPU fallback** for maximum compatibility

## Quick Start

### 1. Get Model Files

You need to obtain or convert TensorFlow Lite (`.tflite`) model files. Two models are supported:

**Option A: FLAN-T5 Small (Recommended for testing)**
- Size: ~80MB
- Good for: Question answering, summarization
- Faster inference, lower quality

**Option B: TinyLlama 1.1B Chat**
- Size: ~1.1GB
- Good for: Conversational AI, creative writing
- Slower inference, higher quality

### 2. Place Files in Assets Folder

Copy your model files to:
```
app/src/main/assets/local_models/
```

**Required files for FLAN-T5:**
- `flan_t5_small_int8.tflite`
- `flan_t5_small_tokenizer.model`

**Required files for TinyLlama:**
- `tinyllama_1.1b_chat_q8.tflite`
- `tinyllama_tokenizer.model`

### 3. Rebuild the App

```bash
./gradlew clean build
```

The models will be automatically detected and appear in the model dropdown with "On-Device" badge.

## Model Conversion Guide

If you need to convert models yourself, see [`app/src/main/assets/local_models/README.md`](app/src/main/assets/local_models/README.md) for detailed conversion instructions.

## Where to Get Pre-Converted Models

### Option 1: Convert Yourself (Recommended)
Use the conversion scripts in the assets README. You'll need:
- Python 3.8+
- TensorFlow 2.x
- Transformers library

### Option 2: Community Resources
Check these sources for pre-converted TFLite models:
- [TensorFlow Hub](https://tfhub.dev/)
- [Hugging Face](https://huggingface.co/models?library=tf-lite)
- [MediaPipe Models](https://developers.google.com/mediapipe/solutions/guide)

**Note:** Make sure models are INT8 quantized for optimal mobile performance.

## Model Format Requirements

✅ **Supported:**
- TensorFlow Lite (`.tflite`) format
- INT8 quantized models
- SentencePiece tokenizers (`.model`)

❌ **Not Supported:**
- PyTorch models (`.pt`, `.pth`)
- ONNX models (`.onnx`)
- Full TensorFlow SavedModel format
- Models without quantization (too large/slow)

## Performance Tips

1. **Use INT8 quantization** - 4x smaller, faster inference
2. **Enable NNAPI on Pixel devices** - Hardware acceleration via NPU
3. **Test model size vs quality** - Smaller models = faster responses
4. **Monitor memory usage** - 1GB+ models may cause issues on low-end devices

## Troubleshooting

### Model doesn't appear in dropdown
- Check file names match exactly (case-sensitive)
- Verify files are in correct folder: `app/src/main/assets/local_models/`
- Rebuild the app completely

### App crashes when loading model
- Model may be too large for device memory
- Try a smaller/quantized version
- Check logcat for error messages: `adb logcat | grep TFLite`

### Slow inference
- Model may not be quantized (should be INT8)
- NNAPI/GPU delegates may not be working
- Try enabling different acceleration in Settings

## File Structure

```
app/
  src/
    main/
      assets/
        local_models/
          ├── README.md                          # Conversion guide
          ├── flan_t5_small_int8.tflite         # FLAN-T5 model (add this)
          ├── flan_t5_small_tokenizer.model     # FLAN-T5 tokenizer (add this)
          ├── tinyllama_1.1b_chat_q8.tflite     # TinyLlama model (optional)
          └── tinyllama_tokenizer.model          # TinyLlama tokenizer (optional)
```

## Privacy & Offline Use

✅ **Benefits of Local Models:**
- 100% offline - no internet required
- Zero data leaves your device
- No API costs
- Works in airplane mode
- Instant responses (no network latency)

❌ **Limitations:**
- Lower quality than cloud models (Gemini, GPT-4)
- Limited context window (512-1024 tokens)
- Requires storage space (80MB - 1.1GB per model)
- May drain battery faster

## Next Steps

1. Convert or download your first model
2. Copy files to assets folder
3. Rebuild and test the app
4. Toggle between cloud and local models in Settings
5. Compare quality and speed

For detailed conversion instructions, see the [models README](app/src/main/assets/local_models/README.md).
