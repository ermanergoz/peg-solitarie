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
 * Scoped layout constants + derived footprint for the European/Continental-37 board.
 *
 * A 7x7 lattice with the four 2-cell corner steps trimmed, forming a 37-hole
 * octagon. Each row is a single contiguous column band whose inset is given by
 * [cornerTrim]; a position is in-play iff its column falls within
 * `cornerTrim[row]..(GRID_DIMENSION - 1 - cornerTrim[row])`.
 *
 * Center-empty `(3,3)` is provably unsolvable to one peg (position-class parity),
 * so the game starts from the off-center [startVacancy] `(3,2)` — a documented
 * solvable single-vacancy start. D-02 ignores the target for stars, so the
 * single-peg finish still earns 3 stars even though it does not land in the start
 * hole (solvability is locked by a recorded-solution fixture, D-19).
 */
private object European37Layout {
    /** The board is a square lattice of this many rows and columns. */
    const val GRID_DIMENSION = 7

    /** Per-row corner inset: row R is in-play for `trim[R]..(dim-1-trim[R])`. */
    private val cornerTrim = intArrayOf(2, 1, 0, 0, 0, 1, 2)

    /** Off-center empty start hole (center is provably unsolvable). */
    val startVacancy = Position(row = 3, col = 2)

    /** Every in-play coordinate of the European-37 octagon, generated from the layout rules. */
    val inPlayPositions: List<Position> =
        buildList {
            for (row in 0 until GRID_DIMENSION) {
                val minCol = cornerTrim[row]
                val maxCol = GRID_DIMENSION - 1 - cornerTrim[row]
                for (col in minCol..maxCol) {
                    add(Position(row = row, col = col))
                }
            }
        }
}

/**
 * Scoped layout constants + derived footprint for George Bell's asymmetric
 * 3-3-2-2 cross (39 holes).
 *
 * An English-style 3-wide cross on an 8x8 bounding box whose four arms have
 * cyclic lengths 3,3,2,2 — genuinely asymmetric within its square box. A position
 * is in-play iff it lies in the central horizontal band ([rowBand]) OR the central
 * vertical band ([colBand]). The center `(4,3)` start is solvable to one peg (the
 * board is "completely solvable" at every vacancy).
 */
private object Asymmetric39Layout {
    /** The board is a square bounding box of this many rows and columns. */
    const val GRID_DIMENSION = 8

    /** Inclusive bounds of the central horizontal band (the full-width arms). */
    private const val ROW_BAND_MIN = 3
    private const val ROW_BAND_MAX = 5

    /** Inclusive bounds of the central vertical band (the 3-wide column strip). */
    private const val COL_BAND_MIN = 2
    private const val COL_BAND_MAX = 4

    private val rowBand = ROW_BAND_MIN..ROW_BAND_MAX
    private val colBand = COL_BAND_MIN..COL_BAND_MAX

    /** The center hole — empty at the start and a solvable single-vacancy start. */
    val center = Position(row = 4, col = 3)

    /** Every in-play coordinate of the asymmetric cross, generated from the layout rules. */
    val inPlayPositions: List<Position> =
        buildList {
            for (row in 0 until GRID_DIMENSION) {
                for (col in 0 until GRID_DIMENSION) {
                    if (row in rowBand || col in colBand) {
                        add(Position(row = row, col = col))
                    }
                }
            }
        }
}

/**
 * Scoped layout constants + derived footprint for the Diamond-41 board (a 9x9
 * square rotated 45 degrees; 41 holes).
 *
 * Each row is a contiguous column band centered on [CENTER_INDEX] whose half-width
 * is given by [halfWidth]; a position is in-play iff its column falls within
 * `(CENTER_INDEX - hw[row])..(CENTER_INDEX + hw[row])`.
 *
 * The diamond is the canonical center-empty board (40 pegs). Unlike the cross
 * boards it is NOT reducible to one peg: the GF(4) "colour" invariant
 * (sum of omega^(r+c) and omega^(r-c) over occupied cells, both conserved by every
 * jump) admits no single-peg target from the centre start, and search confirms the
 * floor is several pegs. So the diamond is scored on fewest pegs remaining (the
 * game's actual objective) rather than a one-peg solve — no D-19 fixture exists or
 * is possible for it. D-02 ignores the target for stars.
 */
private object Diamond41Layout {
    /** The board is a square lattice of this many rows and columns. */
    const val GRID_DIMENSION = 9

    /** The column index of the diamond's vertical axis of symmetry. */
    private const val CENTER_INDEX = 4

    /** Per-row half-width: row R spans `(center-hw[R])..(center+hw[R])`. */
    private val halfWidth = intArrayOf(0, 1, 2, 3, 4, 3, 2, 1, 0)

    /** The canonical center empty start hole (GF(4) invariant: not one-peg solvable; scored on fewest pegs left). */
    val startVacancy = Position(row = CENTER_INDEX, col = CENTER_INDEX)

    /** Every in-play coordinate of the diamond, generated from the layout rules. */
    val inPlayPositions: List<Position> =
        buildList {
            for (row in 0 until GRID_DIMENSION) {
                val minCol = CENTER_INDEX - halfWidth[row]
                val maxCol = CENTER_INDEX + halfWidth[row]
                for (col in minCol..maxCol) {
                    add(Position(row = row, col = col))
                }
            }
        }
}

/**
 * Scoped layout constants + derived footprint for J.C. Wiegleb's German cross
 * (1779; 45 holes).
 *
 * The English cross with every arm one cell longer: a 3-wide cross on a 9x9 grid
 * (equivalently 9x9 minus the four 3x3 corner blocks). A position is in-play iff
 * it lies in the central horizontal OR vertical band ([centralBand]). The center
 * `(4,4)` start is solvable to one peg in 22 moves (the Wiegleb central game).
 */
private object Wiegleb45Layout {
    /** The board is a square lattice of this many rows and columns. */
    const val GRID_DIMENSION = 9

    /** Inclusive bounds of the central (cross) band, shared by both arms. */
    private const val CENTRAL_BAND_MIN = 3
    private const val CENTRAL_BAND_MAX = 5

    private val centralBand = CENTRAL_BAND_MIN..CENTRAL_BAND_MAX

    /** The center hole — empty at the start and a solvable single-vacancy start. */
    val center = Position(row = 4, col = 4)

    /** Every in-play coordinate of the Wiegleb cross, generated from the layout rules. */
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
 * The Classic catalog is five all-orthogonal, square-grid boards (D-18), in
 * ascending hole-count order: English-33 (33), European-37 (37), Asymmetric
 * 3-3-2-2 (39), Diamond-41 (41), Wiegleb-45 (45). Every footprint is <= 64 holes,
 * so the same `Long`-bitmask scheme and the unchanged `RulesEngine` carry all five.
 *
 * European-37 starts OFF-CENTER (its center-empty position is provably unsolvable
 * to one peg, but an off-center start IS — locked by a fixture); the other four
 * start center-empty. The four cross boards (English-33, European-37, Asymmetric-39,
 * Wiegleb-45) each reduce to one peg, locked by a committed recorded-solution
 * fixture replayed through `RulesEngine` (D-19). Diamond-41 is the exception: the
 * GF(4) colour invariant proves its center start has no one-peg solution, so it is
 * scored on fewest pegs remaining (the original game's objective) — no fixture.
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

    /** The European/Continental-37 octagon; the off-center start hole is the (D-02-ignored) target. */
    val European37: BoardSpec =
        BoardSpec(
            inPlay = European37Layout.inPlayPositions,
            target = European37Layout.startVacancy,
        )

    /** The European-37 start: every hole filled except the off-center (3,2) vacancy → 36 pegs. */
    val european37Initial: Board =
        European37.inPlay
            .fold(Board(spec = European37, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(European37Layout.startVacancy)

    /** George Bell's asymmetric 3-3-2-2 cross (39 holes), with the center hole as target. */
    val Asymmetric39: BoardSpec =
        BoardSpec(
            inPlay = Asymmetric39Layout.inPlayPositions,
            target = Asymmetric39Layout.center,
        )

    /** The Asymmetric-39 start: every hole filled except the center (4,3) → 38 pegs. */
    val asymmetric39Initial: Board =
        Asymmetric39.inPlay
            .fold(Board(spec = Asymmetric39, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(Asymmetric39Layout.center)

    /** The Diamond-41 board; the center start hole is the (D-02-ignored) target. */
    val Diamond41: BoardSpec =
        BoardSpec(
            inPlay = Diamond41Layout.inPlayPositions,
            target = Diamond41Layout.startVacancy,
        )

    /** The Diamond-41 start: every hole filled except the center (4,4) vacancy → 40 pegs. */
    val diamond41Initial: Board =
        Diamond41.inPlay
            .fold(Board(spec = Diamond41, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(Diamond41Layout.startVacancy)

    /** J.C. Wiegleb's German cross (45 holes), with the center hole as the solve target. */
    val Wiegleb45: BoardSpec =
        BoardSpec(
            inPlay = Wiegleb45Layout.inPlayPositions,
            target = Wiegleb45Layout.center,
        )

    /** The Wiegleb-45 start: every hole filled except the center (4,4) → 44 pegs. */
    val wiegleb45Initial: Board =
        Wiegleb45.inPlay
            .fold(Board(spec = Wiegleb45, pegs = 0L)) { board, position -> board.withPeg(position) }
            .withoutPeg(Wiegleb45Layout.center)
}
