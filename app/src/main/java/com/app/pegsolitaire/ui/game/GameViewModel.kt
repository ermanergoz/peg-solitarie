package com.app.pegsolitaire.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.data.resume.ChallengeStamp
import com.app.pegsolitaire.data.resume.RestoredGame
import com.app.pegsolitaire.data.resume.SnapshotMapper
import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
import com.app.pegsolitaire.domain.generator.DifficultyTiers
import com.app.pegsolitaire.domain.generator.GeneratorConstants
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
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
import com.app.pegsolitaire.platform.ads.RewardOutcome
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
 * @param challengeProgress the persisted Challenge ladder position; solving a Challenge
 *   level advances it to N+1 (CHAL-05/PERS-02) — a Classic solve never touches it.
 * @param progressRepository the per-mode in-progress snapshot store (PERS-03): written by
 *   [onLeaveScreen] while the game is still Playing (D-04), read back by the `resume`
 *   route flag, and cleared on solve/restart (D-05).
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
        private val challengeProgress: ChallengeProgress,
        private val progressRepository: ProgressRepository,
        private val timeSource: TimeSource,
        private val defaultDispatcher: CoroutineDispatcher,
        private val adGateway: AdGateway,
        private val getHintUseCase: GetHintUseCase,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        /** The selected board's catalog id (read by `GameRoute` to build the Results route). */
        val levelId: String = savedStateHandle.toRoute<Gameplay>().levelId

        /** The raw `Gameplay` route Challenge arg, or `null` for a Classic game. */
        private val routeChallengeLevel: Int? = savedStateHandle.toRoute<Gameplay>().challengeLevel

        /**
         * The Challenge level number when this is a Challenge game, else `null` for
         * Classic (read by `GameRoute` for the context-aware title + Results route).
         * This single value — not any `if (challenge)` in board logic — drives both
         * the load path and the title slot (unified `LevelRef`, Open-Q2).
         *
         * Sourced from the route arg until a restore lands; after a successful restore
         * the snapshot's own [ChallengeStamp] wins (WR-03) — at a cold start the Home
         * Continue tile can navigate with a stale ladder value before the first DataStore
         * emission, and the board actually restored is the stamped level, so the title,
         * the score multiplier and the Results keying must all follow the stamp.
         *
         * **WARN-03: this MUST be the clamped value.** It is the one Challenge level that
         * escapes this ViewModel — `GameRoute` carries it into the `Results` route, from
         * where it becomes the database level key and the argument to the "Next level"
         * action (`onNextLevel(challengeLevel + 1)`). It previously returned the RAW route
         * arg on the non-restore path while the board actually played was the clamped one,
         * so a forged arg would have recorded a best under a level that was never played
         * and offered a "next" level past the frozen ceiling. Delegating to
         * [activeChallengeLevel] — already clamped on both the route and the restore path —
         * is the whole fix, and it restores that value's documented invariant that
         * *everything* level-keyed reads THIS. Do not reintroduce a separate clamp here:
         * a second expression is a second thing that can drift.
         */
        val challengeLevel: Int? get() = activeChallengeLevel

        /**
         * The Challenge level clamped into a valid, bounded range (T-03-09: an untrusted
         * nav arg must never crash — or hang — the load), or `null` for a Classic game.
         * The upper clamp to [ChallengeLevelBounds.MAX] bounds both the O(level)
         * generation cost and the level-scaled score, matching the board the generator
         * actually produces (CR-01). The range itself lives once in `:domain` (D-15).
         */
        private val clampedChallengeLevel: Int? = routeChallengeLevel?.let(ChallengeLevelBounds::clamp)

        /**
         * The Challenge level restored from the snapshot's provenance stamp, clamped into
         * the same valid range as the route arg; `null` until a restore lands and always
         * `null` for Classic. Once set it is authoritative (WR-03): the stamp travels with
         * the board it describes, while the route arg is only what Home believed at tap
         * time.
         */
        private var stampedChallengeLevel: Int? = null

        /**
         * The level this Challenge game is actually being played as — the restored stamp
         * when there is one, else the clamped route arg; `null` for Classic. Everything
         * level-keyed after construction reads THIS: the score multiplier, the ladder
         * advance on solve, and the level stamped into a re-saved snapshot.
         */
        private val activeChallengeLevel: Int? get() = stampedChallengeLevel ?: clampedChallengeLevel

        /**
         * Whether this entry is the Home "Continue" affordance (PERS-03, D-01) rather than a
         * fresh start: when `true` [init] rebuilds the game from the mode's stored snapshot
         * by replay (D-06) and only falls back to the normal load when there is nothing
         * usable to resume.
         */
        private val resume: Boolean = savedStateHandle.toRoute<Gameplay>().resume

        /**
         * Which mode's single snapshot row this game owns (D-03) — the persistence key for
         * every save/load/clear below. Derived from the route's Challenge arg alone, so it
         * agrees with the mode `ResultsViewModel` records results under.
         */
        private val mode: String =
            if (clampedChallengeLevel != null) {
                ProgressPersistenceKeys.MODE_CHALLENGE
            } else {
                ProgressPersistenceKeys.MODE_CLASSIC
            }

        /**
         * The Classic board, resolved synchronously (a cheap catalog lookup) so its load
         * stays byte-identical to Phase 2 and the board is present at the first frame;
         * `null` for a Challenge game, whose board is generated off the main thread in
         * [init] (CR-01) — the O(level) reverse-walk must never block construction.
         *
         * Also `null` on the [resume] path: a Continue entry starts in the loading state
         * and only leaves it when the restore resolves, so an `ON_STOP` during the
         * in-flight restore can never snapshot an untouched fresh board over the very
         * game being restored, and early taps can never play moves the restore would
         * silently discard (WR-01).
         */
        private val classicBoard: Board? =
            if (clampedChallengeLevel == null && !resume) {
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
         * The D-11 level-scaled score factor: the played Challenge level's
         * [DifficultyTiers] multiplier, or [ScoreParams.DEFAULT_LEVEL_MULTIPLIER] (1) for
         * Classic so the numeric score is byte-identical to Phase 2. `DifficultyTiers` is
         * pure `:domain`, so referencing it here keeps `ScoreCalculator` free of level
         * state (no calculator ↔ level coupling). Derived from [activeChallengeLevel] so
         * a restored game is scored at its stamped level, not the route's (WR-03).
         */
        private val levelMultiplier: Int
            get() =
                activeChallengeLevel
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

        /**
         * Seconds already spent on this game before the current [startMark] — non-zero only
         * after a resume, where it carries the snapshot's elapsed time (PERS-03). The ticker
         * adds it to what it measures, so a restored game keeps counting from the time the
         * player left rather than restarting at zero. Cleared by [resetToInitial].
         */
        private var elapsedBaseSeconds: Int = 0

        /** The running per-second ticker, held so a restart/terminal can cancel it. */
        private var tickerJob: Job? = null

        /**
         * Whether the elapsed clock is currently stopped for a rewarded ad presentation
         * (ADSF-01). Set by [freezeClockForAd], cleared by [unfreezeClockAfterAd], and read
         * by [resumeTickerIfNeeded], which MUST refuse to run while it is set — that resume
         * reuses the pre-ad [startMark], which is exactly how the ad's own duration gets
         * silently charged back to the player.
         */
        private var frozenForAd = false

        /** Guards the one-shot [finished] emission so it never re-fires (Pitfall 4). */
        private var finishedEmitted = false

        /**
         * The in-flight hint search, held so a repeat [onHintRequested] cancels the prior
         * job — no overlapping Solver searches (the logic half of D-10, threat T-04-SD).
         */
        private var hintJob: Job? = null

        /**
         * Cancels the in-flight hint search, if any. THE single cancellation idiom — every
         * site that would otherwise leave a search running against a board the player has
         * left calls this, so there are not two idioms that can drift apart.
         *
         * **Every site that mutates the board MUST call it (CR-01).** A hint search can
         * legitimately run the full [SolverBudget.deadline], and the board stays tappable
         * throughout — `GameScreen` disables only the Hint control — so a jump or an undo
         * made mid-search would otherwise land its result on a board it was never computed
         * for: a glow over a cell the player has already vacated, or a "no winning move"
         * banner raised on a position that was never searched.
         *
         * **The caller owes the flag.** Cancelling the job also kills the
         * `solverThinking = false` write inside it, and `HintControl` renders
         * `enabled = false` while that flag is true — so the cancelling site MUST clear
         * [GameUiState.solverThinking] in the same state update. An uncleared flag disables
         * Hint for the rest of the game, which is a worse regression than the race.
         */
        private fun cancelHintSearch() {
            hintJob?.cancel()
            hintJob = null
        }

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
            // Same contract for the persisted haptics flag (SET-02, D-15): the state
            // carries it, the screen fires the tick. Fail-open ON until the first emission.
            viewModelScope.launch {
                settingsRepository.hapticsOn.collect { on ->
                    _uiState.update { it.copy(hapticsOn = on) }
                }
            }
            // Continue (D-01) rebuilds the left-off game from disk; anything else starts the
            // board fresh. The restore is tried FIRST and the normal load only runs when
            // there is nothing usable to resume, so a successful Continue never regenerates
            // (D-06) and a corrupt snapshot still lands the player on a playable board.
            if (resume) {
                viewModelScope.launch {
                    val dto = progressRepository.loadSnapshot(mode)
                    val restored = dto?.let { SnapshotMapper.fromDto(it, rulesEngine) }
                    if (restored != null) {
                        applyRestored(restored)
                    } else {
                        // A blob that decoded but did not rebuild (bad structure or a
                        // history that does not replay legally) is unusable — purge the
                        // row (WR-02) so the Continue affordance disappears instead of
                        // forever restarting a fresh board. An undecodable row was
                        // already purged by the repository's own load.
                        if (dto != null) progressRepository.clearSnapshot(mode)
                        loadFreshBoard()
                    }
                }
            } else {
                viewModelScope.launch { loadChallengeBoardIfNeeded() }
            }
        }

        /**
         * Loads the Challenge board for this route (a no-op for Classic, already resolved
         * synchronously at construction).
         *
         * Challenge generation is O(level), so it runs OFF the main thread and swaps the
         * loading state for the playable board when ready (CR-01, T-03-09) — the load path
         * branches here, never the board/selection/jump logic.
         */
        private suspend fun loadChallengeBoardIfNeeded() {
            val challengeLevelToLoad = clampedChallengeLevel ?: return
            val level =
                withContext(defaultDispatcher) {
                    levelRepository.loadChallenge(challengeLevelToLoad)
                }
            initialBoard = level.initialBoard
            // Capture the recorded winning line so the hint can serve it instantly
            // while the player stays on it (D-03); Classic leaves solution null.
            solution = level.solution
            // Installing a board mutates it, so the CR-01 contract applies here too
            // (WR-01): kill any search launched against the loading placeholder. The
            // "caller owes the flag" obligation is met by the rebuild itself —
            // GameUiState.initial() carries solverThinking = false.
            cancelHintSearch()
            _uiState.update {
                GameUiState
                    .initial(level.initialBoard)
                    .copy(
                        showHints = it.showHints,
                        hapticsOn = it.hapticsOn,
                        selectionTick = it.selectionTick,
                        deselectionTick = it.deselectionTick,
                        undoTick = it.undoTick,
                        rejectedTapTick = it.rejectedTapTick,
                    )
            }
        }

        /**
         * The fallback for a Continue with nothing usable to resume (absent or corrupt
         * snapshot, T-05-03): loads the mode's fresh board so the player still lands on a
         * playable game. Classic is deferred to here on the resume path (never installed
         * at construction) so the fresh board is not live while a restore is in flight
         * (WR-01); Challenge takes the same async generation path as a normal entry.
         */
        private suspend fun loadFreshBoard() {
            if (clampedChallengeLevel != null) {
                loadChallengeBoardIfNeeded()
            } else {
                installClassicBoard()
            }
        }

        /**
         * Resolves and installs the Classic board on the resume-fallback path, replacing
         * the loading state exactly as the Challenge load does.
         *
         * The resume route's `levelId` originates in a stored snapshot blob — untrusted
         * state (Security §V5, T-05-03) — so unlike the fail-fast fresh-entry load, an
         * unknown id here degrades to the default board instead of crashing (CR-01): the
         * player still lands on something playable, and the unusable snapshot has already
         * been dropped by the restore fallback.
         */
        private fun installClassicBoard() {
            val board =
                try {
                    levelRepository.loadClassic(levelId).initialBoard
                } catch (ignored: IllegalArgumentException) {
                    Boards.english33Initial
                }
            initialBoard = board
            // Board-install = board mutation, so the CR-01 idiom applies (WR-01); the
            // GameUiState.initial() rebuild below clears solverThinking for us.
            cancelHintSearch()
            _uiState.update {
                GameUiState
                    .initial(board)
                    .copy(
                        showHints = it.showHints,
                        hapticsOn = it.hapticsOn,
                        selectionTick = it.selectionTick,
                        deselectionTick = it.deselectionTick,
                        undoTick = it.undoTick,
                        rejectedTapTick = it.rejectedTapTick,
                    )
            }
        }

        /**
         * Seeds the game from a snapshot rebuilt by replay (PERS-03, D-06): the stored
         * initial board becomes the undo floor, the validated history becomes [moveHistory]
         * (so [onUndo] works on the very first tap), and the elapsed time resumes from where
         * the player left it rather than from zero.
         *
         * The recorded Challenge line is deliberately NOT restored — it is not stored, and
         * inventing one would mean regenerating the level. Hints simply fall back to the
         * Solver for a resumed Challenge game, which is the same path a deviated line takes.
         */
        private fun applyRestored(restored: RestoredGame) {
            // The snapshot's provenance stamp is authoritative for a restored Challenge
            // game (WR-03): the board being installed IS the stamped level, whatever the
            // route claimed. Clamped like the route arg — the stamp is stored state and
            // equally untrusted (T-03-09).
            restored.challenge
                ?.level
                ?.let(ChallengeLevelBounds::clamp)
                ?.let { stampedChallengeLevel = it }
            initialBoard = restored.initialBoard
            moveHistory.clear()
            moveHistory.addAll(restored.moveHistory)
            onRecordedLine = false
            elapsedBaseSeconds = (restored.elapsedMs / MILLIS_IN_SECOND).toInt()
            // Board-install = board mutation, so the CR-01 idiom applies (WR-01); the
            // GameUiState.initial() rebuild below clears solverThinking for us.
            cancelHintSearch()
            _uiState.update {
                GameUiState
                    .initial(restored.currentBoard)
                    .copy(
                        showHints = it.showHints,
                        hapticsOn = it.hapticsOn,
                        status = statusFor(restored.currentBoard),
                        canUndo = moveHistory.isNotEmpty(),
                        elapsedSeconds = elapsedBaseSeconds,
                        selectionTick = it.selectionTick,
                        deselectionTick = it.deselectionTick,
                        undoTick = it.undoTick,
                        rejectedTapTick = it.rejectedTapTick,
                    )
            }
        }

        /**
         * Persists the game as this mode's resume point when the player leaves the screen —
         * backgrounding or navigating away (D-04); `GameRoute` calls it on `ON_STOP`.
         *
         * Only a game still in progress is saved: a Won or Stuck board would resurrect a
         * finished game as a stale "Continue" (Pitfall 4), and a board still loading —
         * a Challenge board mid-generation, or either mode's in-flight restore — has no
         * [initialBoard] yet, so leaving during that window saves nothing rather than
         * overwriting the stored snapshot with an empty-history one (WR-01). What is
         * stored is the initial
         * board plus the full move history — never the current board — because replaying
         * that history is what keeps undo working after the resume (D-06).
         */
        fun onLeaveScreen() {
            val state = _uiState.value
            if (state.status != GameStatus.Playing) return
            val board = initialBoard ?: return
            val dto =
                SnapshotMapper.toDto(
                    mode = mode,
                    levelId = activeChallengeLevel?.toString() ?: levelId,
                    initialBoard = board,
                    moveHistory = moveHistory.toList(),
                    elapsedMs = state.elapsedSeconds * MILLIS_IN_SECOND,
                    challenge =
                        activeChallengeLevel?.let {
                            ChallengeStamp(
                                level = it,
                                generatorVersion = GeneratorConstants.GENERATOR_VERSION,
                            )
                        },
                )
            viewModelScope.launch { progressRepository.saveSnapshot(dto) }
        }

        /** Drops this mode's resume point (D-05): the game it pointed at is over or gone. */
        private fun clearSnapshot() {
            viewModelScope.launch { progressRepository.clearSnapshot(mode) }
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
                // D-05a (amended 2026-08-28): a deselect is a SUCCESSFUL action, and it now
                // has a cue of its own, so it advances the deselection ordinal rather than
                // the selection one. What is unchanged is the half that matters: it must
                // never be counted as a rejected tap. Keeping this branch ABOVE the
                // rejected-tap `else` is what makes the invalid-tap cue structurally
                // unreachable from a deselect rather than merely unused here.
                state.selected == position ->
                    _uiState.update {
                        it.copy(
                            selected = null,
                            legalTargets = emptyList(),
                            deselectionTick = it.deselectionTick + 1,
                        )
                    }
                state.selected != null && position in state.legalTargets ->
                    performJump(from = state.selected, to = position)
                state.selected == null && state.board.cellAt(position) == Cell.Peg ->
                    select(position)
                // The genuine rejection: a tap that changed nothing at all (an empty or
                // out-of-play cell with no selection, an illegal destination with one).
                // It leaves no other trace on the board, so the ordinal is the only way a
                // consumer can observe it (SOUND-02's quiet invalid tap).
                else -> _uiState.update { it.copy(rejectedTapTick = it.rejectedTapTick + 1) }
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
            _uiState.update {
                it.copy(
                    selected = position,
                    legalTargets = targets,
                    selectionTick = it.selectionTick + 1,
                )
            }
        }

        /**
         * How many consecutive preceding moves chain into the move at [index] — the
         * jump-pitch ladder step (D-05): `0` for the first jump of a run, `1` for the
         * second hop of the same peg, and so on.
         *
         * D-06 makes a run **same-peg only**, which is exactly the `from == previous.to`
         * walk below: a move continues a run only when it starts where the previous move
         * landed. Jumping with any other peg breaks the chain and the count restarts at 0.
         *
         * A pure function of [history] with no state of its own, and that is the whole
         * point of D-08: [onUndo] shortens `moveHistory`, so replaying an undone move
         * recomputes the same index and replays the identical pitch. Nothing to reset,
         * nothing to desynchronise.
         *
         * Total for every [index] — an out-of-range one answers 0 rather than throwing,
         * which pairs with the clamp in the audio layer's rate maths (T-07-02).
         *
         * This is a presentation concern and stays in the ViewModel: `:domain` is pure
         * game rules (`DomainPurityTest` guards it), and a pitch ladder is not a rule.
         */
        private fun runIndexAt(
            history: List<Move>,
            index: Int,
        ): Int {
            if (index !in history.indices) return 0
            var run = 0
            var i = index
            while (i > 0 && history[i].from == history[i - 1].to) {
                run++
                i--
            }
            return run
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
            // CR-01: the board is about to change under any running hint search, so cancel
            // it here — AFTER the legal-move guard above, deliberately. A tap that turns
            // out not to be a legal jump changes no board and must not kill a live search.
            cancelHintSearch()
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
                    // Derived AFTER the add, so the just-played move is part of the run.
                    runIndex = runIndexAt(moveHistory, moveHistory.lastIndex),
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
                    // A new move invalidates any shown hint / no-win state, and the
                    // cancelHintSearch() above killed the in-flight search's own
                    // solverThinking write — so this update owes the control its idle
                    // state back, or Hint stays disabled for the rest of the game (CR-01).
                    hintSource = null,
                    hintTarget = null,
                    noWinningMove = false,
                    solverThinking = false,
                    // A dead-end board becomes a rescuable in-board state (D-06): flag it for
                    // the banner instead of auto-navigating. Won never sets this (it routes
                    // to Results); any non-terminal board clears it.
                    stuck = status == GameStatus.Stuck,
                )
            }
            freezeAndFinishIfTerminal(status, nextBoard)
        }

        /**
         * The post-jump terminal handling, split out of [performJump] so committing the move
         * and reacting to the resulting status are two readable steps rather than one long
         * one. Behaviour is unchanged from when this was inline.
         *
         * A terminal status FREEZES the timer for BOTH Won and Stuck (D-04). Only Won emits
         * the one-shot finish (Pitfall 4) → Results; Stuck is rescuable via Undo (D-06) and
         * deliberately emits nothing / never sets [finishedEmitted].
         */
        private fun freezeAndFinishIfTerminal(
            status: GameStatus,
            nextBoard: Board,
        ) {
            if (status == GameStatus.Playing) return
            tickerJob?.cancel()
            tickerJob = null
            if (status != GameStatus.Won || finishedEmitted) return
            finishedEmitted = true
            // A solved game is not resumable: drop this mode's snapshot so the Home
            // Continue affordance can never point at a board that is already over
            // (D-05). Both modes clear — the latch keeps it to once per solve.
            clearSnapshot()
            // Only a SOLVE unlocks the next Challenge level (D-02: no skipping) —
            // Classic (activeChallengeLevel == null) and Stuck never advance. The
            // advance is keyed to the SOLVED level — the restored stamp when there
            // is one (WR-03) — and idempotent at the store (max(current, N+1)), so
            // a re-solve of the same board — back from Results, undo (which resets
            // finishedEmitted), re-jump — can never skip a level (CR-02); the
            // write is transactional inside ChallengeProgress.
            val solvedChallengeLevel = activeChallengeLevel
            if (solvedChallengeLevel != null) {
                challengeProgress.advancePast(solvedChallengeLevel)
            }
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
                    // D-03/D-04: stars are banded on the FINISHING BOARD's own
                    // perfect finish, taken from the spec that produced this
                    // result — not from the level id. A level-id lookup would be
                    // silently wrong for a resumed game, whose spec is rebuilt
                    // structurally from a persisted blob rather than fetched from
                    // the catalog. Diamond-41's target is a proven 2.
                    minimumPegs = nextBoard.spec.minimumPegs,
                ),
            )
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
            // ADSF-01 / RESEARCH Pitfall 10. This is not defensive padding: [onUndo] ends
            // by calling this, and that is precisely the D-08 rescue-banner path where a
            // rewarded ad may have just played. Resuming here reuses the pre-ad [startMark]
            // and only checks `tickerJob != null`, so without this line the ad's whole
            // duration is silently re-added to the elapsed time the score decays against —
            // charging the player for the ad they were promised was free.
            if (frozenForAd) return
            // The two pre-existing guards are folded into one condition purely to keep this
            // function inside detekt's ReturnCount limit now that the freeze guard above is
            // the first of them; neither condition changed.
            val mark = startMark
            if (mark == null || tickerJob != null) return
            tickerJob = launchTicker(mark)
        }

        /**
         * Stops charging wall-clock time for the duration of a rewarded ad presentation
         * (ADSF-01): cancels the ticker and folds the seconds already earned into
         * [elapsedBaseSeconds], so the clock is stopped rather than merely hidden. This is
         * the same rebase [applyRestored] uses to resume a snapshot, not a new mechanism.
         *
         * A no-op when [startMark] is `null` — the timer never started, so there is nothing
         * to freeze and rebasing would invent elapsed time out of an uninitialised mark.
         *
         * The fold is measured from [startMark] with **exactly [launchTicker]'s expression**,
         * never from [GameUiState.elapsedSeconds]. The displayed value is only current while
         * the ticker is running: at a terminal status the ticker stops but the game's real
         * wall-clock keeps running (D-07 — a rescue-undo out of a frozen board is charged
         * for the time spent stuck), so rebasing onto the displayed number would silently
         * refund every second the board sat at that dead end.
         *
         * **Freeze from the REQUEST, never from a dialog being on screen (RESEARCH Pitfall
         * 9).** Scoring is time-decay based, so a clock that stops while an offer dialog
         * sits open is a real scoring exploit: a player could park the dialog indefinitely
         * and stop the score decaying. Request-to-return is bounded by the gateway's own
         * accept-wait cap plus the ad's duration, which is exactly what ADSF-01 means by
         * "the duration of any ad presentation".
         *
         * [GameUiState.elapsedSeconds] is whole seconds, so each freeze discards up to one
         * sub-second remainder — always in the player's favour, and accepted (Assumption A8).
         */
        private fun freezeClockForAd() {
            val mark = startMark ?: return
            frozenForAd = true
            tickerJob?.cancel()
            tickerJob = null
            elapsedBaseSeconds += mark.elapsedNow().inWholeSeconds.toInt()
        }

        /**
         * Resumes the clock [freezeClockForAd] stopped, on a **fresh** [timeSource] mark
         * (ADSF-01). A no-op unless a freeze is actually in effect.
         *
         * The fresh mark is the whole mechanism: reusing the pre-ad mark is exactly how the
         * ad's duration gets silently re-added. It is taken **unconditionally**, before the
         * status check — a terminal or rescued board that later resumes through
         * [resumeTickerIfNeeded] must find a mark that never saw the ad, or the very
         * D-08 rescue path this exists for re-adds the ad's duration one statement later.
         *
         * The ticker itself only relaunches while the game is still [GameStatus.Playing]:
         * the ad's callback timing is the SDK's, not ours, and it can land after the game
         * has finished or the player has left the board — a late callback must not
         * resurrect a finished game's clock.
         */
        private fun unfreezeClockAfterAd() {
            if (!frozenForAd) return
            frozenForAd = false
            val mark = timeSource.markNow()
            startMark = mark
            if (_uiState.value.status != GameStatus.Playing) return
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
                        it.copy(
                            elapsedSeconds =
                                elapsedBaseSeconds + mark.elapsedNow().inWholeSeconds.toInt(),
                        )
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
         * Undo the last move (ASST-02, D-05): requests the [AdReason.UNDO] grant and reads
         * the three-way [RewardOutcome] — [RewardOutcome.Granted] and
         * [RewardOutcome.Unavailable] both proceed (ADS-03: an assist that could not be paid
         * for is still granted), only [RewardOutcome.Declined] aborts. It then rebuilds the
         * board by folding [RulesEngine.applyMove] over
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
                // ADSF-01: the elapsed clock is stopped for the whole rewarded request and
                // resumed on a fresh mark, so watching an ad can never cost the player
                // score. The `finally` guarantees the unfreeze on EVERY exit — including a
                // cancellation — because a freeze left set would disable
                // [resumeTickerIfNeeded] and kill the clock for the rest of the game.
                freezeClockForAd()
                val outcome =
                    try {
                        adGateway.requestRewarded(AdReason.UNDO)
                    } finally {
                        unfreezeClockAfterAd()
                    }
                when (outcome) {
                    // ADS-03: an assist that could not be paid for is still granted — the
                    // player is never dead-ended by an ad that failed to arrive. Mapping
                    // Unavailable to "skip" would silently break undo offline the moment a
                    // real gateway lands in Phase 9.
                    RewardOutcome.Granted, RewardOutcome.Unavailable -> Unit
                    RewardOutcome.Declined -> return@launch
                }
                if (moveHistory.isEmpty()) return@launch
                // CR-01, placed after BOTH guards above: only an undo that really rewinds
                // the board invalidates a running search. A declined grant and an empty
                // history change nothing and must leave a live search alone.
                cancelHintSearch()
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
                        // Undo invalidates any shown hint and clears the rescue banner. It
                        // also cancelled the in-flight search above, killing that job's own
                        // solverThinking write — so the flag is cleared here, returning the
                        // Hint control to idle rather than leaving it spinning (CR-01).
                        hintSource = null,
                        hintTarget = null,
                        noWinningMove = false,
                        solverThinking = false,
                        stuck = false,
                        // Both guards above (a declined grant, an empty history) return
                        // before here, so this ticks once per undo that really happened.
                        undoTick = it.undoTick + 1,
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
         * immediately, requests the [AdReason.HINT] grant — [RewardOutcome.Granted] and
         * [RewardOutcome.Unavailable] both proceed to the search (ADS-03: a hint that could
         * not be paid for is still given), while [RewardOutcome.Declined] clears
         * [GameUiState.solverThinking] and returns so the control never spins forever — then
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
         *
         * **Two guarantees close CR-01**, and both are needed because the board stays fully
         * tappable while the search runs (`GameScreen` disables only the Hint control):
         *  1. Any board mutation CANCELS this search — see [cancelHintSearch], called by
         *     [performJump], [onUndo] and [resetToInitial], by the three async board-install
         *     sites ([loadChallengeBoardIfNeeded], [installClassicBoard], [applyRestored])
         *     and by a repeat request.
         *  2. Even so, a result that still arrives is APPLIED ONLY IF the board it was
         *     computed for is the board still on screen; otherwise nothing but
         *     [GameUiState.solverThinking] is written. The two together mean a hint can
         *     never point at a board the player has moved past, and a `null` can never
         *     raise the rescue banner over a position that was never searched.
         *
         * The three search inputs (board, recorded line, move count) are read ON THE MAIN
         * THREAD before the dispatcher hop. Reading them inside the [withContext] block —
         * as this did before CR-01 — is an unsynchronised cross-thread read of mutable
         * ViewModel state, and could even hand the use case a board from before a jump with
         * a `movesPlayed` from after it.
         *
         * While [GameUiState.isLoading] the request is refused outright (WR-01): the only
         * board on screen is the loading placeholder (`pegs = 0`), which the Solver answers
         * with an instant `null` — and because the placeholder is still the board when the
         * result lands, the stale-result guard below would PASS and raise the "no winning
         * move" rescue banner over the loading indicator, on a position that was never
         * played. `GameScreen` renders the Hint control unconditionally in the top bar, so
         * the guard has to live here, not in the screen.
         */
        fun onHintRequested() {
            if (_uiState.value.isLoading) return
            cancelHintSearch()
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
                    // ADSF-01, same shape as [onUndo]: freeze from the request, unfreeze on
                    // every exit. The `finally` matters more here than there — this job is
                    // [hintJob], which [cancelHintSearch] cancels from every board mutation,
                    // so a plain sequential unfreeze would be skipped outright whenever the
                    // player moves while the ad is up, leaving the clock frozen forever.
                    freezeClockForAd()
                    val outcome =
                        try {
                            adGateway.requestRewarded(AdReason.HINT)
                        } finally {
                            unfreezeClockAfterAd()
                        }
                    when (outcome) {
                        RewardOutcome.Granted, RewardOutcome.Unavailable -> Unit
                        RewardOutcome.Declined -> {
                            _uiState.update { it.copy(solverThinking = false) }
                            return@launch
                        }
                    }
                    val deadline = timeSource.markNow() + SolverBudget.DEFAULT.deadline
                    // CR-01: capture every mutable input HERE, on the main thread, so the
                    // three are consistent with each other and with one another's instant.
                    // Nothing inside the withContext below may read _uiState, moveHistory
                    // or onRecordedLine.
                    val boardForHint = _uiState.value.board
                    val solutionForHint = solution.takeIf { onRecordedLine }
                    val movesPlayedForHint = moveHistory.size
                    val move =
                        withContext(defaultDispatcher) {
                            getHintUseCase(
                                board = boardForHint,
                                solution = solutionForHint,
                                movesPlayed = movesPlayedForHint,
                                shouldCancel = { !isActive || deadline.hasPassedNow() },
                            )
                        }
                    _uiState.update {
                        when {
                            // The player moved on while this ran: the answer belongs to a
                            // board that is no longer on screen, so it is discarded rather
                            // than displayed. Only the spinner is cleared (CR-01).
                            it.board != boardForHint -> it.copy(solverThinking = false)
                            move != null ->
                                it.copy(hintSource = move.from, hintTarget = move.to, solverThinking = false)
                            else -> it.copy(noWinningMove = true, solverThinking = false)
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
            cancelHintSearch()
            startMark = null
            elapsedBaseSeconds = 0
            // A restart taken while a rewarded request is still in flight must not leave the
            // freeze set over the fresh game: the reset clock has no mark to rebase, and a
            // stale flag would make [resumeTickerIfNeeded] refuse for the rest of the game.
            // The in-flight [unfreezeClockAfterAd] then no-ops, leaving this reset intact.
            frozenForAd = false
            finishedEmitted = false
            // Starting this board over abandons the game the snapshot describes, so the
            // stored resume point goes with it (D-05) — otherwise Continue would drop the
            // player back into the position they just chose to leave.
            clearSnapshot()
            // Back to the start ⇒ back on the recorded line (D-03) with no hint shown; the
            // fresh GameUiState.initial(board) below already carries cleared hint fields.
            onRecordedLine = true
            // Preserve the player's hint + haptics choices across a restart; the collected
            // settings flows only re-emit on change, so carry them forward explicitly.
            //
            // The four cue ordinals ride along for a different reason: they are keys for
            // one-shot effects in the screen. A fresh 0 is a *changed* key for an effect
            // sitting at a higher value, so resetting them here would fire a cue nobody
            // triggered — a pop on restart, on a Challenge load or on Continue-resume
            // (RESEARCH Pitfall 2). Guarding on `> 0` in the screen is the second half of
            // that fix; both halves are required, and both are needed at all four of the
            // state-rebuild sites (this one, loadChallengeBoardIfNeeded, installClassicBoard
            // and applyRestored). The `= 0` defaults on the ordinals cannot be relied on to
            // cover a site that forgets one — a default is exactly what would hide it.
            _uiState.value =
                GameUiState
                    .initial(board)
                    .copy(
                        showHints = _uiState.value.showHints,
                        hapticsOn = _uiState.value.hapticsOn,
                        selectionTick = _uiState.value.selectionTick,
                        deselectionTick = _uiState.value.deselectionTick,
                        undoTick = _uiState.value.undoTick,
                        rejectedTapTick = _uiState.value.rejectedTapTick,
                    )
        }

        /**
         * The lifecycle status of [board]: [GameStatus.Playing] while a legal move
         * remains, else [GameStatus.Won] when the board has reached its own perfect
         * finish, otherwise [GameStatus.Stuck] (GAME-02).
         *
         * The win test is the board's `BoardSpec.minimumPegs`, not a hardcoded one peg
         * (D-03/D-04, DEBT-02). For the four Classic cross boards and every generated
         * Challenge footprint that target IS one, so this is byte-identical to the
         * previous behaviour. Diamond-41 is the board it exists for: one peg is proven
         * UNREACHABLE there (the GF(4) colour invariant excludes all 41 single-peg
         * positions), and two is proven reachable, so a two-peg dead end is the best
         * result the board admits — a win, not a failure. Scoring it as Stuck would
         * hide the perfect finish behind the rescue banner and never award the 3 stars
         * the re-anchored bands compute.
         *
         * The comparison is `<=` rather than `==` so the status can never disagree with
         * [com.app.pegsolitaire.domain.scoring.ScoreCalculator.stars], whose 3-star band
         * is also `<= minimumPegs`.
         */
        private fun statusFor(board: Board): GameStatus =
            when {
                !rulesEngine.isGameOver(board) -> GameStatus.Playing
                board.pegCount <= board.spec.minimumPegs -> GameStatus.Won
                else -> GameStatus.Stuck
            }

        private companion object {
            /** The visible timer ticks once per second (D-04). */
            const val TIMER_TICK_MILLIS = 1_000L

            /** The timer counts whole seconds; the resume snapshot stores milliseconds. */
            const val MILLIS_IN_SECOND = 1_000L
        }
    }
