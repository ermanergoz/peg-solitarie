package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import kotlin.random.Random

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
 * threat T-04-CT), guarded by a [Pagoda] O(cells) fast-reject, a per-attempt node
 * budget ([SolverBudget.maxNodes]) that bounds worst-case time AND memory, and a
 * cooperative `shouldCancel` poll (no coroutine dependency crosses into `:domain`).
 *
 * **Deterministic restarts (D-04 completeness).** A SINGLE fixed move ordering can be
 * "unlucky" on the largest footprint (Wiegleb-45) — it dives into a huge live-but-
 * fruitless subtree and blows the budget before surfacing a solution (measured: a
 * solvable Wiegleb prefix exceeded 40M nodes under a single deterministic order). So,
 * like the proven authoring solver, the search adds a small jitter to the inward move
 * ordering and RESTARTS with a fresh [Random] seed (up to [MAX_ATTEMPTS]) until a
 * solution surfaces — but the seeds are FIXED, so the whole engine stays deterministic
 * (same board ⇒ same move) and pure (`kotlin.random.Random` is Android-free). A single
 * lucky ordering surfaces a solution in far fewer nodes than the pathological order.
 *
 * **Proving `null` is preserved.** An attempt that fully exhausts the reachable forward
 * subtree WITHOUT hitting the node cap proves no solution exists (jitter reorders but
 * never changes reachability), so the search stops and returns `null` immediately —
 * restarts are only spent when an attempt is CUT SHORT by the budget cap.
 */
object Solver {
    /** Fixed base seed for the per-attempt jitter — keeps the deterministic restarts reproducible. */
    private const val ATTEMPT_SEED_BASE = 0x5150_2602L

    /** Deterministic restart budget: distinct jitter seeds tried before giving up (→ `null`, D-04). */
    private const val MAX_ATTEMPTS = 40

    /** Distinct (symmetry-canonical) positions explored by the deciding search attempt (diagnostics). */
    var lastVisited: Long = 0
        private set

    /**
     * The first move of a winning line from [board], or `null` if [board] is already
     * solved, provably unsolvable (pagoda / fully exhausted), or every attempt is
     * budget-missed / cancelled.
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
        return searchWithRestarts(board, budget, shouldCancel)
    }

    /**
     * Runs up to [MAX_ATTEMPTS] jittered, deterministically-seeded searches, returning
     * the first winning move found. Stops early when an attempt is cancelled (caller
     * deadline) or fully exhausts its subtree within budget (provably unsolvable).
     */
    private fun searchWithRestarts(
        board: Board,
        budget: SolverBudget,
        shouldCancel: () -> Boolean,
    ): Move? {
        var result: Move? = null
        var attempt = 0
        while (attempt < MAX_ATTEMPTS) {
            val search = WinningLineSearch(board.spec, budget, shouldCancel, ATTEMPT_SEED_BASE + attempt)
            val line = search.solveFrom(board)
            lastVisited = search.visited
            result = line?.firstOrNull()
            val provenUnsolvable = line == null && !search.cancelled && !search.hitBudget
            if (result != null || search.cancelled || provenUnsolvable) break
            attempt++
        }
        return result
    }
}

/**
 * A single memoised, symmetry-canonical DFS over one board footprint with a jittered
 * inward move ordering — one instance per attempt. Holds the search's mutable state
 * (the visited memo, node counter, cancel/budget latches) and the footprint-derived
 * immutables (symmetry group, centroid) so the recursion stays a small method.
 *
 * @property cancelled set once [shouldCancel] returned `true` — the caller's deadline.
 * @property hitBudget set once the node cap was reached — the attempt was cut short,
 *   so a `null` result is inconclusive (retry a different seed) rather than a proof.
 */
private class WinningLineSearch(
    spec: BoardSpec,
    private val budget: SolverBudget,
    private val shouldCancel: () -> Boolean,
    seed: Long,
) {
    private val symmetries: List<IntArray> = symmetryPermutations(spec)
    private val center: DoubleArray = centroidOf(spec)
    private val seen = VisitedSet()
    private val random = Random(seed)
    private var nodeCounter = 0L

    var cancelled = false
        private set
    var hitBudget = false
        private set

    /** Distinct (symmetry-canonical) positions explored — the budget-headroom diagnostic. */
    val visited: Long get() = seen.size.toLong()

    /**
     * The winning line from [board] (its first move begins a solve to one peg), or
     * `null` if the forward subtree is exhausted within budget, or the attempt is
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
        if (!withinBudget) hitBudget = true
        // Pagoda prune: skip positions whose colour-parity already forbids one peg — a
        // sound necessary-condition cut that keeps the exhaustive worst case tractable.
        val explorable = !cancelled && withinBudget && Pagoda.mayBeSolvable(current)
        if (explorable && seen.add(canonical(current.pegs))) {
            val ordered =
                RulesEngine
                    .legalMoves(current)
                    .sortedByDescending { inwardness(it) + random.nextDouble() * INWARD_JITTER }
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
     * textbook way to solve cross boards — so a solution surfaces early. A small
     * seeded jitter ([INWARD_JITTER]) reshuffles roughly-equal moves so each restart
     * explores a genuinely different near-inward ordering.
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

    private companion object {
        /**
         * Jitter added to the inward score so each restart explores a genuinely
         * different near-inward ordering (not just a tie-break shuffle). Sized to the
         * inward score's scale — it reshuffles roughly-equal moves while keeping the
         * strong inward bias that surfaces a cross-board solution early.
         */
        const val INWARD_JITTER = 4.0
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
