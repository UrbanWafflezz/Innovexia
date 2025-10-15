# Subscriptions Screen Integration Guide

## Overview
The new SubscriptionsScreen provides a full-screen, modern UI for managing subscription tiers. This guide shows how to integrate it with your existing navigation flow.

## Files Created
- `TierTokens.kt` - Design tokens, data models, and plan definitions
- `PlanCards.kt` - Card components with micro-interactions
- `CompareTable.kt` - Feature comparison matrix
- `UsagePreview.kt` - Usage snapshot tile
- `BillingHint.kt` - Billing information component
- `SubscriptionsScreen.kt` - Main screen orchestrator

## Integration Steps

### 1. Add Navigation State to HomeScreen

In `HomeScreen.kt`, add a new state variable to track when to show the subscriptions screen:

```kotlin
var showSubscriptions by rememberSaveable { mutableStateOf(false) }
```

### 2. Wire AccountQuickPanel Callback

Find where `AccountQuickPanel` is called in `HomeScreen.kt` and update the `onSubscription` callback:

```kotlin
AccountQuickPanel(
    onDismiss = { showAccountPanel = false },
    onProfile = {
        showAccountPanel = false
        showProfile = true
    },
    onUsage = {
        showAccountPanel = false
        showUsage = true
    },
    onSubscription = {  // ← Update this
        showAccountPanel = false
        showSubscriptions = true  // ← Add this
    },
    onSettings = {
        showAccountPanel = false
        showSettings = true
    },
    onLogout = { /* ... */ }
)
```

### 3. Add SubscriptionsScreen to UI Hierarchy

In `HomeScreen.kt`, add the screen conditionally (after other dialogs/sheets):

```kotlin
// Subscriptions screen
if (showSubscriptions) {
    SubscriptionsScreen(
        currentPlanId = subscription?.planId ?: "free",  // from subscriptionViewModel
        onContinue = { planId, period ->
            // TODO: Implement upgrade flow
            Toast.makeText(
                context,
                "Upgrading to $planId (${period.name})",
                Toast.LENGTH_SHORT
            ).show()
            // Later: integrate with subscriptionViewModel.upgradeToPlan()
        },
        onOpenUsage = {
            showSubscriptions = false
            showUsage = true  // Navigate to usage screen
        },
        onBack = {
            showSubscriptions = false
        }
    )
}
```

### 4. Import Required Components

Add these imports to `HomeScreen.kt`:

```kotlin
import com.example.innovexia.ui.subscriptions.SubscriptionsScreen
import com.example.innovexia.ui.subscriptions.BillingPeriod
```

### 5. Optional: Integration with SubscriptionViewModel

To get the current plan dynamically, use the existing SubscriptionViewModel:

```kotlin
val subscriptionViewModel: SubscriptionViewModel = viewModel(
    factory = SubscriptionViewModelFactory(app.subscriptionRepository, app.usageRepository)
)
val subscription by subscriptionViewModel.subscription.collectAsState()

// Then pass to SubscriptionsScreen:
SubscriptionsScreen(
    currentPlanId = subscription?.planId ?: "free",
    // ... rest of params
)
```

### 6. Future: Implement Upgrade Flow

When ready to connect billing:

```kotlin
onContinue = { planId, period ->
    val yearlyPlan = period == BillingPeriod.YEARLY
    subscriptionViewModel.upgradeToPlan(planId, yearly = yearlyPlan) { checkoutUrl ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
        context.startActivity(intent)
    }
    showSubscriptions = false
}
```

## Design Tokens Customization

If you need to adjust colors to match your design system, edit `TierTokens.kt`:

```kotlin
object TierTokens {
    // Update these to match your theme
    val Surface = Color(0xFF1A1D23)     // Dark background
    val Card = Color(0xFF0F1117)        // Card surface
    val Border = Color(0xFF2D3139)      // Border color

    // Tier colors
    val Free = Color(0xFF8C95A3)        // Gray
    val Plus = Color(0xFF6EA8FF)        // Blue
    val Pro = Color(0xFFB48EFA)         // Purple
    val Master = Color(0xFFF0C76A)      // Gold
}
```

## Testing

1. Run the app
2. Open drawer → Click profile avatar/name
3. AccountQuickPanel appears → Click "Subscription"
4. New SubscriptionsScreen should display with all 4 tiers
5. Select a plan → Sticky CTA appears at bottom
6. Click "Continue" → Shows toast (or upgrade flow if implemented)

## Notes

- The screen is **UI-only** for now—no backend calls
- Plan data is hardcoded in `TierTokens.kt` (Plans list)
- Callbacks (`onContinue`, `onOpenUsage`) allow future integration
- The old `SubscriptionsTab` in ProfileSheet can remain for backward compatibility or be removed
- Fully themed for dark mode; light mode uses same tokens with adjusted alpha
