package com.app.pegsolitaire.ui.game

import android.content.res.Configuration
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
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R

/**
 * The winning-move Hint action (ASST-01/ASST-03, D-01/D-10) — the second of [GameActionDock]'s
 * three actions.
 *
 * A stateless [IconButton] on a [DockActionChip], showing the local [HintIcon] lightbulb at
 * `game_action_glyph_size`, emitting [onHint] on tap.
 *
 * ## The glyph changed, and that is the point (D-04a, criterion 20)
 *
 * It was the core set's magnifying glass — the most conventional glyph in software for
 * find-by-typing, which is not what this control does. [HintIcon] is a lightbulb: an idea,
 * offered.
 *
 * ## Two looks, never on or off (D-04e)
 *
 * Hint is an *action*: [DockLook.Available] at rest and [DockLook.Working] while the Solver
 * searches, and nothing else. A hint showing on the board is **not** an "on" — the gold glow on
 * the board is the only sign one is up — and neither a dead position nor a spent free-hint
 * allowance changes the look: the rescue banner carries the first, the opt-in dialog the second.
 *
 * ## The working state (D-10, criterion 23)
 *
 * While [solverThinking] the glyph is replaced by a spinner in `primary`, the disc is pressed in,
 * and the button is **disabled**, which is what prevents a double-tap launching an overlapping
 * Solver search (T-04-3B-DT); the content description switches to `hint_thinking` so the wait is
 * announced rather than merely drawn. It never performs the jump (D-01): the ViewModel writes the
 * highlight and the player still moves.
 *
 * @param solverThinking whether the Solver is computing a hint (drives the Working look).
 * @param onHint invoked when the player taps the resting Hint icon.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun HintControl(
    solverThinking: Boolean,
    onHint: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = if (solverThinking) DockLook.Working else DockLook.Available
    DockActionChip(look = look) {
        if (look.recipe.glyph == DockGlyph.Spinner) {
            val thinkingDescription = stringResource(R.string.hint_thinking)
            // The description sits on the disabled button (not the decorative spinner) so the
            // whole control announces as one disabled unit — its own node carries both states.
            IconButton(
                onClick = onHint,
                enabled = false,
                modifier =
                    modifier
                        .size(dimensionResource(R.dimen.game_action_button_size))
                        .semantics { contentDescription = thinkingDescription }
                        .dockLook(look),
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
                )
            }
        } else {
            IconButton(
                onClick = onHint,
                modifier =
                    modifier
                        .size(dimensionResource(R.dimen.game_action_button_size))
                        .dockLook(look),
            ) {
                Icon(
                    imageVector = HintIcon,
                    contentDescription = stringResource(R.string.hint_action),
                    tint = animatedDockGlyphColor(look),
                    modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
                )
            }
        }
    }
}

/** Both of Hint's looks — Available, then Working — on the dock slab, in both themes. */
@Preview(name = "Hint · light")
@Preview(name = "Hint · dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HintControlPreview() {
    DockPreviewSlab {
        HintControl(solverThinking = false, onHint = {})
        HintControl(solverThinking = true, onHint = {})
    }
}
