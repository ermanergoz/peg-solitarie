package com.app.pegsolitaire.domain.model

/**
 * The immutable footprint of a board: the set of in-play [Position]s plus an
 * optional [target] hole (where the final peg should land).
 *
 * Each in-play position is assigned a **stable bit index** `0..size-1` by sorting
 * on `(row, col)`. That deterministic assignment is what makes the [Board]'s
 * backing `Long` bitmask reproducible across runs (load-bearing for the
 * golden-master determinism contract), and lets every board footprint up to 64
 * holes share one `Long`.
 *
 * A [BoardSpec] is created once and shared by every [Board] that uses it.
 */
class BoardSpec(
    val inPlay: List<Position>,
    val target: Position?,
) {
    private val indexOf: Map<Position, Int> =
        inPlay
            .sortedWith(compareBy({ it.row }, { it.col }))
            .withIndex()
            .associate { (index, position) -> position to index }

    /** Number of in-play positions (== number of occupied bit indices). */
    val size: Int get() = indexOf.size

    /** The stable bit index for [position], or `null` if it is outside the footprint. */
    fun bit(position: Position): Int? = indexOf[position]

    /** Whether [position] is part of this board's in-play footprint. */
    operator fun contains(position: Position): Boolean = position in indexOf
}
