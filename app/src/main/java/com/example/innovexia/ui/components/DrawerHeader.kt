package com.example.innovexia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.innovexia.R
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.theme.InnovexiaDesign

/**
 * Drawer header with Innovexia logo and glass-style "+ New" button
 */
@Composable
fun DrawerHeader(
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = InnovexiaDesign.Spacing.LG, vertical = InnovexiaDesign.Spacing.MD),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App logo - using launcher icon
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "Innovexia Logo",
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
        )

        Spacer(Modifier.width(10.dp))

        // App name
        Text(
            text = "Innovexia",
            color = InnovexiaColors.DarkTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.weight(1f))

        // Glass / matte "+ New" button
        GlassButton(
            text = "+ New",
            onClick = onNewChat
        )
    }
}

/**
 * Glass-style button with frosted matte appearance
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = InnovexiaColors.DarkSurfaceElevated.copy(alpha = 0.5f)
    val borderColor = InnovexiaColors.DarkBorder.copy(alpha = 0.65f)

    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(28.dp), // Capsule shape
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = InnovexiaColors.DarkTextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
