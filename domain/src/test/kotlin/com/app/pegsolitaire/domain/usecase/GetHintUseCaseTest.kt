package com.app.pegsolitaire.domain.usecase

import com.app.pegsolitaire.domain.assist.BookAnswer
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
import com.app.pegsolitaire.domain.solver.Pagoda
import com.app.pegsolitaire.domain.solver.Solver
import com.app.pegsolitaire.domain.solver.SolverBudget
import com.app.pegsolitaire.domain.solver.SolverOutcome
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Behaviour spec for the pure [GetHintUseCase] hint decision (ASST-01 / ASST-03, GD-HT-1).
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
 *
 * **And the three outcomes (GD-HT-1).** A hint that reveals no move is
 * [HintOutcome.ProvenDead] only when something proved it — the book's authoritative entry,
 * an exhausted search, or a Pagoda reject — and [HintOutcome.Undecided] whenever the
 * search merely stopped. The positions the device criteria replay (AC-18 to AC-20) are
 * built here from the same explicit jump sequences QA plays, so a sequence that is not
 * what it claims fails here first.
 */
class GetHintUseCaseTest {
    private val useCase = GetHintUseCase()

    @Test
    fun onRecordedLineReturnsTheRecordedMoveWithoutInvokingTheSolver() {
        val level = LevelGenerator.generate(LEVEL_WITH_RECORDED_LINE)
        var solverPolled = 0

        val outcome =
            useCase(
                board = level.startingBoard,
                solution = level.solution,
                movesPlayed = 0,
                // If the Solver ran it would poll this ~every 4096 nodes; a `true`
                // return would also force it to Undecided. The recorded move with
                // zero polls proves the Solver branch was never entered.
                shouldCancel = {
                    solverPolled++
                    true
                },
            )

        assertThat(outcome).isEqualTo(HintOutcome.Found(level.solution[0]))
        assertThat(solverPolled).isEqualTo(0)
        assertThat(RulesEngine.legalMoves(level.startingBoard)).contains(foundMove(outcome, "the recorded move"))
    }

    @Test
    fun afterReplayingKRecordedMovesReturnsSolutionAtK() {
        val level = LevelGenerator.generate(LONGER_RECORDED_LINE)
        val k = level.solution.size / 2
        val board =
            level.solution.take(k).fold(level.startingBoard) { b, m -> RulesEngine.applyMove(b, m) }

        val outcome = useCase(board = board, solution = level.solution, movesPlayed = k)

        assertThat(outcome).isEqualTo(HintOutcome.Found(level.solution[k]))
        assertThat(RulesEngine.legalMoves(board)).contains(foundMove(outcome, "the recorded move"))
    }

    @Test
    fun classicNullSolutionAlwaysUsesTheSolverAndReturnsALegalMove() {
        val board = solvableStrip()

        val outcome = useCase(board = board, solution = null, movesPlayed = 0)

        assertThat(RulesEngine.legalMoves(board)).contains(foundMove(outcome, "the solver's move"))
    }

    @Test
    fun movesPlayedPastTheRecordedLineEndFallsThroughToTheSolver() {
        val level = LevelGenerator.generate(SHORT_LEVEL)

        val outcome =
            useCase(
                board = level.startingBoard,
                solution = level.solution,
                movesPlayed = level.solution.size,
            )

        assertThat(RulesEngine.legalMoves(level.startingBoard)).contains(foundMove(outcome, "the solver's move"))
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

        val outcome = useCase(board = board, solution = listOf(bogus), movesPlayed = 0)

        val move = foundMove(outcome, "the solver's move")
        assertThat(move).isNotEqualTo(bogus)
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
     * Solver to Undecided, so a Found result with ZERO polls is a *proof* that the Solver
     * branch was never entered — not an inference from timing.
     */
    @Test
    fun theWiegleb45StartIsAnsweredFromTheBookWithoutTheSolverEverRunning() {
        val board = Boards.wiegleb45Initial
        var solverPolled = 0

        val outcome =
            useCase(
                board = board,
                solution = null,
                movesPlayed = 0,
                shouldCancel = {
                    solverPolled++
                    true
                },
            )

        val move = foundMove(outcome, "the Wiegleb-45 start's hint")
        assertThat(solverPolled).isEqualTo(0)
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    @Test
    fun aBoardWithNoLegalMoveIsProvenDead() {
        // Two pegs separated by an empty hole: no legal jump, so the search exhausts its
        // (empty) subtree at once — a proof, not a stop.
        val spec = BoardSpec(inPlay = (0..2).map { Position(row = 0, col = it) }, target = null)
        val board =
            Board(spec = spec, pegs = 0L)
                .withPeg(Position(row = 0, col = 0))
                .withPeg(Position(row = 0, col = 2))

        val outcome = useCase(board = board, solution = null, movesPlayed = 0)

        assertThat(outcome).isEqualTo(HintOutcome.ProvenDead)
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

            val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

            val move = foundMove(outcome, "'$name' start (book-served)")
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

            val outcome = useCase(board = here, solution = null, movesPlayed = 0, shouldCancel = spy)

            val move = foundMove(outcome, "diamond41 floor-line ply $ply (book-served)")
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

                val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

                val move = foundMove(outcome, "european37 ply $ply position ${pegs.toHex()} (book-served)")
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
     * **Without this test, a bug that answered "no move" for every miss would look
     * identical to a working book.** Every other test here asserts the solver did NOT run,
     * so a pipeline that answered "no winning move" the moment the book missed would pass
     * all of them and ship the exact untruthful banner DEBT-01 exists to remove.
     *
     * The position is walked six plies down a deterministic path, past the book's depth-4
     * frontier, and its off-book status is ASSERTED as a precondition rather than assumed —
     * a committed-line prefix is covered at any depth, so "deeper than four" alone would
     * not establish it. The forced cancel then cuts the search short, which proves nothing,
     * so the answer must be Undecided (GD-HT-1).
     */
    @Test
    fun anOffBookClassicPositionStillReachesTheSolver() {
        val board = offBookPositionAtDepth(OFF_BOOK_PLIES)
        val spy = SolverSpy(cancel = true)

        assertWithMessage("the depth-%s position must be a book MISS", OFF_BOOK_PLIES)
            .that(OpeningBook.lookup(board))
            .isNull()

        val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

        assertWithMessage("a book miss must fall through to the solver, not answer for itself")
            .that(spy.polls)
            .isGreaterThan(0)
        assertThat(outcome).isEqualTo(HintOutcome.Undecided)
    }

    // ---- The three outcomes (GD-HT-1; the game-designer's AC-1 to AC-6) --------------

    /** AC-1: a search the caller cut short proved nothing. */
    @Test
    fun aSearchCutShortByCancellationIsUndecidedNotDead() {
        val board = offBookPositionAtDepth(OFF_BOOK_PLIES)
        val spy = SolverSpy(cancel = true)
        assertWithMessage("the depth-%s position must be a book MISS", OFF_BOOK_PLIES)
            .that(OpeningBook.lookup(board))
            .isNull()

        val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

        assertWithMessage("the search must have run and been cancelled").that(spy.polls).isGreaterThan(0)
        assertThat(outcome).isEqualTo(HintOutcome.Undecided)
    }

    /**
     * AC-2: Wiegleb-45 one ply past its depth-3 book is never called dead by a search that
     * was cut short. These are the three positions AC-18 replays on a device. Sequence A
     * follows the book's first two hints and then leaves them; B and C are played without
     * Hint. None is on the committed line, whose every position the book holds — so
     * following Hint all the way from the start never leaves the book.
     */
    @Test
    fun aWiegleb45PositionFourJumpsInIsNeverReportedDead() {
        WIEGLEB_45_FOUR_JUMPS_IN.forEach { (name, jumps) ->
            val board = afterJumps(Boards.wiegleb45Initial, jumps)
            val spy = SolverSpy(cancel = true)
            assertWithMessage("Wiegleb-45 sequence %s must be four jumps in", name)
                .that(Boards.wiegleb45Initial.pegCount - board.pegCount)
                .isEqualTo(FOUR_JUMPS)
            assertWithMessage("Wiegleb-45 sequence %s must be a book MISS", name)
                .that(OpeningBook.lookup(board))
                .isNull()

            val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

            assertWithMessage("Wiegleb-45 sequence %s must reach the solver", name).that(spy.polls).isGreaterThan(0)
            assertWithMessage("Wiegleb-45 sequence %s", name).that(outcome).isEqualTo(HintOutcome.Undecided)
        }
    }

    /**
     * AC-2b, the boundary control: three jumps in, the book still answers, with zero
     * solver polls. Sequence A's first three jumps reach a position that is not one of the
     * book's four Wiegleb-45 gap positions.
     */
    @Test
    fun aWiegleb45PositionThreeJumpsInIsStillABookAnswer() {
        val board = afterJumps(Boards.wiegleb45Initial, WIEGLEB_45_FOUR_JUMPS_IN.getValue("A").take(THREE_JUMPS))
        val spy = SolverSpy()

        val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

        val move = foundMove(outcome, "Wiegleb-45 three jumps in (book-served)")
        assertThat(spy.polls).isEqualTo(0)
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    /**
     * AC-3: the book's authoritative dead entry is answered as a proof, without the solver
     * (AC-19 replays this sequence on a device). The position still has legal moves, so the
     * banner a player sees there is the hint's, not a Stuck board's.
     */
    @Test
    fun aBookProvenDeadPositionIsProvenDeadWithoutTheSolver() {
        val board = afterJumps(Boards.english33Initial, ENGLISH_33_TO_A_PROVEN_DEAD_END)
        val spy = SolverSpy()
        assertWithMessage("the position must be one of English-33's authoritative dead entries")
            .that(OpeningBook.lookup(board))
            .isEqualTo(BookAnswer.NoWinningMove)
        assertWithMessage("the dead end must still have moves, or it is a Stuck board")
            .that(RulesEngine.legalMoves(board))
            .isNotEmpty()

        val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

        assertThat(outcome).isEqualTo(HintOutcome.ProvenDead)
        assertThat(spy.polls).isEqualTo(0)
    }

    /**
     * AC-4: a search that exhausts its subtree proves the position dead. The board must
     * have a legal move and pass the Pagoda condition — otherwise this would test the
     * stuck board or the pagoda path again — and the never-cancelling spy must be polled,
     * which shows the proof came from a search.
     */
    @Test
    fun anExhaustedSearchIsProvenDead() {
        val board = fiveHoleStripWithPegsAt(0, 1, 4)
        val spy = SolverSpy(cancel = false)
        assertWithMessage("the board must have a legal move").that(RulesEngine.legalMoves(board)).isNotEmpty()
        assertWithMessage("the board must pass the Pagoda condition").that(Pagoda.mayBeSolvable(board)).isTrue()

        val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

        assertThat(outcome).isEqualTo(HintOutcome.ProvenDead)
        assertWithMessage("the proof must come from a search").that(spy.polls).isGreaterThan(0)
    }

    /**
     * AC-5: a Pagoda reject is a proof. One peg in each `(row + col) mod 3` colour class
     * is three odd classes, more than a one-peg finish allows. The board still has a legal
     * move, and the spy would have forced an Undecided, so ProvenDead with zero polls can
     * only be the Pagoda's answer.
     */
    @Test
    fun aPagodaRejectIsProvenDead() {
        val board = fiveHoleStripWithPegsAt(0, 1, 2)
        val spy = SolverSpy()
        assertWithMessage("the board must have a legal move").that(RulesEngine.legalMoves(board)).isNotEmpty()
        assertWithMessage("the Pagoda condition must reject the board").that(Pagoda.mayBeSolvable(board)).isFalse()

        val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

        assertThat(outcome).isEqualTo(HintOutcome.ProvenDead)
        assertThat(spy.polls).isEqualTo(0)
    }

    /**
     * AC-6: a winnable position whose search runs out of nodes is Undecided — never dead.
     * Its winnability is asserted first, with the shipped budget and no deadline, so a dead
     * verdict here would be a false one.
     */
    @Test
    fun aSearchThatSpendsItsNodeCeilingIsUndecided() {
        val board = offBookPositionAtDepth(OFF_BOOK_PLIES)
        assertWithMessage("the depth-%s position must be a book MISS", OFF_BOOK_PLIES)
            .that(OpeningBook.lookup(board))
            .isNull()
        assertWithMessage("the position must be winnable")
            .that(Solver.decide(board, SolverBudget.DEFAULT))
            .isInstanceOf(SolverOutcome.Found::class.java)
        val starved = SolverBudget(maxNodes = 1L, deadline = SolverBudget.DEFAULT_DEADLINE)

        val outcome = useCase(board = board, solution = null, movesPlayed = 0, budget = starved)

        assertThat(outcome).isEqualTo(HintOutcome.Undecided)
    }

    /**
     * AC-20's domain half: Diamond-41's gap positions lie inside its depth-4 book's remit
     * but are absent from it, so they reach the solver — and a search cut short there is
     * Undecided. AC-20 replays this sequence on a device.
     */
    @Test
    fun aDiamond41GapPositionIsNeverReportedDead() {
        val board = afterJumps(Boards.diamond41Initial, DIAMOND_41_TO_A_GAP)
        val spy = SolverSpy(cancel = true)
        assertWithMessage("the position must lie within the book's depth")
            .that(Boards.diamond41Initial.pegCount - board.pegCount)
            .isAtMost(BOOK_PLIES)
        assertWithMessage("the position must be one of the book's gap positions, a MISS")
            .that(OpeningBook.lookup(board))
            .isNull()

        val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

        assertWithMessage("the gap position must reach the solver").that(spy.polls).isGreaterThan(0)
        assertThat(outcome).isEqualTo(HintOutcome.Undecided)
    }

    /**
     * The solver-never-ran spy, shared by every book test.
     *
     * Returning `true` from `shouldCancel` would force [Solver] to an Undecided, so a Found
     * or ProvenDead result with ZERO polls is a *proof* that the solver branch was never
     * entered rather than an inference from how fast the call returned. [cancel] is flipped
     * to `false` only where the solver is expected to run and the poll count is the
     * assertion.
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

    /** [outcome]'s move, once it is asserted — under [label] — to be [HintOutcome.Found]. */
    private fun foundMove(
        outcome: HintOutcome,
        label: String,
    ): Move {
        assertWithMessage("%s must be Found", label).that(outcome).isInstanceOf(HintOutcome.Found::class.java)
        return (outcome as HintOutcome.Found).move
    }

    /**
     * Plays [jumps] from [start]. Each (from, to) pair must be exactly one legal jump on the
     * board it is played on, so a sequence QA could not play on a device fails here first.
     */
    private fun afterJumps(
        start: Board,
        jumps: List<Pair<Position, Position>>,
    ): Board =
        jumps.fold(start) { board, (from, to) ->
            RulesEngine.applyMove(board, RulesEngine.legalMoves(board).single { it.from == from && it.to == to })
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

    /** A 1x5 strip with no target, holding pegs at [columns] only. */
    private fun fiveHoleStripWithPegsAt(vararg columns: Int): Board {
        val spec = BoardSpec(inPlay = (0..4).map { Position(row = 0, col = it) }, target = null)
        return columns.fold(Board(spec = spec, pegs = 0L)) { board, column ->
            board.withPeg(Position(row = 0, col = column))
        }
    }

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

        /** One ply past Wiegleb-45's depth-3 book: the fifth hint along a line. */
        const val FOUR_JUMPS = 4

        /** Wiegleb-45's book depth: the fourth hint along a line. */
        const val THREE_JUMPS = 3

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

        /**
         * AC-2 and AC-18: three Wiegleb-45 positions four jumps in, each a book MISS — the
         * device sequences, as (row, col) counted from 0 at the top-left of the 9x9 grid.
         * A takes the book's first two hints, then leaves them; B and C never ask for one.
         */
        val WIEGLEB_45_FOUR_JUMPS_IN: Map<String, List<Pair<Position, Position>>> =
            mapOf(
                "A" to listOf(jump(2, 4, 4, 4), jump(0, 4, 2, 4), jump(3, 2, 3, 4), jump(1, 3, 3, 3)),
                "B" to listOf(jump(6, 4, 4, 4), jump(8, 4, 6, 4), jump(5, 6, 5, 4), jump(7, 5, 5, 5)),
                "C" to listOf(jump(4, 2, 4, 4), jump(6, 3, 4, 3), jump(4, 0, 4, 2), jump(8, 3, 6, 3)),
            )

        /** AC-3 and AC-19: English-33 to one of its four book-proven dead positions. */
        val ENGLISH_33_TO_A_PROVEN_DEAD_END: List<Pair<Position, Position>> =
            listOf(jump(1, 3, 3, 3), jump(4, 3, 2, 3), jump(3, 1, 3, 3), jump(2, 3, 4, 3))

        /** AC-20: Diamond-41 to one of its 440 gap positions, three jumps in. */
        val DIAMOND_41_TO_A_GAP: List<Pair<Position, Position>> =
            listOf(jump(2, 4, 4, 4), jump(2, 2, 2, 4), jump(4, 2, 2, 2))

        /** One jump as a player makes it: the peg's (row, col), then the hole it lands in. */
        fun jump(
            fromRow: Int,
            fromCol: Int,
            toRow: Int,
            toCol: Int,
        ): Pair<Position, Position> = Position(row = fromRow, col = fromCol) to Position(row = toRow, col = toCol)
    }
}
