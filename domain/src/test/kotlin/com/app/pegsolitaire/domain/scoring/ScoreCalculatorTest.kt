package com.app.pegsolitaire.domain.scoring

import com.app.pegsolitaire.domain.model.BoardConstants
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

    @Test
    fun scoreWithDefaultMultiplierEqualsThePhase2ScoreForEqualInputs() {
        // D-11: levelMultiplier = 1 is byte-identical to the Phase-2 score — Classic
        // (which never passes a multiplier) is completely untouched.
        val phase2 = ScoreCalculator.score(pegsCleared = 20, elapsedSeconds = 42)

        assertThat(ScoreCalculator.score(pegsCleared = 20, elapsedSeconds = 42, levelMultiplier = 1))
            .isEqualTo(phase2)
    }

    @Test
    fun scoreWithMultiplierGreaterThanOneScalesStrictlyLarger() {
        // D-11: climbing the Challenge ladder scales the numeric score up.
        val base = ScoreCalculator.score(pegsCleared = 20, elapsedSeconds = 42, levelMultiplier = 1)
        val scaled = ScoreCalculator.score(pegsCleared = 20, elapsedSeconds = 42, levelMultiplier = 3)

        assertThat(scaled).isGreaterThan(base)
        assertThat(scaled).isEqualTo(base * 3)
    }

    @Test
    fun levelMultiplierNeverRebasesStars() {
        // D-11: the star model is unchanged — a 1-peg solve is 3 stars regardless of
        // the level multiplier; only the numeric score scales.
        val plain = ScoreCalculator.evaluate(pegsRemaining = 1, pegsCleared = 20, elapsedSeconds = 42)
        val scaled =
            ScoreCalculator.evaluate(
                pegsRemaining = 1,
                pegsCleared = 20,
                elapsedSeconds = 42,
                levelMultiplier = 4,
            )

        assertThat(scaled.stars).isEqualTo(3)
        assertThat(scaled.stars).isEqualTo(plain.stars)
        assertThat(scaled.score).isEqualTo(plain.score * 4)
    }

    // --- Per-board star bands (D-03 / D-04, DEBT-02) -------------------------
    //
    // The band SHAPE is global: `F` earns 3 stars, `F+1..F+2` earns 2, worse earns
    // the floor. Only the anchor `F` — the board's own `BoardSpec.minimumPegs` —
    // moves. Diamond-41's F = 2 is the value proven by `DiamondFloorOptimalityTest`
    // (one peg is GF(4)-unreachable from the centre start) and by `SolutionTest`
    // (the committed `diamond41.txt` line replays to a two-peg dead end). These
    // tests rest on that pair of proofs; if F ever changes there, it changes here.

    @Test
    fun theDefaultTargetReproducesTheGlobalBandsAcrossTheWholePegDomain() {
        // The non-regression sweep the four cross boards depend on: for EVERY peg
        // count a real board can report, the default-target bands must be exactly
        // the pre-phase piecewise function (3 for 0-1, 2 for 2-3, 1 above). 45 is
        // Wiegleb-45's hole count, the largest footprint that ships.
        for (pegsRemaining in 0..45) {
            val expected =
                when {
                    pegsRemaining <= 1 -> 3
                    pegsRemaining <= 3 -> 2
                    else -> 1
                }

            assertThat(ScoreCalculator.stars(pegsRemaining = pegsRemaining, minimumPegs = 1))
                .isEqualTo(expected)
            assertThat(ScoreCalculator.stars(pegsRemaining = pegsRemaining))
                .isEqualTo(expected)
        }
    }

    @Test
    fun reachingTheDiamondFloorEarnsThreeStars() {
        // D-04: the proven floor F = 2 IS Diamond-41's perfect finish.
        assertThat(ScoreCalculator.stars(pegsRemaining = 2, minimumPegs = 2)).isEqualTo(3)
    }

    @Test
    fun finishingBelowTheDiamondFloorNeverScoresWorseThanAtIt() {
        // Unreachable on Diamond-41 by the GF(4) invariant, but the band must still
        // be monotone: `<= F` is the 3-star test, not `== F`.
        assertThat(ScoreCalculator.stars(pegsRemaining = 1, minimumPegs = 2)).isEqualTo(3)
        assertThat(ScoreCalculator.stars(pegsRemaining = 0, minimumPegs = 2)).isEqualTo(3)
    }

    @Test
    fun theDiamondTwoStarBandIsTheSameWidthJustReAnchored() {
        // F+1..F+2 = 3..4 pegs — the same 2-peg-wide band as the global 2..3.
        assertThat(ScoreCalculator.stars(pegsRemaining = 3, minimumPegs = 2)).isEqualTo(2)
        assertThat(ScoreCalculator.stars(pegsRemaining = 4, minimumPegs = 2)).isEqualTo(2)
    }

    @Test
    fun starsFallToTheFloorAboveTheDiamondTwoStarBand() {
        assertThat(ScoreCalculator.stars(pegsRemaining = 5, minimumPegs = 2)).isEqualTo(1)
        assertThat(ScoreCalculator.stars(pegsRemaining = 41, minimumPegs = 2)).isEqualTo(1)
    }

    @Test
    fun evaluateThreadsTheBoardTargetIntoStarsButNeverIntoScore() {
        // D-11 still holds: stars and score are independent axes. Re-anchoring the
        // star bands must leave a Diamond finish's numeric score untouched.
        val globalTarget =
            ScoreCalculator.evaluate(pegsRemaining = 2, pegsCleared = 39, elapsedSeconds = 42)
        val diamondTarget =
            ScoreCalculator.evaluate(
                pegsRemaining = 2,
                pegsCleared = 39,
                elapsedSeconds = 42,
                minimumPegs = 2,
            )

        assertThat(globalTarget.stars).isEqualTo(2)
        assertThat(diamondTarget.stars).isEqualTo(3)
        assertThat(diamondTarget.score).isEqualTo(globalTarget.score)
    }

    @Test
    fun evaluateWithExplicitStarsKeepsScoreAndCarriesTheStars() {
        // The Challenge caller passes the clear rule's stars (three pegs left = one star), where the
        // default one-peg Classic bands would have said two. Only the star count may change: the
        // score is (3 × 100 + (5000 − 42 × 10)) × 3 = 14,640, exactly as without the override.
        val result =
            ScoreCalculator.evaluate(
                pegsRemaining = 3,
                pegsCleared = 3,
                elapsedSeconds = 42,
                levelMultiplier = 3,
                stars = 1,
            )

        assertThat(ScoreCalculator.stars(pegsRemaining = 3)).isEqualTo(2)
        assertThat(result).isEqualTo(
            ScoreResult(stars = 1, score = 14_640, pegsRemaining = 3, pegsCleared = 3, elapsedSeconds = 42),
        )
    }

    @Test
    fun theTwoStarBandWidthIsTheOnlyShapeConstantAndTheLegacyBoundDerivesFromIt() {
        // The legacy global bound must be DERIVED, so re-anchoring cannot silently
        // widen or narrow the band on any board.
        assertThat(ScoreParams.TWO_STAR_MAX_PEGS)
            .isEqualTo(ScoreParams.THREE_STAR_MAX_PEGS + ScoreParams.TWO_STAR_BAND_WIDTH)
        assertThat(ScoreParams.TWO_STAR_MAX_PEGS).isEqualTo(3)
        assertThat(ScoreParams.THREE_STAR_MAX_PEGS).isEqualTo(BoardConstants.MINIMUM_PEGS_DEFAULT)
    }
}
