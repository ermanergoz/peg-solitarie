package com.app.pegsolitaire.ui.backdrop

import androidx.compose.runtime.Immutable
import kotlin.math.abs

/**
 * The backdrop scene as plain px primitives (the tech-lead's A1): what [buildSceneModel] returns
 * and what the draw layer turns into paths and brushes once, in its cache block. It imports no
 * Compose graphics and no `android.*`, so BD-10 and BD-11 test it without composing anything.
 *
 * Every x is already mirrored for right-to-left inside the builder, so the draw layer never
 * branches on direction (B-15).
 */
@Immutable
data class SceneModel(
    val density: BackdropDensity,
    val width: Float,
    val height: Float,
    val sky: List<SkyStop>,
    /** The back wave, then the front wave; empty on Sparse. */
    val waves: List<WaveModel>,
    /** The density's lights in table order; empty on Sparse. */
    val lights: List<LightModel>,
    /** The calm band's end: above it the sky alone (B-20). */
    val calmEnd: Float,
    /** The distance over which a light fades out before its top edge reaches [calmEnd]. */
    val calmFade: Float,
    /** How far below its rest centre a light begins its rise. */
    val riseBelow: Float,
    /** How far above its rest centre a light ends its rise. */
    val riseAbove: Float,
    /** The phase at which a light sits at its rest centre. */
    val restPhase: Float,
)

/**
 * One wave at its rest pose (candy-backdrop.md §3.2): the upper half of an ellipse whose top is
 * the crest, unioned with the rectangle beneath it down to [bottom]. Its pose only translates it,
 * by its bob amplitudes times `−cos(π t / T)`.
 */
@Immutable
data class WaveModel(
    val crestX: Float,
    val crestY: Float,
    val radiusX: Float,
    val radiusY: Float,
    /** The window's bottom edge plus this wave's vertical bob, so no pose shows a gap below it. */
    val bottom: Float,
    val lipDepth: Float,
    val sheenCenterY: Float,
    val sheenRadiusX: Float,
    val sheenRadiusY: Float,
    /** Where the fill takes its body colour, then its deep colour, at rest. */
    val bodyStopY: Float,
    val deepStopY: Float,
    /** The signed bob at swing +1; mirrored across in right-to-left. */
    val bobX: Float,
    val bobY: Float,
    val halfCycleMs: Long,
    /** The per-draw alpha of each of this wave's three draws. */
    val alpha: Float,
) {
    val left: Float get() = crestX - radiusX
    val right: Float get() = crestX + radiusX
}

/** One light at its rest centre, in px. */
@Immutable
data class LightModel(
    val centerX: Float,
    val centerY: Float,
    val diameter: Float,
    val kind: LightKind,
    val periodMs: Long,
    val phase0: Float,
    val opacity: Float,
    val set: LightSet,
)

/**
 * Builds [density]'s scene for a [width] × [height] px window from [BackdropMetrics],
 * [BackdropMotion] and [dimens], mirrored when [rtl]. Pure: the same inputs give the same model,
 * and nothing here reads game state, a clock, a random source or a resource.
 */
fun buildSceneModel(
    density: BackdropDensity,
    width: Float,
    height: Float,
    rtl: Boolean,
    dimens: BackdropDimens,
): SceneModel {
    val mirror: (Float) -> Float = { x -> if (rtl) width - x else x }
    val acrossSign = if (rtl) -1f else 1f
    return SceneModel(
        density = density,
        width = width,
        height = height,
        sky = skyStopsOf(density),
        waves = wavesOf(density).map { it.toModel(width, height, dimens, mirror, acrossSign) },
        lights =
            lightsOf(density).map { (spec, period) ->
                LightModel(
                    centerX = mirror(spec.x * width),
                    centerY = spec.y * height,
                    diameter = dimens.diameter(spec.size),
                    kind = spec.kind,
                    periodMs = period,
                    phase0 = spec.phase0,
                    opacity = spec.opacity,
                    set = spec.set,
                )
            },
        calmEnd = calmEndOf(density) * height,
        calmFade = dimens.lightCalmFade,
        riseBelow = dimens.lightRiseBelow,
        riseAbove = dimens.lightRiseAbove,
        restPhase = dimens.lightRestPhase,
    )
}

private fun skyStopsOf(density: BackdropDensity): List<SkyStop> =
    when (density) {
        BackdropDensity.Landscape -> BackdropMetrics.LANDSCAPE_SKY_STOPS
        BackdropDensity.Quiet -> BackdropMetrics.QUIET_SKY_STOPS
        BackdropDensity.Sparse -> BackdropMetrics.SPARSE_SKY_STOPS
    }

private fun wavesOf(density: BackdropDensity): List<WaveSpec> =
    when (density) {
        BackdropDensity.Landscape ->
            listOf(
                WaveSpec(BackdropMetrics.LANDSCAPE_WAVE_BACK, BackdropMotion.LANDSCAPE_WAVE_BACK_HALF_MS, back = true),
                WaveSpec(
                    BackdropMetrics.LANDSCAPE_WAVE_FRONT,
                    BackdropMotion.LANDSCAPE_WAVE_FRONT_HALF_MS,
                    back = false,
                ),
            )
        BackdropDensity.Quiet ->
            listOf(
                WaveSpec(BackdropMetrics.QUIET_WAVE_BACK, BackdropMotion.QUIET_WAVE_BACK_HALF_MS, back = true),
                WaveSpec(BackdropMetrics.QUIET_WAVE_FRONT, BackdropMotion.QUIET_WAVE_FRONT_HALF_MS, back = false),
            )
        BackdropDensity.Sparse -> emptyList()
    }

private fun lightsOf(density: BackdropDensity): List<Pair<LightSpec, Long>> =
    when (density) {
        BackdropDensity.Landscape -> BackdropMetrics.LANDSCAPE_LIGHTS.zip(BackdropMotion.LANDSCAPE_LIGHT_PERIODS_MS)
        BackdropDensity.Quiet -> BackdropMetrics.QUIET_LIGHTS.zip(BackdropMotion.QUIET_LIGHT_PERIODS_MS)
        BackdropDensity.Sparse -> emptyList()
    }

/** Sparse has no light, so its calm end is never read; 0 keeps the model total. */
private fun calmEndOf(density: BackdropDensity): Float =
    when (density) {
        BackdropDensity.Landscape -> BackdropMetrics.LANDSCAPE_CALM_END
        BackdropDensity.Quiet -> BackdropMetrics.QUIET_CALM_END
        BackdropDensity.Sparse -> 0f
    }

/** Which of the two waves, before px: its placement, half-cycle and the sense of its bob. */
private data class WaveSpec(
    val placement: WavePlacement,
    val halfCycleMs: Long,
    val back: Boolean,
)

/**
 * The back wave drifts toward the end edge and up as its swing rises, the front wave toward the
 * start edge and down (candy-backdrop.md §3.4): `(+24dp, −4dp)` and `(−20dp, +4dp)` at swing +1.
 */
private fun WaveSpec.toModel(
    width: Float,
    height: Float,
    dimens: BackdropDimens,
    mirror: (Float) -> Float,
    acrossSign: Float,
): WaveModel {
    val bobX = if (back) dimens.waveBackBobX else -dimens.waveFrontBobX
    val bobY = if (back) -dimens.waveBackBobY else dimens.waveFrontBobY
    val crestY = placement.crest * height
    val depth = BackdropMetrics.WAVE_DEPTH * height
    return WaveModel(
        crestX = mirror(placement.cx * width),
        crestY = crestY,
        radiusX = BackdropMetrics.WAVE_RX * width,
        radiusY = BackdropMetrics.WAVE_RY * height,
        bottom = height + abs(bobY),
        lipDepth = dimens.waveLipDepth,
        sheenCenterY = crestY + dimens.waveSheenOffset + dimens.waveSheenHeight / 2f,
        sheenRadiusX = BackdropMetrics.WAVE_SHEEN_HALF_WIDTH * width,
        sheenRadiusY = dimens.waveSheenHeight / 2f,
        bodyStopY = crestY + BackdropMetrics.WAVE_BODY_STOP * depth,
        deepStopY = crestY + depth,
        bobX = bobX * acrossSign,
        bobY = bobY,
        halfCycleMs = halfCycleMs,
        alpha = if (back) BackdropMetrics.WAVE_BACK_ALPHA else FRONT_WAVE_ALPHA,
    )
}

/** The front wave draws at full alpha over the back one. */
private const val FRONT_WAVE_ALPHA = 1f
