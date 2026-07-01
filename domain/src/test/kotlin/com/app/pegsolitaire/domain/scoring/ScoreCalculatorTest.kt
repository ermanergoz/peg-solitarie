package com.app.pegsolitaire.domain.scoring

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the pure-domain [ScoreCalculator] (SCOR-01 / SCOR-02).
 *
 * Magic numbers are intentional and acceptable in test code (detekt's
 * `MagicNumber` rule excludes the test source set) — these fixtures ARE the
 * specification of the star bands (D-01) and the time-decay score formula (D-03).
 */
class ScoreCalculatorTest {
    @Test
    fun starsAreThreeAtAndBelowTheThreeStarBoundary() {
        // Boundary at THREE_STAR_MAX_PEGS = 1: a perfect (1-peg) solve scores 3,
        // and the theoretical 0-peg case also scores 3 (never undercut the floor).
        assertThat(ScoreCalculator.stars(pegsRemaining = 1)).isEqualTo(3)
        assertThat(ScoreCalculator.stars(pegsRemaining = 0)).isEqualTo(3)
    }

    @Test
    fun starsAreTwoAcrossTheTwoToThreePegBand() {
        assertThat(ScoreCalculator.stars(pegsRemaining = 2)).isEqualTo(2)
        assertThat(ScoreCalculator.stars(pegsRemaining = 3)).isEqualTo(2)
    }

    @Test
    fun starsAreOneAboveTheTwoStarBandAndNeverZero() {
        // D-01 overrides spec §7: more than 3 pegs still earns MIN_STARS = 1, not 0.
        assertThat(ScoreCalculator.stars(pegsRemaining = 4)).isEqualTo(1)
        assertThat(ScoreCalculator.stars(pegsRemaining = 32)).isEqualTo(1)
    }

    @Test
    fun scoreAtZeroElapsedAddsTheFullTimeBonusToThePegBase() {
        // 31 pegs cleared at 0s → peg base + the uncapped TIME_BONUS_CAP.
        val expected = 31 * ScoreParams.PEG_VALUE + ScoreParams.TIME_BONUS_CAP

        assertThat(ScoreCalculator.score(pegsCleared = 31, elapsedSeconds = 0)).isEqualTo(expected)
    }

    @Test
    fun timeBonusIsFlooredAtZeroSoScoreEqualsThePegBaseOnceItDecays() {
        // 1000s is well past the 500s the bonus needs to reach 0; score is the bare peg base.
        val pegBase = 10 * ScoreParams.PEG_VALUE

        assertThat(ScoreCalculator.score(pegsCleared = 10, elapsedSeconds = 1_000)).isEqualTo(pegBase)
    }

    @Test
    fun scoreNeverDropsBelowThePegBaseRegardlessOfElapsedSeconds() {
        val pegBase = 5 * ScoreParams.PEG_VALUE

        // Even at an extreme elapsed time the time term cannot go negative.
        assertThat(ScoreCalculator.score(pegsCleared = 5, elapsedSeconds = Int.MAX_VALUE))
            .isAtLeast(pegBase)
    }
}
