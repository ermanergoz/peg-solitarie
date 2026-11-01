package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.app.pegsolitaire.testing.contrast
import com.app.pegsolitaire.testing.edgeFloor
import com.app.pegsolitaire.testing.fade
import com.app.pegsolitaire.testing.hueDegrees
import com.app.pegsolitaire.testing.lerpSrgb
import com.app.pegsolitaire.testing.over
import com.app.pegsolitaire.testing.wcagLuminance
import com.app.pegsolitaire.ui.backdrop.BackdropMetrics
import com.app.pegsolitaire.ui.backdrop.LightKind
import com.app.pegsolitaire.ui.backdrop.LightSet
import com.app.pegsolitaire.ui.backdrop.SkyStop
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * BD-08: every constraint of backdrop spec §4.1, computed from the `Color.kt` values the
 * app ships rather than trusted from the document, plus the research-analyst's C1 (lights are bokeh,
 * not bubbles) and C3 (a lilac-to-violet wave body) as the tech-lead's A8 states them. A retuned token must
 * still pass every one of these; the hexes themselves are not asserted.
 */
class BackdropColorsTest {
    private val themes =
        listOf(
            Theme("light", LightColors, LightBackdropColors, LightFinishColors, zoneTLight = true),
            Theme("dark", DarkColors, DarkBackdropColors, DarkFinishColors, zoneTLight = false),
        )

    @Test
    fun theSkysTopColourIsTheThemesBackground() {
        themes.forEach { t ->
            assertWithMessage("${t.name} SkyTop").that(t.backdrop.skyTop).isEqualTo(t.scheme.background)
        }
    }

    @Test
    fun zoneTColoursReadFourAndAHalfToOneUnderEveryTextRoleInBothThemes() {
        themes.forEach { t ->
            t.zoneT().forEach { (name, colour) ->
                val l = colour.wcagLuminance()
                if (t.zoneTLight) {
                    assertWithMessage("${t.name} $name L").that(l).isAtLeast(ZONE_T_LIGHT_MIN_L)
                } else {
                    assertWithMessage("${t.name} $name L").that(l).isAtMost(ZONE_T_DARK_MAX_L)
                }
                listOf(t.scheme.onSurface, t.scheme.onSurfaceVariant, t.scheme.primary).forEach { text ->
                    assertWithMessage("${t.name} text on $name").that(contrast(text, colour)).isAtLeast(TEXT_MIN)
                }
            }
        }
    }

    @Test
    fun theLightSkyDeepensDownwardAndTheDarkMidNeverRisesAboveItsTop() {
        val light = LightBackdropColors
        val l = listOf(light.skyTop, light.skyMid, light.skyLow, light.skyDeep).map { it.wcagLuminance() }
        assertWithMessage("light sky order").that(l).isInStrictOrder(compareByDescending<Double> { it })
        assertWithMessage("dark SkyMid <= SkyTop")
            .that(DarkBackdropColors.skyMid.wcagLuminance())
            .isAtMost(DarkBackdropColors.skyTop.wcagLuminance())
    }

    @Test
    fun everySparseColourStaysOnTheBackgroundsSide() {
        themes.forEach { t ->
            val background = t.scheme.background.wcagLuminance()
            (0..SPARSE_SAMPLES).forEach { i ->
                val l = lerpSrgb(t.backdrop.skyTop, t.backdrop.sparseLow, i / SPARSE_SAMPLES.toFloat()).wcagLuminance()
                if (t.zoneTLight) {
                    assertWithMessage("light wash sample $i").that(l).isAtLeast(background)
                } else {
                    assertWithMessage("dark wash sample $i").that(l).isAtMost(background)
                }
            }
        }
    }

    @Test
    fun aSingleLightNeverBreaksTheTitlesBoundsAboveFortySevenPercent() {
        themes.forEach { t ->
            val sky = skyAt(BackdropMetrics.LANDSCAPE_SKY_STOPS, BackdropMetrics.TITLE_REACH, t.backdrop)
            BackdropMetrics.LANDSCAPE_LIGHTS.filter { it.set == LightSet.Lane }.forEach { light ->
                listOf(t.backdrop.lightCore(light.kind), t.backdrop.lightRim(light.kind)).forEach { stop ->
                    val l = stop.fade(light.opacity).over(sky).wcagLuminance()
                    if (t.zoneTLight) {
                        assertWithMessage("light lane ${light.kind}").that(l).isAtLeast(TITLE_LIGHT_MIN_L)
                    } else {
                        assertWithMessage("dark lane ${light.kind}").that(l).isAtMost(TITLE_DARK_MAX_L)
                    }
                }
            }
        }
    }

    @Test
    fun everyHeroContainersEdgeFloorHoldsAgainstAnyColour() {
        themes.forEach { t ->
            val rim = t.finish.panelRim
            val clay = t.scheme.surfaceContainerHigh
            val today = contrast(clay, t.scheme.background)
            assertWithMessage("${t.name} clay family").that(floorOf(clay, rim)).isAtLeast(today)
            val chips = t.scheme.tertiaryContainer
            val banner = t.scheme.errorContainer
            if (t.zoneTLight) {
                val chipsToday = contrast(chips, t.scheme.background)
                val bannerToday = contrast(banner, t.scheme.background)
                assertWithMessage("light Continue chip").that(floorOf(chips, rim)).isAtLeast(chipsToday)
                assertWithMessage("light banner").that(floorOf(banner, rim)).isAtLeast(bannerToday)
            } else {
                assertWithMessage("dark Continue chip").that(floorOf(chips, rim)).isAtLeast(DARK_FLOOR_MIN)
                assertWithMessage("dark banner").that(floorOf(banner, rim)).isAtLeast(DARK_FLOOR_MIN)
            }
        }
        listOf(GelColors.Violet, GelColors.Pink).forEach { gel ->
            listOf(gel.top, gel.mid, gel.bottom).forEach { fill ->
                assertWithMessage("gel $fill").that(floorOf(fill, GelColors.rim)).isAtLeast(GEL_FLOOR_MIN)
            }
        }
    }

    @Test
    fun whiteTextReadsFourAndAHalfToOneOnTheGelFromItsTopBandDown() {
        listOf(GelColors.Violet, GelColors.Pink).forEach { gel ->
            assertWithMessage("white on ${gel.mid}").that(contrast(GelColors.onGel, gel.mid)).isAtLeast(TEXT_MIN)
            assertWithMessage("red falls").that(gel.bottom.red).isAtMost(gel.mid.red)
            assertWithMessage("green falls").that(gel.bottom.green).isAtMost(gel.mid.green)
            assertWithMessage("blue falls").that(gel.bottom.blue).isAtMost(gel.mid.blue)
            val badge = GelColors.badge.over(gel.mid)
            assertWithMessage("badge glyph").that(contrast(GelColors.onGel, badge)).isAtLeast(GLYPH_MIN)
            val chip = GelColors.chip.over(gel.mid)
            assertWithMessage("level chip").that(contrast(GelColors.chipText, chip)).isAtLeast(TEXT_MIN)
        }
    }

    @Test
    fun theDarkGlossKeepsRibbonLabelsAtFourAndAHalfToOne() {
        val glossed = DarkFinishColors.panelGloss.over(DarkColors.surfaceContainerHigh)
        assertWithMessage("dark onSurfaceVariant under the gloss")
            .that(contrast(DarkColors.onSurfaceVariant, glossed))
            .isAtLeast(TEXT_MIN)
    }

    @Test
    fun everyNewHueLiesInTheVioletPinkArc() {
        newTokens().forEach { (name, colour) ->
            val hue = colour.hueDegrees() ?: return@forEach
            assertWithMessage("$name hue $hue").that(hue in HUE_ARC).isTrue()
        }
    }

    @Test
    fun everyLightIsAFilledDiscWhoseCoreIsAtLeastHalfItsRim() {
        themes.forEach { t ->
            LightKind.entries.forEach { kind ->
                assertWithMessage("${t.name} $kind core alpha")
                    .that(t.backdrop.lightCore(kind).alpha)
                    .isAtLeast(t.backdrop.lightRim(kind).alpha / 2f)
            }
        }
    }

    @Test
    fun theLightWaveBodyStaysLilacToVioletBelowItsPinkCrest() {
        listOf(LightBackdropColors.waveBody, LightBackdropColors.waveDeep).forEach { colour ->
            val hue = colour.hueDegrees()
            assertWithMessage("light wave body hue $hue").that(hue != null && hue in WAVE_BODY_ARC).isTrue()
        }
    }

    private fun floorOf(
        fill: Color,
        rimToken: Color,
    ): Double = edgeFloor(fill.wcagLuminance(), rimToken.over(fill).wcagLuminance())

    private fun skyAt(
        stops: List<SkyStop>,
        fraction: Float,
        colours: BackdropColors,
    ): Color {
        val lower = stops.last { it.fraction <= fraction }
        val upper = stops.first { it.fraction >= fraction }
        if (upper.fraction == lower.fraction) return colours.sky(lower.role)
        val t = (fraction - lower.fraction) / (upper.fraction - lower.fraction)
        return lerpSrgb(colours.sky(lower.role), colours.sky(upper.role), t)
    }

    private fun newTokens(): List<Pair<String, Color>> =
        themes.flatMap { t ->
            val b = t.backdrop
            val f = t.finish
            listOf(
                "skyTop" to b.skyTop,
                "skyMid" to b.skyMid,
                "skyLow" to b.skyLow,
                "skyDeep" to b.skyDeep,
                "sparseLow" to b.sparseLow,
                "waveCrest" to b.waveCrest,
                "waveBody" to b.waveBody,
                "waveDeep" to b.waveDeep,
                "waveLip" to b.waveLip,
                "waveSheen" to b.waveSheen,
                "pearlCore" to b.pearlCore,
                "pearlRim" to b.pearlRim,
                "pinkCore" to b.pinkCore,
                "pinkRim" to b.pinkRim,
                "violetCore" to b.violetCore,
                "violetRim" to b.violetRim,
                "panelRim" to f.panelRim,
                "panelLip" to f.panelLip,
                "panelGloss" to f.panelGloss,
                "panelGlow" to f.panelGlow,
            ).map { (n, c) -> "${t.name} $n" to c }
        } +
            listOf(GelColors.Violet, GelColors.Pink).flatMap { g ->
                listOf("gel top" to g.top, "gel mid" to g.mid, "gel bottom" to g.bottom, "gel glow" to g.glow)
            } +
            listOf(
                "onGel" to GelColors.onGel,
                "badge" to GelColors.badge,
                "chip" to GelColors.chip,
                "chipText" to GelColors.chipText,
                "artPeg" to GelColors.artPeg,
                "artHole" to GelColors.artHole,
                "rim" to GelColors.rim,
                "lip" to GelColors.lip,
                "sheen" to GelColors.sheen,
            )

    private data class Theme(
        val name: String,
        val scheme: ColorScheme,
        val backdrop: BackdropColors,
        val finish: FinishColors,
        val zoneTLight: Boolean,
    ) {
        /** B-03's zone T: light SkyTop, SkyMid and SparseLow; dark, every sky stop. */
        fun zoneT(): List<Pair<String, Color>> =
            if (zoneTLight) {
                listOf("SkyTop" to backdrop.skyTop, "SkyMid" to backdrop.skyMid, "SparseLow" to backdrop.sparseLow)
            } else {
                listOf(
                    "SkyTop" to backdrop.skyTop,
                    "SkyMid" to backdrop.skyMid,
                    "SkyLow" to backdrop.skyLow,
                    "SkyDeep" to backdrop.skyDeep,
                    "SparseLow" to backdrop.sparseLow,
                )
            }
    }

    private companion object {
        const val ZONE_T_LIGHT_MIN_L = 0.779
        const val ZONE_T_DARK_MAX_L = 0.0357
        const val TEXT_MIN = 4.5
        const val GLYPH_MIN = 3.0
        const val SPARSE_SAMPLES = 10
        const val TITLE_LIGHT_MIN_L = 0.503
        const val TITLE_DARK_MAX_L = 0.0786
        const val DARK_FLOOR_MIN = 1.20
        const val GEL_FLOOR_MIN = 1.20
        val HUE_ARC = 250.0..336.0
        val WAVE_BODY_ARC = 260.0..300.0
    }
}
