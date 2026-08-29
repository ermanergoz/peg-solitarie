package com.app.pegsolitaire.domain.assist

import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position

/**
 * RED BASELINE — the naive transcription of the [RecordedSolutions] decoder idiom,
 * committed only so the fail-closed contract in `OpeningBookFormatTest` has something
 * to fail against. Replaced in this plan's GREEN commit.
 */
internal object OpeningBookFormat {
    private const val MASK_MOVE_SEPARATOR = " "
    private const val GROUP_SEPARATOR = "-"
    private const val FIELD_SEPARATOR = ","
    private const val NO_WINNING_MOVE = "-"
    private const val HEX_RADIX = 16
    private const val TOKENS_PER_ENTRY = 2
    private const val GROUPS_PER_MOVE = 3
    private const val FIELDS_PER_GROUP = 2
    private const val MASK_INDEX = 0
    private const val MOVE_INDEX = 1
    private const val FROM_INDEX = 0
    private const val OVER_INDEX = 1
    private const val TO_INDEX = 2
    private const val ROW_INDEX = 0
    private const val COL_INDEX = 1

    /** Encodes one book entry line: the raw peg mask plus its proven first winning move. */
    fun encodeEntry(
        pegs: Long,
        move: Move?,
    ): String =
        java.lang.Long.toHexString(pegs) + MASK_MOVE_SEPARATOR +
            (move?.let(::encodeMove) ?: NO_WINNING_MOVE)

    /** Decodes one book entry line. Throws on malformed input — the defect under test. */
    fun decodeEntry(line: String): Pair<Long, Move?>? {
        val tokens = line.split(MASK_MOVE_SEPARATOR)
        require(tokens.size == TOKENS_PER_ENTRY) { "Malformed book entry '$line'." }
        val pegs = java.lang.Long.parseUnsignedLong(tokens[MASK_INDEX], HEX_RADIX)
        val moveToken = tokens[MOVE_INDEX]
        return pegs to if (moveToken == NO_WINNING_MOVE) null else decodeMove(moveToken)
    }

    private fun encodeMove(move: Move): String =
        encodePosition(move.from) + GROUP_SEPARATOR +
            encodePosition(move.over) + GROUP_SEPARATOR +
            encodePosition(move.to)

    private fun encodePosition(position: Position): String =
        position.row.toString() + FIELD_SEPARATOR + position.col.toString()

    private fun decodeMove(token: String): Move {
        val groups = token.split(GROUP_SEPARATOR)
        require(groups.size == GROUPS_PER_MOVE) { "Malformed move '$token'." }
        return Move(
            from = decodePosition(groups[FROM_INDEX]),
            over = decodePosition(groups[OVER_INDEX]),
            to = decodePosition(groups[TO_INDEX]),
        )
    }

    private fun decodePosition(group: String): Position {
        val fields = group.split(FIELD_SEPARATOR)
        require(fields.size == FIELDS_PER_GROUP) { "Malformed position '$group'." }
        return Position(row = fields[ROW_INDEX].toInt(), col = fields[COL_INDEX].toInt())
    }
}
