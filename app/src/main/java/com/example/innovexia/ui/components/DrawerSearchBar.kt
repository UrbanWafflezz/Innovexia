package com.example.innovexia.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Search bar for the drawer with embedded New button and M3 animations
 * Features animated cycling placeholder text and press animations
 */
@Composable
fun DrawerSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search chats…",
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    onNewChat: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animated placeholder text cycling
    val placeholders = remember {
        listOf(
            "Search chats…",
            "Find conversations…",
            "Look up chats…",
            "Search messages…"
        )
    }
    var currentPlaceholderIndex by remember { mutableIntStateOf(0) }

    // Cycle through placeholders only when not focused and field is empty
    LaunchedEffect(isFocused, value) {
        if (!isFocused && value.isEmpty()) {
            while (true) {
                delay(3000) // Change every 3 seconds
                currentPlaceholderIndex = (currentPlaceholderIndex + 1) % placeholders.size
            }
        }
    }

    // Animate border and background when focused
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.8f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "borderAlpha"
    )

    val surfaceAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.5f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "surfaceAlpha"
    )

    // New chat button interaction source for press animation
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()

    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = surfaceAlpha),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = borderAlpha)),
        tonalElevation = 0.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 14.dp, end = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    interactionSource = interactionSource,
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                // Animated placeholder text
                                AnimatedContent(
                                    targetState = placeholders[currentPlaceholderIndex],
                                    transitionSpec = {
                                        slideInVertically(
                                            animationSpec = tween(400),
                                            initialOffsetY = { it / 2 }
                                        ) + fadeIn(animationSpec = tween(400)) togetherWith
                                                slideOutVertically(
                                                    animationSpec = tween(400),
                                                    targetOffsetY = { -it / 2 }
                                                ) + fadeOut(animationSpec = tween(400))
                                    },
                                    label = "placeholderAnimation"
                                ) { placeholderText ->
                                    Text(
                                        text = placeholderText,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            innerTextField()
                        }
                    },
                    maxLines = 1,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    )
                )
            }

            Spacer(Modifier.width(6.dp))

            // New chat button - animated with press scale and fade in/out
            AnimatedVisibility(
                visible = !isFocused,
                enter = fadeIn(tween(200)) + scaleIn(tween(200)),
                exit = fadeOut(tween(150)) + scaleOut(tween(150))
            ) {
                FilledTonalIconButton(
                    onClick = onNewChat,
                    modifier = Modifier
                        .size(40.dp)
                        .scale(buttonScale),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                    interactionSource = buttonInteractionSource
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New chat",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
