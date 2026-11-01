package com.app.pegsolitaire.ui.classic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The stateful Classic catalog route (top of the Route → Screen → Leaf boundary).
 *
 * Obtains the [ClassicCatalogViewModel] from the Hilt graph via [hiltViewModel],
 * collects its read-only `uiState` in a lifecycle-aware way
 * ([collectAsStateWithLifecycle]), and hands the immutable state plus the nav
 * lambdas down to the stateless [ClassicCatalogScreen]. The `onSelect`/`onNavigateBack`
 * callbacks are forwarded from `AppNavHost` (02-08): selecting a card navigates to
 * `Gameplay(levelId)`, back returns to Home.
 *
 * @param onSelect navigates to the gameplay screen for the selected `levelId`.
 * @param onNavigateBack leaves the catalog (→ Home).
 * @param modifier layout modifier forwarded to [ClassicCatalogScreen].
 */
@Composable
fun ClassicCatalogRoute(
    onSelect: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ClassicCatalogViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ClassicCatalogScreen(
        state = state,
        onSelect = onSelect,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}
