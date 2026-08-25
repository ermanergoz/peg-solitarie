package com.app.pegsolitaire.ui.results

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled

// The Results celebration motion helpers (DESIGN-SYSTEM.md Results). Both return
// their animated value as a lambda so only the layer/text reading it re-runs, and
// both respect the OS animator-duration-scale gate: with decorative animation off
// they are settled on their final value from the first frame.

/**
 * A one-shot overshoot pop-in scale, delayed by [delayMillis] for sequencing. Returns
 * the value as a lambda so only the layer that reads it re-runs. When OS animations are
 * off the scale is settled from the first frame.
 */
@Composable
internal fun rememberPopIn(delayMillis: Int): () -> Float {
    val animationsEnabled = rememberAnimationsEnabled()
    val scale = remember { Animatable(if (animationsEnabled) POP_START_SCALE else SETTLED_SCALE) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = SETTLED_SCALE,
            animationSpec =
                tween(
                    durationMillis = ClayAnim.POP_IN_MS,
                    delayMillis = delayMillis,
                    easing = ClayAnim.OVERSHOOT_EASING,
                ),
        )
    }
    return { scale.value }
}

/**
 * Counts a value up from zero to [target] over [ClayAnim.SCORE_COUNT_UP_MS], or lands on
 * [target] immediately when OS animations are off.
 */
@Composable
internal fun rememberCountUp(target: Int): () -> Int {
    val animationsEnabled = rememberAnimationsEnabled()
    val progress = remember { Animatable(if (animationsEnabled) COUNT_UP_START else target.toFloat()) }
    LaunchedEffect(target) {
        progress.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(ClayAnim.SCORE_COUNT_UP_MS, easing = ClayAnim.ENTRANCE_EASING),
        )
    }
    return { progress.value.toInt() }
}

/** Pop-in animation endpoints. */
private const val POP_START_SCALE = 0f
private const val SETTLED_SCALE = 1f

/** Score count-up starting value. */
private const val COUNT_UP_START = 0f
