package com.example.innovexia.ui.viewmodels

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.data.models.SubscriptionPlan
import com.example.innovexia.data.models.SubscriptionStatus
import com.example.innovexia.data.models.TierInfo
import com.example.innovexia.subscriptions.mock.PlanId
import com.example.innovexia.subscriptions.mock.SubStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for drawer account section
 * Manages Firebase auth state and subscription tier information
 */
class DrawerAccountViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val subscriptionRepository = (application as InnovexiaApplication).subscriptionRepository
    private val entitlementsRepo = (application as InnovexiaApplication).entitlementsRepo

    // Auth state
    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    // Subscription tier (from entitlements - mock billing)
    val tier: StateFlow<TierInfo> = entitlementsRepo.entitlementFlow
        .map { entitlement ->
            val mockPlan = entitlement.planId()
            val mockStatus = entitlement.subscriptionStatus()

            // Map mock plan to old SubscriptionPlan
            val oldPlan = when (mockPlan) {
                PlanId.FREE -> SubscriptionPlan.FREE
                PlanId.PLUS -> SubscriptionPlan.CORE // Map Plus to Core
                PlanId.PRO -> SubscriptionPlan.PRO
                PlanId.MASTER -> SubscriptionPlan.TEAM // Map Master to Team
            }

            // Map mock status to old SubscriptionStatus
            val oldStatus = when (mockStatus) {
                SubStatus.ACTIVE -> SubscriptionStatus.ACTIVE
                SubStatus.TRIALING -> SubscriptionStatus.TRIALING
                SubStatus.CANCELED -> SubscriptionStatus.CANCELED
                SubStatus.GRACE, SubStatus.EXPIRED -> SubscriptionStatus.INACTIVE
            }

            // Get tier label and color
            val (label, color) = when (mockPlan) {
                PlanId.FREE -> "Free" to Color(0xFF9CA3AF)
                PlanId.PLUS -> "Plus" to Color(0xFF6EA8FF)
                PlanId.PRO -> "Pro" to Color(0xFFB48EFA)
                PlanId.MASTER -> "Master" to Color(0xFFF0C76A)
            }

            TierInfo(
                plan = oldPlan,
                status = oldStatus,
                label = label,
                color = color
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TierInfo.default()
        )

    // Guest mode check
    val isGuest: StateFlow<Boolean> = user.map { it == null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = auth.currentUser == null
        )

    init {
        // Listen to auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            _user.value = firebaseAuth.currentUser
        }

        // Refresh tier on initialization if online
        viewModelScope.launch {
            refreshTierIfStale()
        }
    }

    /**
     * Refresh tier information from Firestore if stale
     */
    private suspend fun refreshTierIfStale() {
        try {
            subscriptionRepository.getSubscription()
        } catch (e: Exception) {
            // Ignore errors - we'll use cached data
            e.printStackTrace()
        }
    }

    /**
     * Sign out the current user
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                subscriptionRepository.clear()
                _user.value = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
