package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for player-tunable gameplay settings.
 *
 * The only setting so far is [hintsEnabled] — whether the legal-destination dots are
 * drawn on the board. The read **fails open** to `true`: a missing key (fresh install)
 * or a corrupt/IO read both degrade to "hints on", so a damaged store can never hide a
 * beginner's help, it only re-shows the harmless hint dots.
 */
interface SettingsRepository {
    /** Whether legal-destination hint dots are shown; fail-open default `true`. */
    val hintsEnabled: Flow<Boolean>

    /** Persists the player's hint on/off choice. */
    suspend fun setHintsEnabled(enabled: Boolean)
}

/** Scoped constants for the settings Preferences keys (QUAL-03 — no inline magic strings). */
internal object SettingsPreferences {
    /** The boolean key tracking whether the player wants hint dots shown. */
    val HINTS_KEY = booleanPreferencesKey("hints_enabled")

    /** Fail-open default: a missing/corrupt read is treated as "hints on" (show help). */
    const val DEFAULT_HINTS = true
}

/**
 * DataStore-Preferences-backed [SettingsRepository] over the shared `app_prefs` store.
 *
 * Mirrors [OnboardingRepository]: the read catches any error and degrades to empty
 * preferences (→ the fail-open default). The [DataStore] is Hilt-provided as a shared
 * `@Singleton` (see `DataModule`), so a single `@Inject` constructor over it suffices;
 * unit tests construct the impl directly with a real temp-file store or a throwing one.
 */
@Singleton
class SettingsRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override val hintsEnabled: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[SettingsPreferences.HINTS_KEY] ?: SettingsPreferences.DEFAULT_HINTS }

        override suspend fun setHintsEnabled(enabled: Boolean) {
            dataStore.edit { it[SettingsPreferences.HINTS_KEY] = enabled }
        }
    }
