# ✅ Stripe Integration - Ready to Test!

## 🎉 What's Done

All Stripe integration is **complete** and ready to test!

### ✅ Backend (Complete)
- [x] Node.js + Express server (`stripe-server/`)
- [x] All API endpoints (bootstrap, subscribe, cancel, resume, restore)
- [x] Webhook support
- [x] Your test API keys configured

### ✅ Android App (Complete)
- [x] Stripe SDK added to build.gradle
- [x] StripeBillingProvider implemented
- [x] Retrofit API client configured
- [x] **SubscriptionsScreen updated with PaymentSheet flow**
- [x] Toggle between Mock and Stripe billing
- [x] Full purchase flow integrated

## 🚀 How to Test (5 Minutes)

### Step 1: Configure Price IDs (2 min)

1. Go to https://dashboard.stripe.com/test/products

2. Create 3 products (or use existing):
   - **Innovexia Plus**: Monthly ($9.99) + Yearly ($99.99)
   - **Innovexia Pro**: Monthly ($19.99) + Yearly ($199.99)
   - **Innovexia Master**: Monthly ($39.99) + Yearly ($399.99)

3. Copy the 6 price IDs

4. Open `stripe-server/index.js` and update line 17:

```javascript
function planToPriceId(planId, period) {
  const map = {
    'PLUS:MONTHLY': 'price_xxxxx',    // ← Your Plus monthly price ID
    'PLUS:YEARLY': 'price_xxxxx',     // ← Your Plus yearly price ID
    'PRO:MONTHLY': 'price_xxxxx',     // ← Your Pro monthly price ID
    'PRO:YEARLY': 'price_xxxxx',      // ← Your Pro yearly price ID
    'MASTER:MONTHLY': 'price_xxxxx',  // ← Your Master monthly price ID
    'MASTER:YEARLY': 'price_xxxxx',   // ← Your Master yearly price ID
  };
  return map[\`\${planId}:\${period}\`];
}
```

### Step 2: Start Backend (1 min)

```bash
cd stripe-server
npm install
npm start
```

You should see:
```
🚀 Innovexia Stripe server running on port 4242
📍 Base URL: http://10.0.2.2:4242
🔑 Using publishable key: pk_test_51QTE5N...
```

**Keep this terminal open!**

### Step 3: Enable Stripe in App (30 sec)

Open `app/src/main/java/com/example/innovexia/InnovexiaApplication.kt`

Change line 112:
```kotlin
const val USE_STRIPE = true  // ← Change false to true
```

### Step 4: Sync & Run (1 min)

1. **Sync Gradle** in Android Studio
2. **Run the app** on emulator or device

## 🧪 Test the Flow

### In the App:

1. **Navigate** to Subscriptions screen (Profile → Billing)
2. **Select** a plan (Plus, Pro, or Master)
3. **Choose** Monthly or Yearly
4. **Click** "Continue"
5. **Confirm** in dialog
6. **PaymentSheet opens** 🎉
7. **Enter test card**:
   ```
   Card: 4242 4242 4242 4242
   Exp:  12/25 (any future date)
   CVC:  123 (any 3 digits)
   ZIP:  12345 (any 5 digits)
   ```
8. **Submit** → Card saved
9. **Subscription created** → Features unlocked!

### Verify Success:

**In the App:**
- ✅ Toast: "Subscription activated!"
- ✅ Plan badge shows new tier
- ✅ Features unlocked

**In Stripe Dashboard:**
1. Go to https://dashboard.stripe.com/test/customers
2. Click on the newest customer
3. See active subscription
4. Status: "Active"

**In Backend Terminal:**
```
POST /billing/bootstrap 200
POST /billing/subscribe 200
```

## 🔄 What Happens

### Stripe Flow (USE_STRIPE = true):
```
1. User selects plan → Clicks Continue
2. Confirms in dialog
3. App calls /billing/bootstrap → Creates Stripe customer
4. PaymentSheet opens → User enters card
5. Card saved successfully
6. App calls /billing/subscribe → Creates subscription
7. Subscription active → Entitlement updated
8. Features unlocked! 🎉
```

### Mock Flow (USE_STRIPE = false):
```
1. User selects plan → Clicks Continue
2. Confirms in dialog
3. Instant subscription (no card required)
4. Features unlocked! 🎉
```

## 🐛 Troubleshooting

### "Stripe setup failed"
**Solution**: Check backend is running and accessible
- Emulator: Use `http://10.0.2.2:4242`
- Device: Use your computer's IP (e.g., `http://192.168.1.100:4242`)

### "Invalid price ID"
**Solution**: Update price IDs in `stripe-server/index.js` with actual IDs from Stripe Dashboard

### "Payment failed"
**Solution**:
- Check backend terminal for errors
- Use test card `4242 4242 4242 4242`
- Try again with valid card details

### "PaymentSheet not showing"
**Solution**:
- Ensure `USE_STRIPE = true`
- Check LogCat for errors
- Verify backend responded to /billing/bootstrap

### Backend not accessible from device
**Solution**:
1. Find your computer's local IP: `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
2. Update `BillingRetrofitClient.kt`:
   ```kotlin
   private const val BASE_URL = "http://YOUR_IP_HERE:4242/"
   ```
3. Make sure device and computer are on same WiFi network

## 📊 Monitoring

### Watch Backend Logs:
```bash
# In stripe-server terminal
npm start
```

Look for:
```
POST /billing/bootstrap 200
POST /billing/subscribe 200
✅ Customer created: cus_xxxxx
✅ Subscription created: sub_xxxxx
```

### Watch Android Logs:
**Logcat → Filter: OkHttp**
```
--> POST http://10.0.2.2:4242/billing/bootstrap
<-- 200 OK
```

**Logcat → Filter: Stripe**
```
PaymentSheet presented
Payment method saved: pm_xxxxx
```

## 🔐 Security Notes

### ✅ What's Safe:
- Secret key (`sk_test_...`) is ONLY on backend
- `.env` file is gitignored
- Test mode only (no real charges)

### ⚠️ Before Production:
- Get live Stripe keys (not test keys)
- Deploy backend to production server (Heroku, AWS, etc.)
- Use HTTPS
- Configure webhooks
- Review Google Play Billing requirements

## 🎯 Test Checklist

- [ ] Backend running (`npm start`)
- [ ] Price IDs configured in `index.js`
- [ ] `USE_STRIPE = true` in app
- [ ] Gradle synced
- [ ] App running on emulator/device
- [ ] Navigate to Subscriptions
- [ ] Select plan → Continue
- [ ] PaymentSheet opens
- [ ] Enter test card `4242 4242 4242 4242`
- [ ] Subscription created
- [ ] Verify in Stripe Dashboard
- [ ] Features unlocked in app

## 🔄 Switch Back to Mock

Anytime you want to test without Stripe:

```kotlin
// InnovexiaApplication.kt
const val USE_STRIPE = false  // Back to mock
```

Mock mode:
- ✅ No backend required
- ✅ Instant subscriptions
- ✅ Perfect for UI testing

## 📚 Documentation

- **This Guide**: Quick start and testing
- **Full Integration**: `STRIPE_INTEGRATION_GUIDE.md`
- **Summary**: `STRIPE_SETUP_SUMMARY.md`
- **Backend**: `stripe-server/README.md`

## 🆘 Still Having Issues?

### Check These Files:

1. **Backend Config**: `stripe-server/index.js` (price IDs)
2. **Backend Running**: Terminal shows "server running on port 4242"
3. **App Config**: `InnovexiaApplication.kt` (USE_STRIPE = true)
4. **Network**: Device can reach `http://10.0.2.2:4242/health`

### Test Backend Health:

```bash
# In another terminal
curl http://localhost:4242/health
# Should return: {"status":"ok","timestamp":"..."}
```

---

## 🎉 You're Ready!

Everything is integrated and ready to test. Just:

1. ✅ Configure price IDs
2. ✅ Start backend (`npm start`)
3. ✅ Set `USE_STRIPE = true`
4. ✅ Run app and test!

**Happy testing!** 🚀
