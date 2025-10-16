# Subscription, Usage & Billing Setup Guide

This guide walks you through setting up the complete subscription, usage tracking, and billing system for Innovexia.

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Firestore Setup](#firestore-setup)
4. [Stripe Setup](#stripe-setup)
5. [Cloud Functions Setup](#cloud-functions-setup)
6. [Testing](#testing)
7. [Going Live](#going-live)

---

## Overview

The subscription system consists of:

- **4 Tiers**: Free, Core ($18/mo), Pro ($28/mo), Team ($42/mo per seat)
- **Usage Tracking**: Monthly and daily token limits, burst rate limiting
- **Billing**: Stripe integration for payments, invoices, and customer portal
- **Real-time Sync**: Firestore for subscription and usage data
- **Local Caching**: Room database for offline access

---

## Prerequisites

- Firebase project with Firestore and Authentication enabled
- Stripe account (test mode for development)
- Node.js 18+ and Firebase CLI installed
- Android app with Firebase configured

---

## Firestore Setup

### 1. Firestore Data Structure

The following collections and documents will be created:

```
/users/{uid}
  ├─ profile: {...}
  ├─ settings: {...}
  └─ subscription/ (subcollection)
      └─ current (document)
          ├─ plan: "free" | "core" | "pro" | "team"
          ├─ status: "active" | "trialing" | "past_due" | "canceled"
          ├─ currentPeriodStart: timestamp
          ├─ currentPeriodEnd: timestamp
          ├─ cancelAtPeriodEnd: boolean
          ├─ stripeCustomerId: string?
          ├─ stripeSubscriptionId: string?
          └─ trialEnd: timestamp?

  └─ usage/ (subcollection)
      └─ {periodId} (document, e.g., "2025-10")
          ├─ tokensIn: number
          ├─ tokensOut: number
          ├─ requests: number
          ├─ attachmentsBytes: number
          └─ lastUpdated: timestamp

  └─ rate/ (subcollection)
      └─ now (document)
          ├─ minuteWindowStart: timestamp
          └─ requestsThisMinute: number
```

### 2. Initialize Default Subscriptions

For existing users, you can run a migration script to create default free subscriptions:

```javascript
// migration.js
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

async function migrateUsers() {
  const usersSnapshot = await db.collection('users').get();

  for (const userDoc of usersSnapshot.docs) {
    const uid = userDoc.id;
    const subscriptionRef = db.collection('users').doc(uid)
      .collection('subscription').doc('current');

    const subscriptionDoc = await subscriptionRef.get();

    if (!subscriptionDoc.exists) {
      const now = admin.firestore.Timestamp.now();
      const oneMonthLater = new admin.firestore.Timestamp(
        now.seconds + 30 * 24 * 60 * 60,
        now.nanoseconds
      );

      await subscriptionRef.set({
        plan: 'free',
        status: 'active',
        currentPeriodStart: now,
        currentPeriodEnd: oneMonthLater,
        cancelAtPeriodEnd: false,
        stripeCustomerId: null,
        stripeSubscriptionId: null,
        trialEnd: null
      });

      console.log(`Created default subscription for user ${uid}`);
    }
  }

  console.log('Migration complete!');
}

migrateUsers().catch(console.error);
```

Run with: `node migration.js`

---

## Stripe Setup

### 1. Create Stripe Account

1. Sign up at [stripe.com](https://stripe.com)
2. Get your **Test API Keys** from Dashboard → Developers → API keys
3. Note down:
   - Publishable key (starts with `pk_test_`)
   - Secret key (starts with `sk_test_`)
   - Webhook signing secret (created later)

### 2. Create Products and Prices

In Stripe Dashboard → Products:

1. **Free Plan** (no product needed)

2. **Core Plan**
   - Name: "Innovexia Core"
   - Price: $18.00 USD / month recurring
   - Copy the Price ID (e.g., `price_1xxxCore`)

3. **Pro Plan**
   - Name: "Innovexia Pro"
   - Price: $28.00 USD / month recurring
   - Copy the Price ID (e.g., `price_1xxxPro`)

4. **Team Plan**
   - Name: "Innovexia Team"
   - Price: $42.00 USD / month recurring per seat
   - Copy the Price ID (e.g., `price_1xxxTeam`)

### 3. Configure Webhook

In Stripe Dashboard → Developers → Webhooks:

1. Click "Add endpoint"
2. Endpoint URL: `https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/stripeWebhook`
3. Select events:
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`
4. Copy the **Signing secret** (starts with `whsec_`)

---

## Cloud Functions Setup

### 1. Initialize Firebase Functions

```bash
cd Innovexia/
firebase init functions
# Select TypeScript
# Install dependencies: Yes
```

### 2. Install Dependencies

```bash
cd functions
npm install stripe @google-cloud/firestore express cors
npm install --save-dev @types/express @types/cors
```

### 3. Set Environment Variables

```bash
firebase functions:config:set \
  stripe.secret_key="sk_test_YOUR_SECRET_KEY" \
  stripe.webhook_secret="whsec_YOUR_WEBHOOK_SECRET" \
  stripe.price_ids.core="price_1xxxCore" \
  stripe.price_ids.pro="price_1xxxPro" \
  stripe.price_ids.team="price_1xxxTeam"
```

### 4. Create Cloud Functions

Create `functions/src/index.ts`:

```typescript
import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import Stripe from 'stripe';
import * as express from 'express';
import * as cors from 'cors';

admin.initializeApp();
const db = admin.firestore();

// Initialize Stripe
const stripeSecretKey = functions.config().stripe.secret_key;
const stripe = new Stripe(stripeSecretKey, {
  apiVersion: '2023-10-16',
});

// Plan limits configuration
const LIMITS = {
  free:  { monthly: 1_000_000,  daily: 100_000,  burst: 10 },
  core:  { monthly:10_000_000, daily:1_500_000, burst: 30 },
  pro:   { monthly:25_000_000, daily:4_000_000, burst: 60 },
  team:  { monthly:60_000_000, daily:8_000_000, burst: 90 },
};

// ==================== Generate API ====================

export const generate = functions.https.onCall(async (data, context) => {
  // 1. Verify authentication
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const uid = context.auth.uid;
  const { prompt, attachments = [], estimatedTokens = 0 } = data;

  try {
    // 2. Load subscription
    const subscriptionDoc = await db.collection('users').doc(uid)
      .collection('subscription').doc('current').get();

    const subscription = subscriptionDoc.data() || { plan: 'free', status: 'active' };
    const planLimits = LIMITS[subscription.plan as keyof typeof LIMITS] || LIMITS.free;

    // 3. Check burst rate limit
    const rateDoc = await db.collection('users').doc(uid)
      .collection('rate').doc('now').get();

    const rateData = rateDoc.data();
    const now = admin.firestore.Timestamp.now();

    if (rateData) {
      const windowStart = rateData.minuteWindowStart.seconds;
      const inSameWindow = (now.seconds - windowStart) < 60;

      if (inSameWindow && rateData.requestsThisMinute >= planLimits.burst) {
        throw new functions.https.HttpsError('resource-exhausted', 'Burst rate limit exceeded. Try again in a minute.');
      }

      // Update rate limit
      if (inSameWindow) {
        await db.collection('users').doc(uid).collection('rate').doc('now').update({
          requestsThisMinute: admin.firestore.FieldValue.increment(1),
        });
      } else {
        await db.collection('users').doc(uid).collection('rate').doc('now').set({
          minuteWindowStart: now,
          requestsThisMinute: 1,
        });
      }
    } else {
      await db.collection('users').doc(uid).collection('rate').doc('now').set({
        minuteWindowStart: now,
        requestsThisMinute: 1,
      });
    }

    // 4. Check monthly quota
    const periodId = `${now.toDate().getFullYear()}-${String(now.toDate().getMonth() + 1).padStart(2, '0')}`;
    const usageDoc = await db.collection('users').doc(uid)
      .collection('usage').doc(periodId).get();

    const usage = usageDoc.data() || { tokensIn: 0, tokensOut: 0, requests: 0, attachmentsBytes: 0 };
    const totalTokens = usage.tokensIn + usage.tokensOut;

    if (totalTokens + estimatedTokens > planLimits.monthly) {
      throw new functions.https.HttpsError('resource-exhausted', 'Monthly quota exceeded. Upgrade to continue.');
    }

    // 5. Call AI model (TODO: Replace with your actual AI service)
    // For now, return a mock response
    const mockResponse = {
      text: `Mock response to: ${prompt}`,
      tokensIn: Math.floor(prompt.length / 4),
      tokensOut: 100,
    };

    // 6. Update usage
    const attachmentBytes = attachments.reduce((sum: number, att: any) => sum + (att.size || 0), 0);

    await db.collection('users').doc(uid).collection('usage').doc(periodId).set({
      tokensIn: admin.firestore.FieldValue.increment(mockResponse.tokensIn),
      tokensOut: admin.firestore.FieldValue.increment(mockResponse.tokensOut),
      requests: admin.firestore.FieldValue.increment(1),
      attachmentsBytes: admin.firestore.FieldValue.increment(attachmentBytes),
      lastUpdated: now,
    }, { merge: true });

    // 7. Return response with usage snapshot
    const updatedUsage = await db.collection('users').doc(uid)
      .collection('usage').doc(periodId).get();
    const updatedData = updatedUsage.data() || {};

    return {
      text: mockResponse.text,
      usage: {
        monthId: periodId,
        tokensIn: updatedData.tokensIn || 0,
        tokensOut: updatedData.tokensOut || 0,
        requests: updatedData.requests || 0,
        attachmentsBytes: updatedData.attachmentsBytes || 0,
        minuteCount: rateData?.requestsThisMinute || 1,
        burstLimit: planLimits.burst,
        monthlyLimit: planLimits.monthly,
        dailyLimit: planLimits.daily,
        periodEnd: subscription.currentPeriodEnd?.seconds || 0,
      },
    };
  } catch (error: any) {
    console.error('Generate error:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

// ==================== Stripe Checkout ====================

export const createCheckoutSession = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const { plan } = data; // 'core', 'pro', or 'team'
  const uid = context.auth.uid;
  const email = context.auth.token.email;

  const priceIds = functions.config().stripe.price_ids;
  const priceId = priceIds[plan];

  if (!priceId) {
    throw new functions.https.HttpsError('invalid-argument', 'Invalid plan');
  }

  try {
    // Get or create Stripe customer
    const userDoc = await db.collection('users').doc(uid)
      .collection('subscription').doc('current').get();

    let customerId = userDoc.data()?.stripeCustomerId;

    if (!customerId) {
      const customer = await stripe.customers.create({
        email,
        metadata: { firebaseUID: uid },
      });
      customerId = customer.id;

      await db.collection('users').doc(uid)
        .collection('subscription').doc('current').set({
          stripeCustomerId: customerId,
        }, { merge: true });
    }

    // Create checkout session
    const session = await stripe.checkout.sessions.create({
      customer: customerId,
      payment_method_types: ['card'],
      line_items: [{
        price: priceId,
        quantity: 1,
      }],
      mode: 'subscription',
      success_url: 'innovexia://subscription-success',
      cancel_url: 'innovexia://subscription-cancel',
      metadata: { firebaseUID: uid, plan },
    });

    return { url: session.url };
  } catch (error: any) {
    console.error('Checkout error:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

// ==================== Customer Portal ====================

export const createPortalSession = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const uid = context.auth.uid;

  try {
    const userDoc = await db.collection('users').doc(uid)
      .collection('subscription').doc('current').get();

    const customerId = userDoc.data()?.stripeCustomerId;

    if (!customerId) {
      throw new functions.https.HttpsError('failed-precondition', 'No Stripe customer found');
    }

    const session = await stripe.billingPortal.sessions.create({
      customer: customerId,
      return_url: 'innovexia://billing',
    });

    return { url: session.url };
  } catch (error: any) {
    console.error('Portal error:', error);
    throw new functions.https.HttpsError('internal', error.message);
  }
});

// ==================== Stripe Webhooks ====================

const webhookApp = express();
webhookApp.use(cors({ origin: true }));

webhookApp.post('/webhook', express.raw({ type: 'application/json' }), async (req, res) => {
  const sig = req.headers['stripe-signature'] as string;
  const webhookSecret = functions.config().stripe.webhook_secret;

  let event: Stripe.Event;

  try {
    event = stripe.webhooks.constructEvent(req.body, sig, webhookSecret);
  } catch (err: any) {
    console.error('Webhook signature verification failed:', err.message);
    res.status(400).send(`Webhook Error: ${err.message}`);
    return;
  }

  try {
    switch (event.type) {
      case 'customer.subscription.created':
      case 'customer.subscription.updated': {
        const subscription = event.data.object as Stripe.Subscription;
        const uid = subscription.metadata.firebaseUID;

        if (!uid) break;

        const planMap: Record<string, string> = {
          [functions.config().stripe.price_ids.core]: 'core',
          [functions.config().stripe.price_ids.pro]: 'pro',
          [functions.config().stripe.price_ids.team]: 'team',
        };

        const priceId = subscription.items.data[0]?.price.id;
        const plan = planMap[priceId] || 'free';

        await db.collection('users').doc(uid).collection('subscription').doc('current').set({
          plan,
          status: subscription.status,
          currentPeriodStart: admin.firestore.Timestamp.fromDate(new Date(subscription.current_period_start * 1000)),
          currentPeriodEnd: admin.firestore.Timestamp.fromDate(new Date(subscription.current_period_end * 1000)),
          cancelAtPeriodEnd: subscription.cancel_at_period_end,
          stripeSubscriptionId: subscription.id,
        }, { merge: true });

        break;
      }

      case 'customer.subscription.deleted': {
        const subscription = event.data.object as Stripe.Subscription;
        const uid = subscription.metadata.firebaseUID;

        if (!uid) break;

        await db.collection('users').doc(uid).collection('subscription').doc('current').set({
          plan: 'free',
          status: 'canceled',
        }, { merge: true });

        break;
      }

      case 'invoice.payment_failed': {
        const invoice = event.data.object as Stripe.Invoice;
        const subscription = await stripe.subscriptions.retrieve(invoice.subscription as string);
        const uid = subscription.metadata.firebaseUID;

        if (!uid) break;

        await db.collection('users').doc(uid).collection('subscription').doc('current').update({
          status: 'past_due',
        });

        break;
      }
    }

    res.json({ received: true });
  } catch (error: any) {
    console.error('Webhook handler error:', error);
    res.status(500).send('Internal error');
  }
});

export const stripeWebhook = functions.https.onRequest(webhookApp);
```

### 5. Deploy Functions

```bash
firebase deploy --only functions
```

---

## Firestore Security Rules

Add to your `firestore.rules`:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // User subscriptions
    match /users/{uid}/subscription/{doc} {
      allow read: if request.auth.uid == uid;
      allow write: if false; // Only Cloud Functions can write
    }

    // User usage
    match /users/{uid}/usage/{period} {
      allow read: if request.auth.uid == uid;
      allow write: if false; // Only Cloud Functions can write
    }

    // User rate limits
    match /users/{uid}/rate/{doc} {
      allow read: if request.auth.uid == uid;
      allow write: if false; // Only Cloud Functions can write
    }
  }
}
```

Deploy rules:

```bash
firebase deploy --only firestore:rules
```

---

## Testing

### 1. Test in Emulator (Optional)

```bash
firebase emulators:start
```

### 2. Test Subscription Flow

1. Sign in to the app
2. Go to Profile → Billing tab
3. Select a plan (Core/Pro/Team)
4. Complete test payment with Stripe test card: `4242 4242 4242 4242`
5. Verify subscription updates in Profile → Usage tab

### 3. Test Usage Tracking

1. Send a message in the app
2. Check Profile → Usage tab
3. Verify tokens increase

### 4. Test Webhooks

Use Stripe CLI:

```bash
stripe listen --forward-to https://YOUR_REGION-YOUR_PROJECT.cloudfunctions.net/stripeWebhook
stripe trigger customer.subscription.updated
```

---

## Going Live

### 1. Switch to Production Stripe Keys

```bash
firebase functions:config:set \
  stripe.secret_key="sk_live_YOUR_LIVE_KEY" \
  stripe.webhook_secret="whsec_YOUR_LIVE_SECRET"
```

### 2. Update Webhook Endpoint

In Stripe Dashboard, update webhook URL to production Cloud Function URL.

### 3. Redeploy Functions

```bash
firebase deploy --only functions
```

---

## Troubleshooting

### Subscription not updating

- Check Cloud Function logs: `firebase functions:log`
- Verify webhook is receiving events in Stripe Dashboard
- Ensure firebaseUID metadata is set in Stripe subscription

### Usage not incrementing

- Verify `generate` function is being called
- Check Firestore rules allow reads
- Ensure user is authenticated

### Checkout not working

- Verify Stripe Price IDs are correct in Firebase config
- Check success/cancel URLs match your app's deep link scheme
- Test with Stripe test cards in test mode

---

## Support

For issues or questions:
- Check Firebase Console logs
- Review Stripe Dashboard event history
- Test with Firebase Emulator Suite

---

**Next Steps**:
1. Complete Stripe setup
2. Deploy Cloud Functions
3. Test subscription flow
4. Enable production mode
