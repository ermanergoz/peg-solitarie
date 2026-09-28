package com.app.pegsolitaire.ui.game

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R

/**
 * The restart control (D-06) — the last of [GameActionDock]'s three actions.
 *
 * A stateless [IconButton] on a [DockActionChip], showing [Icons.Rounded.Refresh] at
 * `game_action_glyph_size`, emitting [onRestart] on tap. It carries no logic — the mid-game
 * confirm vs. immediate-reset decision lives in the ViewModel; this leaf only forwards the intent.
 * The `restart_board` content description keeps the icon-only control accessible.
 *
 * ## Always Available (D-04e)
 *
 * Restart has one look: [DockLook.Available], the same pale raised disc as Undo and Hint. It used
 * to be outlined so the dock would not invite a careless tap, but an outline is the "off" half of
 * the on/off vocabulary, and a restart is not a setting that is off. Its protection is where it
 * always was: the mid-game confirm dialog (D-06). It is also **silent** (D-09) — that dialog is
 * where the press cues live.
 *
 * @param onRestart invoked when the player taps the restart icon.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun RestartControl(
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = DockLook.Available
    DockActionChip(look = look) {
        IconButton(
            onClick = onRestart,
            modifier =
                modifier
                    .size(dimensionResource(R.dimen.game_action_button_size))
                    .dockLook(look),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.restart_board),
                tint = animatedDockGlyphColor(look),
                modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
            )
        }
    }
}

/** Restart's one look on the dock slab, in both themes. */
@Preview(name = "Restart · light")
@Preview(name = "Restart · dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RestartControlPreview() {
    DockPreviewSlab {
        RestartControl(onRestart = {})
    }
}
