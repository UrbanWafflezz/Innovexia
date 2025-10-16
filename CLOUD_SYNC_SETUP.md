# Firebase Cloud Sync Setup Guide

This guide walks you through setting up Firebase Cloud Sync for Innovexia.

## Overview

The Cloud Sync feature enables:
- ✅ **Signed-in users only** - Guest mode stays local
- ✅ **No size limits** - Messages and attachments chunked to Storage
- ✅ **Firestore for structure** - Compact documents (≤ 1 MB)
- ✅ **Storage for content** - Large messages, attachments, summaries
- ✅ **Secure by default** - User-scoped rules

## Architecture

```
/users/{uid}/
  ├── /chats/{chatId}                 (Firestore doc)
  │   ├── /messages/{messageId}       (Firestore doc)
  │   └── ...
  └── /storage/                        (Cloud Storage)
      ├── /attachments/{chatId}/{messageId}/{filename}
      └── /msgchunks/{chatId}/{messageId}/{seq}.txt
```

## Step 1: Firebase Project Setup

1. **Create Firebase project** (if not already done):
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Click "Add project" or select existing project
   - Enable Google Analytics (optional)

2. **Enable Firestore**:
   - In Firebase Console, go to Firestore Database
   - Click "Create database"
   - Start in **production mode** (we'll add rules next)
   - Choose a location (closest to your users)

3. **Enable Cloud Storage**:
   - In Firebase Console, go to Storage
   - Click "Get started"
   - Start in **production mode**
   - Use default bucket or create new one

## Step 2: Deploy Security Rules

### Firestore Rules

1. In Firebase Console → Firestore Database → Rules
2. Replace with contents of `firestore.rules`:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function authed() { return request.auth != null; }
    function isOwner(uid) { return authed() && request.auth.uid == uid; }

    match /users/{uid} {
      allow read, write: if isOwner(uid);
      match /chats/{chatId} {
        allow read, write: if isOwner(uid);
        match /messages/{messageId} {
          allow read, write: if isOwner(uid);
        }
      }
      match /chunks/{chunkId} {
        allow read, write: if isOwner(uid);
      }
      match /{document=**} {
        allow read, write: if isOwner(uid);
      }
    }
  }
}
```

3. Click "Publish"

### Storage Rules

1. In Firebase Console → Storage → Rules
2. Replace with contents of `storage.rules`:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    function authed() { return request.auth != null; }
    function isOwner(uid) { return authed() && request.auth.uid == uid; }

    match /users/{uid}/{allPaths=**} {
      allow read, write: if isOwner(uid);
    }
  }
}
```

3. Click "Publish"

## Step 3: Create Firestore Indexes

See [FIRESTORE_INDEXES.md](./FIRESTORE_INDEXES.md) for detailed instructions.

**Quick method**:
1. Enable cloud sync in the app
2. Try syncing some chats
3. Check Logcat for "index not found" errors
4. Click the auto-generated link in the error to create indexes

**Required indexes**:
- `chats` collection: `lastMsgAt DESC`
- `chats` collection: `updatedAt DESC`
- `messages` subcollection: `createdAt ASC` (usually auto-created)

## Step 4: Test the Integration

1. **Build and run** the app
2. **Sign in** with a Firebase account
3. **Open Profile** → **Cloud Sync** tab
4. **Enable Cloud Sync** toggle
5. **Check logs**:
   ```
   adb logcat | grep -E "CloudSync|Firestore|Storage"
   ```
6. **Verify in Firebase Console**:
   - Firestore Database → Data → Check `/users/{uid}/chats`
   - Storage → Files → Check `/users/{uid}/`

## Step 5: Integrate CloudSyncTab in ProfileSheet

Add the CloudSyncTab to your ProfileSheet tabbed navigation:

1. Open `ProfileSheet.kt` or similar
2. Add tab for "Cloud Sync"
3. Render `CloudSyncTab` component when selected

Example:
```kotlin
when (selectedTab) {
    0 -> ProfileTab(...)
    1 -> SecurityTab(...)
    2 -> CloudSyncTab(authViewModel = authViewModel, darkTheme = darkTheme)
    3 -> SubscriptionsTab(...)
}
```

## Cost Optimization

### Firestore
- **Reads**: ~$0.06 per 100K reads
- **Writes**: ~$0.18 per 100K writes
- **Storage**: ~$0.18/GB/month

**Tips**:
- Keep message head ≤ 12 KB (larger goes to Storage)
- Use local cache (enabled by default)
- Paginate queries with `.limit(100)`

### Cloud Storage
- **Storage**: ~$0.026/GB/month
- **Downloads**: ~$0.12/GB (class A), ~$0.01/GB (class B)

**Tips**:
- Chunk large messages into ~12 KB pieces
- Compress attachments before upload
- Use Firestore for metadata, Storage for blobs

### Free Tier (Spark Plan)
- Firestore: 50K reads/day, 20K writes/day, 1 GB storage
- Storage: 5 GB storage, 1 GB/day downloads
- Good for development and small user bases

## Monitoring

1. **Firebase Console → Usage & Billing**:
   - Check Firestore reads/writes/deletes
   - Check Storage uploads/downloads
   - Set budget alerts

2. **Firestore Console → Usage**:
   - Daily read/write/delete counts
   - Document count
   - Storage usage

3. **App Logs**:
   - CloudSyncWorker logs upload progress
   - CloudSyncEngine logs errors

## Troubleshooting

### "Missing index" error
→ Click the link in Logcat to auto-create index, or see [FIRESTORE_INDEXES.md](./FIRESTORE_INDEXES.md)

### "Permission denied" error
→ Verify security rules are deployed and user is signed in

### Sync not starting
→ Check:
- User is signed in (not guest)
- Cloud sync toggle is ON
- Network connection is active
- Check Logcat for CloudSyncWorker errors

### Messages not appearing in Firestore
→ Check:
- Message entity has correct ownerId (UID, not "guest")
- Firestore rules allow writes
- No quota limits exceeded

## Next Steps

- [ ] Deploy rules to production
- [ ] Create Firestore indexes
- [ ] Test with real user accounts
- [ ] Monitor usage and costs
- [ ] Add progress indicators in UI
- [ ] Implement download/restore from cloud (optional)
- [ ] Add conflict resolution (optional)

## Security Notes

⚠️ **Important**:
- Never commit `google-services.json` to public repos
- Use Firebase App Check to prevent abuse
- Monitor usage regularly to detect anomalies
- Implement rate limiting for expensive operations
- Consider adding re-auth requirement for delete operations

## Support

For issues or questions:
- Check Firebase Console logs
- Review Logcat for errors
- Consult [Firebase documentation](https://firebase.google.com/docs)
- Open an issue in the project repo
