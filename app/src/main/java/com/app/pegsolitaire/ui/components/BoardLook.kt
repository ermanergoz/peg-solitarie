package com.app.pegsolitaire.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.ui.game.BoardMarkRecipe
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.game.SOLID_RING
import com.app.pegsolitaire.ui.game.recipe
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.app.pegsolitaire.ui.theme.LocalBoardMarks
import com.app.pegsolitaire.ui.theme.LocalClayColors

/**
 * What [BoardCanvas]'s draw phase reads: where everything is, and how it is coloured and stroked.
 * [recipe] is the one [geometry] was built with, so a frame drawn at a size the composition has not
 * seen yet builds its own geometry with the same mark radii.
 */
internal class BoardLook(
    val geometry: BoardGeometry,
    val palette: BoardPalette,
    val recipe: BoardMarkRecipe,
)

/**
 * The board's geometry and palette for one composition. Colors and stroke widths are resolved
 * here because DrawScope can read neither `MaterialTheme` nor resources; dimens are converted
 * dp → px (DrawScope draws in pixels), and every stroke the palette carries is built once per
 * geometry. The draw lambda rebuilds none of them on a frame the composition sized; on one it has
 * not, it rebuilds only the dashed hint rings' strokes, for that frame's geometry ([hintRingStroke]).
 *
 * The palette is read on the interactive path only (docs/ux/colour-vision.md §3.3): a read-only
 * board draws pegs and holes, which no palette touches, so it reads no `LocalBoardMarks` and takes
 * the standard recipe's geometry — Home's boards are the same pixels with the option on or off.
 */
@Composable
internal fun rememberBoardLook(
    spec: BoardSpec,
    canvasSize: Size,
    interactive: Boolean,
): BoardLook {
    val marks = if (interactive) LocalBoardMarks.current else null
    val recipe = marks?.palette?.recipe ?: BoardMarkPalette.Standard.recipe
    val geometry = remember(spec, canvasSize, recipe) { BoardGeometry.forCanvas(spec, canvasSize, recipe) }
    val density = LocalDensity.current
    val clay = LocalClayColors.current
    val markWidthPx = with(density) { dimensionResource(R.dimen.wrong_tap_mark_width).toPx() }
    val ringWidthPx = with(density) { dimensionResource(R.dimen.peg_selected_ring_width).toPx() }
    val haloWidthPx = with(density) { dimensionResource(R.dimen.peg_selection_halo_width).toPx() }
    val hintSourceStroke =
        remember(geometry, recipe, ringWidthPx) {
            hintRingStroke(geometry.hintSourceRingRadius, ringWidthPx, recipe.hintRingDashes)
        }
    val hintTargetStroke =
        remember(geometry, recipe, ringWidthPx) {
            hintRingStroke(geometry.hintTargetRingRadius, ringWidthPx, recipe.hintRingDashes)
        }
    val haloStroke = remember(haloWidthPx) { Stroke(width = haloWidthPx) }
    val markStroke =
        remember(geometry.cellSize, markWidthPx) {
            wrongTapMarkStroke(
                radius = BoardMetrics.WRONG_TAP_MARK_RING_RATIO * geometry.cellSize,
                widthPx = markWidthPx,
            )
        }
    val palette =
        BoardPalette(
            pegGradient = clay.pegGradient,
            pegHighlight = clay.pegHighlight,
            dimpleFill = clay.dimpleFill,
            dimpleShadow = clay.dimpleShadow,
            accent = MaterialTheme.colorScheme.primary,
            hint = marks?.hintRing ?: MaterialTheme.colorScheme.tertiary,
            ringWidthPx = ringWidthPx,
            mark = MaterialTheme.colorScheme.onSurfaceVariant,
            markStroke = markStroke,
            selectionHalo = marks?.selectionHalo,
            haloWidthPx = haloWidthPx,
            haloStroke = haloStroke,
            destinationDotAlpha = recipe.destinationDotAlpha,
            hintRingAlpha = recipe.hintRingAlpha,
            hintConnectorAlpha = recipe.hintConnectorAlpha,
            hintRingsDashed = recipe.hintRingDashes != SOLID_RING,
            hintSourceStroke = hintSourceStroke,
            hintTargetStroke = hintTargetStroke,
        )
    return BoardLook(geometry, palette, recipe)
}

/**
 * Resolved theme + clay colors and pixel stroke widths handed to the draw phase, plus the wrong
 * tap's mark: `onSurfaceVariant` (§3.2) and its dashed stroke; and the palette's marks
 * (docs/ux/colour-vision.md §3.4): the halo inside the selection ring when the palette has one,
 * the legal-target and hint alphas, and the hint rings' strokes, dashed or solid.
 */
internal class BoardPalette(
    val pegGradient: List<Color>,
    val pegHighlight: Color,
    val dimpleFill: Color,
    val dimpleShadow: Color,
    val accent: Color,
    val hint: Color,
    val ringWidthPx: Float,
    val mark: Color,
    val markStroke: Stroke,
    val selectionHalo: Color?,
    val haloWidthPx: Float,
    val haloStroke: Stroke,
    val destinationDotAlpha: Float,
    val hintRingAlpha: Float,
    val hintConnectorAlpha: Float,
    val hintRingsDashed: Boolean,
    val hintSourceStroke: Stroke,
    val hintTargetStroke: Stroke,
)

/**
 * One hint ring of [radius] around [center]. A dashed ring is a full-turn arc that starts at the
 * ring's topmost point, so its fixed pattern starts there (colour-vision §3.4) — a circle's own path
 * starts at three o'clock; a solid ring is today's circle, so the standard palette draws exactly the
 * pixels it drew before the option existed (CV-13).
 */
internal fun DrawScope.drawHintRing(
    color: Color,
    center: Offset,
    radius: Float,
    stroke: Stroke,
    dashed: Boolean,
) {
    if (dashed) {
        drawArc(
            color = color,
            startAngle = RING_START_ANGLE_DEG,
            sweepAngle = FULL_SWEEP_DEG,
            useCenter = false,
            topLeft = center - Offset(radius, radius),
            size = Size(radius * 2, radius * 2),
            style = stroke,
        )
    } else {
        drawCircle(color = color, radius = radius, center = center, style = stroke)
    }
}

/**
 * A hint ring's stroke: solid for [SOLID_RING], else [dashes] dashes around a ring of [radius]. The
 * one builder of both hint strokes: [rememberBoardLook] builds them for the composed geometry, and
 * `BoardCanvas` rebuilds the dashed ones for a frame the composition has not sized, since a dash
 * pattern is cut for one radius (colour-vision §3.2, amended 2026-11-01).
 */
internal fun hintRingStroke(
    radius: Float,
    widthPx: Float,
    dashes: Int,
): Stroke = if (dashes == SOLID_RING) Stroke(width = widthPx) else dashedRingStroke(radius, widthPx, dashes)

/** A dashed hint ring starts at its topmost point: −90° from three o'clock. */
private const val RING_START_ANGLE_DEG = -90f

/** A dashed hint ring is one full turn. */
private const val FULL_SWEEP_DEG = 360f
