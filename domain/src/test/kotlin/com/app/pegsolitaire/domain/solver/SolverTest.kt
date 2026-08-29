package com.app.pegsolitaire.domain.solver

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
 * Behaviour spec for the production [Solver.firstWinningMove] (ASST-01 / ASST-03).
 *
 * Magic numbers (board coordinates, prefix lengths) are intentional and acceptable
 * in test code — they ARE the specification of the sampled positions. The five
 * fixtures ([SolutionFixtures.ALL]) plus their committed golden lines
 * ([RecordedSolutions.load]) supply the solvable mid-game corpus; a small hand-built
 * strip supplies the provably-dead and already-solved positions.
 *
 * **The corpus is [SolutionFixtures.ALL], not [SolutionFixtures.ONE_PEG].** Plan 08-06
 * narrowed the sweeps here to the four one-peg boards, because the solver searched for a
 * hardcoded one-peg terminal and Diamond-41's floor is a proven two (DEBT-02) — asking
 * it a one-peg question would have asserted the negation of the invariant that proof
 * rests on. The goal test now reads [BoardSpec.minimumPegs], so the question each board
 * is asked is its own, and the diamond belongs in every sweep again.
 *
 * **On full-footprint STARTS (DEBT-01).** Three of the five shipped starts are asserted
 * below to resolve inside the unchanged 1.5s production deadline. Two are deliberately
 * absent, and neither omission is an oversight:
 *
 * - **Wiegleb-45** is outside the deadline by roughly four orders of magnitude. RESEARCH
 *   measured 0 of its 76 depth-≤4 positions resolving in 1.5s even at 15M nodes/s, so
 *   asserting it here would be a known-red test rather than a contract. Its truthfulness
 *   is proven instead by `OpeningBookTest`/`DepthSweepTest` (plans 08-09/08-10), where a
 *   committed book entry replaces the race against the clock.
 * - **Diamond-41** sits right ON the deadline: measured between ~1.18s and a deadline
 *   miss across runs on the same machine, because [Pagoda]'s colour-parity prune is
 *   provably vacuous at a two-peg target (08-04) so the search is effectively unpruned.
 *   That is a coin-flip, not a contract, and pinning it would be a flaky test. It is the
 *   sharpest evidence that the opening book is load-bearing for the diamond rather than
 *   an optimisation. Every diamond position after the opening — the mid-game below and
 *   the endgame tail in [SolverBudgetTest] — resolves in tens of milliseconds.
 */
class SolverTest {
    @Test
    fun returnsALegalWinPreservingMoveFromASolvableMidGameState() {
        SolutionFixtures.ALL.forEach { fixture ->
            val line = RecordedSolutions.load(fixture.name)
            val start = SolverSamples.startBoard(fixture)
            // Replay a mid-length prefix to reach a genuinely solvable mid-game board.
            val midGame = SolverSamples.replay(start, line.take(line.size / 2))

            val move = Solver.firstWinningMove(midGame, SolverBudget.DEFAULT)

            assertWithMessage("%s mid-game must have a winning move", fixture.name).that(move).isNotNull()
            // The hinted move must be a legal move of the current board (same authority as gameplay).
            assertThat(RulesEngine.legalMoves(midGame)).contains(move)
            // Win-preserving: after performing the move the board is still solvable to its
            // OWN target — spec.minimumPegs, not a duplicated one-peg literal.
            val afterMove = RulesEngine.applyMove(midGame, move!!)
            val stillSolvable =
                afterMove.pegCount <= fixture.spec.minimumPegs ||
                    Solver.firstWinningMove(afterMove, SolverBudget.DEFAULT) != null
            assertWithMessage("%s must stay solvable after the hinted move", fixture.name)
                .that(stillSolvable)
                .isTrue()
        }
    }

    @Test
    fun returnsNullOnAGenuinelyUnsolvablePosition() {
        assertThat(Solver.firstWinningMove(SolverSamples.deadBoard, SolverBudget.DEFAULT)).isNull()
    }

    @Test
    fun returnsNullWhenAlreadySolved() {
        assertThat(Solver.firstWinningMove(SolverSamples.solvedBoard, SolverBudget.DEFAULT)).isNull()
    }

    /**
     * The reference board's full start, as a control for the two DEBT-01 cases below:
     * English-33 resolved inside the deadline before this phase too, and must still.
     * Measured ~204ms against the 1500ms ceiling (~7x margin), the slowest of the three
     * only because it is the first solve in a cold JVM.
     */
    @Test
    fun resolvesTheEnglish33FullStartInsideTheProductionDeadline() {
        assertFullStartResolves(Boards.english33Initial, "english33")
    }

    /**
     * **DEBT-01 regression test.** Before this plan the European-37 start folded to "no
     * winning move": the allocation-heavy loop ran at ~0.27M nodes/s and every attempt
     * was handed the whole 40M-node budget, so the 1.5s deadline always fired inside one
     * unlucky dive. Measured after the rewrite at ~84ms against the 1500ms ceiling — an
     * ~18x margin, so this asserts a capability envelope rather than a stopwatch.
     */
    @Test
    fun resolvesTheEuropean37FullStartInsideTheProductionDeadline() {
        assertFullStartResolves(Boards.european37Initial, "european37")
    }

    /**
     * **DEBT-01 regression test.** Same pre-phase behaviour as European-37 — the start
     * folded to "no winning move". Its symmetry group has order 2 rather than 8 (08-02),
     * so the memo folds far fewer positions together and it was the more expensive of the
     * two before the rewrite. Measured after it at well under 10ms: the inwardness-ordered
     * bitboard loop finds this line in ~1k positions, a >100x margin on the 1500ms ceiling.
     */
    @Test
    fun resolvesTheAsymmetric39FullStartInsideTheProductionDeadline() {
        assertFullStartResolves(Boards.asymmetric39Initial, "asymmetric39")
    }

    /**
     * The engine is deterministic: the restart seeds are fixed, and the per-attempt move
     * ordering is derived from them, so the same board under the same budget always yields
     * the same move. A player who asks twice must not be shown two different hints.
     *
     * Queried WITHOUT a wall-clock `shouldCancel`, deliberately — a time-based deadline is
     * the one genuinely non-deterministic input to the search, so including it would test
     * the machine rather than the engine.
     */
    @Test
    fun returnsTheSameMoveWhenTheSameBoardIsQueriedTwice() {
        val first = Solver.firstWinningMove(Boards.european37Initial, SolverBudget.DEFAULT)
        val second = Solver.firstWinningMove(Boards.european37Initial, SolverBudget.DEFAULT)

        assertThat(first).isNotNull()
        assertThat(second).isEqualTo(first)
    }

    /**
     * **The restart loop is reachable.** Before this plan `Solver.MAX_ATTEMPTS` was dead
     * code in production: `searchWithRestarts` handed every attempt the full
     * [SolverBudget.maxNodes], so the deadline always fired inside attempt 0 and no
     * restart ever ran. This drives the corpus through an explicitly-constructed budget
     * whose [SolverBudget.perAttemptMaxNodes] is far too small to decide a full start in
     * one dive, and asserts that positions still resolve HAVING EXCEEDED that cap — which
     * only a later, larger attempt can do.
     *
     * `Solver.lastVisited` reports the DECIDING attempt's distinct positions, so
     * `lastVisited > cap` is a direct witness that attempt 0 was abandoned and a restart
     * decided the position. Measured at this cap: english33 (38k), european37 (60k) and
     * diamond41 (75k) all decide via a restart; asymmetric39 decides inside attempt 0
     * (~1k positions) and wiegleb45 does not decide at all, so the assertion is "at least
     * one", not "every".
     */
    @Test
    fun aPositionAbandonedByItsFirstAttemptIsStillDecidedByARestart() {
        val cappedBudget =
            SolverBudget(
                maxNodes = SolverBudget.DEFAULT_MAX_NODES,
                deadline = SolverBudget.DEFAULT_DEADLINE,
                perAttemptMaxNodes = RESTART_PROBE_MAX_NODES,
            )

        val decidedByARestart =
            SolutionFixtures.ALL.count { fixture ->
                val move = Solver.firstWinningMove(SolverSamples.startBoard(fixture), cappedBudget)
                move != null && Solver.lastVisited > RESTART_PROBE_MAX_NODES
            }

        assertWithMessage("no position outlived its first attempt — MAX_ATTEMPTS is dead code again")
            .that(decidedByARestart)
            .isAtLeast(1)
    }

    /**
     * Asserts [board]'s full-footprint start resolves to a legal move inside the shipped
     * deadline, driving the search through the SAME envelope `GameViewModel` builds: a
     * nanotime-based `shouldCancel` closed over [SolverBudget.DEFAULT]'s deadline, rather
     * than an unbounded search that would prove nothing about production behaviour.
     */
    private fun assertFullStartResolves(
        board: Board,
        label: String,
    ) {
        val startedAt = System.nanoTime()
        val move =
            Solver.firstWinningMove(board, SolverBudget.DEFAULT) {
                System.nanoTime() - startedAt > SolverBudget.DEFAULT.deadline.inWholeNanoseconds
            }

        assertWithMessage("%s full start must resolve inside %s", label, SolverBudget.DEFAULT.deadline)
            .that(move)
            .isNotNull()
        assertWithMessage("%s hint must be a legal move of the board it was asked about", label)
            .that(RulesEngine.legalMoves(board))
            .contains(move)
    }

    private companion object {
        /**
         * A deliberately undersized per-attempt cap for the restart-reachability probe.
         * Small enough that no full start except Asymmetric-39's decides within it, large
         * enough that the escalating restarts still finish the sweep in well under a
         * second — this is a fast per-commit gate, not a solver benchmark.
         */
        private const val RESTART_PROBE_MAX_NODES = 5_000L
    }
}

/**
 * Shared position builders for the solver test suite ([SolverTest], [PagodaTest],
 * [SolverBudgetTest]) — folds fixture footprints and hand-built strips into the
 * [Board]s each test drives, using only [RulesEngine] and the domain model.
 */
internal object SolverSamples {
    /** The single-vacancy start board for [fixture] (identical to the shipped `Boards.*Initial`). */
    fun startBoard(fixture: SolutionFixtures.Fixture): Board =
        fixture.spec.inPlay
            .fold(Board(spec = fixture.spec, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(fixture.startVacancy)

    /** Folds [moves] forward through [RulesEngine.applyMove] from [start]. */
    fun replay(
        start: Board,
        moves: List<Move>,
    ): Board = moves.fold(start) { board, move -> RulesEngine.applyMove(board, move) }

    /** A short 1x5 strip footprint used to hand-build dead / solved positions. */
    private val stripSpec =
        BoardSpec(
            inPlay = (0..4).map { column -> Position(row = 0, col = column) },
            target = Position(row = 0, col = 2),
        )

    /**
     * A provably-dead position: two pegs at (0,0) and (0,3) — same `(row+col) mod 3`
     * colour class, with no peg between them to jump, so no legal move exists AND the
     * mod-3 parity necessary condition fails. Cannot reach one peg.
     */
    val deadBoard: Board =
        Board(spec = stripSpec, pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 3))

    /** An already-solved single-peg board — the Solver has no move to give. */
    val solvedBoard: Board =
        Board(spec = stripSpec, pegs = 0L)
            .withPeg(Position(row = 0, col = 2))
}
