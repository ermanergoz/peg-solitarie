package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.app.pegsolitaire.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Scoped constants for the Challenge-progress Preferences keys (QUAL-03 — no inline literals). */
internal object ChallengeProgressPreferences {
    /** The int key holding the player's current Challenge level (1-based). */
    val CURRENT_LEVEL_KEY = intPreferencesKey("challenge_current_level")

    /** Fail-open default: a fresh install — or a corrupt/IO read — starts at level 1. */
    const val DEFAULT_CURRENT_LEVEL = 1
}

/**
 * The single source of truth for the player's current Challenge level (PERS-02 / CHAL-05).
 *
 * Backed by the **shared** `app_prefs` DataStore (never a second `preferencesDataStore`
 * delegate — that crashes at runtime; see `AppPreferences`), so the level survives process
 * death: closing and reopening the app resumes the same Challenge level.
 *
 * The public surface is deliberately unchanged from the Phase-3 in-memory seam —
 * [currentLevel] is still a hot [StateFlow] with a synchronously readable `.value` (via
 * `stateIn` over the DataStore flow), and [advancePast]/[reset] are still plain
 * (non-`suspend`) calls that write through on the injected app-lifetime [CoroutineScope].
 * Reads **fail open** to [ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL], mirroring
 * [SettingsRepositoryImpl]: a damaged store drops the player back to level 1 rather than
 * crashing the Challenge entry point.
 *
 * Phase 13 added the **awaitable** half of that surface — [mergeLadder], [settledLevel]
 * and [resetAwaiting] — for the cloud sync pass, which must know a write landed before it
 * reads the value back. They are not a second ladder: [advancePast] and [reset] are now
 * thin `scope.launch` wrappers over them, so the `max` merge is defined exactly once.
 */
@Singleton
class ChallengeProgress
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        /**
         * The current Challenge level (1-based).
         *
         * `stateIn(..., Eagerly, DEFAULT)` keeps the pre-existing synchronous `.value`
         * contract intact while the actual value is sourced from disk: the initial value is
         * the fail-open default and the persisted level lands as soon as the first
         * DataStore read completes.
         */
        val currentLevel: StateFlow<Int> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map {
                    it[ChallengeProgressPreferences.CURRENT_LEVEL_KEY]
                        ?: ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL
                }.stateIn(
                    scope = scope,
                    started = SharingStarted.Eagerly,
                    initialValue = ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL,
                )

        /**
         * Merges [level] into the stored ladder — `max(current, level)` — and returns only
         * once the write has **committed**. The one place the ladder is ever raised.
         *
         * The merge is computed *inside* the `edit` transaction (not from the cached
         * [currentLevel] value) so concurrent writes can never interleave into a lost
         * update. Because it is `max`, it is idempotent and monotonic: applying the same
         * level twice changes nothing, and no caller — local solve or cloud import — can
         * walk the ladder backwards.
         *
         * ## Why an awaitable variant exists
         *
         * `ProgressSubmitter` pulls the cloud save and then pushes in the **same pass**.
         * The push reads the ladder to put in the payload; if that read came from the
         * [currentLevel] StateFlow it could still hold the pre-merge value, because the
         * flow re-emits asynchronously after DataStore commits. The device would then push
         * back a ladder *lower* than the one it had just imported. That is harmless in the
         * end — the next pull maxes it back up — but it makes "one pass converges" untrue,
         * and untestable, which is exactly the property this phase is selling. Returning
         * only after the commit removes the window; [settledLevel] is the matching read.
         *
         * ## The ladder and the bests are not atomic with each other
         *
         * This value lives in DataStore while the per-level bests live in Room, so a cloud
         * import writes two stores and **cannot** do so in one transaction. That is
         * accepted, not overlooked: both writes are idempotent and monotonic, so a crash
         * between them leaves work for the next pass rather than an inconsistent state. No
         * ordering can produce a regression; the worst case is a ladder that catches up a
         * pass later.
         */
        suspend fun mergeLadder(level: Int) {
            // Best-effort (T-05-13). Unlike advancePast this runs on the CALLER's
            // coroutine — the sync pass — so a raw IO failure would propagate into it.
            storageFailSafe(fallback = Unit) {
                dataStore.edit { preferences ->
                    val current =
                        preferences[ChallengeProgressPreferences.CURRENT_LEVEL_KEY]
                            ?: ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL
                    preferences[ChallengeProgressPreferences.CURRENT_LEVEL_KEY] = maxOf(current, level)
                }
            }
        }

        /**
         * The ladder value currently **committed to disk**, read straight from the store
         * rather than from [currentLevel]'s cache — so immediately after [mergeLadder]
         * returns, this already reflects the merge even if the StateFlow has not re-emitted.
         *
         * Fails open to [ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL], the same
         * degradation [currentLevel] applies to a damaged store.
         */
        suspend fun settledLevel(): Int =
            storageFailSafe(fallback = ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL) {
                dataStore.data.first()[ChallengeProgressPreferences.CURRENT_LEVEL_KEY]
                    ?: ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL
            }

        /**
         * Resets the ladder to the first level and returns once the write has committed —
         * the awaitable [reset], for D-14's profile-replace path.
         *
         * It is the one operation on this class that is deliberately **not** monotonic,
         * which is why it has exactly one non-test caller: the player explicitly chose to
         * hand this device to another account. It runs beside
         * `ProgressRepository.resetAllProgress()`, which wipes the Room half; the ladder
         * needs its own call because it lives in DataStore (see [mergeLadder]).
         *
         * Best-effort (T-05-13): a failed write keeps the current ladder.
         */
        suspend fun resetAwaiting() {
            storageFailSafe(fallback = Unit) {
                dataStore.edit {
                    it[ChallengeProgressPreferences.CURRENT_LEVEL_KEY] =
                        ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL
                }
            }
        }

        /**
         * Advances the ladder past [solvedLevel] — called when the player solves that level.
         *
         * The `+ 1` lives here, at the call boundary, and the merge itself is [mergeLadder]:
         * keying on the level actually solved rather than a blind `current + 1` is what
         * makes re-solving the same board (back from Results → undo → re-jump)
         * **idempotent** so it can never skip a level (Phase-3 D-02), and what stops an
         * older board restored by Continue from moving the ladder backwards.
         *
         * Fire-and-forget by design — the Results screen must not wait on a disk write. The
         * cloud pull wants the opposite and calls [mergeLadder] directly.
         */
        fun advancePast(solvedLevel: Int) {
            // The launch runs on the app-lifetime scope, whose SupervisorJob isolates a
            // failed child but does NOT swallow the exception; mergeLadder's own
            // storageFailSafe is what keeps an IO failure from crashing the app (T-05-13).
            scope.launch { mergeLadder(solvedLevel + 1) }
        }

        /**
         * Resets progress back to the first level (a fresh Challenge run), fire-and-forget
         * like [advancePast]. [resetAwaiting] is the same write for callers that must know
         * it landed.
         */
        fun reset() {
            scope.launch { resetAwaiting() }
        }
    }
