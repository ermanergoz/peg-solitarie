package com.app.pegsolitaire.domain.usecase

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.solver.Solver
import com.app.pegsolitaire.domain.solver.SolverBudget

/**
 * The pure-`:domain` hint decision (ASST-01 / ASST-03): resolve the next genuine
 * winning move for the current [Board], or `null` when there is none.
 *
 * Two paths, in strict priority order:
 *  1. **Recorded line (Challenge, still on-line).** When a non-null [solution] is
 *     supplied AND it covers [movesPlayed], the recorded move `solution[movesPlayed]`
 *     is returned instantly with NO Solver call — the generated line is a proven,
 *     legal winning continuation. The caller (`GameViewModel`) passes a non-null
 *     [solution] ONLY while the player is still on the recorded line (it tracks an
 *     `onRecordedLine` boolean, recomputed on undo); this use case never trusts that
 *     alone — see the guard below.
 *  2. **Solver fallback (Classic, or deviated, or off-the-end).** Otherwise defer to
 *     the budgeted [Solver.firstWinningMove], which returns a legal first move of a
 *     winning line or `null` (unsolvable / budget-missed / cancelled, folded per D-04).
 *
 * **Guarded recorded branch (Pitfall 5 / threat T-04-CT).** Blind `solution[moveCount]`
 * indexing is FORBIDDEN. The recorded move is returned only when it is both in range
 * ([List.getOrNull] handles the bound) AND currently a member of
 * [RulesEngine.legalMoves] — the same move authority as gameplay. A stale index or an
 * illegal recorded move never leaks out; it falls through to the Solver instead.
 *
 * Purity (QUAL-01): no Android, no coroutines. Cancellation crosses the boundary as a
 * plain [shouldCancel] lambda the caller constructs (e.g. a deadline check), which is
 * forwarded verbatim to the Solver.
 */
class GetHintUseCase {
    /**
     * @param board the current post-move board the hint is for.
     * @param solution the Challenge recorded winning line, or `null` for Classic /
     *   once the player has deviated (the caller nulls it after deviation).
     * @param movesPlayed how many moves have been played from the starting board —
     *   the index into [solution] of the next recorded move.
     * @param budget the Solver resource budget (defaults to [SolverBudget.DEFAULT]).
     * @param shouldCancel cooperative cancel poll forwarded to the Solver.
     * @return the next winning [Move], or `null` when the position has no winning move.
     */
    operator fun invoke(
        board: Board,
        solution: List<Move>?,
        movesPlayed: Int,
        budget: SolverBudget = SolverBudget.DEFAULT,
        shouldCancel: () -> Boolean = { false },
    ): Move? {
        val recorded = solution?.getOrNull(movesPlayed)
        if (recorded != null && recorded in RulesEngine.legalMoves(board)) {
            return recorded
        }
        return Solver.firstWinningMove(board, budget, shouldCancel)
    }
}
