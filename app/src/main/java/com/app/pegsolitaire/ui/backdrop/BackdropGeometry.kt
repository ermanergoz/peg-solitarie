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
): SceneModel = SceneModel(density, width, height, emptyList(), emptyList(), emptyList(), 0f, 1f, 0f, 0f, 0f)
