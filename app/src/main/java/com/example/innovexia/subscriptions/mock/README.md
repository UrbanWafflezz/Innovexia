# Mock Subscriptions & Entitlements System

Complete local billing provider with entitlements, purchase flow, and feature gating—no Google Play or emulator required.

## 📁 Files Created

### Core Models & Logic
- **[Entitlements.kt](Entitlements.kt)** - Data models (`PlanId`, `Period`, `SubStatus`, `Entitlement`, `FeatureCaps`, `PlanPricing`)
- **[BillingProvider.kt](BillingProvider.kt)** - Provider interface + `MockBillingProvider` implementation
- **[TimeEngine.kt](TimeEngine.kt)** - Real/accelerated time for renewal simulation
- **[EntitlementsRepo.kt](EntitlementsRepo.kt)** - DataStore persistence + `Gate` helper for feature gating
- **[EntitlementsVM.kt](EntitlementsVM.kt)** - ViewModel for UI state management
- **[PurchaseFlowDialog.kt](PurchaseFlowDialog.kt)** - Mock checkout UI

### Integration
- **[EntitlementCheckWorker.kt](../../workers/EntitlementCheckWorker.kt)** - WorkManager for periodic state checks
- **[InnovexiaApplication.kt](../../InnovexiaApplication.kt)** - DI setup (entitlementsRepo, billingProvider)
- **[HomeScreen.kt](../../ui/screens/HomeScreen.kt)** - Wired EntitlementsVM and current plan
- **[SubscriptionsScreen.kt](../../ui/subscriptions/SubscriptionsScreen.kt)** - Purchase flow + status badges

## 🎯 Features

### ✅ Purchase Flow
- **New Subscription** - Free users get 7-day trial by default
- **Plan Switch** - Immediate upgrade/downgrade (no proration in mock)
- **Cancel** - Keeps access until renewal date, then downgrades to Free
- **Resume** - Reactivate canceled subscription if still in period

### ✅ Subscription States
- **ACTIVE** - Active subscription with access
- **TRIALING** - In trial period (7 days default)
- **CANCELED** - Canceled but still has access until period end
- **EXPIRED** - No access, downgraded to Free

### ✅ Feature Capabilities

**Plan Tiers:**
| Feature | Free | Plus | Pro | Master |
|---------|------|------|-----|--------|
| **Models** | Gemini 2.5 Flash | Flash + Pro | Flash, Pro, GPT-5, Claude 4.5, Perplexity | All + Perplexity Pro |
| **Sources** | 5 | 50 | 250 | 1,000 |
| **Upload Size** | 10MB | 50MB | 100MB | 250MB |
| **Memory** | 50 entries | 500 entries | Unlimited | Unlimited |
| **Cloud Backup** | ❌ | ✅ | ✅ | ✅ |
| **Team Spaces** | 0 | 0 | 2 members | 5 members |
| **Priority Class** | 1 | 2 | 3 | 4 |

### ✅ Pricing
- **Free**: $0
- **Plus**: $9.99/mo or $99.99/yr (17% savings)
- **Pro**: $19.99/mo or $199.99/yr (17% savings)
- **Master**: $39.99/mo or $399.99/yr (17% savings)

## 🚀 Usage

### Access Current Entitlement

```kotlin
// In any Composable with entitlementsVM
val entitlement by entitlementsVM.entitlement.collectAsState()
val caps by entitlementsVM.caps.collectAsState()

// Check plan
val isProOrAbove = entitlement.planId() in listOf(PlanId.PRO, PlanId.MASTER)

// Check status
if (entitlement.isTrialing()) {
    Text("Trial: ${entitlement.daysRemaining()} days left")
}
```

### Feature Gating

```kotlin
// Check model access
if (Gate.hasModel("gpt-5", caps)) {
    // Show GPT-5 option
} else {
    // Show upgrade prompt
    Text(Gate.upgradeMessage("GPT-5", PlanId.PRO))
}

// Check source limits
if (Gate.canAddSource(currentSourceCount, caps)) {
    // Allow adding source
} else {
    Text("Upgrade to add more sources (limit: ${Gate.maxSources(caps)})")
}

// Check upload size
val maxBytes = Gate.maxUploadBytes(caps)
if (fileSize > maxBytes) {
    Text("File too large. Max: ${caps.maxUploadMb}MB")
}

// Check memory limit
val memoryLimit = Gate.memoryLimit(caps) // null = unlimited
if (memoryLimit != null && count >= memoryLimit) {
    Text("Memory full. Upgrade for unlimited entries.")
}
```

### Purchase/Manage Subscriptions

```kotlin
// Purchase new plan
entitlementsVM.purchase(
    plan = PlanId.PRO,
    period = BillingPeriod.YEARLY,
    trialDays = 7
)

// Switch plan
entitlementsVM.switchPlan(PlanId.PLUS, BillingPeriod.MONTHLY)

// Cancel subscription
entitlementsVM.cancel()

// Resume canceled subscription
entitlementsVM.resume()

// Restore purchases
entitlementsVM.restore()
```

## ⏱️ Time Acceleration (Dev Mode)

Speed up renewals/expirations for testing:

```kotlin
// Enable 1 day per minute
TimeEngineFactory.enableAcceleration(1440.0f)

// Back to real time
TimeEngineFactory.disableAcceleration()
```

## 🔄 Auto State Updates

- **WorkManager** checks state every 6 hours
- Handles trial expiration → ACTIVE
- Handles renewal → EXPIRED → FREE
- Handles grace period end → FREE

## 🎨 UI Components

### SubscriptionsScreen
- Shows current plan with badge (Current/Trial/Canceled)
- Monthly/Yearly toggle
- 4 tier cards (Free, Plus, Pro, Master)
- Compare Plans table
- Usage preview + Billing hints
- Sticky CTA rail when plan selected

### PurchaseFlowDialog
- Plan summary with price
- Trial notice (if applicable)
- Feature highlights
- Terms acceptance
- "Start Trial" / "Subscribe" CTA

### Status Badges
- **Trial Badge** - Shows days remaining
- **Canceled Badge** - Shows expiration date

## 📦 DataStore Persistence

Entitlements stored in `entitlements` DataStore:
- `current_entitlement` - Active entitlement JSON
- `entitlement_history` - Last 20 entitlements (audit trail)

## 🔮 Future: Real Billing Providers

The `BillingProvider` interface is stable for swapping implementations:

```kotlin
interface BillingProvider {
    suspend fun current(): Entitlement
    suspend fun purchase(plan, period, trialDays): Result<Entitlement>
    suspend fun cancelAtPeriodEnd(): Result<Entitlement>
    suspend fun resume(): Result<Entitlement>
    suspend fun switch(plan, period): Result<Entitlement>
    suspend fun restore(): Result<Entitlement>
    suspend fun checkAndUpdateState(): Entitlement
}
```

Later implementations:
- `PlayBillingProvider` - Google Play Billing
- `StripeBillingProvider` - Stripe subscriptions
- `ServerBillingProvider` - Custom backend

## ✅ Acceptance Criteria

✅ Purchase flow opens mock checkout dialog
✅ Trials set status=TRIALING with 7-day period
✅ Cancel moves to CANCELED, keeps access until renewsAt
✅ Upgrade/downgrade works with immediate switch
✅ Entitlements Flow available app-wide
✅ Feature gates read from `capsFor(plan)`
✅ Works offline, no Play/Stripe/emulator needed
✅ UI reflects trial/renewal/cancel status
✅ WorkManager handles background state transitions

## 🛠️ Dev Tools (Optional)

Create a hidden developer panel:
- Set plan instantly: `entitlementsVM.setDirectPlan(PlanId.PRO, Period.MONTHLY)`
- Toggle time acceleration
- Force renewal check
- Clear entitlements: `repo.clear()`

## 🔗 Integration Points

**Rate Limiting** (next prompt):
- Use `caps.priorityClass` for tier-based rate limits
- Check `Gate.priorityClass(caps)` in rate limiter

**Model Switcher**:
```kotlin
val availableModels = caps.models.filter { modelId ->
    Gate.hasModel(modelId, caps)
}
```

**Sources Tab**:
```kotlin
if (!Gate.canAddSource(sources.size, caps)) {
    ShowUpgradePrompt()
}
```

**Memory UI**:
```kotlin
val limit = Gate.memoryLimit(caps)
if (limit != null) {
    ProgressBar(current = memoryCount, max = limit)
}
```

## 🧪 Testing

1. **Open app** → Drawer → Profile → Subscription
2. **Select Plus** → Continue → PurchaseFlowDialog appears
3. **Confirm** → Trial badge shows "7 days remaining"
4. **Wait 7 days** (or accelerate time) → Auto-transitions to ACTIVE
5. **Cancel** → Shows "Expires on {date}"
6. **Wait until expiration** → Auto-downgrades to FREE

---

**Status**: ✅ Complete
**Next**: Hook rate limits per tier & per model
