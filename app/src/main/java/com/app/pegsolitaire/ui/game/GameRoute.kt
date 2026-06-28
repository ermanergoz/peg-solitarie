package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The stateful route (top of the Route/Screen/Leaf boundary).
 *
 * Obtains the [GameViewModel] from the Hilt graph via [hiltViewModel], collects its
 * read-only `uiState` in a lifecycle-aware way ([collectAsStateWithLifecycle]), and
 * hands the immutable state plus the VM's method references down to the stateless
 * [GameScreen]. It contains no logic itself — purely the state-collection + wiring
 * seam between the ViewModel and the stateless UI.
 *
 * @param modifier layout modifier forwarded to [GameScreen].
 */
@Composable
fun GameRoute(modifier: Modifier = Modifier) {
    val viewModel: GameViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GameScreen(
        state = state,
        onCellTap = viewModel::onCellTap,
        onRestart = viewModel::onRestart,
        onConfirmRestart = viewModel::onConfirmRestart,
        onDismissRestart = viewModel::onDismissRestart,
        onAnimationFinished = viewModel::onAnimationFinished,
        modifier = modifier,
    )
}
