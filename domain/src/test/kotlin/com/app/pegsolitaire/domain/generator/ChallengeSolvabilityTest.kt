package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Output-agnostic invariant spec for [LevelGenerator] (CHAL-02 / CHAL-03).
 *
 * These are *property* assertions, not byte fixtures, so they survive the quality
 * gates and freeform footprints added in 03-02. Magic numbers (the sampled level
 * set) are intentional and acceptable in test code — the sample IS the spec.
 *
 * The two load-bearing guarantees:
 *  - **Solvable by construction (CHAL-02):** the recorded [solution] replays legally
 *    through `RulesEngine` and folds the starting board to a single peg.
 *  - **Deterministic (CHAL-03):** regenerating level N yields the identical board and
 *    winning line.
 */
class ChallengeSolvabilityTest {
    private val sampleLevels = listOf(1, 2, 3, 10, 100, 1000)

    @Test
    fun everyGeneratedSolutionReplaysLegallyAndFoldsToOnePeg() {
        sampleLevels.forEach { n ->
            val level = LevelGenerator.generate(n)

            val finalBoard =
                level.solution.fold(level.startingBoard) { board, move ->
                    // Each recorded move must be a legal forward move on the replayed board.
                    assertThat(move in RulesEngine.legalMoves(board)).isTrue()
                    RulesEngine.applyMove(board, move)
                }

            assertThat(finalBoard.pegCount).isEqualTo(1)
        }
    }

    @Test
    fun regeneratingALevelYieldsTheIdenticalBoardAndSolution() {
        sampleLevels.forEach { n ->
            val first = LevelGenerator.generate(n)
            val second = LevelGenerator.generate(n)

            assertThat(second.startingBoard.pegs).isEqualTo(first.startingBoard.pegs)
            assertThat(second.startingBoard.spec).isSameInstanceAs(first.startingBoard.spec)
            assertThat(second.solution).isEqualTo(first.solution)
        }
    }

    @Test
    fun nonPositiveLevelsClampToLevelOneWithoutCrashing() {
        val levelOne = LevelGenerator.generate(1)

        listOf(0, -5).forEach { n ->
            val clamped = LevelGenerator.generate(n)

            assertThat(clamped.levelNumber).isEqualTo(1)
            assertThat(clamped.startingBoard.pegs).isEqualTo(levelOne.startingBoard.pegs)
            assertThat(clamped.solution).isEqualTo(levelOne.solution)
        }
    }

    @Test
    fun everyBoardAndSolutionPositionStaysInsideTheFootprint() {
        sampleLevels.forEach { n ->
            val level = LevelGenerator.generate(n)
            val spec = level.startingBoard.spec

            level.solution.forEach { move ->
                assertThat(move.from in spec).isTrue()
                assertThat(move.over in spec).isTrue()
                assertThat(move.to in spec).isTrue()
            }
        }
    }

    @Test
    fun everyGeneratedLevelStampsTheGeneratorVersion() {
        sampleLevels.forEach { n ->
            assertThat(LevelGenerator.generate(n).generatorVersion)
                .isEqualTo(GeneratorConstants.GENERATOR_VERSION)
        }
    }
}
