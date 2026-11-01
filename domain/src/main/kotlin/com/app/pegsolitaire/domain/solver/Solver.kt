package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Move
import kotlin.random.Random

/**
 * The production, pure-`:domain` winning-move engine (ASST-01 / ASST-03).
 *
 * [decide] answers one of three [SolverOutcome]s (GD-HT-1): [SolverOutcome.Found], a legal
 * move that begins a line reducing an arbitrary mid-game [Board] to its board's
 * [BoardSpec.minimumPegs] target; [SolverOutcome.ProvenDead], when the position is proved
 * to have no such line; or [SolverOutcome.Undecided], when the search stopped — cancelled,
 * or out of budget — without proving either. D-04 used to fold the last two into one
 * `null`. It powers the Challenge deviation hint and every Classic hint, and only its
 * ProvenDead may answer the "no winning move" state.
 *
 * The search is a memoised, symmetry-canonical depth-first search whose move authority
 * is [SolverTables] — precomputed FROM
 * [RulesEngine][com.app.pegsolitaire.domain.rules.RulesEngine] and proven set-equal to
 * it by `SolverTablesTest`, so a hint is still always a legal, reachable move
 * (threat T-04-CT). It is guarded by a [Pagoda] colour-parity fast-reject, a per-attempt
 * node cap ([SolverBudget.perAttemptMaxNodes]), a cumulative ceiling
 * ([SolverBudget.maxNodes]) that bounds worst-case time AND memory, and a cooperative
 * `shouldCancel` poll (no coroutine dependency crosses into `:domain`).
 *
 * **The goal is per-board, not a hardcoded one peg.** `pegCount <= spec.minimumPegs`
 * is what lets Diamond-41 (proven floor two, DEBT-02) get a truthful hint at all. The
 * `<=` form makes a forged sub-target dangerous — a `minimumPegs <= 0` would make the
 * goal test trivially true and every hint permanently ProvenDead — so two guards are
 * load-bearing here and must not be removed: [BoardSpec] validates
 * `minimumPegs in 1..size` at construction, and `SnapshotMapper` fail-closes a forged
 * resume blob before a spec is ever constructed from it (threat T-08-13).
 *
 * **Allocation-free hot loop.** A node is three mask operations and no allocation:
 * `pegs and needMask[m] == needMask[m] && pegs and toMask[m] == 0L`, then
 * `pegs xor xorMask[m]`. The previous loop allocated a `List<Move>` of data-class
 * objects, a `sortedByDescending` with a fresh `Random.nextDouble()` per move, and a
 * `Board` copy per move — measured at ~0.27M nodes/s against ~15M for the tuned
 * reference, a ~55x gap that was pure allocation rather than algorithm.
 *
 * **Deterministic restarts (D-04 completeness), and the policy defect this fixes.** A
 * SINGLE move ordering can be "unlucky": it dives into a huge live-but-fruitless subtree
 * and burns the whole deadline before surfacing a solution. Peg-solitaire DFS runtime is
 * heavy-tailed, and the textbook response is many short restarts. The previous code
 * handed EVERY attempt the full [SolverBudget.maxNodes], so in production the 1.5s
 * deadline always fired inside attempt 0 and [MAX_ATTEMPTS] was unreachable dead code —
 * exactly one unlucky dive, then "no winning move". Each attempt now carries
 * [SolverBudget.perAttemptMaxNodes] instead, so restarts genuinely happen inside the
 * unchanged deadline (measured: European-37's start 172ms as one unbounded attempt
 * versus 33ms across two capped ones). The seeds are FIXED, so the whole engine stays
 * deterministic (same board ⇒ same move) and pure (`kotlin.random.Random` is
 * Android-free).
 *
 * **The proof is preserved.** An attempt that fully exhausts the reachable forward
 * subtree WITHOUT hitting its cap proves no solution exists (reordering moves changes
 * the visit order but never reachability), so the search stops and returns
 * [SolverOutcome.ProvenDead] immediately — restarts are only spent when an attempt is
 * CUT SHORT by its cap, and a search whose every attempt was cut short proved nothing.
 *
 * **What this rewrite does NOT fix.** Wiegleb-45's early positions remain outside the
 * 1.5s deadline by roughly four orders of magnitude: RESEARCH measured 0 of 76 of its
 * depth-≤4 positions resolving in 1.5s even at 15M nodes/s. No amount of solver
 * strengthening closes that. The committed opening book (plans 08-08 through 08-10) is
 * what makes those hints truthful, and it is load-bearing rather than an optimisation.
 */
object Solver {
    /** Fixed base seed for the per-attempt move ordering — keeps the restarts reproducible. */
    private const val ATTEMPT_SEED_BASE = 0x5150_2602L

    /** Deterministic restart budget: distinct orderings tried before giving up undecided. */
    private const val MAX_ATTEMPTS = 40

    /**
     * Factor by which each restart's node cap grows over the previous attempt's.
     *
     * A FLAT cap is not a usable restart schedule, and this is measured rather than
     * theoretical: Wiegleb-45's mid-line position needs a single 830k-position dive,
     * which it completes in ~269ms — but under a flat 200k cap none of the 40 orderings
     * can reach it, so the solver burns ~2.1s and gives up undecided on a
     * position it can actually decide well inside the deadline. That is the classic
     * heavy-tail result (Luby/Sinclair/Zuckerman): a restart strategy whose run lengths
     * are bounded can never solve instances whose solution lies past the bound. Doubling
     * keeps the cheap early restarts that fix European-37 and Asymmetric-39 while still
     * reaching the deep dives, and [SolverBudget.maxNodes] remains the cumulative
     * ceiling that terminates the escalation.
     */
    private const val ATTEMPT_CAP_GROWTH = 2L

    /**
     * How many times the attempt cap may double before it stops growing.
     *
     * **This bound is what actually bounds MEMORY.** The memo ([VisitedSet]) is per
     * ATTEMPT and is discarded between restarts, so peak memory tracks the largest single
     * attempt, never [SolverBudget.maxNodes]. Letting the escalation run all the way to
     * that cumulative ceiling means a 40M-entry memo — a 1 GB `long[]`, and ~1.5 GB while
     * it doubles — which is a heap exhaustion on a phone rather than a bounded worst case.
     * Observed: a Diamond-41 search with no caller deadline and an unbounded escalation
     * dies in `VisitedSet.grow` with `OutOfMemoryError`.
     *
     * Four doublings puts the largest attempt at 16x [SolverBudget.perAttemptMaxNodes]
     * (3.2M positions, a ~67 MB memo) — comfortably above the deepest dive the corpus
     * needs (Wiegleb-45's mid-line position at ~830k) and above what the 1.5s deadline can
     * afford at the measured node rate anyway, so the bound costs nothing in production
     * while restoring the memory guarantee (threat T-08-14).
     */
    private const val MAX_CAP_ESCALATIONS = 4

    /** Distinct (symmetry-canonical) positions explored by the deciding search attempt (diagnostics). */
    var lastVisited: Long = 0
        private set

    /**
     * What a search from [board] proves (GD-HT-1): [SolverOutcome.Found] with the first
     * move of a winning line; [SolverOutcome.ProvenDead] when [board] is already at its
     * target, the [Pagoda] condition rejects it, or an attempt exhausted its whole forward
     * subtree; [SolverOutcome.Undecided] for every stop that proved nothing — the cancel
     * latch, [MAX_ATTEMPTS] spent, or the cumulative [SolverBudget.maxNodes] reached
     * (including a budget so small that no attempt runs at all).
     *
     * A board at its target is ProvenDead to keep the old behaviour: a reachable board at
     * its target has no legal move to give. It is unreachable in play, because a Won
     * board routes to Results before a hint can be asked for.
     *
     * The solver reports what it proved, never why it stopped: a deadline and a
     * superseded request set the same latch, and both are Undecided here. Telling them
     * apart is the caller's job, and the caller already does — a superseded search is
     * never observed.
     *
     * @param shouldCancel polled cooperatively (~every 4096 nodes); returning `true`
     *   abandons the search, which then yields [SolverOutcome.Undecided]. Kept a plain
     *   lambda so no coroutine type crosses the domain boundary (QUAL-01 purity).
     */
    fun decide(
        board: Board,
        budget: SolverBudget,
        shouldCancel: () -> Boolean = { false },
    ): SolverOutcome {
        if (board.pegCount <= board.spec.minimumPegs || !Pagoda.mayBeSolvable(board)) {
            lastVisited = 0
            return SolverOutcome.ProvenDead
        }
        return searchWithRestarts(board, budget, shouldCancel)
    }

    /**
     * Runs up to [MAX_ATTEMPTS] deterministically-seeded searches, the first capped at
     * [SolverBudget.perAttemptMaxNodes] and each subsequent one at
     * [ATTEMPT_CAP_GROWTH] times its predecessor, returning the first winning move found.
     *
     * Stops when a move is found, the caller cancelled, unsolvability is proven (an
     * attempt exhausted its subtree without hitting its cap), [MAX_ATTEMPTS] is spent,
     * or the CUMULATIVE node total reaches [SolverBudget.maxNodes] — the safety ceiling
     * that keeps the worst case bounded no matter how far the schedule escalates. A found
     * move and a proof are the only answers; every other stop is
     * [SolverOutcome.Undecided].
     */
    private fun searchWithRestarts(
        board: Board,
        budget: SolverBudget,
        shouldCancel: () -> Boolean,
    ): SolverOutcome {
        val tables = SolverTables.forSpec(board.spec)
        var result: Move? = null
        var provenUnsolvable = false
        var attempt = 0
        var totalVisited = 0L
        var attemptCap = minOf(budget.perAttemptMaxNodes, budget.maxNodes)
        while (attempt < MAX_ATTEMPTS && totalVisited < budget.maxNodes) {
            val cap = minOf(attemptCap, budget.maxNodes - totalVisited)
            val search = WinningLineSearch(board.spec, tables, cap, shouldCancel, ATTEMPT_SEED_BASE + attempt)
            val line = search.solveFrom(board)
            lastVisited = search.visited
            totalVisited += search.visited
            result = line?.firstOrNull()
            provenUnsolvable = line == null && !search.cancelled && !search.hitBudget
            if (result != null || search.cancelled || provenUnsolvable) break
            if (attempt < MAX_CAP_ESCALATIONS) attemptCap = escalate(attemptCap, budget.maxNodes)
            attempt++
        }
        return when {
            result != null -> SolverOutcome.Found(result)
            provenUnsolvable -> SolverOutcome.ProvenDead
            else -> SolverOutcome.Undecided
        }
    }

    /** The next attempt's cap: [current] grown by [ATTEMPT_CAP_GROWTH], never past [ceiling]. */
    private fun escalate(
        current: Long,
        ceiling: Long,
    ): Long = if (current >= ceiling / ATTEMPT_CAP_GROWTH) ceiling else current * ATTEMPT_CAP_GROWTH
}

/**
 * A single memoised, symmetry-canonical DFS over one board footprint's [SolverTables] —
 * one instance per restart attempt. Holds the search's mutable state (the visited memo,
 * node counter, cancel/budget latches, the played-move path buffer) and this attempt's
 * move ordering, so the recursion itself stays a small allocation-free method.
 *
 * The recursion carries a `Long` peg mask and its peg count rather than a [Board]: a
 * child position is `pegs xor tables.xorMask[m]`, which allocates nothing, where
 * `RulesEngine.applyMove` copied a data class per move. The winning path is held as move
 * INDICES in a single [IntArray] allocated once per attempt, and is converted back to
 * `List<Move>` only when a solution is actually found — never inside the loop.
 *
 * Memo keys come from [BoardCanonical.canonicalFast]: symmetric positions collapse
 * onto one key, shrinking the explored space by up to the symmetry order (8x for the
 * fully D4-symmetric 9x9 Wiegleb board, 2x for Asymmetric-39). Sound for first-solution
 * search — symmetric positions have symmetric solution sets, so if one is a dead end so
 * are its images.
 *
 * @property cancelled set once [shouldCancel] returned `true` — the caller's deadline.
 * @property hitBudget set once [nodeCap] was reached — the attempt was cut short, so a
 *   `null` result is inconclusive (retry a different seed at a larger cap) rather than
 *   a proof.
 */
private class WinningLineSearch(
    spec: BoardSpec,
    private val tables: SolverTables,
    private val nodeCap: Long,
    private val shouldCancel: () -> Boolean,
    seed: Long,
) {
    /** The board's own perfect-finish peg count — the per-board goal, never a hardcoded 1. */
    private val target: Int = spec.minimumPegs

    /**
     * The played-move indices, one slot per ply. A forward move removes exactly one peg,
     * so no line from a `spec.size`-cell footprint can be longer than `spec.size`.
     * Allocated once per ATTEMPT, not per node.
     */
    private val path = IntArray(spec.size)

    private val order: IntArray = reorderedForThisAttempt(Random(seed))
    private val seen = VisitedSet()
    private var nodeCounter = 0L
    private var solutionDepth = 0

    var cancelled = false
        private set
    var hitBudget = false
        private set

    /** Distinct (symmetry-canonical) positions explored — the budget-headroom diagnostic. */
    val visited: Long get() = seen.size.toLong()

    /**
     * The winning line from [board] (its first move begins a solve to the board's
     * target), or `null` if the forward subtree is exhausted within the attempt's cap,
     * or the attempt is budget-missed / cancelled. Goal check runs BEFORE the memo
     * insert.
     */
    fun solveFrom(board: Board): List<Move>? {
        val solved = search(board.pegs, board.pegCount, depth = 0)
        return if (solved) List(solutionDepth) { ply -> tables.moveAt(path[ply]) } else null
    }

    private fun search(
        pegs: Long,
        pegCount: Int,
        depth: Int,
    ): Boolean {
        if (pegCount <= target) {
            solutionDepth = depth
            return true
        }
        var solved = false
        val withinBudget = seen.size < nodeCap
        if (!withinBudget) hitBudget = true
        // Pagoda prune: skip positions whose colour parity already forbids the board's
        // target — a sound necessary-condition cut, evaluated here against the tables
        // this attempt already holds so no per-node cache lookup or lock is paid.
        val explorable = !cancelled && withinBudget && Pagoda.mayBeSolvable(pegs, pegCount, target, tables)
        if (explorable && seen.add(BoardCanonical.canonicalFast(pegs, tables.chunkTables))) {
            solved = expand(pegs, pegCount, depth)
        }
        return solved
    }

    /**
     * Tries every legal move of [pegs] in this attempt's ordering. A move is legal when
     * its two source cells hold pegs and its destination is empty — three mask tests and
     * one xor, with no [Board] or `List<Move>` allocated anywhere in the loop.
     */
    private fun expand(
        pegs: Long,
        pegCount: Int,
        depth: Int,
    ): Boolean {
        var solved = false
        var index = 0
        while (index < order.size && !solved && !cancelled) {
            val move = order[index]
            index++
            if (isLegal(pegs, move)) {
                pollCancellation()
                if (!cancelled) {
                    path[depth] = move
                    solved = search(pegs xor tables.xorMask[move], pegCount - 1, depth + 1)
                }
            }
        }
        return solved
    }

    /** Whether move [move] applies to [pegs]: both source cells hold pegs, the destination is empty. */
    private fun isLegal(
        pegs: Long,
        move: Int,
    ): Boolean {
        val need = tables.needMask[move]
        return pegs and need == need && pegs and tables.toMask[move] == 0L
    }

    /**
     * Polls the caller's cancel lambda once every `CANCEL_MASK + 1` expansions — frequent
     * enough to stay responsive to the deadline, rare enough to cost nothing against the
     * search itself.
     */
    private fun pollCancellation() {
        if ((nodeCounter++ and SolverBudget.CANCEL_MASK) == 0L && shouldCancel()) {
            cancelled = true
        }
    }

    /**
     * This attempt's move ordering: [SolverTables.moveOrder] (the inwardness-sorted base
     * order, computed once per footprint) put through a seeded bounded-displacement
     * shuffle.
     *
     * The property being preserved from the previous per-node
     * `inwardness + random * INWARD_JITTER` sort is that **each restart explores a
     * genuinely different near-inward ordering** — not merely a tie-break reshuffle —
     * while keeping the strong inward bias that collapses a cross board's arms toward
     * its centre and surfaces a solution early. Because the base order is already sorted
     * by inwardness, a swap bounded to [ORDER_JITTER_WINDOW] positions can only exchange
     * moves of near-equal inwardness, which is exactly what the jittered sort did. It is
     * computed ONCE per attempt rather than once per node, which is where the old sort's
     * allocation went.
     *
     * The [Random] is seeded from a FIXED per-attempt seed, so the whole engine stays
     * deterministic: the same board under the same budget always yields the same move.
     */
    private fun reorderedForThisAttempt(random: Random): IntArray {
        val reordered = tables.moveOrder.copyOf()
        for (index in reordered.indices) {
            val window = minOf(ORDER_JITTER_WINDOW, reordered.size - index)
            val swapWith = index + random.nextInt(window)
            val held = reordered[index]
            reordered[index] = reordered[swapWith]
            reordered[swapWith] = held
        }
        return reordered
    }

    private companion object {
        /**
         * How far a move may be displaced from its inwardness-sorted position by the
         * per-attempt shuffle. Sized to reshuffle roughly-equal moves — the role
         * `INWARD_JITTER = 4.0` played in the old per-node sort — while keeping the
         * strong inward bias intact. This is a restart-DIVERSITY knob: widening it makes
         * attempts differ more but weakens the heuristic; narrowing it makes every
         * attempt look like the base order, which is the single-dive behaviour the
         * restart loop exists to avoid.
         */
        const val ORDER_JITTER_WINDOW = 8
    }
}

/**
 * A memory-lean, growable open-addressing `Long` hash set for the search memo.
 *
 * `HashSet<Long>` boxes every key (~72 bytes/entry), exhausting the heap after a few
 * million positions. This `long[]` with linear probing stores ~16 bytes/entry (incl.
 * the 0.5 load factor), so the search can memoise tens of millions of distinct
 * positions. An inserted key is never 0: the goal test returns before the insert
 * whenever `pegCount <= target`, and `BoardSpec` requires `minimumPegs >= 1`, so every
 * memoised position holds at least two pegs and its canonical mask is non-zero. 0 is
 * therefore a safe empty-slot sentinel.
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
