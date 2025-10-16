package com.example.innovexia.ui.sheets

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.InnovexiaTheme
import com.example.innovexia.ui.theme.LightColors

/**
 * Modal sheet asking for user consent to save chat history locally.
 *
 * This is shown the first time the user tries to send a message.
 * - Allow: Save chats to Room database on device
 * - Not now: Ephemeral mode (in-memory only, no persistence)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryConsentSheet(
    onAllow: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "History & Privacy",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp
                ),
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation
            Text(
                text = "Innovexia can save your conversations locally on this device for easy access.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "• Your chats stay private on your device\n" +
                      "• No cloud sync or external storage\n" +
                      "• You can delete history anytime from Settings",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "If you choose \"Not now\", your conversations won't be saved and will disappear when you close the app.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = if (darkTheme) DarkColors.SecondaryText.copy(alpha = 0.8f)
                        else LightColors.SecondaryText.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Decline button
                TextButton(
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                    )
                ) {
                    Text(
                        text = "Not now",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
                    )
                }

                // Allow button
                Button(
                    onClick = onAllow,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (darkTheme) DarkColors.AccentBlue else LightColors.AccentBlue,
                        contentColor = if (darkTheme) DarkColors.SurfaceElevated else LightColors.SurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Allow",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(name = "HistoryConsentSheet Light", showBackground = true)
@Composable
fun HistoryConsentSheetPreview() {
    InnovexiaTheme(darkTheme = false) {
        HistoryConsentSheet(
            onAllow = {},
            onDecline = {},
            darkTheme = false
        )
    }
}

@Preview(name = "HistoryConsentSheet Dark", showBackground = true)
@Composable
fun HistoryConsentSheetDarkPreview() {
    InnovexiaTheme(darkTheme = true) {
        HistoryConsentSheet(
            onAllow = {},
            onDecline = {},
            darkTheme = true
        )
    }
}
