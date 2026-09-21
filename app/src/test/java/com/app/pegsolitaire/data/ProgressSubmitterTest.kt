package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pegsolitaire.data.HomeProgress
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.achievement.AchievementId
import com.app.pegsolitaire.domain.achievement.AchievementRules
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.platform.games.FakeGamesService
import com.app.pegsolitaire.platform.games.GamesIds
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.app.pegsolitaire.platform.games.save.CloudSaveOutcome
import com.app.pegsolitaire.platform.games.save.FakeCloudSaveStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.io.IOException

/**
 * The behaviour spec for [ProgressSubmitter] — the one data-layer `@Singleton` (D-13) that
 * turns stored progress into Play Games submissions.
 *
 * Four properties are under test here, and they are the whole of PGSL-01..PGSL-05:
 *
 *  - **What a pass submits** is a function of the SQL aggregate and nothing else: the two
 *    Challenge leaderboards (skipped when their values are SQL `NULL`, never submitted as
 *    zero), one submission per stored Classic best, and exactly the unlocks and ABSOLUTE
 *    step values one [AchievementRules.evaluate] call produced (D-11/D-12/D-03).
 *  - **When the dirty flag moves** (D-10): set when the session is not
 *    [GamesSignInState.SignedIn] — `Unknown` included (RESEARCH Pitfall 8) — and on any
 *    `false` or any throw; cleared only when a pass that actually *issued* calls saw every
 *    one of them succeed.
 *  - **That the backfill needs no ViewModel** (RESEARCH Pitfall 5): the whole
 *    `start()` → `SignedIn` → resubmit path is exercised here, and **no ViewModel of any
 *    kind is constructed in this file** — a grep gate asserts the Results one is not even
 *    named. That is the point rather than an accident, since the player the backfill exists
 *    for is precisely the one who never opened a Results screen.
 *  - **That two passes never interleave** (PGSL-05): a solve landing while sign-in resolves
 *    produces two serialized passes, not one racing pair whose flag writes could be lost.
 *
 * Every expected achievement ID set is computed by calling [AchievementRules.evaluate] in
 * the test rather than hand-listed. Hand-listing would restate plan 12-02's spec here,
 * where the two copies could only ever drift together; what this class asserts is that the
 * submitter *forwards the rules' answer*, not what that answer is.
 *
 * The seam is the shipped [FakeGamesService] (12-03) — never a second recording fake —
 * wrapped by [ScriptedGamesService] for the two failure shapes a single
 * [FakeGamesService.submissionResult] cannot express: failing one call out of many, and
 * throwing instead of returning.
 */
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent/advanceUntilIdle virtual-time control
class ProgressSubmitterTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Stands in for the injected `@ApplicationScope`. It shares [testDispatcher]'s
     * scheduler, so `advanceUntilIdle()` drives the submitter's fire-and-forget launches,
     * and it is cancelled in [tearDown] because [start]'s session collector never completes
     * on its own — exactly as it never does in production.
     */
    private val submitterScope = CoroutineScope(testDispatcher)

    @After
    fun tearDown() {
        submitterScope.cancel()
    }

    // ---- The session gate (D-10, Pitfall 8) ------------------------------------

    @Test
    fun aSolveWhileSignedOutSetsTheFlagAndSubmitsNothing() =
        runTest(testDispatcher) {
            val games = FakeGamesService(GamesSignInState.SignedOut)
            val repository = FakeProgressRepository(populatedAggregate(), populatedBests())
            val store = FakeSubmissionSyncStore()
            val submitter = submitterFor(repository, games, store)

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(games.submittedScores).isEmpty()
            assertThat(games.unlockedAchievements).isEmpty()
            assertThat(games.achievementSteps).isEmpty()
            assertThat(store.value).isTrue()
        }

    @Test
    fun anUnresolvedSessionBehavesIdenticallyToSignedOut() =
        runTest(testDispatcher) {
            // Pitfall 8: `Unknown` means "the first check has not answered", NOT "offline".
            // Reading it as either "probably fine" (issue calls the SDK drops) or as a
            // distinct third case is the mistake; it takes the SignedOut branch verbatim.
            val games = FakeGamesService(GamesSignInState.Unknown)
            val repository = FakeProgressRepository(populatedAggregate(), populatedBests())
            val store = FakeSubmissionSyncStore()
            val submitter = submitterFor(repository, games, store)

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(games.submittedScores).isEmpty()
            assertThat(games.unlockedAchievements).isEmpty()
            assertThat(games.achievementSteps).isEmpty()
            assertThat(store.value).isTrue()
            // The repository is not even read: nothing leaves storage for a player whose
            // identity has not resolved (T-12-71).
            assertThat(repository.passCount).isEqualTo(0)
        }

    // ---- What a signed-in pass submits (PGSL-01/02/03) -------------------------

    @Test
    fun aSignedInPassSubmitsExactlyWhatTheAggregateSays() =
        runTest(testDispatcher) {
            val games = signedInGamesService()
            val repository = FakeProgressRepository(populatedAggregate(), populatedBests())
            val submitter = submitterFor(repository, games, FakeSubmissionSyncStore())

            submitter.onResultRecorded()
            advanceUntilIdle()

            val submitted = games.submittedScores.map { it.leaderboardId to it.score }
            assertThat(submitted)
                .containsExactly(
                    GamesIds.Leaderboards.challengeLadder to HIGHEST_CHALLENGE_LEVEL.toLong(),
                    GamesIds.Leaderboards.challengeScore to CHALLENGE_SCORE_TOTAL,
                    leaderboardFor(ClassicBoard.ENGLISH33) to ENGLISH33_BEST.toLong(),
                    leaderboardFor(ClassicBoard.EUROPEAN37) to EUROPEAN37_BEST.toLong(),
                    leaderboardFor(ClassicBoard.DIAMOND41) to DIAMOND41_BEST.toLong(),
                )
            // `containsExactly` already pins the count; stated again so a future edit that
            // relaxes it to `containsAtLeast` cannot let a stray extra submission through.
            assertThat(games.submittedScores).hasSize(EXPECTED_POPULATED_LEADERBOARD_CALLS)
        }

    @Test
    fun anAbsentChallengeHistoryIsSkippedRatherThanSubmittedAsZero() =
        runTest(testDispatcher) {
            // PGSL-01/02: SQL `NULL` means "no Challenge level has ever been solved", which
            // is not the same claim as "the highest solved Challenge level is 0" — and a
            // zero on a public leaderboard is a claim.
            val games = signedInGamesService()
            val repository = FakeProgressRepository(ProgressAggregate(totalStars = TOTAL_STARS), emptyMap())
            val submitter = submitterFor(repository, games, FakeSubmissionSyncStore())

            submitter.onResultRecorded()
            advanceUntilIdle()

            val ids = games.submittedScores.map { it.leaderboardId }
            assertThat(ids).doesNotContain(GamesIds.Leaderboards.challengeLadder)
            assertThat(ids).doesNotContain(GamesIds.Leaderboards.challengeScore)
            assertThat(games.submittedScores).isEmpty()
        }

    @Test
    fun everyShippedClassicBoardResolvesToALeaderboardSoTheSkipBranchIsUnreachableToday() {
        // PGSL-03: a `null` from `forClassic` means "this level has no board leaderboard",
        // which the pass treats as a SKIP that contributes no `false` — a skip is not a
        // failure and must not set the dirty flag.
        //
        // That branch cannot be reached through the shipped objects, and this case asserts
        // the precondition that makes it unreachable rather than faking one: every
        // `ClassicBoard` resolves through `ClassicBoardIds.levelIdFor` to a constant
        // `GamesIds.Leaderboards.forClassic` maps. The branch exists for the sixth board
        // that ships before its Console leaderboard does — and a test that stubbed
        // `GamesIds` to produce a null would be asserting against the stub, not the map.
        ClassicBoard.entries.forEach { board ->
            assertThat(GamesIds.Leaderboards.forClassic(ClassicBoardIds.levelIdFor(board))).isNotNull()
        }
    }

    // ---- What a signed-in pass unlocks and steps (PGSA-02/03, D-03/D-11/D-12) ---

    @Test
    fun unlocksAndStepsAreExactlyWhatTheRulesProduced() =
        runTest(testDispatcher) {
            val aggregate = populatedAggregate()
            val expected = AchievementRules.evaluate(aggregate)
            val games = signedInGamesService()
            val submitter =
                submitterFor(
                    FakeProgressRepository(aggregate, populatedBests()),
                    games,
                    FakeSubmissionSyncStore(),
                )

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(games.unlockedAchievements.toSet())
                .isEqualTo(expected.unlocked.map(GamesIds::of).toSet())
            assertThat(games.achievementSteps.map { it.achievementId to it.steps }.toSet())
                .isEqualTo(expected.steps.map { GamesIds.of(it.key) to it.value }.toSet())
        }

    @Test
    fun everyTierReceivesTheAbsoluteValueNeverADeltaOrAClamp() =
        runTest(testDispatcher) {
            // D-03/D-12: both ladder tiers below the player's level carry the LEVEL (50),
            // not the tier's own total (10 / 50) and not the distance to it (40). Absolute
            // is what makes the D-10 backfill idempotent rather than additive.
            val games = signedInGamesService()
            val submitter =
                submitterFor(
                    FakeProgressRepository(populatedAggregate(), populatedBests()),
                    games,
                    FakeSubmissionSyncStore(),
                )

            submitter.onResultRecorded()
            advanceUntilIdle()

            val stepsById = games.achievementSteps.associate { it.achievementId to it.steps }
            assertThat(stepsById[GamesIds.of(AchievementId.LADDER_10)]).isEqualTo(HIGHEST_CHALLENGE_LEVEL)
            assertThat(stepsById[GamesIds.of(AchievementId.LADDER_50)]).isEqualTo(HIGHEST_CHALLENGE_LEVEL)
            assertThat(stepsById[GamesIds.of(AchievementId.STARS_25)]).isEqualTo(TOTAL_STARS)
        }

    @Test
    fun noTieredAchievementIsEverUnlockedDirectly() =
        runTest(testDispatcher) {
            // D-12: Play Games auto-unlocks an incremental achievement once its steps reach
            // the total, so unlocking one directly is both redundant and destructive of the
            // progress bar the tiers exist to show.
            val games = signedInGamesService()
            val submitter =
                submitterFor(
                    FakeProgressRepository(maximalAggregate(), populatedBests()),
                    games,
                    FakeSubmissionSyncStore(),
                )

            submitter.onResultRecorded()
            advanceUntilIdle()

            val tieredIds =
                AchievementId.entries
                    .filter { it.isIncremental }
                    .map(GamesIds::of)
                    .toSet()
            assertThat(games.unlockedAchievements.toSet().intersect(tieredIds)).isEmpty()
        }

    @Test
    fun noStepSubmissionEverCarriesANonPositiveValue() =
        runTest(testDispatcher) {
            // Pitfall 4 / the 12-02 `AchievementOutcome` invariant, asserted HERE rather
            // than re-filtered in production: a second filter would be a second source of
            // truth for a guarantee the rules already make.
            val games = signedInGamesService()
            listOf(ProgressAggregate(), populatedAggregate(), maximalAggregate()).forEach { aggregate ->
                submitterFor(
                    FakeProgressRepository(aggregate, populatedBests()),
                    games,
                    FakeSubmissionSyncStore(),
                ).onResultRecorded()
                advanceUntilIdle()
            }

            assertThat(games.achievementSteps).isNotEmpty()
            games.achievementSteps.forEach { assertThat(it.steps).isGreaterThan(0) }
        }

    // ---- The dirty-flag lifecycle (D-10) ---------------------------------------

    @Test
    fun anAllSuccessPassThatIssuedCallsClearsTheFlag() =
        runTest(testDispatcher) {
            val store = FakeSubmissionSyncStore(initial = true)
            val submitter =
                submitterFor(
                    FakeProgressRepository(populatedAggregate(), populatedBests()),
                    signedInGamesService(),
                    store,
                )

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(store.value).isFalse()
        }

    @Test
    fun aWhollyFailingPassLeavesTheFlagSetAndStillAttemptsEveryCall() =
        runTest(testDispatcher) {
            val aggregate = populatedAggregate()
            val outcome = AchievementRules.evaluate(aggregate)
            val games = signedInGamesService().apply { submissionResult = false }
            val store = FakeSubmissionSyncStore()
            val submitter = submitterFor(FakeProgressRepository(aggregate, populatedBests()), games, store)

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(store.value).isTrue()
            // No short-circuit: a `&&`-accumulating fold would stop after the first `false`
            // and leave most of the pass unattempted, so a later retry would be the only
            // thing that ever submitted them.
            assertThat(games.submittedScores).hasSize(EXPECTED_POPULATED_LEADERBOARD_CALLS)
            assertThat(games.unlockedAchievements).hasSize(outcome.unlocked.size)
            assertThat(games.achievementSteps).hasSize(outcome.steps.size)
        }

    @Test
    fun oneFailureAmongManySuccessesStillLeavesTheFlagSet() =
        runTest(testDispatcher) {
            val games = ScriptedGamesService(signedInGamesService(), failAtCall = SECOND_CALL)
            val store = FakeSubmissionSyncStore()
            val submitter =
                submitterFor(FakeProgressRepository(populatedAggregate(), populatedBests()), games, store)

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(store.value).isTrue()
        }

    @Test
    fun aThrowingSeamIsAFailureRatherThanACrash() =
        runTest(testDispatcher) {
            // The seam documents failure as `false`, but an SDK that throws anyway must be
            // indistinguishable from one that reported it: same flag, no propagation into a
            // fire-and-forget launch that has no handler. [SeamFailure] is an unchecked
            // exception the submitter has no special knowledge of — which is the case.
            val games = ScriptedGamesService(signedInGamesService(), throwAtCall = FIRST_CALL)
            val store = FakeSubmissionSyncStore()
            val submitter =
                submitterFor(FakeProgressRepository(populatedAggregate(), populatedBests()), games, store)

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(store.value).isTrue()
        }

    @Test
    fun aPassThatIssuedZeroCallsNeverClearsTheFlag() =
        runTest(testDispatcher) {
            // T-12-75, and an addition to D-10's literal text. `aggregate()` fails safe to
            // `ProgressAggregate()` and `classicBests()` to `emptyMap()`, so a corrupt-DB
            // read is byte-identical to a brand-new player. Clearing on that pass would
            // discard a pending backfill no later signed-in solve would re-set; leaving the
            // flag alone is a no-op for the genuinely-empty player, whose flag is already
            // `false`.
            val store = FakeSubmissionSyncStore(initial = true)
            val games = signedInGamesService()
            val submitter = submitterFor(FakeProgressRepository(ProgressAggregate(), emptyMap()), games, store)

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(games.submittedScores).isEmpty()
            assertThat(games.unlockedAchievements).isEmpty()
            assertThat(games.achievementSteps).isEmpty()
            assertThat(store.value).isTrue()
        }

    // ---- The session collector, i.e. the backfill (PGSL-05, Pitfall 5) ---------

    @Test
    fun aPlayerWhoBankedBestsOfflineGetsEverythingResubmittedOnSignIn() =
        runTest(testDispatcher) {
            // The whole point of the phase, and note what is NOT in this test: no
            // ViewModel, no Results screen, no solve. The player this path exists for
            // signed in from Settings and may never finish a level at all.
            val aggregate = populatedAggregate()
            val expected = AchievementRules.evaluate(aggregate)
            val games = FakeGamesService(GamesSignInState.SignedOut)
            val store = FakeSubmissionSyncStore(initial = true)
            val submitter = submitterFor(FakeProgressRepository(aggregate, populatedBests()), games, store)

            submitter.start()
            advanceUntilIdle()
            games.emitSession(signedIn())
            advanceUntilIdle()

            assertThat(games.submittedScores).hasSize(EXPECTED_POPULATED_LEADERBOARD_CALLS)
            assertThat(games.unlockedAchievements.toSet())
                .isEqualTo(expected.unlocked.map(GamesIds::of).toSet())
            assertThat(games.achievementSteps.map { it.achievementId to it.steps }.toSet())
                .isEqualTo(expected.steps.map { GamesIds.of(it.key) to it.value }.toSet())
            assertThat(store.value).isFalse()
        }

    @Test
    fun oneSignedInResolutionRunsExactlyOnePass() =
        runTest(testDispatcher) {
            // The fake fails every submission, so the flag STAYS set — which is what makes
            // this case load-bearing: a second pass is not merely unobserved, it is
            // unblocked and still does not happen, because StateFlow conflates the equal
            // value rather than re-delivering it.
            val games = signedInGamesService().apply { submissionResult = false }
            games.emitSession(GamesSignInState.SignedOut)
            val repository = FakeProgressRepository(populatedAggregate(), populatedBests())
            val submitter = submitterFor(repository, games, FakeSubmissionSyncStore(initial = true))

            submitter.start()
            advanceUntilIdle()
            games.emitSession(signedIn())
            advanceUntilIdle()
            games.emitSession(signedIn())
            advanceUntilIdle()

            assertThat(repository.passCount).isEqualTo(1)
        }

    @Test
    fun aSignedInResolutionWithTheFlagClearRunsNoPassAtAll() =
        runTest(testDispatcher) {
            // D-10: clean launches make no calls. This is the property the fail-open `false`
            // default was chosen to protect.
            val games = FakeGamesService(GamesSignInState.SignedOut)
            val repository = FakeProgressRepository(populatedAggregate(), populatedBests())
            val submitter = submitterFor(repository, games, FakeSubmissionSyncStore(initial = false))

            submitter.start()
            advanceUntilIdle()
            games.emitSession(signedIn())
            advanceUntilIdle()

            assertThat(repository.passCount).isEqualTo(0)
            assertThat(games.submittedScores).isEmpty()
        }

    @Test
    fun callingStartTwiceStillCollectsOnce() =
        runTest(testDispatcher) {
            // `MainActivity.onCreate` runs again on every configuration change. Two
            // collectors would double every backfill pass for the rest of the process.
            val games = signedInGamesService().apply { submissionResult = false }
            games.emitSession(GamesSignInState.SignedOut)
            val repository = FakeProgressRepository(populatedAggregate(), populatedBests())
            val submitter = submitterFor(repository, games, FakeSubmissionSyncStore(initial = true))

            submitter.start()
            submitter.start()
            advanceUntilIdle()
            games.emitSession(signedIn())
            advanceUntilIdle()

            assertThat(repository.passCount).isEqualTo(1)
        }

    // ---- Serialization (PGSL-05, T-12-72) --------------------------------------

    @Test
    fun twoOverlappingPassesSerializeRatherThanInterleaveOrDrop() =
        runTest(testDispatcher) {
            // The production race: a solve lands while sign-in resolves. Unserialized, the
            // two passes' flag writes could interleave and lose the pending state. `withLock`
            // (wait) rather than `tryLock` (skip) is the deliberate choice — a skipped pass
            // could discard the newest aggregate, whereas a queued one merely resubmits
            // idempotent values.
            val gate = CompletableDeferred<Unit>()
            val repository = FakeProgressRepository(populatedAggregate(), populatedBests(), gate = gate)
            val submitter =
                submitterFor(repository, signedInGamesService(), FakeSubmissionSyncStore())

            submitter.onResultRecorded()
            submitter.onResultRecorded()
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()

            assertThat(repository.maxConcurrentPasses).isEqualTo(1)
            assertThat(repository.passCount).isEqualTo(2)
        }

    @Test
    fun aSignInThatLandsWhileTheFlagWriteIsStillInFlightStillRunsItsBackfillOnce() =
        runTest(testDispatcher) {
            // WR-01: the pending flag must be read UNDER the pass mutex, not in the
            // collector before it. The interleaving: a solve is banked while the session is
            // still unresolved, so its pass takes the signed-out branch and parks inside the
            // suspending `setPending(true)` while holding the lock; sign-in resolves in that
            // window. A collector that read the flag before taking the lock would see the
            // not-yet-committed `false`, skip, and strand the backfill until the next
            // signed-in solve or the next process launch — the session StateFlow does not
            // re-emit an equal value.
            val writeGate = CompletableDeferred<Unit>()
            val games = FakeGamesService(GamesSignInState.Unknown)
            val repository = FakeProgressRepository(populatedAggregate(), populatedBests())
            val store = FakeSubmissionSyncStore(initial = false, writeGate = writeGate)
            val submitter = submitterFor(repository, games, store)

            submitter.start()
            advanceUntilIdle()

            submitter.onResultRecorded()
            advanceUntilIdle()
            // The write is in flight: asked for, not committed.
            assertThat(store.value).isFalse()

            games.emitSession(signedIn())
            advanceUntilIdle()

            writeGate.complete(Unit)
            advanceUntilIdle()

            // Exactly one pass — the queued backfill — and it really submitted.
            assertThat(repository.passCount).isEqualTo(1)
            assertThat(games.submittedScores).isNotEmpty()
            // Everything succeeded, so the drained flag is left clear.
            assertThat(store.value).isFalse()
        }

    // ---- Harness ---------------------------------------------------------------

    /**
     * The Phase 12 submitter, wired to inert cloud collaborators.
     *
     * Phase 13 widened the constructor with the four stores its cloud half sequences, and
     * this factory supplies the quietest possible version of each: an empty
     * [FakeCloudSaveStore] (no payload, so nothing is ever imported), both cloud flags
     * clear, and no stored player id — a fresh install, which by D-12 never raises the
     * profile-swap dialog. **Every assertion in this file is therefore unchanged**, because
     * the cloud half of a pass over those fakes is observationally inert with respect to
     * `GamesService` and the submissions flag. The cloud half's own behaviour is specified
     * next door, in `ProgressSubmitterCloudTest`.
     */
    private fun submitterFor(
        repository: ProgressRepository,
        games: GamesService,
        store: SubmissionSyncStore,
    ): ProgressSubmitter =
        ProgressSubmitter(
            progressRepository = repository,
            gamesService = games,
            syncStore = store,
            cloudSyncStore = InertCloudSyncStore(),
            cloudProfileStore = InertCloudProfileStore(),
            cloudSaveStore = FakeCloudSaveStore(),
            challengeProgress = ChallengeProgress(unusedDataStore(), submitterScope),
            scope = submitterScope,
        )

    private fun signedInGamesService(): FakeGamesService = FakeGamesService(signedIn())
}

private fun signedIn(): GamesSignInState.SignedIn = GamesSignInState.SignedIn(playerId = TEST_PLAYER_ID)

private fun leaderboardFor(board: ClassicBoard): String =
    requireNonNullLeaderboard(GamesIds.Leaderboards.forClassic(ClassicBoardIds.levelIdFor(board)))

private fun requireNonNullLeaderboard(id: String?): String =
    requireNotNull(id) { "every shipped Classic board has a board leaderboard" }

/** A player deep into both modes: every one-shot but the Classic sweep, and both tier axes. */
private fun populatedAggregate(): ProgressAggregate =
    ProgressAggregate(
        highestChallengeLevel = HIGHEST_CHALLENGE_LEVEL,
        challengeScoreTotal = CHALLENGE_SCORE_TOTAL,
        totalStars = TOTAL_STARS,
        solvedLevelCount = SOLVED_LEVEL_COUNT,
        threeStarCount = THREE_STAR_COUNT,
        clearedClassicBoards = setOf(ClassicBoard.ENGLISH33, ClassicBoard.EUROPEAN37, ClassicBoard.DIAMOND41),
        anyHintFreeSolve = true,
        anyUndoFreeSolve = true,
        anyCenterFinish = true,
        anySpeedSolve = true,
    )

/** Everything earned — the shape that puts all six tiers and all fourteen one-shots in play. */
private fun maximalAggregate(): ProgressAggregate =
    populatedAggregate().copy(
        clearedClassicBoards = ClassicBoard.entries.toSet(),
        perfectClassicBoards = ClassicBoard.entries.toSet(),
    )

/** Three stored Classic bests — fewer than five, so an absent board is visibly absent. */
private fun populatedBests(): Map<ClassicBoard, Int> =
    mapOf(
        ClassicBoard.ENGLISH33 to ENGLISH33_BEST,
        ClassicBoard.EUROPEAN37 to EUROPEAN37_BEST,
        ClassicBoard.DIAMOND41 to DIAMOND41_BEST,
    )

/**
 * A settable [ProgressRepository] over the two reads the submitter makes, counting passes
 * and — through [gate] — able to hold one open while a second is attempted.
 *
 * [maxConcurrentPasses] is the concurrency claim itself: it records the high-water mark of
 * simultaneously-open `aggregate()` calls, which is `1` if and only if the Mutex serialized
 * them. Every other member of the interface is unreachable from the submitter and says so.
 */
private class FakeProgressRepository(
    private val aggregateValue: ProgressAggregate,
    private val bestsValue: Map<ClassicBoard, Int>,
    private val gate: CompletableDeferred<Unit>? = null,
) : ProgressRepository {
    var passCount = 0
        private set

    var maxConcurrentPasses = 0
        private set

    private var inFlight = 0

    override suspend fun aggregate(): ProgressAggregate {
        passCount++
        inFlight++
        maxConcurrentPasses = maxOf(maxConcurrentPasses, inFlight)
        gate?.await()
        inFlight--
        return aggregateValue
    }

    override suspend fun classicBests(): Map<ClassicBoard, Int> = bestsValue

    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        telemetry: SolveTelemetry,
        nowMs: Long,
    ): NewBests = error("the submitter never records a result")

    override fun observeHomeProgress(): Flow<HomeProgress> = flowOf(HomeProgress.UNKNOWN)

    override fun observeResults(mode: String): Flow<List<LevelResult>> = flowOf(emptyList())

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) = error("the submitter never touches a resume snapshot")

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? =
        error("the submitter never touches a resume snapshot")

    override suspend fun clearSnapshot(mode: String) = error("the submitter never touches a resume snapshot")

    override fun observeResumeModes(): Flow<List<String>> = flowOf(emptyList())

    /**
     * The payload a push would write — reachable from here, but only ever empty.
     *
     * A solve-triggered pass pushes (D-01), so this is called; it is deliberately the
     * fresh-player shape, since nothing in this file asserts anything about the payload.
     * What the push actually writes is specified in `ProgressSubmitterCloudTest`.
     */
    override suspend fun exportProgress(ladder: Int): CloudProgress = CloudProgress(ladder = ladder)

    /** Unreachable here: the [FakeCloudSaveStore] this file wires in holds no payload to import. */
    override suspend fun importProgress(
        remote: CloudProgress,
        nowMs: Long,
    ): ImportOutcome = error("the cloud import is specified in ProgressSubmitterCloudTest")

    /** Unreachable here: only an answered profile-swap dialog wipes, and this file raises none. */
    override suspend fun resetAllProgress() = error("the D-14 wipe is specified in ProgressSubmitterCloudTest")
}

/**
 * A [CloudSyncStore] that reports no outstanding push and remembers nothing — the shape that
 * keeps this file's passes free of the cloud half.
 *
 * A subclass over a `MutableStateFlow`, the [FakeSubmissionSyncStore] trick, which is why
 * plan 13-02 made the class and both its members `open`.
 */
private class InertCloudSyncStore : CloudSyncStore(unusedDataStore()) {
    private val state = MutableStateFlow(false)

    override val pushPending: Flow<Boolean> = state

    override suspend fun setPushPending(pending: Boolean) {
        state.value = pending
    }

    override suspend fun recordPushOutcome(outcome: CloudSaveOutcome) {
        state.value = !outcome.committed
    }
}

/** A [CloudProfileStore] stuck on `null` — a fresh install, which by D-12 never shows the dialog. */
private class InertCloudProfileStore : CloudProfileStore(unusedDataStore()) {
    private val state = MutableStateFlow<String?>(null)

    override val mergedPlayerId: Flow<String?> = state

    override suspend fun setMergedPlayerId(playerId: String) {
        state.value = playerId
    }
}

/**
 * An in-memory [SubmissionSyncStore] — a subclass over a `MutableStateFlow`, which is why
 * plan 12-07 Task 1 made the class and both its members `open`.
 *
 * It takes the real (never-touched) constructor's [DataStore] parameter because the
 * superclass demands one; both overridden members bypass it entirely.
 */
private class FakeSubmissionSyncStore(
    initial: Boolean = false,
    private val writeGate: CompletableDeferred<Unit>? = null,
) : SubmissionSyncStore(unusedDataStore()) {
    private val state = MutableStateFlow(initial)

    override val pending: Flow<Boolean> = state

    /** The flag as the last write left it — the single value most cases here assert on. */
    val value: Boolean get() = state.value

    /**
     * Parks every write until [writeGate] completes, when one is supplied.
     *
     * The real store's write is a suspending `DataStore.edit`, so "the flag has been asked
     * to change but has not committed yet" is a state production genuinely occupies. That
     * window is the whole of WR-01, and it is unobservable without a gate.
     */
    override suspend fun setPending(pending: Boolean) {
        writeGate?.await()
        state.value = pending
    }
}

/** A [DataStore] that exists only to satisfy a superclass constructor; every use throws. */
private fun unusedDataStore(): DataStore<Preferences> =
    object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("the fake never reads the store") }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            throw IOException("the fake never writes the store")
    }

/**
 * A [GamesService] that forwards to a [FakeGamesService] — so every submission is still
 * recorded — while scripting one call in the sequence to fail or to throw.
 *
 * It exists because [FakeGamesService.submissionResult] is one boolean for the whole pass,
 * which cannot express either of the two failure shapes D-10 turns on: a single failure
 * among many successes, and a seam that throws rather than reporting.
 */
private class ScriptedGamesService(
    private val delegate: FakeGamesService,
    private val failAtCall: Int = NO_SCRIPTED_CALL,
    private val throwAtCall: Int = NO_SCRIPTED_CALL,
) : GamesService by delegate {
    private var callCount = 0

    override suspend fun submitScore(
        leaderboardId: String,
        score: Long,
    ): Boolean = scripted { delegate.submitScore(leaderboardId, score) }

    override suspend fun unlockAchievement(achievementId: String): Boolean =
        scripted { delegate.unlockAchievement(achievementId) }

    override suspend fun setAchievementSteps(
        achievementId: String,
        steps: Int,
    ): Boolean = scripted { delegate.setAchievementSteps(achievementId, steps) }

    /** Records through the delegate first, so a scripted failure is still an ATTEMPTED call. */
    private suspend fun scripted(call: suspend () -> Boolean): Boolean {
        callCount++
        val recorded = call()
        return when (callCount) {
            throwAtCall -> throw SeamFailure()
            failAtCall -> false
            else -> recorded
        }
    }
}

/**
 * What [ScriptedGamesService] throws instead of reporting.
 *
 * A named type rather than a bare `RuntimeException` for detekt's `TooGenericExceptionThrown`,
 * and it reads better at the assertion: the claim is that an unchecked exception the
 * submitter knows nothing about is caught and classified as a failure, not that one
 * particular stdlib type is.
 */
private class SeamFailure : RuntimeException("the seam exploded")

/** The Play Games player id the fake session carries; no assertion depends on its value. */
private const val TEST_PLAYER_ID = "backfill-player"

/** Ordinals for [ScriptedGamesService]; `0` scripts nothing because call counts are 1-based. */
private const val NO_SCRIPTED_CALL = 0
private const val FIRST_CALL = 1
private const val SECOND_CALL = 2

/** Above both the first two ladder tiers, so LADDER_10 and LADDER_50 both receive it. */
private const val HIGHEST_CHALLENGE_LEVEL = 50

/** `SUM(bestScore)` over Challenge — a `Long`, per Pitfall 6, never an `Int`. */
private const val CHALLENGE_SCORE_TOTAL = 123_456L

/** Above the first star tier and below the second. */
private const val TOTAL_STARS = 40

private const val SOLVED_LEVEL_COUNT = 60
private const val THREE_STAR_COUNT = 7

/** Three stored Classic bests, each distinct so a swapped leaderboard id is visible. */
private const val ENGLISH33_BEST = 1_000
private const val EUROPEAN37_BEST = 2_000
private const val DIAMOND41_BEST = 3_000

/** Two Challenge leaderboards plus one per stored Classic best. */
private const val EXPECTED_POPULATED_LEADERBOARD_CALLS = 5
