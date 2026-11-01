package com.app.pegsolitaire.ui.components

import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.CENTRE_HOLE
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.DESTINATION
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.FAR_HOLE
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.FAR_PEG
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.FRAME_MS
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.HELD_PEG
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What the gameplay board draws for a wrong tap with the system's animations off (D-06a,
 * `docs/ux/invalid-tap-feedback.md` §3.2; WT-10, WT-19's mark, and the reduced-motion halves of
 * WT-11, WT-16 and WT-17), read off its pixels against a control board ([WrongTapBoards]).
 *
 * The phone is modelled whole: the animation scale is 0 in the composition's `MotionDurationScale`,
 * which both the board's reduced-motion gate and every `Animatable` read. The clock half matters
 * because the mark's 450 ms is functional: held on an animation clock it would end in its first
 * frame on exactly the phones it exists for, and this class would see it go.
 *
 * WT-10's two instants are read on the paused clock's 16 ms frames. The mark is drawn in the tap's
 * own frame and in every frame through 440 ms, whole each time; the hold's timer ends at 450 ms
 * and the mark is gone by the frame after the next one. That one frame is the test harness's own:
 * Robolectric's paused looper delivers a timer's state write to the composition one frame late,
 * where a phone's main thread delivers it before the next vsync.
 *
 * WT-19 asks for Roborazzi golden images; the repo has none by the 2026-09-21 ruling (see
 * `BoardCanvasWrongTapTest`), so the mark is asserted as its contract: 12 dashes,
 * `onSurfaceVariant`, 3 px wide (`wrong_tap_mark_width` at density 1) at 0.40 × cellSize, on a
 * hole and on a peg, in light and dark.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [WRONG_TAP_CANVAS_SDK], qualifiers = WRONG_TAP_CANVAS_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoardCanvasWrongTapReducedMotionTest {
    @get:Rule
    val composeRule = createWrongTapComposeRule(animationsOn = false)

    private val boards = WrongTapBoards(composeRule)

    @Test
    fun aWrongTapMarksTheHoleFromItsOwnFrameWholeAndStillThroughTheHoldThenLeavesNothing() {
        boards.show()
        boards.wrongTapOn(CENTRE_HOLE)
        boards.frame()
        val tapFrame = boards.now

        val marked = mutableListOf<Long>()
        repeat(WATCH_FRAMES) {
            val now = boards.render()
            val atMs = boards.now - tapFrame
            val dashes = boards.dashesAround(now, CENTRE_HOLE)
            assertWithMessage("dashes at $atMs ms: whole or none, never a fade").that(dashes).isAnyOf(0, MARK_DASHES)
            if (dashes == MARK_DASHES) {
                marked += atMs
                val dash = boards.firstDashMiddle(now, CENTRE_HOLE)
                boards.assertColour("the mark at $atMs ms", dash, boards.scheme.onSurfaceVariant)
            }
            assertWithMessage("the hole at $atMs ms").that(boards.differingDrawingPixels(now, CENTRE_HOLE)).isEqualTo(0)
            boards.frame()
        }

        assertWithMessage("frames showing the mark, ms after the tap's frame").that(marked.first()).isEqualTo(0L)
        assertThat(marked).isEqualTo((0L..marked.last() step FRAME_MS).toList())
        assertThat(marked.last()).isAtLeast(STILL_DRAWN_AT_MS)
        assertThat(marked.last()).isAtMost(MARK_HOLD_MS + FRAME_MS)
        assertThat(boards.differingPixelsAnywhere(boards.render())).isEqualTo(0)
    }

    @Test
    fun noWrongTapMovesARingRePopsADotOrDisplacesTheTappedCell() {
        boards.show()
        boards.select(HELD_PEG)
        boards.frame()

        // W3, then W2: the redirects are off with animations (§3.2) and the mark replaces the shake.
        listOf(FAR_HOLE, FAR_PEG).forEach { cell ->
            boards.wrongTapOn(cell)
            repeat(WATCH_FRAMES) {
                boards.frame()
                val now = boards.render()
                assertWithMessage("the held ring, after $cell").that(boards.differingPixels(now, HELD_PEG)).isEqualTo(0)
                assertWithMessage("the dot, after $cell").that(boards.differingPixels(now, DESTINATION)).isEqualTo(0)
                assertWithMessage("$cell itself").that(boards.differingDrawingPixels(now, cell)).isEqualTo(0)
            }
        }
    }

    @Test
    fun theMarkIsTwelveOnSurfaceVariantDashesOfItsOwnWidthOnAHoleAndOnAPegInLightAndDark() {
        boards.show()
        listOf(false, true).forEach { dark ->
            listOf(CENTRE_HOLE, FAR_PEG).forEach { cell ->
                boards.install(darkTheme = dark)
                // A tap on a peg is wrong only while another is held (W2).
                if (cell == FAR_PEG) boards.select(HELD_PEG)
                boards.frame()
                boards.wrongTapOn(cell)
                boards.frame()

                val marked = boards.render()
                val where = "$cell, ${if (dark) "dark" else "light"}"
                assertWithMessage("dashes, $where").that(boards.dashesAround(marked, cell)).isEqualTo(MARK_DASHES)
                val (radius, width) = boards.markStroke(marked, cell)
                val dash = boards.firstDashMiddle(marked, cell)
                boards.assertColour("the mark, $where", dash, boards.scheme.onSurfaceVariant)
                assertWithMessage("radius, $where").that(radius).isWithin(STROKE_TOLERANCE_PX).of(MARK_RADIUS_PX)
                assertWithMessage("width, $where").that(width).isWithin(STROKE_TOLERANCE_PX).of(MARK_WIDTH_PX)
            }
        }
    }

    @Test
    fun aSuccessfulActionTakesTheMarkAwayInItsOwnFrameAndForGood() {
        // WT-11 with animations off: the mark ends like the shake does.
        boards.show()
        boards.wrongTapOn(CENTRE_HOLE)
        boards.frame()
        assertThat(boards.dashesAround(boards.render(), CENTRE_HOLE)).isEqualTo(MARK_DASHES)

        boards.select(HELD_PEG)
        boards.frame()
        assertThat(boards.differingPixelsAnywhere(boards.render())).isEqualTo(0)

        // Put back down, the wrong tap's context returns, and the mark does not.
        boards.putDown()
        assertNothingDrawnFor(WATCH_FRAMES)
    }

    @Test
    fun theTutorialBoardDrawsNothingForAnyWrongTap() {
        // WT-16 with animations off: the tutorial never marks, and its redirects are off too.
        boards.show(correction = false)
        boards.wrongTapOn(CENTRE_HOLE)
        assertNothingDrawnFor(WATCH_FRAMES)

        boards.select(HELD_PEG)
        boards.wrongTapOn(FAR_HOLE)
        assertNothingDrawnFor(WATCH_FRAMES)

        boards.wrongTapOn(FAR_PEG)
        assertNothingDrawnFor(WATCH_FRAMES)
    }

    @Test
    fun aReadOnlyBoardNeverMarksAWrongTap() {
        // WT-17 with animations off.
        boards.show(interactive = false)
        boards.wrongTapOn(CENTRE_HOLE)

        assertNothingDrawnFor(WATCH_FRAMES)
    }

    private fun assertNothingDrawnFor(frames: Int) {
        repeat(frames) {
            boards.frame()
            assertThat(boards.differingPixelsAnywhere(boards.render())).isEqualTo(0)
        }
    }

    private companion object {
        /** Past the hold and its frames, with room to see that nothing comes back. */
        const val WATCH_FRAMES = 36

        const val MARK_DASHES = 12

        /** WT-10's instants, ms after the tap. */
        const val STILL_DRAWN_AT_MS = 440L
        const val MARK_HOLD_MS = 450L

        /** 0.40 × a 100 px cell, and 3 dp at density 1. */
        const val MARK_RADIUS_PX = 40f
        const val MARK_WIDTH_PX = 3f
        const val STROKE_TOLERANCE_PX = 0.4f
    }
}
