package com.example.innovexia

import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.innovexia.core.session.SessionRecorder
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
        }
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
}
