package com.app.pegsolitaire.ui.onboarding

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.tutorial.TutorialBoards
import com.app.pegsolitaire.testing.RecordingHapticFeedback
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

    private val haptics = RecordingHapticFeedback()
    private val sounds = FakeSoundPlayer()

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

    /**
     * D-TBa items 2 and 3 (A1.2 item 5 of the talkback-board brief): the tutorial now speaks D-TB's
     * neutral line for each wrong tap, once, on the board's one channel — its words, not a cue. What
     * stays is D-01j(2)'s silence of the correction: no reject haptic and no invalid-tap sound here,
     * and no shake or mark, which the two tests above pin by the board's pixels.
     */
    @Test
    fun aWrongTapInTheTutorialSpeaksItsLineOnceWithNoRejectHapticAndNoSound() {
        val viewModel = startOnT2()

        tapCell(T2_EMPTY_HOLE) // T-W1
        assertThat(channel()).isEqualTo("No peg here.")
        tapCell(T2_MOVABLE_PEG)
        tapCell(T2_IMMOVABLE_PEG) // T-W2
        assertThat(channel()).isEqualTo("Put down the selected peg first.")
        tapCell(T2_ILLEGAL_HOLE) // T-W3
        assertThat(channel()).isEqualTo("Not a jump target.")

        assertThat(viewModel.uiState.value.board.rejectedTapTick).isEqualTo(THREE_WRONG_TAPS)
        assertThat(haptics.events).doesNotContain(HapticFeedbackType.Reject)
        assertThat(sounds.cues).doesNotContain(SoundCue.InvalidTap)
    }

    // ---- Helpers ---------------------------------------------------------------------------

    /** Plays T1's only move, lets the step beat pass, and leaves the screen on T2. */
    private fun startOnT2(): TutorialViewModel {
        val viewModel =
            TutorialViewModel(SilentOnboardingRepository(), FixedHapticsSetting(on = true), TestApplicationScope)
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalHapticFeedback provides haptics, LocalSoundPlayer provides sounds) {
                    TutorialRouteUnderTest(viewModel)
                }
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

    /** The board's channel as a speech engine reads it: the repeat mark is unspoken. */
    private fun channel(): String? =
        composeRule
            .onNodeWithTag(BOARD_CANVAS_TEST_TAG)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
            ?.single()
            ?.replace("\u200B", "")

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
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val TUTORIAL_WRONG_TAP_SDK = 34

/** A tall phone at 1.5× density, so the board's cells are big enough to read. */
private const val TUTORIAL_WRONG_TAP_QUALIFIERS = "w400dp-h800dp-hdpi"
