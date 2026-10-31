package com.app.pegsolitaire.ui.backdrop

import androidx.compose.animation.core.FastOutSlowInEasing
import kotlin.math.PI
import kotlin.math.cos

/*
 * The backdrop's poses as pure functions of a scene element and the clock's time (candy-backdrop.md
 * §3.4, B-24, the tech-lead's A1). Time is `Long` milliseconds of backdrop time, reduced modulo
 * the element's own loop in `Long` before any floating-point step, so a session of any length is
 * exact. Each function returns a primitive and allocates nothing: the draw lambda calls them per
 * element per frame. Rest is its own path, never a magic time, because each light rests at its own
 * phase.
 */

/** The value [gelSheenProgress] returns outside the sheen's pass: nothing is drawn. */
const val SHEEN_HIDDEN = -1f

/** A wave's rest offset, on both axes: the reduced-motion and battery-saver picture. */
const val WAVE_REST_OFFSET = 0f

/** A wave's swing `p(t) = −cos(π t / T)`, −1 at the clock's start, +1 a half-cycle later. */
fun waveSwing(
    wave: WaveModel,
    timeMs: Long,
): Float {
    val cycle = wave.halfCycleMs * 2
    val inCycle = timeMs.mod(cycle)
    return -cos(PI * inCycle / wave.halfCycleMs).toFloat()
}

/** A wave's offset across at [timeMs]: its signed bob times its swing. */
fun waveOffsetX(
    wave: WaveModel,
    timeMs: Long,
): Float = wave.bobX * waveSwing(wave, timeMs)

/** A wave's offset down at [timeMs]: its signed bob times its swing. */
fun waveOffsetY(
    wave: WaveModel,
    timeMs: Long,
): Float = wave.bobY * waveSwing(wave, timeMs)

/** A light's phase `frac(φ0 + t / period)` at [timeMs]. */
fun lightPhase(
    light: LightModel,
    timeMs: Long,
): Float {
    val inPeriod = timeMs.mod(light.periodMs)
    val phase = light.phase0 + inPeriod.toFloat() / light.periodMs
    return phase - phase.toInt()
}

/**
 * A light's rise at [phase]: [SceneModel.riseBelow] below its rest centre at 0, rising linearly to
 * [SceneModel.riseAbove] above it at 1.
 */
fun lightDyAt(
    scene: SceneModel,
    phase: Float,
): Float = scene.riseBelow - (scene.riseBelow + scene.riseAbove) * phase

/** A light's envelope at [phase]: up over `LIGHT_FADE_IN`, held at 1, down from `LIGHT_FADE_OUT` to 0 at the wrap. */
fun lightEnvelope(phase: Float): Float =
    when {
        phase < BackdropMetrics.LIGHT_FADE_IN -> phase / BackdropMetrics.LIGHT_FADE_IN
        phase <= BackdropMetrics.LIGHT_FADE_OUT -> 1f
        else -> (1f - phase) / (1f - BackdropMetrics.LIGHT_FADE_OUT)
    }

/**
 * The calm factor of a light offset by [dy] (B-20): 0 once its top edge reaches the calm end, rising
 * to 1 a calm fade below it.
 */
fun lightCalm(
    scene: SceneModel,
    light: LightModel,
    dy: Float,
): Float {
    val topEdge = light.centerY + dy - light.diameter / 2f
    return ((topEdge - scene.calmEnd) / scene.calmFade).coerceIn(0f, 1f)
}

/** A light's rise at [timeMs]. */
fun lightDy(
    scene: SceneModel,
    light: LightModel,
    timeMs: Long,
): Float = lightDyAt(scene, lightPhase(light, timeMs))

/** A light's alpha at [timeMs]: opacity × envelope × calm. */
fun lightAlpha(
    scene: SceneModel,
    light: LightModel,
    timeMs: Long,
): Float {
    val phase = lightPhase(light, timeMs)
    return light.opacity * lightEnvelope(phase) * lightCalm(scene, light, lightDyAt(scene, phase))
}

/** A light's rest alpha: at its rest centre, where the envelope is 1. */
fun lightRestAlpha(
    scene: SceneModel,
    light: LightModel,
): Float = light.opacity * lightCalm(scene, light, LIGHT_REST_DY)

/** A light's rise at rest: at its rest centre. */
const val LIGHT_REST_DY = 0f

/**
 * The gel sheen's travel across its tile at [timeMs], 0 wholly before the start edge to 1 wholly
 * past the end edge, `FastOutSlowIn` over the last quarter of each loop; [SHEEN_HIDDEN] for the
 * rest of it. A tile [delayMs] behind lags by that much (Challenge follows Classic).
 */
fun gelSheenProgress(
    timeMs: Long,
    delayMs: Long,
): Float {
    val inLoop = (timeMs - delayMs).mod(BackdropMotion.GEL_SHEEN_PERIOD_MS)
    val loop = inLoop.toFloat() / BackdropMotion.GEL_SHEEN_PERIOD_MS
    if (loop < BackdropMetrics.GEL_SHEEN_START) return SHEEN_HIDDEN
    val pass = (loop - BackdropMetrics.GEL_SHEEN_START) / (1f - BackdropMetrics.GEL_SHEEN_START)
    return FastOutSlowInEasing.transform(pass)
}
