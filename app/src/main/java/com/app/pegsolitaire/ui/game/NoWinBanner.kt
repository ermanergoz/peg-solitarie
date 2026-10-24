package com.app.pegsolitaire.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish

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
 * @param message the resolved rescue copy to show (no-win vs. stuck; hoisted by the caller).
 * @param onUndo backs the board out of the rescue into a playable position and clears it.
 * @param onRestart restarts the board (routed through the existing restart confirmation).
 * @param onHome leaves to the Home root.
 * @param modifier layout modifier for the banner surface.
 */
@Composable
fun NoWinBanner(
    message: String,
    onUndo: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier =
            modifier
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.medium)
                .heightIn(min = dimensionResource(R.dimen.nowin_banner_min_height))
                .testTag(NOWIN_BANNER_TEST_TAG),
    ) {
        Column(modifier = Modifier.padding(dimensionResource(R.dimen.spacing_md))) {
            Text(
                text = message,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
            ) {
                TextButton(
                    onClick = onUndo,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.claySquish(),
                ) {
                    Text(
                        text = stringResource(R.string.undo_action),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(
                    onClick = onRestart,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.claySquish(),
                ) {
                    Text(
                        text = stringResource(R.string.rescue_restart),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(
                    onClick = onHome,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.claySquish(),
                ) {
                    Text(
                        text = stringResource(R.string.action_home),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
