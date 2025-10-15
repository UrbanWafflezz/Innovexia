package com.example.innovexia.ui.sheets.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.innovexia.ui.glass.GlassButton
import com.example.innovexia.ui.glass.GlassButtonStyle
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Dialog for editing user profile information.
 */
@Composable
fun EditProfileDialog(
    currentName: String,
    currentEmail: String,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    var displayName by rememberSaveable { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (darkTheme) InnovexiaColors.DarkSurface
                    else InnovexiaColors.LightSurface
                )
                .border(
                    1.dp,
                    if (darkTheme) InnovexiaColors.DarkBorder
                    else InnovexiaColors.LightBorder,
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (darkTheme) InnovexiaColors.DarkTextPrimary
                else InnovexiaColors.LightTextPrimary
            )

            // Display name field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Display Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                TextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = if (darkTheme) InnovexiaColors.DarkSurfaceElevated
                        else InnovexiaColors.LightSurfaceElevated,
                        unfocusedContainerColor = if (darkTheme) InnovexiaColors.DarkSurfaceElevated
                        else InnovexiaColors.LightSurfaceElevated,
                        focusedTextColor = if (darkTheme) InnovexiaColors.DarkTextPrimary
                        else InnovexiaColors.LightTextPrimary,
                        unfocusedTextColor = if (darkTheme) InnovexiaColors.DarkTextPrimary
                        else InnovexiaColors.LightTextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = InnovexiaColors.BlueAccent
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true
                )
            }

            // Email (read-only)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Email",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = currentEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) InnovexiaColors.DarkTextMuted
                    else InnovexiaColors.LightTextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (darkTheme) InnovexiaColors.DarkSurfaceElevated
                            else InnovexiaColors.LightSurfaceElevated
                        )
                        .padding(16.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    style = GlassButtonStyle.Ghost,
                    modifier = Modifier.weight(1f),
                    darkTheme = darkTheme
                )

                GlassButton(
                    text = "Save",
                    onClick = {
                        if (displayName.isNotBlank() && displayName != currentName) {
                            onSave(displayName)
                        }
                        onDismiss()
                    },
                    style = GlassButtonStyle.Primary,
                    modifier = Modifier.weight(1f),
                    darkTheme = darkTheme,
                    enabled = displayName.isNotBlank()
                )
            }
        }
    }
}
