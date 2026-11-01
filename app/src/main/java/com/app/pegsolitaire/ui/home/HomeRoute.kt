package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The stateful Home route (top of the Route → Screen → Leaf boundary).
 *
 * Obtains the [HomeViewModel] from the Hilt graph via [hiltViewModel], collects its
 * read-only `uiState` in a lifecycle-aware way ([collectAsStateWithLifecycle]), and
 * hands the immutable state plus the nav/handler lambdas down to the stateless
 * [HomeScreen]. The `onPlayClassic`/`onPlayChallenge` callbacks are forwarded from
 * `AppNavHost` (→ `ClassicCatalog` / → Challenge gameplay); the Play Games opt-in
 * accept/decline, the two account-switch answers (D-13/D-14) and the
 * sign-in-error-shown acknowledgement come from the ViewModel.
 *
 * ## The first-run auto-start (D-02a)
 *
 * While [HomeUiState.tutorialPending] stands, this route pushes the `Tutorial` destination
 * once. First-run behaviour is therefore unchanged from the retired overlay's — the player
 * still meets the tutorial before anything else — but it is now a destination rather than a
 * sheet Home hosts, which is what keeps the gameplay chrome and `SoundCue.Win` out of a
 * teaching screen (ux-spec §5.1).
 *
 * The effect is keyed on the flag, so it fires on the transition into "pending" and not once
 * per recomposition. It cannot loop: the tutorial **awaits** `markSeen()` before it emits its
 * exit signal, so by the time this route re-enters composition on the pop the flag is already
 * false. A fire-and-forget write is exactly what would make it loop, which is why the ordering
 * lives in `TutorialViewModel` and is asserted there rather than being guarded here.
 *
 * @param onPlayClassic navigates to the Classic catalog (supplied by `AppNavHost`).
 * @param onPlayChallenge enters Challenge at the level the screen hands it — the persisted
 *   `HomeUiState.challengeCurrentLevel` (supplied by `AppNavHost`, CHAL-05).
 * @param onResumeClassic continues the saved Classic game on the stored board id
 *   (supplied by `AppNavHost`, PERS-03).
 * @param onResumeChallenge continues the saved Challenge game at the persisted level
 *   (supplied by `AppNavHost`, PERS-03).
 * @param onOpenSettings opens the Settings destination (supplied by `AppNavHost`, D-14).
 * @param onStartTutorial pushes the `Tutorial` destination (supplied by `AppNavHost`, D-02a);
 *   called once, from the auto-start effect above, and from nowhere else — Home carries no
 *   tutorial affordance for the player to press.
 * @param modifier layout modifier forwarded to [HomeScreen].
 */
@Composable
fun HomeRoute(
    onPlayClassic: () -> Unit,
    onPlayChallenge: (level: Int) -> Unit,
    onResumeClassic: (levelId: String) -> Unit,
    onResumeChallenge: (level: Int) -> Unit,
    onOpenSettings: () -> Unit,
    onStartTutorial: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val latestOnStartTutorial by rememberUpdatedState(onStartTutorial)
    LaunchedEffect(state.tutorialPending) {
        if (state.tutorialPending) latestOnStartTutorial()
    }

    HomeScreen(
        state = state,
        onPlayClassic = onPlayClassic,
        onPlayChallenge = onPlayChallenge,
        onResumeClassic = onResumeClassic,
        onResumeChallenge = onResumeChallenge,
        onAcceptPlayGamesPrompt = viewModel::onAcceptPlayGamesPrompt,
        onDeclinePlayGamesPrompt = viewModel::onDeclinePlayGamesPrompt,
        onKeepDeviceProgress = viewModel::onKeepDeviceProgress,
        onUseCloudProgress = viewModel::onUseCloudProgress,
        onPlayGamesSignInErrorShown = viewModel::onPlayGamesSignInErrorShown,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}
