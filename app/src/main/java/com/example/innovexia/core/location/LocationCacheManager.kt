package com.example.innovexia.core.location

import android.content.Context
import android.location.Location
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Manages cached location data with automatic staleness tracking.
 * Provides instant location retrieval with fallback to fresh GPS data.
 */
object LocationCacheManager {

    // DataStore for persisting location data
    private val Context.locationDataStore: DataStore<Preferences> by preferencesDataStore(name = "location_cache")

    // Keys for stored location data
    private val KEY_LATITUDE = doublePreferencesKey("latitude")
    private val KEY_LONGITUDE = doublePreferencesKey("longitude")
    private val KEY_ACCURACY = floatPreferencesKey("accuracy")
    private val KEY_TIMESTAMP = longPreferencesKey("timestamp")

    // Cache freshness threshold (15 minutes in milliseconds)
    private const val CACHE_FRESHNESS_MS = 15 * 60 * 1000L

    /**
     * Get the last known location from cache.
     * Returns null if cache is empty or too stale (> 15 minutes old).
     *
     * @param context Android Context
     * @return Cached Location if fresh enough, null otherwise
     */
    suspend fun getLastKnownLocation(context: Context): Location? {
        return try {
            val preferences = context.locationDataStore.data.first()

            val latitude = preferences[KEY_LATITUDE] ?: return null
            val longitude = preferences[KEY_LONGITUDE] ?: return null
            val accuracy = preferences[KEY_ACCURACY] ?: return null
            val timestamp = preferences[KEY_TIMESTAMP] ?: return null

            // Check if location is too stale
            val ageMs = System.currentTimeMillis() - timestamp
            if (ageMs > CACHE_FRESHNESS_MS) {
                android.util.Log.d("LocationCacheManager", "Cached location is stale (${ageMs / 1000}s old) - returning null")
                return null
            }

            // Reconstruct Location object from cached data
            val location = Location("cache").apply {
                this.latitude = latitude
                this.longitude = longitude
                this.accuracy = accuracy
                this.time = timestamp
            }

            android.util.Log.d("LocationCacheManager", "✓ Retrieved cached location: $latitude, $longitude (age: ${ageMs / 1000}s, accuracy: ${accuracy}m)")
            location

        } catch (e: Exception) {
            android.util.Log.e("LocationCacheManager", "Failed to retrieve cached location: ${e.message}", e)
            null
        }
    }

    /**
     * Get the last known location from cache, regardless of staleness.
     * Useful for fallback when GPS is unavailable.
     *
     * @param context Android Context
     * @return Cached Location (may be stale), null if cache is empty
     */
    suspend fun getLastKnownLocationAnyAge(context: Context): Location? {
        return try {
            val preferences = context.locationDataStore.data.first()

            val latitude = preferences[KEY_LATITUDE] ?: return null
            val longitude = preferences[KEY_LONGITUDE] ?: return null
            val accuracy = preferences[KEY_ACCURACY] ?: return null
            val timestamp = preferences[KEY_TIMESTAMP] ?: return null

            val ageMs = System.currentTimeMillis() - timestamp

            val location = Location("cache").apply {
                this.latitude = latitude
                this.longitude = longitude
                this.accuracy = accuracy
                this.time = timestamp
            }

            android.util.Log.d("LocationCacheManager", "✓ Retrieved cached location (any age): $latitude, $longitude (age: ${ageMs / 1000}s)")
            location

        } catch (e: Exception) {
            android.util.Log.e("LocationCacheManager", "Failed to retrieve cached location: ${e.message}", e)
            null
        }
    }

    /**
     * Update the location cache with fresh GPS data.
     *
     * @param context Android Context
     * @param location Fresh location from GPS
     */
    suspend fun updateLocation(context: Context, location: Location) {
        try {
            context.locationDataStore.edit { preferences ->
                preferences[KEY_LATITUDE] = location.latitude
                preferences[KEY_LONGITUDE] = location.longitude
                preferences[KEY_ACCURACY] = location.accuracy
                preferences[KEY_TIMESTAMP] = System.currentTimeMillis()
            }

            android.util.Log.d("LocationCacheManager", "✓ Location cache updated: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}m")

        } catch (e: Exception) {
            android.util.Log.e("LocationCacheManager", "Failed to update location cache: ${e.message}", e)
        }
    }

    /**
     * Get the age of the cached location in milliseconds.
     *
     * @param context Android Context
     * @return Age in milliseconds, or null if cache is empty
     */
    suspend fun getLocationAge(context: Context): Long? {
        return try {
            val preferences = context.locationDataStore.data.first()
            val timestamp = preferences[KEY_TIMESTAMP] ?: return null
            System.currentTimeMillis() - timestamp
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if the cached location is stale (> 15 minutes old).
     *
     * @param context Android Context
     * @return True if location is stale or missing, false if fresh
     */
    suspend fun isLocationStale(context: Context): Boolean {
        val ageMs = getLocationAge(context) ?: return true
        return ageMs > CACHE_FRESHNESS_MS
    }

    /**
     * Get formatted location age string for display.
     * Examples: "2 minutes ago", "45 seconds ago", "1 hour ago"
     *
     * @param context Android Context
     * @return Formatted age string, or null if cache is empty
     */
    suspend fun getFormattedLocationAge(context: Context): String? {
        val ageMs = getLocationAge(context) ?: return null

        return when {
            ageMs < 60 * 1000 -> "${ageMs / 1000} seconds ago"
            ageMs < 60 * 60 * 1000 -> "${ageMs / (60 * 1000)} minutes ago"
            else -> "${ageMs / (60 * 60 * 1000)} hours ago"
        }
    }

    /**
     * Clear the location cache.
     *
     * @param context Android Context
     */
    suspend fun clearCache(context: Context) {
        try {
            context.locationDataStore.edit { preferences ->
                preferences.clear()
            }
            android.util.Log.d("LocationCacheManager", "Location cache cleared")
        } catch (e: Exception) {
            android.util.Log.e("LocationCacheManager", "Failed to clear location cache: ${e.message}", e)
        }
    }
}
