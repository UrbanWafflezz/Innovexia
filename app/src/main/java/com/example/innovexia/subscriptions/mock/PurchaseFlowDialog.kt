package com.example.innovexia.subscriptions.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.innovexia.ui.subscriptions.BillingPeriod
import com.example.innovexia.ui.subscriptions.TierTokens

/**
 * Mock purchase flow dialog
 * Simulates a checkout experience
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFlowDialog(
    plan: PlanId,
    period: BillingPeriod,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    isStripeMode: Boolean = false  // If true, shows different messaging for Stripe flow
) {
    val pricing = PlanPricing.get(plan)
    val price = when (period) {
        BillingPeriod.MONTHLY -> pricing.monthly
        BillingPeriod.YEARLY -> pricing.yearly
    }

    val periodLabel = when (period) {
        BillingPeriod.MONTHLY -> "month"
        BillingPeriod.YEARLY -> "year"
    }

    // Stripe mode has no trial - payment is immediate
    val hasTrial = !isStripeMode && plan != PlanId.FREE
    val trialDays = if (hasTrial) MockBillingProvider.DEFAULT_TRIAL_DAYS else 0

    val savings = if (period == BillingPeriod.YEARLY) {
        PlanPricing.yearlySavings(plan)
    } else 0

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = TierTokens.Surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isStripeMode) "Subscribe" else "Confirm Purchase",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Plan card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = TierTokens.Card,
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        getPlanColor(plan).copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = plan.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = getPlanColor(plan)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "$price/$periodLabel",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )

                        if (savings > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = TierTokens.Master.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Save $savings%",
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TierTokens.Master
                                    )
                                )
                            }
                        }

                        if (hasTrial) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = TierTokens.Plus,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$trialDays-day free trial",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Features included
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "What's included:",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    val caps = FeatureCaps.forPlan(plan)
                    FeatureItem("${caps.models.size} AI models")
                    FeatureItem("${caps.maxSources} sources")
                    FeatureItem("${caps.maxUploadMb}MB file uploads")
                    if (caps.memoryEntries == null) {
                        FeatureItem("Unlimited memory")
                    } else {
                        FeatureItem("${caps.memoryEntries} memory entries")
                    }
                    if (caps.cloudBackup) {
                        FeatureItem("Cloud backup")
                    }
                    if (caps.teamSpaces > 0) {
                        FeatureItem("Team spaces (${caps.teamSpaces} members)")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Terms
                Text(
                    text = if (isStripeMode) {
                        "Next, you'll add a payment method. You'll be charged $price per $periodLabel. Cancel anytime."
                    } else if (hasTrial) {
                        "Your $trialDays-day trial starts today. After the trial, you'll be charged $price per $periodLabel. Cancel anytime."
                    } else {
                        "You'll be charged $price per $periodLabel. Cancel anytime."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "By continuing, you agree to our Terms of Service and Privacy Policy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = getPlanColor(plan)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isStripeMode) {
                                "Continue to Payment"
                            } else if (hasTrial) {
                                "Start Trial"
                            } else {
                                "Subscribe"
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = TierTokens.Plus,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

private fun getPlanColor(plan: PlanId): Color {
    return when (plan) {
        PlanId.FREE -> TierTokens.Free
        PlanId.PLUS -> TierTokens.Plus
        PlanId.PRO -> TierTokens.Pro
        PlanId.MASTER -> TierTokens.Master
    }
}
