package com.example.innovexia.subscriptions.mock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for entitlements and subscription management
 * Used by SubscriptionsScreen and other UI
 */
class EntitlementsVM(
    private val billingProvider: BillingProvider,
    private val repo: EntitlementsRepo
) : ViewModel() {

    // Current entitlement
    val entitlement: StateFlow<Entitlement> = repo.entitlementFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Entitlement.free()
        )

    // Feature capabilities
    val caps: StateFlow<FeatureCaps> = repo.capsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeatureCaps.forPlan(PlanId.FREE)
        )

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _success = MutableSharedFlow<String>()
    val success: SharedFlow<String> = _success.asSharedFlow()

    init {
        // Check state on init
        checkState()
    }

    /**
     * Purchase a plan
     */
    fun purchase(plan: PlanId, period: com.example.innovexia.ui.subscriptions.BillingPeriod, trialDays: Int? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val mockPeriod = when (period) {
                    com.example.innovexia.ui.subscriptions.BillingPeriod.MONTHLY -> Period.MONTHLY
                    com.example.innovexia.ui.subscriptions.BillingPeriod.YEARLY -> Period.YEARLY
                }

                val result = billingProvider.purchase(plan, mockPeriod, trialDays)
                result.onSuccess { ent ->
                    val message = if (ent.isTrialing()) {
                        "Trial started! ${ent.daysRemaining()} days remaining."
                    } else {
                        "Subscription activated!"
                    }
                    _success.emit(message)
                }.onFailure { e ->
                    _error.emit(e.message ?: "Purchase failed")
                }
            } catch (e: Exception) {
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Switch to a different plan
     */
    fun switchPlan(plan: PlanId, period: com.example.innovexia.ui.subscriptions.BillingPeriod) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val mockPeriod = when (period) {
                    com.example.innovexia.ui.subscriptions.BillingPeriod.MONTHLY -> Period.MONTHLY
                    com.example.innovexia.ui.subscriptions.BillingPeriod.YEARLY -> Period.YEARLY
                }

                val result = billingProvider.switch(plan, mockPeriod)
                result.onSuccess {
                    _success.emit("Plan updated successfully!")
                }.onFailure { e ->
                    _error.emit(e.message ?: "Failed to switch plan")
                }
            } catch (e: Exception) {
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Cancel subscription at period end
     */
    fun cancel() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = billingProvider.cancelAtPeriodEnd()
                result.onSuccess { ent ->
                    val expiresDate = ent.renewsAt?.let { TimeUtils.formatDate(it) } ?: "soon"
                    _success.emit("Subscription canceled. Access until $expiresDate.")
                }.onFailure { e ->
                    _error.emit(e.message ?: "Cancel failed")
                }
            } catch (e: Exception) {
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Resume a canceled subscription
     */
    fun resume() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = billingProvider.resume()
                result.onSuccess {
                    _success.emit("Subscription resumed!")
                }.onFailure { e ->
                    _error.emit(e.message ?: "Resume failed")
                }
            } catch (e: Exception) {
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Restore purchases
     */
    fun restore() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = billingProvider.restore()
                result.onSuccess {
                    _success.emit("Purchases restored")
                }.onFailure { e ->
                    _error.emit(e.message ?: "Restore failed")
                }
            } catch (e: Exception) {
                _error.emit(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check and update entitlement state
     */
    fun checkState() {
        viewModelScope.launch {
            try {
                billingProvider.checkAndUpdateState()
            } catch (e: Exception) {
                // Silent fail for background checks
            }
        }
    }

    /**
     * Clear entitlements (dev only)
     */
    fun clearEntitlements() {
        viewModelScope.launch {
            repo.clear()
            _success.emit("Entitlements cleared")
        }
    }

    /**
     * Set plan directly (dev only)
     */
    fun setDirectPlan(plan: PlanId, period: Period) {
        viewModelScope.launch {
            repo.setDirect(plan, period)
            _success.emit("Plan set to ${plan.name}")
        }
    }
}

/**
 * Factory for EntitlementsVM
 */
class EntitlementsVMFactory(
    private val billingProvider: BillingProvider,
    private val repo: EntitlementsRepo
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EntitlementsVM::class.java)) {
            return EntitlementsVM(billingProvider, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
