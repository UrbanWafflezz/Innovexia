# System Health Integration Example

## Adding System Health Tab to Settings

Here's a complete example of how to integrate the System Health monitoring into your Settings UI.

### Step 1: Create a Settings Screen with Tabs

```kotlin
package com.example.innovexia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.ui.components.SystemHealthTab
import com.example.innovexia.ui.viewmodels.SystemHealthViewModel
import com.example.innovexia.ui.viewmodels.SystemHealthViewModelFactory

@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    darkTheme: Boolean
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("General", "System Health", "About")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> GeneralSettingsTab(darkTheme)
                1 -> SystemHealthTabWrapper(darkTheme)
                2 -> AboutTab(darkTheme)
            }
        }
    }
}

@Composable
private fun SystemHealthTabWrapper(darkTheme: Boolean) {
    val context = LocalContext.current
    val app = context.applicationContext as InnovexiaApplication

    val viewModel: SystemHealthViewModel = viewModel(
        factory = SystemHealthViewModelFactory(
            healthApi = app.healthApi,
            database = app.database
        )
    )

    val checks by viewModel.checks.collectAsState()
    val overallState by viewModel.overallState.collectAsState()
    val openIncidents by viewModel.openIncidents.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lastRefresh by viewModel.lastRefresh.collectAsState()
    val isConnected by app.connectivity.isConnected.collectAsState()

    // Start foreground monitoring when this tab is visible
    DisposableEffect(Unit) {
        viewModel.startForegroundMonitoring(intervalMs = 20_000)
        onDispose {
            viewModel.stopForegroundMonitoring()
        }
    }

    // Initial refresh
    LaunchedEffect(Unit) {
        viewModel.refreshAll()
    }

    SystemHealthTab(
        checks = checks,
        overallState = overallState,
        openIncidents = openIncidents,
        isRefreshing = isRefreshing,
        lastRefresh = lastRefresh,
        isConnected = isConnected,
        onRefresh = { viewModel.refreshAll() },
        onCheckService = { serviceId -> viewModel.checkService(serviceId) },
        darkTheme = darkTheme
    )
}

@Composable
private fun GeneralSettingsTab(darkTheme: Boolean) {
    // Your existing general settings
    Text("General Settings", modifier = Modifier.padding(16.dp))
}

@Composable
private fun AboutTab(darkTheme: Boolean) {
    // About/version info
    Text("About Innovexia", modifier = Modifier.padding(16.dp))
}
```

### Step 2: Add BuildConfig Fields

In `app/build.gradle.kts`:

```kotlin
android {
    // ...

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            // Health monitoring endpoints
            buildConfigField("String", "CONTEXT_BASE", "\"https://api-dev.innovexia.local/context\"")
            buildConfigField("String", "CONTEXT_WS", "\"wss://api-dev.innovexia.local/context/ws\"")
            buildConfigField("String", "SUMMARIZER_BASE", "\"https://api-dev.innovexia.local/summarizer\"")
            buildConfigField("String", "GEMINI_BRIDGE_BASE", "\"https://api-dev.innovexia.local/gemini\"")
            buildConfigField("String", "EMBEDDINGS_BASE", "\"https://api-dev.innovexia.local/embeddings\"")
            buildConfigField("String", "FILES_BASE", "\"https://api-dev.innovexia.local/files\"")
            buildConfigField("String", "PERSONA_BASE", "\"https://api-dev.innovexia.local/persona\"")
        }

        release {
            // Production endpoints
            buildConfigField("String", "CONTEXT_BASE", "\"https://api.innovexia.com/context\"")
            buildConfigField("String", "CONTEXT_WS", "\"wss://api.innovexia.com/context/ws\"")
            buildConfigField("String", "SUMMARIZER_BASE", "\"https://api.innovexia.com/summarizer\"")
            buildConfigField("String", "GEMINI_BRIDGE_BASE", "\"https://api.innovexia.com/gemini\"")
            buildConfigField("String", "EMBEDDINGS_BASE", "\"https://api.innovexia.com/embeddings\"")
            buildConfigField("String", "FILES_BASE", "\"https://api.innovexia.com/files\"")
            buildConfigField("String", "PERSONA_BASE", "\"https://api.innovexia.com/persona\"")
        }
    }
}

dependencies {
    // Required dependencies (if not already added)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
```

### Step 3: Add Required Permissions

In `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Network permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Notification permission (Android 13+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".InnovexiaApplication"
        ...>

        <!-- WorkManager initialization -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup" />
        </provider>

    </application>
</manifest>
```

### Step 4: Request Notification Permission (Android 13+)

```kotlin
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            // Handle permission result
        }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
```

### Step 5: Testing

#### Local Testing (No Backend)

If you don't have backend services yet, use empty strings:

```kotlin
buildConfigField("String", "CONTEXT_BASE", "\"\"")
```

Services will show as `UNKNOWN` status, but the database check will work.

#### With Mock Server

Use a tool like [MockServer](https://www.mock-server.com/) or create simple Flask/Express endpoints:

```python
# Python Flask example
from flask import Flask, jsonify

app = Flask(__name__)

@app.route('/health')
def health():
    return jsonify({
        'status': 'online',
        'version': '1.0.0',
        'latencyMs': 10,
        'notes': 'OK'
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080)
```

Then use `http://10.0.2.2:8080` (Android emulator localhost).

---

## Complete File Structure

```
app/src/main/java/com/example/innovexia/
├── core/
│   ├── health/
│   │   ├── HealthModels.kt              ✅ Created
│   │   ├── ServiceCatalog.kt            ✅ Created
│   │   ├── HealthRetrofitApi.kt         ✅ Created
│   │   └── RealHealthApi.kt             ✅ Created
│   └── net/
│       └── Connectivity.kt              ✅ Created
│
├── data/
│   └── local/
│       ├── entities/
│       │   └── HealthEntities.kt        ✅ Created
│       ├── dao/
│       │   ├── HealthCheckDao.kt        ✅ Created
│       │   └── IncidentDao.kt           ✅ Created
│       └── AppDatabase.kt               ✅ Updated
│
├── ui/
│   ├── components/
│   │   └── SystemHealthTab.kt           ✅ Created
│   └── viewmodels/
│       ├── SystemHealthViewModel.kt     ✅ Created
│       └── SystemHealthViewModelFactory.kt ✅ Created
│
├── workers/
│   └── HealthWorker.kt                  ✅ Created
│
└── InnovexiaApplication.kt              ✅ Updated
```

---

## Verification Checklist

- [ ] Database version incremented to 3
- [ ] BuildConfig fields added to build.gradle.kts
- [ ] Permissions added to AndroidManifest.xml
- [ ] InnovexiaApplication updated with health monitoring
- [ ] Settings UI includes System Health tab
- [ ] App builds successfully
- [ ] System Health tab loads and shows services
- [ ] Refresh button triggers health checks
- [ ] Database check shows latency
- [ ] WorkManager scheduled (check in Settings → Developer Options → Foreground Services)
- [ ] Notifications appear on state transitions (if permission granted)

---

## Next Steps

1. **Deploy Backend Services**: Implement `/health` endpoints on your services
2. **Configure TLS Pinning**: Add certificate pinning for production
3. **Customize Notifications**: Adjust notification behavior in `HealthWorker.kt`
4. **Add WebSocket Support**: Implement real-time health updates (optional)
5. **Monitor Incidents**: Use the incidents table to track historical outages

