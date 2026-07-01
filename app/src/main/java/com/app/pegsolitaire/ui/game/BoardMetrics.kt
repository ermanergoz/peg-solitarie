package com.app.pegsolitaire.ui.game

/**
 * Unitless board-geometry ratios for the English-33 Canvas render.
 *
 * These are deliberately **not** `dimens.xml` tokens: they are proportional, not
 * fixed dp. Every radius is `ratio × cellSize`, where
 * `cellSize = min(width, height) / gridDimension` is derived from the measured
 * canvas size and the per-board `gridDimension` (7/8/9, computed in
 * [com.app.pegsolitaire.ui.components.BoardGeometry] from the `BoardSpec` bounds —
 * not hardcoded here, so every footprint renders in bounds). Keeping these ratios
 * in this scoped object (rather than inline) satisfies QUAL-03's no-magic-number
 * rule while keeping the geometry resolution-independent.
 */
object BoardMetrics {
    /** Resting/jumping peg radius = ratio × cellSize. */
    const val PEG_RADIUS_RATIO = 0.34f

    /** Empty-hole indicator radius = ratio × cellSize. */
    const val HOLE_RADIUS_RATIO = 0.16f

    /** Legal-destination dot radius = ratio × cellSize. */
    const val DESTINATION_DOT_RATIO = 0.18f

    /** Outer radius of the selected-peg ring = ratio × cellSize. */
    const val SELECTED_RING_RATIO = 0.40f

    /** The selected peg grows slightly for affordance. */
    const val SELECTED_PEG_SCALE = 1.08f

    /** A jumped peg shrinks to this fraction of its size before vanishing (D-08). */
    const val REMOVAL_SCALE_TARGET = 0.30f

    /** Alpha of the `primary` legal-destination dot. */
    const val DESTINATION_DOT_ALPHA = 0.38f

    /** Fraction of a cell from its top-left edge to its center (geometry origin offset). */
    const val CELL_CENTER_FRACTION = 0.5f

    /**
     * Max distance (as a fraction of cellSize) from a cell center within which a
     * tap counts as hitting that cell — the inscribed-circle hit area. Taps in the
     * dead corners of a cell, or outside the footprint entirely, are dropped
     * (T-04-01 bounds-check before [com.app.pegsolitaire.ui.components.BoardCanvas]
     * emits `onCellTap`).
     */
    const val HIT_RADIUS_RATIO = 0.5f

    /** The board is a square. */
    const val BOARD_ASPECT_RATIO = 1f
}
