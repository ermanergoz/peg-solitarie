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
 * Four flags so far: [hintsEnabled] (legal-destination dots drawn on the board),
 * [soundOn] (SET-01 — its gate is the sound player's own, SOUND-05: the player collects this
 * flow, so switching it off also silences cues already playing), [hapticsOn] (SET-02 —
 * gates the jump's light tick, D-15, and the wrong tap's reject buzz, D-06b) and
 * [colorblindPaletteOn] (docs/ux/colour-vision.md CV-04 — the board marks, stars and done pip
 * in the colour-blind palette).
 *
 * Every **assist** read fails open to `true` (D-17): a missing key (fresh install) or a
 * corrupt/IO read both degrade to "helper on", so a damaged store can never silently
 * strip a beginner's help — it only re-enables the harmless assists. The palette flag fails
 * to its default, `false`, instead: see [colorblindPaletteOn].
 */
interface SettingsRepository : HapticsSetting {
    /** Whether legal-destination hint dots are shown; fail-open default `true`. */
    val hintsEnabled: Flow<Boolean>

    /** Whether sound effects are enabled; fail-open default `true` (SET-01). */
    val soundOn: Flow<Boolean>

    /** Whether haptic feedback is enabled; fail-open default `true` (SET-02). */
    override val hapticsOn: Flow<Boolean>

    /** Persists the player's hint on/off choice. */
    suspend fun setHintsEnabled(enabled: Boolean)

    /** Persists the player's sound on/off choice. */
    suspend fun setSoundOn(on: Boolean)

    /** Persists the player's haptics on/off choice. */
    suspend fun setHapticsOn(on: Boolean)

    /**
     * Whether the colour-blind palette is on (CV-04); default `false`, and a failed read gives
     * `false` too — the repository's only flag that does **not** fail open. D-17 protects
     * assists: a damaged store must never strip a beginner's help. This flag is a look, not an
     * assist. A corrupt store resets every setting at once, and showing the alternative look to
     * every player whose store failed would misstate the default the founder approved; a player
     * who had chosen the palette sees the change at once, one tap from the row. Device-local: it
     * is not part of the cloud progress payload, and nothing but the Settings switch writes it.
     */
    val colorblindPaletteOn: Flow<Boolean>

    /** Persists the player's colour-blind palette on/off choice. */
    suspend fun setColorblindPaletteOn(on: Boolean)
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

    /** The boolean key tracking whether the player wants the colour-blind palette (CV-04). */
    val COLORBLIND_PALETTE_KEY = booleanPreferencesKey("colorblind_palette_on")

    /** Off on a fresh install, and off on a failed read: a look, not an assist (CV-04, not D-17). */
    const val DEFAULT_COLORBLIND_PALETTE = false
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

        // The setters are best-effort (T-05-13): a disk-full/IO failure drops the write —
        // the toggle simply does not stick — instead of crashing the Settings screen.
        // The flows above stay the single source of truth, so the UI keeps showing the
        // value that actually persisted.

        override suspend fun setHintsEnabled(enabled: Boolean) {
            storageFailSafe(fallback = Unit) {
                dataStore.edit { it[SettingsPreferences.HINTS_KEY] = enabled }
            }
        }

        override suspend fun setSoundOn(on: Boolean) {
            storageFailSafe(fallback = Unit) {
                dataStore.edit { it[SettingsPreferences.SOUND_KEY] = on }
            }
        }

        override suspend fun setHapticsOn(on: Boolean) {
            storageFailSafe(fallback = Unit) {
                dataStore.edit { it[SettingsPreferences.HAPTICS_KEY] = on }
            }
        }

        override val colorblindPaletteOn: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map {
                    it[SettingsPreferences.COLORBLIND_PALETTE_KEY]
                        ?: SettingsPreferences.DEFAULT_COLORBLIND_PALETTE
                }

        override suspend fun setColorblindPaletteOn(on: Boolean) {
            storageFailSafe(fallback = Unit) {
                dataStore.edit { it[SettingsPreferences.COLORBLIND_PALETTE_KEY] = on }
            }
        }
    }
