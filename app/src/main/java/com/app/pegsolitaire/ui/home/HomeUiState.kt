package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.data.ChallengeProgressPreferences
import com.app.pegsolitaire.domain.model.Board

/**
 * The single immutable UI state for the Home mode-select screen (NAV-01, ONB-01).
 *
 * Owned by [HomeViewModel] and exposed over a read-only `StateFlow`; the screen is a
 * stateless render of this object (state hoisting — the same UDF convention as
 * `GameUiState`).
 *
 * @property tutorialPending whether the interactive tutorial is still owed to this player —
 *   the mirror of `!onboardingSeen` (D-01f/D-02b), `true` on a first launch (or a fail-open
 *   corrupt read) and `false` from the moment the tutorial persists the flag. It is a
 *   **standing** flag rather than a visibility one: since D-02 the tutorial is a nav
 *   destination, not an overlay Home hosts, so Home reads this to auto-start the destination
 *   once and to withhold the two questions sequenced behind it. It has exactly one writer —
 *   the persisted flag. The deleted How-to-play affordance was a second one, and Settings ›
 *   How to play deliberately does not replace it: that row navigates, so a replay never
 *   re-raises the wipe question (§7 D-01f).
 * @property challengeCurrentLevel the player's persisted Challenge level (CHAL-05 /
 *   PERS-02), collected from `ChallengeProgress.currentLevel`. It labels the Challenge
 *   tile ("Continue — Level N") and is the level the tile enters — never a hardcoded 1.
 *   Defaults to the same fail-open level the store degrades to, so the tile paints a
 *   usable value before the first disk read lands (T-05-02).
 * @property classicResumeLevelId the Classic board with a saved in-progress game
 *   (PERS-03, D-01/D-03), or `null` when there is nothing to continue — which is also
 *   what an unreadable snapshot degrades to, so a corrupt blob simply hides the
 *   affordance instead of surfacing an error (T-05-03). Doubles as the board the
 *   Continue affordance resumes into: the screen carries the id, the host never guesses.
 * @property classicResumeBoard the footprint of that same saved Classic board, for the
 *   Continue chip's thumbnail (§4.2 band 3) — `null` under exactly the conditions
 *   [classicResumeLevelId] is, and never set without it. It is resolved by the ViewModel from
 *   the **static catalogue read it already performs** to validate the stored id (CR-01), so
 *   the thumbnail costs no new read on the start destination (D-03g) and the screen needs no
 *   second id-to-board mapping of its own — `:app`'s level source stays the one place a level
 *   id becomes a board (QUAL-01).
 * @property challengeResumeLevel the level of the saved in-progress Challenge game
 *   (PERS-03), read from the snapshot's own provenance stamp — or `null` when there is
 *   nothing usable to continue. Independent of [classicResumeLevelId] — a Classic and a
 *   Challenge game coexist, so Home can show up to two Continue affordances (D-03).
 *   Deliberately NOT [challengeCurrentLevel] (WR-03): at a cold start the ladder flow
 *   can still be at its fail-open initial value while the stamp always names the level
 *   the snapshot actually holds, so the tile labels — and resumes into — the real one.
 * @property playGamesPromptVisible whether the one-time Play Games opt-in prompt is
 *   showing (PGS-01, D-15). A peer of [tutorialPending] rather than a mode of it: the
 *   two first-run surfaces are sequenced, never stacked — this is `true` only once
 *   onboarding has been seen AND the question is still unanswered, and it is `false`
 *   forever after either answer (D-17). Nothing sets it back to `true`; there is no
 *   per-session re-ask.
 * @property profileSwapVisible whether the account-switch dialog is showing (CLOUD-02,
 *   D-12/D-15) — the one dialog this milestone reserved, and the only route to the only
 *   destructive path in the app. A **third peer** of [tutorialPending] and
 *   [playGamesPromptVisible] rather than a mode of either: the three surfaces are
 *   sequenced, never stacked, so this is `true` only once onboarding has been seen AND
 *   the opt-in question has been answered AND the data layer has a pending swap. Unlike
 *   [playGamesPromptVisible] it is **not** one-time — a player who signs into a third
 *   account is asked again, because each switch is its own unanswered question — but it
 *   is never re-raised for an answer already given.
 * @property profileSwapAccountName the arriving account's Play Games display name, or
 *   `null` when the SDK's lookup failed or stalled (Phase 11 D-14) — a degraded name,
 *   never a failed sign-in, which the dialog falls back from with neutral copy. A
 *   separate field rather than a nullable [profileSwapVisible] precisely because that
 *   `null` is legitimate: one nullable field could not distinguish "visible, name
 *   unknown" from "not visible", and collapsing them would hide the wipe question from
 *   exactly the player whose name could not be read. Follows
 *   [classicResumeLevelId]'s convention — the screen carries the value, the host never
 *   guesses.
 * @property playGamesSignInErrorTick a one-shot ordinal raised once per genuine SDK
 *   sign-in error on the accept path (D-18) and consumed back to 0 by the screen through
 *   `HomeViewModel.onPlayGamesSignInErrorShown` the moment its snackbar is raised. It is
 *   consumed rather than left standing because this ViewModel outlives every visit to
 *   Home: `HomeScreen` re-enters composition on every return from a forward destination
 *   and on every configuration change, and an unconsumed ordinal would re-announce one
 *   failure on each of them (CR-01). An ordinal rather than a `Boolean` for the same
 *   reason `GameUiState`'s `adMessageTick` is one: two consecutive failures must leave a
 *   distinguishable state trace. Deliberately NOT raised by a plain chooser dismissal:
 *   `signIn()` returning `false` is a player's own cancel and stays silent (D-10 parity).
 *   Starts at 0, which the screen reads as "nothing to show".
 * @property totalStars stars earned across both modes, for the progress ribbon's Stars pill
 *   (§7 R-1) — or `null` when the value is **not yet known**, which is both "the database
 *   has not answered yet" and "the read failed" (D-03c/D-03h). The pill paints
 *   `stat_value_unknown` ("—") for `null`, never `0` and never a blank: zero is a real
 *   answer for a brand-new player, so the two must stay distinguishable. `null` is the
 *   default because Home paints before any read lands — the ribbon is placeholder-first and
 *   the first frame never waits on disk (D-03g).
 * @property classicBoardsCleared how many Classic boards have been cleared, the numerator
 *   of the Boards pill (§7 R-2) — `null` under exactly the same two conditions as
 *   [totalStars], and from the same single aggregate row, so the two pills cannot disagree.
 *   The pill's **denominator** is the Classic catalogue size, a compile-time fact, and is
 *   deliberately absent from this state: a stored row must never be able to change it.
 */
@Immutable
data class HomeUiState(
    val tutorialPending: Boolean = false,
    val totalStars: Int? = null,
    val classicBoardsCleared: Int? = null,
    val challengeCurrentLevel: Int = ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL,
    val classicResumeLevelId: String? = null,
    val classicResumeBoard: Board? = null,
    val challengeResumeLevel: Int? = null,
    val playGamesPromptVisible: Boolean = false,
    val profileSwapVisible: Boolean = false,
    val profileSwapAccountName: String? = null,
    val playGamesSignInErrorTick: Int = 0,
) {
    /** Whether a Classic game can be continued — i.e. a resumable board id is known. */
    val classicResumeAvailable: Boolean get() = classicResumeLevelId != null

    /** Whether a Challenge game can be continued — i.e. a stamped resume level is known. */
    val challengeResumeAvailable: Boolean get() = challengeResumeLevel != null
}
