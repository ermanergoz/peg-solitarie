package com.app.pegsolitaire.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the immutable English-33 domain model.
 *
 * Magic numbers are intentional and acceptable in test code (detekt's
 * `MagicNumber` rule excludes `**/test/**`) — the fixtures here ARE the
 * specification of the canonical board geometry.
 */
class BoardTest {
    @Test
    fun english33HasExactly33InPlayPositions() {
        assertThat(Boards.English33.size).isEqualTo(33)
        assertThat(Boards.English33.inPlay).hasSize(33)
    }

    @Test
    fun english33ExcludesTheFour2x2CornerBlocks() {
        val cornerPositions =
            listOf(
                Position(0, 0), Position(0, 1), Position(1, 0), Position(1, 1),
                Position(0, 5), Position(0, 6), Position(1, 5), Position(1, 6),
                Position(5, 0), Position(5, 1), Position(6, 0), Position(6, 1),
                Position(5, 5), Position(5, 6), Position(6, 5), Position(6, 6),
            )

        cornerPositions.forEach { corner ->
            assertThat(Boards.English33.contains(corner)).isFalse()
            assertThat(Boards.english33Initial.cellAt(corner)).isEqualTo(Cell.Blocked)
        }
    }

    @Test
    fun english33CrossArmsAndCentreAreInPlay() {
        // Centre column band and centre row band make up the cross.
        assertThat(Boards.English33.contains(Position(0, 3))).isTrue()
        assertThat(Boards.English33.contains(Position(3, 0))).isTrue()
        assertThat(Boards.English33.contains(Position(3, 3))).isTrue()
        assertThat(Boards.English33.contains(Position(6, 4))).isTrue()
    }

    @Test
    fun initialBoardHasCentreEmptyAnd32Pegs() {
        assertThat(Boards.english33Initial.pegCount).isEqualTo(32)
        assertThat(Boards.english33Initial.cellAt(Position(3, 3))).isEqualTo(Cell.Empty)
    }

    @Test
    fun everyNonCentreInPlayCellStartsAsAPeg() {
        Boards.English33.inPlay
            .filter { it != Position(3, 3) }
            .forEach { position ->
                assertThat(Boards.english33Initial.cellAt(position)).isEqualTo(Cell.Peg)
            }
    }

    @Test
    fun withPegThenWithoutPegReturnsAnEqualBoardWithoutMutatingTheOriginal() {
        val original = Boards.english33Initial
        val originalPegs = original.pegs
        val centre = Position(3, 3)

        val roundTripped = original.withPeg(centre).withoutPeg(centre)

        assertThat(roundTripped).isEqualTo(original)
        // Original instance was never mutated in place.
        assertThat(original.pegs).isEqualTo(originalPegs)
    }

    @Test
    fun withPegSetsAPegAndWithoutPegClearsItImmutably() {
        val centre = Position(3, 3)
        val withCentrePeg = Boards.english33Initial.withPeg(centre)

        assertThat(withCentrePeg.cellAt(centre)).isEqualTo(Cell.Peg)
        assertThat(withCentrePeg.pegCount).isEqualTo(33)
        // Original is unchanged — center still empty.
        assertThat(Boards.english33Initial.cellAt(centre)).isEqualTo(Cell.Empty)
    }

    @Test
    fun bitIndicesAreDeterministicallyAssignedBySortedRowThenColumn() {
        val sorted = Boards.English33.inPlay.sortedWith(compareBy({ it.row }, { it.col }))

        sorted.forEachIndexed { expectedBit, position ->
            assertThat(Boards.English33.bit(position)).isEqualTo(expectedBit)
        }
        // Off-board positions have no bit index.
        assertThat(Boards.English33.bit(Position(0, 0))).isNull()
    }

    @Test
    fun boardEqualityIsValueBasedOverTheLongBitmask() {
        val a = Boards.english33Initial.withoutPeg(Position(3, 1))
        val b = Boards.english33Initial.withoutPeg(Position(3, 1))

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }
}
