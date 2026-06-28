package com.app.pegsolitaire.ui.game

import androidx.lifecycle.ViewModel
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * The single authority that turns untrusted taps into validated domain moves and
 * drives the playable board over an immutable [StateFlow]<[GameUiState]>.
 *
 * All gameplay logic lives here; the Canvas (later plans) is a pure render that
 * only emits [onCellTap] upward. The [RulesEngine] is the sole move authority —
 * every selection rebuilds its legal targets from the engine and a jump only ever
 * applies a [Move] that is a member of `legalMoves` (security seam T-03-01).
 *
 * Selection/jump/restart/game-over follow the locked decisions D-02..D-06:
 * tap-to-select with legal-destination highlights, always-two-step jump (no
 * auto-jump), deselect by re-tap, mid-game restart confirmation, and game-over as
 * replayable [GameUiState.status] (never a one-shot effect).
 *
 * @param rulesEngine the pure-domain rules authority (injected via `DomainModule`).
 * @param initialBoard the starting board to play and to restart back to (D-01/D-06).
 */
@HiltViewModel
class GameViewModel
    @Inject
    constructor(
        private val rulesEngine: RulesEngine,
        private val initialBoard: Board,
    ) : ViewModel() {
        private val moveHistory: MutableList<Move> = mutableListOf()

        private val _uiState = MutableStateFlow(GameUiState.initial(initialBoard))

        /** Read-only state stream for the UI; the mutable flow never leaks (Pitfall 10). */
        val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

        /**
         * Routes a tap on [position] through the locked tap-behavior table
         * (D-02/D-03/D-04). Order matters: re-tap-to-deselect is checked before a
         * jump, and a jump before selection. Selection only happens when nothing is
         * currently selected — re-tapping the selected peg is the ONLY way to
         * deselect, and tapping a DIFFERENT peg while one is selected is a no-op
         * (selection unchanged). An empty/illegal cell while a peg is selected also
         * falls through to a no-op (never deselects).
         */
        fun onCellTap(position: Position) {
            val state = _uiState.value
            when {
                state.selected == position -> clearSelection()
                state.selected != null && position in state.legalTargets ->
                    performJump(from = state.selected, to = position)
                state.selected == null && state.board.cellAt(position) == Cell.Peg ->
                    select(position)
                else -> Unit
            }
        }

        /** Selects [position] and highlights its legal destinations from the engine. */
        private fun select(position: Position) {
            val board = _uiState.value.board
            val targets =
                rulesEngine
                    .legalMoves(board)
                    .filter { it.from == position }
                    .map { it.to }
            _uiState.update { it.copy(selected = position, legalTargets = targets) }
        }

        /** Clears the current selection and its highlights (D-04 deselect). */
        private fun clearSelection() {
            _uiState.update { it.copy(selected = null, legalTargets = emptyList()) }
        }

        /**
         * Commits the jump [from] → [to] to domain state, THEN sets the animation
         * transition (animate-from-committed-state, RESEARCH Q5). The move is
         * re-derived from [RulesEngine.legalMoves] and only applied if present —
         * the tap-asserted destination is never trusted (T-03-01).
         */
        private fun performJump(
            from: Position,
            to: Position,
        ) {
            val board = _uiState.value.board
            val move =
                rulesEngine
                    .legalMoves(board)
                    .firstOrNull { it.from == from && it.to == to } ?: return
            val nextBoard = rulesEngine.applyMove(board, move)
            moveHistory.add(move)
            val transition =
                MoveTransition(
                    from = move.from,
                    to = move.to,
                    removed = move.over,
                    id = moveHistory.size,
                )
            _uiState.update {
                it.copy(
                    board = nextBoard,
                    selected = null,
                    legalTargets = emptyList(),
                    status = statusFor(nextBoard),
                    animating = transition,
                )
            }
        }

        /** Clears the in-flight animation once the slide completes (state already committed). */
        fun onAnimationFinished() {
            _uiState.update { it.copy(animating = null) }
        }

        /**
         * Restart entry point (D-06). Mid-game (moves made, still playing) this only
         * raises the confirmation flag; at an end-state, or with no moves yet, it
         * resets immediately.
         */
        fun onRestart() {
            val state = _uiState.value
            if (state.status == GameStatus.Playing && moveHistory.isNotEmpty()) {
                _uiState.update { it.copy(confirmRestart = true) }
            } else {
                resetToInitial()
            }
        }

        /** Confirms a mid-game restart: resets to the initial board (D-06). */
        fun onConfirmRestart() {
            resetToInitial()
        }

        /** Dismisses the restart confirmation, keeping the current game (D-06). */
        fun onDismissRestart() {
            _uiState.update { it.copy(confirmRestart = false) }
        }

        private fun resetToInitial() {
            moveHistory.clear()
            _uiState.value = GameUiState.initial(initialBoard)
        }

        /**
         * The lifecycle status of [board]: [GameStatus.Playing] while a legal move
         * remains, else [GameStatus.Won] when a single peg is left, otherwise
         * [GameStatus.Stuck] (GAME-02).
         */
        private fun statusFor(board: Board): GameStatus =
            when {
                !rulesEngine.isGameOver(board) -> GameStatus.Playing
                board.pegCount == SOLVED_PEG_COUNT -> GameStatus.Won
                else -> GameStatus.Stuck
            }

        private companion object {
            /** A board solved to a single remaining peg is a Won game (D-05 "Perfect"). */
            const val SOLVED_PEG_COUNT = 1
        }
    }
