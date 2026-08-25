package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position

/**
 * Symmetry canonicalization for a board's `pegs` bitmask — the dedup gate's key.
 *
 * Lifted **verbatim** from the test-source `AuthoringSolver.symmetryPermutations`
 * /`canonical` (RESEARCH §Quality Gates — "lift that canonicalization rather than
 * re-deriving it"; §Don't Hand-Roll). The generator's symmetry-normalized dedup
 * gate (Pitfall 3) folds symmetric starting boards onto one key so a board and its
 * dihedral images count as the same puzzle "in the player's sequence".
 *
 * Pure Kotlin, zero Android deps (QUAL-01): operates only on [BoardSpec]/[Position]
 * and a `Long` mask.
 */
object BoardCanonical {
    /**
     * The bit permutations of [spec]'s symmetry group (the dihedral transforms of
     * its square bounding box that map the footprint onto itself). Applying any of
     * these to a `pegs` mask yields a symmetric position; folding a mask to the
     * lexicographically smallest image under them ([canonical]) gives a
     * symmetry-invariant key.
     *
     * A transform is included only if it maps every in-play cell onto another
     * in-play cell (preserves the footprint); asymmetric footprints keep just the
     * identity.
     */
    fun symmetryPermutations(spec: BoardSpec): List<IntArray> {
        val dimension = spec.inPlay.maxOf { maxOf(it.row, it.col) } + 1
        val last = dimension - 1
        val transforms: List<(Position) -> Position> =
            listOf(
                { p -> p },
                { p -> Position(row = p.col, col = last - p.row) },
                { p -> Position(row = last - p.row, col = last - p.col) },
                { p -> Position(row = last - p.col, col = p.row) },
                { p -> Position(row = p.row, col = last - p.col) },
                { p -> Position(row = last - p.row, col = p.col) },
                { p -> Position(row = p.col, col = p.row) },
                { p -> Position(row = last - p.col, col = last - p.row) },
            )
        return transforms.mapNotNull { transform ->
            val perm = IntArray(spec.size)
            var preservesFootprint = true
            for (position in spec.inPlay) {
                val image = transform(position)
                val sourceBit = spec.bit(position) ?: error("in-play position must have a bit")
                val imageBit = spec.bit(image)
                if (imageBit == null) {
                    preservesFootprint = false
                    break
                }
                perm[sourceBit] = imageBit
            }
            if (preservesFootprint) perm else null
        }
    }

    /** The lexicographically smallest of [pegs] under all [symmetries] — a canonical key. */
    fun canonical(
        pegs: Long,
        symmetries: List<IntArray>,
    ): Long {
        var minimum = pegs
        for (perm in symmetries) {
            var transformed = 0L
            var remaining = pegs
            while (remaining != 0L) {
                val bit = java.lang.Long.numberOfTrailingZeros(remaining)
                transformed = transformed or (1L shl perm[bit])
                remaining = remaining and (remaining - 1)
            }
            if (transformed < minimum) minimum = transformed
        }
        return minimum
    }
}
