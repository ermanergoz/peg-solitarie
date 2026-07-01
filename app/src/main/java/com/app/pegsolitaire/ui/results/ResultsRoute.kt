package com.app.pegsolitaire.ui.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The stateful route for the Results destination (top of the Route/Screen/Leaf
 * boundary).
 *
 * Obtains the [ResultsViewModel] from the Hilt graph via [hiltViewModel] (it reads
 * the typed `Results` args from the back-stack entry's `SavedStateHandle`), collects
 * its read-only `uiState` in a lifecycle-aware way ([collectAsStateWithLifecycle]),
 * and hands the immutable state plus the hoisted navigation lambdas down to the
 * stateless [ResultsScreen]. It owns no logic — the headline tier is computed in the
 * ViewModel; the actual navigation targets are supplied by the `AppNavHost` (02-08).
 *
 * @param onReplay re-plays the same level (the VM's `replayId`).
 * @param onCatalog pops back to the Classic catalog.
 * @param onHome pops back to Home.
 * @param onNavigateBack leaves the Results screen (top-bar back nav).
 * @param modifier layout modifier forwarded to [ResultsScreen].
 */
@Composable
fun ResultsRoute(
    onReplay: (String) -> Unit,
    onCatalog: () -> Unit,
    onHome: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ResultsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ResultsScreen(
        state = state,
        onReplay = onReplay,
        onCatalog = onCatalog,
        onHome = onHome,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}
