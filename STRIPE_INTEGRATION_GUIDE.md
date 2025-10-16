# Stripe Test Subscriptions Integration Guide

This guide explains how to set up and test Stripe subscriptions in Innovexia.

## 🏗️ Architecture Overview

### Components

1. **Backend (Node.js + Express)**
   - Location: `stripe-server/`
   - Handles Stripe API calls with secret key
   - Manages customer creation, subscriptions, and webhooks
   - Test mode only (uses `sk_test_...` keys)

2. **Android App**
   - Stripe Android SDK for PaymentSheet
   - `StripeBillingProvider` implements `BillingProvider` interface
   - Retrofit API client for backend communication
   - Toggle between Mock and Stripe billing

### Data Flow

```
1. User selects plan in SubscriptionsScreen
2. App calls backend /billing/bootstrap (creates Stripe customer)
3. Backend returns PaymentSheet config
4. App presents PaymentSheet (user adds card)
5. On success, app calls /billing/subscribe
6. Backend creates subscription in Stripe
7. Backend returns entitlement to app
8. App saves entitlement locally
```

## 🚀 Setup Instructions

### Step 1: Configure Stripe Dashboard

1. Go to [Stripe Dashboard → Products](https://dashboard.stripe.com/test/products)

2. Create 3 products with recurring prices:

   **Plus Plan**
   - Name: "Innovexia Plus"
   - Monthly price: $9.99 → Note the price ID (e.g., `price_1ABC123...`)
   - Yearly price: $99.99 → Note the price ID (e.g., `price_1ABC456...`)

   **Pro Plan**
   - Name: "Innovexia Pro"
   - Monthly price: $19.99 → Note the price ID
   - Yearly price: $199.99 → Note the price ID

   **Master Plan**
   - Name: "Innovexia Master"
   - Monthly price: $39.99 → Note the price ID
   - Yearly price: $399.99 → Note the price ID

3. Update `stripe-server/index.js` with your actual price IDs:

```javascript
function planToPriceId(planId, period) {
  const map = {
    'PLUS:MONTHLY': 'price_1ABC123...',     // Replace with your Plus monthly price ID
    'PLUS:YEARLY': 'price_1ABC456...',      // Replace with your Plus yearly price ID
    'PRO:MONTHLY': 'price_1DEF123...',      // Replace with your Pro monthly price ID
    'PRO:YEARLY': 'price_1DEF456...',       // Replace with your Pro yearly price ID
    'MASTER:MONTHLY': 'price_1GHI123...',   // Replace with your Master monthly price ID
    'MASTER:YEARLY': 'price_1GHI456...',    // Replace with your Master yearly price ID
  };
  return map[\`\${planId}:\${period}\`];
}
```

### Step 2: Start the Backend Server

```bash
cd stripe-server
npm install
npm start
```

Server will run on `http://localhost:4242`
- For Android emulator: Use `http://10.0.2.2:4242`
- For physical device: Use your computer's local IP (e.g., `http://192.168.1.100:4242`)

### Step 3: Configure Android App

1. **Enable Stripe billing** in `InnovexiaApplication.kt`:

```kotlin
companion object {
    const val USE_STRIPE = true  // Change to true
}
```

2. **Update base URL** (if not using emulator) in `BillingRetrofitClient.kt`:

```kotlin
// For physical device on same network
private const val BASE_URL = "http://192.168.1.100:4242/"
```

3. **Sync Gradle** to download Stripe SDK

### Step 4: Initialize StripeBillingProvider in Activity

The StripeBillingProvider needs to be initialized with an Activity for PaymentSheet. You'll need to modify your SubscriptionsScreen or ViewModel to handle this.

**Option A: Update SubscriptionsScreen to handle PaymentSheet**

Add this to your SubscriptionsScreen composable:

```kotlin
val activity = LocalContext.current as? ComponentActivity
val app = LocalContext.current.applicationContext as InnovexiaApplication

LaunchedEffect(Unit) {
    if (activity != null && app.USE_STRIPE) {
        val provider = app.stripeBillingProvider
        provider.init(activity)

        // Bootstrap Stripe customer
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val email = FirebaseAuth.getInstance().currentUser?.email ?: "test@example.com"
        provider.bootstrap(uid, email)
    }
}

// Listen for PaymentSheet state
LaunchedEffect(Unit) {
    if (app.USE_STRIPE) {
        app.stripeBillingProvider.paymentSheetState.collect { state ->
            when (state) {
                is StripeBillingProvider.PaymentSheetState.Success -> {
                    // Payment method added successfully
                    // Now call purchase()
                }
                is StripeBillingProvider.PaymentSheetState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                }
                is StripeBillingProvider.PaymentSheetState.Canceled -> {
                    Toast.makeText(context, "Payment canceled", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}
```

**Option B: Modify the purchase flow**

Update the purchase flow in SubscriptionsScreen to present PaymentSheet first:

```kotlin
// When user confirms purchase
if (app.USE_STRIPE) {
    // Step 1: Present PaymentSheet to collect payment method
    app.stripeBillingProvider.presentPaymentSheet()

    // Step 2: After PaymentSheet success, call purchase
    // (Handle this in PaymentSheetState.Success callback)
} else {
    // Mock billing - direct purchase
    entitlementsVM.purchase(plan, period, trialDays)
}
```

## 🧪 Testing

### Test Cards

Use Stripe test cards (no real charges):

- **Success**: `4242 4242 4242 4242`
- **Decline**: `4000 0000 0000 0002`
- **Requires Authentication**: `4000 0025 0000 3155`

Any future expiry date and any CVC work.

### Test Flow

1. **Launch app** → Navigate to Subscriptions
2. **Select a plan** (Plus/Pro/Master)
3. **Choose period** (Monthly/Yearly)
4. **Click Continue**
5. **PaymentSheet appears** → Enter test card
6. **Submit** → Payment method saved
7. **Subscription created** → Entitlement updated
8. **Verify** → Check if plan features are unlocked

### Verify Subscription in Stripe Dashboard

1. Go to [Stripe Dashboard → Customers](https://dashboard.stripe.com/test/customers)
2. Find your test customer
3. View active subscriptions
4. Check payment methods

## 🔧 Debugging

### Enable Logging

Retrofit logging is already enabled in debug builds. Check Logcat for:
- `OkHttp` → API requests/responses
- `Stripe` → PaymentSheet events

### Common Issues

**Issue: "No setup intent"**
- Solution: Ensure `bootstrap()` is called before `presentPaymentSheet()`

**Issue: "Customer not found"**
- Solution: Call `/billing/bootstrap` first to create customer

**Issue: "Invalid price ID"**
- Solution: Update price IDs in `index.js` with actual IDs from Stripe Dashboard

**Issue: Network timeout**
- Solution: Check server is running and BASE_URL is correct
- For emulator: Use `http://10.0.2.2:4242`
- For device: Use your computer's local IP

## 🔐 Security Checklist

✅ **DO:**
- Keep `sk_test_...` secret key ONLY on backend
- Use HTTPS in production
- Validate webhook signatures
- Use Firebase Auth UID for customer mapping

❌ **DON'T:**
- Never embed `sk_test_...` in Android app
- Never commit `.env` file
- Never skip webhook validation in production
- Never use test keys in production

## 🚢 Production Preparation

Before going live:

1. **Replace test keys with live keys**
   - Get live keys from Stripe Dashboard
   - Update `.env` on backend server

2. **Set up production webhook endpoint**
   - Deploy backend to production server (Heroku, AWS, etc.)
   - Configure webhook in Stripe Dashboard
   - Update `STRIPE_WEBHOOK_SECRET` in `.env`

3. **Update Android app configuration**
   - Point `BASE_URL` to production server
   - Remove test/debug code

4. **Google Play Billing compliance**
   - ⚠️ Google Play requires Play Billing for digital goods
   - Use Stripe for web/enterprise only
   - Or: Implement both and switch based on platform
   - Review: https://support.google.com/googleplay/android-developer/answer/140504

5. **Test production flow**
   - Use live Stripe cards (real charges!)
   - Test subscriptions, cancellations, renewals
   - Verify webhooks work correctly

## 📊 Monitoring

### Backend Monitoring

Add logging for:
- Subscription creations
- Payment failures
- Webhook events
- Error rates

### App Monitoring

Track:
- PaymentSheet success/failure rates
- Subscription conversion rates
- Cancellation reasons
- Restore success rates

## 🆘 Support

### Stripe Resources
- [Stripe Docs](https://stripe.com/docs)
- [Stripe Android SDK](https://stripe.com/docs/payments/accept-a-payment?platform=android)
- [Test Cards](https://stripe.com/docs/testing)

### Troubleshooting
- Check backend logs: `npm start` output
- Check app logs: Android Studio Logcat
- Test webhooks: `stripe listen --forward-to localhost:4242/billing/webhook`

## 📝 Implementation Checklist

- [x] Backend server created (`stripe-server/`)
- [x] Stripe API keys configured
- [x] Price IDs mapped in `index.js`
- [x] Android Stripe SDK added to `build.gradle.kts`
- [x] `StripeBillingProvider` implemented
- [x] Retrofit API client configured
- [x] `InnovexiaApplication` updated with Stripe provider
- [ ] SubscriptionsScreen updated to present PaymentSheet
- [ ] Activity initialization added for PaymentSheet
- [ ] Test flow verified end-to-end
- [ ] Production deployment planned

## 🔄 Switching Between Mock and Stripe

Toggle in `InnovexiaApplication.kt`:

```kotlin
companion object {
    const val USE_STRIPE = false  // false = Mock, true = Stripe
}
```

**Mock Mode** (default):
- No network calls
- Instant subscriptions
- Local-only entitlements
- Perfect for UI development

**Stripe Mode**:
- Real PaymentSheet
- Backend API calls
- Stripe subscription management
- Production-ready flow

---

**Next Steps:**
1. Start backend server
2. Configure price IDs
3. Enable Stripe in app (`USE_STRIPE = true`)
4. Update SubscriptionsScreen for PaymentSheet
5. Test with test cards
6. Verify in Stripe Dashboard
