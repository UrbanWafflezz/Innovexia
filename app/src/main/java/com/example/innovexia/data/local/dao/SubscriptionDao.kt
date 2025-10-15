package com.example.innovexia.data.local.dao

import androidx.room.*
import com.example.innovexia.data.local.entities.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for subscription data
 */
@Dao
interface SubscriptionDao {
    /**
     * Get subscription for a user (Flow for reactive updates)
     */
    @Query("SELECT * FROM subscriptions WHERE userId = :userId LIMIT 1")
    fun getSubscriptionFlow(userId: String): Flow<SubscriptionEntity?>

    /**
     * Get subscription for a user (one-time)
     */
    @Query("SELECT * FROM subscriptions WHERE userId = :userId LIMIT 1")
    suspend fun getSubscription(userId: String): SubscriptionEntity?

    /**
     * Insert or update subscription
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subscription: SubscriptionEntity)

    /**
     * Delete subscription for a user
     */
    @Query("DELETE FROM subscriptions WHERE userId = :userId")
    suspend fun delete(userId: String)

    /**
     * Delete all subscriptions (for logout/cleanup)
     */
    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()

    /**
     * Get all subscriptions (admin/debug)
     */
    @Query("SELECT * FROM subscriptions")
    suspend fun getAll(): List<SubscriptionEntity>
}
