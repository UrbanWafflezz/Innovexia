# System Health Monitoring - Setup Guide

## Overview

Innovexia includes a production-grade health monitoring system that performs **real HTTP checks** against configured service endpoints, maintains a persistent incident database, and provides 24/7 background monitoring via WorkManager.

## Features

✅ **Real HTTP Health Checks** - No simulation, actual endpoint calls
✅ **Local Database Probes** - Round-trip latency measurement
✅ **24/7 Background Monitoring** - WorkManager periodic tasks
✅ **Foreground Real-time Updates** - 20-second interval when UI visible
✅ **Incident Tracking** - Automatic incident creation and resolution
✅ **Push Notifications** - Alerts on service state transitions
✅ **Network Awareness** - Detects connectivity and avoids false alarms
✅ **System Health UI** - Comprehensive monitoring dashboard in Settings

---

## BuildConfig Configuration

### 1. Add Service Endpoints to `app/build.gradle.kts`

Health endpoints are configured via `BuildConfig` fields. Add these to your `android.buildTypes` or `android.productFlavors`:

```kotlin
android {
    // ...

    buildTypes {
        debug {
            buildConfigField("String", "CONTEXT_BASE", "\"https://api-dev.innovexia.local/context\"")
            buildConfigField("String", "CONTEXT_WS", "\"wss://api-dev.innovexia.local/context/ws\"")
            buildConfigField("String", "SUMMARIZER_BASE", "\"https://api-dev.innovexia.local/summarizer\"")
            buildConfigField("String", "GEMINI_BRIDGE_BASE", "\"https://api-dev.innovexia.local/gemini\"")
            buildConfigField("String", "EMBEDDINGS_BASE", "\"https://api-dev.innovexia.local/embeddings\"")
            buildConfigField("String", "FILES_BASE", "\"https://api-dev.innovexia.local/files\"")
            buildConfigField("String", "PERSONA_BASE", "\"https://api-dev.innovexia.local/persona\"")
        }

        release {
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
```

### 2. Alternative: Product Flavors

For multi-environment setups (dev/stage/prod):

```kotlin
android {
    // ...

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            buildConfigField("String", "CONTEXT_BASE", "\"https://api-dev.innovexia.local/context\"")
            // ... other fields
        }

        create("stage") {
            dimension = "environment"
            applicationIdSuffix = ".stage"
            buildConfigField("String", "CONTEXT_BASE", "\"https://api-stage.innovexia.com/context\"")
            // ... other fields
        }

        create("prod") {
            dimension = "environment"
            buildConfigField("String", "CONTEXT_BASE", "\"https://api.innovexia.com/context\"")
            // ... other fields
        }
    }
}
```

### 3. Leave Empty for Development Without Services

If you don't have backend services yet, the system will gracefully handle empty URLs:

```kotlin
buildConfigField("String", "CONTEXT_BASE", "\"\"")
```

Services with empty URLs will show as `UNKNOWN` status.

---

## Expected Health Endpoint Contract

Each service should expose a `/health` endpoint returning JSON:

```json
{
  "status": "online",
  "version": "2025.10.1",
  "uptimeSec": 123456,
  "latencyMs": 18,
  "notes": "OK",
  "dependencies": [
    {"id": "db", "status": "online"},
    {"id": "redis", "status": "online"}
  ]
}
```

### Required Fields

- **`status`** (string): `"online"` | `"degraded"` | `"offline"`

### Optional Fields

- **`version`** (string): Service version
- **`uptimeSec`** (number): Uptime in seconds
- **`latencyMs`** (number): Internal latency
- **`notes`** (string): Human-readable status message
- **`dependencies`** (array): Status of downstream dependencies

---

## Service Catalog

Configured services (see `ServiceCatalog.kt`):

| Service ID        | Name                    | Type     | Notes                          |
|-------------------|-------------------------|----------|--------------------------------|
| `context-engine`  | Context Memory Engine   | HTTP     | Memory storage and retrieval   |
| `summarizer`      | Rolling Summarizer      | HTTP     | Chat summarization service     |
| `gemini-bridge`   | Gemini Bridge           | HTTP     | AI API gateway                 |
| `database`        | Local Database          | Local    | Room DB round-trip latency     |
| `cache`           | Local Cache             | Local    | In-memory cache (placeholder)  |
| `embeddings`      | Embeddings Service      | HTTP     | Vector embeddings              |
| `files`           | File Service            | HTTP     | File storage and retrieval     |
| `persona`         | Persona Service         | HTTP     | Persona management             |

---

## Background Monitoring

### WorkManager Configuration

- **Interval**: 15 minutes (minimum for `PeriodicWorkRequest`)
- **Constraints**: Network required (`NetworkType.CONNECTED`)
- **Backoff**: Exponential with minimum delay

### Notifications

When a service transitions state (e.g., `ONLINE` → `DEGRADED` or `OFFLINE`), a notification is posted:

- **Channel**: `system_health`
- **Title**: "System Health Alert"
- **Content**: List of services that changed status

To customize notification behavior, edit `HealthWorker.kt`.

---

## Foreground Monitoring

When the System Health UI is visible:

- **Interval**: 20 seconds (default)
- **Method**: `SystemHealthViewModel.startForegroundMonitoring()`
- **Auto-stops**: When ViewModel is cleared

---

## Database Schema

### `health_checks` Table

| Column           | Type    | Description                      |
|------------------|---------|----------------------------------|
| `serviceId`      | String  | Primary key                      |
| `name`           | String  | Human-readable service name      |
| `status`         | String  | `ONLINE`/`DEGRADED`/`OFFLINE`/`UNKNOWN` |
| `latencyMs`      | Long?   | Measured latency                 |
| `version`        | String? | Service version                  |
| `lastCheckedAt`  | Long    | Timestamp (ms)                   |
| `notes`          | String? | Additional information           |

### `incidents` Table

| Column           | Type    | Description                      |
|------------------|---------|----------------------------------|
| `id`             | String  | Primary key (UUID)               |
| `serviceId`      | String  | Foreign key to service           |
| `status`         | String  | `Open`/`Monitoring`/`Resolved`   |
| `impact`         | String  | Description of impact            |
| `startedAt`      | Long    | Incident start timestamp         |
| `endedAt`        | Long?   | Resolution timestamp (null if open) |

---

## Usage in UI

### Integrate System Health Tab

Example integration in Settings sheet:

```kotlin
@Composable
fun SettingsScreen() {
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

    // Start foreground monitoring
    LaunchedEffect(Unit) {
        viewModel.startForegroundMonitoring()
    }

    // Perform initial refresh
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
        darkTheme = isSystemInDarkTheme()
    )
}
```

---

## Security & Hardening

### HTTP Client Configuration

- ✅ **Timeouts**: 3s connect, 3s read, 3s write
- ✅ **No retries**: Manual control via backoff logic
- ✅ **TLS**: HTTPS enforced
- ⚠️ **Certificate Pinning**: Optional (add to `OkHttpClient` if needed)

### Example: Add Certificate Pinning

```kotlin
val okHttp = OkHttpClient.Builder()
    .certificatePinner(
        CertificatePinner.Builder()
            .add("api.innovexia.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()
    )
    .build()
```

---

## Troubleshooting

### Services Show as `UNKNOWN`

- Check that `BuildConfig` fields are defined and not empty
- Verify endpoints are accessible from the device/emulator
- Check network connectivity

### HTTP Checks Fail

- Ensure services return HTTP 200 with valid JSON
- Check for CORS/SSL certificate issues
- Verify `/health` path is correct

### No Notifications

- Check notification permissions (Android 13+)
- Verify notification channel is created
- Ensure WorkManager constraints are satisfied (network available)

### Database Version Conflicts

If Room migration fails:
- Increment database version in `AppDatabase`
- Add migration or use `.fallbackToDestructiveMigration()`

---

## Next Steps

1. **Add BuildConfig Fields**: Update `app/build.gradle.kts` with your service URLs
2. **Rebuild Project**: `./gradlew clean build`
3. **Test Health Checks**: Navigate to Settings → System Health
4. **Configure Notifications**: Customize `HealthWorker` notification behavior
5. **Add Certificate Pinning**: For production, add TLS pinning to `OkHttpClient`

---

## Support

For issues or questions, see:
- `ServiceCatalog.kt` - Service configuration
- `RealHealthApi.kt` - Health check logic
- `SystemHealthTab.kt` - UI implementation
- `HealthWorker.kt` - Background monitoring

