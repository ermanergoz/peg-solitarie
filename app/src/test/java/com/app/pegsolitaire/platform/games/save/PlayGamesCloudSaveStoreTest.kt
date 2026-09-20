package com.app.pegsolitaire.platform.games.save

import android.app.Activity
import android.graphics.Bitmap
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotContents
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Behaviour spec for [PlayGamesCloudSaveStore] — the SDK plumbing behind the cloud seam.
 *
 * **The one rule this file exists to pin: the snapshot opens under the only conflict policy
 * that cannot lose a player's progress.** Google's own javadoc says of
 * `RESOLUTION_POLICY_MANUAL` that it *"ensures that no user changes to the state of the save
 * game will ever be lost"*, and of the most-recently-modified policy that *"a player's changes
 * may get lost"*. A version of this class that passed the wrong integer would look identical in
 * review and would silently delete progress on the one device that hit a conflict, so the open
 * triple is asserted literally (T-13-34).
 *
 * **The second rule: every opened handle reaches exactly one terminal call.** A leaked handle
 * fails the *next* open for the rest of the process — cloud save that works once per launch and
 * then silently stops (RESEARCH Pitfall 1). Each path below asserts the close count, not just
 * the return value, because the return value is identical whether or not the handle leaked.
 *
 * **The third rule: nothing ever throws out of the seam.** No Activity, a thrown SDK call, a
 * conflict that never resolves and a payload the server will not accept are all `null` or an
 * uncommitted [CloudSaveOutcome] naming the stage that refused (CLOUD-05) — with the single
 * documented exception of `CancellationException`, which is a caller's structured concurrency
 * and not an SDK failure (WR-04).
 *
 * Same harness as `PlayGamesServiceTest`: `mockkStatic(PlayGames::class)` over the real static
 * entry point, `Tasks.forResult(...)` for completed `Task`s — taken by `await()`'s fast path with
 * no listener, executor or `Looper`, so this stays a plain JVM test — and
 * `Dispatchers.setMain(StandardTestDispatcher())` so the ten-second Activity bound elapses in
 * virtual time.
 */
class PlayGamesCloudSaveStoreTest {
    private val testDispatcher = StandardTestDispatcher()
    private val activityProvider = mockk<ForegroundActivityProvider>()
    private val activity = mockk<Activity>()
    private val client = mockk<SnapshotsClient>()
    private val coverImageSource = mockk<CoverImageSource>()
    private val descriptionFormatter = mockk<CloudSaveDescriptionFormatter>()

    /** The snapshot a non-conflicted open hands back, and the contents it reads through. */
    private val contents = mockk<SnapshotContents>()
    private val metadata = mockk<SnapshotMetadata>()
    private val snapshot = mockk<Snapshot>()

    /** The contents a conflict resolution writes the merged bytes into. */
    private val resolutionContents = mockk<SnapshotContents>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(PlayGames::class)
        every { PlayGames.getSnapshotsClient(any()) } returns client
        coEvery { activityProvider.awaitActivity() } returns activity

        every { snapshot.snapshotContents } returns contents
        every { snapshot.metadata } returns metadata
        every { metadata.snapshotId } returns SNAPSHOT_ID
        every { metadata.hasChangePending() } returns false
        every { metadata.lastModifiedTimestamp } returns LAST_MODIFIED_AT
        every { contents.readFully() } returns REMOTE_BYTES
        every { contents.writeBytes(any()) } returns true
        every { resolutionContents.writeBytes(any()) } returns true

        every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(dataResult(snapshot))
        every { client.resolveConflict(any(), any(), any(), any()) } returns Tasks.forResult(dataResult(snapshot))
        every { client.commitAndClose(any(), any()) } returns Tasks.forResult(metadata)
        every { client.discardAndClose(any()) } returns Tasks.forResult(null)
        every { client.getMaxDataSize() } returns Tasks.forResult(SERVER_CAP_BYTES)

        coEvery { coverImageSource.coverOrNull() } returns null
        every { descriptionFormatter.format(any()) } returns DESCRIPTION_LINE
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ---- load: the Activity bound ---------------------------------------------------

    @Test
    fun loadWithoutAForegroundActivityReturnsNullAndNeverTouchesTheSdk() =
        runTest(testDispatcher) {
            coEvery { activityProvider.awaitActivity() } coAnswers { awaitCancellation() }

            val result = store().load(::concat).bytes

            assertThat(result).isNull()
            // The bound is what makes this a failed pass rather than a hung singleton: the SDK
            // is never reached at all, so there is no handle to leak either (T-13-38).
            verify(exactly = 0) { PlayGames.getSnapshotsClient(any()) }
        }

    // ---- load: the open triple ------------------------------------------------------

    @Test
    fun loadOpensTheOneSnapshotNameUnderTheOnlyPolicyThatCannotLoseData() =
        runTest(testDispatcher) {
            store().load(::concat)

            // Asserted literally rather than through a helper: this is the line the whole
            // phase is judged on, and a helper would hide which integer was actually passed.
            verify(exactly = 1) {
                client.open(
                    CloudSaveConstants.SNAPSHOT_NAME,
                    true,
                    SnapshotsClient.RESOLUTION_POLICY_MANUAL,
                )
            }
        }

    // ---- load: reading ---------------------------------------------------------------

    @Test
    fun loadOnANonConflictedOpenReturnsTheSnapshotBytes() =
        runTest(testDispatcher) {
            val result = store().load(::concat).bytes

            assertThat(result).isEqualTo(REMOTE_BYTES)
        }

    @Test
    fun loadOnAnEmptySnapshotReturnsNullRatherThanAnEmptyArray() =
        runTest(testDispatcher) {
            every { contents.readFully() } returns ByteArray(0)

            val result = store().load(::concat).bytes

            // "There is no payload" and "the payload is empty" must be indistinguishable to the
            // caller: both mean stay local. A caller handed an empty array could reasonably
            // decode it, fail, and treat that as a corrupt cloud save.
            assertThat(result).isNull()
        }

    @Test
    fun loadDiscardsItsHandleAndNeverCommits() =
        runTest(testDispatcher) {
            store().load(::concat)

            // A pull that changed nothing must not rewrite the snapshot and move the server's
            // lastModifiedTimestamp — which is exactly what a commit on the read path would do.
            verify(exactly = 1) { client.discardAndClose(snapshot) }
            verify(exactly = 0) { client.commitAndClose(any(), any()) }
        }

    // ---- load: the conflict loop ------------------------------------------------------

    @Test
    fun aConflictedOpenMergesTheServerVersionAgainstTheModifiedVersionInThatOrder() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())
            val seen = mutableListOf<Pair<ByteArray, ByteArray>>()

            store().load { server, mine ->
                seen += server to mine
                concat(server, mine)
            }

            assertThat(seen).hasSize(1)
            // The merge is commutative today, so swapping these is harmless *now* — which is
            // precisely why the orientation is pinned, so a future non-commutative field cannot
            // be added silently. getSnapshot() is the SERVER's version; getConflictingSnapshot()
            // is the MODIFIED one.
            assertThat(seen.single().first).isEqualTo(SERVER_BYTES)
            assertThat(seen.single().second).isEqualTo(MODIFIED_BYTES)
        }

    @Test
    fun aConflictWritesTheMergedBytesIntoTheResolutionContentsAndResolvesWithAnEmptyChange() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())
            val written = slot<ByteArray>()
            every { resolutionContents.writeBytes(capture(written)) } returns true

            store().load(::concat)

            assertThat(written.captured).isEqualTo(SERVER_BYTES + MODIFIED_BYTES)
            // EMPTY_CHANGE mid-loop, real metadata only at the final commit: the javadoc says
            // values absent from a metadata change resolve to the server's current version, so a
            // half-built change here would fight the commit that follows.
            verify(exactly = 1) {
                client.resolveConflict(
                    CONFLICT_ID,
                    SNAPSHOT_ID,
                    SnapshotMetadataChange.EMPTY_CHANGE,
                    resolutionContents,
                )
            }
        }

    @Test
    fun aResolvedConflictStillYieldsTheSnapshotBytes() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())

            val result = store().load(::concat).bytes

            assertThat(result).isEqualTo(REMOTE_BYTES)
        }

    @Test
    fun aConflictThatKeepsConflictingStopsAtTheRetryBound() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())
            every { client.resolveConflict(any(), any(), any(), any()) } answers { Tasks.forResult(conflictResult()) }

            val result = store().load(::concat).bytes

            assertThat(result).isNull()
            // Bounded, not defensive: resolveConflict is documented to be able to produce a
            // conflict itself, so this really is a loop against the SDK (T-13-37).
            verify(exactly = CloudSaveConstants.MAX_CONFLICT_RETRIES) {
                client.resolveConflict(any(), any(), any(), any())
            }
        }

    @Test
    fun anExhaustedConflictLoopStillClosesTheHandleItWasHolding() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())
            every { client.resolveConflict(any(), any(), any(), any()) } answers { Tasks.forResult(conflictResult()) }

            store().load(::concat)

            // Giving up is not a licence to leak: an abandoned handle would fail every later
            // open for the rest of the process (Pitfall 1).
            verify(exactly = 1) { client.discardAndClose(any()) }
        }

    // ---- load: failure classification --------------------------------------------------

    @Test
    fun aThrownSdkCallOnTheReadPathYieldsNullAndStillClosesTheSnapshot() =
        runTest(testDispatcher) {
            every { contents.readFully() } throws IOException(SDK_FAILURE)

            val result = store().load(::concat).bytes

            assertThat(result).isNull()
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun aFailedOpenYieldsNullWithNothingToClose() =
        runTest(testDispatcher) {
            every {
                client.open(any<String>(), any(), any())
            } returns Tasks.forException(IllegalStateException(SDK_FAILURE))

            val result = store().load(::concat).bytes

            assertThat(result).isNull()
            verify(exactly = 0) { client.discardAndClose(any()) }
            verify(exactly = 0) { client.commitAndClose(any(), any()) }
        }

    @Test
    fun cancellationPropagatesOutOfLoadRatherThanBeingClassifiedAsAFailure() =
        runTest(testDispatcher) {
            every { contents.readFully() } throws CancellationException(CANCELLED)

            // runCatching rather than assertThrows: a CancellationException escaping into the
            // test coroutine would otherwise be indistinguishable from the test itself being
            // cancelled. A caller's structured-concurrency cancel is not an SDK failure (WR-04).
            val outcome = runCatching { store().load(::concat) }

            assertThat(outcome.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        }

    // ---- save: the Activity bound and the open triple ------------------------------------

    @Test
    fun saveWithoutAForegroundActivityReturnsFalseAndNeverTouchesTheSdk() =
        runTest(testDispatcher) {
            coEvery { activityProvider.awaitActivity() } coAnswers { awaitCancellation() }

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.NO_ACTIVITY))
            verify(exactly = 0) { PlayGames.getSnapshotsClient(any()) }
        }

    @Test
    fun saveOpensTheSameNameCreateFlagAndPolicyTripleThatLoadDoes() =
        runTest(testDispatcher) {
            store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            verify(exactly = 1) {
                client.open(
                    CloudSaveConstants.SNAPSHOT_NAME,
                    true,
                    SnapshotsClient.RESOLUTION_POLICY_MANUAL,
                )
            }
        }

    // ---- save: what actually gets written --------------------------------------------------

    @Test
    fun saveFoldsTheBytesAlreadyOnTheSnapshotIntoWhatItWrites() =
        runTest(testDispatcher) {
            val written = slot<ByteArray>()
            every { contents.writeBytes(capture(written)) } returns true

            store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            // Bytes that arrived during conflict resolution are already in the server copy by the
            // time the handle comes back open. Writing the payload alone would undo, at commit
            // time, the resolution that had just succeeded.
            assertThat(written.captured).isEqualTo(REMOTE_BYTES + PAYLOAD)
        }

    @Test
    fun saveOnAnEmptySnapshotWritesThePayloadVerbatim() =
        runTest(testDispatcher) {
            every { contents.readFully() } returns ByteArray(0)
            val written = slot<ByteArray>()
            every { contents.writeBytes(capture(written)) } returns true
            var merged = false

            store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS) { first, second ->
                merged = true
                concat(first, second)
            }

            assertThat(written.captured).isEqualTo(PAYLOAD)
            // Merging against nothing would be a no-op at best; asserting it is skipped pins that
            // a first-ever push cannot be perturbed by a merge it never needed.
            assertThat(merged).isFalse()
        }

    // ---- save: the listing metadata (CLOUD-03) ----------------------------------------------

    @Test
    fun theCommitCarriesTheDescriptionThePlayedTimeAndTheCover() =
        runTest(testDispatcher) {
            coEvery { coverImageSource.coverOrNull() } returns mockk<Bitmap>()
            val change = slot<SnapshotMetadataChange>()
            every { client.commitAndClose(any(), capture(change)) } returns Tasks.forResult(metadata)

            store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(change.captured.description).isEqualTo(DESCRIPTION_LINE)
            assertThat(change.captured.playedTimeMillis).isEqualTo(PLAYED_TIME_MS)
            assertThat(change.captured.coverImage).isNotNull()
        }

    @Test
    fun aNullCoverOmitsTheArtAndStillCommits() =
        runTest(testDispatcher) {
            coEvery { coverImageSource.coverOrNull() } returns null
            val change = slot<SnapshotMetadataChange>()
            every { client.commitAndClose(any(), capture(change)) } returns Tasks.forResult(metadata)

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            // A save without decoration is worth immeasurably more than no save at all, so an
            // unavailable cover degrades the listing and never the push (T-13-26).
            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.COMMITTED))
            assertThat(change.captured.coverImage).isNull()
            assertThat(change.captured.description).isEqualTo(DESCRIPTION_LINE)
        }

    // ---- save: the terminal-call guarantee ---------------------------------------------------

    @Test
    fun aSuccessfulSaveCommitsExactlyOnceAndNeverDiscards() =
        runTest(testDispatcher) {
            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.COMMITTED))
            verify(exactly = 1) { client.commitAndClose(snapshot, any()) }
            verify(exactly = 0) { client.discardAndClose(any()) }
        }

    @Test
    fun aFailingCommitReturnsFalseAndStillCloses() =
        runTest(testDispatcher) {
            every {
                client.commitAndClose(any(), any())
            } returns Tasks.forException(IllegalStateException(SDK_FAILURE))

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.COMMIT_FAILED))
            // Exactly one terminal call, never both and never neither: a handle left open would
            // fail every later open for the rest of the process (Pitfall 1).
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    // ---- save: the runtime cap guard ----------------------------------------------------------

    @Test
    fun aPayloadLargerThanTheServerCapAbortsThePushWithoutCommitting() =
        runTest(testDispatcher) {
            every { contents.readFully() } returns ByteArray(0)
            every { client.getMaxDataSize() } returns Tasks.forResult(TINY_SERVER_CAP_BYTES)

            val outcome = store().save(OVERSIZED_PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            // The ROADMAP's "confirm it at runtime", implemented as an enforced guard: committing
            // a payload the server will reject would burn the push and leave the handle to be
            // cleaned up by the failure path instead of by the decision (T-13-39).
            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.OVER_CAP))
            verify(exactly = 0) { client.commitAndClose(any(), any()) }
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun aFailingCapQueryDoesNotAbortThePush() =
        runTest(testDispatcher) {
            every { client.getMaxDataSize() } returns Tasks.forException(IllegalStateException(SDK_FAILURE))

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            // A guard, not a precondition: the query is documented to be able to fail, and a guard
            // that cannot answer must not become a reason to drop a save.
            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.COMMITTED))
            verify(exactly = 1) { client.commitAndClose(snapshot, any()) }
        }

    // ---- save: failure classification -----------------------------------------------------------

    @Test
    fun aThrownSdkCallOnTheWritePathYieldsFalseAndStillClosesTheSnapshot() =
        runTest(testDispatcher) {
            every { contents.writeBytes(any()) } throws IllegalStateException(SDK_FAILURE)

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.FAILED_OTHER))
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun anUnresolvableConflictOnThePushPathYieldsFalse() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())
            every { client.resolveConflict(any(), any(), any(), any()) } answers { Tasks.forResult(conflictResult()) }

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.CONFLICT_UNRESOLVED))
            verify(exactly = 0) { client.commitAndClose(any(), any()) }
        }

    @Test
    fun cancellationPropagatesOutOfSaveRatherThanBeingClassifiedAsAFailure() =
        runTest(testDispatcher) {
            every { contents.readFully() } throws CancellationException(CANCELLED)

            val attempt = runCatching { store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat) }

            assertThat(attempt.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        }

    // ---- save: the outcome names the stage and carries the status code, and nothing else ----

    @Test
    fun aRefusedOpenReportsTheOpenStageWithTheSdkStatusCode() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forException(apiFailure())

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            // The status code is the datum that discriminates: OPEN_FAILED alone cannot tell a
            // disabled feature from a network or an auth refusal. Equality on the whole value is
            // also the privacy assertion — the exception's message went nowhere.
            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.OPEN_FAILED, API_STATUS_CODE))
            verify(exactly = 0) { client.discardAndClose(any()) }
        }

    @Test
    fun anOpenThatFailsWithoutAnApiExceptionReportsTheOpenStageWithNoStatusCode() =
        runTest(testDispatcher) {
            every {
                client.open(any<String>(), any(), any())
            } returns Tasks.forException(IllegalStateException(SDK_FAILURE))

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.OPEN_FAILED))
        }

    @Test
    fun aRefusedConflictResolutionReportsTheConflictStageWithTheSdkStatusCode() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())
            every { client.resolveConflict(any(), any(), any(), any()) } returns Tasks.forException(apiFailure())

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome)
                .isEqualTo(CloudSaveOutcome(CloudSaveStage.CONFLICT_UNRESOLVED, API_STATUS_CODE))
        }

    @Test
    fun aRefusedCommitReportsTheCommitStageWithTheSdkStatusCodeAndStillCloses() =
        runTest(testDispatcher) {
            every { client.commitAndClose(any(), any()) } returns Tasks.forException(apiFailure())

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.COMMIT_FAILED, API_STATUS_CODE))
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun anApiExceptionOutsideTheThreeAwaitsStillSurfacesItsStatusCode() =
        runTest(testDispatcher) {
            every { contents.readFully() } throws apiFailure()

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.FAILED_OTHER, API_STATUS_CODE))
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun aRefusedOpenOnTheReadPathIsStillJustNull() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forException(apiFailure())

            // `load` shares the open with `save` but deliberately reports nothing further: the
            // push outcome diagnoses the shared open, and "stay local" needs no reason.
            assertThat(store().load(::concat).bytes).isNull()
        }

    @Test
    fun cancellationDuringAnAwaitPropagatesRatherThanBecomingAStage() =
        runTest(testDispatcher) {
            every {
                client.commitAndClose(any(), any())
            } returns Tasks.forException(CancellationException(CANCELLED))

            val attempt = runCatching { store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat) }

            assertThat(attempt.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
        }

    // ---- the SDK's real DataOrConflict contract (device diagnosis, plan 13-11) ----------------

    @Test
    fun theSdkThrowsFromGetConflictWhenThereIsNoConflict() {
        // Pins the fixture to the pinned SDK (play-services-games-v2 22.0.0), whose bytecode
        // reads `if (!isConflict()) throw IllegalStateException("getConflict called when there
        // is no conflict.")`. It never returns null, whatever a nullable-looking getter suggests.
        val attempt = runCatching { dataResult(snapshot).conflict }

        assertThat(attempt.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun theSdkThrowsFromGetDataWhenThereIsAConflict() {
        val attempt = runCatching { conflictResult().data }

        assertThat(attempt.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun aPushOnTheNoConflictPathCommits() =
        runTest(testDispatcher) {
            val opened = dataResult(snapshot)
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(opened)

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            // The normal path of every push: no conflict, and an SDK value whose `conflict`
            // getter throws. This is the exact case that ended at FAILED_OTHER on both devices.
            assertThat(opened.isConflict).isFalse()
            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.COMMITTED))
            verify(exactly = 1) { client.commitAndClose(snapshot, any()) }
            verify(exactly = 0) { client.discardAndClose(any()) }
        }

    @Test
    fun aPullOnTheNoConflictPathReturnsTheBytes() =
        runTest(testDispatcher) {
            val opened = dataResult(snapshot)
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(opened)

            val result = store().load(::concat).bytes

            assertThat(opened.isConflict).isFalse()
            assertThat(result).isEqualTo(REMOTE_BYTES)
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun aConflictedPushResolvesOnceAndThenCommitsTheResolvedSnapshot() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.COMMITTED))
            verify(exactly = 1) { client.resolveConflict(any(), any(), any(), any()) }
            verify(exactly = 1) { client.commitAndClose(snapshot, any()) }
        }

    @Test
    fun aPushThatGivesUpOnAConflictReportsTheConflictStageAndClosesTheServerVersion() =
        runTest(testDispatcher) {
            val server = snapshotOf(SERVER_BYTES)
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult(server))
            every {
                client.resolveConflict(any(), any(), any(), any())
            } answers { Tasks.forResult(conflictResult(server)) }

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.CONFLICT_UNRESOLVED))
            // Pins today's choice for the auditor's F-4: the give-up path gives a terminal call
            // to the server's version and to nothing else. Whether the modified version needs
            // one too is not something the SDK's javadoc or bytecode settles.
            verify(exactly = 1) { client.discardAndClose(any()) }
            verify(exactly = 1) { client.discardAndClose(server) }
        }

    // ---- ownership between the open and the caller's try/finally ------------------------------

    @Test
    fun aServerVersionThatCannotBeReadMidResolutionStillClosesTheConflictHandle() =
        runTest(testDispatcher) {
            val server = snapshotOf(SERVER_BYTES)
            every { server.snapshotContents.readFully() } throws IOException(SDK_FAILURE)
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult(server))

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.FAILED_OTHER))
            // The open succeeded, so a handle exists; a throw before the caller's try/finally
            // owns anything must not be a licence to leak it (Pitfall 1).
            verify(exactly = 1) { client.discardAndClose(server) }
        }

    @Test
    fun aMergeThatThrowsMidResolutionStillClosesTheConflictHandle() =
        runTest(testDispatcher) {
            val server = snapshotOf(SERVER_BYTES)
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult(server))

            val result = store().load { _, _ -> error(SDK_FAILURE) }.bytes

            assertThat(result).isNull()
            verify(exactly = 1) { client.discardAndClose(server) }
        }

    @Test
    fun aRefusedConflictResolutionStillClosesTheConflictHandle() =
        runTest(testDispatcher) {
            val server = snapshotOf(SERVER_BYTES)
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult(server))
            every { client.resolveConflict(any(), any(), any(), any()) } returns Tasks.forException(apiFailure())

            val outcome = store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome)
                .isEqualTo(CloudSaveOutcome(CloudSaveStage.CONFLICT_UNRESOLVED, API_STATUS_CODE))
            verify(exactly = 1) { client.discardAndClose(server) }
        }

    @Test
    fun cancellationMidResolutionClosesTheConflictHandleAndStillPropagates() =
        runTest(testDispatcher) {
            val server = snapshotOf(SERVER_BYTES)
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult(server))
            every {
                client.resolveConflict(any(), any(), any(), any())
            } returns Tasks.forException(CancellationException(CANCELLED))

            val attempt = runCatching { store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat) }

            // Closing the handle must never come at the price of swallowing the cancel (WR-04).
            assertThat(attempt.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
            verify(exactly = 1) { client.discardAndClose(server) }
        }

    // ---- load: the pull probe (what the opened snapshot says about itself) -----------------------

    @Test
    fun aPullReportsWhetherTheOpenedSnapshotStillHasChangesThatNeverReachedTheServer() =
        runTest(testDispatcher) {
            // `hasChangePending()` is the SDK's own answer to "did the last commit upload?" — the
            // one question a locally-COMMITTED push outcome cannot answer.
            every { metadata.hasChangePending() } returns true

            val probe = store().load(::concat).probe

            assertThat(probe).isEqualTo(
                CloudPullProbe(changePending = true, conflictsResolved = 0, lastModifiedAt = LAST_MODIFIED_AT),
            )
        }

    @Test
    fun aPullThatResolvedConflictsCountsThem() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())
            every { client.resolveConflict(any(), any(), any(), any()) } returnsMany
                listOf(Tasks.forResult(conflictResult()), Tasks.forResult(dataResult(snapshot)))

            val probe = store().load(::concat).probe

            assertThat(probe?.conflictsResolved).isEqualTo(2)
        }

    @Test
    fun aPullOfAnEmptySnapshotStillReportsItsProbe() =
        runTest(testDispatcher) {
            // "No payload" and "the snapshot never opened" are the same instruction to the caller
            // and different facts to a diagnostic: only the second has nothing to say.
            every { contents.readFully() } returns ByteArray(0)

            val pull = store().load(::concat)

            assertThat(pull.bytes).isNull()
            assertThat(pull.probe).isNotNull()
        }

    @Test
    fun aPullThatNeverOpenedTheSnapshotHasNoProbe() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forException(apiFailure())

            assertThat(store().load(::concat).probe).isNull()
        }

    @Test
    fun aPullWithoutAForegroundActivityHasNoProbe() =
        runTest(testDispatcher) {
            coEvery { activityProvider.awaitActivity() } coAnswers { awaitCancellation() }

            assertThat(store().load(::concat).probe).isNull()
        }

    @Test
    fun aProbeThatCannotBeReadNeverCostsThePullItsBytes() =
        runTest(testDispatcher) {
            // A diagnostic is decoration on a restore. Metadata the SDK refuses to hand over must
            // not turn a readable cloud save into "stay local".
            every { metadata.hasChangePending() } throws IllegalStateException(SDK_FAILURE)

            val pull = store().load(::concat)

            assertThat(pull.bytes).isEqualTo(REMOTE_BYTES)
            assertThat(pull.probe).isNull()
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun theProbeNeverReadsAnythingThatNamesThePlayerTheDeviceOrTheSave() =
        runTest(testDispatcher) {
            store().load(::concat)

            // T-11-04 / T-13-40: no SDK string is read, so none can be stored. The device name is
            // in this list on purpose — see the KDoc on CloudPullProbe for why it was dropped.
            verify(exactly = 0) { metadata.deviceName }
            verify(exactly = 0) { metadata.description }
            verify(exactly = 0) { metadata.uniqueName }
            verify(exactly = 0) { metadata.snapshotId }
            verify(exactly = 0) { metadata.owner }
            verify(exactly = 0) { metadata.coverImageUri }
        }

    // ---- cancellation is never absorbed by a cleanup or a guard ---------------------------------

    @Test
    fun aPullCancelledWhileItsHandleIsClosingDoesNotReturnNormally() =
        runTest(testDispatcher) {
            var returnedNormally = false
            lateinit var pass: Job
            every { client.discardAndClose(any()) } answers {
                pass.cancel()
                Tasks.forResult(null)
            }

            pass =
                launch {
                    store().load(::concat)
                    returnedNormally = true
                }
            pass.join()

            // The close itself must complete — the handle is real — but a cancelled pass that
            // then hands bytes to its caller keeps working after it was told to stop.
            assertThat(returnedNormally).isFalse()
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun aPushCancelledWhileItsHandleIsClosingDoesNotReturnNormally() =
        runTest(testDispatcher) {
            var returnedNormally = false
            lateinit var pass: Job
            every { client.getMaxDataSize() } returns Tasks.forResult(TINY_SERVER_CAP_BYTES)
            every { client.discardAndClose(any()) } answers {
                pass.cancel()
                Tasks.forResult(null)
            }

            pass =
                launch {
                    store().save(OVERSIZED_PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)
                    returnedNormally = true
                }
            pass.join()

            assertThat(returnedNormally).isFalse()
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun cancellationDuringTheCapQueryPropagatesAndNeverCommits() =
        runTest(testDispatcher) {
            every { client.getMaxDataSize() } returns Tasks.forException(CancellationException(CANCELLED))

            val attempt = runCatching { store().save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat) }

            // "A failed query is not a failed push" covers SDK failures. A cancel is not one: a
            // guard that absorbed it would go on to commit on behalf of a pass that was stopped.
            assertThat(attempt.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
            verify(exactly = 0) { client.commitAndClose(any(), any()) }
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    // ---- a close that failed is visible on the next push ---------------------------------------

    @Test
    fun aPullWhoseCloseFailedLeavesTheNextPushToReportTheRefusedOpen() =
        runTest(testDispatcher) {
            val store = store()
            every { client.discardAndClose(any()) } returns Tasks.forException(apiFailure())

            val pulled = store.load(::concat).bytes

            // The pull itself is still a success: the bytes were read before the close failed.
            assertThat(pulled).isEqualTo(REMOTE_BYTES)

            // What a leaked handle does next is refuse the open (Pitfall 1). The push must say
            // exactly that, with the SDK's integer, rather than anything vaguer.
            every { client.open(any<String>(), any(), any()) } returns Tasks.forException(apiFailure())

            val outcome = store.save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS, ::concat)

            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.OPEN_FAILED, API_STATUS_CODE))
        }

    // ---- fixtures ------------------------------------------------------------------------

    private fun store() =
        PlayGamesCloudSaveStore(
            activityProvider = activityProvider,
            coverImageSource = coverImageSource,
            descriptionFormatter = descriptionFormatter,
        )

    /**
     * A non-conflicted open: the SDK hands back one open [Snapshot].
     *
     * **A real `DataOrConflict`, never a mock.** The class is final with a public constructor, so
     * the fixture *is* the pinned SDK's behaviour: `getConflict()` on this value throws
     * `IllegalStateException`, it does not return `null`. A mock stubbed to return `null` here is
     * what let a store that read `conflict` as a nullable discriminator pass 33 tests and fail
     * every push and every pull on a device.
     */
    private fun dataResult(value: Snapshot): SnapshotsClient.DataOrConflict<Snapshot> =
        SnapshotsClient.DataOrConflict(value, null)

    /**
     * A conflicted open: two versions of the snapshot plus the contents the merge is written
     * into. Built fresh per call so the retry-bound test can hand back a new one every time.
     * Real SDK value types for the same reason as [dataResult]: `getData()` on this throws.
     */
    private fun conflictResult(
        server: Snapshot = snapshotOf(SERVER_BYTES),
        modified: Snapshot = snapshotOf(MODIFIED_BYTES),
    ): SnapshotsClient.DataOrConflict<Snapshot> =
        SnapshotsClient.DataOrConflict(
            null,
            SnapshotsClient.SnapshotConflict(server, CONFLICT_ID, modified, resolutionContents),
        )

    /**
     * The failure shape GMS reports a refusal in. It carries a message on purpose: the outcome
     * assertions compare whole values, so a message that leaked into one would fail them.
     */
    private fun apiFailure() = ApiException(Status(API_STATUS_CODE, SDK_FAILURE))

    private fun snapshotOf(bytes: ByteArray): Snapshot {
        val snapshotContents = mockk<SnapshotContents>()
        every { snapshotContents.readFully() } returns bytes
        val snapshotMetadata = mockk<SnapshotMetadata>()
        every { snapshotMetadata.snapshotId } returns SNAPSHOT_ID
        val value = mockk<Snapshot>()
        every { value.snapshotContents } returns snapshotContents
        every { value.metadata } returns snapshotMetadata
        return value
    }
}

/**
 * The merge the tests supply: concatenation is *order-revealing*, so the conflict-orientation
 * test can tell which argument the server's bytes arrived in — something a max-style merge over
 * opaque bytes could never show.
 */
private fun concat(
    first: ByteArray,
    second: ByteArray,
): ByteArray = first + second

private val REMOTE_BYTES = "remote".toByteArray()
private val SERVER_BYTES = "server".toByteArray()
private val MODIFIED_BYTES = "mine".toByteArray()
private val PAYLOAD = "payload".toByteArray()

/** Comfortably larger than [TINY_SERVER_CAP_BYTES], so the cap guard has something to reject. */
private val OVERSIZED_PAYLOAD = ByteArray(100) { 'x'.code.toByte() }

private const val SNAPSHOT_ID = "placeholder-snapshot-id"
private const val CONFLICT_ID = "placeholder-conflict-id"
private const val DESCRIPTION_LINE = "Challenge level 7 - 21 stars - 3/5 Classic boards"
private const val SDK_FAILURE = "sdk failure"
private const val CANCELLED = "cancelled"

/** An arbitrary GMS status integer; the store must pass it through untouched, whatever it is. */
private const val API_STATUS_CODE = 4_002
private const val PLAYED_TIME_MS = 987_654L

/** An arbitrary epoch-millis value for the opened snapshot's last modification. */
private const val LAST_MODIFIED_AT = 1_789_000_000_000L

/** The documented floor: the javadoc guarantees at least 3 MB per snapshot. */
private const val SERVER_CAP_BYTES = 3 * 1024 * 1024

/** A contract-violating server answer — the only thing that can make the guard fire. */
private const val TINY_SERVER_CAP_BYTES = 10

private val DESCRIPTION =
    CloudSaveDescription(
        ladder = 7,
        totalStars = 21,
        classicCleared = 3,
    )
