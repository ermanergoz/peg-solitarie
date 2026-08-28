package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.model.Board

/**
 * A necessary-condition fast-reject for peg-solitaire solvability — the resource-count
 * / "pagoda" guard the [Solver] runs before any search so provably-dead positions
 * collapse to `null` without exploring a single node (threat T-04-SD).
 *
 * The condition is the mod-3 colour-parity invariant of the same GF(4)-style colour
 * family documented for Diamond-41 (`SolutionFixtures`). Colour every cell by
 * `(row + col) mod 3` and, independently, by `(row - col) mod 3`. Because the three
 * cells of any orthogonal jump are colinear and consecutive, each jump touches all
 * three colours of a colouring exactly once (destination `+1`, the two sources `-1`),
 * so a single move flips the parity of ALL three colour counts. Reaching a `T`-peg
 * terminal from a `P`-peg position takes exactly `P - T` moves, so the terminal's
 * parity vector is forced: project the current vector forward by that many flips and
 * a `T`-peg terminal can have at most `T` odd classes. A position whose forced
 * terminal parity needs more odd classes than the board's target admits (for EITHER
 * colouring) can never reach that target — so it is rejected.
 *
 * **The target is per-board ([com.app.pegsolitaire.domain.model.BoardSpec.minimumPegs]),
 * not a hardcoded one peg.** Diamond-41's proven fewest-pegs floor is two, and while
 * this condition assumed a single-peg terminal it returned `false` for EVERY
 * Diamond-41 position — so `Solver.firstWinningMove` answered `null` in O(cells) with
 * zero search and the Diamond hint was structurally incapable of saying anything but
 * "no winning move" (DEBT-01).
 *
 * **RESEARCH Pitfall 1 — the prune weakens sharply above one peg, and on three colour
 * classes it weakens all the way to nothing.** The parity half of the test is
 * automatic: the number of odd classes always has the same parity as the peg count,
 * and the forward projection preserves that relation, so the surviving constraint is
 * only `oddClasses <= target`. At `T = 1` that is a real cut — it rejects exactly the
 * positions whose projected vector has all three classes odd, which is 1 of 8
 * admissible vectors and is what makes the four one-peg boards' searches tractable.
 * At `T >= 2` the projected count is bounded by 3 and parity-locked to `T`, so every
 * position passes: with three classes the invariant simply carries no information
 * about a multi-peg terminal. This is mathematics, not a defect, and it is asserted
 * as a fact in `PagodaTest` so nobody later "fixes" a bug that is not there.
 *
 * **Second-order consequence (the dangerous one, threat T-08-11).** A Diamond hint
 * that previously cost O(cells) and returned instantly can now launch a real,
 * unpruned search against the 1.5 s deadline — sustained CPU/battery/thermal load on a
 * board that used to cost nothing. The mitigation is named and load-bearing: the
 * **opening book** (plans 08-08/08-09/08-10) is the PRIMARY Diamond hint path, so the
 * solver is the rare fallback rather than the default. The node cap and the deadline
 * both still apply, and `GameViewModel.onHintRequested` already keeps a single search
 * in flight via `hintJob?.cancel()`.
 *
 * **Still one-directional.** `false` remains a PROOF of unreachability; `true` is only
 * "not excluded", never a proof of solvability. A false reject is the failure mode
 * that would make a hint lie in the worst possible direction — telling a player a
 * winnable board is dead — so `PagodaTest` replays every prefix of every committed
 * winning line through it.
 *
 * **Still board-agnostic.** Freeform Challenge footprints run through this too and
 * carry the default one-peg target, so their prune strength is exactly unchanged.
 */
object Pagoda {
    /**
     * Number of colour classes in each colouring (the invariant is taken mod 3).
     *
     * `internal` so [SolverTables] builds exactly this many colour-class masks rather
     * than declaring a second `3` of its own (CLAUDE.md: no magic numbers).
     */
    internal const val COLORING_MODULUS = 3

    /**
     * `true` when [board] passes the mod-3 colour-parity necessary condition (may be
     * able to reach its board's [com.app.pegsolitaire.domain.model.BoardSpec.minimumPegs]
     * target), `false` only when it is provably unable to.
     *
     * Evaluated with six popcounts over the per-footprint colour-class masks
     * precomputed in [SolverTables] — never by iterating the in-play cells.
     */
    fun mayBeSolvable(board: Board): Boolean {
        val tables = SolverTables.forSpec(board.spec)
        val target = board.spec.minimumPegs
        // Each move flips all three colour parities; reaching the target takes
        // pegCount - target moves, so an odd number of moves inverts the whole vector.
        val movesFlipParity = ((board.pegCount - target) and 1) == 1
        return terminalParityFitsTarget(board.pegs, tables.classPlus, target, movesFlipParity) &&
            terminalParityFitsTarget(board.pegs, tables.classMinus, target, movesFlipParity)
    }

    /**
     * Whether the [classMasks] colouring's parity vector, advanced by the forced moves
     * ([movesFlipParity]), lands on a vector a [target]-peg terminal could actually
     * have — at most [target] odd classes, with matching parity.
     */
    private fun terminalParityFitsTarget(
        pegs: Long,
        classMasks: LongArray,
        target: Int,
        movesFlipParity: Boolean,
    ): Boolean {
        var oddClasses = 0
        for (mask in classMasks) {
            val classIsOdd = ((pegs and mask).countOneBits() and 1) == 1
            if (classIsOdd xor movesFlipParity) oddClasses++
        }
        return oddClasses <= target && (oddClasses and 1) == (target and 1)
    }
}
