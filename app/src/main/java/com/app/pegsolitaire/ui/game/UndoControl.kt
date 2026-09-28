package com.app.pegsolitaire.ui.game

import androidx.compose.foundation.layout.size
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
 * The Undo action (ASST-02) — one of [GameActionDock]'s two **loud** controls.
 *
 * A stateless [IconButton] on a filled [DockActionChip], showing the auto-mirrored [UndoIcon] at
 * `game_action_glyph_size` in a `game_action_button_size` target, emitting [onUndo] on tap. It
 * carries no logic — the replay lives in the ViewModel; this leaf only forwards the intent.
 *
 * ## Three channels for one state (criterion 22, §11)
 *
 * It is enabled only when [canUndo], and that fact reaches the player three ways: `enabled`
 * itself (which TalkBack announces as "disabled"), the chip's fill dropping from
 * `primaryContainer` to `surfaceVariant`, and the glyph tint. Never the tint alone.
 *
 * ## Why it is here rather than in a bar
 *
 * Undo is the control a peg-solitaire player uses most, and until Phase 15 it sat in the top-right
 * corner — the worst-reachable spot on a 6.7-inch phone. D-04 is that fix, and the reason this
 * control's target is `game_action_button_size` (56dp, the largest in the app) rather than the
 * top-bar token Phase 15 retires.
 *
 * @param canUndo whether there is a move to undo (drives enabled state, fill and tint).
 * @param onUndo invoked when the player taps the undo icon.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun UndoControl(
    canUndo: Boolean,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DockActionChip(filled = true, enabled = canUndo) {
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier = modifier.claySquish().size(dimensionResource(R.dimen.game_action_button_size)),
        ) {
            Icon(
                imageVector = UndoIcon,
                contentDescription = stringResource(R.string.undo_action),
                tint =
                    if (canUndo) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
            )
        }
    }
}
