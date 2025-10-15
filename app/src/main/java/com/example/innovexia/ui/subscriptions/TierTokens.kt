package com.example.innovexia.ui.subscriptions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.innovexia.subscriptions.mock.PlanId

object TierTokens {
    // Surfaces (dark matte/glass look)
    val Surface = Color(0xFF1A1D23)
    val Card = Color(0xFF0F1117)
    val Border = Color(0xFF2D3139).copy(alpha = 0.6f)
    val Glow = Color(0xFFF0C76A).copy(alpha = 0.25f)

    // Tier brand colors
    val Free = Color(0xFF8C95A3)
    val Plus = Color(0xFF6EA8FF)
    val Pro = Color(0xFFB48EFA)
    val Master = Color(0xFFF0C76A)

    val Radius = 20.dp

    /**
     * Get tier color for a plan
     */
    fun getTierColor(plan: PlanId): Color {
        return when (plan) {
            PlanId.FREE -> Free
            PlanId.PLUS -> Plus
            PlanId.PRO -> Pro
            PlanId.MASTER -> Master
        }
    }
}

data class Plan(
    val id: String,
    val title: String,
    val priceMonthly: String,
    val priceYearly: String,
    val tagline: String,
    val highlights: List<String>,
    val color: Color
)

enum class BillingPeriod {
    MONTHLY, YEARLY
}

data class SubscriptionsUi(
    val currentPlanId: String = "free",
    val selectedPlanId: String = "pro",
    val period: BillingPeriod = BillingPeriod.MONTHLY
)

val Plans = listOf(
    Plan(
        id = "free",
        title = "Free",
        priceMonthly = "$0",
        priceYearly = "$0",
        tagline = "Start your journey.",
        highlights = listOf(
            "Gemini 2.5 Flash",
            "Basic local memory (50 entries)",
            "5 sources (files/URLs)",
            "Incognito chats"
        ),
        color = TierTokens.Free
    ),
    Plan(
        id = "plus",
        title = "Plus",
        priceMonthly = "$9.99",
        priceYearly = "$99.99",
        tagline = "Unlock more intelligence.",
        highlights = listOf(
            "Gemini 2.5 Pro",
            "Expanded memory (500 entries)",
            "50 sources, 50MB uploads",
            "Optional cloud backup"
        ),
        color = TierTokens.Plus
    ),
    Plan(
        id = "pro",
        title = "Pro",
        priceMonthly = "$19.99",
        priceYearly = "$199.99",
        tagline = "Your AI, fully realized.",
        highlights = listOf(
            "GPT-5 & Claude 4.5 access",
            "Unlimited local memory",
            "250 sources, 100MB uploads",
            "Team spaces (2 members)"
        ),
        color = TierTokens.Pro
    ),
    Plan(
        id = "master",
        title = "Master",
        priceMonthly = "$39.99",
        priceYearly = "$399.99",
        tagline = "Unleash everything.",
        highlights = listOf(
            "All models + priority lane",
            "1,000+ sources, 250MB uploads",
            "Advanced persona controls",
            "Team spaces (5 members)"
        ),
        color = TierTokens.Master
    )
)
