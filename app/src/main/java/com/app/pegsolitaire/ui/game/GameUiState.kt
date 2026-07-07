package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
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
 * @property status the lifecycle status driving the finish hand-off to Results (D-05).
 * @property animating the in-flight jump animation, or `null` when settled (D-07/D-08).
 * @property confirmRestart whether the mid-game restart confirmation is showing (D-06).
 * @property elapsedSeconds whole seconds shown on the in-game timer (D-04); 0 until
 *   the first move, then ticked once per second by the ViewModel until terminal.
 * @property showHints whether legal-destination hint dots are drawn (player-toggled,
 *   persisted via `SettingsRepository`); fail-open default `true`. Purely a render
 *   gate — selection/jump logic is unchanged whether hints are on or off.
 * @property isLoading whether the board is still being generated off the main thread
 *   (Challenge only, CR-01): while `true` the screen shows a loading indicator instead
 *   of the board. Classic boards resolve synchronously, so they are never in this state.
 * @property canUndo whether there is at least one move to undo (ASST-02, D-05): mirrors
 *   `moveHistory.isNotEmpty()`, so it is `false` at the initial board, `true` after any
 *   jump, and `false` again once undo walks back to the start. Drives the Undo control's
 *   enabled state; render-only, no game logic depends on it.
 * @property hintSource the peg to move for the revealed winning move (ASST-01, D-01), or
 *   `null` when no hint is shown; the [BoardCanvas] glows this cell (rendering ships in
 *   04-03b). Cleared on any jump/undo (a new position invalidates a shown hint).
 * @property hintTarget the destination hole of the revealed winning move (ASST-01, D-01),
 *   or `null` when no hint is shown; paired with [hintSource] to draw the A→B highlight.
 * @property solverThinking whether the off-thread Solver is computing a hint (D-10): set
 *   the instant a hint is requested and cleared when the search resolves (to a highlight
 *   or [noWinningMove]); the Hint control shows a spinner/disabled state while `true`.
 * @property noWinningMove whether the last hint found no winning move (ASST-03, D-04/D-08):
 *   the Solver returned nothing (unsolvable / budget-missed) so the position is dead; the
 *   inline "no winning move — undo or restart" banner (deferred to 04-04) renders on this.
 *   Cleared on any jump/undo and when a new hint is requested.
 */
@Immutable
data class GameUiState(
    val board: Board,
    val selected: Position?,
    val legalTargets: List<Position>,
    val status: GameStatus,
    val animating: MoveTransition?,
    val confirmRestart: Boolean,
    val elapsedSeconds: Int = 0,
    val showHints: Boolean = true,
    val isLoading: Boolean = false,
    val canUndo: Boolean = false,
    val hintSource: Position? = null,
    val hintTarget: Position? = null,
    val solverThinking: Boolean = false,
    val noWinningMove: Boolean = false,
) {
    companion object {
        /**
         * The deterministic starting state: a full board with its start hole empty,
         * nothing selected, no highlights, `Playing`, no animation, no restart
         * prompt, the timer at 0, and hints on. [board] defaults to
         * [Boards.english33Initial] but is overridable so the ViewModel can reset to
         * its selected board.
         */
        fun initial(board: Board = Boards.english33Initial): GameUiState =
            GameUiState(
                board = board,
                selected = null,
                legalTargets = emptyList(),
                status = GameStatus.Playing,
                animating = null,
                confirmRestart = false,
                elapsedSeconds = 0,
                showHints = true,
                isLoading = false,
                canUndo = false,
                hintSource = null,
                hintTarget = null,
                solverThinking = false,
                noWinningMove = false,
            )

        /**
         * The transient loading state shown while a Challenge board is generated off
         * the main thread (CR-01). It carries an empty placeholder board (the screen
         * renders a progress indicator, not the board, while [isLoading]) so the state
         * stays a total, non-null value — no board logic runs against it.
         */
        fun loading(): GameUiState =
            GameUiState(
                board = Board(spec = EMPTY_SPEC, pegs = 0L),
                selected = null,
                legalTargets = emptyList(),
                status = GameStatus.Playing,
                animating = null,
                confirmRestart = false,
                elapsedSeconds = 0,
                showHints = true,
                isLoading = true,
                canUndo = false,
                hintSource = null,
                hintTarget = null,
                solverThinking = false,
                noWinningMove = false,
            )

        /** An empty footprint backing the [loading] placeholder board (no in-play cells). */
        private val EMPTY_SPEC = BoardSpec(inPlay = emptyList(), target = null)
    }
}
