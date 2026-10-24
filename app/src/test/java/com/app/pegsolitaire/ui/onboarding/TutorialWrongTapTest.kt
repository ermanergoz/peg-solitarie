package com.app.pegsolitaire.ui.onboarding

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.tutorial.TutorialBoards
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.components.BoardGeometry
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * A wrong tap in the tutorial, on the tutorial's own screen (D-01j(2); WT-16's screen half;
 * tech-lead Ruling 3): the redirect, and nothing else. The tapped cell is never shaken or marked,
 * a tap with nothing held shows nothing new, and the board is no live region, so no wrong tap is
 * ever announced. The haptic and sound halves are `TutorialHapticsTest` and `TutorialSoundTest`.
 *
 * Driven through the real [TutorialScreen] over a real [TutorialViewModel] on T2, and read off the
 * window's pixels on a paused clock. A pixel test here takes at most two wrong taps: the third
 * escalates the coach (§4.4 class 3), whose slab pulse moves every pixel on the board. The
 * redirect's own curves are `BoardCanvasWrongTapTest`'s; this class holds the tutorial's wiring.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TUTORIAL_WRONG_TAP_SDK], qualifiers = TUTORIAL_WRONG_TAP_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TutorialWrongTapTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aWrongTapWithNothingHeldShowsNothingNewOnTheBoard() {
        startOnT2()
        composeRule.mainClock.autoAdvance = false
        val before = composeRule.renderedWindow()

        clickCell(T2_EMPTY_HOLE) // T-W1
        repeat(WATCH_FRAMES) {
            frame()
            assertWithMessage("board pixels changed")
                .that(changedPixels(before, composeRule.renderedWindow(), boardBox()))
                .isEqualTo(0)
        }
    }

    @Test
    fun aWrongTapOnAHoleWhileAPegIsHeldRePopsTheDotButNeverMovesTheTappedHole() {
        startOnT2()
        tapCell(T2_MOVABLE_PEG)
        composeRule.mainClock.autoAdvance = false
        val before = composeRule.renderedWindow()

        clickCell(T2_ILLEGAL_HOLE) // T-W3
        val dotChanged = mutableListOf<Boolean>()
        repeat(WATCH_FRAMES) {
            frame()
            val now = composeRule.renderedWindow()
            assertWithMessage("the tapped hole").that(changedPixels(before, now, cellBox(T2_ILLEGAL_HOLE))).isEqualTo(0)
            dotChanged += changedPixels(before, now, cellBox(T2_DESTINATION)) > 0
        }

        // The redirect: the held peg's one dot drops and pops back in, then rests as it was.
        assertWithMessage("frames whose dot differs").that(dotChanged.take(REPOP_WITHIN_FRAMES)).contains(true)
        assertThat(dotChanged.last()).isFalse()
    }

    @Test
    fun theTutorialBoardIsNoLiveRegionAndNoWrongTapIsAnnounced() {
        val viewModel = startOnT2()

        tapCell(T2_EMPTY_HOLE) // T-W1
        tapCell(T2_MOVABLE_PEG)
        tapCell(T2_IMMOVABLE_PEG) // T-W2
        tapCell(T2_ILLEGAL_HOLE) // T-W3

        assertThat(viewModel.uiState.value.board.rejectedTapTick).isEqualTo(THREE_WRONG_TAPS)
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).assertCountEquals(0)
        WRONG_TAP_SENTENCES.map(RuntimeEnvironment.getApplication()::getString).forEach { sentence ->
            composeRule.onAllNodesWithContentDescription(sentence, substring = true).assertCountEquals(0)
            composeRule.onAllNodesWithText(sentence, substring = true).assertCountEquals(0)
        }
    }

    // ---- Helpers ---------------------------------------------------------------------------

    /** Plays T1's only move, lets the step beat pass, and leaves the screen on T2. */
    private fun startOnT2(): TutorialViewModel {
        val viewModel =
            TutorialViewModel(SilentOnboardingRepository(), FixedHapticsSetting(on = true), TestApplicationScope)
        composeRule.setContent {
            AppTheme {
                TutorialRouteUnderTest(viewModel)
            }
        }
        composeRule.waitForIdle()
        val move = RulesEngine.legalMoves(viewModel.uiState.value.board.board).single()
        tapCell(move.from)
        tapCell(move.to)
        idlePastTheStepBeat()
        composeRule.waitForIdle()
        assertThat(viewModel.uiState.value.board.board).isEqualTo(TutorialBoards.T2)
        return viewModel
    }

    private fun tapCell(cell: Position) {
        clickCell(cell)
        composeRule.waitForIdle()
    }

    private fun clickCell(cell: Position) {
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(
                percentOffset(
                    x = (cell.col + BoardMetrics.CELL_CENTER_FRACTION) / TUTORIAL_GRID,
                    y = (cell.row + BoardMetrics.CELL_CENTER_FRACTION) / TUTORIAL_GRID,
                ),
            )
        }
    }

    /** One frame, after the main thread has idled, so a state written since the last frame is in it. */
    private fun frame() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun boardBox(): Rect = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).fetchSemanticsNode().boundsInRoot

    private fun cellBox(cell: Position): Rect {
        val canvas = boardBox()
        val geometry = BoardGeometry.forCanvas(TutorialBoards.T2.spec, Size(canvas.width, canvas.height))
        val center = canvas.topLeft + geometry.center(cell)
        val half = geometry.cellSize / 2
        return Rect(center.x - half, center.y - half, center.x + half, center.y + half)
    }

    /** How many pixels of [box] differ between [before] and [now]. */
    private fun changedPixels(
        before: Bitmap,
        now: Bitmap,
        box: Rect,
    ): Int {
        var changed = 0
        for (y in box.top.roundToInt() until box.bottom.roundToInt()) {
            for (x in box.left.roundToInt() until box.right.roundToInt()) {
                if (before.getPixel(x, y) != now.getPixel(x, y)) changed++
            }
        }
        return changed
    }

    private companion object {
        /** Every tutorial footprint sits in a 3-cell square grid. */
        const val TUTORIAL_GRID = 3

        /** Past the 270 ms shake and a dot's pop-in, with the frames either takes to start. */
        const val WATCH_FRAMES = 45

        /** The dot is gone or small within a few frames of the tap, then pops back. */
        const val REPOP_WITHIN_FRAMES = 6

        /** T-W1, T-W2 and T-W3, one each. */
        const val THREE_WRONG_TAPS = 3

        /** T2's only movable peg, and the one hole it can land on (over the middle peg). */
        val T2_MOVABLE_PEG = Position(row = 0, col = 1)
        val T2_DESTINATION = Position(row = 2, col = 1)

        /** T2's middle peg, which has no legal jump. */
        val T2_IMMOVABLE_PEG = Position(row = 1, col = 1)

        /** An in-play empty hole on T2 with nothing pointing at it. */
        val T2_EMPTY_HOLE = Position(row = 0, col = 0)

        /** An in-play empty hole on T2 that is never a legal destination. */
        val T2_ILLEGAL_HOLE = Position(row = 2, col = 2)

        /** The gameplay board's three sentences (D-06c), which the tutorial must never speak. */
        val WRONG_TAP_SENTENCES =
            listOf(R.string.wrong_tap_empty_hole, R.string.wrong_tap_other_peg, R.string.wrong_tap_not_a_destination)
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val TUTORIAL_WRONG_TAP_SDK = 34

/** A tall phone at 1.5× density, so the board's cells are big enough to read. */
private const val TUTORIAL_WRONG_TAP_QUALIFIERS = "w400dp-h800dp-hdpi"
