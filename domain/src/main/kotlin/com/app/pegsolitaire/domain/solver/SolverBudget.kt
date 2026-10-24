package com.app.pegsolitaire.domain.solver

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The resource budget that bounds a single [Solver.decide] search so an
 * untrusted board can never hang or drain a core (threat T-04-SD).
 *
 * @property maxNodes the maximum number of distinct (symmetry-canonical) positions
 *   the search may memoise CUMULATIVELY, across every restart attempt, before it stops
 *   expanding and reports [SolverOutcome.Undecided] (GD-HT-1). The forward peg
 *   count is strictly monotone-decreasing, so the search space is finite; this cap makes
 *   the WORST case bounded in TIME. Memory is bounded separately, by
 *   [perAttemptMaxNodes]: the search memo is per attempt and is discarded between
 *   restarts, so peak heap tracks the largest single attempt rather than this total.
 * @property deadline the wall-clock ceiling the CALLER enforces via the
 *   `shouldCancel` lambda it passes to the Solver. The domain stays coroutine-free —
 *   [Duration] is pure-JVM ([kotlin.time]) and carried here only as data; the
 *   ViewModel (04-03) turns it into an off-thread `shouldCancel { deadline passed }`.
 * @property perAttemptMaxNodes the cap on ONE deterministic restart attempt — the knob
 *   that makes the restart loop reachable at all. See [DEFAULT_PER_ATTEMPT_MAX_NODES].
 */
data class SolverBudget(
    val maxNodes: Long,
    val deadline: Duration,
    val perAttemptMaxNodes: Long = DEFAULT_PER_ATTEMPT_MAX_NODES,
) {
    companion object {
        /**
         * Default node cap — a memory/time SAFETY CEILING for the search as a WHOLE
         * (summed across every restart attempt), not the operative production limit and
         * not a per-attempt limit; that is [DEFAULT_PER_ATTEMPT_MAX_NODES]. In production
         * [DEFAULT_DEADLINE] governs: even at the bitboard loop's throughput a 1.5s search
         * stays far below this cap, so a hard board (e.g. a large Classic footprint
         * mid-game) is cut by the deadline and ends [SolverOutcome.Undecided] (GD-HT-1)
         * long before this cap is approached. Gated by
         * [SolverBudgetTest][com.app.pegsolitaire.domain.solver]: it must stay >= 4x the
         * largest search observed over the TRACTABLE corpus (english33 end-to-end plus
         * every board's endgame tail) so a board the Solver is relied on to decide fast is
         * never cut short undecided. RAISE this value if the measurement ever
         * exceeds the 4x headroom — never lower the assertion.
         *
         * **What the 08-05 bitboard rewrite did and did not change here (RESEARCH
         * Pitfall 2).** The headroom assertion still measures the TOTAL distinct
         * symmetry-canonical positions reported by [Solver.lastVisited] over the tractable
         * corpus — that meaning is unchanged. The rewrite changed what a node *costs*
         * (allocation-free mask operations instead of a `List<Move>` sort plus a `Board`
         * copy), not what `lastVisited` *counts*. Re-measured over the corpus after the
         * rewrite the maximum stayed inside the 4x band, so this number was neither raised
         * nor lowered; the standing instruction above still applies to the next reader.
         */
        const val DEFAULT_MAX_NODES = 40_000_000L

        /**
         * Cap on the distinct symmetry-canonical positions ONE search attempt may explore
         * before it is abandoned and a fresh deterministically-seeded restart begins. It
         * bounds a single attempt, never the search total — [DEFAULT_MAX_NODES] is the
         * cumulative ceiling and the two are independent.
         *
         * **Why it exists.** Without it every attempt is handed the whole
         * [DEFAULT_MAX_NODES], so the 1.5s deadline always fires inside attempt 0 and
         * `Solver.MAX_ATTEMPTS` is unreachable dead code in production: the solver performs
         * exactly one unlucky dive and gives up. Peg-solitaire DFS runtime is heavy-tailed,
         * and the textbook response to a heavy tail is many short restarts rather than one
         * long one.
         *
         * **The measurement that fixes the value.** European-37's full start resolves in
         * ~33 ms across 2 attempts at a 100k cap, versus ~172 ms for one unbounded attempt;
         * Asymmetric-39's in ~59 ms across 2 attempts at a 200k cap versus ~235 ms
         * unbounded.
         *
         * **This is also the memory knob.** The search memo is per attempt and discarded
         * between restarts, so peak heap tracks the largest single attempt — this value
         * grown by `Solver.MAX_CAP_ESCALATIONS` doublings — not [DEFAULT_MAX_NODES].
         *
         * **Standing instruction.** This is a RESTART-FREQUENCY knob, not a safety ceiling.
         * Lowering it makes restarts more frequent and each attempt shallower; raising it
         * approaches the old single-dive behaviour AND raises peak memory. Tune it against
         * measured full-start latency, and leave [DEFAULT_MAX_NODES] alone when you do.
         */
        const val DEFAULT_PER_ATTEMPT_MAX_NODES = 200_000L

        /**
         * Default wall-clock ceiling. Solvable Classic positions resolve far inside a
         * second; this is the D-10 "thinking…" upper bound before the off-thread search
         * is abandoned, and ends [SolverOutcome.Undecided] like a budget miss (GD-HT-1).
         *
         * **Frozen by D-07, and deliberately not per-board.** The 1.5s value is product UX,
         * not a tuning parameter: a truthful hint comes from a stronger search (the 08-05
         * bitboard rewrite and its restart policy) and a committed opening book, never from
         * making the player wait longer, and never from quietly granting the big boards a
         * longer think than the small ones.
         */
        val DEFAULT_DEADLINE = 1_500.milliseconds

        /**
         * Poll mask for the cooperative cancel check: the search tests `shouldCancel()`
         * once every `CANCEL_MASK + 1` (= 4096) node expansions — frequent enough to
         * stay responsive, rare enough to be free relative to the search itself.
         */
        const val CANCEL_MASK = 4_095L

        /**
         * The shipped budget: the measured [DEFAULT_MAX_NODES] cumulative ceiling, the
         * D-07-frozen [DEFAULT_DEADLINE], and the [DEFAULT_PER_ATTEMPT_MAX_NODES] restart cap.
         */
        val DEFAULT =
            SolverBudget(
                maxNodes = DEFAULT_MAX_NODES,
                deadline = DEFAULT_DEADLINE,
                perAttemptMaxNodes = DEFAULT_PER_ATTEMPT_MAX_NODES,
            )
    }
}
