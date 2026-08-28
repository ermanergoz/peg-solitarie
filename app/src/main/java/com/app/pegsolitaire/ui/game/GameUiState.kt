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
 * @property hapticsOn whether haptic feedback is enabled (SET-02, player-toggled in
 *   Settings and persisted via `SettingsRepository`); fail-open default `true`. Purely a
 *   feedback gate — the screen fires a light tick as each jump animation begins only
 *   while this is on; selection/jump logic is identical either way.
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
 *   inline "no winning move — undo or restart" banner (04-04) renders on this. Cleared on
 *   any jump/undo and when a new hint is requested.
 * @property stuck whether the board has reached a dead-end with 2+ pegs (D-06): a Stuck
 *   position no longer auto-navigates to Results — it becomes a rescuable in-board state
 *   showing the "no moves left — undo or restart" banner. Set the moment a jump strands the
 *   board; cleared when an undo returns it to a Playing position (or on restart). A Won game
 *   never sets this (Won still proceeds to Results).
 * @property selectionTick a monotonic cue ordinal, incremented on every peg selection AND on
 *   every re-tap deselection (D-05a — a deselect is a successful action that reuses the
 *   selection cue, never the invalid-tap one). It exists because selecting and deselecting
 *   otherwise leave no *distinguishable* state trace for a one-shot consumer: `selected` goes
 *   back to `null`, which is where it started. Same idiom as [MoveTransition.id] — a consumer
 *   keys a one-shot effect on it. A consumer MUST also guard on `> 0`, because a fresh state
 *   starts at 0 and would otherwise fire on first composition.
 * @property undoTick a monotonic cue ordinal, incremented once per undo that actually shortened
 *   the move history — never on a declined grant and never on an empty history. An undo's board
 *   update is otherwise indistinguishable from any other, so this is the only trace of it.
 *   Guard on `> 0` at the consumer, as with [selectionTick].
 * @property rejectedTapTick a monotonic cue ordinal, incremented on every tap that genuinely
 *   changed nothing — an empty/blocked cell with no selection, or an illegal destination while a
 *   peg is selected (SOUND-02's quiet invalid tap). That branch is a pure no-op on the board, so
 *   without this ordinal the event leaves no observable trace at all. Guard on `> 0` at the
 *   consumer, as with [selectionTick].
 * @property showRescueBanner derived: whether the inline rescue banner should render over the
 *   still-interactive board (ASST-03, D-08). `true` when the board is [stuck] or the last
 *   hint found [noWinningMove]. Not a constructor field, so it never affects equality — it is
 *   a pure view of the two rescue flags for the UI to switch on.
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
    val hapticsOn: Boolean = true,
    val isLoading: Boolean = false,
    val canUndo: Boolean = false,
    val hintSource: Position? = null,
    val hintTarget: Position? = null,
    val solverThinking: Boolean = false,
    val noWinningMove: Boolean = false,
    val stuck: Boolean = false,
    val selectionTick: Int = 0,
    val undoTick: Int = 0,
    val rejectedTapTick: Int = 0,
) {
    /** Whether the inline no-win/stuck rescue banner should surface over the live board (D-08). */
    val showRescueBanner: Boolean get() = stuck || noWinningMove

    companion object {
        /**
         * The deterministic starting state: a full board with its start hole empty,
         * nothing selected, no highlights, `Playing`, no animation, no restart
         * prompt, the timer at 0, and hints + haptics on. [board] defaults to
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
                hapticsOn = true,
                isLoading = false,
                canUndo = false,
                hintSource = null,
                hintTarget = null,
                solverThinking = false,
                noWinningMove = false,
                stuck = false,
                selectionTick = 0,
                undoTick = 0,
                rejectedTapTick = 0,
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
                hapticsOn = true,
                isLoading = true,
                canUndo = false,
                hintSource = null,
                hintTarget = null,
                solverThinking = false,
                noWinningMove = false,
                stuck = false,
                selectionTick = 0,
                undoTick = 0,
                rejectedTapTick = 0,
            )

        /** An empty footprint backing the [loading] placeholder board (no in-play cells). */
        private val EMPTY_SPEC = BoardSpec(inPlay = emptyList(), target = null)
    }
}
