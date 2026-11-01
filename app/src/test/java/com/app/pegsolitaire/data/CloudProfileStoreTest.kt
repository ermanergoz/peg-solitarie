package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pegsolitaire.testing.InMemoryPreferencesDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * Unit coverage of [CloudProfileStore] — D-12's "last player id this device merged with".
 *
 * Same harness as [SubmissionSyncStoreTest] and [CloudSyncStoreTest]: an
 * [InMemoryPreferencesDataStore] for the default and the round trip, a deliberately-throwing
 * store for the fail-open halves.
 *
 * The fail-open direction is the point of this file (T-13-04). `null` — from a fresh
 * install, a first-ever sign-in, or a preferences file the store cannot read — means
 * "merge silently per D-02". It must never mean "show the profile-swap dialog", because
 * that dialog owns the only destructive path in the app (D-13/D-14's "Replace").
 */
class CloudProfileStoreTest {
    @Test
    fun aFreshStoreHasNoMergedPlayerId() =
        runTest {
            val store = CloudProfileStore(InMemoryPreferencesDataStore())

            // D-12: a fresh install has never merged with anyone — merge silently, no dialog.
            assertThat(store.mergedPlayerId.first()).isNull()
        }

    @Test
    fun settingTheMergedPlayerIdPersistsIt() =
        runTest {
            val store = CloudProfileStore(InMemoryPreferencesDataStore())

            store.setMergedPlayerId(PLAYER_A)

            assertThat(store.mergedPlayerId.first()).isEqualTo(PLAYER_A)
        }

    @Test
    fun settingTheMergedPlayerIdOverwritesThePreviousOne() =
        runTest {
            // D-15: after the swap dialog is answered either way, the stored id becomes the
            // signed-in one. There is no un-remembering, only overwriting.
            val store = CloudProfileStore(InMemoryPreferencesDataStore())
            store.setMergedPlayerId(PLAYER_A)

            store.setMergedPlayerId(PLAYER_B)

            assertThat(store.mergedPlayerId.first()).isEqualTo(PLAYER_B)
        }

    @Test
    fun aFailingReadFailsOpenToNoMergedPlayerId() =
        runTest {
            // T-13-04: a damaged preferences file reads as null, and null is the *silent
            // merge*, not the dialog that can wipe the device. This is the direction that
            // keeps a corrupt store from offering a player the destructive path.
            val store = CloudProfileStore(throwingDataStore())

            assertThat(store.mergedPlayerId.first()).isNull()
        }

    @Test
    fun aFailingWriteIsDroppedRatherThanThrown() =
        runTest {
            val store = CloudProfileStore(throwingDataStore())

            store.setMergedPlayerId(PLAYER_A)

            assertThat(store.mergedPlayerId.first()).isNull()
        }

    @Test
    fun aCancelledWriteStillPropagatesAsCancellation() =
        runTest {
            // The fail-open guard must not swallow structured concurrency's own signal.
            val store = CloudProfileStore(cancellingDataStore())

            val thrown = runCatching { store.setMergedPlayerId(PLAYER_A) }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
        }

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
        /** Stand-in ids; the real value is `GamesSignInState.SignedIn.playerId`. */
        const val PLAYER_A = "player-a"
        const val PLAYER_B = "player-b"
    }
}
