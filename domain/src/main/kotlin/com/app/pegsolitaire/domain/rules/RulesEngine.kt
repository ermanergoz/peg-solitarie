package com.app.pegsolitaire.domain.rules

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position

/**
 * The stateless, pure-Kotlin authority for peg-solitaire rules.
 *
 * [RulesEngine] is the single source of legal moves: callers (e.g. the Phase-3
 * ViewModel) only ever apply a [Move] that is a member of [legalMoves] for the
 * current board. [applyMove] is therefore a pure transform and deliberately does
 * not re-validate — re-validating would duplicate authority. Out-of-spec
 * positions resolve to [Cell.Blocked] via [Board.cellAt], so the engine never
 * crashes on a malformed coordinate.
 *
 * Move enumeration is **deterministically sorted**, establishing the stable
 * enumeration discipline the Phase-3 generator's determinism contract depends on.
 */
object RulesEngine {
    /**
     * Orthogonal jump directions in a fixed enumeration order. Diagonals are
     * intentionally absent — both game modes use orthogonal-only rules.
     */
    private val DIRECTIONS: List<Direction> =
        listOf(
            Direction(rowDelta = 0, colDelta = -1),
            Direction(rowDelta = 0, colDelta = 1),
            Direction(rowDelta = -1, colDelta = 0),
            Direction(rowDelta = 1, colDelta = 0),
        )

    /** A legal jump spans two cells along one axis; the jumped peg is the midpoint. */
    private const val JUMP_SPAN = 2

    /**
     * All legal jumps from [board], in a stable order sorted by
     * `(from.row, from.col, to.row, to.col)`. A jump is legal when a peg at `from`
     * can leap an adjacent peg (`over`) into an in-spec empty hole (`to`).
     */
    fun legalMoves(board: Board): List<Move> =
        buildList {
            for (from in board.spec.inPlay) {
                if (board.cellAt(from) != Cell.Peg) continue
                for (direction in DIRECTIONS) {
                    val over = Position(row = from.row + direction.rowDelta, col = from.col + direction.colDelta)
                    val to =
                        Position(
                            row = from.row + JUMP_SPAN * direction.rowDelta,
                            col = from.col + JUMP_SPAN * direction.colDelta,
                        )
                    if (to in board.spec &&
                        board.cellAt(over) == Cell.Peg &&
                        board.cellAt(to) == Cell.Empty
                    ) {
                        add(Move(from = from, over = over, to = to))
                    }
                }
            }
        }.sortedWith(
            compareBy({ it.from.row }, { it.from.col }, { it.to.row }, { it.to.col }),
        )

    /**
     * Applies [move] to [board], returning a new immutable board: the source peg
     * and the jumped-over peg are removed and one peg is placed at the destination.
     */
    fun applyMove(board: Board, move: Move): Board =
        board
            .withoutPeg(move.from)
            .withoutPeg(move.over)
            .withPeg(move.to)

    /** Whether no legal move remains on [board]. */
    fun isGameOver(board: Board): Boolean = legalMoves(board).isEmpty()

    /** The number of pegs currently on [board]. */
    fun pegCount(board: Board): Int = board.pegCount

    /** An orthogonal step vector used to enumerate candidate jumps. */
    private data class Direction(
        val rowDelta: Int,
        val colDelta: Int,
    )
}
