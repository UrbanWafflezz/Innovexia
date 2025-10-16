package com.example.innovexia.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.innovexia.R
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors

@Composable
fun ChevronButton(
    onClick: () -> Unit,
    isOpen: Boolean,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Menu,
            contentDescription = if (isOpen) "Close menu" else "Open menu",
            modifier = Modifier.size(24.dp),
            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
        )
    }
}

@Composable
fun GearButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "Settings",
            modifier = Modifier.size(24.dp),
            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
        )
    }
}
