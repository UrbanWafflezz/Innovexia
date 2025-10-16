package com.example.innovexia.data.models

/**
 * Feature gating utility for subscription tiers
 * Provides centralized access control for tier-specific features
 */
object SubscriptionGate {

    /**
     * Check if user has access to a specific model
     */
    fun hasModelAccess(plan: SubscriptionPlan, modelId: String): Boolean {
        val limits = PlanLimits.getLimits(plan)
        return modelId.lowercase() in limits.modelAccess.map { it.lowercase() }
    }

    /**
     * Check if user can add more sources
     */
    fun canAddSource(plan: SubscriptionPlan, currentCount: Int): Boolean {
        val limits = PlanLimits.getLimits(plan)
        return currentCount < limits.maxSources
    }

    /**
     * Get maximum number of sources allowed
     */
    fun getMaxSources(plan: SubscriptionPlan): Int {
        return PlanLimits.getLimits(plan).maxSources
    }

    /**
     * Check if file size is within upload limit
     */
    fun canUploadFile(plan: SubscriptionPlan, fileSizeBytes: Long): Boolean {
        val limits = PlanLimits.getLimits(plan)
        val maxBytes = limits.maxUploadMB * 1024L * 1024L
        return fileSizeBytes <= maxBytes
    }

    /**
     * Get max upload size in bytes
     */
    fun getMaxUploadBytes(plan: SubscriptionPlan): Long {
        val limits = PlanLimits.getLimits(plan)
        return limits.maxUploadMB * 1024L * 1024L
    }

    /**
     * Get max upload size in MB
     */
    fun getMaxUploadMB(plan: SubscriptionPlan): Int {
        return PlanLimits.getLimits(plan).maxUploadMB
    }

    /**
     * Check if user can add more memory entries
     */
    fun canAddMemory(plan: SubscriptionPlan, currentCount: Int): Boolean {
        val limits = PlanLimits.getLimits(plan)
        val maxEntries = limits.memoryEntries ?: return true // unlimited
        return currentCount < maxEntries
    }

    /**
     * Get memory entry limit (null = unlimited)
     */
    fun getMemoryLimit(plan: SubscriptionPlan): Int? {
        return PlanLimits.getLimits(plan).memoryEntries
    }

    /**
     * Check if cloud backup is available
     */
    fun hasCloudBackup(plan: SubscriptionPlan): Boolean {
        return PlanLimits.getLimits(plan).cloudBackup
    }

    /**
     * Check if team spaces are available
     */
    fun hasTeamSpaces(plan: SubscriptionPlan): Boolean {
        return PlanLimits.getLimits(plan).teamSpaces > 0
    }

    /**
     * Get maximum team size
     */
    fun getMaxTeamSize(plan: SubscriptionPlan): Int {
        return PlanLimits.getLimits(plan).teamSpaces
    }

    /**
     * Get priority class for rate limiting (1=lowest, 4=highest)
     */
    fun getPriorityClass(plan: SubscriptionPlan): Int {
        return PlanLimits.getLimits(plan).priorityClass
    }

    /**
     * Get context length for the plan
     */
    fun getContextLength(plan: SubscriptionPlan): String {
        return PlanLimits.getLimits(plan).contextLength
    }

    /**
     * Get context length in tokens (approximate)
     */
    fun getContextLengthTokens(plan: SubscriptionPlan): Int {
        return when (PlanLimits.getLimits(plan).contextLength) {
            "32K" -> 32_000
            "128K" -> 128_000
            "256K" -> 256_000
            "512K" -> 512_000
            else -> 32_000
        }
    }

    /**
     * Format upgrade message for gated feature
     */
    fun upgradeMessage(feature: String, requiredPlan: SubscriptionPlan): String {
        val planName = requiredPlan.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$feature is available on $planName and above. Upgrade to unlock."
    }

    /**
     * Get list of available models for plan
     */
    fun getAvailableModels(plan: SubscriptionPlan): List<String> {
        return PlanLimits.getLimits(plan).modelAccess
    }

    /**
     * Check if plan has priority lane access (Master only)
     */
    fun hasPriorityLane(plan: SubscriptionPlan): Boolean {
        return plan == SubscriptionPlan.MASTER
    }

    /**
     * Check if plan has advanced persona features (Master only)
     */
    fun hasAdvancedPersonas(plan: SubscriptionPlan): Boolean {
        return plan == SubscriptionPlan.MASTER
    }
}
