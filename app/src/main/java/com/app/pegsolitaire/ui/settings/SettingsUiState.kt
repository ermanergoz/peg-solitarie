package com.app.pegsolitaire.ui.settings

import androidx.compose.runtime.Immutable

/**
 * The single immutable UI state for the Settings screen (SET-01, SET-02, D-14).
 *
 * Owned by [SettingsViewModel] and exposed over a read-only `StateFlow`; the screen is a
 * stateless render of this object (state hoisting — the same UDF convention as
 * `HomeUiState`/`GameUiState`).
 *
 * Both flags default `true`: that is the fail-open default the persisted store itself
 * degrades to (D-17), so the pre-first-emission state the screen paints already matches
 * what a fresh install (or a corrupt read) will report — the switches never flicker off.
 *
 * @property soundOn whether sound effects are enabled (SET-01). Persisted only this
 *   phase — there is no audio engine yet (D-16), so the switch saves the choice for the
 *   later audio pass rather than muting anything today.
 * @property hapticsOn whether haptic feedback is enabled (SET-02). Wired for real:
 *   `GameScreen` fires a light tick on each jump only while this is on (D-15).
 */
@Immutable
data class SettingsUiState(
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
)
