package com.example.innovexia.subscriptions.stripe

import androidx.activity.ComponentActivity
import com.example.innovexia.subscriptions.mock.*
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Stripe billing provider implementation
 * Handles subscription purchases via Stripe PaymentSheet
 */
class StripeBillingProvider(
    private val api: BillingApi,
    private val entitlementsRepo: EntitlementsRepo
) : BillingProvider {

    private var paymentSheetLauncher: ((String, PaymentSheet.Configuration) -> Unit)? = null
    private var paymentSheetConfig: PaymentSheet.Configuration? = null
    private var setupClientSecret: String? = null
    private var lastBootstrappedUid: String? = null  // Store UID from bootstrap

    // State for payment sheet result
    private val _paymentSheetState = MutableStateFlow<PaymentSheetState>(PaymentSheetState.Idle)
    val paymentSheetState: StateFlow<PaymentSheetState> = _paymentSheetState

    sealed class PaymentSheetState {
        object Idle : PaymentSheetState()
        object Loading : PaymentSheetState()
        object Success : PaymentSheetState()
        data class Error(val message: String) : PaymentSheetState()
        object Canceled : PaymentSheetState()
    }

    /**
     * Set the PaymentSheet launcher (called from Composable)
     */
    fun setPaymentSheetLauncher(launcher: (String, PaymentSheet.Configuration) -> Unit) {
        this.paymentSheetLauncher = launcher
    }

    override suspend fun current(): Entitlement {
        return entitlementsRepo.getCurrent()
    }

    /**
     * Bootstrap Stripe customer and prepare PaymentSheet
     */
    suspend fun bootstrap(uid: String, email: String): Result<Unit> = runCatching {
        _paymentSheetState.value = PaymentSheetState.Loading

        // Store UID for subsequent API calls
        lastBootstrappedUid = uid

        val resp = api.bootstrap(BootstrapRequest(uid, email))

        setupClientSecret = resp.setupIntentClientSecret
        paymentSheetConfig = PaymentSheet.Configuration(
            merchantDisplayName = "Innovexia",
            customer = PaymentSheet.CustomerConfiguration(
                id = resp.customerId,
                ephemeralKeySecret = resp.ephemeralKeySecret
            ),
            allowsDelayedPaymentMethods = true
        )

        _paymentSheetState.value = PaymentSheetState.Idle
    }

    /**
     * Handle PaymentSheet result
     */
    fun handlePaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                _paymentSheetState.value = PaymentSheetState.Success
            }
            is PaymentSheetResult.Canceled -> {
                _paymentSheetState.value = PaymentSheetState.Canceled
            }
            is PaymentSheetResult.Failed -> {
                _paymentSheetState.value = PaymentSheetState.Error(
                    result.error.message ?: "Payment failed"
                )
            }
        }
    }

    /**
     * Present PaymentSheet to collect payment method
     */
    fun presentPaymentSheet() {
        val launcher = paymentSheetLauncher ?: run {
            _paymentSheetState.value = PaymentSheetState.Error("PaymentSheet launcher not set")
            return
        }

        val secret = setupClientSecret ?: run {
            _paymentSheetState.value = PaymentSheetState.Error("No setup intent. Call bootstrap() first.")
            return
        }

        val config = paymentSheetConfig ?: run {
            _paymentSheetState.value = PaymentSheetState.Error("No configuration. Call bootstrap() first.")
            return
        }

        launcher(secret, config)
    }

    override suspend fun purchase(
        plan: PlanId,
        period: Period,
        trialDays: Int?
    ): Result<Entitlement> = runCatching {
        val uid = getCurrentUid()
        val res = api.subscribe(
            SubscribeRequest(
                uid = uid,
                planId = plan.name,
                period = period.name
            )
        )

        // Convert DTO to local Entitlement
        val entitlement = res.entitlement.toEntitlement()
        entitlementsRepo.save(entitlement)
        entitlement
    }

    override suspend fun cancelAtPeriodEnd(): Result<Entitlement> = runCatching {
        val uid = getCurrentUid()
        val res = api.cancel(CancelRequest(uid))

        val entitlement = res.entitlement.toEntitlement()
        entitlementsRepo.save(entitlement)
        entitlement
    }

    override suspend fun resume(): Result<Entitlement> = runCatching {
        val uid = getCurrentUid()
        val res = api.resume(ResumeRequest(uid))

        val entitlement = res.entitlement.toEntitlement()
        entitlementsRepo.save(entitlement)
        entitlement
    }

    override suspend fun switch(plan: PlanId, period: Period): Result<Entitlement> = runCatching {
        val uid = getCurrentUid()
        val res = api.switch(
            SwitchRequest(
                uid = uid,
                planId = plan.name,
                period = period.name
            )
        )

        // Convert DTO to local Entitlement
        val entitlement = res.entitlement.toEntitlement()
        entitlementsRepo.save(entitlement)
        entitlement
    }

    override suspend fun restore(): Result<Entitlement> = runCatching {
        val uid = getCurrentUid()
        val res = api.restore(RestoreRequest(uid))

        if (res.entitlement != null) {
            val entitlement = res.entitlement.toEntitlement()
            entitlementsRepo.save(entitlement)
            entitlement
        } else {
            // No active subscription, return free
            val free = Entitlement.free()
            entitlementsRepo.save(free)
            free
        }
    }

    override suspend fun checkAndUpdateState(): Entitlement {
        // For Stripe, state is managed server-side via webhooks
        // Just return current local state
        return entitlementsRepo.getCurrent()
    }

    /**
     * Get current user ID (from stored bootstrap UID)
     */
    private fun getCurrentUid(): String {
        return lastBootstrappedUid ?: "guest-${System.currentTimeMillis()}"
    }

    /**
     * Convert server DTO to local Entitlement model
     */
    private fun EntitlementDto.toEntitlement(): Entitlement {
        return Entitlement(
            plan = this.plan,
            period = this.period,
            status = this.status,
            startedAt = this.startedAt,
            renewsAt = this.renewsAt,
            trialEndsAt = this.trialEndsAt,
            graceEndsAt = this.graceEndsAt,
            source = this.source,
            orderId = this.orderId
        )
    }
}
