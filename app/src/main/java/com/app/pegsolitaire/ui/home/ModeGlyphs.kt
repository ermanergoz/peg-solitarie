package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.R

/**
 * Small local [ImageVector] glyphs for the Home mode tiles (DESIGN-SYSTEM.md
 * Home): a 2×2 board-grid glyph for Classic and a lightning bolt for Challenge.
 *
 * The core Material icon set carries neither shape, and the extended set stays
 * banned, so these two tiny vectors are hand-built here. They are drawn in a
 * fixed 24-unit viewport, sized via the `mode_glyph_size` dimen, filled black
 * and recolored by the host `Icon`'s tint (exactly like stock material icons).
 */
@Composable
internal fun rememberClassicGlyph(): ImageVector {
    val size = dimensionResource(R.dimen.mode_glyph_size)
    return remember(size) { buildClassicGlyph(size) }
}

/** The Challenge lightning-bolt glyph (see [rememberClassicGlyph]). */
@Composable
internal fun rememberChallengeGlyph(): ImageVector {
    val size = dimensionResource(R.dimen.mode_glyph_size)
    return remember(size) { buildChallengeGlyph(size) }
}

/** The 24-unit square viewport both glyphs are drawn in. */
private const val GLYPH_VIEWPORT = 24f

@Suppress("MagicNumber") // Vector path data: raw viewport coordinates, like any compiled icon.
private fun buildClassicGlyph(size: Dp): ImageVector =
    ImageVector
        .Builder(
            name = "ClassicGridGlyph",
            defaultWidth = size,
            defaultHeight = size,
            viewportWidth = GLYPH_VIEWPORT,
            viewportHeight = GLYPH_VIEWPORT,
        ).path(fill = SolidColor(Color.Black)) {
            // Four board cells in a 2×2 grid.
            moveTo(3f, 3f)
            lineTo(10.5f, 3f)
            lineTo(10.5f, 10.5f)
            lineTo(3f, 10.5f)
            close()
            moveTo(13.5f, 3f)
            lineTo(21f, 3f)
            lineTo(21f, 10.5f)
            lineTo(13.5f, 10.5f)
            close()
            moveTo(3f, 13.5f)
            lineTo(10.5f, 13.5f)
            lineTo(10.5f, 21f)
            lineTo(3f, 21f)
            close()
            moveTo(13.5f, 13.5f)
            lineTo(21f, 13.5f)
            lineTo(21f, 21f)
            lineTo(13.5f, 21f)
            close()
        }.build()

@Suppress("MagicNumber") // Vector path data: raw viewport coordinates, like any compiled icon.
private fun buildChallengeGlyph(size: Dp): ImageVector =
    ImageVector
        .Builder(
            name = "ChallengeBoltGlyph",
            defaultWidth = size,
            defaultHeight = size,
            viewportWidth = GLYPH_VIEWPORT,
            viewportHeight = GLYPH_VIEWPORT,
        ).path(fill = SolidColor(Color.Black)) {
            // A chunky lightning bolt.
            moveTo(13f, 2f)
            lineTo(4.8f, 13.2f)
            lineTo(10.2f, 13.2f)
            lineTo(9f, 22f)
            lineTo(19.2f, 9.8f)
            lineTo(13.8f, 9.8f)
            close()
        }.build()
