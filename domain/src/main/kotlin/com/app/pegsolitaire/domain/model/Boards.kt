package com.app.pegsolitaire.domain.model

/**
 * Scoped layout constants + derived footprint for the canonical English-33 board.
 *
 * The board is the classic 7x7 lattice with the four 2x2 corner blocks removed,
 * forming a plus/cross of 33 holes. A position is in-play iff it falls within the
 * central vertical band (a full-height column strip) OR the central horizontal
 * band (a full-width row strip) — every other 2x2 corner coordinate is blocked.
 *
 * No literals leak into logic: the grid dimension, the central band, and the
 * center hole are named constants here (QUAL-03).
 */
private object English33Layout {
    /** The board is a square lattice of this many rows and columns. */
    const val GRID_DIMENSION = 7

    /** Inclusive lower bound of the central (cross) band, in row/col indices. */
    private const val CENTRAL_BAND_MIN = 2

    /** Inclusive upper bound of the central (cross) band, in row/col indices. */
    private const val CENTRAL_BAND_MAX = 4

    /** Row/col indices that make up the cross arms (both bands share this range). */
    private val centralBand = CENTRAL_BAND_MIN..CENTRAL_BAND_MAX

    /** The center hole — empty at the start of an English-33 game and the solve target (D-01). */
    val center = Position(row = 3, col = 3)

    /** Every in-play coordinate of the English-33 cross, generated from the layout rules. */
    val inPlayPositions: List<Position> =
        buildList {
            for (row in 0 until GRID_DIMENSION) {
                for (col in 0 until GRID_DIMENSION) {
                    if (row in centralBand || col in centralBand) {
                        add(Position(row = row, col = col))
                    }
                }
            }
        }
}

/**
 * Canonical board specs and their initial states.
 *
 * Phase 1 ships exactly one board — English-33 — with a single center-empty start
 * (D-01). The same `Long`-bitmask scheme carries forward unchanged to the other
 * Classic boards in Phase 2 (all footprints are <= 64 holes).
 */
object Boards {
    /** The English-33 cross footprint, with the center hole as the solve target. */
    val English33: BoardSpec =
        BoardSpec(
            inPlay = English33Layout.inPlayPositions,
            target = English33Layout.center,
        )

    /** The standard English-33 start: every hole filled except the center → 32 pegs. */
    val english33Initial: Board =
        English33.inPlay
            .fold(Board(spec = English33, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(English33Layout.center)
}
