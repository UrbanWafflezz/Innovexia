package com.example.innovexia.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.innovexia.InnovexiaApplication
import com.example.innovexia.data.models.PlanLimits
import com.example.innovexia.data.models.SubscriptionPlan
import com.example.innovexia.data.models.TierInfo
import com.example.innovexia.ui.theme.InnovexiaColors
import com.example.innovexia.ui.viewmodels.DrawerAccountViewModel
import androidx.compose.ui.platform.LocalContext

/**
 * Account Quick Panel dialog
 * Compact popover with account actions:
 * - Profile
 * - Usage and Limit
 * - Subscription
 * - Settings
 * - Log out
 */
@Composable
fun AccountQuickPanel(
    onDismiss: () -> Unit,
    onProfile: () -> Unit,
    onUsage: () -> Unit,
    onSubscription: () -> Unit,
    onManageSubscription: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DrawerAccountViewModel = viewModel()
) {
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val tier by viewModel.tier.collectAsState(initial = TierInfo.default())
    val isGuest by viewModel.isGuest.collectAsState(initial = user == null)

    // Get guest rate limit info
    val firebaseRateLimiter = remember {
        (context.applicationContext as InnovexiaApplication).firebaseRateLimiter
    }
    val guestPlan = SubscriptionPlan.FREE
    val guestLimits = remember { PlanLimits.getLimits(guestPlan) }
    val burstCount by firebaseRateLimiter.currentCount.collectAsState()
    val isRateLimited by firebaseRateLimiter.isLimited.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = InnovexiaColors.DarkSurfaceElevated,
            border = BorderStroke(1.dp, InnovexiaColors.DarkBorder.copy(alpha = 0.6f)),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(Modifier.padding(8.dp)) {
                // Header
                Row(
                    modifier = Modifier.padding(8.dp),
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
                            text = user?.email ?: "Guest mode",
                            color = InnovexiaColors.DarkTextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        val statusText = if (isGuest) {
                            "Local only"
                        } else {
                            "${tier.label} • ${tier.getStatusText()}"
                        }
                        Text(
                            text = statusText,
                            color = InnovexiaColors.DarkTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    TierBadge(tier)
                }

                HorizontalDivider(
                    color = InnovexiaColors.DarkBorder.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Guest mode rate limit display
                if (isGuest) {
                    GuestRateLimitDisplay(
                        currentCount = burstCount,
                        limit = guestLimits.burstRequestsPerMinute,
                        isLimited = isRateLimited
                    )

                    HorizontalDivider(
                        color = InnovexiaColors.DarkBorder.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Menu items
                QuickItem(Icons.Outlined.Person, "Profile") {
                    onDismiss()
                    onProfile()
                }

                if (!isGuest) {
                    QuickItem(Icons.Outlined.BarChart, "Usage and Limit") {
                        onDismiss()
                        onUsage()
                    }
                    QuickItem(Icons.Outlined.CardMembership, "Subscription") {
                        onDismiss()
                        onSubscription()
                    }
                    QuickItem(Icons.Outlined.ManageAccounts, "Manage Subscription") {
                        onDismiss()
                        onManageSubscription()
                    }
                }

                QuickItem(Icons.Outlined.Settings, "Settings") {
                    onDismiss()
                    onSettings()
                }

                if (!isGuest) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = InnovexiaColors.DarkBorder.copy(alpha = 0.6f)
                    )
                    QuickItem(Icons.Outlined.Logout, "Log out") {
                        onDismiss()
                        onLogout()
                    }
                } else {
                    Spacer(Modifier.height(6.dp))
                    // Sign in button for guests
                    Surface(
                        onClick = {
                            onDismiss()
                            onProfile() // Opens profile sheet for sign-in
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = InnovexiaColors.Gold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Sign in",
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = InnovexiaColors.DarkTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Quick panel menu item
 */
@Composable
private fun QuickItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = InnovexiaColors.DarkTextPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            color = InnovexiaColors.DarkTextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = InnovexiaColors.DarkTextSecondary
        )
    }
}

/**
 * Guest mode rate limit display
 * Shows current usage and rate limit status
 */
@Composable
private fun GuestRateLimitDisplay(
    currentCount: Int,
    limit: Int,
    isLimited: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = InnovexiaColors.DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Speed,
                    contentDescription = "Rate Limit",
                    tint = if (isLimited) InnovexiaColors.Error else InnovexiaColors.Gold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Rate Limit",
                    color = InnovexiaColors.DarkTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            // Progress bar
            val progress = (currentCount.toFloat() / limit.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    isLimited -> InnovexiaColors.Error
                    progress >= 0.8f -> InnovexiaColors.Gold
                    else -> InnovexiaColors.BlueAccent
                },
                trackColor = InnovexiaColors.DarkBorder.copy(alpha = 0.3f)
            )

            Spacer(Modifier.height(6.dp))

            // Usage text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$currentCount / $limit requests",
                    color = InnovexiaColors.DarkTextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "per minute",
                    color = InnovexiaColors.DarkTextSecondary,
                    fontSize = 11.sp
                )
            }

            if (isLimited) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Rate limit exceeded. Please wait before sending more messages.",
                    color = InnovexiaColors.Error,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
