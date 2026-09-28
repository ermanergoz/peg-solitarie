package com.app.pegsolitaire.ui.game

import androidx.compose.foundation.layout.size
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
 * The winning-move Hint action (ASST-01/ASST-03, D-01/D-10) — [GameActionDock]'s other **loud**
 * control.
 *
 * A stateless [IconButton] on a filled [DockActionChip], showing the local [HintIcon] lightbulb
 * at `game_action_glyph_size`, emitting [onHint] on tap.
 *
 * ## The glyph changed, and that is the point (D-04a, criterion 20)
 *
 * It was the core set's magnifying glass — the most conventional glyph in software for
 * find-by-typing, which is not what this control does. [HintIcon] is a lightbulb: an idea,
 * offered. It is an *action*, not a persisted toggle, so it keeps no ON/OFF treatment and stays
 * deliberately distinct from [HintToggleControl].
 *
 * ## The thinking state (D-10, criterion 23)
 *
 * While [solverThinking] the glyph is replaced by a spinner and the button is **disabled**, which
 * is what prevents a double-tap launching an overlapping Solver search (T-04-3B-DT); the content
 * description switches to `hint_thinking` so the wait is announced rather than merely drawn. It
 * never performs the jump (D-01): the ViewModel writes the highlight and the player still moves.
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
    DockActionChip(filled = true, enabled = !solverThinking) {
        if (solverThinking) {
            val thinkingDescription = stringResource(R.string.hint_thinking)
            // The description sits on the disabled button (not the decorative spinner) so the
            // whole control announces as one disabled unit — its own node carries both states.
            IconButton(
                onClick = onHint,
                enabled = false,
                modifier =
                    modifier
                        .size(dimensionResource(R.dimen.game_action_button_size))
                        .semantics { contentDescription = thinkingDescription },
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
                )
            }
        } else {
            IconButton(
                onClick = onHint,
                modifier = modifier.claySquish().size(dimensionResource(R.dimen.game_action_button_size)),
            ) {
                Icon(
                    imageVector = HintIcon,
                    contentDescription = stringResource(R.string.hint_action),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
                )
            }
        }
    }
}
