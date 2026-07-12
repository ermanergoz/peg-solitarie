package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.domain.generator.DifficultyTiers
import com.app.pegsolitaire.domain.generator.GeneratorConstants
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.scoring.ScoreParams
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.solver.SolverBudget
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.navigation.Gameplay
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
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
import kotlinx.coroutines.withContext
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
 * @param defaultDispatcher the background dispatcher the O(level) Challenge generation
 *   runs on, off the main thread (CR-01); tests swap in the test dispatcher.
 * @param adGateway the Slice-1 auto-grant gateway hint/undo request grants through (D-11);
 *   the no-op stub always grants, so undo is free — Slice 2 swaps only the binding.
 * @param getHintUseCase the pure-domain hint decision (ASST-01/ASST-03): recorded-line-first
 *   then the budgeted Solver, run off the main thread by [onHintRequested].
 * @param savedStateHandle carries the typed `Gameplay(levelId)` route arg.
 */
@Suppress("TooManyFunctions", "LongParameterList") // UI event hub: each gesture is one small handler
@HiltViewModel
class GameViewModel
    @Inject
    constructor(
        private val rulesEngine: RulesEngine,
        private val levelRepository: LevelRepository,
        private val scoreCalculator: ScoreCalculator,
        private val settingsRepository: SettingsRepository,
        private val timeSource: TimeSource,
        private val defaultDispatcher: CoroutineDispatcher,
        private val adGateway: AdGateway,
        private val getHintUseCase: GetHintUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        /** The selected board's catalog id (read by `GameRoute` to build the Results route). */
        val levelId: String = savedStateHandle.toRoute<Gameplay>().levelId

        /**
         * The Challenge level number when this is a Challenge game, else `null` for
         * Classic (read by `GameRoute` for the context-aware title + Results route).
         * This single route arg — not any `if (challenge)` in board logic — drives both
         * the load path and the title slot (unified `LevelRef`, Open-Q2).
         */
        val challengeLevel: Int? = savedStateHandle.toRoute<Gameplay>().challengeLevel

        /**
         * The Challenge level clamped into a valid, bounded range (T-03-09: an untrusted
         * nav arg must never crash — or hang — the load), or `null` for a Classic game.
         * The upper clamp to [GeneratorConstants.MAX_CHALLENGE_LEVEL] bounds both the
         * O(level) generation cost and the level-scaled score, matching the board the
         * generator actually produces (CR-01).
         */
        private val clampedChallengeLevel: Int? =
            challengeLevel?.coerceIn(MIN_CHALLENGE_LEVEL, GeneratorConstants.MAX_CHALLENGE_LEVEL)

        /**
         * The Classic board, resolved synchronously (a cheap catalog lookup) so its load
         * stays byte-identical to Phase 2 and the board is present at the first frame;
         * `null` for a Challenge game, whose board is generated off the main thread in
         * [init] (CR-01) — the O(level) reverse-walk must never block construction.
         */
        private val classicBoard: Board? =
            if (clampedChallengeLevel == null) {
                levelRepository.loadClassic(levelId).initialBoard
            } else {
                null
            }

        /**
         * The board the game plays and resets to: set at construction for Classic, set
         * by the async load for Challenge (`null` until the generation completes).
         */
        private var initialBoard: Board? = classicBoard

        /**
         * The D-11 level-scaled score factor: the loaded Challenge level's
         * [DifficultyTiers] multiplier, or [ScoreParams.DEFAULT_LEVEL_MULTIPLIER] (1) for
         * Classic so the numeric score is byte-identical to Phase 2. `DifficultyTiers` is
         * pure `:domain`, so referencing it here keeps `ScoreCalculator` free of level
         * state (no calculator ↔ level coupling).
         */
        private val levelMultiplier: Int =
            clampedChallengeLevel
                ?.let { DifficultyTiers.forLevel(it).scoreMultiplier }
                ?: ScoreParams.DEFAULT_LEVEL_MULTIPLIER

        private val moveHistory: MutableList<Move> = mutableListOf()

        private val _uiState =
            MutableStateFlow(
                classicBoard?.let { GameUiState.initial(it) } ?: GameUiState.loading(),
            )

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

        /**
         * The in-flight hint search, held so a repeat [onHintRequested] cancels the prior
         * job — no overlapping Solver searches (the logic half of D-10, threat T-04-SD).
         */
        private var hintJob: Job? = null

        /**
         * The Challenge recorded winning line (D-03), captured by the async [init] load;
         * `null` for Classic (which has no recorded line and always uses the Solver).
         */
        private var solution: List<Move>? = null

        /**
         * Whether the player is still on the recorded [solution] line (D-03). Starts `true`,
         * flips to `false` the moment a played move deviates from the recorded move at that
         * ply, and is recomputed by [onUndo] (undo can walk back onto the line). Only while
         * `true` is [solution] handed to [getHintUseCase] — so the recorded branch is never
         * blind-indexed after a deviation (Pitfall 5).
         */
        private var onRecordedLine = true

        init {
            // The persisted hint setting is the single source of truth for showHints:
            // collect it into state (fail-open default true until the first emission).
            viewModelScope.launch {
                settingsRepository.hintsEnabled.collect { enabled ->
                    _uiState.update { it.copy(showHints = enabled) }
                }
            }
            // Challenge generation is O(level); load it OFF the main thread and swap the
            // loading state for the playable board when ready (CR-01, T-03-09). Classic
            // is already resolved synchronously above, so this runs for Challenge only —
            // the load path branches here, never the board/selection/jump logic.
            val challengeLevelToLoad = clampedChallengeLevel
            if (challengeLevelToLoad != null) {
                viewModelScope.launch {
                    val level =
                        withContext(defaultDispatcher) {
                            levelRepository.loadChallenge(challengeLevelToLoad)
                        }
                    initialBoard = level.initialBoard
                    // Capture the recorded winning line so the hint can serve it instantly
                    // while the player stays on it (D-03); Classic leaves solution null.
                    solution = level.solution
                    _uiState.update {
                        GameUiState.initial(level.initialBoard).copy(showHints = it.showHints)
                    }
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
                // Re-tapping the selected peg is the only deselect (D-04); clear inline.
                state.selected == position ->
                    _uiState.update { it.copy(selected = null, legalTargets = emptyList()) }
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
            // Deviation is permanent (D-03): once a played move differs from the recorded
            // move at this ply, drop off the line so the hint uses the Solver, not a stale
            // recorded index. Never flips back except via undo (which recomputes it).
            if (onRecordedLine && move != solution?.getOrNull(moveHistory.size - 1)) {
                onRecordedLine = false
            }
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
                    canUndo = moveHistory.isNotEmpty(),
                    // A new move invalidates any shown hint / no-win state.
                    hintSource = null,
                    hintTarget = null,
                    noWinningMove = false,
                    // A dead-end board becomes a rescuable in-board state (D-06): flag it for
                    // the banner instead of auto-navigating. Won never sets this (it routes
                    // to Results); any non-terminal board clears it.
                    stuck = status == GameStatus.Stuck,
                )
            }
            // A terminal status FREEZES the timer for BOTH Won and Stuck (D-04). Only Won
            // emits the one-shot finish (Pitfall 4) → Results; Stuck is now rescuable via
            // Undo (D-06) and deliberately emits nothing / never sets finishedEmitted.
            if (status != GameStatus.Playing) {
                tickerJob?.cancel()
                tickerJob = null
                if (status == GameStatus.Won && !finishedEmitted) {
                    finishedEmitted = true
                    val pegsRemaining = nextBoard.pegCount
                    // A jump can only fire once the board is loaded, so initialBoard is
                    // non-null here; fall back to the current count defensively.
                    val startingPegCount = initialBoard?.pegCount ?: pegsRemaining
                    _finished.trySend(
                        scoreCalculator.evaluate(
                            pegsRemaining = pegsRemaining,
                            pegsCleared = startingPegCount - pegsRemaining,
                            elapsedSeconds = _uiState.value.elapsedSeconds,
                            levelMultiplier = levelMultiplier,
                        ),
                    )
                }
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
            tickerJob = launchTicker(mark)
        }

        /**
         * Resumes the per-second ticker after a rescue-undo returns a frozen (Stuck) board to
         * [GameStatus.Playing] (D-06/D-07). The existing [startMark] is reused — never reset —
         * so the displayed elapsed time reflects the real wall-clock since the first move; the
         * ticker only relaunches when it is not already running (a mid-game undo is a no-op).
         */
        private fun resumeTickerIfNeeded() {
            val mark = startMark ?: return
            if (tickerJob != null) return
            tickerJob = launchTicker(mark)
        }

        /**
         * The shared per-second ticker loop: writes whole elapsed seconds (measured from
         * [mark]) into state once per second while the game is [GameStatus.Playing], stopping
         * at a terminal status so the displayed time freezes. Used by both the first-move
         * start ([startTimerIfNeeded]) and the rescue-undo resume ([resumeTickerIfNeeded]).
         */
        private fun launchTicker(mark: TimeMark): Job =
            viewModelScope.launch {
                while (isActive && _uiState.value.status == GameStatus.Playing) {
                    _uiState.update {
                        it.copy(elapsedSeconds = mark.elapsedNow().inWholeSeconds.toInt())
                    }
                    delay(TIMER_TICK_MILLIS)
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

        /**
         * Undo the last move (ASST-02, D-05): requests the [AdReason.UNDO] grant (Slice-1
         * stub auto-grants), then rebuilds the board by folding [RulesEngine.applyMove] over
         * the remaining [moveHistory] from [initialBoard] — never trusting any external or
         * serialized state (T-04-01-U). Repeated calls walk all the way back to the initial
         * board; an empty history is a no-op. The board's [statusFor] status is recomputed
         * and [finishedEmitted] is cleared when the undone board is Playing again, so a
         * previously Stuck game becomes playable and can re-emit a later terminal result.
         *
         * The timer is deliberately untouched (D-07): [startMark], [tickerJob], and
         * [GameUiState.elapsedSeconds] all keep counting real time across an undo.
         */
        fun onUndo() {
            viewModelScope.launch {
                if (!adGateway.requestRewarded(AdReason.UNDO)) return@launch
                if (moveHistory.isEmpty()) return@launch
                moveHistory.removeAt(moveHistory.lastIndex)
                val base = initialBoard ?: return@launch
                val board = moveHistory.fold(base) { b, m -> rulesEngine.applyMove(b, m) }
                val status = statusFor(board)
                if (status == GameStatus.Playing) finishedEmitted = false
                // Undo can walk back onto the recorded line (D-03): recompute by comparing
                // the remaining moveHistory prefix against the recorded solution. Classic
                // (null solution) has no line, so it stays off. Never blind-indexed.
                onRecordedLine =
                    solution?.let { line -> moveHistory.indices.all { i -> moveHistory[i] == line.getOrNull(i) } }
                        ?: false
                // D-07: startMark / elapsedSeconds are intentionally NOT reset here — undo
                // rescues the board but the real elapsed time keeps running; the ticker
                // itself is resumed below when the board returns to Playing.
                _uiState.update {
                    it.copy(
                        board = board,
                        selected = null,
                        legalTargets = emptyList(),
                        animating = null,
                        status = status,
                        canUndo = moveHistory.isNotEmpty(),
                        // Undo invalidates any shown hint and clears the rescue banner.
                        hintSource = null,
                        hintTarget = null,
                        noWinningMove = false,
                        stuck = false,
                    )
                }
                // Undoing out of a frozen (Stuck) board back to Playing resumes the timer that
                // the terminal freeze stopped, reusing the never-reset startMark (D-06/D-07).
                if (status == GameStatus.Playing) resumeTickerIfNeeded()
            }
        }

        /**
         * Reveal the next winning move (ASST-01/ASST-03, D-01/D-10). Cancels any prior
         * search (no overlapping Solver work, threat T-04-SD), raises [GameUiState.solverThinking]
         * immediately, requests the [AdReason.HINT] grant (Slice-1 stub auto-grants), then
         * resolves the move OFF the main thread via [getHintUseCase] on [defaultDispatcher]:
         *  - on the recorded line (Challenge) the use case returns the recorded move instantly,
         *    with no Solver call;
         *  - otherwise (Classic, or after a deviation) it runs the budgeted Solver, cancelled
         *    cooperatively once the wall-clock [SolverBudget.deadline] passes or the job is
         *    cancelled — the deadline crosses into pure `:domain` only as a plain lambda.
         *
         * A found move sets [GameUiState.hintSource]/[GameUiState.hintTarget]; a `null`
         * result sets [GameUiState.noWinningMove] (D-04; its banner ships in 04-04). Either
         * way [GameUiState.solverThinking] clears.
         */
        fun onHintRequested() {
            hintJob?.cancel()
            _uiState.update {
                it.copy(
                    solverThinking = true,
                    noWinningMove = false,
                    hintSource = null,
                    hintTarget = null,
                )
            }
            hintJob =
                viewModelScope.launch {
                    if (!adGateway.requestRewarded(AdReason.HINT)) {
                        _uiState.update { it.copy(solverThinking = false) }
                        return@launch
                    }
                    val deadline = timeSource.markNow() + SolverBudget.DEFAULT.deadline
                    val move =
                        withContext(defaultDispatcher) {
                            getHintUseCase(
                                board = _uiState.value.board,
                                solution = solution.takeIf { onRecordedLine },
                                movesPlayed = moveHistory.size,
                                shouldCancel = { !isActive || deadline.hasPassedNow() },
                            )
                        }
                    _uiState.update {
                        if (move != null) {
                            it.copy(hintSource = move.from, hintTarget = move.to, solverThinking = false)
                        } else {
                            it.copy(noWinningMove = true, solverThinking = false)
                        }
                    }
                }
        }

        /**
         * Whether the player is still on the Challenge recorded line (drives which hint path
         * runs). Exposed `internal` purely so the ViewModel test can assert the D-03 deviation
         * tracking; production code never reads it from outside the ViewModel.
         */
        internal fun isOnRecordedLine(): Boolean = onRecordedLine

        private fun resetToInitial() {
            // No-op while the Challenge board is still loading (no board to reset to).
            val board = initialBoard ?: return
            moveHistory.clear()
            tickerJob?.cancel()
            tickerJob = null
            hintJob?.cancel()
            hintJob = null
            startMark = null
            finishedEmitted = false
            // Back to the start ⇒ back on the recorded line (D-03) with no hint shown; the
            // fresh GameUiState.initial(board) below already carries cleared hint fields.
            onRecordedLine = true
            // Preserve the player's hint choice across a restart; the collected
            // settings flow only re-emits on change, so carry it forward explicitly.
            _uiState.value = GameUiState.initial(board).copy(showHints = _uiState.value.showHints)
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

            /** The lowest valid Challenge level; a `< 1` nav arg clamps up to it (T-03-09). */
            const val MIN_CHALLENGE_LEVEL = 1
        }
    }
