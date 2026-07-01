package com.app.pegsolitaire.ui.game

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.app.pegsolitaire.R

/**
 * The top-bar hint toggle (player control over the legal-destination dots).
 *
 * A stateless `TopAppBar` action mirroring [RestartControl]: an [IconButton] showing
 * [Icons.Rounded.Info] sized to `top_bar_icon_size`, emitting [onToggleHints] on tap.
 * Core icon set has no lightbulb/visibility glyph, so the on/off state is conveyed by
 * the tint — `primary` when hints are ON, `onSurfaceVariant` when OFF — and by the
 * content description (`hints_on` / `hints_off`) so the icon-only control is accessible.
 * It carries no logic; the persisted flip lives in the ViewModel.
 *
 * @param showHints whether hints are currently on (drives tint + content description).
 * @param onToggleHints invoked when the player taps the toggle.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun HintToggleControl(
    showHints: Boolean,
    onToggleHints: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onToggleHints, modifier = modifier) {
        Icon(
            imageVector = Icons.Rounded.Info,
            contentDescription =
                stringResource(if (showHints) R.string.hints_on else R.string.hints_off),
            tint =
                if (showHints) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
        )
    }
}
