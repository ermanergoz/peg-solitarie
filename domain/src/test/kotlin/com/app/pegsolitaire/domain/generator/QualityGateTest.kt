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
 * The five quality gates asserted over every sampled level (the forced-line gate was
 * removed as dead code — WR-02/IN-03 — since the branching-factor floor already
 * guarantees the opening move is never the sole legal move):
 *  - non-trivial depth (`solution.size >= tier.minSolutionLength`);
 *  - peg-density bounds (`pegCount in tier.pegRange`);
 *  - branching-factor floor (`legalMoves(start).size >= tier.minOpeningMoves`);
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
    fun startingPegsAreNotClumpedBelowTheTierSpreadFloor() {
        sampleLevels.forEach { n ->
            val level = LevelGenerator.generate(n)
            val tier = DifficultyTiers.forLevel(n)
            assertThat(boundingBoxSpread(level.startingBoard)).isAtLeast(tier.minSpread)
        }
    }

    /**
     * The **duplicate-rejection** leg of the phase-3 validation contract row "Quality
     * gates reject trivial/degenerate/duplicate boards deterministically" (CHAL-04,
     * `03-VALIDATION.md`). Mapped back to the contract by v1.1 Phase 8 / DEBT-03, and
     * widened from `DEDUP_WINDOW * 4` to [DEDUP_SPAN_LEVELS] consecutive levels so the
     * span exceeds the 40-level audit floor.
     *
     * Scope is deliberately **window-pairwise, not span-global**: `DEDUP_WINDOW`'s
     * contract is "unique in the player's sequence", explicitly NOT a global uniqueness
     * guarantee, so asserting pairwise distinctness across the whole span would encode
     * an invariant the generator does not promise and would false-red on a legitimate
     * knob tune.
     */
    @Test
    fun noTwoBoardsInASlidingWindowShareASymmetryCanonicalKey() {
        val window = GeneratorConstants.DEDUP_WINDOW
        val keys = canonicalKeysOverSpan()

        // Within any window of `DEDUP_WINDOW` consecutive levels no canonical key repeats.
        keys.indices.forEach { i ->
            val windowStart = maxOf(0, i - window + 1)
            for (j in windowStart until i) {
                assertThat(keys[i]).isNotEqualTo(keys[j])
            }
        }
    }

    /**
     * The **deterministic** half of the same contract row: a reject-and-reseed must never
     * depend on iteration order or wall-clock, so regenerating a whole consecutive span
     * reproduces the identical *ordered* sequence of starting boards — not merely the
     * same board for an individually re-requested level.
     *
     * Added by v1.1 Phase 8 / DEBT-03 (the one leg the audit found genuinely unmapped).
     */
    @Test
    fun theDedupGateIsDeterministicAcrossRegeneration() {
        val first = startingPegsOverSpan()
        val second = startingPegsOverSpan()

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun rejectAndReseedStaysRegenerateIdenticalThroughTheGates() {
        sampleLevels.forEach { n ->
            val first = LevelGenerator.generate(n)
            val second = LevelGenerator.generate(n)

            // Freeform footprints are fresh BoardSpec instances per call, so identity
            // does not hold — but the CONTENT (footprint + pegs + solution) is fully
            // deterministic, which is what CHAL-03 requires.
            assertThat(second.startingBoard.pegs).isEqualTo(first.startingBoard.pegs)
            assertThat(second.startingBoard.spec.inPlay).isEqualTo(first.startingBoard.spec.inPlay)
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

    private fun canonicalKeysOverSpan(): List<Long> =
        (FIRST_SPAN_LEVEL..DEDUP_SPAN_LEVELS).map { n ->
            canonicalKey(LevelGenerator.generate(n).startingBoard)
        }

    private fun startingPegsOverSpan(): List<Long> =
        (FIRST_SPAN_LEVEL..DEDUP_SPAN_LEVELS).map { n ->
            LevelGenerator.generate(n).startingBoard.pegs
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

/** First level of the consecutive dedup span (the ladder always starts at level 1). */
private const val FIRST_SPAN_LEVEL = 1

/**
 * Length of the consecutive level span the dedup gate is exercised over — six
 * `DEDUP_WINDOW`s (48 levels), comfortably above the 40-level floor the DEBT-03 audit
 * set, while keeping `:domain:test` inside its seconds-scale per-commit budget.
 */
private const val DEDUP_SPAN_LEVELS = GeneratorConstants.DEDUP_WINDOW * 6
