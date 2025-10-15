package com.example.innovexia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.InnovexiaDesign

/**
 * Footer for the drawer with About link and version
 */
@Composable
fun DrawerFooter(
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        InnovexiaColors.DarkGradientEnd.copy(alpha = 0f),
                        InnovexiaColors.DarkGradientEnd.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // About button as a card
        Surface(
            onClick = onAboutClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = InnovexiaColors.DarkSurfaceElevated.copy(alpha = 0.6f),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = "About",
                    modifier = Modifier.size(20.dp),
                    tint = InnovexiaColors.DarkTextSecondary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "About Innovexia",
                        color = InnovexiaColors.DarkTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Learn more about the app",
                        color = InnovexiaColors.DarkTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Version with better styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Version 1.0.0",
                fontSize = 11.sp,
                color = InnovexiaColors.DarkTextSecondary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Normal
            )

            Text(
                text = "Built with ❤️",
                fontSize = 11.sp,
                color = InnovexiaColors.DarkTextSecondary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}
