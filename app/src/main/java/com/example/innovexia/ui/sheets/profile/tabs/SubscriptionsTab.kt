package com.example.innovexia.ui.sheets.profile.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.glass.GlassButton
import com.example.innovexia.ui.glass.GlassButtonStyle
import com.example.innovexia.ui.theme.InnovexiaColors

private data class SubscriptionPlan(
    val name: String,
    val emoji: String,
    val features: List<String>,
    val monthlyPrice: String,
    val yearlyPrice: String
)

/**
 * Subscriptions tab showing available plans with refined cards.
 */
@Composable
fun SubscriptionsTab(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    var selectedPlan by rememberSaveable { mutableIntStateOf(0) } // 0 = Free (current)

    val plans = listOf(
        SubscriptionPlan(
            name = "Free",
            emoji = "✨",
            features = listOf("Basic chat", "Local history", "Standard models"),
            monthlyPrice = "$0",
            yearlyPrice = "$0"
        ),
        SubscriptionPlan(
            name = "Pro",
            emoji = "🚀",
            features = listOf("Longer context", "Persona memory", "File attachments"),
            monthlyPrice = "$9.99",
            yearlyPrice = "$7.99"
        ),
        SubscriptionPlan(
            name = "Pro Plus",
            emoji = "💎",
            features = listOf("Multi-model access", "Web tools", "Priority support"),
            monthlyPrice = "$19.99",
            yearlyPrice = "$15.99"
        ),
        SubscriptionPlan(
            name = "Mega",
            emoji = "⚡",
            features = listOf("Max context", "Team features", "Advanced tools"),
            monthlyPrice = "$39.99",
            yearlyPrice = "$31.99"
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Choose your plan",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (darkTheme) InnovexiaColors.DarkTextPrimary
            else InnovexiaColors.LightTextPrimary
        )

        Text(
            text = "Unlock more features with a premium subscription",
            style = MaterialTheme.typography.bodyMedium,
            color = if (darkTheme) InnovexiaColors.DarkTextSecondary
            else InnovexiaColors.LightTextSecondary
        )

        Spacer(Modifier.height(8.dp))

        // Plan cards
        plans.forEachIndexed { index, plan ->
            PlanCard(
                plan = plan,
                isSelected = index == selectedPlan,
                isCurrent = index == 0, // Demo: Free is current
                onClick = { selectedPlan = index },
                darkTheme = darkTheme
            )
        }

        Spacer(Modifier.height(8.dp))

        // Manage subscription button
        GlassButton(
            text = if (selectedPlan == 0) "Current plan" else "Upgrade to ${plans[selectedPlan].name}",
            onClick = {
                // TODO: Show "This is a demo" toast or implement upgrade flow
            },
            style = GlassButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedPlan != 0,
            darkTheme = darkTheme
        )

        // Footer links
        Text(
            text = "Restore purchases • Manage payment method",
            style = MaterialTheme.typography.bodySmall,
            color = if (darkTheme) InnovexiaColors.DarkTextSecondary
            else InnovexiaColors.LightTextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    darkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (darkTheme) InnovexiaColors.DarkSurface
                else InnovexiaColors.LightSurface
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) InnovexiaColors.BlueAccent
                else if (darkTheme) InnovexiaColors.DarkBorder
                else InnovexiaColors.LightBorder,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick, role = Role.RadioButton)
            .padding(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emoji icon
                    Text(
                        text = plan.emoji,
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 32.sp
                    )

                    Column {
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                            else InnovexiaColors.LightTextPrimary
                        )

                        Text(
                            text = "${plan.monthlyPrice}/month",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (darkTheme) InnovexiaColors.BlueAccent
                            else InnovexiaColors.BlueBright
                        )
                    }
                }

                // Current plan badge or checkmark
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(InnovexiaColors.Success)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = InnovexiaColors.White
                        )
                    }
                } else if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = InnovexiaColors.BlueAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Features list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                plan.features.forEach { feature ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = InnovexiaColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                            else InnovexiaColors.LightTextSecondary
                        )
                    }
                }
            }

            // Yearly savings note (if applicable)
            if (plan.yearlyPrice != plan.monthlyPrice && plan.yearlyPrice != "$0") {
                Text(
                    text = "Save 20% with yearly: ${plan.yearlyPrice}/month",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = InnovexiaColors.Warning,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(InnovexiaColors.Warning.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
