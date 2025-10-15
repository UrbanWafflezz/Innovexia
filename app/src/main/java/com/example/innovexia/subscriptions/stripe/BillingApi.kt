package com.example.innovexia.subscriptions.stripe

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for Stripe billing endpoints
 */
interface BillingApi {

    @POST("billing/bootstrap")
    suspend fun bootstrap(@Body request: BootstrapRequest): BootstrapResponse

    @POST("billing/subscribe")
    suspend fun subscribe(@Body request: SubscribeRequest): SubscribeResponse

    @POST("billing/cancel")
    suspend fun cancel(@Body request: CancelRequest): CancelResponse

    @POST("billing/resume")
    suspend fun resume(@Body request: ResumeRequest): ResumeResponse

    @POST("billing/restore")
    suspend fun restore(@Body request: RestoreRequest): RestoreResponse

    @POST("billing/switch")
    suspend fun switch(@Body request: SwitchRequest): SwitchResponse
}

// Request/Response DTOs

data class BootstrapRequest(
    val uid: String,
    val email: String
)

data class BootstrapResponse(
    val customerId: String,
    val ephemeralKeySecret: String,
    val setupIntentClientSecret: String,
    val publishableKey: String
)

data class SubscribeRequest(
    val uid: String,
    val planId: String,
    val period: String
)

data class SubscribeResponse(
    val ok: Boolean,
    val subscriptionId: String,
    val entitlement: EntitlementDto
)

data class CancelRequest(
    val uid: String
)

data class CancelResponse(
    val ok: Boolean,
    val entitlement: EntitlementDto
)

data class ResumeRequest(
    val uid: String
)

data class ResumeResponse(
    val ok: Boolean,
    val entitlement: EntitlementDto
)

data class RestoreRequest(
    val uid: String
)

data class RestoreResponse(
    val entitlement: EntitlementDto?
)

data class SwitchRequest(
    val uid: String,
    val planId: String,
    val period: String
)

data class SwitchResponse(
    val ok: Boolean,
    val subscriptionId: String,
    val entitlement: EntitlementDto
)

/**
 * Entitlement DTO matching server response
 */
data class EntitlementDto(
    val plan: String,
    val period: String,
    val status: String,
    val startedAt: Long,
    val renewsAt: Long?,
    val trialEndsAt: Long? = null,
    val graceEndsAt: Long? = null,
    val source: String,
    val orderId: String?
)
