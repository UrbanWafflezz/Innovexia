package com.example.innovexia.ui.sheets.profile.tabs

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.innovexia.ui.theme.InnovexiaColors

@Composable
fun ChangePasswordDialog(
    darkTheme: Boolean = isSystemInDarkTheme(),
    onDismiss: () -> Unit,
    onConfirm: (newPassword: String, currentPassword: String?) -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = if (darkTheme) InnovexiaColors.DarkSurface
            else InnovexiaColors.LightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Change Password",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                    else InnovexiaColors.LightTextPrimary
                )

                Text(
                    text = "For security, please enter your current password to confirm this change.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary
                )

                // Current Password
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; errorMessage = null },
                    label = { Text("Current Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showCurrentPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                            Icon(
                                imageVector = if (showCurrentPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InnovexiaColors.Gold,
                        unfocusedBorderColor = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                )

                // New Password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; errorMessage = null },
                    label = { Text("New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showNewPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                imageVector = if (showNewPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InnovexiaColors.Gold,
                        unfocusedBorderColor = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                )

                // Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    label = { Text("Confirm New Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InnovexiaColors.Gold,
                        unfocusedBorderColor = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                )

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF3B30),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (darkTheme) InnovexiaColors.Gold
                            else InnovexiaColors.Gold
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            when {
                                currentPassword.isBlank() -> {
                                    errorMessage = "Current password is required"
                                }
                                newPassword.isBlank() -> {
                                    errorMessage = "New password is required"
                                }
                                newPassword.length < 6 -> {
                                    errorMessage = "Password must be at least 6 characters"
                                }
                                newPassword != confirmPassword -> {
                                    errorMessage = "Passwords do not match"
                                }
                                else -> {
                                    onConfirm(newPassword, currentPassword.ifBlank { null })
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InnovexiaColors.Gold
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteAccountDialog(
    darkTheme: Boolean = isSystemInDarkTheme(),
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String?) -> Unit
) {
    var confirmationText by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = if (darkTheme) InnovexiaColors.DarkSurface
            else InnovexiaColors.LightSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFFFF3B30)
                )

                Text(
                    text = "This action cannot be undone. All your data will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                    else InnovexiaColors.LightTextPrimary
                )

                Text(
                    text = "Type DELETE to confirm:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary
                )

                // Confirmation text field
                OutlinedTextField(
                    value = confirmationText,
                    onValueChange = { confirmationText = it; errorMessage = null },
                    label = { Text("Type DELETE") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF3B30),
                        unfocusedBorderColor = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                )

                // Current Password
                Text(
                    text = "Enter your password to confirm:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; errorMessage = null },
                    label = { Text("Current Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF3B30),
                        unfocusedBorderColor = if (darkTheme) InnovexiaColors.DarkTextSecondary
                        else InnovexiaColors.LightTextSecondary
                    )
                )

                // Error message
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF3B30),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (darkTheme) InnovexiaColors.DarkTextPrimary
                            else InnovexiaColors.LightTextPrimary
                        )
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            when {
                                confirmationText != "DELETE" -> {
                                    errorMessage = "Please type DELETE to confirm"
                                }
                                currentPassword.isBlank() -> {
                                    errorMessage = "Password is required"
                                }
                                else -> {
                                    onConfirm(currentPassword.ifBlank { null })
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF3B30)
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
