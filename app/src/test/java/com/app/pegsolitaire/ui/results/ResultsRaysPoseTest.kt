package com.app.pegsolitaire.ui.results

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.app.pegsolitaire.ui.theme.DarkClayColors
import com.app.pegsolitaire.ui.theme.DarkColors
import com.app.pegsolitaire.ui.theme.LightClayColors
import com.app.pegsolitaire.ui.theme.LightColors
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot

/**
 * The rays' pure pose model (ux-spec D-RC-1 to D-RC-3, §4.7; criteria RC-2, RC-3): geometry, motion,
 * the radial envelope, the rest pose and the two peaks, asserted without a frame clock or a pixel.
 */
class ResultsRaysPoseTest {
    @Test
    fun theBurstIsTwelveWedgesOfFifteenDegreesOnAThirtyDegreePitch() {
        assertWithMessage("wedges").that(ResultsRays.WEDGES).isEqualTo(12)
        repeat(ResultsRays.WEDGES) { wedge ->
            val bisector = ResultsRays.bisectorDegrees(wedge)
            assertWithMessage("wedge $wedge's bisector").that(bisector).isWithin(EPSILON).of(wedge * PITCH)
            val width = ResultsRays.endEdgeDegrees(wedge) - ResultsRays.startEdgeDegrees(wedge)
            assertWithMessage("wedge $wedge's width").that(width).isWithin(EPSILON).of(WEDGE)
            assertWithMessage("wedge $wedge is centred on its bisector")
                .that((ResultsRays.startEdgeDegrees(wedge) + ResultsRays.endEdgeDegrees(wedge)) / 2)
                .isWithin(EPSILON)
                .of(bisector)
        }
    }

    @Test
    fun theBurstTurnsSevenAndAHalfDegreesASecondAndRepeatsEveryFortyEightSeconds() {
        assertWithMessage("angle at 0").that(ResultsRays.angleAt(0L)).isWithin(EPSILON).of(0f)
        assertWithMessage("angle after 1 s")
            .that(ResultsRays.angleAt(ONE_SECOND))
            .isWithin(EPSILON)
            .of(DEGREES_PER_SECOND)
        assertWithMessage("one pitch every 4 s").that(ResultsRays.angleAt(FOUR_SECONDS)).isWithin(EPSILON).of(PITCH)
        for (t in listOf(0L, 1_234L, 17_000L, 47_999L)) {
            assertWithMessage("angle($t + period) == angle($t)")
                .that(ResultsRays.angleAt(t + PERIOD))
                .isWithin(EPSILON)
                .of(ResultsRays.angleAt(t))
        }
    }

    @Test
    fun eachWedgeBreathesBetweenSixtyAndOneHundredPercentATwelfthOfAPeriodBehindTheLast() {
        var lowest = Float.MAX_VALUE
        var highest = -Float.MAX_VALUE
        for (t in 0L..SHIMMER step SHIMMER_SAMPLE_STEP) {
            repeat(ResultsRays.WEDGES) { wedge ->
                val shimmer = ResultsRays.shimmerAt(t, wedge)
                lowest = minOf(lowest, shimmer)
                highest = maxOf(highest, shimmer)
                assertWithMessage("wedge $wedge lags wedge 0 by $wedge twelfths at $t")
                    .that(shimmer)
                    .isWithin(EPSILON)
                    .of(ResultsRays.shimmerAt(t + SHIMMER * (ResultsRays.WEDGES - wedge) / ResultsRays.WEDGES, 0))
            }
        }
        assertWithMessage("the breath's floor").that(lowest).isWithin(SHIMMER_SAMPLING_SLACK).of(SHIMMER_MIN)
        assertWithMessage("the breath's floor is never crossed").that(lowest).isAtLeast(SHIMMER_MIN - EPSILON)
        assertWithMessage("the breath's ceiling").that(highest).isWithin(SHIMMER_SAMPLING_SLACK).of(SHIMMER_MAX)
        assertWithMessage("the breath's ceiling is never crossed").that(highest).isAtMost(SHIMMER_MAX + EPSILON)
    }

    @Test
    fun theEnvelopeIsFullToATenthOfTheHeightAndGoneFromSixTenths() {
        assertWithMessage("at the centre").that(ResultsRays.envelope(0f, HEIGHT)).isWithin(EPSILON).of(1f)
        assertWithMessage("at 0.10 H").that(ResultsRays.envelope(CORE * HEIGHT, HEIGHT)).isWithin(EPSILON).of(1f)
        assertWithMessage("at 0.60 H").that(ResultsRays.envelope(FADE * HEIGHT, HEIGHT)).isWithin(EPSILON).of(0f)
        assertWithMessage("past 0.60 H").that(ResultsRays.envelope(HEIGHT, HEIGHT)).isWithin(EPSILON).of(0f)
        var previous = 1f
        for (step in 0..ENVELOPE_STEPS) {
            val radius = HEIGHT * (CORE + (FADE - CORE) * step / ENVELOPE_STEPS)
            val value = ResultsRays.envelope(radius, HEIGHT)
            assertWithMessage("monotone at $radius").that(value).isAtMost(previous + EPSILON)
            previous = value
        }
        assertWithMessage("linear: half-way at 0.35 H")
            .that(ResultsRays.envelope(MID * HEIGHT, HEIGHT))
            .isWithin(EPSILON)
            .of(HALF)
        assertWithMessage("the brush's core stop").that(ResultsRays.CORE_STOP).isWithin(EPSILON).of(CORE / FADE)
    }

    @Test
    fun theRestPoseIsWedgeZeroStraightUpEveryWedgeAtTheMeanAndNoFade() {
        val rest = ResultsRays.poseAt(timeMs = UNEVEN_TIME, isStatic = true)
        assertWithMessage("rest angle").that(rest.angleDegrees).isWithin(EPSILON).of(0f)
        assertWithMessage("rest wedges").that(rest.wedgeAlphas).hasSize(ResultsRays.WEDGES)
        rest.wedgeAlphas.forEach { assertWithMessage("rest shimmer").that(it).isWithin(EPSILON).of(SHIMMER_MEAN) }
        assertWithMessage("rest layer alpha").that(ResultsRays.layerAlpha(entrance = 0f, isStatic = true)).isEqualTo(1f)
        assertWithMessage("live layer alpha follows the entrance")
            .that(ResultsRays.layerAlpha(entrance = HALF, isStatic = false))
            .isEqualTo(HALF)
        val live = ResultsRays.poseAt(timeMs = UNEVEN_TIME, isStatic = false)
        assertWithMessage("live angle").that(live.angleDegrees).isWithin(EPSILON).of(ResultsRays.angleAt(UNEVEN_TIME))
        repeat(ResultsRays.WEDGES) { wedge ->
            assertWithMessage("live wedge $wedge")
                .that(live.wedgeAlphas[wedge])
                .isWithin(EPSILON)
                .of(ResultsRays.shimmerAt(UNEVEN_TIME, wedge))
        }
    }

    @Test
    fun theCentreIsTheStarRowsOnceReportedAndTheFallbackBefore() {
        val window = Size(WIDTH, HEIGHT)
        assertWithMessage("before the row reports")
            .that(RaysGeometry.centre(Offset.Unspecified, Offset.Zero, window))
            .isEqualTo(Offset(WIDTH * FALLBACK_X, HEIGHT * FALLBACK_Y))
        assertWithMessage("before the layer reports")
            .that(RaysGeometry.centre(Offset(ROW_X, ROW_Y), Offset.Unspecified, window))
            .isEqualTo(Offset(WIDTH * FALLBACK_X, HEIGHT * FALLBACK_Y))
        assertWithMessage("the row's centre, in the layer's space")
            .that(RaysGeometry.centre(Offset(ROW_X, ROW_Y), Offset(0f, LAYER_TOP), window))
            .isEqualTo(Offset(ROW_X, ROW_Y - LAYER_TOP))
    }

    @Test
    fun noEndOfAWedgeIsEverOnScreen() {
        val window = Size(WIDTH, HEIGHT)
        for (centre in listOf(Offset(WIDTH / 2, HEIGHT * FALLBACK_Y), Offset(0f, 0f), Offset(WIDTH, HEIGHT))) {
            val nearestFarEdge = RaysGeometry.reach(centre, window) * cos(Math.toRadians(WEDGE / 2.0)).toFloat()
            for (corner in listOf(Offset.Zero, Offset(WIDTH, 0f), Offset(0f, HEIGHT), Offset(WIDTH, HEIGHT))) {
                assertWithMessage("corner $corner from $centre")
                    .that(hypot(corner.x - centre.x, corner.y - centre.y))
                    .isAtMost(nearestFarEdge)
            }
        }
    }

    @Test
    fun theRaysAreTertiaryAtTheirNamedPeakInEachTheme() {
        val light = LightClayColors.celebrationRay
        val dark = DarkClayColors.celebrationRay
        assertWithMessage("light hue").that(light.copy(alpha = 1f)).isEqualTo(LightColors.tertiary)
        assertWithMessage("light peak").that(light.alpha).isWithin(ALPHA_QUANTUM).of(PEAK_LIGHT)
        assertWithMessage("dark hue").that(dark.copy(alpha = 1f)).isEqualTo(DarkColors.tertiary)
        assertWithMessage("dark peak").that(dark.alpha).isWithin(ALPHA_QUANTUM).of(PEAK_DARK)
    }

    @Test
    fun theConfettiPaletteIsSixExistingTokensInTheirFixedOrder() {
        for ((clay, scheme) in listOf(LightClayColors to LightColors, DarkClayColors to DarkColors)) {
            assertWithMessage("palette")
                .that(clay.confettiPalette)
                .containsExactly(
                    scheme.primary,
                    scheme.secondary,
                    scheme.tertiary,
                    clay.ctaGradient.first(),
                    clay.pegGradient.first(),
                    clay.goldGradient.first(),
                ).inOrder()
        }
    }

    private companion object {
        const val EPSILON = 1e-3f
        const val WEDGE = 15f
        const val PITCH = 30f
        const val DEGREES_PER_SECOND = 7.5f
        const val ONE_SECOND = 1_000L
        const val FOUR_SECONDS = 4_000L
        const val PERIOD = 48_000L
        const val SHIMMER = 2_800L
        const val SHIMMER_SAMPLE_STEP = 7L
        const val SHIMMER_SAMPLING_SLACK = 1e-2f
        const val SHIMMER_MIN = 0.60f
        const val SHIMMER_MAX = 1.00f
        const val SHIMMER_MEAN = 0.80f
        const val HEIGHT = 2_400f
        const val WIDTH = 1_080f
        const val CORE = 0.10f
        const val FADE = 0.60f
        const val MID = 0.35f
        const val HALF = 0.5f
        const val ENVELOPE_STEPS = 50
        const val UNEVEN_TIME = 13_337L
        const val FALLBACK_X = 0.5f
        const val FALLBACK_Y = 0.42f
        const val ROW_X = 540f
        const val ROW_Y = 900f
        const val LAYER_TOP = 24f
        const val PEAK_LIGHT = 0.20f
        const val PEAK_DARK = 0.16f
        const val ALPHA_QUANTUM = 1f / 255f
    }
}
