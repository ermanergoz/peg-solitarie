package com.app.pegsolitaire.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the **on mark** over a dock disc while [showing] (ux-spec §4.4.1, D-04e): a
 * `game_action_mark_size` circle filled `onPrimary`, bordered `primary` at
 * `game_action_ring_width`, holding `Icons.Rounded.Check` in `primary` at
 * `game_action_mark_glyph_size`, centred on the disc's rim at its top-end 45° point.
 *
 * Only [DockLook.On] shows it, and only Show-moves ever reaches On, so it marks a **setting**,
 * never a count: it is a state mark and nothing about it is an assist-allowance surface. That is
 * also why it is called the on mark (or check mark) everywhere — never by the word the dock's
 * allowance-surface grep forbids — so that grep stays a meaningful zero (§4.4.1, plan §10 P10).
 *
 * It is **drawn, not composed**: no node, no semantics, nothing for TalkBack to reach — the
 * switch's own node already announces "On". It is drawn after the disc and outside the disc's
 * clip, which is why this sits on the modifier *around* the disc's `Surface`; centred on the rim,
 * it overhangs the disc and stays inside the 72dp slab.
 *
 * It scales in with `ClayAnim.overshootSpring()` and out without overshoot, over
 * `ClayAnim.PRESS_IN_MS`; with OS animations off it simply appears or goes (§10). The scale is
 * read in the draw phase, so an animating mark redraws without recomposing anything.
 */
@Composable
internal fun Modifier.onMark(showing: Boolean): Modifier {
    val animationsEnabled = rememberAnimationsEnabled()
    val scale by animateFloatAsState(
        targetValue = if (showing) SHOWN else HIDDEN,
        animationSpec =
            when {
                !animationsEnabled -> snap()
                showing -> ClayAnim.overshootSpring()
                else -> tween(ClayAnim.PRESS_IN_MS)
            },
        label = "dockOnMark",
    )
    val check = rememberVectorPainter(Icons.Rounded.Check)
    val style =
        OnMarkStyle(
            fill = MaterialTheme.colorScheme.onPrimary,
            stroke = MaterialTheme.colorScheme.primary,
            size = dimensionResource(R.dimen.game_action_mark_size),
            glyphSize = dimensionResource(R.dimen.game_action_mark_glyph_size),
            borderWidth = dimensionResource(R.dimen.game_action_ring_width),
        )
    return drawWithContent {
        drawContent()
        val current = scale
        if (current > HIDDEN) drawOnMark(current, style, check)
    }
}

/** The on mark's colours and sizes, resolved in composition because a `DrawScope` cannot read them. */
@Immutable
private data class OnMarkStyle(
    val fill: Color,
    val stroke: Color,
    val size: Dp,
    val glyphSize: Dp,
    val borderWidth: Dp,
)

/** Draws the mark at [scale] on the rim of the disc this scope's size describes. */
private fun DrawScope.drawOnMark(
    scale: Float,
    style: OnMarkStyle,
    check: VectorPainter,
) {
    val discRadius = size.minDimension / 2
    val angle = Math.toRadians(DockMetrics.ON_MARK_RIM_ANGLE_DEGREES)
    val across = (discRadius * cos(angle)).toFloat()
    val up = (discRadius * sin(angle)).toFloat()
    val towardEnd = if (layoutDirection == LayoutDirection.Ltr) across else -across
    val markCentre = center + Offset(towardEnd, -up)
    val markRadius = style.size.toPx() / 2
    val border = style.borderWidth.toPx()
    val glyph = style.glyphSize.toPx()
    scale(scale = scale, pivot = markCentre) {
        drawCircle(color = style.fill, radius = markRadius, center = markCentre)
        drawCircle(
            color = style.stroke,
            radius = markRadius - border / 2,
            center = markCentre,
            style = Stroke(width = border),
        )
        translate(left = markCentre.x - glyph / 2, top = markCentre.y - glyph / 2) {
            with(check) { draw(size = Size(glyph, glyph), colorFilter = ColorFilter.tint(style.stroke)) }
        }
    }
}

/** The mark's scale when gone, and when fully drawn. */
private const val HIDDEN = 0f
private const val SHOWN = 1f
