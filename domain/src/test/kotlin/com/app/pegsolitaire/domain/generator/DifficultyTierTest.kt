package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.Boards
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the continuous difficulty curve `DifficultyTiers.forLevel`
 * (CHAL-04, D-05/D-06/D-08).
 *
 * These assertions pin the curve *shape* (monotonic, continuous, primary lever =
 * solution length, freeform gated) without hardcoding the playtest-tunable knob
 * VALUES (D-07). Magic numbers (the sampled level bounds) are intentional and
 * acceptable in test code — the sampled range IS the spec.
 */
class DifficultyTierTest {
    private val sampledRange = 1..1000

    private val canonicalSpecs =
        setOf(
            Boards.English33,
            Boards.European37,
            Boards.Asymmetric39,
            Boards.Diamond41,
            Boards.Wiegleb45,
        )

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
                assertThat(spec in canonicalSpecs).isTrue()
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
            .isAtMost(canonicalSpecs.size)
        // Pool is always a prefix of the canonical set — every entry is canonical.
        DifficultyTiers.forLevel(sampledRange.last).footprintPool.forEach { spec ->
            assertThat(spec in canonicalSpecs).isTrue()
        }
    }

    @Test
    fun nonPositiveLevelsClampToTheLevelOneTier() {
        val levelOne = DifficultyTiers.forLevel(1)

        assertThat(DifficultyTiers.forLevel(0)).isEqualTo(levelOne)
        assertThat(DifficultyTiers.forLevel(-5)).isEqualTo(levelOne)
    }
}
