package com.app.pegsolitaire.ui.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R

/**
 * The Undo action (ASST-02) — the first of [GameActionDock]'s three actions.
 *
 * A stateless [IconButton] on a [DockActionChip], showing the auto-mirrored [UndoIcon] at
 * `game_action_glyph_size` in a `game_action_button_size` target, emitting [onUndo] on tap. It
 * carries no logic — the replay lives in the ViewModel; this leaf only forwards the intent.
 *
 * ## Two looks, never on or off (D-04e, criterion 22)
 *
 * [DockLook.Available] while [canUndo], [DockLook.Unavailable] when there is nothing to undo — a
 * fresh board, undone back to the start, or a Challenge board still loading. Unavailable reaches
 * the player on four channels: `enabled` itself (which TalkBack announces as "disabled"), the disc
 * losing its fill, the disc losing its shadow, and the glyph fading. Never the tint alone.
 *
 * A spent free-undo allowance is **not** a third look: the control stays Available and the tap
 * raises the opt-in dialog (criterion 21c), because the dialog is the only signal that allowance
 * exists.
 *
 * ## Why it is here rather than in a bar
 *
 * Undo is the control a peg-solitaire player uses most, and until Phase 15 it sat in the top-right
 * corner — the worst-reachable spot on a 6.7-inch phone. D-04 is that fix, and the reason this
 * control's target is `game_action_button_size` (56dp, the largest in the app).
 *
 * @param canUndo whether there is a move to undo (drives the look and the enabled state).
 * @param onUndo invoked when the player taps the undo icon.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun UndoControl(
    canUndo: Boolean,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = if (canUndo) DockLook.Available else DockLook.Unavailable
    DockActionChip(look = look) {
        IconButton(
            onClick = onUndo,
            enabled = canUndo,
            modifier =
                modifier
                    .size(dimensionResource(R.dimen.game_action_button_size))
                    .dockLook(look),
        ) {
            Icon(
                imageVector = UndoIcon,
                contentDescription = stringResource(R.string.undo_action),
                tint = animatedDockGlyphColor(look),
                modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
            )
        }
    }
}

/** Both of Undo's looks — Available, then Unavailable — on the dock slab, in both themes. */
@Preview(name = "Undo · light")
@Preview(name = "Undo · dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun UndoControlPreview() {
    DockPreviewSlab {
        UndoControl(canUndo = true, onUndo = {})
        UndoControl(canUndo = false, onUndo = {})
    }
}
