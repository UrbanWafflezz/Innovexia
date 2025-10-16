package com.example.innovexia.data.ai

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Result of image processing
 */
data class ProcessedImage(
    val uri: Uri,
    val displayName: String,
    val size: Long,
    val width: Int,
    val height: Int
)

/**
 * Utility for processing images for Gemini
 * - Converts HEIC/HEIF to JPEG
 * - Downscales to max dimension 2048px
 * - Strips EXIF data (privacy)
 * - Compresses to JPEG 85% quality
 */
class ImageProcessor(
    private val cacheDir: File
) {

    companion object {
        private const val MAX_DIMENSION = 2048
        private const val JPEG_QUALITY = 85
        private const val MAX_SIZE_BYTES = 20 * 1024 * 1024L // 20 MB
    }

    /**
     * Prepare an image for Gemini by:
     * 1. Decoding and getting dimensions
     * 2. Reading EXIF for rotation
     * 3. Downscaling if needed
     * 4. Converting to JPEG (strips EXIF)
     * 5. Caching the result
     */
    fun prepareForGemini(
        resolver: ContentResolver,
        uri: Uri
    ): ProcessedImage {
        val inputStream = resolver.openInputStream(uri)
            ?: throw IOException("Cannot open input stream for $uri")

        // Decode bitmap
        val options = BitmapFactory.Options().apply {
            // First pass: get dimensions only
            inJustDecodeBounds = true
        }

        inputStream.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // Calculate sample size for downscaling
        val sampleSize = calculateSampleSize(originalWidth, originalHeight, MAX_DIMENSION)

        // Second pass: decode with sample size
        val decodedBitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            })
        } ?: throw IOException("Failed to decode bitmap")

        // Get EXIF orientation for rotation correction
        val rotation = getExifRotation(resolver, uri)

        // Apply rotation if needed
        val rotatedBitmap = if (rotation != 0f) {
            rotateBitmap(decodedBitmap, rotation)
        } else {
            decodedBitmap
        }

        // Further downscale if still too large
        val finalBitmap = if (rotatedBitmap.width > MAX_DIMENSION || rotatedBitmap.height > MAX_DIMENSION) {
            scaleBitmap(rotatedBitmap, MAX_DIMENSION)
        } else {
            rotatedBitmap
        }

        // Get original filename
        val displayName = getDisplayName(resolver, uri) ?: "image_${System.currentTimeMillis()}.jpg"
        val safeName = displayName.substringBeforeLast('.') + ".jpg"

        // Save as JPEG (strips EXIF automatically)
        val outputFile = File(cacheDir, "attachment_${System.currentTimeMillis()}_$safeName")
        FileOutputStream(outputFile).use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }

        // Clean up bitmaps
        if (rotatedBitmap != decodedBitmap) rotatedBitmap.recycle()
        if (finalBitmap != rotatedBitmap) finalBitmap.recycle()
        decodedBitmap.recycle()

        return ProcessedImage(
            uri = Uri.fromFile(outputFile),
            displayName = safeName,
            size = outputFile.length(),
            width = finalBitmap.width,
            height = finalBitmap.height
        )
    }

    /**
     * Calculate sample size for initial decoding
     */
    private fun calculateSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sampleSize = 1
        val maxDimension = maxOf(width, height)

        if (maxDimension > maxDim) {
            sampleSize = maxDimension / maxDim
        }

        return sampleSize
    }

    /**
     * Get EXIF rotation in degrees
     */
    private fun getExifRotation(resolver: ContentResolver, uri: Uri): Float {
        return try {
            resolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    /**
     * Rotate bitmap by given degrees
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Scale bitmap to fit within maxDim while maintaining aspect ratio
     */
    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val aspectRatio = width.toFloat() / height.toFloat()

        val (newWidth, newHeight) = if (width > height) {
            maxDim to (maxDim / aspectRatio).toInt()
        } else {
            (maxDim * aspectRatio).toInt() to maxDim
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Get display name from URI
     */
    private fun getDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
