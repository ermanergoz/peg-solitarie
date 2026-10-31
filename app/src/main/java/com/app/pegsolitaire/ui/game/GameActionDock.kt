package com.app.pegsolitaire.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.FinishedContainer
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.finishGlow
import com.app.pegsolitaire.ui.theme.glossFinish
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled

/** Semantics tag the tests use to locate the action dock — never its copy (ux-spec §13). */
const val GAME_ACTION_DOCK_TEST_TAG = "game_action_dock"

/**
 * The gameplay action dock (ASST-01, ASST-02, ux-spec §4.4, D-04/D-04a-e) — one clay slab below
 * the board holding all four in-game controls, in the thumb zone.
 *
 * ## Why it is down here
 *
 * Undo is the control a peg-solitaire player uses most and it sat in the worst-reachable corner
 * of the screen. rules/compose.md requires primary actions in the lower thumb zone of a
 * 6.7-inch phone; **Q-902 is answered as "the split"** — read-only status stays up in
 * [GameStatusStrip], the four controls come down here.
 *
 * ## One look, one meaning (D-04e, §4.4.1)
 *
 * The dock holds two kinds of control and draws them in two grammars. Undo, Hint and Restart are
 * **actions**: one pale raised disc when they can be pressed, and only ever Available, Working or
 * Unavailable. Show-moves is the one **toggle**, and the only control ever drawn On or Off. Every
 * look is a [DockLookRecipe], and [DockActionChip] is the one place a recipe becomes pixels.
 *
 * This replaces D-04c's loud/quiet split, which drew Undo and Hint filled and Show-moves and
 * Restart outlined *by importance* — the on/off vocabulary — so the founder read two controls on
 * and two off while three of the four have no on/off at all.
 *
 * ## The navigation bar is not applied here (D-05d)
 *
 * The gameplay `Scaffold`'s content padding already holds the navigation bar, and `GameScreen`
 * applies it once to the column this dock closes. The dock used to add `navigationBarsPadding()`
 * on top, riding a second navigation inset — which, with the strip's matching second status
 * inset, took 72dp from the board on a 360x640dp phone (§4.3.1).
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
 * @param showHints whether the legal-destination dots are on (the toggle's On or Off).
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
    val slab = MaterialTheme.colorScheme.surfaceContainerHigh
    Surface(
        shape = MaterialTheme.shapes.large,
        // A rim, a lip and the glow; no gloss, so the fill around the discs stays the flat surface 21e
        // and 21f measure them against (candy-backdrop.md §3.5.1).
        color = Color.Transparent,
        contentColor = contentColorFor(slab),
        modifier =
            modifier
                .finishGlow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .glossFinish(FinishedContainer.DockSlab, MaterialTheme.shapes.large, fill = slab)
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
            // Order is §4.4's and is not arbitrary: Undo and Hint first, so the pair the player
            // reaches for most sits under the thumb's natural arc.
            UndoControl(canUndo = canUndo, onUndo = onUndo)
            HintControl(solverThinking = solverThinking, onHint = onHint)
            HintToggleControl(showHints = showHints, onToggleHints = onToggleHints)
            RestartControl(onRestart = onRestart)
        }
    }
}

/**
 * The shared backing of one dock control: a circular clay disc drawn from [look]'s recipe — its
 * fill, its ring, its shadow, its on mark and whether it squishes under a press. (The glyph is
 * the control's own, drawn from the same recipe by the control.)
 *
 * One composable rather than four copies, because the recipe *is* the accessibility contract
 * (§11 "State, not just colour"): four copies of a contract are four places for it to drift, and
 * [DockLookRecipeTest] can only vouch for a dock that draws from the recipe and nothing else.
 *
 * Every change of look cross-fades over `ClayAnim.PRESS_IN_MS` — except a change **to**
 * [DockLook.Working], which is instant because it confirms the tap registered — and with OS
 * animations off every look is simply the new one (§10).
 *
 * @param look the look to draw; the control reports the same look on its own node.
 * @param content the control's own button or switch.
 */
@Composable
internal fun DockActionChip(
    look: DockLook,
    content: @Composable () -> Unit,
) {
    val recipe = look.recipe
    val animate = rememberAnimationsEnabled() && look != DockLook.Working
    val fill by animateColorAsState(dockFillColor(recipe.fill), dockSpec(animate), label = "dockFill")
    val ring by animateColorAsState(dockRingColor(recipe.ring), dockSpec(animate), label = "dockRing")
    val lift by animateFloatAsState(if (recipe.shadow) RAISED else FLAT, dockSpec(animate), label = "dockLift")
    Surface(
        shape = CircleShape,
        color = fill,
        border = BorderStroke(dimensionResource(R.dimen.game_action_ring_width), ring),
        modifier =
            Modifier
                .then(if (recipe.respondsToPress) Modifier.claySquish() else Modifier)
                .onMark(showing = recipe.onMark)
                .clayShadow(dimensionResource(R.dimen.elevation_pressed) * lift, CircleShape),
        content = content,
    )
}

/**
 * The colour a dock glyph is drawn in for [look], cross-faded like the disc behind it — so an
 * action becoming Unavailable fades its glyph with its fill and its shadow, in one beat.
 *
 * Derived from the recipe, never chosen per control: a pale disc carries `onPrimaryContainer`, the
 * strong disc `onPrimary`, an empty ring `onSurfaceVariant`, and a faded glyph `onSurface` at
 * [DockMetrics.UNAVAILABLE_GLYPH_ALPHA].
 */
@Composable
internal fun animatedDockGlyphColor(look: DockLook): Color {
    val animate = rememberAnimationsEnabled() && look != DockLook.Working
    val color by animateColorAsState(dockGlyphColor(look), dockSpec(animate), label = "dockGlyph")
    return color
}

/** [animatedDockGlyphColor]'s target — the resting glyph colour of [look]. */
@Composable
internal fun dockGlyphColor(look: DockLook): Color {
    val recipe = look.recipe
    val scheme = MaterialTheme.colorScheme
    return when {
        recipe.faded -> scheme.onSurface.copy(alpha = DockMetrics.UNAVAILABLE_GLYPH_ALPHA)
        recipe.fill == DockFill.Strong -> scheme.onPrimary
        recipe.fill == DockFill.Pale -> scheme.onPrimaryContainer
        else -> scheme.onSurfaceVariant
    }
}

/**
 * The disc colour of a fill. [DockFill.None] is the slab's own colour at zero alpha rather than
 * `Color.Transparent`, so a disc fading out fades into the slab instead of through black.
 */
@Composable
private fun dockFillColor(fill: DockFill): Color {
    val scheme = MaterialTheme.colorScheme
    return when (fill) {
        DockFill.None -> scheme.surfaceContainerHigh.copy(alpha = TRANSPARENT)
        DockFill.Pale -> scheme.primaryContainer
        DockFill.Strong -> scheme.primary
    }
}

/** The ring colour: `onSurfaceVariant` when the recipe draws a ring, the same at zero alpha when not. */
@Composable
private fun dockRingColor(ring: Boolean): Color {
    val ringColor = MaterialTheme.colorScheme.onSurfaceVariant
    return if (ring) ringColor else ringColor.copy(alpha = TRANSPARENT)
}

/** The dock's one transition: a press-in-length cross-fade, or none at all. */
private fun <T> dockSpec(animate: Boolean): AnimationSpec<T> = if (animate) tween(ClayAnim.PRESS_IN_MS) else snap()

/**
 * The dock slab as the previews of each control draw it: the controls in a row on
 * `surfaceContainerHigh`, so a look is judged against the surface it really sits on.
 */
@Composable
internal fun DockPreviewSlab(content: @Composable RowScope.() -> Unit) {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_md)),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** A disc with no shadow, and a disc raised on its full `elevation_pressed`. */
private const val FLAT = 0f
private const val RAISED = 1f

/** A colour's alpha when the recipe does not draw it: fully transparent. */
private const val TRANSPARENT = 0f
