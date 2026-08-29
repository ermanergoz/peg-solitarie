package com.app.pegsolitaire.domain.usecase

import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the pure [GetHintUseCase] hint decision (ASST-01 / ASST-03).
 *
 * Magic numbers (board coordinates, level numbers, prefix lengths) are intentional
 * and acceptable in test code — they ARE the specification of the sampled positions.
 * Challenge fixtures come from the deterministic [LevelGenerator] (each generated
 * level carries a recorded winning `solution`); a small hand-built strip supplies the
 * Classic (solution == null), already-decided, and provably-illegal cases.
 *
 * The two hard guarantees under test:
 *  - **recorded-line-first**: while a non-null `solution` covers `movesPlayed`, the
 *    recorded move is returned WITHOUT ever entering the Solver (proven by a
 *    `shouldCancel` spy that the Solver would have polled had it run);
 *  - **legality guard**: the recorded branch is taken only when that move is a member
 *    of `RulesEngine.legalMoves(board)` — a stale/illegal index falls through to the
 *    Solver rather than being returned blind (Pitfall 5).
 */
class GetHintUseCaseTest {
    private val useCase = GetHintUseCase()

    @Test
    fun onRecordedLineReturnsTheRecordedMoveWithoutInvokingTheSolver() {
        val level = LevelGenerator.generate(LEVEL_WITH_RECORDED_LINE)
        var solverPolled = 0

        val move =
            useCase(
                board = level.startingBoard,
                solution = level.solution,
                movesPlayed = 0,
                // If the Solver ran it would poll this ~every 4096 nodes; a `true`
                // return would also force it to null. A non-null recorded result with
                // zero polls proves the Solver branch was never entered.
                shouldCancel = {
                    solverPolled++
                    true
                },
            )

        assertThat(move).isEqualTo(level.solution[0])
        assertThat(solverPolled).isEqualTo(0)
        assertThat(RulesEngine.legalMoves(level.startingBoard)).contains(move)
    }

    @Test
    fun afterReplayingKRecordedMovesReturnsSolutionAtK() {
        val level = LevelGenerator.generate(LONGER_RECORDED_LINE)
        val k = level.solution.size / 2
        val board =
            level.solution.take(k).fold(level.startingBoard) { b, m -> RulesEngine.applyMove(b, m) }

        val move = useCase(board = board, solution = level.solution, movesPlayed = k)

        assertThat(move).isEqualTo(level.solution[k])
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    @Test
    fun classicNullSolutionAlwaysUsesTheSolverAndReturnsALegalMove() {
        val board = solvableStrip()

        val move = useCase(board = board, solution = null, movesPlayed = 0)

        assertThat(move).isNotNull()
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    @Test
    fun movesPlayedPastTheRecordedLineEndFallsThroughToTheSolver() {
        val level = LevelGenerator.generate(SHORT_LEVEL)

        val move =
            useCase(
                board = level.startingBoard,
                solution = level.solution,
                movesPlayed = level.solution.size,
            )

        assertThat(move).isNotNull()
        assertThat(RulesEngine.legalMoves(level.startingBoard)).contains(move)
    }

    @Test
    fun anIllegalRecordedMoveIsRejectedByTheLegalityGuardAndTheSolverIsUsed() {
        val board = solvableStrip()
        // A fabricated move whose `from` is the empty hole — never a member of
        // legalMoves(board); the guard must reject it and defer to the Solver.
        val bogus =
            Move(
                from = Position(row = 0, col = 2),
                over = Position(row = 0, col = 1),
                to = Position(row = 0, col = 0),
            )

        val move = useCase(board = board, solution = listOf(bogus), movesPlayed = 0)

        assertThat(move).isNotEqualTo(bogus)
        assertThat(move).isNotNull()
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    /**
     * **The DEBT-01 headline case.** Wiegleb-45's start is the shipped board whose one-peg
     * line is a committed fixture — it is provably winnable — yet RESEARCH measured
     * **0 of 76** of its depth-≤4 positions resolving inside the 1.5s deadline even at
     * ~15M nodes/s, the start included. Before the book was wired in, asking for a hint
     * here produced the "no winning move" rescue banner on a position that has a proven
     * winning move: a flat lie, and the defect this requirement exists to remove.
     *
     * The spy is the load-bearing part. `shouldCancel` returning `true` would force the
     * Solver to `null`, so a NON-NULL result with ZERO polls is a *proof* that the Solver
     * branch was never entered — not an inference from timing.
     */
    @Test
    fun theWiegleb45StartIsAnsweredFromTheBookWithoutTheSolverEverRunning() {
        val board = Boards.wiegleb45Initial
        var solverPolled = 0

        val move =
            useCase(
                board = board,
                solution = null,
                movesPlayed = 0,
                shouldCancel = {
                    solverPolled++
                    true
                },
            )

        assertThat(move).isNotNull()
        assertThat(solverPolled).isEqualTo(0)
        assertThat(RulesEngine.legalMoves(board)).contains(move)
    }

    @Test
    fun aSolverReturningNullYieldsNull() {
        // Two pegs separated by an empty hole: no legal jump, unsolvable → Solver null.
        val spec = BoardSpec(inPlay = (0..2).map { Position(row = 0, col = it) }, target = null)
        val board =
            Board(spec = spec, pegs = 0L)
                .withPeg(Position(row = 0, col = 0))
                .withPeg(Position(row = 0, col = 2))

        val move = useCase(board = board, solution = null, movesPlayed = 0)

        assertThat(move).isNull()
    }

    /** A 1x3 strip (pegs at cols 0,1; hole at col 2): exactly one legal move → solvable to one peg. */
    private fun solvableStrip(): Board =
        Board(spec = BoardSpec(inPlay = (0..2).map { Position(row = 0, col = it) }, target = null), pegs = 0L)
            .withPeg(Position(row = 0, col = 0))
            .withPeg(Position(row = 0, col = 1))

    private companion object {
        const val LEVEL_WITH_RECORDED_LINE = 3
        const val LONGER_RECORDED_LINE = 10
        const val SHORT_LEVEL = 1
    }
}
