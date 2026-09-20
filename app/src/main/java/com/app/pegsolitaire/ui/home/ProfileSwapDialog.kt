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

/** Test tag of the profile-swap dialog surface so screen-level Compose tests can locate it. */
const val PROFILE_SWAP_DIALOG_TEST_TAG = "profile_swap_dialog"

/**
 * The account-switch question (CLOUD-02, D-12 to D-15) — the **one** dialog this milestone
 * reserved.
 *
 * Every other conflict in the phase resolves silently by monotonic merge: two devices on
 * one account always converge to the union, with no prompt and no loss. This case cannot,
 * because merging two *different* players' progress is not a merge, it is a leak. So the
 * player is asked — once, on Home ([HomeUiState.profileSwapVisible], D-15), never over
 * gameplay, Results or Settings, and never stacked with the two first-run surfaces.
 *
 * A stateless leaf, exactly like [PlayGamesPromptDialog]: it owns nothing, remembers
 * nothing and decides nothing. It is hosted in a raw [Dialog] with
 * `usePlatformDefaultWidth = false` so the clay [Surface] controls its own width — the same
 * shell the opt-in prompt and `AdOfferDialog` use.
 *
 * ## Every dismissal is "Keep this device's progress"
 *
 * `onDismissRequest` is [onKeepLocal]. That single assignment **is** D-13: system back, a
 * tap outside the scrim and any other dismissal all count as the safe, non-destructive
 * answer. It is written as the existing lambda rather than as a separate `onDismiss`
 * parameter deliberately — there is then nowhere for a second, divergent default to live,
 * and no way for a later edit to route a walk-away into the wipe (T-13-42). The data layer
 * has no notion of "dismissed" for the same reason: the mapping exists exactly once, here.
 *
 * ## The gradient is on the SAFE answer — the inverse of the opt-in prompt
 *
 * [PlayGamesPromptDialog] puts the gradient CTA on its affirmative action, because there
 * the affirmative action is the offer. Here the emphasis is inverted on purpose: the
 * gradient goes on "Keep this device's progress" and the destructive "Use their cloud
 * progress" is a plain [TextButton]. D-13 makes non-destruction the default, and a gradient
 * on a wipe is a trap — it makes the irreversible option the one a thumb finds first
 * (T-13-43). The sound cues follow the same inversion: [SoundCue.ButtonForward] on the safe
 * answer, [SoundCue.ButtonDismiss] on the replace.
 *
 * ## The body names what is destroyed
 *
 * D-14 requires the copy to state exactly what the replace path replaces, so the body
 * enumerates all four categories it deletes — level results, play history, the Challenge
 * level and both in-progress games (T-13-44). [accountName] is legitimately `null` when the
 * SDK's display-name lookup failed or stalled (Phase 11 D-14, a degraded name and never a
 * failed sign-in), and the fallback body carries the same enumeration in neutral phrasing
 * rather than rendering a gap.
 *
 * Tokens: surface `shapes.extraLarge` + `surfaceContainerHigh` +
 * `clayShadow(elevation_dialog)`, inset by `spacing_lg` and capped at `content_max_width`;
 * content column padded `spacing_lg` and spaced `spacing_md`, actions spaced `spacing_sm`;
 * title `headlineSmall`/`onSurface`, body `bodyMedium`/`onSurfaceVariant`, labels
 * `labelLarge`.
 *
 * @param accountName the arriving account's Play Games display name, or `null` when the
 *   lookup did not land — the body falls back to neutral phrasing, never to "null".
 * @param onKeepLocal the player kept this device's progress (D-13's merge) — **and also
 *   what a tap outside or a back press means**.
 * @param onUseCloud the player chose the arriving account's cloud progress (D-14's
 *   replace); the only destructive path in the app.
 * @param modifier layout modifier for the dialog surface.
 */
@Composable
fun ProfileSwapDialog(
    accountName: String?,
    onKeepLocal: () -> Unit,
    onUseCloud: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        // D-13: scrim tap, system back and "Keep this device's progress" are one answer.
        onDismissRequest = onKeepLocal,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
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
                    .testTag(PROFILE_SWAP_DIALOG_TEST_TAG),
        ) {
            ProfileSwapContent(
                accountName = accountName,
                onKeepLocal = onKeepLocal,
                onUseCloud = onUseCloud,
            )
        }
    }
}

/** The sheet body: the question, D-14's wipe list, and the two answers. */
@Composable
private fun ProfileSwapContent(
    accountName: String?,
    onKeepLocal: () -> Unit,
    onUseCloud: () -> Unit,
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
            text = stringResource(R.string.profile_swap_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            // An unknown display name is a degraded name, not a failed sign-in (Phase 11
            // D-14): the fallback says "this account" and carries the same wipe list.
            text =
                if (accountName == null) {
                    stringResource(R.string.profile_swap_body_unknown_account)
                } else {
                    stringResource(R.string.profile_swap_body, accountName)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        ProfileSwapActions(onKeepLocal = onKeepLocal, onUseCloud = onUseCloud)
    }
}

/**
 * The stacked full-width action pair — with the emphasis **inverted** relative to
 * [PlayGamesPromptDialog].
 *
 * There the gradient carries the affirmative answer, because the affirmative answer is the
 * offer and declining costs nothing. Here it carries the *safe* answer: D-13 makes "Keep
 * this device's progress" the default that back and tap-outside both resolve to, and
 * putting the CTA gradient on the irreversible wipe would make the destructive option the
 * one a thumb finds first (T-13-43). The replace is a [TextButton] with the dismiss cue —
 * reachable, clearly labelled, and deliberately not the path of least resistance.
 */
@Composable
private fun ProfileSwapActions(
    onKeepLocal: () -> Unit,
    onUseCloud: () -> Unit,
) {
    val actionShape = MaterialTheme.shapes.medium
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
    ) {
        // SOUND-04 / D-09 / D-11: both cues are attached per callsite and NEVER to the
        // Modifier.claySquish() below them (the exclusion list is in withClickSound's KDoc).
        Button(
            onClick = withClickSound(SoundCue.ButtonForward, onKeepLocal),
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
                text = stringResource(R.string.profile_swap_keep),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(
            onClick = withClickSound(SoundCue.ButtonDismiss, onUseCloud),
            shape = actionShape,
            modifier =
                Modifier
                    .claySquish()
                    .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.profile_swap_use_cloud),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
