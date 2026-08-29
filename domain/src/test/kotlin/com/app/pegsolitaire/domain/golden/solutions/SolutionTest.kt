package com.app.pegsolitaire.domain.golden.solutions

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

/**
 * Recorded-solution proof that every non-trivial Classic board actually reaches its
 * own best achievable finish (D-19), plus footprint sanity for the boards added this
 * phase.
 *
 * The shipped tests only REPLAY: each committed line ([RecordedSolutions]) is folded
 * through the unchanged [RulesEngine], re-validating every step via
 * `RulesEngine.legalMoves` and asserting the board reduces to exactly that board's
 * `spec.minimumPegs`. Re-validating each step is what makes a fixture a **proof**
 * rather than a transcript: a tampered or hand-edited line fails here instead of
 * silently redefining what "proven" means (T-08-17). Reaching the target proves
 * 3-star is reachable (D-01 fairness) without shipping a Solver, and locks the
 * off-center start for European-37.
 *
 * **Diamond-41 has NO one-peg fixture by design** (see [SolutionFixtures]): the GF(4)
 * colour invariant proves its canonical center start cannot reduce to one peg —
 * exhaustively, over all 41 single-peg positions, in `DiamondFloorOptimalityTest`.
 * Its fewest-pegs floor is exactly **F = 2**
 * ([BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR]), not "several": reachability is
 * proven by `diamond41.txt` replaying to a two-peg dead end below, and optimality
 * follows because F - 1 = 1 is precisely what the invariant excludes. The diamond is
 * therefore scored on fewest pegs remaining (the original game's objective) rather
 * than a one-peg solve.
 *
 * If a replay here ever fails, [BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR] is NOT to
 * be adjusted to make it pass — D-05's escalation clause fires instead.
 *
 * The fixtures were GENERATED once by [AuthoringSolver] (and, for targets above one
 * peg, [BeamSearchAuthoring]) against the real `Boards.*` values (Pitfall 7 — never
 * hand-transcribe). Re-run with `UPDATE_GOLDEN=true` to regenerate them; a normal run
 * never searches or rewrites a fixture.
 */
class SolutionTest {
    @Test
    fun english33RecordedSolutionReducesToOnePeg() {
        assertReplayReachesBoardTarget(fixtureName = "english33", initial = Boards.english33Initial)
    }

    @Test
    fun european37RecordedSolutionReducesToOnePegFromOffCenterStart() {
        assertReplayReachesBoardTarget(fixtureName = "european37", initial = Boards.european37Initial)
    }

    @Test
    fun asymmetric39RecordedSolutionReducesToOnePeg() {
        assertReplayReachesBoardTarget(fixtureName = "asymmetric39", initial = Boards.asymmetric39Initial)
    }

    @Test
    fun wiegleb45RecordedSolutionReducesToOnePeg() {
        assertReplayReachesBoardTarget(fixtureName = "wiegleb45", initial = Boards.wiegleb45Initial)
    }

    /**
     * DEBT-02 (D-05a), the reachability half of the Diamond floor proof: the shipped
     * centre start really does reach two pegs, by a line this project's own
     * [RulesEngine] accepts move by move.
     */
    @Test
    fun diamond41RecordedSolutionReducesToItsProvenTwoPegFloor() {
        assertReplayReachesBoardTarget(fixtureName = DIAMOND_FIXTURE, initial = Boards.diamond41Initial)
    }

    /**
     * The target is only a FLOOR if the line actually runs out of moves there. A line
     * that merely stopped at two pegs with jumps still available would prove nothing
     * about the fewest pegs reachable.
     */
    @Test
    fun diamond41TerminalIsAGenuineDeadEnd() {
        val terminal = replayFixture(DIAMOND_FIXTURE, Boards.diamond41Initial)
        assertWithMessage("the Diamond-41 floor witness must have no legal move left")
            .that(RulesEngine.legalMoves(terminal))
            .isEmpty()
    }

    /**
     * The frozen floor and the committed witness must agree. Asserting the literal too
     * means a future edit to the constant cannot silently redefine what was proven.
     */
    @Test
    fun diamond41TargetIsTheFrozenProvenFloor() {
        assertThat(Boards.Diamond41.minimumPegs).isEqualTo(BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR)
        assertThat(Boards.Diamond41.minimumPegs).isEqualTo(DIAMOND_41_FLOOR_PEGS)
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

    /**
     * Replays [fixtureName] from [initial] and asserts it lands on **that board's**
     * `spec.minimumPegs` — one peg for the four cross boards, two for Diamond-41.
     * The target is read from the spec rather than from a file-local constant so
     * there is exactly one source of truth for it (plan 08-01 put it there).
     */
    private fun assertReplayReachesBoardTarget(
        fixtureName: String,
        initial: Board,
    ) {
        val finalBoard = replayFixture(fixtureName, initial)
        assertWithMessage("'%s' recorded solution must reduce to its board target.", fixtureName)
            .that(finalBoard.pegCount)
            .isEqualTo(initial.spec.minimumPegs)
    }

    /**
     * Folds the committed line for [fixtureName] forward from [initial], asserting
     * BEFORE each step that the recorded move is a member of
     * [RulesEngine.legalMoves] on the board it acts upon. That per-step guard is the
     * whole point of the fixture pipeline — it is what makes the file a proof.
     */
    private fun replayFixture(
        fixtureName: String,
        initial: Board,
    ): Board =
        RecordedSolutions.load(fixtureName).fold(initial) { board, move ->
            assertWithMessage(
                "Recorded move %s for '%s' is not legal on the replayed board.",
                move,
                fixtureName,
            ).that(move in RulesEngine.legalMoves(board)).isTrue()
            RulesEngine.applyMove(board, move)
        }

    private fun isUpdateEnabled(): Boolean =
        isFlagTrue(System.getenv(UPDATE_GOLDEN_FLAG)) ||
            isFlagTrue(System.getProperty(UPDATE_GOLDEN_FLAG))

    private fun isFlagTrue(value: String?): Boolean = value?.equals("true", ignoreCase = true) == true

    private companion object {
        const val UPDATE_GOLDEN_FLAG = "UPDATE_GOLDEN"
        const val DIAMOND_FIXTURE = "diamond41"

        /** The literal value of Diamond-41's frozen floor, asserted alongside the constant. */
        const val DIAMOND_41_FLOOR_PEGS = 2
        const val EUROPEAN_37_HOLES = 37
        const val ASYMMETRIC_39_HOLES = 39
        const val DIAMOND_41_HOLES = 41
        const val WIEGLEB_45_HOLES = 45
    }
}
