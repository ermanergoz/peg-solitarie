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

    /**
     * Glow-ring radius around the peg to move (A) for a revealed hint = ratio × cellSize.
     * Slightly larger than [SELECTED_RING_RATIO] so a hint never reads as a selection (D-01).
     */
    const val HINT_SOURCE_RING_RATIO = 0.42f

    /**
     * Ring radius around the destination hole (B) for a revealed hint = ratio × cellSize.
     * Larger than [DESTINATION_DOT_RATIO] so it reads as a target, not a legal-move dot.
     */
    const val HINT_TARGET_RING_RATIO = 0.30f

    /** Stroke width of the faint A→B hint arrow as a fraction of cellSize. */
    const val HINT_ARROW_WIDTH_RATIO = 0.06f

    /** Alpha of the `tertiary` hint source/target rings. */
    const val HINT_HIGHLIGHT_ALPHA = 0.9f

    /** Alpha of the deliberately faint `tertiary` A→B hint arrow (D-01). */
    const val HINT_ARROW_ALPHA = 0.5f

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

    // ---- Candy-Clay rendering ratios (DESIGN-SYSTEM.md Gameplay) ----

    /** Radial PegGradient center offset toward the top-left = ratio × pegRadius. */
    const val PEG_GRADIENT_OFFSET_RATIO = 0.35f

    /** Radial PegGradient extent = ratio × pegRadius (rim color reaches the edge). */
    const val PEG_GRADIENT_RADIUS_RATIO = 1.7f

    /** Gradient stop where the peg body color sits between highlight (0) and rim (1). */
    const val PEG_GRADIENT_MID_STOP = 0.55f

    /** Specular highlight dot radius = ratio × pegRadius. */
    const val SPECULAR_RADIUS_RATIO = 0.28f

    /** Specular highlight dot offset toward the top-left = ratio × pegRadius. */
    const val SPECULAR_OFFSET_RATIO = 0.42f

    /** Inner dimple-fill radius of the two-tone inset hole = ratio × holeRadius. */
    const val DIMPLE_INNER_RATIO = 0.78f

    /** Downward offset of the dimple fill = ratio × holeRadius (dark top ring reads inset). */
    const val DIMPLE_OFFSET_RATIO = 0.22f

    /** Peak scale of the selected peg's breathing pulse (1.0 → 1.06). */
    const val PULSE_MAX_SCALE = 1.06f

    /** The landed jumper briefly squashes to this scale before springing back. */
    const val LANDING_SQUASH_SCALE = 0.86f

    // ---- The wrong tap (D-06, docs/ux/invalid-tap-feedback.md §3.1–§3.2, §5) ----

    /**
     * The shake's amplitude A = ratio × cellSize: about 4.2 dp on a 7×7 board on a 360 dp phone,
     * 3.3 dp at 9×9 and 6.4 dp at the 480 dp tablet cap.
     */
    const val WRONG_TAP_SHAKE_RATIO = 0.10f

    /** The held peg's ring at the peak of its nudge, × its radius; it multiplies the breathing pulse. */
    const val WRONG_TAP_RING_NUDGE_SCALE = 1.18f

    /**
     * The reduced-motion mark's radius = ratio × cellSize: outside the peg's 0.34 and the hole's
     * 0.16, so it sits on the slab. Its own constant, although it equals [SELECTED_RING_RATIO].
     */
    const val WRONG_TAP_MARK_RING_RATIO = 0.40f

    /** The mark is this many equal dashes at a 50 % duty cycle, so a shape tells it from the solid rings. */
    const val WRONG_TAP_MARK_DASHES = 12
}
