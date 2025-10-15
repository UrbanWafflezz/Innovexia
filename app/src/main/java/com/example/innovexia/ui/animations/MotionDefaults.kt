package com.example.innovexia.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.unit.IntOffset
import com.example.innovexia.ui.theme.InnovexiaDesign

/**
 * Innovexia Motion System
 * Standardized animations and transitions for consistency
 */
object MotionDefaults {

    // ═══════════════════════════════════════════════════════════════════
    // Animation Specs
    // ═══════════════════════════════════════════════════════════════════

    val fastTween: TweenSpec<Float> = tween(
        durationMillis = InnovexiaDesign.Motion.Fast,
        easing = FastOutSlowInEasing
    )

    val normalTween: TweenSpec<Float> = tween(
        durationMillis = InnovexiaDesign.Motion.Normal,
        easing = FastOutSlowInEasing
    )

    val moderateTween: TweenSpec<Float> = tween(
        durationMillis = InnovexiaDesign.Motion.Moderate,
        easing = FastOutSlowInEasing
    )

    val slowTween: TweenSpec<Float> = tween(
        durationMillis = InnovexiaDesign.Motion.Slow,
        easing = FastOutSlowInEasing
    )

    // Spring animations for natural feel
    val spring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val gentleSpring: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // ═══════════════════════════════════════════════════════════════════
    // Fade Animations
    // ═══════════════════════════════════════════════════════════════════

    val fadeIn: EnterTransition = fadeIn(
        animationSpec = normalTween
    )

    val fadeOut: ExitTransition = fadeOut(
        animationSpec = normalTween
    )

    val fadeInFast: EnterTransition = fadeIn(
        animationSpec = fastTween
    )

    val fadeOutFast: ExitTransition = fadeOut(
        animationSpec = fastTween
    )

    val fadeInSlow: EnterTransition = fadeIn(
        animationSpec = slowTween
    )

    val fadeOutSlow: ExitTransition = fadeOut(
        animationSpec = slowTween
    )

    // ═══════════════════════════════════════════════════════════════════
    // Scale Animations
    // ═══════════════════════════════════════════════════════════════════

    val scaleIn: EnterTransition = scaleIn(
        initialScale = 0.96f,
        animationSpec = normalTween
    )

    val scaleOut: ExitTransition = scaleOut(
        targetScale = 0.96f,
        animationSpec = normalTween
    )

    val scaleInLarge: EnterTransition = scaleIn(
        initialScale = 0.9f,
        animationSpec = normalTween
    )

    val scaleOutLarge: ExitTransition = scaleOut(
        targetScale = 0.9f,
        animationSpec = normalTween
    )

    // ═══════════════════════════════════════════════════════════════════
    // Slide Animations
    // ═══════════════════════════════════════════════════════════════════

    val slideInFromBottom: EnterTransition = slideInVertically(
        initialOffsetY = { it / 3 },
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing)
    )

    val slideOutToBottom: ExitTransition = slideOutVertically(
        targetOffsetY = { it / 3 },
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing)
    )

    val slideInFromTop: EnterTransition = slideInVertically(
        initialOffsetY = { -it / 3 },
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing)
    )

    val slideOutToTop: ExitTransition = slideOutVertically(
        targetOffsetY = { -it / 3 },
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing)
    )

    val slideInFromStart: EnterTransition = slideInHorizontally(
        initialOffsetX = { -it / 2 },
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing)
    )

    val slideOutToStart: ExitTransition = slideOutHorizontally(
        targetOffsetX = { -it / 2 },
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing)
    )

    val slideInFromEnd: EnterTransition = slideInHorizontally(
        initialOffsetX = { it / 2 },
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing)
    )

    val slideOutToEnd: ExitTransition = slideOutHorizontally(
        targetOffsetX = { it / 2 },
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing)
    )

    // ═══════════════════════════════════════════════════════════════════
    // Combined Animations
    // ═══════════════════════════════════════════════════════════════════

    // Sheet/Modal enter/exit
    val sheetEnter: EnterTransition = fadeIn + scaleIn + slideInFromBottom
    val sheetExit: ExitTransition = fadeOut + scaleOut + slideOutToBottom

    // Dialog enter/exit
    val dialogEnter: EnterTransition = fadeIn + scaleIn
    val dialogExit: ExitTransition = fadeOut + scaleOut

    // Drawer enter/exit
    val drawerEnter: EnterTransition = fadeIn + slideInFromStart
    val drawerExit: ExitTransition = fadeOut + slideOutToStart

    // Menu enter/exit
    val menuEnter: EnterTransition = fadeIn + scaleIn
    val menuExit: ExitTransition = fadeOut + scaleOut

    // Content fade
    val contentEnter: EnterTransition = fadeIn
    val contentExit: ExitTransition = fadeOut

    // Expand/Collapse
    val expandVertically: EnterTransition = expandVertically(
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing),
        expandFrom = androidx.compose.ui.Alignment.Top
    )

    val shrinkVertically: ExitTransition = shrinkVertically(
        animationSpec = tween(InnovexiaDesign.Motion.Normal, easing = FastOutSlowInEasing),
        shrinkTowards = androidx.compose.ui.Alignment.Top
    )

    // ═══════════════════════════════════════════════════════════════════
    // Utility Functions
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Get a fade animation with custom duration
     */
    fun fadeInWith(durationMillis: Int): EnterTransition = fadeIn(
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
    )

    fun fadeOutWith(durationMillis: Int): ExitTransition = fadeOut(
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
    )

    /**
     * Get a scale animation with custom parameters
     */
    fun scaleInWith(scale: Float, durationMillis: Int): EnterTransition = scaleIn(
        initialScale = scale,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
    )

    fun scaleOutWith(scale: Float, durationMillis: Int): ExitTransition = scaleOut(
        targetScale = scale,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
    )
}
