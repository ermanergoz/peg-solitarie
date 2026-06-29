package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pegsolitaire.navigation.Results

/**
 * The stateful route (top of the Route/Screen/Leaf boundary).
 *
 * Obtains the [GameViewModel] from the Hilt graph via [hiltViewModel], collects its
 * read-only `uiState` in a lifecycle-aware way ([collectAsStateWithLifecycle]), and
 * hands the immutable state plus the VM's method references down to the stateless
 * [GameScreen].
 *
 * Game-over is a one-shot hand-off, not screen state: a single [LaunchedEffect]
 * collects the VM's `finished` flow and, per emitted `ScoreResult`, navigates exactly
 * once to [Results] (carrying the played `levelId` + computed stars/score). Because
 * the effect collects a `Channel`-backed flow keyed on `Unit`, a recomposition can
 * never re-fire the navigation or pollute the back stack (T-02-05-NAV, Pitfall 4).
 *
 * @param onFinished navigates to the Results destination for a finished game.
 * @param onNavigateBack leaves the gameplay screen (top-bar back nav).
 * @param modifier layout modifier forwarded to [GameScreen].
 */
@Composable
fun GameRoute(
    onFinished: (Results) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: GameViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.finished.collect { result ->
            onFinished(
                Results(
                    levelId = viewModel.levelId,
                    stars = result.stars,
                    pegsRemaining = result.pegsRemaining,
                    elapsedSeconds = result.elapsedSeconds,
                    score = result.score,
                ),
            )
        }
    }

    GameScreen(
        state = state,
        onCellTap = viewModel::onCellTap,
        onRestart = viewModel::onRestart,
        onConfirmRestart = viewModel::onConfirmRestart,
        onDismissRestart = viewModel::onDismissRestart,
        onAnimationFinished = viewModel::onAnimationFinished,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}
