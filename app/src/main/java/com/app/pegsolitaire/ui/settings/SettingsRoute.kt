package com.app.pegsolitaire.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The stateful Settings route (top of the Route → Screen → Leaf boundary).
 *
 * Obtains the [SettingsViewModel] from the Hilt graph via [hiltViewModel], collects its
 * read-only `uiState` in a lifecycle-aware way ([collectAsStateWithLifecycle]), and hands
 * the immutable state plus the VM's method references down to the stateless
 * [SettingsScreen]. The `onBack` lambda is supplied by `AppNavHost` (pops back to Home),
 * so the screen never touches a `NavController`.
 *
 * @param onBack leaves Settings (supplied by `AppNavHost`).
 * @param onShowHowToPlay opens the tutorial destination (supplied by `AppNavHost`, D-02a).
 * @param onOpenLicenses opens the Licenses screen (supplied by `AppNavHost`, spec LS-01).
 * @param modifier layout modifier forwarded to [SettingsScreen].
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onShowHowToPlay: () -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onToggleSound = viewModel::onToggleSound,
        onToggleHaptics = viewModel::onToggleHaptics,
        onShowHowToPlay = onShowHowToPlay,
        onOpenLicenses = onOpenLicenses,
        onSignInToPlayGames = viewModel::onSignInToPlayGames,
        onShowLeaderboards = viewModel::onShowLeaderboards,
        onShowAchievements = viewModel::onShowAchievements,
        onPlayGamesSignInErrorShown = viewModel::onPlayGamesSignInErrorShown,
        onManagePrivacyChoices = viewModel::onManagePrivacyChoices,
        onBack = onBack,
        modifier = modifier,
    )
}
