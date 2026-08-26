package com.app.pegsolitaire.ui.game

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.claySquish

/**
 * The top-bar winning-move Hint action (ASST-01/ASST-03, D-01/D-10).
 *
 * A stateless `TopAppBar` action mirroring [RestartControl]: an [IconButton] showing the
 * core-set [Icons.Rounded.Search] glyph ("find the winning move") sized to
 * `top_bar_icon_size` and tinted `onSurface`, emitting [onHint] on tap. It is an action,
 * not a persisted toggle, so it does not use the `primary` ON/OFF tint pattern — it stays
 * neutral like [RestartControl] and is deliberately distinct from the [HintToggleControl].
 *
 * While [solverThinking] the icon is replaced by a `top_bar_icon_size`
 * [CircularProgressIndicator] and the button is **disabled** — the D-10 thinking state,
 * which also prevents a double-tap launching an overlapping Solver search (T-04-3B-DT); its
 * content description switches to `hint_thinking` so the wait is announced. It carries no
 * logic — the off-thread search + the highlight fields it writes live in the ViewModel; this
 * leaf only forwards the intent and reflects `solverThinking`. It never performs the jump
 * (D-01): the ViewModel writes `hintSource`/`hintTarget` for the board to highlight and the
 * player still makes the move.
 *
 * @param solverThinking whether the Solver is computing a hint (drives the spinner + disable).
 * @param onHint invoked when the player taps the resting Hint icon.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun HintControl(
    solverThinking: Boolean,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (solverThinking) {
        val thinkingDescription = stringResource(R.string.hint_thinking)
        // The description sits on the disabled button (not the decorative spinner) so the whole
        // control announces as one disabled unit — its own semantics node carries both states.
        IconButton(
            onClick = onHint,
            enabled = false,
            modifier =
                modifier
                    .size(dimensionResource(R.dimen.top_bar_touch_target))
                    .semantics { contentDescription = thinkingDescription },
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
            )
        }
    } else {
        IconButton(
            onClick = onHint,
            modifier = modifier.claySquish().size(dimensionResource(R.dimen.top_bar_touch_target)),
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(R.string.hint_action),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
            )
        }
    }
}
