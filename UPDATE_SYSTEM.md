# GitHub-Based Update System - Complete Guide

## Overview

The Innovexia app uses a GitHub Releases-based update system that automatically detects, downloads, and installs new versions from GitHub.

---

## How It Works

### 1. Update Detection
- **On App Startup**: Checks for updates when the app is launched
- **Background Checks**: Every 12 hours via WorkManager
- **Rate Limiting**: Minimum 15 minutes between checks to avoid API spam

### 2. Update Sources
- Fetches latest release from: `https://api.github.com/repos/UrbanWafflezz/Innovexia/releases/latest`
- Looks for `.apk` files in release assets
- Compares current version (`BuildConfig.VERSION_NAME`) with latest version (GitHub tag)

### 3. Update Flow
```
App Launch → Check GitHub API → Compare Versions → Show Update Dialog
     ↓                                                        ↓
Background Check (12h) ←──────────────────────────── Download APK → Install
```

---

## Releasing a New Version

### Step-by-Step Release Process

#### 1. Update Version Numbers
Edit `app/build.gradle.kts`:
```kotlin
versionCode = 8  // Increment by 1
versionName = "1.0.8"  // Follow semantic versioning
```

#### 2. Commit Changes
```bash
git add app/build.gradle.kts
git commit -m "Release v1.0.8: [Brief description of changes]"
```

#### 3. Create and Push Git Tag
```bash
# Create tag matching versionName with 'v' prefix
git tag v1.0.8

# Push commits and tag to GitHub
git push origin master
git push origin v1.0.8
```

#### 4. GitHub Actions Workflow Triggers
The `.github/workflows/release.yml` workflow will automatically:
- Build the release APK with signing
- Create a GitHub Release
- Upload the APK as a release asset
- Generate release notes from commits

#### 5. Verify Release
1. Go to https://github.com/UrbanWafflezz/Innovexia/releases
2. Verify the new release exists with the correct tag (e.g., `v1.0.8`)
3. Verify the APK file is attached (e.g., `Innovexia-1.0.8.apk`)

---

## Important Release Rules

### ✅ DO:
- Always increment both `versionCode` AND `versionName`
- Use semantic versioning: `major.minor.patch` (e.g., `1.0.8`)
- Create git tags with `v` prefix (e.g., `v1.0.8`)
- Push tags to trigger the workflow: `git push origin v1.0.8`
- Test the release APK before pushing to users

### ❌ DON'T:
- Push commits without creating a git tag (no release will be created)
- Use version numbers that don't match the tag (causes confusion)
- Delete or rename releases (breaks update detection)
- Upload multiple APK files to a single release (app grabs the first one)

---

## Update System Features

### 1. **Automatic Update Checking**
- **Frequency**: Every 15 minutes minimum (rate-limited)
- **Background**: Every 12 hours via WorkManager
- **Triggers**:
  - App startup
  - Background scheduled task
  - Manual check (future feature)

### 2. **Smart Notifications**
- Push notification when update is found (background check)
- In-app dialog when app is open
- Forced update after 7 days

### 3. **Robust Error Handling**
- Network failures → Retry with exponential backoff (1s, 2s, 4s)
- GitHub API rate limits → Automatic retry
- Server errors (5xx) → Retry up to 3 times
- Clear error messages shown to users

### 4. **Version Comparison**
- Semantic version parsing: `1.0.8` vs `1.0.7`
- Handles `v` prefix: `v1.0.8` → `1.0.8`
- Ignores pre-release suffixes: `1.0.8-beta` → `1.0.8`
- Robust fallback for malformed versions

### 5. **HTTP Logging (Debug Builds)**
- Full request/response logging in debug mode
- Helps diagnose API issues during development

### 6. **Retry Logic**
- Automatically retries failed network requests
- Exponential backoff: 1s → 2s → 4s
- Maximum 3 retries before giving up

---

## Configuration

### Rate Limit Adjustment
Edit `UpdateChecker.kt`:
```kotlin
private const val CHECK_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
```

### Background Check Interval
Edit `InnovexiaApplication.kt`:
```kotlin
val updateWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(
    12, TimeUnit.HOURS // Change to desired interval
)
```

### GitHub API Authentication (Optional)
To avoid rate limits, add a GitHub Personal Access Token:

1. Create token at: https://github.com/settings/tokens
2. No scopes needed for public repos
3. Edit `GitHubApi.kt`:
```kotlin
private const val GITHUB_TOKEN = "ghp_your_token_here"
```

**Note**: For production, store token in `local.properties` or Firebase Remote Config.

---

## Monitoring & Debugging

### Check Logs
Use Logcat filters:
- `UpdateChecker` - Version comparison and GitHub API
- `UpdateWorker` - Background update checks
- `MainActivity` - Update flow coordination
- `ApkDownloader` - Download progress

### Common Log Messages
```
D/UpdateChecker: Checking for updates from GitHub...
D/UpdateChecker: Current version: 1.0.7, Latest version: 1.0.8
D/UpdateChecker: Update available: 1.0.8
D/MainActivity: Update available: 1.0.8
D/UpdateWorker: Update notification shown for version 1.0.8
```

### Error Scenarios

| Error | Cause | Solution |
|-------|-------|----------|
| `HTTP 403` | GitHub API rate limit | Add GitHub token or wait 1 hour |
| `HTTP 404` | No releases found | Verify release exists on GitHub |
| `No APK file found` | Release has no .apk asset | Ensure workflow built and uploaded APK |
| `Connection timeout` | Network issue | Check internet connection |
| `Already checked recently` | Rate limit active | Wait 15 minutes or clear app data |

---

## Testing the Update System

### 1. **Test on Debug Build**
```bash
# Build and install current version
./gradlew installDebug

# Update version in build.gradle.kts
versionCode = 999
versionName = "9.9.9"

# Create test release on GitHub
git tag v9.9.9 && git push origin v9.9.9

# Launch app and verify update dialog appears
```

### 2. **Force Update Check**
Clear app data to reset rate limits:
```bash
adb shell pm clear com.example.innovexia.debug
```

### 3. **Test Background Worker**
Trigger WorkManager manually:
```bash
adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" \
  -p com.example.innovexia.debug
```

---

## Troubleshooting

### Updates Not Showing Up?

**Checklist**:
1. ✅ Did you create a git tag? (`git tag v1.0.8`)
2. ✅ Did you push the tag? (`git push origin v1.0.8`)
3. ✅ Did GitHub Actions complete successfully?
4. ✅ Does the release exist at https://github.com/UrbanWafflezz/Innovexia/releases/latest?
5. ✅ Is there a `.apk` file attached to the release?
6. ✅ Is the version name in `build.gradle.kts` correct?
7. ✅ Has it been more than 15 minutes since the last check?

### Clear Update Preferences (for testing)
```kotlin
// Add to MainActivity temporarily for testing
updateChecker.clearAllPreferences()
```

### Verify GitHub API Response
Test the API manually:
```bash
curl https://api.github.com/repos/UrbanWafflezz/Innovexia/releases/latest
```

---

## Architecture

### Key Files

| File | Purpose |
|------|---------|
| `GitHubApi.kt` | Retrofit interface for GitHub Releases API |
| `UpdateChecker.kt` | Core update detection logic |
| `UpdateModels.kt` | Data classes and version comparison |
| `UpdateWorker.kt` | Background WorkManager task |
| `UpdateAvailableDialog.kt` | Update prompt UI |
| `ApkDownloader.kt` | APK download with progress |
| `MainActivity.kt` | Update flow coordination |
| `InnovexiaApplication.kt` | WorkManager scheduling |

### Data Flow
```
GitHub API → UpdateChecker → UpdateCheckResult
     ↓                              ↓
UpdateWorker (background)    MainActivity (foreground)
     ↓                              ↓
Notification                  UpdateDialog
     ↓                              ↓
     └───────→ ApkDownloader ←──────┘
                    ↓
              Install APK
```

---

## Security Considerations

1. **APK Signing**: All releases are signed with the app's release keystore
2. **HTTPS Only**: GitHub API uses HTTPS for secure communication
3. **FileProvider**: APK files are shared securely via FileProvider
4. **Permissions**: `REQUEST_INSTALL_PACKAGES` required for Android 8.0+

---

## Future Enhancements

- [ ] Manual "Check for Updates" button in settings
- [ ] Rollback mechanism for bad updates
- [ ] Delta updates (only download changes)
- [ ] Multi-APK support (different architectures)
- [ ] Update changelog view before download
- [ ] Analytics for update acceptance rates
- [ ] Background download without user prompt
- [ ] Auto-install after download (root required)

---

## Support

For issues or questions:
- GitHub Issues: https://github.com/UrbanWafflezz/Innovexia/issues
- Check logs with `adb logcat | grep -E "Update|GitHub"`
- Verify release workflow at: https://github.com/UrbanWafflezz/Innovexia/actions

---

**Last Updated**: 2025-10-17
**System Version**: 2.0 (Enhanced with background checks and error handling)
