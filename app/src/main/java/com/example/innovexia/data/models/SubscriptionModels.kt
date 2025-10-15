package com.example.innovexia.data.models

import com.google.firebase.Timestamp

/**
 * Subscription plan tiers
 */
enum class SubscriptionPlan {
    FREE,
    CORE,
    PRO,
    TEAM;

    companion object {
        fun fromString(value: String): SubscriptionPlan {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: FREE
        }
    }
}

/**
 * Subscription status
 */
enum class SubscriptionStatus {
    ACTIVE,
    TRIALING,
    PAST_DUE,
    CANCELED,
    INACTIVE;

    companion object {
        fun fromString(value: String): SubscriptionStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: INACTIVE
        }
    }
}

/**
 * Plan limits and capabilities
 */
data class PlanLimits(
    val plan: SubscriptionPlan,
    val monthlyTokens: Long,
    val dailyTokens: Long,
    val burstRequestsPerMinute: Int,
    val maxAttachmentSizeMB: Int,
    val modelAccess: List<String>,
    val pricePerMonth: Int // in cents
) {
    companion object {
        fun getLimits(plan: SubscriptionPlan): PlanLimits {
            return when (plan) {
                SubscriptionPlan.FREE -> PlanLimits(
                    plan = SubscriptionPlan.FREE,
                    monthlyTokens = 1_000_000L,
                    dailyTokens = 100_000L,
                    burstRequestsPerMinute = 10,
                    maxAttachmentSizeMB = 2,
                    modelAccess = listOf("gemini-2.5-flash"),
                    pricePerMonth = 0
                )
                SubscriptionPlan.CORE -> PlanLimits(
                    plan = SubscriptionPlan.CORE,
                    monthlyTokens = 10_000_000L,
                    dailyTokens = 1_500_000L,
                    burstRequestsPerMinute = 30,
                    maxAttachmentSizeMB = 8,
                    modelAccess = listOf("gemini-2.5-flash", "gemini-2.5-pro"),
                    pricePerMonth = 1800 // $18.00
                )
                SubscriptionPlan.PRO -> PlanLimits(
                    plan = SubscriptionPlan.PRO,
                    monthlyTokens = 25_000_000L,
                    dailyTokens = 4_000_000L,
                    burstRequestsPerMinute = 60,
                    maxAttachmentSizeMB = 16,
                    modelAccess = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-thinking", "grounding"),
                    pricePerMonth = 2800 // $28.00
                )
                SubscriptionPlan.TEAM -> PlanLimits(
                    plan = SubscriptionPlan.TEAM,
                    monthlyTokens = 60_000_000L,
                    dailyTokens = 8_000_000L,
                    burstRequestsPerMinute = 90,
                    maxAttachmentSizeMB = 20,
                    modelAccess = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-thinking", "grounding", "sso"),
                    pricePerMonth = 4200 // $42.00 per seat
                )
            }
        }
    }
}

/**
 * User subscription data (stored in Firestore /users/{uid}/subscription)
 */
data class UserSubscription(
    val plan: SubscriptionPlan = SubscriptionPlan.FREE,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val currentPeriodStart: Timestamp? = null,
    val currentPeriodEnd: Timestamp? = null,
    val cancelAtPeriodEnd: Boolean = false,
    val stripeCustomerId: String? = null,
    val stripeSubscriptionId: String? = null,
    val trialEnd: Timestamp? = null
) {
    /**
     * Convert to Firestore map
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "plan" to plan.name.lowercase(),
            "status" to status.name.lowercase(),
            "currentPeriodStart" to currentPeriodStart,
            "currentPeriodEnd" to currentPeriodEnd,
            "cancelAtPeriodEnd" to cancelAtPeriodEnd,
            "stripeCustomerId" to stripeCustomerId,
            "stripeSubscriptionId" to stripeSubscriptionId,
            "trialEnd" to trialEnd
        )
    }

    companion object {
        /**
         * Parse from Firestore document
         */
        fun fromMap(map: Map<String, Any>): UserSubscription {
            return UserSubscription(
                plan = SubscriptionPlan.fromString(map["plan"] as? String ?: "free"),
                status = SubscriptionStatus.fromString(map["status"] as? String ?: "active"),
                currentPeriodStart = map["currentPeriodStart"] as? Timestamp,
                currentPeriodEnd = map["currentPeriodEnd"] as? Timestamp,
                cancelAtPeriodEnd = map["cancelAtPeriodEnd"] as? Boolean ?: false,
                stripeCustomerId = map["stripeCustomerId"] as? String,
                stripeSubscriptionId = map["stripeSubscriptionId"] as? String,
                trialEnd = map["trialEnd"] as? Timestamp
            )
        }

        /**
         * Default free subscription
         */
        fun default(): UserSubscription {
            val now = Timestamp.now()
            val oneMonthLater = Timestamp(now.seconds + 30L * 24 * 60 * 60, now.nanoseconds)
            return UserSubscription(
                plan = SubscriptionPlan.FREE,
                status = SubscriptionStatus.ACTIVE,
                currentPeriodStart = now,
                currentPeriodEnd = oneMonthLater
            )
        }
    }

    /**
     * Check if subscription is active (including trial)
     */
    fun isActive(): Boolean {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING
    }

    /**
     * Check if currently in trial period
     */
    fun isTrialing(): Boolean {
        if (status != SubscriptionStatus.TRIALING) return false
        val trialEndTime = trialEnd ?: return false
        return Timestamp.now().seconds < trialEndTime.seconds
    }

    /**
     * Get current plan limits
     */
    fun getLimits(): PlanLimits {
        return PlanLimits.getLimits(plan)
    }
}
