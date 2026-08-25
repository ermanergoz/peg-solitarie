package com.app.pegsolitaire.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.onboarding.OnboardingOverlay

/**
 * The stateless Home mode-select screen (NAV-01, D-09/D-10/D-14/D-15).
 *
 * Renders [state] and forwards events; owns no logic (state hoisting). An M3
 * [Scaffold] hosts a [TopAppBar] (title "Peg Solitaire", a trailing How-to-play
 * affordance — D-15 — and a Settings gear opening the Settings destination, Phase-5
 * D-14) over a centered column, capped at `content_max_width`, of two
 * stacked mode tiles: an enabled Classic tile whose whole surface navigates to the
 * catalog ([onPlayClassic]) and an enabled Challenge tile whose whole surface enters
 * Challenge ([onPlayChallenge]) at the player's persisted
 * [HomeUiState.challengeCurrentLevel] (CHAL-05). While [HomeUiState.onboardingVisible],
 * the [OnboardingOverlay] is hosted over the body (first-run, D-14, or the help
 * affordance, D-15); dismissing it calls [onDismissOnboarding] (which `markSeen()`s, D-13).
 *
 * Above the mode tiles sit up to two "Continue" affordances (PERS-03, D-01/D-03) — one per
 * mode holding a saved in-progress game. They are independent: a Classic and a Challenge
 * game can both be waiting, and each carries what its resume needs (the stored board id /
 * the persisted level) so the host never has to guess.
 *
 * @param state the immutable Home UI state (the onboarding gate + the persisted level).
 * @param onPlayClassic navigates to the Classic catalog (the screen's primary affordance).
 * @param onPlayChallenge enters Challenge at the level it is handed — the persisted
 *   [HomeUiState.challengeCurrentLevel] (CHAL-05); the screen never invents the level.
 * @param onResumeClassic continues the saved Classic game on the board id it is handed —
 *   [HomeUiState.classicResumeLevelId], the board the snapshot was taken on (PERS-03).
 * @param onResumeChallenge continues the saved Challenge game at the snapshot's own
 *   stamped level ([HomeUiState.challengeResumeLevel], WR-03) — never the ladder level.
 * @param onShowHowToPlay re-opens the onboarding overlay any time (D-15).
 * @param onDismissOnboarding dismisses + persists the onboarding (D-13).
 * @param onOpenSettings opens the Settings destination from the top-bar gear (Phase-5
 *   D-14); a hoisted lambda supplied by `AppNavHost` — the screen never sees a controller.
 * @param modifier layout modifier for the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlayClassic: () -> Unit,
    onPlayChallenge: (level: Int) -> Unit,
    onResumeClassic: (levelId: String) -> Unit,
    onResumeChallenge: (level: Int) -> Unit,
    onShowHowToPlay: () -> Unit,
    onDismissOnboarding: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                onShowHowToPlay = onShowHowToPlay,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = dimensionResource(R.dimen.content_max_width))
                        .padding(dimensionResource(R.dimen.spacing_md)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            ) {
                // Up to two Continue affordances, above the mode tiles: whatever the player
                // left unfinished is the first thing they see (D-01/D-03).
                val resumeLevelId = state.classicResumeLevelId
                if (resumeLevelId != null) {
                    ResumeTile(
                        title = stringResource(R.string.resume_classic_title),
                        supporting = stringResource(R.string.resume_classic_supporting),
                        onResume = { onResumeClassic(resumeLevelId) },
                    )
                }
                // The Challenge Continue carries the SNAPSHOT's stamped level (WR-03),
                // never challengeCurrentLevel — the ladder flow can lag at a cold start
                // while the stamp always names the level the saved board really is.
                val challengeResumeLevel = state.challengeResumeLevel
                if (challengeResumeLevel != null) {
                    ResumeTile(
                        title = stringResource(R.string.resume_challenge_title),
                        supporting =
                            stringResource(
                                R.string.resume_challenge_supporting,
                                challengeResumeLevel,
                            ),
                        onResume = { onResumeChallenge(challengeResumeLevel) },
                    )
                }
                ClassicTile(onPlayClassic = onPlayClassic)
                ChallengeTile(
                    currentLevel = state.challengeCurrentLevel,
                    onPlayChallenge = onPlayChallenge,
                )
            }
        }
    }

    if (state.onboardingVisible) {
        OnboardingOverlay(onDismiss = onDismissOnboarding)
    }
}

/**
 * The Home top bar: the app title, the permanent How-to-play affordance (D-15) and the
 * Settings gear (Phase-5 D-14). The gear uses the curated core Material icon set already
 * on the classpath (`Icons.Rounded.Settings`) — no `material-icons-extended` dependency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onShowHowToPlay: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        actions = {
            IconButton(onClick = onShowHowToPlay) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = stringResource(R.string.how_to_play),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings_action),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
        },
    )
}

/**
 * A "Continue" affordance for one mode's saved in-progress game (PERS-03, D-01).
 *
 * A `secondaryContainer` `Card` — visually distinct from the neutral mode tiles, because it
 * is an offer to return to something rather than a mode choice — whose whole surface
 * resumes. Stateless: the caller decides whether it renders at all and what [onResume]
 * carries (the stored board id / the persisted level).
 */
@Composable
private fun ResumeTile(
    title: String,
    supporting: String,
    onResume: () -> Unit,
) {
    Card(
        onClick = onResume,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.resume_tile_min_height)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

/**
 * The enabled Classic tile (D-10): a `surfaceContainerHigh` `Card` whose whole surface
 * navigates to the catalog. Title "Classic" + supporting "5 boards". The screen's
 * primary affordance.
 */
@Composable
private fun ClassicTile(onPlayClassic: () -> Unit) {
    Card(
        onClick = onPlayClassic,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.mode_tile_min_height)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
        ) {
            Text(
                text = stringResource(R.string.classic_tile_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.classic_tile_supporting),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The enabled Challenge tile (D-04): a neutral `surfaceContainerHigh` `Card` — an exact
 * peer of [ClassicTile] (NOT accent; enabling it does not make it accent, UI-SPEC S1) —
 * whose whole surface enters Challenge at [currentLevel], the player's persisted ladder
 * position (CHAL-05). Title "Challenge" + supporting "Continue — Level N", so the stored
 * progress is visible, not just used. Same `mode_tile_min_height` footprint as Classic.
 */
@Composable
private fun ChallengeTile(
    currentLevel: Int,
    onPlayChallenge: (level: Int) -> Unit,
) {
    Card(
        onClick = { onPlayChallenge(currentLevel) },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.mode_tile_min_height)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
        ) {
            Text(
                text = stringResource(R.string.challenge_tile_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.challenge_tile_continue, currentLevel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
