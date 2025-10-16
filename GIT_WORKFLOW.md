# Git & GitHub Workflow Guide

This guide contains all the commands and steps you need to work with Git and GitHub for the Innovexia project.

---

## Table of Contents

1. [Daily Development Workflow](#daily-development-workflow)
2. [Creating a New Release](#creating-a-new-release)
3. [Checking Status](#checking-status)
4. [Fixing Mistakes](#fixing-mistakes)
5. [Branch Management](#branch-management)
6. [Useful Commands Reference](#useful-commands-reference)

---

## Daily Development Workflow

### Step 1: Check What Changed

Before making changes, see what files have been modified:

```bash
git status
```

To see exactly what changed in files:

```bash
git diff
```

### Step 2: Stage Your Changes

Add all changed files:

```bash
git add .
```

Or add specific files:

```bash
git add path/to/file.kt
git add app/build.gradle.kts
```

### Step 3: Commit Your Changes

Create a commit with a descriptive message:

```bash
git commit -m "Brief description of what you changed"
```

**Good commit message examples:**
- `git commit -m "Fix: Crash when opening settings"`
- `git commit -m "Add: Voice input feature"`
- `git commit -m "Update: Improve UI performance"`
- `git commit -m "Remove: Unused TensorFlow dependencies"`

### Step 4: Push to GitHub

Push your commits to GitHub:

```bash
git push origin master
```

### Complete Daily Workflow (All Steps Combined)

```bash
# 1. Check status
git status

# 2. Add all changes
git add .

# 3. Commit with message
git commit -m "Your commit message here"

# 4. Push to GitHub
git push origin master
```

---

## Creating a New Release

When you're ready to release a new version to users:

### Step 1: Update Version Numbers

Edit `app/build.gradle.kts` and update these two lines:

```kotlin
versionCode = 2  // Increment by 1 (was 1, now 2)
versionName = "1.1.0"  // Follow semantic versioning
```

**Version Naming:**
- **Major** (1.0.0 → 2.0.0): Breaking changes, major new features
- **Minor** (1.0.0 → 1.1.0): New features, backwards compatible
- **Patch** (1.0.0 → 1.0.1): Bug fixes only

### Step 2: Commit Version Changes

```bash
git add app/build.gradle.kts
git commit -m "Release v1.1.0: Brief description of changes"
git push origin master
```

### Step 3: Create and Push Release Tag

```bash
# Create tag (must match versionName with 'v' prefix)
git tag v1.1.0

# Push tag to trigger release build
git push origin v1.1.0
```

### Step 4: Monitor Build

1. Go to: https://github.com/UrbanWafflezz/Innovexia/actions
2. Watch "Build and Release APK" workflow
3. Wait 5-10 minutes for completion

### Step 5: Verify Release

Once complete, check:
- Release page: https://github.com/UrbanWafflezz/Innovexia/releases
- Download link: https://github.com/UrbanWafflezz/Innovexia/releases/latest

### Complete Release Workflow (Example for v1.1.0)

```bash
# 1. Edit app/build.gradle.kts (update versionCode and versionName)

# 2. Commit version change
git add app/build.gradle.kts
git commit -m "Release v1.1.0: Add voice input and improve performance"
git push origin master

# 3. Create and push tag
git tag v1.1.0
git push origin v1.1.0

# 4. Monitor at: https://github.com/UrbanWafflezz/Innovexia/actions
```

---

## Checking Status

### See What Files Changed

```bash
git status
```

### See What Lines Changed

```bash
# See all changes
git diff

# See changes in specific file
git diff path/to/file.kt
```

### View Commit History

```bash
# See recent commits
git log --oneline -10

# See all commits
git log

# See commits with file changes
git log --stat
```

### View Current Branch

```bash
git branch
```

### View Remote URL

```bash
git remote -v
```

---

## Fixing Mistakes

### Undo Changes to a File (Before Commit)

```bash
# Discard changes to specific file
git checkout -- path/to/file.kt

# Discard all changes (WARNING: loses all uncommitted work!)
git checkout -- .
```

### Unstage a File (After `git add`)

```bash
# Unstage specific file
git reset HEAD path/to/file.kt

# Unstage all files
git reset HEAD
```

### Undo Last Commit (Keep Changes)

```bash
# Undo commit but keep changes staged
git reset --soft HEAD~1

# Undo commit and unstage changes
git reset HEAD~1
```

### Undo Last Commit (Delete Changes)

```bash
# WARNING: This deletes your changes permanently!
git reset --hard HEAD~1
```

### Fix Last Commit Message

```bash
git commit --amend -m "New commit message"

# If already pushed, force push
git push origin master --force
```

### Delete a Tag (If You Made a Mistake)

```bash
# Delete tag locally
git tag -d v1.0.0

# Delete tag from GitHub
git push origin :refs/tags/v1.0.0

# Then create correct tag and push
git tag v1.0.0
git push origin v1.0.0
```

---

## Branch Management

### Create a New Branch

```bash
# Create and switch to new branch
git checkout -b feature-name

# Or separate commands
git branch feature-name
git checkout feature-name
```

### Switch Between Branches

```bash
git checkout master
git checkout feature-name
```

### Push Branch to GitHub

```bash
git push origin feature-name
```

### Merge Branch into Master

```bash
# Switch to master
git checkout master

# Merge feature branch
git merge feature-name

# Push merged changes
git push origin master

# Delete merged branch (optional)
git branch -d feature-name
git push origin --delete feature-name
```

---

## Useful Commands Reference

### Quick Reference Table

| Task | Command |
|------|---------|
| Check status | `git status` |
| See changes | `git diff` |
| Stage all changes | `git add .` |
| Stage specific file | `git add path/to/file` |
| Commit | `git commit -m "message"` |
| Push to GitHub | `git push origin master` |
| Pull from GitHub | `git pull origin master` |
| Create tag | `git tag v1.0.0` |
| Push tag | `git push origin v1.0.0` |
| Delete local tag | `git tag -d v1.0.0` |
| Delete remote tag | `git push origin :refs/tags/v1.0.0` |
| View commits | `git log --oneline -10` |
| Discard changes | `git checkout -- file` |
| Unstage file | `git reset HEAD file` |
| Undo last commit | `git reset HEAD~1` |

### Daily Shortcuts

**Make changes and push:**
```bash
git add . && git commit -m "Your message" && git push origin master
```

**Create release:**
```bash
git tag v1.1.0 && git push origin v1.1.0
```

**Delete and recreate tag:**
```bash
git tag -d v1.0.0 && git push origin :refs/tags/v1.0.0 && git tag v1.0.0 && git push origin v1.0.0
```

---

## Common Scenarios

### Scenario 1: I Made Changes and Want to Push

```bash
git status                           # See what changed
git add .                            # Stage all changes
git commit -m "Fixed bug in chat"   # Commit with message
git push origin master               # Push to GitHub
```

### Scenario 2: I Want to Release a New Version

```bash
# 1. Edit app/build.gradle.kts (update versionCode and versionName)

git add app/build.gradle.kts
git commit -m "Release v1.2.0: New features"
git push origin master
git tag v1.2.0
git push origin v1.2.0

# 2. Wait for GitHub Actions to build
# 3. Check: https://github.com/UrbanWafflezz/Innovexia/releases
```

### Scenario 3: Release Build Failed, Need to Fix and Retry

```bash
# 1. Fix the issue in your code

git add .
git commit -m "Fix: Build error in release"
git push origin master

# 2. Delete old tag
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0

# 3. Create new tag
git tag v1.0.0
git push origin v1.0.0
```

### Scenario 4: I Want to See Recent Changes

```bash
git log --oneline -10               # Last 10 commits
git diff                            # Changes since last commit
git status                          # Current file status
```

### Scenario 5: I Made a Mistake and Want to Undo

```bash
# Undo uncommitted changes
git checkout -- .

# Undo last commit but keep changes
git reset HEAD~1

# Undo last commit and delete changes (DANGEROUS!)
git reset --hard HEAD~1
```

---

## GitHub Links

- **Repository**: https://github.com/UrbanWafflezz/Innovexia
- **Actions (Build Status)**: https://github.com/UrbanWafflezz/Innovexia/actions
- **Releases**: https://github.com/UrbanWafflezz/Innovexia/releases
- **Latest Release**: https://github.com/UrbanWafflezz/Innovexia/releases/latest
- **Settings/Secrets**: https://github.com/UrbanWafflezz/Innovexia/settings/secrets/actions

---

## Tips

1. **Commit Often**: Make small, focused commits rather than huge ones
2. **Write Good Messages**: Future you will thank you
3. **Check Before Push**: Always run `git status` and `git diff` before committing
4. **Test Before Release**: Make sure the app works before creating a release tag
5. **Keep Master Stable**: Don't push broken code to master
6. **Use Branches**: For experimental features, create a branch first

---

## Getting Help

If you see an error:
1. Read the error message carefully
2. Check `git status` to see current state
3. Check the Actions logs: https://github.com/UrbanWafflezz/Innovexia/actions
4. See [RELEASE_GUIDE.md](RELEASE_GUIDE.md) for release-specific help

---

## Version History

- **v1.0.0**: Initial release with all core features
- *(Update this when you create new releases)*
