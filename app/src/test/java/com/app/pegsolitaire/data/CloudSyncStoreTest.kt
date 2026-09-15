package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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

    private companion object {
        const val TEST_STORE_FILE = "cloud_sync_test.preferences_pb"
    }
}
