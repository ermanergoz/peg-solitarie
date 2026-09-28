package com.app.pegsolitaire.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.clayShadow

/** Semantics tag the tests use to locate the action dock — never its copy (ux-spec §13). */
const val GAME_ACTION_DOCK_TEST_TAG = "game_action_dock"

/**
 * Semantics: whether a dock action is drawn **filled** rather than outlined.
 *
 * It exists so "state by fill, not by tint alone" (D-04b, criterion 21) is a property the build
 * re-proves on every run instead of a claim only a screenshot could settle — the same
 * custom-semantics idiom `GameScreen`'s `PegCountKey` uses, and for the same reason: a fill is a
 * pixel fact that no ordinary assertion can reach.
 */
val DockActionFilledKey = SemanticsPropertyKey<Boolean>("DockActionFilled")

private var SemanticsPropertyReceiver.dockActionFilled by DockActionFilledKey

/**
 * The gameplay action dock (ASST-01, ASST-02, ux-spec §4.4, D-04/D-04a-d) — one clay slab below
 * the board holding all four in-game actions, in the thumb zone.
 *
 * ## Why it is down here
 *
 * Undo is the control a peg-solitaire player uses most and it sat in the worst-reachable corner
 * of the screen. rules/compose.md requires primary actions in the lower thumb zone of a
 * 6.7-inch phone; **Q-902 is answered as "the split"** — read-only status stays up in
 * [GameStatusStrip], the four actions come down here.
 *
 * ## Loud and quiet, and why the pairs are pairs
 *
 * Undo and Hint are filled `primaryContainer`: they *do* something to the board and are what the
 * player reaches for. Show-moves and Restart are outlined: a render gate and a reset are not
 * moves. Show-moves ON becomes filled, which is the third channel D-04b's fix needs — the toggle
 * used to carry its whole state in tint.
 *
 * ## Not a Material bar (D-05c)
 *
 * It is deliberately **not** a `BottomAppBar` or a `NavigationBar`. A `NavigationBar` in
 * particular would announce four in-game actions to TalkBack as navigation *destinations*, which
 * they are not — the player is not going anywhere.
 *
 * ## No allowance surface, ever (criterion 24)
 *
 * No badge, no "×3" chip, no ad glyph, no allowance-driven disable. That negative contract is
 * inherited verbatim from the ad-allowance ruling the code records at `GameUiState.kt:131-135`,
 * and this refresh does not relax it: the opt-in dialog stays the only signal the allowance
 * system exists. The dock renders **no text at all**, which is what makes the rule checkable.
 *
 * @param showHints whether the legal-destination dots are on (drives the toggle's fill).
 * @param canUndo whether there is a move to undo (ASST-02, criterion 22).
 * @param solverThinking whether the Solver is computing a hint (D-10, criterion 23).
 * @param onUndo undoes the last move.
 * @param onHint requests a winning-move hint.
 * @param onToggleHints flips the persisted show-moves setting.
 * @param onRestart restarts the board (the ViewModel decides confirm vs. immediate).
 * @param modifier layout modifier for the dock.
 */
@Composable
internal fun GameActionDock(
    showHints: Boolean,
    canUndo: Boolean,
    solverThinking: Boolean,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onToggleHints: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            modifier
                .navigationBarsPadding()
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .fillMaxWidth()
                .testTag(GAME_ACTION_DOCK_TEST_TAG),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimensionResource(R.dimen.game_action_dock_height))
                    .padding(horizontal = dimensionResource(R.dimen.spacing_sm)),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Order is §4.4's and is not arbitrary: the two loud actions first, so the pair the
            // player reaches for most sits under the thumb's natural arc.
            UndoControl(canUndo = canUndo, onUndo = onUndo)
            HintControl(solverThinking = solverThinking, onHint = onHint)
            HintToggleControl(showHints = showHints, onToggleHints = onToggleHints)
            RestartControl(onRestart = onRestart)
        }
    }
}

/**
 * The shared backing of one dock action: a circular clay chip that is either **filled** or
 * **outlined**, carrying [DockActionFilledKey] so the distinction is assertable.
 *
 * One composable rather than four copies, because the fill rule is the accessibility contract
 * (§11 "State, not just colour") and four copies of a contract is four places for it to drift.
 *
 * @param filled whether this action is one of the loud pair, or a toggle that is currently ON.
 * @param enabled whether the action can be taken. A **disabled but filled** action drops to
 *   `surfaceVariant` rather than becoming outlined: an outline would read as a *quiet* control
 *   instead of an unavailable one, and "disabled" is already announced by `enabled` itself.
 * @param content the action's own [androidx.compose.material3.IconButton].
 */
@Composable
internal fun DockActionChip(
    filled: Boolean,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color =
            when {
                !filled -> MaterialTheme.colorScheme.surface.copy(alpha = TRANSPARENT)
                enabled -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
        border =
            if (filled) {
                null
            } else {
                BorderStroke(
                    width = dimensionResource(R.dimen.peg_selected_ring_width),
                    color = MaterialTheme.colorScheme.outline,
                )
            },
        modifier = Modifier.semantics { dockActionFilled = filled },
        content = { content() },
    )
}

/** A quiet action's surface is fully transparent; the outline is what draws its edge. */
private const val TRANSPARENT = 0f
