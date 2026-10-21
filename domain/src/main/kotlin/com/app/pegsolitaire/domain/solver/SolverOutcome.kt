package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.model.Move

/**
 * What one [Solver.decide] search proved about a position (GD-HT-1).
 *
 * There are three outcomes, and the difference between the last two is the whole point:
 * a search reports what it **proved**, never why it stopped. Nothing here says whether a
 * search ran out of time or was superseded, because the solver cannot tell, and the
 * caller already can: a superseded search is never observed at all.
 *
 * Read by `GetHintUseCase` and the solver tests only. The UI sees the use case's own
 * outcome type, never this one.
 */
sealed interface SolverOutcome {
    /** The first move of a winning line the search completed from this position. */
    data class Found(
        val move: Move,
    ) : SolverOutcome

    /**
     * PROVED: no winning line exists from this position.
     *
     * Earned only three ways: the board is already at its target, the [Pagoda] condition
     * rejects it, or a search attempt exhausted its whole forward subtree without hitting
     * its node cap and without being cancelled.
     */
    data object ProvenDead : SolverOutcome

    /**
     * The search stopped with no move and no proof: the caller's cancel poll fired, the
     * attempt limit was spent, or the cumulative node ceiling was reached. It says nothing
     * about the position, which may still be winnable.
     */
    data object Undecided : SolverOutcome
}
