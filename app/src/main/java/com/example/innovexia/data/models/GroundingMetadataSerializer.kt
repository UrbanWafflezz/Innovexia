package com.example.innovexia.data.models

import com.example.innovexia.data.ai.GroundingMetadata
import com.google.gson.Gson

/**
 * Helper to serialize/deserialize GroundingMetadata to JSON for database storage
 */
object GroundingMetadataSerializer {
    private val gson = Gson()

    fun toJson(metadata: GroundingMetadata): String {
        return gson.toJson(metadata)
    }

    fun fromJson(json: String?): GroundingMetadata? {
        if (json.isNullOrEmpty()) return null
        return try {
            gson.fromJson(json, GroundingMetadata::class.java)
        } catch (e: Exception) {
            android.util.Log.e("GroundingMetadataSerializer", "Failed to parse grounding metadata: ${e.message}")
            null
        }
    }
}
