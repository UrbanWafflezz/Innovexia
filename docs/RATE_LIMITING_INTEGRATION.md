# Rate Limiting Integration Guide

## Overview

Complete client-side rate limiting has been implemented for all subscription tiers (Free: 10 req/min, Core: 30 req/min, Pro: 60 req/min, Team: 90 req/min).

## What's Been Implemented

### 1. RateLimitManager (`core/ratelimit/RateLimitManager.kt`)

A singleton manager that enforces burst rate limits using a sliding 60-second window.

**Features:**
- ✅ Tracks request timestamps in a 60-second window
- ✅ Enforces per-plan burst limits
- ✅ Provides real-time count and cooldown
- ✅ Thread-safe with ConcurrentLinkedQueue
- ✅ Automatic cleanup of old timestamps

**Usage:**
```kotlin
val rateLimiter = RateLimitManager.getInstance()

// Check if request can be made
val (canMake, secondsUntil) = rateLimiter.canMakeRequest(SubscriptionPlan.FREE)

if (canMake) {
    // Make the request
    rateLimiter.recordRequest()
    sendMessageToAI()
} else {
    // Show error: "Wait $secondsUntil seconds"
}
```

### 2. SubscriptionViewModel Integration

Added rate limit state and methods:

**New State Flows:**
```kotlin
val burstCount: StateFlow<Int> // Current request count in last minute
val isRateLimited: StateFlow<Boolean> // Is currently rate limited
val rateLimitCooldown: StateFlow<Int> // Seconds until allowed
val rateLimitMessage: StateFlow<String> // Human-readable status
```

**New Methods:**
```kotlin
fun canMakeRequest(): Pair<Boolean, String> // Check + get error message
fun recordRequest() // Record that a request was made
fun resetRateLimiter() // Reset (for testing)
```

### 3. UI Integration

**ProfileSheet** → **UsageTab**:
- ✅ Shows real-time burst count (e.g., "7 / 10 req/min")
- ✅ Progress bar with color coding
- ✅ Cooldown warning when limited

---

## How to Integrate in HomeViewModel

### Step 1: Add SubscriptionViewModel Dependency

Update `HomeViewModelFactory.kt`:

```kotlin
class HomeViewModelFactory(
    private val chatRepository: ChatRepository,
    private val userPreferences: UserPreferences,
    private val geminiService: GeminiService,
    private val personaRepository: PersonaRepository,
    private val subscriptionViewModel: SubscriptionViewModel // ADD THIS
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                chatRepository,
                userPreferences,
                geminiService,
                personaRepository,
                subscriptionViewModel // ADD THIS
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
```

### Step 2: Update HomeViewModel Constructor

```kotlin
class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val userPreferences: UserPreferences,
    private val geminiService: GeminiService,
    private val personaRepository: PersonaRepository,
    private val subscriptionViewModel: SubscriptionViewModel // ADD THIS
) : ViewModel() {
```

### Step 3: Add Rate Limit Check Before Sending Messages

Find where messages are sent (likely a function like `sendMessage` or `streamResponse`) and add the check:

```kotlin
fun sendMessage(text: String, attachments: List<Attachment> = emptyList()) {
    viewModelScope.launch {
        // ✅ CHECK RATE LIMIT BEFORE SENDING
        val (canSend, errorMessage) = subscriptionViewModel.canMakeRequest()

        if (!canSend) {
            // Show error to user
            _errorMessage.value = errorMessage
            return@launch
        }

        // ✅ RECORD THE REQUEST
        subscriptionViewModel.recordRequest()

        // Continue with normal message sending...
        try {
            val response = geminiService.generateResponse(text, attachments)

            // Track usage
            subscriptionViewModel.trackUsage(
                tokensIn = estimateTokens(text),
                tokensOut = estimateTokens(response)
            )

            // Update UI...
        } catch (e: Exception) {
            _errorMessage.value = e.message
        }
    }
}
```

### Step 4: Show Rate Limit Status in UI (Optional)

Add a StateFlow to expose rate limit info to the UI:

```kotlin
// In HomeViewModel
val rateLimitStatus: StateFlow<String> = subscriptionViewModel.rateLimitMessage
val isRateLimited: StateFlow<Boolean> = subscriptionViewModel.isRateLimited
```

Then in the Compose UI (ChatComposer or similar):

```kotlin
val rateLimitStatus by viewModel.rateLimitStatus.collectAsState()
val isRateLimited by viewModel.isRateLimited.collectAsState()

// Show banner if rate limited
if (isRateLimited) {
    Text(
        text = rateLimitStatus,
        color = InnovexiaColors.ErrorRed,
        modifier = Modifier.padding(8.dp)
    )
}

// Disable send button if rate limited
GlassButton(
    text = "Send",
    onClick = { viewModel.sendMessage(message) },
    enabled = !isRateLimited && message.isNotBlank()
)
```

---

## Testing Rate Limiting

### Test 1: Free Tier (10 req/min)

1. Make sure you're on FREE plan
2. Send 10 messages rapidly
3. On the 11th message, you should see: **"Rate limit exceeded. Wait X seconds..."**
4. Wait for cooldown, then send again ✅

### Test 2: Verify Cooldown

1. Hit rate limit
2. Observe cooldown countdown in error message
3. After cooldown expires, next message should work ✅

### Test 3: Check Usage Tab

1. Open Profile → Usage tab
2. Send messages
3. Watch "Burst Rate" counter increment in real-time
4. Progress bar should update
5. When limit hit, progress bar turns red ✅

### Test 4: Plan Upgrade

1. Upgrade from FREE (10/min) to CORE (30/min)
2. Rate limit should immediately increase to 30 req/min ✅

---

## Rate Limit Behavior by Plan

| Plan | Burst Limit | Behavior |
|------|-------------|----------|
| **Free** | 10 req/min | After 10 requests in 60s, blocks until oldest request expires |
| **Core** | 30 req/min | After 30 requests in 60s, blocks until oldest request expires |
| **Pro** | 60 req/min | After 60 requests in 60s, blocks until oldest request expires |
| **Team** | 90 req/min | After 90 requests in 60s, blocks until oldest request expires |

**Example:**
- User sends 10 messages in 5 seconds (on Free tier)
- 11th message is blocked
- After 60 seconds from the FIRST message, oldest timestamp expires
- User can send 1 more message (now only 9 in the window)
- This creates a "sliding window" effect

---

## Error Messages

**Rate Limited:**
```
"Rate limit exceeded. Wait 23 seconds before sending another message."
```

**Approaching Limit:**
```
"Approaching rate limit: 8/10 requests this minute"
```

**Normal:**
```
"3/10 requests this minute"
```

---

## Example: HomeScreen Integration

```kotlin
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    subscriptionViewModel: SubscriptionViewModel, // ADD THIS
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as InnovexiaApplication

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            app.chatRepository,
            app.userPreferences,
            app.geminiService,
            PersonaRepository(app.database.personaDao()),
            subscriptionViewModel // PASS IT HERE
        )
    )

    val isRateLimited by subscriptionViewModel.isRateLimited.collectAsState()
    val rateLimitMessage by subscriptionViewModel.rateLimitMessage.collectAsState()

    // Show rate limit warning
    if (isRateLimited) {
        Snackbar {
            Text(rateLimitMessage)
        }
    }

    // Rest of HomeScreen...
}
```

---

## Server-Side Integration (Future)

When Cloud Functions are deployed, the `/v1/generate` endpoint will:

1. ✅ Check rate limit server-side (Firestore `/rate/now`)
2. ✅ Return 429 error if exceeded
3. ✅ Return usage snapshot with `minuteCount` and `burstLimit`

Client should then:
```kotlin
try {
    val response = functions.getHttpsCallable("generate").call(data).await()
    val usageSnapshot = response.data["usage"] as UsageSnapshot
    subscriptionViewModel.updateUsageSnapshot(usageSnapshot) // Server truth
} catch (e: FirebaseFunctionsException) {
    if (e.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED) {
        // Server-side rate limit hit
        showError(e.message)
    }
}
```

---

## Debugging

**Check current rate limit status:**
```kotlin
val (count, limit) = subscriptionViewModel.rateLimitManager.getCurrentCount(plan)
println("Current: $count/$limit requests")
```

**Reset rate limiter (testing only):**
```kotlin
subscriptionViewModel.resetRateLimiter()
```

**Check if can make request:**
```kotlin
val (canMake, secondsUntil) = subscriptionViewModel.canMakeRequest()
if (!canMake) {
    println("Blocked. Wait $secondsUntil seconds")
}
```

---

## Summary

✅ **Implemented:**
- RateLimitManager with sliding 60-second window
- SubscriptionViewModel integration
- Real-time UI updates in UsageTab
- Cooldown countdown
- Per-plan burst limits

📋 **TODO:**
- Integrate into HomeViewModel's message sending
- Show rate limit warning in ChatComposer
- Handle server-side rate limit responses (when Cloud Functions deployed)

**The core rate limiting system is complete and ready to use!**
