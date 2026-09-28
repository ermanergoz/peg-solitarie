package com.app.pegsolitaire.ui.game

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.claySquish

/**
 * The restart control (D-06) — [GameActionDock]'s other **quiet** action.
 *
 * A stateless [IconButton] on an outlined [DockActionChip], showing [Icons.Rounded.Refresh] at
 * `game_action_glyph_size` and tinted `onSurfaceVariant`, emitting [onRestart] on tap. It carries
 * no logic — the mid-game confirm vs. immediate-reset decision lives in the ViewModel; this leaf
 * only forwards the intent. The `restart_board` content description keeps the icon-only control
 * accessible.
 *
 * Quiet rather than loud, deliberately: throwing the board away is not something the dock should
 * invite, and the outline puts it visually beside the toggle rather than beside Undo and Hint. It
 * is also **silent** (D-09) — the confirm dialog it raises is where the press cues live.
 *
 * @param onRestart invoked when the player taps the restart icon.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun RestartControl(
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DockActionChip(filled = false) {
        IconButton(
            onClick = onRestart,
            modifier = modifier.claySquish().size(dimensionResource(R.dimen.game_action_button_size)),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.restart_board),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
            )
        }
    }
}
