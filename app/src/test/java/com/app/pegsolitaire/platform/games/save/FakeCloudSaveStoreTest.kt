package com.app.pegsolitaire.platform.games.save

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Pins [FakeCloudSaveStore]'s own contract, because plans 13-08 and 13-10 assert *through* it and a
 * fake that quietly stopped doing its job would make those suites green for the wrong reason.
 *
 * Two of its behaviours are load-bearing rather than incidental, and both are tested here:
 *
 * - **`save` invokes `mergeRemote`.** It is the only place in the JVM suite where the conflict merge
 *   runs at all, since the real conflict path lives inside the SDK. A fake that ignored the lambda
 *   would let a broken merge pass every test in the project.
 * - **`remote` only advances on a successful save.** A caller's retry bookkeeping is written against
 *   exactly that: a `false` save must leave the server's payload where it was, so the next pass
 *   merges against the same bytes rather than against its own failed write.
 */
class FakeCloudSaveStoreTest {
    @Test
    fun loadReturnsTheCurrentRemoteAndCountsTheCall() =
        runTest {
            val store = FakeCloudSaveStore(remote = REMOTE)

            val loaded = store.load(CONCATENATING_MERGE)

            assertThat(loaded).isEqualTo(REMOTE)
            assertThat(store.loadCallCount).isEqualTo(1)
        }

    @Test
    fun loadReturnsNullWhenThereIsNoRemotePayload() =
        runTest {
            val store = FakeCloudSaveStore()

            assertThat(store.load(CONCATENATING_MERGE)).isNull()
        }

    @Test
    fun saveMergesAgainstTheRemotePayloadAndRecordsTheMergedBytes() =
        runTest {
            val store = FakeCloudSaveStore(remote = REMOTE)

            store.save(LOCAL, DESCRIPTION, PLAYED_TIME_MS, CONCATENATING_MERGE)

            assertThat(store.savedPayloads).hasSize(1)
            assertThat(store.savedPayloads.single()).isEqualTo(REMOTE + LOCAL)
        }

    @Test
    fun saveRecordsTheLocalPayloadVerbatimWhenThereIsNoRemoteToMergeAgainst() =
        runTest {
            val store = FakeCloudSaveStore()

            store.save(LOCAL, DESCRIPTION, PLAYED_TIME_MS, CONCATENATING_MERGE)

            assertThat(store.savedPayloads.single()).isEqualTo(LOCAL)
        }

    @Test
    fun saveRecordsTheListingMetadataAndCountsTheCall() =
        runTest {
            val store = FakeCloudSaveStore()

            store.save(LOCAL, DESCRIPTION, PLAYED_TIME_MS, CONCATENATING_MERGE)

            assertThat(store.lastDescription).isEqualTo(DESCRIPTION)
            assertThat(store.lastPlayedTimeMs).isEqualTo(PLAYED_TIME_MS)
            assertThat(store.saveCallCount).isEqualTo(1)
        }

    @Test
    fun aSuccessfulSaveAdvancesTheRemotePayload() =
        runTest {
            val store = FakeCloudSaveStore(remote = REMOTE)

            assertThat(store.save(LOCAL, DESCRIPTION, PLAYED_TIME_MS, CONCATENATING_MERGE)).isTrue()
            assertThat(store.remote).isEqualTo(REMOTE + LOCAL)
        }

    @Test
    fun aFailedSaveLeavesTheRemotePayloadWhereItWas() =
        runTest {
            val store = FakeCloudSaveStore(remote = REMOTE, saveResult = false)

            assertThat(store.save(LOCAL, DESCRIPTION, PLAYED_TIME_MS, CONCATENATING_MERGE)).isFalse()
            assertThat(store.remote).isEqualTo(REMOTE)
        }

    @Test
    fun throwOnSaveThrowsOutOfSaveSoACallerCanProveItSurvivesAThrowingStore() =
        runTest {
            val store = FakeCloudSaveStore(throwOnSave = IllegalStateException(FAILURE_MESSAGE))

            assertThrows(IllegalStateException::class.java) {
                runTest { store.save(LOCAL, DESCRIPTION, PLAYED_TIME_MS, CONCATENATING_MERGE) }
            }
        }

    @Test
    fun throwOnLoadThrowsOutOfLoad() =
        runTest {
            val store = FakeCloudSaveStore(throwOnLoad = IllegalStateException(FAILURE_MESSAGE))

            assertThrows(IllegalStateException::class.java) {
                runTest { store.load(CONCATENATING_MERGE) }
            }
        }

    private companion object {
        val REMOTE = "remote".toByteArray()
        val LOCAL = "local".toByteArray()
        val DESCRIPTION = CloudSaveDescription(ladder = 7, totalStars = 15, classicCleared = 2)
        const val PLAYED_TIME_MS = 4_200L
        const val FAILURE_MESSAGE = "cloud unavailable"

        /**
         * A merge that is trivially order-sensitive, so an assertion can tell "the remote payload was
         * merged with the local one" apart from "one of the two was picked".
         */
        val CONCATENATING_MERGE: (ByteArray, ByteArray) -> ByteArray = { remote, local -> remote + local }
    }
}
