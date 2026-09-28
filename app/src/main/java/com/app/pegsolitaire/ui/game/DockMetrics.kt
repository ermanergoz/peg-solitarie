package com.app.pegsolitaire.ui.game

/**
 * Unitless ratios the gameplay action dock draws with (ux-spec §4.4.1, §6.1).
 *
 * Alphas and angles are not dimensions, so they live in this scoped object rather than in
 * `dimens.xml` — the same split `BoardMetrics` makes for the board (QUAL-03).
 */
internal object DockMetrics {
    /**
     * The glyph alpha of an [DockLook.Unavailable] action — Material's own disabled-content alpha.
     *
     * Deliberately below 3:1 against the slab: WCAG exempts an inactive control, and the look does
     * not rely on it alone, since the disc also loses its fill and its shadow (§4.4.1 "Contrast").
     */
    const val UNAVAILABLE_GLYPH_ALPHA = 0.38f
}
