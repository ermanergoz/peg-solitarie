package com.app.pegsolitaire.ui.backdrop

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * BD-10: the composition constants are backdrop spec §3.3, §4.3 and §4.4, transcribed here from
 * the spec's tables, so a drifted constant fails rather than moving the scene.
 */
class BackdropMetricsTest {
    @Test
    fun eachDensitysSkyStopsAreTheSpecs() {
        val deep =
            listOf(
                0f to SkyRole.Top,
                0.12f to SkyRole.Top,
                0.34f to SkyRole.Mid,
                0.66f to SkyRole.Low,
                1f to SkyRole.Deep,
            )
        assertThat(BackdropMetrics.LANDSCAPE_SKY_STOPS.map { it.fraction to it.role }).isEqualTo(deep)
        assertThat(BackdropMetrics.QUIET_SKY_STOPS.map { it.fraction to it.role }).isEqualTo(deep)
        assertThat(BackdropMetrics.SPARSE_SKY_STOPS.map { it.fraction to it.role })
            .isEqualTo(listOf(0f to SkyRole.Top, 0.12f to SkyRole.Top, 1f to SkyRole.SparseLow))
    }

    @Test
    fun theCalmEndsTheTitlesReachAndTheSpinnersBandAreTheSpecs() {
        assertThat(BackdropMetrics.LANDSCAPE_CALM_END).isEqualTo(0.14f)
        assertThat(BackdropMetrics.QUIET_CALM_END).isEqualTo(0.14f)
        assertThat(BackdropMetrics.TITLE_REACH).isEqualTo(0.47f)
        assertThat(BackdropMetrics.QUIET_SPINNER_BAND).isEqualTo(0.33f..0.59f)
    }

    @Test
    fun eachDensitysWavesAreTheSpecs() {
        assertThat(listOf(BackdropMetrics.WAVE_RX, BackdropMetrics.WAVE_RY, BackdropMetrics.WAVE_DEPTH))
            .containsExactly(0.75f, 0.112f, 0.70f)
            .inOrder()
        val body = BackdropMetrics.run { listOf(WAVE_BODY_STOP, WAVE_BACK_ALPHA, WAVE_SHEEN_HALF_WIDTH) }
        assertThat(body)
            .containsExactly(0.18f, 0.60f, 0.42f)
            .inOrder()
        assertThat(BackdropMetrics.LANDSCAPE_WAVE_BACK).isEqualTo(WavePlacement(0.50f, 0.49f))
        assertThat(BackdropMetrics.LANDSCAPE_WAVE_FRONT).isEqualTo(WavePlacement(0.35f, 0.62f))
        assertThat(BackdropMetrics.QUIET_WAVE_BACK).isEqualTo(WavePlacement(0.50f, 0.72f))
        assertThat(BackdropMetrics.QUIET_WAVE_FRONT).isEqualTo(WavePlacement(0.35f, 0.80f))
        assertThat(BackdropMotion.LANDSCAPE_WAVE_BACK_HALF_MS).isEqualTo(12_000L)
        assertThat(BackdropMotion.LANDSCAPE_WAVE_FRONT_HALF_MS).isEqualTo(9_000L)
        assertThat(BackdropMotion.QUIET_WAVE_BACK_HALF_MS).isEqualTo(18_000L)
        assertThat(BackdropMotion.QUIET_WAVE_FRONT_HALF_MS).isEqualTo(14_000L)
    }

    @Test
    fun eachDensitysLightsAreTheSpecs() {
        assertThat(BackdropMetrics.LANDSCAPE_LIGHTS).isEqualTo(LANDSCAPE_TABLE)
        assertThat(BackdropMotion.LANDSCAPE_LIGHT_PERIODS_MS)
            .containsExactly(30_000L, 24_000L, 36_000L, 26_000L, 34_000L, 40_000L, 28_000L, 32_000L, 22_000L)
            .inOrder()
        assertThat(BackdropMetrics.QUIET_LIGHTS).isEqualTo(QUIET_TABLE)
        assertThat(BackdropMotion.QUIET_LIGHT_PERIODS_MS)
            .containsExactly(36_000L, 42_000L, 30_000L, 44_000L, 38_000L)
            .inOrder()
        assertThat(listOf(BackdropMetrics.LIGHT_CORE_STOP, BackdropMetrics.LIGHT_RIM_STOP))
            .containsExactly(0.64f, 0.84f)
            .inOrder()
        assertThat(listOf(BackdropMetrics.LIGHT_FADE_IN, BackdropMetrics.LIGHT_FADE_OUT))
            .containsExactly(0.15f, 0.85f)
            .inOrder()
    }

    @Test
    fun theFinishGelSheenSettleAndHostConstantsAreTheSpecs() {
        assertThat(BackdropMetrics.FINISH_GLOSS_FADE).isEqualTo(0.46f)
        val sheen = BackdropMetrics.run { listOf(GEL_SHEEN_WIDTH, GEL_SHEEN_SKEW_DEG, GEL_SHEEN_START) }
        assertThat(sheen)
            .containsExactly(0.30f, 14f, 0.75f)
            .inOrder()
        assertThat(BackdropMotion.GEL_SHEEN_PERIOD_MS).isEqualTo(10_000L)
        assertThat(BackdropMotion.GEL_SHEEN_CHALLENGE_DELAY_MS).isEqualTo(600L)
        assertThat(BackdropMotion.BACKDROP_HOME_IDLE_MS).isEqualTo(60_000L)
        assertThat(BackdropMotion.BACKDROP_GAME_IDLE_MS).isEqualTo(20_000L)
        assertThat(BackdropMotion.BACKDROP_SETTLE_MS).isEqualTo(2_000L)
        assertThat(BackdropMetrics.BACKDROP_Z_INDEX).isEqualTo(-1f)
    }

    @Test
    fun everyLoopIsAtLeastNineSeconds() {
        assertThat(BackdropMotion.MIN_LOOP_MS).isEqualTo(9_000L)
        val loops =
            listOf(
                BackdropMotion.LANDSCAPE_WAVE_BACK_HALF_MS,
                BackdropMotion.LANDSCAPE_WAVE_FRONT_HALF_MS,
                BackdropMotion.QUIET_WAVE_BACK_HALF_MS,
                BackdropMotion.QUIET_WAVE_FRONT_HALF_MS,
                BackdropMotion.GEL_SHEEN_PERIOD_MS,
            ) + BackdropMotion.LANDSCAPE_LIGHT_PERIODS_MS + BackdropMotion.QUIET_LIGHT_PERIODS_MS
        loops.forEach { assertWithMessage("loop $it ms").that(it).isAtLeast(BackdropMotion.MIN_LOOP_MS) }
    }

    private companion object {
        val LANDSCAPE_TABLE =
            listOf(
                LightSpec(0.08f, 0.27f, LightSize.Md, LightKind.Pearl, 0.13f, 0.90f, LightSet.Lane),
                LightSpec(0.22f, 0.40f, LightSize.Xs, LightKind.Pink, 0.67f, 0.85f, LightSet.Lane),
                LightSpec(0.50f, 0.33f, LightSize.Xl, LightKind.Violet, 0.30f, 0.70f, LightSet.Lane),
                LightSpec(0.76f, 0.22f, LightSize.Sm, LightKind.Pink, 0.42f, 0.85f, LightSet.Lane),
                LightSpec(0.92f, 0.37f, LightSize.Lg, LightKind.Violet, 0.06f, 0.75f, LightSet.Lane),
                LightSpec(0.39f, 0.91f, LightSize.Xxl, LightKind.Violet, 0.20f, 0.55f, LightSet.Low),
                LightSpec(0.92f, 0.90f, LightSize.Sm, LightKind.Pink, 0.50f, 0.80f, LightSet.Low),
                LightSpec(0.16f, 0.67f, LightSize.Md, LightKind.Pearl, 0.19f, 0.60f, LightSet.Low),
                LightSpec(0.50f, 0.94f, LightSize.Xs, LightKind.Pearl, 0.41f, 0.80f, LightSet.Low),
            )
        val QUIET_TABLE =
            listOf(
                LightSpec(0.13f, 0.19f, LightSize.Sm, LightKind.Pearl, 0.14f, 0.80f, LightSet.Upper),
                LightSpec(0.81f, 0.23f, LightSize.Lg, LightKind.Violet, 0.43f, 0.70f, LightSet.Upper),
                LightSpec(0.46f, 0.25f, LightSize.Xs, LightKind.Pink, 0.33f, 0.80f, LightSet.Upper),
                LightSpec(0.24f, 0.77f, LightSize.Lg, LightKind.Violet, 0.27f, 0.60f, LightSet.Lower),
                LightSpec(0.82f, 0.79f, LightSize.Md, LightKind.Pink, 0.63f, 0.80f, LightSet.Lower),
            )
    }
}
