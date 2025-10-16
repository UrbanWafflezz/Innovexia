package com.example.innovexia.core.update

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    onUpdateNow: () -> Unit,
    onRemindLater: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
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
                    color = InnovexiaColors.DarkTextTertiary,
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
                        color = InnovexiaColors.DarkTextTertiary,
                        textAlign = TextAlign.Center
                    )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InnovexiaColors.BlueAccent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Update Now",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Remind Later button (secondary)
                    OutlinedButton(
                        onClick = onRemindLater,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = InnovexiaColors.DarkTextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Remind Me Later",
                            fontSize = 14.sp
                        )
                    }

                    // Skip button (tertiary)
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Text(
                            text = "Skip This Version",
                            fontSize = 12.sp,
                            color = InnovexiaColors.DarkTextTertiary
                        )
                    }
                }
            }
        }
    }
}
