package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
 * Leaving the screen is the resume-snapshot write point (PERS-03, D-04): a
 * [DisposableEffect]-registered [LifecycleEventObserver] calls
 * [GameViewModel.onLeaveScreen] on [Lifecycle.Event.ON_STOP] — the one event that
 * covers backgrounding, screen-off and navigating away, and the last moment before the
 * process may be killed. It lives here rather than in `onCleared()`, which does not map
 * to `onStop` and is not guaranteed on process death. The ViewModel decides whether
 * anything is actually worth saving (a finished game never is, Pitfall 4).
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
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) viewModel.onLeaveScreen()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.finished.collect { result ->
            onFinished(
                Results(
                    levelId = viewModel.levelId,
                    stars = result.stars,
                    pegsRemaining = result.pegsRemaining,
                    elapsedSeconds = result.elapsedSeconds,
                    score = result.score,
                    challengeLevel = viewModel.challengeLevel,
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
        onToggleHints = viewModel::onToggleHints,
        onUndo = viewModel::onUndo,
        onHint = viewModel::onHintRequested,
        onHome = onNavigateBack,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
        challengeLevel = viewModel.challengeLevel,
    )
}
