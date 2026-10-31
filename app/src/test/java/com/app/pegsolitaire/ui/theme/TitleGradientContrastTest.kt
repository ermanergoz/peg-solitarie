package com.app.pegsolitaire.ui.theme

import androidx.compose.ui.graphics.Color
import com.app.pegsolitaire.testing.contrast
import com.app.pegsolitaire.testing.fade
import com.app.pegsolitaire.testing.lerpSrgb
import com.app.pegsolitaire.testing.over
import com.app.pegsolitaire.ui.backdrop.BackdropMetrics
import com.app.pegsolitaire.ui.backdrop.LightSet
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * TG-AC02 as amended for direction B (colour-vision.md §5.2, §11.3), and TG-AC05's pin of the CTA
 * gradient: the title fix moves only the title.
 */
class TitleGradientContrastTest {
    @Test
    fun bothTitleStopsReadFourAndAHalfToOneOnTheBackgroundInBothThemes() {
        val pairs = listOf(LightClayColors to LightColors.background, DarkClayColors to DarkColors.background)
        pairs.forEach { (clay, bg) ->
            clay.titleGradient.forEach { stop ->
                assertWithMessage("$stop on $bg").that(contrast(stop, bg)).isAtLeast(TEXT_MIN)
            }
        }
    }

    @Test
    fun everyTitleChannelMovesTheSameWayBetweenItsStops() {
        listOf(LightClayColors, DarkClayColors).forEach { clay ->
            val (top, deep) = clay.titleGradient
            listOf(top.red to deep.red, top.green to deep.green, top.blue to deep.blue).forEach { (a, b) ->
                assertWithMessage("channel falls from the light stop").that(b).isAtMost(a)
            }
        }
    }

    @Test
    fun bothTitleStopsReadFourAndAHalfToOneOnEveryZoneTSkyToken() {
        val light = listOf(LightBackdropColors.skyTop, LightBackdropColors.skyMid)
        val dark =
            DarkBackdropColors.run { listOf(skyTop, skyMid, skyLow, skyDeep) }
        listOf(LightClayColors to light, DarkClayColors to dark).forEach { (clay, skies) ->
            clay.titleGradient.forEach { stop ->
                skies.forEach { sky ->
                    assertWithMessage("$stop on $sky").that(contrast(stop, sky)).isAtLeast(TEXT_MIN)
                }
            }
        }
    }

    @Test
    fun theTitleClearsThreeToOneOverEveryBackdropColourItCanReachInLight() {
        val stops = BackdropMetrics.LANDSCAPE_SKY_STOPS
        val t = (BackdropMetrics.TITLE_REACH - stops[2].fraction) / (stops[3].fraction - stops[2].fraction)
        val sky = lerpSrgb(LightBackdropColors.skyMid, LightBackdropColors.skyLow, t)
        BackdropMetrics.LANDSCAPE_LIGHTS.filter { it.set == LightSet.Lane }.forEach { light ->
            val rim = LightBackdropColors.lightRim(light.kind).fade(light.opacity).over(sky)
            LightClayColors.titleGradient.forEach { stop ->
                assertWithMessage("$stop over a ${light.kind} rim").that(contrast(stop, rim)).isAtLeast(LARGE_TEXT_MIN)
            }
        }
    }

    @Test
    fun theTitleGradientIsTheSpecsAndTheCtaGradientIsUnchanged() {
        assertThat(LightClayColors.titleGradient).containsExactly(Color(0xFF7C3AED), Color(0xFF5B21B6)).inOrder()
        assertThat(DarkClayColors.titleGradient).containsExactly(Color(0xFFC4B5FD), Color(0xFFA78BFA)).inOrder()
        // TG-AC05: the CTA gradient's colours as `develop` ships them (at 3ca373b), written here so a
        // change to them fails rather than silently moving every CTA button with the title.
        assertThat(LightClayColors.ctaGradient).containsExactly(Color(0xFFA78BFA), Color(0xFF7C3AED)).inOrder()
        assertThat(DarkClayColors.ctaGradient).containsExactly(Color(0xFFB6A0FC), Color(0xFF8B5CF6)).inOrder()
    }

    private companion object {
        const val TEXT_MIN = 4.5
        const val LARGE_TEXT_MIN = 3.0
    }
}
