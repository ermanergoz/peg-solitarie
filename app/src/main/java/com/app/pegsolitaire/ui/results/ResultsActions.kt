package com.app.pegsolitaire.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush

/**
 * The end-of-game actions, mode-aware with strict one-accent discipline (UI-SPEC S3):
 * exactly one CtaGradient primary action per state, the sole accent on the screen; the
 * rest are quiet clay chips.
 *
 * - Classic ([ResultsUiState.challengeLevel] == null): a gradient Replay (carrying
 *   `replayId`) over quiet Catalog / Home chips — Classic has no "next board".
 * - Challenge solved ([ResultsUiState.showNextLevel], D-01): a gradient "Next level"
 *   advancing to N+1 over a quiet Home. No Replay/Catalog.
 * - Challenge stuck (D-10): a gradient "Retry" reloading the same deterministic board (N)
 *   over a quiet Home. "Next level" is absent.
 */
@Composable
internal fun ResultsActions(
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
 * The Classic action pair: the one CtaGradient Replay accent over a quiet clay
 * `OutlinedButton` Catalog (the shared Home chip is appended by the caller).
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
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .claySquish()
                .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.action_catalog),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The single CtaGradient accent button for the current state (the one accent, UI-SPEC
 * S3): a transparent-container [Button] whose background is the gradient brush, so the
 * label still sits on the high-contrast half of the ramp.
 */
@Composable
private fun PrimaryAction(
    textRes: Int,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Button(
        onClick = onClick,
        shape = shape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier =
            Modifier
                .claySquish()
                .clayShadow(dimensionResource(R.dimen.elevation_card), shape)
                .background(brush = ctaGradientBrush(), shape = shape)
                .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/** A quiet clay chip secondary action with an `onSurface` label — deliberately not accent. */
@Composable
private fun NeutralAction(
    textRes: Int,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .claySquish()
                .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
