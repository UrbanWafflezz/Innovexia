package com.example.innovexia.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Settings tab enum
 */
enum class SettingsTab(val title: String) {
    Account("Account"),
    AI("AI"),
    Privacy("Privacy"),
    Notifications("Notifications"),
    SystemHealth("System Health"),
    Help("Help")
}

/**
 * ViewModel for managing Settings UI state
 */
class SettingsViewModel : ViewModel() {

    private val _tab = MutableStateFlow(SettingsTab.Account)
    val tab: StateFlow<SettingsTab> = _tab

    fun setTab(t: SettingsTab) {
        _tab.value = t
    }
}
