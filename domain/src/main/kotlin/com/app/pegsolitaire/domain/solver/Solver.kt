package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine

/** Lone peg count that marks a fully-solved board (D-02: any final peg counts). */
private const val SOLVED_PEG_COUNT = 1

/**
 * The production, pure-`:domain` winning-move engine (ASST-01 / ASST-03).
 *
 * [firstWinningMove] returns a legal move that begins a line reducing an arbitrary
 * mid-game [Board] to a single peg, or `null` when the position is genuinely
 * unsolvable (or the search is budget-missed / cancelled — folded to the same `null`
 * per D-04). It powers the Challenge deviation hint and every Classic hint, and
 * answers the "no winning move" state.
 *
 * It is a hardened port of the test-only authoring solver: a memoised, symmetry-
 * canonical depth-first search over [RulesEngine.legalMoves]/[RulesEngine.applyMove]
 * (the same move authority as gameplay, so a hint is always a legal, reachable move —
 * threat T-04-CT), with:
 *  - a [Pagoda] O(cells) necessary-condition fast-reject for dead positions;
 *  - a node budget ([SolverBudget.maxNodes]) that bounds worst-case time AND memory;
 *  - a cooperative `shouldCancel` poll (every [SolverBudget.CANCEL_MASK]+1 nodes) so
 *    the caller can enforce a deadline / cancellation off the domain (no coroutine
 *    dependency crosses into `:domain`).
 *
 * The goal check runs BEFORE the memo insert so a solved board always returns its
 * line even if a symmetric position was already memoised. Finding a move is fast;
 * proving `null` exhausts the (monotone-shrinking) forward subtree within budget.
 */
object Solver {
    /** Distinct (symmetry-canonical) positions explored by the most recent search (diagnostics). */
    var lastVisited: Long = 0
        private set

    /**
     * The first move of a winning line from [board], or `null` if [board] is already
     * solved, provably unsolvable (pagoda / exhausted), or the search is stopped by
     * [budget] or [shouldCancel].
     *
     * @param shouldCancel polled cooperatively (~every 4096 nodes); returning `true`
     *   abandons the search and yields `null`. Kept a plain lambda so no coroutine
     *   type crosses the domain boundary (QUAL-01 purity).
     */
    fun firstWinningMove(
        board: Board,
        budget: SolverBudget,
        shouldCancel: () -> Boolean = { false },
    ): Move? {
        if (board.pegCount == SOLVED_PEG_COUNT || !Pagoda.mayBeSolvable(board)) {
            lastVisited = 0
            return null
        }
        val search = WinningLineSearch(board.spec, budget, shouldCancel)
        val line = search.solveFrom(board)
        lastVisited = search.visited
        return line?.firstOrNull()
    }
}

/**
 * A single memoised, symmetry-canonical DFS over one board footprint — one instance
 * per [Solver.firstWinningMove] call. Holds the search's mutable state (the visited
 * memo, node counter, cancel latch) and the footprint-derived immutables (symmetry
 * group, centroid) so the recursion stays a small method and the public entry point
 * stays trivial.
 */
private class WinningLineSearch(
    spec: BoardSpec,
    private val budget: SolverBudget,
    private val shouldCancel: () -> Boolean,
) {
    private val symmetries: List<IntArray> = symmetryPermutations(spec)
    private val center: DoubleArray = centroidOf(spec)
    private val seen = VisitedSet()
    private var nodeCounter = 0L
    private var cancelled = false

    /** Distinct (symmetry-canonical) positions explored — the budget-headroom diagnostic. */
    val visited: Long get() = seen.size.toLong()

    /**
     * The winning line from [board] (its first move begins a solve to one peg), or
     * `null` if the forward subtree is exhausted within budget, or the search is
     * budget-missed / cancelled. Goal check runs BEFORE the memo insert.
     */
    fun solveFrom(board: Board): List<Move>? = search(board, ArrayDeque())

    private fun search(
        current: Board,
        path: ArrayDeque<Move>,
    ): List<Move>? {
        if (current.pegCount == SOLVED_PEG_COUNT) return path.toList()
        var solution: List<Move>? = null
        val withinBudget = seen.size < budget.maxNodes
        if (!cancelled && withinBudget && seen.add(canonical(current.pegs))) {
            val ordered =
                RulesEngine
                    .legalMoves(current)
                    .sortedByDescending { inwardness(it) }
            for (move in ordered) {
                if ((nodeCounter++ and SolverBudget.CANCEL_MASK) == 0L && shouldCancel()) {
                    cancelled = true
                }
                if (!cancelled) {
                    path.addLast(move)
                    solution = search(RulesEngine.applyMove(current, move), path)
                    path.removeLast()
                }
                if (cancelled || solution != null) break
            }
        }
        return solution
    }

    /** The mean `(row, col)` of the footprint — the attractor the inward heuristic collapses toward. */
    private fun centroidOf(spec: BoardSpec): DoubleArray =
        doubleArrayOf(
            spec.inPlay.sumOf { it.row }.toDouble() / spec.size,
            spec.inPlay.sumOf { it.col }.toDouble() / spec.size,
        )

    /**
     * Inwardness of a [move]: how much closer to the centroid the peg lands than it
     * started. Preferring inward jumps collapses the arms toward the centre — the
     * textbook way to solve cross boards — so a solution surfaces early. Used as a
     * deterministic move ordering (no jitter, unlike the authoring analog).
     */
    private fun inwardness(move: Move): Double = distanceSquared(move.from) - distanceSquared(move.to)

    private fun distanceSquared(position: Position): Double {
        val dr = position.row - center[0]
        val dc = position.col - center[1]
        return dr * dr + dc * dc
    }

    /** The lexicographically smallest of [pegs] under all [symmetries] — a canonical memo key. */
    private fun canonical(pegs: Long): Long {
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

    /**
     * The bit permutations of the board's symmetry group (the dihedral transforms of
     * its square bounding box that map the footprint onto itself). Folds symmetric
     * positions onto one [canonical] memo key, shrinking the explored space by up to
     * the symmetry order (8x for the fully D4-symmetric 9x9 Wiegleb board). Sound for
     * first-solution search: symmetric positions have symmetric solution sets.
     */
    private fun symmetryPermutations(spec: BoardSpec): List<IntArray> {
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
}

/**
 * A memory-lean, growable open-addressing `Long` hash set for the search memo.
 *
 * `HashSet<Long>` boxes every key (~72 bytes/entry), exhausting the heap after a few
 * million positions. This `long[]` with linear probing stores ~16 bytes/entry (incl.
 * the 0.5 load factor), so the search can memoise tens of millions of distinct
 * positions. A board's `pegs` mask is never 0 during a search (a solved board has one
 * peg and is returned before insertion), so 0 is a safe empty-slot sentinel.
 */
private class VisitedSet {
    private var slots = LongArray(INITIAL_CAPACITY)
    private var mask = INITIAL_CAPACITY - 1
    private var growThreshold = (INITIAL_CAPACITY * LOAD_FACTOR).toInt()

    var size: Int = 0
        private set

    /** Adds [key]; returns `true` if newly inserted, `false` if already present. */
    fun add(key: Long): Boolean {
        var index = (mix(key) and mask.toLong()).toInt()
        while (true) {
            val current = slots[index]
            if (current == EMPTY) {
                slots[index] = key
                size++
                if (size >= growThreshold) grow()
                return true
            }
            if (current == key) return false
            index = (index + 1) and mask
        }
    }

    private fun grow() {
        val old = slots
        val newCapacity = old.size * GROWTH_FACTOR
        slots = LongArray(newCapacity)
        mask = newCapacity - 1
        growThreshold = (newCapacity * LOAD_FACTOR).toInt()
        for (key in old) {
            if (key != EMPTY) reinsert(key)
        }
    }

    private fun reinsert(key: Long) {
        var index = (mix(key) and mask.toLong()).toInt()
        while (slots[index] != EMPTY) {
            index = (index + 1) and mask
        }
        slots[index] = key
    }

    /** Fibonacci-style mixing so dense `pegs` masks spread across the table. */
    private fun mix(key: Long): Long {
        var hash = key * -0x61c8864680b583ebL
        hash = hash xor (hash ushr HASH_SHIFT)
        return hash and Long.MAX_VALUE
    }

    private companion object {
        const val EMPTY = 0L
        const val INITIAL_CAPACITY = 1 shl 20
        const val GROWTH_FACTOR = 2
        const val LOAD_FACTOR = 0.5
        const val HASH_SHIFT = 32
    }
}
