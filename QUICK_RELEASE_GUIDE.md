# Quick Release Guide - Copy & Paste Commands

Use this guide when you want to release a new version of your app.

---

## Step 1: Update Version Number

**Open this file:** `app/build.gradle.kts`

**Find these lines** (around line 19-20):
```kotlin
versionCode = 2
versionName = "1.0.1"
```

**Change them to:**
```kotlin
versionCode = 3          // Increment by 1 (was 2, now 3)
versionName = "1.1.0"    // New version number
```

**Version Naming:**
- **Patch** (1.0.0 → 1.0.1): Bug fixes only
- **Minor** (1.0.0 → 1.1.0): New features, backwards compatible
- **Major** (1.0.0 → 2.0.0): Breaking changes

**Save the file.**

---

## Step 2: Commit and Push

**Copy and paste these commands:**

```bash
git add app/build.gradle.kts
git commit -m "Release v1.1.0: Brief description of changes"
git push origin master
```

**Note:** Replace `v1.1.0` and the description with your actual version and changes.

---

## Step 3: Create and Push Release Tag

**Copy and paste these commands:**

```bash
git tag v1.1.0
git push origin v1.0.1
```

**IMPORTANT:** Replace `v1.1.0` with your actual version number!

---

## Complete Example: Release v1.2.0

Here's a complete example for releasing version 1.2.0:

### 1. Edit `app/build.gradle.kts`:
```kotlin
versionCode = 4          // Was 3, now 4
versionName = "1.2.0"    // Was 1.1.0, now 1.2.0
```

### 2. Run these commands:
```bash
git add app/build.gradle.kts
git commit -m "Release v1.2.0: Added voice input and improved UI"
git push origin master
git tag v1.2.0
git push origin v1.2.0
```

### 3. Done!
- Monitor build: https://github.com/UrbanWafflezz/Innovexia/actions
- Download when ready: https://github.com/UrbanWafflezz/Innovexia/releases/latest

---

## Quick Reference Table

| Version Type | Example Change | When to Use |
|--------------|---------------|-------------|
| **Patch** | 1.0.0 → 1.0.1 | Bug fixes, small tweaks |
| **Minor** | 1.0.0 → 1.1.0 | New features, improvements |
| **Major** | 1.0.0 → 2.0.0 | Big changes, redesign |

---

## Common Release Examples

### Example 1: Bug Fix (Patch)

**Edit build.gradle.kts:**
```kotlin
versionCode = 5
versionName = "1.2.1"
```

**Commands:**
```bash
git add app/build.gradle.kts
git commit -m "Release v1.2.1: Fix crash when opening settings"
git push origin master
git tag v1.2.1
git push origin v1.2.1
```

---

### Example 2: New Feature (Minor)

**Edit build.gradle.kts:**
```kotlin
versionCode = 6
versionName = "1.3.0"
```

**Commands:**
```bash
git add app/build.gradle.kts
git commit -m "Release v1.3.0: Add dark mode and voice input"
git push origin master
git tag v1.3.0
git push origin v1.3.0
```

---

### Example 3: Major Update (Major)

**Edit build.gradle.kts:**
```kotlin
versionCode = 7
versionName = "2.0.0"
```

**Commands:**
```bash
git add app/build.gradle.kts
git commit -m "Release v2.0.0: Complete redesign with new features"
git push origin master
git tag v2.0.0
git push origin v2.0.0
```

---

## What Happens After You Push the Tag?

1. **GitHub Actions automatically starts building** (5-10 minutes)
2. **APK is signed** with your keystore
3. **Release is created** at: https://github.com/UrbanWafflezz/Innovexia/releases
4. **Users can download** the new APK

---

## Troubleshooting

### If build fails:

1. Check logs: https://github.com/UrbanWafflezz/Innovexia/actions
2. Fix the issue in your code
3. Commit and push the fix:
   ```bash
   git add .
   git commit -m "Fix: Build error"
   git push origin master
   ```
4. Delete and recreate the tag:
   ```bash
   git tag -d v1.1.0
   git push origin :refs/tags/v1.1.0
   git tag v1.1.0
   git push origin v1.1.0
   ```

---

## Template for Your Notes

Copy this template and fill in the blanks:

```
RELEASE v_____

1. Edit app/build.gradle.kts:
   versionCode = ___
   versionName = "_____"

2. Commands:
   git add app/build.gradle.kts
   git commit -m "Release v_____: _____________________"
   git push origin master
   git tag v_____
   git push origin v_____

3. Monitor: https://github.com/UrbanWafflezz/Innovexia/actions
4. Download: https://github.com/UrbanWafflezz/Innovexia/releases/latest
```

---

## Current Version

**Last Release:** v1.0.1
**Next Version Should Be:** v1.0.2 (bug fix) or v1.1.0 (new feature)

---

## Quick Copy-Paste Checklist

Before each release, copy and check these:

- [ ] Updated `versionCode` (increment by 1)
- [ ] Updated `versionName` (use semantic versioning)
- [ ] Tested app locally (works in Android Studio)
- [ ] No uncommitted changes (`git status` is clean)
- [ ] Ready to push to GitHub

Then run:
```bash
git add app/build.gradle.kts
git commit -m "Release vX.X.X: Description"
git push origin master
git tag vX.X.X
git push origin vX.X.X
```

**Done!** ✅
