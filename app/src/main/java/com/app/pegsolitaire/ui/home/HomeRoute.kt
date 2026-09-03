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
 * [HomeScreen]. The `onPlayClassic`/`onPlayChallenge` callbacks are forwarded from
 * `AppNavHost` (→ `ClassicCatalog` / → Challenge gameplay); the onboarding show/dismiss
 * and Play Games opt-in accept/decline handlers come from the ViewModel.
 *
 * @param onPlayClassic navigates to the Classic catalog (supplied by `AppNavHost`).
 * @param onPlayChallenge enters Challenge at the level the screen hands it — the persisted
 *   `HomeUiState.challengeCurrentLevel` (supplied by `AppNavHost`, CHAL-05).
 * @param onResumeClassic continues the saved Classic game on the stored board id
 *   (supplied by `AppNavHost`, PERS-03).
 * @param onResumeChallenge continues the saved Challenge game at the persisted level
 *   (supplied by `AppNavHost`, PERS-03).
 * @param onOpenSettings opens the Settings destination (supplied by `AppNavHost`, D-14).
 * @param modifier layout modifier forwarded to [HomeScreen].
 */
@Composable
fun HomeRoute(
    onPlayClassic: () -> Unit,
    onPlayChallenge: (level: Int) -> Unit,
    onResumeClassic: (levelId: String) -> Unit,
    onResumeChallenge: (level: Int) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onPlayClassic = onPlayClassic,
        onPlayChallenge = onPlayChallenge,
        onResumeClassic = onResumeClassic,
        onResumeChallenge = onResumeChallenge,
        onShowHowToPlay = viewModel::onShowHowToPlay,
        onDismissOnboarding = viewModel::onDismissOnboarding,
        onAcceptPlayGamesPrompt = viewModel::onAcceptPlayGamesPrompt,
        onDeclinePlayGamesPrompt = viewModel::onDeclinePlayGamesPrompt,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}
