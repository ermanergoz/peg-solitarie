package com.app.pegsolitaire.data

import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.domain.achievement.AchievementOutcome
import com.app.pegsolitaire.domain.achievement.AchievementRules
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.platform.games.GamesIds
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.GamesSignInState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single orchestrator that turns stored progress into Play Games submissions (D-13).
 *
 * It owns the persisted "submissions pending" flag ([SubmissionSyncStore]), reads the SQL
 * aggregates ([ProgressRepository.aggregate] / [ProgressRepository.classicBests]), applies
 * [AchievementRules] once (D-11), and issues every call through [GamesService]. There are
 * exactly two triggers — a solved level ([onResultRecorded], from `ResultsViewModel`) and a
 * session resolving to [GamesSignInState.SignedIn] ([start]'s collector) — and they share
 * one pass, so the offline backfill has no separate code path (PGSL-05).
 *
 * **Nothing in `ui/` names a leaderboard or achievement ID.** [GamesIds] is read here and in
 * `platform/games/` and nowhere else, which is what makes D-13's containment structural
 * rather than a convention. Note the layering this depends on: `data/` → `platform.games`
 * is legal (`PlatformBoundaryRules` forbids only gms/UMP/ads SDK types and `Activity` names
 * outside `..platform..`), and no Play Games SDK type appears in any signature here.
 *
 * **No Hilt module entry exists or is needed.** A `@Singleton` with an `@Inject`
 * constructor is constructible by Hilt on its own — the [ChallengeProgress] precedent — so
 * a reader should not go looking in `di/` for a missing `@Provides`.
 *
 * ## The dirty flag, and the one case D-10's text does not cover
 *
 * The flag is set whenever the session is not [GamesSignInState.SignedIn] and whenever any
 * submission in a pass returns `false` or throws; it is cleared only when every call in the
 * pass succeeded. **A pass that issued ZERO calls leaves it untouched**, which is an
 * addition to D-10's literal wording and must survive any later refactor:
 * [ProgressRepository.aggregate] fails safe to [ProgressAggregate] and
 * [ProgressRepository.classicBests] to an empty map, so a storage fault is byte-identical
 * to a brand-new player. Clearing on that pass would silently discard a pending backfill
 * that no later signed-in solve would re-set. Leaving it alone is a no-op for the genuinely
 * empty player — whose flag is already `false` — and preserves the backfill for the fault.
 *
 * ## Every pass resubmits everything (RESEARCH Pitfall 3, decided not missed)
 *
 * Pitfall 3 counts up to ~29 SDK calls on a fully-unlocked player's solve and proposes a
 * process-lifetime `Set<AchievementId>` de-dup cache. That cache is deliberately **not**
 * implemented here, per CONTEXT's "resubmit everything, it is safe and idempotent":
 * passes are human-paced (one per solved level, plus one per session resolution), a fresh
 * player's pass issues 0–3 calls, and every call is idempotent by construction — absolute
 * steps (D-12/D-03) and unlocks that no-op on an already-unlocked achievement. A cache
 * would be a second source of truth that the D-10 backfill would have to bypass anyway,
 * buying an optimisation at the cost of the one property — every pass is a complete,
 * self-healing resubmission — that makes PGSL-05 provable. **If Play Console ever reports
 * quota pressure, that de-dup set is the first lever to pull.**
 *
 * ## No logging, anywhere
 *
 * There is no logging call anywhere in `app/src/main` — a build gate greps for the platform
 * logger's fully-qualified name and expects zero hits, which is why this paragraph does not
 * spell it — and that absence is itself the ASVS V7 control: no player id, no achievement id
 * and no SDK message reaches logcat. "Log the failure" is therefore not an available
 * convention here; a failure is swallowed silently, exactly as `InterstitialAdSlot` and
 * `SettingsViewModel` swallow theirs.
 */
@Singleton
open class ProgressSubmitter
    @Inject
    internal constructor(
        private val progressRepository: ProgressRepository,
        private val gamesService: GamesService,
        private val syncStore: SubmissionSyncStore,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        /**
         * One session collector per process.
         *
         * `MainActivity.onCreate` calls [start] on every Activity creation, so a
         * configuration change alone reaches it a second time; a second collector would
         * double every backfill pass for the rest of the process. The idiom is
         * `PlayGamesService.bootstrapped`'s, for the same reason.
         */
        private val started = AtomicBoolean(false)

        /**
         * Serializes passes (PGSL-05, T-12-72).
         *
         * [withLock] — wait — rather than `tryLock` — skip — and the choice is deliberate:
         * a skipped pass could discard the newest aggregate, whereas a queued one merely
         * re-reads and resubmits idempotent values. The cost of waiting is a duplicate
         * inert call; the cost of skipping is a lost update.
         */
        private val passMutex = Mutex()

        /**
         * Starts the one session collector, which runs a pass whenever the session resolves
         * to [GamesSignInState.SignedIn] while the flag is set — the whole of PGSL-05.
         *
         * **Called explicitly from `MainActivity.onCreate`, and that is load-bearing
         * (RESEARCH Pitfall 5).** Hilt constructs a `@Singleton` lazily, on first injection.
         * A submitter injected only by `ResultsViewModel` would have no collector until the
         * player first reached a Results screen — so the backfill would never run for
         * exactly the player it was written for, the one who signed in from Settings and
         * closed the app, while passing every JVM test. That is why this method exists at
         * all, and why [started] guards it.
         *
         * The flag is read INSIDE the collector, once per emission. Reading it once outside
         * would freeze a launch-time value and miss every solve banked since.
         */
        open fun start() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                gamesService.session.collect { session ->
                    if (session is GamesSignInState.SignedIn && syncStore.pending.first()) {
                        submitAll()
                    }
                }
            }
        }

        /**
         * Submits everything the player's stored progress now implies — fire-and-forget
         * (PGSL-04, D-13).
         *
         * The caller gets control back immediately: the whole body is a launch on the
         * application-lifetime scope, so the submission survives the Results screen being
         * navigated away from and cannot delay, fail or alter the solved level that
         * triggered it.
         *
         * **Nothing here may be made `suspend`.** Awaiting it from `viewModelScope` would
         * tie submission lifetime to the screen, and a back-navigation mid-submit would
         * cancel the very dirty-flag write that makes the failure recoverable — RESEARCH
         * Pattern 1's named anti-pattern, and the reason `ResultsViewModel` does not wrap
         * this call in anything.
         */
        open fun onResultRecorded() {
            scope.launch { submitAll() }
        }

        /**
         * One serialized pass: gate on the session, read the aggregates, evaluate the rules
         * once, issue every call, then move the flag.
         */
        private suspend fun submitAll() =
            passMutex.withLock {
                // `Unknown` takes this branch too (RESEARCH Pitfall 8). It means "the first
                // check has not answered", and reading it as "offline" — or as "probably
                // fine" — is the mistake this line exists to prevent: the subsequent
                // SignedIn emission triggers the backfill, so nothing is lost by waiting.
                if (gamesService.session.value !is GamesSignInState.SignedIn) {
                    syncStore.setPending(true)
                    return@withLock
                }
                // Both reads already fail safe inside `storageFailSafe` (12-06), so neither
                // needs wrapping here — and note what that means for the flag write below.
                val aggregate = progressRepository.aggregate()
                val bests = progressRepository.classicBests()
                val outcome = AchievementRules.evaluate(aggregate)

                val results =
                    submitLeaderboards(aggregate, bests) +
                        submitUnlocks(outcome) +
                        submitSteps(outcome)

                recordPassOutcome(issued = results.isNotEmpty(), allOk = results.all { it })
            }

        /**
         * Moves the dirty flag to match a completed pass.
         *
         * Three cases, written as a `when` so the middle one is impossible to read past: a
         * pass that **issued no calls at all** leaves the flag exactly as it was. See the
         * class KDoc — a storage fault and a brand-new player produce identical empty
         * reads, and only one of them has a backfill worth keeping.
         */
        private suspend fun recordPassOutcome(
            issued: Boolean,
            allOk: Boolean,
        ) {
            when {
                !allOk -> syncStore.setPending(true)
                issued -> syncStore.setPending(false)
                else -> Unit
            }
        }

        /**
         * The two Challenge leaderboards plus one per stored Classic best (PGSL-01/02/03).
         *
         * A `null` aggregate value is an OMISSION, not a zero: "no Challenge level has ever
         * been solved" is a different claim from "the highest solved Challenge level is 0",
         * and only one of them belongs on a public leaderboard. `challengeScoreTotal` is
         * already a `Long` because it is the SQL `SUM(bestScore)` (D-07, Pitfall 6) — a
         * few tens of thousands of levels overflow an `Int`, and the wrapped negative would
         * be submitted.
         */
        private suspend fun submitLeaderboards(
            aggregate: ProgressAggregate,
            bests: Map<ClassicBoard, Int>,
        ): List<Boolean> =
            buildList {
                aggregate.highestChallengeLevel?.let {
                    add(attempt { gamesService.submitScore(GamesIds.Leaderboards.challengeLadder, it.toLong()) })
                }
                aggregate.challengeScoreTotal?.let {
                    add(attempt { gamesService.submitScore(GamesIds.Leaderboards.challengeScore, it) })
                }
                bests.forEach { (board, best) ->
                    // A `null` lookup means "this level has no board leaderboard" — the
                    // correct answer, not an error — so it is SKIPPED without contributing a
                    // `false` that would set the dirty flag (PGSL-03, and 12-04's own KDoc).
                    val leaderboardId = GamesIds.Leaderboards.forClassic(ClassicBoardIds.levelIdFor(board))
                    if (leaderboardId != null) {
                        add(attempt { gamesService.submitScore(leaderboardId, best.toLong()) })
                    }
                }
            }

        /** The one-shot unlocks the rules produced. */
        private suspend fun submitUnlocks(outcome: AchievementOutcome): List<Boolean> =
            outcome.unlocked.map { attempt { gamesService.unlockAchievement(GamesIds.of(it)) } }

        /**
         * The tiers' ABSOLUTE step values, passed verbatim (D-03/D-12).
         *
         * There is deliberately **no `steps <= 0` filter here** (Pitfall 4). 12-02's
         * `AchievementOutcome` invariant already guarantees it, and a second filter would be
         * a silent second source of truth for the same rule; the guarantee is asserted by a
         * dedicated `ProgressSubmitterTest` case instead. Nothing is clamped to a tier's own
         * total either — Play Games auto-unlocks once steps reach it and ignores the rest.
         */
        private suspend fun submitSteps(outcome: AchievementOutcome): List<Boolean> =
            outcome.steps.map { (id, steps) -> attempt { gamesService.setAchievementSteps(GamesIds.of(id), steps) } }

        /**
         * Runs one seam call, degrading a throw to the `false` the seam is supposed to have
         * returned.
         *
         * The `InterstitialAdSlot`/`SettingsViewModel` idiom: `runCatching` around the single
         * call it guards, with cancellation rethrown before classification so structured
         * concurrency still works. A thrown failure is therefore indistinguishable from a
         * reported one and sets the dirty flag exactly the same way.
         *
         * **Every call is issued and every result collected — the verdict is `all { it }`
         * over the completed list, never a short-circuiting `&&`.** A short-circuit would
         * skip real submissions after the first failure and leave half the pass unattempted,
         * so the flag would promise a retry for work that had never been tried once.
         */
        private suspend fun attempt(block: suspend () -> Boolean): Boolean =
            runCatching { block() }
                .onFailure { if (it is CancellationException) throw it }
                .getOrDefault(false)
    }
