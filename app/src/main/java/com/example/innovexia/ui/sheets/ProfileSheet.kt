package com.example.innovexia.ui.sheets

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.ui.auth.AuthPanel
import com.example.innovexia.ui.sheets.profile.ProfileTab
import com.example.innovexia.ui.sheets.profile.ProfileViewModel
import com.example.innovexia.ui.sheets.profile.tabs.CloudSyncTab
import com.example.innovexia.ui.sheets.profile.tabs.ProfileTab as ProfileTabContent
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors
import com.example.innovexia.ui.theme.getBackgroundGradient
import com.example.innovexia.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

/**
 * Profile Dialog with proper scrolling behavior
 * - Dialog has bounded height
 * - Header + tabs are fixed
 * - Only tab body scrolls (LazyColumn in each tab)
 */
@Composable
fun ProfileDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: ProfileTab? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: com.example.innovexia.ui.viewmodels.HomeViewModel? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as InnovexiaApplication
    val profileViewModel = viewModel { ProfileViewModel(context) }
    val signedIn by authViewModel.signedIn.collectAsState()
    val scope = rememberCoroutineScope()

    // Refresh auth state when sheet is opened
    LaunchedEffect(Unit) {
        authViewModel.refresh()
        profileViewModel.refreshUser()
    }

    // Set initial tab if provided
    LaunchedEffect(initialTab) {
        initialTab?.let { profileViewModel.setTab(it) }
    }

    // Note: Don't auto-dismiss when signedIn changes here - let AuthPanel handle it via onSignInSuccess
    // Only refresh profile data when already signed in (e.g., when opening profile settings while logged in)
    LaunchedEffect(signedIn) {
        if (signedIn) {
            profileViewModel.refreshUser()
        }
    }

    Dialog(
        onDismissRequest = {}, // Only X button can close
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // Outer surface (rounded card) with gradient background
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.88f) // Bounded height
                .imePadding()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(20.dp))
                .background(brush = getBackgroundGradient(darkTheme))
                .border(
                    1.dp,
                    if (darkTheme) Color(0xFF253041).copy(alpha = 0.6f) else Color(0xFFE7EDF5).copy(alpha = 0.6f),
                    RoundedCornerShape(20.dp)
                )
        ) {
            // Layout: header + tabs fixed; body scrolls
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ---- Header (fixed) ----
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Only show "Profile" title when user is signed in
                    if (signedIn) {
                        Column {
                            Text(
                                "Profile",
                                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            profileViewModel.user.collectAsState().value?.email?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (signedIn) {
                    // ---- Tabs (fixed) ----
                    ProfileTabs(
                        selected = profileViewModel.tab.collectAsState().value,
                        onSelect = profileViewModel::setTab,
                        darkTheme = darkTheme
                    )

                    HorizontalDivider(
                        Modifier.padding(top = 8.dp, bottom = 8.dp),
                        color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f)
                        else LightColors.SecondaryText.copy(alpha = 0.2f)
                    )

                    // ---- Scrollable body (the ONLY scroller) ----
                    Box(Modifier.weight(1f)) {
                        when (profileViewModel.tab.collectAsState().value) {
                            ProfileTab.Profile -> ProfileTabContent(
                                darkTheme = darkTheme,
                                viewModel = profileViewModel,
                                onSignOut = {
                                    authViewModel.signOut()
                                    profileViewModel.refreshUser()
                                    homeViewModel?.clearAllChatState()
                                    onDismiss()
                                }
                            )
                            ProfileTab.CloudSync -> CloudSyncTab(
                                darkTheme = darkTheme,
                                authViewModel = authViewModel
                            )
                        }
                    }
                } else {
                    // Not signed in - show auth panel
                    Box(Modifier.weight(1f)) {
                        AuthPanel(
                            onSignInSuccess = {
                                // Clear chat state when signing in with a different account
                                homeViewModel?.clearAllChatState()
                                // Force sync entitlements from Firestore
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    app.entitlementsRepo.forceSync()
                                }
                                // Dismiss the dialog to return to chat page
                                onDismiss()
                            },
                            darkTheme = darkTheme,
                            viewModel = authViewModel
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tabs row (fixed at top)
 */
@Composable
private fun ProfileTabs(
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
    darkTheme: Boolean
) {
    val tabs = listOf(
        ProfileTab.Profile,
        ProfileTab.CloudSync
    )

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0),
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        indicator = { tabPositions ->
            val idx = tabs.indexOf(selected)
            if (idx >= 0) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[idx]),
                    color = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                    height = 2.dp
                )
            }
        },
        divider = {}
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        tab.title,
                        color = if (tab == selected) {
                            if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                        } else {
                            if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        },
                        fontWeight = if (tab == selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            )
        }
    }
}

// Backward compatibility: Keep old ProfileSheet name
@Deprecated(
    "Use ProfileDialog instead",
    ReplaceWith("ProfileDialog(onDismiss, modifier, initialTab, darkTheme, authViewModel, homeViewModel)")
)
@Composable
fun ProfileSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialTab: ProfileTab? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: com.example.innovexia.ui.viewmodels.HomeViewModel? = null
) {
    ProfileDialog(
        onDismiss = onDismiss,
        modifier = modifier,
        initialTab = initialTab,
        darkTheme = darkTheme,
        authViewModel = authViewModel,
        homeViewModel = homeViewModel
    )
}
