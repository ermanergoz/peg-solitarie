package com.app.pegsolitaire.ui.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
 * **The six lambdas are split into two groups, and that split IS D-06.** Only the two
 * forward-moving *win* actions — Challenge "Next level" and Classic "Catalog" — are routed
 * into the ViewModel, so only they can ever reach the interstitial cadence policy. The other
 * four go straight down to the screen and never enter the ViewModel at all:
 *
 *  - **`onReplay`** is a retry, and retry stays free (`AdReason.RETRY` was dropped at
 *    requirements time so that repeating a level you just lost never costs an ad).
 *  - **`onHome`** is an exit, which AdMob policy singles out as a disallowed interstitial
 *    placement.
 *  - **`onRetry`** is the Challenge-stuck loss, which INT-03 excludes outright.
 *  - **`onNavigateBack`** is the top-bar back nav, an exit like Home.
 *
 * That makes those four *structurally* unable to gain an ad later. Doing so would require
 * adding a member to [ResultsNavTarget] and re-pointing the lambda here — a visible,
 * reviewable change rather than a silently widened condition somewhere in the ViewModel.
 *
 * @param onReplay re-plays the same level (the VM's `replayId`). Classic only. Direct
 *   pass-through — never ad-gated.
 * @param onCatalog pops back to the Classic catalog. Classic only. Ad-eligible (D-06):
 *   the tap goes to `ResultsViewModel.onCatalogTapped` and this lambda is invoked later,
 *   from the navigation effect below, rather than from the button's `onClick`.
 * @param onHome pops back to Home. Direct pass-through — never ad-gated.
 * @param onNextLevel navigates to the next Challenge level (D-01), which is
 *   `max(ladder head, level just solved + 1)` as resolved by the ViewModel (D-04a) — never an
 *   arithmetic successor computed by the screen. Ad-eligible (D-06): the tap goes to
 *   `ResultsViewModel.onNextLevelTapped` and this lambda is invoked later, from the navigation
 *   effect below, with the resolved level, rather than from the button's `onClick`.
 * @param onRetry reloads the same deterministic Challenge board when stuck (D-10). Direct
 *   pass-through — never ad-gated (INT-03).
 * @param onNavigateBack leaves the Results screen (top-bar back nav). Direct pass-through.
 * @param modifier layout modifier forwarded to [ResultsScreen].
 */
@Composable
fun ResultsRoute(
    onReplay: (String) -> Unit,
    onCatalog: () -> Unit,
    onHome: () -> Unit,
    onNextLevel: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ResultsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The one-shot navigation consumer. Two properties make it correct, and both are
    // deliberate:
    //
    //  1. It is KEYED on the target, so the effect body runs once per distinct target. A
    //     recomposition — of which there are several during the celebration animations —
    //     cannot re-fire it, and onNavigationHandled() clearing the field to null is itself
    //     a distinct key that does nothing.
    //  2. The target is read from a StateFlow FIELD, not from an event stream. The host is
    //     STOPPED for the whole fullscreen ad presentation, so this collector is not running
    //     when the dismiss callback lands. A state field is simply read when collection
    //     resumes; a Channel/SharedFlow emission may be dropped outright or re-delivered
    //     (RESEARCH Pitfall 6). GameRoute's finished-collector is the shape borrowed here,
    //     but NOT its Channel backing — a re-delivery on this path is a Challenge ladder that
    //     advances two levels for one tap.
    LaunchedEffect(state.pendingNavigation) {
        when (val target = state.pendingNavigation) {
            is ResultsNavTarget.NextLevel -> {
                onNextLevel(target.level)
                viewModel.onNavigationHandled()
            }

            ResultsNavTarget.Catalog -> {
                onCatalog()
                viewModel.onNavigationHandled()
            }

            null -> Unit
        }
    }

    ResultsScreen(
        state = state,
        onReplay = onReplay,
        onCatalog = viewModel::onCatalogTapped,
        onHome = onHome,
        onNextLevel = viewModel::onNextLevelTapped,
        onRetry = onRetry,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}
