package com.app.pegsolitaire.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.domain.assist.AssistAllowanceLimits
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush
import com.app.pegsolitaire.ui.theme.goldGradientFill

/** Test tag of the opt-in dialog surface so screen-level Compose tests can locate it. */
const val AD_OFFER_DIALOG_TEST_TAG = "ad_offer_dialog"

/**
 * The rewarded-ad opt-in dialog (ADS-01, D-06/D-07/D-10).
 *
 * A stateless leaf: it owns no state and no logic, renders the hoisted [reason]/[loading]
 * pair and forwards every interaction through [onAccept]/[onDismiss]. It is hosted in a
 * raw [Dialog] with `usePlatformDefaultWidth = false` rather than the stock M3 alert
 * component (which `RestartConfirmDialog` uses) so the clay [Surface] controls its own
 * width; D-06 rules that component out. Being modal is load-bearing, not cosmetic — the
 * board is inert behind the scrim, which is what makes "declining leaves the game exactly
 * as it was" literally true.
 *
 * ## The accept/decline hierarchy is a policy constraint, not a taste preference
 *
 * The two actions are **stacked full-width visual peers**: the accept is a gradient
 * [Button], the decline a real [TextButton] of the same width directly beneath it — never
 * a muted micro-link tucked under the accept. AdMob's rewarded-inventory policy forbids
 * text or styling that misleads or incentivizes the player towards a particular choice, so
 * restyling the decline down is an account-level risk rather than a visual tweak (T-09-09).
 * The stack is also why a side-by-side row is wrong: the accept button's content swaps to a
 * spinner while loading, and a row would reflow mid-interaction.
 *
 * The body names the count, the scope ("for this level") and the reward before any ad can
 * play (T-09-10), formatted from [AssistAllowanceLimits] so the copy cannot drift from the
 * policy that grants it — D-02's two numbers are written exactly once in the codebase.
 *
 * ## Loading is a lock, not a hint
 *
 * While [loading] both actions are disabled and `dismissOnBackPress`/`dismissOnClickOutside`
 * are false. `requestRewarded` is already in flight, and a dismissal racing a returning
 * `Granted` into a torn-down dialog is a correctness hazard, not a UX nicety (T-09-11). The
 * wait is hard-bounded by `AdConstants.ACCEPT_WAIT_MS`, so the lock can never dead-end.
 * When not loading, scrim tap, system back and "No thanks" are all the same [onDismiss].
 *
 * Tokens: surface `shapes.extraLarge` + `surfaceContainerHigh` +
 * `clayShadow(elevation_dialog)`, inset by `spacing_lg` and capped at `content_max_width`;
 * content column padded `spacing_lg` and spaced `spacing_md`, actions spaced `spacing_sm`;
 * gold reward glyph at `ad_offer_glyph_size`, spinner at `ad_offer_progress_size`; title
 * `headlineSmall`/`onSurface`, body `bodyMedium`/`onSurfaceVariant`, labels `labelLarge`.
 *
 * @param reason which assist ran out — selects the hint or undo title/body copy.
 * @param loading whether the accepted request is in flight (the bounded D-10 wait).
 * @param onAccept the player agreed to watch an ad for more of [reason]'s assist.
 * @param onDismiss the player declined, tapped the scrim or pressed back; nothing changes.
 * @param modifier layout modifier for the dialog surface.
 */
@Composable
fun AdOfferDialog(
    reason: AdReason,
    loading: Boolean,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            if (loading) {
                // D-10 / T-09-11: the request is already in flight, so no player input may
                // tear the dialog down underneath a returning Granted. Bounded by
                // AdConstants.ACCEPT_WAIT_MS, so the lock can never dead-end.
                DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    usePlatformDefaultWidth = false,
                )
            } else {
                // Scrim tap, system back and "No thanks" are all the same onDismiss event.
                DialogProperties(usePlatformDefaultWidth = false)
            },
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
                    .testTag(AD_OFFER_DIALOG_TEST_TAG),
        ) {
            AdOfferContent(
                reason = reason,
                loading = loading,
                onAccept = onAccept,
                onDismiss = onDismiss,
            )
        }
    }
}

/** The sheet body: the gold reward glyph over the title, the body copy and the actions. */
@Composable
private fun AdOfferContent(
    reason: AdReason,
    loading: Boolean,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_lg)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
    ) {
        // Decorative: the title and body carry every bit of the glyph's meaning, so a
        // description here would only make TalkBack read the offer's subject twice.
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier =
                Modifier
                    .size(dimensionResource(R.dimen.ad_offer_glyph_size))
                    .goldGradientFill(),
        )
        Text(
            text = stringResource(reason.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(reason.bodyRes, reason.freeAllowance),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        AdOfferActions(loading = loading, onAccept = onAccept, onDismiss = onDismiss)
    }
}

/** The stacked full-width action pair: the gradient accept over its peer decline. */
@Composable
private fun AdOfferActions(
    loading: Boolean,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    val actionShape = MaterialTheme.shapes.medium
    val loadingLabel = stringResource(R.string.ad_offer_loading)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
    ) {
        // SOUND-04 / D-09 / D-11: both button cues meet here, exactly as they do on
        // RestartConfirmDialog — attached per callsite and NEVER to Modifier.claySquish(),
        // which is also on the controls D-09 keeps silent.
        Button(
            onClick = withClickSound(SoundCue.ButtonForward, onAccept),
            enabled = !loading,
            shape = actionShape,
            // The disabled container stays transparent so the CTA gradient painted by the
            // modifier below still reads as the CTA while its request is in flight; the
            // M3 default would drop an opaque slab over it.
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            modifier =
                Modifier
                    .claySquish()
                    .background(brush = ctaGradientBrush(), shape = actionShape)
                    .fillMaxWidth()
                    .then(
                        if (loading) {
                            Modifier.semantics { contentDescription = loadingLabel }
                        } else {
                            Modifier
                        },
                    ),
        ) {
            if (loading) {
                // Functional, not decorative: it runs regardless of the OS animator scale.
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(dimensionResource(R.dimen.ad_offer_progress_size)),
                )
            } else {
                Text(
                    text = stringResource(R.string.ad_offer_watch),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        TextButton(
            onClick = withClickSound(SoundCue.ButtonDismiss, onDismiss),
            enabled = !loading,
            shape = actionShape,
            modifier =
                Modifier
                    .claySquish()
                    .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.ad_offer_decline),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** The title copy for the assist that ran out. */
private val AdReason.titleRes: Int
    get() =
        when (this) {
            AdReason.HINT -> R.string.ad_offer_hint_title
            AdReason.UNDO -> R.string.ad_offer_undo_title
        }

/** The body copy for the assist that ran out; takes [freeAllowance] as its one argument. */
private val AdReason.bodyRes: Int
    get() =
        when (this) {
            AdReason.HINT -> R.string.ad_offer_hint_body
            AdReason.UNDO -> R.string.ad_offer_undo_body
        }

/**
 * The per-level free allowance this reason names in its copy — the single source of the
 * number the dialog discloses (D-02), read straight from the policy that grants it.
 */
private val AdReason.freeAllowance: Int
    get() =
        when (this) {
            AdReason.HINT -> AssistAllowanceLimits.FREE_HINTS
            AdReason.UNDO -> AssistAllowanceLimits.FREE_UNDOS
        }
