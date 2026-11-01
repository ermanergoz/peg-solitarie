package com.app.pegsolitaire.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush
import com.app.pegsolitaire.ui.window.MirrorSystemBars

/** Test tag of the opt-in dialog surface so screen-level Compose tests can locate it. */
const val PLAY_GAMES_PROMPT_DIALOG_TEST_TAG = "play_games_prompt_dialog"

/**
 * The one-time Play Games opt-in prompt (PGS-01, D-15/D-16) — the single moment a player
 * is ever asked whether to sign in.
 *
 * A stateless leaf: it owns nothing and decides nothing. Whether it appears at all is
 * `HomeViewModel`'s call (`HomeUiState.playGamesPromptVisible`, sequenced after onboarding
 * and retired permanently once answered), and every interaction leaves through
 * [onAccept]/[onDecline]. It is hosted in a raw [Dialog] with
 * `usePlatformDefaultWidth = false` rather than the stock M3 alert component so the clay
 * [Surface] controls its own width — the same shell [com.app.pegsolitaire.ui.game.AdOfferDialog]
 * uses, which D-15 names directly as the model.
 *
 * ## Every exit that is not "Sign in" is "Not now"
 *
 * `onDismissRequest` is wired to [onDecline], not to a separate no-op: D-16 rules that a
 * tap outside the dialog and a press of system back both **count as an answer** — the
 * negative one. That is deliberate and is what makes the prompt genuinely one-time. If
 * dismissal were a non-answer, the flags would stay unwritten and the dialog would return
 * on the next launch, which is exactly the nag the decision forbids (D-17 / T-11-08). A
 * player who walks away has still answered, and the Settings row is the only way back in
 * (D-19).
 *
 * The default [DialogProperties] are therefore correct as-is: nothing here may be locked
 * down. Unlike `AdOfferDialog` there is no `loading` state and no spinner — accepting is
 * fire-and-forget from this composable's point of view. `HomeViewModel` hides the dialog
 * synchronously and only then awaits `signIn()`, so there is no in-flight window for a
 * dismissal to race (T-11-09) and nothing for the player to sit and wait through.
 *
 * ## The two actions are visual peers
 *
 * Stacked full-width, the gradient accept over a real [TextButton] decline of the same
 * width — the same hierarchy `AdOfferDialog` uses. Sign-in is optional by design, so
 * styling the decline down into a micro-link would misrepresent a genuine choice as a
 * formality. No icon: D-16 leaves one optional, and the title and body already carry the
 * whole offer.
 *
 * The body names the same three benefits as the Settings row (leaderboards, achievements,
 * cloud save) and states plainly that the game is fully playable either way, per D-16 —
 * the prompt is an offer, never a wall.
 *
 * Tokens: surface `shapes.extraLarge` + `surfaceContainerHigh` +
 * `clayShadow(elevation_dialog)`, inset by `spacing_lg` and capped at `content_max_width`;
 * content column padded `spacing_lg` and spaced `spacing_md`, actions spaced `spacing_sm`;
 * title `headlineSmall`/`onSurface`, body `bodyMedium`/`onSurfaceVariant`, labels
 * `labelLarge`.
 *
 * @param onAccept the player chose to sign in; the caller persists the opt-in and calls
 *   `signIn()`.
 * @param onDecline the player chose "Not now", tapped outside, or pressed back (D-16);
 *   the caller persists the answer and the SDK is never touched.
 * @param modifier layout modifier for the dialog surface.
 */
@Composable
fun PlayGamesPromptDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        // D-16: scrim tap, system back and "Not now" are all the same answer.
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // D-05g: this window takes the bar state of the screen it opens over (tech design §3.8 (c)).
        MirrorSystemBars()
        val surfaceShape = MaterialTheme.shapes.extraLarge
        Surface(
            shape = surfaceShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_lg))
                    .widthIn(max = dimensionResource(R.dimen.content_max_width))
                    .clayShadow(dimensionResource(R.dimen.elevation_dialog), surfaceShape)
                    .testTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG),
        ) {
            PlayGamesPromptContent(onAccept = onAccept, onDecline = onDecline)
        }
    }
}

/** The sheet body: the question, what signing in buys, and the two actions. */
@Composable
private fun PlayGamesPromptContent(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_lg)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
    ) {
        Text(
            text = stringResource(R.string.play_games_prompt_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.play_games_prompt_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PlayGamesPromptActions(onAccept = onAccept, onDecline = onDecline)
    }
}

/** The stacked full-width action pair: the gradient accept over its peer decline. */
@Composable
private fun PlayGamesPromptActions(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val actionShape = MaterialTheme.shapes.medium
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
    ) {
        // SOUND-04 / D-09 / D-11: both button cues are attached per callsite and NEVER to
        // Modifier.claySquish() below them — that modifier is also on the controls D-09
        // requires to stay silent (the full exclusion list is in the withClickSound KDoc).
        Button(
            onClick = withClickSound(SoundCue.ButtonForward, onAccept),
            shape = actionShape,
            // Transparent container so the CTA gradient painted by the modifier below is
            // what the player sees; the M3 default would drop an opaque slab over it.
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            modifier =
                Modifier
                    .claySquish()
                    .background(brush = ctaGradientBrush(), shape = actionShape)
                    .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.play_games_prompt_accept),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(
            onClick = withClickSound(SoundCue.ButtonDismiss, onDecline),
            shape = actionShape,
            modifier =
                Modifier
                    .claySquish()
                    .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.play_games_prompt_decline),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
