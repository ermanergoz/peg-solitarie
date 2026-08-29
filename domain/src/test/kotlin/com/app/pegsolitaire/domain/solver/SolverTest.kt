package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the production [Solver.firstWinningMove] (ASST-01 / ASST-03).
 *
 * Magic numbers (board coordinates, prefix lengths) are intentional and acceptable
 * in test code — they ARE the specification of the sampled positions. The four
 * one-peg fixtures ([SolutionFixtures.ALL]) plus their committed golden winning
 * lines ([RecordedSolutions.load]) supply the solvable mid-game corpus; a small
 * hand-built strip supplies the provably-dead and already-solved positions.
 */
class SolverTest {
    @Test
    fun returnsALegalWinPreservingMoveFromASolvableMidGameState() {
        // ONE_PEG, not ALL: `firstWinningMove` searches for a ONE-peg line, so
        // Diamond-41 (floor two) has no winning move to find. Plan 08-05 generalises
        // the goal test to `spec.minimumPegs`, at which point the diamond joins in.
        SolutionFixtures.ONE_PEG.forEach { fixture ->
            val line = RecordedSolutions.load(fixture.name)
            val start = SolverSamples.startBoard(fixture)
            // Replay a mid-length prefix to reach a genuinely solvable mid-game board.
            val midGame = SolverSamples.replay(start, line.take(line.size / 2))

            val move = Solver.firstWinningMove(midGame, SolverBudget.DEFAULT)

            assertThat(move).isNotNull()
            // The hinted move must be a legal move of the current board (same authority as gameplay).
            assertThat(RulesEngine.legalMoves(midGame)).contains(move)
            // Win-preserving: after performing the move the board is still solvable to one peg.
            val afterMove = RulesEngine.applyMove(midGame, move!!)
            val stillSolvable =
                afterMove.pegCount == 1 ||
                    Solver.firstWinningMove(afterMove, SolverBudget.DEFAULT) != null
            assertThat(stillSolvable).isTrue()
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
