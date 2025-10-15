package com.example.innovexia.ui.subscriptions

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.subscriptions.mock.*
import com.example.innovexia.subscriptions.stripe.StripeBillingProvider
import com.google.firebase.auth.FirebaseAuth
// Stripe imports - ensure Gradle is synced to resolve these
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    currentPlanId: String,
    entitlementsVM: EntitlementsVM,
    usageVM: UsageVM,
    onOpenUsage: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as InnovexiaApplication

    val entitlement by entitlementsVM.entitlement.collectAsState()
    val isLoading by entitlementsVM.isLoading.collectAsState()
    val usageState by usageVM.usageState.collectAsState()

    var ui by remember {
        mutableStateOf(
            SubscriptionsUi(
                currentPlanId = currentPlanId,
                selectedPlanId = currentPlanId
            )
        )
    }
    val plans = remember { Plans }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var pendingPurchase by remember { mutableStateOf<Pair<PlanId, BillingPeriod>?>(null) }

    // Setup PaymentSheet launcher (Stripe only)
    val paymentSheetLauncher = rememberPaymentSheet { result ->
        app.stripeBillingProvider.handlePaymentSheetResult(result)
    }

    // Initialize Stripe if enabled
    LaunchedEffect(Unit) {
        if (InnovexiaApplication.USE_STRIPE) {
            val provider = app.stripeBillingProvider

            // Set the PaymentSheet launcher
            provider.setPaymentSheetLauncher { secret: String, config: PaymentSheet.Configuration ->
                paymentSheetLauncher.presentWithSetupIntent(secret, config)
            }

            // Bootstrap Stripe customer
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest-${System.currentTimeMillis()}"
            val email = FirebaseAuth.getInstance().currentUser?.email ?: "test@example.com"

            provider.bootstrap(uid, email).onFailure { error ->
                Toast.makeText(context, "Stripe setup failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Listen for PaymentSheet results (Stripe only)
    LaunchedEffect(Unit) {
        if (InnovexiaApplication.USE_STRIPE) {
            app.stripeBillingProvider.paymentSheetState.collect { state ->
                when (state) {
                    is StripeBillingProvider.PaymentSheetState.Success -> {
                        // Payment method saved successfully - now create subscription
                        val pending = pendingPurchase
                        if (pending != null) {
                            val (plan, period) = pending
                            entitlementsVM.purchase(plan, period, null)
                            pendingPurchase = null
                        }
                    }
                    is StripeBillingProvider.PaymentSheetState.Error -> {
                        Toast.makeText(context, "Payment failed: ${state.message}", Toast.LENGTH_LONG).show()
                        pendingPurchase = null
                    }
                    is StripeBillingProvider.PaymentSheetState.Canceled -> {
                        Toast.makeText(context, "Payment canceled", Toast.LENGTH_SHORT).show()
                        pendingPurchase = null
                    }
                    else -> {}
                }
            }
        }
    }

    // Listen for success/error messages
    LaunchedEffect(Unit) {
        entitlementsVM.success.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            // Navigate back to chat on successful upgrade
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        entitlementsVM.error.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            BackTopBar(title = "Subscriptions", onBack = onBack)
        },
        bottomBar = {
            val selected = plans.firstOrNull { it.id == ui.selectedPlanId }
            AnimatedVisibility(
                visible = selected != null && ui.selectedPlanId != ui.currentPlanId,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                if (selected != null) {
                    StickyCtaRail(
                        selected = selected,
                        period = ui.period,
                        onContinue = {
                            showPurchaseDialog = true
                        }
                    )
                }
            }
        },
        containerColor = TierTokens.Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            SubsHeader()

            // Trial/Status badge
            if (entitlement.isTrialing()) {
                TrialBadge(daysRemaining = entitlement.daysRemaining() ?: 0)
            } else if (entitlement.isCanceled()) {
                CanceledBadge(
                    expiresDate = entitlement.renewsAt?.let { TimeUtils.formatDate(it) } ?: ""
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tier toggle bar
            TierToggleBar(
                period = ui.period,
                onChange = { ui = ui.copy(period = it) }
            )

            // Plan cards
            PlanCardsRow(
                plans = plans,
                ui = ui,
                onSelect = { picked -> ui = ui.copy(selectedPlanId = picked) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = TierTokens.Border
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Compare table
            CompareTable(onSeeDetails = { /* Optional: scroll or expand */ })

            Spacer(modifier = Modifier.height(24.dp))

            // Usage preview and billing hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UsagePreview(
                    usageState = usageState,
                    onOpen = onOpenUsage,
                    modifier = Modifier.weight(1f)
                )
                BillingHint(
                    period = ui.period,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Legal footer
            LegalFooter()

            Spacer(modifier = Modifier.height(80.dp)) // Extra space for sticky CTA
        }
    }

    // Purchase flow dialog
    if (showPurchaseDialog) {
        val selected = plans.firstOrNull { it.id == ui.selectedPlanId }
        if (selected != null) {
            val mockPlan = PlanId.fromString(selected.id)
            PurchaseFlowDialog(
                plan = mockPlan,
                period = ui.period,
                isStripeMode = InnovexiaApplication.USE_STRIPE,
                onDismiss = { showPurchaseDialog = false },
                onConfirm = {
                    showPurchaseDialog = false

                    if (InnovexiaApplication.USE_STRIPE) {
                        // Stripe flow: Show PaymentSheet first
                        pendingPurchase = Pair(mockPlan, ui.period)
                        app.stripeBillingProvider.presentPaymentSheet()
                    } else {
                        // Mock flow: Direct purchase
                        val currentMockPlan = entitlement.planId()
                        if (currentMockPlan == PlanId.FREE) {
                            // New purchase with trial
                            entitlementsVM.purchase(
                                plan = mockPlan,
                                period = ui.period,
                                trialDays = if (mockPlan != PlanId.FREE) MockBillingProvider.DEFAULT_TRIAL_DAYS else null
                            )
                        } else {
                            // Plan switch (no trial)
                            entitlementsVM.switchPlan(mockPlan, ui.period)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun SubsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Subscriptions",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose a plan that fits how you create with Innovexia.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TierToggleBar(
    period: BillingPeriod,
    onChange: (BillingPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = TierTokens.Card,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Monthly option
                ToggleOption(
                    text = "Monthly",
                    isSelected = period == BillingPeriod.MONTHLY,
                    onClick = { onChange(BillingPeriod.MONTHLY) }
                )

                // Yearly option with badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToggleOption(
                        text = "Yearly",
                        isSelected = period == BillingPeriod.YEARLY,
                        onClick = { onChange(BillingPeriod.YEARLY) }
                    )

                    AnimatedVisibility(
                        visible = period == BillingPeriod.YEARLY,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .offset(x = (-8).dp),
                            shape = RoundedCornerShape(8.dp),
                            color = TierTokens.Master.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Save 15%",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TierTokens.Master
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TierTokens.Surface else Color.Transparent
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun LegalFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TierTokens.Card.copy(alpha = 0.5f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Frequently Asked Questions",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color.White
        )

        FAQItem(
            question = "What happens if I cancel?",
            answer = "You can cancel anytime. Your subscription will remain active until the end of your billing period, and you'll retain access to all features until then."
        )

        FAQItem(
            question = "Do my memories remain?",
            answer = "Local memories always remain on your device. Cloud-synced memories (Plus and above) remain accessible as long as you maintain an active subscription."
        )

        FAQItem(
            question = "What about refunds?",
            answer = "Refunds are handled through Google Play according to their refund policy. Contact support within 48 hours of purchase for assistance."
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "By subscribing, you agree to our Terms of Service and Privacy Policy.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FAQItem(question: String, answer: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White.copy(alpha = 0.9f)
        )
        Text(
            text = answer,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(
    title: String,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TierTokens.Surface
        )
    )
}


@Composable
private fun TrialBadge(daysRemaining: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = TierTokens.Plus.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, TierTokens.Plus.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏱️",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Free Trial Active",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TierTokens.Plus
                    )
                )
                Text(
                    text = "$daysRemaining days remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun CanceledBadge(expiresDate: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFF6B6B).copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Subscription Canceled",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B)
                    )
                )
                Text(
                    text = "Access until $expiresDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

