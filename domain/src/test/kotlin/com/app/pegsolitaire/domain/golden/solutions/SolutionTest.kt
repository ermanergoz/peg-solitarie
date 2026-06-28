package com.app.pegsolitaire.domain.golden.solutions

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Recorded-solution proof that the non-trivial Classic boards are solvable to one
 * peg (D-19), plus footprint sanity for all four boards added this phase.
 *
 * The shipped tests only REPLAY: each committed winning line ([RecordedSolutions])
 * is folded through the unchanged [RulesEngine], re-validating every step via
 * `RulesEngine.legalMoves` and asserting the board reduces to exactly one peg.
 * A single peg proves 3-star is reachable (D-01 fairness) without shipping a
 * Solver, and locks the off-center start for European-37.
 *
 * **Diamond-41's recorded-solution proof is DEFERRED** (see [SolutionFixtures]): its
 * 41-hole footprint defeats the authoring solver, so no fixture is committed and no
 * replay test exists for it yet. Its footprint and start peg-count are still covered
 * by the sanity checks below.
 *
 * The fixtures were GENERATED once by [AuthoringSolver] against the real `Boards.*`
 * values (Pitfall 7 — never hand-transcribe). Re-run with `UPDATE_GOLDEN=true` to
 * regenerate them; a normal run never searches or rewrites a fixture.
 */
class SolutionTest {
    @Test
    fun english33RecordedSolutionReducesToOnePeg() {
        assertReplayReducesToOnePeg(fixtureName = "english33", initial = Boards.english33Initial)
    }

    @Test
    fun european37RecordedSolutionReducesToOnePegFromOffCenterStart() {
        assertReplayReducesToOnePeg(fixtureName = "european37", initial = Boards.european37Initial)
    }

    @Test
    fun asymmetric39RecordedSolutionReducesToOnePeg() {
        assertReplayReducesToOnePeg(fixtureName = "asymmetric39", initial = Boards.asymmetric39Initial)
    }

    @Test
    fun wiegleb45RecordedSolutionReducesToOnePeg() {
        assertReplayReducesToOnePeg(fixtureName = "wiegleb45", initial = Boards.wiegleb45Initial)
    }

    @Test
    fun footprintsHaveTheExpectedHoleCounts() {
        assertThat(Boards.European37.inPlay.size).isEqualTo(EUROPEAN_37_HOLES)
        assertThat(Boards.Asymmetric39.inPlay.size).isEqualTo(ASYMMETRIC_39_HOLES)
        assertThat(Boards.Diamond41.inPlay.size).isEqualTo(DIAMOND_41_HOLES)
        assertThat(Boards.Wiegleb45.inPlay.size).isEqualTo(WIEGLEB_45_HOLES)
    }

    @Test
    fun initialsFillEveryHoleExceptTheSingleStartVacancy() {
        assertThat(Boards.european37Initial.pegCount).isEqualTo(EUROPEAN_37_HOLES - 1)
        assertThat(Boards.asymmetric39Initial.pegCount).isEqualTo(ASYMMETRIC_39_HOLES - 1)
        assertThat(Boards.diamond41Initial.pegCount).isEqualTo(DIAMOND_41_HOLES - 1)
        assertThat(Boards.wiegleb45Initial.pegCount).isEqualTo(WIEGLEB_45_HOLES - 1)
    }

    /**
     * Authoring-only regeneration of the committed fixtures (D-19). A no-op unless
     * `UPDATE_GOLDEN=true` is set, so a normal CI run performs zero search — the
     * search code only ever executes at authoring time.
     */
    @Test
    fun regenerateSolutionFixturesWhenUpdateFlagSet() {
        if (!isUpdateEnabled()) return
        AuthoringSolver.writeFixtures(File(SolutionFixtures.DIRECTORY))
    }

    private fun assertReplayReducesToOnePeg(
        fixtureName: String,
        initial: Board,
    ) {
        val moves = RecordedSolutions.load(fixtureName)
        val finalBoard =
            moves.fold(initial) { board, move ->
                assertWithMessage(
                    "Recorded move %s for '%s' is not legal on the replayed board.",
                    move,
                    fixtureName,
                ).that(move in RulesEngine.legalMoves(board)).isTrue()
                RulesEngine.applyMove(board, move)
            }
        assertWithMessage("'%s' recorded solution must reduce to one peg.", fixtureName)
            .that(finalBoard.pegCount)
            .isEqualTo(SOLVED_PEG_COUNT)
    }

    private fun isUpdateEnabled(): Boolean =
        isFlagTrue(System.getenv(UPDATE_GOLDEN_FLAG)) ||
            isFlagTrue(System.getProperty(UPDATE_GOLDEN_FLAG))

    private fun isFlagTrue(value: String?): Boolean = value?.equals("true", ignoreCase = true) == true

    private companion object {
        const val UPDATE_GOLDEN_FLAG = "UPDATE_GOLDEN"
        const val SOLVED_PEG_COUNT = 1
        const val EUROPEAN_37_HOLES = 37
        const val ASYMMETRIC_39_HOLES = 39
        const val DIAMOND_41_HOLES = 41
        const val WIEGLEB_45_HOLES = 45
    }
}
