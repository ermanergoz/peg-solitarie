package com.app.pegsolitaire.ui.backdrop

import com.app.pegsolitaire.R
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * BD-11 (a)–(f), (h), (i) on the pure builder, with [BackdropDimens] resolved from real `Resources`
 * at the spec's four windows (backdrop spec §11). Lights move only vertically, so (a)–(c) are
 * proved exactly from each light's extremes rather than sampled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [GEOMETRY_ROBOLECTRIC_SDK])
class BackdropGeometryTest {
    @Test
    fun laneLightsNeverOverlap() {
        windows { scene, _ ->
            val sets =
                when (scene.density) {
                    BackdropDensity.Landscape -> listOf(LightSet.Lane)
                    BackdropDensity.Quiet -> listOf(LightSet.Upper, LightSet.Lower)
                    BackdropDensity.Sparse -> emptyList()
                }
            sets.forEach { set ->
                val extents =
                    scene.lights
                        .filter { it.set == set }
                        .map { it.centerX - it.diameter / 2 to it.centerX + it.diameter / 2 }
                extents.forEachIndexed { i, a ->
                    extents.drop(i + 1).forEach { b ->
                        val disjoint = a.second <= b.first || b.second <= a.first
                        assertWithMessage("$set extents $a and $b").that(disjoint).isTrue()
                    }
                }
            }
        }
    }

    @Test
    fun lowLightsNeverRiseIntoTheTitlesReach() {
        windows(BackdropDensity.Landscape) { scene, _ ->
            scene.lights.filter { it.set == LightSet.Low }.forEach { light ->
                val highestTop = light.centerY - scene.riseAbove - light.diameter / 2
                assertThat(highestTop).isAtLeast(BackdropMetrics.TITLE_REACH * scene.height)
            }
        }
    }

    @Test
    fun noGameLightCrossesTheSpinnersBand() {
        windows(BackdropDensity.Quiet) { scene, _ ->
            val band = BackdropMetrics.QUIET_SPINNER_BAND
            scene.lights.forEach { light ->
                when (light.set) {
                    LightSet.Upper -> {
                        val lowestBottom = light.centerY + scene.riseBelow + light.diameter / 2
                        assertThat(lowestBottom).isAtMost(band.start * scene.height)
                    }
                    else -> {
                        val highestTop = light.centerY - scene.riseAbove - light.diameter / 2
                        assertThat(highestTop).isAtLeast(band.endInclusive * scene.height)
                    }
                }
            }
        }
    }

    @Test
    fun noWaveRisesAboveTheTitlesReachAtAnyPose() {
        windows(BackdropDensity.Landscape) { scene, _ ->
            scene.waves.forEach { wave ->
                val highest = wave.crestY - abs(wave.bobY)
                assertThat(highest).isAtLeast(BackdropMetrics.TITLE_REACH * scene.height)
            }
        }
    }

    @Test
    fun theWavesAlwaysCoverTheWindow() {
        windows { scene, _ ->
            scene.waves.forEach { wave ->
                val swing = abs(wave.bobX)
                assertWithMessage("start side").that(wave.left + swing).isAtMost(0f)
                assertWithMessage("end side").that(wave.right - swing).isAtLeast(scene.width)
                assertWithMessage("bottom").that(wave.bottom - abs(wave.bobY)).isAtLeast(scene.height)
            }
        }
    }

    @Test
    fun aLightIsInvisibleBeforeItReachesTheCalmBand() {
        windows { scene, _ ->
            scene.lights.forEach { light ->
                (0L..light.periodMs step SAMPLE_MS).forEach { t ->
                    val top = light.centerY + lightDy(scene, light, t) - light.diameter / 2
                    val alpha = lightAlpha(scene, light, t)
                    if (top <= scene.calmEnd) assertThat(alpha).isEqualTo(0f)
                    val bound = light.opacity * ((top - scene.calmEnd) / scene.calmFade).coerceAtLeast(0f)
                    assertThat(alpha).isAtMost(bound + EPS)
                }
            }
        }
    }

    @Test
    fun theGelsTextBandFitsInsideTheTilesPadding() {
        val resources = RuntimeEnvironment.getApplication().resources
        assertThat(resources.getDimension(R.dimen.gel_top_band)).isAtMost(resources.getDimension(R.dimen.spacing_md))
    }

    @Test
    fun theSceneInRightToLeftIsTheExactMirror() {
        windows { ltr, dimens ->
            val rtl = buildSceneModel(ltr.density, ltr.width, ltr.height, rtl = true, dimens = dimens)
            ltr.waves.zip(rtl.waves).forEach { (a, b) ->
                assertThat(b.crestX).isWithin(EPS).of(ltr.width - a.crestX)
                assertThat(b.bobX).isEqualTo(-a.bobX)
                assertThat(b.copy(crestX = a.crestX, bobX = a.bobX)).isEqualTo(a)
            }
            ltr.lights.zip(rtl.lights).forEach { (a, b) ->
                assertThat(b.centerX).isWithin(EPS).of(ltr.width - a.centerX)
                assertThat(b.copy(centerX = a.centerX)).isEqualTo(a)
            }
        }
    }

    @Test
    fun sparseHoldsTheSkyAloneAndTheMovingDensitiesHoldNineAndFiveLights() {
        windows { scene, _ ->
            val expected =
                when (scene.density) {
                    BackdropDensity.Landscape -> LANDSCAPE_LIGHT_COUNT
                    BackdropDensity.Quiet -> QUIET_LIGHT_COUNT
                    BackdropDensity.Sparse -> 0
                }
            assertThat(scene.lights).hasSize(expected)
            assertThat(scene.waves).hasSize(if (scene.density == BackdropDensity.Sparse) 0 else 2)
        }
    }

    /** Runs [check] on each density's scene at every BD-11 window, from that window's real resources. */
    private fun windows(
        vararg densities: BackdropDensity = BackdropDensity.entries.toTypedArray(),
        check: (SceneModel, BackdropDimens) -> Unit,
    ) {
        WINDOWS.forEach { qualifiers ->
            RuntimeEnvironment.setQualifiers(qualifiers)
            val resources = RuntimeEnvironment.getApplication().resources
            val config = resources.configuration
            val px = resources.displayMetrics.density
            val dimens = BackdropDimens.from(resources)
            densities.forEach { density ->
                val width = config.screenWidthDp * px
                val height = config.screenHeightDp * px
                val scene = buildSceneModel(density, width, height, rtl = false, dimens = dimens)
                check(scene, dimens)
            }
        }
    }

    private companion object {
        val WINDOWS = listOf("w360dp-h640dp", "w412dp-h915dp", "w800dp-h1280dp", "w1280dp-h800dp")
        const val SAMPLE_MS = 50L
        const val EPS = 1e-3f
        const val LANDSCAPE_LIGHT_COUNT = 9
        const val QUIET_LIGHT_COUNT = 5
    }
}

private const val GEOMETRY_ROBOLECTRIC_SDK = 34
