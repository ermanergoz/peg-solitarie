package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.app.pegsolitaire.platform.games.save.CloudPullProbe
import com.app.pegsolitaire.platform.games.save.CloudSaveOutcome
import com.app.pegsolitaire.platform.games.save.CloudSaveStage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

/**
 * Unit coverage of [CloudSyncStore] — the one persisted boolean behind D-01's cloud-push
 * retry.
 *
 * Built from [SubmissionSyncStoreTest]'s harness, which is the shape this store copies: a
 * real Preferences store over a temp file proves the default and the round trip, and a
 * deliberately-throwing store proves the read and the write both **fail open**.
 *
 * The last case is the one that justifies this file existing separately at all — the
 * cloud marker and the submissions marker are independent, because a pass can succeed at
 * the leaderboards and fail at the snapshot.
 */
class CloudSyncStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun aFreshStoreHasNoPushPending() =
        runTest {
            val store = CloudSyncStore(realDataStore())

            // D-01 inherits D-10's "a brand-new install makes no calls at launch".
            assertThat(store.pushPending.first()).isFalse()
        }

    @Test
    fun settingPushPendingPersistsTrue() =
        runTest {
            val store = CloudSyncStore(realDataStore())

            store.setPushPending(true)

            assertThat(store.pushPending.first()).isTrue()
        }

    @Test
    fun clearingPushPendingPersistsFalse() =
        runTest {
            val store = CloudSyncStore(realDataStore())
            store.setPushPending(true)

            store.setPushPending(false)

            assertThat(store.pushPending.first()).isFalse()
        }

    @Test
    fun aFailingReadFailsOpenToNoPushPending() =
        runTest {
            // A corrupt/IO read degrades to "no push outstanding" rather than propagating
            // into a fire-and-forget pass. The cost is at most one solve's delay before the
            // next recorded result re-sets the marker.
            val store = CloudSyncStore(throwingDataStore())

            assertThat(store.pushPending.first()).isFalse()
        }

    @Test
    fun aFailingWriteIsDroppedRatherThanThrown() =
        runTest {
            // T-13-06: the marker write happens inside an application-scoped launch with no
            // handler of its own; an unguarded IOException there would take the process
            // down long after the player left the Results screen.
            val store = CloudSyncStore(throwingDataStore())

            store.setPushPending(true)

            assertThat(store.pushPending.first()).isFalse()
        }

    @Test
    fun aCancelledWriteStillPropagatesAsCancellation() =
        runTest {
            // The fail-open guard must not swallow structured concurrency's own signal.
            val store = CloudSyncStore(cancellingDataStore())

            val thrown = runCatching { store.setPushPending(true) }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }

    @Test
    fun recordingAFailedPushPersistsTheMarkerTheStageNameAndTheStatusCode() =
        runTest {
            val dataStore = realDataStore()
            val store = CloudSyncStore(dataStore)

            store.recordPushOutcome(CloudSaveOutcome(CloudSaveStage.OPEN_FAILED, STATUS_CODE))

            val stored = dataStore.data.first()
            assertThat(store.pushPending.first()).isTrue()
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PUSH_STAGE_KEY]).isEqualTo("OPEN_FAILED")
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PUSH_STATUS_KEY]).isEqualTo(STATUS_CODE)
        }

    @Test
    fun recordingACommittedPushClearsTheMarkerAndRemovesAStaleStatusCode() =
        runTest {
            val dataStore = realDataStore()
            val store = CloudSyncStore(dataStore)
            store.recordPushOutcome(CloudSaveOutcome(CloudSaveStage.OPEN_FAILED, STATUS_CODE))

            store.recordPushOutcome(CloudSaveOutcome(CloudSaveStage.COMMITTED))

            // "Absent when null": a status code left over from an earlier refusal, sitting
            // beside COMMITTED, would be read as belonging to the push that succeeded.
            val stored = dataStore.data.first()
            assertThat(store.pushPending.first()).isFalse()
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PUSH_STAGE_KEY]).isEqualTo("COMMITTED")
            assertThat(stored.contains(PlayGamesPreferences.CLOUD_LAST_PUSH_STATUS_KEY)).isFalse()
        }

    @Test
    fun anOutcomeIsRecordedInASingleEditSoTheThreeKeysCannotDisagree() =
        runTest {
            val dataStore = CountingDataStore(realDataStore())
            val store = CloudSyncStore(dataStore)

            store.recordPushOutcome(CloudSaveOutcome(CloudSaveStage.COMMIT_FAILED, STATUS_CODE))

            assertThat(dataStore.updateCount).isEqualTo(1)
        }

    @Test
    fun theDiagnosticKeysKeepTheNamesTheDeviceToolReads() {
        // `tools/prefs.py` and the QA brief name these literally; a rename here would make the
        // next device pass read "absent" for a value that was written.
        assertThat(PlayGamesPreferences.CLOUD_LAST_PUSH_STAGE_KEY.name)
            .isEqualTo("play_games_cloud_last_push_stage")
        assertThat(PlayGamesPreferences.CLOUD_LAST_PUSH_STATUS_KEY.name)
            .isEqualTo("play_games_cloud_last_push_status")
    }

    @Test
    fun aFailingOutcomeWriteIsDroppedRatherThanThrown() =
        runTest {
            val store = CloudSyncStore(throwingDataStore())

            store.recordPushOutcome(CloudSaveOutcome(CloudSaveStage.OPEN_FAILED, STATUS_CODE))

            assertThat(store.pushPending.first()).isFalse()
        }

    @Test
    fun aCancelledOutcomeWriteStillPropagatesAsCancellation() =
        runTest {
            val store = CloudSyncStore(cancellingDataStore())

            val thrown =
                runCatching {
                    store.recordPushOutcome(CloudSaveOutcome(CloudSaveStage.OPEN_FAILED, STATUS_CODE))
                }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }

    // ---- The pull probe ----------------------------------------------------------------

    @Test
    fun recordingAPullProbePersistsItsThreeValuesAndThatTheSnapshotOpened() =
        runTest {
            val dataStore = realDataStore()
            val store = CloudSyncStore(dataStore)

            store.recordPullProbe(PROBE)

            val stored = dataStore.data.first()
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PULL_OPENED_KEY]).isTrue()
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PULL_CHANGE_PENDING_KEY]).isTrue()
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PULL_CONFLICTS_RESOLVED_KEY]).isEqualTo(CONFLICTS)
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PULL_LAST_MODIFIED_AT_KEY]).isEqualTo(MODIFIED_AT)
        }

    @Test
    fun recordingAPullThatNeverOpenedRemovesTheStaleValuesOfTheLastOneThatDid() =
        runTest {
            val dataStore = realDataStore()
            val store = CloudSyncStore(dataStore)
            store.recordPullProbe(PROBE)

            store.recordPullProbe(null)

            // "Absent when unknown", the status key's rule: values left over from an earlier pull,
            // sitting beside opened = false, would be read as describing the pull that failed.
            val stored = dataStore.data.first()
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PULL_OPENED_KEY]).isFalse()
            assertThat(stored.contains(PlayGamesPreferences.CLOUD_LAST_PULL_CHANGE_PENDING_KEY)).isFalse()
            assertThat(stored.contains(PlayGamesPreferences.CLOUD_LAST_PULL_CONFLICTS_RESOLVED_KEY)).isFalse()
            assertThat(stored.contains(PlayGamesPreferences.CLOUD_LAST_PULL_LAST_MODIFIED_AT_KEY)).isFalse()
        }

    @Test
    fun aPullProbeIsRecordedInASingleEditSoItsKeysCannotDisagree() =
        runTest {
            val dataStore = CountingDataStore(realDataStore())
            val store = CloudSyncStore(dataStore)

            store.recordPullProbe(PROBE)

            assertThat(dataStore.updateCount).isEqualTo(1)
        }

    @Test
    fun recordingAPullProbeLeavesThePushMarkerAndThePushDiagnosticAlone() =
        runTest {
            val dataStore = realDataStore()
            val store = CloudSyncStore(dataStore)
            store.recordPushOutcome(CloudSaveOutcome(CloudSaveStage.OPEN_FAILED, STATUS_CODE))

            store.recordPullProbe(PROBE)

            val stored = dataStore.data.first()
            assertThat(store.pushPending.first()).isTrue()
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PUSH_STAGE_KEY]).isEqualTo("OPEN_FAILED")
            assertThat(stored[PlayGamesPreferences.CLOUD_LAST_PUSH_STATUS_KEY]).isEqualTo(STATUS_CODE)
        }

    @Test
    fun thePullProbeKeysKeepTheNamesTheDeviceToolReads() {
        // `tools/prefs.py` prints every `play_games*` key; QA briefs quote these four literally.
        assertThat(PlayGamesPreferences.CLOUD_LAST_PULL_OPENED_KEY.name)
            .isEqualTo("play_games_cloud_last_pull_opened")
        assertThat(PlayGamesPreferences.CLOUD_LAST_PULL_CHANGE_PENDING_KEY.name)
            .isEqualTo("play_games_cloud_last_pull_change_pending")
        assertThat(PlayGamesPreferences.CLOUD_LAST_PULL_CONFLICTS_RESOLVED_KEY.name)
            .isEqualTo("play_games_cloud_last_pull_conflicts_resolved")
        assertThat(PlayGamesPreferences.CLOUD_LAST_PULL_LAST_MODIFIED_AT_KEY.name)
            .isEqualTo("play_games_cloud_last_pull_last_modified_at")
    }

    @Test
    fun aFailingPullProbeWriteIsDroppedRatherThanThrown() =
        runTest {
            val store = CloudSyncStore(throwingDataStore())

            store.recordPullProbe(PROBE)

            assertThat(store.pushPending.first()).isFalse()
        }

    @Test
    fun aCancelledPullProbeWriteStillPropagatesAsCancellation() =
        runTest {
            val store = CloudSyncStore(cancellingDataStore())

            val thrown = runCatching { store.recordPullProbe(PROBE) }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }

    @Test
    fun aPendingCloudPushLeavesTheSubmissionsMarkerAlone() =
        runTest {
            // The reason this is a sibling store and not a fifth key on SubmissionSyncStore:
            // a pass can fail at the snapshot while every leaderboard submission succeeded,
            // and each outcome has to be recoverable on its own.
            val dataStore = realDataStore()
            val cloudStore = CloudSyncStore(dataStore)
            val submissionStore = SubmissionSyncStore(dataStore)

            cloudStore.setPushPending(true)

            assertThat(cloudStore.pushPending.first()).isTrue()
            assertThat(submissionStore.pending.first()).isFalse()
        }

    @Test
    fun aPendingSubmissionLeavesTheCloudMarkerAlone() =
        runTest {
            // …and symmetrically: leaderboards can fail while the snapshot landed.
            val dataStore = realDataStore()
            val cloudStore = CloudSyncStore(dataStore)
            val submissionStore = SubmissionSyncStore(dataStore)

            submissionStore.setPending(true)

            assertThat(submissionStore.pending.first()).isTrue()
            assertThat(cloudStore.pushPending.first()).isFalse()
        }

    private fun realDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { tempFolder.newFile(TEST_STORE_FILE) },
        )

    /** A store that fails every read AND every write with the I/O error DataStore reports. */
    private fun throwingDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk full")
        }

    /** A store whose write is cancelled underneath the caller. */
    private fun cancellingDataStore(): DataStore<Preferences> =
        object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw CancellationException("cancelled underneath the write")
        }

    /** Counts writes, so "one edit" is an assertion rather than a reading of the source. */
    private class CountingDataStore(
        private val delegate: DataStore<Preferences>,
    ) : DataStore<Preferences> {
        var updateCount = 0
            private set

        override val data: Flow<Preferences> = delegate.data

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            updateCount++
            return delegate.updateData(transform)
        }
    }

    private companion object {
        const val TEST_STORE_FILE = "cloud_sync_test.preferences_pb"

        /** An arbitrary GMS status integer; the store persists whatever it is given. */
        const val STATUS_CODE = 4_002

        const val CONFLICTS = 2
        const val MODIFIED_AT = 1_789_000_000_000L

        /** A pull that opened, resolved two conflicts and found an upload still outstanding. */
        val PROBE = CloudPullProbe(changePending = true, conflictsResolved = CONFLICTS, lastModifiedAt = MODIFIED_AT)
    }
}
