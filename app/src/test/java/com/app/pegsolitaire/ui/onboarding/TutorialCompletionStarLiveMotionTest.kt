package com.app.pegsolitaire.ui.onboarding

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The tutorial's completion star caught mid-pop by "Remove animations" turning on with the app
 * resident (`reduced-motion-live-gate`, finding 1): the spring takes the new scale from the
 * composition's clock and ends within two frames, so the star stands whole, and stays whole. It
 * must never be left at the size the pop had reached. The first frame after the switch is not
 * read: a running animation takes a new scale from its next frame, a detail of Compose's loop that
 * this test does not pin.
 *
 * The star is read off the pixels around it, against the first frame of the same pop, in which it
 * is still at its starting size and draws nothing; everything else on the finished screen is
 * still, so what differs is the star. The settled reading is a star composed afresh with
 * animations off, which starts whole. The scale is turned live by [SystemMotionScale].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [COMPLETION_STAR_SDK], qualifiers = COMPLETION_STAR_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TutorialCompletionStarLiveMotionTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val tutorialShown = mutableStateOf(true)

    @Test
    fun aStarCaughtMidPopByAnimationsTurningOffSettlesWholeWithinTwoFrames() {
        playToCompletion(startTutorial())

        // A fresh pop with animations on: the screen leaves and comes back over the finished tutorial.
        composeRule.mainClock.autoAdvance = false
        showTheTutorialAgain()
        val popStart = composeRule.renderedWindow()
        repeat(INTO_THE_POP_FRAMES) { frame() }
        val midPop = composeRule.renderedWindow()

        motion.turnAnimationsOff()
        repeat(SETTLING_FRAMES) { frame() }
        val withinTwoFrames = composeRule.renderedWindow()
        repeat(HELD_FRAMES) { frame() }
        val held = composeRule.renderedWindow()

        showTheTutorialAgain()
        val settled = starPixels(composeRule.renderedWindow(), popStart)

        assertWithMessage("the settled star's pixels").that(settled).isAtLeast(SETTLED_MIN_PIXELS)
        assertWithMessage("the star mid-pop, before the switch").that(starPixels(midPop, popStart)).isLessThan(settled)
        assertWithMessage("the star $SETTLING_FRAMES frames after animations turn off")
            .that(starPixels(withinTwoFrames, popStart))
            .isEqualTo(settled)
        assertWithMessage("the star $HELD_FRAMES frames later").that(starPixels(held, popStart)).isEqualTo(settled)
    }

    private fun startTutorial(): TutorialViewModel {
        val viewModel =
            TutorialViewModel(SilentOnboardingRepository(), FixedHapticsSetting(on = true), TestApplicationScope)
        composeRule.setContent {
            AppTheme {
                if (tutorialShown.value) TutorialRouteUnderTest(viewModel)
            }
        }
        composeRule.waitForIdle()
        return viewModel
    }

    /** Plays every step's winning line, leaving the tutorial on its completion card. */
    private fun playToCompletion(viewModel: TutorialViewModel) {
        repeat(TutorialUiState.STEP_COUNT) {
            while (viewModel.uiState.value.board.board.pegCount > TutorialUiState.STEP_COMPLETE_PEGS) {
                val next = RulesEngine.legalMoves(viewModel.uiState.value.board.board).first()
                tapCell(next.from)
                tapCell(next.to)
            }
            idlePastTheStepBeat()
            composeRule.waitForIdle()
        }
    }

    private fun tapCell(position: Position) {
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(
                percentOffset(
                    x = (position.col + BoardMetrics.CELL_CENTER_FRACTION) / TUTORIAL_GRID,
                    y = (position.row + BoardMetrics.CELL_CENTER_FRACTION) / TUTORIAL_GRID,
                ),
            )
        }
        composeRule.waitForIdle()
    }

    /** The screen leaves and is composed again, so its completion star pops afresh. */
    private fun showTheTutorialAgain() {
        tutorialShown.value = false
        frame()
        tutorialShown.value = true
        frame()
    }

    /** One frame, after the main thread has idled, so a state written since the last is composed. */
    private fun frame() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
    }

    /** Pixels around the star that [window] draws differently from [popStart], the empty first frame. */
    private fun starPixels(
        window: Bitmap,
        popStart: Bitmap,
    ): Int {
        val around = starSurroundings()
        var drawn = 0
        for (x in around.left.roundToInt() until around.right.roundToInt()) {
            for (y in around.top.roundToInt() until around.bottom.roundToInt()) {
                if (distance(window.getPixel(x, y), popStart.getPixel(x, y)) > PIXEL_NOISE) drawn++
            }
        }
        return drawn
    }

    /** The column above the completion button that the star stands in, with room on every side. */
    private fun starSurroundings(): Rect {
        val gotIt = RuntimeEnvironment.getApplication().getString(R.string.onboarding_got_it)
        val button = composeRule.onNodeWithText(gotIt).fetchSemanticsNode().boundsInRoot
        val centre = button.center.x
        return Rect(
            left = centre - SURROUNDINGS_HALF_WIDTH_PX,
            top = button.top - SURROUNDINGS_HEIGHT_PX,
            right = centre + SURROUNDINGS_HALF_WIDTH_PX,
            bottom = button.top,
        )
    }

    private fun distance(
        first: Int,
        second: Int,
    ): Int = CHANNEL_SHIFTS.sumOf { shift -> abs((first shr shift and CHANNEL) - (second shr shift and CHANNEL)) }

    private companion object {
        /** Every tutorial footprint sits in a 3-cell square grid. */
        const val TUTORIAL_GRID = 3

        /** Into the spring, well short of its end. */
        const val INTO_THE_POP_FRAMES = 4

        /** No later than this many frames after the switch, the star stands whole (F1 amendment). */
        const val SETTLING_FRAMES = 2

        /** The 90 frames the finding was measured over: the star must still be whole after them. */
        const val HELD_FRAMES = 90

        /** A 48 dp star at density 1 covers far more than this; nothing at its starting size covers any. */
        const val SETTLED_MIN_PIXELS = 300

        /** The star is 48 dp square, 16 dp above the button; at density 1, dp are px. */
        const val SURROUNDINGS_HALF_WIDTH_PX = 48f
        const val SURROUNDINGS_HEIGHT_PX = 96f

        /** Summed per-channel distance below which two pixels count as the same colour. */
        const val PIXEL_NOISE = 30
        val CHANNEL_SHIFTS = listOf(16, 8, 0)
        const val CHANNEL = 0xFF
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val COMPLETION_STAR_SDK = 34

/** A density of 1, so every dp figure above is exact in px. */
private const val COMPLETION_STAR_QUALIFIERS = "w400dp-h800dp-mdpi"
