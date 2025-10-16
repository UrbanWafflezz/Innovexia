package com.example.innovexia.ui.sheets.profile.tabs

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.core.permissions.PermissionHelper
import com.example.innovexia.core.permissions.rememberLocationPermissionLauncher
import com.example.innovexia.core.session.SessionRecorder
import com.example.innovexia.ui.sheets.profile.ProfileViewModel
import com.example.innovexia.ui.theme.InnovexiaColors
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SecurityTab(
    darkTheme: Boolean = isSystemInDarkTheme(),
    onSignOut: () -> Unit,
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val securityViewModel: SecurityViewModel = viewModel { SecurityViewModel(context.applicationContext as android.app.Application) }
    val scope = rememberCoroutineScope()

    val busy by securityViewModel.busy.collectAsState()
    val sessions by securityViewModel.sessions.collectAsState()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar messages
    LaunchedEffect(Unit) {
        securityViewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Load sessions
    LaunchedEffect(Unit) {
        securityViewModel.loadSessions()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
        // Password Management Section
        item {
            SectionHeader(
                text = "Password & Authentication",
                darkTheme = darkTheme
            )
        }

        item {
            SecurityActionCard(
                icon = Icons.Default.Lock,
                title = "Change Password",
                subtitle = "Update your account password",
                darkTheme = darkTheme,
                onClick = { showChangePasswordDialog = true },
                enabled = !busy
            )
        }

        item {
            SecurityActionCard(
                icon = Icons.Default.Email,
                title = "Send Password Reset Link",
                subtitle = "Receive a reset link via email",
                darkTheme = darkTheme,
                onClick = { securityViewModel.sendPasswordReset() },
                enabled = !busy
            )
        }

        // Account Actions Section
        item {
            Spacer(Modifier.height(12.dp))
            SectionHeader(
                text = "Account Actions",
                darkTheme = darkTheme
            )
        }

        item {
            SecurityActionCard(
                icon = Icons.Default.ExitToApp,
                title = "Sign Out",
                subtitle = "Sign out from this device",
                darkTheme = darkTheme,
                onClick = {
                    securityViewModel.signOut(onSignedOut = onSignOut)
                },
                enabled = !busy
            )
        }

        item {
            SecurityActionCard(
                icon = Icons.Default.Delete,
                title = "Delete Account",
                subtitle = "Permanently delete your account and all data",
                darkTheme = darkTheme,
                onClick = { showDeleteAccountDialog = true },
                enabled = !busy,
                isDanger = true
            )
        }

        // Sessions Section
        item {
            Spacer(Modifier.height(12.dp))
            SectionHeader(
                text = "Active Sessions (Last 30)",
                subtitle = "Tap a session to view details",
                darkTheme = darkTheme
            )
        }

        item {
            SessionsLocationPrompt(
                darkTheme = darkTheme,
                securityViewModel = securityViewModel
            )
        }

        items(sessions, key = { it.id }) { session ->
            SessionCard(
                session = session,
                darkTheme = darkTheme
            )
        }

        if (sessions.isEmpty()) {
            item {
                Text(
                    text = "No active sessions found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }

    // Dialogs
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

                    // TODO: Sign out this device button
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
