package com.app.pegsolitaire.domain.usecase

import com.app.pegsolitaire.domain.assist.OpeningBook
import com.app.pegsolitaire.domain.generator.BoardCanonical
import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Behaviour spec for the pure [GetHintUseCase] hint decision (ASST-01 / ASST-03).
 *
 * Magic numbers (board coordinates, level numbers, prefix lengths) are intentional
 * and acceptable in test code — they ARE the specification of the sampled positions.
 * Challenge fixtures come from the deterministic [LevelGenerator] (each generated
 * level carries a recorded winning `solution`); a small hand-built strip supplies the
 * Classic (solution == null), already-decided, and provably-illegal cases.
 *
 * The two hard guarantees under test:
 *  - **recorded-line-first**: while a non-null `solution` covers `movesPlayed`, the
 *    recorded move is returned WITHOUT ever entering the Solver (proven by a
 *    `shouldCancel` spy that the Solver would have polled had it run);
 *  - **legality guard**: the recorded branch is taken only when that move is a member
 *    of `RulesEngine.legalMoves(board)` — a stale/illegal index falls through to the
 *    Solver rather than being returned blind (Pitfall 5).
 */
class GetHintUseCaseTest {
    private val useCase = GetHintUseCase()

    @Test
    fun onRecordedLineReturnsTheRecordedMoveWithoutInvokingTheSolver() {
        val level = LevelGenerator.generate(LEVEL_WITH_RECORDED_LINE)
        var solverPolled = 0

        val move =
            useCase(
                board = level.startingBoard,
                solution = level.solution,
                movesPlayed = 0,
                // If the Solver ran it would poll this ~every 4096 nodes; a `true`
                // return would also force it to null. A non-null recorded result with
                // zero polls proves the Solver branch was never entered.
                shouldCancel = {
                    solverPolled++
                    true
                },
            )

        assertThat(move).isEqualTo(level.solution[0])
        assertThat(solverPolled).isEqualTo(0)
        assertThat(RulesEngine.legalMoves(level.startingBoard)).contains(move)
    }

    @Test
    fun afterReplayingKRecordedMovesReturnsSolutionAtK() {
        val level = LevelGenerator.generate(LONGER_RECORDED_LINE)
        val k = level.solution.size / 2
        val board =
            level.solution.take(k).fold(level.startingBoard) { b, m -> RulesEngine.applyMove(b, m) }

        val move = useCase(board = board, solution = level.solution, movesPlayed = k)

        assertThat(move).isEqualTo(level.solution[k])
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    @Test
    fun classicNullSolutionAlwaysUsesTheSolverAndReturnsALegalMove() {
        val board = solvableStrip()

        val move = useCase(board = board, solution = null, movesPlayed = 0)

        assertThat(move).isNotNull()
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    @Test
    fun movesPlayedPastTheRecordedLineEndFallsThroughToTheSolver() {
        val level = LevelGenerator.generate(SHORT_LEVEL)

        val move =
            useCase(
                board = level.startingBoard,
                solution = level.solution,
                movesPlayed = level.solution.size,
            )

        assertThat(move).isNotNull()
        assertThat(RulesEngine.legalMoves(level.startingBoard)).contains(move)
    }

    @Test
    fun anIllegalRecordedMoveIsRejectedByTheLegalityGuardAndTheSolverIsUsed() {
        val board = solvableStrip()
        // A fabricated move whose `from` is the empty hole — never a member of
        // legalMoves(board); the guard must reject it and defer to the Solver.
        val bogus =
            Move(
                from = Position(row = 0, col = 2),
                over = Position(row = 0, col = 1),
                to = Position(row = 0, col = 0),
            )

        val move = useCase(board = board, solution = listOf(bogus), movesPlayed = 0)

        assertThat(move).isNotEqualTo(bogus)
        assertThat(move).isNotNull()
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    /**
     * **The DEBT-01 headline case.** Wiegleb-45's start is the shipped board whose one-peg
     * line is a committed fixture — it is provably winnable — yet RESEARCH measured
     * **0 of 76** of its depth-≤4 positions resolving inside the 1.5s deadline even at
     * ~15M nodes/s, the start included. Before the book was wired in, asking for a hint
     * here produced the "no winning move" rescue banner on a position that has a proven
     * winning move: a flat lie, and the defect this requirement exists to remove.
     *
     * The spy is the load-bearing part. `shouldCancel` returning `true` would force the
     * Solver to `null`, so a NON-NULL result with ZERO polls is a *proof* that the Solver
     * branch was never entered — not an inference from timing.
     */
    @Test
    fun theWiegleb45StartIsAnsweredFromTheBookWithoutTheSolverEverRunning() {
        val board = Boards.wiegleb45Initial
        var solverPolled = 0

        val move =
            useCase(
                board = board,
                solution = null,
                movesPlayed = 0,
                shouldCancel = {
                    solverPolled++
                    true
                },
            )

        assertThat(move).isNotNull()
        assertThat(solverPolled).isEqualTo(0)
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    @Test
    fun aSolverReturningNullYieldsNull() {
        // Two pegs separated by an empty hole: no legal jump, unsolvable → Solver null.
        val spec = BoardSpec(inPlay = (0..2).map { Position(row = 0, col = it) }, target = null)
        val board =
            Board(spec = spec, pegs = 0L)
                .withPeg(Position(row = 0, col = 0))
                .withPeg(Position(row = 0, col = 2))

        val move = useCase(board = board, solution = null, movesPlayed = 0)

        assertThat(move).isNull()
    }

    /**
     * Every shipped Classic board's opening hint is a book lookup, not a search.
     *
     * The starts are where a hint is most likely to be asked for and — on the larger
     * footprints — where the solver is least able to answer. Diamond-41's start alone was
     * measured right ON the 1.5s deadline (~1181 ms) before the book, so it answered
     * truthfully only by luck of the machine.
     */
    @Test
    fun everyShippedClassicStartIsAnsweredFromTheBookWithoutTheSolverEverRunning() {
        CLASSIC_STARTS.forEach { (name, board) ->
            val spy = SolverSpy()

            val move = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

            assertWithMessage("'%s' start must be book-served", name).that(move).isNotNull()
            assertWithMessage("'%s' start must not enter the solver", name).that(spy.polls).isEqualTo(0)
            assertWithMessage("'%s' start hint must be legal on the board shown", name)
                .that(RulesEngine.legalMoves(board))
                .contains(move)
        }
    }

    /**
     * **Pitfall 1's mitigation, asserted along its whole length.** Diamond-41's proven
     * floor is two pegs, which makes the pagoda prune provably vacuous there — so an
     * OFF-book Diamond hint launches a real, unpruned search rather than being rejected
     * cheaply. The committed floor line's positions are therefore exactly the ones that
     * must never reach the solver, and every prefix of it is checked here rather than a
     * sample of them.
     */
    @Test
    fun everyPositionOnTheCommittedDiamondFloorLineIsBookServed() {
        val fixture = SolutionFixtures.ALL.first { it.name == DIAMOND_41 }
        var board = startBoardOf(fixture)

        RecordedSolutions.load(DIAMOND_41).forEachIndexed { ply, recorded ->
            val spy = SolverSpy()
            val here = board

            val move = useCase(board = here, solution = null, movesPlayed = 0, shouldCancel = spy)

            assertWithMessage("diamond41 floor-line ply %s must be book-served", ply).that(move).isNotNull()
            assertWithMessage("diamond41 floor-line ply %s must not enter the solver", ply)
                .that(spy.polls)
                .isEqualTo(0)
            assertWithMessage("diamond41 floor-line ply %s hint must be legal", ply)
                .that(RulesEngine.legalMoves(here))
                .contains(move)
            board = RulesEngine.applyMove(here, recorded)
        }
    }

    /**
     * The DEBT-01 frontier claim on the board whose book is complete: a bounded
     * ENUMERATION of every position reachable within [BOOK_PLIES] moves of European-37's
     * start, not a sample of them.
     *
     * RESEARCH measured 312 canonical positions there at K≤4, so the raw set is bounded
     * in the low thousands and every answer is a map hit — which is what keeps an
     * exhaustive assertion affordable in the per-commit gate at all. Before the book,
     * this same frontier cost 158.6 seconds and left 57 positions unresolved.
     */
    @Test
    fun everyEuropean37PositionWithinFourMovesIsBookServed() {
        val fixture = SolutionFixtures.ALL.first { it.name == EUROPEAN_37 }
        var examined = 0

        layersOf(fixture, BOOK_PLIES).forEachIndexed { ply, layer ->
            layer.forEach { pegs ->
                val board = Board(spec = fixture.spec, pegs = pegs)
                val spy = SolverSpy()

                val move = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

                assertWithMessage("european37 ply %s position %s must be book-served", ply, pegs.toHex())
                    .that(move)
                    .isNotNull()
                assertWithMessage("european37 ply %s position %s must not enter the solver", ply, pegs.toHex())
                    .that(spy.polls)
                    .isEqualTo(0)
                assertWithMessage("european37 ply %s position %s hint must be legal", ply, pegs.toHex())
                    .that(RulesEngine.legalMoves(board))
                    .contains(move)
                examined++
            }
        }

        // Guards against a vacuous pass: an enumeration that silently produced nothing (or
        // a handful) would satisfy every assertion in the loop above. RESEARCH measured
        // this frontier's canonical size independently at 312, so checking against that
        // number — rather than against one derived from this very enumeration — is a
        // genuine external check that the whole frontier was walked.
        val tables = BoardCanonical.chunkTables(fixture.spec)
        val canonical =
            layersOf(fixture, BOOK_PLIES).flatten().mapTo(HashSet()) { BoardCanonical.canonicalFast(it, tables) }
        assertWithMessage("european37's canonical depth-%s frontier (RESEARCH's measurement)", BOOK_PLIES)
            .that(canonical.size)
            .isEqualTo(EUROPEAN_37_CANONICAL_AT_DEPTH_4)
        assertWithMessage("every raw depth-%s position must have been examined", BOOK_PLIES)
            .that(examined)
            .isEqualTo(layersOf(fixture, BOOK_PLIES).sumOf { it.size })
    }

    /**
     * **Without this test, a bug that returned `null` for every miss would look identical
     * to a working book.** Every other test here asserts the solver did NOT run, so a
     * pipeline that answered "no winning move" the moment the book missed would pass all
     * of them and ship the exact untruthful banner DEBT-01 exists to remove.
     *
     * The position is walked six plies down a deterministic path, past the book's depth-4
     * frontier, and its off-book status is ASSERTED as a precondition rather than assumed —
     * a committed-line prefix is covered at any depth, so "deeper than four" alone would
     * not establish it.
     */
    @Test
    fun anOffBookClassicPositionStillReachesTheSolver() {
        val board = offBookPositionAtDepth(OFF_BOOK_PLIES)
        val spy = SolverSpy(cancel = true)

        assertWithMessage("the depth-%s position must be a book MISS", OFF_BOOK_PLIES)
            .that(OpeningBook.lookup(board))
            .isNull()

        useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

        assertWithMessage("a book miss must fall through to the solver, not answer null itself")
            .that(spy.polls)
            .isGreaterThan(0)
    }

    /**
     * The solver-never-ran spy, shared by every book test.
     *
     * Returning `true` from `shouldCancel` would force [Solver][
     * com.app.pegsolitaire.domain.solver.Solver] to `null`, so a NON-NULL result with
     * ZERO polls is a *proof* that the solver branch was never entered rather than an
     * inference from how fast the call returned. [cancel] is flipped to `false` only where
     * the solver is expected to run and the poll count is the assertion.
     */
    private class SolverSpy(
        private val cancel: Boolean = true,
    ) : () -> Boolean {
        var polls = 0
            private set

        override fun invoke(): Boolean {
            polls++
            return cancel
        }
    }

    private fun startBoardOf(fixture: SolutionFixtures.Fixture): Board =
        fixture.spec.inPlay
            .fold(Board(spec = fixture.spec, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(fixture.startVacancy)

    /**
     * The raw masks reachable in exactly 0, 1, ... [plies] moves, expanded through the
     * unchanged [RulesEngine] rather than a private re-derivation of the jump rule.
     */
    private fun layersOf(
        fixture: SolutionFixtures.Fixture,
        plies: Int,
    ): List<Set<Long>> {
        val layers = ArrayList<Set<Long>>()
        val start = startBoardOf(fixture).pegs
        val seen = hashSetOf(start)
        var layer: Set<Long> = setOf(start)
        layers.add(layer)
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
            layers.add(layer)
        }
        return layers
    }

    /**
     * Walks [plies] moves down English-33 taking the LAST legal move at each step — a
     * deterministic path chosen to diverge from the committed golden line, whose prefixes
     * are book-covered at every depth.
     */
    private fun offBookPositionAtDepth(plies: Int): Board {
        val fixture = SolutionFixtures.ALL.first { it.name == ENGLISH_33 }
        var board = startBoardOf(fixture)
        repeat(plies) {
            board = RulesEngine.applyMove(board, RulesEngine.legalMoves(board).last())
        }
        return board
    }

    private fun Long.toHex(): String = java.lang.Long.toHexString(this)

    /** A 1x3 strip (pegs at cols 0,1; hole at col 2): exactly one legal move → solvable to one peg. */
    private fun solvableStrip(): Board =
        Board(spec = BoardSpec(inPlay = (0..2).map { Position(row = 0, col = it) }, target = null), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))

    private companion object {
        const val LEVEL_WITH_RECORDED_LINE = 3
        const val LONGER_RECORDED_LINE = 10
        const val SHORT_LEVEL = 1

        const val ENGLISH_33 = "english33"
        const val EUROPEAN_37 = "european37"
        const val DIAMOND_41 = "diamond41"

        /** The depth European-37's committed book covers — the frontier enumerated exhaustively. */
        const val BOOK_PLIES = 4

        /** Two plies past the deepest frontier any committed book holds. */
        const val OFF_BOOK_PLIES = 6

        /**
         * RESEARCH's independently-measured canonical depth-≤4 frontier size for
         * European-37. Written out on purpose: deriving it from the enumeration it is
         * meant to verify would make the check vacuous.
         */
        const val EUROPEAN_37_CANONICAL_AT_DEPTH_4 = 312

        /** The five shipped Classic starts, by the slug their book is committed under. */
        val CLASSIC_STARTS =
            listOf(
                ENGLISH_33 to Boards.english33Initial,
                EUROPEAN_37 to Boards.european37Initial,
                "asymmetric39" to Boards.asymmetric39Initial,
                DIAMOND_41 to Boards.diamond41Initial,
                "wiegleb45" to Boards.wiegleb45Initial,
            )
    }
}
