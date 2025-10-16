# Stripe PaymentSheet Crash Fix

## 🐛 Issue
App was crashing with:
```
java.lang.IllegalStateException: LifecycleOwner is attempting to register
while current state is RESUMED. LifecycleOwners must call register before
they are STARTED.
```

## 🔍 Root Cause
The `PaymentSheet` was being initialized in `LaunchedEffect` which runs AFTER the Activity is already in RESUMED state. Stripe's `PaymentSheet` requires registration **before** the Activity starts, which violates the lifecycle requirement.

## ✅ Solution
Changed from creating `PaymentSheet` directly to using Compose's `rememberPaymentSheet` API, which properly handles the lifecycle registration.

### Before (Broken):
```kotlin
// In LaunchedEffect - TOO LATE!
LaunchedEffect(Unit) {
    val provider = app.stripeBillingProvider
    provider.init(activity)  // Creates PaymentSheet here - CRASH!
}
```

### After (Fixed):
```kotlin
// At composable level - proper lifecycle timing
val paymentSheetLauncher = rememberPaymentSheet { result ->
    app.stripeBillingProvider.handlePaymentSheetResult(result)
}

LaunchedEffect(Unit) {
    // Just configure the launcher
    provider.setPaymentSheetLauncher { secret, config ->
        paymentSheetLauncher.presentWithSetupIntent(secret, config)
    }
}
```

## 📝 Files Changed

### 1. **StripeBillingProvider.kt**
- ❌ Removed: `PaymentSheet` instance and `init()` method
- ✅ Added: `setPaymentSheetLauncher()` to receive launcher from Composable
- ✅ Added: `handlePaymentSheetResult()` to process results
- ✅ Changed: `presentPaymentSheet()` now uses the provided launcher

### 2. **SubscriptionsScreen.kt**
- ✅ Added: `rememberPaymentSheet` to create launcher at composable level
- ✅ Updated: Pass launcher to provider via `setPaymentSheetLauncher()`
- ❌ Removed: Activity reference and `init()` call

## 🎯 How It Works Now

```
1. SubscriptionsScreen composable starts
2. rememberPaymentSheet() creates launcher (proper lifecycle timing)
3. LaunchedEffect passes launcher to StripeBillingProvider
4. When user clicks "Continue to Payment"
5. Provider calls launcher (which was created at right time)
6. PaymentSheet shows without crash!
```

## ✅ Testing
1. **Rebuild the app** in Android Studio
2. Run on emulator/device
3. Navigate to Subscriptions
4. Select a plan → Click Continue
5. Click "Continue to Payment"
6. **PaymentSheet should open without crash**

## 🔑 Key Takeaway
When using Stripe PaymentSheet in Jetpack Compose:
- ✅ Use `rememberPaymentSheet` at composable level
- ❌ Don't create `PaymentSheet` directly in LaunchedEffect
- ✅ Respect Android lifecycle requirements

---

**Status**: ✅ Fixed and ready to test!
