package com.app.pegsolitaire.ui.results

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * One pose of the Results rays (ux-spec §4.7, "Pure model"): the burst's clockwise turn and each
 * wedge's alpha multiplier on the shared envelope, in wedge order.
 */
@Immutable
internal data class RaysPose(
    val angleDegrees: Float,
    val wedgeAlphas: List<Float>,
)

/**
 * The rays' pure model (ux-spec D-RC-1 to D-RC-3, §4.7): geometry, motion, the radial envelope and the
 * rest pose, as functions of the clock time, the window and the centre only (B-24's reading), so a
 * test sets a time and asserts a pose without a frame clock or a pixel (BD-11's idiom). The draw
 * reads these per frame; none of them allocates except [poseAt], which only tests call.
 */
internal object ResultsRays {
    /** Wedges in the burst. */
    const val WEDGES = CelebrationFx.RAY_COUNT

    /** The brush's core stop: full strength to 0.10 H on a gradient whose radius is 0.60 H. */
    const val CORE_STOP = CelebrationFx.RAY_CORE_RADIUS_FRACTION / CelebrationFx.RAY_FADE_RADIUS_FRACTION

    private const val PITCH_DEGREES = CelebrationFx.FULL_TURN_DEGREES / WEDGES
    private const val HALF_WEDGE_DEGREES = CelebrationFx.RAY_WEDGE_DEGREES / 2
    private const val TWO_PI = 2 * PI
    private const val SHIMMER_MEAN = 1f - CelebrationFx.RAY_SHIMMER_DEPTH / 2

    /** Wedge [wedge]'s bisector, clockwise from straight up, at angle 0. */
    fun bisectorDegrees(wedge: Int): Float = wedge * PITCH_DEGREES

    /** Wedge [wedge]'s anticlockwise edge. */
    fun startEdgeDegrees(wedge: Int): Float = bisectorDegrees(wedge) - HALF_WEDGE_DEGREES

    /** Wedge [wedge]'s clockwise edge. */
    fun endEdgeDegrees(wedge: Int): Float = bisectorDegrees(wedge) + HALF_WEDGE_DEGREES

    /** The burst's clockwise turn at [timeMs]: linear, one revolution per 48 s. */
    fun angleAt(timeMs: Long): Float {
        val period = CelebrationFx.RAY_ROTATION_PERIOD_MS
        return CelebrationFx.FULL_TURN_DEGREES * (timeMs.mod(period)).toFloat() / period
    }

    /**
     * Wedge [wedge]'s alpha multiplier at [timeMs]: a 2.8 s sine between 0.60 and 1.00, each wedge a
     * twelfth of a period behind the last (RC-3), so the brightening walks clockwise round the burst.
     */
    fun shimmerAt(
        timeMs: Long,
        wedge: Int,
    ): Float {
        val period = CelebrationFx.RAY_SHIMMER_PERIOD_MS
        val phase = timeMs.mod(period).toDouble() / period - wedge.toDouble() / WEDGES
        val wave = sin(TWO_PI * phase).toFloat()
        return 1f - CelebrationFx.RAY_SHIMMER_DEPTH * (1f - wave) / 2
    }

    /** Wedge [wedge]'s multiplier at [timeMs], or the shimmer's mean on the rest pose. */
    fun wedgeAlpha(
        timeMs: Long,
        wedge: Int,
        isStatic: Boolean,
    ): Float = if (isStatic) SHIMMER_MEAN else shimmerAt(timeMs, wedge)

    /** The burst's turn at [timeMs], or the rest angle on the rest pose. */
    fun angle(
        timeMs: Long,
        isStatic: Boolean,
    ): Float = if (isStatic) CelebrationFx.RAY_REST_ANGLE_DEGREES else angleAt(timeMs)

    /** The whole layer's alpha: the entrance's while live, 1 from the first frame on the rest pose. */
    fun layerAlpha(
        entrance: Float,
        isStatic: Boolean,
    ): Float = if (isStatic) 1f else entrance

    /** The pose at [timeMs], or the rest pose when [isStatic]. For tests; the draw reads [angle] and [wedgeAlpha]. */
    fun poseAt(
        timeMs: Long,
        isStatic: Boolean,
    ): RaysPose = RaysPose(angle(timeMs, isStatic), List(WEDGES) { wedgeAlpha(timeMs, it, isStatic) })

    /**
     * The envelope the shared radial brush draws at [radiusPx] from the centre: 1 to 0.10 H, linear to
     * 0 at 0.60 H, 0 beyond. The brush itself is built from [CORE_STOP] on a 0.60 H radius; this is
     * its value, named for the tests.
     */
    fun envelope(
        radiusPx: Float,
        heightPx: Float,
    ): Float {
        val core = CelebrationFx.RAY_CORE_RADIUS_FRACTION * heightPx
        val fade = CelebrationFx.RAY_FADE_RADIUS_FRACTION * heightPx
        return when {
            radiusPx <= core -> 1f
            radiusPx >= fade -> 0f
            else -> (fade - radiusPx) / (fade - core)
        }
    }
}

/**
 * Where the burst sits on the window (ux-spec §4.7, "Centre" and "Reach"): pure functions of the
 * reported positions and the window.
 */
internal object RaysGeometry {
    private const val HALF_WEDGE_DEGREES = CelebrationFx.RAY_WEDGE_DEGREES / 2

    /**
     * The burst's centre in the layer's space: the star row's centre once both it and the layer have
     * reported their positions in the root, `(0.5 W, 0.42 H)` before.
     */
    fun centre(
        rowCentreInRoot: Offset,
        layerOriginInRoot: Offset,
        layerSize: Size,
    ): Offset =
        if (rowCentreInRoot.isSpecified && layerOriginInRoot.isSpecified) {
            rowCentreInRoot - layerOriginInRoot
        } else {
            Offset(
                layerSize.width * CelebrationFx.RAY_FALLBACK_CENTRE_X,
                layerSize.height * CelebrationFx.RAY_FALLBACK_CENTRE_Y,
            )
        }

    /**
     * How far a wedge's two long edges run from [centre]: past the farthest window corner, divided by
     * the cosine of the half-wedge so even the far edge's nearest point clears it. No end of a wedge
     * is on screen at any turn of the rotation (§4.7, "Reach").
     */
    fun reach(
        centre: Offset,
        window: Size,
    ): Float {
        val farX = maxOf(centre.x, window.width - centre.x)
        val farY = maxOf(centre.y, window.height - centre.y)
        return hypot(farX, farY) / cos(Math.toRadians(HALF_WEDGE_DEGREES.toDouble())).toFloat()
    }
}
