package com.app.pegsolitaire.domain.golden.solutions

import com.app.pegsolitaire.domain.assist.OpeningBookFormat
import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.rules.RulesEngine
import java.io.File

/**
 * One-off AUTHORING-TIME generator for the committed depth-[BOOK_DEPTH] opening book
 * (DEBT-01).
 *
 * Confined to the `:domain` **test** source set and never on a shipped or runtime
 * classpath — `DomainPurityTest` is the structural gate and
 * `grep -rn 'OpeningBookAuthoring' domain/src/main` returns nothing. It runs only under
 * `UPDATE_GOLDEN=true`, exactly like [AuthoringSolver], so a normal `:domain:test` run
 * performs zero search.
 *
 * **Why a book exists at all.** RESEARCH measured the strengthened solver from plan
 * 08-05 resolving 0 of 76 Wiegleb-45 depth-≤4 positions inside the 1.5 s player-facing
 * deadline, including depth 0 — the shipped start whose one-peg line is already a
 * committed fixture. The gap is roughly four orders of magnitude, so no constant-factor
 * improvement closes it. But the deadline binds only the *player*: the authoring budget
 * here is unbounded, and a book entry validated by replaying a complete winning line
 * through the unchanged [RulesEngine] is correct by construction. Precompute the answers
 * the runtime cannot reach.
 *
 * **What is proven, and what is not.** Every emitted entry is a *witness*: a first move
 * that begins a line this tool replayed, step by step, through
 * [RulesEngine.legalMoves]/[RulesEngine.applyMove] down to the board's own
 * `spec.minimumPegs`. A position this tool could NOT decide is reported and fails the
 * run — it is never written as an authoritative "no winning move". Both searches used
 * here ([BeamSearchAuthoring] and [AuthoringSolver]) are incomplete at the budgets they
 * are given, so neither can prove unsolvability, and recording an unproven `-` would
 * reintroduce precisely the untruthful banner DEBT-01 exists to remove.
 */
internal object OpeningBookAuthoring {
    /**
     * Plies of player deviation the book covers, inclusive.
     *
     * K = 4, not 5 or 6. The depth-≤4 frontier is 51 / 312 / 233 / 107 / 76 CANONICAL
     * positions across English-33 / European-37 / Asymmetric-39 / Diamond-41 /
     * Wiegleb-45 — roughly 2 800 entries stored as RAW masks and ~100 KB of text. Raw
     * masks are the point: a raw mask is exactly what a player's board holds, so lookup
     * needs no canonicalisation and, crucially, no symmetry-INVERSE mapping to transport
     * a move back out of canonical space. K = 6 is 14 566 canonical positions, which
     * makes raw storage impractical, forces that inverse machinery, and adds meaningful
     * APK weight.
     *
     * Revisit trigger: playtesting showing players routinely deviate past move 4 and
     * then hit a stale "no winning move" banner.
     */
    const val BOOK_DEPTH = 4

    /** Where the committed books live — `:domain` MAIN resources, because the decoder ships. */
    const val DIRECTORY = "src/main/resources/golden/book"

    /**
     * Generates and writes one book per fixture in [SolutionFixtures.ALL], returning the
     * report Task 3's blocking checkpoint reviews.
     *
     * Fails — loudly, and without emitting that board's file — if any position could not
     * be decided. RESEARCH assumption A2 ("every Wiegleb-45 position at depth ≤ K is
     * actually winnable") is unverified, and this is where it gets verified.
     */
    fun writeBook(bookDir: File): BookReport {
        bookDir.mkdirs()
        val report = BookReport(SolutionFixtures.ALL.map { fixture -> BoardAuthor(fixture).author(bookDir) })
        println(report.render())
        check(report.totalUndecided == 0) { report.undecidedFailure() }
        return report
    }
}

/** One board's generation outcome — the row Task 3's checkpoint reads. */
internal data class BoardReport(
    val board: String,
    val target: Int,
    val frontierSize: Int,
    val canonicalFrontierSize: Int,
    val prefixEntries: Int,
    val totalEntries: Int,
    val bytes: Int,
    val undecided: List<Long>,
    val wallClockMs: Long,
)

/** The whole generation run, per board plus totals. */
internal data class BookReport(
    val boards: List<BoardReport>,
) {
    val totalEntries: Int get() = boards.sumOf { it.totalEntries }
    val totalBytes: Int get() = boards.sumOf { it.bytes }
    val totalUndecided: Int get() = boards.sumOf { it.undecided.size }
    val totalWallClockMs: Long get() = boards.sumOf { it.wallClockMs }

    /** A readable per-board table, printed by the generator and copied into the SUMMARY. */
    fun render(): String {
        val header =
            "| board | target | frontier (raw) | frontier (canonical) | prefix entries | " +
                "total entries | bytes | undecided | wall clock |"
        val rule = "|---|---|---|---|---|---|---|---|---|"
        val rows =
            boards.map { row ->
                "| ${row.board} | ${row.target} | ${row.frontierSize} | ${row.canonicalFrontierSize} | " +
                    "${row.prefixEntries} | ${row.totalEntries} | ${row.bytes} | ${row.undecided.size} | " +
                    "${row.wallClockMs} ms |"
            }
        val totals =
            "| **total** | — | — | — | — | $totalEntries | $totalBytes | $totalUndecided | $totalWallClockMs ms |"
        return (listOf("", "BOOK REPORT (k=${OpeningBookAuthoring.BOOK_DEPTH})", header, rule) + rows + totals)
            .joinToString("\n")
    }

    /** Names every board, depth and mask that stayed undecided, so A2's break is diagnosable. */
    fun undecidedFailure(): String {
        val depth = OpeningBookAuthoring.BOOK_DEPTH
        val detail =
            boards.filter { it.undecided.isNotEmpty() }.joinToString(separator = "; ") { row ->
                val masks =
                    row.undecided.joinToString(",", limit = UNDECIDED_MASKS_SHOWN) {
                        java.lang.Long.toHexString(it)
                    }
                "${row.board} has ${row.undecided.size} undecided (masks: $masks)"
            }
        return "Undecided positions at depth <= $depth: $detail. No 'no winning move' marker " +
            "was emitted: neither search here can prove unsolvability."
    }

    private companion object {
        const val UNDECIDED_MASKS_SHOWN = 12
    }
}

/**
 * Generates one board's book.
 *
 * The whole design turns on a single invariant held by [winningMove]: **every key is a
 * position proven to reach [target], and following the chain of recorded moves from any
 * key arrives there.** That makes three things fall out for free:
 *  - a position whose SUCCESSOR is already a key is decided without any search, which is
 *    what makes the shallow layers cost nothing once the deepest layer is done;
 *  - a short bounded search that merely reaches an existing key ([stitch]) decides a
 *    position, so the map accelerates itself as it grows — the dominant saving on
 *    Wiegleb-45, where a full solve costs tens of seconds;
 *  - validation is a single replay of the chain through [RulesEngine], which is the step
 *    that turns a witness into a proof.
 *
 * Entries are only ever added, never overwritten, so a chain that has already been shown
 * to reach the target cannot be broken by a later, shorter path through the same
 * position.
 */
private class BoardAuthor(
    private val fixture: SolutionFixtures.Fixture,
) {
    private val spec = fixture.spec
    private val target = spec.minimumPegs
    private val start: Board =
        spec.inPlay
            .fold(Board(spec = spec, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(fixture.startVacancy)

    /** Raw peg mask -> a proven move toward [target]. See the class KDoc's invariant. */
    private val winningMove = HashMap<Long, Move>()

    private val undecided = ArrayList<Long>()

    fun author(bookDir: File): BoardReport {
        val started = System.nanoTime()
        val layers = enumerateLayers()
        val frontier = layers.flatten()
        val canonicalSize = canonicalSizeOf(frontier)
        verifyFrontier(canonicalSize)
        val prefixes = seedFromCommittedLine()
        // Deepest first: every shallower layer's positions then have a decided successor
        // and cost no search at all.
        layers.reversed().forEach { layer -> layer.forEach(::decide) }
        val entries = collectEntries(frontier + prefixes)
        val bytes = if (undecided.isEmpty()) emit(bookDir, entries) else 0
        return BoardReport(
            board = fixture.name,
            target = target,
            frontierSize = frontier.size,
            canonicalFrontierSize = canonicalSize,
            prefixEntries = prefixes.count { it !in frontier.toSet() },
            totalEntries = entries.size,
            bytes = bytes,
            undecided = undecided.toList(),
            wallClockMs = (System.nanoTime() - started) / NANOS_PER_MILLI,
        )
    }

    /**
     * The raw masks reachable in exactly 0, 1, ... [OpeningBookAuthoring.BOOK_DEPTH]
     * moves, expanded through [RulesEngine] rather than a private re-derivation of the
     * jump rule. Masks are kept RAW and deduplicated per layer.
     */
    private fun enumerateLayers(): List<List<Long>> {
        val layers = ArrayList<List<Long>>()
        val seen = linkedSetOf(start.pegs)
        var layer = listOf(start.pegs)
        layers.add(layer)
        repeat(OpeningBookAuthoring.BOOK_DEPTH) {
            val next = LinkedHashSet<Long>()
            layer.forEach { pegs ->
                val board = Board(spec = spec, pegs = pegs)
                RulesEngine.legalMoves(board).forEach { move ->
                    val child = RulesEngine.applyMove(board, move).pegs
                    if (seen.add(child)) next.add(child)
                }
            }
            layer = next.toList()
            layers.add(layer)
        }
        return layers
    }

    private fun canonicalSizeOf(frontier: List<Long>): Int {
        val tables = BoardCanonical.chunkTables(spec)
        return frontier.mapTo(HashSet()) { BoardCanonical.canonicalFast(it, tables) }.size
    }

    /**
     * Cross-checks the enumeration against RESEARCH's independently-measured canonical
     * depth table. That table is an external verification of this BFS — its English-33
     * row reproduces the published depth sequence for the English central game — so a
     * mismatch means the enumeration is wrong, not that the table is stale.
     */
    private fun verifyFrontier(canonicalSize: Int) {
        val expected = CANONICAL_FRONTIER_SIZE.getValue(fixture.name)
        check(canonicalSize == expected) {
            "Frontier enumeration for '${fixture.name}' at k=${OpeningBookAuthoring.BOOK_DEPTH} produced " +
                "$canonicalSize canonical positions, but RESEARCH measured $expected."
        }
    }

    /**
     * Seeds [winningMove] with every prefix state of the board's committed golden line
     * and returns those masks.
     *
     * These answers are already proven — `SolutionTest` replays the same line through
     * [RulesEngine] on every CI run — so they cost nothing, they deliver CONTEXT's
     * "Classic recorded-line fast path" structurally as book entries keyed by mask, and
     * they give the chain its first anchor into the endgame.
     */
    private fun seedFromCommittedLine(): List<Long> {
        val masks = ArrayList<Long>()
        var board = start
        RecordedSolutions.load(fixture.name).forEach { move ->
            masks.add(board.pegs)
            winningMove.putIfAbsent(board.pegs, move)
            board = RulesEngine.applyMove(board, move)
        }
        return masks
    }

    /** Records a proven answer for [mask], or files it as undecided. */
    private fun decide(mask: Long) {
        if (mask !in winningMove) {
            findPath(Board(spec = spec, pegs = mask))?.let { path -> record(mask, path) }
        }
        if (validatedFirstMove(mask) == null) undecided.add(mask)
    }

    /**
     * A path from [board] that ends either at [target] or at a position already in
     * [winningMove] — the cheap stitch first, then a real solve.
     */
    private fun findPath(board: Board): List<Move>? =
        stitch(board) ?: beamAt(board, BEAM_WIDTHS) ?: forwardSolveIfOnePeg(board)

    /**
     * A short breadth-first probe for a position already known to reach [target].
     *
     * This is where the run's cost actually collapses. Every solved line adds its whole
     * trajectory to [winningMove], and sibling positions in the same layer share a great
     * many successors, so after the first handful of real solves most of a layer is
     * decided by a probe measured in milliseconds instead of a beam measured in seconds.
     */
    private fun stitch(board: Board): List<Move>? {
        val seen = hashSetOf(board.pegs)
        var frontier = listOf(PathNode(pegs = board.pegs, move = null, parent = null))
        var hit: PathNode? = null
        var ply = 0
        while (hit == null && ply < STITCH_PLIES && frontier.isNotEmpty()) {
            val next = ArrayList<PathNode>()
            frontier.forEach { node -> if (hit == null) hit = expandForStitch(node, seen, next) }
            frontier = if (next.size > STITCH_WIDTH) next.subList(0, STITCH_WIDTH) else next
            ply++
        }
        return hit?.pathFromRoot()
    }

    /** Expands [node], appending unseen children to [out] and returning the first known hit. */
    private fun expandForStitch(
        node: PathNode,
        seen: MutableSet<Long>,
        out: MutableList<PathNode>,
    ): PathNode? {
        val board = Board(spec = spec, pegs = node.pegs)
        var hit: PathNode? = null
        RulesEngine.legalMoves(board).forEach { move ->
            val childPegs = RulesEngine.applyMove(board, move).pegs
            if (hit == null && seen.add(childPegs)) {
                val child = PathNode(pegs = childPegs, move = move, parent = node)
                if (childPegs in winningMove || java.lang.Long.bitCount(childPegs) <= target) {
                    hit = child
                } else {
                    out.add(child)
                }
            }
        }
        return hit
    }

    /** The first of [widths] that yields a full line to [target], or `null`. */
    private fun beamAt(
        board: Board,
        widths: List<Int>,
    ): List<Move>? {
        var line: List<Move>? = null
        var index = 0
        while (line == null && index < widths.size) {
            line = BeamSearchAuthoring.beamSolve(board, targetPegs = target, width = widths[index])
            index++
        }
        return line
    }

    /**
     * The jittered DFS, which is only defined for a one-peg target and is the LAST
     * resort rather than the first.
     *
     * Measured on a single board's depth-4 layer it is wildly heavy-tailed — 53 ms,
     * 61 ms, 14 s, 90 s on four European-37 positions, and it does not return at all
     * within minutes on Wiegleb-45, the one board the book exists for. The beam is
     * slower at its median and far better behaved at its tail, so it goes first; the DFS
     * is kept because it does crack positions the beam abandons.
     */
    private fun forwardSolveIfOnePeg(board: Board): List<Move>? =
        if (target == BoardConstants.MINIMUM_PEGS_DEFAULT) AuthoringSolver.forwardSolve(board) else null

    /** Adds every position along [path] from [mask], never overwriting a proven answer. */
    private fun record(
        mask: Long,
        path: List<Move>,
    ) {
        var board = Board(spec = spec, pegs = mask)
        path.forEach { move ->
            winningMove.putIfAbsent(board.pegs, move)
            board = RulesEngine.applyMove(board, move)
        }
    }

    /**
     * [mask]'s recorded first move, but **only** once the COMPLETE chain from it has been
     * replayed through the unchanged [RulesEngine] — every step asserted to be a member
     * of [RulesEngine.legalMoves] on the board it acts upon, and the terminal peg count
     * asserted to equal the board's own [target]. This replay is what makes a book hit a
     * proof rather than a guess (threat T-08-25).
     */
    private fun validatedFirstMove(mask: Long): Move? {
        var board = Board(spec = spec, pegs = mask)
        var valid = true
        var steps = 0
        while (valid && board.pegCount > target) {
            val move = winningMove[board.pegs]
            if (move != null && steps < spec.size && move in RulesEngine.legalMoves(board)) {
                board = RulesEngine.applyMove(board, move)
                steps++
            } else {
                valid = false
            }
        }
        return if (valid && board.pegCount == target) winningMove[mask] else null
    }

    /** The validated entries for [masks], in a deterministic ascending-mask order. */
    private fun collectEntries(masks: List<Long>): Map<Long, Move> {
        val entries = LinkedHashMap<Long, Move>()
        masks
            .distinct()
            .sortedWith { left, right -> java.lang.Long.compareUnsigned(left, right) }
            .forEach { mask -> validatedFirstMove(mask)?.let { move -> entries[mask] = move } }
        return entries
    }

    /** Writes `{name}-k{K}.txt` and returns its size in bytes. */
    private fun emit(
        bookDir: File,
        entries: Map<Long, Move>,
    ): Int {
        val header =
            "$COMMENT_PREFIX ${fixture.name} k=${OpeningBookAuthoring.BOOK_DEPTH} " +
                "target=$target entries=${entries.size}"
        val text =
            (listOf(header) + entries.map { (mask, move) -> OpeningBookFormat.encodeEntry(mask, move) })
                .joinToString(separator = LINE_SEPARATOR, postfix = LINE_SEPARATOR)
        val file = File(bookDir, "${fixture.name}$FILE_INFIX${OpeningBookAuthoring.BOOK_DEPTH}$FILE_SUFFIX")
        file.writeText(text)
        return text.toByteArray().size
    }

    /** A node in the [stitch] probe: a mask plus the move and parent that reached it. */
    private class PathNode(
        val pegs: Long,
        val move: Move?,
        val parent: PathNode?,
    ) {
        fun pathFromRoot(): List<Move> {
            val reversed = ArrayList<Move>()
            var current: PathNode? = this
            while (current != null) {
                val move = current.move ?: break
                reversed.add(move)
                current = current.parent
            }
            reversed.reverse()
            return reversed
        }
    }

    private companion object {
        /**
         * RESEARCH's independently-measured canonical depth-≤4 frontier sizes. Hard-coded
         * on purpose: they are an EXTERNAL check on [enumerateLayers], so deriving them
         * from the same BFS they verify would make the check vacuous.
         */
        val CANONICAL_FRONTIER_SIZE =
            mapOf(
                "english33" to 51,
                "european37" to 312,
                "asymmetric39" to 233,
                "diamond41" to 107,
                "wiegleb45" to 76,
            )

        /**
         * Escalating beam widths, tried in order until one returns a line.
         *
         * Escalation rather than a single wide beam because the cost is roughly linear in
         * width and most positions fall to the narrowest: measured on depth-4 layers,
         * width 500 costs ~250 ms and already decides European-37 and Asymmetric-39
         * outright, while Wiegleb-45 needs 32 000 or more. Paying the widest beam for
         * every position would multiply the run by two orders of magnitude.
         */
        val BEAM_WIDTHS = listOf(500, 4_000, 32_000, 256_000)

        const val STITCH_PLIES = 7
        const val STITCH_WIDTH = 20_000
        const val COMMENT_PREFIX = "#"
        const val LINE_SEPARATOR = "\n"
        const val FILE_INFIX = "-k"
        const val FILE_SUFFIX = ".txt"
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
