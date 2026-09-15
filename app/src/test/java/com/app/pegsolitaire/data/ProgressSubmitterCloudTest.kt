package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.app.pegsolitaire.data.cloud.CloudProgressCodec
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.progress.LevelBest
import com.app.pegsolitaire.domain.progress.LevelKey
import com.app.pegsolitaire.domain.progress.ProgressMerge
import com.app.pegsolitaire.domain.progress.ProgressMode
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.app.pegsolitaire.platform.games.FakeGamesService
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.app.pegsolitaire.platform.games.save.CloudSaveDescription
import com.app.pegsolitaire.platform.games.save.CloudSaveStore
import com.app.pegsolitaire.platform.games.save.FakeCloudSaveStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.io.IOException

/**
 * The behaviour spec for [ProgressSubmitter]'s **cloud** half — the pull (D-02), the push
 * (D-01) and the account-switch gate (D-12/D-15) that must run before either of them.
 *
 * It is a second file rather than more cases in `ProgressSubmitterTest` because the two
 * specify different claims over the same object. That file owns "what a pass submits and
 * when the submissions flag moves", and its fakes are deliberately inert about the cloud;
 * this one owns "what a pass reads from and writes to the cloud save, and in what order",
 * and wires a real [FakeCloudSaveStore] and a real [ChallengeProgress] to do it.
 *
 * Four properties are under test, and between them they are the whole of the phase's
 * ordering guarantee:
 *
 *  - **The profile gate runs first (D-12/D-15, RESEARCH Pitfall 8).** A stored player id
 *    that disagrees with the signed-in one raises the Home flag and performs **zero** store
 *    calls — asserted over `loadCallCount` *and* `saveCallCount`, because "zero store calls"
 *    is a statement about the pair. This is the one assertion in the file that protects
 *    against unrecoverable data loss: a pull that ran first would already have merged the
 *    new account's progress into the old account's local store.
 *  - **The pull runs exactly once per process, on its own guard (D-02, Pitfall 6).** Two
 *    `SignedIn` emissions produce one [CloudSaveStore.load], and a clean launch — the
 *    submissions flag clear — still pulls. That second half is RESEARCH assumption A7's
 *    reviewed change to D-10's "clean launches make no calls", and it is the whole point of
 *    the phase for the player who just installed the game on a second device.
 *  - **Every cloud failure shape is the same failure (CLOUD-05, T-13-29).** `null`, `false`,
 *    undecodable bytes and a seam that *throws* all leave local play untouched and surface
 *    nothing. [CancellationException] is the one exception to that, and it is rethrown.
 *  - **One pass converges two devices (D-01).** The push runs after the pull, over the
 *    settled ladder, through the same `CloudProgressCodec::mergeBytes` the conflict path
 *    uses, and a push that did not land is remembered and retried.
 *
 * The harness is `ProgressSubmitterTest`'s, deliberately: the same [StandardTestDispatcher],
 * the same app-scope stand-in cancelled in [tearDown], and the same subclass-a-real-store
 * trick for the DataStore-backed flags. [ChallengeProgress] is the real class over an
 * in-memory store rather than a double, because the property under test in the push — that
 * the ladder is read back through `settledLevel()` and not through `currentLevel`'s
 * asynchronous `stateIn` cache — is only observable against a store whose commit and whose
 * re-emission are genuinely two different moments.
 */
@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle virtual-time control
class ProgressSubmitterCloudTest {
    private val testDispatcher = StandardTestDispatcher()

    /**
     * Stands in for the injected `@ApplicationScope`. It shares [testDispatcher]'s
     * scheduler, so `advanceUntilIdle()` drives the submitter's fire-and-forget launches,
     * and it is cancelled in [tearDown] because [ProgressSubmitter.start]'s session
     * collector never completes on its own — exactly as it never does in production.
     */
    private val submitterScope = CoroutineScope(testDispatcher)

    @After
    fun tearDown() {
        submitterScope.cancel()
    }

    // ---- Step 0: the session gate ----------------------------------------------

    @Test
    fun aSessionThatIsNotSignedInSetsBothMarkersAndTouchesNoStore() =
        runTest(testDispatcher) {
            // `Unknown` means "the first check has not answered", never "offline" — and a
            // pass that cannot name the player must not read or write that player's cloud
            // save (T-12-71, extended to T-13-28).
            val saveStore = FakeCloudSaveStore()
            val syncStore = FakeSyncStore()
            val cloudSyncStore = FakeCloudSyncStore()
            val submitter =
                submitterFor(
                    games = FakeGamesService(GamesSignInState.Unknown),
                    syncStore = syncStore,
                    cloudSyncStore = cloudSyncStore,
                    saveStore = saveStore,
                )

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(saveStore.loadCallCount).isEqualTo(0)
            assertThat(saveStore.saveCallCount).isEqualTo(0)
            // BOTH backends are owed a retry, and they are owed it independently.
            assertThat(syncStore.value).isTrue()
            assertThat(cloudSyncStore.value).isTrue()
        }

    // ---- Step 1: the profile gate (D-12/D-15, Pitfall 8) -----------------------

    @Test
    fun aDifferentStoredPlayerIdRaisesTheSwapAndTouchesNeitherStore() =
        runTest(testDispatcher) {
            // The mitigation for T-13-28, and the reason the gate is step 1 rather than
            // step 3: had the pull run first, player B's progress would already be merged
            // into player A's local store, unrecoverably.
            val saveStore = FakeCloudSaveStore(remote = remotePayload())
            val profileStore = FakeCloudProfileStore(initial = OTHER_PLAYER_ID)
            val repository = FakeCloudRepository()
            val submitter =
                submitterFor(
                    repository = repository,
                    games = FakeGamesService(signedIn()),
                    profileStore = profileStore,
                    saveStore = saveStore,
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(submitter.profileSwap.value)
                .isEqualTo(ProfileSwapRequest(playerId = TEST_PLAYER_ID, displayName = TEST_DISPLAY_NAME))
            assertThat(saveStore.loadCallCount).isEqualTo(0)
            assertThat(saveStore.saveCallCount).isEqualTo(0)
            assertThat(repository.importCallCount).isEqualTo(0)
            // Unanswered, the stored id is left alone: nothing has been merged, so nothing
            // may be recorded as merged.
            assertThat(profileStore.value).isEqualTo(OTHER_PLAYER_ID)
        }

    @Test
    fun aFreshInstallNeverRaisesTheSwapAndRecordsTheAccountItMergedWith() =
        runTest(testDispatcher) {
            // D-12: a `null` stored id is the *silent merge*, never the dialog — that
            // asymmetry is what keeps a damaged preferences file from putting a player in
            // front of the app's only destructive path (T-13-04).
            val profileStore = FakeCloudProfileStore(initial = null)
            val saveStore = FakeCloudSaveStore()
            val submitter =
                submitterFor(
                    games = FakeGamesService(signedIn()),
                    profileStore = profileStore,
                    saveStore = saveStore,
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(submitter.profileSwap.value).isNull()
            assertThat(saveStore.loadCallCount).isEqualTo(1)
            // This write is what makes the NEXT launch's step 1 meaningful.
            assertThat(profileStore.value).isEqualTo(TEST_PLAYER_ID)
        }

    @Test
    fun aMatchingStoredPlayerIdNeverRaisesTheSwap() =
        runTest(testDispatcher) {
            val saveStore = FakeCloudSaveStore()
            val submitter =
                submitterFor(
                    games = FakeGamesService(signedIn()),
                    profileStore = FakeCloudProfileStore(initial = TEST_PLAYER_ID),
                    saveStore = saveStore,
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(submitter.profileSwap.value).isNull()
            assertThat(saveStore.loadCallCount).isEqualTo(1)
        }

    // ---- Step 2: the once-per-process pull (D-02, Pitfall 6) -------------------

    @Test
    fun twoSignedInEmissionsInOneProcessPullExactlyOnce() =
        runTest(testDispatcher) {
            // Pitfall 6: `session` is a StateFlow that replays to its collector, and the
            // real one legitimately emits SignedIn twice on a launch — once from
            // `bootstrapIfOptedIn`, once from an explicit `signIn()`. The collector guard
            // does nothing about that; only a guard consumed inside the pass does.
            val games = FakeGamesService(signedIn())
            val saveStore = FakeCloudSaveStore(remote = remotePayload())
            val submitter = submitterFor(games = games, saveStore = saveStore)

            submitter.start()
            advanceUntilIdle()
            games.emitSession(signedIn(displayName = RENAMED_DISPLAY_NAME))
            advanceUntilIdle()

            assertThat(saveStore.loadCallCount).isEqualTo(1)
        }

    @Test
    fun aCleanLaunchStillPullsEvenThoughTheSubmissionsFlagIsClear() =
        runTest(testDispatcher) {
            // RESEARCH assumption A7, stated as a test: the submissions half keeps D-10's
            // "clean launches make no calls", the cloud half deliberately does not. Without
            // this, the new-device player D-02 exists for gets nothing back until they
            // happen to solve a level.
            val games = FakeGamesService(GamesSignInState.SignedOut)
            val saveStore = FakeCloudSaveStore(remote = remotePayload())
            val submitter =
                submitterFor(
                    games = games,
                    syncStore = FakeSyncStore(initial = false),
                    saveStore = saveStore,
                )

            submitter.start()
            advanceUntilIdle()
            games.emitSession(signedIn())
            advanceUntilIdle()

            assertThat(saveStore.loadCallCount).isEqualTo(1)
        }

    @Test
    fun aPulledPayloadIsImportedAndItsLadderMerged() =
        runTest(testDispatcher) {
            val ladderStore = inMemoryDataStore()
            val challengeProgress = ChallengeProgress(ladderStore, submitterScope)
            val repository = FakeCloudRepository()
            val submitter =
                submitterFor(
                    repository = repository,
                    games = FakeGamesService(signedIn()),
                    saveStore = FakeCloudSaveStore(remote = remotePayload()),
                    challengeProgress = challengeProgress,
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(repository.importCallCount).isEqualTo(1)
            assertThat(repository.stored.keys).contains(challengeKey(REMOTE_LEVEL))
            assertThat(challengeProgress.settledLevel()).isEqualTo(REMOTE_LADDER)
        }

    @Test
    fun aCloudImportThatChangedAnythingRefiresTheSubmissionPassImmediately() =
        runTest(testDispatcher) {
            // The step-2-before-step-3 ordering, as an observable claim: the flag is set by
            // the import and read by the submissions in the SAME pass, so a restored
            // player's leaderboards catch up now rather than on their next solve.
            val games = FakeGamesService(signedIn())
            val syncStore = FakeSyncStore(initial = false)
            val submitter =
                submitterFor(
                    games = games,
                    syncStore = syncStore,
                    saveStore = FakeCloudSaveStore(remote = remotePayload()),
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(games.submittedScores).isNotEmpty()
        }

    @Test
    fun anEmptyCloudImportsNothingAndLeavesTheLadderAlone() =
        runTest(testDispatcher) {
            // `null` means "stay local" (CLOUD-05), NOT "the player has no progress". The
            // two are indistinguishable from the caller, so they must be treated alike.
            val challengeProgress = ChallengeProgress(inMemoryDataStore(), submitterScope)
            val repository = FakeCloudRepository()
            val submitter =
                submitterFor(
                    repository = repository,
                    games = FakeGamesService(signedIn()),
                    saveStore = FakeCloudSaveStore(remote = null),
                    challengeProgress = challengeProgress,
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(repository.importCallCount).isEqualTo(0)
            assertThat(challengeProgress.settledLevel()).isEqualTo(ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL)
        }

    @Test
    fun undecodableBytesImportNothing() =
        runTest(testDispatcher) {
            // The codec's `null` is the trust boundary's answer and is honoured verbatim:
            // a payload this build cannot read leaves local storage exactly as it was.
            val challengeProgress = ChallengeProgress(inMemoryDataStore(), submitterScope)
            val repository = FakeCloudRepository()
            val submitter =
                submitterFor(
                    repository = repository,
                    games = FakeGamesService(signedIn()),
                    saveStore = FakeCloudSaveStore(remote = NOT_A_PAYLOAD.toByteArray()),
                    challengeProgress = challengeProgress,
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(repository.importCallCount).isEqualTo(0)
            assertThat(challengeProgress.settledLevel()).isEqualTo(ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL)
        }

    @Test
    fun aThrowingStoreIsIndistinguishableFromAnEmptyOne() =
        runTest(testDispatcher) {
            // T-13-29 / CLOUD-05. The real seam never throws, but the caller must not depend
            // on that: an SDK exception escaping into the application-scoped coroutine that
            // drives this pass would take the process rather than the pass.
            val repository = FakeCloudRepository()
            val profileStore = FakeCloudProfileStore(initial = null)
            val submitter =
                submitterFor(
                    repository = repository,
                    games = FakeGamesService(signedIn()),
                    profileStore = profileStore,
                    saveStore = FakeCloudSaveStore(throwOnLoad = StoreFailure()),
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(repository.importCallCount).isEqualTo(0)
            // The pass RAN TO COMPLETION — the same observable outcome an empty cloud
            // produces, which is the whole claim.
            assertThat(profileStore.value).isEqualTo(TEST_PLAYER_ID)
        }

    @Test
    fun aCancellationFromTheStoreIsRethrownRatherThanClassifiedAsAFailure() =
        runTest(testDispatcher) {
            // WR-04: cancellation is structured concurrency's signal, never a seam failure.
            // Swallowing it would let a cancelled pass carry on writing, which is exactly
            // what the surviving `setMergedPlayerId` would prove — so its ABSENCE is the
            // assertion.
            val profileStore = FakeCloudProfileStore(initial = null)
            val submitter =
                submitterFor(
                    games = FakeGamesService(signedIn()),
                    profileStore = profileStore,
                    saveStore = FakeCloudSaveStore(throwOnLoad = CancellationException("pass cancelled")),
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(profileStore.value).isNull()
        }

    // ---- Step 4: the push, its metadata and its retry marker (D-01/D-11) -------

    @Test
    fun aSolveTriggeredPassPushesTheStoredProgressWithItsListingMetadata() =
        runTest(testDispatcher) {
            val challengeProgress = ChallengeProgress(inMemoryDataStore(), submitterScope)
            val repository = FakeCloudRepository(initial = localBests())
            val saveStore = FakeCloudSaveStore()
            val submitter =
                submitterFor(
                    repository = repository,
                    games = FakeGamesService(signedIn()),
                    saveStore = saveStore,
                    challengeProgress = challengeProgress,
                )

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(saveStore.saveCallCount).isEqualTo(1)
            val pushed = decodePushed(saveStore.savedPayloads.single())
            assertThat(pushed.bests).containsExactlyEntriesIn(localBests())
            assertThat(pushed.ladder).isEqualTo(ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL)
            // D-11's three numbers, carried as numbers so `platform/` owns the wording.
            assertThat(saveStore.lastDescription)
                .isEqualTo(
                    CloudSaveDescription(
                        ladder = ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL,
                        totalStars = TOTAL_STARS,
                        classicCleared = cloudClassicBests().size,
                    ),
                )
            // D-10: the snapshot's played time is the payload's, i.e. the SUM(elapsedMs).
            assertThat(saveStore.lastPlayedTimeMs).isEqualTo(LOCAL_PLAYED_TIME_MS)
        }

    @Test
    fun theConflictMergeIsTheCodecsOwnUnionRatherThanASecondImplementation() =
        runTest(testDispatcher) {
            // The one place in the JVM suite where the conflict lambda actually runs. The
            // server gains a payload AFTER this process's single pull, so the pushed bytes
            // cannot already contain it — the union can only come from `mergeRemote`.
            val repository = FakeCloudRepository(initial = localBests())
            val saveStore = FakeCloudSaveStore(remote = null)
            val submitter =
                submitterFor(
                    repository = repository,
                    games = FakeGamesService(signedIn()),
                    saveStore = saveStore,
                )

            submitter.start()
            advanceUntilIdle()
            saveStore.remote = remotePayload()
            submitter.onResultRecorded()
            advanceUntilIdle()

            val pushed = decodePushed(saveStore.savedPayloads.single())
            assertThat(pushed.bests.keys).containsExactly(challengeKey(LOCAL_LEVEL), challengeKey(REMOTE_LEVEL))
            assertThat(pushed.ladder).isEqualTo(REMOTE_LADDER)
            assertThat(pushed.playedTimeMs).isEqualTo(REMOTE_PLAYED_TIME_MS)
        }

    @Test
    fun aPushThatDidNotLandIsRememberedForTheNextPass() =
        runTest(testDispatcher) {
            // `false` is not an error (CLOUD-05): it means "nothing reached the cloud", and
            // the answer is the persisted marker, never a snackbar (D-01, T-13-30).
            val cloudSyncStore = FakeCloudSyncStore(initial = false)
            val submitter =
                submitterFor(
                    games = FakeGamesService(signedIn()),
                    cloudSyncStore = cloudSyncStore,
                    saveStore = FakeCloudSaveStore(saveResult = false),
                )

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(cloudSyncStore.value).isTrue()
        }

    @Test
    fun aPushThatLandedClearsTheMarker() =
        runTest(testDispatcher) {
            val cloudSyncStore = FakeCloudSyncStore(initial = true)
            val submitter =
                submitterFor(
                    games = FakeGamesService(signedIn()),
                    cloudSyncStore = cloudSyncStore,
                    saveStore = FakeCloudSaveStore(saveResult = true),
                )

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(cloudSyncStore.value).isFalse()
        }

    @Test
    fun aThrowingPushIsTreatedExactlyAsAReportedFailure() =
        runTest(testDispatcher) {
            val cloudSyncStore = FakeCloudSyncStore(initial = false)
            val saveStore = FakeCloudSaveStore(throwOnSave = StoreFailure())
            val repository = FakeCloudRepository(initial = localBests())
            val submitter =
                submitterFor(
                    repository = repository,
                    games = FakeGamesService(signedIn()),
                    cloudSyncStore = cloudSyncStore,
                    saveStore = saveStore,
                )

            submitter.onResultRecorded()
            advanceUntilIdle()

            assertThat(saveStore.saveCallCount).isEqualTo(1)
            assertThat(cloudSyncStore.value).isTrue()
            // The pass completed: the local half of the solve is untouched by a cloud fault.
            assertThat(repository.stored).isEqualTo(localBests())
        }

    @Test
    fun aCleanLaunchThatPulledNothingNeverPushes() =
        runTest(testDispatcher) {
            // The other half of assumption A7: the pull is the only cloud call a clean
            // launch may make. A push here would upload a fresh device's payload for no
            // reason, every launch, forever.
            val games = FakeGamesService(signedIn())
            val saveStore = FakeCloudSaveStore(remote = null)
            val submitter =
                submitterFor(
                    games = games,
                    cloudSyncStore = FakeCloudSyncStore(initial = false),
                    saveStore = saveStore,
                )

            submitter.start()
            advanceUntilIdle()
            assertThat(saveStore.saveCallCount).isEqualTo(0)

            // A second SignedIn emission, with the pull guard now consumed as well.
            games.emitSession(signedIn(displayName = RENAMED_DISPLAY_NAME))
            advanceUntilIdle()

            assertThat(saveStore.loadCallCount).isEqualTo(1)
            assertThat(saveStore.saveCallCount).isEqualTo(0)
        }

    @Test
    fun anOutstandingRetryMarkerPushesOnTheNextStartTriggeredPass() =
        runTest(testDispatcher) {
            // D-01's retry: the marker survived the last process, and draining it is the
            // whole reason it is persisted.
            val cloudSyncStore = FakeCloudSyncStore(initial = true)
            val saveStore = FakeCloudSaveStore(remote = null)
            val submitter =
                submitterFor(
                    games = FakeGamesService(signedIn()),
                    cloudSyncStore = cloudSyncStore,
                    saveStore = saveStore,
                )

            submitter.start()
            advanceUntilIdle()

            assertThat(saveStore.saveCallCount).isEqualTo(1)
            assertThat(cloudSyncStore.value).isFalse()
        }

    @Test
    fun aPassThatImportedALadderPushesThatLadderBackRatherThanTheCachedOne() =
        runTest(testDispatcher) {
            // RESEARCH open question 4, and the reason `ChallengeProgress.settledLevel()`
            // exists: the `stateIn` cache re-emits asynchronously after `mergeLadder`
            // commits, so a push that read it would write back a LOWER ladder than the one
            // this very pass imported. It would also make "one pass converges two devices"
            // untrue, which is the property the phase is selling.
            val challengeProgress = ChallengeProgress(inMemoryDataStore(), submitterScope)
            val repository = FakeCloudRepository(initial = localBests())
            val saveStore = FakeCloudSaveStore(remote = remotePayload())
            val submitter =
                submitterFor(
                    repository = repository,
                    games = FakeGamesService(signedIn()),
                    cloudSyncStore = FakeCloudSyncStore(initial = false),
                    saveStore = saveStore,
                    challengeProgress = challengeProgress,
                )

            submitter.start()
            advanceUntilIdle()

            // A pulled payload forces the write-back even on a clean, start-triggered pass.
            assertThat(saveStore.saveCallCount).isEqualTo(1)
            assertThat(repository.exportedLadders).containsExactly(REMOTE_LADDER)
            assertThat(saveStore.lastDescription?.ladder).isEqualTo(REMOTE_LADDER)
            assertThat(decodePushed(saveStore.savedPayloads.single()).ladder).isAtLeast(REMOTE_LADDER)
        }

    // ---- Harness ---------------------------------------------------------------

    @Suppress("LongParameterList") // one optional collaborator per store the pass sequences
    private fun submitterFor(
        repository: ProgressRepository = FakeCloudRepository(),
        games: GamesService = FakeGamesService(signedIn()),
        syncStore: SubmissionSyncStore = FakeSyncStore(),
        cloudSyncStore: CloudSyncStore = FakeCloudSyncStore(),
        profileStore: CloudProfileStore = FakeCloudProfileStore(),
        saveStore: CloudSaveStore = FakeCloudSaveStore(),
        challengeProgress: ChallengeProgress = ChallengeProgress(inMemoryDataStore(), submitterScope),
    ): ProgressSubmitter =
        ProgressSubmitter(
            progressRepository = repository,
            gamesService = games,
            syncStore = syncStore,
            cloudSyncStore = cloudSyncStore,
            cloudProfileStore = profileStore,
            cloudSaveStore = saveStore,
            challengeProgress = challengeProgress,
            scope = submitterScope,
        )
}

private fun signedIn(displayName: String? = TEST_DISPLAY_NAME): GamesSignInState.SignedIn =
    GamesSignInState.SignedIn(playerId = TEST_PLAYER_ID, displayName = displayName)

/** One Challenge level's key — the shape both the payload fixtures and the assertions use. */
private fun challengeKey(level: Int): LevelKey = LevelKey(mode = ProgressMode.CHALLENGE, levelId = level.toString())

/** What this device stores: one Challenge level the remote fixture never carries. */
private fun localBests(): Map<LevelKey, LevelBest> = mapOf(challengeKey(LOCAL_LEVEL) to challengeBest(LOCAL_SCORE))

/** The pushed bytes as progress again — a payload the push wrote must be one this build can read. */
private fun decodePushed(bytes: ByteArray): CloudProgress =
    requireNotNull(CloudProgressCodec.decode(bytes)) { "the push must write a payload this build can decode" }

private fun challengeBest(score: Int): LevelBest =
    LevelBest(stars = FULL_STARS, bestTimeMs = SOLVE_TIME_MS, bestScore = score)

/** The payload "the server" holds: one Challenge level this device has never seen, and a high ladder. */
private fun remotePayload(
    ladder: Int = REMOTE_LADDER,
    level: Int = REMOTE_LEVEL,
): ByteArray =
    CloudProgressCodec.encode(
        CloudProgress(
            ladder = ladder,
            playedTimeMs = REMOTE_PLAYED_TIME_MS,
            bests = mapOf(challengeKey(level) to challengeBest(REMOTE_SCORE)),
        ),
    )

/** A player with something to submit, so a pass that reaches step 3 is visibly different from one that does not. */
private fun cloudAggregate(): ProgressAggregate =
    ProgressAggregate(
        highestChallengeLevel = LOCAL_LADDER,
        challengeScoreTotal = CHALLENGE_SCORE_TOTAL,
        totalStars = TOTAL_STARS,
        solvedLevelCount = SOLVED_LEVEL_COUNT,
        threeStarCount = THREE_STAR_COUNT,
        clearedClassicBoards = setOf(ClassicBoard.ENGLISH33, ClassicBoard.EUROPEAN37),
    )

/** Two cleared Classic boards — so `classicCleared` in the listing metadata is visibly `2`, not `5`. */
private fun cloudClassicBests(): Map<ClassicBoard, Int> =
    mapOf(
        ClassicBoard.ENGLISH33 to ENGLISH33_BEST,
        ClassicBoard.EUROPEAN37 to EUROPEAN37_BEST,
    )

/**
 * A [ProgressRepository] that really merges, so the payload assertions mean something.
 *
 * [importProgress] runs the same `ProgressMerge.merge` the shipped repository runs and
 * reports [ImportOutcome] the same way, and [exportProgress] hands back whatever the merges
 * have left behind. A fake that simply recorded the call could not tell a push of the union
 * from a push of the local half, which is the claim half this file's cases rest on.
 */
private class FakeCloudRepository(
    initial: Map<LevelKey, LevelBest> = emptyMap(),
    private val aggregateValue: ProgressAggregate = cloudAggregate(),
    private val bestsValue: Map<ClassicBoard, Int> = cloudClassicBests(),
    private val playedTimeMs: Long = LOCAL_PLAYED_TIME_MS,
) : ProgressRepository {
    /** Everything the device currently stores, after every merge and every wipe so far. */
    var stored: Map<LevelKey, LevelBest> = initial
        private set

    /** How many payloads reached [importProgress] — `0` proves the pull never landed. */
    var importCallCount = 0
        private set

    /** How many times the D-14 wipe ran; its single production caller is the answered dialog. */
    var resetCallCount = 0
        private set

    /** Every ladder [exportProgress] was asked for, in order — the `settledLevel()` claim. */
    var exportedLadders: List<Int> = emptyList()
        private set

    override suspend fun aggregate(): ProgressAggregate = aggregateValue

    override suspend fun classicBests(): Map<ClassicBoard, Int> = bestsValue

    override suspend fun exportProgress(ladder: Int): CloudProgress {
        exportedLadders = exportedLadders + ladder
        return CloudProgress(ladder = ladder, playedTimeMs = playedTimeMs, bests = stored)
    }

    override suspend fun importProgress(
        remote: CloudProgress,
        nowMs: Long,
    ): ImportOutcome {
        importCallCount++
        val merged = ProgressMerge.merge(CloudProgress(bests = stored), remote).bests
        val improved = merged.filter { (key, best) -> stored[key] != best }
        stored = merged
        return if (improved.isEmpty()) ImportOutcome.UNCHANGED else ImportOutcome(true, improved.size)
    }

    override suspend fun resetAllProgress() {
        resetCallCount++
        stored = emptyMap()
    }

    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        telemetry: SolveTelemetry,
        nowMs: Long,
    ): NewBests = error("the submitter never records a result")

    override fun observeResults(mode: String): Flow<List<LevelResult>> = flowOf(emptyList())

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) = error("the submitter never writes a resume snapshot — CLOUD-04 is a type, not a habit")

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? =
        error("the submitter never reads a resume snapshot — CLOUD-04 is a type, not a habit")

    override suspend fun clearSnapshot(mode: String) = error("only resetAllProgress clears snapshots here (D-14)")

    override fun observeResumeModes(): Flow<List<String>> = flowOf(emptyList())
}

/** An in-memory [SubmissionSyncStore] — the `ProgressSubmitterTest` subclassing trick. */
private class FakeSyncStore(
    initial: Boolean = false,
) : SubmissionSyncStore(unusedDataStore()) {
    private val state = MutableStateFlow(initial)

    override val pending: Flow<Boolean> = state

    /** The flag as the last write left it. */
    val value: Boolean get() = state.value

    override suspend fun setPending(pending: Boolean) {
        state.value = pending
    }
}

/** An in-memory [CloudSyncStore] — D-01's retry marker, without a DataStore. */
private class FakeCloudSyncStore(
    initial: Boolean = false,
) : CloudSyncStore(unusedDataStore()) {
    private val state = MutableStateFlow(initial)

    override val pushPending: Flow<Boolean> = state

    /** The marker as the last write left it — set on a failed push, cleared on a landed one. */
    val value: Boolean get() = state.value

    override suspend fun setPushPending(pending: Boolean) {
        state.value = pending
    }
}

/**
 * An in-memory [CloudProfileStore] — D-12's stored player id.
 *
 * `null` is the fresh install and is the default, because that is the majority production
 * case and the only one that must never raise the dialog.
 */
private class FakeCloudProfileStore(
    initial: String? = null,
) : CloudProfileStore(unusedDataStore()) {
    private val state = MutableStateFlow(initial)

    override val mergedPlayerId: Flow<String?> = state

    /** The id as the last write left it; `null` until a merge has actually happened. */
    val value: String? get() = state.value

    override suspend fun setMergedPlayerId(playerId: String) {
        state.value = playerId
    }
}

/**
 * A [DataStore] that lives entirely in memory and commits on the calling coroutine.
 *
 * Deliberately not `PreferenceDataStoreFactory` over a `TemporaryFolder`, the way
 * `ChallengeProgressTest` does it: that store commits on `Dispatchers.IO`, which
 * `advanceUntilIdle()` cannot drive, so a pass's ladder write would land at a moment virtual
 * time knows nothing about. Committing inline keeps every step of a pass inside the one
 * scheduler the assertions advance — while still leaving `currentLevel`'s `stateIn` cache a
 * separate coroutine, which is what makes the `settledLevel()`-versus-`.value` distinction
 * observable here at all.
 */
private fun inMemoryDataStore(): DataStore<Preferences> =
    object : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
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
 * What a throwing [CloudSaveStore] throws.
 *
 * A named type rather than a bare `RuntimeException`, for detekt's
 * `TooGenericExceptionThrown` and because the claim reads better: an unchecked exception the
 * submitter knows nothing about is swallowed, not one particular stdlib type.
 */
private class StoreFailure : RuntimeException("the saved-games seam exploded")

/** The Play Games player id the fake session carries. */
private const val TEST_PLAYER_ID = "cloud-player"

/** Its display name — carried into [ProfileSwapRequest] so the dialog can name the account. */
private const val TEST_DISPLAY_NAME = "Cloud Player"

/** A second display name for the SAME id, so a second `SignedIn` emission is not conflated away. */
private const val RENAMED_DISPLAY_NAME = "Cloud Player (2)"

/** A DIFFERENT account — the whole of the D-12 mismatch. */
private const val OTHER_PLAYER_ID = "other-player"

/** Bytes that are not a payload at all, so the codec refuses them. */
private const val NOT_A_PAYLOAD = "{not json"

/** The remote ladder: far above anything the local fixtures reach, so a regression is visible. */
private const val REMOTE_LADDER = 50

/** A Challenge level the local store never has — its arrival proves the import landed. */
private const val REMOTE_LEVEL = 40

private const val REMOTE_SCORE = 900
private const val REMOTE_PLAYED_TIME_MS = 90_000L

/** The local side: a lower ladder and a distinct played time, so the two are never confusable. */
private const val LOCAL_LADDER = 10
private const val LOCAL_PLAYED_TIME_MS = 45_000L

/** A Challenge level only this device has — its survival proves the push carried local progress. */
private const val LOCAL_LEVEL = 7
private const val LOCAL_SCORE = 500

private const val FULL_STARS = 3
private const val SOLVE_TIME_MS = 30_000L
private const val CHALLENGE_SCORE_TOTAL = 9_000L
private const val TOTAL_STARS = 24
private const val SOLVED_LEVEL_COUNT = 12
private const val THREE_STAR_COUNT = 4
private const val ENGLISH33_BEST = 1_100
private const val EUROPEAN37_BEST = 2_200
