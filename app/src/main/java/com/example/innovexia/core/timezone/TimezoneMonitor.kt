package com.example.innovexia.core.timezone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.innovexia.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Monitors timezone changes and automatically updates UserPreferences
 *
 * This service:
 * - Detects when the device timezone changes (user travels, manually changes timezone)
 * - Automatically updates the saved timezone in UserPreferences
 * - Ensures AI models always use the correct current timezone
 */
class TimezoneMonitor(
    private val context: Context,
    private val userPreferences: UserPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var receiver: BroadcastReceiver? = null

    /**
     * Timezone change broadcast receiver
     */
    private val timezoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_TIMEZONE_CHANGED) {
                val newTimezoneId = ZoneId.systemDefault().id
                android.util.Log.i("TimezoneMonitor", "Timezone changed detected: $newTimezoneId")

                scope.launch {
                    updateTimezone(newTimezoneId)
                }
            }
        }
    }

    /**
     * Start monitoring timezone changes
     */
    fun startMonitoring() {
        // Register broadcast receiver for timezone changes
        val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
        context.registerReceiver(timezoneReceiver, filter)
        receiver = timezoneReceiver

        android.util.Log.d("TimezoneMonitor", "Started monitoring timezone changes")

        // Also check and update current timezone on start
        scope.launch {
            checkAndUpdateTimezone()
        }
    }

    /**
     * Stop monitoring timezone changes
     */
    fun stopMonitoring() {
        receiver?.let {
            context.unregisterReceiver(it)
            receiver = null
        }
        android.util.Log.d("TimezoneMonitor", "Stopped monitoring timezone changes")
    }

    /**
     * Check current timezone and update if different from saved
     */
    suspend fun checkAndUpdateTimezone() {
        val currentTimezoneId = ZoneId.systemDefault().id
        val savedTimezoneId = userPreferences.userTimezoneId.first()

        if (currentTimezoneId != savedTimezoneId) {
            android.util.Log.i("TimezoneMonitor",
                "Timezone mismatch detected - Current: $currentTimezoneId, Saved: $savedTimezoneId")
            updateTimezone(currentTimezoneId)
        } else {
            android.util.Log.d("TimezoneMonitor", "Timezone is current: $currentTimezoneId")
        }
    }

    /**
     * Update saved timezone
     */
    private suspend fun updateTimezone(timezoneId: String) {
        userPreferences.setUserTimezoneId(timezoneId)
        android.util.Log.i("TimezoneMonitor", "Timezone updated in preferences: $timezoneId")
    }

    companion object {
        /**
         * Get current system timezone
         */
        fun getCurrentTimezone(): ZoneId = ZoneId.systemDefault()

        /**
         * Get current timezone ID as string
         */
        fun getCurrentTimezoneId(): String = ZoneId.systemDefault().id
    }
}
