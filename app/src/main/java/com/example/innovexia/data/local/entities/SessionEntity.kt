package com.example.innovexia.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey
    val id: String,  // deviceId (stable UUID)
    val platform: String = "Android",
    val model: String,
    val appVersion: String,
    val createdAt: Long,
    val lastActiveAt: Long,
    val ipCountry: String? = null,
    val city: String? = null,
    val region: String? = null,
    val approxLat: Double? = null,
    val approxLon: Double? = null,
    val fcmToken: String? = null,
    val revoked: Boolean = false
)
