package com.app.pegsolitaire.domain.golden.solutions

import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import java.io.File

/**
 * Loads the committed recorded-solution fixtures back into a `List<Move>` for the
 * shipped [SolutionTest] replay (D-19).
 *
 * Fixtures are written by [AuthoringSolver.writeFixtures] using
 * `BoardCodec.encodeMoves` — one move per line as `(r,c)->(r,c)->(r,c)` — so this
 * decoder is the exact inverse of that encoding. No search runs here: replay only
 * folds the committed line through `RulesEngine`.
 */
object RecordedSolutions {
    private const val MOVE_ARROW = "->"
    private const val OPEN_PAREN = "("
    private const val CLOSE_PAREN = ")"
    private const val FIELD_SEPARATOR = ","
    private const val FROM_OVER_TO = 3
    private const val ROW_INDEX = 0
    private const val COL_INDEX = 1

    /** Loads and decodes the winning line committed for [fixtureName] (e.g. `"european37"`). */
    fun load(fixtureName: String): List<Move> {
        val file = File(SolutionFixtures.DIRECTORY, "$fixtureName.txt")
        require(file.exists()) {
            "Missing recorded-solution fixture '$fixtureName.txt' at ${file.path}. " +
                "Re-run with UPDATE_GOLDEN=true to (re)generate fixtures."
        }
        return file
            .readText()
            .trim()
            .lineSequence()
            .filter { it.isNotBlank() }
            .map(::decodeMove)
            .toList()
    }

    private fun decodeMove(line: String): Move {
        val parts = line.split(MOVE_ARROW)
        require(parts.size == FROM_OVER_TO) { "Malformed move '$line': expected from->over->to." }
        return Move(
            from = decodePosition(parts[0]),
            over = decodePosition(parts[1]),
            to = decodePosition(parts[2]),
        )
    }

    private fun decodePosition(token: String): Position {
        val fields =
            token
                .trim()
                .removePrefix(OPEN_PAREN)
                .removeSuffix(CLOSE_PAREN)
                .split(FIELD_SEPARATOR)
        require(fields.size == 2) { "Malformed position '$token': expected (row,col)." }
        return Position(
            row = fields[ROW_INDEX].trim().toInt(),
            col = fields[COL_INDEX].trim().toInt(),
        )
    }
}
