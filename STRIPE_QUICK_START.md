# Stripe Quick Start - 5 Minute Setup

## 📋 Prerequisites
- [x] Stripe account with test API keys (already configured in `.env`)
- [ ] Node.js installed
- [ ] Android Studio with project open

## 🚀 Setup (3 Commands)

### 1️⃣ Configure Price IDs (2 minutes)

Go to: https://dashboard.stripe.com/test/products

Create 3 products or use existing:
- **Plus**: Monthly + Yearly prices
- **Pro**: Monthly + Yearly prices
- **Master**: Monthly + Yearly prices

Copy the 6 price IDs, then update **`stripe-server/index.js`** line 17:

```javascript
function planToPriceId(planId, period) {
  const map = {
    'PLUS:MONTHLY': 'price_xxxxx',    // ← Paste your price ID here
    'PLUS:YEARLY': 'price_xxxxx',     // ← Paste your price ID here
    'PRO:MONTHLY': 'price_xxxxx',     // ← Paste your price ID here
    'PRO:YEARLY': 'price_xxxxx',      // ← Paste your price ID here
    'MASTER:MONTHLY': 'price_xxxxx',  // ← Paste your price ID here
    'MASTER:YEARLY': 'price_xxxxx',   // ← Paste your price ID here
  };
  return map[\`\${planId}:\${period}\`];
}
```

### 2️⃣ Start Backend (1 minute)

```bash
cd stripe-server
npm install
npm start
```

You should see:
```
🚀 Innovexia Stripe server running on port 4242
📍 Base URL: http://10.0.2.2:4242
```

Keep this terminal open!

### 3️⃣ Enable Stripe in App (30 seconds)

Open **`app/src/main/java/com/example/innovexia/InnovexiaApplication.kt`**

Change line 112:
```kotlin
const val USE_STRIPE = true  // ← Change false to true
```

**Sync Gradle** and run the app!

## 🧪 Test (2 minutes)

### In the App:
1. Navigate to **Subscriptions** screen
2. Select **Plus** plan
3. Choose **Monthly** or **Yearly**
4. Click **Continue**
5. PaymentSheet opens *(currently needs UI integration - see below)*

### Test Card:
```
Card: 4242 4242 4242 4242
Exp: Any future date (e.g., 12/25)
CVC: Any 3 digits (e.g., 123)
ZIP: Any 5 digits (e.g., 12345)
```

### Verify:
Go to https://dashboard.stripe.com/test/customers
- You should see a new customer
- With an active subscription

## ⚠️ Important: UI Integration Still Needed

The PaymentSheet flow requires a small update to SubscriptionsScreen. Here's what to add:

### Option 1: Quick Test (Minimal Changes)

Add to **`SubscriptionsScreen.kt`** after line 33:

```kotlin
val activity = LocalContext.current as? ComponentActivity
val app = LocalContext.current.applicationContext as InnovexiaApplication

// Initialize Stripe if enabled
LaunchedEffect(Unit) {
    if (activity != null && app.USE_STRIPE) {
        val provider = app.stripeBillingProvider as StripeBillingProvider
        provider.init(activity)

        // Bootstrap customer
        val uid = "test-user-${System.currentTimeMillis()}"
        val email = "test@example.com"
        provider.bootstrap(uid, email)
    }
}

// Handle PaymentSheet state
LaunchedEffect(Unit) {
    if (app.USE_STRIPE) {
        val provider = app.stripeBillingProvider as StripeBillingProvider
        provider.paymentSheetState.collect { state ->
            when (state) {
                is StripeBillingProvider.PaymentSheetState.Success -> {
                    // Payment method saved - proceed with purchase
                    Toast.makeText(context, "Card added successfully!", Toast.LENGTH_SHORT).show()
                }
                is StripeBillingProvider.PaymentSheetState.Error -> {
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
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

Then in the purchase button handler (around line 169), replace:

```kotlin
// OLD:
entitlementsVM.purchase(mockPlan, ui.period, trialDays)

// NEW:
if (app.USE_STRIPE) {
    val provider = app.stripeBillingProvider as StripeBillingProvider
    provider.presentPaymentSheet()
    // After PaymentSheet success, call purchase
} else {
    entitlementsVM.purchase(mockPlan, ui.period, trialDays)
}
```

### Option 2: Full Flow (Better UX)

See **`STRIPE_INTEGRATION_GUIDE.md`** section "Step 4: Initialize StripeBillingProvider" for the complete implementation with proper state handling.

## 🐛 Troubleshooting

### "Network Error" or "Connection Refused"
✅ **Fix**: Check server is running (`npm start` in stripe-server)
✅ **Fix**: For emulator use `http://10.0.2.2:4242`
✅ **Fix**: For device use your computer's IP

### "Invalid Price ID"
✅ **Fix**: Update price IDs in `stripe-server/index.js`
✅ **Fix**: Make sure using TEST mode price IDs (start with `price_`)

### "PaymentSheet not initialized"
✅ **Fix**: Call `provider.init(activity)` before presenting
✅ **Fix**: Add UI integration code above

### "Customer not found"
✅ **Fix**: Call `bootstrap()` before `presentPaymentSheet()`

## 📊 Check It Works

### Backend Logs (in terminal):
```
POST /billing/bootstrap 200
POST /billing/subscribe 200
```

### Stripe Dashboard:
1. Go to https://dashboard.stripe.com/test/customers
2. See new customer with email "test@example.com"
3. Click customer → View subscription
4. Status should be "Active"

### Android Logs (Logcat):
Filter: `OkHttp` to see API requests
```
--> POST http://10.0.2.2:4242/billing/bootstrap
<-- 200 OK
```

## 🎯 What You Get

With Stripe enabled:
- ✅ Real payment collection (test mode)
- ✅ Subscription management
- ✅ Cancel/Resume flows
- ✅ Webhook support (optional)
- ✅ Production-ready architecture

Toggle back to Mock anytime:
```kotlin
const val USE_STRIPE = false  // Back to mock
```

## 📚 Full Documentation

- **Complete Guide**: `STRIPE_INTEGRATION_GUIDE.md`
- **Summary**: `STRIPE_SETUP_SUMMARY.md`
- **Backend**: `stripe-server/README.md`

## 🔑 Your API Keys (Already Configured)

✅ **Publishable Key**: `pk_test_51QTE5N...`
✅ **Secret Key**: `sk_test_51QTE5N...` (backend only)

**⚠️ NEVER commit these to public repos!** (already in `.gitignore`)

---

**Ready to test?** 🚀

1. ✅ Price IDs configured in `index.js`
2. ✅ Backend running (`npm start`)
3. ✅ `USE_STRIPE = true` in app
4. ✅ UI integration added (see above)
5. ✅ Test card: `4242 4242 4242 4242`

**You're all set!** 🎉
