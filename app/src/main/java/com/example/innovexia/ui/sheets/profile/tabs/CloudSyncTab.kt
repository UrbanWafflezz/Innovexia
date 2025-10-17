package com.example.innovexia.ui.sheets.profile.tabs

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.InnovexiaApplication
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.sheets.CloudRestoreSheet
import com.example.innovexia.ui.viewmodels.AuthViewModel
import com.example.innovexia.ui.viewmodels.CloudSyncViewModel
import com.example.innovexia.data.preferences.UserPreferences
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Enhanced Cloud Sync settings tab with:
 * - Manual sync button
 * - Last sync time display
 * - Sync statistics (chats/messages)
 * - Auto-sync status (hourly)
 * - Progress indicators
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncTab(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    authViewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as InnovexiaApplication

    val cloudSyncVm: CloudSyncViewModel = viewModel { CloudSyncViewModel(context) }
    val authVm = authViewModel ?: viewModel { AuthViewModel(app) }

    val signedIn by authVm.signedIn.collectAsState()
    val syncEnabled by cloudSyncVm.syncEnabled.collectAsState()
    val syncInProgress by cloudSyncVm.syncInProgress.collectAsState()
    val syncProgress by cloudSyncVm.syncProgress.collectAsState()
    val error by cloudSyncVm.error.collectAsState()
    val lastSyncTime by cloudSyncVm.lastSyncTime.collectAsState()
    val lastSyncChatCount by cloudSyncVm.lastSyncChatCount.collectAsState()
    val lastSyncMessageCount by cloudSyncVm.lastSyncMessageCount.collectAsState()

    val snackbarHost = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showRestoreSheet by rememberSaveable { mutableStateOf(false) }

    // User preferences for cloud restore button - use singleton from Application
    val userPrefs = app.userPreferences
    val scope = rememberCoroutineScope()
    val hideRestoreButton by userPrefs.hideCloudRestoreButton.collectAsState(initial = false)
    val cloudDeleteEnabled by userPrefs.cloudDeleteEnabled.collectAsState(initial = true)

    // Show error snackbar
    LaunchedEffect(error) {
        error?.let {
            snackbarHost.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            cloudSyncVm.clearError()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (syncEnabled) Icons.Rounded.CloudQueue else Icons.Rounded.CloudOff,
                    contentDescription = "Cloud sync",
                    tint = if (syncEnabled) InnovexiaColors.BlueAccent else InnovexiaColors.Gold,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Cloud Sync",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                        else InnovexiaColors.LightTextPrimary
                    )
                    Text(
                        text = if (signedIn) "Sync your chats across devices"
                        else "Sign in to enable cloud sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Sync toggle
            SettingCard(
                title = "Enable Cloud Sync",
                description = if (signedIn)
                    "Backup and sync chats to Firebase Cloud"
                else
                    "Available for signed-in users only",
                darkTheme = darkTheme,
                trailing = {
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = { cloudSyncVm.toggleSync(it) },
                        enabled = signedIn && !syncInProgress,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                            checkedTrackColor = if (darkTheme) Color(0xFF60A5FA).copy(alpha = 0.5f) else Color(0xFF3B82F6).copy(alpha = 0.5f),
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                            uncheckedTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                            uncheckedBorderColor = Color.Transparent,
                            disabledCheckedThumbColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                            disabledCheckedTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                            disabledUncheckedThumbColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                            disabledUncheckedTrackColor = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        )
                    )
                }
            )

            // Cloud delete toggle
            if (syncEnabled) {
                SettingCard(
                    title = "Delete from Cloud",
                    description = "Also delete chats from Firebase when deleted locally",
                    darkTheme = darkTheme,
                    trailing = {
                        Switch(
                            checked = cloudDeleteEnabled,
                            onCheckedChange = {
                                scope.launch {
                                    userPrefs.setCloudDeleteEnabled(it)
                                }
                            },
                            enabled = signedIn && syncEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                                checkedTrackColor = if (darkTheme) Color(0xFF60A5FA).copy(alpha = 0.5f) else Color(0xFF3B82F6).copy(alpha = 0.5f),
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                                uncheckedTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                                uncheckedBorderColor = Color.Transparent,
                                disabledCheckedThumbColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                                disabledCheckedTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                                disabledUncheckedThumbColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                                disabledUncheckedTrackColor = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            )
                        )
                    }
                )
            }

            // Restore button visibility toggle
            if (signedIn) {
                SettingCard(
                    title = "Show Restore Button in Menu",
                    description = "Display cloud restore button in the side menu",
                    darkTheme = darkTheme,
                    trailing = {
                        Switch(
                            checked = !hideRestoreButton,
                            onCheckedChange = {
                                scope.launch {
                                    userPrefs.setHideCloudRestoreButton(!it)
                                }
                            },
                            enabled = signedIn,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                                checkedTrackColor = if (darkTheme) Color(0xFF60A5FA).copy(alpha = 0.5f) else Color(0xFF3B82F6).copy(alpha = 0.5f),
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                                uncheckedTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                                uncheckedBorderColor = Color.Transparent,
                                disabledCheckedThumbColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                                disabledCheckedTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                                disabledUncheckedThumbColor = if (darkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                                disabledUncheckedTrackColor = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            )
                        )
                    }
                )
            }

            // Sync status card (only when enabled)
            if (syncEnabled) {
                SyncStatusCard(
                    lastSyncTime = lastSyncTime,
                    chatCount = lastSyncChatCount,
                    messageCount = lastSyncMessageCount,
                    darkTheme = darkTheme
                )
            }

            // Sync progress
            if (syncInProgress) {
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = InnovexiaColors.BlueAccent
                                )
                                Text(
                                    text = "Syncing...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                                    else InnovexiaColors.LightTextPrimary
                                )
                            }
                            if (syncProgress.second > 0) {
                                Text(
                                    text = "${syncProgress.first} / ${syncProgress.second}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                                    else InnovexiaColors.LightTextSecondary
                                )
                            }
                        }
                        if (syncProgress.second > 0) {
                            LinearProgressIndicator(
                                progress = syncProgress.first.toFloat() / syncProgress.second.toFloat(),
                                modifier = Modifier.fillMaxWidth(),
                                color = InnovexiaColors.BlueAccent
                            )
                        }
                    }
                }
            }

            // Manual sync button
            if (syncEnabled && !syncInProgress) {
                Button(
                    onClick = { cloudSyncVm.manualSync() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Sync",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = "Sync Now",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Restore from cloud button
            if (signedIn && !syncInProgress) {
                OutlinedButton(
                    onClick = { showRestoreSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = "Restore",
                        tint = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = "Restore from Cloud",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Auto-sync info
            if (syncEnabled) {
                InfoBox(
                    darkTheme = darkTheme,
                    icon = Icons.Rounded.Schedule,
                    text = "Auto-sync runs every hour when connected to the internet."
                )
            }

            // Info section
            InfoBox(
                darkTheme = darkTheme,
                text = """
                    Cloud Sync uses Firebase Firestore and Cloud Storage to backup your chats.

                    • Firestore stores chat structure and metadata
                    • Storage handles large messages and attachments
                    • All data is encrypted and private to your account
                    • Guest mode data is never synced
                """.trimIndent()
            )

            Spacer(Modifier.height(16.dp))

            // Danger zone
            if (syncEnabled) {
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFFFF3B30)
                )

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = signedIn && syncEnabled && !syncInProgress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White,
                        disabledContainerColor = if (darkTheme) Color(0xFF334155) else Color(0xFFCBD5E1),
                        disabledContentColor = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(
                        text = "Delete Cloud Data",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
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

    // Cloud Restore Sheet
    if (showRestoreSheet) {
        CloudRestoreSheet(
            onDismiss = { showRestoreSheet = false },
            darkTheme = darkTheme
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Delete",
                    tint = InnovexiaColors.Error
                )
            },
            title = {
                Text("Delete Cloud Data?")
            },
            text = {
                Text(
                    "This will permanently delete all your chats and messages from the cloud. " +
                            "Your local data will NOT be affected. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        cloudSyncVm.deleteCloudData()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Delete",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (darkTheme) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun SyncStatusCard(
    lastSyncTime: Long,
    chatCount: Int,
    messageCount: Int,
    darkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (darkTheme)
                    InnovexiaColors.BlueAccent.copy(alpha = 0.1f)
                else
                    InnovexiaColors.BlueAccent.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                InnovexiaColors.BlueAccent.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sync Status",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                    else InnovexiaColors.LightTextPrimary
                )
            }

            // Last sync time
            if (lastSyncTime > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudQueue,
                        contentDescription = "Last sync",
                        tint = InnovexiaColors.BlueAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Last synced ${formatTimeAgo(lastSyncTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudQueue,
                        contentDescription = "Never synced",
                        tint = InnovexiaColors.BlueAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Not synced yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                }
            }

            // Statistics
            if (chatCount > 0 || messageCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatItem(
                        value = chatCount.toString(),
                        label = "chats",
                        darkTheme = darkTheme
                    )
                    StatItem(
                        value = messageCount.toString(),
                        label = "messages",
                        darkTheme = darkTheme
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    darkTheme: Boolean
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = InnovexiaColors.BlueAccent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (darkTheme) InnovexiaColors.DarkTextSecondary
            else InnovexiaColors.LightTextSecondary
        )
    }
}

@Composable
private fun SettingCard(
    title: String,
    description: String,
    darkTheme: Boolean,
    trailing: @Composable () -> Unit
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
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                    else InnovexiaColors.LightTextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary
                )
            }
            trailing()
        }
    }
}

@Composable
private fun InfoBox(
    darkTheme: Boolean,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Info
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (darkTheme)
                    InnovexiaColors.BlueAccent.copy(alpha = 0.1f)
                else
                    InnovexiaColors.BlueAccent.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                InnovexiaColors.BlueAccent.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Info",
                tint = InnovexiaColors.BlueAccent,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp
                ),
                color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                else InnovexiaColors.LightTextPrimary
            )
        }
    }
}

/**
 * Format timestamp as relative time (e.g., "5 minutes ago").
 */
private fun formatTimeAgo(timestamp: Long): String {
    if (timestamp == 0L) return "never"

    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "$minutes minute${if (minutes != 1L) "s" else ""} ago"
        hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
        days < 7 -> "$days day${if (days != 1L) "s" else ""} ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
