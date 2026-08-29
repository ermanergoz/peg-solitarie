package com.app.pegsolitaire.domain.assist

import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position

/**
 * The single definition — encoder AND decoder — of one opening-book entry line
 * (DEBT-01).
 *
 * ## Format
 *
 * One entry per line, in exactly one of two shapes:
 *
 * ```text
 * <pegsMaskHex> <fromRow>,<fromCol>-<overRow>,<overCol>-<toRow>,<toCol>
 * <pegsMaskHex> -
 * ```
 *
 * `<pegsMaskHex>` is the position's RAW [com.app.pegsolitaire.domain.model.Board.pegs]
 * mask in lowercase, unsigned, unpadded hex; the move is the proven first winning move
 * from that position. The lone `-` is the AUTHORITATIVE "no winning move" marker — a
 * position the authoring run *proved* has no winning line. It is a completely different
 * thing from a decode failure: a marker is truth the caller may act on, a decode failure
 * means the file cannot be trusted at all. The two must never be conflated, because
 * conflating them reintroduces exactly the untruthful "no winning move" banner DEBT-01
 * exists to remove.
 *
 * Comment lines (leading `#`) and blank lines are **the caller's** responsibility — the
 * loader in plan 08-09 filters them before it ever calls [decodeEntry], so this object
 * has exactly one notion of a valid line and no second, quieter one.
 *
 * ## Why the decoder cannot throw
 *
 * `RecordedSolutions`, the analogous fixture decoder, throws via `require` on a
 * malformed line. That is right for a test fixture and wrong here: this decoder runs
 * inside a shipped APK, against a resource that could be truncated or corrupt, on the
 * hint path. So the idiom copied is `SnapshotMapper.decode`'s instead — **every
 * malformed input yields `null`, and no input throws.** `NumberFormatException` is
 * caught by name rather than by a blanket `catch (Exception)`, so a genuine programming
 * error elsewhere still surfaces.
 *
 * A `null` from [decodeEntry] must make the whole board's book unavailable, not merely
 * drop that one line. A partial book is worse than no book: a missing entry silently
 * becomes a book MISS, and a miss falls through to the budgeted `Solver` — which on
 * Wiegleb-45 is precisely the four-orders-of-magnitude gap the book exists to close.
 *
 * ## Why this file is in `:domain` main
 *
 * The decoder must run in a shipped APK; the generator (the authoring tool under
 * `domain/src/test/.../golden/solutions/`) must not. Putting the ENCODER here too — and
 * having the test-source authoring tool call it — is deliberate: a decoder in main with
 * an encoder in test is exactly how a wire format drifts, and the round-trip test in
 * `OpeningBookFormatTest` is only meaningful if both sides are the same code.
 *
 * ## Encoding notes
 *
 * The mask is written with `java.lang.Long.toHexString` and read with
 * `java.lang.Long.parseUnsignedLong`, i.e. UNSIGNED, so a mask with bit 63 conceptually
 * set still round-trips. Board footprints are capped at 64 holes (the `Long` peg mask),
 * so the unsigned reading is the correct one — a signed parse would reject the top half
 * of the representable range for no reason.
 *
 * Decoding is strict rather than forgiving: the mask must be non-empty lowercase hex
 * with no sign, and every coordinate must be non-empty ASCII digits. Anything a caller
 * might consider "nearly right" (`1F`, `+1f`, a padded ` 2`) is refused. Strictness is
 * what makes the encoding CANONICAL, and a canonical encoding is what lets a regenerated
 * book be compared byte for byte against the committed one.
 *
 * Coordinates are non-negative by construction — they are footprint positions. A
 * negative coordinate cannot round-trip, because `-` is the group separator; it is
 * refused by [decodeEntry] rather than silently mis-parsed.
 */
internal object OpeningBookFormat {
    /** The single space between the mask token and the move token. */
    private const val MASK_MOVE_SEPARATOR = " "

    /** Separates the three `(row,col)` groups of an encoded move. */
    private const val GROUP_SEPARATOR = "-"

    /** Separates `row` from `col` inside one group. */
    private const val FIELD_SEPARATOR = ","

    /** The authoritative "this position has no winning move" marker. */
    private const val NO_WINNING_MOVE = "-"

    /** Base of the mask encoding. */
    private const val HEX_RADIX = 16

    /** Every character a canonical mask token may contain — lowercase, unsigned. */
    private const val HEX_DIGITS = "0123456789abcdef"

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

    /**
     * Encodes one book entry: [pegs] plus the proven first winning move from it, or
     * [NO_WINNING_MOVE] when the authoring run proved there is none.
     *
     * Deterministic — the same `(pegs, move)` always produces the same string — which is
     * what makes a regenerated book byte-comparable against the committed one.
     */
    fun encodeEntry(
        pegs: Long,
        move: Move?,
    ): String =
        java.lang.Long.toHexString(pegs) +
            MASK_MOVE_SEPARATOR +
            (move?.let(::encodeMove) ?: NO_WINNING_MOVE)

    /**
     * The exact inverse of [encodeEntry]: `(pegs, move)` for a well-formed entry,
     * `(pegs, null)` for the authoritative no-winning-move marker, and `null` — never an
     * exception — for anything else.
     *
     * The caller must treat `null` as "this book is unusable", not as "skip this line".
     */
    fun decodeEntry(line: String): Pair<Long, Move?>? {
        val tokens = line.split(MASK_MOVE_SEPARATOR)
        val pegs = if (tokens.size == TOKENS_PER_ENTRY) decodeMask(tokens[MASK_INDEX]) else null
        val moveToken = tokens.getOrNull(MOVE_INDEX)
        return when {
            pegs == null || moveToken == null -> null
            moveToken == NO_WINNING_MOVE -> pegs to null
            else -> decodeMove(moveToken)?.let { move -> pegs to move }
        }
    }

    private fun encodeMove(move: Move): String =
        encodePosition(move.from) +
            GROUP_SEPARATOR +
            encodePosition(move.over) +
            GROUP_SEPARATOR +
            encodePosition(move.to)

    private fun encodePosition(position: Position): String =
        position.row.toString() + FIELD_SEPARATOR + position.col.toString()

    /**
     * The unsigned hex mask, or `null` if [token] is not canonical hex or does not fit a
     * `Long`. The character check runs first so a `+`, a `0x` prefix or an uppercase
     * digit is refused outright; the `catch` then covers the only remaining failure,
     * a token of more than 16 digits.
     */
    private fun decodeMask(token: String): Long? =
        if (token.isEmpty() || !token.all { it in HEX_DIGITS }) {
            null
        } else {
            try {
                java.lang.Long.parseUnsignedLong(token, HEX_RADIX)
            } catch (ignored: NumberFormatException) {
                null
            }
        }

    private fun decodeMove(token: String): Move? {
        val groups = token.split(GROUP_SEPARATOR)
        val positions = groups.mapNotNull(::decodePosition)
        return if (groups.size == GROUPS_PER_MOVE && positions.size == GROUPS_PER_MOVE) {
            Move(
                from = positions[FROM_INDEX],
                over = positions[OVER_INDEX],
                to = positions[TO_INDEX],
            )
        } else {
            null
        }
    }

    private fun decodePosition(group: String): Position? {
        val fields = group.split(FIELD_SEPARATOR)
        val row = if (fields.size == FIELDS_PER_GROUP) decodeCoordinate(fields[ROW_INDEX]) else null
        val col = if (fields.size == FIELDS_PER_GROUP) decodeCoordinate(fields[COL_INDEX]) else null
        return if (row != null && col != null) Position(row = row, col = col) else null
    }

    /**
     * A non-negative coordinate, or `null`. ASCII digits only — `Char.isDigit` would
     * also admit non-ASCII digit code points, which `toIntOrNull` then parses, so two
     * different byte sequences would decode to the same entry and the encoding would
     * stop being canonical. `toIntOrNull` itself never throws, and returns `null` on
     * overflow.
     */
    private fun decodeCoordinate(field: String): Int? =
        if (field.isEmpty() || !field.all { it in ASCII_DIGITS }) null else field.toIntOrNull()

    /** The only characters a coordinate field may contain. */
    private val ASCII_DIGITS = '0'..'9'
}
