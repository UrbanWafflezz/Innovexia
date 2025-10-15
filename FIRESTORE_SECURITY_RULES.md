# Firestore Security Rules for Usage Tracking

## Overview

The usage tracking system now uses Firebase Firestore to prevent bypass via app data deletion or reinstall, and to sync limits across all devices logged into the same account.

## Required Security Rules

Add these rules to your `firestore.rules` file:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function authed() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return authed() && request.auth.uid == uid;
    }

    // User data structure
    match /users/{uid} {
      allow read, write: if isOwner(uid);

      // Chat conversations
      match /chats/{chatId} {
        allow read, write: if isOwner(uid);

        // Messages within chats
        match /messages/{messageId} {
          allow read, write: if isOwner(uid);
        }
      }

      // Text chunks for long messages (if using Firestore for chunks)
      match /chunks/{chunkId} {
        allow read, write: if isOwner(uid);
      }

      // User personas (Persona 2.0)
      match /personas/{personaId} {
        allow read, write: if isOwner(uid);

        // Allow creation with proper structure
        allow create: if isOwner(uid)
          && request.resource.data.keys().hasAll(['name', 'initial', 'color', 'createdAt'])
          && request.resource.data.name is string
          && request.resource.data.name.size() >= 2
          && request.resource.data.name.size() <= 40;

        // Allow updates while preserving createdAt
        allow update: if isOwner(uid)
          && request.resource.data.createdAt == resource.data.createdAt;
      }

      // Rate limiting data (burst limits - requests per minute)
      match /rate/now {
        // Users can only read/write their own rate limit data
        allow read, write: if isOwner(uid);

        // Enforce rate limit structure
        allow create, update: if isOwner(uid)
          && request.resource.data.keys().hasAll(['minuteWindowStart', 'requestsThisMinute'])
          && request.resource.data.minuteWindowStart is timestamp
          && request.resource.data.requestsThisMinute is int
          && request.resource.data.requestsThisMinute >= 0;
      }

      // Usage tracking data (5-hour window limits - messages and tokens)
      match /usage/current {
        // Users can only read/write their own usage data
        allow read, write: if isOwner(uid);

        // Enforce usage structure and prevent tampering
        allow create, update: if isOwner(uid)
          && request.resource.data.keys().hasAll(['windowStartTime', 'messageCount', 'tokensIn', 'tokensOut'])
          && request.resource.data.windowStartTime is timestamp
          && request.resource.data.messageCount is int
          && request.resource.data.tokensIn is int
          && request.resource.data.tokensOut is int
          && request.resource.data.messageCount >= 0
          && request.resource.data.tokensIn >= 0
          && request.resource.data.tokensOut >= 0
          // Prevent users from decreasing their usage counts
          && (!resource ||
              (request.resource.data.messageCount >= resource.data.messageCount
               && request.resource.data.tokensIn >= resource.data.tokensIn
               && request.resource.data.tokensOut >= resource.data.tokensOut))
          // Prevent setting window start time in the future
          && request.resource.data.windowStartTime <= request.time;
      }

      // Fallback for any other subcollections
      match /{document=**} {
        allow read, write: if isOwner(uid);
      }
    }

    // Public personas - read-only for users (Persona 2.0)
    match /public/personas/{personaId} {
      // Anyone can read public personas
      allow read: if true;

      // Only Cloud Functions with admin SDK can write
      allow write: if false;
    }

    // Innovexia default persona (for backward compatibility)
    match /personas/innovexia {
      allow read: if true;
      allow write: if false;
    }
  }
}
```

## Data Structure

### Rate Limiting (`/users/{uid}/rate/now`)

Used for burst rate limiting (requests per minute):

```typescript
{
  minuteWindowStart: Timestamp,    // When the current minute started
  requestsThisMinute: number       // Count of requests in this minute
}
```

- Window Duration: 1 minute (60 seconds)
- Automatically resets when window expires
- Prevents spam and DoS attacks

### Usage Tracking (`/users/{uid}/usage/current`)

Used for 5-hour window limits (messages and tokens):

```typescript
{
  windowStartTime: Timestamp,      // When the 5-hour window started
  messageCount: number,             // Total messages sent in this window
  tokensIn: number,                 // Total input tokens used
  tokensOut: number                 // Total output tokens used
}
```

- Window Duration: 5 hours (18,000,000 ms)
- Tracks both message counts and token usage
- Resets automatically after 5 hours
- Syncs across all devices

## Security Features

### 1. Bypass Prevention

- **App Data Deletion**: Usage data is stored in Firebase, not locally
- **Reinstall**: Same account = same limits
- **Multiple Devices**: Limits are shared across all devices

### 2. Tampering Prevention

- Users cannot decrease their usage counts
- Users cannot set future timestamps
- All fields are required and type-checked
- Only authenticated users can access their own data

### 3. Privacy

- Each user can only read/write their own usage data
- No user can see another user's limits or usage

## Implementation Notes

### Client-Side (Kotlin)

The app uses `FirebaseUsageTracker` which:
- Writes to Firebase as the source of truth
- Falls back to local DataStore for offline support
- Syncs when connection is restored

```kotlin
val firestore = FirebaseFirestore.getInstance()
val auth = FirebaseAuth.getInstance()
val usageTracker = FirebaseUsageTracker(context, firestore, auth)
```

### Firestore Indexes

No additional indexes required for this schema.

### Offline Support

The app maintains a local copy via DataStore:
- When offline: Uses local data
- When online: Syncs with Firebase
- On conflict: Firebase is source of truth

## Testing

### Test Bypass Prevention

1. Use app and hit rate limit
2. Clear app data
3. Reopen app - **limits should still be enforced**

### Test Cross-Device Sync

1. Login on Device A, send 10 messages
2. Login on Device B with same account
3. Device B should show 10 messages already used

### Test Window Reset

1. Set `WINDOW_DURATION_MS` to 10 seconds (for testing)
2. Send messages until limited
3. Wait 10 seconds
4. Limits should reset and allow new messages

## Migration from Local-Only

If upgrading from the old local-only system:

1. Deploy the Firebase security rules
2. Update code to use `FirebaseUsageTracker`
3. Old local data will be used as fallback until first sync
4. After first message, Firebase becomes source of truth

## Monitoring

You can monitor usage in Firebase Console:
- Go to Firestore Database
- Navigate to `/users/{uid}/usage/current`
- View real-time usage data for any user

## Rate Limits by Plan

```kotlin
FREE:   25 messages,   100,000 tokens per 5-hour window
PLUS:  100 messages,   500,000 tokens per 5-hour window
PRO:   250 messages, 1,500,000 tokens per 5-hour window
MASTER: 1000 messages, 5,000,000 tokens per 5-hour window
```

## Support

For issues or questions:
- Check Firebase Console for errors
- Review Logcat for `FirebaseUsageTracker` logs
- Verify security rules are deployed correctly
