package com.app.pegsolitaire.ui.components

import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.AMPLITUDE_PX
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.CENTRE_HOLE
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.FAR_HOLE
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.FAR_PEG
import com.app.pegsolitaire.ui.components.WrongTapBoards.Companion.HELD_PEG
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * "Remove animations" turned on and off **while the board stays composed** (QA's FAIL,
 * tech-lead-signoff.md §3): the next wrong tap takes the path the system scale says now, not the
 * one it said when the board was first composed. Read off the pixels against the control board
 * ([WrongTapBoards]), on a scale the test turns live ([SystemMotionScale]).
 *
 * The gameplay board answers with the mark and no displacement while animations are off, and with
 * the shake once they are back on. The tutorial board draws nothing for any wrong tap while they
 * are off, its redirects included, and nudges again once they are back on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [WRONG_TAP_CANVAS_SDK], qualifiers = WRONG_TAP_CANVAS_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoardCanvasWrongTapLiveMotionTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createWrongTapComposeRule(motion)

    private val boards = WrongTapBoards(composeRule)

    @Test
    fun aLiveToggleOfTheSystemAnimationsSwitchesTheNextWrongTapBetweenTheMarkAndTheShake() {
        boards.show()

        motion.turnAnimationsOff()
        boards.frame()
        boards.wrongTapOn(CENTRE_HOLE)
        boards.frame()
        val marked = boards.render()
        assertWithMessage("dashes in the tap's frame")
            .that(boards.dashesAround(marked, CENTRE_HOLE))
            .isEqualTo(MARK_DASHES)
        repeat(SHAKE_FRAMES) {
            boards.frame()
            val now = boards.render()
            assertWithMessage("the tapped hole, animations off")
                .that(boards.differingDrawingPixels(now, CENTRE_HOLE))
                .isEqualTo(0)
        }
        boards.settle()

        motion.turnAnimationsOn()
        boards.frame()
        boards.wrongTapOn(CENTRE_HOLE)
        val offsets = mutableListOf<Float>()
        repeat(SHAKE_FRAMES) {
            boards.frame()
            val now = boards.render()
            assertWithMessage("dashes, animations on").that(boards.dashesAround(now, CENTRE_HOLE)).isEqualTo(0)
            offsets += abs(boards.shakeOf(now, CENTRE_HOLE))
        }
        assertWithMessage("the shake's largest offset, px").that(offsets.max()).isGreaterThan(AMPLITUDE_PX / 2)
    }

    @Test
    fun aLiveToggleOfTheSystemAnimationsStillsTheTutorialsRedirectsAndThenLetsThemMoveAgain() {
        boards.show(correction = false)
        boards.select(HELD_PEG)
        boards.settle()

        motion.turnAnimationsOff()
        boards.frame()
        // T-W2 would nudge the held ring, T-W3 would re-pop the dots: with animations off, neither.
        listOf(FAR_PEG, FAR_HOLE).forEach { cell ->
            boards.wrongTapOn(cell)
            assertNothingDrawnFor(SHAKE_FRAMES, cell)
        }

        motion.turnAnimationsOn()
        boards.frame()
        boards.wrongTapOn(FAR_PEG)
        val nudges = mutableListOf<Float>()
        repeat(SHAKE_FRAMES) {
            boards.frame()
            nudges += boards.nudgeOf(boards.render(), HELD_PEG)
        }
        assertWithMessage("the held ring's largest nudge").that(nudges.max()).isWithin(NUDGE_TOLERANCE).of(NUDGE_PEAK)
    }

    private fun assertNothingDrawnFor(
        frames: Int,
        after: Position,
    ) {
        repeat(frames) {
            boards.frame()
            assertWithMessage("pixels changed after a wrong tap on $after")
                .that(boards.differingPixelsAnywhere(boards.render()))
                .isEqualTo(0)
        }
    }

    private companion object {
        const val MARK_DASHES = 12

        /** Past the shake's 270 ms and the nudge's 260 ms, in 16 ms frames. */
        const val SHAKE_FRAMES = 22

        /** The ring at its nudge peak, 1.18 × its rest radius (§3.3). */
        const val NUDGE_PEAK = 1.18f
        const val NUDGE_TOLERANCE = 0.02f
    }
}
