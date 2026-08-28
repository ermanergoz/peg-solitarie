package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell

/**
 * An O(cells) necessary-condition fast-reject for peg-solitaire solvability — the
 * resource-count / "pagoda" guard the [Solver] runs before any search so provably-
 * dead positions collapse to `null` without exploring a single node (threat T-04-SD).
 *
 * The condition is the mod-3 colour-parity invariant of the same GF(4)-style colour
 * family documented for Diamond-41 (`SolutionFixtures`). Colour every cell by
 * `(row + col) mod 3` and, independently, by `(row - col) mod 3`. Because the three
 * cells of any orthogonal jump are colinear and consecutive, each jump touches all
 * three colours of a colouring exactly once (destination `+1`, the two sources `-1`),
 * so a single move flips the parity of ALL three colour counts. After the
 * `pegCount - 1` moves needed to reach one peg, each colouring's parity vector is
 * therefore forced, and the terminal single-peg state has exactly ONE odd colour
 * class. A position whose forced terminal parity is not weight-1 (for EITHER
 * colouring) can never reach a single peg — so it is rejected.
 *
 * This is a NECESSARY (one-directional) condition: it returns `false` only for
 * provably-unsolvable positions and never rejects a solvable one, so it is safe to
 * short-circuit on. It is fully board-agnostic (freeform Challenge footprints and
 * every Classic spec alike) — when it cannot prove unsolvability it returns `true`
 * and the search runs.
 */
object Pagoda {
    /**
     * Number of colour classes in each colouring (the invariant is taken mod 3).
     *
     * `internal` so [SolverTables] builds exactly this many colour-class masks rather
     * than declaring a second `3` of its own (CLAUDE.md: no magic numbers).
     */
    internal const val COLORING_MODULUS = 3

    /** A reachable single-peg terminal has exactly one odd colour class per colouring. */
    private const val SINGLE_PEG_ODD_CLASSES = 1

    /**
     * `true` when [board] passes the mod-3 colour-parity necessary condition (may be
     * solvable), `false` only when it is provably unable to reach a single peg.
     */
    fun mayBeSolvable(board: Board): Boolean {
        val pegCount = board.pegCount
        return terminalParityIsSinglePeg(board, pegCount) { row, col -> row + col } &&
            terminalParityIsSinglePeg(board, pegCount) { row, col -> row - col }
    }

    /**
     * Whether the parity of the [coordinate] colouring, advanced by the `pegCount - 1`
     * forced moves, lands on a weight-1 (single-peg) parity vector.
     */
    private inline fun terminalParityIsSinglePeg(
        board: Board,
        pegCount: Int,
        coordinate: (Int, Int) -> Int,
    ): Boolean {
        val classParity = BooleanArray(COLORING_MODULUS)
        for (position in board.spec.inPlay) {
            if (board.cellAt(position) != Cell.Peg) continue
            val residue = Math.floorMod(coordinate(position.row, position.col), COLORING_MODULUS)
            classParity[residue] = !classParity[residue]
        }
        // Each move flips all three colour parities; reaching one peg takes pegCount-1 moves.
        val movesFlipParity = ((pegCount - 1) and 1) == 1
        var oddClasses = 0
        for (parity in classParity) {
            if (parity xor movesFlipParity) oddClasses++
        }
        return oddClasses == SINGLE_PEG_ODD_CLASSES
    }
}
