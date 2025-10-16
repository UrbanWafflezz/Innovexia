package com.example.innovexia.data.models

import com.google.firebase.Timestamp

/**
 * Subscription plan tiers
 * Aligned with mock system: FREE, PLUS, PRO, MASTER
 */
enum class SubscriptionPlan {
    FREE,
    PLUS,
    PRO,
    MASTER;

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
 * Unified system with mock entitlements
 */
data class PlanLimits(
    val plan: SubscriptionPlan,
    // Token limits (5-hour windows)
    val tokensPerWindow: Long,
    val messagesPerWindow: Int,
    val windowDurationHours: Int,
    // Burst rate limiting (per minute)
    val burstRequestsPerMinute: Int,
    // Model access
    val modelAccess: List<String>,
    // Upload & storage
    val maxUploadMB: Int,
    val maxSources: Int,
    val memoryEntries: Int?, // null = unlimited
    // Context
    val contextLength: String,
    // Features
    val cloudBackup: Boolean,
    val teamSpaces: Int, // 0 = none, N = max members
    val priorityClass: Int, // 1..4 (1=lowest, 4=highest)
    // Pricing
    val pricePerMonth: Int // in cents
) {
    companion object {
        fun getLimits(plan: SubscriptionPlan): PlanLimits {
            return when (plan) {
                SubscriptionPlan.FREE -> PlanLimits(
                    plan = SubscriptionPlan.FREE,
                    tokensPerWindow = 100_000L,
                    messagesPerWindow = 25,
                    windowDurationHours = 5,
                    burstRequestsPerMinute = 10,
                    modelAccess = listOf("gemini-2.5-flash"),
                    maxUploadMB = 10,
                    maxSources = 5,
                    memoryEntries = 50,
                    contextLength = "32K",
                    cloudBackup = false,
                    teamSpaces = 0,
                    priorityClass = 1,
                    pricePerMonth = 0
                )
                SubscriptionPlan.PLUS -> PlanLimits(
                    plan = SubscriptionPlan.PLUS,
                    tokensPerWindow = 500_000L,
                    messagesPerWindow = 100,
                    windowDurationHours = 5,
                    burstRequestsPerMinute = 30,
                    modelAccess = listOf("gemini-2.5-flash", "gemini-2.5-pro"),
                    maxUploadMB = 50,
                    maxSources = 50,
                    memoryEntries = 500,
                    contextLength = "128K",
                    cloudBackup = true,
                    teamSpaces = 0,
                    priorityClass = 2,
                    pricePerMonth = 999 // $9.99
                )
                SubscriptionPlan.PRO -> PlanLimits(
                    plan = SubscriptionPlan.PRO,
                    tokensPerWindow = 1_500_000L,
                    messagesPerWindow = 250,
                    windowDurationHours = 5,
                    burstRequestsPerMinute = 60,
                    modelAccess = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gpt-5", "claude-4.5", "perplexity"),
                    maxUploadMB = 100,
                    maxSources = 250,
                    memoryEntries = null, // unlimited
                    contextLength = "256K",
                    cloudBackup = true,
                    teamSpaces = 2,
                    priorityClass = 3,
                    pricePerMonth = 1999 // $19.99
                )
                SubscriptionPlan.MASTER -> PlanLimits(
                    plan = SubscriptionPlan.MASTER,
                    tokensPerWindow = 5_000_000L,
                    messagesPerWindow = 1000,
                    windowDurationHours = 5,
                    burstRequestsPerMinute = 90,
                    modelAccess = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gpt-5", "claude-4.5", "perplexity", "perplexity-pro"),
                    maxUploadMB = 250,
                    maxSources = 1000,
                    memoryEntries = null, // unlimited
                    contextLength = "512K",
                    cloudBackup = true,
                    teamSpaces = 5,
                    priorityClass = 4,
                    pricePerMonth = 3999 // $39.99
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
