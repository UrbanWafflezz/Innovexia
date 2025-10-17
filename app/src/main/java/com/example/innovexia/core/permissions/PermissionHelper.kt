package com.example.innovexia.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

object PermissionHelper {

    /**
     * Check if location permission is granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if photo/media permission is granted
     */
    fun hasPhotoPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses Photo Picker (no permission needed for images)
            // or READ_MEDIA_IMAGES for direct access
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if audio recording permission is granted
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get a single location update with coarse accuracy
     */
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        return try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationToken = CancellationTokenSource()

            @Suppress("MissingPermission")
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token
            ).await()
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Composable to request location permission
 */
@Composable
fun rememberLocationPermissionLauncher(
    onPermissionResult: (Boolean) -> Unit
): ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )
}

/**
 * Composable to request photo/media permission
 */
@Composable
fun rememberPhotoPermissionLauncher(
    onPermissionResult: (Boolean) -> Unit
): ManagedActivityResultLauncher<String, Boolean> {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )
}

/**
 * Composable to request audio recording permission
 */
@Composable
fun rememberAudioPermissionLauncher(
    onPermissionResult: (Boolean) -> Unit
): ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )
}

/**
 * Composable to request camera permission
 */
@Composable
fun rememberCameraPermissionLauncher(
    onPermissionResult: (Boolean) -> Unit
): ManagedActivityResultLauncher<String, Boolean> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )
}

/**
 * State holder for permission dialogs
 */
@Stable
class PermissionState {
    var showLocationRationale by mutableStateOf(false)
    var showPhotoRationale by mutableStateOf(false)
    var showAudioRationale by mutableStateOf(false)
    var showCameraRationale by mutableStateOf(false)
}

@Composable
fun rememberPermissionState(): PermissionState {
    return remember { PermissionState() }
}
