package com.app.pegsolitaire.ui.game

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.scoring.ChallengeClearRule
import com.app.pegsolitaire.ui.theme.FinishedContainer
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.glossFinish

/** Test tag of the inline rescue banner so the screen-level Compose test can locate it. */
const val NOWIN_BANNER_TEST_TAG = "nowin_banner"

/**
 * The inline no-win / stuck rescue banner (ASST-03, D-06/D-08/D-09).
 *
 * A single shared, **non-blocking** M3 [Surface] (NOT an `AlertDialog`) rendered over the
 * still-visible, still-interactive board so the player can act immediately (D-08). It is
 * parameterized by [message] — the caller passes `nowin_message` when the Solver confirms
 * no winning move and `stuck_message` when the board has no legal move at all — so one
 * component serves both rescue surfaces (RESEARCH Open-Q2). `nowin_message` now means a
 * proven dead end and nothing else (GD-HT-1): a search that ends with no move and no proof
 * shows the Undecided notice, [HintUndecidedNotice], never this banner.
 *
 * Its message is a polite live region (D-04f, criterion 23k), so TalkBack speaks a proof, or a
 * stuck board, as it speaks the Undecided notice; the three actions stay outside the region and
 * are read when they are focused.
 *
 * It is a stateless leaf: it owns no logic and holds no state. It presents three
 * [TextButton] actions — Undo · Restart · Home (D-09) — and forwards each tap through the
 * hoisted [onUndo]/[onRestart]/[onHome] handlers. Undo backs the board out into a playable
 * position; Restart routes through the ViewModel's existing restart path (no second confirm
 * dialog is introduced); Home exits to the Home root.
 *
 * Colors are the soft M3 problem-surface roles (`errorContainer` background,
 * `onErrorContainer` text — non-alarming), the corner radius is `shapes.medium`, content
 * padding is `spacing_md`, the action row is spaced by `spacing_sm`, and the whole banner
 * keeps a `nowin_banner_min_height` minimum so its actions stay tap-reachable (UI-SPEC §4).
 *
 * **Putting it away (ux-spec D-SB-4; docs/gdd.md GD-SB-19).** With an [onDismiss], a tap on the
 * banner anywhere but its three buttons — the message, the padding, the gap — invokes it, and the
 * message node offers TalkBack the same choice as a `dismiss` action, in the Undecided notice's
 * idiom (D-04g): no click action and no role, because the banner is a message, not a control. The
 * buttons keep their own taps. Without one (the default) neither exists, which is every banner but
 * Challenge's proven-dead one ([RescueBanner] decides).
 *
 * @param message the resolved rescue copy to show (no-win vs. stuck; hoisted by the caller).
 * @param onUndo backs the board out of the rescue into a playable position and clears it.
 * @param onRestart restarts the board (routed through the existing restart confirmation).
 * @param onHome leaves to the Home root.
 * @param modifier layout modifier for the banner surface.
 * @param onDismiss puts the banner away, or `null` for a banner that cannot be put away.
 */
@Composable
fun NoWinBanner(
    message: String,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val banner = MaterialTheme.colorScheme.errorContainer
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val dismissable = onDismiss != null
    // Remembered, so the message leaf skips recomposition; it reads the latest callback when invoked.
    val dismissMessage: (() -> Unit)? =
        remember(dismissable) { if (dismissable) ({ currentOnDismiss?.invoke() }) else null }
    Surface(
        // Its clay shadow stays; the finish adds the rim and the lip that give its edge the B-14a floor.
        color = Color.Transparent,
        contentColor = contentColorFor(banner),
        shape = MaterialTheme.shapes.medium,
        modifier =
            modifier
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.medium)
                .glossFinish(FinishedContainer.RescueBanner, MaterialTheme.shapes.medium, fill = banner)
                .heightIn(min = dimensionResource(R.dimen.nowin_banner_min_height))
                .then(
                    if (dismissable) {
                        // The buttons consume their own taps, so only a tap outside them gets here.
                        Modifier.pointerInput(Unit) { detectTapGestures(onTap = { currentOnDismiss?.invoke() }) }
                    } else {
                        Modifier
                    },
                ).testTag(NOWIN_BANNER_TEST_TAG),
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_md))) {
            RescueMessage(message = message, onDismiss = dismissMessage)
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
            ) {
                RescueAction(label = R.string.undo_action, onClick = onUndo)
                RescueAction(label = R.string.rescue_restart, onClick = onRestart)
                RescueAction(label = R.string.action_home, onClick = onHome)
            }
        }
    }
}

/**
 * The banner's message: a polite live region (D-04f, criterion 23k), and with an [onDismiss] the
 * TalkBack `dismiss` action that puts the banner away — no click action and no role (D-SB-4).
 */
@Composable
private fun RescueMessage(
    message: String,
    onDismiss: (() -> Unit)?,
) {
    Text(
        text = message,
        modifier =
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (onDismiss != null) {
                    dismiss {
                        onDismiss()
                        true
                    }
                }
            },
        color = MaterialTheme.colorScheme.onErrorContainer,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/** One of the banner's three actions (D-09): a clay text button, read when it is focused. */
@Composable
private fun RescueAction(
    @StringRes label: Int,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.claySquish(),
    ) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * The rescue banner for [state] in its mode: which of the four messages it says, and whether it can
 * be put away (ux-spec D-SB-3/D-SB-4, §4.4.3; docs/gdd.md R6, GD-SB-18/19). `GameScreen` composes it
 * whenever [GameUiState.showRescueBanner]; the copy stays in `strings.xml` and every number in it is
 * a format argument from the clear rule.
 *
 * - **Challenge** ([isChallenge]): a stuck board says how many pegs it was left with and that
 *   [ChallengeClearRule.CLEAR_MAX_PEGS] or fewer clears the level; a Hint's proof says one peg is out
 *   of reach *from here* and that the level may still be cleared with that many or fewer — never
 *   that it is lost. The stuck message wins when both flags are up (a Hint asked on a dead board):
 *   there is nothing left to clear with. Only the proven-dead banner of a still-playable board can
 *   be put away, through [onDismissProvenDead].
 * - **Classic** keeps its two messages, in their shipped precedence, and no put-away (R8).
 */
@Composable
internal fun RescueBanner(
    state: GameUiState,
    isChallenge: Boolean,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    onDismissProvenDead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clearMaxPegs = ChallengeClearRule.CLEAR_MAX_PEGS
    val message =
        when {
            !isChallenge && state.noWinningMove -> stringResource(R.string.nowin_message)
            !isChallenge -> stringResource(R.string.stuck_message)
            state.stuck ->
                pluralStringResource(
                    R.plurals.stuck_message_challenge,
                    state.board.pegCount,
                    state.board.pegCount,
                    clearMaxPegs,
                )
            else -> pluralStringResource(R.plurals.nowin_message_challenge, clearMaxPegs, clearMaxPegs)
        }
    NoWinBanner(
        message = message,
        onUndo = onUndo,
        onRestart = onRestart,
        onHome = onHome,
        modifier = modifier,
        onDismiss = if (isChallenge && state.noWinningMove && !state.stuck) onDismissProvenDead else null,
    )
}
