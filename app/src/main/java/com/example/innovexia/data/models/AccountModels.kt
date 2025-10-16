package com.example.innovexia.data.models

import androidx.compose.ui.graphics.Color
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Tier information for display in drawer profile
 */
data class TierInfo(
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val label: String = "Free",
    val color: Color = InnovexiaColors.DarkTextSecondary
) {
    companion object {
        /**
         * Create TierInfo from UserSubscription
         */
        fun fromSubscription(subscription: UserSubscription): TierInfo {
            val (label, color) = when (subscription.plan) {
                SubscriptionPlan.FREE -> "Free" to Color(0xFF9CA3AF) // neutral gray
                SubscriptionPlan.PLUS -> "Plus" to Color(0xFF3B82F6) // blue
                SubscriptionPlan.PRO -> "Pro" to Color(0xFF8B5CF6) // purple
                SubscriptionPlan.MASTER -> "Master" to Color(0xFFF0C76A) // gold
            }

            val statusLabel = when (subscription.status) {
                SubscriptionStatus.ACTIVE -> "Active"
                SubscriptionStatus.TRIALING -> "Trial"
                SubscriptionStatus.PAST_DUE -> "Past due"
                SubscriptionStatus.CANCELED -> "Canceled"
                SubscriptionStatus.INACTIVE -> "Inactive"
            }

            return TierInfo(
                plan = subscription.plan,
                status = subscription.status,
                label = label,
                color = color
            )
        }

        /**
         * Default free tier
         */
        fun default(): TierInfo = TierInfo()
    }

    /**
     * Get status text for display
     */
    fun getStatusText(): String {
        return when (status) {
            SubscriptionStatus.ACTIVE -> "Active"
            SubscriptionStatus.TRIALING -> "Trial"
            SubscriptionStatus.PAST_DUE -> "Past due"
            SubscriptionStatus.CANCELED -> "Canceled"
            SubscriptionStatus.INACTIVE -> "Inactive"
        }
    }
}
