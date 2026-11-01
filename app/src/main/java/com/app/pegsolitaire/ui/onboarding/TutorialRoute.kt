package com.app.pegsolitaire.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The stateful tutorial route (top of the Route → Screen → Leaf boundary).
 *
 * [hiltViewModel] is called **inside** `composable<Tutorial>`, so the ViewModel binds to the
 * `NavBackStackEntry` and is cleared on pop: rotation keeps the current step for free, and a
 * replay from Settings always restarts at step 1 (D-01g). Obtained above the `NavHost` it
 * would bind to the Activity instead, and a player who skipped at T2 would later reopen
 * Settings › How to play and land on T2 with the previous board still installed.
 *
 * Navigation happens on the ViewModel's **exit signal**, never on the tap: the signal is
 * emitted only after the seen flag is persisted, which is what stops Home re-pushing the
 * tutorial on the frame after the player left it.
 *
 * @param onExit pops back to wherever the tutorial was entered from (supplied by
 *   `AppNavHost` as `popBackStack()`, and nothing else — §5.1 prohibition 4).
 * @param modifier layout modifier forwarded to [TutorialScreen].
 */
@Composable
fun TutorialRoute(
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TutorialViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // System back is the third caller of the one exit path (D-01d): it must persist the seen
    // flag exactly as Skip does, because a back press that left the flag false would re-force
    // the tutorial on the next launch for a player who deliberately left it.
    //
    // This is the codebase's SECOND BackHandler — the first is `AdQuiescenceScrim` — and it
    // exists for the same class of reason: the default back behaviour would be correct on
    // screen and wrong in the store.
    BackHandler(onBack = viewModel::onExit)

    LaunchedEffect(viewModel) {
        viewModel.exitSignal.collect { onExit() }
    }

    TutorialScreen(
        state = state,
        onCellTap = viewModel::onCellTap,
        onAnimationFinished = viewModel::onAnimationFinished,
        onSkip = viewModel::onExit,
        onDone = viewModel::onExit,
        modifier = modifier,
    )
}
