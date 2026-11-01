package com.app.pegsolitaire.ui.settings

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.platform.games.GamesSignInState

/**
 * The single immutable UI state for the Settings screen (SET-01, SET-02, D-14).
 *
 * Owned by [SettingsViewModel] and exposed over a read-only `StateFlow`; the screen is a
 * stateless render of this object (state hoisting — the same UDF convention as
 * `HomeUiState`/`GameUiState`).
 *
 * The two persisted *assist* flags default `true`: that is the fail-open default the
 * persisted store itself degrades to (D-17), so the pre-first-emission state the screen paints
 * already matches what a fresh install (or a corrupt read) will report — the switches never
 * flicker off. The colour-blind palette flag defaults `false` for the same reason read the
 * other way: its store default, and its failed-read value, is `false` (docs/ux/colour-vision.md
 * §3.2), so the pre-first-emission value equals the persisted default and that switch never
 * flickers either.
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
 * @property soundOn whether sound effects are enabled (SET-01). The switch is live, and its
 *   gate is the sound player's own (SOUND-05): the player reads the choice straight from the
 *   repository, so switching it off also silences cues already playing.
 * @property hapticsOn whether haptic feedback is enabled (SET-02). It gates the jump's light
 *   tick (D-15) and the wrong tap's reject buzz (D-06b).
 * @property colorblindPaletteOn whether the colour-blind palette is on (CV-03, CV-04): the board
 *   marks, the stars and the tutorial's done pip draw in it. Device-local; default `false`.
 * @property privacyOptionsRequired whether UMP reports that a privacy-options form must be
 *   offered to this player (CONS-03, D-14). Drives the visibility of the "Manage privacy
 *   choices" row alone; the "Privacy policy" row above it is unconditional (REL-05).
 * @property gamesSignInState the player's observed Play Games session (PGS-02/PGS-03,
 *   D-11), collected from the `GamesService` seam. It defaults
 *   [GamesSignInState.Unknown] for exactly the reason [privacyOptionsRequired] defaults
 *   `false`, applied to a third kind of affordance: a wrong guess must still leave the
 *   player able to keep playing, and here that means **absent**. D-11 forbids the Play
 *   Games section — header, row and placeholder alike — while the state is `Unknown`, so
 *   the pre-first-emission default *is* the hidden state and no other value could be
 *   correct: defaulting `SignedOut` would flash a sign-in row at an already-signed-in
 *   player, and defaulting `SignedIn` is not even expressible without a player id. The
 *   section costs nothing by appearing a frame late; it costs a lie by appearing wrong.
 *   A player who never opted in is never `Unknown` for long — with the SDK untouched the
 *   seam resolves straight to [GamesSignInState.SignedOut] (D-19).
 * @property playGamesSignInErrorTick a one-shot ordinal raised once per genuine SDK
 *   sign-in error on the Settings row's path (D-10) and consumed back to 0 by the screen
 *   through `SettingsViewModel.onPlayGamesSignInErrorShown` the moment its snackbar is
 *   raised — the same consumed-event idiom `HomeUiState`'s `playGamesSignInErrorTick`
 *   uses, so a rotation after a failed tap does not announce the failure twice (CR-01).
 *   An ordinal rather than a `Boolean` for the reason `GameUiState`'s `adMessageTick` is
 *   one: a repeated identical error must leave a distinguishable state trace. Deliberately
 *   NOT raised by a plain chooser dismissal: `signIn()` returning `false` is the player's
 *   own cancel and stays silent. Starts at 0, which the screen reads as "nothing to show".
 */
@Immutable
data class SettingsUiState(
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
    val colorblindPaletteOn: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val gamesSignInState: GamesSignInState = GamesSignInState.Unknown,
    val playGamesSignInErrorTick: Int = 0,
)
