package com.app.pegsolitaire.domain.golden

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position

/**
 * A deterministic, human-diffable text encoding of [Board] and [Move] used by the
 * golden-master harness.
 *
 * A board is rendered as a row-major ASCII grid (`O` peg, `.` empty, space blocked)
 * for readable CI diffs, plus the backing `pegs` `Long` as hex for byte-exactness.
 * The encoding has no timestamps or ordering nondeterminism, so re-encoding the
 * same value always produces identical bytes.
 *
 * This codec is the load-bearing artifact Phase 3 reuses verbatim to snapshot
 * generator output — only the inputs change.
 */
object BoardCodec {
    private const val PEG_CHAR = 'O'
    private const val EMPTY_CHAR = '.'
    private const val BLOCKED_CHAR = ' '
    private const val LINE_SEPARATOR = "\n"
    private const val HEX_PREFIX = "pegs=0x"
    private const val MOVE_ARROW = "->"
    private const val EMPTY_MOVES = "(none)"
    private const val GRID_ORIGIN = 0

    /** Encodes [board] as an ASCII grid followed by a `pegs=0x<hex>` exactness line. */
    fun encodeBoard(board: Board): String {
        val rows = board.spec.inPlay.maxOf { it.row }
        val cols = board.spec.inPlay.maxOf { it.col }
        val grid =
            (GRID_ORIGIN..rows).joinToString(LINE_SEPARATOR) { row ->
                (GRID_ORIGIN..cols).joinToString(separator = "") { col ->
                    charFor(board.cellAt(Position(row = row, col = col))).toString()
                }
            }
        return grid + LINE_SEPARATOR + HEX_PREFIX + java.lang.Long.toHexString(board.pegs)
    }

    /** Encodes [moves] one-per-line as `(r,c)->(r,c)->(r,c)`, stable across runs. */
    fun encodeMoves(moves: List<Move>): String {
        if (moves.isEmpty()) return EMPTY_MOVES
        return moves.joinToString(LINE_SEPARATOR) { move ->
            encodePosition(move.from) + MOVE_ARROW + encodePosition(move.over) + MOVE_ARROW + encodePosition(move.to)
        }
    }

    private fun encodePosition(position: Position): String = "(${position.row},${position.col})"

    private fun charFor(cell: Cell): Char =
        when (cell) {
            Cell.Peg -> PEG_CHAR
            Cell.Empty -> EMPTY_CHAR
            Cell.Blocked -> BLOCKED_CHAR
        }
}
