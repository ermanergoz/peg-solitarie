package com.app.pegsolitaire.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.mutableStateOf
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.game.WrongTapRedirect
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What one frame of the board's answer reads, given the answer, the elapsed-time clock and which
 * answer that clock is timing (`docs/ux/invalid-tap-feedback.md` §3.1–§3.3). Runner-free.
 *
 * The first test pins what the board pixel tests cannot see. On a phone, a new answer's effect
 * restarts the clock only after the tap's own frame has been drawn, so that frame reads whatever
 * phase an interrupted shake left on the clock — the new cell would jump to it for one frame.
 * The paused test clock runs effects before it draws, so only this test catches that frame.
 */
class WrongTapFeedbackTest {
    @Test
    fun aNewAnswerIsDrawnAtRestUntilItsOwnEffectHasClaimedTheClock() {
        val clock = Animatable(INTERRUPTED_AT_MS)
        val timing = mutableStateOf<WrongTapAnswer?>(EARLIER)
        val feedback = WrongTapFeedback(answer = SHAKE_AND_NUDGE, clock = clock, timing = timing, dotRePops = 0)

        assertThat(feedback.shakeAt(TAPPED, CELL_PX)).isEqualTo(0f)
        assertThat(feedback.ringNudge()).isEqualTo(1f)

        timing.value = SHAKE_AND_NUDGE
        assertThat(feedback.shakeAt(TAPPED, CELL_PX)).isNotEqualTo(0f)
        assertThat(feedback.ringNudge()).isGreaterThan(1f)
    }

    @Test
    fun onlyTheTappedCellIsDisplaced() {
        val feedback = timed(SHAKE_AND_NUDGE)

        assertThat(feedback.shakeAt(TAPPED, CELL_PX)).isNotEqualTo(0f)
        assertThat(feedback.shakeAt(ELSEWHERE, CELL_PX)).isEqualTo(0f)
    }

    @Test
    fun aMarkIsDrawnOnItsCellAndNeitherShakesNorNudges() {
        val feedback = timed(MARK)

        assertThat(feedback.markedCell()).isEqualTo(TAPPED)
        assertThat(feedback.shakeAt(TAPPED, CELL_PX)).isEqualTo(0f)
        assertThat(feedback.ringNudge()).isEqualTo(1f)
    }

    @Test
    fun aShakeMarksNothingAndNoAnswerDrawsNothing() {
        assertThat(timed(SHAKE_AND_NUDGE).markedCell()).isNull()

        val none =
            WrongTapFeedback(
                answer = null,
                clock = Animatable(INTERRUPTED_AT_MS),
                timing = mutableStateOf(null),
                dotRePops = 0,
            )
        assertThat(none.markedCell()).isNull()
        assertThat(none.shakeAt(TAPPED, CELL_PX)).isEqualTo(0f)
        assertThat(none.ringNudge()).isEqualTo(1f)
    }

    /** [answer] with its own clock running, [INTERRUPTED_AT_MS] into it. */
    private fun timed(answer: WrongTapAnswer): WrongTapFeedback =
        WrongTapFeedback(
            answer = answer,
            clock = Animatable(INTERRUPTED_AT_MS),
            timing = mutableStateOf(answer),
            dotRePops = 0,
        )

    private companion object {
        /** Past the shake's first keyframe and inside the nudge: both are away from rest here. */
        const val INTERRUPTED_AT_MS = 64f
        const val CELL_PX = 100f

        val TAPPED = Position(1, 1)
        val ELSEWHERE = Position(0, 0)

        private val CONTEXT =
            WrongTapContext(
                board = Board(spec = BoardSpec(inPlay = listOf(TAPPED, ELSEWHERE), target = null), pegs = 0L),
                selected = null,
                hintSource = null,
                rejectedTapCell = TAPPED,
            )

        val SHAKE_AND_NUDGE = WrongTapAnswer(TAPPED, WrongTapCorrection.Shake, WrongTapRedirect.RingNudge, CONTEXT)
        val MARK = WrongTapAnswer(TAPPED, WrongTapCorrection.Mark, WrongTapRedirect.None, CONTEXT)
        val EARLIER = WrongTapAnswer(ELSEWHERE, WrongTapCorrection.Shake, WrongTapRedirect.None, CONTEXT)
    }
}
