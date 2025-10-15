# Firestore Composite Indexes

This document describes the Firestore composite indexes required for cloud sync to work efficiently.

## Required Indexes

### 1. Chats Collection - Sort by lastMsgAt

**Collection**: `/users/{uid}/chats`
**Fields**:
- `lastMsgAt` (Descending)

**Purpose**: Efficiently query chats ordered by most recent message.

**To create**:
1. Go to Firebase Console → Firestore Database → Indexes
2. Click "Create Index"
3. Collection: `chats`
4. Fields to index:
   - Field: `lastMsgAt`, Order: Descending
5. Click "Create"

Alternatively, run a query that requires this index in your app and Firebase will provide a link to auto-generate it.

---

### 2. Chats Collection - Sort by updatedAt (backup)

**Collection**: `/users/{uid}/chats`
**Fields**:
- `updatedAt` (Descending)

**Purpose**: Alternative sorting by chat update time.

---

### 3. Messages Collection - Sort by createdAt per Chat

**Collection**: `/users/{uid}/chats/{chatId}/messages`
**Fields**:
- `chatId` (Ascending)
- `createdAt` (Ascending)

**Purpose**: Retrieve messages for a specific chat in chronological order.

**Note**: This might be auto-created when you first query messages within a chat subcollection.

---

### 4. Messages Collection - Filter by role and sort by createdAt (optional)

**Collection**: `/users/{uid}/chats/{chatId}/messages`
**Fields**:
- `role` (Ascending)
- `createdAt` (Ascending)

**Purpose**: Query messages by role (user/model/system) in chronological order.

Only create this if you need to filter messages by role.

---

## Creating Indexes via Firebase CLI

You can also define indexes in a `firestore.indexes.json` file and deploy with Firebase CLI:

```json
{
  "indexes": [
    {
      "collectionGroup": "chats",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "lastMsgAt",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "chats",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "updatedAt",
          "order": "DESCENDING"
        }
      ]
    },
    {
      "collectionGroup": "messages",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "chatId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "ASCENDING"
        }
      ]
    }
  ],
  "fieldOverrides": []
}
```

Deploy with:
```bash
firebase deploy --only firestore:indexes
```

---

## Performance Notes

- **Pagination**: Always use `.limit()` and `.startAfter()` for paginated queries
- **Cache**: Firestore SDK caches data locally by default, reducing reads
- **Avoid N+1**: Batch reads when fetching multiple chats or messages
- **Monitor Usage**: Check Firebase Console → Usage to track reads/writes and optimize

---

## Testing Indexes

After creating indexes, test with:
1. Enable cloud sync in the app
2. Upload some chats
3. Check Firestore Console → Data to verify documents are created
4. Check Firestore Console → Indexes to verify indexes are "Enabled"
5. Perform queries and check that they don't trigger missing index errors
