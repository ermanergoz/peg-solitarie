package com.app.pegsolitaire.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.home.HOME_STAGE_ART_TEST_TAG
import com.app.pegsolitaire.ui.home.StaticBoardArt
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.chipsState
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The first frame that draws a board draws its pegs (ux-spec §4.2.5, "The first frame is whole";
 * criterion 15v part 4's JVM half).
 *
 * QA's cold start showed the Home stage's slab ≈0.26s before its pegs, and the Classic tile before
 * its art. Each is one `BoardCanvas` draw of a constant, so nothing justifies two pieces. The
 * cause: the canvas built its geometry from a size held in state, filled by `onSizeChanged` during
 * the **first layout** — after the first composition — so the first draw read a geometry that was
 * not ready and drew nothing, and the pegs waited for the recomposition that state write scheduled.
 *
 * ## How frame 1 is isolated
 *
 * The test clock is paused before the content is set, so the composition, the layout and the draw
 * Robolectric's looper performs happen, and the recomposition the layout's state write schedules
 * does not: that waits for a frame of the paused clock. What [renderedWindow] then draws is the
 * first frame. The clock is then let run until idle, and the settled frame drawn again.
 *
 * Both reads are of the canvas's own bounds, in the flat software render, so each is exact: frame
 * 1 must already carry ink — the pegs and the holes — and must be pixel for pixel the settled frame.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoardCanvasFirstFrameTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    /** A read-only board, as the Classic tile and the Continue thumbnail draw one. */
    @Test
    fun theFirstFrameOfAReadOnlyBoardDrawsItsPegs() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                StaticBoardArt(
                    board = Boards.english33Initial,
                    modifier = Modifier.size(dimensionResource(R.dimen.home_tile_art_size)),
                )
            }
        }

        assertTheFirstFrameIsTheSettledFrame(BOARD_CANVAS_TEST_TAG)
    }

    /**
     * The Home stage on the 6.7-inch window, as QA's cold start drew it: animations off, so the
     * slab is drawn at full alpha in the first frame it is drawn at all (with them on, the hero's
     * entrance fades slab and pegs in together, at one alpha, and the first frame is blank).
     */
    @Test
    @Config(qualifiers = "w411dp-h915dp")
    fun theFirstFrameOfTheHomeStageDrawsItsPegsOnTheSlab() {
        motion.turnAnimationsOff()
        composeRule.mainClock.autoAdvance = false
        composeRule.renderHome(state = chipsState(classic = false, challenge = false))
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)

        assertTheFirstFrameIsTheSettledFrame(HOME_STAGE_ART_TEST_TAG)
    }

    /**
     * An interactive board in the colour-blind palette, a peg held with its targets showing: the
     * first frame rings the held peg at the palette's radius (docs/ux/colour-vision.md §3.4), as
     * every later frame does. The frame-size geometry keeps the composed geometry's recipe.
     */
    @Test
    fun theFirstFrameOfAColourBlindBoardRingsTheHeldPegAsTheSettledFrameDoes() {
        val scene = PaletteScene.midGame(Boards.english33Initial)
        motion.turnAnimationsOff()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme(darkTheme = false, boardMarkPalette = BoardMarkPalette.Colorblind) {
                BoardCanvas(
                    state = scene.state.copy(hintSource = null, hintTarget = null),
                    onCellTap = {},
                    onAnimationFinished = {},
                    modifier = Modifier.size(INTERACTIVE_CANVAS_DP.dp),
                )
            }
        }

        assertTheFirstFrameIsTheSettledFrame(BOARD_CANVAS_TEST_TAG)
    }

    /**
     * Reads the tagged node's pixels in the frame the paused clock left, then in the settled frame:
     * the first must carry ink and equal the second.
     */
    private fun assertTheFirstFrameIsTheSettledFrame(tag: String) {
        val bounds = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val firstFrame = pixelsWithin(composeRule.renderedWindow(), bounds)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        val settledFrame = pixelsWithin(composeRule.renderedWindow(), bounds)

        assertWithMessage("peg ink inside $tag in the settled frame, in px")
            .that(settledFrame.count(::isPegInk))
            .isGreaterThan(0)
        assertWithMessage("peg ink inside $tag in the first frame, in px")
            .that(firstFrame.count(::isPegInk))
            .isGreaterThan(0)
        assertWithMessage("pixels inside $tag that differ between the first frame and the settled one")
            .that(firstFrame.indices.count { firstFrame[it] != settledFrame[it] })
            .isEqualTo(0)
    }

    /** The pixels lying wholly inside [bounds], row by row. */
    private fun pixelsWithin(
        window: Bitmap,
        bounds: Rect,
    ): List<Int> =
        (ceil(bounds.top).toInt() until floor(bounds.bottom).toInt()).flatMap { y ->
            (ceil(bounds.left).toInt() until floor(bounds.right).toInt()).map { x -> window.getPixel(x, y) }
        }
}

/** The design baseline's system bars: a 24dp status bar and a 48dp navigation bar. */
private const val STATUS_BAR_DP = 24
private const val NAVIGATION_BAR_DP = 48

/** An interactive board of 350dp, as the motion test draws one. */
private const val INTERACTIVE_CANVAS_DP = 350

/**
 * Whether [pixel] is a candy peg's: the `PegGradient` runs from #F9A8D4 through #EC4899 to #BE185D,
 * whose red stands at least 81 levels over its green, while the slab (#F1EAFB), both dimple tones
 * (#CFC2E8, #E4DBF4) and an undrawn, transparent canvas stand at 13 or less.
 */
private fun isPegInk(pixel: Int): Boolean =
    Color.alpha(pixel) > 0 && Color.red(pixel) - Color.green(pixel) >= PEG_RED_OVER_GREEN

/** Well clear of the slab's and the holes' 13 levels, and under the pegs' 81 to 166. */
private const val PEG_RED_OVER_GREEN = 48
