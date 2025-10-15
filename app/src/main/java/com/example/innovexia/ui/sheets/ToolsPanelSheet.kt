package com.example.innovexia.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaTheme
import com.example.innovexia.ui.theme.LightColors

/**
 * A modal bottom sheet displaying available tools and their settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsPanelSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    thinkingEnabled: Boolean = true,
    onThinkingChanged: (Boolean) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local state for web tools toggle (UI-only)
    var webToolsEnabled by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        modifier = modifier,
        containerColor = if (darkTheme) DarkColors.SurfaceElevated else LightColors.SurfaceElevated,
        dragHandle = null,
        properties = ModalBottomSheetDefaults.properties(
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tools",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f)
                        else LightColors.SecondaryText.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section: Toggles
            Text(
                text = "CAPABILITIES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.7f)
                        else LightColors.SecondaryText.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Thinking (Models) toggle
            ToolToggleRow(
                icon = Icons.Rounded.Psychology,
                title = "Thinking (Models)",
                subtitle = "Enable advanced reasoning",
                checked = thinkingEnabled,
                onCheckedChange = onThinkingChanged,
                darkTheme = darkTheme
            )

            // Web tools toggle
            ToolToggleRow(
                icon = Icons.Rounded.Public,
                title = "Web tools",
                subtitle = "Access to web search and browsing",
                checked = webToolsEnabled,
                onCheckedChange = { webToolsEnabled = it },
                darkTheme = darkTheme
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f)
                        else LightColors.SecondaryText.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Section: Actions
            Text(
                text = "ACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.7f)
                        else LightColors.SecondaryText.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Search action
            ToolActionRow(
                icon = Icons.Rounded.Search,
                title = "Search",
                subtitle = "Search through your conversations",
                onClick = { /* TODO: Open search */ },
                darkTheme = darkTheme
            )

            // Connect services (disabled demo)
            ToolActionRow(
                icon = Icons.Rounded.Link,
                title = "Connect services",
                subtitle = "Coming soon",
                onClick = { /* Disabled */ },
                enabled = false,
                darkTheme = darkTheme
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ToolToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onCheckedChange(!checked) }, role = Role.Switch)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (darkTheme) DarkColors.SurfaceElevated else LightColors.SurfaceElevated,
                checkedTrackColor = if (darkTheme) DarkColors.AccentBlue else LightColors.AccentBlue,
                uncheckedThumbColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                uncheckedTrackColor = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.3f)
                                      else LightColors.SecondaryText.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun ToolActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick, role = Role.Button)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (enabled) {
                if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
            } else {
                if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.4f)
                else LightColors.SecondaryText.copy(alpha = 0.4f)
            }
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) {
                    if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                } else {
                    if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.5f)
                    else LightColors.SecondaryText.copy(alpha = 0.5f)
                }
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
            )
        }
    }
}

@Preview(name = "ToolsPanelSheet Light", showBackground = true)
@Composable
fun ToolsPanelSheetPreview() {
    InnovexiaTheme(darkTheme = false) {
        ToolsPanelSheet(
            onDismiss = {},
            darkTheme = false
        )
    }
}

@Preview(name = "ToolsPanelSheet Dark", showBackground = true)
@Composable
fun ToolsPanelSheetDarkPreview() {
    InnovexiaTheme(darkTheme = true) {
        ToolsPanelSheet(
            onDismiss = {},
            darkTheme = true
        )
    }
}
