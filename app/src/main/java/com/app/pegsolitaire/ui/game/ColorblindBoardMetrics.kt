package com.app.pegsolitaire.ui.game

/**
 * The colour-blind palette's board geometry, alphas and dash count (docs/ux/colour-vision.md §3.4),
 * beside [BoardMetrics], whose standard values they replace when the option is on. Radii are
 * fractions of the cell edge, like [BoardMetrics]'. The board reads them only through
 * [BoardMarkRecipe] (`BoardMarkPalette.Colorblind.recipe`), never directly.
 */
object ColorblindBoardMetrics {
    /** CV-07: the selection ring moves out from 0.40c to make room for the halo inside it. */
    const val SELECTED_RING_RATIO = 0.42f

    /** CV-09: the hint source ring, still strictly larger than the selection ring. */
    const val HINT_SOURCE_RING_RATIO = 0.44f

    /** CV-09: the hint target ring, unchanged in size from the standard palette. */
    const val HINT_TARGET_RING_RATIO = 0.30f

    /** CV-08: the legal-target mark is opaque. */
    const val DESTINATION_DOT_ALPHA = 1f

    /** CV-09: both hint rings are opaque. */
    const val HINT_HIGHLIGHT_ALPHA = 1f

    /** §3.4: the hint connector keeps the standard palette's alpha. */
    const val HINT_ARROW_ALPHA = 0.5f

    /** CV-09: eight dashes per hint ring, each 22.5° of arc with a gap of the same length. */
    const val HINT_RING_DASHES = 8
}
