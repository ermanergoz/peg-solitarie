package com.app.pegsolitaire.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.LayoutDirection
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.backdrop.BackdropMetrics
import com.app.pegsolitaire.ui.backdrop.LocalBackdropClock
import com.app.pegsolitaire.ui.backdrop.SHEEN_HIDDEN
import com.app.pegsolitaire.ui.backdrop.gelSheenProgress
import com.app.pegsolitaire.ui.theme.GelColors
import kotlin.math.abs
import kotlin.math.tan

/**
 * A mode tile's gel (backdrop spec §3.5.2, B-18, the tech-lead's A4): the vertical fill — light
 * at the top edge, [GelColors.mid] from `gel_top_band`, deep at the bottom — the 1dp rim on the
 * tile's edge, the lip across the top, and the sheen that glides along the lip in the last quarter
 * of each 10 s loop on the backdrop's one clock, [sheenDelayMs] behind Classic's.
 *
 * Drawn on the `Card`'s modifier, which paints nothing itself, so all of it lies under the content.
 * Built once per size and direction; the draw reads a derived sheen value that stays
 * [SHEEN_HIDDEN] for 7.5 s of every 10, so the tile is not invalidated then. The sheen's clip to the
 * cached lip path is the one clip in a draw lambda on this branch (the tech-lead's E9).
 */
@Composable
internal fun Modifier.gelSurface(
    gel: GelColors,
    shape: Shape,
    sheenDelayMs: Long,
): Modifier {
    val px = GelPx.resolve()
    val clock = LocalBackdropClock.current
    val sheen =
        remember(clock, sheenDelayMs) {
            derivedStateOf { if (clock.isStatic) SHEEN_HIDDEN else gelSheenProgress(clock.timeMs, sheenDelayMs) }
        }
    return drawWithCache {
        val body = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val fill =
            Brush.verticalGradient(
                0f to gel.top,
                (px.topBand / size.height).coerceIn(0f, 1f) to gel.mid,
                1f to gel.bottom,
            )
        val rim =
            Path().apply {
                val inset = Size(size.width - px.rim, size.height - px.rim)
                addOutline(shape.createOutline(inset, layoutDirection, this@drawWithCache))
                translate(Offset(px.rim / 2f, px.rim / 2f))
            }
        val rimStroke = Stroke(width = px.rim)
        val lip =
            Path().apply {
                addRoundRect(
                    RoundRect(
                        left = px.lipInset,
                        top = px.lipTop,
                        right = size.width - px.lipInset,
                        bottom = px.lipBottom,
                        cornerRadius = CornerRadius(px.lipCorner),
                    ),
                )
            }
        val lipBrush =
            Brush.verticalGradient(
                0f to GelColors.lip,
                1f to Color.Transparent,
                startY = px.lipTop,
                endY = px.lipBottom,
            )
        val band = SheenBand.of(size.width, px, layoutDirection)
        onDrawBehind {
            drawPath(body, fill)
            drawPath(rim, GelColors.rim, style = rimStroke)
            drawPath(lip, lipBrush)
            val progress = sheen.value
            if (progress != SHEEN_HIDDEN) {
                clipPath(lip) {
                    val x = band.startX + (band.endX - band.startX) * progress
                    translate(x, 0f) { drawPath(band.path, band.brush) }
                }
            }
        }
    }
}

/** A gel tile's row: the badge, the title over the supporting slot, and the trailing art, white on the gel. */
@Composable
internal fun GelTileContent(
    glyph: ImageVector,
    title: String,
    art: @Composable () -> Unit,
    supporting: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_md)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = GelColors.badge,
        ) {
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = GelColors.onGel,
                modifier =
                    Modifier
                        .padding(dimensionResource(R.dimen.spacing_sm))
                        .size(dimensionResource(R.dimen.mode_glyph_size)),
            )
        }
        Column(
            modifier = Modifier.weight(CONTENT_WEIGHT),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = GelColors.onGel,
            )
            supporting()
        }
        art()
    }
}

/**
 * The gel tile's pressed and focused state layer, darkening: the `Card`'s own ripple in the tile's
 * deep colour at M3's default alphas, so pressing never lightens the gel under its white text.
 */
@Composable
internal fun GelRipple(
    gel: GelColors,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = gel.bottom),
        content = content,
    )
}

/** The gel's `dimens.xml` tokens in px. */
private data class GelPx(
    val topBand: Float,
    val rim: Float,
    val lipTop: Float,
    val lipBottom: Float,
    val lipInset: Float,
    val lipCorner: Float,
) {
    companion object {
        @Composable
        fun resolve(): GelPx =
            with(LocalDensity.current) {
                GelPx(
                    topBand = dimensionResource(R.dimen.gel_top_band).toPx(),
                    rim = dimensionResource(R.dimen.gel_rim_width).toPx(),
                    lipTop = dimensionResource(R.dimen.gel_lip_top).toPx(),
                    lipBottom = dimensionResource(R.dimen.gel_lip_bottom).toPx(),
                    lipInset = dimensionResource(R.dimen.gel_lip_inset).toPx(),
                    lipCorner = dimensionResource(R.dimen.gel_lip_corner).toPx(),
                )
            }
    }
}

/**
 * The sheen: a band `GEL_SHEEN_WIDTH` of the tile wide over the lip's height, leaning
 * `GEL_SHEEN_SKEW_DEG` with its top toward the end edge, centred on x = 0 so the draw only
 * translates it from wholly before the start edge to wholly past the end edge (mirrored in RTL).
 */
private class SheenBand(
    val path: Path,
    val brush: Brush,
    val startX: Float,
    val endX: Float,
) {
    companion object {
        fun of(
            width: Float,
            px: GelPx,
            direction: LayoutDirection,
        ): SheenBand {
            val half = BackdropMetrics.GEL_SHEEN_WIDTH * width / 2f
            val toEnd = if (direction == LayoutDirection.Rtl) -1f else 1f
            val slope = tan(Math.toRadians(BackdropMetrics.GEL_SHEEN_SKEW_DEG.toDouble())).toFloat()
            val lean = (px.lipBottom - px.lipTop) / 2f * slope * toEnd
            val path =
                Path().apply {
                    moveTo(-half + lean, px.lipTop)
                    lineTo(half + lean, px.lipTop)
                    lineTo(half - lean, px.lipBottom)
                    lineTo(-half - lean, px.lipBottom)
                    close()
                }
            val brush =
                Brush.horizontalGradient(
                    0f to Color.Transparent,
                    SHEEN_CENTRE to GelColors.sheen,
                    1f to Color.Transparent,
                    startX = -half - abs(lean),
                    endX = half + abs(lean),
                )
            val reach = half + abs(lean)
            val before = if (toEnd > 0f) -reach else width + reach
            val after = if (toEnd > 0f) width + reach else -reach
            return SheenBand(path, brush, before, after)
        }

        private const val SHEEN_CENTRE = 0.5f
    }
}
