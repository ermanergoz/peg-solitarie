package com.app.pegsolitaire.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.backdrop.BackdropMetrics

/**
 * The finish's glow (backdrop spec §3.5.1, the tech-lead's A3): `clayShadow`'s geometry and
 * elevation, unchanged, with [tint] as its ambient and spot colour in place of the clay shadow's —
 * pink in light, violet in dark, or a gel tile's own glow. Used only where the recipe marks a glow;
 * `clayShadow` and its other callers are untouched.
 */
@Composable
fun Modifier.finishGlow(
    elevation: Dp,
    shape: Shape,
    tint: Color = LocalFinishColors.current.panelGlow,
): Modifier = shadow(elevation = elevation, shape = shape, ambientColor = tint, spotColor = tint)

/**
 * The gloss finish drawn as part of a container's own fill (backdrop spec §3.5.1, A3): its
 * [fill] when given, the white gloss fading from the top edge when [container]'s recipe has one,
 * the 1dp rim whose outer edge is the container's edge, and the 2dp lip inside its top — in that
 * order, behind the content. Everything is built once per size, direction and theme.
 *
 * Placement is the one binding rule: after the container's fill and before its content. On a `Box`
 * with `.background(fill, shape)`, put this right after that `.background` and pass no [fill]. On an
 * M3 `Surface` or `Card`, give it a transparent colour and hand the token to [fill]. It adds no
 * semantics, size, padding, target or click.
 */
@Composable
fun Modifier.glossFinish(
    container: FinishedContainer,
    shape: Shape,
    fill: Color? = null,
): Modifier {
    val colors = LocalFinishColors.current
    val recipe = container.recipe
    val density = LocalDensity.current
    val rimWidth = with(density) { dimensionResource(R.dimen.finish_rim_width).toPx() }
    val lipHeight = with(density) { dimensionResource(R.dimen.finish_lip_height).toPx() }
    return drawWithCache {
        val body = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val inset = Size(size.width - rimWidth, size.height - rimWidth)
        val rim =
            Path().apply {
                addOutline(shape.createOutline(inset, layoutDirection, this@drawWithCache))
                translate(Offset(rimWidth / 2f, rimWidth / 2f))
            }
        val rimStroke = Stroke(width = rimWidth)
        val lowered = Path().apply { addPath(body, Offset(0f, lipHeight)) }
        val lip = Path().apply { op(body, lowered, PathOperation.Difference) }
        val gloss =
            Brush.verticalGradient(
                0f to colors.panelGloss,
                BackdropMetrics.FINISH_GLOSS_FADE to Color.Transparent,
                startY = 0f,
                endY = size.height,
            )
        onDrawBehind {
            if (fill != null) drawPath(body, fill)
            if (recipe.gloss) drawPath(body, gloss)
            if (recipe.rim) drawPath(rim, colors.panelRim, style = rimStroke)
            if (recipe.lip) drawPath(lip, colors.panelLip)
        }
    }
}
