package com.example.innovexia.ui.sheets.profile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.innovexia.data.models.PlanLimits
import com.example.innovexia.data.models.SubscriptionPlan
import com.example.innovexia.data.models.SubscriptionStatus
import com.example.innovexia.data.models.UserSubscription
import com.example.innovexia.ui.glass.GlassButton
import com.example.innovexia.ui.glass.GlassButtonStyle
import com.example.innovexia.ui.theme.InnovexiaColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Billing tab for managing subscriptions and payment
 */
@Composable
fun BillingTab(
    subscription: UserSubscription,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onManageBilling: () -> Unit,
    onCancelSubscription: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val textPrimary = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
    val textSecondary = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary
    val surface = if (darkTheme) InnovexiaColors.DarkSurface else InnovexiaColors.LightSurface

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Billing & Plans",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = textPrimary
        )

        // Current subscription card
        CurrentSubscriptionCard(
            subscription = subscription,
            surface = surface,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )

        // Available plans
        Text(
            text = "Available Plans",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textPrimary
        )

        SubscriptionPlan.values().forEach { plan ->
            PlanOptionCard(
                plan = plan,
                isCurrent = plan == subscription.plan,
                onClick = { onSelectPlan(plan) },
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }

        Spacer(Modifier.height(8.dp))

        // Manage billing portal
        if (subscription.stripeCustomerId != null) {
            GlassButton(
                text = "Manage in Stripe Portal",
                onClick = onManageBilling,
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
                darkTheme = darkTheme
            )
        }

        // Cancel subscription
        if (subscription.plan != SubscriptionPlan.FREE && !subscription.cancelAtPeriodEnd) {
            TextButton(
                onClick = onCancelSubscription,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Cancel Subscription",
                    color = InnovexiaColors.ErrorRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Footer notes
        Text(
            text = "• Changes take effect immediately\n• Billing handled securely via Stripe\n• Cancel anytime with no fees",
            style = MaterialTheme.typography.bodySmall,
            color = textSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Current subscription status card
 */
@Composable
private fun CurrentSubscriptionCard(
    subscription: UserSubscription,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Current Plan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
                    )
                    Text(
                        text = subscription.plan.name.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                }

                StatusBadge(subscription.status)
            }

            Divider(color = Color.Gray.copy(alpha = 0.3f))

            // Subscription details
            if (subscription.plan != SubscriptionPlan.FREE) {
                val limits = PlanLimits.getLimits(subscription.plan)

                InfoRow(
                    icon = Icons.Default.Star,
                    label = "Price",
                    value = "$${limits.pricePerMonth / 100.0} / month",
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                subscription.currentPeriodEnd?.let { end ->
                    val dateStr = formatTimestamp(end.seconds * 1000)
                    InfoRow(
                        icon = Icons.Default.DateRange,
                        label = if (subscription.cancelAtPeriodEnd) "Ends on" else "Renews on",
                        value = dateStr,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                if (subscription.cancelAtPeriodEnd) {
                    Text(
                        text = "⚠️ Your subscription will cancel at the end of the billing period",
                        style = MaterialTheme.typography.bodySmall,
                        color = InnovexiaColors.ErrorRed
                    )
                }

                if (subscription.isTrialing()) {
                    subscription.trialEnd?.let { trialEnd ->
                        val dateStr = formatTimestamp(trialEnd.seconds * 1000)
                        InfoRow(
                            icon = Icons.Default.Check,
                            label = "Trial ends",
                            value = dateStr,
                            textPrimary = textPrimary,
                            textSecondary = InnovexiaColors.BlueAccent
                        )
                    }
                }
            } else {
                Text(
                    text = "You're on the free plan with basic features",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary
                )
            }
        }
    }
}

/**
 * Plan option card
 */
@Composable
private fun PlanOptionCard(
    plan: SubscriptionPlan,
    isCurrent: Boolean,
    onClick: () -> Unit,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val limits = PlanLimits.getLimits(plan)
    val emoji = when (plan) {
        SubscriptionPlan.FREE -> "✨"
        SubscriptionPlan.CORE -> "🚀"
        SubscriptionPlan.PRO -> "💎"
        SubscriptionPlan.TEAM -> "⚡"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = if (isCurrent) InnovexiaColors.BlueAccent else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick, enabled = !isCurrent, role = Role.RadioButton)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium
                )

                Column {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plan.name.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textPrimary
                        )
                        if (isCurrent) {
                            Surface(
                                color = InnovexiaColors.BlueAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Current",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InnovexiaColors.BlueAccent
                                )
                            }
                        }
                    }

                    Text(
                        text = "${formatTokenCount(limits.monthlyTokens)} tokens/mo • ${limits.burstRequestsPerMinute} req/min",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }

            if (plan != SubscriptionPlan.FREE) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${limits.pricePerMonth / 100}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                    Text(
                        text = "/ month",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            } else {
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = InnovexiaColors.BlueAccent
                )
            }
        }
    }
}

/**
 * Status badge
 */
@Composable
private fun StatusBadge(status: SubscriptionStatus) {
    val colorAndText = when (status) {
        SubscriptionStatus.ACTIVE -> Pair(InnovexiaColors.BlueAccent, "Active")
        SubscriptionStatus.TRIALING -> Pair(InnovexiaColors.GoldDim, "Trial")
        SubscriptionStatus.PAST_DUE -> Pair(InnovexiaColors.ErrorRed, "Past Due")
        SubscriptionStatus.CANCELED -> Pair(Color.Gray, "Canceled")
        SubscriptionStatus.INACTIVE -> Pair(Color.Gray, "Inactive")
    }
    val color = colorAndText.first
    val text = colorAndText.second

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

/**
 * Info row with icon
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(20.dp)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textPrimary
            )
        }
    }
}

// ==================== Helper Functions ====================

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun formatTokenCount(tokens: Long): String {
    return when {
        tokens >= 1_000_000 -> "${tokens / 1_000_000}M"
        tokens >= 1_000 -> "${tokens / 1_000}K"
        else -> tokens.toString()
    }
}
