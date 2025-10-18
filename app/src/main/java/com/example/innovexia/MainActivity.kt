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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
    private var updateCheckError by mutableStateOf<String?>(null)

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
            } else {
                // If location permission was granted, schedule background location worker
                if (permission == Manifest.permission.ACCESS_COARSE_LOCATION ||
                    permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                    Log.d("MainActivity", "Location permission granted - scheduling background location updates")
                    (application as InnovexiaApplication).scheduleLocationUpdatesPublic()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep splash screen visible while loading (optional)
        // You can add logic here to keep it visible during initial data loading
        // splashScreen.setKeepOnScreenCondition { false }

        // Request all necessary permissions on launch
        requestAllPermissions()

        // Add auth state listener
        Firebase.auth.addAuthStateListener(authStateListener)

        setContent {
            InnovexiaTheme {
                androidx.compose.material3.Scaffold { paddingValues ->
                    // Main content
                    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                        HomeScreen()

                        // Show update error snackbar if there's an error
                        updateCheckError?.let { errorMessage ->
                            androidx.compose.material3.Snackbar(
                                modifier = androidx.compose.ui.Modifier
                                    .align(androidx.compose.ui.Alignment.BottomCenter)
                                    .padding(16.dp),
                                action = {
                                    androidx.compose.material3.TextButton(onClick = { updateCheckError = null }) {
                                        androidx.compose.material3.Text("Dismiss")
                                    }
                                }
                            ) {
                                androidx.compose.material3.Text(errorMessage)
                            }
                        }
                    }
                }

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

        // Location permission (for AI context and grounding)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
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

        // Restore pending update dialog if user went to install permissions
        restorePendingUpdateDialog()
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
                // First check if we should show update notification (respects "Remind Later")
                if (!updateChecker.shouldShowUpdateNotification()) {
                    Log.d("MainActivity", "Skipping update notification (remind later active)")
                    return@launch
                }

                // Check if forced update is required (7 days passed)
                val forceUpdate = updateChecker.shouldForceUpdate()

                Log.d("MainActivity", "Checking for updates... (forced: $forceUpdate)")
                val result = updateChecker.checkForUpdates()

                when (result) {
                    is UpdateChecker.UpdateCheckResult.UpdateAvailable -> {
                        val update = result.updateInfo
                        Log.d("MainActivity", "Update available: ${update.latestVersion}")
                        updateInfo = update
                        updateCheckError = null

                        // If forced update, automatically start download
                        if (forceUpdate) {
                            Log.d("MainActivity", "Force update triggered - auto-downloading")
                            downloadAndInstallUpdate(update.downloadUrl)
                        }
                    }
                    is UpdateChecker.UpdateCheckResult.NoUpdateAvailable -> {
                        Log.d("MainActivity", "No update available")
                        updateCheckError = null
                    }
                    is UpdateChecker.UpdateCheckResult.Error -> {
                        Log.e("MainActivity", "Update check failed: ${result.message}", result.exception)
                        updateCheckError = result.message
                        // Show error for 10 seconds then clear
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(10000)
                            updateCheckError = null
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking for updates", e)
                updateCheckError = "Unexpected error: ${e.message}"
            }
        }
    }

    /**
     * Restore pending update dialog when app resumes
     * (e.g., user went to install permissions and came back)
     */
    private fun restorePendingUpdateDialog() {
        // Only restore if no dialog is currently showing and we should show notifications
        if (updateInfo == null && updateChecker.shouldShowUpdateNotification()) {
            val pendingUpdate = updateChecker.getPendingUpdate()
            if (pendingUpdate != null) {
                Log.d("MainActivity", "Restoring pending update dialog: ${pendingUpdate.latestVersion}")
                updateInfo = pendingUpdate

                // Also check if APK was already downloaded
                val downloadedApk = apkDownloader.getDownloadedApk()
                if (downloadedApk != null && downloadedApk.exists()) {
                    Log.d("MainActivity", "APK already downloaded, restoring completed state")
                    downloadState = ApkDownloader.DownloadState.Completed(downloadedApk)
                }
            }
        }
    }

    /**
     * Manual update check (can be called from settings or user action)
     */
    fun manualUpdateCheck() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Manual update check requested")
                val result = updateChecker.checkForUpdates()

                when (result) {
                    is UpdateChecker.UpdateCheckResult.UpdateAvailable -> {
                        updateInfo = result.updateInfo
                        updateCheckError = null
                    }
                    is UpdateChecker.UpdateCheckResult.NoUpdateAvailable -> {
                        updateCheckError = "You're on the latest version!"
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(3000)
                            updateCheckError = null
                        }
                    }
                    is UpdateChecker.UpdateCheckResult.Error -> {
                        updateCheckError = result.message
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(10000)
                            updateCheckError = null
                        }
                    }
                }
            } catch (e: Exception) {
                updateCheckError = "Unexpected error: ${e.message}"
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
                            // DON'T reset state here - let it persist if user needs to enable install permissions
                            // It will be cleared when user actually installs the update
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
                    Log.w("MainActivity", "Need permission to install APKs - dialog will persist on return")
                    // DON'T clear update info - it will be restored when user comes back
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

            // Clear pending update only when installer is launched successfully
            // (User is now in the installation flow, update will complete soon)
            updateChecker.clearPendingUpdate()
            updateInfo = null
            downloadState = ApkDownloader.DownloadState.Idle
            Log.d("MainActivity", "Cleared pending update - installer launched")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error installing APK", e)
        }
    }
}
