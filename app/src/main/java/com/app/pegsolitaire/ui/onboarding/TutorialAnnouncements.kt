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
import com.app.pegsolitaire.ui.components.JumpAnnouncements
import com.app.pegsolitaire.ui.components.OrdinalLine
import com.app.pegsolitaire.ui.components.RejectedTapAnnouncements
import com.app.pegsolitaire.ui.components.SelectionAnnouncements
import com.app.pegsolitaire.ui.components.moveInputFocusTo
import com.app.pegsolitaire.ui.components.rememberBoardSpeechFormat

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
 * order — and "Got it" on completion.
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
    JumpAnnouncements(state.board, announcer)
    CoachAnnouncements(state, announcer)
    StepAnnouncements(state, announcer, boardStopFocus, touchExploration)
    CompletionAnnouncement(state.completed, announcer, gotItFocus, touchExploration)
}

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
 * A step's line, "Step 2 of 3. …", when the tutorial opens and whenever a step's board is installed.
 * Focus moves to the new board stop only for a step that replaced the one the player was on: the
 * value this composition first sees moves nothing (D-TBa item 6).
 */
@Composable
private fun StepAnnouncements(
    state: TutorialUiState,
    announcer: BoardAnnouncer,
    boardStopFocus: FocusRequester,
    touchExploration: Boolean,
) {
    val format = rememberBoardSpeechFormat()
    val focusManager = LocalFocusManager.current
    val window = LocalWindowInfo.current
    val seen = remember { SeenStep() }
    LaunchedEffect(state.stepIndex) {
        val replacedAStep = seen.index != null
        seen.index = state.stepIndex
        if (state.completed) return@LaunchedEffect
        announcer.announce(format.stepLine(state.stepNumber, TutorialUiState.STEP_COUNT, state.captionRes))
        if (replacedAStep && touchExploration) focusManager.moveInputFocusTo(boardStopFocus, window)
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
