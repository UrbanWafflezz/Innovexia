package com.example.innovexia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.InnovexiaDesign

/**
 * Search bar for the drawer that matches ChatInputField styling exactly
 */
@Composable
fun DrawerSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search Innovexia chats…",
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme()
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Adaptive colors for light/dark mode
    val containerColor = if (darkTheme) Color(0xFF121821) else Color(0xFFFFFFFF)
    val borderColor = if (darkTheme) InnovexiaColors.DarkBorder.copy(alpha = 0.55f)
                      else InnovexiaColors.LightBorder.copy(alpha = 0.6f)
    val placeholderColor = if (darkTheme) Color(0xFF9BA8B5).copy(alpha = 0.80f)
                           else InnovexiaColors.LightTextSecondary.copy(alpha = 0.7f)
    val textColor = if (darkTheme) InnovexiaColors.DarkTextPrimary
                    else InnovexiaColors.LightTextPrimary
    val iconColor = if (darkTheme) InnovexiaColors.DarkTextSecondary
                    else InnovexiaColors.LightTextSecondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(28.dp), // Capsule shape to match composer
        color = containerColor,
        border = BorderStroke(InnovexiaDesign.Border.Default, borderColor),
        shadowElevation = InnovexiaDesign.Elevation.Sheet
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(10.dp))

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = textColor,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(textColor),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = placeholderColor,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                },
                maxLines = 1,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                )
            )
        }
    }
}
