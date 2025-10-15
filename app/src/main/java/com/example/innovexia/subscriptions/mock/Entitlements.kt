package com.example.innovexia.subscriptions.mock

/**
 * Plan tiers matching the UI (Free, Plus, Pro, Master)
 */
enum class PlanId {
    FREE, PLUS, PRO, MASTER;

    companion object {
        fun fromString(value: String): PlanId {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: FREE
        }
    }
}

/**
 * Billing period (Monthly or Yearly)
 */
enum class Period {
    MONTHLY, YEARLY;

    companion object {
        fun fromString(value: String): Period {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

/**
 * Subscription status
 */
enum class SubStatus {
    ACTIVE,      // Active subscription with access
    TRIALING,    // In trial period
    GRACE,       // Grace period (e.g., payment failed but still has access)
    CANCELED,    // Canceled but still has access until period end
    EXPIRED;     // Expired, no access

    companion object {
        fun fromString(value: String): SubStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}

/**
 * User's subscription entitlement
 * For DataStore persistence
 */
data class Entitlement(
    val plan: String = PlanId.FREE.name,
    val period: String = Period.MONTHLY.name,
    val status: String = SubStatus.ACTIVE.name,
    val startedAt: Long = System.currentTimeMillis(),
    val renewsAt: Long? = null,           // null if FREE or canceled (end-of-term handled)
    val trialEndsAt: Long? = null,        // null if no trial
    val graceEndsAt: Long? = null,        // null if not in grace
    val source: String = "local-mock",    // later: "google-play" | "stripe"
    val orderId: String? = null           // mock order token
) {
    // Convenience accessors
    fun planId(): PlanId = PlanId.fromString(plan)
    fun billingPeriod(): Period = Period.fromString(period)
    fun subscriptionStatus(): SubStatus = SubStatus.fromString(status)

    /**
     * Check if subscription is active (including trial and grace)
     */
    fun isActive(): Boolean {
        return when (subscriptionStatus()) {
            SubStatus.ACTIVE, SubStatus.TRIALING, SubStatus.GRACE -> true
            SubStatus.CANCELED -> {
                // Still active if before renewal date
                val renews = renewsAt ?: return false
                System.currentTimeMillis() < renews
            }
            SubStatus.EXPIRED -> false
        }
    }

    /**
     * Check if currently in trial
     */
    fun isTrialing(): Boolean {
        if (subscriptionStatus() != SubStatus.TRIALING) return false
        val trialEnd = trialEndsAt ?: return false
        return System.currentTimeMillis() < trialEnd
    }

    /**
     * Check if canceled but still has access
     */
    fun isCanceled(): Boolean {
        return subscriptionStatus() == SubStatus.CANCELED && isActive()
    }

    /**
     * Get days remaining in trial/subscription
     */
    fun daysRemaining(): Int? {
        val targetTime = when {
            isTrialing() -> trialEndsAt
            isCanceled() -> renewsAt
            else -> renewsAt
        } ?: return null

        val now = System.currentTimeMillis()
        if (targetTime <= now) return 0

        return ((targetTime - now) / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * Convert to Firestore map
     */
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "plan" to plan,
            "period" to period,
            "status" to status,
            "startedAt" to startedAt,
            "renewsAt" to renewsAt,
            "trialEndsAt" to trialEndsAt,
            "graceEndsAt" to graceEndsAt,
            "source" to source,
            "orderId" to orderId
        )
    }

    companion object {
        /**
         * Default free entitlement
         */
        fun free(): Entitlement {
            return Entitlement(
                plan = PlanId.FREE.name,
                period = Period.MONTHLY.name,
                status = SubStatus.ACTIVE.name,
                startedAt = System.currentTimeMillis(),
                renewsAt = null,
                trialEndsAt = null,
                source = "local-mock"
            )
        }

        /**
         * Create from Firestore map
         */
        fun fromMap(map: Map<String, Any>): Entitlement {
            return Entitlement(
                plan = map["plan"] as? String ?: PlanId.FREE.name,
                period = map["period"] as? String ?: Period.MONTHLY.name,
                status = map["status"] as? String ?: SubStatus.ACTIVE.name,
                startedAt = (map["startedAt"] as? Long) ?: System.currentTimeMillis(),
                renewsAt = map["renewsAt"] as? Long,
                trialEndsAt = map["trialEndsAt"] as? Long,
                graceEndsAt = map["graceEndsAt"] as? Long,
                source = map["source"] as? String ?: "unknown",
                orderId = map["orderId"] as? String
            )
        }
    }
}

/**
 * Feature capabilities for each plan tier
 */
data class FeatureCaps(
    val models: Set<String>,
    val maxSources: Int,
    val maxUploadMb: Int,
    val memoryEntries: Int?,  // null = unlimited
    val priorityClass: Int,   // 1..4 (1=lowest, 4=highest)
    val cloudBackup: Boolean,
    val teamSpaces: Int       // 0 = none, N = max members
) {
    companion object {
        /**
         * Get feature capabilities for a plan
         */
        fun forPlan(plan: PlanId): FeatureCaps {
            return when (plan) {
                PlanId.FREE -> FeatureCaps(
                    models = setOf("gemini-2.5-flash"),
                    maxSources = 5,
                    maxUploadMb = 10,
                    memoryEntries = 50,
                    priorityClass = 1,
                    cloudBackup = false,
                    teamSpaces = 0
                )
                PlanId.PLUS -> FeatureCaps(
                    models = setOf(
                        "gemini-2.5-flash",
                        "gemini-2.5-pro"
                    ),
                    maxSources = 50,
                    maxUploadMb = 50,
                    memoryEntries = 500,
                    priorityClass = 2,
                    cloudBackup = true,
                    teamSpaces = 0
                )
                PlanId.PRO -> FeatureCaps(
                    models = setOf(
                        "gemini-2.5-flash",
                        "gemini-2.5-pro",
                        "gpt-5",
                        "claude-4.5",
                        "perplexity"
                    ),
                    maxSources = 250,
                    maxUploadMb = 100,
                    memoryEntries = null, // unlimited
                    priorityClass = 3,
                    cloudBackup = true,
                    teamSpaces = 2
                )
                PlanId.MASTER -> FeatureCaps(
                    models = setOf(
                        "gemini-2.5-flash",
                        "gemini-2.5-pro",
                        "gpt-5",
                        "claude-4.5",
                        "perplexity",
                        "perplexity-pro"
                    ),
                    maxSources = 1000,
                    maxUploadMb = 250,
                    memoryEntries = null, // unlimited
                    priorityClass = 4,
                    cloudBackup = true,
                    teamSpaces = 5
                )
            }
        }
    }
}

/**
 * Pricing configuration (used by UI and billing)
 */
object PlanPricing {
    data class Price(
        val monthly: String,
        val yearly: String,
        val monthlyRaw: Int,  // cents
        val yearlyRaw: Int    // cents
    )

    private val prices = mapOf(
        PlanId.FREE to Price("$0", "$0", 0, 0),
        PlanId.PLUS to Price("$9.99", "$99.99", 999, 9999),
        PlanId.PRO to Price("$19.99", "$199.99", 1999, 19999),
        PlanId.MASTER to Price("$39.99", "$399.99", 3999, 39999)
    )

    fun get(plan: PlanId): Price = prices[plan] ?: prices[PlanId.FREE]!!

    /**
     * Calculate yearly savings percentage
     */
    fun yearlySavings(plan: PlanId): Int {
        val price = get(plan)
        if (price.monthlyRaw == 0) return 0
        val yearlyMonthly = price.yearlyRaw / 12
        val savings = ((price.monthlyRaw - yearlyMonthly).toFloat() / price.monthlyRaw * 100).toInt()
        return savings
    }
}
