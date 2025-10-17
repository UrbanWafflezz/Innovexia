package com.example.innovexia.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.data.models.TierInfo
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.viewmodels.DrawerAccountViewModel
import com.example.innovexia.ui.viewmodels.SubscriptionViewModel
import com.example.innovexia.ui.viewmodels.SubscriptionViewModelFactory

/**
 * Drawer footer profile card
 * Shows user avatar, name, tier badge, status, and rate limit counter
 * Tapping opens the Account Quick Panel
 */
@Composable
fun DrawerFooterProfile(
    onOpenQuickPanel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DrawerAccountViewModel = viewModel()
) {
    val context = LocalContext.current
    val app = context.applicationContext as InnovexiaApplication

    // Get subscription view model for rate limit info
    val subscriptionViewModel: SubscriptionViewModel = viewModel(
        factory = SubscriptionViewModelFactory(
            subscriptionRepository = app.subscriptionRepository,
            usageRepository = app.usageRepository
        )
    )

    val user by viewModel.user.collectAsState()
    val tier by viewModel.tier.collectAsState(initial = TierInfo.default())
    val isGuest by viewModel.isGuest.collectAsState(initial = user == null)

    // Get real-time rate limit info
    val burstCount by subscriptionViewModel.burstCount.collectAsState()
    val planLimits by subscriptionViewModel.planLimits.collectAsState()

    val profileInteraction = remember { MutableInteractionSource() }
    val isProfilePressed by profileInteraction.collectIsPressedAsState()

    // Animate profile card scale when pressed
    val profileScale by animateFloatAsState(
        targetValue = if (isProfilePressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "profileScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Profile card
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .scale(profileScale)
                .clickable(
                    interactionSource = profileInteraction,
                    indication = null
                ) { onOpenQuickPanel() }
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
                        text = if (isGuest) "Guest mode" else (user?.displayName ?: "Guest"),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subText = if (isGuest) {
                        "Local only"
                    } else {
                        "${tier.label} • ${tier.getStatusText()}"
                    }
                    Text(
                        text = subText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!isGuest) {
                    TierBadge(tier)
                } else {
                    // Show "Free" badge for guests
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Free",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Rate Limit card - only show for guest users
        if (isGuest) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = "Rate Limit",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = "Rate Limit",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "$burstCount / ${planLimits.burstRequestsPerMinute} requests",
                            color = if (burstCount >= planLimits.burstRequestsPerMinute * 0.9f) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "per minute",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
