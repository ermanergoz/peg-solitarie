package com.app.pegsolitaire.domain.usecase

import com.app.pegsolitaire.domain.assist.BookAnswer
import com.app.pegsolitaire.domain.assist.OpeningBook
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.solver.Solver
import com.app.pegsolitaire.domain.solver.SolverBudget

/**
 * The pure-`:domain` hint decision (ASST-01 / ASST-03 / DEBT-01): resolve the next
 * genuine winning move for the current [Board], or `null` when there is none.
 *
 * Three paths, in strict priority order. Each is listed with what it COSTS and what
 * makes it trustworthy, because those two things differ sharply between them:
 *  1. **Recorded line (Challenge, still on-line).** When a non-null [solution] is
 *     supplied AND it covers [movesPlayed], the recorded move `solution[movesPlayed]`
 *     is returned instantly with NO Solver call. Cost: an index. Trustworthy because
 *     the generated line is a proven, legal winning continuation produced with the
 *     level. The caller (`GameViewModel`) passes a non-null [solution] ONLY while the
 *     player is still on the recorded line (it tracks an `onRecordedLine` boolean,
 *     recomputed on undo); this use case never trusts that alone — see the guard below.
 *  2. **Opening book (Classic, inside the committed depth).** [OpeningBook.lookup]
 *     answers from the committed depth-K book. Cost: one map lookup, plus one classpath
 *     read the first time that board is consulted. Trustworthy because each entry was
 *     proven at AUTHORING time by replaying its complete winning line through the
 *     unchanged [RulesEngine] with an unbounded budget — so the player-facing deadline
 *     is irrelevant to a book hit, which is precisely why the book exists.
 *  3. **Solver fallback (off the book, or off the recorded line).** Otherwise defer to
 *     the budgeted [Solver.firstWinningMove]. Cost: up to the caller's whole deadline.
 *     Trustworthy only within its budget: unsolvable, budget-missed and cancelled all
 *     fold to the same `null` per D-04, so its `null` is "no move found in time", not a
 *     proof.
 *
 * **The three book outcomes are NOT interchangeable.** [BookAnswer.NoWinningMove] is
 * AUTHORITATIVE — a completed exhaustive search — so it short-circuits and returns
 * `null` without entering the Solver; re-running a 1.5s search against an already-proven
 * dead position would spend the whole deadline reaching the same conclusion less
 * reliably (threat T-08-37). A `null` lookup is a MISS — "the book does not know" — and
 * MUST fall through to the Solver. Collapsing the two would put the untruthful "no
 * winning move" banner back on winnable positions, which is the defect DEBT-01 exists to
 * remove (threat T-08-36).
 *
 * **One guard for both stored-move paths (Pitfall 5 / threat T-04-CT).** Blind
 * `solution[moveCount]` indexing is FORBIDDEN, and so is trusting a book entry. Both
 * stores can go stale relative to the board actually in play, and a highlighted move
 * gameplay then rejects is a visible correctness failure. So a stored move — from EITHER
 * store — is returned only when it is currently a member of [RulesEngine.legalMoves], the
 * same move authority as gameplay. The candidates are checked in priority order through a
 * SINGLE guard rather than one guard per store, so there are not two checks that can
 * drift apart; a rejected candidate simply falls through to the next path.
 *
 * **The two boards this changes most.** *Wiegleb-45*: RESEARCH measured **0 of 76** of
 * its depth-≤4 positions resolving inside the 1.5s deadline even at ~15M nodes/s — the
 * shipped start included, whose one-peg line is a committed fixture and so is provably
 * winnable. No constant-factor solver improvement closes a 40 000x gap; the book is what
 * makes those hints truthful, and it is load-bearing rather than an optimisation.
 * *Diamond-41*: its hint could previously only ever say "no winning move", because the
 * Solver targeted one peg on a board whose proven floor is two. The book is now the
 * PRIMARY path there and the Solver the rare fallback — which is the documented
 * mitigation for the weakened pagoda prune from plan 08-04, since that prune is provably
 * vacuous at a two-peg target and an off-book Diamond hint therefore launches a real
 * search.
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
        val fromBook = OpeningBook.lookup(board)
        // Stored-move candidates in strict priority order. `listOfNotNull` collapses to
        // empty for a Classic board the book misses, in which case the guard below never
        // evaluates its predicate and no legality set is built at all.
        val stored = listOfNotNull(solution?.getOrNull(movesPlayed), (fromBook as? BookAnswer.Hit)?.move)
        // THE single legality guard (T-04-CT), serving the recorded line and the book
        // alike. A candidate that is not legal on THIS board falls through to the next
        // path rather than being returned.
        val trusted = stored.firstOrNull { candidate -> candidate in RulesEngine.legalMoves(board) }
        return trusted ?: whenNoStoredMoveIsTrusted(board, fromBook, budget, shouldCancel)
    }

    /**
     * The answer when neither store produced a move this board accepts: `null` without a
     * search when the book AUTHORITATIVELY proved the position dead, otherwise the
     * budgeted Solver.
     *
     * Split out so the authoritative short-circuit is visible in the code's shape rather
     * than only in a comment — a miss and a proof are answered in different branches, and
     * the miss is the one that must reach the Solver.
     */
    private fun whenNoStoredMoveIsTrusted(
        board: Board,
        fromBook: BookAnswer?,
        budget: SolverBudget,
        shouldCancel: () -> Boolean,
    ): Move? =
        when (fromBook) {
            // AUTHORITATIVE: proven dead at authoring time by a search that COMPLETED.
            BookAnswer.NoWinningMove -> null
            // A Hit whose move this board rejected, or a MISS. Either way the book has
            // nothing usable to say and the Solver decides within its budget.
            else -> Solver.firstWinningMove(board, budget, shouldCancel)
        }
}
