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
         * Advances the ladder past [solvedLevel] — called when the player solves that level.
         *
         * The write is `max(current, solvedLevel + 1)`, keyed on the level actually solved
         * rather than a blind `current + 1`: re-solving the same board (back from Results →
         * undo → re-jump) is **idempotent** and can never skip a level (Phase-3 D-02), and
         * solving an older board a Continue restored can never move the ladder backwards.
         * The merge is computed *inside* the `edit` transaction (not from the cached
         * [currentLevel] value) so concurrent writes can never interleave into a lost update.
         */
        fun advancePast(solvedLevel: Int) {
            scope.launch {
                dataStore.edit { preferences ->
                    val current =
                        preferences[ChallengeProgressPreferences.CURRENT_LEVEL_KEY]
                            ?: ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL
                    preferences[ChallengeProgressPreferences.CURRENT_LEVEL_KEY] =
                        maxOf(current, solvedLevel + 1)
                }
            }
        }

        /** Resets progress back to the first level (a fresh Challenge run). */
        fun reset() {
            scope.launch {
                dataStore.edit {
                    it[ChallengeProgressPreferences.CURRENT_LEVEL_KEY] =
                        ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL
                }
            }
        }
    }
