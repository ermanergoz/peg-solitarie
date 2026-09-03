package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.data.ChallengeProgressPreferences

/**
 * The single immutable UI state for the Home mode-select screen (NAV-01, ONB-01).
 *
 * Owned by [HomeViewModel] and exposed over a read-only `StateFlow`; the screen is a
 * stateless render of this object (state hoisting — the same UDF convention as
 * `GameUiState`).
 *
 * @property onboardingVisible whether the first-run how-to-play overlay is showing —
 *   `true` on first launch while `onboardingSeen == false` (D-14) and whenever the
 *   permanent How-to-play affordance forces it open (D-15); `false` once seen/dismissed.
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
 * @property challengeResumeLevel the level of the saved in-progress Challenge game
 *   (PERS-03), read from the snapshot's own provenance stamp — or `null` when there is
 *   nothing usable to continue. Independent of [classicResumeLevelId] — a Classic and a
 *   Challenge game coexist, so Home can show up to two Continue affordances (D-03).
 *   Deliberately NOT [challengeCurrentLevel] (WR-03): at a cold start the ladder flow
 *   can still be at its fail-open initial value while the stamp always names the level
 *   the snapshot actually holds, so the tile labels — and resumes into — the real one.
 * @property playGamesPromptVisible whether the one-time Play Games opt-in prompt is
 *   showing (PGS-01, D-15). A peer of [onboardingVisible] rather than a mode of it: the
 *   two first-run surfaces are sequenced, never stacked — this is `true` only once
 *   onboarding has been seen AND the question is still unanswered, and it is `false`
 *   forever after either answer (D-17). Nothing sets it back to `true`; there is no
 *   per-session re-ask.
 * @property playGamesSignInErrorTick a monotonic ordinal incremented once per genuine
 *   SDK sign-in error on the accept path (D-18), consumed by the screen's
 *   `LaunchedEffect(tick)` snackbar — the same one-shot-event idiom `GameUiState`'s
 *   `adMessageTick` uses, because a repeated identical error otherwise leaves no
 *   distinguishable state trace. Deliberately NOT raised by a plain chooser dismissal:
 *   `signIn()` returning `false` is a player's own cancel and stays silent (D-10 parity).
 *   Starts at 0, which the screen's `> 0` guard reads as "nothing to show".
 */
@Immutable
data class HomeUiState(
    val onboardingVisible: Boolean = false,
    val challengeCurrentLevel: Int = ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL,
    val classicResumeLevelId: String? = null,
    val challengeResumeLevel: Int? = null,
    val playGamesPromptVisible: Boolean = false,
    val playGamesSignInErrorTick: Int = 0,
) {
    /** Whether a Classic game can be continued — i.e. a resumable board id is known. */
    val classicResumeAvailable: Boolean get() = classicResumeLevelId != null

    /** Whether a Challenge game can be continued — i.e. a stamped resume level is known. */
    val challengeResumeAvailable: Boolean get() = challengeResumeLevel != null
}
