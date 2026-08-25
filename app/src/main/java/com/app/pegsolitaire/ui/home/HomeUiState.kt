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
 */
@Immutable
data class HomeUiState(
    val onboardingVisible: Boolean = false,
    val challengeCurrentLevel: Int = ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL,
)
