package com.example.innovexia.ui.sheets.profile.tabs

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.core.permissions.PermissionHelper
import com.example.innovexia.core.permissions.rememberLocationPermissionLauncher
import com.example.innovexia.core.session.SessionRecorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import com.example.innovexia.ui.sheets.profile.EditProfileDialog
import com.example.innovexia.ui.sheets.profile.ProfileViewModel
import com.example.innovexia.ui.theme.InnovexiaColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Profile tab showing user avatar, info, edit controls, security, and sessions.
 * Now includes merged Security tab functionality.
 */
@Composable
fun ProfileTab(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    viewModel: ProfileViewModel? = null,
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val vm = viewModel ?: viewModel { ProfileViewModel(context) }
    val user by vm.user.collectAsState()
    val busy by vm.busy.collectAsState()
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }

    // Security-related state
    val securityViewModel: SecurityViewModel = viewModel { SecurityViewModel(context.applicationContext as android.app.Application) }
    val scope = rememberCoroutineScope()
    val securityBusy by securityViewModel.busy.collectAsState()
    val sessions by securityViewModel.sessions.collectAsState()
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    // Handle success/error messages from profile VM
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

    // Handle messages from security VM
    LaunchedEffect(Unit) {
        securityViewModel.message.collect { msg ->
            snackbarHost.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Load sessions
    LaunchedEffect(Unit) {
        securityViewModel.loadSessions()
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
            Button(
                onClick = { showEditDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    contentColor = Color.White,
                    disabledContainerColor = if (darkTheme) Color(0xFF334155) else Color(0xFFCBD5E1),
                    disabledContentColor = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Change photo button
            OutlinedButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !busy,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                    disabledContentColor = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (!busy) {
                        if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                    } else {
                        if (darkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Change Photo",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

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

            // ===== SECURITY SECTION (MERGED FROM SECURITY TAB) =====

            Spacer(Modifier.height(16.dp))

            // Password Management Section
            SectionHeader(
                text = "Password & Authentication",
                darkTheme = darkTheme
            )

            Spacer(Modifier.height(12.dp))

            SecurityActionCard(
                icon = Icons.Default.Lock,
                title = "Change Password",
                subtitle = "Update your account password",
                darkTheme = darkTheme,
                onClick = { showChangePasswordDialog = true },
                enabled = !securityBusy
            )

            Spacer(Modifier.height(8.dp))

            SecurityActionCard(
                icon = Icons.Default.Email,
                title = "Send Password Reset Link",
                subtitle = "Receive a reset link via email",
                darkTheme = darkTheme,
                onClick = { securityViewModel.sendPasswordReset() },
                enabled = !securityBusy
            )

            Spacer(Modifier.height(16.dp))

            // Account Actions Section
            SectionHeader(
                text = "Account Actions",
                darkTheme = darkTheme
            )

            Spacer(Modifier.height(12.dp))

            SecurityActionCard(
                icon = Icons.Default.ExitToApp,
                title = "Sign Out",
                subtitle = "Sign out from this device",
                darkTheme = darkTheme,
                onClick = {
                    securityViewModel.signOut(onSignedOut = onSignOut)
                },
                enabled = !securityBusy
            )

            Spacer(Modifier.height(8.dp))

            SecurityActionCard(
                icon = Icons.Default.Delete,
                title = "Delete Account",
                subtitle = "Permanently delete your account and all data",
                darkTheme = darkTheme,
                onClick = { showDeleteAccountDialog = true },
                enabled = !securityBusy,
                isDanger = true
            )

            Spacer(Modifier.height(16.dp))

            // Sessions Section
            SectionHeader(
                text = "Active Sessions (Last 30)",
                subtitle = "Tap a session to view details",
                darkTheme = darkTheme
            )

            Spacer(Modifier.height(12.dp))

            SessionsLocationPrompt(
                darkTheme = darkTheme,
                securityViewModel = securityViewModel
            )

            if (sessions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                sessions.forEach { session ->
                    SessionCard(
                        session = session,
                        darkTheme = darkTheme
                    )
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "No active sessions found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
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

    // Security Dialogs
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            darkTheme = darkTheme,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { newPassword, currentPassword ->
                val email = Firebase.auth.currentUser?.email ?: ""
                securityViewModel.changePassword(newPassword, email, currentPassword)
                showChangePasswordDialog = false
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            darkTheme = darkTheme,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = { currentPassword ->
                val email = Firebase.auth.currentUser?.email ?: ""
                scope.launch {
                    securityViewModel.deleteAccount(email, currentPassword)
                    showDeleteAccountDialog = false
                }
            }
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

@Composable
private fun SectionHeader(
    text: String,
    subtitle: String? = null,
    darkTheme: Boolean
) {
    Column {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (darkTheme) InnovexiaColors.DarkTextPrimary
            else InnovexiaColors.LightTextPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                else InnovexiaColors.LightTextSecondary
            )
        }
    }
}

@Composable
private fun SecurityActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    darkTheme: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isDanger: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button),
        color = if (darkTheme) InnovexiaColors.DarkSurfaceElevated
        else InnovexiaColors.LightSurfaceElevated,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDanger) Color(0x22FF3B30)
                        else if (darkTheme) InnovexiaColors.DarkBackground
                        else InnovexiaColors.LightBackground
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDanger) Color(0xFFFF3B30)
                    else if (darkTheme) InnovexiaColors.Gold
                    else InnovexiaColors.Gold,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isDanger) Color(0xFFFF3B30)
                    else if (darkTheme) InnovexiaColors.DarkTextPrimary
                    else InnovexiaColors.LightTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (darkTheme) InnovexiaColors.DarkTextSecondary
                else InnovexiaColors.LightTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionItem,
    darkTheme: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron_rotation"
    )

    val now = System.currentTimeMillis()
    val isRecent = (now - session.lastActiveAt) < 24 * 60 * 60 * 1000 // 24h

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        color = if (darkTheme) InnovexiaColors.DarkSurfaceElevated
        else InnovexiaColors.LightSurfaceElevated,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecent) Color(0xFF34C759) else Color(0xFF8E8E93)
                        )
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = session.device,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                        else InnovexiaColors.LightTextPrimary
                    )
                    Text(
                        text = getRelativeTime(session.lastActiveAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(rotationAngle)
                )
            }

            // Expanded content
            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(
                        color = if (darkTheme) Color(0x22FFFFFF) else Color(0x22000000),
                        thickness = 1.dp
                    )

                    SessionDetailRow(
                        label = "Location",
                        value = when {
                            session.city != null && session.region != null -> "${session.city}, ${session.region}"
                            session.city != null -> session.city
                            session.region != null -> session.region
                            else -> "Location unavailable"
                        },
                        darkTheme = darkTheme
                    )

                    if (session.approxLat != null && session.approxLon != null) {
                        SessionDetailRow(
                            label = "Coordinates",
                            value = "${session.approxLat}, ${session.approxLon}",
                            darkTheme = darkTheme
                        )
                    }

                    SessionDetailRow(
                        label = "First seen",
                        value = formatAbsoluteTime(session.createdAt),
                        darkTheme = darkTheme
                    )

                    SessionDetailRow(
                        label = "Last active",
                        value = formatAbsoluteTime(session.lastActiveAt),
                        darkTheme = darkTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionDetailRow(
    label: String,
    value: String,
    darkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (darkTheme) InnovexiaColors.DarkTextSecondary
            else InnovexiaColors.LightTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = if (darkTheme) InnovexiaColors.DarkTextPrimary
            else InnovexiaColors.LightTextPrimary
        )
    }
}

@Composable
private fun SessionsLocationPrompt(
    darkTheme: Boolean,
    securityViewModel: SecurityViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember {
        mutableStateOf(PermissionHelper.hasLocationPermission(context))
    }

    val locationLauncher = rememberLocationPermissionLauncher { granted ->
        hasLocationPermission = granted
        if (granted) {
            scope.launch {
                val location = PermissionHelper.getCurrentLocation(context)
                SessionRecorder.upsertCurrentSession(context, location)
                securityViewModel.loadSessions()
            }
        }
    }

    if (!hasLocationPermission) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            color = if (darkTheme) Color(0x22FFB800) else Color(0x22FFB800),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = InnovexiaColors.Gold,
                    modifier = Modifier.size(20.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Add location to sessions",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                        else InnovexiaColors.LightTextPrimary
                    )
                    Text(
                        text = "Enable location to see approximate city/region for each session",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary,
                        fontSize = 11.sp
                    )
                }
                TextButton(
                    onClick = { locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                ) {
                    Text(
                        text = "Enable",
                        color = InnovexiaColors.Gold,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

// Helper functions

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Active now"
        diff < 3600_000 -> "Active ${diff / 60_000}m ago"
        diff < 86400_000 -> "Active ${diff / 3600_000}h ago"
        diff < 604800_000 -> "Active ${diff / 86400_000}d ago"
        else -> "Active ${diff / 604800_000}w ago"
    }
}

private fun formatAbsoluteTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
