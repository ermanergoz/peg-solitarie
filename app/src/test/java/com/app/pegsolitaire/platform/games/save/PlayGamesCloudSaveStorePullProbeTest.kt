package com.app.pegsolitaire.platform.games.save

import android.app.Activity
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotContents
import com.google.android.gms.games.snapshot.SnapshotMetadata
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Behaviour spec for the **pull probe** half of [PlayGamesCloudSaveStore]: what `load` reports
 * about the snapshot it opened, beside the bytes.
 *
 * A file of its own only because `PlayGamesCloudSaveStoreTest` had reached detekt's size bound; the
 * harness is that file's, reduced to what a read needs.
 *
 * Three claims. The probe is **read off the opened snapshot's metadata** — `hasChangePending()`,
 * the last-modified time, and the loop's own conflict count. It **can never cost the pull its
 * bytes**. And it **reads no SDK string**, so there is none to store (T-11-04 / T-13-40).
 */
class PlayGamesCloudSaveStorePullProbeTest {
    private val testDispatcher = StandardTestDispatcher()
    private val activityProvider = mockk<ForegroundActivityProvider>()
    private val activity = mockk<Activity>()
    private val client = mockk<SnapshotsClient>()
    private val contents = mockk<SnapshotContents>()
    private val metadata = mockk<SnapshotMetadata>()
    private val snapshot = mockk<Snapshot>()
    private val resolutionContents = mockk<SnapshotContents>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(PlayGames::class)
        every { PlayGames.getSnapshotsClient(any()) } returns client
        coEvery { activityProvider.awaitActivity() } returns activity

        every { snapshot.snapshotContents } returns contents
        every { snapshot.metadata } returns metadata
        every { metadata.hasChangePending() } returns false
        every { metadata.lastModifiedTimestamp } returns LAST_MODIFIED_AT
        every { contents.readFully() } returns REMOTE_BYTES
        every { resolutionContents.writeBytes(any()) } returns true

        every { client.open(any<String>(), any(), any()) } returns Tasks.forResult(dataResult(snapshot))
        every { client.discardAndClose(any()) } returns Tasks.forResult(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

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

    // ---- fixtures ------------------------------------------------------------------------

    private fun store() =
        PlayGamesCloudSaveStore(
            activityProvider = activityProvider,
            coverImageSource = mockk(),
            descriptionFormatter = mockk(),
        )

    /** Real SDK value types, never mocks — see `PlayGamesCloudSaveStoreTest.dataResult` for why. */
    private fun dataResult(value: Snapshot): SnapshotsClient.DataOrConflict<Snapshot> =
        SnapshotsClient.DataOrConflict(value, null)

    private fun conflictResult(): SnapshotsClient.DataOrConflict<Snapshot> =
        SnapshotsClient.DataOrConflict(
            null,
            SnapshotsClient.SnapshotConflict(
                snapshotOf(SERVER_BYTES),
                CONFLICT_ID,
                snapshotOf(MODIFIED_BYTES),
                resolutionContents,
            ),
        )

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

private fun concat(
    server: ByteArray,
    mine: ByteArray,
): ByteArray = server + mine

private val REMOTE_BYTES = "remote".toByteArray()
private val SERVER_BYTES = "server".toByteArray()
private val MODIFIED_BYTES = "mine".toByteArray()

private const val SNAPSHOT_ID = "placeholder-snapshot-id"
private const val CONFLICT_ID = "placeholder-conflict-id"
private const val SDK_FAILURE = "sdk failure"
private const val API_STATUS_CODE = 4_002

/** An arbitrary epoch-millis value for the opened snapshot's last modification. */
private const val LAST_MODIFIED_AT = 1_789_000_000_000L
