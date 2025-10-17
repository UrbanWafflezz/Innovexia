package com.example.innovexia.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Material 3 styled authentication text field
 * Replaces GlassField with proper Material 3 design
 */
@Composable
fun M3AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    isPassword: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    var passwordVisible by remember { mutableStateOf(false) }

    val textColor = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
    val labelColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
    val focusedBorderColor = if (darkTheme) InnovexiaColors.BlueAccent else Color(0xFF3B82F6)
    val unfocusedBorderColor = if (darkTheme) Color(0xFF374151) else Color(0xFFD1D5DB)
    val backgroundColor = if (darkTheme) Color(0xFF1F2937).copy(alpha = 0.3f) else Color(0xFFF9FAFB)
    val cursorColor = if (darkTheme) InnovexiaColors.BlueAccent else Color(0xFF3B82F6)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.colorScheme.error else labelColor
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = labelColor
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType
        ),
        isError = isError,
        supportingText = if (supportingText != null) {
            {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error else labelColor
                    )
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedBorderColor = focusedBorderColor,
            unfocusedBorderColor = unfocusedBorderColor,
            cursorColor = cursorColor,
            focusedLabelColor = focusedBorderColor,
            unfocusedLabelColor = labelColor,
            focusedLeadingIconColor = focusedBorderColor,
            unfocusedLeadingIconColor = labelColor,
            focusedContainerColor = backgroundColor,
            unfocusedContainerColor = backgroundColor,
            errorBorderColor = MaterialTheme.colorScheme.error,
            errorLabelColor = MaterialTheme.colorScheme.error,
            errorLeadingIconColor = MaterialTheme.colorScheme.error,
            errorContainerColor = if (darkTheme) Color(0xFF2D1313) else Color(0xFFFEF2F2)
        ),
        modifier = modifier.fillMaxWidth()
    )
}
