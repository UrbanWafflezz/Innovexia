package com.example.innovexia.ui.persona.sources

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Compact add bar for adding URLs, files, and images
 */
@Composable
fun SourceAddBar(
    onAddUrl: (String) -> Unit,
    onAddFile: () -> Unit,
    onAddImage: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var showUrlPopup by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add URL button
            AddButton(
                icon = Icons.Rounded.Link,
                label = "URL",
                onClick = { showUrlPopup = true },
                darkTheme = darkTheme,
                modifier = Modifier.weight(1f)
            )

            // Add File button
            AddButton(
                icon = Icons.Rounded.InsertDriveFile,
                label = "File",
                onClick = onAddFile,
                darkTheme = darkTheme,
                modifier = Modifier.weight(1f)
            )

            // Add Image button
            AddButton(
                icon = Icons.Rounded.Image,
                label = "Image",
                onClick = onAddImage,
                darkTheme = darkTheme,
                modifier = Modifier.weight(1f)
            )
        }

        // URL input popup (overlays)
        if (showUrlPopup) {
            AddUrlPopup(
                onDismiss = { showUrlPopup = false },
                onAdd = { url ->
                    onAddUrl(url)
                    showUrlPopup = false
                },
                darkTheme = darkTheme
            )
        }
    }
}

/**
 * Glass-style add button
 */
@Composable
private fun AddButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (darkTheme) Color(0xFF1E2530) else Color(0xFFF5F5F5),
        border = BorderStroke(
            1.dp,
            if (darkTheme) Color(0xFF253041).copy(alpha = 0.4f) else Color(0xFFE7EDF5).copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Popup for adding a URL
 */
@Composable
private fun AddUrlPopup(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    darkTheme: Boolean
) {
    var url by remember { mutableStateOf("") }

    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(top = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = if (darkTheme) Color(0xFF1E2530) else Color.White,
            tonalElevation = 8.dp,
            border = BorderStroke(
                1.dp,
                if (darkTheme) Color(0xFF253041).copy(alpha = 0.6f) else Color(0xFFE7EDF5).copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add URL",
                        color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // URL input field
                BasicTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (darkTheme) Color(0xFF0F1419) else Color(0xFFF5F5F5))
                        .padding(horizontal = 12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
                    ),
                    cursorBrush = SolidColor(if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (url.isEmpty()) {
                                Text(
                                    text = "https://example.com",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancel button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.3f) else LightColors.SecondaryText.copy(alpha = 0.3f)
                        )
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }

                    // Add button
                    Button(
                        onClick = {
                            if (url.isNotBlank()) {
                                onAdd(url)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = url.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (darkTheme) InnovexiaColors.GoldDim else InnovexiaColors.Gold,
                            contentColor = Color.White,
                            disabledContainerColor = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.2f) else LightColors.SecondaryText.copy(alpha = 0.2f)
                        )
                    ) {
                        Text("Add", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
