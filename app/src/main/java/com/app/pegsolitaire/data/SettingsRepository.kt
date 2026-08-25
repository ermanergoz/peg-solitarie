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
 * Three flags so far: [hintsEnabled] (legal-destination dots drawn on the board),
 * [soundOn] (SET-01 — persisted flag only for now; no audio engine yet, D-16) and
 * [hapticsOn] (SET-02 — gates the light haptic fired on a jump, D-15).
 *
 * Every read **fails open** to `true` (D-17): a missing key (fresh install) or a
 * corrupt/IO read both degrade to "helper on", so a damaged store can never silently
 * strip a beginner's help — it only re-enables the harmless assists.
 */
interface SettingsRepository {
    /** Whether legal-destination hint dots are shown; fail-open default `true`. */
    val hintsEnabled: Flow<Boolean>

    /** Whether sound effects are enabled; fail-open default `true` (SET-01). */
    val soundOn: Flow<Boolean>

    /** Whether haptic feedback is enabled; fail-open default `true` (SET-02). */
    val hapticsOn: Flow<Boolean>

    /** Persists the player's hint on/off choice. */
    suspend fun setHintsEnabled(enabled: Boolean)

    /** Persists the player's sound on/off choice. */
    suspend fun setSoundOn(on: Boolean)

    /** Persists the player's haptics on/off choice. */
    suspend fun setHapticsOn(on: Boolean)
}

/** Scoped constants for the settings Preferences keys (QUAL-03 — no inline magic strings). */
internal object SettingsPreferences {
    /** The boolean key tracking whether the player wants hint dots shown. */
    val HINTS_KEY = booleanPreferencesKey("hints_enabled")

    /** Fail-open default: a missing/corrupt read is treated as "hints on" (show help). */
    const val DEFAULT_HINTS = true

    /** The boolean key tracking whether the player wants sound effects (SET-01). */
    val SOUND_KEY = booleanPreferencesKey("sound_on")

    /** Fail-open default: a missing/corrupt read is treated as "sound on" (D-17). */
    const val DEFAULT_SOUND = true

    /** The boolean key tracking whether the player wants haptic feedback (SET-02). */
    val HAPTICS_KEY = booleanPreferencesKey("haptics_on")

    /** Fail-open default: a missing/corrupt read is treated as "haptics on" (D-17). */
    const val DEFAULT_HAPTICS = true
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

        override val soundOn: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[SettingsPreferences.SOUND_KEY] ?: SettingsPreferences.DEFAULT_SOUND }

        override val hapticsOn: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[SettingsPreferences.HAPTICS_KEY] ?: SettingsPreferences.DEFAULT_HAPTICS }

        override suspend fun setHintsEnabled(enabled: Boolean) {
            dataStore.edit { it[SettingsPreferences.HINTS_KEY] = enabled }
        }

        override suspend fun setSoundOn(on: Boolean) {
            dataStore.edit { it[SettingsPreferences.SOUND_KEY] = on }
        }

        override suspend fun setHapticsOn(on: Boolean) {
            dataStore.edit { it[SettingsPreferences.HAPTICS_KEY] = on }
        }
    }
