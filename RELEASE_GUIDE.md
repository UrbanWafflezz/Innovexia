# Release Guide for Developers

This guide explains how to create new releases and distribute APKs via GitHub.

## Prerequisites

Before your first release, you need to set up GitHub secrets:

### 1. Generate a Keystore (One-time Setup)

If you don't have a keystore yet, generate one:

```bash
keytool -genkey -v -keystore innovexia-release.keystore -alias innovexia -keyalg RSA -keysize 2048 -validity 10000
```

You'll be prompted to enter:
- **Keystore password**: Choose a strong password
- **Key password**: Choose a strong password (can be same as keystore)
- Your name, organization, etc.

**IMPORTANT:** Keep this keystore file safe! You'll need it for all future releases.

### 2. Convert Keystore to Base64

```bash
# On Windows (PowerShell):
[Convert]::ToBase64String([IO.File]::ReadAllBytes("innovexia-release.keystore")) | Out-File -Encoding ASCII keystore.base64.txt

# On Linux/Mac:
base64 -i innovexia-release.keystore -o keystore.base64.txt
```

### 3. Set Up GitHub Secrets

1. Go to your repository: https://github.com/UrbanWafflezz/Innovexia
2. Click **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret** and add each of these:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `KEYSTORE_BASE64` | Contents of `keystore.base64.txt` | Base64-encoded keystore |
| `KEYSTORE_PASSWORD` | Your keystore password | Password for the keystore file |
| `KEY_ALIAS` | `innovexia` (or what you chose) | Key alias from keytool |
| `KEY_PASSWORD` | Your key password | Password for the key |
| `GEMINI_API_KEY` | Your Gemini API key | For building the app |

**Note:** GitHub automatically provides `GITHUB_TOKEN` - you don't need to create this.

---

## Creating a Release

### Step 1: Update Version

Edit [`app/build.gradle.kts`](app/build.gradle.kts):

```kotlin
defaultConfig {
    versionCode = 2  // Increment this (was 1)
    versionName = "1.1.0"  // Update semantic version
    // ...
}
```

**Version Numbering:**
- `versionCode`: Integer that must increase with each release (1, 2, 3...)
- `versionName`: Semantic version string (1.0.0, 1.1.0, 2.0.0, etc.)
  - Major.Minor.Patch
  - **Major**: Breaking changes
  - **Minor**: New features (backwards compatible)
  - **Patch**: Bug fixes

### Step 2: Commit Your Changes

```bash
git add .
git commit -m "Release v1.1.0: Add [feature name]"
git push origin master
```

### Step 3: Create a Release Tag

```bash
git tag v1.1.0
git push origin v1.1.0
```

**Tag Format:** Must be `v` followed by version number (e.g., `v1.0.0`, `v2.3.1`)

### Step 4: Monitor GitHub Actions

1. Go to: https://github.com/UrbanWafflezz/Innovexia/actions
2. You'll see "Build and Release APK" workflow running
3. Wait for it to complete (~5-10 minutes)
4. If it fails, check the logs and fix any issues

### Step 5: Verify the Release

1. Go to: https://github.com/UrbanWafflezz/Innovexia/releases
2. You should see your new release with:
   - Version tag (e.g., `v1.1.0`)
   - Release notes (auto-generated from commits)
   - Downloadable APK file (e.g., `Innovexia-1.1.0.apk`)

---

## Quick Release Commands

Once everything is set up, creating a new release is just:

```bash
# 1. Edit app/build.gradle.kts to bump version
# 2. Then run:
git add app/build.gradle.kts
git commit -m "Release v1.1.0: Brief description of changes"
git push origin master
git tag v1.1.0
git push origin v1.1.0
```

That's it! GitHub Actions will automatically:
- Build the release APK
- Sign it with your keystore
- Create a GitHub Release
- Upload the APK for download

---

## GitHub CLI Authentication (2FA Setup)

Since you have 2FA enabled, you'll need to authenticate with GitHub:

### Option 1: GitHub CLI (Recommended)

```bash
# Install GitHub CLI (if not installed)
# Windows: winget install --id GitHub.cli
# Mac: brew install gh
# Linux: See https://github.com/cli/cli#installation

# Authenticate
gh auth login
```

Follow the prompts:
1. Choose **GitHub.com**
2. Choose **HTTPS**
3. Choose **Login with a web browser**
4. Copy the one-time code and paste it in browser
5. Authorize GitHub CLI

### Option 2: Personal Access Token

1. Go to: https://github.com/settings/tokens
2. Click **Generate new token (classic)**
3. Give it a name: "Innovexia Development"
4. Select scopes: `repo`, `workflow`
5. Click **Generate token**
6. Copy the token and save it securely

Then configure git to use the token:

```bash
# When pushing, use token as password
git push https://github.com/UrbanWafflezz/Innovexia.git master

# Or store it in credential manager:
git config --global credential.helper manager-core
# Next time you push, enter your GitHub username and token as password
```

---

## Testing Locally

Before pushing a release, test the build locally:

```bash
# Build debug version
./gradlew assembleDebug

# Build release version (will use debug signing since no keystore)
./gradlew assembleRelease

# Find APK at:
# app/build/outputs/apk/release/Innovexia-v1.0.0-release.apk
```

---

## Troubleshooting

### Build Fails in GitHub Actions

1. Check the error message in Actions logs
2. Common issues:
   - **Missing secret**: Add it in repository settings
   - **Wrong keystore password**: Update the secret
   - **Build error**: Fix locally first, then push

### Can't Push to GitHub (2FA)

- Use GitHub CLI authentication (see above)
- Or use personal access token instead of password

### APK Not Signed Properly

- Verify all 4 keystore secrets are set correctly in GitHub
- Make sure `KEYSTORE_BASE64` contains the entire base64 string (no line breaks)

### Users Can't Install APK

- Make sure they enabled "Install from Unknown Sources"
- Check if version was actually incremented (can't downgrade)
- For updates, they can install over existing app

---

## Release Checklist

Before each release:

- [ ] Update `versionCode` and `versionName` in `app/build.gradle.kts`
- [ ] Test app locally (run and verify features work)
- [ ] Build locally to check for errors (`./gradlew assembleRelease`)
- [ ] Commit changes with descriptive message
- [ ] Push to master branch
- [ ] Create and push version tag
- [ ] Monitor GitHub Actions for success
- [ ] Verify release appears with correct APK
- [ ] Test installing APK on real device
- [ ] Announce update to users (if applicable)

---

## Distribution

Users can download the latest version at:
- **Direct link**: https://github.com/UrbanWafflezz/Innovexia/releases/latest
- **Releases page**: https://github.com/UrbanWafflezz/Innovexia/releases

Share these links with your users for easy updates!
