package com.app.pegsolitaire.ui.backdrop

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

/**
 * BD-11 (g) and BD-16 (a), (b) on the pure pose functions (candy-backdrop.md §3.4): the rest poses,
 * the envelope's wrap, the sheen's quarter, every drifting speed ≤ 8dp/s, and exact `Long` time.
 * Dimensions are the spec's dp at one px per dp, so a px speed is a dp speed.
 */
class BackdropPoseTest {
    private val dimens =
        BackdropDimens(
            lightXs = 28f, lightSm = 36f, lightMd = 44f, lightLg = 52f, lightXl = 64f, lightXxl = 76f,
            lightRiseBelow = 32f, lightRiseAbove = 80f, lightCalmFade = 48f,
            waveLipDepth = 4f, waveSheenOffset = 8f, waveSheenHeight = 24f,
            waveBackBobX = 24f, waveBackBobY = 4f, waveFrontBobX = 20f, waveFrontBobY = 4f,
        )
    private val scenes =
        listOf(BackdropDensity.Landscape, BackdropDensity.Quiet).map {
            buildSceneModel(it, width = 412f, height = 915f, rtl = false, dimens = dimens)
        }

    @Test
    fun aWaveSwingsBetweenItsBobLimitsAndIsStillAtRest() {
        assertThat(WAVE_REST_OFFSET).isEqualTo(0f)
        scenes.flatMap { it.waves }.forEach { wave ->
            assertWithMessage("starts at swing -1").that(waveSwing(wave, 0L)).isWithin(EPS).of(-1f)
            assertWithMessage("+1 a half-cycle later").that(waveSwing(wave, wave.halfCycleMs)).isWithin(EPS).of(1f)
            (0L..wave.halfCycleMs * 2 step STEP_MS).forEach { t ->
                assertThat(abs(waveOffsetX(wave, t))).isAtMost(abs(wave.bobX) + EPS)
                assertThat(abs(waveOffsetY(wave, t))).isAtMost(abs(wave.bobY) + EPS)
            }
        }
    }

    @Test
    fun theTwoWavesMoveAgainstEachOther() {
        scenes.forEach { scene ->
            val (back, front) = scene.waves
            assertWithMessage("back goes to the end edge and up").that(back.bobX > 0 && back.bobY < 0).isTrue()
            assertWithMessage("front goes to the start edge and down").that(front.bobX < 0 && front.bobY > 0).isTrue()
        }
    }

    @Test
    fun aLightWrapsOnlyWhileInvisible() {
        assertThat(lightEnvelope(0f)).isEqualTo(0f)
        assertThat(lightEnvelope(WRAP_EDGE)).isLessThan(EPS)
        assertThat(lightEnvelope(BackdropMetrics.LIGHT_FADE_IN)).isEqualTo(1f)
        assertThat(lightEnvelope(BackdropMetrics.LIGHT_FADE_OUT)).isEqualTo(1f)
        scenes.forEach { scene ->
            scene.lights.forEach { light ->
                // The phase wraps from just below 1 to 0 between these two times.
                val wrapAt = ((1f - light.phase0) * light.periodMs).toLong()
                val before = lightAlpha(scene, light, wrapAt - 1)
                assertWithMessage("alpha just before the wrap").that(before).isLessThan(INVISIBLE_ALPHA)
                val after = lightAlpha(scene, light, wrapAt + 1)
                assertWithMessage("alpha at the wrap").that(after).isLessThan(INVISIBLE_ALPHA)
            }
        }
    }

    @Test
    fun theRestPoseIsTheReducedMotionPicture() {
        scenes.forEach { scene ->
            assertWithMessage("rest phase").that(scene.restPhase).isWithin(EPS).of(32f / 112f)
            assertWithMessage("dy at rest").that(lightDyAt(scene, scene.restPhase)).isWithin(EPS).of(LIGHT_REST_DY)
            assertWithMessage("envelope at rest").that(lightEnvelope(scene.restPhase)).isEqualTo(1f)
            scene.lights.forEach { light ->
                assertThat(lightRestAlpha(scene, light)).isEqualTo(light.opacity * lightCalm(scene, light, 0f))
            }
        }
    }

    @Test
    fun theSheenPassesOnlyInTheLastQuarterOfItsLoop() {
        val start = (BackdropMotion.GEL_SHEEN_PERIOD_MS * BackdropMetrics.GEL_SHEEN_START).toLong()
        (0L until start step STEP_MS).forEach { t -> assertThat(gelSheenProgress(t, 0L)).isEqualTo(SHEEN_HIDDEN) }
        var last = -1f
        (start until BackdropMotion.GEL_SHEEN_PERIOD_MS step STEP_MS).forEach { t ->
            val p = gelSheenProgress(t, 0L)
            assertThat(p).isAtLeast(last)
            assertThat(p).isAtLeast(0f)
            assertThat(p).isAtMost(1f)
            last = p
        }
        assertThat(gelSheenProgress(start, 0L)).isWithin(EPS).of(0f)
        (0L..BackdropMotion.GEL_SHEEN_PERIOD_MS step STEP_MS).forEach { t ->
            val delay = BackdropMotion.GEL_SHEEN_CHALLENGE_DELAY_MS
            val challenge = gelSheenProgress(t + delay, delay)
            assertWithMessage("Challenge follows Classic").that(challenge).isEqualTo(gelSheenProgress(t, 0L))
        }
    }

    @Test
    fun everyDriftingElementMovesAtNoMoreThanEightDpASecond() {
        scenes.forEach { scene ->
            scene.waves.forEach { wave ->
                (0L..wave.halfCycleMs * 2 step STEP_MS).forEach { t ->
                    val dx = waveOffsetX(wave, t + STEP_MS) - waveOffsetX(wave, t)
                    val dy = waveOffsetY(wave, t + STEP_MS) - waveOffsetY(wave, t)
                    assertThat(hypot(dx, dy) / STEP_SECONDS).isAtMost(MAX_DP_PER_S)
                }
            }
            scene.lights.forEach { light ->
                (0L..light.periodMs step STEP_MS).forEach { t ->
                    val wraps = lightPhase(light, t + STEP_MS) < lightPhase(light, t)
                    if (!wraps) {
                        val speed = abs(lightDy(scene, light, t + STEP_MS) - lightDy(scene, light, t)) / STEP_SECONDS
                        assertThat(speed).isAtMost(MAX_DP_PER_S)
                    }
                }
            }
        }
    }

    @Test
    fun timeIsExactForASessionOfAnyLength() {
        scenes.forEach { scene ->
            scene.waves.forEach { wave ->
                val cycles = LONG_SESSION_MS / (wave.halfCycleMs * 2)
                val late = cycles * wave.halfCycleMs * 2 + OFFSET_MS
                assertThat(waveSwing(wave, late)).isEqualTo(waveSwing(wave, OFFSET_MS))
            }
            scene.lights.forEach { light ->
                val periods = LONG_SESSION_MS / light.periodMs
                val late = periods * light.periodMs + OFFSET_MS
                assertThat(lightPhase(light, late)).isEqualTo(lightPhase(light, OFFSET_MS))
            }
        }
        val loops = LONG_SESSION_MS / BackdropMotion.GEL_SHEEN_PERIOD_MS
        val late = loops * BackdropMotion.GEL_SHEEN_PERIOD_MS + SHEEN_OFFSET_MS
        assertThat(gelSheenProgress(late, 0L)).isEqualTo(gelSheenProgress(SHEEN_OFFSET_MS, 0L))
    }

    private companion object {
        const val EPS = 1e-4f

        /** A light at a thousandth of its alpha draws nothing a player can see, within 1 ms of its wrap. */
        const val INVISIBLE_ALPHA = 1e-3f
        const val WRAP_EDGE = 0.99999f
        const val STEP_MS = 10L
        const val STEP_SECONDS = 0.010f
        const val MAX_DP_PER_S = 8f
        const val LONG_SESSION_MS = 1_000_000_000L
        const val OFFSET_MS = 1_234L
        const val SHEEN_OFFSET_MS = 8_765L
    }
}
