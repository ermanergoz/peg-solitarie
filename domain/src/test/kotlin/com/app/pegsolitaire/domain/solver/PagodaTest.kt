package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for [Pagoda.mayBeSolvable] — the O(cells) necessary-condition
 * fast-reject that lets the Solver drop provably-dead positions without search.
 *
 * The contract is one-directional: it may only return `false` when a position is
 * genuinely unsolvable, and MUST never reject a solvable one (a false reject would
 * make the game tell a player that a winnable board is dead).
 */
class PagodaTest {
    @Test
    fun rejectsAKnownDeadPosition() {
        // Two pegs on the same mod-3 colour class with no peg between them: the
        // colour-parity necessary condition proves this cannot reach a single peg.
        assertThat(Pagoda.mayBeSolvable(SolverSamples.deadBoard)).isFalse()
    }

    @Test
    fun neverRejectsASolvablePosition() {
        // ONE_PEG, not ALL: `mayBeSolvable` asks "can this still reach a SINGLE peg",
        // and Diamond-41 correctly answers no from every position (its floor is two).
        // Widening this to per-board targets is plan 08-04's job.
        SolutionFixtures.ONE_PEG.forEach { fixture ->
            val line = RecordedSolutions.load(fixture.name)
            val start = SolverSamples.startBoard(fixture)
            // Every prefix of a real winning line is, by construction, still solvable —
            // a necessary condition must accept all of them.
            for (prefixLength in 0 until line.size) {
                val board = SolverSamples.replay(start, line.take(prefixLength))
                assertThat(Pagoda.mayBeSolvable(board)).isTrue()
            }
        }
    }
}
