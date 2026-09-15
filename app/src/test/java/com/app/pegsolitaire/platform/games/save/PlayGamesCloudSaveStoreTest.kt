package com.app.pegsolitaire.platform.games.save

import android.app.Activity
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
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
import kotlinx.coroutines.awaitCancellation
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
 * **The third rule: nothing ever throws out of the seam.** No Activity, a thrown SDK call and a
 * conflict that never resolves are all `null` (CLOUD-05) — with the single documented exception
 * of `CancellationException`, which is a caller's structured concurrency and not an SDK failure
 * (WR-04).
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

            val result = store().load(::concat)

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
            val result = store().load(::concat)

            assertThat(result).isEqualTo(REMOTE_BYTES)
        }

    @Test
    fun loadOnAnEmptySnapshotReturnsNullRatherThanAnEmptyArray() =
        runTest(testDispatcher) {
            every { contents.readFully() } returns ByteArray(0)

            val result = store().load(::concat)

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

            val result = store().load(::concat)

            assertThat(result).isEqualTo(REMOTE_BYTES)
        }

    @Test
    fun aConflictThatKeepsConflictingStopsAtTheRetryBound() =
        runTest(testDispatcher) {
            every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(conflictResult())
            every { client.resolveConflict(any(), any(), any(), any()) } answers { Tasks.forResult(conflictResult()) }

            val result = store().load(::concat)

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

            val result = store().load(::concat)

            assertThat(result).isNull()
            verify(exactly = 1) { client.discardAndClose(snapshot) }
        }

    @Test
    fun aFailedOpenYieldsNullWithNothingToClose() =
        runTest(testDispatcher) {
            every {
                client.open(any<String>(), any(), any())
            } returns Tasks.forException(IllegalStateException(SDK_FAILURE))

            val result = store().load(::concat)

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

    // ---- fixtures ------------------------------------------------------------------------

    private fun store() =
        PlayGamesCloudSaveStore(
            activityProvider = activityProvider,
            coverImageSource = coverImageSource,
            descriptionFormatter = descriptionFormatter,
        )

    /** A non-conflicted open: the SDK hands back one open [Snapshot]. */
    private fun dataResult(value: Snapshot): SnapshotsClient.DataOrConflict<Snapshot> {
        val result = mockk<SnapshotsClient.DataOrConflict<Snapshot>>()
        every { result.isConflict } returns false
        // `getConflict()` is `@Nullable` and is null exactly when `isConflict()` is false; the
        // store reads it as the discriminator, so the fixture must model both halves.
        every { result.conflict } returns null
        every { result.data } returns value
        return result
    }

    /**
     * A conflicted open: two versions of the snapshot plus the contents the merge is written
     * into. Built fresh per call so the retry-bound test can hand back a new one every time.
     */
    private fun conflictResult(): SnapshotsClient.DataOrConflict<Snapshot> {
        val conflict = mockk<SnapshotsClient.SnapshotConflict>()
        every { conflict.snapshot } returns snapshotOf(SERVER_BYTES)
        every { conflict.conflictingSnapshot } returns snapshotOf(MODIFIED_BYTES)
        every { conflict.conflictId } returns CONFLICT_ID
        every { conflict.resolutionSnapshotContents } returns resolutionContents

        val result = mockk<SnapshotsClient.DataOrConflict<Snapshot>>()
        every { result.isConflict } returns true
        every { result.conflict } returns conflict
        return result
    }

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
private const val SNAPSHOT_ID = "placeholder-snapshot-id"
private const val CONFLICT_ID = "placeholder-conflict-id"
private const val DESCRIPTION_LINE = "Challenge level 7 - 21 stars - 3/5 Classic boards"
private const val SDK_FAILURE = "sdk failure"
private const val CANCELLED = "cancelled"
private const val SERVER_CAP_BYTES = 3 * 1024 * 1024
