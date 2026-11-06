package com.app.pegsolitaire.ui.results

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.testing.BackdropTestHost
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.backdrop.BackdropDensity
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * What the rays draw, in the `ResultsStarPaletteTest` Bitmap idiom (ux-spec §13's
 * `ResultsRaysPixelTest`; criteria RC-2, RC-12): at the rest pose, on the Results sky
 * (`BackdropTestHost` at Sparse), in the same run and with no stored golden, each frame is compared
 * with the same frame drawn with the layer absent. On wedge 0's bisector just above the star row's
 * centre the gold tint shows; on the first gap's bisector it does not; past 0.60 H it has faded to
 * nothing; and the colour-blind palette draws the same pixels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RAYS_PIXEL_ROBOLECTRIC_SDK], qualifiers = PORTRAIT_WINDOW)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResultsRaysPixelTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val showRays = mutableStateOf(true)
    private val palette = mutableStateOf(BoardMarkPalette.Standard)

    @Before
    fun restPose() {
        motion.turnAnimationsOff()
    }

    @Test
    fun theRestPoseTintsWedgeZeroAndLeavesTheGapClearInTheLightTheme() = assertTheRays(dark = false)

    @Test
    fun theRestPoseTintsWedgeZeroAndLeavesTheGapClearInTheDarkTheme() = assertTheRays(dark = true)

    /**
     * In portrait no point 0.60 H from the star row lies inside the window — the burst has faded out
     * before the window's edges, as designed — so the fade's end is read on a landscape window, where
     * the horizontal bisectors reach 0.62 H on screen.
     */
    @Test
    @Config(qualifiers = LANDSCAPE_WINDOW)
    fun pastSixTenthsOfTheHeightTheRaysHaveFadedToNothingInTheLightTheme() = assertTheFade(dark = false)

    @Test
    @Config(qualifiers = LANDSCAPE_WINDOW)
    fun pastSixTenthsOfTheHeightTheRaysHaveFadedToNothingInTheDarkTheme() = assertTheFade(dark = true)

    private fun assertTheRays(dark: Boolean) {
        setScreen(dark)
        val withRays = frame(rays = true, shown = BoardMarkPalette.Standard)
        val withoutRays = frame(rays = false, shown = BoardMarkPalette.Standard)
        val colourBlind = frame(rays = true, shown = BoardMarkPalette.Colorblind)
        val height = withRays.height.toFloat()
        val centre = starRowCentre()

        val onWedge = along(centre, ResultsRays.bisectorDegrees(0), NEAR_FRACTION * height)
        val ray = withRays.at(onWedge)
        val sky = withoutRays.at(onWedge)
        assertWithMessage("dark=$dark: wedge 0's bisector near the centre is tinted").that(ray).isNotEqualTo(sky)
        assertWithMessage("dark=$dark: the tint is gold, so it takes blue out")
            .that(ray and BLUE)
            .isLessThan(sky and BLUE)
        assertWithMessage("dark=$dark: the colour-blind palette draws the same ray")
            .that(colourBlind.at(onWedge))
            .isEqualTo(ray)

        val gapBisector = (ResultsRays.bisectorDegrees(0) + ResultsRays.bisectorDegrees(1)) / 2
        val inGap = along(centre, gapBisector, NEAR_FRACTION * height)
        assertWithMessage("dark=$dark: the first gap is clear")
            .that(withRays.at(inGap))
            .isEqualTo(withoutRays.at(inGap))
    }

    private fun assertTheFade(dark: Boolean) {
        setScreen(dark)
        val withRays = frame(rays = true, shown = BoardMarkPalette.Standard)
        val withoutRays = frame(rays = false, shown = BoardMarkPalette.Standard)
        val height = withRays.height.toFloat()
        val centre = starRowCentre()
        val far =
            (0 until ResultsRays.WEDGES)
                .map { along(centre, ResultsRays.bisectorDegrees(it), FAR_FRACTION * height) }
                .filter { it.x in 0f..(withRays.width - 1f) && it.y in 0f..(height - 1f) }
        assertWithMessage("dark=$dark: some bisector reaches 0.62 H inside the window").that(far).isNotEmpty()
        far.forEach { point ->
            assertWithMessage("dark=$dark: faded to nothing at $point")
                .that(withRays.at(point))
                .isEqualTo(withoutRays.at(point))
        }
        assertWithMessage("dark=$dark: the rays do draw in this frame, so the far read is not vacuous")
            .that(withRays.sameAs(withoutRays))
            .isFalse()
    }

    private fun setScreen(dark: Boolean) {
        composeRule.setContent {
            AppTheme(darkTheme = dark, boardMarkPalette = palette.value) {
                BackdropTestHost(BackdropDensity.Sparse) {
                    ResultsScreen(
                        state = RESULT,
                        onReplay = {},
                        onCatalog = {},
                        onHome = {},
                        onNextLevel = {},
                        onRetry = {},
                        onNavigateBack = {},
                        showRays = showRays.value,
                    )
                }
            }
        }
    }

    private fun starRowCentre(): Offset =
        composeRule
            .onNodeWithTag(RESULTS_STAR_ROW_TEST_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .center

    private fun frame(
        rays: Boolean,
        shown: BoardMarkPalette,
    ): Bitmap {
        composeRule.runOnUiThread {
            showRays.value = rays
            palette.value = shown
        }
        composeRule.waitForIdle()
        return composeRule.renderedWindow()
    }

    /** The point [distance] from [centre] at [degrees] clockwise from straight up. */
    private fun along(
        centre: Offset,
        degrees: Float,
        distance: Float,
    ): Offset {
        val radians = Math.toRadians(degrees.toDouble())
        return Offset(centre.x + distance * sin(radians).toFloat(), centre.y - distance * cos(radians).toFloat())
    }

    private fun Bitmap.at(point: Offset): Int = getPixel(point.x.roundToInt(), point.y.roundToInt())

    private companion object {
        val RESULT =
            ResultsUiState(
                stars = 2,
                pegsRemaining = 5,
                elapsedSeconds = 75,
                score = 1_280,
                headlineTier = ResultsHeadlineTier.SOLVED,
                replayId = ClassicLevelIds.ENGLISH33,
            )

        /** 0.05 H from the centre: inside the core, clear of the stars and the headline. */
        const val NEAR_FRACTION = 0.05f

        /** Past the fade's 0.60 H. */
        const val FAR_FRACTION = 0.62f

        /** The blue channel of an ARGB pixel. */
        const val BLUE = 0xFF
    }
}

private const val PORTRAIT_WINDOW = "w400dp-h800dp-xxhdpi"
private const val LANDSCAPE_WINDOW = "w1000dp-h480dp-xxhdpi"

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val RAYS_PIXEL_ROBOLECTRIC_SDK = 34
