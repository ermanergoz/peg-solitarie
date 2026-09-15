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
 * Unit coverage of [SubmissionSyncStore] — the one persisted boolean behind D-10's
 * "submissions pending" backfill.
 *
 * Built from [PlayGamesOptInRepositoryTest]'s harness for the same reason it exists
 * there: a real Preferences store over a temp file proves the default and the round trip,
 * and a deliberately-throwing store proves the read and the write both **fail open**.
 *
 * The fail-open direction here is `false` — "nothing pending" — and that choice is the
 * subject of two of the cases below. It is the *less* recoverable of the two directions
 * (see [PlayGamesPreferences.SUBMISSIONS_PENDING_KEY]'s KDoc) and is taken anyway,
 * because `true` would make every clean launch of a damaged install issue a pass D-10
 * says it must not.
 */
class SubmissionSyncStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun aFreshStoreHasNothingPending() =
        runTest {
            val store = SubmissionSyncStore(realDataStore())

            // D-10: a brand-new install makes no submission calls at launch.
            assertThat(store.pending.first()).isFalse()
        }

    @Test
    fun settingPendingPersistsTrue() =
        runTest {
            val store = SubmissionSyncStore(realDataStore())

            store.setPending(true)

            assertThat(store.pending.first()).isTrue()
        }

    @Test
    fun clearingPendingPersistsFalse() =
        runTest {
            val store = SubmissionSyncStore(realDataStore())
            store.setPending(true)

            store.setPending(false)

            assertThat(store.pending.first()).isFalse()
        }

    @Test
    fun aFailingReadFailsOpenToNothingPending() =
        runTest {
            // A corrupt/IO read degrades to "nothing pending" rather than propagating into
            // a fire-and-forget pass with no handler of its own. The cost is at most one
            // solve's delay before the next write re-sets the flag.
            val store = SubmissionSyncStore(throwingDataStore())

            assertThat(store.pending.first()).isFalse()
        }

    @Test
    fun aFailingWriteIsDroppedRatherThanThrown() =
        runTest {
            // The submitter's flag writes happen inside an application-scoped launch with
            // no handler; an unguarded IOException there would take the process down long
            // after the player left the Results screen.
            val store = SubmissionSyncStore(throwingDataStore())

            store.setPending(true)

            assertThat(store.pending.first()).isFalse()
        }

    @Test
    fun aCancelledWriteStillPropagatesAsCancellation() =
        runTest {
            // The fail-open guard must not swallow structured concurrency's own signal.
            val store = SubmissionSyncStore(cancellingDataStore())

            val thrown = runCatching { store.setPending(true) }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
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
        const val TEST_STORE_FILE = "submission_sync_test.preferences_pb"
    }
}
