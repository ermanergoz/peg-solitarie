package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Immutable

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
 */
@Immutable
data class HomeUiState(
    val onboardingVisible: Boolean = false,
)
