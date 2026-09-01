package com.app.pegsolitaire.ui.settings

import androidx.compose.runtime.Immutable

/**
 * The single immutable UI state for the Settings screen (SET-01, SET-02, D-14).
 *
 * Owned by [SettingsViewModel] and exposed over a read-only `StateFlow`; the screen is a
 * stateless render of this object (state hoisting — the same UDF convention as
 * `HomeUiState`/`GameUiState`).
 *
 * Both *persisted* flags default `true`: that is the fail-open default the persisted store
 * itself degrades to (D-17), so the pre-first-emission state the screen paints already
 * matches what a fresh install (or a corrupt read) will report — the switches never
 * flicker off.
 *
 * [privacyOptionsRequired] deliberately defaults the **other** way, and the file would
 * contradict itself without saying why. The two defaults are not in tension: both pick the
 * direction in which a wrong guess still leaves the player able to keep playing. For a
 * toggle that means ON — a sound switch stuck off is a feature silently withheld. For the
 * consent entry point it means hidden: a row that is briefly absent costs a player who does
 * not need one exactly nothing, whereas a row shown on a false guess opens a UMP form that
 * does not exist, which is a dead end on a compliance surface. So this field is
 * fail-*closed*, and its only authority is the consent framework itself (T-09-30 — no
 * geo-IP check, no region string, no cached inference).
 *
 * @property soundOn whether sound effects are enabled (SET-01). Persisted only this
 *   phase — there is no audio engine yet (D-16), so the switch saves the choice for the
 *   later audio pass rather than muting anything today.
 * @property hapticsOn whether haptic feedback is enabled (SET-02). Wired for real:
 *   `GameScreen` fires a light tick on each jump only while this is on (D-15).
 * @property privacyOptionsRequired whether UMP reports that a privacy-options form must be
 *   offered to this player (CONS-03, D-14). Drives the visibility of the "Manage privacy
 *   choices" row alone; the "Privacy policy" row above it is unconditional (REL-05).
 */
@Immutable
data class SettingsUiState(
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
    val privacyOptionsRequired: Boolean = false,
)
