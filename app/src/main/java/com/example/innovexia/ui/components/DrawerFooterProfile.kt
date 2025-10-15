package com.example.innovexia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.data.models.TierInfo
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.viewmodels.DrawerAccountViewModel

/**
 * Drawer footer profile card
 * Shows user avatar, name, tier badge, and status
 * Tapping opens the Account Quick Panel
 */
@Composable
fun DrawerFooterProfile(
    onOpenQuickPanel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DrawerAccountViewModel = viewModel()
) {
    val user by viewModel.user.collectAsState()
    val tier by viewModel.tier.collectAsState(initial = TierInfo.default())
    val isGuest by viewModel.isGuest.collectAsState(initial = user == null)

    Surface(
        color = InnovexiaColors.DarkSurfaceElevated.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, InnovexiaColors.DarkBorder.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable { onOpenQuickPanel() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                name = user?.displayName ?: "Guest",
                photoUrl = user?.photoUrl?.toString(),
                size = 36.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = user?.displayName ?: "Guest",
                    color = InnovexiaColors.DarkTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subText = if (isGuest) {
                    "Not signed in"
                } else {
                    "${tier.label} • ${tier.getStatusText()}"
                }
                Text(
                    text = subText,
                    color = InnovexiaColors.DarkTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TierBadge(tier)
        }
    }
}
