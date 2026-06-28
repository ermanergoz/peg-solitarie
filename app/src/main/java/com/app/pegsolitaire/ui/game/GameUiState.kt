package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position

/**
 * The single immutable UI state for the playable board screen.
 *
 * Owned by `GameViewModel` and exposed over a read-only `StateFlow`; the Canvas
 * is a stateless render of this object (state hoisting — RESEARCH Pitfall 10).
 * All fields are `val` and the type is `@Immutable`, so Compose can skip
 * recomposition on equal instances and game-over is **replayable state**
 * (`status`), never a one-shot effect (T-03-02).
 *
 * @property board the current post-move board (single source of truth for cells).
 * @property selected the currently-selected peg, or `null` when nothing is selected.
 * @property legalTargets the empty holes the [selected] peg may legally jump to
 *   (D-04 highlight); always empty when [selected] is `null`.
 * @property status the lifecycle status driving the game-over overlay (D-05).
 * @property animating the in-flight jump animation, or `null` when settled (D-07/D-08).
 * @property confirmRestart whether the mid-game restart confirmation is showing (D-06).
 */
@Immutable
data class GameUiState(
    val board: Board,
    val selected: Position?,
    val legalTargets: List<Position>,
    val status: GameStatus,
    val animating: MoveTransition?,
    val confirmRestart: Boolean,
) {
    companion object {
        /**
         * The deterministic English-33 starting state (D-01): a full board with
         * the center hole empty, nothing selected, no highlights, `Playing`, no
         * animation, and no restart prompt. [board] defaults to
         * [Boards.english33Initial] but is overridable so the ViewModel can reset
         * to its injected initial board.
         */
        fun initial(board: Board = Boards.english33Initial): GameUiState =
            GameUiState(
                board = board,
                selected = null,
                legalTargets = emptyList(),
                status = GameStatus.Playing,
                animating = null,
                confirmRestart = false,
            )
    }
}
