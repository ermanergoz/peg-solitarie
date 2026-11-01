package com.app.pegsolitaire.ui.game

/**
 * TalkBack's reading order on the gameplay screen, where geometry alone does not give it (D-TBa's
 * loop-5 line on item 1, the check's F-38; `docs/ux-spec.md` §11.1).
 *
 * In landscape, on a short window and on any window wider than 512dp, the full-width rescue banner's
 * left edge is outside the slab's, so a geometric order would read the banner before the cells. The
 * board region is one traversal group, and inside it the slab carries the one index below.
 */
object GameTraversal {
    /** The slab's index in the board region: before the banner or the notice, which keep the default 0. */
    const val BOARD_BEFORE_BANNER = -1f
}
