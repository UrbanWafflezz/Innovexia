package com.example.innovexia.ui.sheets.profile.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.ui.glass.GlassButton
import com.example.innovexia.ui.glass.GlassButtonStyle
import com.example.innovexia.ui.sheets.profile.EditProfileDialog
import com.example.innovexia.ui.sheets.profile.ProfileViewModel
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Profile tab showing user avatar, info, and edit controls.
 */
@Composable
fun ProfileTab(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    viewModel: ProfileViewModel? = null
) {
    val context = LocalContext.current
    val vm = viewModel ?: viewModel { ProfileViewModel(context) }
    val user by vm.user.collectAsState()
    val busy by vm.busy.collectAsState()
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    // Handle success/error messages
    LaunchedEffect(Unit) {
        vm.success.collect { message ->
            snackbarHost.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(Unit) {
        vm.error.collect { message ->
            snackbarHost.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
        }
    }

    // Photo picker launcher
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                vm.uploadAvatar(uri)
            }
        }
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero profile card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (darkTheme) InnovexiaColors.DarkSurface
                        else InnovexiaColors.LightSurface
                    )
                    .border(
                        1.dp,
                        if (darkTheme) InnovexiaColors.DarkBorder
                        else InnovexiaColors.LightBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar with gold ring
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        // Gold ring
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .border(
                                    width = 2.dp,
                                    color = InnovexiaColors.Gold,
                                    shape = CircleShape
                                )
                        )

                        // Avatar (showing initials - image loading can be added later with Coil)
                        val displayName = user?.displayName ?: user?.email ?: "User"
                        val initials = displayName.take(2).uppercase()

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(InnovexiaColors.BlueAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = InnovexiaColors.White
                            )
                        }

                        // Loading indicator
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(72.dp),
                                color = InnovexiaColors.Gold,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    // User info
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = user?.displayName ?: "User",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp
                            ),
                            color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                            else InnovexiaColors.LightTextPrimary
                        )

                        user?.email?.let { email ->
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp
                                ),
                                color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                                else InnovexiaColors.LightTextSecondary
                            )
                        }
                    }
                }
            }

            // Edit profile button
            GlassButton(
                text = "Edit profile",
                onClick = { showEditDialog = true },
                style = GlassButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
                darkTheme = darkTheme,
                enabled = !busy
            )

            // Change photo button
            GlassButton(
                text = "Change photo",
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                style = GlassButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
                darkTheme = darkTheme,
                enabled = !busy
            )

            Spacer(Modifier.height(8.dp))

            // Additional info/links section
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                else InnovexiaColors.LightTextPrimary
            )

            InfoCard(
                title = "User ID",
                value = user?.uid?.take(12) + "..." ?: "Not available",
                darkTheme = darkTheme
            )

            InfoCard(
                title = "Account created",
                value = user?.metadata?.creationTimestamp?.let {
                    java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(it))
                } ?: "Unknown",
                darkTheme = darkTheme
            )
        }

        // Snackbar host
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Edit profile dialog
    if (showEditDialog && user != null) {
        EditProfileDialog(
            currentName = user?.displayName ?: "",
            currentEmail = user?.email ?: "",
            onDismiss = { showEditDialog = false },
            onSave = { newName ->
                vm.updateDisplayName(newName)
            },
            darkTheme = darkTheme
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    darkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (darkTheme) InnovexiaColors.DarkSurfaceElevated
                else InnovexiaColors.LightSurfaceElevated
            )
            .border(
                1.dp,
                if (darkTheme) InnovexiaColors.DarkBorder
                else InnovexiaColors.LightBorder,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                else InnovexiaColors.LightTextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                else InnovexiaColors.LightTextPrimary
            )
        }
    }
}
