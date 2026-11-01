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
 *
 * @property minimumPegs the board's "perfect finish" peg count — see the property KDoc.
 */
class BoardSpec(
    val inPlay: List<Position>,
    val target: Position?,
    val minimumPegs: Int = BoardConstants.MINIMUM_PEGS_DEFAULT,
) {
    /**
     * The board's **structural identity key**: the in-play positions sorted
     * ascending by `(row, col)`.
     *
     * [Position] is a data class, so two independently-constructed specs of the
     * same footprint have `footprint` lists that are `==` with equal `hashCode`s
     * regardless of the order `inPlay` was passed in. That is exactly what lets a
     * spec rebuilt from a resume blob be recognised as the shipped catalog spec it
     * came from, and what makes the footprint usable as an opening-book lookup key.
     * [BoardSpec] itself declares no `equals`, so referential identity would be the
     * wrong test — always compare footprints.
     *
     * This is also the single source of the bit ordering: [indexOf] is derived from
     * it, so the sort happens exactly once and the bit assignment is provably the
     * same one the golden masters were recorded against.
     */
    val footprint: List<Position> = inPlay.sortedWith(compareBy({ it.row }, { it.col }))

    private val indexOf: Map<Position, Int> =
        footprint
            .withIndex()
            .associate { (index, position) -> position to index }

    init {
        /*
         * Defence in depth for ASVS V5 / threat T-08-01. The fail-closed boundary
         * for untrusted input is `SnapshotMapper`, which validates the decoded
         * integer BEFORE constructing a spec; this `require` exists so that no
         * other path can smuggle in a target that makes the solver's
         * `pegCount <= minimumPegs` goal test trivially true — which would leave
         * every hint permanently `null`.
         */
        require(minimumPegs in 1..size) {
            "minimumPegs must be in 1..$size (footprint size), was $minimumPegs"
        }
    }

    /** Number of in-play positions (== number of occupied bit indices). */
    val size: Int get() = indexOf.size

    /** The stable bit index for [position], or `null` if it is outside the footprint. */
    fun bit(position: Position): Int? = indexOf[position]

    /** Whether [position] is part of this board's in-play footprint. */
    operator fun contains(position: Position): Boolean = position in indexOf
}
