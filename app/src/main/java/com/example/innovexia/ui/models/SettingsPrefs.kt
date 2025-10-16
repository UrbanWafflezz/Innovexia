package com.example.innovexia.ui.models

import android.os.Parcelable
import androidx.compose.runtime.Stable
import kotlinx.parcelize.Parcelize

@Parcelize
@Stable
data class SettingsPrefs(
    val hideSensitivePreviews: Boolean = false,
    val typingIndicator: Boolean = true,
    val sendWithEnter: Boolean = false,
    val autoScrollOnNewMessages: Boolean = true
) : Parcelable
