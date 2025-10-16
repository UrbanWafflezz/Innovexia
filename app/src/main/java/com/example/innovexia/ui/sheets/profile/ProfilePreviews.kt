package com.example.innovexia.ui.sheets.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.innovexia.ui.sheets.profile.tabs.ProfileTab
import com.example.innovexia.ui.sheets.profile.tabs.SecurityTab
import com.example.innovexia.ui.sheets.profile.tabs.SubscriptionsTab

/**
 * Preview composables for the Profile Sheet components.
 */

@Preview(name = "Profile Tab - Dark", showBackground = true)
@Composable
private fun ProfileTabPreview_Dark() {
    ProfileTab(darkTheme = true)
}

@Preview(name = "Profile Tab - Light", showBackground = true)
@Composable
private fun ProfileTabPreview_Light() {
    ProfileTab(darkTheme = false)
}

@Preview(name = "Subscriptions Tab - Dark", showBackground = true)
@Composable
private fun SubscriptionsTabPreview_Dark() {
    SubscriptionsTab(darkTheme = true)
}

@Preview(name = "Subscriptions Tab - Light", showBackground = true)
@Composable
private fun SubscriptionsTabPreview_Light() {
    SubscriptionsTab(darkTheme = false)
}

@Preview(name = "Security Tab - Dark", showBackground = true)
@Composable
private fun SecurityTabPreview_Dark() {
    SecurityTab(
        darkTheme = true,
        onSignOut = {},
        viewModel = null as? ProfileViewModel ?: return
    )
}

@Preview(name = "Security Tab - Light", showBackground = true)
@Composable
private fun SecurityTabPreview_Light() {
    SecurityTab(
        darkTheme = false,
        onSignOut = {},
        viewModel = null as? ProfileViewModel ?: return
    )
}

@Preview(name = "Edit Profile Dialog - Dark", showBackground = true)
@Composable
private fun EditProfileDialogPreview_Dark() {
    EditProfileDialog(
        currentName = "John Doe",
        currentEmail = "john.doe@example.com",
        onDismiss = {},
        onSave = {},
        darkTheme = true
    )
}

@Preview(name = "Edit Profile Dialog - Light", showBackground = true)
@Composable
private fun EditProfileDialogPreview_Light() {
    EditProfileDialog(
        currentName = "John Doe",
        currentEmail = "john.doe@example.com",
        onDismiss = {},
        onSave = {},
        darkTheme = false
    )
}
