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
 * The "Show possible moves" toggle — one of [GameActionDock]'s two **quiet** controls (D-04b).
 *
 * A stateless [IconButton] on a [DockActionChip] that is outlined when the dots are OFF and
 * **filled** when they are ON, showing the local [ShowMovesIcon] at `game_action_glyph_size`.
 * It stays a pure render gate for the dots and is deliberately distinct from the separate
 * winning-move [HintControl] (D-02); the persisted flip lives in the ViewModel.
 *
 * ## Two fixes, not a reskin (criterion 20, criterion 21)
 *
 * **The glyph.** It was the core set's information glyph — the same drawing Home used for its
 * help button, so one picture meant two unrelated things in one app. [ShowMovesIcon] is a peg
 * with two trailing
 * destination dots: literally what the feature draws.
 *
 * **The state channel.** Until Phase 15 this control carried its whole ON/OFF state in tint —
 * `primary` versus `onSurfaceVariant` — which breaks rules/compose.md's "never colour alone" and
 * is invisible to a colour-blind player and in a screenshot. The fill is now the primary channel,
 * the tint the second and `show_moves_on`/`show_moves_off` the third, and the fill is reported
 * through [DockActionFilledKey] so a regression fails a test rather than a review.
 *
 * It is **silent** (D-09): a toggle is not a navigational CTA, and `show_moves_on` must stay its
 * ON description — `ButtonSoundTest.kt:226-244` locates it by that string.
 *
 * @param showHints whether hints are currently on (drives fill, tint and content description).
 * @param onToggleHints invoked when the player taps the toggle.
 * @param modifier layout modifier for the icon button.
 */
@Composable
fun HintToggleControl(
    showHints: Boolean,
    onToggleHints: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DockActionChip(filled = showHints) {
        IconButton(
            onClick = onToggleHints,
            modifier = modifier.claySquish().size(dimensionResource(R.dimen.game_action_button_size)),
        ) {
            Icon(
                imageVector = ShowMovesIcon,
                contentDescription =
                    stringResource(if (showHints) R.string.show_moves_on else R.string.show_moves_off),
                tint =
                    if (showHints) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
            )
        }
    }
}
