package com.example.innovexia.subscriptions.mock

/**
 * Time engine for renewal simulation
 * Allows accelerated time for development/testing
 */
interface TimeEngine {
    /**
     * Get current time in milliseconds
     */
    fun now(): Long

    /**
     * Time acceleration factor
     * 1.0 = real time
     * 60.0 = 1 minute per second (1 hour in 1 minute)
     * 1440.0 = 1 day per minute
     */
    val acceleration: Float
}

/**
 * Real-time engine (production)
 */
class RealTimeEngine : TimeEngine {
    override fun now(): Long = System.currentTimeMillis()
    override val acceleration: Float = 1.0f
}

/**
 * Accelerated time engine for development
 * Simulates faster time progression
 */
class AcceleratedTimeEngine(
    override val acceleration: Float = 60.0f,
    private val baseTime: Long = System.currentTimeMillis()
) : TimeEngine {
    private val startRealTime = System.currentTimeMillis()

    override fun now(): Long {
        val elapsed = System.currentTimeMillis() - startRealTime
        val acceleratedElapsed = (elapsed * acceleration).toLong()
        return baseTime + acceleratedElapsed
    }
}

/**
 * Time engine factory
 */
object TimeEngineFactory {
    private var instance: TimeEngine = RealTimeEngine()
    private var isAccelerated = false

    /**
     * Get current time engine instance
     */
    fun get(): TimeEngine = instance

    /**
     * Enable accelerated time (for development)
     */
    fun enableAcceleration(factor: Float = 1440.0f) {
        instance = AcceleratedTimeEngine(acceleration = factor)
        isAccelerated = true
    }

    /**
     * Disable acceleration (use real time)
     */
    fun disableAcceleration() {
        instance = RealTimeEngine()
        isAccelerated = false
    }

    /**
     * Check if acceleration is enabled
     */
    fun isAccelerated(): Boolean = isAccelerated

    /**
     * Get current acceleration factor
     */
    fun getAcceleration(): Float = instance.acceleration
}

/**
 * Time utility functions
 */
object TimeUtils {
    private val engine get() = TimeEngineFactory.get()

    fun now(): Long = engine.now()

    fun daysFromNow(days: Int): Long {
        return now() + (days * 24 * 60 * 60 * 1000L)
    }

    fun monthsFromNow(months: Int): Long {
        return now() + (months * 30 * 24 * 60 * 60 * 1000L)
    }

    fun yearsFromNow(years: Int): Long {
        return now() + (years * 365 * 24 * 60 * 60 * 1000L)
    }

    fun formatDaysRemaining(targetTime: Long): String {
        val now = now()
        if (targetTime <= now) return "Expired"

        val daysRemaining = ((targetTime - now) / (24 * 60 * 60 * 1000)).toInt()
        return when {
            daysRemaining == 0 -> "Today"
            daysRemaining == 1 -> "1 day"
            daysRemaining < 30 -> "$daysRemaining days"
            else -> {
                val months = daysRemaining / 30
                if (months == 1) "1 month" else "$months months"
            }
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
