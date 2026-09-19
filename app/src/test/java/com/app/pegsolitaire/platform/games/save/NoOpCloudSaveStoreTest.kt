package com.app.pegsolitaire.platform.games.save

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves [NoOpCloudSaveStore] degrades silently: no payload read, nothing written, and the caller's
 * merge never invoked.
 *
 * The assertions are `null` and `false` rather than "does not throw", and the distinction is the
 * point — exactly as in `NoOpGamesServiceTest`. `null` is what tells a caller to *stay local*, and
 * `false` is what keeps its cloud-push-pending marker set; a stub that reported success would have
 * the caller clear that marker for a payload that never left the device, losing the retry silently
 * (CLOUD-05).
 *
 * The merge-not-invoked assertions matter beyond tidiness. [CloudSaveStore]'s `mergeRemote` is a
 * live, pure function over real progress whose result is written back to the server on the conflict
 * path; a stub that ran it would be performing a merge against nothing, and any future
 * implementation that inherited that habit would write a half-empty payload over a real save.
 *
 * This test stays in the tree after plan 13-09 rebinds the seam, for the same reason
 * `NoOpGamesServiceTest` and `NoOpAdGatewayTest` did: it is where the seam's fail-quiet reference
 * behaviour remains written down somewhere executable.
 */
class NoOpCloudSaveStoreTest {
    private val store = NoOpCloudSaveStore()

    @Test
    fun loadReportsNoPayloadSoTheCallerStaysLocal() =
        runTest {
            assertThat(store.load { _, _ -> ByteArray(0) }).isNull()
        }

    @Test
    fun loadNeverInvokesTheCallersMerge() =
        runTest {
            var merged = false

            store.load { _, _ ->
                merged = true
                ByteArray(0)
            }

            assertThat(merged).isFalse()
        }

    @Test
    fun saveReportsNothingWasWritten() =
        runTest {
            val outcome = store.save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS) { _, _ -> ByteArray(0) }

            // Not committed is the contract; FAILED_OTHER with no status code is the honest
            // label for a stub that never reached any SDK stage at all.
            assertThat(outcome.committed).isFalse()
            assertThat(outcome).isEqualTo(CloudSaveOutcome(CloudSaveStage.FAILED_OTHER))
        }

    @Test
    fun saveNeverInvokesTheCallersMerge() =
        runTest {
            var merged = false

            store.save(PAYLOAD, DESCRIPTION, PLAYED_TIME_MS) { _, _ ->
                merged = true
                ByteArray(0)
            }

            assertThat(merged).isFalse()
        }

    private companion object {
        val PAYLOAD = "payload".toByteArray()
        val DESCRIPTION = CloudSaveDescription(ladder = 12, totalStars = 30, classicCleared = 3)
        const val PLAYED_TIME_MS = 1_200L
    }
}
