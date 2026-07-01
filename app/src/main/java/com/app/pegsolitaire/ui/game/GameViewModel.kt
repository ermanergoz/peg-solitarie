package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.navigation.Gameplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The single authority that turns untrusted taps into validated domain moves and
 * drives the playable board over an immutable [StateFlow]<[GameUiState]>.
 *
 * The board it plays is selected by the user: the `Gameplay` route's `levelId`
 * (read via [SavedStateHandle.toRoute]) is resolved through [LevelRepository] —
 * a forged/unknown id fails fast in the repository, never silently loading the
 * wrong board (T-02-05-ID); there is no hardcoded English-33 inject (CLAS-02).
 *
 * All gameplay logic (selection/jump/restart, the [RulesEngine] legal-move
 * authority T-03-01, D-02..D-06) is unchanged from Phase 1. This plan adds two
 * things: a visible elapsed [GameUiState.elapsedSeconds] timer that starts on the
 * first move, ticks once per second against the injected [TimeSource], and stops
 * at a terminal status (D-04); and a one-shot [finished] event carrying the
 * [ScoreResult] (stars + score) emitted exactly once on Won/Stuck (SCOR-02) — the
 * Phase-1 game-over overlay is replaced by a navigation to Results (D-05).
 *
 * A player-toggleable, persisted hint gate ([GameUiState.showHints]) is collected from
 * the [SettingsRepository] (fail-open default ON) and flipped by [onToggleHints]; it is
 * render-only — the destination dots are hidden when off, but selection/jump logic is
 * unchanged.
 *
 * @param rulesEngine the pure-domain rules authority (injected via `DomainModule`).
 * @param levelRepository resolves the selected board from the route's `levelId`.
 * @param scoreCalculator the pure-domain scoring authority (stars + score).
 * @param settingsRepository the persisted hint on/off setting (fail-open default ON).
 * @param timeSource the monotonic clock the timer marks against (test-swappable).
 * @param savedStateHandle carries the typed `Gameplay(levelId)` route arg.
 */
@HiltViewModel
class GameViewModel
    @Inject
    constructor(
        private val rulesEngine: RulesEngine,
        levelRepository: LevelRepository,
        private val scoreCalculator: ScoreCalculator,
        private val settingsRepository: SettingsRepository,
        private val timeSource: TimeSource,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        /** The selected board's catalog id (read by `GameRoute` to build the Results route). */
        val levelId: String = savedStateHandle.toRoute<Gameplay>().levelId

        private val initialBoard: Board = levelRepository.loadClassic(levelId).initialBoard

        private val moveHistory: MutableList<Move> = mutableListOf()

        private val _uiState = MutableStateFlow(GameUiState.initial(initialBoard))

        /** Read-only state stream for the UI; the mutable flow never leaks (Pitfall 10). */
        val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

        private val _finished = Channel<ScoreResult>(Channel.CONFLATED)

        /**
         * Fires exactly once when the game reaches Won/Stuck, carrying the computed
         * [ScoreResult]. Consumed once by `GameRoute` inside a `LaunchedEffect` to
         * navigate to Results (one-shot — a recomposition can never re-fire it,
         * Pitfall 4).
         */
        val finished: Flow<ScoreResult> = _finished.receiveAsFlow()

        /** The instant the first move was applied; `null` until then (timer not started). */
        private var startMark: TimeMark? = null

        /** The running per-second ticker, held so a restart/terminal can cancel it. */
        private var tickerJob: Job? = null

        /** Guards the one-shot [finished] emission so it never re-fires (Pitfall 4). */
        private var finishedEmitted = false

        init {
            // The persisted hint setting is the single source of truth for showHints:
            // collect it into state (fail-open default true until the first emission).
            viewModelScope.launch {
                settingsRepository.hintsEnabled.collect { enabled ->
                    _uiState.update { it.copy(showHints = enabled) }
                }
            }
        }

        /**
         * Toggles the persisted hint setting (D — player control). Writes the flipped
         * value through [SettingsRepository]; the collected [SettingsRepository.hintsEnabled]
         * flow is the authority that drives [GameUiState.showHints], so state updates when
         * the write lands (single source of truth — no separate optimistic local flip).
         */
        fun onToggleHints() {
            val current = _uiState.value.showHints
            viewModelScope.launch { settingsRepository.setHintsEnabled(!current) }
        }

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
         * the tap-asserted destination is never trusted (T-03-01). The first applied
         * move starts the timer; a terminal result emits the one-shot [finished].
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
            startTimerIfNeeded()
            val transition =
                MoveTransition(
                    from = move.from,
                    to = move.to,
                    removed = move.over,
                    id = moveHistory.size,
                )
            val status = statusFor(nextBoard)
            _uiState.update {
                it.copy(
                    board = nextBoard,
                    selected = null,
                    legalTargets = emptyList(),
                    status = status,
                    animating = transition,
                )
            }
            // One-shot finish (Pitfall 4): on the FIRST transition to a terminal
            // status, emit the ScoreResult exactly once for GameRoute to consume.
            if (!finishedEmitted && status != GameStatus.Playing) {
                finishedEmitted = true
                tickerJob?.cancel()
                tickerJob = null
                val pegsRemaining = nextBoard.pegCount
                _finished.trySend(
                    scoreCalculator.evaluate(
                        pegsRemaining = pegsRemaining,
                        pegsCleared = initialBoard.pegCount - pegsRemaining,
                        elapsedSeconds = _uiState.value.elapsedSeconds,
                    ),
                )
            }
        }

        /**
         * Starts the visible elapsed timer on the first move (D-04): marks the
         * monotonic [timeSource] and launches a ticker that writes whole elapsed
         * seconds into state once per second while the game is [GameStatus.Playing],
         * stopping at a terminal status so the displayed time freezes.
         */
        private fun startTimerIfNeeded() {
            if (startMark != null) return
            val mark = timeSource.markNow()
            startMark = mark
            tickerJob =
                viewModelScope.launch {
                    while (isActive && _uiState.value.status == GameStatus.Playing) {
                        _uiState.update {
                            it.copy(elapsedSeconds = mark.elapsedNow().inWholeSeconds.toInt())
                        }
                        delay(TIMER_TICK_MILLIS)
                    }
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
            tickerJob?.cancel()
            tickerJob = null
            startMark = null
            finishedEmitted = false
            // Preserve the player's hint choice across a restart; the collected
            // settings flow only re-emits on change, so carry it forward explicitly.
            _uiState.value = GameUiState.initial(initialBoard).copy(showHints = _uiState.value.showHints)
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

            /** The visible timer ticks once per second (D-04). */
            const val TIMER_TICK_MILLIS = 1_000L
        }
    }
