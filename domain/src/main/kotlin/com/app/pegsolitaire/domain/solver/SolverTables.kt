package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine

/**
 * The per-footprint precomputed tables the bitboard search core runs on — built once
 * per board footprint, then reused by every search over it.
 *
 * **Why these exist.** The shipped tree search costs a `List<Move>` of data-class
 * objects, a `sortedByDescending` with a fresh random draw per move, and a `Board`
 * data-class copy *per node*. Measured against a tuned bitboard reference that is a
 * ~55x pure constant factor (~0.27M vs ~15M nodes/s), entirely allocation. With these
 * tables a node costs three mask operations and no allocation at all:
 *
 * ```
 * if (pegs and needMask[m] == needMask[m] && pegs and toMask[m] == 0L) {
 *     search(pegs xor xorMask[m], depth + 1)
 * }
 * ```
 *
 * **[RulesEngine] remains the single move authority.** These tables are a
 * *precomputation of* the rules, never a second definition of them: the jump triples
 * are enumerated by asking [RulesEngine] for the move set of the footprint rather
 * than by re-deriving adjacency here. `SolverTablesTest` proves set-equality against
 * [RulesEngine.legalMoves] and mask-equality against [RulesEngine.applyMove] over a
 * seeded corpus on every shipped footprint — if the two ever drift, the solver would
 * explore moves gameplay rejects and a hint would highlight an illegal move
 * (threat T-08-09).
 *
 * Pure Kotlin, zero Android deps (QUAL-01).
 *
 * @property needMask per move index, `bit(from) or bit(over)` — both cells must hold pegs.
 * @property toMask per move index, `bit(to)` — that cell must be empty.
 * @property xorMask per move index, `needMask or toMask` — applying the move is ONE xor.
 * @property moveOrder move indices pre-sorted by inwardness (descending), computed once.
 * @property classPlus the three `(row + col) mod 3` colour-class masks.
 * @property classMinus the three `(row - col) mod 3` colour-class masks.
 * @property chunkTables this footprint's symmetry canonicalisation tables.
 */
internal class SolverTables private constructor(
    spec: BoardSpec,
) {
    /** Every jump triple of this footprint, in [RulesEngine]'s stable enumeration order. */
    private val moves: List<Move> = enumerateJumpTriples(spec)

    val needMask: LongArray = buildNeedMasks(spec, moves)
    val toMask: LongArray = buildToMasks(spec, moves)
    val xorMask: LongArray = LongArray(moves.size) { index -> needMask[index] or toMask[index] }
    val moveOrder: IntArray = buildMoveOrder(spec, moves)
    val classPlus: LongArray = buildColourClassMasks(spec) { row, col -> row + col }
    val classMinus: LongArray = buildColourClassMasks(spec) { row, col -> row - col }
    val chunkTables: BoardCanonical.ChunkTables = BoardCanonical.chunkTables(spec)

    /** The [Move] denoted by move [index] — used to surface a search result to a caller. */
    fun moveAt(index: Int): Move = moves[index]

    companion object {
        /**
         * Footprints whose tables stay resident. Challenge generates a FRESH footprint
         * per level, so an unbounded cache would grow without limit at roughly 100 KB
         * per entry (the canonicalisation chunk tables dominate) — a slow leak over a
         * long session, which is why this bound exists (threat T-08-12). Six covers the
         * five Classic footprints plus the one Challenge footprint in play, i.e. the
         * whole working set of any real session.
         *
         * `internal` rather than `private` so `SolverTablesTest` can churn exactly past
         * the real bound instead of restating it as a literal.
         */
        internal const val MAX_CACHED_FOOTPRINTS = 6

        /** `LinkedHashMap`'s default load factor — carried explicitly by the access-order constructor. */
        private const val CACHE_LOAD_FACTOR = 0.75f

        /**
         * Access-ordered, capacity-bounded table cache keyed on [BoardSpec.footprint],
         * the *structural* identity of a board: a spec rebuilt from a resume blob is a
         * different instance carrying the same positions and must hit the same entry.
         */
        private val cache =
            object : LinkedHashMap<List<Position>, SolverTables>(MAX_CACHED_FOOTPRINTS, CACHE_LOAD_FACTOR, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<List<Position>, SolverTables>): Boolean =
                    size > MAX_CACHED_FOOTPRINTS
            }

        /** The spec instance of the most recent [forSpec] call — see the fast path in [forSpec]. */
        private var lastSpec: BoardSpec? = null

        /** The tables returned for [lastSpec]. */
        private var lastTables: SolverTables? = null

        /**
         * The tables for [spec], built once per footprint and memoised.
         *
         * **Referential fast path.** [Pagoda] calls this once per search node, and the
         * map key is a `List<Position>` whose `hashCode` is O(footprint size) — paying
         * that per node would hand back the allocation win these tables exist to
         * deliver. A search hammers ONE spec instance, so a single-slot identity check
         * short-circuits it. Correctness does not depend on the slot: the tables are a
         * pure function of the footprint, so a stale-but-equal hit is the same object a
         * map lookup would return.
         *
         * Guarded by `@Synchronized` rather than any coroutine primitive — `:domain` is
         * pure Kotlin/JVM with no coroutine dependency (QUAL-01).
         */
        @Synchronized
        fun forSpec(spec: BoardSpec): SolverTables {
            lastTables?.let { cached -> if (lastSpec === spec) return cached }
            val tables = cache.getOrPut(spec.footprint) { SolverTables(spec) }
            lastSpec = spec
            lastTables = tables
            return tables
        }
    }
}

/**
 * Every jump triple of [spec], obtained FROM [RulesEngine] rather than by re-deriving
 * adjacency.
 *
 * A fully-populated board has no legal move (a jump needs an empty destination), so
 * the enumeration asks for the moves of the board that is full except one hole, once
 * per hole: that board offers exactly the jumps landing in that hole, and the union
 * over every hole is the whole jump set. The result is re-sorted into [RulesEngine]'s
 * own stable `(from, to)` order so move indices are deterministic across runs.
 */
private fun enumerateJumpTriples(spec: BoardSpec): List<Move> {
    val full = spec.inPlay.fold(Board(spec = spec, pegs = 0L)) { board, position -> board.withPeg(position) }
    return spec.footprint
        .flatMap { hole -> RulesEngine.legalMoves(full.withoutPeg(hole)) }
        .distinct()
        .sortedWith(compareBy({ it.from.row }, { it.from.col }, { it.to.row }, { it.to.col }))
}

private fun buildNeedMasks(
    spec: BoardSpec,
    moves: List<Move>,
): LongArray = LongArray(moves.size) { index -> maskOf(spec, moves[index].from) or maskOf(spec, moves[index].over) }

private fun buildToMasks(
    spec: BoardSpec,
    moves: List<Move>,
): LongArray = LongArray(moves.size) { index -> maskOf(spec, moves[index].to) }

/**
 * Move indices sorted by descending inwardness — the deterministic base ordering the
 * search walks, computed once here instead of re-sorting a fresh `List<Move>` at every
 * node.
 *
 * Inwardness is the same score the production search uses today: how much closer to
 * the footprint's centroid the peg lands than it started. Preferring inward jumps
 * collapses the arms toward the centre, which is the textbook way to solve cross
 * boards, so a solution surfaces early. Ties break on the move index so the table is
 * reproducible. Per-attempt jitter is deliberately NOT baked in — jitter belongs to
 * the search, which restarts with fresh seeds; the table carries only the base order.
 */
private fun buildMoveOrder(
    spec: BoardSpec,
    moves: List<Move>,
): IntArray {
    val centre = centroidOf(spec)
    val inwardness = DoubleArray(moves.size) { index -> inwardnessOf(moves[index], centre) }
    return moves.indices
        .sortedWith(compareByDescending<Int> { index -> inwardness[index] }.thenBy { index -> index })
        .toIntArray()
}

/**
 * The three colour-class masks of [spec] under [coordinate] — bit `i` of mask `c` is
 * set when the in-play cell at bit `i` has `coordinate(row, col) mod 3 == c`.
 *
 * These turn [Pagoda]'s per-cell residue loop into a popcount per class. Every
 * orthogonal jump's three cells are colinear and consecutive, so a jump touches all
 * three classes of a colouring exactly once — which is what makes the class parities
 * a conserved quantity worth precomputing.
 */
private fun buildColourClassMasks(
    spec: BoardSpec,
    coordinate: (Int, Int) -> Int,
): LongArray {
    val masks = LongArray(Pagoda.COLORING_MODULUS)
    for (position in spec.footprint) {
        val residue = Math.floorMod(coordinate(position.row, position.col), Pagoda.COLORING_MODULUS)
        masks[residue] = masks[residue] or maskOf(spec, position)
    }
    return masks
}

/** The mean `(row, col)` of the footprint — the attractor the inward heuristic collapses toward. */
private fun centroidOf(spec: BoardSpec): DoubleArray =
    doubleArrayOf(
        spec.inPlay.sumOf { it.row }.toDouble() / spec.size,
        spec.inPlay.sumOf { it.col }.toDouble() / spec.size,
    )

private fun inwardnessOf(
    move: Move,
    centre: DoubleArray,
): Double = distanceSquared(move.from, centre) - distanceSquared(move.to, centre)

private fun distanceSquared(
    position: Position,
    centre: DoubleArray,
): Double {
    val rowDelta = position.row - centre[0]
    val colDelta = position.col - centre[1]
    return rowDelta * rowDelta + colDelta * colDelta
}

/** The single-bit mask of [position] in [spec]. An off-footprint position is a programming error. */
private fun maskOf(
    spec: BoardSpec,
    position: Position,
): Long {
    val bit = spec.bit(position) ?: error("$position is not in the footprint these tables are built for")
    return 1L shl bit
}
