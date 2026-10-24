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
import com.app.pegsolitaire.domain.model.Cell
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
     * AC-2 (the ruling's Amendment 1): the positions after walks A, B and C are never called
     * dead by a search that was cut short. AC-18b to AC-18d replay these walks on a device.
     *
     * Following Hint from the start never reaches such a position, because the book holds
     * every state of the golden line (AC-2c). Each walk instead plays golden moves 1 to 4
     * under one symmetry of the board, so its position is the golden depth-4 state seen in a
     * mirror. The literal is pinned to that construction, and to the empty holes the ruling's
     * table lists, before the ruling's three preconditions: a book miss, not the golden
     * line's own depth-4 state, and winnable.
     */
    @Test
    fun aWinnableWiegleb45PositionOffTheGoldenLineIsNeverReportedDead() {
        assertWithMessage("the golden line's own depth-4 state, as the ruling describes it")
            .that(emptyHoles(wiegleb45GoldenStateAt(FOUR_JUMPS)))
            .containsExactlyElementsIn(GOLDEN_DEPTH_4_EMPTY_HOLES)

        WIEGLEB_45_WALKS.forEach { (name, walk) ->
            assertWithMessage("walk %s must be its symmetry's image of golden moves 1 to 4", name)
                .that(walk.jumps)
                .isEqualTo(wiegleb45GoldenOpeningUnder(walk.symmetry))
            val board = afterMoves(Boards.wiegleb45Initial, walk.jumps)
            assertWithMessage("walk %s's empty holes, as the ruling's table lists them", name)
                .that(emptyHoles(board))
                .containsExactlyElementsIn(walk.emptyAfterwards)
            assertIsAWinnableBookMissOffTheGoldenLine(board, walk.symmetry, "walk $name")
            val spy = SolverSpy(cancel = true)

            val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

            assertWithMessage("walk %s must reach the solver", name).that(spy.polls).isGreaterThan(0)
            assertWithMessage("walk %s", name).that(outcome).isEqualTo(HintOutcome.Undecided)
        }
    }

    /**
     * The four spare walks: the golden opening under the board's other four symmetries
     * (the top-bottom mirror, the anti-transpose and the 90° and 270° rotations), checked
     * exactly as walks A to C are. AC-18's pass line asks for a replacement walk if one
     * shows a move on a device; these are the replacements, already proven.
     */
    @Test
    fun everyOtherSymmetryImageOfTheGoldenOpeningIsAWinnableBookMiss() {
        val spares = GridSymmetry.entries - WIEGLEB_45_WALKS.values.map { it.symmetry }.toSet()
        assertWithMessage("the board's seven symmetries, less walks A to C").that(spares).hasSize(SPARE_WALKS)

        spares.forEach { symmetry ->
            val board = afterMoves(Boards.wiegleb45Initial, wiegleb45GoldenOpeningUnder(symmetry))
            assertIsAWinnableBookMissOffTheGoldenLine(board, symmetry, symmetry.name)
            val spy = SolverSpy(cancel = true)

            val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

            assertWithMessage("%s must reach the solver", symmetry.name).that(spy.polls).isGreaterThan(0)
            assertWithMessage(symmetry.name).that(outcome).isEqualTo(HintOutcome.Undecided)
        }
    }

    /**
     * AC-2b, the boundary control: three jumps in, the book still answers, with zero
     * solver polls. Walk A's first three jumps reach a position inside the depth-3 frontier
     * that is asserted to be a book hit — so if it were one of the book's four Wiegleb-45
     * gap positions, this would fail loudly rather than test the solver instead.
     */
    @Test
    fun aWiegleb45PositionThreeJumpsInIsStillABookAnswer() {
        val board = afterMoves(Boards.wiegleb45Initial, WIEGLEB_45_WALKS.getValue("A").jumps.take(THREE_JUMPS))
        assertWithMessage("walk A three jumps in must be a book HIT, not a gap position")
            .that(OpeningBook.lookup(board))
            .isInstanceOf(BookAnswer.Hit::class.java)
        val spy = SolverSpy()

        val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

        val move = foundMove(outcome, "Wiegleb-45 three jumps in (book-served)")
        assertThat(spy.polls).isEqualTo(0)
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    /**
     * AC-2c (the ruling's Amendment 1, §16 #22): a player who follows Hint from an untouched
     * Classic start never reaches the solver. Each book holds every state of its board's
     * golden line and answers that line's own move there, so every hint, all the way to the
     * board's target, is Found, from the book, with zero polls.
     */
    @Test
    fun followingHintFromEachClassicStartStaysOnTheBookToTheEnd() {
        SolutionFixtures.ALL.forEach { fixture ->
            val golden = RecordedSolutions.load(fixture.name)
            var board = startBoardOf(fixture)

            golden.forEachIndexed { depth, goldenMove ->
                val spy = SolverSpy()

                val outcome = useCase(board = board, solution = null, movesPlayed = 0, shouldCancel = spy)

                assertWithMessage("%s, hint %s", fixture.name, depth + 1)
                    .that(outcome)
                    .isEqualTo(HintOutcome.Found(goldenMove))
                assertWithMessage("%s, hint %s must not reach the solver", fixture.name, depth + 1)
                    .that(spy.polls)
                    .isEqualTo(0)
                board = RulesEngine.applyMove(board, foundMove(outcome, "${fixture.name}, hint ${depth + 1}"))
            }

            assertWithMessage("following Hint on %s must reach its target", fixture.name)
                .that(board.pegCount)
                .isEqualTo(fixture.spec.minimumPegs)
        }
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

    /** One of the ruling's walks: its symmetry, its four jumps and the holes it leaves empty. */
    private data class Walk(
        val symmetry: GridSymmetry,
        val jumps: List<Move>,
        val emptyAfterwards: List<Position>,
    )

    /**
     * The seven symmetries of Wiegleb-45's 9x9 grid other than the identity (the square's
     * D4 group), each a map on positions. The footprint is symmetric under every one of
     * them, so each takes a legal line to a legal line. Walks A, B and C use the first three
     * (the ruling's Amendment 1); the other four are the spares. The rotations turn clockwise.
     */
    private enum class GridSymmetry(
        private val image: (row: Int, col: Int) -> Position,
    ) {
        LEFT_RIGHT_MIRROR({ row, col -> Position(row = row, col = LAST_GRID_INDEX - col) }),
        ROTATION_180({ row, col -> Position(row = LAST_GRID_INDEX - row, col = LAST_GRID_INDEX - col) }),
        TRANSPOSE({ row, col -> Position(row = col, col = row) }),
        TOP_BOTTOM_MIRROR({ row, col -> Position(row = LAST_GRID_INDEX - row, col = col) }),
        ANTI_TRANSPOSE({ row, col -> Position(row = LAST_GRID_INDEX - col, col = LAST_GRID_INDEX - row) }),
        ROTATION_90({ row, col -> Position(row = col, col = LAST_GRID_INDEX - row) }),
        ROTATION_270({ row, col -> Position(row = LAST_GRID_INDEX - col, col = row) }),
        ;

        fun of(position: Position): Position = image(position.row, position.col)

        fun of(move: Move): Move = Move(from = of(move.from), over = of(move.over), to = of(move.to))
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

    /**
     * Plays [moves] — written from → over → to, as the golden files and the ruling write
     * them — from [start], asserting that each is a legal jump on the board it is played on.
     */
    private fun afterMoves(
        start: Board,
        moves: List<Move>,
    ): Board =
        moves.fold(start) { board, move ->
            assertWithMessage("%s must be legal where it is played", move)
                .that(RulesEngine.legalMoves(board))
                .contains(move)
            RulesEngine.applyMove(board, move)
        }

    /** The in-play holes of [board] that hold no peg. */
    private fun emptyHoles(board: Board): List<Position> = board.spec.inPlay.filter { board.cellAt(it) == Cell.Empty }

    /** Wiegleb-45's golden line, played [depth] moves from the start. */
    private fun wiegleb45GoldenStateAt(depth: Int): Board =
        afterMoves(Boards.wiegleb45Initial, RecordedSolutions.load(WIEGLEB_45).take(depth))

    /** Golden moves 1 to 4 of Wiegleb-45 under [symmetry]: how every walk is built. */
    private fun wiegleb45GoldenOpeningUnder(symmetry: GridSymmetry): List<Move> =
        RecordedSolutions.load(WIEGLEB_45).take(FOUR_JUMPS).map { symmetry.of(it) }

    /**
     * The ruling's three preconditions on a walk's position (AC-2), under [label]: a book
     * miss; not the golden line's own depth-4 state; and winnable — [symmetry] applied to
     * golden moves 5 to 43 and replayed move by move through [RulesEngine] ends at one peg.
     * The symmetry is checked first to map the board's footprint onto itself.
     */
    private fun assertIsAWinnableBookMissOffTheGoldenLine(
        board: Board,
        symmetry: GridSymmetry,
        label: String,
    ) {
        val footprint = Boards.Wiegleb45.footprint
        assertWithMessage("%s: %s must map the footprint onto itself", label, symmetry.name)
            .that(footprint.map { symmetry.of(it) })
            .containsExactlyElementsIn(footprint)
        assertWithMessage("%s must be a book MISS", label).that(OpeningBook.lookup(board)).isNull()
        assertWithMessage("%s must not be the golden line's own depth-4 state", label)
            .that(board)
            .isNotEqualTo(wiegleb45GoldenStateAt(FOUR_JUMPS))
        val tail = RecordedSolutions.load(WIEGLEB_45).drop(FOUR_JUMPS).map { symmetry.of(it) }
        assertWithMessage("%s must be winnable: the image of golden moves 5 to 43 ends at one peg", label)
            .that(afterMoves(board, tail).pegCount)
            .isEqualTo(ONE_PEG)
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

        /** Four jumps in: one ply past Wiegleb-45's depth-3 book, where every walk lands. */
        const val FOUR_JUMPS = 4

        /** Wiegleb-45's book depth. */
        const val THREE_JUMPS = 3

        const val WIEGLEB_45 = "wiegleb45"

        /** Wiegleb-45's proven target: a line from its start ends at one peg. */
        const val ONE_PEG = 1

        /** The board's seven symmetries, less the three that walks A to C use. */
        const val SPARE_WALKS = 4

        /** The last row and column index of Wiegleb-45's 9x9 grid. */
        const val LAST_GRID_INDEX = 8

        /** The separator between the cells of one jump, as the golden files write it. */
        const val JUMP_ARROW = "->"

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
                WIEGLEB_45 to Boards.wiegleb45Initial,
            )

        /** The golden line's own depth-4 state, by its empty holes, as the ruling gives them. */
        val GOLDEN_DEPTH_4_EMPTY_HOLES: List<Position> = positions("(0,4)", "(1,4)", "(3,5)", "(3,7)", "(3,8)")

        /**
         * AC-2 and AC-18b to AC-18d: the ruling's walks A, B and C (Amendment 1), copied from
         * its table — (row, col) counted from 0 at the top-left of the 9x9 grid, each jump
         * from → over → to — with the empty holes the table lists for each.
         */
        val WIEGLEB_45_WALKS: Map<String, Walk> =
            mapOf(
                "A" to
                    Walk(
                        symmetry = GridSymmetry.LEFT_RIGHT_MIRROR,
                        jumps =
                            moves(
                                "(2,4)->(3,4)->(4,4)",
                                "(0,4)->(1,4)->(2,4)",
                                "(3,2)->(3,3)->(3,4)",
                                "(3,0)->(3,1)->(3,2)",
                            ),
                        emptyAfterwards = positions("(0,4)", "(1,4)", "(3,3)", "(3,1)", "(3,0)"),
                    ),
                "B" to
                    Walk(
                        symmetry = GridSymmetry.ROTATION_180,
                        jumps =
                            moves(
                                "(6,4)->(5,4)->(4,4)",
                                "(8,4)->(7,4)->(6,4)",
                                "(5,2)->(5,3)->(5,4)",
                                "(5,0)->(5,1)->(5,2)",
                            ),
                        emptyAfterwards = positions("(8,4)", "(7,4)", "(5,3)", "(5,1)", "(5,0)"),
                    ),
                "C" to
                    Walk(
                        symmetry = GridSymmetry.TRANSPOSE,
                        jumps =
                            moves(
                                "(4,2)->(4,3)->(4,4)",
                                "(4,0)->(4,1)->(4,2)",
                                "(6,3)->(5,3)->(4,3)",
                                "(8,3)->(7,3)->(6,3)",
                            ),
                        emptyAfterwards = positions("(4,0)", "(4,1)", "(5,3)", "(7,3)", "(8,3)"),
                    ),
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

        /** Jumps written as the golden files write them: `(row,col)->(row,col)->(row,col)`. */
        fun moves(vararg notations: String): List<Move> =
            notations.map { notation ->
                val (from, over, to) = notation.split(JUMP_ARROW).map { position(it) }
                Move(from = from, over = over, to = to)
            }

        fun positions(vararg notations: String): List<Position> = notations.map { position(it) }

        /** One cell written `(row,col)`. */
        fun position(notation: String): Position {
            val (row, col) = notation.removeSurrounding("(", ")").split(",").map { it.toInt() }
            return Position(row = row, col = col)
        }
    }
}
