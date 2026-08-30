package com.app.pegsolitaire.domain.model

import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.random.Random

/**
 * Behaviour spec for [BoardSpec]'s per-board "perfect finish" peg target
 * ([BoardSpec.minimumPegs]) and its structural identity key ([BoardSpec.footprint]).
 *
 * Magic numbers are intentional and acceptable in test code (detekt's `MagicNumber`
 * rule excludes the test source set) — the fixtures here ARE the specification of
 * the per-board target contract that the solver goal test, the star bands and the
 * opening-book lookup all read.
 */
class BoardSpecTest {
    private val english33Positions = Boards.English33.inPlay
    private val english33Center = Position(row = 3, col = 3)

    @Test
    fun minimumPegsDefaultsToOneWhenNotSpecified() {
        val spec = BoardSpec(inPlay = english33Positions, target = english33Center)

        assertThat(spec.minimumPegs).isEqualTo(BoardConstants.MINIMUM_PEGS_DEFAULT)
        assertThat(spec.minimumPegs).isEqualTo(1)
    }

    @Test
    fun minimumPegsHonoursAnExplicitPerBoardTarget() {
        val spec =
            BoardSpec(
                inPlay = english33Positions,
                target = english33Center,
                minimumPegs = 2,
            )

        assertThat(spec.minimumPegs).isEqualTo(2)
    }

    @Test
    fun aZeroMinimumPegsTargetIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException::class.java) {
            BoardSpec(inPlay = english33Positions, target = english33Center, minimumPegs = 0)
        }
    }

    @Test
    fun aNegativeMinimumPegsTargetIsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException::class.java) {
            BoardSpec(inPlay = english33Positions, target = english33Center, minimumPegs = -1)
        }
    }

    @Test
    fun aMinimumPegsTargetLargerThanTheFootprintIsRejectedAtConstruction() {
        val oversized = english33Positions.size + 1

        assertThrows(IllegalArgumentException::class.java) {
            BoardSpec(
                inPlay = english33Positions,
                target = english33Center,
                minimumPegs = oversized,
            )
        }
    }

    @Test
    fun footprintIsTheInPlayPositionsSortedByRowThenColumn() {
        val shuffled = english33Positions.shuffled(Random(SHUFFLE_SEED))
        val spec = BoardSpec(inPlay = shuffled, target = english33Center)

        val expected = english33Positions.sortedWith(compareBy({ it.row }, { it.col }))
        assertThat(spec.footprint).isEqualTo(expected)
    }

    @Test
    fun twoSpecsBuiltFromTheSamePositionsInDifferentOrdersShareOneStructuralKey() {
        val forward = BoardSpec(inPlay = english33Positions, target = english33Center)
        val reversed = BoardSpec(inPlay = english33Positions.reversed(), target = null)

        assertThat(forward.footprint).isEqualTo(reversed.footprint)
        assertThat(forward.footprint.hashCode()).isEqualTo(reversed.footprint.hashCode())
        // The point of the key: a rebuilt spec looks up as the spec it came from.
        assertThat(mapOf(forward.footprint to CATALOG_HIT)[reversed.footprint])
            .isEqualTo(CATALOG_HIT)
    }

    @Test
    fun bitAssignmentIsTheIndexWithinTheFootprintRegardlessOfInputOrder() {
        val shuffled = english33Positions.shuffled(Random(SHUFFLE_SEED))
        val spec = BoardSpec(inPlay = shuffled, target = english33Center)

        spec.footprint.forEachIndexed { expectedBit, position ->
            assertThat(spec.bit(position)).isEqualTo(expectedBit)
        }
        // The assignment is unchanged from before minimumPegs/footprint existed.
        spec.footprint.forEachIndexed { expectedBit, position ->
            assertThat(Boards.English33.bit(position)).isEqualTo(expectedBit)
        }
    }

    @Test
    fun theFourCrossBoardsCarryTheDefaultOnePegTarget() {
        listOf(
            Boards.English33,
            Boards.European37,
            Boards.Asymmetric39,
            Boards.Wiegleb45,
        ).forEach { spec ->
            assertThat(spec.minimumPegs).isEqualTo(BoardConstants.MINIMUM_PEGS_DEFAULT)
        }
    }

    @Test
    fun diamond41CarriesItsProvenFewestPegsFloorOfTwo() {
        // Assert the wiring (the named constant) AND the value it is frozen at.
        assertThat(Boards.Diamond41.minimumPegs)
            .isEqualTo(BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR)
        assertThat(Boards.Diamond41.minimumPegs).isEqualTo(2)
    }

    @Test
    fun classicSpecsListsAllFiveShippedBoardsInAscendingHoleCountOrder() {
        assertThat(Boards.CLASSIC_SPECS).hasSize(5)
        assertThat(Boards.CLASSIC_SPECS.map { it.size })
            .isEqualTo(listOf(33, 37, 39, 41, 45))
        // D-18's ordering contract, stated independently of the literal sizes.
        Boards.CLASSIC_SPECS.map { it.size }.zipWithNext().forEach { (smaller, larger) ->
            assertThat(smaller).isLessThan(larger)
        }
    }

    @Test
    fun classicSpecFootprintsArePairwiseDistinctSoAKeyedLookupIsNeverAmbiguous() {
        val keys = Boards.CLASSIC_SPECS.map { it.footprint }.toSet()

        assertThat(keys).hasSize(Boards.CLASSIC_SPECS.size)
    }

    @Test
    fun aSpecRebuiltFromAShippedFootprintKeysBackToTheBoardItCameFrom() {
        val shuffled = Boards.Diamond41.footprint.shuffled(Random(SHUFFLE_SEED))
        val rebuilt =
            BoardSpec(
                inPlay = shuffled,
                target = null,
                minimumPegs = BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR,
            )

        assertThat(rebuilt.footprint).isEqualTo(Boards.Diamond41.footprint)
        val catalog = Boards.CLASSIC_SPECS.associateBy { it.footprint }
        assertThat(catalog[rebuilt.footprint]).isSameInstanceAs(Boards.Diamond41)
    }

    /**
     * The RESEARCH §Security Domain check that the freeform generator never emits a
     * target above 1. A generated footprint is solvable-by-construction to one peg
     * (it is grown by a reverse walk from a single peg), so any other value would be
     * a generator defect rather than a genuine board property — and it would weaken
     * the solver's prune on every Challenge level (T-08-03).
     */
    @Test
    fun everyGeneratedChallengeFootprintKeepsTheDefaultOnePegTarget() {
        SAMPLED_CHALLENGE_LEVELS.forEach { level ->
            val spec = LevelGenerator.generate(level).startingBoard.spec

            assertThat(spec.minimumPegs).isEqualTo(BoardConstants.MINIMUM_PEGS_DEFAULT)
        }
    }

    private companion object {
        /** Fixed seed so the shuffled-input fixtures are reproducible. */
        const val SHUFFLE_SEED = 20250801L

        /** Sentinel value proving a footprint-keyed map lookup hit. */
        const val CATALOG_HIT = "english33"

        /** Challenge level numbers spanning the ladder (canonical through freeform). */
        val SAMPLED_CHALLENGE_LEVELS = listOf(1, 2, 10, 100, 1000)
    }
}
