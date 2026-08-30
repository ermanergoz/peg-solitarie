package com.app.pegsolitaire.domain.golden.solutions

import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.rules.RulesEngine
import kotlin.random.Random

/**
 * One-off AUTHORING-TIME beam search that GENERATES committed recorded-solution
 * fixtures the jittered DFS in [AuthoringSolver] cannot reach (D-19, DEBT-02).
 *
 * Confined to the `:domain` **test** source set and never on any shipped or runtime
 * classpath — `DomainPurityTest` is the structural gate, and no reference to this
 * object exists under `domain/src/main`.
 *
 * **This search is INCOMPLETE and must never be the production "no winning move"
 * authority.** A beam keeps only [WIDTH] positions per ply and discards the rest, so
 * it can miss a line that exists and it can NEVER prove that no line exists: an
 * empty frontier means "this beam found nothing", not "this board is dead". Only an
 * exhaustive search (the production `Solver`) may answer `null` truthfully. What the
 * beam produces is a *witness* — a concrete move line — and a witness is verified by
 * replaying it through the unchanged [RulesEngine], which is exactly what
 * `SolutionTest` does. That asymmetry is why an incomplete search is sound here and
 * unsound in production.
 *
 * **Algorithm.** Level-synchronous best-first search. At each ply the whole frontier
 * is expanded through [RulesEngine.legalMoves]/[RulesEngine.applyMove] (never a
 * private re-derivation of the rules), survivors are deduplicated by
 * [BoardCanonical.canonicalFast] so a position and its dihedral images count once,
 * and the survivors are ranked by MOBILITY — the number of legal moves the position
 * still has. The top [ELITE_NUMERATOR]/[ELITE_DENOMINATOR] of the beam is kept
 * strictly by mobility and the remainder is sampled from the rest. **The random tail
 * is load-bearing, not decoration:** a pure top-W beam collapses into a single basin
 * and stalls above the floor. Where 50 000 random rollouts plateau at five pegs and
 * the jittered DFS in [AuthoringSolver] does not reach the floor at all, this beam
 * reaches two.
 *
 * **Byte-stability contract (Pitfall 6).** The existing fixture pipeline promises
 * that regenerating under a fixed seed produces byte-identical files. A beam with an
 * unordered frontier plus a random tail would break that, so every source of order
 * here is pinned:
 *  - [RulesEngine.legalMoves] already returns moves sorted by
 *    `(from.row, from.col, to.row, to.col)`, so one node's expansions are ordered;
 *  - the frontier is an ordered `List` and is expanded front to back, so a ply's
 *    candidate order is a deterministic function of the previous ply's order;
 *  - per-ply canonical deduplication uses a [LinkedHashSet], which preserves that
 *    insertion order (a `HashSet` would not);
 *  - ranking uses a **stable** sort on a precomputed mobility field, so equal-mobility
 *    candidates keep their deterministic order;
 *  - all sampling draws from one [Random] seeded with [BASE_SEED], and the number of
 *    draws per ply is itself a deterministic function of the ply.
 *
 * Consequently the emitted line depends only on `(start, targetPegs, width, seed)`.
 * Changing [WIDTH] or [BASE_SEED] changes the fixture, so both are frozen constants
 * rather than call-site literals.
 */
internal object BeamSearchAuthoring {
    /**
     * Positions carried forward per ply.
     *
     * Measured against `Boards.diamond41Initial` through this repository's own
     * [RulesEngine], targeting two pegs: width 2 000 reaches the floor in ~1.5 s,
     * 10 000 in ~6 s and 400 000 in ~277 s — every width tried lands on a 38-move
     * line at a dead end. 60 000 is a 30x margin over the smallest measured success
     * while keeping a full regeneration under a minute, which matters because
     * byte-stability is only a real guarantee if re-running is cheap enough that
     * people actually do it. A board that needs more can be given more: [width] is a
     * parameter and the authoring heap is sized for the beam well past this default.
     *
     * Part of the emitted fixture's identity — changing it changes the line.
     */
    const val WIDTH = 60_000

    /** Fixed seed for the diversity tail, so regenerated fixtures are byte-stable. */
    const val BASE_SEED = 0x0D1A_4041L

    /** Share of the beam kept strictly by mobility: [ELITE_NUMERATOR]/[ELITE_DENOMINATOR]. */
    private const val ELITE_NUMERATOR = 1

    /** Denominator of the elite share — the remaining half is the diversity sample. */
    private const val ELITE_DENOMINATOR = 2

    /** Mobility of a candidate that has not been ranked yet (beam not over capacity). */
    private const val UNSCORED = -1

    /** Positions expanded by the most recent [beamSolve] call (diagnostics). */
    var lastExpanded: Long = 0
        private set

    /**
     * A move line from [start] down to exactly [targetPegs] pegs, or `null` if this
     * beam did not find one.
     *
     * `null` means **"not found by this beam"**, never "unreachable" — see the object
     * KDoc. A returned line is a witness: every move in it is, by construction, a
     * member of [RulesEngine.legalMoves] on the board it acts upon, and `SolutionTest`
     * re-validates that on replay rather than trusting this search.
     */
    fun beamSolve(
        start: Board,
        targetPegs: Int,
        width: Int = WIDTH,
        seed: Long = BASE_SEED,
    ): List<Move>? {
        require(width > 0) { "Beam width must be positive, was $width" }
        require(targetPegs >= 1) { "Target peg count must be at least 1, was $targetPegs" }
        lastExpanded = 0
        if (start.pegCount == targetPegs) return emptyList()
        return descend(start, targetPegs, width, Random(seed))
    }

    /**
     * Ply-by-ply descent from [start] until a frontier board holds [targetPegs] pegs,
     * or `null` when the frontier empties — which means only that this beam found
     * nothing (see the object KDoc), never that the target is unreachable.
     */
    private fun descend(
        start: Board,
        targetPegs: Int,
        width: Int,
        random: Random,
    ): List<Move>? {
        val spec = start.spec
        val tables = BoardCanonical.chunkTables(spec)
        var frontier: List<Node> = listOf(Node(pegs = start.pegs, move = null, parent = null))

        while (frontier.isNotEmpty()) {
            val seen = LinkedHashSet<Long>()
            val candidates = ArrayList<Node>()
            for (node in frontier) {
                val board = Board(spec = spec, pegs = node.pegs)
                for (move in RulesEngine.legalMoves(board)) {
                    lastExpanded++
                    val next = RulesEngine.applyMove(board, move)
                    if (!seen.add(BoardCanonical.canonicalFast(next.pegs, tables))) continue
                    val child = Node(pegs = next.pegs, move = move, parent = node)
                    if (next.pegCount == targetPegs) return child.lineFromRoot()
                    candidates.add(child)
                }
            }
            frontier = survivors(spec, candidates, width, random)
        }
        return null
    }

    /**
     * The at-most-[width] candidates carried into the next ply: the highest-mobility
     * half kept strictly, plus a seeded sample of the rest for diversity. Ranking is
     * skipped entirely when the beam is not over capacity, so the mobility pass (one
     * [RulesEngine.legalMoves] call per candidate) is only paid when it can change the
     * outcome.
     */
    private fun survivors(
        spec: BoardSpec,
        candidates: ArrayList<Node>,
        width: Int,
        random: Random,
    ): List<Node> {
        if (candidates.size <= width) return candidates
        for (candidate in candidates) {
            candidate.mobility = RulesEngine.legalMoves(Board(spec = spec, pegs = candidate.pegs)).size
        }
        // Stable sort on the precomputed field: equal-mobility candidates keep the
        // deterministic expansion order, and the selector is read once per comparison
        // rather than recomputing mobility.
        candidates.sortWith(compareByDescending { it.mobility })
        val eliteCount = width * ELITE_NUMERATOR / ELITE_DENOMINATOR
        // Partial Fisher-Yates over the non-elite region, in place: draws the diversity
        // sample into positions [eliteCount, width) without copying the whole tail.
        for (index in eliteCount until width) {
            val pick = index + random.nextInt(candidates.size - index)
            val held = candidates[index]
            candidates[index] = candidates[pick]
            candidates[pick] = held
        }
        return ArrayList(candidates.subList(0, width))
    }

    /**
     * One position in the beam's search tree, holding only the `pegs` mask plus a
     * parent link rather than a whole path. Millions of nodes are live at once, so
     * per-node cost is the binding memory constraint; sharing ancestors through
     * [parent] keeps the retained set proportional to the surviving tree rather than
     * to `width * depth`.
     */
    private class Node(
        val pegs: Long,
        val move: Move?,
        val parent: Node?,
    ) {
        /** Legal-move count, filled in only when a ply is over capacity and must rank. */
        var mobility: Int = UNSCORED

        /** The move line from the root down to this node, in play order. */
        fun lineFromRoot(): List<Move> {
            val reversed = ArrayList<Move>()
            var current: Node? = this
            while (current != null) {
                val move = current.move ?: break
                reversed.add(move)
                current = current.parent
            }
            reversed.reverse()
            return reversed
        }
    }
}
