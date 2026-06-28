package com.app.pegsolitaire.domain.rules

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the stateless [RulesEngine].
 *
 * Magic numbers are intentional and acceptable in test code — the board
 * coordinates here ARE the specification of legal English-33 play.
 */
class RulesEngineTest {
    @Test
    fun initialPositionHasExactlyFourLegalMovesInDeterministicSortedOrder() {
        val moves = RulesEngine.legalMoves(Boards.english33Initial)

        val expected =
            listOf(
                Move(from = Position(1, 3), over = Position(2, 3), to = Position(3, 3)),
                Move(from = Position(3, 1), over = Position(3, 2), to = Position(3, 3)),
                Move(from = Position(3, 5), over = Position(3, 4), to = Position(3, 3)),
                Move(from = Position(5, 3), over = Position(4, 3), to = Position(3, 3)),
            )

        assertThat(moves).hasSize(4)
        // Exact list equality also pins the stable sorted enumeration order.
        assertThat(moves).isEqualTo(expected)
    }

    @Test
    fun legalMovesEnumerationIsStableAcrossRepeatedCalls() {
        val first = RulesEngine.legalMoves(Boards.english33Initial)
        val second = RulesEngine.legalMoves(Boards.english33Initial)

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun applyMoveClearsSourceAndJumpedPegSetsDestinationAndDropsPegCountByOne() {
        val board = Boards.english33Initial
        val move = Move(from = Position(1, 3), over = Position(2, 3), to = Position(3, 3))

        val result = RulesEngine.applyMove(board, move)

        assertThat(result.cellAt(Position(1, 3))).isEqualTo(Cell.Empty)
        assertThat(result.cellAt(Position(2, 3))).isEqualTo(Cell.Empty)
        assertThat(result.cellAt(Position(3, 3))).isEqualTo(Cell.Peg)
        assertThat(result.pegCount).isEqualTo(board.pegCount - 1)
        // applyMove returns a NEW board — the original is untouched (GAME-01).
        assertThat(board.cellAt(Position(1, 3))).isEqualTo(Cell.Peg)
    }

    @Test
    fun legalMovesNeverIncludesDiagonalOrNonSpacingTwoJumps() {
        val moves = RulesEngine.legalMoves(Boards.english33Initial)

        moves.forEach { move ->
            val sameRow = move.from.row == move.to.row
            val sameCol = move.from.col == move.to.col
            // Orthogonal: exactly one axis changes.
            assertThat(sameRow != sameCol).isTrue()
            // Spacing 2 along the moving axis; the jumped peg is the midpoint.
            val rowSpan = kotlin.math.abs(move.to.row - move.from.row)
            val colSpan = kotlin.math.abs(move.to.col - move.from.col)
            assertThat(rowSpan + colSpan).isEqualTo(2)
            assertThat(move.over.row).isEqualTo((move.from.row + move.to.row) / 2)
            assertThat(move.over.col).isEqualTo((move.from.col + move.to.col) / 2)
        }
    }

    @Test
    fun isGameOverIsFalseOnTheInitialPosition() {
        assertThat(RulesEngine.isGameOver(Boards.english33Initial)).isFalse()
    }

    @Test
    fun isGameOverIsTrueWhenASinglePegRemains() {
        val singlePeg = Board(spec = Boards.English33, pegs = 0L).withPeg(Position(3, 3))

        assertThat(RulesEngine.legalMoves(singlePeg)).isEmpty()
        assertThat(RulesEngine.isGameOver(singlePeg)).isTrue()
    }

    @Test
    fun isGameOverIsTrueAfterAGreedyLinePlaysItselfOut() {
        var board = Boards.english33Initial
        // Deterministically play the first legal move until none remain.
        while (true) {
            val moves = RulesEngine.legalMoves(board)
            if (moves.isEmpty()) break
            board = RulesEngine.applyMove(board, moves.first())
        }

        assertThat(RulesEngine.isGameOver(board)).isTrue()
        assertThat(RulesEngine.pegCount(board)).isEqualTo(board.pegCount)
        // A played-out line must end with more than zero pegs and no available move.
        assertThat(board.pegCount).isGreaterThan(0)
    }
}
