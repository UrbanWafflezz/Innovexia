package com.example.innovexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors

@Composable
fun BottomBar(
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    userInitials: String = "AS"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.Transparent)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with initials (clickable)
        Box(
            modifier = Modifier
                .size(40.dp) // 40dp touch target
                .clip(CircleShape)
                .clickable(
                    onClick = onAvatarClick,
                    role = Role.Button
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (darkTheme) DarkColors.SurfaceElevated else LightColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitials,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 11.sp
                    ),
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                )
            }
        }

        // Gear icon button
        GearButton(
            onClick = onSettingsClick,
            darkTheme = darkTheme
        )
    }
}
