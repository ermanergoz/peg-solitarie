package com.app.pegsolitaire.domain.achievement

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the pure-domain [AchievementRules] kernel (PGSA-01..PGSA-03).
 *
 * Every one of the 20 roster entries is exercised here, because this object is the whole
 * of what `:domain` contributes to Phase 12: everything downstream — the SQL projection
 * (12-06), the submission loop (12-07), the Play Console entries (12-08) — is a faithful
 * transport of what these rules decide, and none of it is JVM-testable the way this is.
 *
 * Expectations about POLICY values are written as their named [AchievementThresholds]
 * constants, never as bare `10`/`250`/`5` literals, per `AssistAllowanceTest`'s rule: a
 * literal here would be a second statement of a number the constants object exists to
 * state once. Bare literals used as arbitrary *fixture* values (a ladder of `137`, a star
 * total of `400`) are fine — they are inputs chosen to be far from any boundary, not
 * restatements of a tunable.
 */
class AchievementRulesTest {
    private val clearedIdByBoard =
        mapOf(
            ClassicBoard.ENGLISH33 to AchievementId.CLEARED_ENGLISH33,
            ClassicBoard.EUROPEAN37 to AchievementId.CLEARED_EUROPEAN37,
            ClassicBoard.ASYMMETRIC39 to AchievementId.CLEARED_ASYMMETRIC39,
            ClassicBoard.DIAMOND41 to AchievementId.CLEARED_DIAMOND41,
            ClassicBoard.WIEGLEB45 to AchievementId.CLEARED_WIEGLEB45,
        )

    private val ladderTiers =
        listOf(AchievementId.LADDER_10, AchievementId.LADDER_50, AchievementId.LADDER_100)

    private val starTiers =
        listOf(AchievementId.STARS_25, AchievementId.STARS_100, AchievementId.STARS_250)

    /** Everything earned: the fixture that must reach all 20 roster entries. */
    private val maximalAggregate =
        ProgressAggregate(
            highestChallengeLevel = 500,
            challengeScoreTotal = 1_234_567L,
            totalStars = 900,
            solvedLevelCount = 400,
            threeStarCount = 300,
            clearedClassicBoards = ClassicBoard.entries.toSet(),
            perfectClassicBoards = ClassicBoard.entries.toSet(),
            anyHintFreeSolve = true,
            anyUndoFreeSolve = true,
            anyCenterFinish = true,
            anySpeedSolve = true,
        )

    // ---- The fresh player, and the never-zero-steps rule ---------------------

    @Test
    fun aFreshPlayerUnlocksNothing() {
        val outcome = AchievementRules.evaluate(ProgressAggregate())

        assertThat(outcome.unlocked).isEmpty()
    }

    @Test
    fun aFreshPlayerEmitsAnEmptyStepsMapRatherThanZeroValuedEntries() {
        // T-12-05 / Pitfall 4: `setSteps` requires numSteps > 0, and a rejected submission
        // sets the D-10 pending flag — a zero here would give every new install an
        // unclearable dirty state on each clean launch.
        val outcome = AchievementRules.evaluate(ProgressAggregate())

        assertThat(outcome.steps).isEmpty()
    }

    @Test
    fun noFixtureEverProducesANonPositiveStepValue() {
        val fixtures =
            listOf(
                ProgressAggregate(),
                ProgressAggregate(highestChallengeLevel = 0, totalStars = 0),
                ProgressAggregate(highestChallengeLevel = 1, totalStars = 1),
                ProgressAggregate(solvedLevelCount = 1, threeStarCount = 1),
                maximalAggregate,
            )

        fixtures.forEach { aggregate ->
            val outcome = AchievementRules.evaluate(aggregate)

            assertThat(outcome.steps.values.none { it <= 0 }).isTrue()
        }
    }

    // ---- The four first-hour early wins (D-02) --------------------------------

    @Test
    fun oneSolvedLevelUnlocksFirstSolveAndNothingUnrelated() {
        val outcome = AchievementRules.evaluate(ProgressAggregate(solvedLevelCount = 1))

        assertThat(outcome.unlocked).containsExactly(AchievementId.FIRST_SOLVE)
    }

    @Test
    fun oneThreeStarResultUnlocksFirstThreeStar() {
        val outcome = AchievementRules.evaluate(ProgressAggregate(threeStarCount = 1))

        assertThat(outcome.unlocked).contains(AchievementId.FIRST_THREE_STAR)
    }

    @Test
    fun reachingTheEarlyChallengeLevelUnlocksTheOneShot() {
        val aggregate =
            ProgressAggregate(highestChallengeLevel = AchievementThresholds.EARLY_CHALLENGE_LEVEL)

        val outcome = AchievementRules.evaluate(aggregate)

        assertThat(outcome.unlocked).contains(AchievementId.CHALLENGE_LEVEL_5)
    }

    @Test
    fun oneLevelBelowTheEarlyChallengeLevelDoesNotUnlockIt() {
        val aggregate =
            ProgressAggregate(
                highestChallengeLevel = AchievementThresholds.EARLY_CHALLENGE_LEVEL - 1,
            )

        val outcome = AchievementRules.evaluate(aggregate)

        assertThat(outcome.unlocked).doesNotContain(AchievementId.CHALLENGE_LEVEL_5)
    }

    @Test
    fun aPlayerWhoHasNeverPlayedChallengeDoesNotUnlockTheEarlyChallengeWin() {
        // `highestChallengeLevel = null` is SQL NULL over an empty Challenge history — it
        // must read as "no level", not blow up and not compare as a level.
        val outcome = AchievementRules.evaluate(ProgressAggregate(highestChallengeLevel = null))

        assertThat(outcome.unlocked).doesNotContain(AchievementId.CHALLENGE_LEVEL_5)
    }

    // ---- The tiered axes: absolute steps, never deltas (D-03/D-12) ------------

    @Test
    fun theLadderTiersAllReceiveTheAbsoluteHighestChallengeLevelUnclamped() {
        // RESEARCH: steps above a tier's total auto-unlock it and further mutation is
        // inert, so clamping would be lossy for no benefit.
        val outcome = AchievementRules.evaluate(ProgressAggregate(highestChallengeLevel = 137))

        ladderTiers.forEach { tier ->
            assertThat(outcome.steps[tier]).isEqualTo(137)
        }
    }

    @Test
    fun theStarTiersAllReceiveTheAbsoluteTotalStarsUnclamped() {
        val outcome = AchievementRules.evaluate(ProgressAggregate(totalStars = 400))

        starTiers.forEach { tier ->
            assertThat(outcome.steps[tier]).isEqualTo(400)
        }
    }

    @Test
    fun aNullLadderEmitsNoLadderStepEntries() {
        val outcome = AchievementRules.evaluate(ProgressAggregate(highestChallengeLevel = null))

        assertThat(outcome.steps.keys).containsNoneIn(ladderTiers)
    }

    @Test
    fun zeroTotalStarsEmitsNoStarStepEntries() {
        val outcome = AchievementRules.evaluate(ProgressAggregate(totalStars = 0))

        assertThat(outcome.steps.keys).containsNoneIn(starTiers)
    }

    @Test
    fun theTieredIdsAreNeverUnlockedDirectlyEvenFarAboveTheTopTier() {
        // D-12: Play Games auto-unlocks an incremental achievement when its steps reach
        // the total. Unlocking one here would be a redundant call that also throws away
        // the progress bar the tiers exist to show.
        val outcome = AchievementRules.evaluate(maximalAggregate)

        assertThat(outcome.unlocked.none { it.isIncremental }).isTrue()
    }

    @Test
    fun theUnlockSetAndTheStepsMapAreDisjoint() {
        val outcome = AchievementRules.evaluate(maximalAggregate)

        assertThat(outcome.unlocked.intersect(outcome.steps.keys)).isEmpty()
    }

    // ---- The four skill flags, each independent (D-01/D-04) -------------------

    @Test
    fun theHintFreeFlagUnlocksOnlyHintFree() {
        val outcome = AchievementRules.evaluate(ProgressAggregate(anyHintFreeSolve = true))

        assertThat(outcome.unlocked).containsExactly(AchievementId.HINT_FREE)
    }

    @Test
    fun theUndoFreeFlagUnlocksOnlyUndoFree() {
        val outcome = AchievementRules.evaluate(ProgressAggregate(anyUndoFreeSolve = true))

        assertThat(outcome.unlocked).containsExactly(AchievementId.UNDO_FREE)
    }

    @Test
    fun theCenterFinishFlagUnlocksOnlyCenterFinish() {
        val outcome = AchievementRules.evaluate(ProgressAggregate(anyCenterFinish = true))

        assertThat(outcome.unlocked).containsExactly(AchievementId.CENTER_FINISH)
    }

    @Test
    fun theSpeedSolveFlagUnlocksOnlySpeedSolve() {
        val outcome = AchievementRules.evaluate(ProgressAggregate(anySpeedSolve = true))

        assertThat(outcome.unlocked).containsExactly(AchievementId.SPEED_SOLVE)
    }

    // ---- The five per-board unlocks and the two completionist one-shots -------

    @Test
    fun eachClearedBoardUnlocksExactlyItsOwnAchievementAndNoOtherBoardsOne() {
        ClassicBoard.entries.forEach { board ->
            val outcome =
                AchievementRules.evaluate(ProgressAggregate(clearedClassicBoards = setOf(board)))

            val otherBoardIds = clearedIdByBoard.filterKeys { it != board }.values

            assertThat(outcome.unlocked).contains(clearedIdByBoard.getValue(board))
            assertThat(outcome.unlocked).containsNoneIn(otherBoardIds)
        }
    }

    @Test
    fun clearingAllFiveBoardsUnlocksGrandTour() {
        val aggregate = ProgressAggregate(clearedClassicBoards = ClassicBoard.entries.toSet())

        val outcome = AchievementRules.evaluate(aggregate)

        assertThat(outcome.unlocked).contains(AchievementId.GRAND_TOUR)
    }

    @Test
    fun clearingFourOfFiveBoardsDoesNotUnlockGrandTour() {
        val fourBoards = ClassicBoard.entries.toSet() - ClassicBoard.WIEGLEB45

        val outcome = AchievementRules.evaluate(ProgressAggregate(clearedClassicBoards = fourBoards))

        assertThat(outcome.unlocked).doesNotContain(AchievementId.GRAND_TOUR)
    }

    @Test
    fun perfectingAllFiveBoardsUnlocksPerfectionist() {
        val aggregate = ProgressAggregate(perfectClassicBoards = ClassicBoard.entries.toSet())

        val outcome = AchievementRules.evaluate(aggregate)

        assertThat(outcome.unlocked).contains(AchievementId.PERFECTIONIST)
    }

    @Test
    fun perfectingFourOfFiveBoardsDoesNotUnlockPerfectionist() {
        // Diamond-41 is deliberately one of the four: its 3-star band IS its proven 2-peg
        // floor (D-04), so it must be as ordinarily perfectible as any other board.
        val fourBoards = ClassicBoard.entries.toSet() - ClassicBoard.DIAMOND41

        val outcome = AchievementRules.evaluate(ProgressAggregate(perfectClassicBoards = fourBoards))

        assertThat(outcome.unlocked).doesNotContain(AchievementId.PERFECTIONIST)
    }

    @Test
    fun clearingABoardDoesNotByItselfMakeItPerfect() {
        val aggregate = ProgressAggregate(clearedClassicBoards = ClassicBoard.entries.toSet())

        val outcome = AchievementRules.evaluate(aggregate)

        assertThat(outcome.unlocked).doesNotContain(AchievementId.PERFECTIONIST)
    }

    // ---- Leaderboard-only field, purity, and roster completeness --------------

    @Test
    fun theLeaderboardOnlyChallengeScoreTotalChangesNothingAboutTheOutcome() {
        val withoutScore = ProgressAggregate(solvedLevelCount = 1, challengeScoreTotal = null)
        val withScore = withoutScore.copy(challengeScoreTotal = 9_876_543_210L)

        assertThat(AchievementRules.evaluate(withScore))
            .isEqualTo(AchievementRules.evaluate(withoutScore))
    }

    @Test
    fun everyOneOfTheTwentyAchievementsIsReachableFromASingleMaximalAggregate() {
        // PGSA-01 / D-05: an id no aggregate can ever produce is a Play Console entry and
        // an uploaded icon that no player can earn.
        val outcome = AchievementRules.evaluate(maximalAggregate)

        assertThat(outcome.unlocked + outcome.steps.keys)
            .isEqualTo(AchievementId.entries.toSet())
    }

    @Test
    fun evaluatingTheSameAggregateTwiceYieldsEqualOutcomes() {
        // D-11: the rules are a pure total function, which is what makes the backfill's
        // resubmission of the same aggregate idempotent rather than merely harmless.
        assertThat(AchievementRules.evaluate(maximalAggregate))
            .isEqualTo(AchievementRules.evaluate(maximalAggregate))
    }
}
