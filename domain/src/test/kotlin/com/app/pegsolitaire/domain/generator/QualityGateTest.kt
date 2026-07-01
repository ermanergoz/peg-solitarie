package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Output-agnostic property spec for the 03-02 quality gates + deterministic
 * reject-and-reseed (CHAL-04, Pitfall 3).
 *
 * These are *property* assertions (not byte fixtures), so they precede — and
 * survive — the golden-master determinism lock in Task 3. Magic numbers (the
 * sampled level set) are intentional and acceptable in test code — the sample IS
 * the spec.
 *
 * The six gates asserted over every sampled level:
 *  - non-trivial depth (`solution.size >= tier.minSolutionLength`);
 *  - peg-density bounds (`pegCount in tier.pegRange`);
 *  - branching-factor floor (`legalMoves(start).size >= tier.minOpeningMoves`);
 *  - forced-line rejection (the opening `FORCED_LINE_PREFIX` moves are not each the
 *    only legal move);
 *  - spatial spread (bounding-box spread `>= tier.minSpread`);
 *  - symmetry-normalized dedup (no two starting boards in a sliding window of
 *    consecutive levels share a symmetry-canonical `pegs` key).
 *
 * Plus the two whole-pipeline invariants: reject-and-reseed stays
 * regenerate-identical, and generation is bounded (terminates for every n).
 */
class QualityGateTest {
    private val sampleLevels = listOf(1, 2, 3, 5, 10, 25, 40, 100, 1000)

    @Test
    fun everyGeneratedSolutionMeetsTheMinimumSolutionLengthFloor() {
        sampleLevels.forEach { n ->
            val level = LevelGenerator.generate(n)
            val tier = DifficultyTiers.forLevel(n)
            assertThat(level.solution.size).isAtLeast(tier.minSolutionLength)
        }
    }

    @Test
    fun everyStartingBoardPegCountStaysWithinTheTierDensityBand() {
        sampleLevels.forEach { n ->
            val level = LevelGenerator.generate(n)
            val tier = DifficultyTiers.forLevel(n)
            assertThat(level.startingBoard.pegCount).isIn(tier.pegRange.toList())
        }
    }

    @Test
    fun everyOpeningOffersAtLeastTheBranchingFloorOfLegalMoves() {
        sampleLevels.forEach { n ->
            val level = LevelGenerator.generate(n)
            val tier = DifficultyTiers.forLevel(n)
            assertThat(RulesEngine.legalMoves(level.startingBoard).size)
                .isAtLeast(tier.minOpeningMoves)
        }
    }

    @Test
    fun theOpeningIsNotAScriptedForcedLine() {
        sampleLevels.forEach { n ->
            val level = LevelGenerator.generate(n)
            val prefix = minOf(GeneratorConstants.FORCED_LINE_PREFIX, level.solution.size)

            // At least one of the first K positions must offer more than one legal move —
            // otherwise the opening is a single forced line (feels scripted, D-06).
            var board = level.startingBoard
            var anyBranching = false
            for (i in 0 until prefix) {
                if (RulesEngine.legalMoves(board).size > 1) anyBranching = true
                board = RulesEngine.applyMove(board, level.solution[i])
            }
            assertThat(anyBranching).isTrue()
        }
    }

    @Test
    fun startingPegsAreNotClumpedBelowTheTierSpreadFloor() {
        sampleLevels.forEach { n ->
            val level = LevelGenerator.generate(n)
            val tier = DifficultyTiers.forLevel(n)
            assertThat(boundingBoxSpread(level.startingBoard)).isAtLeast(tier.minSpread)
        }
    }

    @Test
    fun noTwoBoardsInASlidingWindowShareASymmetryCanonicalKey() {
        val window = GeneratorConstants.DEDUP_WINDOW
        val firstLevel = 1
        val lastLevel = window * 4
        val keys =
            (firstLevel..lastLevel).map { n ->
                canonicalKey(LevelGenerator.generate(n).startingBoard)
            }

        // Within any window of `DEDUP_WINDOW` consecutive levels no canonical key repeats.
        keys.indices.forEach { i ->
            val windowStart = maxOf(0, i - window + 1)
            for (j in windowStart until i) {
                assertThat(keys[i]).isNotEqualTo(keys[j])
            }
        }
    }

    @Test
    fun rejectAndReseedStaysRegenerateIdenticalThroughTheGates() {
        sampleLevels.forEach { n ->
            val first = LevelGenerator.generate(n)
            val second = LevelGenerator.generate(n)

            assertThat(second.startingBoard.pegs).isEqualTo(first.startingBoard.pegs)
            assertThat(second.startingBoard.spec).isSameInstanceAs(first.startingBoard.spec)
            assertThat(second.solution).isEqualTo(first.solution)
        }
    }

    @Test
    fun generationTerminatesForEverySampledLevel() {
        // A bounded reject-and-reseed loop must never hang: reaching this assertion
        // at all proves each sampled generate(n) returned within MAX_ATTEMPTS.
        sampleLevels.forEach { n ->
            assertThat(LevelGenerator.generate(n).startingBoard.pegCount).isGreaterThan(0)
        }
    }

    private fun canonicalKey(board: Board): Long =
        BoardCanonical.canonical(board.pegs, BoardCanonical.symmetryPermutations(board.spec))

    private fun boundingBoxSpread(board: Board): Int {
        val pegs = board.spec.inPlay.filter { board.cellAt(it) == Cell.Peg }
        val minRow = pegs.minOf { it.row }
        val maxRow = pegs.maxOf { it.row }
        val minCol = pegs.minOf { it.col }
        val maxCol = pegs.maxOf { it.col }
        return (maxRow - minRow) + (maxCol - minCol)
    }
}
