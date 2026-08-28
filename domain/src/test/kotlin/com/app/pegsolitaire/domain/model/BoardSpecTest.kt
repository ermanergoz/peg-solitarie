package com.app.pegsolitaire.domain.model

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

    private companion object {
        /** Fixed seed so the shuffled-input fixtures are reproducible. */
        const val SHUFFLE_SEED = 20250801L

        /** Sentinel value proving a footprint-keyed map lookup hit. */
        const val CATALOG_HIT = "english33"
    }
}
