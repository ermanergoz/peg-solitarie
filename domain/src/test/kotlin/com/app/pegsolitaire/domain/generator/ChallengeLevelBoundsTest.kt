package com.app.pegsolitaire.domain.generator

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the single source of the Challenge level range (D-15).
 *
 * `ChallengeLevelBounds` is the one place the lowest/highest valid Challenge level is
 * defined, so every clamp site — the generator, the data-layer source, and both
 * ViewModels — cannot drift apart (T-06-21). The clamp is **fail-safe**: an untrusted
 * value plays the nearest legal level rather than crashing or being refused (T-06-22).
 */
class ChallengeLevelBoundsTest {
    @Test
    fun theRangeIsTheGeneratorsFrozenCeilingAndLevelOne() {
        assertThat(ChallengeLevelBounds.MIN).isEqualTo(1)
        assertThat(ChallengeLevelBounds.MAX).isEqualTo(GeneratorConstants.MAX_CHALLENGE_LEVEL)
    }

    @Test
    fun aLevelBelowTheMinimumClampsUpToTheMinimum() {
        assertThat(ChallengeLevelBounds.clamp(0)).isEqualTo(ChallengeLevelBounds.MIN)
        assertThat(ChallengeLevelBounds.clamp(-5)).isEqualTo(ChallengeLevelBounds.MIN)
        assertThat(ChallengeLevelBounds.clamp(Int.MIN_VALUE)).isEqualTo(ChallengeLevelBounds.MIN)
    }

    @Test
    fun aForgedHugeLevelClampsDownToTheFrozenCeiling() {
        assertThat(ChallengeLevelBounds.clamp(Int.MAX_VALUE))
            .isEqualTo(GeneratorConstants.MAX_CHALLENGE_LEVEL)
    }

    @Test
    fun anInRangeLevelIsReturnedUnchanged() {
        assertThat(ChallengeLevelBounds.clamp(ChallengeLevelBounds.MIN)).isEqualTo(ChallengeLevelBounds.MIN)
        assertThat(ChallengeLevelBounds.clamp(SAMPLE_IN_RANGE_LEVEL)).isEqualTo(SAMPLE_IN_RANGE_LEVEL)
        assertThat(ChallengeLevelBounds.clamp(ChallengeLevelBounds.MAX)).isEqualTo(ChallengeLevelBounds.MAX)
    }
}

/** An arbitrary level comfortably inside the range — the non-regression case. */
private const val SAMPLE_IN_RANGE_LEVEL = 42
