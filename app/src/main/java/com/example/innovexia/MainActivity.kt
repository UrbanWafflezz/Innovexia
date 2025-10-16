package com.example.innovexia

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.innovexia.core.session.SessionRecorder
import com.example.innovexia.core.update.ApkDownloader
import com.example.innovexia.core.update.UpdateAvailableDialog
import com.example.innovexia.core.update.UpdateChecker
import com.example.innovexia.core.update.UpdateInfo
import com.example.innovexia.ui.screens.HomeScreen
import com.example.innovexia.ui.theme.InnovexiaTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("session_throttle", MODE_PRIVATE)
    }

    private val updateChecker: UpdateChecker by lazy {
        UpdateChecker(this)
    }

    private val apkDownloader: ApkDownloader by lazy {
        ApkDownloader(this)
    }

    private var updateInfo by mutableStateOf<UpdateInfo?>(null)
    private var downloadState by mutableStateOf<ApkDownloader.DownloadState>(ApkDownloader.DownloadState.Idle)

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        // Notify ProfileScopedRepository of auth changes
        val app = application as InnovexiaApplication
        app.chatRepository.getProfileRepository()?.onAuthChanged()
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val granted = entry.value
            Log.d("MainActivity", "Permission $permission granted: $granted")

            if (!granted) {
                Log.w("MainActivity", "Permission $permission was denied")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request all necessary permissions on launch
        requestAllPermissions()

        // Add auth state listener
        Firebase.auth.addAuthStateListener(authStateListener)

        setContent {
            HomeScreen()

            // Show update dialog if update is available
            updateInfo?.let { info ->
                val forceUpdate = updateChecker.shouldForceUpdate()

                UpdateAvailableDialog(
                    updateInfo = info,
                    downloadState = downloadState,
                    forceUpdate = forceUpdate,
                    onUpdateNow = {
                        // Start in-app download
                        downloadAndInstallUpdate(info.downloadUrl)
                    },
                    onRemindLater = { hours ->
                        // Set remind later preference with custom duration
                        if (hours == 0) {
                            // Next app launch - clear remind later
                            updateChecker.clearRemindLater()
                        } else {
                            // Set custom remind duration
                            val durationMs = hours * 60 * 60 * 1000L
                            updateChecker.setRemindLater(durationMs)
                        }
                        updateInfo = null
                        downloadState = ApkDownloader.DownloadState.Idle
                    },
                    onDismiss = {
                        // User closed dialog - only allow if not forced
                        if (!forceUpdate) {
                            updateChecker.clearRemindLater() // Show on next launch
                            updateInfo = null
                            downloadState = ApkDownloader.DownloadState.Idle
                        }
                    }
                )
            }
        }

        // Check for updates on startup
        checkForUpdates()
    }

    /**
     * Request all permissions required by the app
     */
    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Media permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            // Notification permission for Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                // Only request WRITE for Android 10 and below
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        // Camera permission (optional, for future use)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        // Request permissions if any are missing
        if (permissionsToRequest.isNotEmpty()) {
            Log.d("MainActivity", "Requesting ${permissionsToRequest.size} permissions: $permissionsToRequest")
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            Log.d("MainActivity", "All permissions already granted")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove auth state listener
        Firebase.auth.removeAuthStateListener(authStateListener)
    }

    override fun onResume() {
        super.onResume()
        // Update session on foreground (throttled to 6 hours)
        updateSessionThrottled()
    }

    private fun updateSessionThrottled() {
        val now = System.currentTimeMillis()
        val lastUpdate = prefs.getLong("last_session_update", 0L)
        val sixHours = 6 * 60 * 60 * 1000L // 6 hours in milliseconds

        if (Firebase.auth.currentUser != null && (now - lastUpdate) > sixHours) {
            lifecycleScope.launch {
                SessionRecorder.updateLastActive(this@MainActivity)
                prefs.edit().putLong("last_session_update", now).apply()
            }
        }
    }

    /**
     * Check for app updates from GitHub Releases
     */
    private fun checkForUpdates() {
        lifecycleScope.launch {
            try {
                // Check if forced update is required (7 days passed)
                val forceUpdate = updateChecker.shouldForceUpdate()

                if (!forceUpdate && !updateChecker.shouldCheckForUpdates()) {
                    Log.d("MainActivity", "Skipping update check (rate limited or remind later)")
                    return@launch
                }

                Log.d("MainActivity", "Checking for updates... (forced: $forceUpdate)")
                val update = updateChecker.checkForUpdates()

                if (update != null) {
                    Log.d("MainActivity", "Update available: ${update.latestVersion}")
                    updateInfo = update

                    // If forced update, automatically start download
                    if (forceUpdate) {
                        Log.d("MainActivity", "Force update triggered - auto-downloading")
                        downloadAndInstallUpdate(update.downloadUrl)
                    }
                } else {
                    Log.d("MainActivity", "No update available")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking for updates", e)
            }
        }
    }

    /**
     * Download APK and install it
     */
    private fun downloadAndInstallUpdate(downloadUrl: String) {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Starting APK download from: $downloadUrl")

                apkDownloader.downloadApk(downloadUrl).collect { state ->
                    downloadState = state

                    when (state) {
                        is ApkDownloader.DownloadState.Completed -> {
                            Log.d("MainActivity", "Download completed, starting installation")
                            installApk(state.file)
                            // Reset state after installation prompt
                            updateInfo = null
                            downloadState = ApkDownloader.DownloadState.Idle
                        }
                        is ApkDownloader.DownloadState.Error -> {
                            Log.e("MainActivity", "Download error: ${state.message}")
                            // Keep dialog open to show error
                        }
                        else -> {
                            // Update UI with progress
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error downloading update", e)
                downloadState = ApkDownloader.DownloadState.Error("Download failed: ${e.message}", e)
            }
        }
    }

    /**
     * Install downloaded APK
     */
    private fun installApk(apkFile: java.io.File) {
        try {
            // Check if we need to request install permission (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    // Request permission to install packages
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Log.w("MainActivity", "Need permission to install APKs")
                    return
                }
            }

            // Create content URI using FileProvider
            val apkUri = FileProvider.getUriForFile(
                this,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                apkFile
            )

            // Create install intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            Log.d("MainActivity", "Launching APK installer")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error installing APK", e)
        }
    }
}
