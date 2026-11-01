package com.app.pegsolitaire.ui.game

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.data.resume.ChallengeStamp
import com.app.pegsolitaire.data.resume.RestoredGame
import com.app.pegsolitaire.data.resume.SnapshotMapper
import com.app.pegsolitaire.domain.assist.AssistAllowance
import com.app.pegsolitaire.domain.assist.AssistKind
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
import com.app.pegsolitaire.domain.scoring.ChallengeClearRule
import com.app.pegsolitaire.domain.scoring.ChallengeFinish
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.scoring.ScoreParams
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.solver.SolverBudget
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.domain.usecase.HintOutcome
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
 * @param adGateway the seam the assist funnel consults: it answers whether a rewarded offer
 *   is worth showing at all ([AdGateway.rewardedOfferable], D-12), warms an ad at the moment
 *   a free counter empties ([AdGateway.preloadRewarded], D-09), and serves the rewarded
 *   request that only [onAcceptAdOffer] ever makes. The Slice-1 stub is offerable and
 *   auto-grants, so the whole funnel is exercised before any real SDK exists.
 * @param getHintUseCase the pure-domain hint decision (ASST-01/ASST-03): recorded-line-first
 *   then the budgeted Solver, run off the main thread by [onHintRequested].
 * @param savedStateHandle carries the typed `Gameplay(levelId)` route arg.
 *
 * ## Size suppressions
 *
 * `TooManyFunctions` / `LongParameterList` / `LargeClass` are suppressed together for one
 * reason: this is the screen's single state owner, and every gesture on it is one small
 * handler. `LargeClass` joined the list with the assist funnel (plan 09-08). The only way to
 * drop under the threshold would be a second state owner holding the allowance and the offer
 * — and separating the counters from the board they are spent on, and from the clock the ad
 * freezes, is precisely how the three would drift out of step.
 */
@Suppress("TooManyFunctions", "LongParameterList", "LargeClass") // see "Size suppressions" above
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
         * where it becomes the database level key and the level the "Next level" action
         * resolves its destination from (`max(ladder head, N + 1)`, D-04a, in
         * `ResultsViewModel.onNextLevelTapped`). It previously returned the RAW route
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
         * The generator contract that made the Challenge board being played: this build's
         * [GeneratorConstants.GENERATOR_VERSION] for a board this ViewModel generated, else the
         * restored snapshot's own stamp (docs/gdd.md §12.3(3); tech-lead brief §1(b)).
         *
         * It travels with the board, like [stampedChallengeLevel] travels with the level: a re-save
         * writes it back unchanged ([onLeaveScreen]) — relabelling a version-1 board as the current
         * version would make it permanent — and a restart whose stamp is not the current contract
         * takes the level-load path instead of rebuilding the old board ([resetToInitial]). Stored
         * state, so untrusted, but harmless either way: a forged value can only choose between the
         * level's current board and the player's own stored one. Meaningless for Classic.
         */
        private var boardGeneratorVersion: Int = GeneratorConstants.GENERATOR_VERSION

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

        private val _finished = Channel<GameFinish>(Channel.CONFLATED)

        /**
         * Fires exactly once when the game reaches Won, carrying both halves of the play:
         * the computed [ScoreResult] and the [SolveTelemetry] of the very same solve
         * (D-08). Consumed once by `GameRoute` inside a `LaunchedEffect` to navigate to
         * Results (one-shot — a recomposition can never re-fire it, Pitfall 4).
         *
         * The two travel as one [GameFinish] rather than on two flows so a consumer cannot
         * receive a score without the telemetry that belongs to it.
         */
        val finished: Flow<GameFinish> = _finished.receiveAsFlow()

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
         * The winning move the solver has already found, held back until the assist is paid
         * for (ADS-06). It exists because plan 09-06 inverted the hint order: the search now
         * runs BEFORE the gateway is consulted, so that a position with no winning move costs
         * neither an allowance unit nor an ad — and the found move therefore has to survive
         * the ad round trip somewhere. This is that somewhere.
         *
         * Written only on the Found branch of [onHintRequested]. Its readers are
         * [onAcceptAdOffer], which reveals it once the offer resolves, and [onHintRequested]
         * itself, which reveals it inline whenever the assist never needed an offer at all.
         *
         * Cleared by [cancelHintSearch], which is the single idiom every board mutation
         * already calls (the four board-install sites, [performJump] and [onUndo]) — so a
         * move computed for a superseded board can never be revealed after an ad returns.
         */
        private var pendingHint: Move? = null

        /**
         * The board [pendingHint] was computed for, stashed with it so [revealPendingHint]
         * can re-check staleness on the far side of an ad the caller no longer has a local
         * for (CR-01).
         *
         * It travels with the move rather than being passed as a parameter because the
         * reveal now has two callers whose distance from the search differs by an entire ad
         * presentation, and a board captured in one of them would simply not be in scope in
         * the other. Written and cleared only together with [pendingHint] — see
         * [clearPendingHint], which is the one place either is dropped.
         */
        private var pendingHintBoard: Board? = null

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
            // A stashed move is only ever valid for the board it was computed on, and every
            // site that cancels a search is a site where that board is going away. Dropping
            // it here rather than at each caller keeps the "one cancellation idiom" property
            // that the KDoc above depends on (T-09-25).
            clearPendingHint()
        }

        /**
         * Drops the stashed hint and the board it was computed for, together and always.
         *
         * The pair is meaningless apart — a move with no board to check it against cannot be
         * revealed safely, and a board with no move is dead weight — so there is exactly one
         * place either is cleared. Called by [cancelHintSearch] (every board mutation), by
         * [onDismissAdOffer] and by the declined arm of [onAcceptAdOffer]: an offer the
         * player refused leaves no hint waiting to be revealed by the next grant.
         */
        private fun clearPendingHint() {
            pendingHint = null
            pendingHintBoard = null
        }

        /**
         * The free hints and undos this level still owes the player (ADS-02, D-01/D-02).
         *
         * Deliberately **private**, and deliberately absent from [GameUiState] (D-05): the
         * Hint and Undo controls must never change appearance with the counters — no badge,
         * no "×3" chip, no allowance-driven disabled state — so the opt-in dialog stays the
         * only signal the allowance system exists at all.
         *
         * **A genuinely new level gets a fresh allowance for free, with no reset call.**
         * `GameViewModel` is scoped to one Gameplay destination, so playing a different level
         * is a different instance and this field's initializer *is* the reset. Adding an
         * explicit reset would necessarily also fire on retry, which is precisely what D-04
         * forbids — see [resetToInitial].
         *
         * Restored by [applyRestored] and written back by [onLeaveScreen], so an exhausted
         * allowance survives process death as exhausted (D-04/D-17): backgrounding the app is
         * not a free refill.
         */
        private var allowance: AssistAllowance = AssistAllowance()

        /**
         * How many hints the player has actually **received** on this play, and how many
         * undos have actually **rewound the board** (D-09, PGSA-02).
         *
         * Every one counts — free-allowance, silently-free-because-ads-are-unservable, and
         * rewarded alike. There is no "this one was paid for so it does not spoil the run"
         * carve-out: the achievements these feed (Hint-free, Undo-free) are claims about
         * the *play*, not about the payment.
         *
         * Deliberately **private**, and deliberately absent from [GameUiState], for the
         * same D-05 reason [allowance] is: no counter may become a visible badge. Their
         * only legitimate exits are the finish event ([finished]) and the resume snapshot.
         *
         * **They ARE reset by a restart and by a new level — the opposite of [allowance].**
         * [resetToInitial] clears them explicitly, and a genuinely new level is a new
         * `GameViewModel` whose field initializers are the reset. The asymmetry is the
         * point: a restart must not refill free assists (D-04), but neither may it let a
         * player launder a hint-free badge by retrying the level after taking a hint.
         *
         * They ride the resume snapshot ([onLeaveScreen]/[applyRestored]) so backgrounding
         * or killing the app mid-level cannot erase an assist already taken (D-09).
         */
        private var hintsUsed: Int = NO_ASSISTS_USED

        /** Undos that really rewound the board on this play — see [hintsUsed]. */
        private var undosUsed: Int = NO_ASSISTS_USED

        /**
         * Records one **applied** assist of [kind] — the single place either counter is
         * ever incremented, in the same register as [performUndo] being the single place
         * the move history is ever shortened.
         *
         * It is called from the two APPLICATION points ([performUndo] and
         * [revealPendingHint]) and never from [enterAssistFunnel], which is what makes
         * "a declined or failed offer costs nothing" structurally true instead of a rule
         * somebody has to remember (D-09, threat T-12-23).
         *
         * The `when` is exhaustive with no `else`: a third [AssistKind] must fail the build
         * rather than silently go uncounted.
         */
        private fun countAssist(kind: AssistKind) {
            when (kind) {
                AssistKind.HINT -> {
                    hintsUsed += 1
                }
                AssistKind.UNDO -> {
                    undosUsed += 1
                }
            }
        }

        /**
         * This ad reason as the `:domain` policy names it.
         *
         * The two enums duplicate each other **by design, not by oversight**: [AdReason]
         * lives in `platform/ads/`, and `DomainPurityTest` forbids `:domain` from depending
         * on app code at all, so the free-allowance policy has to own its own [AssistKind]
         * and this ViewModel is the single place that knows both. Do not "tidy" the
         * duplication away — collapsing it would invert the module graph and take the
         * allowance policy out of pure-JVM test reach.
         */
        private fun AdReason.toAssistKind(): AssistKind =
            when (this) {
                AdReason.HINT -> AssistKind.HINT
                AdReason.UNDO -> AssistKind.UNDO
            }

        /**
         * The one gate every assist passes through, answering whether it may proceed **right
         * now** — `true` to go ahead, `false` because an opt-in offer is now on screen and
         * [onAcceptAdOffer] / [onDismissAdOffer] will resolve it.
         *
         * Three outcomes, in this order, and the order is the whole design:
         *  1. **Free while the allowance lasts (ADS-02).** The counter is spent and the
         *     assist proceeds with no dialog and no ad. If that spend is what emptied the
         *     counter — and only then — the gateway is asked to warm an ad (D-09): the
         *     earliest moment an offer can possibly occur, which is why most levels issue no
         *     ad request at all.
         *  2. **Silently free when ads are knowably unservable (CONS-04/D-12).** Consent was
         *     refused, the SDK never initialized, or readiness has not resolved yet
         *     (Pitfall 16 — `Unknown` is NOT offerable). The assist is granted for free with
         *     **no dialog and no snackbar**, byte-identical to v1.0. Prompting "watch an ad?"
         *     and then granting instantly because nothing could be served is exactly the
         *     absurdity D-12 rules out, and telling a puzzle player about a consent failure
         *     is noise about a system they were never told existed.
         *  3. **Otherwise, an explicit opt-in offer (ADS-01).** No ad is requested here; the
         *     dialog is raised and the player decides. This is what makes AdMob's
         *     "affirmatively and unambiguously opts in" requirement structural rather than
         *     a convention — the only `requestRewarded` call site in this file is behind an
         *     accept tap (T-09-32).
         *
         * The `rewardedOfferable` read sits **after** the allowance check on purpose: a
         * player who still has free assists must not have their behaviour depend on consent
         * state at all.
         *
         * **Nothing here counts an assist (D-09).** [countAssist] is called only from the
         * two APPLICATION points, [performUndo] and [revealPendingHint]. Two of this gate's
         * three `true` outcomes can still be followed by a no-op action — an undo whose
         * history was emptied while an ad played, a hint discarded as stale — and
         * [onDismissAdOffer] / [RewardOutcome.Declined] must cost nothing at all. Counting
         * where the assist is *applied* rather than where it is *permitted* makes every one
         * of those fall out with no second rule to keep in sync.
         */
        private fun enterAssistFunnel(
            kind: AssistKind,
            reason: AdReason,
        ): Boolean =
            when {
                allowance.hasFree(kind) -> {
                    allowance = allowance.spend(kind)
                    if (!allowance.hasFree(kind)) adGateway.preloadRewarded(reason)
                    true
                }
                !adGateway.rewardedOfferable.value -> true
                else -> {
                    _uiState.update { it.copy(pendingAdOffer = reason) }
                    false
                }
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
            // The active level, not the route's: on a fresh entry the two are the same, and on a
            // restart of a restored game the stamp is the level being played (WR-03).
            val challengeLevelToLoad = activeChallengeLevel ?: return
            val level =
                withContext(defaultDispatcher) {
                    levelRepository.loadChallenge(challengeLevelToLoad)
                }
            initialBoard = level.initialBoard
            // This build's generator made it, whatever the board it replaces was stamped.
            boardGeneratorVersion = GeneratorConstants.GENERATOR_VERSION
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
                        restartTick = it.restartTick,
                        // Carried, never reset — a fresh 0 is a *changed* key for an effect
                        // sitting higher, which fires a snackbar nobody triggered. The offer
                        // fields are the opposite case and are deliberately absent here: the
                        // GameUiState.initial() rebuild CLEARS pendingAdOffer/adOfferLoading,
                        // because a dialog for a board that no longer exists must not survive.
                        adMessageTick = it.adMessageTick,
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
            // GameUiState.initial() rebuild below clears solverThinking — and, equally, the
            // pendingAdOffer / adOfferLoading pair — for us.
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
                        restartTick = it.restartTick,
                        // Carried like the other four ordinals; see loadChallengeBoardIfNeeded.
                        adMessageTick = it.adMessageTick,
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
            // The contract that made the stored board, kept with it (docs/gdd.md §12.3(3)).
            restored.challenge?.let { boardGeneratorVersion = it.generatorVersion }
            initialBoard = restored.initialBoard
            moveHistory.clear()
            moveHistory.addAll(restored.moveHistory)
            onRecordedLine = false
            elapsedBaseSeconds = (restored.elapsedMs / MILLIS_IN_SECOND).toInt()
            // D-04/D-17. `RestoredGame.allowance` is non-null by construction: SnapshotMapper
            // resolved it through AssistAllowance.fromStoredOrFull, which already rejected an
            // out-of-range stored counter in favour of the full default (T-09-35). The raw
            // DTO ints are never read here — the forged-counter decision belongs to the codec,
            // in one place, not to every consumer of a restore.
            allowance = restored.allowance
            // D-09, and the same argument as the allowance above: `RestoredGame`'s counters
            // are non-null by construction because SnapshotMapper already resolved the
            // untrusted stored value. Absent resolves to zero per D-09 — which fails OPEN
            // toward the achievement, accepted because the store is local-only and the
            // exposure is one self-awarded badge — while a negative counter is forgery and
            // refuses the resume outright, so a restore never carries one. The raw DTO ints
            // are never read here; the forged-counter decision belongs to the codec, in one
            // place, not to every consumer of a restore.
            hintsUsed = restored.hintsUsed
            undosUsed = restored.undosUsed
            // Board-install = board mutation, so the CR-01 idiom applies (WR-01); the
            // GameUiState.initial() rebuild below clears solverThinking — and, equally, the
            // pendingAdOffer / adOfferLoading pair — for us.
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
                        restartTick = it.restartTick,
                        // Carried like the other four ordinals; see loadChallengeBoardIfNeeded.
                        adMessageTick = it.adMessageTick,
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
                                // The board's own contract, not this build's: a resumed old board
                                // stays labelled old, so its restart still regenerates.
                                generatorVersion = boardGeneratorVersion,
                            )
                        },
                    // D-04/D-17: the counters ride the snapshot so process death is not a
                    // free refill. A stored 0 is in range and is preserved as 0 — an
                    // exhausted allowance must come back exhausted, or backgrounding the
                    // app would be the loophole the persistence exists to close.
                    freeHintsRemaining = allowance.hintsRemaining,
                    freeUndosRemaining = allowance.undosRemaining,
                    // D-09's anti-laundering half: the assists already TAKEN ride the
                    // snapshot too, so killing the process mid-level cannot erase one and
                    // turn a hinted solve into a hint-free one. A stored 0 is an honest
                    // claim ("no assists yet") and comes back as 0, not as "unknown".
                    hintsUsed = hintsUsed,
                    undosUsed = undosUsed,
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
                // consumer can observe it (SOUND-02's quiet invalid tap), and the cell is where
                // the board answers it (D-06's shake, mark and redirect).
                else ->
                    _uiState.update {
                        it.copy(rejectedTapTick = it.rejectedTapTick + 1, rejectedTapCell = position)
                    }
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
                    hintUndecided = false,
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
         * deliberately emits nothing / never sets [finishedEmitted]. Won is the mode's finish
         * test ([statusFor]): in Challenge, any clear — one to three pegs (docs/gdd.md R5, R7).
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
            // Only a CLEAR unlocks the next Challenge level (D-02: no skipping) — any star
            // count, one to three pegs, since 3.0.0 (docs/gdd.md R7) — and Classic
            // (activeChallengeLevel == null) and Stuck never advance. The advance is keyed to
            // the CLEARED level — the restored stamp when there is one (WR-03) — and
            // idempotent at the store (max(current, N+1)), so a re-clear of the same board —
            // back from Results, undo (which resets finishedEmitted), re-jump — can never skip
            // a level (CR-02); the write is transactional inside ChallengeProgress.
            val clearedChallengeLevel = activeChallengeLevel
            if (clearedChallengeLevel != null) {
                challengeProgress.advancePast(clearedChallengeLevel)
            }
            _finished.trySend(
                GameFinish(
                    result = scoreFor(nextBoard),
                    telemetry = telemetryFor(nextBoard),
                    // The finishing board's own floor, carried on so Results can apply the SAME
                    // test Classic's win just passed (`pegCount <= spec.minimumPegs`) rather than
                    // a hardcoded one peg. A Challenge Results screen reads ChallengeClearRule
                    // instead; every generated footprint carries one here (docs/gdd.md I5).
                    minimumPegs = nextBoard.spec.minimumPegs,
                ),
            )
        }

        /**
         * The [ScoreResult] of a WON [finishingBoard], graded the mode's way.
         *
         * Challenge stars come from [ChallengeClearRule] — the absolute band, one peg three stars,
         * two pegs two, three pegs one (docs/gdd.md R5) — and are passed to the calculator as
         * explicit stars. Classic passes none, so the calculator bands on the FINISHING BOARD's
         * own perfect finish (D-03/D-04), taken from the spec that produced this result — not
         * from the level id. A level-id lookup would be silently wrong for a resumed game, whose
         * spec is rebuilt structurally from a persisted blob rather than fetched from the
         * catalog. Diamond-41's target is a proven 2.
         *
         * Only a Won board gets here, and a Won Challenge board is cleared by construction
         * ([statusFor] reads the same rule), so [ChallengeFinish.NotCleared] shares the Classic
         * arm only to keep the `when` exhaustive; no play reaches it there.
         */
        private fun scoreFor(finishingBoard: Board): ScoreResult {
            val pegsRemaining = finishingBoard.pegCount
            // A jump can only fire once the board is loaded, so initialBoard is
            // non-null here; fall back to the current count defensively.
            val pegsCleared = (initialBoard?.pegCount ?: pegsRemaining) - pegsRemaining
            val elapsedSeconds = _uiState.value.elapsedSeconds
            val minimumPegs = finishingBoard.spec.minimumPegs
            return when (val challengeFinish = activeChallengeLevel?.let { ChallengeClearRule.finish(pegsRemaining) }) {
                is ChallengeFinish.Cleared ->
                    scoreCalculator.evaluate(
                        pegsRemaining = pegsRemaining,
                        pegsCleared = pegsCleared,
                        elapsedSeconds = elapsedSeconds,
                        levelMultiplier = levelMultiplier,
                        minimumPegs = minimumPegs,
                        stars = challengeFinish.stars,
                    )
                ChallengeFinish.NotCleared, null ->
                    scoreCalculator.evaluate(
                        pegsRemaining = pegsRemaining,
                        pegsCleared = pegsCleared,
                        elapsedSeconds = elapsedSeconds,
                        levelMultiplier = levelMultiplier,
                        minimumPegs = minimumPegs,
                    )
            }
        }

        /**
         * This play's [SolveTelemetry] (D-08): the two counters as they stand at the finish,
         * plus the D-01 target test against the board that was just played.
         *
         * Extracted purely to keep [freezeAndFinishIfTerminal] inside detekt's `LongMethod`
         * budget without an early `return` (its `ReturnCount` allowance is already spent).
         */
        private fun telemetryFor(finishingBoard: Board): SolveTelemetry =
            SolveTelemetry(
                hintsUsed = hintsUsed,
                undosUsed = undosUsed,
                lastPegOnTarget = lastPegOnTarget(finishingBoard),
            )

        /**
         * Whether [finishingBoard] holds exactly one peg AND that peg sits on the board's
         * own target hole (D-01).
         *
         * Three facts this encodes, in the order they bite:
         *  1. **The target comes from the FINISHING BOARD's own spec**, never from a catalog
         *     lookup by [levelId] — the same reasoning the `minimumPegs` comment a few lines
         *     above already makes. A resumed game's spec is rebuilt *structurally* from the
         *     persisted blob, so a level-id lookup would judge a different board than the one
         *     actually played.
         *  2. **A board with no target is always false**, which covers every freeform
         *     Challenge footprint (they are built with `target = null`), and a multi-peg Won
         *     finish is false too — Diamond-41's perfect finish is a proven TWO pegs, which
         *     is a flawless win but not a target finish.
         *  3. **English-33's target IS `(3, 3)`** (`Boards.English33` is built with
         *     `target = English33Layout.center`), so a true here on English-33 is exactly
         *     D-01's Center finish. This records the GENERAL fact for every board on purpose:
         *     the narrowing to English-33 is a `mode = 'classic' AND levelId = 'english33'`
         *     SQL predicate that plan 12-06 owns, which keeps `play_session` honest and makes
         *     a future per-board variant a query change rather than a migration.
         */
        private fun lastPegOnTarget(finishingBoard: Board): Boolean {
            val target = finishingBoard.spec.target ?: return false
            return finishingBoard.pegCount == SINGLE_PEG_REMAINING &&
                finishingBoard.cellAt(target) == Cell.Peg
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
         * Undo the last move (ASST-02, D-05): checks there is anything to undo, then puts the
         * request through the shared [enterAssistFunnel] — free while the per-level allowance
         * lasts (ADS-02), silently free when ads are unservable (D-12), and otherwise an
         * explicit opt-in offer that [onAcceptAdOffer] resolves (ADS-01). Nothing here
         * touches the gateway directly any more.
         *
         * The rewind itself lives in [performUndo], which this and the accept path both call.
         * An empty history is a no-op **that never reaches the funnel at all** (T-09-24): no
         * counter is spent and no ad is requested to do nothing.
         *
         * **This is also the ADS-04 rescue-banner path, with no banner-specific code.**
         * `NoWinBanner`'s undo action routes straight into this function, so it spends a
         * normal undo while any remain and raises the very same dialog at zero (D-08) — one
         * allowance, one dialog, no second offer surface.
         *
         * The timer is deliberately untouched (D-07): [startMark], [tickerJob], and
         * [GameUiState.elapsedSeconds] all keep counting real time across an undo.
         */
        fun onUndo() {
            viewModelScope.launch {
                // T-09-24, and the reason this guard is FIRST: an undo on a board with no
                // history is a pure no-op, so there is no assist to charge for. Below the
                // funnel — where the gateway call used to sit — tapping Undo on a fresh board
                // would burn an allowance unit or play a thirty-second ad to do nothing at
                // all. A unit could be refunded; an ad already watched cannot.
                if (moveHistory.isEmpty()) return@launch
                // false ⇒ the opt-in dialog is now up and this undo is suspended, not
                // refused: the board, the history and the ticker are all left exactly as
                // they were until the player answers.
                if (!enterAssistFunnel(AssistKind.UNDO, AdReason.UNDO)) return@launch
                performUndo()
            }
        }

        /**
         * Rewinds the board by one move — **the single place the move history is ever
         * shortened**, called both by [onUndo] (free or unservable-free) and by
         * [onAcceptAdOffer] (paid for). Keeping it in one place is what stops the two paths
         * from drifting into subtly different undos.
         *
         * It rebuilds the board by folding [RulesEngine.applyMove] over the remaining
         * [moveHistory] from [initialBoard] — never trusting any external or serialized state
         * (T-04-01-U). Repeated calls walk all the way back to the initial board. The board's
         * [statusFor] status is recomputed and [finishedEmitted] is cleared when the undone
         * board is Playing again, so a previously Stuck game becomes playable and can re-emit
         * a later terminal result.
         *
         * The empty-history guard is repeated here rather than assumed: the accept path
         * reaches this on the far side of an SDK-timed ad callback, and a restart taken while
         * the ad was up clears [moveHistory] underneath it. Without the guard that lands as
         * an index-out-of-bounds crash on the return from an ad.
         */
        private fun performUndo() {
            if (moveHistory.isEmpty()) return
            // CR-01: only an undo that really rewinds the board invalidates a running search,
            // which is why this sits here rather than in [onUndo] — a declined offer and an
            // empty history change nothing and must leave a live search alone.
            cancelHintSearch()
            val undone = moveHistory.removeAt(moveHistory.lastIndex)
            val base = initialBoard ?: return
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
            //
            // D-09: counted HERE, alongside undoTick, and deliberately so — that ordinal
            // already means "an undo that really happened", so the counter and the cue are
            // advanced by the same condition and cannot drift apart. Every guard and every
            // refusal (an empty history, a raised offer, a declined grant) returned before
            // this line.
            countAssist(AssistKind.UNDO)
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
                    hintUndecided = false,
                    solverThinking = false,
                    stuck = false,
                    // Every guard and every refusal returns before here — an empty history,
                    // a raised offer, a declined grant — so this ticks once per undo that
                    // really happened.
                    undoTick = it.undoTick + 1,
                    // D-TB: the move this undo took back, which the undo's spoken line names.
                    undoneMove = undone,
                )
            }
            // Undoing out of a frozen (Stuck) board back to Playing resumes the timer that
            // the terminal freeze stopped, reusing the never-reset startMark (D-06/D-07).
            if (status == GameStatus.Playing) resumeTickerIfNeeded()
        }

        /**
         * Reveal the next winning move (ASST-01/ASST-03, D-01/D-10). Cancels any prior
         * search (no overlapping Solver work, threat T-04-SD), raises [GameUiState.solverThinking]
         * immediately, and resolves the move OFF the main thread via [getHintUseCase] on
         * [defaultDispatcher]:
         *  - on the recorded line (Challenge) the use case returns the recorded move instantly,
         *    with no Solver call;
         *  - otherwise (Classic, or after a deviation) it runs the budgeted Solver, cancelled
         *    cooperatively once the wall-clock [SolverBudget.deadline] passes or the job is
         *    cancelled — the deadline crosses into pure `:domain` only as a plain lambda.
         *
         * **It searches first and charges second (ADS-06).** Only once a move has actually
         * been found does the request enter [enterAssistFunnel] — free while the allowance
         * lasts (ADS-02), silently free when ads are unservable (D-12), otherwise an opt-in
         * offer that [onAcceptAdOffer] resolves. An answer that reveals no move enters the
         * funnel **not at all** (GD-HT-5): a [HintOutcome.ProvenDead] sets
         * [GameUiState.noWinningMove] (its banner ships in 04-04), and a
         * [HintOutcome.Undecided] sets [GameUiState.hintUndecided] — the Undecided notice,
         * never the banner, because nothing was proved (GD-HT-1, GD-HT-2). An allowance unit
         * could be refunded, but a thirty-second ad already watched to be told nothing cannot,
         * so the order — not a refund — is the mechanism. Every path clears
         * [GameUiState.solverThinking], and it is cleared before the funnel is entered, so the
         * control is never left spinning behind a dialog.
         *
         * **A deadline is not a cancel (GD-HT-3).** The `shouldCancel` lambda latches on both
         * a superseded job and the deadline, and the search answers Undecided for either. A
         * superseded search is never observed — its job was cancelled, so `withContext` never
         * returns into it, and the board guard below is the second lock — so the only latched
         * stop that reaches the outcome is a deadline on an unchanged board, and it is shown,
         * never swallowed into silence.
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
         *     never point at a board the player has moved past, and an answer with no move
         *     can never raise the banner or the notice over a position never searched.
         *
         * The three search inputs (board, recorded line, move count) are read ON THE MAIN
         * THREAD before the dispatcher hop. Reading them inside the [withContext] block —
         * as this did before CR-01 — is an unsynchronised cross-thread read of mutable
         * ViewModel state, and could even hand the use case a board from before a jump with
         * a `movesPlayed` from after it.
         *
         * While [GameUiState.isLoading] the request is refused outright (WR-01): the only
         * board on screen is the loading placeholder (`pegs = 0`), which the Solver answers
         * with an instant ProvenDead — and because the placeholder is still the board when the
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
                    hintUndecided = false,
                    hintSource = null,
                    hintTarget = null,
                )
            }
            hintJob =
                viewModelScope.launch {
                    val deadline = timeSource.markNow() + SolverBudget.DEFAULT.deadline
                    // CR-01: capture every mutable input HERE, on the main thread, so the
                    // three are consistent with each other and with one another's instant.
                    // Nothing inside the withContext below may read _uiState, moveHistory
                    // or onRecordedLine.
                    val boardForHint = _uiState.value.board
                    val solutionForHint = solution.takeIf { onRecordedLine }
                    val movesPlayedForHint = moveHistory.size
                    val outcome =
                        withContext(defaultDispatcher) {
                            getHintUseCase(
                                board = boardForHint,
                                solution = solutionForHint,
                                movesPlayed = movesPlayedForHint,
                                shouldCancel = { !isActive || deadline.hasPassedNow() },
                            )
                        }
                    // The player moved on while this ran: the answer belongs to a board that
                    // is no longer on screen, so it is discarded rather than displayed. Only
                    // the spinner is cleared (CR-01) — and because this now sits AHEAD of the
                    // gateway, a discarded answer is not charged for either.
                    if (_uiState.value.board != boardForHint) {
                        _uiState.update { it.copy(solverThinking = false) }
                        return@launch
                    }
                    // ADS-06, and the whole point of searching first: the answer is already
                    // known, so an answer that reveals no move is FREE — no allowance unit, no
                    // ad, no gateway call at all (GD-HT-5). Only a proof raises the "no winning
                    // move" banner; a search that stopped without one raises the Undecided
                    // notice instead, and never the banner (GD-HT-1, GD-HT-2).
                    val move =
                        when (outcome) {
                            is HintOutcome.Found -> outcome.move
                            HintOutcome.ProvenDead -> {
                                _uiState.update { it.copy(noWinningMove = true, solverThinking = false) }
                                return@launch
                            }
                            HintOutcome.Undecided -> {
                                _uiState.update { it.copy(hintUndecided = true, solverThinking = false) }
                                return@launch
                            }
                        }
                    // A real assist was found, so this one IS chargeable. Stash it — with the
                    // board it was computed for — before charging: the reveal may happen on
                    // the far side of an ad the player spends thirty seconds watching, from
                    // [onAcceptAdOffer], where neither of these locals is in scope.
                    pendingHint = move
                    pendingHintBoard = boardForHint
                    // Cleared BEFORE the funnel, so the Hint control is never left spinning
                    // behind an offer dialog or an ad.
                    _uiState.update { it.copy(solverThinking = false) }
                    // false ⇒ the offer is on screen and the stashed hint waits for the
                    // player's answer; [onAcceptAdOffer] reveals it, [onDismissAdOffer]
                    // drops it.
                    if (!enterAssistFunnel(AssistKind.HINT, AdReason.HINT)) return@launch
                    revealPendingHint()
                }
        }

        /**
         * Puts the Challenge proven-dead banner away (ux-spec D-SB-4; docs/gdd.md GD-SB-19): a tap on
         * the banner outside its buttons, or TalkBack's Dismiss. It clears the proof flag,
         * [GameUiState.noWinningMove], and **nothing else** — it is not a hint, not a move and not an
         * undo, so no allowance unit is spent, no assist is counted, no cue plays, and the clock, the
         * board, the selection and focus are left as they are (D-09: dismissals stay quiet). The proof
         * still holds: a Hint asked on the same position answers Proven dead again, free (GD-HT-5), and
         * raises the banner again. [GameUiState.stuck] is not touched, because a stuck banner is never
         * put away; the screen offers this only for the proven-dead banner of a Challenge board.
         */
        fun onDismissProvenDeadBanner() {
            _uiState.update { it.copy(noWinningMove = false) }
        }

        /**
         * The player agreed to watch an ad for more of the offered assist (ADS-01).
         *
         * The only place in this file that requests a rewarded ad, and it is reachable only
         * from an explicit tap on a dialog that already named the reward — which is what
         * makes AdMob's "must only be served after a user affirmatively and unambiguously
         * opts in" a structural property here rather than a convention (T-09-32).
         *
         * The order of the body is load-bearing:
         *  - [GameUiState.adOfferLoading] goes up **first**, so the dialog swaps its actions
         *    for a spinner and locks itself (D-10) rather than letting a scrim tap race a
         *    returning `Granted` into a torn-down dialog (T-09-11). The bound that keeps that
         *    lock from dead-ending is `AdConstants.ACCEPT_WAIT_MS`, applied inside the
         *    gateway around its ad *load* — deliberately not wrapped around this whole call,
         *    which also spans the ad the player is actually watching.
         *  - [freezeClockForAd] runs **here, at accept — never where [pendingAdOffer] was
         *    set** (ADSF-01, Pitfall 9). Scoring decays with elapsed time, so a clock that
         *    stopped while an offer dialog sat open would let a player park the dialog to
         *    halt the decay indefinitely. Accept-to-return is bounded by the gateway's own
         *    accept wait plus the ad's duration, which is exactly what ADSF-01 covers. The
         *    `finally` guarantees the unfreeze on every exit including cancellation: a freeze
         *    left set disables [resumeTickerIfNeeded] and kills the clock for the whole game.
         *  - The assist itself is performed last, and only for the two outcomes that earned
         *    it — see [grantReward].
         */
        fun onAcceptAdOffer() {
            val reason = _uiState.value.pendingAdOffer ?: return
            val kind = reason.toAssistKind()
            viewModelScope.launch {
                _uiState.update { it.copy(adOfferLoading = true) }
                freezeClockForAd()
                val outcome =
                    try {
                        adGateway.requestRewarded(reason)
                    } finally {
                        unfreezeClockAfterAd()
                    }
                _uiState.update { it.copy(pendingAdOffer = null, adOfferLoading = false) }
                if (!grantReward(outcome, kind)) return@launch
                when (kind) {
                    AssistKind.HINT -> revealPendingHint()
                    AssistKind.UNDO -> performUndo()
                }
            }
        }

        /**
         * "No thanks", a scrim tap, or system back on the opt-in dialog (D-06).
         *
         * It drops the stashed hint and takes the dialog down, and it changes **nothing
         * else**: not a counter, not the board, not the move history, not the ticker. D-06's
         * "declining leaves the game exactly as it was" has to be literally true, and AdMob's
         * requirement that declining "must not impede or interfere with the normal usage of
         * the app" rests on the same line (T-09-33). There is deliberately no reference to
         * [allowance] anywhere in this function.
         */
        fun onDismissAdOffer() {
            clearPendingHint()
            _uiState.update { it.copy(pendingAdOffer = null, adOfferLoading = false) }
        }

        /**
         * Applies the three-way [RewardOutcome] to the allowance, answering whether the
         * assist that triggered the offer should now be performed.
         *
         * [RewardOutcome.Granted] and [RewardOutcome.Unavailable] both **refill then spend**,
         * in that order (D-03/D-07): one ad buys a whole new allowance of that assist, and
         * the assist being paid for is the first use of it — so under the dialog's own copy
         * ("watch a short ad for 3 more?") the player really does receive three further uses,
         * this one plus two held. Reversing the two would silently make the copy a lie.
         *
         * [RewardOutcome.Unavailable] additionally raises the D-11 snackbar (ADS-03). The
         * assist lands **first** and the message states both the problem and its
         * already-applied resolution, so there is no action label — there is nothing left for
         * the player to do. The player is never dead-ended by an ad that failed to arrive
         * (T-09-37).
         *
         * [RewardOutcome.Declined] touches neither counter. An unearned early dismiss arrives
         * here as `Declined` — the gateway sets `Granted` only from the SDK's earned-reward
         * callback — so a player who closes the ad early gets nothing, and the advertiser is
         * not billed for a reward that was not earned (ADS-05, T-09-34).
         */
        private fun grantReward(
            outcome: RewardOutcome,
            kind: AssistKind,
        ): Boolean =
            when (outcome) {
                RewardOutcome.Granted -> {
                    allowance = allowance.refill(kind).spend(kind)
                    true
                }
                RewardOutcome.Unavailable -> {
                    allowance = allowance.refill(kind).spend(kind)
                    emitAdMessage(adUnavailableMessageFor(kind))
                    true
                }
                RewardOutcome.Declined -> {
                    clearPendingHint()
                    false
                }
            }

        /** The D-11 "ad unavailable, here it is anyway" copy for [kind] (ADS-03). */
        @StringRes
        private fun adUnavailableMessageFor(kind: AssistKind): Int =
            when (kind) {
                AssistKind.HINT -> R.string.ad_unavailable_hints
                AssistKind.UNDO -> R.string.ad_unavailable_undos
            }

        /**
         * Raises the one-shot ad snackbar carrying [res] (D-11).
         *
         * The monotonic ordinal is the event, the string id is only its text — the same idiom
         * this file already uses four times over ([GameUiState.undoTick],
         * [GameUiState.selectionTick], [GameUiState.deselectionTick],
         * [GameUiState.rejectedTapTick]), and for the same reason: the same message shown
         * twice in a row leaves no distinguishable state trace for a one-shot consumer.
         *
         * A tick rather than a second eventing mechanism (a one-shot `SharedFlow`) because it
         * survives configuration change with no replay bug, is trivially assertable, and does
         * not introduce a second way of doing events into a ViewModel that has exactly one.
         */
        private fun emitAdMessage(
            @StringRes res: Int,
        ) {
            _uiState.update { it.copy(adMessage = res, adMessageTick = it.adMessageTick + 1) }
        }

        /**
         * Reveals the move [onHintRequested] stashed in [pendingHint], once the assist it
         * represents has actually been paid for (ADS-06).
         *
         * The board is re-checked against [pendingHintBoard] even though the offer is modal:
         * the ad's return is an SDK callback whose timing the app does not control, and the
         * CR-01 discipline — a hint is never shown for a board the player has moved past — is
         * not relaxed merely because a dialog was in the way. The stash is cleared either
         * way, so a superseded move can never be revealed by a later grant (T-09-25).
         */
        private fun revealPendingHint() {
            val move = pendingHint ?: return
            val boardAtRequest = pendingHintBoard
            clearPendingHint()
            if (_uiState.value.board != boardAtRequest) return
            // D-09: counted HERE, past the null stash and past the staleness return — a
            // hint the player never saw is not a hint they used.
            countAssist(AssistKind.HINT)
            _uiState.update { it.copy(hintSource = move.from, hintTarget = move.to) }
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
            // D-09, and the deliberate MIRROR IMAGE of the allowance paragraph below: the
            // assist counters ARE reset by a restart. A retry may not refill free assists,
            // but it equally may not let a player take a hint, restart, and bank a
            // hint-free badge on the back of the second attempt.
            //
            // Because this function also calls clearSnapshot() below, the reset is durable:
            // unlike the allowance's accepted D-17 Option A hole, a restart cannot be undone
            // by a later restore, since the row the restore would read is gone.
            hintsUsed = NO_ASSISTS_USED
            undosUsed = NO_ASSISTS_USED
            // [allowance] is DELIBERATELY not reset here, and the omission is the D-04
            // mechanism rather than a forgotten line: retrying a level must not refill the
            // free assists, or "restart to farm three more hints" is a one-tap loophole.
            // Every sibling piece of per-game state above is cleared, so a reader will
            // otherwise read this absence as a bug — hence this comment.
            //
            // It has to live in the ViewModel instance to work at all, because the line
            // below calls clearSnapshot(): the stored row is deleted by a restart, so
            // counters that lived only on disk would be refilled by one. The ViewModel is
            // scoped to this Gameplay destination and survives the restart, so the counters
            // do too.
            //
            // The one accepted hole (D-17, Option A): restart and then force-kill the app
            // before leaving the screen, and the counters come back full. The snapshot is
            // only written on ON_STOP, so an ordinary background-and-return preserves them
            // correctly; closing this last gap would mean a second persistence mechanism
            // (DataStore) that D-17 explicitly rejected.
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
            // The five cue ordinals ride along for a different reason (and restartTick, the sixth,
            // advances here — the restart's own trace; the other three sites carry it): they are keys for
            // one-shot effects in the screen. A fresh 0 is a *changed* key for an effect
            // sitting at a higher value, so resetting them here would fire a cue nobody
            // triggered — a pop on restart, on a Challenge load or on Continue-resume
            // (RESEARCH Pitfall 2). Guarding on `> 0` in the screen is the second half of
            // that fix; both halves are required, and both are needed at all four of the
            // state-rebuild sites (this one, loadChallengeBoardIfNeeded, installClassicBoard
            // and applyRestored). The `= 0` defaults on the ordinals cannot be relied on to
            // cover a site that forgets one — a default is exactly what would hide it.
            //
            // pendingAdOffer / adOfferLoading are the deliberate opposite: they are NOT
            // carried, so the GameUiState.initial(board) rebuild clears them. An offer
            // dialog belongs to the position it was raised on, and a restart destroys that
            // position — accepting an ad for an assist on a board that no longer exists is
            // exactly the charge-for-nothing this plan exists to prevent. rejectedTapCell is
            // not carried either, at any of the four sites: it names a cell of the board being
            // replaced, while the rejectedTapTick ordinal beside it rides along (D-06, §3.7).
            if (activeChallengeLevel != null && boardGeneratorVersion != GeneratorConstants.GENERATOR_VERSION) {
                restartOnTheCurrentContract()
            } else {
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
                            adMessageTick = _uiState.value.adMessageTick,
                            // D-TB: a restart is the one thing a rebuilt state cannot otherwise show.
                            restartTick = _uiState.value.restartTick + 1,
                        )
            }
        }

        /**
         * The restart of a restored Challenge game whose board an older generator contract made
         * (docs/gdd.md §12.3(3), SB-V8; tech-lead brief §1(b)): the player chose to start the
         * level over, so they get the level as this build deals it — its current board and its
         * recorded line — through the same off-main-thread load a fresh entry takes, instead of
         * the stored board for ever. [resetToInitial] has already done the bookkeeping and cleared
         * the snapshot; a current-stamp or fresh game never comes here and keeps the
         * regeneration-free restart (D-06).
         *
         * The old board goes with [initialBoard], so the load window has nothing to save or reset
         * to (WR-01), and the screen shows the loading state, with the cue ordinals carried as at
         * every board install. The restart's own ordinal advances only once the level's board is
         * on screen, so its spoken line counts that board's pegs rather than the placeholder's. The
         * one cost, for this once-per-tester path: a Restart taken from the rescue banner under
         * TalkBack does not move focus to the board stop, because the banner leaves with the
         * loading state before the ordinal advances.
         */
        private fun restartOnTheCurrentContract() {
            initialBoard = null
            solution = null
            _uiState.value =
                GameUiState
                    .loading()
                    .copy(
                        showHints = _uiState.value.showHints,
                        hapticsOn = _uiState.value.hapticsOn,
                        selectionTick = _uiState.value.selectionTick,
                        deselectionTick = _uiState.value.deselectionTick,
                        undoTick = _uiState.value.undoTick,
                        rejectedTapTick = _uiState.value.rejectedTapTick,
                        adMessageTick = _uiState.value.adMessageTick,
                        restartTick = _uiState.value.restartTick,
                    )
            viewModelScope.launch {
                loadChallengeBoardIfNeeded()
                _uiState.update { it.copy(restartTick = it.restartTick + 1) }
            }
        }

        /**
         * The lifecycle status of [board]: [GameStatus.Playing] while a legal move
         * remains, else [GameStatus.Won] when the board passes its mode's finish test,
         * otherwise [GameStatus.Stuck] (GAME-02). One of the two places the mode decides the
         * finish (with `ResultsUiState.isSolved`; tech-lead brief §1(b)).
         *
         * **Challenge** ([activeChallengeLevel] set) asks [ChallengeClearRule.status]: a terminal
         * board with one to three pegs is cleared (Won, with three to one stars) and four or
         * more is stuck (docs/gdd.md R5, R6). The count is absolute and reads no `minimumPegs`.
         *
         * **Classic** keeps its own floor (R4, R8): the win test is the board's
         * `BoardSpec.minimumPegs`, not a hardcoded one peg (D-03/D-04, DEBT-02). For the four
         * cross boards that target IS one. Diamond-41 is the board it exists for: one peg is
         * proven UNREACHABLE there (the GF(4) colour invariant excludes all 41 single-peg
         * positions), and two is proven reachable, so a two-peg dead end is the best result the
         * board admits — a win, not a failure. Scoring it as Stuck would hide the perfect finish
         * behind the rescue banner and never award the 3 stars the re-anchored bands compute.
         * The comparison is `<=` rather than `==` so the status can never disagree with
         * [com.app.pegsolitaire.domain.scoring.ScoreCalculator.stars], whose 3-star band is also
         * `<= minimumPegs`.
         */
        private fun statusFor(board: Board): GameStatus =
            if (activeChallengeLevel != null) {
                ChallengeClearRule.status(board)
            } else {
                when {
                    !rulesEngine.isGameOver(board) -> GameStatus.Playing
                    board.pegCount <= board.spec.minimumPegs -> GameStatus.Won
                    else -> GameStatus.Stuck
                }
            }

        private companion object {
            /** The visible timer ticks once per second (D-04). */
            const val TIMER_TICK_MILLIS = 1_000L

            /** The timer counts whole seconds; the resume snapshot stores milliseconds. */
            const val MILLIS_IN_SECOND = 1_000L

            /** A fresh play — and a restarted one — has taken no hints and no undos (D-09). */
            const val NO_ASSISTS_USED = 0

            /** The peg count a "last peg on the target hole" finish requires (D-01). */
            const val SINGLE_PEG_REMAINING = 1
        }
    }
