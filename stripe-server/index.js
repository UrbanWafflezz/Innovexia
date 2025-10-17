import 'dotenv/config';
import express from 'express';
import Stripe from 'stripe';
import cors from 'cors';
import bodyParser from 'body-parser';
import admin from 'firebase-admin';
import { readFileSync, existsSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const app = express();
app.use(cors({ origin: true }));

// Initialize Stripe
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY, { apiVersion: '2024-06-20' });

// Validate environment
const isProduction = process.env.NODE_ENV === 'production';
const isTestKey = process.env.STRIPE_SECRET_KEY?.startsWith('sk_test_');
const isLiveKey = process.env.STRIPE_SECRET_KEY?.startsWith('sk_live_');

console.log(`\n🚀 Starting Innovexia Stripe Server`);
console.log(`📍 Environment: ${isProduction ? 'PRODUCTION' : 'DEVELOPMENT'}`);
console.log(`🔑 Stripe Mode: ${isLiveKey ? 'LIVE' : 'TEST'}`);

if (isProduction && !isLiveKey) {
  console.warn('⚠️  WARNING: Running in production mode with TEST Stripe keys!');
}

// Initialize Firebase Admin (for Firestore)
try {
  // Try to initialize with environment variables first (for production/Render)
  if (process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_PRIVATE_KEY && process.env.FIREBASE_CLIENT_EMAIL) {
    admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      })
    });
    console.log('✅ Firebase Admin initialized with environment variables');
  }
  // Try to find service account file (for local development)
  else {
    const serviceAccountPaths = [
      join(__dirname, 'serviceAccountKey.json'),
      join(__dirname, '..', 'app', 'google-services.json'),
    ];

    let initialized = false;
    for (const path of serviceAccountPaths) {
      if (existsSync(path)) {
        try {
          const serviceAccount = JSON.parse(readFileSync(path, 'utf8'));
          admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
          });
          console.log(`✅ Firebase Admin initialized from: ${path}`);
          initialized = true;
          break;
        } catch (err) {
          // Try next path
        }
      }
    }

    if (!initialized) {
      console.warn('⚠️  Firebase Admin not initialized - running without Firestore persistence');
      console.warn('   Subscription data will be stored in memory only (not recommended for production)');
    }
  }
} catch (error) {
  console.warn('⚠️  Firebase Admin initialization failed:', error.message);
  console.warn('   Subscription data will be stored in memory only (not recommended for production)');
}

const db = admin.apps.length > 0 ? admin.firestore() : null;

// In-memory fallback store (used if Firestore is not available)
const users = new Map(); // uid -> { customerId, activeEntitlement }

// Map your app plan/period to Stripe price IDs
// TODO: Update these with your actual Stripe price IDs
// Run: npm run setup:test (for test mode) or npm run setup:prod (for production)
function planToPriceId(planId, period) {
  // Try to load from JSON config first
  const configFileName = isLiveKey ? 'price-ids.production.json' : 'price-ids.test.json';
  const configPath = join(__dirname, configFileName);

  if (existsSync(configPath)) {
    try {
      const config = JSON.parse(readFileSync(configPath, 'utf8'));
      return config.priceIds[`${planId}:${period}`];
    } catch (err) {
      console.warn('Failed to load price IDs from config file:', err.message);
    }
  }

  // Fallback to hardcoded IDs (test mode only)
  const map = {
    'PLUS:MONTHLY': 'price_1SG7plRutIy9oqiF45T5OrXR',
    'PLUS:YEARLY': 'price_1SG7plRutIy9oqiFvzawPEC4',
    'PRO:MONTHLY': 'price_1SG7plRutIy9oqiFj9mfAMoR',
    'PRO:YEARLY': 'price_1SG7pmRutIy9oqiFY7obhe66',
    'MASTER:MONTHLY': 'price_1SG7pmRutIy9oqiFuQKEXvVc',
    'MASTER:YEARLY': 'price_1SG7pmRutIy9oqiFAdfInzrR',
  };
  return map[`${planId}:${period}`];
}

// Helper: Get or create user record (with Firestore persistence)
async function getUserRecord(uid, email = null) {
  // Try Firestore first
  if (db) {
    try {
      const userRef = db.collection('users').doc(uid).collection('stripe').doc('customer');
      const doc = await userRef.get();

      if (doc.exists) {
        return doc.data();
      }

      // Create new customer if needed
      if (email) {
        const customer = await stripe.customers.create({
          email,
          metadata: { uid }
        });

        const record = {
          customerId: customer.id,
          activeEntitlement: null,
          createdAt: new Date().toISOString()
        };

        await userRef.set(record);
        return record;
      }

      return null;
    } catch (error) {
      console.error('Firestore error:', error);
      // Fall through to in-memory store
    }
  }

  // Fallback to in-memory store
  let rec = users.get(uid);
  if (!rec && email) {
    const customer = await stripe.customers.create({
      email,
      metadata: { uid }
    });
    rec = { customerId: customer.id, activeEntitlement: null };
    users.set(uid, rec);
  }
  return rec;
}

// Helper: Update user entitlement (with Firestore persistence)
async function updateUserEntitlement(uid, entitlement) {
  // Update Firestore
  if (db) {
    try {
      // Update stripe customer record
      await db.collection('users').doc(uid).collection('stripe').doc('customer')
        .set({ activeEntitlement: entitlement }, { merge: true });

      // Also update the main subscription document that the Android app uses
      await db.collection('users').doc(uid).collection('subscription').doc('current')
        .set({
          plan: entitlement.plan,
          status: entitlement.status,
          currentPeriodStart: new Date(entitlement.startedAt),
          currentPeriodEnd: entitlement.renewsAt ? new Date(entitlement.renewsAt) : null,
          stripeCustomerId: (await getUserRecord(uid))?.customerId,
          stripeSubscriptionId: entitlement.orderId,
          updatedAt: new Date()
        }, { merge: true });

      console.log(`✅ Updated Firestore for user ${uid}`);
    } catch (error) {
      console.error('Firestore update error:', error);
    }
  }

  // Also update in-memory store
  const rec = users.get(uid);
  if (rec) {
    rec.activeEntitlement = entitlement;
  }
}

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// 1) Bootstrap: Create or fetch Stripe Customer, Ephemeral Key, and SetupIntent
app.post('/billing/bootstrap', bodyParser.json(), async (req, res) => {
  try {
    const { uid, email } = req.body;
    if (!uid) {
      return res.status(400).json({ error: 'uid required' });
    }

    const rec = await getUserRecord(uid, email);
    if (!rec) {
      return res.status(500).json({ error: 'Failed to create customer' });
    }

    const ephemeralKey = await stripe.ephemeralKeys.create(
      { customer: rec.customerId },
      { apiVersion: '2024-06-20' }
    );

    // Use SetupIntent to collect a reusable payment method for subscriptions
    const setupIntent = await stripe.setupIntents.create({
      customer: rec.customerId,
      payment_method_types: ['card'],
    });

    res.json({
      customerId: rec.customerId,
      ephemeralKeySecret: ephemeralKey.secret,
      setupIntentClientSecret: setupIntent.client_secret,
      publishableKey: process.env.STRIPE_PUBLISHABLE_KEY
    });
  } catch (error) {
    console.error('Bootstrap error:', error);
    res.status(500).json({ error: error.message });
  }
});

// 2) Create subscription using the saved payment method from PaymentSheet
app.post('/billing/subscribe', bodyParser.json(), async (req, res) => {
  try {
    const { uid, planId, period } = req.body;
    if (!uid || !planId || !period) {
      return res.status(400).json({ error: 'uid, planId, period required' });
    }

    const rec = await getUserRecord(uid);
    if (!rec) {
      return res.status(404).json({ error: 'customer not found, call /billing/bootstrap first' });
    }

    const priceId = planToPriceId(planId, period);
    if (!priceId) {
      return res.status(400).json({ error: 'invalid plan/period' });
    }

    // Create subscription
    const sub = await stripe.subscriptions.create({
      customer: rec.customerId,
      items: [{ price: priceId }],
      payment_behavior: 'default_incomplete',
      expand: ['latest_invoice.payment_intent', 'pending_setup_intent']
    });

    // Build entitlement response
    const entitlement = {
      plan: planId,
      period,
      status: 'ACTIVE',
      startedAt: sub.created * 1000,
      renewsAt: sub.current_period_end * 1000,
      source: 'stripe',
      orderId: sub.id
    };

    await updateUserEntitlement(uid, entitlement);

    res.json({
      ok: true,
      subscriptionId: sub.id,
      entitlement
    });
  } catch (error) {
    console.error('Subscribe error:', error);
    res.status(500).json({ error: error.message });
  }
});

// 3) Cancel subscription at period end
app.post('/billing/cancel', bodyParser.json(), async (req, res) => {
  try {
    const { uid } = req.body;
    const rec = await getUserRecord(uid);
    if (!rec) {
      return res.json({ ok: true });
    }

    const subscriptions = await stripe.subscriptions.list({
      customer: rec.customerId,
      status: 'active',
      limit: 1
    });

    const sub = subscriptions.data[0];
    if (!sub) {
      return res.json({ ok: true });
    }

    const updated = await stripe.subscriptions.update(sub.id, {
      cancel_at_period_end: true
    });

    const entitlement = {
      ...rec.activeEntitlement,
      status: 'CANCELED',
      renewsAt: updated.current_period_end * 1000
    };

    await updateUserEntitlement(uid, entitlement);

    res.json({ ok: true, entitlement });
  } catch (error) {
    console.error('Cancel error:', error);
    res.status(500).json({ error: error.message });
  }
});

// 4) Resume a canceled subscription
app.post('/billing/resume', bodyParser.json(), async (req, res) => {
  try {
    const { uid } = req.body;
    const rec = await getUserRecord(uid);
    if (!rec) {
      return res.status(404).json({ error: 'customer not found' });
    }

    const subscriptions = await stripe.subscriptions.list({
      customer: rec.customerId,
      status: 'active',
      limit: 1
    });

    const sub = subscriptions.data[0];
    if (!sub || !sub.cancel_at_period_end) {
      return res.status(400).json({ error: 'No canceled subscription to resume' });
    }

    const updated = await stripe.subscriptions.update(sub.id, {
      cancel_at_period_end: false
    });

    const entitlement = {
      ...rec.activeEntitlement,
      status: 'ACTIVE',
      renewsAt: updated.current_period_end * 1000
    };

    await updateUserEntitlement(uid, entitlement);

    res.json({ ok: true, entitlement });
  } catch (error) {
    console.error('Resume error:', error);
    res.status(500).json({ error: error.message });
  }
});

// 5) Switch/Upgrade plan
app.post('/billing/switch', bodyParser.json(), async (req, res) => {
  try {
    const { uid, planId, period } = req.body;
    if (!uid || !planId || !period) {
      return res.status(400).json({ error: 'uid, planId, period required' });
    }

    const rec = await getUserRecord(uid);
    if (!rec) {
      return res.status(404).json({ error: 'customer not found' });
    }

    const newPriceId = planToPriceId(planId, period);
    if (!newPriceId) {
      return res.status(400).json({ error: 'invalid plan/period' });
    }

    // Get current active subscription
    const subscriptions = await stripe.subscriptions.list({
      customer: rec.customerId,
      status: 'active',
      limit: 1
    });

    const currentSub = subscriptions.data[0];

    if (!currentSub) {
      // No active subscription - create new one
      const sub = await stripe.subscriptions.create({
        customer: rec.customerId,
        items: [{ price: newPriceId }],
        payment_behavior: 'default_incomplete',
        expand: ['latest_invoice.payment_intent', 'pending_setup_intent']
      });

      const entitlement = {
        plan: planId,
        period,
        status: 'ACTIVE',
        startedAt: sub.created * 1000,
        renewsAt: sub.current_period_end * 1000,
        source: 'stripe',
        orderId: sub.id
      };

      await updateUserEntitlement(uid, entitlement);

      return res.json({
        ok: true,
        subscriptionId: sub.id,
        entitlement
      });
    }

    // Update existing subscription
    const updated = await stripe.subscriptions.update(currentSub.id, {
      items: [{
        id: currentSub.items.data[0].id,
        price: newPriceId,
      }],
      proration_behavior: 'always_invoice', // Prorate the difference
    });

    const entitlement = {
      plan: planId,
      period,
      status: updated.cancel_at_period_end ? 'CANCELED' : 'ACTIVE',
      startedAt: updated.created * 1000,
      renewsAt: updated.current_period_end * 1000,
      source: 'stripe',
      orderId: updated.id
    };

    await updateUserEntitlement(uid, entitlement);

    res.json({
      ok: true,
      subscriptionId: updated.id,
      entitlement
    });
  } catch (error) {
    console.error('Switch error:', error);
    res.status(500).json({ error: error.message });
  }
});

// 6) Restore purchases (fetch current subscription state)
app.post('/billing/restore', bodyParser.json(), async (req, res) => {
  try {
    const { uid } = req.body;
    const rec = await getUserRecord(uid);
    if (!rec) {
      return res.json({ entitlement: null });
    }

    const subscriptions = await stripe.subscriptions.list({
      customer: rec.customerId,
      status: 'active',
      limit: 1
    });

    const sub = subscriptions.data[0];
    if (!sub) {
      return res.json({ entitlement: null });
    }

    // Parse plan from subscription metadata or price
    const entitlement = {
      plan: rec.activeEntitlement?.plan || 'UNKNOWN',
      period: rec.activeEntitlement?.period || 'MONTHLY',
      status: sub.cancel_at_period_end ? 'CANCELED' : 'ACTIVE',
      startedAt: sub.created * 1000,
      renewsAt: sub.current_period_end * 1000,
      source: 'stripe',
      orderId: sub.id
    };

    await updateUserEntitlement(uid, entitlement);
    res.json({ entitlement });
  } catch (error) {
    console.error('Restore error:', error);
    res.status(500).json({ error: error.message });
  }
});

// 7) Webhook endpoint (for production - handle subscription lifecycle events)
app.post('/billing/webhook', bodyParser.raw({ type: 'application/json' }), async (req, res) => {
  const sig = req.headers['stripe-signature'];
  let event;

  try {
    event = stripe.webhooks.constructEvent(
      req.body,
      sig,
      process.env.STRIPE_WEBHOOK_SECRET
    );
  } catch (err) {
    console.error('⚠️  Webhook signature verification failed:', err.message);
    return res.status(400).send(`Webhook Error: ${err.message}`);
  }

  console.log(`📥 Webhook received: ${event.type}`);

  // Handle the event
  try {
    switch (event.type) {
      case 'customer.subscription.created':
      case 'customer.subscription.updated': {
        const subscription = event.data.object;
        const customerId = subscription.customer;

        // Find user by customer ID
        const customer = await stripe.customers.retrieve(customerId);
        const uid = customer.metadata?.uid;

        if (uid) {
          const entitlement = {
            plan: 'UNKNOWN', // Would need to map price ID back to plan
            period: subscription.items.data[0].price.recurring?.interval === 'year' ? 'YEARLY' : 'MONTHLY',
            status: subscription.cancel_at_period_end ? 'CANCELED' : subscription.status.toUpperCase(),
            startedAt: subscription.created * 1000,
            renewsAt: subscription.current_period_end * 1000,
            source: 'stripe',
            orderId: subscription.id
          };

          await updateUserEntitlement(uid, entitlement);
          console.log(`✅ Updated subscription for user ${uid}`);
        }
        break;
      }

      case 'customer.subscription.deleted': {
        const subscription = event.data.object;
        const customerId = subscription.customer;

        const customer = await stripe.customers.retrieve(customerId);
        const uid = customer.metadata?.uid;

        if (uid && db) {
          // Mark subscription as inactive
          await db.collection('users').doc(uid).collection('subscription').doc('current')
            .update({
              status: 'INACTIVE',
              updatedAt: new Date()
            });
          console.log(`✅ Marked subscription as deleted for user ${uid}`);
        }
        break;
      }

      case 'invoice.paid': {
        const invoice = event.data.object;
        console.log(`✅ Invoice paid: ${invoice.id} for customer ${invoice.customer}`);
        break;
      }

      case 'invoice.payment_failed': {
        const invoice = event.data.object;
        const customerId = invoice.customer;

        const customer = await stripe.customers.retrieve(customerId);
        const uid = customer.metadata?.uid;

        if (uid && db) {
          // Put subscription in grace period
          await db.collection('users').doc(uid).collection('subscription').doc('current')
            .update({
              status: 'PAST_DUE',
              updatedAt: new Date()
            });
          console.log(`⚠️  Payment failed for user ${uid}, set to PAST_DUE`);
        }
        break;
      }

      default:
        console.log(`ℹ️  Unhandled event type: ${event.type}`);
    }
  } catch (error) {
    console.error('❌ Error processing webhook:', error);
    return res.status(500).json({ error: 'Webhook processing failed' });
  }

  res.json({ received: true });
});

const PORT = process.env.PORT || 4242;
app.listen(PORT, () => {
  console.log(`🚀 Innovexia Stripe server running on port ${PORT}`);
  console.log(`📍 Base URL: ${process.env.BASE_URL || `http://localhost:${PORT}`}`);
  console.log(`🔑 Using publishable key: ${process.env.STRIPE_PUBLISHABLE_KEY?.substring(0, 20)}...`);
});
