package com.app.pegsolitaire.ui.results

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R

/**
 * The INT-04 quiescence scrim: a full-bleed dim that makes the Results screen inert for
 * the instant between an ad-eligible continue tap and the interstitial rendering.
 *
 * This is a **correctness** device, not a UX flourish — the same argument
 * [com.app.pegsolitaire.ui.game.AdOfferDialog] makes for locking its dialog while a
 * request is in flight. In the `AdConstants.INTERSTITIAL_QUIESCENCE_MS` window the player's
 * finger is still travelling, and a stray second tap that lands after a fullscreen ad has
 * appeared underneath it is an accidental click — invalid traffic, which AdMob attributes
 * to the publisher. Closing that window is the whole reason this composable exists.
 *
 * D-08 governs what is DRAWN here: no text, no spinner, no progress. The dim *is* the
 * "something is coming" signal. It is not a timed animation either — it renders purely from
 * [ResultsUiState.adScrimVisible], which the ViewModel lowers in a `finally` on every exit
 * path (dismissed, failed to show, cancelled), so the block can neither outlive the
 * presentation nor lift while the ad is still opening.
 *
 * Two input channels reach a Compose screen, and covering the buttons only closes one of
 * them, so each is swallowed explicitly:
 *
 *  - **Pointer.** Every event is consumed at [PointerEventPass.Initial]. The `Initial` pass
 *    is the point: it runs top-down before any descendant or sibling sees the event, so a
 *    tap is dead before it can reach a `clickable`. Disabling each action button instead
 *    would leave the `TopAppBar`'s back arrow and the scrollable content live, which is
 *    exactly the gap INT-04 names.
 *  - **Back.** [BackHandler] takes the back press and does nothing with it. A queued back
 *    press during the pause must not escape either — INT-04's window closes on *every*
 *    input route or it does not close at all.
 *
 * Neither `BackHandler` nor pointer-event consumption appears anywhere else under
 * `app/src/main` today, so this file is the repo's first use of each. There is no earlier
 * precedent to go hunting for.
 *
 * It is still announced to accessibility services via `results_ad_scrim_description`.
 * D-08's "no text, no spinner" is about pixels; it is not a licence to leave a screen
 * reader facing a silent dead zone that swallows its user's taps without explanation.
 *
 * @param modifier layout modifier for the scrim; callers pass none — it is always full-bleed.
 */
@Composable
internal fun AdQuiescenceScrim(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.results_ad_scrim_description)

    BackHandler(enabled = true) {
        // Deliberately swallowed: see the KDoc above. A back press during the quiescence
        // pause must not tear the screen down underneath an ad that is already opening.
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = AD_SCRIM_ALPHA))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach {
                                it.consume()
                            }
                        }
                    }
                }.testTag(RESULTS_AD_SCRIM_TEST_TAG)
                .semantics { contentDescription = description },
    )
}

/**
 * Opacity of the quiescence dim.
 *
 * Deliberately mid-range: heavy enough that every control reads as unavailable at a glance
 * (so the dim itself carries D-08's "something is coming" message with no copy), but not
 * opaque — the celebration the player just earned stays visible behind it, and a screen that
 * went fully black for half a second would read as a crash rather than as a pause.
 *
 * An alpha `Float` as a private `const` is the shape [STAR_GLOW_ALPHA] already establishes in
 * `ResultsScreen.kt`: it is not a dimension, so it belongs here rather than in `dimens.xml`,
 * and it does not trip the `dpSpLiteralCheck` gate.
 */
private const val AD_SCRIM_ALPHA = 0.6f
