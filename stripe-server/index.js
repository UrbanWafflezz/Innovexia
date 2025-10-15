import 'dotenv/config';
import express from 'express';
import Stripe from 'stripe';
import cors from 'cors';
import bodyParser from 'body-parser';

const app = express();
app.use(cors({ origin: true }));

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY, { apiVersion: '2024-06-20' });

// In-memory demo store. In production, persist to database keyed by your auth UID.
const users = new Map(); // uid -> { customerId, activeEntitlement }

// Map your app plan/period to Stripe price IDs
function planToPriceId(planId, period) {
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

    let rec = users.get(uid);
    if (!rec) {
      const customer = await stripe.customers.create({
        email,
        metadata: { uid }
      });
      rec = { customerId: customer.id, activeEntitlement: null };
      users.set(uid, rec);
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

    const rec = users.get(uid);
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
    rec.activeEntitlement = {
      plan: planId,
      period,
      status: 'ACTIVE',
      startedAt: sub.created * 1000,
      renewsAt: sub.current_period_end * 1000,
      source: 'stripe',
      orderId: sub.id
    };

    res.json({
      ok: true,
      subscriptionId: sub.id,
      entitlement: rec.activeEntitlement
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
    const rec = users.get(uid);
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

    rec.activeEntitlement = {
      ...rec.activeEntitlement,
      status: 'CANCELED',
      renewsAt: updated.current_period_end * 1000
    };

    res.json({ ok: true, entitlement: rec.activeEntitlement });
  } catch (error) {
    console.error('Cancel error:', error);
    res.status(500).json({ error: error.message });
  }
});

// 4) Resume a canceled subscription
app.post('/billing/resume', bodyParser.json(), async (req, res) => {
  try {
    const { uid } = req.body;
    const rec = users.get(uid);
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

    rec.activeEntitlement = {
      ...rec.activeEntitlement,
      status: 'ACTIVE',
      renewsAt: updated.current_period_end * 1000
    };

    res.json({ ok: true, entitlement: rec.activeEntitlement });
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

    const rec = users.get(uid);
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

      rec.activeEntitlement = {
        plan: planId,
        period,
        status: 'ACTIVE',
        startedAt: sub.created * 1000,
        renewsAt: sub.current_period_end * 1000,
        source: 'stripe',
        orderId: sub.id
      };

      return res.json({
        ok: true,
        subscriptionId: sub.id,
        entitlement: rec.activeEntitlement
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

    rec.activeEntitlement = {
      plan: planId,
      period,
      status: updated.cancel_at_period_end ? 'CANCELED' : 'ACTIVE',
      startedAt: updated.created * 1000,
      renewsAt: updated.current_period_end * 1000,
      source: 'stripe',
      orderId: updated.id
    };

    res.json({
      ok: true,
      subscriptionId: updated.id,
      entitlement: rec.activeEntitlement
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
    const rec = users.get(uid);
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

    rec.activeEntitlement = entitlement;
    res.json({ entitlement });
  } catch (error) {
    console.error('Restore error:', error);
    res.status(500).json({ error: error.message });
  }
});

// 6) Webhook endpoint (for production - handle subscription lifecycle events)
app.post('/billing/webhook', bodyParser.raw({ type: 'application/json' }), (req, res) => {
  const sig = req.headers['stripe-signature'];
  let event;

  try {
    event = stripe.webhooks.constructEvent(
      req.body,
      sig,
      process.env.STRIPE_WEBHOOK_SECRET
    );
  } catch (err) {
    console.error('Webhook signature verification failed:', err.message);
    return res.status(400).send(`Webhook Error: ${err.message}`);
  }

  // Handle the event
  switch (event.type) {
    case 'customer.subscription.created':
    case 'customer.subscription.updated':
      console.log('Subscription updated:', event.data.object.id);
      // TODO: Update users map based on customer ID
      break;
    case 'customer.subscription.deleted':
      console.log('Subscription deleted:', event.data.object.id);
      // TODO: Mark subscription as expired
      break;
    case 'invoice.paid':
      console.log('Invoice paid:', event.data.object.id);
      break;
    case 'invoice.payment_failed':
      console.log('Payment failed:', event.data.object.id);
      // TODO: Put subscription in grace period
      break;
    default:
      console.log(`Unhandled event type: ${event.type}`);
  }

  res.json({ received: true });
});

const PORT = process.env.PORT || 4242;
app.listen(PORT, () => {
  console.log(`🚀 Innovexia Stripe server running on port ${PORT}`);
  console.log(`📍 Base URL: ${process.env.BASE_URL || `http://localhost:${PORT}`}`);
  console.log(`🔑 Using publishable key: ${process.env.STRIPE_PUBLISHABLE_KEY?.substring(0, 20)}...`);
});
