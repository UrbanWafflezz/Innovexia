# Persona 2.0 Cloud Functions

This document describes the Cloud Functions needed for the Persona 2.0 publish/unpublish feature.

## Prerequisites

- Firebase Cloud Functions setup
- Firebase Admin SDK
- Authentication required for all endpoints

## Functions

### 1. publishPersona

**Endpoint:** `POST /personas/publish`

**Authentication:** Required (Firebase Auth)

**Request Body:**
```json
{
  "personaId": "string"
}
```

**Function:**
```javascript
const functions = require('firebase-functions');
const admin = require('firebase-admin');

exports.publishPersona = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }

  const uid = context.auth.uid;
  const { personaId } = data;

  if (!personaId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'personaId is required'
    );
  }

  const db = admin.firestore();

  try {
    // 1. Get the persona from user's collection
    const personaRef = db
      .collection('users')
      .doc(uid)
      .collection('personas')
      .doc(personaId);

    const personaDoc = await personaRef.get();

    if (!personaDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Persona not found'
      );
    }

    const personaData = personaDoc.data();

    // 2. Validate persona data
    if (!personaData.name || personaData.name.length < 2) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Persona must have a valid name'
      );
    }

    // 3. Strip private/sensitive fields
    const publicData = {
      ...personaData,
      status: 'published',
      visibility: 'public',
      publishedAt: admin.firestore.FieldValue.serverTimestamp(),
      publicMeta: {
        authorId: uid,
        authorName: context.auth.token.name || 'Anonymous',
        likes: 0,
        downloads: 0,
        reports: 0
      }
    };

    // Remove sensitive fields if any
    delete publicData.privateNotes;

    // 4. Write to public collection
    await db
      .collection('public')
      .doc('personas')
      .collection('personas')
      .doc(personaId)
      .set(publicData);

    // 5. Update user's persona with published status
    await personaRef.update({
      status: 'published',
      visibility: 'public',
      publishedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return {
      success: true,
      message: 'Persona published successfully',
      personaId
    };
  } catch (error) {
    console.error('Error publishing persona:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to publish persona'
    );
  }
});
```

### 2. unpublishPersona

**Endpoint:** `POST /personas/unpublish`

**Authentication:** Required (Firebase Auth)

**Request Body:**
```json
{
  "personaId": "string"
}
```

**Function:**
```javascript
exports.unpublishPersona = functions.https.onCall(async (data, context) => {
  // Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }

  const uid = context.auth.uid;
  const { personaId } = data;

  if (!personaId) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'personaId is required'
    );
  }

  const db = admin.firestore();

  try {
    // 1. Verify ownership by checking user's persona
    const personaRef = db
      .collection('users')
      .doc(uid)
      .collection('personas')
      .doc(personaId);

    const personaDoc = await personaRef.get();

    if (!personaDoc.exists) {
      throw new functions.https.HttpsError(
        'not-found',
        'Persona not found'
      );
    }

    // 2. Delete from public collection
    await db
      .collection('public')
      .doc('personas')
      .collection('personas')
      .doc(personaId)
      .delete();

    // 3. Update user's persona to draft/private
    await personaRef.update({
      status: 'draft',
      visibility: 'private',
      unpublishedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return {
      success: true,
      message: 'Persona unpublished successfully',
      personaId
    };
  } catch (error) {
    console.error('Error unpublishing persona:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to unpublish persona'
    );
  }
});
```

### 3. likePersona (Optional - Future Feature)

**Endpoint:** `POST /personas/like`

**Authentication:** Required

**Request Body:**
```json
{
  "personaId": "string"
}
```

**Function:**
```javascript
exports.likePersona = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const uid = context.auth.uid;
  const { personaId } = data;

  const db = admin.firestore();
  const publicPersonaRef = db
    .collection('public')
    .doc('personas')
    .collection('personas')
    .doc(personaId);

  try {
    await db.runTransaction(async (transaction) => {
      const doc = await transaction.get(publicPersonaRef);
      if (!doc.exists) {
        throw new Error('Persona not found');
      }

      const currentLikes = doc.data().publicMeta?.likes || 0;
      transaction.update(publicPersonaRef, {
        'publicMeta.likes': currentLikes + 1
      });
    });

    return { success: true };
  } catch (error) {
    throw new functions.https.HttpsError('internal', error.message);
  }
});
```

## Deployment

```bash
# Initialize Firebase Functions (if not already done)
firebase init functions

# Deploy functions
firebase deploy --only functions

# Or deploy specific functions
firebase deploy --only functions:publishPersona,functions:unpublishPersona
```

## Security Considerations

1. **Authentication:** All functions require Firebase Auth
2. **Ownership Verification:** Functions verify that the user owns the persona
3. **Data Validation:** Input data is validated before processing
4. **Rate Limiting:** Consider adding rate limiting in production
5. **Moderation:** Consider adding a moderation queue for published personas
6. **Content Filtering:** Add checks for inappropriate content before publishing

## Testing

```javascript
// Test publish
const result = await firebase.functions().httpsCallable('publishPersona')({
  personaId: 'test-persona-id'
});

// Test unpublish
const result = await firebase.functions().httpsCallable('unpublishPersona')({
  personaId: 'test-persona-id'
});
```

## Integration with Android App

In your Kotlin code:

```kotlin
// In CreatePersonaViewModel or a repository class
private val functions = Firebase.functions

suspend fun publishPersona(personaId: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val data = hashMapOf("personaId" to personaId)
        val result = functions
            .getHttpsCallable("publishPersona")
            .call(data)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun unpublishPersona(personaId: String): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val data = hashMapOf("personaId" to personaId)
        val result = functions
            .getHttpsCallable("unpublishPersona")
            .call(data)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## Future Enhancements

1. **Moderation Queue:** Add pending status until admin approves
2. **Categories:** Add persona categories/tags for discovery
3. **Search:** Implement full-text search for public personas
4. **Analytics:** Track usage statistics for published personas
5. **Versioning:** Support multiple versions of published personas
6. **Comments/Reviews:** Allow users to review public personas
