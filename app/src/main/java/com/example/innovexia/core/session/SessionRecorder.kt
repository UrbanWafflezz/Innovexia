package com.example.innovexia.core.session

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.innovexia.BuildConfig
import com.example.innovexia.data.local.AppDatabase
import com.example.innovexia.data.local.entities.SessionEntity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*
import kotlin.math.roundToInt

private val Context.sessionDataStore by preferencesDataStore(name = "session_prefs")
private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

object SessionRecorder {

    /**
     * Get or create a stable device ID for this installation
     */
    private suspend fun getDeviceId(ctx: Context): String {
        return ctx.sessionDataStore.data.map { prefs ->
            prefs[DEVICE_ID_KEY]
        }.first() ?: run {
            val newId = UUID.randomUUID().toString()
            ctx.sessionDataStore.edit { prefs ->
                prefs[DEVICE_ID_KEY] = newId
            }
            newId
        }
    }

    /**
     * Upsert the current session with optional location data.
     * Call on sign-in and periodically on app foreground (throttled to ~6h).
     */
    suspend fun upsertCurrentSession(
        ctx: Context,
        location: Location? = null
    ) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val deviceId = getDeviceId(ctx)
        val db = AppDatabase.getInstance(ctx)
        val sessionDao = db.sessionDao()

        val now = System.currentTimeMillis()
        val existing = sessionDao.getSessionById(deviceId)

        val city: String?
        val region: String?
        val approxLat: Double?
        val approxLon: Double?

        if (location != null) {
            approxLat = (location.latitude * 100.0).roundToInt() / 100.0
            approxLon = (location.longitude * 100.0).roundToInt() / 100.0

            // Geocode to get city/region if available
            val geocodeResult = runCatching {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    var addresses: List<Address>? = null
                    Geocoder(ctx, Locale.getDefault()).getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    ) { addresses = it }
                    addresses
                } else {
                    Geocoder(ctx, Locale.getDefault()).getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    )
                }
            }.getOrNull()

            val address = geocodeResult?.firstOrNull()
            city = address?.locality
            region = address?.adminArea
        } else {
            city = null
            region = null
            approxLat = null
            approxLon = null
        }

        if (existing != null) {
            // Update existing session
            val updated = existing.copy(
                lastActiveAt = now,
                city = city ?: existing.city,
                region = region ?: existing.region,
                approxLat = approxLat ?: existing.approxLat,
                approxLon = approxLon ?: existing.approxLon,
                appVersion = BuildConfig.VERSION_NAME
            )
            sessionDao.updateSession(updated)
        } else {
            // Create new session
            val newSession = SessionEntity(
                id = deviceId,
                platform = "Android",
                model = Build.MODEL,
                appVersion = BuildConfig.VERSION_NAME,
                createdAt = now,
                lastActiveAt = now,
                city = city,
                region = region,
                approxLat = approxLat,
                approxLon = approxLon
            )
            sessionDao.insertSession(newSession)
        }
    }

    /**
     * Update last active timestamp only (lightweight update for throttled foreground calls)
     */
    suspend fun updateLastActive(ctx: Context) {
        val deviceId = getDeviceId(ctx)
        val db = AppDatabase.getInstance(ctx)
        db.sessionDao().updateLastActive(deviceId, System.currentTimeMillis())
    }
}
