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

/**
 * The persistent top-bar restart control (D-06).
 *
 * A stateless `TopAppBar` action: an [IconButton] showing [Icons.Rounded.Refresh]
 * sized to `top_bar_icon_size` and tinted `onSurface`, emitting [onRestart] on tap.
 * It carries no logic — the mid-game confirm vs. immediate-reset decision lives in
 * the ViewModel; this leaf only forwards the intent. The content description is the
 * `restart_board` copy so the icon-only control is accessible.
 *
 * @param onRestart invoked when the player taps the restart icon.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun RestartControl(
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onRestart, modifier = modifier) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.restart_board),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
        )
    }
}
