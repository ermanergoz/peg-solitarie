package com.app.pegsolitaire.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * An in-memory [DataStore] of [Preferences] for the JVM suite — the deterministic replacement for
 * `PreferenceDataStoreFactory.create(produceFile = { tempFolder.newFile(…) })`.
 *
 * ## Why this exists (F-10)
 *
 * A real `PreferenceDataStoreFactory.create` call with no `scope` argument gives DataStore its own
 * `CoroutineScope(Dispatchers.IO + SupervisorJob())`, so the store's actor does real file IO on a
 * dispatcher the test does not own, **outside** `runTest`'s virtual clock, while the test body waits
 * against `runTest`'s real 60-second timeout. One test in this suite duly hung at 60.041 s on a run
 * that normally takes 0.005 s — four orders of magnitude, which is an ordering-dependent block, not
 * load. `rules/testing.md` forbids the whole shape in one sentence: *"Deterministic tests only: no
 * wall-clock, no network, no unseeded random, no sleeps."*
 *
 * This double removes all three causes at once — no file, no foreign scope, no wall clock. Every
 * operation is a plain suspend call that completes on the caller's own dispatcher, so a `runTest`
 * body drives the store entirely through its own scheduler and **cannot** hang against real time.
 *
 * Passing the *test* scope to a real store was considered and rejected: it moves DataStore's
 * blocking file IO onto the single-threaded test dispatcher, which is as likely to change the
 * symptom as to remove it.
 *
 * ## The contract, and why each half is load-bearing
 *
 * Ten test classes assert through this double, so it reproduces the two `DataStore` semantics they
 * actually depend on. [InMemoryPreferencesDataStoreTest] pins both, and each test there was watched
 * to fail against a variant lacking the property.
 *
 * 1. **[updateData] is atomic.** Callers write read-modify-write transforms, and [transform] may
 *    suspend. Without serialisation two of them interleave, both read the same value, and one
 *    caller's write is silently lost — a store reporting a value nobody wrote. [writeLock] is held
 *    across the whole read-transform-write, exactly as the real implementation's single writer
 *    actor is.
 * 2. **[data] emits the current value on collection, then every subsequent value.** The suite's
 *    "killed and relaunched" simulation is a **second holder constructed over the same store
 *    object** — so a collector that subscribes late must see the writes that preceded it (that is
 *    `replay = 1`), and a collector already running must not miss an intermediate value that a
 *    caller's `data.first { … }` predicate is waiting for (that is the unbounded buffer).
 *
 * ## Two deliberate, documented divergences from a real store
 *
 * - **No conflation.** A real store's `data` is backed by a `StateFlow`, so a slow collector can
 *   miss intermediate values. This double drops nothing. That is strictly *weaker* as a
 *   requirement on the code under test — a predicate that would be satisfied against a real store
 *   is still satisfied here — so it cannot make a passing test fail, and it is what makes
 *   `data.first { … }` deterministic instead of a race.
 * - **An unchanged write still emits.** A real store skips the write when `transform` returns a
 *   value equal to the current one. Invisible downstream: every consumer reads through a
 *   `StateFlow`, which de-duplicates equal values, and no test in the suite asserts an emission
 *   count on `data`.
 *
 * It does **not** simulate failure. The classes that need a hostile store keep their own
 * throwing/cancelling anonymous `DataStore` objects, which is where that behaviour belongs — the
 * failure is the point of those tests, not an option on this one.
 */
class InMemoryPreferencesDataStore : DataStore<Preferences> {
    /**
     * Serialises [updateData] end to end.
     *
     * Held across the caller's suspending [transform], which is the whole point: the suspension
     * inside a read-modify-write is the only window in which two updates on one dispatcher can
     * interleave and lose one of them.
     */
    private val writeLock = Mutex()

    /**
     * The store's state and its change feed in one object.
     *
     * `replay = 1` is "the current value on collection" — a holder constructed after a write reads
     * that write back, which is how the suite simulates a process relaunch. The effectively
     * unbounded extra buffer is "then every subsequent value": a write never has to drop a value to
     * make room, and — because [MutableSharedFlow.tryEmit] therefore always succeeds — a write can
     * never suspend waiting for a slow collector either, which is what keeps [updateData] free of
     * any deadlock of its own.
     */
    private val updates =
        MutableSharedFlow<Preferences>(
            replay = REPLAY_LATEST,
            extraBufferCapacity = UNBOUNDED_EXTRA_BUFFER,
        )

    init {
        // A store with no file yet reads as empty; `tryEmit` into a free buffer cannot fail.
        check(updates.tryEmit(emptyPreferences())) { "the replay cache rejected the initial value" }
    }

    override val data: Flow<Preferences> = updates.asSharedFlow()

    /**
     * Applies [transform] to the current value and publishes the result, one caller at a time.
     *
     * @return the value stored, matching [DataStore.updateData]'s contract.
     */
    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        writeLock.withLock {
            val updated = transform(currentValue())
            check(updates.tryEmit(updated)) { "the update buffer rejected a value" }
            updated
        }

    /**
     * The value last published.
     *
     * Read from the replay cache rather than a second field, so there is exactly one place the
     * state lives and no way for a reader and a collector to disagree. Safe to assume non-empty:
     * `init` seeds it and nothing ever resets it.
     */
    private fun currentValue(): Preferences = updates.replayCache.first()

    private companion object {
        /** One replayed value = "the current value on collection", a real store's read semantics. */
        const val REPLAY_LATEST = 1

        /**
         * Effectively unbounded, so no update is ever dropped or made to wait.
         *
         * `replay + extraBufferCapacity` must not overflow `Int`, hence the subtraction. The buffer
         * array is grown lazily by the flow, so a large bound costs nothing until it is used.
         */
        const val UNBOUNDED_EXTRA_BUFFER = Int.MAX_VALUE - REPLAY_LATEST
    }
}
