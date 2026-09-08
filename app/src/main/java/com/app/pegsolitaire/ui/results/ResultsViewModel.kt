package com.app.pegsolitaire.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.InterstitialCadenceStore
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.domain.ads.InterstitialCadenceState
import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
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
 *  - **Only a solved play is recorded (D-08).** A board that did not fold to a single
 *    peg reaches Results in its stuck/Retry state (D-10) and must not leave a stored
 *    best behind for a level that was never solved.
 *  - **It is recorded at most once (T-05-09).** The guard flag lives in the
 *    [SavedStateHandle], so neither a configuration change (same instance) nor a
 *    process-death restore (new instance, same handle) can double-count the play.
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
 *    [ResultsUiState.challengeLevel] that drives `onNextLevel(challengeLevel + 1)`, the
 *    mode branch, and the database level key — uses that one value. A forged or
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
 * @param savedStateHandle carries the typed `Results(...)` route args and the
 *   already-recorded guard.
 * @param progressRepository the single source of truth for per-level bests.
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
         * This result's outcome bucket, computed ONCE and shared by the state the screen
         * renders and the ad-eligibility gate below. Sharing one value is what makes the
         * headline and the gate structurally incapable of disagreeing (see
         * [isInterstitialEligible]); calling [tierFor] twice would allow exactly that.
         */
        private val headlineTier: ResultsHeadlineTier =
            tierFor(
                stars = args.stars,
                pegsRemaining = args.pegsRemaining,
                challengeLevel = challengeLevel,
            )

        /** Whether this finish is the kind of win D-14 lets an interstitial follow (INT-03). */
        private val interstitialEligible: Boolean = isInterstitialEligible(headlineTier)

        private val _uiState =
            MutableStateFlow(
                ResultsUiState(
                    stars = args.stars,
                    pegsRemaining = args.pegsRemaining,
                    elapsedSeconds = args.elapsedSeconds,
                    score = args.score,
                    headlineTier = headlineTier,
                    replayId = args.levelId,
                    challengeLevel = challengeLevel,
                ),
            )

        /** Read-only state stream for the UI; the mutable flow never leaks (state hoisting). */
        val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

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
         * Challenge "Next level" — one of the two D-06 forward-moving win actions.
         *
         * [nextLevel] comes from the screen, which computes it off the clamped
         * [ResultsUiState.challengeLevel], so the WARN-03 boundary still holds.
         */
        fun onNextLevelTapped(nextLevel: Int) {
            continueWith(ResultsNavTarget.NextLevel(nextLevel))
        }

        /** Classic "Catalog" after a win — the other D-06 forward-moving win action. */
        fun onCatalogTapped() {
            continueWith(ResultsNavTarget.Catalog)
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
         * Accepts a continue tap and routes it down one of exactly two paths.
         *
         * The fast path (D-10) publishes [ResultsUiState.pendingNavigation] on the same turn
         * as the tap, with no scrim and no delay: the quiescence pause is part of
         * *presenting an ad*, never part of navigating, so a player who cannot be served one
         * is a player who is never delayed. The ad path defers both to
         * [presentThenNavigate].
         */
        private fun continueWith(target: ResultsNavTarget) {
            if (continueClaimed) return
            continueClaimed = true

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
            if (args.pegsRemaining != PEGS_REMAINING_SOLVED) return
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
            /** A solved board leaves exactly one peg; a Challenge board that did not is stuck. */
            const val PEGS_REMAINING_SOLVED = 1

            /** SavedStateHandle flag marking this play as already merged into the stored bests. */
            const val RECORDED_KEY = "results_recorded"

            /** SavedStateHandle flag marking this result's celebration as already claimed (D-16). */
            const val CELEBRATED_KEY = "results_celebrated"

            /** SavedStateHandle flag marking this result's win as already banked (INT-02). */
            const val WIN_COUNTED_KEY = "results_win_counted"

            /** The `Results` route does not carry pegsCleared, and no stored best uses it. */
            const val PEGS_CLEARED_NOT_CARRIED = 0

            /**
             * Maps the terminal outcome to its headline bucket (D-08). For a Challenge
             * result the headline must agree with the offered action (WR-03): a board
             * that did NOT fold to a single peg is stuck regardless of star count, so a
             * 2-star Challenge board left with 2-3 pegs reads "Out of moves" (STUCK) over
             * its Retry action instead of a contradictory "Solved!". Otherwise (and for
             * Classic, [challengeLevel] `null`, star-only headline D-08): the top band
             * (>= [ScoreParams.THREE_STARS]) is PERFECT, the middle band
             * ([ScoreParams.TWO_STARS]) is SOLVED, everything else (the 1-star floor) STUCK.
             */
            fun tierFor(
                stars: Int,
                pegsRemaining: Int,
                challengeLevel: Int?,
            ): ResultsHeadlineTier =
                when {
                    challengeLevel != null && pegsRemaining != PEGS_REMAINING_SOLVED ->
                        ResultsHeadlineTier.STUCK
                    stars >= ScoreParams.THREE_STARS -> ResultsHeadlineTier.PERFECT
                    stars == ScoreParams.TWO_STARS -> ResultsHeadlineTier.SOLVED
                    else -> ResultsHeadlineTier.STUCK
                }

            /**
             * Whether a finish in [tier] is the kind of win an interstitial may follow
             * (D-14, INT-03).
             *
             * D-14 defines an interstitial-eligible win as **any solved finish — two or
             * three stars** — not only the strict `GameStatus.Won` / three-star condition.
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
             *  - It must **never** be written as a `pegsRemaining == 1` /
             *    [PEGS_REMAINING_SOLVED] comparison. That constant is correct where it is
             *    already used (every Challenge footprint has `minimumPegs == 1`) and it sits
             *    a few lines away in this very file, which is exactly why it is the line a
             *    developer would copy into a Classic gate. Doing so would misclassify a
             *    **perfect Diamond-41 finish as a loss**, because Diamond-41's proven floor
             *    is two pegs (Phase 8, GF(4) colour invariant) — the player who played it
             *    flawlessly would be the one permanently denied.
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
