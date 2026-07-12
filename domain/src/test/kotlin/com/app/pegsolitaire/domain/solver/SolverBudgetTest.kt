package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The D-04 completeness + budget-headroom check, sized to run in CI seconds.
 *
 * D-04 makes any budget/deadline miss read as "no winning move", so a solvable board
 * that the Solver is EXPECTED to decide must return a real move within the shipped
 * [SolverBudget.DEFAULT]. This test proves that over the **tractable** corpus and asserts
 * [SolverBudget.DEFAULT_MAX_NODES] keeps [HEADROOM_FACTOR]x node headroom over the
 * largest deciding search it performs.
 *
 * **Why not every prefix of every board?** Measured node/time facts (see
 * `04-02-SUMMARY.md`): node cost is high-variance even on the smallest board — english33
 * decides most positions in <1s but one mid-line prefix needs ~2.2M nodes (~8.9s), and
 * the *early* positions of the larger footprints are worse (european37's 36-peg start
 * ~2.2M nodes / ~7.7s; the 38/44-peg asymmetric39 / wiegleb45 starts do not resolve
 * inside 8s). Jittered restarts do NOT reliably shortcut these. In PRODUCTION such
 * positions are governed by [SolverBudget.DEFAULT_DEADLINE] (1.5s), not the node cap, and
 * D-04 folds a deadline miss to the "no winning move" state — a documented capability
 * envelope, not a solver bug. So CI measures what the Solver is reliably relied on to
 * decide fast: the reference board [REFERENCE_BOARD]'s full-footprint start plus every
 * board's cheap [ENDGAME_DEPTH]-move endgame tail. Each solve carries a hard
 * [PER_CALL_CAP_MS] wall-clock guard, so the whole suite is bounded regardless of
 * machine; a tripped guard is a real regression, not flake.
 */
class SolverBudgetTest {
    @Test
    fun everyTractableSolvablePositionReturnsAMoveWithinBudget() {
        var maxObservedNodes = 0L
        val perCallCapNanos = PER_CALL_CAP_MS * NANOS_PER_MILLI

        fun decide(
            board: Board,
            label: String,
        ) {
            val startedAt = System.nanoTime()
            val move =
                Solver.firstWinningMove(board, SolverBudget.DEFAULT) {
                    System.nanoTime() - startedAt > perCallCapNanos
                }
            assertWithMessage("solvable position %s must return a move within budget", label)
                .that(move)
                .isNotNull()
            maxObservedNodes = maxOf(maxObservedNodes, Solver.lastVisited)
        }

        SolutionFixtures.ALL.forEach { fixture ->
            val line = RecordedSolutions.load(fixture.name)
            val start = SolverSamples.startBoard(fixture)
            // Every board's cheap endgame tail must decide fast — that is where a finishing
            // hint matters most and where the Solver fallback is reliably inside the
            // deadline. The small reference board additionally decides its full-footprint
            // START fast; the larger boards' early positions are deadline-governed in
            // production (see class KDoc) and are exercised by 04-03's ViewModel
            // integration and the 04-05 playtest, not by this fast CI gate.
            if (fixture.name == REFERENCE_BOARD) decide(start, "${fixture.name}@start")
            for (prefixLength in maxOf(0, line.size - ENDGAME_DEPTH) until line.size) {
                val board = SolverSamples.replay(start, line.take(prefixLength))
                decide(board, "${fixture.name}@prefix$prefixLength")
            }
        }

        // A few small constructed positions that pass the pagoda pre-check but are
        // genuinely unsolvable, so the Solver performs a FULL exhaustive search of
        // their (small) forward space — the exhaustive-search node counts to headroom.
        exhaustiveUnsolvableSamples().forEach { board ->
            val move = Solver.firstWinningMove(board, SolverBudget.DEFAULT)
            assertThat(move).isNull()
            maxObservedNodes = maxOf(maxObservedNodes, Solver.lastVisited)
        }

        // The shipped node cap must keep >= HEADROOM_FACTOR headroom over the largest
        // deciding search observed in the tractable corpus (D-04: a board the Solver is
        // relied on to decide must never be cut short before it surfaces a move). If this
        // fails, RAISE DEFAULT_MAX_NODES — never lower the assertion.
        assertThat(SolverBudget.DEFAULT_MAX_NODES)
            .isAtLeast(HEADROOM_FACTOR * maxObservedNodes)
    }

    /**
     * Small unsolvable positions that still pass [Pagoda.mayBeSolvable] (so the Solver
     * runs a full search rather than short-circuiting). Each is a tiny cross/strip
     * whose forward space is exhausted cheaply, giving a representative exhaustive
     * node count to size the budget headroom against.
     */
    private fun exhaustiveUnsolvableSamples(): List<Board> {
        // A 5-cell plus with all four arms pegged and the centre empty. Any first jump
        // strands the remaining pegs; it cannot reach one peg, but colour-parity does
        // not prove that, so the Solver exhausts the (small) tree.
        val plusSpec =
            BoardSpec(
                inPlay =
                    listOf(
                        Position(row = 0, col = 1),
                        Position(row = 1, col = 0),
                        Position(row = 1, col = 1),
                        Position(row = 1, col = 2),
                        Position(row = 2, col = 1),
                    ),
                target = Position(row = 1, col = 1),
            )
        val plusArmsPegged =
            plusSpec.inPlay
                .fold(Board(spec = plusSpec, pegs = 0L)) { board, position -> board.withPeg(position) }
                .withoutPeg(Position(row = 1, col = 1))

        // A 1x5 strip with three pegs that can make one jump then stall short of one peg.
        val stripSpec =
            BoardSpec(
                inPlay = (0..4).map { column -> Position(row = 0, col = column) },
                target = Position(row = 0, col = 2),
            )
        val stripThreePegs =
            Board(spec = stripSpec, pegs = 0L)
                .withPeg(Position(row = 0, col = 0))
                .withPeg(Position(row = 0, col = 1))
                .withPeg(Position(row = 0, col = 4))

        return listOf(plusArmsPegged, stripThreePegs)
            .filter { Pagoda.mayBeSolvable(it) && RulesEngine.legalMoves(it).isNotEmpty() }
    }

    private companion object {
        /** Minimum node headroom the budget must keep over the largest observed search. */
        private const val HEADROOM_FACTOR = 4L

        /** The board whose full solution line is tractable end-to-end (measured ~21k nodes). */
        private const val REFERENCE_BOARD = "english33"

        /** Endgame-tail depth measured on the larger boards (cheap, few-peg positions). */
        private const val ENDGAME_DEPTH = 10

        /** Hard per-solve wall-clock guard — tractable positions resolve far inside it. */
        private const val PER_CALL_CAP_MS = 2_000L

        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
