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
 * (sum of omega^((r+c) mod 3) and omega^((r-c) mod 3) over occupied cells, both
 * conserved by every jump) admits no single-peg target from the centre start —
 * verified exhaustively over all 41 single-peg positions, zero of which pass.
 *
 * Its fewest-pegs floor is exactly **F = 2** (DEBT-02, D-05) — a beam search
 * superseded the earlier random-rollout estimate, which was far too weak to find
 * the true floor. Reachability is locked by the committed `diamond41.txt` line
 * replayed move by move through the unchanged `RulesEngine` in `SolutionTest`, and
 * optimality follows because F - 1 = 1 is precisely what the invariant excludes.
 * The reachability half is proven by the fixture landing in plan 08-06; if that
 * replay ever fails, [BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR] is NOT to be
 * adjusted — D-05's escalation clause fires instead.
 *
 * So the diamond is scored on fewest pegs remaining (the original game's
 * objective) rather than a one-peg solve, with its star bands re-anchored on F.
 * D-02 ignores the target for stars.
 */
private object Diamond41Layout {
    /** The board is a square lattice of this many rows and columns. */
    const val GRID_DIMENSION = 9

    /** The column index of the diamond's vertical axis of symmetry. */
    private const val CENTER_INDEX = 4

    /** Per-row half-width: row R spans `(center-hw[R])..(center+hw[R])`. */
    private val halfWidth = intArrayOf(0, 1, 2, 3, 4, 3, 2, 1, 0)

    /**
     * The canonical center empty start hole. The GF(4) invariant rules out a
     * one-peg finish from here, so the board is scored on fewest pegs left, with
     * a proven floor of F = 2 ([BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR]).
     */
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
 * scored on fewest pegs remaining (the original game's objective) down to its
 * proven floor of F = 2 ([BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR], DEBT-02 /
 * D-05). It DOES have a fixture — the committed line records a solve to F = 2
 * rather than to one peg, which is what makes the floor's reachability half a
 * replayed fact rather than a claim.
 *
 * Every spec's per-board target is carried by [BoardSpec.minimumPegs]; the shipped
 * footprints are published as [CLASSIC_SPECS].
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

    /**
     * The Diamond-41 board; the center start hole is the (D-02-ignored) target.
     *
     * The only shipped spec whose "perfect finish" is not one peg: its proven
     * fewest-pegs floor is F = 2 (DEBT-02, D-05), so it carries that target
     * explicitly rather than the [BoardConstants.MINIMUM_PEGS_DEFAULT].
     */
    val Diamond41: BoardSpec =
        BoardSpec(
            inPlay = Diamond41Layout.inPlayPositions,
            target = Diamond41Layout.startVacancy,
            minimumPegs = BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR,
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

    /**
     * The pure-domain catalog of shipped Classic footprints, in D-18 ascending
     * hole-count order (33, 37, 39, 41, 45).
     *
     * Consumed by the opening-book resource catalog and by the resume path's
     * legacy-blob target resolution, both of which recognise a structurally-rebuilt
     * spec by its [BoardSpec.footprint] rather than by referential identity (a spec
     * decoded from a snapshot is a different instance carrying the same positions).
     * The footprints are pairwise distinct, so such a lookup is never ambiguous.
     *
     * This list carries **footprints only**. `:app`'s `ClassicLevelSource` remains
     * the authority for display names and level ids — `:domain` never learns about
     * `ClassicLevelIds`.
     */
    val CLASSIC_SPECS: List<BoardSpec> =
        listOf(English33, European37, Asymmetric39, Diamond41, Wiegleb45)
}
