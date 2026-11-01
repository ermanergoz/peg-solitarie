package com.app.pegsolitaire.ui.results

import com.app.pegsolitaire.domain.scoring.ChallengeBand
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The next-star line's numbers on [ResultsUiState] (ux-spec D-SB-2, criterion 30c's JVM half;
 * docs/gdd.md GD-SB-08/20): the state carries the band one star up, taken from the `:domain` clear
 * rule, so the screen only reads two numbers and never computes a band.
 *
 * Expected bands are the founder's table written out by hand: a one-star clear (three pegs) needs
 * two pegs for two stars, a two-star clear (two pegs) one peg for three.
 */
class ResultsUiStateTest {
    @Test
    fun aOneStarChallengeClearNamesTwoPegsForTwoStars() {
        assertThat(challengeResult(stars = 1, pegsRemaining = 3).nextStarBand)
            .isEqualTo(ChallengeBand(pegsRemaining = 2, stars = 2))
    }

    @Test
    fun aTwoStarChallengeClearNamesOnePegForThreeStars() {
        assertThat(challengeResult(stars = 2, pegsRemaining = 2).nextStarBand)
            .isEqualTo(ChallengeBand(pegsRemaining = 1, stars = 3))
    }

    @Test
    fun aPerfectChallengeClearAndAStuckRouteNameNoNextBand() {
        assertThat(challengeResult(stars = 3, pegsRemaining = 1).nextStarBand).isNull()
        // The defensive STUCK branch: a route play never sends (R6) has no band to step up from.
        assertThat(challengeResult(stars = 1, pegsRemaining = 4, tier = ResultsHeadlineTier.STUCK).nextStarBand)
            .isNull()
    }

    @Test
    fun classicResultsNameNoNextBandAtAnyStarCount() {
        // R8: Classic keeps its own bands and its Results screen gains no line.
        listOf(1 to 3, 2 to 2, 3 to 1).forEach { (stars, pegs) ->
            val classic = challengeResult(stars = stars, pegsRemaining = pegs).copy(challengeLevel = null)

            assertWithMessage("Classic at %s stars", stars).that(classic.nextStarBand).isNull()
        }
    }

    @Test
    fun theNextBandsStarCountIsNeverOne() {
        // "%2$d stars" needs no plural of its own only because the band above any clear is at
        // least two stars (ux-spec §4.6, Numbers).
        (1..64).mapNotNull { pegs -> challengeResult(stars = 1, pegsRemaining = pegs).nextStarBand }.forEach { band ->
            assertThat(band.stars).isAtLeast(2)
        }
    }

    private fun challengeResult(
        stars: Int,
        pegsRemaining: Int,
        tier: ResultsHeadlineTier = ResultsHeadlineTier.CLEARED,
    ): ResultsUiState =
        ResultsUiState(
            stars = stars,
            pegsRemaining = pegsRemaining,
            elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
            score = SAMPLE_SCORE,
            headlineTier = tier,
            replayId = "",
            challengeLevel = SAMPLE_CHALLENGE_LEVEL,
        )

    private companion object {
        const val SAMPLE_ELAPSED_SECONDS = 41
        const val SAMPLE_SCORE = 3_200
        const val SAMPLE_CHALLENGE_LEVEL = 3
    }
}
