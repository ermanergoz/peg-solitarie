package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The stateful Home route (top of the Route → Screen → Leaf boundary).
 *
 * Obtains the [HomeViewModel] from the Hilt graph via [hiltViewModel], collects its
 * read-only `uiState` in a lifecycle-aware way ([collectAsStateWithLifecycle]), and
 * hands the immutable state plus the nav/handler lambdas down to the stateless
 * [HomeScreen]. The `onPlayClassic` callback is forwarded from `AppNavHost` (→
 * `ClassicCatalog`); the onboarding show/dismiss handlers come from the ViewModel.
 *
 * @param onPlayClassic navigates to the Classic catalog (supplied by `AppNavHost`).
 * @param modifier layout modifier forwarded to [HomeScreen].
 */
@Composable
fun HomeRoute(
    onPlayClassic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onPlayClassic = onPlayClassic,
        onShowHowToPlay = viewModel::onShowHowToPlay,
        onDismissOnboarding = viewModel::onDismissOnboarding,
        modifier = modifier,
    )
}
