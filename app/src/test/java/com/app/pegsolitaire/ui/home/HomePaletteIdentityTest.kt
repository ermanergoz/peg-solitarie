package com.app.pegsolitaire.ui.home

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.home.fixture.EVERY_CHIP_STATE
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home is untouched by the palette (docs/ux/colour-vision.md CV-05, CV-AC06): in every chip state
 * and both themes it is the same pixels with the option off and on. Home draws nothing the palette
 * changes — its two boards are read-only and read no palette, the ribbon keeps its gilded star —
 * so flipping the palette under a composed Home must change no pixel. One composition per theme,
 * the palette flipped in place, animations off so every frame is still.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomePaletteIdentityTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val palette = mutableStateOf(BoardMarkPalette.Standard)
    private val chips = mutableStateOf(EVERY_CHIP_STATE.first())

    @Before
    fun animationsOff() {
        motion.turnAnimationsOff()
    }

    @Test
    fun homeIsTheSamePixelsWithThePaletteOffAndOnInTheLightTheme() = assertHomeIgnoresThePalette(dark = false)

    @Test
    fun homeIsTheSamePixelsWithThePaletteOffAndOnInTheDarkTheme() = assertHomeIgnoresThePalette(dark = true)

    private fun assertHomeIgnoresThePalette(dark: Boolean) {
        composeRule.setContent {
            AppTheme(darkTheme = dark, boardMarkPalette = palette.value) {
                HomeScreen(
                    state = chips.value,
                    onPlayClassic = {},
                    onPlayChallenge = {},
                    onResumeClassic = {},
                    onResumeChallenge = {},
                    onAcceptPlayGamesPrompt = {},
                    onDeclinePlayGamesPrompt = {},
                    onKeepDeviceProgress = {},
                    onUseCloudProgress = {},
                    onPlayGamesSignInErrorShown = {},
                    onOpenSettings = {},
                )
            }
        }
        EVERY_CHIP_STATE.forEachIndexed { index, state ->
            composeRule.runOnUiThread { chips.value = state }
            val standard = frameIn(BoardMarkPalette.Standard)
            val colorblind = frameIn(BoardMarkPalette.Colorblind)

            assertWithMessage("chip state $index, dark = $dark: pixels the palette changed")
                .that(differingPixels(standard, colorblind))
                .isEqualTo(0)
        }
    }

    private fun frameIn(shown: BoardMarkPalette): Bitmap {
        composeRule.runOnUiThread { palette.value = shown }
        return composeRule.renderedWindow()
    }

    private fun differingPixels(
        first: Bitmap,
        second: Bitmap,
    ): Int {
        var differing = 0
        for (y in 0 until first.height) {
            for (x in 0 until first.width) {
                if (first.getPixel(x, y) != second.getPixel(x, y)) differing++
            }
        }
        return differing
    }
}
