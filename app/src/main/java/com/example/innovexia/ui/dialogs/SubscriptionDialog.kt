package com.example.innovexia.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.innovexia.ui.sheets.profile.tabs.SubscriptionsTab
import com.example.innovexia.ui.theme.DarkColors
import com.example.innovexia.ui.theme.LightColors

/**
 * Standalone Subscription Dialog
 * Shows available subscription plans
 */
@Composable
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (darkTheme) Color(0xFF141A22) else Color.White,
            tonalElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                if (darkTheme) Color(0xFF253041).copy(alpha = 0.6f) else Color(0xFFE7EDF5).copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header with close button
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Subscription",
                        color = if (darkTheme) DarkColors.PrimaryText else LightColors.PrimaryText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = if (darkTheme) DarkColors.SecondaryText else LightColors.SecondaryText
                        )
                    }
                }

                androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))

                // Subscriptions tab content
                SubscriptionsTab(darkTheme = darkTheme)
            }
        }
    }
}
