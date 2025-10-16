package com.example.innovexia.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.innovexia.subscriptions.mock.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manage subscription screen showing current subscription details,
 * billing period, usage statistics, and options to upgrade/downgrade/cancel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubscriptionScreen(
    entitlement: Entitlement,
    onUpgrade: () -> Unit,
    onDowngrade: () -> Unit,
    onCancel: () -> Unit,
    onResume: () -> Unit,
    onBack: () -> Unit
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Subscription",
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
        },
        containerColor = TierTokens.Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current plan card
            CurrentPlanCard(entitlement)

            // Billing period card
            BillingPeriodCard(entitlement)

            // Usage statistics card
            UsageStatisticsCard()

            // Action buttons
            ActionButtons(
                entitlement = entitlement,
                onUpgrade = onUpgrade,
                onDowngrade = onDowngrade,
                onCancel = { showCancelDialog = true },
                onResume = onResume
            )
        }
    }

    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "Cancel Subscription?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "Your subscription will remain active until ${formatDate(entitlement.renewsAt ?: System.currentTimeMillis())}. You won't be charged again.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        onCancel()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Cancel Subscription")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Subscription")
                }
            },
            containerColor = TierTokens.Card
        )
    }
}

@Composable
private fun CurrentPlanCard(entitlement: Entitlement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TierTokens.Card
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Plan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                // Status badge
                val statusColor = when (entitlement.subscriptionStatus()) {
                    SubStatus.ACTIVE -> Color(0xFF4CAF50)
                    SubStatus.TRIALING -> Color(0xFF2196F3)
                    SubStatus.CANCELED -> Color(0xFFFF9800)
                    SubStatus.EXPIRED -> Color(0xFFF44336)
                    SubStatus.GRACE -> Color(0xFFFF9800)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = entitlement.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = statusColor
                    )
                }
            }

            HorizontalDivider(color = TierTokens.Border)

            // Plan name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = getPlanColor(entitlement.planId()),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "${entitlement.plan} Plan",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }

            // Period
            InfoRow(
                icon = Icons.Default.DateRange,
                label = "Billing Period",
                value = entitlement.period.lowercase().replaceFirstChar { it.uppercase() }
            )

            // Price
            val pricing = PlanPricing.get(entitlement.planId())
            val price = when (entitlement.billingPeriod()) {
                Period.MONTHLY -> pricing.monthly
                Period.YEARLY -> pricing.yearly
            }

            InfoRow(
                icon = Icons.Default.AccountBalanceWallet,
                label = "Price",
                value = "$price/${entitlement.period.lowercase()}"
            )
        }
    }
}

@Composable
private fun BillingPeriodCard(entitlement: Entitlement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TierTokens.Card
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Billing Information",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )

            HorizontalDivider(color = TierTokens.Border)

            // Start date
            InfoRow(
                icon = Icons.Default.PlayCircleFilled,
                label = "Started",
                value = formatDate(entitlement.startedAt)
            )

            // Renewal date
            if (entitlement.renewsAt != null) {
                InfoRow(
                    icon = Icons.Default.Refresh,
                    label = if (entitlement.isCanceled()) "Expires" else "Renews",
                    value = formatDate(entitlement.renewsAt)
                )

                // Days remaining
                val daysLeft = entitlement.daysRemaining()
                if (daysLeft != null) {
                    InfoRow(
                        icon = Icons.Default.Timer,
                        label = "Days Remaining",
                        value = "$daysLeft days"
                    )
                }
            }

            // Trial info
            if (entitlement.isTrialing() && entitlement.trialEndsAt != null) {
                InfoRow(
                    icon = Icons.Default.CardGiftcard,
                    label = "Trial Ends",
                    value = formatDate(entitlement.trialEndsAt)
                )
            }

            // Payment source
            InfoRow(
                icon = Icons.Default.Payment,
                label = "Payment Method",
                value = when {
                    entitlement.source.contains("stripe", ignoreCase = true) -> "Stripe"
                    entitlement.source.contains("google", ignoreCase = true) -> "Google Play"
                    else -> "Test Mode"
                }
            )
        }
    }
}

@Composable
private fun UsageStatisticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = TierTokens.Card
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "This Month's Usage",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )

            HorizontalDivider(color = TierTokens.Border)

            // TODO: Replace with real data
            // For now, showing placeholder values
            StatRow(
                icon = Icons.Default.Chat,
                label = "Total Chats",
                value = "42",
                color = Color(0xFF4CAF50)
            )

            StatRow(
                icon = Icons.Default.Image,
                label = "Images Uploaded",
                value = "15",
                color = Color(0xFF2196F3)
            )

            StatRow(
                icon = Icons.Default.Description,
                label = "Files Uploaded",
                value = "8",
                color = Color(0xFFFF9800)
            )

            StatRow(
                icon = Icons.Default.CloudUpload,
                label = "Storage Used",
                value = "2.3 GB",
                color = Color(0xFF9C27B0)
            )

            // Info note
            Text(
                text = "Statistics reset on the 1st of each month",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ActionButtons(
    entitlement: Entitlement,
    onUpgrade: () -> Unit,
    onDowngrade: () -> Unit,
    onCancel: () -> Unit,
    onResume: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Upgrade button (if not on highest tier)
        if (entitlement.planId() != PlanId.MASTER) {
            Button(
                onClick = onUpgrade,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TierTokens.Plus
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upgrade Plan",
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Downgrade button (if not on free tier)
        if (entitlement.planId() != PlanId.FREE) {
            val downgradeTierName = when (entitlement.planId()) {
                PlanId.MASTER -> "Pro"
                PlanId.PRO -> "Plus"
                PlanId.PLUS -> "Free"
                else -> "Free"
            }

            OutlinedButton(
                onClick = onDowngrade,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Downgrade to $downgradeTierName",
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        // Cancel/Resume button
        if (entitlement.planId() != PlanId.FREE) {
            if (entitlement.isCanceled()) {
                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resume Subscription",
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel Subscription",
                        modifier = Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = Color.White
        )
    }
}

@Composable
private fun StatRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = color
        )
    }
}

private fun getPlanColor(plan: PlanId): Color {
    return when (plan) {
        PlanId.FREE -> Color(0xFF9E9E9E)
        PlanId.PLUS -> TierTokens.Plus
        PlanId.PRO -> TierTokens.Pro
        PlanId.MASTER -> TierTokens.Master
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
