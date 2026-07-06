package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The D-04 completeness + budget-headroom measurement corpus.
 *
 * D-04 makes any budget miss read as "no winning move", so a solvable Classic board
 * must NEVER hit the budget. This test enumerates every solvable golden-solution
 * prefix across the four one-peg fixtures, asserts 100% return a move within
 * [SolverBudget.DEFAULT], records the largest search actually performed, and asserts
 * [SolverBudget.DEFAULT] keeps at least [HEADROOM_FACTOR]x node headroom over it — so
 * the shipped budget is a measured figure, not a guess.
 */
class SolverBudgetTest {
    @Test
    fun everySolvableGoldenPrefixReturnsAMoveWithinBudget() {
        var maxObservedNodes = 0L

        SolutionFixtures.ALL.forEach { fixture ->
            val line = RecordedSolutions.load(fixture.name)
            val start = SolverSamples.startBoard(fixture)
            // Enumerate EVERY solvable mid-game state (each prefix of the winning line,
            // excluding the already-solved final one-peg board).
            for (prefixLength in 0 until line.size) {
                val board = SolverSamples.replay(start, line.take(prefixLength))

                val move = Solver.firstWinningMove(board, SolverBudget.DEFAULT)

                assertThat(move).isNotNull()
                maxObservedNodes = maxOf(maxObservedNodes, Solver.lastVisited)
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

        // The shipped budget must keep >= HEADROOM_FACTOR headroom over the largest
        // search observed anywhere in the corpus (D-04: solvable boards never falsely
        // read as unsolvable). If this fails, RAISE DEFAULT_MAX_NODES — never lower it.
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
    }
}
