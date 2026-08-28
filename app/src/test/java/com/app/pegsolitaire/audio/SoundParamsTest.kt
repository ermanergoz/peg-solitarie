package com.app.pegsolitaire.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Float comparison tolerance: the ladder is tuned by ear, not to the last ULP. */
private const val RATE_TOLERANCE = 1e-4f

/** The rung count of one octave — where the D-07a ceiling is reached exactly. */
private const val OCTAVE_RUNGS = 12

/** A run index far past any reachable gameplay state, used to prove the clamp holds. */
private const val ABSURD_RUN_INDEX = 50

/** The eleven cues the phase committed assets for. */
private const val EXPECTED_CUE_COUNT = 11

/**
 * Specification for the pure audio maths: the jump pitch ladder (D-05/D-06/D-07a), the
 * star-pop triad (D-15) and the relative cue mix.
 *
 * Runner-free — no JUnit runner and no Android runtime — because [SoundParams] is pure
 * Kotlin. [SoundCue] is touched only through its `volume`/`priority`/`resId` fields, which
 * are plain constants; the `R.raw.*` ids resolve to `Int`s at compile time, so no resource
 * table is needed either.
 *
 * The out-of-range indices ([jumpRateIsClampedAtTheOctaveCeiling],
 * [jumpRateOfANegativeIndexStaysAboveTheFloor]) are the T-07-02 mitigation: both functions
 * must be *total* over `Int`, so the clamp is a tested contract rather than a reliance on
 * `SoundPool` clamping silently at playback time.
 */
class SoundParamsTest {
    @Test
    fun jumpRateOfTheFirstJumpIsTheSamplesOwnPitch() {
        val rate = SoundParams.jumpRate(0)

        assertThat(rate).isEqualTo(SoundParams.JUMP_BASE_RATE)
    }

    @Test
    fun jumpRateOfTheSecondRungIsOneSemitoneUp() {
        val rate = SoundParams.jumpRate(1)

        assertThat(rate).isWithin(RATE_TOLERANCE).of(SoundParams.JUMP_BASE_RATE * SoundParams.SEMITONE_STEP)
    }

    @Test
    fun jumpRateRisesStrictlyAcrossAWholeOctaveOfRungs() {
        val ladder = (0 until OCTAVE_RUNGS).map { SoundParams.jumpRate(it) }

        assertThat(ladder).isInStrictOrder()
    }

    @Test
    fun jumpRateIsClampedAtTheOctaveCeiling() {
        val atOctave = SoundParams.jumpRate(OCTAVE_RUNGS)
        val farPast = SoundParams.jumpRate(ABSURD_RUN_INDEX)

        assertThat(atOctave).isWithin(RATE_TOLERANCE).of(SoundParams.MAX_RATE)
        assertThat(farPast).isEqualTo(SoundParams.MAX_RATE)
    }

    @Test
    fun jumpRateOfANegativeIndexStaysAboveTheFloor() {
        val rate = SoundParams.jumpRate(-1)

        assertThat(rate).isAtLeast(SoundParams.MIN_RATE)
    }

    @Test
    fun starPopRateOfTheFirstStarIsTheSamplesOwnPitch() {
        val rate = SoundParams.starPopRate(0)

        assertThat(rate).isEqualTo(SoundParams.STAR_POP_BASE_RATE)
    }

    @Test
    fun starPopRateRisesOncePerStar() {
        val triad = listOf(SoundParams.starPopRate(0), SoundParams.starPopRate(1), SoundParams.starPopRate(2))

        assertThat(triad).isInStrictOrder()
    }

    @Test
    fun starPopRateOfTheThirdStarStaysUnderTheCeiling() {
        val rate = SoundParams.starPopRate(2)

        assertThat(rate).isAtMost(SoundParams.MAX_RATE)
    }

    @Test
    fun theRejectedTapIsQuieterThanTheJump() {
        val invalid = SoundCue.InvalidTap.volume

        assertThat(invalid).isLessThan(SoundCue.Jump.volume)
    }

    @Test
    fun theRemovalSitsUnderTheJumpRatherThanCompetingWithIt() {
        val removal = SoundCue.PegRemove.volume

        assertThat(removal).isLessThan(SoundCue.Jump.volume)
    }

    @Test
    fun theWinStingerIsTheLoudestCue() {
        val loudest = SoundCue.entries.maxOf { it.volume }

        assertThat(SoundCue.Win.volume).isEqualTo(loudest)
    }

    @Test
    fun everyCommittedSampleHasExactlyOneCue() {
        val ids = SoundCue.entries.map { it.resId }

        assertThat(SoundCue.entries).hasSize(EXPECTED_CUE_COUNT)
        assertThat(ids).containsNoDuplicates()
    }

    @Test
    fun voiceStealingSacrificesAJumpBeforeTheStinger() {
        val stinger = SoundCue.Win.priority

        assertThat(stinger).isGreaterThan(SoundCue.Jump.priority)
    }
}
