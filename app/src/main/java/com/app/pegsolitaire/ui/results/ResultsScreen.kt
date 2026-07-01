package com.app.pegsolitaire.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.game.GameTimerFormat
import androidx.compose.material.icons.outlined.Star as StarBorder

/** Test tag of the star row; carries the [EarnedStarCountKey] semantics for the screen test. */
const val RESULTS_STAR_ROW_TEST_TAG = "results_star_row"

/**
 * Semantics: how many of the three stars are filled (earned). The individual star
 * icons are decorative (`contentDescription = null`) — the headline + metrics convey
 * the outcome to screen readers — so this row-level count is what the test asserts.
 */
val EarnedStarCountKey = SemanticsPropertyKey<Int>("EarnedStarCount")

private var SemanticsPropertyReceiver.earnedStarCountValue by EarnedStarCountKey

/**
 * The stateless end-of-game Results screen (SCOR-03, D-05 — replaces the Phase-1
 * `GameOverDialog`).
 *
 * Renders [state] and forwards events; owns no logic (state hoisting). An M3
 * [Scaffold] hosts a [TopAppBar] (a back nav, the "Results" title) over a centered
 * content column capped at `content_max_width`, stacking: the star-tiered headline
 * (D-08), the 3-star row (the first [ResultsUiState.stars] filled `primary`, the rest
 * outline `outlineVariant`), the SCORE / TIME / PEGS LEFT metric block, and the three
 * actions — a single filled `primary` Replay [Button] (the one accent, wrapping
 * `replayId`) plus neutral Catalog / Home buttons. Classic has no "next board", so that
 * Challenge-mode action is intentionally absent.
 *
 * Every value is read straight from [state]; mm:ss is formatted from
 * [ResultsUiState.elapsedSeconds] via [GameTimerFormat] — no scoring is recomputed here.
 *
 * @param state the immutable Results UI state to render.
 * @param onReplay re-plays the same level (carries `replayId`). Classic only.
 * @param onCatalog pops back to the Classic catalog. Classic only.
 * @param onHome pops back to Home.
 * @param onNextLevel advances a solved Challenge level to N+1 (carries the next level, D-01).
 * @param onRetry reloads the same deterministic Challenge board when stuck (carries N, D-10).
 * @param onNavigateBack leaves the Results screen (top-bar back nav).
 * @param modifier layout modifier for the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    state: ResultsUiState,
    onReplay: (String) -> Unit,
    onCatalog: () -> Unit,
    onHome: () -> Unit,
    onNextLevel: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ResultsTopBar(onNavigateBack = onNavigateBack) },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(dimensionResource(R.dimen.spacing_md)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .widthIn(max = dimensionResource(R.dimen.content_max_width))
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_lg)),
            ) {
                Text(
                    text = stringResource(headlineResFor(state.headlineTier)),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                StarRow(earned = state.stars)
                MetricBlock(state = state)
                ResultsActions(
                    state = state,
                    onReplay = onReplay,
                    onCatalog = onCatalog,
                    onHome = onHome,
                    onNextLevel = onNextLevel,
                    onRetry = onRetry,
                )
            }
        }
    }
}

/** The Results top bar: a back nav and the "Results" title. Stateless. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.results_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
    )
}

/**
 * The three-star row (D-01): the first [earned] stars are filled [Icons.Filled.Star]
 * tinted `primary`, the remaining stars are outline [StarBorder] tinted
 * `outlineVariant`. Each icon is decorative; the row exposes [EarnedStarCountKey] so
 * the test can assert the filled count without per-icon semantics.
 */
@Composable
private fun StarRow(earned: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
        modifier =
            Modifier
                .testTag(RESULTS_STAR_ROW_TEST_TAG)
                .semantics { earnedStarCountValue = earned },
    ) {
        repeat(TOTAL_STARS) { index ->
            val isEarned = index < earned
            Icon(
                imageVector = if (isEarned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint =
                    if (isEarned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                modifier = Modifier.size(dimensionResource(R.dimen.results_star_size)),
            )
        }
    }
}

/**
 * The SCORE / TIME / PEGS LEFT metric block: three labelled values read straight from
 * [state]. The score and pegs are the raw route-arg integers; the time is the
 * `elapsedSeconds` rendered `mm:ss` via [GameTimerFormat] (no scoring recomputed here).
 */
@Composable
private fun MetricBlock(state: ResultsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Metric(value = state.score.toString(), label = stringResource(R.string.results_label_score))
        Metric(
            value = GameTimerFormat.format(state.elapsedSeconds),
            label = stringResource(R.string.results_label_time),
        )
        Metric(
            value = state.pegsRemaining.toString(),
            label = stringResource(R.string.results_label_pegs_left),
        )
    }
}

/**
 * One labelled metric: the [value] (`titleLarge`, `onSurface`) over its [label]
 * caption (`labelLarge`, `onSurfaceVariant`).
 */
@Composable
private fun Metric(
    value: String,
    label: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The end-of-game actions, mode-aware with strict one-accent discipline (UI-SPEC S3):
 * exactly one filled `primary` [Button] per state, the sole accent on the screen.
 *
 * - Classic ([ResultsUiState.challengeLevel] == null): a filled Replay (carrying
 *   `replayId`) over neutral Catalog / Home buttons — Classic has no "next board".
 * - Challenge solved ([ResultsUiState.showNextLevel], D-01): a filled "Next level"
 *   advancing to N+1 over a neutral Home. No Replay/Catalog.
 * - Challenge stuck (D-10): a filled "Retry" reloading the same deterministic board (N)
 *   over a neutral Home. "Next level" is absent.
 */
@Composable
private fun ResultsActions(
    state: ResultsUiState,
    onReplay: (String) -> Unit,
    onCatalog: () -> Unit,
    onHome: () -> Unit,
    onNextLevel: (Int) -> Unit,
    onRetry: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
    ) {
        val challengeLevel = state.challengeLevel
        if (challengeLevel == null) {
            ClassicActions(replayId = state.replayId, onReplay = onReplay, onCatalog = onCatalog)
        } else if (state.showNextLevel) {
            PrimaryAction(
                textRes = R.string.action_next_level,
                onClick = { onNextLevel(challengeLevel + 1) },
            )
        } else {
            PrimaryAction(
                textRes = R.string.action_retry,
                onClick = { onRetry(challengeLevel) },
            )
        }
        NeutralAction(textRes = R.string.action_home, onClick = onHome)
    }
}

/**
 * The Classic action pair: the one filled `primary` Replay accent over a neutral
 * `OutlinedButton` Catalog (the shared Home neutral is appended by the caller).
 */
@Composable
private fun ClassicActions(
    replayId: String,
    onReplay: (String) -> Unit,
    onCatalog: () -> Unit,
) {
    PrimaryAction(textRes = R.string.action_replay, onClick = { onReplay(replayId) })
    OutlinedButton(
        onClick = onCatalog,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.action_catalog),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The single filled `primary` accent button for the current state (the one accent, UI-SPEC S3). */
@Composable
private fun PrimaryAction(
    textRes: Int,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** A neutral secondary action with an `onSurface` label — deliberately not accent. */
@Composable
private fun NeutralAction(
    textRes: Int,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The headline string resource for [tier] (D-08). */
private fun headlineResFor(tier: ResultsHeadlineTier): Int =
    when (tier) {
        ResultsHeadlineTier.PERFECT -> R.string.results_headline_perfect
        ResultsHeadlineTier.SOLVED -> R.string.results_headline_solved
        ResultsHeadlineTier.STUCK -> R.string.results_headline_stuck
    }

/** The total number of stars rendered on the row (earned + unearned). */
private const val TOTAL_STARS = 3
