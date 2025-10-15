# Firestore Setup Guide

## Overview

This guide covers setting up Firestore for Innovexia's subscription, usage, and billing system.

---

## 1. Firestore Indices

### ✅ **Good News: No Custom Indices Required!**

Your subscription/usage system uses **simple queries only**:

```kotlin
// All queries are simple document reads by ID
firestore.collection("users").document(uid)
    .collection("subscription").document("current").get()

firestore.collection("users").document(uid)
    .collection("usage").document(periodId).get()

firestore.collection("users").document(uid)
    .collection("rate").document("now").get()
```

**No compound queries** = **No indices needed** ✅

### When Would You Need Indices?

Only if you add queries like:
```kotlin
// ❌ This would require an index
firestore.collection("users")
    .where("plan", "==", "pro")
    .where("status", "==", "active")
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .get()
```

Firestore will tell you in the logs if an index is needed and provide a direct link to create it.

---

## 2. Deploy Security Rules

### Step 1: Copy the Rules

Use the rules from [firestore.rules.subscription](../firestore.rules.subscription):

```bash
cd "c:\Users\Kobes Work Account\Documents\Innovexia"

# Copy subscription rules to main firestore.rules file
# Or merge them into your existing rules
```

### Step 2: Deploy

```bash
firebase deploy --only firestore:rules
```

### Step 3: Verify

Go to Firebase Console → Firestore → Rules:
- ✅ Users can read their own `/users/{uid}/subscription`
- ✅ Users can read their own `/users/{uid}/usage/{period}`
- ✅ Users can read their own `/users/{uid}/rate/now`
- ✅ Only Cloud Functions can write (enforced by `allow write: if false`)

---

## 3. Initialize Default Subscriptions (Optional)

If you have existing users, create default subscriptions for them:

### Option A: Cloud Function (Recommended)

Create `functions/src/initSubscriptions.ts`:

```typescript
import * as admin from 'firebase-admin';
admin.initializeApp();

const db = admin.firestore();

export async function initializeDefaultSubscriptions() {
  const usersSnapshot = await db.collection('users').get();

  for (const userDoc of usersSnapshot.docs) {
    const uid = userDoc.id;

    const subRef = db.collection('users').doc(uid)
      .collection('subscription').doc('current');

    const subDoc = await subRef.get();

    if (!subDoc.exists) {
      const now = admin.firestore.Timestamp.now();
      const oneMonthLater = new admin.firestore.Timestamp(
        now.seconds + 30 * 24 * 60 * 60,
        now.nanoseconds
      );

      await subRef.set({
        plan: 'free',
        status: 'active',
        currentPeriodStart: now,
        currentPeriodEnd: oneMonthLater,
        cancelAtPeriodEnd: false,
        stripeCustomerId: null,
        stripeSubscriptionId: null,
        trialEnd: null
      });

      console.log(`✅ Created subscription for user ${uid}`);
    }
  }

  console.log('✅ All subscriptions initialized!');
}
```

Run once:
```bash
firebase functions:shell
> initializeDefaultSubscriptions()
```

### Option B: Client-Side (Auto-create on first load)

The `SubscriptionRepository` already handles this:

```kotlin
// In SubscriptionRepository.fetchFromFirestore()
val subscription = if (snapshot.exists()) {
    UserSubscription.fromMap(snapshot.data ?: emptyMap())
} else {
    // Create default subscription if doesn't exist
    val default = UserSubscription.default()
    docRef.set(default.toMap()).await()
    default
}
```

**No action needed** - subscriptions are auto-created when users first access the app! ✅

---

## 4. Firestore Database Structure

```
/users/{uid}
  ├─ profile: { ... }
  ├─ settings: { ... }
  │
  ├─ subscription/ (subcollection)
  │   └─ current (document)
  │       ├─ plan: "free"
  │       ├─ status: "active"
  │       ├─ currentPeriodStart: Timestamp
  │       ├─ currentPeriodEnd: Timestamp
  │       ├─ cancelAtPeriodEnd: false
  │       ├─ stripeCustomerId: null
  │       └─ stripeSubscriptionId: null
  │
  ├─ usage/ (subcollection)
  │   └─ {periodId} (e.g., "2025-10")
  │       ├─ tokensIn: 0
  │       ├─ tokensOut: 0
  │       ├─ requests: 0
  │       ├─ attachmentsBytes: 0
  │       └─ lastUpdated: Timestamp
  │
  └─ rate/ (subcollection)
      └─ now (document)
          ├─ minuteWindowStart: Timestamp
          └─ requestsThisMinute: 0
```

---

## 5. Test Data Creation (for Development)

### Create Test User with Pro Plan

```typescript
// In Firebase Console → Firestore
// Or via Cloud Functions

await db.collection('users').doc('test-user-123')
  .collection('subscription').doc('current').set({
    plan: 'pro',
    status: 'active',
    currentPeriodStart: Timestamp.now(),
    currentPeriodEnd: Timestamp.fromMillis(Date.now() + 30*24*60*60*1000),
    cancelAtPeriodEnd: false,
    stripeCustomerId: 'cus_test123',
    stripeSubscriptionId: 'sub_test123',
    trialEnd: null
  });
```

### Simulate Usage Data

```typescript
await db.collection('users').doc('test-user-123')
  .collection('usage').doc('2025-10').set({
    tokensIn: 5000000,
    tokensOut: 3000000,
    requests: 150,
    attachmentsBytes: 2048000,
    lastUpdated: Timestamp.now()
  });
```

---

## 6. Monitoring Queries

### View Active Subscriptions

Firebase Console → Firestore:
1. Click `users` collection
2. Pick any user
3. Expand `subscription` → `current`
4. See plan, status, period dates

### View Usage Stats

1. Expand `usage` → `2025-10` (or current month)
2. See tokensIn, tokensOut, requests

### Debug Rate Limiting

1. Expand `rate` → `now`
2. See minuteWindowStart, requestsThisMinute

---

## 7. Common Firestore Operations

### Read Subscription (Android)

```kotlin
val docRef = firestore.collection("users").document(uid)
    .collection("subscription").document("current")

val snapshot = docRef.get().await()
val subscription = UserSubscription.fromMap(snapshot.data ?: emptyMap())
```

### Update Usage (Server-Side Only)

```typescript
await db.collection('users').doc(uid)
  .collection('usage').doc(periodId).set({
    tokensIn: FieldValue.increment(1000),
    tokensOut: FieldValue.increment(500),
    requests: FieldValue.increment(1),
    lastUpdated: Timestamp.now()
  }, { merge: true });
```

### Check Rate Limit (Server-Side Only)

```typescript
const rateDoc = await db.collection('users').doc(uid)
  .collection('rate').doc('now').get();

const rateData = rateDoc.data();
const now = Timestamp.now().seconds;
const windowStart = rateData?.minuteWindowStart?.seconds || 0;

if ((now - windowStart) < 60 && rateData.requestsThisMinute >= burstLimit) {
  throw new Error('Rate limit exceeded');
}
```

---

## 8. Firestore Costs (Estimate)

### Free Tier Limits:
- ✅ 50,000 reads/day
- ✅ 20,000 writes/day
- ✅ 20,000 deletes/day
- ✅ 1 GiB storage

### Your Usage:
Per user per day:
- 1-2 subscription reads (cached locally)
- 1 usage read
- ~10-100 usage writes (depending on messages sent)

**1000 active users/day** = ~2000 reads + ~50,000 writes
- ✅ Easily within free tier!
- When you exceed: ~$0.06 per 100K reads, ~$0.18 per 100K writes

---

## 9. Backup Strategy

### Enable Point-in-Time Recovery (PITR)

1. Firebase Console → Firestore → Settings
2. Enable "Point-in-time recovery"
3. Set retention period: 7 days

### Export Data (Manual Backup)

```bash
gcloud firestore export gs://YOUR-BUCKET/backups/$(date +%Y%m%d)
```

---

## 10. Troubleshooting

### Issue: "Permission denied" when reading subscription

**Fix:** Ensure security rules are deployed:
```bash
firebase deploy --only firestore:rules
```

### Issue: "Missing or insufficient permissions" when writing

**Expected!** Only Cloud Functions should write to subscription/usage.

Clients should only read. If you need to test writes:
```bash
# In Firebase Console → Firestore → Rules (temporarily)
allow write: if request.auth.uid == uid; // ONLY FOR TESTING
```

**⚠️ Remove this after testing!**

### Issue: Data not syncing

1. Check internet connection
2. Verify Firebase is initialized in app
3. Check Logcat for Firestore errors
4. Ensure user is authenticated

---

## Summary

✅ **No indices needed** - all queries are simple document reads
✅ **Security rules** - Deploy [firestore.rules.subscription](../firestore.rules.subscription)
✅ **Auto-initialization** - Subscriptions auto-created on first access
✅ **Cost-effective** - Well within free tier for most apps

**Next Steps:**
1. Deploy security rules: `firebase deploy --only firestore:rules`
2. Test subscription read in app
3. Monitor in Firebase Console
4. Deploy Cloud Functions for write operations

That's it! 🎉
