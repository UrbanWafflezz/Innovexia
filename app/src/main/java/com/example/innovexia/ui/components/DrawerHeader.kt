package com.example.innovexia.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.innovexia.ui.theme.InnovexiaDesign

/**
 * Drawer header with Innovexia logo and Material 3 FilledTonalButton
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
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.weight(1f))

        // Material 3 FilledTonalButton for new chat
        FilledTonalButton(
            onClick = onNewChat,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "New",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
        }
    }
}
