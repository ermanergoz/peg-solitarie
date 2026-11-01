package com.app.pegsolitaire.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.ui.game.GameUiState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A mid-game board for the palette's pixel tests (docs/ux/colour-vision.md §3.4, CV-AC05): a held
 * peg with its legal targets showing, and a revealed hint on a different move, placed so that no
 * mark the tests sample overlaps another — the held peg at least two cells from both ends of the
 * hint, and the hint's target not one of the held peg's targets.
 *
 * @property state the board with the selection, Show-moves on and the hint.
 * @property selection a legal move of the held peg; its target carries a legal-target mark.
 * @property hint the revealed move.
 */
internal class PaletteScene(
    val state: GameUiState,
    val selection: Move,
    val hint: Move,
) {
    companion object {
        /** The first position along a greedy line of play from [start] that offers such a pair of moves. */
        fun midGame(start: Board): PaletteScene {
            var board = start
            repeat(MAX_OPENING_MOVES) {
                sceneOn(board)?.let { return it }
                board = RulesEngine.applyMove(board, RulesEngine.legalMoves(board).first())
            }
            error("No mid-game scene within $MAX_OPENING_MOVES moves of ${start.spec}")
        }

        private fun sceneOn(board: Board): PaletteScene? {
            val moves = RulesEngine.legalMoves(board)
            for (selection in moves) {
                val targets = moves.filter { it.from == selection.from }.map { it.to }
                val hint =
                    moves.firstOrNull {
                        it.from.cellsFrom(selection.from) >= CLEARANCE_CELLS &&
                            it.to.cellsFrom(selection.from) >= CLEARANCE_CELLS &&
                            it.to !in targets
                    }
                if (hint != null) {
                    val state =
                        GameUiState.initial(board).copy(
                            selected = selection.from,
                            legalTargets = targets,
                            showHints = true,
                            hintSource = hint.from,
                            hintTarget = hint.to,
                        )
                    return PaletteScene(state, selection, hint)
                }
            }
            return null
        }

        private fun Position.cellsFrom(other: Position): Int = max(abs(row - other.row), abs(col - other.col))

        private const val MAX_OPENING_MOVES = 6
        private const val CLEARANCE_CELLS = 2
    }
}

/**
 * The quadrant point of a ring that faces away from the hint's connector: a dash of a pattern that
 * starts at the top begins there too (eight dashes repeat every 45°), and nothing else is drawn
 * near it. [toward] is where the connector leaves this ring's centre for.
 */
internal fun quadrantAwayFrom(
    center: Position,
    toward: Position,
): Float =
    when {
        toward.row < center.row -> DOWN_DEGREES
        toward.row > center.row -> UP_DEGREES
        toward.col < center.col -> RIGHT_DEGREES
        else -> LEFT_DEGREES
    }

/** The pixel at [radius] from [center], [degrees] clockwise from three o'clock, inside [canvas]. */
internal fun Bitmap.pixelAt(
    canvas: Rect,
    center: Offset,
    radius: Float,
    degrees: Float,
): Int {
    val theta = Math.toRadians(degrees.toDouble())
    val x = canvas.left + center.x + radius * cos(theta).toFloat()
    val y = canvas.top + center.y + radius * sin(theta).toFloat()
    return getPixel(x.roundToInt(), y.roundToInt())
}

/** Whether [pixel] is [expected], each channel within [tolerance] (anti-aliasing and 8-bit rounding). */
internal fun isColour(
    pixel: Int,
    expected: Color,
    tolerance: Int = CHANNEL_TOLERANCE,
): Boolean {
    val want = expected.toArgb()
    return CHANNEL_SHIFTS.all { shift -> abs(((pixel shr shift) and BYTE) - ((want shr shift) and BYTE)) <= tolerance }
}

private const val UP_DEGREES = -90f
private const val DOWN_DEGREES = 90f
private const val LEFT_DEGREES = 180f
private const val RIGHT_DEGREES = 0f
private const val BYTE = 0xFF
private val CHANNEL_SHIFTS = listOf(16, 8, 0)

/** Per-channel allowance for a pixel read off a stroke's centre or a fill's interior. */
internal const val CHANNEL_TOLERANCE = 6

/** Half of one dash of an eight-dash ring, in degrees: the middle of the dash that starts at a quadrant point. */
internal const val HALF_DASH_DEGREES = 11.25f

/** One and a half dashes: the middle of the gap after the first dash. */
internal const val MID_GAP_DEGREES = 33.75f
