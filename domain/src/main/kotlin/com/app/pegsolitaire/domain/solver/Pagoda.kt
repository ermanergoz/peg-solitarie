package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.model.Board

/**
 * TEMPORARY scaffold (Task 2) — the real necessary-condition fast-reject lands in
 * Task 3. Accepting every board is safe: it only means the Solver never short-
 * circuits and always runs its (sound) search, so correctness holds even before the
 * pagoda is implemented.
 */
object Pagoda {
    /** Placeholder: accepts every non-empty board until the Task-3 pagoda replaces this. */
    fun mayBeSolvable(board: Board): Boolean = board.pegCount >= 0
}
