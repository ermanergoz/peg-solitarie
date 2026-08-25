package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import kotlin.random.Random

/**
 * Builds arbitrary **connected** freeform board footprints on the square grid
 * (D-08/D-09), the higher-level variety lever beyond the canonical catalog.
 *
 * A shape is grown by seeded frontier expansion (so it is connected by
 * construction) inside a bounding box capped by
 * [GeneratorConstants.FREEFORM_BOUNDING_DIM], then validated by the two
 * genuinely-new guardrails (no in-repo analog): an INDEPENDENT orthogonal
 * flood-fill connectivity check and the `size <= 64` `Long`-bitmask cap
 * ([GeneratorConstants.MAX_IN_PLAY_CELLS], T-03-05). Where cheap, the most
 * symmetric of a few candidates is kept (D-09).
 *
 * All coordinates land on the existing `(row, col)` lattice, so `BoardGeometry`
 * and `BoardCanvas` render freeform boards unchanged (RESEARCH §Freeform, A3).
 * Pure Kotlin, zero Android deps (QUAL-01); the PRNG index is only ever drawn
 * against a `List` (Pitfall 1). All tuning values are scoped
 * [GeneratorConstants] (QUAL-03).
 */
object FreeformFootprints {
    /** The four orthogonal neighbour offsets `(rowDelta, colDelta)`. */
    private val ORTHOGONAL_NEIGHBORS: List<Pair<Int, Int>> =
        listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)

    /**
     * Grows and validates a freeform [BoardSpec] for [tier], or `null` if no valid
     * candidate was produced (the caller then falls back to canonical / reseeds).
     *
     * Grows [GeneratorConstants.FREEFORM_SYMMETRY_CANDIDATES] connected shapes and
     * keeps the one with the largest symmetry group (D-09), each drawn
     * deterministically from [rng].
     */
    fun build(
        rng: Random,
        tier: DifficultyTier,
    ): BoardSpec? {
        val dimension = GeneratorConstants.FREEFORM_BOUNDING_DIM
        val targetCells = targetCellCount(tier, dimension)

        var bestSpec: BoardSpec? = null
        var bestSymmetry = NO_SYMMETRY_SCORE
        repeat(GeneratorConstants.FREEFORM_SYMMETRY_CANDIDATES) {
            val shape = growConnected(rng, dimension, targetCells)
            if (shape.size <= GeneratorConstants.MAX_IN_PLAY_CELLS && isConnected(shape)) {
                val spec = BoardSpec(inPlay = shape, target = null)
                val symmetry = BoardCanonical.symmetryPermutations(spec).size
                if (symmetry > bestSymmetry) {
                    bestSpec = spec
                    bestSymmetry = symmetry
                }
            }
        }
        return bestSpec
    }

    /**
     * Independent orthogonal (4-neighbour) flood-fill connectivity check: from the
     * first cell, every in-play cell must be reachable. Rejects any shape with an
     * isolated cell or a disconnected region (D-09). Public so callers/tests can
     * validate an arbitrary candidate `List<Position>`.
     */
    fun isConnected(positions: List<Position>): Boolean {
        val cells = positions.toHashSet()
        if (cells.isEmpty()) return false
        val visited = HashSet<Position>()
        val stack = ArrayDeque<Position>()
        val start = positions.first()
        visited += start
        stack.addLast(start)
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast()
            for ((rowDelta, colDelta) in ORTHOGONAL_NEIGHBORS) {
                val next = Position(row = cell.row + rowDelta, col = cell.col + colDelta)
                if (next in cells && visited.add(next)) stack.addLast(next)
            }
        }
        return visited.size == cells.size
    }

    /**
     * The freeform cell-count target: the tier's upper peg bound plus
     * [GeneratorConstants.FREEFORM_SIZE_HEADROOM] (empty room for the reverse walk),
     * clamped to `[FREEFORM_MIN_CELLS, min(dimension^2, MAX_IN_PLAY_CELLS)]`.
     */
    private fun targetCellCount(
        tier: DifficultyTier,
        dimension: Int,
    ): Int {
        val cap = minOf(dimension * dimension, GeneratorConstants.MAX_IN_PLAY_CELLS)
        val desired = tier.pegRange.last + GeneratorConstants.FREEFORM_SIZE_HEADROOM
        return desired.coerceIn(GeneratorConstants.FREEFORM_MIN_CELLS, cap)
    }

    /**
     * Grows a connected blob of up to [target] cells from the centre of a
     * [dimension]x[dimension] box by seeded frontier expansion. Only cells adjacent
     * to the current shape are ever added, so the result is connected by
     * construction (independently re-checked by [isConnected]). The PRNG index is
     * drawn against the frontier `List`, never a set (Pitfall 1).
     */
    private fun growConnected(
        rng: Random,
        dimension: Int,
        target: Int,
    ): List<Position> {
        val start = Position(row = dimension / 2, col = dimension / 2)
        val inShape = LinkedHashSet<Position>()
        inShape += start
        val frontier = ArrayList<Position>()
        addFrontier(frontier, start, dimension, inShape)
        while (inShape.size < target && frontier.isNotEmpty()) {
            val next = frontier.removeAt(rng.nextInt(frontier.size))
            if (inShape.add(next)) addFrontier(frontier, next, dimension, inShape)
        }
        return inShape.toList()
    }

    /** Appends [cell]'s in-bounds, not-yet-included orthogonal neighbours to [frontier]. */
    private fun addFrontier(
        frontier: MutableList<Position>,
        cell: Position,
        dimension: Int,
        inShape: Set<Position>,
    ) {
        for ((rowDelta, colDelta) in ORTHOGONAL_NEIGHBORS) {
            val neighbor = Position(row = cell.row + rowDelta, col = cell.col + colDelta)
            if (neighbor.row in 0 until dimension &&
                neighbor.col in 0 until dimension &&
                neighbor !in inShape
            ) {
                frontier.add(neighbor)
            }
        }
    }

    /** Sentinel "no symmetric candidate kept yet" (any real symmetry order is >= 1). */
    private const val NO_SYMMETRY_SCORE = -1
}
