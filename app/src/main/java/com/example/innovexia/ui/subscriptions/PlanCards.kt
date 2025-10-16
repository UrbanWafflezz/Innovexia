package com.example.innovexia.ui.subscriptions

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlanCardsRow(
    plans: List<Plan>,
    ui: SubscriptionsUi,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(plans) { plan ->
            PlanCard(
                plan = plan,
                isSelected = plan.id == ui.selectedPlanId,
                isCurrent = plan.id == ui.currentPlanId,
                period = ui.period,
                onSelect = { onSelect(plan.id) }
            )
        }
    }
}

@Composable
fun PlanCard(
    plan: Plan,
    isSelected: Boolean,
    isCurrent: Boolean,
    period: BillingPeriod,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            isSelected -> 1.0f
            else -> 1.0f
        },
        label = "cardScale"
    )

    val price = when (period) {
        BillingPeriod.MONTHLY -> plan.priceMonthly
        BillingPeriod.YEARLY -> plan.priceYearly
    }

    val periodLabel = when (period) {
        BillingPeriod.MONTHLY -> "/month"
        BillingPeriod.YEARLY -> "/year"
    }

    Card(
        modifier = modifier
            .width(280.dp)
            .scale(scale)
            .clickable(
                enabled = !isCurrent,
                onClick = onSelect
            )
            .semantics {
                contentDescription = "${plan.title} plan: $price $periodLabel. ${plan.tagline}"
            },
        shape = RoundedCornerShape(TierTokens.Radius),
        colors = CardDefaults.cardColors(
            containerColor = TierTokens.Card
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, plan.color.copy(alpha = 0.8f))
        } else {
            BorderStroke(1.dp, TierTokens.Border)
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Tier label + badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = plan.color
                    )
                )

                if (isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = plan.color.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Current",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = plan.color,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                // Gold glow for Master when not selected
                if (plan.id == "master" && !isSelected) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TierTokens.Glow
                    ) {
                        Text(
                            text = "✨",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = price,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                if (price != "$0") {
                    Text(
                        text = periodLabel,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = plan.tagline,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.7f)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Highlights
            plan.highlights.forEach { highlight ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        modifier = Modifier.padding(end = 8.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = plan.color
                        )
                    )
                    Text(
                        text = highlight,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = TierTokens.Border
            )

            // CTA Button
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCurrent,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrent) {
                        Color.Gray.copy(alpha = 0.3f)
                    } else {
                        plan.color
                    },
                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isCurrent) "Current plan" else "Choose ${plan.title}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isCurrent) {
                        Color.White.copy(alpha = 0.5f)
                    } else {
                        Color.Black
                    }
                )
            }
        }
    }
}

@Composable
fun StickyCtaRail(
    selected: Plan,
    period: BillingPeriod,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val price = when (period) {
        BillingPeriod.MONTHLY -> selected.priceMonthly
        BillingPeriod.YEARLY -> selected.priceYearly
    }

    val periodLabel = when (period) {
        BillingPeriod.MONTHLY -> "/mo"
        BillingPeriod.YEARLY -> "/yr"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = TierTokens.Surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Selected: ${selected.title}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
                Text(
                    text = "$price$periodLabel",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = selected.color
                    )
                )
            }

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = selected.color
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
            }
        }
    }
}
