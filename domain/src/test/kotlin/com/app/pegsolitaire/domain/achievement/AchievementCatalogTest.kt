package com.app.pegsolitaire.domain.achievement

import com.app.pegsolitaire.domain.scoring.ScoreParams
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Shape guard over the achievement catalog — the roster, the tiers and the thresholds.
 *
 * ## Why the size assertions exist
 *
 * D-05 fixes the roster at **20 visible achievements**, which is simultaneously 20 Play
 * Console entries, 20 uploaded 512×512 icons and 20 `BuildConfig` ID keys (D-17). None of
 * those three live in this repository, so a constant silently dropped from or added to
 * [AchievementId] would desynchronise the code from artefacts nobody can diff — the
 * failure would surface as an achievement that never fires, or a crash on a missing ID,
 * on a device. Asserting the count here turns that into a build failure instead.
 *
 * Every expectation about a POLICY value is written as its named
 * [AchievementThresholds] constant rather than as a bare `10`/`250`/`3`: a test literal
 * would be a second statement of a number the constants object exists to state once, and
 * the two copies would eventually drift. Bare literals for *structural* facts (how many
 * constants there are, how many are incremental) are fine — those are the assertions'
 * whole subject, not restatements of a tunable.
 */
class AchievementCatalogTest {
    @Test
    fun theRosterIsExactlyTwentyAchievements() {
        assertThat(AchievementId.entries).hasSize(20)
    }

    @Test
    fun exactlySixAchievementsAreIncrementalAndTheyAreTheLadderAndStarTiers() {
        val incremental = AchievementId.entries.filter { it.isIncremental }

        assertThat(incremental)
            .containsExactly(
                AchievementId.LADDER_10,
                AchievementId.LADDER_50,
                AchievementId.LADDER_100,
                AchievementId.STARS_25,
                AchievementId.STARS_100,
                AchievementId.STARS_250,
            ).inOrder()
    }

    @Test
    fun everyOneShotAchievementHasNoStepTotal() {
        val oneShots = AchievementId.entries.filterNot { it.isIncremental }

        assertThat(oneShots.map { it.stepTotal }.toSet()).containsExactly(null)
    }

    @Test
    fun eachLadderTierCarriesItsNamedThresholdAsItsStepTotal() {
        assertThat(AchievementId.LADDER_10.stepTotal).isEqualTo(AchievementThresholds.LADDER_TIER_1)
        assertThat(AchievementId.LADDER_50.stepTotal).isEqualTo(AchievementThresholds.LADDER_TIER_2)
        assertThat(AchievementId.LADDER_100.stepTotal).isEqualTo(AchievementThresholds.LADDER_TIER_3)
    }

    @Test
    fun eachStarTierCarriesItsNamedThresholdAsItsStepTotal() {
        assertThat(AchievementId.STARS_25.stepTotal).isEqualTo(AchievementThresholds.STARS_TIER_1)
        assertThat(AchievementId.STARS_100.stepTotal).isEqualTo(AchievementThresholds.STARS_TIER_2)
        assertThat(AchievementId.STARS_250.stepTotal).isEqualTo(AchievementThresholds.STARS_TIER_3)
    }

    @Test
    fun theRosterCarriesOneClearedAchievementPerClassicBoard() {
        val clearedIds = AchievementId.entries.filter { it.name.startsWith("CLEARED_") }

        assertThat(clearedIds).hasSize(ClassicBoard.entries.size)
    }

    @Test
    fun thereAreFiveClassicBoardsAndTheDenominatorIsDerivedFromThem() {
        // The Grand tour / Perfectionist denominator must MOVE with the enum, never be a
        // stale literal that hands the achievement out one board early.
        assertThat(ClassicBoard.entries).hasSize(5)
        assertThat(AchievementThresholds.CLASSIC_BOARD_TOTAL).isEqualTo(ClassicBoard.entries.size)
    }

    @Test
    fun theAchievementPerfectStarBandStillAgreesWithTheScoringOne() {
        // Belt and braces, exactly as ScoreParams.THREE_STAR_MAX_PEGS documents for its
        // own twin: two independently-motivated constants that happen to coincide, with a
        // test to notice the day they stop.
        assertThat(AchievementThresholds.PERFECT_STARS).isEqualTo(ScoreParams.THREE_STARS)
    }

    @Test
    fun theSpeedSolveBoundInMillisecondsIsDerivedFromTheBoundInSeconds() {
        val expected = AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_SECONDS * AchievementThresholds.MILLIS_PER_SECOND

        assertThat(AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_MS).isEqualTo(expected)
    }

    @Test
    fun theEarlyChallengeWinSitsBelowTheFirstLadderTier() {
        // D-02: the one-shot must be reachable inside the first session, before the
        // incremental ladder has moved far enough to feel like progress.
        assertThat(AchievementThresholds.EARLY_CHALLENGE_LEVEL)
            .isLessThan(AchievementThresholds.LADDER_TIER_1)
    }
}
