package com.app.pegsolitaire.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The gloss finish's themed colours (docs/ux/candy-backdrop.md §3.5.1, B-19): the 1dp rim, the
 * 2dp lip, the white gloss and the coloured glow that replaces the clay shadow's tint on Home's
 * and the game screen's hero containers. Provided by `AppTheme` through [LocalFinishColors].
 */
@Immutable
data class FinishColors(
    val panelRim: Color,
    val panelLip: Color,
    val panelGloss: Color,
    val panelGlow: Color,
)

internal val LightFinishColors =
    FinishColors(
        panelRim = Color.Transparent,
        panelLip = Color.Transparent,
        panelGloss = Color.Transparent,
        panelGlow = Color.Transparent,
    )

internal val DarkFinishColors =
    FinishColors(
        panelRim = Color.Transparent,
        panelLip = Color.Transparent,
        panelGloss = Color.Transparent,
        panelGlow = Color.Transparent,
    )

/** The active finish palette, provided by `AppTheme` beside [LocalBackdropColors]. */
val LocalFinishColors = staticCompositionLocalOf { LightFinishColors }

/**
 * One gel mode tile's colour family (docs/ux/candy-backdrop.md §3.5.2, B-18): its vertical fill,
 * light at the top edge, [mid] from `gel_top_band` and deep at the bottom edge, and its glow. The
 * same in both themes, so a plain holder with no `CompositionLocal` (the tech-lead's A3, E12).
 *
 * The tokens the two tiles share are on the companion: the white content, the badge, the level
 * chip, the flat art, the rim, the lip and the sheen.
 */
@Immutable
data class GelColors(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val glow: Color,
) {
    companion object {
        /** The Classic tile: violet, the house lead. */
        val Violet = GelColors(top = Color.Transparent, mid = Color.Transparent, bottom = Color.Transparent, glow = GelGlowViolet)

        /** The Challenge tile: pink, the house second. */
        val Pink = GelColors(top = Color.Transparent, mid = Color.Transparent, bottom = Color.Transparent, glow = GelGlowPink)

        /** Text and glyphs on the gel: white. */
        val onGel: Color = Color.Transparent

        /** The glyph badge's fill. */
        val badge: Color = Color.Transparent

        /** The Challenge level chip's fill, and its text. */
        val chip: Color = Color.Transparent
        val chipText: Color = Color.Transparent

        /** The Classic tile's flat footprint: pegs and holes, and the Challenge bolt's tint. */
        val artPeg: Color = Color.Transparent
        val artHole: Color = Color.Transparent

        /** The tile's 1dp rim, its lip's top colour and its sheen's centre colour. */
        val rim: Color = Color.Transparent
        val lip: Color = Color.Transparent
        val sheen: Color = Color.Transparent
    }
}
