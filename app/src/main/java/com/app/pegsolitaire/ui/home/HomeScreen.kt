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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
 * affordance — D-15) over a centered column, capped at `content_max_width`, of two
 * stacked mode tiles: an enabled Classic tile whose whole surface navigates to the
 * catalog ([onPlayClassic]) and a disabled Challenge tile carrying a "Coming soon"
 * badge with no route (D-09). While [HomeUiState.onboardingVisible], the
 * [OnboardingOverlay] is hosted over the body (first-run, D-14, or the help affordance,
 * D-15); dismissing it calls [onDismissOnboarding] (which `markSeen()`s, D-13).
 *
 * @param state the immutable Home UI state (the onboarding-visibility gate).
 * @param onPlayClassic navigates to the Classic catalog (the screen's primary affordance).
 * @param onShowHowToPlay re-opens the onboarding overlay any time (D-15).
 * @param onDismissOnboarding dismisses + persists the onboarding (D-13).
 * @param modifier layout modifier for the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlayClassic: () -> Unit,
    onShowHowToPlay: () -> Unit,
    onDismissOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { HomeTopBar(onShowHowToPlay = onShowHowToPlay) },
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
                ClassicTile(onPlayClassic = onPlayClassic)
                ChallengeTile()
            }
        }
    }

    if (state.onboardingVisible) {
        OnboardingOverlay(onDismiss = onDismissOnboarding)
    }
}

/** The Home top bar: the app title and the permanent How-to-play affordance (D-15). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onShowHowToPlay: () -> Unit) {
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
        },
    )
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
 * The disabled Challenge tile (D-09): an identical-footprint `surfaceContainerHigh`
 * `Card`, `enabled = false` (disabled-alpha content), with a `secondaryContainer`
 * "Coming soon" pill badge top-right. No `onClick`, no route — it reserves the slot
 * for Phase 3.
 */
@Composable
private fun ChallengeTile() {
    Card(
        enabled = false,
        onClick = {},
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.mode_tile_min_height)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
        ) {
            Text(
                text = stringResource(R.string.challenge_tile_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            ComingSoonBadge(modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/** The "Coming soon" pill badge on the disabled Challenge tile (D-09) — deliberately not accent. */
@Composable
private fun ComingSoonBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.coming_soon),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier =
                Modifier.padding(
                    horizontal = dimensionResource(R.dimen.spacing_sm),
                    vertical = dimensionResource(R.dimen.spacing_xs),
                ),
        )
    }
}
