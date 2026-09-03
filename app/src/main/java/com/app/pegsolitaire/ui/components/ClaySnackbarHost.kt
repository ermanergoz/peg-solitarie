package com.app.pegsolitaire.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.clayShadow

/**
 * The app's one clay-styled snackbar surface (ADS-03, D-11 / REL-05).
 *
 * A stateless leaf: it owns nothing, renders the hoisted [hostState] and has no branching
 * behaviour of its own.
 *
 * ## Why the styling is mandatory rather than optional
 *
 * The stock M3 [Snackbar] defaults its container to the M3 inverse-surface role — a dark
 * slab that is foreign to the Candy Clay palette and lands squarely on the design system's
 * "never pure-white/gray, never low energy" anti-pattern list. No `SnackbarHost` existed
 * anywhere in the app before this, so there was no inherited treatment to fall back on;
 * these tokens (`shapes.medium`, `surfaceContainerHighest`, `onSurface`, `spacing_md`
 * padding and a `clayShadow` at `elevation_card`) **are** the app's snackbar contract.
 * Dropping a bare `SnackbarHost` in beside this one would visibly break the palette.
 *
 * ## Why it is extracted, and why it carries no action
 *
 * Two screens need it — `GameScreen` shows the `ad_unavailable_*` message, `SettingsScreen`
 * the `settings_privacy_policy_error` one — and duplicating the styling in both is exactly
 * how the two would drift apart. It deliberately exposes **no action label**: by the time
 * either message shows, the resolution has already happened (D-11 refills the assist for
 * free before the message is raised), so there is nothing left for the player to do.
 * Messages therefore use `SnackbarDuration.Short`, set by the caller when it shows them.
 *
 * ## Why there is no test for this file
 *
 * Deliberate, not an omission: the host has no branching behaviour to assert — it is a
 * pure restyle of the M3 component. Both consuming screens assert that *their* message
 * actually reaches the player through their own screen-level tests, which is the claim that
 * can regress. A test here could only re-state the token list.
 *
 * @param hostState the snackbar queue, hoisted by the owning screen's `Scaffold`.
 * @param modifier layout modifier for the host.
 */
@Composable
fun ClaySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        val snackbarShape = MaterialTheme.shapes.medium
        Snackbar(
            snackbarData = data,
            shape = snackbarShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .padding(dimensionResource(R.dimen.spacing_md))
                    .clayShadow(dimensionResource(R.dimen.elevation_card), snackbarShape),
        )
    }
}
