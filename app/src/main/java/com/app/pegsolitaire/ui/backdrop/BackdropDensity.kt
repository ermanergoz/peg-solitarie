package com.app.pegsolitaire.ui.backdrop

/**
 * How much of the backdrop a destination carries (backdrop spec §2, B-01): the deep
 * sky with its waves and lights behind Home ([Landscape]) and the game screen ([Quiet]), and a
 * static wash behind every other screen ([Sparse]).
 *
 * A density selects which layers draw, which sky stops, wave places and light table apply, and
 * which idle delay settles the clock. It never changes a token's meaning or a legibility rule.
 */
enum class BackdropDensity {
    /** Home: the sky, two waves at 0.49 H and 0.62 H, and nine lights. */
    Landscape,

    /** The game screen, Classic and Challenge alike (B-07): the sky, two lower waves, five lights. */
    Quiet,

    /** Every other screen: the static wash alone, with no motion and no idle frame (B-23). */
    Sparse,
    ;

    /** Whether this density has anything for the clock to move. */
    val moves: Boolean get() = this != Sparse
}
