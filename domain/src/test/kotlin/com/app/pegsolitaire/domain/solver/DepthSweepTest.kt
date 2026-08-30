package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.TestFlags
import com.app.pegsolitaire.domain.assist.OpeningBook
import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * DEBT-01's guarantee, asserted EXHAUSTIVELY over the depth-K frontier of all five shipped
 * Classic boards, through the real hint pipeline, inside the real 1.5 s deadline.
 *
 * ## What this sweep claims — and what it deliberately does not
 *
 * D-08 was originally written as a single universal claim: every position within the first
 * four moves of European-37, Asymmetric-39 **and Wiegleb-45** resolves inside the deadline.
 * The committed artifacts do not support that sentence as written, and asserting it here
 * would make this file a lie rather than a proof:
 *  - **Wiegleb-45's book covers depth 3, not 4** (plan 08-08's checkpoint rescope; its
 *    resource is named `wiegleb45-k3.txt` precisely so the artifact cannot misrepresent
 *    its own coverage), and 4 of its 140 in-remit positions are an accepted documented gap.
 *  - **Diamond-41's book is missing 440 of its 818 in-remit positions**, also a developer-
 *    approved documented gap.
 *  - **English-33 ships four positions that are AUTHORITATIVELY dead** — proven by an
 *    exhaustive search that completed — so "returns a non-null move" is false of them by
 *    design, and "no winning move" is the *correct* answer there.
 *
 * So the property asserted is the honest one the artifacts genuinely support, which is
 * also DEBT-01's actual thesis: **the pipeline never claims a move or a "no winning move"
 * it did not prove.** Concretely, for every position in every board's covered frontier:
 *  1. a book HIT is served instantly, with the solver never polled, and its move is legal
 *     on the exact board it was served for;
 *  2. an AUTHORITATIVE "no winning move" is served instantly, with the solver never polled;
 *  3. a book MISS falls through to the solver, and whatever the solver returns within its
 *     deadline is either `null` or a legal move — never a stale or fabricated one.
 *
 * On top of that, the FULL universal claim is asserted where it is true: on every board
 * whose book declares a **zero gap** — English-33, European-37 and Asymmetric-39 — the
 * count of positions that fall through to the solver is exactly **0**. That is D-08 met in
 * full on the boards whose artifacts can carry it, and it is a strictly stronger assertion
 * than a timing measurement, because a book hit is proven at authoring time and the
 * deadline is irrelevant to it.
 *
 * ## Measured (this sweep, post-book)
 *
 * | Board | k | examined | canonical | book-served | authoritative dead | solver fallback | max/call |
 * |-------|---|----------|-----------|-------------|--------------------|-----------------|----------|
 * | English-33 | 4 | 373 | 51 | 369 | 4 | **0** | 9 ms |
 * | European-37 | 4 | 609 | 312 | 609 | 0 | **0** | 2 ms |
 * | Asymmetric-39 | 4 | 461 | 233 | 461 | 0 | **0** | 1 ms |
 * | Diamond-41 | 4 | 785 | 107 | 345 | 0 | 440 | 1511 ms |
 * | Wiegleb-45 | 3 | 101 | 16 | 97 | 0 | 4 | 1500 ms |
 *
 * Against RESEARCH's PRE-book measurement of the same frontiers: European-37 took 158.6 s
 * and left 57 positions unresolved, Asymmetric-39 73.2 s and 26 unresolved, Wiegleb-45
 * 114.0 s and **all 76** unresolved, English-33 10.9 s and 4 unresolved. Those four boards
 * now cost single-digit milliseconds apiece and leave nothing unresolved — English-33's
 * former 4 "unresolved" are precisely its four AUTHORITATIVE dead positions, where "no
 * winning move" was the correct answer all along and is now served as a proof rather than
 * guessed at from a timeout.
 *
 * The fall-through counts are pinned to each book's documented gap EXACTLY, which makes
 * this sweep the runtime restatement of plan 08-08's header claims. Diamond-41's 440 is
 * also the honest cost of that gap made visible: those are positions where a player still
 * waits out the full 1.5 s deadline, and they are what dominates this sweep's ~11 minute
 * wall-clock. Closing that gap is the obvious future win, and this file is the measurement
 * that would prove it closed.
 *
 * ## Why the sweep drives [GetHintUseCase] rather than [Solver]
 *
 * D-08's claim is about what a PLAYER receives, and the book is now part of that answer. A
 * sweep against the raw solver would assert something the product does not do — and would
 * still fail on Wiegleb-45, exactly as RESEARCH measured it failing.
 *
 * ## Why all five boards, when DEBT-01 names three
 *
 * English-33 and Diamond-41 are covered by the same book and cost the same lookups, so
 * including them is nearly free and a regression on them would otherwise go unnoticed —
 * Diamond-41 especially, whose pagoda prune is provably vacuous at a two-peg target, which
 * makes an off-book Diamond hint an unpruned search rather than a cheap rejection.
 *
 * ## Why this is not in the per-commit gate (Pitfall 5)
 *
 * RESEARCH measured the PRE-book K=4 sweep at ~6 minutes across four boards — 158.6 s for
 * European-37 alone — dominated by positions that burned the whole deadline before
 * answering. The phase 3 and phase 4 validation contracts signed off in plan 08-03 both
 * declare `./gradlew :domain:test` the per-commit gate at seconds latency, so a test of
 * that shape landing in this source set would have violated two freshly-signed contracts on
 * the day they were signed. The body is therefore gated on [TestFlags.RUN_SWEEP] and is
 * reachable only through the dedicated `:domain:depthSweep` task, which `check` depends on.
 * Under a normal `:domain:test` this class returns immediately.
 *
 * ## Anti-vacuity (threat T-08-39)
 *
 * A gated sweep's worst failure is passing while examining nothing. Two guards: the
 * examined count is asserted against a frontier total re-derived here by BFS through the
 * unchanged [RulesEngine], and that frontier's canonical size is cross-checked against
 * RESEARCH's independently measured depth table. "0 positions examined, 0 failures" cannot
 * read as a pass.
 */
class DepthSweepTest {
    @Test
    fun everyPositionInEveryBoardsCoveredFrontierIsAnsweredTruthfullyWithinTheDeadline() {
        if (!TestFlags.isEnabled(TestFlags.RUN_SWEEP)) return

        val useCase = GetHintUseCase()
        val reports = SolutionFixtures.ALL.map { fixture -> sweepBoard(useCase, fixture) }

        reports.forEach { report -> println(report.render()) }

        reports.forEach { report ->
            // Anti-vacuity, in three independent directions (T-08-39).
            // 1. The loop body actually ran once per enumerated position.
            assertWithMessage("'%s' must examine its whole re-derived frontier", report.board)
                .that(report.examined)
                .isEqualTo(report.frontierTotal)
            // 2. Every examined position was classified into exactly one bucket, so a
            //    position cannot be silently counted as neither answered nor deferred.
            assertWithMessage("'%s' classification must account for every position", report.board)
                .that(report.served + report.authoritative + report.deferred)
                .isEqualTo(report.examined)
            // 3. The BFS itself is checked against RESEARCH's INDEPENDENTLY measured
            //    canonical depth table — a number this file cannot derive from the
            //    enumeration it is verifying. A truncated walk fails here.
            assertWithMessage("'%s' canonical depth-%s frontier (RESEARCH's measurement)", report.board, report.depth)
                .that(report.canonical)
                .isEqualTo(RESEARCH_CANONICAL.getValue(report.board))
            assertWithMessage(
                "'%s' worst per-call wall-clock (%s ms) must stay inside the shipped envelope",
                report.board,
                report.maxMillis,
            ).that(report.maxMillis)
                .isAtMost(PER_CALL_CAP_MS)
        }

        // The claim, pinned EXACTLY rather than bounded. Measured: the set of positions
        // that fall through to the solver is precisely each board's documented gap — 0 on
        // the three complete books, 440 on Diamond-41, 4 on Wiegleb-45. So on English-33,
        // European-37 and Asymmetric-39 this is D-08's universal claim in full: not one
        // position in the whole depth-4 frontier reaches the solver, where before the book
        // this same frontier left 57 European-37 and 26 Asymmetric-39 positions unresolved.
        //
        // Equality, not `isAtMost`, in BOTH directions. A book that quietly widened its gap
        // fails, and so does one that quietly narrowed it — the latter matters because a gap
        // closing without its header changing would mean the artifact and its own
        // self-description had drifted apart.
        reports.forEach { report ->
            assertWithMessage(
                "'%s' solver-fallback count must equal its documented gap (first fallback: %s)",
                report.board,
                report.firstDeferred,
            ).that(report.deferred)
                .isEqualTo(DOCUMENTED_GAP.getValue(report.board))
            assertWithMessage("'%s' authoritative 'no winning move' count must match its book header", report.board)
                .that(report.authoritative)
                .isEqualTo(PROVEN_DEAD.getValue(report.board))
        }
    }

    /**
     * Sweeps one board's whole covered frontier, classifying every position and timing
     * every call.
     *
     * The per-call deadline is built exactly as `GameViewModel.onHintRequested` builds it —
     * a wall-clock check against [SolverBudget.DEFAULT]'s deadline — so the sweep exercises
     * the shipped envelope rather than a laboratory one.
     */
    private fun sweepBoard(
        useCase: GetHintUseCase,
        fixture: SolutionFixtures.Fixture,
    ): BoardReport {
        val depth = OpeningBook.CATALOG.getValue(fixture.spec.footprint).depth
        val frontier = frontierOf(fixture, depth)
        val deadlineNanos = SolverBudget.DEFAULT.deadline.inWholeNanoseconds
        var examined = 0
        var served = 0
        var authoritative = 0
        var deferred = 0
        var deferredAnswered = 0
        var maxNanos = 0L
        var firstDeferred: String? = null

        frontier.forEach { pegs ->
            examined++
            val board = Board(spec = fixture.spec, pegs = pegs)
            var polls = 0
            val startedAt = System.nanoTime()
            val move =
                useCase(board = board, solution = null, movesPlayed = 0) {
                    polls++
                    System.nanoTime() - startedAt > deadlineNanos
                }
            maxNanos = maxOf(maxNanos, System.nanoTime() - startedAt)

            // Truthfulness, asserted for EVERY position regardless of which path answered:
            // a move the player is shown must be legal on the board the player is looking at.
            if (move != null) {
                assertWithMessage("'%s' position %s was given an ILLEGAL hint %s", fixture.name, pegs.toHex(), move)
                    .that(RulesEngine.legalMoves(board))
                    .contains(move)
            }

            when {
                polls > 0 -> {
                    // A book MISS. The solver's answer within its deadline is what it is —
                    // `null` here means "not found in time", never a proof, and the pipeline
                    // does not dress it up as one. Both outcomes are counted so the report
                    // says how much of each gap the solver still rescues.
                    if (firstDeferred == null) firstDeferred = pegs.toHex()
                    deferred++
                    if (move != null) deferredAnswered++
                }
                move != null -> served++
                // Zero polls AND a null answer: the book proved this position dead.
                else -> authoritative++
            }
        }

        return BoardReport(
            board = fixture.name,
            depth = depth,
            examined = examined,
            frontierTotal = frontier.size,
            canonical = canonicalSizeOf(fixture, frontier),
            served = served,
            authoritative = authoritative,
            deferred = deferred,
            deferredAnswered = deferredAnswered,
            firstDeferred = firstDeferred,
            maxMillis = maxNanos / NANOS_PER_MILLI,
        )
    }

    /** One board's sweep outcome — printed in full so a green run is still informative. */
    private data class BoardReport(
        val board: String,
        val depth: Int,
        val examined: Int,
        val frontierTotal: Int,
        val canonical: Int,
        val served: Int,
        val authoritative: Int,
        val deferred: Int,
        val deferredAnswered: Int,
        val firstDeferred: String?,
        val maxMillis: Long,
    ) {
        fun render(): String =
            "$board k=$depth examined=$examined (canonical=$canonical) " +
                "book-served=$served authoritative-dead=$authoritative " +
                "solver-fallback=$deferred (of which answered=$deferredAnswered) " +
                "maxPerCall=${maxMillis}ms"
    }

    /**
     * The raw masks reachable within [plies] moves of the board's committed start, expanded
     * through the unchanged [RulesEngine] rather than a private re-derivation of the jump
     * rule — and deliberately NOT the authoring tool's enumeration, so an authoring bug
     * cannot agree with its own test.
     */
    private fun frontierOf(
        fixture: SolutionFixtures.Fixture,
        plies: Int,
    ): Set<Long> {
        val start = startBoardOf(fixture).pegs
        val seen = LinkedHashSet<Long>()
        seen.add(start)
        var layer: Set<Long> = setOf(start)
        repeat(plies) {
            val next = LinkedHashSet<Long>()
            layer.forEach { pegs ->
                val board = Board(spec = fixture.spec, pegs = pegs)
                RulesEngine.legalMoves(board).forEach { move ->
                    val child = RulesEngine.applyMove(board, move).pegs
                    if (seen.add(child)) next.add(child)
                }
            }
            layer = next
        }
        return seen
    }

    private fun startBoardOf(fixture: SolutionFixtures.Fixture): Board =
        fixture.spec.inPlay
            .fold(Board(spec = fixture.spec, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(fixture.startVacancy)

    private fun canonicalSizeOf(
        fixture: SolutionFixtures.Fixture,
        frontier: Set<Long>,
    ): Int {
        val tables = BoardCanonical.chunkTables(fixture.spec)
        return frontier.mapTo(HashSet()) { BoardCanonical.canonicalFast(it, tables) }.size
    }

    private fun Long.toHex(): String = java.lang.Long.toHexString(this)

    private companion object {
        /**
         * The boards whose committed books declare a gap of ZERO, and which therefore carry
         * D-08's universal claim in full: not one frontier position may reach the solver.
         *
         * Diamond-41 and Wiegleb-45 are absent on purpose — their documented gaps were
         * approved at plan 08-08's checkpoint, and listing them here would assert something
         * known to be false of the shipped artifacts.
         */
        val DOCUMENTED_GAP =
            mapOf(
                "english33" to 0,
                "european37" to 0,
                "asymmetric39" to 0,
                // Plan 08-08's developer-approved gaps, restated here as a RUNTIME
                // expectation: these are the positions on which a player still gets a real
                // search rather than an instant answer.
                "diamond41" to 440,
                "wiegleb45" to 4,
            )

        /**
         * Positions the book proves DEAD, per each file's own `proven-dead=N` header. Only
         * English-33's authoring run ever exhausted a subtree, so it is the only board that
         * can answer "no winning move" authoritatively.
         */
        val PROVEN_DEAD =
            mapOf(
                "english33" to 4,
                "european37" to 0,
                "asymmetric39" to 0,
                "diamond41" to 0,
                "wiegleb45" to 0,
            )

        /**
         * RESEARCH's independently measured canonical frontier size for each board, at the
         * depth that board's committed book actually covers — 4 everywhere except
         * Wiegleb-45, whose book ships at 3.
         *
         * These are written out rather than computed, on purpose: they are an EXTERNAL
         * check on this file's own BFS, and RESEARCH's English-33 row reproduces the
         * published depth sequence for the English central game. Deriving them from the
         * enumeration they verify would make the check vacuous.
         */
        val RESEARCH_CANONICAL =
            mapOf(
                "english33" to 51,
                "european37" to 312,
                "asymmetric39" to 233,
                "diamond41" to 107,
                "wiegleb45" to 16,
            )

        /**
         * Hard per-call wall-clock guard. A book-served position costs a map lookup; only a
         * gap-board miss reaches the solver, and that is bounded by
         * [SolverBudget.DEFAULT]'s own deadline. The slack over 1.5 s absorbs the poll
         * granularity — the solver checks the clock every 4096 expansions, so it can
         * overshoot by one such interval — plus CI scheduling jitter.
         */
        const val PER_CALL_CAP_MS = 3_000L

        const val NANOS_PER_MILLI = 1_000_000L
    }
}
