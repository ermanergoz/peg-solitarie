package com.app.pegsolitaire.ui.game

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled

/**
 * The "Show possible moves" toggle — [GameActionDock]'s one **toggle**, and the only control in
 * the dock ever drawn on or off (D-04b, D-04e).
 *
 * A stateless 56dp switch on a [DockActionChip], showing the local [ShowMovesIcon] — or, while
 * Off, [ShowMovesOffIcon] — at `game_action_glyph_size`. It stays a pure render gate for the dots
 * and is deliberately distinct from the separate winning-move [HintControl] (D-02); the persisted
 * flip lives in the ViewModel.
 *
 * ## On and Off, and nothing that could be mistaken for them (D-04e, §4.4.1)
 *
 * [DockLook.On] is a strong `primary` disc with the on mark on its rim and the plain glyph;
 * [DockLook.Off] an empty ring with the slashed glyph. They differ in all five channels the dock
 * has, and no action ever wears any of the four that are theirs alone — strong fill, ring, on mark,
 * slash — so the pair can mean nothing but this control's state (criterion 21e).
 *
 * ## A switch with one name (criterion 21d)
 *
 * It speaks as a switch: [Role.Switch] with the state announced as On or Off, under one constant
 * name — `show_moves_on`, "Show possible moves" — in both states, which is how TalkBack reads
 * every switch in Settings. The old pattern swapped the whole description with the state, between
 * two imperatives that each named the *opposite* of what the next tap does, so a TalkBack user
 * heard "Hide possible moves" on a control whose tap shows them.
 *
 * The name sits on the switch's own node, not on the glyph, so the node TalkBack focuses carries
 * name, role and state together; the glyph is decoration and says nothing. The node is neither
 * merged away nor cleared (§11 "The unmerged tree is not the contract").
 *
 * It is **silent** (D-09): a toggle is not a navigational CTA, and `ButtonSoundTest.kt:226-244`
 * finds it by `show_moves_on`, which is now its name in both states.
 *
 * @param showHints whether the dots are on (drives the look and the announced state).
 * @param onToggleHints invoked when the player flips the switch.
 * @param modifier layout modifier for the switch's target.
 */
@Composable
fun HintToggleControl(
    showHints: Boolean,
    onToggleHints: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val look = if (showHints) DockLook.On else DockLook.Off
    val name = stringResource(R.string.show_moves_on)
    DockActionChip(look = look) {
        Box(
            modifier =
                modifier
                    .size(dimensionResource(R.dimen.game_action_button_size))
                    .toggleable(value = showHints, role = Role.Switch, onValueChange = { onToggleHints() })
                    .semantics { contentDescription = name }
                    .dockLook(look),
            contentAlignment = Alignment.Center,
        ) {
            // The glyph cross-fades to its other variant with the disc (§4.4.1 "Transitions"):
            // each side of the fade draws its own look's glyph in its own look's colour.
            Crossfade(
                targetState = look,
                animationSpec = if (rememberAnimationsEnabled()) tween(ClayAnim.PRESS_IN_MS) else snap(),
                label = "showMovesGlyph",
            ) { shown ->
                Icon(
                    imageVector = if (shown.recipe.glyph == DockGlyph.Slashed) ShowMovesOffIcon else ShowMovesIcon,
                    contentDescription = null,
                    tint = dockGlyphColor(shown),
                    modifier = Modifier.size(dimensionResource(R.dimen.game_action_glyph_size)),
                )
            }
        }
    }
}

/** Both of the toggle's looks — On, then Off — on the dock slab, in both themes. */
@Preview(name = "Show moves · light")
@Preview(name = "Show moves · dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HintToggleControlPreview() {
    DockPreviewSlab {
        HintToggleControl(showHints = true, onToggleHints = {})
        HintToggleControl(showHints = false, onToggleHints = {})
    }
}
