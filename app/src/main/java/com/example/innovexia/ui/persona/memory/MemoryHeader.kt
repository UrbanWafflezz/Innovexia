package com.example.innovexia.ui.persona.memory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.persona.Persona
import com.example.innovexia.ui.theme.InnovexiaColors

/**
 * Header section for Memory Tab
 * Shows persona avatar, name, memory toggle, and info banner
 */
@Composable
fun MemoryHeader(
    persona: Persona,
    isMemoryEnabled: Boolean,
    onMemoryToggleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Persona info and memory toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Persona avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(persona.color).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = persona.initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(persona.color),
                    fontSize = 20.sp
                )
            }

            // Name and toggle
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = persona.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (darkTheme) InnovexiaColors.DarkTextPrimary else InnovexiaColors.LightTextPrimary
                )
                Text(
                    text = "Personal Memory",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                    fontSize = 12.sp
                )
            }

            // Memory toggle
            Switch(
                checked = isMemoryEnabled,
                onCheckedChange = onMemoryToggleChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                    checkedTrackColor = if (darkTheme) InnovexiaColors.GoldDim.copy(alpha = 0.5f) else InnovexiaColors.Gold.copy(alpha = 0.5f),
                    uncheckedThumbColor = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                    uncheckedTrackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                )
            )

            // Info icon
            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Memory information",
                    tint = if (darkTheme) InnovexiaColors.DarkTextSecondary else InnovexiaColors.LightTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Privacy banner (when enabled)
        if (isMemoryEnabled) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (darkTheme) Color(0xFF1E3A5F).copy(alpha = 0.3f) else Color(0xFFDCEEFF),
                border = BorderStroke(
                    1.dp,
                    if (darkTheme) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color(0xFF60A5FA).copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\uD83E\uDDE0",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Persona memory is stored locally on your device for privacy. Nothing is uploaded to the cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (darkTheme) Color(0xFF93C5FD) else Color(0xFF1E40AF),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Info dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold
                )
            },
            title = {
                Text(
                    text = "About Memory",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Innovexia remembers key things locally so this persona can understand you better. Memories never leave your device.\n\n" +
                            "Memories include:\n" +
                            "• Facts about you\n" +
                            "• Important events\n" +
                            "• Your preferences\n" +
                            "• Emotional context\n" +
                            "• Ongoing projects\n" +
                            "• Knowledge shared\n\n" +
                            "You can delete any memory at any time.\n\n" +
                            "⚠️ Important: Memories are stored in the app's local database. If you clear app data or uninstall the app, all memories will be permanently deleted.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it")
                }
            },
            containerColor = if (darkTheme) Color(0xFF1E2530) else Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
