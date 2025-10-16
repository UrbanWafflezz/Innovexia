package com.example.innovexia.core.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Dialog shown when a new app update is available
 */
@Composable
fun UpdateAvailableDialog(
    updateInfo: UpdateInfo,
    downloadState: ApkDownloader.DownloadState = ApkDownloader.DownloadState.Idle,
    forceUpdate: Boolean = false,
    onUpdateNow: () -> Unit,
    onRemindLater: (hours: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isDownloading = downloadState is ApkDownloader.DownloadState.Downloading
    var showRemindMenu by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = if (forceUpdate) {
            {} // Can't dismiss if forced update
        } else {
            onDismiss
        },
        properties = DialogProperties(
            dismissOnBackPress = !forceUpdate,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = InnovexiaColors.DarkSurface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = Icons.Rounded.NewReleases,
                    contentDescription = "Update available",
                    tint = InnovexiaColors.BlueAccent,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Update Available",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = InnovexiaColors.DarkTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Version info
                Text(
                    text = "Version ${updateInfo.latestVersion} is now available",
                    fontSize = 14.sp,
                    color = InnovexiaColors.DarkTextSecondary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "You're currently on version ${updateInfo.currentVersion}",
                    fontSize = 12.sp,
                    color = InnovexiaColors.DarkTextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Release notes (if available)
                if (!updateInfo.releaseNotes.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = InnovexiaColors.DarkSurfaceElevated.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "What's New:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = InnovexiaColors.BlueAccent,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = updateInfo.releaseNotes,
                                fontSize = 13.sp,
                                color = InnovexiaColors.DarkTextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // APK size info (if available)
                if (updateInfo.apkSize != null) {
                    val sizeMB = updateInfo.apkSize / (1024 * 1024f)
                    Text(
                        text = "Download size: %.1f MB".format(sizeMB),
                        fontSize = 12.sp,
                        color = InnovexiaColors.DarkTextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Download progress (if downloading)
                when (downloadState) {
                    is ApkDownloader.DownloadState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Downloading update...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = InnovexiaColors.BlueAccent
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = downloadState.progress / 100f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = InnovexiaColors.BlueAccent,
                                trackColor = InnovexiaColors.DarkSurfaceElevated
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${downloadState.progress}% (${formatBytes(downloadState.bytesDownloaded)} / ${formatBytes(downloadState.totalBytes)})",
                                fontSize = 12.sp,
                                color = InnovexiaColors.DarkTextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    is ApkDownloader.DownloadState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Red.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Download failed: ${downloadState.message}",
                                fontSize = 12.sp,
                                color = Color.Red,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    else -> {}
                }

                // Force update warning
                if (forceUpdate) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = InnovexiaColors.BlueAccent.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = InnovexiaColors.BlueAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Update required: 7 days have passed",
                                fontSize = 12.sp,
                                color = InnovexiaColors.BlueAccent,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Update Now button (primary)
                    Button(
                        onClick = onUpdateNow,
                        enabled = !isDownloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InnovexiaColors.BlueAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDownloading) "Downloading..." else "Update Now",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Remind Later button (secondary) - only if not forced update
                    if (!forceUpdate) {
                        Box {
                            OutlinedButton(
                                onClick = { showRemindMenu = true },
                                enabled = !isDownloading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = InnovexiaColors.DarkTextSecondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Remind Me Later",
                                    fontSize = 14.sp
                                )
                            }

                            // Dropdown menu for remind later options
                            DropdownMenu(
                                expanded = showRemindMenu,
                                onDismissRequest = { showRemindMenu = false },
                                modifier = Modifier.background(InnovexiaColors.DarkSurfaceElevated)
                            ) {
                                // Next app launch (0 hours - clear remind later)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Next time I open the app",
                                            color = InnovexiaColors.DarkTextPrimary,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        showRemindMenu = false
                                        onRemindLater(0)
                                    }
                                )
                                Divider(color = InnovexiaColors.DarkSurface)

                                // 4 hours
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "In 4 hours",
                                            color = InnovexiaColors.DarkTextPrimary,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        showRemindMenu = false
                                        onRemindLater(4)
                                    }
                                )
                                Divider(color = InnovexiaColors.DarkSurface)

                                // 8 hours
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "In 8 hours",
                                            color = InnovexiaColors.DarkTextPrimary,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        showRemindMenu = false
                                        onRemindLater(8)
                                    }
                                )
                                Divider(color = InnovexiaColors.DarkSurface)

                                // 24 hours
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "In 24 hours",
                                            color = InnovexiaColors.DarkTextPrimary,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        showRemindMenu = false
                                        onRemindLater(24)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format bytes to human-readable string
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
