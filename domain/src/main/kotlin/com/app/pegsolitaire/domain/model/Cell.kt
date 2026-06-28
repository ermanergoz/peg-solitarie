package com.app.pegsolitaire.domain.model

/**
 * The derived state of a single board coordinate.
 *
 * - [Peg]: an in-play hole currently occupied by a peg.
 * - [Empty]: an in-play hole with no peg.
 * - [Blocked]: a coordinate outside the board footprint (e.g. an English-33 corner).
 *
 * A cell is always derived from a [Board] via [Board.cellAt] — it is never stored
 * redundantly alongside the bitmask.
 */
enum class Cell {
    Peg,
    Empty,
    Blocked,
}
