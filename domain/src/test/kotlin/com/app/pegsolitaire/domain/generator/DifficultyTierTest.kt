package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.Boards
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Behaviour spec for the continuous difficulty curve `DifficultyTiers.forLevel`
 * (CHAL-04, D-05/D-06/D-08).
 *
 * These assertions pin the curve *shape* (monotonic, continuous, primary lever =
 * solution length, freeform gated) without hardcoding the playtest-tunable knob
 * VALUES (D-07). Magic numbers (the sampled level bounds) are intentional and
 * acceptable in test code — the sampled range IS the spec.
 *
 * Generator contract 2 (docs/gdd.md §7.1, GD-SB-09/10): level N deals what the previous curve
 * dealt at N + 50. The previous curve is read through the `internal` oracle
 * `forLevel(levelNumber, levelOffset = 0)`, and its level-1 values are also written out by hand
 * (the GDD's worked table), so the oracle itself is checked against arithmetic, not trusted.
 *
 * The pool holds footprint copies of the Classic boards with a one-peg target (I5), so a pool
 * entry is never the catalog instance: membership is tested by `footprint`, never by identity.
 */
class DifficultyTierTest {
    private val sampledRange = 1..1000

    private val canonicalFootprints = Boards.CLASSIC_SPECS.map { it.footprint }.toSet()

    @Test
    fun solutionLengthIsMonotonicNonDecreasingAndStrictlyGrowsOverTheRange() {
        for (n in sampledRange.first until sampledRange.last) {
            assertThat(DifficultyTiers.forLevel(n + 1).solutionLength)
                .isAtLeast(DifficultyTiers.forLevel(n).solutionLength)
        }

        // Endlessly trickier (D-05): the far end of the range is strictly harder than the start.
        assertThat(DifficultyTiers.forLevel(sampledRange.last).solutionLength)
            .isGreaterThan(DifficultyTiers.forLevel(sampledRange.first).solutionLength)
    }

    @Test
    fun consecutiveSolutionLengthDeltasAreBoundedSoThereIsNoHardTierWall() {
        for (n in sampledRange.first until sampledRange.last) {
            val delta =
                DifficultyTiers.forLevel(n + 1).solutionLength -
                    DifficultyTiers.forLevel(n).solutionLength
            // Continuity (D-05): each step changes the primary lever by at most one.
            assertThat(delta).isIn(0..1)
        }
    }

    @Test
    fun solutionLengthReachesANonTrivialFloorBeforeTheFootprintPoolGrows() {
        val basePoolSize = DifficultyTiers.forLevel(sampledRange.first).footprintPool.size
        val firstPoolGrowthLevel =
            sampledRange.first { DifficultyTiers.forLevel(it).footprintPool.size > basePoolSize }

        // The pool stays at its base width until that level (footprint grows LATER, D-06).
        for (n in sampledRange.first until firstPoolGrowthLevel) {
            assertThat(DifficultyTiers.forLevel(n).footprintPool.size).isEqualTo(basePoolSize)
        }

        // By the time the board pool widens, solution length (the PRIMARY lever) has
        // already climbed past its level-1 value — trickier before bigger (D-06).
        assertThat(DifficultyTiers.forLevel(firstPoolGrowthLevel).solutionLength)
            .isGreaterThan(DifficultyTiers.forLevel(sampledRange.first).solutionLength)
    }

    @Test
    fun footprintPoolIsCanonicalOnlyBelowTheFreeformIntroLevel() {
        val introLevel = DifficultyTiers.forLevel(sampledRange.first).freeformIntroLevel

        for (n in sampledRange.first until introLevel) {
            DifficultyTiers.forLevel(n).footprintPool.forEach { spec ->
                assertThat(spec.footprint in canonicalFootprints).isTrue()
            }
        }
    }

    @Test
    fun freeformEligibilityFlipsAtTheIntroLevel() {
        val introLevel = DifficultyTiers.forLevel(sampledRange.first).freeformIntroLevel

        fun eligible(n: Int): Boolean = n >= DifficultyTiers.forLevel(n).freeformIntroLevel

        assertThat(eligible(introLevel - 1)).isFalse()
        assertThat(eligible(introLevel)).isTrue()
        assertThat(eligible(introLevel + 1)).isTrue()
    }

    @Test
    fun footprintPoolGrowsMoreSlowlyThanSolutionLengthAndStaysCanonical() {
        // Footprint pool never exceeds the canonical catalog this phase (freeform: 03-02).
        assertThat(DifficultyTiers.forLevel(sampledRange.last).footprintPool.size)
            .isAtMost(canonicalFootprints.size)
        // Pool is always a prefix of the canonical set — every entry is canonical.
        DifficultyTiers.forLevel(sampledRange.last).footprintPool.forEach { spec ->
            assertThat(spec.footprint in canonicalFootprints).isTrue()
        }
    }

    @Test
    fun nonPositiveLevelsClampToTheLevelOneTier() {
        val levelOne = DifficultyTiers.forLevel(1)

        assertThat(DifficultyTiers.forLevel(0)).isEqualTo(levelOne)
        assertThat(DifficultyTiers.forLevel(-5)).isEqualTo(levelOne)
    }

    // ---- Generator contract 2: the ladder cut (GD-SB-09), the fail-proof flag (GD-SB-10) ----

    @Test
    fun levelOneDealsTheTierTheOldCurveDealtAtLevelFiftyOne() {
        val levelOne = DifficultyTiers.forLevel(1)

        // The GDD §7.1 worked row for visible level 1, by hand: S = 3 + 50 / 25 = 5, a gate floor
        // of max(3, 5 - 1) = 4, pegs (4 + 1)..(5 + 1 + 2), a pool of 1 + 50 / 50 = 2 footprints
        // (English-33, European-37), a multiplier of 1 + 50 / 20 = 3.
        assertThat(levelOne.solutionLength).isEqualTo(5)
        assertThat(levelOne.minSolutionLength).isEqualTo(4)
        assertThat(levelOne.pegRange).isEqualTo(5..8)
        assertThat(levelOne.footprintPool.map { it.footprint })
            .containsExactly(Boards.English33.footprint, Boards.European37.footprint)
            .inOrder()
        assertThat(levelOne.scoreMultiplier).isEqualTo(3)
        assertThat(levelOne.failProof).isTrue()
        // ...and it is exactly the previous curve's level 51, the fail-proof flag aside.
        assertThat(levelOne.copy(failProof = false))
            .isEqualTo(DifficultyTiers.forLevel(51, levelOffset = 0))
    }

    @Test
    fun everyLevelDealsTheTierTheOldCurveDealtFiftyLevelsLater() {
        // SB-D5: every field but the fail-proof flag, which reads the visible level (GD-SB-10).
        // The freeform intro is one constant (10) on both sides, compared against the visible level.
        for (n in 1..2000) {
            val shifted = DifficultyTiers.forLevel(n)
            val previousCurve = DifficultyTiers.forLevel(n + 50, levelOffset = 0)

            assertWithMessage("tier of level %s against the previous curve's level %s", n, n + 50)
                .that(shifted.copy(failProof = false))
                .isEqualTo(previousCurve)
            assertWithMessage("failProof of level %s", n).that(shifted.failProof).isEqualTo(n <= 4)
        }
    }

    @Test
    fun theSolutionLengthCapIsReachedAtLevelSixHundredAndTwentySix() {
        // 3 + (625 + 50) / 25 = 30 is the cap; one level earlier, 3 + 674 / 25 = 29.
        assertThat(DifficultyTiers.forLevel(625).solutionLength).isEqualTo(29)
        assertThat(DifficultyTiers.forLevel(626).solutionLength).isEqualTo(30)
        assertThat(DifficultyTiers.forLevel(ChallengeLevelBounds.MAX).solutionLength).isEqualTo(30)
    }

    @Test
    fun onlyTheFirstFourLevelsAreFailProof() {
        assertThat((1..4).map { DifficultyTiers.forLevel(it).failProof }).containsExactly(true, true, true, true)
        assertThat(DifficultyTiers.forLevel(5).failProof).isFalse()
        // A forged level below one plays level 1, so it is fail-proof too (the clamp, T-03-01).
        assertThat(DifficultyTiers.forLevel(0).failProof).isTrue()
    }

    @Test
    fun everyPoolEntryCarriesTheOnePegTargetWhateverItsClassicFloor() {
        // I5 (GD-SB-15): the Diamond-41 footprint may ride into Challenge, its Classic floor of two
        // may not. Level 151 is the first whose pool holds all five footprints (1 + 200 / 50 = 5).
        val pool = DifficultyTiers.forLevel(151).footprintPool

        assertThat(pool.map { it.footprint }).containsExactlyElementsIn(canonicalFootprints).inOrder()
        pool.forEach { spec -> assertThat(spec.minimumPegs).isEqualTo(BoardConstants.MINIMUM_PEGS_DEFAULT) }
        assertThat(Boards.Diamond41.minimumPegs).isEqualTo(BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR)
    }
}
