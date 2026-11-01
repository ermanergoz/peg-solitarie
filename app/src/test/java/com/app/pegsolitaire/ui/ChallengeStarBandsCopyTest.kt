package com.app.pegsolitaire.ui

import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.scoring.ChallengeClearRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The words of the Challenge star bands' four strings (ux-spec §8 D-SB, criteria 30c, 30h, 30i;
 * docs/gdd.md §16): each resolved at the numbers the clear rule supplies, compared with the decided
 * copy, and checked against the words the copy contract forbids. Here, and only here, the English
 * text is the thing under test; the screen tests find every string by its resource id.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [COPY_ROBOLECTRIC_SDK])
class ChallengeStarBandsCopyTest {
    private val resources get() = RuntimeEnvironment.getApplication().resources

    @Test
    fun theNextStarLineNamesTheNextBandUpOnly() {
        val atOneStar = resources.getQuantityString(R.plurals.results_next_star_hint, 2, 2, 2)
        val atTwoStars = resources.getQuantityString(R.plurals.results_next_star_hint, 1, 1, 3)

        assertThat(atOneStar).isEqualTo("Leave 2 pegs for 2 stars.")
        assertThat(atTwoStars).isEqualTo("Leave 1 peg for 3 stars.")
        listOf(atOneStar, atTwoStars).forEach { line -> assertNoneOf(NEXT_STAR_FORBIDDEN, line) }
        // "time" as a word, never as part of another word.
        listOf(atOneStar, atTwoStars).forEach { line ->
            assertThat(Regex("\\btime\\b", RegexOption.IGNORE_CASE).containsMatchIn(line)).isFalse()
        }
    }

    @Test
    fun theChallengeStuckMessageNamesItsCountAndTheThreshold() {
        val message =
            resources.getQuantityString(R.plurals.stuck_message_challenge, 5, 5, ChallengeClearRule.CLEAR_MAX_PEGS)

        assertThat(message)
            .isEqualTo("No moves left with 5 pegs — leave 3 or fewer to clear the level. Undo or restart.")
        assertNoneOf(BANNER_FORBIDDEN, message)
        // The `one` item is for lint and grammar only: the fewest pegs a stuck Challenge board holds
        // is one past the threshold, four, never one (R5, R6).
        assertThat(ChallengeClearRule.isCleared(ChallengeClearRule.CLEAR_MAX_PEGS)).isTrue()
        assertThat(ChallengeClearRule.isCleared(ChallengeClearRule.CLEAR_MAX_PEGS + 1)).isFalse()
        assertThat(ChallengeClearRule.CLEAR_MAX_PEGS + 1).isEqualTo(4)
    }

    @Test
    fun theChallengeProvenDeadMessageNamesTheThresholdAndNeverALoss() {
        val message =
            resources.getQuantityString(
                R.plurals.nowin_message_challenge,
                ChallengeClearRule.CLEAR_MAX_PEGS,
                ChallengeClearRule.CLEAR_MAX_PEGS,
            )

        assertThat(message)
            .isEqualTo(
                "One peg is out of reach from here — you may still clear the level with 3 pegs or fewer, " +
                    "or undo or restart.",
            )
        assertNoneOf(BANNER_FORBIDDEN + "no winning move", message)
    }

    @Test
    fun theClearedHeadlineIsItsOwnWordNotTheStuckOne() {
        val cleared = resources.getString(R.string.results_headline_cleared)

        assertThat(cleared).isEqualTo("Cleared!")
        assertThat(cleared).isNotEqualTo(resources.getString(R.string.results_headline_stuck))
    }

    private fun assertNoneOf(
        forbidden: List<String>,
        text: String,
    ) {
        forbidden.forEach { word ->
            assertWithMessage("\"%s\" in \"%s\"", word, text).that(text.contains(word, ignoreCase = true)).isFalse()
        }
    }

    private companion object {
        /** GDD §16 and ux-spec §4.6: no grading of the player and no time or score advice on the line. */
        val NEXT_STAR_FORBIDDEN = listOf("almost", "so close", "try again", "next time", "score")

        /** GDD §16 and ux-spec §4.4.3: a stuck or proven-dead board is never a loss. */
        val BANNER_FORBIDDEN = listOf("lost", "failed", "game over", "try again")
    }
}

private const val COPY_ROBOLECTRIC_SDK = 34
