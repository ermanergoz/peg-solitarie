package com.app.pegsolitaire.testing

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

/**
 * Pins [InMemoryPreferencesDataStore]'s own contract, because ten test classes and roughly two
 * hundred tests now assert *through* it. An unverified double under that much of the suite is a
 * worse risk than the flake it replaces: it would make those suites green for the wrong reason,
 * silently, and the failure would surface as a shipped bug rather than as a red test.
 *
 * Two semantics are load-bearing — the tech-lead's F-10 ruling names both — and each has its own
 * tests here:
 *
 * - **`updateData` is atomic.** Callers write read-modify-write transforms (`edit { it[k] = it[k]
 *   + 1 }`). If two of those interleave, one increment is lost and the store reports a value that
 *   no caller ever wrote. [concurrentReadModifyWriteUpdatesAllLandBecauseUpdateDataSerialises]
 *   suspends inside the transform, which is the only way the interleaving can happen on a
 *   single-threaded test dispatcher, and is therefore the only test that can catch its absence.
 * - **`data` emits the current value on collection, then every subsequent value.** The "killed and
 *   relaunched" simulation the ten classes rely on is a second holder constructed over the same
 *   store object, so a late collector MUST see the writes that happened before it subscribed, and
 *   an early collector MUST NOT miss an intermediate value.
 *
 * [everyOperationCompletesWithoutAdvancingTheTestClock] pins the third property, the one this
 * whole task exists for: no operation involves a delay, a timeout or a dispatcher this test does
 * not own, so nothing here can hang against a wall clock (`rules/testing.md`: "Deterministic tests
 * only: no wall-clock, no network, no unseeded random, no sleeps").
 */
class InMemoryPreferencesDataStoreTest {
    // ---- updateData atomicity ---------------------------------------------------------

    @Test
    fun concurrentReadModifyWriteUpdatesAllLandBecauseUpdateDataSerialises() =
        runTest {
            val store = InMemoryPreferencesDataStore()

            // Every transform suspends between its read and its write. Unserialised, all
            // WRITERS read the same stale value and the last write wins, leaving 1.
            val writers =
                List(WRITERS) {
                    launch {
                        store.updateData { current ->
                            val seen = current[COUNTER_KEY] ?: NONE
                            yield()
                            current
                                .toMutablePreferences()
                                .apply { this[COUNTER_KEY] = seen + 1 }
                                .toPreferences()
                        }
                    }
                }
            writers.joinAll()

            assertThat(store.data.first()[COUNTER_KEY]).isEqualTo(WRITERS)
        }

    @Test
    fun updateDataHandsBackTheValueItStored() =
        runTest {
            val store = InMemoryPreferencesDataStore()

            val returned =
                store.updateData {
                    it.toMutablePreferences().apply { this[COUNTER_KEY] = FIRST }.toPreferences()
                }

            assertThat(returned[COUNTER_KEY]).isEqualTo(FIRST)
            assertThat(store.data.first()).isEqualTo(returned)
        }

    @Test
    fun aLaterTransformReadsTheValueTheEarlierOneWrote() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            store.edit { it[COUNTER_KEY] = FIRST }

            var seenByTheSecondTransform: Int? = null
            store.edit {
                seenByTheSecondTransform = it[COUNTER_KEY]
                it[COUNTER_KEY] = SECOND
            }

            assertThat(seenByTheSecondTransform).isEqualTo(FIRST)
            assertThat(store.data.first()[COUNTER_KEY]).isEqualTo(SECOND)
        }

    // ---- data emission semantics ------------------------------------------------------

    @Test
    fun aFreshStoreEmitsEmptyPreferencesJustLikeAStoreWithNoFileYet() =
        runTest {
            val store = InMemoryPreferencesDataStore()

            assertThat(store.data.first()).isEqualTo(emptyPreferences())
        }

    @Test
    fun aCollectorThatSubscribesAfterAWriteSeesTheLatestValueFirst() =
        runTest {
            // This is the "killed and relaunched" property: a second holder over the same store
            // must read back what the first holder wrote, not the value the store started at.
            val store = InMemoryPreferencesDataStore()
            store.edit { it[COUNTER_KEY] = FIRST }

            assertThat(store.data.first()[COUNTER_KEY]).isEqualTo(FIRST)
        }

    @Test
    fun everyCollectorOfDataObservesTheOneSharedState() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val firstHolder = store.data
            val secondHolder = store.data

            store.edit { it[COUNTER_KEY] = FIRST }

            assertThat(firstHolder.first()[COUNTER_KEY]).isEqualTo(FIRST)
            assertThat(secondHolder.first()[COUNTER_KEY]).isEqualTo(FIRST)
        }

    @Test
    fun anAlreadyCollectingCollectorSeesEverySubsequentValueInOrder() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val seen = mutableListOf<Int?>()
            val collector = launch { store.data.collect { seen += it[COUNTER_KEY] } }
            runCurrent()

            store.edit { it[COUNTER_KEY] = FIRST }
            store.edit { it[COUNTER_KEY] = SECOND }
            advanceUntilIdle()
            collector.cancel()

            // Conflating the two writes would leave [null, SECOND] — the intermediate value a
            // caller awaiting `first { it == FIRST }` is waiting for would never arrive.
            assertThat(seen).containsExactly(null, FIRST, SECOND).inOrder()
        }

    @Test
    fun noUpdateIsDroppedWhileTheCollectorIsSuspended() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            val seen = mutableListOf<Int?>()
            val collector = launch { store.data.collect { seen += it[COUNTER_KEY] } }
            runCurrent()

            // A burst with no suspension point between the writes: the collector cannot run
            // until the burst is over, so every value has to be buffered rather than dropped.
            repeat(BURST) { index -> store.edit { it[COUNTER_KEY] = index } }
            advanceUntilIdle()
            collector.cancel()

            assertThat(seen).hasSize(BURST + 1)
            assertThat(seen.last()).isEqualTo(BURST - 1)
        }

    @Test
    fun dataNeverCompletesSoAHolderKeepsObservingForTheWholeTest() =
        runTest {
            val store = InMemoryPreferencesDataStore()
            var completed = false
            val collector = launch { store.data.onCompletion { completed = true }.collect { } }
            runCurrent()

            store.edit { it[COUNTER_KEY] = FIRST }
            advanceUntilIdle()

            assertThat(completed).isFalse()
            collector.cancel()
        }

    // ---- determinism ------------------------------------------------------------------

    @Test
    fun everyOperationCompletesWithoutAdvancingTheTestClock() =
        runTest {
            // Zero virtual time means no `delay`, no timeout and no scheduled work anywhere in
            // the double — the property that makes it impossible for it to hang on a wall clock.
            val store = InMemoryPreferencesDataStore()

            store.edit { it[COUNTER_KEY] = FIRST }
            store.data.first { it[COUNTER_KEY] == FIRST }
            advanceUntilIdle()

            assertThat(currentTime).isEqualTo(NO_TIME_ELAPSED)
        }

    private companion object {
        val COUNTER_KEY = intPreferencesKey("counter")

        const val NONE = 0
        const val FIRST = 7
        const val SECOND = 9
        const val NO_TIME_ELAPSED = 0L

        /** Enough concurrent writers that a single lost increment shows up as a wrong total. */
        const val WRITERS = 50

        /** More unread values than any plausible fixed buffer, so the buffer must be unbounded. */
        const val BURST = 64
    }
}
