package com.app.pegsolitaire.domain.golden.solutions

import com.app.pegsolitaire.domain.assist.OpeningBookFormat
import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.solver.Pagoda
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
    val provenDead: Int,
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
    val totalProvenDead: Int get() = boards.sumOf { it.provenDead }
    val totalWallClockMs: Long get() = boards.sumOf { it.wallClockMs }

    /** A readable per-board table, printed by the generator and copied into the SUMMARY. */
    fun render(): String {
        val header =
            "| board | target | frontier (raw) | frontier (canonical) | prefix entries | " +
                "total entries | proven dead | bytes | undecided | wall clock |"
        val rule = "|---|---|---|---|---|---|---|---|---|---|"
        val rows =
            boards.map { row ->
                "| ${row.board} | ${row.target} | ${row.frontierSize} | ${row.canonicalFrontierSize} | " +
                    "${row.prefixEntries} | ${row.totalEntries} | ${row.provenDead} | ${row.bytes} | " +
                    "${row.undecided.size} | ${row.wallClockMs} ms |"
            }
        val totals =
            "| **total** | — | — | — | — | $totalEntries | $totalProvenDead | $totalBytes | " +
                "$totalUndecided | $totalWallClockMs ms |"
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

    /**
     * Masks PROVEN to have no line to [target] — [ExhaustiveSearch] exhausted the whole
     * subtree below them without a cutoff. These get an authoritative `-` entry, which is
     * truthful; a position the beam merely failed on goes to [undecided] instead.
     */
    private val provenDead = HashSet<Long>()

    /** How each position was decided, keyed by stage name — printed as run progress. */
    private val decidedBy = LinkedHashMap<String, Int>()

    private var positionsDone = 0

    fun author(bookDir: File): BoardReport {
        val started = System.nanoTime()
        val layers = enumerateLayers()
        val frontier = layers.flatten()
        val canonicalSize = canonicalSizeOf(frontier)
        verifyFrontier(canonicalSize)
        val prefixes = seedFromCommittedLine()
        println("BOOK ${fixture.name}: frontier=${frontier.size} canonical=$canonicalSize target=$target")
        // Deepest first: every shallower layer's positions then have a decided successor
        // and cost no search at all.
        layers.reversed().forEach { layer -> layer.forEach { mask -> decide(mask, frontier.size, started) } }
        val entries = collectEntries(frontier + prefixes)
        val bytes = if (undecided.isEmpty()) emit(bookDir, entries) else 0
        return BoardReport(
            board = fixture.name,
            target = target,
            frontierSize = frontier.size,
            canonicalFrontierSize = canonicalSize,
            prefixEntries = prefixes.count { it !in frontier.toSet() },
            totalEntries = entries.size,
            provenDead = provenDead.size,
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
    private fun decide(
        mask: Long,
        total: Int,
        started: Long,
    ) {
        if (mask in winningMove || mask in provenDead) {
            tally(STAGE_CACHED)
        } else {
            resolve(Board(spec = spec, pegs = mask), mask)
        }
        if (mask !in provenDead && validatedFirstMove(mask) == null) undecided.add(mask)
        positionsDone++
        if (positionsDone % PROGRESS_EVERY == 0) {
            val elapsed = (System.nanoTime() - started) / NANOS_PER_MILLI
            println(
                "BOOK ${fixture.name}: $positionsDone/$total decided, " +
                    "undecided=${undecided.size}, ${elapsed}ms, by=$decidedBy",
            )
        }
    }

    private fun tally(stage: String) {
        decidedBy[stage] = (decidedBy[stage] ?: 0) + 1
    }

    /**
     * Decides one position, cheapest stage first.
     *
     * The ordering is measured, not assumed. [stitch] and the narrow beams settle the
     * large majority in milliseconds. What is left is either genuinely hard or genuinely
     * DEAD, and those two look identical to a beam — which is exactly why
     * [ExhaustiveSearch] runs before the wide beams rather than after: on a board whose
     * subtree it can exhaust it converts "the beam gave up" into a *proof*, at a fraction
     * of the cost of the wide beams that would have failed anyway.
     */
    private fun resolve(
        board: Board,
        mask: Long,
    ) {
        val quick = stitch(board) ?: beamAt(board, FAST_WIDTHS)
        if (quick != null) {
            record(mask, quick)
        } else {
            resolveHardPosition(board, mask)
        }
    }

    private fun resolveHardPosition(
        board: Board,
        mask: Long,
    ) {
        when (val outcome = ExhaustiveSearch(spec, target, EXHAUSTIVE_CAP).run(board)) {
            is Exhaustive.Solved -> {
                tally(STAGE_EXHAUSTIVE)
                record(mask, outcome.line)
            }

            Exhaustive.ProvenDead -> {
                tally(STAGE_PROVEN_DEAD)
                provenDead.add(mask)
            }

            Exhaustive.Unknown -> beamAt(board, DEEP_WIDTHS)?.let { line -> record(mask, line) }
        }
    }

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
        if (hit != null) tally(STAGE_STITCH)
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
            if (line != null) tally("$STAGE_BEAM${widths[index]}")
            index++
        }
        return line
    }

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

    /**
     * The validated entries for [masks], ascending by unsigned mask so regeneration is
     * byte-stable.
     *
     * A `null` value is the AUTHORITATIVE no-winning-move marker, and it is only ever
     * written for a mask [ExhaustiveSearch] proved dead by exhausting its subtree — never
     * for one a beam merely failed on.
     */
    private fun collectEntries(masks: List<Long>): Map<Long, Move?> {
        val entries = LinkedHashMap<Long, Move?>()
        masks
            .distinct()
            .sortedWith { left, right -> java.lang.Long.compareUnsigned(left, right) }
            .forEach { mask ->
                if (mask in provenDead) {
                    entries[mask] = null
                } else {
                    validatedFirstMove(mask)?.let { move -> entries[mask] = move }
                }
            }
        return entries
    }

    /** Writes `{name}-k{K}.txt` and returns its size in bytes. */
    private fun emit(
        bookDir: File,
        entries: Map<Long, Move?>,
    ): Int {
        val header =
            "$COMMENT_PREFIX ${fixture.name} k=${OpeningBookAuthoring.BOOK_DEPTH} " +
                "target=$target entries=${entries.size} proven-dead=${provenDead.size}"
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
        val FAST_WIDTHS = listOf(500, 4_000)

        /** Tried only after [ExhaustiveSearch] came back Unknown — the genuinely hard tail. */
        val DEEP_WIDTHS = listOf(32_000, 200_000)

        /**
         * Canonical positions [ExhaustiveSearch] may memoise before it gives up and
         * answers Unknown.
         *
         * Sized from the published figure for the English central game: ~23.5M reachable
         * positions, ~3.3M up to its order-8 symmetry. A depth-4 English-33 subtree is a
         * strict subset of that, so 4M is enough to EXHAUST it and turn a beam failure
         * into a proof. Diamond-41 and Wiegleb-45 are ~10^11 positions and will always cut
         * off here — deliberately: the cap is what stops an unprovable board from eating
         * the run, and Unknown is an honest answer where a guess would not be.
         */
        const val EXHAUSTIVE_CAP = 4_000_000

        /**
         * The stitch probe's shape. Deliberately small: a stitch that is going to succeed
         * almost always succeeds within a ply or two, so extra plies and extra width buy
         * nothing on a hit and are pure waste on a miss — and a miss is what every
         * position pays before the beam even starts. Measured: at 7 plies x 20 000 the
         * failed probe alone dominated the run (English-33 had not finished its 373
         * positions in 11 minutes); at 4 x 1 500 it is a few milliseconds.
         */
        const val STITCH_PLIES = 4
        const val STITCH_WIDTH = 1_500
        const val COMMENT_PREFIX = "#"
        const val LINE_SEPARATOR = "\n"
        const val FILE_INFIX = "-k"
        const val FILE_SUFFIX = ".txt"
        const val NANOS_PER_MILLI = 1_000_000L

        /** Positions between progress lines — the run is long enough to need a heartbeat. */
        const val PROGRESS_EVERY = 25

        const val STAGE_CACHED = "cached"
        const val STAGE_STITCH = "stitch"
        const val STAGE_BEAM = "beam"
        const val STAGE_EXHAUSTIVE = "exhaustive"
        const val STAGE_PROVEN_DEAD = "provenDead"
    }
}

/**
 * What a bounded exhaustive search can conclude about one position.
 *
 * The three-way answer is the whole point. [BeamSearchAuthoring] and
 * [AuthoringSolver] both collapse "no line exists" and "I did not find one" into a single
 * `null`, which is precisely the conflation DEBT-01 exists to remove — so neither may
 * ever produce a "no winning move" entry. Separating [ProvenDead] from [Unknown] is what
 * makes an authoritative marker sayable at all.
 */
private sealed interface Exhaustive {
    /** A witness. Still replayed through [RulesEngine] before it is believed. */
    data class Solved(
        val line: List<Move>,
    ) : Exhaustive

    /** The subtree was fully enumerated and holds no line to the target. A proof. */
    data object ProvenDead : Exhaustive

    /** The search hit its cap. Says nothing either way. */
    data object Unknown : Exhaustive
}

/**
 * A complete memoised DFS over symmetry-canonical positions, bounded by a cap on distinct
 * positions, that reports WHETHER IT FINISHED.
 *
 * Two sound devices, and nothing else — an unsound prune here would turn a false
 * [Exhaustive.ProvenDead] into a shipped lie:
 *  - the [Pagoda] colour-parity test, a necessary condition for reaching [target], so a
 *    rejected subtree provably contains no solution;
 *  - a transposition memo keyed on [BoardCanonical.canonicalFast], sound because
 *    symmetric positions have symmetric solution sets — if one is a dead end so are its
 *    images.
 *
 * [exhausted] is the load-bearing bit: it is cleared the first time the cap stops an
 * expansion, anywhere in the tree. Only a search that never once cut off may answer
 * [Exhaustive.ProvenDead].
 */
private class ExhaustiveSearch(
    private val spec: BoardSpec,
    private val target: Int,
    private val cap: Int,
) {
    private val tables = BoardCanonical.chunkTables(spec)
    private val seen = HashSet<Long>()
    private var exhausted = true

    fun run(start: Board): Exhaustive {
        val line = search(start, ArrayDeque())
        return when {
            line != null -> Exhaustive.Solved(line)
            exhausted -> Exhaustive.ProvenDead
            else -> Exhaustive.Unknown
        }
    }

    private fun search(
        board: Board,
        path: ArrayDeque<Move>,
    ): List<Move>? {
        var solution: List<Move>? = null
        if (board.pegCount <= target) {
            solution = path.toList()
        } else if (seen.size >= cap) {
            exhausted = false
        } else if (Pagoda.mayBeSolvable(board) && seen.add(BoardCanonical.canonicalFast(board.pegs, tables))) {
            val moves = RulesEngine.legalMoves(board)
            var index = 0
            while (solution == null && index < moves.size) {
                val move = moves[index]
                path.addLast(move)
                solution = search(RulesEngine.applyMove(board, move), path)
                path.removeLast()
                index++
            }
        }
        return solution
    }
}
