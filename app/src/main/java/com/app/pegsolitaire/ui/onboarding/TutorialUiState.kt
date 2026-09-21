package com.app.pegsolitaire.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.tutorial.TutorialBoards
import com.app.pegsolitaire.ui.game.GameUiState

/**
 * The single immutable UI state for the tutorial screen (ONB-01, D-01/D-01a-i).
 *
 * Owned by [TutorialViewModel] and exposed over a read-only `StateFlow`; `TutorialScreen` is
 * a stateless render of this object.
 *
 * ## What it deliberately does not carry
 *
 * **No error, no score, no retry count** (D-01c) — a wrong tap is never a failure, so there is
 * nothing for such a field to hold, and `TutorialViewModelTest` asserts their absence by
 * reflection precisely because a behavioural test cannot prove a field is missing. No
 * move-rewind state either: §5 removes that control from the tutorial entirely, and these
 * boards make being boxed in impossible, so there would be nothing to rescue the player from.
 *
 * @property stepIndex which of [TutorialBoards.ALL] is installed, 0-based. Drives the pips.
 * @property board the `GameUiState` handed straight to `BoardCanvas`. It is a **UI model
 *   assembled here**, not evidence that the game's own state holder is involved (D-01a as
 *   clarified by D-01g): `GameUiState` requires a `status`, and `BoardCanvas` never reads it,
 *   so it is pinned at [GameStatus.Playing] and the step-completion predicate is evaluated
 *   separately as `board.pegCount == 1`. Reaching for `GameStatus` to decide completion is
 *   the first step down the path that ends at the gameplay state holder §5.1 forbids here.
 * @property captionRes the current caption — the installed step's, or `tutorial_done_caption`
 *   once the third step is cleared.
 * @property completed whether T3 is cleared, which is the only thing that raises the footer's
 *   completion CTA. Nothing behind it moves: no star is banked, no snapshot cleared, no ladder
 *   advanced (§5.1).
 * @property coachTick a monotonic ordinal advanced once per coach escalation (§4.4 class 3),
 *   so the screen can run a single pulse per escalation. An ordinal rather than a `Boolean`
 *   for the same reason the gameplay state's one-shot cue ordinals are: two escalations must leave
 *   a distinguishable state trace. Starts at 0, which the screen reads as "nothing to pulse".
 */
@Immutable
data class TutorialUiState(
    val stepIndex: Int,
    val board: GameUiState,
    @param:StringRes val captionRes: Int,
    val completed: Boolean = false,
    val coachTick: Int = 0,
) {
    /** The 1-based step number the pip row announces ("Step 2 of 3"). */
    val stepNumber: Int get() = stepIndex + STEP_NUMBER_OFFSET

    /** This state with [board] replaced; the step cursor and caption are untouched. */
    fun withBoard(board: GameUiState): TutorialUiState = copy(board = board)

    /** The finished state: the done caption, the CTA raised, the board left as the player left it. */
    fun completed(): TutorialUiState = copy(completed = true, captionRes = R.string.tutorial_done_caption)

    companion object {
        /** How many steps the tutorial runs — the boards themselves are the authority. */
        val STEP_COUNT: Int get() = TutorialBoards.ALL.size

        /**
         * The peg count at which a step is complete — **the same test for all three steps**,
         * because all three footprints are proven dead-end-free to one peg. Named rather than
         * inlined (QUAL-03), and deliberately not `BoardSpec.minimumPegs`: §4.1 rules that
         * comparison out, because it is the game's own solved test and reaching for it leads
         * straight back to the state holder §5.1 rules out here.
         */
        const val STEP_COMPLETE_PEGS = 1

        /** The first step, T1. */
        private const val FIRST_STEP = 0

        /** Step indices are 0-based; the pip row counts from 1. */
        private const val STEP_NUMBER_OFFSET = 1

        /** The tutorial as it opens, and as a replay from Settings always re-opens: T1. */
        fun initial(): TutorialUiState = forStep(FIRST_STEP)

        /** The state that installs step [index]: its board, its caption, a cleared coach. */
        fun forStep(index: Int): TutorialUiState =
            TutorialUiState(
                stepIndex = index,
                board =
                    GameUiState.initial(board = TutorialBoards.ALL[index]).copy(
                        // D-01b: forced on regardless of the persisted setting, so legal
                        // destinations are always visible and no step is a guessing game.
                        showHints = true,
                    ),
                captionRes = CAPTIONS[index],
            )

        /** One caption per step, in [TutorialBoards.ALL] order. */
        private val CAPTIONS =
            listOf(
                R.string.tutorial_step_1_caption,
                R.string.tutorial_step_2_caption,
                R.string.tutorial_step_3_caption,
            )
    }
}
