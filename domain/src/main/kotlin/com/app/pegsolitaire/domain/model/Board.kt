package com.app.pegsolitaire.domain.model

/**
 * An immutable board state: peg occupancy over a [BoardSpec], backed by a single
 * `Long` peg-bitmask ([pegs]). A set bit means a peg is present at that position's
 * stable bit index; a clear bit on an in-play position means an empty hole.
 *
 * The bitmask is encapsulated behind [cellAt]/[withPeg]/[withoutPeg]/[pegCount] so
 * the representation stays swappable, while a `data class` over a `Long` yields a
 * free, collision-free `equals`/`hashCode` (used for golden-master snapshotting)
 * and never mutates in place — every transform returns a new [Board] via `copy`.
 */
data class Board(
    val spec: BoardSpec,
    val pegs: Long,
) {
    /** The derived [Cell] at [position]: [Cell.Blocked] off-footprint, else peg/empty per the bitmask. */
    fun cellAt(position: Position): Cell =
        when {
            position !in spec -> Cell.Blocked
            isPegSet(position) -> Cell.Peg
            else -> Cell.Empty
        }

    /** A new board with a peg set at [position]. [position] must be in-spec. */
    fun withPeg(position: Position): Board = copy(pegs = pegs or maskFor(position))

    /** A new board with the peg cleared at [position]. [position] must be in-spec. */
    fun withoutPeg(position: Position): Board = copy(pegs = pegs and maskFor(position).inv())

    /** The number of pegs currently on the board. */
    val pegCount: Int get() = java.lang.Long.bitCount(pegs)

    private fun isPegSet(position: Position): Boolean {
        val bit = spec.bit(position) ?: return false
        return (pegs shr bit) and PEG_BIT == PEG_BIT
    }

    private fun maskFor(position: Position): Long {
        val bit =
            requireNotNull(spec.bit(position)) {
                "Position $position is not in this board's spec"
            }
        return PEG_BIT shl bit
    }

    private companion object {
        /** A single set bit — the unit mask for one peg. */
        const val PEG_BIT = 1L
    }
}
