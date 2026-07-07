package com.app.pegsolitaire.domain.solver

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The resource budget that bounds a single [Solver.firstWinningMove] search so an
 * untrusted board can never hang or drain a core (threat T-04-SD).
 *
 * @property maxNodes the maximum number of distinct (symmetry-canonical) positions
 *   the search may memoise before it stops expanding and reports "no move found"
 *   (folded to `null` per D-04). The forward peg count is strictly monotone-
 *   decreasing, so the search space is finite; this cap makes the WORST case bounded
 *   in memory as well as time.
 * @property deadline the wall-clock ceiling the CALLER enforces via the
 *   `shouldCancel` lambda it passes to the Solver. The domain stays coroutine-free —
 *   [Duration] is pure-JVM ([kotlin.time]) and carried here only as data; the
 *   ViewModel (04-03) turns it into an off-thread `shouldCancel { deadline passed }`.
 */
data class SolverBudget(
    val maxNodes: Long,
    val deadline: Duration,
) {
    companion object {
        /**
         * Default node cap — a memory/time SAFETY CEILING, not the operative production
         * limit. In production [DEFAULT_DEADLINE] governs: at the measured ~0.27M nodes/s
         * a 1.5s search reaches only ~0.4M nodes, far below this cap, so a hard board
         * (e.g. a large Classic footprint mid-game) is cut by the deadline and folds to
         * the "no winning move" state (D-04) long before this cap is approached. Gated by
         * [SolverBudgetTest][com.app.pegsolitaire.domain.solver]: it must stay >= 4x the
         * largest search observed over the TRACTABLE corpus (english33 end-to-end plus
         * every board's endgame tail) so a board the Solver is relied on to decide fast is
         * never falsely reported unsolvable. RAISE this value if the measurement ever
         * exceeds the 4x headroom — never lower the assertion.
         */
        const val DEFAULT_MAX_NODES = 40_000_000L

        /**
         * Default wall-clock ceiling. Solvable Classic positions resolve far inside a
         * second; this is the D-10 "thinking…" upper bound before the off-thread search
         * is abandoned to the same "no winning move" state as a budget miss (D-04).
         */
        val DEFAULT_DEADLINE = 1_500.milliseconds

        /**
         * Poll mask for the cooperative cancel check: the search tests `shouldCancel()`
         * once every `CANCEL_MASK + 1` (= 4096) node expansions — frequent enough to
         * stay responsive, rare enough to be free relative to the search itself.
         */
        const val CANCEL_MASK = 4_095L

        /** The shipped budget: the measured [DEFAULT_MAX_NODES] cap + [DEFAULT_DEADLINE]. */
        val DEFAULT = SolverBudget(maxNodes = DEFAULT_MAX_NODES, deadline = DEFAULT_DEADLINE)
    }
}
