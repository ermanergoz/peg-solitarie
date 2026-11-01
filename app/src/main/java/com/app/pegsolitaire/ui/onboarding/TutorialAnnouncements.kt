package com.app.pegsolitaire.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.components.BoardAnnouncer
import com.app.pegsolitaire.ui.components.BoardEvent
import com.app.pegsolitaire.ui.components.ChangeEffect
import com.app.pegsolitaire.ui.components.OrdinalLine
import com.app.pegsolitaire.ui.components.RejectedTapAnnouncements
import com.app.pegsolitaire.ui.components.SelectionAnnouncements
import com.app.pegsolitaire.ui.components.moveInputFocusTo
import com.app.pegsolitaire.ui.components.rememberBoardSpeechFormat
import kotlinx.coroutines.delay

/**
 * Every line the tutorial's board speaks (D-TB as amended by D-TBa items 3 and 6, `docs/ux-spec.md`
 * §11.1, "The tutorial"), through the shared board announcer — the same channel and the same
 * selection, rejected-tap and jump lines as gameplay — plus the tutorial's own three: the coach's
 * hint, each step's line and the done line.
 *
 * Composed in that order, so when one state update brings two events — the third non-progressing
 * tap and the coach it raises — the later, newer line is the one left on the channel.
 *
 * Two focus moves (§11.1's focus table, made by input focus and only while touch exploration is on):
 * a new step's board stop on steps 2 and 3 — never on the first, which the screen meets in its own
 * order — and "Got it" on completion. On steps 2 and 3 the step's line follows its board stop's
 * focus, and the jump that cleared the step before puts no line of its own (the talkback-board
 * device-2 ruling §2, F-B1; D-TBe).
 */
@Composable
internal fun TutorialBoardAnnouncements(
    state: TutorialUiState,
    announcer: BoardAnnouncer,
    boardStopFocus: FocusRequester,
    gotItFocus: FocusRequester,
    touchExploration: Boolean,
) {
    SelectionAnnouncements(state.board, announcer)
    RejectedTapAnnouncements(state.board, announcer)
    TutorialJumpAnnouncements(state, announcer, touchExploration)
    CoachAnnouncements(state, announcer)
    StepAnnouncements(state, announcer, boardStopFocus, touchExploration)
    CompletionAnnouncement(state.completed, announcer, gotItFocus, touchExploration)
}

/**
 * A jump's line, as gameplay's shared jump source words it, as its animation begins — except, while
 * touch exploration is on, for the jump that clears step 1 or step 2: the next step's line stands for
 * it (a line a newer one replaced is not owed, §11.1). Spoken first, that line held TalkBack's queue
 * while the step swap's events arrived, and the step line waiting behind it was dropped (the
 * talkback-board device-2 ruling §1–§2, F-B1). Step 3's jumps, its completing jump included, keep
 * their lines.
 */
@Composable
private fun TutorialJumpAnnouncements(
    state: TutorialUiState,
    announcer: BoardAnnouncer,
    touchExploration: Boolean,
) {
    val format = rememberBoardSpeechFormat()
    ChangeEffect(state.board.animating) { _, now ->
        if (now != null && !(touchExploration && state.clearsAStepBeforeTheLast)) {
            announcer.announce(
                format.announcement(BoardEvent.Jumped(now.from, now.to, now.removed, state.board.board.pegCount)),
            )
        }
    }
}

/**
 * Whether this state's board, just jumped, clears a step the tutorial has another after: one peg
 * left on a step whose number is below the count, so a new step's board is about to replace it.
 */
private val TutorialUiState.clearsAStepBeforeTheLast: Boolean
    get() = board.board.pegCount == TutorialUiState.STEP_COMPLETE_PEGS && stepNumber < TutorialUiState.STEP_COUNT

/** Each coach escalation speaks the move it points at, as gameplay's revealed hint does. */
@Composable
private fun CoachAnnouncements(
    state: TutorialUiState,
    announcer: BoardAnnouncer,
) {
    val format = rememberBoardSpeechFormat()
    OrdinalLine(state.coachTick, announcer) {
        val from = state.board.hintSource
        val to = state.board.hintTarget
        if (from != null && to != null) format.announcement(BoardEvent.Hinted(from, to)) else null
    }
}

/**
 * A step's line, "Step 2 of 3. …", whenever a step's board replaces the one the player was on, and
 * the step's caption alone when the tutorial opens: the pips' own first focus already says
 * "Step 1 of 3" (brief A2.4; the talkback-board device-2 ruling §3). Focus moves to the new board
 * stop only for a step that replaced the one the player was on: the value this composition first
 * sees moves nothing (D-TBa item 6).
 *
 * While touch exploration is on, that step's line goes on [TutorialAnim.STEP_LINE_AFTER_FOCUS_MS]
 * after the move, so TalkBack hears the board stop, then the line: a line already waiting in
 * TalkBack's queue when the swap's events arrive can be dropped, and one queued after them is
 * spoken (the device-2 ruling §1–§2, F-B1; D-TBe). With touch exploration off nothing moves, and
 * the line goes on at once.
 */
@Composable
private fun StepAnnouncements(
    state: TutorialUiState,
    announcer: BoardAnnouncer,
    boardStopFocus: FocusRequester,
    touchExploration: Boolean,
) {
    val format = rememberBoardSpeechFormat()
    val caption = stringResource(state.captionRes)
    val focusManager = LocalFocusManager.current
    val window = LocalWindowInfo.current
    val seen = remember { SeenStep() }
    LaunchedEffect(state.stepIndex) {
        val replacedAStep = seen.index != null
        seen.index = state.stepIndex
        if (state.completed) return@LaunchedEffect
        if (!replacedAStep) {
            announcer.announce(caption)
            return@LaunchedEffect
        }
        val stepLine = format.stepLine(state.stepNumber, TutorialUiState.STEP_COUNT, state.captionRes)
        if (touchExploration) {
            focusManager.moveInputFocusTo(boardStopFocus, window)
            delay(TutorialAnim.STEP_LINE_AFTER_FOCUS_MS)
        }
        announcer.announce(stepLine)
    }
}

/** The done line when the third step is cleared, then focus on "Got it". */
@Composable
private fun CompletionAnnouncement(
    completed: Boolean,
    announcer: BoardAnnouncer,
    gotItFocus: FocusRequester,
    touchExploration: Boolean,
) {
    val doneLine = stringResource(R.string.tutorial_done_caption)
    val focusManager = LocalFocusManager.current
    val window = LocalWindowInfo.current
    ChangeEffect(completed) { _, now ->
        if (now) {
            announcer.announce(doneLine)
            if (touchExploration) focusManager.moveInputFocusTo(gotItFocus, window)
        }
    }
}

/** The step a [StepAnnouncements] last saw, or `null` before its first. Read and written only in its effect. */
private class SeenStep(
    var index: Int? = null,
)
