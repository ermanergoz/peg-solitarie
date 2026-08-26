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
 * The top-bar Undo control (ASST-02).
 *
 * A stateless `TopAppBar` action mirroring [RestartControl]: an [IconButton] showing the
 * auto-mirrored [UndoIcon] glyph sized to `top_bar_icon_size`,
 * emitting [onUndo] on tap. It is enabled only when [canUndo] (there is at least one move
 * to undo); when disabled the tint drops to `onSurfaceVariant` (mirroring the
 * [HintToggleControl] off-state ternary) and taps are ignored. The `undo_action` content
 * description keeps the icon-only control accessible. It carries no logic — the replay
 * lives in the ViewModel; this leaf only forwards the intent.
 *
 * @param canUndo whether there is a move to undo (drives enabled state + tint).
 * @param onUndo invoked when the player taps the undo icon.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun UndoControl(
    canUndo: Boolean,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onUndo,
        enabled = canUndo,
        modifier = modifier.claySquish().size(dimensionResource(R.dimen.top_bar_touch_target)),
    ) {
        Icon(
            imageVector = UndoIcon,
            contentDescription = stringResource(R.string.undo_action),
            tint =
                if (canUndo) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
        )
    }
}
