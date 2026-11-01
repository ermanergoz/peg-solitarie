package com.app.pegsolitaire.domain.model

/**
 * A board coordinate. Immutable `(row, col)` pair — the public coordinate type
 * the UI and tests speak. Bit indices for the backing bitmask are derived from a
 * [BoardSpec], never stored on the position itself.
 */
data class Position(
    val row: Int,
    val col: Int,
)
