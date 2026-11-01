package com.app.pegsolitaire.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.ChallengeProgress
import com.app.pegsolitaire.data.InterstitialCadenceStore
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.ProgressSubmitter
import com.app.pegsolitaire.domain.ads.InterstitialCadenceState
import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.scoring.ScoreParams
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.navigation.Results
import com.app.pegsolitaire.platform.ads.AdConstants
import com.app.pegsolitaire.platform.ads.AdGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Exposes the immutable [ResultsUiState] for the end-of-game Results destination
 * (SCOR-03) from the typed `Results` route args, with a star-tiered headline (D-08),
 * and persists the play's per-level best (PERS-01).
 *
 * The score/stars/pegs/time arrive already computed by the pure-domain
 * `ScoreCalculator` (02-01) as `Results` route args (read via [SavedStateHandle.toRoute])
 * and are passed through verbatim — nothing trust-sensitive is re-derived here
 * (T-02-06-ARG). Beyond the star-to-headline-tier mapping, the ViewModel's one side
 * effect is a single [ProgressRepository.recordResult] call: the repository merges each
 * best independently (D-07) and reports which fields improved, which becomes the
 * "New best!" cue on the screen (D-09).
 *
 * Three guards keep that write honest:
 *  - **Only a solved play is recorded (D-08).** A Classic board that did not fold to its own
 *    `BoardSpec.minimumPegs` floor, or a Challenge route past the clear threshold, reaches
 *    Results in its stuck/Retry state (D-10) and must not leave a stored best behind for a
 *    level that was never solved. The gate is the state's one solved test
 *    ([ResultsUiState.isSolved]): Classic's floor is per-board and rides on the route —
 *    Diamond-41's is a proven TWO pegs — so it is `pegsRemaining <= minimumPegs` and never a
 *    hardcoded one peg (CR-01); Challenge's is `ChallengeClearRule` (GD-SB-14).
 *  - **It is recorded at most once (T-05-09).** The guard flag lives in the
 *    [SavedStateHandle], so neither a configuration change (same instance) nor a
 *    process-death restore (new instance, same handle) can double-count the play.
 *  - **The Play Games submission rides that same latch, rather than adding a fourth
 *    (D-13/PGSL-04).** [ProgressSubmitter.onResultRecorded] is called once, immediately
 *    after [ProgressRepository.recordResult] returns and the screen's own state is
 *    committed — inside the [RECORDED_KEY] guard, so a configuration change, a
 *    process-death restore and a back navigation onto the same entry all bank and submit
 *    nothing, because none of them gets past the latch. **This is why the hook is here and
 *    not in `GameViewModel`**: that ViewModel's `finishedEmitted` latch is reset by
 *    `performUndo()` when the undone board returns to `Playing`, so an undo/re-jump cycle
 *    re-emits the finish and a hook there would resubmit on every cycle. [RECORDED_KEY] is
 *    the only latch in this codebase that survives both configuration change and process
 *    death, which no `LaunchedEffect` does. The call is deliberately **not** wrapped: it is
 *    non-suspend and its whole body is a launch on the application scope, so it can neither
 *    throw a submission failure at this coroutine nor delay it — and a defensive
 *    `runCatching` here would disguise a regression in exactly that shape rather than guard
 *    against one.
 *  - **The celebration sounds at most once (D-16).** The star-pop ceremony on the
 *    Results screen gets its own latch in the same [SavedStateHandle], for the same
 *    reason and with the same reach: neither a configuration change (same instance) nor
 *    a process-death restore / back navigation (new instance, same handle) can sound it
 *    twice. It is a deliberately *separate* key from the record guard above — that one
 *    is set only for a solved play, whereas every result celebrates its stars, including
 *    a stuck one-star finish that is never recorded.
 *  - **The Challenge level is clamped at this boundary (WARN-03, T-06-20).** `Results`
 *    is a typed route in `AppNavHost` reachable *independently* of `GameRoute`, so its
 *    `challengeLevel` arg is untrusted in its own right rather than merely downstream of
 *    `GameViewModel`'s clamp. It is coerced once into the `:domain`
 *    [ChallengeLevelBounds] range and every read below — the headline tier, the exposed
 *    [ResultsUiState.challengeLevel], the level [onNextLevelTapped] resolves the next one
 *    from, the mode branch, and the database level key — uses that one value. A forged or
 *    out-of-range arg therefore cannot reach the stored key or the "Next level" action.
 *  - **The solve telemetry is clamped at the same boundary (D-08/D-09, T-12-23).** The
 *    three `Results` telemetry args pass through [SolveTelemetry.coerced] exactly once
 *    into [solveTelemetry]; the raw args are never read again. Absent args read as
 *    [SolveTelemetry.NONE] and a forged counter is bounded rather than refused, on the
 *    same reasoning as the Challenge level above.
 *  - **The win is banked at most once (INT-02).** The interstitial cadence counts wins,
 *    and a re-entry into the same result is not a second win. A THIRD
 *    [SavedStateHandle] latch — [WIN_COUNTED_KEY] — guards [claimWinOnce] with the same
 *    guard-before-effect ordering as the two above, so neither a configuration change nor
 *    a process-death restore can inflate the counter that decides how often ads appear.
 *  - **An interstitial is decided and shown only on a continue tap (INT-01).** Nothing in
 *    construction shows an ad; [onNextLevelTapped] and [onCatalogTapped] are the only
 *    entry points that can, and they are the only two D-06 admits (see
 *    [ResultsNavTarget]). Construction may at most *warm* one (D-12).
 *
 * @see ChallengeLevelBounds.clamp for why the policy is fail-safe rather than a reject.
 *
 * Replay re-plays the played `levelId`. Classic has no "next board" (a Challenge-mode
 * concept), so no catalog lookup is needed.
 *
 * ## Navigation timing lives here, not in the composable
 *
 * The forward-moving win actions do NOT call their navigation lambdas from `onClick` any
 * more: they call into this ViewModel, which decides whether an ad presents first and
 * publishes [ResultsUiState.pendingNavigation] when the tap's intent is due to be honoured
 * (D-09). That is the structural reason an interstitial can never appear over a fresh
 * board — the next `GameViewModel` is not constructed until the route consumes that field.
 *
 * The *destination* of the Challenge continue action lives here for the same reason
 * (**D-04a**, see [onNextLevelTapped]): it is a read of the ladder, and a stateless leaf
 * composable has no business holding one.
 *
 * @param savedStateHandle carries the typed `Results(...)` route args and the
 *   already-recorded guard.
 * @param progressRepository the single source of truth for per-level bests.
 * @param progressSubmitter the Play Games submission orchestrator (D-13) — positioned
 *   after [progressRepository] because it is that call's consequence, and reached through
 *   exactly one fire-and-forget line. Nothing here ever names a leaderboard or achievement
 *   ID; the submitter resolves every one of them behind this seam.
 * @param challengeProgress the Challenge ladder, read — never written — by
 *   [onNextLevelTapped] (D-04a). The write for this solve is `GameViewModel`'s
 *   `advancePast(N)`; this screen only asks where the ladder now stands.
 * @param cadenceStore the memory of the interstitial cadence (INT-02) — banks the win and
 *   records a presentation; it never decides anything, [InterstitialCadenceState] does.
 * @param adGateway the ad seam: readiness to decide with, a warm-up to call at eligibility
 *   (D-12), and a presented/not-presented show to sequence navigation around (D-09).
 */
@HiltViewModel
class ResultsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val progressRepository: ProgressRepository,
        private val progressSubmitter: ProgressSubmitter,
        private val challengeProgress: ChallengeProgress,
        private val cadenceStore: InterstitialCadenceStore,
        private val adGateway: AdGateway,
    ) : ViewModel() {
        private val args = savedStateHandle.toRoute<Results>()

        /**
         * The one clamped Challenge level for this result, or `null` for Classic — the
         * WARN-03 boundary. Everything level-keyed below reads THIS and never the raw
         * route arg again — this line is its only read — mirroring the "one clamped
         * value" structure `GameViewModel` uses. The null stays null so a Classic play
         * is never mis-recorded as Challenge level 1.
         */
        private val challengeLevel: Int? = args.challengeLevel?.let(ChallengeLevelBounds::clamp)

        /**
         * The finishing board's perfect-finish floor, clamped once at this boundary — the
         * same WARN-03 treatment [challengeLevel] gets, for the same reason.
         *
         * The clamp ceiling is [BoardConstants.HIGHEST_MINIMUM_PEGS] (the highest floor any
         * shipped board declares), **never** the play's own `pegsRemaining`: clamping
         * against `pegsRemaining` would make every forged value satisfy
         * `pegsRemaining <= minimumPegs` and hand a stored best, a `play_session` row and a
         * leaderboard submission to a board that was never played. The floor is
         * [BoardConstants.MINIMUM_PEGS_DEFAULT], so a forged zero or negative cannot make
         * the solved test unsatisfiable either.
         */
        private val minimumPegs: Int =
            args.minimumPegs.coerceIn(
                BoardConstants.MINIMUM_PEGS_DEFAULT,
                BoardConstants.HIGHEST_MINIMUM_PEGS,
            )

        /**
         * This play's per-solve telemetry, resolved ONCE from the route args at the same
         * boundary — and by the same idiom — as [challengeLevel] above (D-08/D-09).
         *
         * **Coerced here and only here.** These three lines are the sole read of
         * `args.hintsUsed` / `args.undosUsed` / `args.lastPegOnTarget`; every later use is
         * of this value. `Results` is registered in `AppNavHost` *independently* of
         * `GameRoute` and its args survive process death through the [SavedStateHandle], so
         * they are untrusted in their own right rather than merely downstream of
         * `GameViewModel`'s counters — exactly the WARN-03 argument the Challenge level
         * already makes.
         *
         * **Clamped, never refused, never fatal (threat T-12-23).**
         * [SolveTelemetry.coerced] bounds the counters at zero and passes a large forged
         * value through untouched. Refusing a forged arg would convert a one-byte edit into
         * a crashed Results screen and a lost recorded best for an honest player, in
         * exchange for denying a cheat a cosmetic badge — the identical argument
         * `SnapshotMapper` makes for the free-assist allowance. A route reached without the
         * args reads as [SolveTelemetry.NONE], because the three are defaulted primitives.
         *
         * `internal` rather than `private` for two reasons: [recordSolvedPlayOnce] passes it
         * to `ProgressRepository.recordResult`, which writes it to `play_session` in the
         * same transaction as the best row (D-08); and exposing the *resolved* value is what
         * lets the arg-clamping boundary be asserted directly — the same narrow precedent
         * `GameViewModel.isOnRecordedLine()` sets.
         *
         * It is passed through **verbatim**: the coercion happened here, once, at the nav
         * boundary, and the repository must not re-do or second-guess it.
         */
        internal val solveTelemetry: SolveTelemetry =
            SolveTelemetry.coerced(
                hintsUsed = args.hintsUsed,
                undosUsed = args.undosUsed,
                lastPegOnTarget = args.lastPegOnTarget,
            )

        /**
         * The state the screen renders, built once from the clamped route args — and the one place
         * this ViewModel's solved test comes from. The state's own [ResultsUiState.isSolved] is the
         * single solved test of the Results feature (CR-01, docs/gdd.md GD-SB-14), so the result is
         * built first and ranked second: the headline is chosen from that very state's answer, and
         * [isSolved] and [headlineTier] below are reads of it. Holding a second copy of the
         * expression here, as this class did until 3.0.0, is how three `pegsRemaining == 1`
         * comparisons across two files let the Diamond-41 gap survive: the copy the player read said
         * "Perfect!" while the gate a few lines away called the same play unsolved.
         */
        private val _uiState =
            MutableStateFlow(
                ResultsUiState(
                    stars = args.stars,
                    pegsRemaining = args.pegsRemaining,
                    elapsedSeconds = args.elapsedSeconds,
                    score = args.score,
                    // Replaced on the next line by the rank of this same state's solved test.
                    headlineTier = ResultsHeadlineTier.STUCK,
                    replayId = args.levelId,
                    challengeLevel = challengeLevel,
                    minimumPegs = minimumPegs,
                ).let { result ->
                    result.copy(
                        headlineTier =
                            tierFor(
                                stars = result.stars,
                                isSolved = result.isSolved,
                                challengeLevel = result.challengeLevel,
                            ),
                    )
                },
            )

        /** Read-only state stream for the UI; the mutable flow never leaks (state hoisting). */
        val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

        /**
         * Whether this play solved its board — read from the state, never re-derived. Two more
         * answers read it: the record gate ([recordSolvedPlayOnce], which also decides whether the
         * play is submitted to Play Games) and, through the state, [ResultsUiState.showNextLevel].
         */
        private val isSolved: Boolean = _uiState.value.isSolved

        /**
         * This result's outcome bucket, read ONCE from the state and shared by the state the screen
         * renders and the ad-eligibility gate below. Sharing one value is what makes the headline and
         * the gate structurally incapable of disagreeing (see [isInterstitialEligible]); calling
         * [tierFor] twice would allow exactly that.
         */
        private val headlineTier: ResultsHeadlineTier = _uiState.value.headlineTier

        /** Whether this finish is the kind of win D-14 lets an interstitial follow (INT-03). */
        private val interstitialEligible: Boolean = isInterstitialEligible(headlineTier)

        /**
         * The cadence this Results entry decides from — the persisted state with THIS win
         * already projected onto it, computed once at construction by [claimWinOnce].
         *
         * The decision deliberately reads this rather than re-reading
         * `cadenceStore.state.value` at tap time. Banking the win is a fire-and-forget
         * DataStore write, so the store's flow may not have re-emitted by the moment the
         * player taps — a decision taken from it would differ between a fast tap and a slow
         * one, which is a cadence that depends on disk latency. Projecting the win locally,
         * once, makes this entry's answer deterministic. It is also the *same* value the
         * D-12 preload decision used, so the warm-up and the show can never disagree about
         * whether this result was eligible.
         */
        private val cadenceForThisResult: InterstitialCadenceState = claimWinOnce()

        /**
         * Whether a continue tap has already been accepted for this Results entry.
         *
         * A second tap on a screen that is already leaving must do nothing (D-09 — the
         * tap's intent is honoured exactly once). The scrim swallows input for the whole ad
         * path, so this guard covers the fast path and the instant between the tap and the
         * scrim rendering. It is plain instance state rather than a [SavedStateHandle]
         * latch on purpose: it guards one screen's lifetime, not one *result's* lifetime —
         * a genuine re-entry into Results has to be tappable again.
         */
        private var continueClaimed = false

        init {
            recordSolvedPlayOnce()
            claimCelebrationPlayOnce()
        }

        /**
         * Challenge "Next level" — one of the two D-06 forward-moving win actions — which
         * serves **`max(ladder head, N + 1)`** and takes no number from the screen (**D-04a**).
         *
         * ## Why the ladder decides, and not `N + 1`
         *
         * D-04 deliberately keeps a stale in-progress Challenge game when a merged cloud save
         * moves the ladder ahead of it, so `N` can be *behind* the head: resumed level 11 under
         * a merged head of 13. `N + 1` then points at a level the player already completed on
         * the other device, and every "Next level" after it points at another one — `head − N − 1`
         * consecutive replays, each ending on a Results screen that can award no new star and
         * move no ladder (D-04's own worked example is a head of 50 under a resumed level 10).
         * The ladder head *is* the Challenge navigation: it is what the Home tile shows and what
         * Continue resumes, so a forward action that disagrees with it is the one thing the mode
         * cannot do.
         *
         * ## Why `N + 1` stays inside the `max`
         *
         * Serving the head alone would make "the player loops forever on one board" reachable:
         * if the ladder read landed before `GameViewModel.advancePast(N)` committed, the head
         * would still be `N` and the tap would re-serve the level just finished. The `max` makes
         * that state unreachable by construction rather than unlikely — and this repo already
         * carries a documented ladder/load-window race (WR-01), so the guarantee is worth having
         * even though the read below is ordered.
         *
         * ## Why the read is [ChallengeProgress.settledLevel] and not `currentLevel.value`
         *
         * `currentLevel` is a `stateIn` cache refreshed by a separate coroutine, so its `.value`
         * can still hold the pre-solve ladder at the moment of the tap; `settledLevel()` reads
         * what DataStore has committed. That is what orders this read *after* the solve's
         * `advancePast(N)` rather than after whenever the cache happens to re-emit — the same
         * distinction, and the same reason, as `ProgressSubmitter`'s push.
         *
         * The result is clamped by [ChallengeLevelBounds], so neither a ladder stored out of
         * range nor `N + 1` at the ceiling can name a level the generator refuses.
         *
         * **The one exception the `max` does not cover, named on purpose.** Because the clamp
         * is `coerceIn(MIN, MAX)`, at `N == ChallengeLevelBounds.MAX` it returns `MAX` — so the
         * "never re-serves the level just played" guarantee above holds for `N < MAX` only, and
         * the very last level re-serves itself. That is the correct behaviour for a *bound*
         * (there is no level above the ceiling to serve, and fail-safe clamping beats crashing
         * — see [ChallengeLevelBounds.clamp]) and it is unreachable in practice: the ceiling is
         * `GeneratorConstants.MAX_CHALLENGE_LEVEL`, 10,000, chosen to sit well above any level a
         * player can hand-reach. Anyone tightening the `max` should not treat this as a bug to
         * fix, and anyone lowering the ceiling to a reachable number must revisit it.
         *
         * **Nothing is shown for any of this.** D-03 (silent restore) stands unamended and the
         * label carries no number, so no promise of `N + 1` is ever made on screen to be broken.
         *
         * Classic reaches this only by a bug — the action is not rendered — and answers with
         * nothing rather than inventing Challenge level 1.
         */
        fun onNextLevelTapped() {
            val solvedLevel = challengeLevel ?: return
            if (!claimContinue()) return
            viewModelScope.launch {
                val ladderHead = challengeProgress.settledLevel()
                val nextLevel = ChallengeLevelBounds.clamp(maxOf(ladderHead, solvedLevel + 1))
                dispatchContinue(ResultsNavTarget.NextLevel(nextLevel))
            }
        }

        /** Classic "Catalog" after a win — the other D-06 forward-moving win action. */
        fun onCatalogTapped() {
            if (!claimContinue()) return
            dispatchContinue(ResultsNavTarget.Catalog)
        }

        /**
         * Clears the consumed navigation target, for the route to call once it has acted on
         * it. Pairs with [ResultsUiState.pendingNavigation] being state rather than an
         * event: the field survives a stopped collector, and this is what stops it being
         * re-consumed when collection resumes (RESEARCH Pitfall 6).
         */
        fun onNavigationHandled() {
            _uiState.update { it.copy(pendingNavigation = null) }
        }

        /**
         * Claims this Results entry's single continue tap, answering whether the caller owns it.
         *
         * A second tap on a screen that is already leaving must do nothing (D-09 — the tap's
         * intent is honoured exactly once), and the claim is taken **synchronously on the tap**
         * rather than inside the coroutine that resolves the destination: the Challenge path
         * suspends on a ladder read, and a claim behind that suspension would let two taps in
         * the same frame both pass.
         */
        private fun claimContinue(): Boolean {
            if (continueClaimed) return false
            continueClaimed = true
            return true
        }

        /**
         * Routes an already-claimed continue tap down one of exactly two paths.
         *
         * The fast path (D-10) publishes [ResultsUiState.pendingNavigation] on the same turn
         * as the tap, with no scrim and no delay: the quiescence pause is part of
         * *presenting an ad*, never part of navigating, so a player who cannot be served one
         * is a player who is never delayed. The ad path defers both to
         * [presentThenNavigate].
         */
        private fun dispatchContinue(target: ResultsNavTarget) {
            if (shouldShowInterstitial()) {
                viewModelScope.launch { presentThenNavigate(target) }
            } else {
                _uiState.update { it.copy(pendingNavigation = target) }
            }
        }

        /**
         * Whether this tap should present an interstitial — three clauses, all of which must
         * hold, and none of which waits for anything.
         *
         *  1. [interstitialEligible] — D-14 / INT-03: this finish is a solved one.
         *  2. `adGateway.interstitialReady.value` — D-10 / D-11: an ad is *already held*. No
         *     ad means a silent skip, never a wait, and never a pause with nothing behind it.
         *  3. [InterstitialCadenceState.mayShow] — D-01..D-04: the cadence permits it. This
         *     is the single place the cadence is asked, and it is asked once per tap.
         *
         * It never awaits a load and it never blocks, which is what makes the `false` answer
         * cost the player literally nothing.
         */
        private fun shouldShowInterstitial(): Boolean =
            interstitialEligible &&
                adGateway.interstitialReady.value &&
                cadenceForThisResult.mayShow(System.currentTimeMillis())

        /**
         * Raises the scrim, pauses, presents the ad, records it if it really appeared, and
         * then honours the tap — in that order (D-08, D-09, D-11, INT-04).
         *
         * The ordering follows `GameViewModel.onAcceptAdOffer`: state up FIRST, then the
         * suspending gateway call inside `try`, then the resolution in `finally`.
         *
         * **Both the scrim teardown and the navigation live in the same `finally`, and that
         * is the mechanism rather than tidiness.** It is what makes D-09's "the tap's intent
         * is honoured exactly once either way" true on *every* exit — a dismissal, a show
         * failure, an exception, a cancelled `viewModelScope` — and it is what guarantees
         * the scrim can never be left up over a live screen with no ad coming. Splitting
         * them, or moving the navigation into the `try`, reintroduces both failures at once.
         *
         * The recording takes a FRESH `System.currentTimeMillis()` rather than reusing the
         * decision timestamp: the ad occupied real time, and stamping the later instant
         * makes the next 90-second gate strictly more conservative.
         */
        private suspend fun presentThenNavigate(target: ResultsNavTarget) {
            _uiState.update { it.copy(adScrimVisible = true) }
            try {
                delay(AdConstants.INTERSTITIAL_QUIESCENCE_MS)
                if (adGateway.showInterstitial()) {
                    cadenceStore.recordInterstitialShown(System.currentTimeMillis())
                }
            } finally {
                _uiState.update { it.copy(adScrimVisible = false, pendingNavigation = target) }
            }
        }

        /**
         * Banks this result's win exactly once and warms an ad if the next continue tap
         * could show one (D-12), answering with the cadence this entry will decide from.
         *
         * Guard-before-effect, like its two sibling latches: [WIN_COUNTED_KEY] goes up
         * before anything is written, so a second ViewModel over the same handle — a
         * configuration change, a process-death restore, a back navigation onto the same
         * Results entry — banks nothing and warms nothing. A non-eligible finish is not a
         * win at all (INT-03), so it neither counts nor preloads.
         *
         * The preload is conditioned on the *projected* state's [InterstitialCadenceState
         * .mayShow], which is exactly D-12: warm an ad at the moment a win makes the next
         * continue tap ad-eligible, and never while first-session suppression, the session
         * cap or the level and time gates make one impossible. Issuing the request is itself
         * the exposure the gates exist to avoid, so "load it just in case" is not a cheaper
         * option.
         *
         * Written as one `when` expression rather than a chain of early returns because
         * detekt's `ReturnCount` is a build gate here — `GameViewModel` carries an in-source
         * note about a function restructured for the same reason.
         */
        private fun claimWinOnce(): InterstitialCadenceState =
            when {
                !interstitialEligible -> cadenceStore.state.value
                savedStateHandle.get<Boolean>(WIN_COUNTED_KEY) == true -> cadenceStore.state.value
                else -> {
                    savedStateHandle[WIN_COUNTED_KEY] = true
                    cadenceStore.recordWin()
                    val projected = cadenceStore.state.value.recordWin()
                    if (projected.mayShow(System.currentTimeMillis())) adGateway.preloadInterstitial()
                    projected
                }
            }

        /**
         * Merges this play into the level's stored bests exactly once — writing its
         * [solveTelemetry] to `play_session` in the same transaction (D-08) — then surfaces
         * the returned per-field flags as the one-shot "New best!" cue (D-09).
         *
         * Returns without writing when the play was not a solve (D-08) or when this play
         * has already been recorded — the flag is set *before* the write is launched, so
         * a second ViewModel built over the same handle cannot race a duplicate in.
         */
        private fun recordSolvedPlayOnce() {
            if (!isSolved) return
            if (savedStateHandle.get<Boolean>(RECORDED_KEY) == true) return
            savedStateHandle[RECORDED_KEY] = true

            val mode =
                if (challengeLevel != null) {
                    ProgressPersistenceKeys.MODE_CHALLENGE
                } else {
                    ProgressPersistenceKeys.MODE_CLASSIC
                }
            val levelId = challengeLevel?.toString() ?: args.levelId

            viewModelScope.launch {
                val newBests =
                    progressRepository.recordResult(
                        mode = mode,
                        levelId = levelId,
                        result = scoreResultFromArgs(),
                        telemetry = solveTelemetry,
                        nowMs = System.currentTimeMillis(),
                    )
                _uiState.update { it.copy(newBests = newBests) }
                progressSubmitter.onResultRecorded()
            }
        }

        /**
         * Claims this entry's right to sound the star-pop celebration, so it plays exactly
         * once per result (D-16).
         *
         * Mirrors [recordSolvedPlayOnce]'s shape, ordering discipline included: the guard is
         * set *before* [ResultsUiState.playCelebration] is raised, which is what makes a
         * second ViewModel built over the same handle unable to race a second celebration
         * in. Both re-entry paths D-16 names — a process-death restore and a back navigation
         * onto the same Results entry — arrive as exactly that second construction, observe
         * the claim, and leave the flag at its `false` default.
         *
         * Unlike the bests merge this is deliberately not conditioned on a solve: the stars
         * are shown for every result, so they are sounded for every result.
         */
        private fun claimCelebrationPlayOnce() {
            if (savedStateHandle.get<Boolean>(CELEBRATED_KEY) == true) return
            savedStateHandle[CELEBRATED_KEY] = true
            _uiState.update { it.copy(playCelebration = true) }
        }

        /**
         * Rebuilds the domain [ScoreResult] from the route args so the repository merges
         * the same numbers the screen displays. `pegsCleared` is not carried by the route
         * (the Results screen never shows it) and is not part of any stored best, so it is
         * filled with [PEGS_CLEARED_NOT_CARRIED] rather than being re-derived here.
         */
        private fun scoreResultFromArgs(): ScoreResult =
            ScoreResult(
                stars = args.stars,
                score = args.score,
                pegsRemaining = args.pegsRemaining,
                pegsCleared = PEGS_CLEARED_NOT_CARRIED,
                elapsedSeconds = args.elapsedSeconds,
            )

        private companion object {
            /** SavedStateHandle flag marking this play as already merged into the stored bests. */
            const val RECORDED_KEY = "results_recorded"

            /** SavedStateHandle flag marking this result's celebration as already claimed (D-16). */
            const val CELEBRATED_KEY = "results_celebrated"

            /** SavedStateHandle flag marking this result's win as already banked (INT-02). */
            const val WIN_COUNTED_KEY = "results_win_counted"

            /** The `Results` route does not carry pegsCleared, and no stored best uses it. */
            const val PEGS_CLEARED_NOT_CARRIED = 0

            /**
             * Maps the terminal outcome to its headline bucket (D-08; docs/gdd.md GD-SB-07). For
             * a Challenge result the headline must agree with the offered action (WR-03): a route
             * that did NOT clear is stuck regardless of star count, so it reads "Out of moves"
             * (STUCK) over its Retry action instead of a contradictory "Solved!" — the defensive
             * branch, since play keeps a stuck board on the board (R6). Otherwise the top band
             * (>= [ScoreParams.THREE_STARS]) is PERFECT and the middle band
             * ([ScoreParams.TWO_STARS]) is SOLVED; below them a Challenge clear is CLEARED
             * ("Cleared!", one star, a pass that is not a solve) and Classic's 1-star floor stays
             * STUCK (star-only headline D-08; unreachable in play, R4).
             *
             * [isSolved] is passed in rather than recomputed from a peg count so this
             * branch, the record gate and [ResultsUiState.showNextLevel] are the same
             * answer by construction (CR-01).
             */
            fun tierFor(
                stars: Int,
                isSolved: Boolean,
                challengeLevel: Int?,
            ): ResultsHeadlineTier =
                when {
                    challengeLevel != null && !isSolved -> ResultsHeadlineTier.STUCK
                    stars >= ScoreParams.THREE_STARS -> ResultsHeadlineTier.PERFECT
                    stars == ScoreParams.TWO_STARS -> ResultsHeadlineTier.SOLVED
                    challengeLevel != null -> ResultsHeadlineTier.CLEARED
                    else -> ResultsHeadlineTier.STUCK
                }

            /**
             * Whether a finish in [tier] is the kind of win an interstitial may follow
             * (D-14, INT-03).
             *
             * D-14 defines an interstitial-eligible win as **any solved finish — two or
             * three stars, and in Challenge a one-star clear too (GD-SB-16)** — not only the
             * strict `GameStatus.Won` / three-star condition.
             * Deriving it from the already-computed [headlineTier] rather than from the raw
             * args means the gate and the copy the player reads can never disagree: a screen
             * that says "Solved!" is ad-eligible and a screen that says "Out of moves" is
             * not. It also inherits [tierFor]'s Challenge-stuck branch for free, so a stuck
             * Challenge board is excluded *by construction* rather than by a second rule
             * that could drift from the first.
             *
             * Two traps this deliberately avoids, both of which look like improvements:
             *
             *  - It is **not** `stars == ScoreParams.THREE_STARS`. That is the strict-win
             *    test, and using it here would deny an interstitial after every two-star
             *    Classic solve — a finish D-14 explicitly includes.
             *  - It must **never** be written as a `pegsRemaining == 1` comparison. That
             *    test is wrong for Challenge since 3.0.0 (one to three pegs clear, R5, while
             *    every generated footprint's own floor is one, I5); used anywhere Classic can
             *    reach it, it misclassifies a
             *    **perfect Diamond-41 finish as a loss**, because Diamond-41's proven floor
             *    is two pegs (Phase 8, GF(4) colour invariant) — the player who played it
             *    flawlessly would be the one permanently denied. That is not hypothetical:
             *    the record gate in this file was written that way and silently dropped
             *    every Diamond-41 win until CR-01. The per-board answer is [isSolved], which
             *    compares against the route's clamped `minimumPegs`.
             *
             * T-10-06 (accepted, not mitigated): `Results` is an independently reachable
             * typed route, so a forged `stars = 3` arg could make a never-played result
             * ad-eligible. No privilege is gained — the "attacker" chooses to watch an ad —
             * and `challengeLevel` stays clamped by the WARN-03 boundary, so no forged value
             * reaches a stored key or the ladder. The risk is accepted deliberately.
             */
            fun isInterstitialEligible(tier: ResultsHeadlineTier): Boolean = tier != ResultsHeadlineTier.STUCK
        }
    }
