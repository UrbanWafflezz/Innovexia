# Release Checklist

## Pre-Release

- [ ] Test all new features thoroughly
- [ ] Run all unit tests: `./gradlew test`
- [ ] Check for compilation errors: `./gradlew build`
- [ ] Review changes since last release
- [ ] Update version numbers in `app/build.gradle.kts`:
  - [ ] Increment `versionCode` by 1
  - [ ] Update `versionName` (e.g., `1.0.8`)
- [ ] Commit version changes:
  ```bash
  git add app/build.gradle.kts
  git commit -m "Release v1.0.8: [description]"
  ```

## Release

- [ ] Create git tag matching versionName:
  ```bash
  git tag v1.0.8
  ```
- [ ] Push commits and tag to GitHub:
  ```bash
  git push origin master
  git push origin v1.0.8
  ```
- [ ] Wait for GitHub Actions to complete (3-5 minutes)
- [ ] Verify release at: https://github.com/UrbanWafflezz/Innovexia/releases
- [ ] Confirm APK file is attached to release
- [ ] Download and test the release APK manually

## Post-Release

- [ ] Install release APK on test device
- [ ] Verify app launches correctly
- [ ] Check that in-app version matches release version
- [ ] Test update detection:
  - [ ] Install previous version
  - [ ] Open app and verify update dialog appears
  - [ ] Test "Update Now" flow
  - [ ] Test "Remind Later" flow
- [ ] Monitor logs for errors:
  ```bash
  adb logcat | grep -E "Update|GitHub|Error"
  ```
- [ ] Announce release to team/users

## Troubleshooting

If update doesn't appear:
- [ ] Verify tag was pushed: `git tag -l`
- [ ] Check GitHub Actions: https://github.com/UrbanWafflezz/Innovexia/actions
- [ ] Verify release exists: https://github.com/UrbanWafflezz/Innovexia/releases/latest
- [ ] Clear app data and relaunch: `adb shell pm clear com.example.innovexia`
- [ ] Check API response:
  ```bash
  curl https://api.github.com/repos/UrbanWafflezz/Innovexia/releases/latest
  ```

---

**Template Commit Message**:
```
Release v1.0.8: [Brief description]

Changes:
- [Feature/fix 1]
- [Feature/fix 2]
- [Feature/fix 3]

Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```
