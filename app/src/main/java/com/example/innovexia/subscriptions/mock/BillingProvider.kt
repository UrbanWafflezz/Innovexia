package com.example.innovexia.subscriptions.mock

/**
 * Billing provider interface
 * Future-proof for Google Play Billing, Stripe, etc.
 */
interface BillingProvider {
    /**
     * Get current entitlement
     */
    suspend fun current(): Entitlement

    /**
     * Purchase a new plan
     * @param plan Target plan to purchase
     * @param period Billing period (monthly/yearly)
     * @param trialDays Optional trial period in days
     * @return Result with new entitlement or error
     */
    suspend fun purchase(
        plan: PlanId,
        period: Period,
        trialDays: Int? = null
    ): Result<Entitlement>

    /**
     * Cancel subscription at period end
     * Keeps access until renewal date
     */
    suspend fun cancelAtPeriodEnd(): Result<Entitlement>

    /**
     * Resume a canceled subscription
     * Only works if still within the current period
     */
    suspend fun resume(): Result<Entitlement>

    /**
     * Switch to a different plan (upgrade/downgrade)
     * @param plan Target plan
     * @param period Billing period
     * @return Result with updated entitlement
     */
    suspend fun switch(
        plan: PlanId,
        period: Period
    ): Result<Entitlement>

    /**
     * Restore purchases (reload from source)
     * For mock: just returns current
     * For real providers: validates with Play/Stripe
     */
    suspend fun restore(): Result<Entitlement>

    /**
     * Check and update entitlement state
     * Handles renewals, expirations, trial ends
     */
    suspend fun checkAndUpdateState(): Entitlement
}

/**
 * Mock billing provider implementation
 * Simulates local purchase flow with persistence
 */
class MockBillingProvider(
    private val repo: EntitlementsRepo
) : BillingProvider {

    companion object {
        const val DEFAULT_TRIAL_DAYS = 7
        const val MONTHLY_DAYS = 30
        const val YEARLY_DAYS = 365
    }

    override suspend fun current(): Entitlement {
        return repo.getCurrent()
    }

    override suspend fun purchase(
        plan: PlanId,
        period: Period,
        trialDays: Int?
    ): Result<Entitlement> {
        return try {
            val current = repo.getCurrent()

            // Cannot purchase if already on a paid plan
            if (current.planId() != PlanId.FREE && current.isActive()) {
                return Result.failure(Exception("Already subscribed. Use switch() to change plans."))
            }

            val now = TimeUtils.now()
            val orderId = generateOrderId()

            // Calculate trial if applicable
            val hasTrialParam = trialDays != null && trialDays > 0
            val hasTrial = (plan != PlanId.FREE && hasTrialParam)
            val trialEnds = if (hasTrial) {
                TimeUtils.daysFromNow(trialDays ?: DEFAULT_TRIAL_DAYS)
            } else null

            // Calculate renewal date
            val renewalDays = when (period) {
                Period.MONTHLY -> MONTHLY_DAYS
                Period.YEARLY -> YEARLY_DAYS
            }
            val renewsAt = if (plan == PlanId.FREE) {
                null
            } else {
                TimeUtils.daysFromNow(renewalDays)
            }

            val entitlement = Entitlement(
                plan = plan.name,
                period = period.name,
                status = if (hasTrial) SubStatus.TRIALING.name else SubStatus.ACTIVE.name,
                startedAt = now,
                renewsAt = renewsAt,
                trialEndsAt = trialEnds,
                graceEndsAt = null,
                source = "local-mock",
                orderId = orderId
            )

            repo.save(entitlement)
            Result.success(entitlement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelAtPeriodEnd(): Result<Entitlement> {
        return try {
            val current = repo.getCurrent()

            if (current.planId() == PlanId.FREE) {
                return Result.failure(Exception("Cannot cancel free plan"))
            }

            if (current.subscriptionStatus() == SubStatus.CANCELED) {
                return Result.failure(Exception("Already canceled"))
            }

            // Mark as canceled but keep renewsAt
            val updated = current.copy(
                status = SubStatus.CANCELED.name
            )

            repo.save(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resume(): Result<Entitlement> {
        return try {
            val current = repo.getCurrent()

            if (current.subscriptionStatus() != SubStatus.CANCELED) {
                return Result.failure(Exception("Subscription is not canceled"))
            }

            // Check if still within period
            val renewsAt = current.renewsAt
            if (renewsAt == null || TimeUtils.now() >= renewsAt) {
                return Result.failure(Exception("Subscription period has ended. Please purchase again."))
            }

            // Reactivate
            val updated = current.copy(
                status = SubStatus.ACTIVE.name
            )

            repo.save(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun switch(plan: PlanId, period: Period): Result<Entitlement> {
        return try {
            val current = repo.getCurrent()

            if (plan == current.planId() && period == current.billingPeriod()) {
                return Result.failure(Exception("Already on this plan"))
            }

            val now = TimeUtils.now()

            // Immediate switch (no proration in mock)
            val renewalDays = when (period) {
                Period.MONTHLY -> MONTHLY_DAYS
                Period.YEARLY -> YEARLY_DAYS
            }

            val renewsAt = if (plan == PlanId.FREE) {
                null
            } else {
                TimeUtils.daysFromNow(renewalDays)
            }

            val updated = Entitlement(
                plan = plan.name,
                period = period.name,
                status = SubStatus.ACTIVE.name,
                startedAt = now,
                renewsAt = renewsAt,
                trialEndsAt = null,  // No trial on switches
                graceEndsAt = null,
                source = "local-mock",
                orderId = generateOrderId()
            )

            repo.save(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restore(): Result<Entitlement> {
        // For mock, just return current
        // For real providers, would validate with Play/Stripe
        return try {
            val current = repo.getCurrent()
            Result.success(current)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkAndUpdateState(): Entitlement {
        val current = repo.getCurrent()
        val now = TimeUtils.now()

        // Check trial expiration
        if (current.isTrialing()) {
            val trialEnd = current.trialEndsAt
            if (trialEnd != null && now >= trialEnd) {
                // Trial ended, move to active
                val updated = current.copy(
                    status = SubStatus.ACTIVE.name,
                    trialEndsAt = null
                )
                repo.save(updated)
                return updated
            }
        }

        // Check renewal/expiration
        val renewsAt = current.renewsAt
        if (renewsAt != null && now >= renewsAt) {
            when (current.subscriptionStatus()) {
                SubStatus.CANCELED, SubStatus.ACTIVE, SubStatus.TRIALING -> {
                    // Subscription expired, downgrade to free
                    val expired = Entitlement.free().copy(
                        startedAt = now
                    )
                    repo.save(expired)
                    return expired
                }
                SubStatus.GRACE -> {
                    // Grace period ended, expire
                    val expired = current.copy(
                        status = SubStatus.EXPIRED.name
                    )
                    repo.save(expired)
                    return expired
                }
                else -> {}
            }
        }

        // Check grace period
        val graceEndsAt = current.graceEndsAt
        if (graceEndsAt != null && now >= graceEndsAt) {
            // Grace ended, downgrade to free
            val expired = Entitlement.free().copy(
                startedAt = now
            )
            repo.save(expired)
            return expired
        }

        return current
    }

    /**
     * Generate a mock order ID
     */
    private fun generateOrderId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "INV-$timestamp-$random"
    }
}
