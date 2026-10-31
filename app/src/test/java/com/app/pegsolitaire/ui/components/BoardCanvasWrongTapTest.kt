package com.app.pegsolitaire.ui.components

import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.ALL_CELLS
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.AMPLITUDE_PX
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.BOARD
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.CENTRE_HOLE
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.DESTINATION
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.FAR_HOLE
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.FAR_PEG
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.FRAME_MS
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.HELD_PEG
import com.app.pegsolitaire.ui.game.GameUiState
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * What the gameplay board draws for a wrong tap with animations on, read off its pixels
 * (D-06, `docs/ux/invalid-tap-feedback.md` §3.1 and §3.3; the board halves of WT-6 to WT-9, WT-11,
 * WT-16 and WT-17, and WT-19's ring).
 *
 * Every frame is compared with a control board that took the same selections and installs but no
 * wrong tap ([WrongTapBoards]), so the shake is a cell's distance from where the control draws it
 * and the nudge is the ring's radius over the control's — exact, whatever the breathing pulse is
 * doing. The motions' curves are `WrongTapMotionTest`'s; this class proves the board draws them on
 * the right cell, sideways only, in the right frames, and ends them when it should.
 *
 * WT-19 asks for Roborazzi golden images. The repo has no screenshot infrastructure, by the
 * tech-lead's ruling of 2026-09-21 (`docs/ux-spec.md` §13: "no golden-image suite in Phase 15;
 * filed as a post-v1.1 follow-up"), so the pictures are asserted as their contract instead: here
 * the ring at its nudge peak, `primary` at 1.18×, light and dark; the mark's in
 * `BoardCanvasWrongTapReducedMotionTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [WRONG_TAP_CANVAS_SDK], qualifiers = WRONG_TAP_CANVAS_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoardCanvasWrongTapTest {
    @get:Rule
    val composeRule = createWrongTapComposeRule(animationsOn = true)

    private val boards = WrongTapBoards(composeRule)

    // ---- The shake (WT-7's board half) -----------------------------------------------------

    @Test
    fun aWrongTapOnAnEmptyHoleShakesOnlyThatHoleSidewaysThroughTheSpecsKeyframes() {
        boards.show()
        val bounds = boards.canvas

        boards.wrongTapOn(CENTRE_HOLE)
        val offsets = mutableListOf<Pair<Long, Float>>()
        var tapFrame = NOT_YET
        repeat(SHAKE_FRAMES) {
            boards.frame()
            if (tapFrame == NOT_YET) tapFrame = boards.now
            val now = boards.render()
            offsets += (boards.now - tapFrame) to boards.shakeOf(now, CENTRE_HOLE)
            assertWithMessage("vertical drift").that(abs(boards.driftOf(now, CENTRE_HOLE))).isLessThan(SUBPIXEL)
            (ALL_CELLS - CENTRE_HOLE).forEach { other ->
                assertWithMessage("pixels changed in $other").that(boards.differingPixels(now, other)).isEqualTo(0)
            }
            assertWithMessage("the canvas's bounds").that(boards.canvas).isEqualTo(bounds)
        }

        // §3.1, typed in from the spec: (ms after the tap, multiples of A), each ± 1 px, ± 1 frame.
        SHAKE_KEYFRAMES.forEach { (atMs, amplitudes) ->
            val near = offsets.filter { abs(it.first - atMs) <= FRAME_MS }
            assertWithMessage("offset within a frame of $atMs ms, in px: $near")
                .that(near.any { abs(it.second - amplitudes * AMPLITUDE_PX) <= ONE_PX })
                .isTrue()
        }
        assertWithMessage("first move").that(offsets.first { abs(it.second) > ONE_PX }.second).isGreaterThan(0f)
        assertThat(offsets.maxOf { abs(it.second) }).isAtMost(AMPLITUDE_PX + ONE_PX)
        assertThat(boards.differingPixelsAnywhere(boards.render())).isEqualTo(0)
    }

    // ---- The redirects (WT-8, WT-9) --------------------------------------------------------

    @Test
    fun aWrongTapOnAnotherPegShakesItAndNudgesTheHeldRingWithoutRePoppingADot() {
        boards.show()
        boards.select(HELD_PEG)
        boards.frame()
        boards.frame()

        boards.wrongTapOn(FAR_PEG)
        val nudges = mutableListOf<Pair<Long, Float>>()
        val shakes = mutableListOf<Float>()
        var tapFrame = NOT_YET
        repeat(NUDGE_FRAMES) {
            boards.frame()
            if (tapFrame == NOT_YET) tapFrame = boards.now
            val now = boards.render()
            nudges += (boards.now - tapFrame) to boards.nudgeOf(now, HELD_PEG)
            shakes += boards.shakeOf(now, FAR_PEG)
            assertWithMessage("the destination's dot").that(boards.differingPixels(now, DESTINATION)).isEqualTo(0)
        }

        // 1.18× within 110 ms and back to 1.0× by 260 ms, each ± 1 frame (WT-8).
        assertThat(nudges.maxOf { it.second }).isWithin(NUDGE_TOLERANCE).of(NUDGE_PEAK)
        assertThat(nudges.first { it.second >= NUDGE_PEAK - NUDGE_TOLERANCE }.first).isAtMost(NUDGE_OUT_MS + FRAME_MS)
        nudges.filter { it.first >= NUDGE_BACK_BY_MS + FRAME_MS }.forEach { (atMs, nudge) ->
            assertWithMessage("nudge at $atMs ms").that(nudge).isWithin(SETTLED_TOLERANCE).of(1f)
        }
        assertThat(shakes.max()).isAtLeast(PEAK_SAMPLE_FLOOR * AMPLITUDE_PX)
    }

    @Test
    fun aWrongTapOnAHoleThatIsNotADestinationShakesItAndReplaysTheDotsFromNothing() {
        boards.show()
        boards.select(HELD_PEG)
        boards.settle()

        boards.wrongTapOn(FAR_HOLE)
        val bareDestination = mutableListOf<Boolean>()
        val shakes = mutableListOf<Float>()
        repeat(NUDGE_FRAMES) {
            boards.frame()
            val now = boards.render()
            bareDestination += boards.differingPixels(now, DESTINATION, against = CENTRE_HOLE, onControl = false) == 0
            shakes += boards.shakeOf(now, FAR_HOLE)
            assertWithMessage("the held ring").that(boards.differingPixels(now, HELD_PEG)).isEqualTo(0)
        }
        boards.settle()

        // From DOT_POP_START: the dot is gone entirely before it pops back (WT-9).
        assertWithMessage("frames showing a bare destination")
            .that(bareDestination.take(REPOP_WITHIN_FRAMES))
            .contains(true)
        assertThat(shakes.max()).isAtLeast(PEAK_SAMPLE_FLOOR * AMPLITUDE_PX)
        assertThat(boards.differingPixelsAnywhere(boards.render())).isEqualTo(0)
    }

    @Test
    fun withShowMovesOffAWrongTapOnAHoleNeverDrawsADotAndNudgesTheRingInstead() {
        // WT-9: re-popping dots the player has hidden would be a free hint.
        boards.show()
        boards.both { it.copy(showHints = false) }
        boards.select(HELD_PEG)
        boards.frame()
        boards.frame()

        boards.wrongTapOn(FAR_HOLE)
        val nudges = mutableListOf<Float>()
        repeat(NUDGE_FRAMES) {
            boards.frame()
            val now = boards.render()
            assertWithMessage("the destination is a bare hole")
                .that(boards.differingPixels(now, DESTINATION, against = CENTRE_HOLE, onControl = false))
                .isEqualTo(0)
            nudges += boards.nudgeOf(now, HELD_PEG)
        }

        assertThat(nudges.max()).isWithin(NUDGE_TOLERANCE).of(NUDGE_PEAK)
    }

    @Test
    fun holdingAPegThatCannotMoveAWrongTapOnAHoleNudgesItsRing() {
        // WT-9: zero destinations, so there is no dot to pop.
        boards.show()
        boards.select(FAR_PEG)
        boards.frame()
        boards.frame()

        boards.wrongTapOn(CENTRE_HOLE)
        val nudges = mutableListOf<Float>()
        repeat(NUDGE_FRAMES) {
            boards.frame()
            nudges += boards.nudgeOf(boards.render(), FAR_PEG)
        }

        assertThat(nudges.max()).isWithin(NUDGE_TOLERANCE).of(NUDGE_PEAK)
    }

    @Test
    fun theRingAtItsNudgePeakIsThePrimarySelectionRingAt118PercentInLightAndDark() {
        // WT-19's ring, as its contract.
        boards.show()
        listOf(false, true).forEach { dark ->
            boards.install(darkTheme = dark)
            boards.select(HELD_PEG)
            boards.frame()
            boards.frame()
            boards.wrongTapOn(FAR_PEG)
            val frames = (1..NUDGE_FRAMES).map { boards.frame().let { boards.render() } }

            val peak = frames.maxBy { boards.nudgeOf(it, HELD_PEG) }
            val theme = if (dark) "dark" else "light"
            assertWithMessage("peak nudge, $theme")
                .that(boards.nudgeOf(peak, HELD_PEG))
                .isWithin(NUDGE_TOLERANCE)
                .of(NUDGE_PEAK)
            boards.assertColour("the ring at its peak, $theme", boards.ringPixel(peak, HELD_PEG), boards.scheme.primary)
        }
    }

    // ---- Interruption (WT-11) -------------------------------------------------------------

    @Test
    fun aSecondWrongTapMidShakeRestartsTheShakeOnTheNewCellAndStillsTheFirstAtOnce() {
        boards.show()
        boards.wrongTapOn(CENTRE_HOLE)
        shakeUnderway(CENTRE_HOLE)

        boards.wrongTapOn(FAR_HOLE)
        val shakes = mutableListOf<Float>()
        repeat(SHAKE_FRAMES) {
            boards.frame()
            val now = boards.render()
            assertWithMessage("the first cell").that(boards.differingPixels(now, CENTRE_HOLE)).isEqualTo(0)
            shakes += boards.shakeOf(now, FAR_HOLE)
        }

        // Restarted at t = 0: at rest in the second tap's own frame — not at the phase the first
        // shake had reached — then rightward first, and the whole shake again.
        assertWithMessage("the new cell in the tap's frame").that(abs(shakes.first())).isLessThan(SUBPIXEL)
        assertThat(shakes.first { abs(it) > ONE_PX }).isGreaterThan(0f)
        assertThat(shakes.max()).isAtLeast(PEAK_SAMPLE_FLOOR * AMPLITUDE_PX)
        assertThat(boards.differingPixelsAnywhere(boards.render())).isEqualTo(0)
    }

    @Test
    fun everySuccessfulActionEndsTheShakeInItsOwnFrameAndForGood() {
        boards.show()
        successfulActions().forEach { action ->
            boards.install()
            action.before(boards)
            boards.frame()
            boards.wrongTapOn(action.tapped)
            shakeUnderway(action.tapped)

            action.act(boards)
            boards.frame()
            assertWithMessage("${action.name}: its own frame")
                .that(boards.differingPixelsAnywhere(boards.render()))
                .isEqualTo(0)

            // Undone again — the context of the wrong tap restored — the shake stays over.
            action.undo(boards)
            repeat(SHAKE_FRAMES) {
                boards.frame()
                assertWithMessage("${action.name}, undone")
                    .that(boards.differingPixelsAnywhere(boards.render()))
                    .isEqualTo(0)
            }
        }
    }

    // ---- When nothing is drawn (WT-6, WT-16, WT-17) -----------------------------------------

    @Test
    fun aBoardComposedWithACarriedWrongTapNeverReplaysIt() {
        // WT-6: a rotation recomposes the screen from scratch while the ViewModel keeps its state.
        boards.underTest =
            GameUiState.initial(BOARD).copy(rejectedTapTick = CARRIED_ORDINAL, rejectedTapCell = CENTRE_HOLE)
        boards.show()

        assertNothingDrawnFor(SHAKE_FRAMES)
    }

    @Test
    fun aReadOnlyBoardNeverDrawsAWrongTap() {
        // WT-17.
        boards.show(interactive = false)
        listOf(CENTRE_HOLE, FAR_PEG).forEach { cell ->
            boards.wrongTapOn(cell)
            assertNothingDrawnFor(SHAKE_FRAMES)
        }
    }

    @Test
    fun theTutorialBoardRedirectsButNeverShakesTheTappedCell() {
        // WT-16's board half (D-01j(2)): T-W1 shows nothing new, T-W2 nudges, T-W3 re-pops.
        boards.show(correction = false)
        boards.wrongTapOn(CENTRE_HOLE)
        assertNothingDrawnFor(SHAKE_FRAMES)

        boards.select(HELD_PEG)
        boards.frame()
        boards.frame()
        boards.wrongTapOn(FAR_PEG)
        val nudges = mutableListOf<Float>()
        repeat(NUDGE_FRAMES) {
            boards.frame()
            val now = boards.render()
            assertWithMessage("the tapped peg").that(boards.differingPixels(now, FAR_PEG)).isEqualTo(0)
            nudges += boards.nudgeOf(now, HELD_PEG)
        }
        assertThat(nudges.max()).isWithin(NUDGE_TOLERANCE).of(NUDGE_PEAK)

        boards.settle()
        boards.wrongTapOn(FAR_HOLE)
        val bareDestination = mutableListOf<Boolean>()
        repeat(NUDGE_FRAMES) {
            boards.frame()
            val now = boards.render()
            assertWithMessage("the tapped hole").that(boards.differingPixels(now, FAR_HOLE)).isEqualTo(0)
            bareDestination += boards.differingPixels(now, DESTINATION, against = CENTRE_HOLE, onControl = false) == 0
        }
        assertThat(bareDestination.take(REPOP_WITHIN_FRAMES)).contains(true)
    }

    // ---- Helpers ---------------------------------------------------------------------------

    /** Runs the frames that take a fresh shake on [cell] well past its first keyframe. */
    private fun shakeUnderway(cell: Position) {
        repeat(INTO_THE_SHAKE_FRAMES) { boards.frame() }
        assertWithMessage("$cell shaking").that(abs(boards.shakeOf(boards.render(), cell))).isGreaterThan(ONE_PX)
    }

    private fun assertNothingDrawnFor(frames: Int) {
        repeat(frames) {
            boards.frame()
            assertThat(boards.differingPixelsAnywhere(boards.render())).isEqualTo(0)
        }
    }

    /** One successful action of WT-11: what it needs [before] the wrong tap, the [act], its [undo]. */
    private class SuccessfulAction(
        val name: String,
        val tapped: Position,
        val before: (WrongTapBoards) -> Unit,
        val act: (WrongTapBoards) -> Unit,
        val undo: (WrongTapBoards) -> Unit,
    )

    /**
     * WT-11's list: a selection, a put-down, a jump, an undo, a hint reveal, and a board install
     * (a restart on an untouched board changes nothing but the wrong tap's cell). Each is applied
     * to both boards at once, as the ViewModel would; each undo restores the wrong tap's context.
     */
    private fun successfulActions(): List<SuccessfulAction> {
        val jump = RulesEngine.legalMoves(BOARD).first { it.from == HELD_PEG }
        val jumped = RulesEngine.applyMove(BOARD, jump)
        val none: (WrongTapBoards) -> Unit = {}
        return listOf(
            SuccessfulAction("a selection", CENTRE_HOLE, none, { it.select(HELD_PEG) }, { it.putDown() }),
            SuccessfulAction(
                "a put-down",
                FAR_HOLE,
                { it.select(HELD_PEG) },
                { it.putDown() },
                { it.select(HELD_PEG) },
            ),
            SuccessfulAction(
                "a jump",
                FAR_HOLE,
                { it.select(HELD_PEG) },
                { b -> b.both { it.copy(board = jumped, selected = null, legalTargets = emptyList()) } },
                { b -> b.both { it.copy(board = BOARD) } },
            ),
            SuccessfulAction(
                "an undo",
                CENTRE_HOLE,
                { b -> b.both { it.copy(board = jumped) } },
                { b -> b.both { it.copy(board = BOARD) } },
                { b -> b.both { it.copy(board = jumped) } },
            ),
            SuccessfulAction(
                "a hint reveal",
                CENTRE_HOLE,
                none,
                { b -> b.both { it.copy(hintSource = HELD_PEG, hintTarget = DESTINATION) } },
                { b -> b.both { it.copy(hintSource = null, hintTarget = null) } },
            ),
            SuccessfulAction("a board install", CENTRE_HOLE, none, { it.install() }, none),
        )
    }

    private companion object {
        /** §3.1's keyframes: (ms after the tap, multiples of A). */
        val SHAKE_KEYFRAMES =
            listOf(
                46L to 1.0f,
                105L to -0.75f,
                165L to 0.45f,
                219L to -0.2f,
                270L to 0f,
            )

        const val NOT_YET = -1L
        const val ONE_PX = 1f
        const val SUBPIXEL = 0.5f

        /** On 16 ms frames the sampled peak lands within a few percent of +A. */
        const val PEAK_SAMPLE_FLOOR = 0.85f

        const val NUDGE_PEAK = 1.18f
        const val NUDGE_TOLERANCE = 0.02f
        const val SETTLED_TOLERANCE = 0.005f
        const val NUDGE_OUT_MS = 110L
        const val NUDGE_BACK_BY_MS = 260L

        /** The dot is gone entirely within a few frames of the tap, then pops back. */
        const val REPOP_WITHIN_FRAMES = 6

        const val SHAKE_FRAMES = 22
        const val NUDGE_FRAMES = 20
        const val INTO_THE_SHAKE_FRAMES = 5
        const val CARRIED_ORDINAL = 3
    }
}
