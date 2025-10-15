package com.example.innovexia.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.innovexia.data.models.SubscriptionPlan
import com.example.innovexia.data.models.SubscriptionStatus
import com.example.innovexia.data.models.UserSubscription
import com.google.firebase.Timestamp

/**
 * Local cache of user subscription data
 */
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey
    val userId: String,
    val plan: String, // Stored as string for Room compatibility
    val status: String,
    val currentPeriodStart: Long?, // Timestamp in milliseconds
    val currentPeriodEnd: Long?,
    val cancelAtPeriodEnd: Boolean,
    val stripeCustomerId: String?,
    val stripeSubscriptionId: String?,
    val trialEnd: Long?,
    val lastSynced: Long = System.currentTimeMillis()
) {
    /**
     * Convert to domain model
     */
    fun toUserSubscription(): UserSubscription {
        return UserSubscription(
            plan = SubscriptionPlan.fromString(plan),
            status = SubscriptionStatus.fromString(status),
            currentPeriodStart = currentPeriodStart?.let {
                Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt())
            },
            currentPeriodEnd = currentPeriodEnd?.let {
                Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt())
            },
            cancelAtPeriodEnd = cancelAtPeriodEnd,
            stripeCustomerId = stripeCustomerId,
            stripeSubscriptionId = stripeSubscriptionId,
            trialEnd = trialEnd?.let {
                Timestamp(it / 1000, ((it % 1000) * 1_000_000).toInt())
            }
        )
    }

    companion object {
        /**
         * Create from domain model
         */
        fun fromUserSubscription(userId: String, subscription: UserSubscription): SubscriptionEntity {
            return SubscriptionEntity(
                userId = userId,
                plan = subscription.plan.name.lowercase(),
                status = subscription.status.name.lowercase(),
                currentPeriodStart = subscription.currentPeriodStart?.let {
                    it.seconds * 1000 + it.nanoseconds / 1_000_000
                },
                currentPeriodEnd = subscription.currentPeriodEnd?.let {
                    it.seconds * 1000 + it.nanoseconds / 1_000_000
                },
                cancelAtPeriodEnd = subscription.cancelAtPeriodEnd,
                stripeCustomerId = subscription.stripeCustomerId,
                stripeSubscriptionId = subscription.stripeSubscriptionId,
                trialEnd = subscription.trialEnd?.let {
                    it.seconds * 1000 + it.nanoseconds / 1_000_000
                }
            )
        }
    }
}
