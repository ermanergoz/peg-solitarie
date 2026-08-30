package com.app.pegsolitaire.domain.golden.solutions

import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.golden.BoardCodec
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import java.io.File
import kotlin.random.Random

/**
 * One-off AUTHORING-TIME solver that GENERATES the committed recorded-solution
 * fixtures (D-19). Confined to the `:domain` **test** source set and never on any
 * shipped/runtime classpath, so the "no Solver in `:domain` main" scope (and
 * `DomainPurityTest`) holds.
 *
 * [forwardSolve] is a memoized DFS that folds a single-vacancy start down to one
 * peg, with three devices that together solve the four cross/octagon catalog boards
 * (English-33, European-37, Asymmetric-39 and the 45-hole Wiegleb cross):
 *  - a memory-lean [VisitedSet] transposition memo, keyed on the **symmetry-
 *    canonical** [Board.pegs] (the smallest mask under the footprint's dihedral
 *    group, via the shared [BoardCanonical]) so symmetric positions collapse onto
 *    one key — up to an 8x reduction that makes the 9x9 Wiegleb board reachable;
 *  - an **inward** move ordering (collapse pegs toward the footprint centre, the
 *    textbook way to solve cross boards) with per-restart random **jitter**, so a
 *    short solution surfaces early and independent restarts explore different
 *    near-inward orderings;
 *  - a per-attempt distinct-position cap with seeded restarts, abandoning an unlucky
 *    ordering so the next seed can retry.
 *
 * The 41-hole Diamond-41 is NOT solved here: its target is two pegs, not one, and
 * this DFS is built around a one-peg goal and an inward heuristic tuned for cross
 * boards. Its fixture is authored by [BeamSearchAuthoring] instead, which
 * [writeFixtures] routes to for any board whose target is above one peg or which
 * this DFS fails to crack. Fixtures are emitted in our exact `(row,col)`/[Move]
 * encoding by construction (Pitfall 7); a fixed seed sequence keeps regenerated
 * fixtures byte-stable.
 */
object AuthoringSolver {
    /** Lone peg count that marks a fully-solved board (D-02: any final peg counts). */
    private const val SOLVED_PEG_COUNT = 1

    /** Fixed base seed so the randomized tie-breaking is fully reproducible (byte-stable fixtures). */
    private const val BASE_SEED = 0x5150_2602L

    /** Independent randomized attempts (fresh seed + memo each) before giving up. */
    private const val MAX_RESTARTS = 30

    /**
     * Per-attempt ceiling on distinct (symmetry-canonical) positions. The 9x9
     * Wiegleb board's full space is far larger, so a single attempt is NOT
     * exhaustive; instead the inward heuristic + jittered restarts find one of its
     * (short, 43-jump) solutions early — the committed run reached it in ~4.5M
     * positions — and the cap abandons an unlucky ordering so the next seed retries.
     * The lean memo keeps this within the authoring heap.
     */
    private const val MAX_DISTINCT_PER_ATTEMPT = 25_000_000L

    /**
     * Random jitter added to the inward score so independent restarts explore
     * genuinely different near-inward orderings (not just tie-break shuffles). Sized
     * to the inward score's scale so it reshuffles roughly-equal moves while keeping
     * the strong inward bias that makes a cross-board solution surface early.
     */
    private const val INWARD_JITTER = 4.0

    /** Distinct positions explored by the most recent successful attempt (diagnostics). */
    var lastVisited: Long = 0
        private set

    /**
     * Folds the single-vacancy [start] down to one peg, or `null` if no attempt
     * succeeded within the restart budget.
     *
     * A correct memoized DFS (no unsound pruning) over symmetry-canonical positions,
     * with an **inward** move ordering plus per-restart [INWARD_JITTER] so a solution
     * surfaces early and unlucky orderings retry. The lean memo lets a single attempt
     * span up to [MAX_DISTINCT_PER_ATTEMPT] canonical positions within the heap.
     */
    fun forwardSolve(start: Board): List<Move>? {
        val canonicalTables = BoardCanonical.chunkTables(start.spec)
        val center = centroidOf(start.spec)
        for (attempt in 0 until MAX_RESTARTS) {
            val random = Random(BASE_SEED + attempt)
            val seen = VisitedSet()

            fun search(
                board: Board,
                path: ArrayDeque<Move>,
            ): List<Move>? {
                // Goal check FIRST (before the memo): a one-peg board must always return its
                // winning line, even if a symmetric position was already memoized as visited.
                if (board.pegCount == SOLVED_PEG_COUNT) return path.toList()
                var solution: List<Move>? = null
                val withinBudget = seen.size < MAX_DISTINCT_PER_ATTEMPT
                if (withinBudget && seen.add(BoardCanonical.canonicalFast(board.pegs, canonicalTables))) {
                    val ordered =
                        RulesEngine
                            .legalMoves(board)
                            .sortedByDescending { inwardness(it, center) + random.nextDouble() * INWARD_JITTER }
                    for (move in ordered) {
                        path.addLast(move)
                        solution = search(RulesEngine.applyMove(board, move), path)
                        path.removeLast()
                        if (solution != null) break
                    }
                }
                return solution
            }

            val result = search(start, ArrayDeque())
            if (result != null) {
                lastVisited = seen.size.toLong()
                return result
            }
        }
        return null
    }

    /** The mean `(row, col)` of the footprint — the attractor the inward heuristic collapses toward. */
    private fun centroidOf(spec: BoardSpec): DoubleArray =
        doubleArrayOf(
            spec.inPlay.sumOf { it.row }.toDouble() / spec.size,
            spec.inPlay.sumOf { it.col }.toDouble() / spec.size,
        )

    /**
     * Inwardness of a [move]: how much closer to [center] the peg lands than it
     * started (squared-distance of `from` minus that of `to`). Preferring inward
     * jumps collapses the cross arms toward the centre — the textbook way to solve
     * cross-shaped boards — so a short solution surfaces far sooner than under a
     * neutral ordering.
     */
    private fun inwardness(
        move: Move,
        center: DoubleArray,
    ): Double = distanceSquared(move.from, center) - distanceSquared(move.to, center)

    private fun distanceSquared(
        position: Position,
        center: DoubleArray,
    ): Double {
        val dr = position.row - center[0]
        val dc = position.col - center[1]
        return dr * dr + dc * dc
    }

    /**
     * Solves each board in [SolutionFixtures.ALL] from its committed start down to
     * **that board's own** `spec.minimumPegs` and writes the jump list
     * (`<name>.txt`, [BoardCodec.encodeMoves] text) into [solutionsDir]. Throws if
     * any board fails to generate within the search budget.
     *
     * The per-board target is read from [BoardSpec.minimumPegs] at the point of use
     * rather than duplicated onto [SolutionFixtures.Fixture], so the board catalogue
     * stays the single source of truth for what a "perfect finish" is.
     */
    fun writeFixtures(solutionsDir: File) {
        solutionsDir.mkdirs()
        SolutionFixtures.ALL.forEach { fixture ->
            val start =
                fixture.spec.inPlay
                    .fold(Board(spec = fixture.spec, pegs = 0L)) { board, position -> board.withPeg(position) }
                    .withoutPeg(fixture.startVacancy)
            val target = fixture.spec.minimumPegs
            val line =
                requireNotNull(solveToTarget(start, target)) {
                    "Board '${fixture.name}' did not solve to $target peg(s) from ${fixture.startVacancy} " +
                        "within the search budget (dfsVisited=$lastVisited, " +
                        "beamExpanded=${BeamSearchAuthoring.lastExpanded})."
                }
            File(solutionsDir, fixture.fileName).writeText(BoardCodec.encodeMoves(line))
        }
    }

    /**
     * A line from [start] down to [target] pegs, preferring the DFS and falling back
     * to the beam.
     *
     * [forwardSolve] is a one-peg search, so it is only consulted when that is the
     * target; when it exhausts its restart budget without a line, [BeamSearchAuthoring]
     * takes over — measured, the jittered DFS gave up on Wiegleb-45 after 865M nodes
     * and 57 s where the beam solved it. Every other target goes straight to the beam.
     * Both return a witness that `SolutionTest` re-validates step by step, so neither
     * is trusted on its own word.
     */
    private fun solveToTarget(
        start: Board,
        target: Int,
    ): List<Move>? {
        if (target == SOLVED_PEG_COUNT) {
            forwardSolve(start)?.let { return it }
        }
        return BeamSearchAuthoring.beamSolve(start, targetPegs = target)
    }
}

/**
 * The catalog boards whose best achievable finish is locked by a committed
 * recorded-solution fixture (D-19). [Fixture.startVacancy] pins each board's
 * documented start so the fixture replays from the same `Boards.*Initial` the
 * shipped test folds, and each board is solved to its own
 * [BoardSpec.minimumPegs] rather than to a single hardcoded target.
 *
 * **Diamond-41 IS in this catalog, and its target is two pegs — not one.** The GF(4)
 * colour invariant (sum of omega^(r+c) and omega^(r-c) over occupied cells, both
 * conserved by every jump) admits no single-peg target from the diamond's canonical
 * center start: verified exhaustively over all 41 single-peg positions, zero of which
 * pass (`DiamondFloorOptimalityTest`). Its fewest-pegs floor is exactly
 * **F = 2** ([BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR]) — the earlier "several
 * pegs" estimate came from random rollouts, which plateau at five because rollouts
 * are far too weak a search to find a floor. `diamond41.txt` is the committed line
 * that reaches two, so DEBT-02's reachability half is a replayed proof rather than a
 * best-known result. The diamond is scored on fewest pegs remaining (the original
 * game's objective), with its bands anchored on F.
 */
internal object SolutionFixtures {
    /** Subdirectory (under `src/test/resources/golden`) holding the committed fixtures. */
    const val DIRECTORY = "src/test/resources/golden/solutions"

    private const val FIXTURE_SUFFIX = ".txt"

    /** One board's fixture recipe: solve [spec] from the single-vacancy [startVacancy]. */
    internal data class Fixture(
        val name: String,
        val spec: BoardSpec,
        val startVacancy: Position,
    ) {
        val fileName: String get() = name + FIXTURE_SUFFIX
    }

    val ALL: List<Fixture> =
        listOf(
            Fixture(name = "english33", spec = Boards.English33, startVacancy = Position(row = 3, col = 3)),
            Fixture(name = "european37", spec = Boards.European37, startVacancy = Position(row = 3, col = 2)),
            Fixture(name = "asymmetric39", spec = Boards.Asymmetric39, startVacancy = Position(row = 4, col = 3)),
            Fixture(name = "diamond41", spec = Boards.Diamond41, startVacancy = Position(row = 4, col = 4)),
            Fixture(name = "wiegleb45", spec = Boards.Wiegleb45, startVacancy = Position(row = 4, col = 4)),
        )

    /**
     * The subset of [ALL] whose boards genuinely finish on ONE peg.
     *
     * Several suites in the solver package are specifications about *one-peg*
     * solvability — `Pagoda.mayBeSolvable` asks whether a single peg is still
     * reachable, and `Solver.firstWinningMove` searches for a one-peg line. Every
     * Diamond-41 position is correctly rejected by that question, so feeding the
     * diamond into those suites would assert the opposite of what the invariant
     * proves. They iterate this list instead; the per-board generalisation of the
     * pruner and the solver lands in plans 08-04 and 08-05, at which point those
     * suites can move back to [ALL].
     */
    val ONE_PEG: List<Fixture> = ALL.filter { it.spec.minimumPegs == BoardConstants.MINIMUM_PEGS_DEFAULT }
}

/**
 * A memory-lean, growable open-addressing `Long` hash set for the search memo.
 *
 * `HashSet<Long>` boxes every key into a map node (~72 bytes/entry), exhausting the
 * heap after a few million positions. This `long[]` with linear probing stores
 * ~16 bytes/entry (incl. the 0.5 load factor), so the search can memoize tens of
 * millions of distinct positions. A board's `pegs` mask is never 0 during a search
 * (a solved board has one peg and is returned before insertion), so 0 is a safe
 * empty-slot sentinel.
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
