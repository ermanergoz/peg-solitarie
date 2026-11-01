package com.app.pegsolitaire.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.testing.TURKISH_DEVICE
import com.app.pegsolitaire.testing.englishString
import com.app.pegsolitaire.ui.home.fixture.CLEARED_BOARDS
import com.app.pegsolitaire.ui.home.fixture.EARNED_STARS
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.LEVEL_PILL
import com.app.pegsolitaire.ui.home.fixture.PERSISTED_LEVEL
import com.app.pegsolitaire.ui.home.fixture.RESUMABLE_LEVEL_ID
import com.app.pegsolitaire.ui.home.fixture.STAMPED_RESUME_LEVEL
import com.app.pegsolitaire.ui.home.fixture.STARS_PILL
import com.app.pegsolitaire.ui.home.fixture.announcement
import com.app.pegsolitaire.ui.home.fixture.challengeTileTitle
import com.app.pegsolitaire.ui.home.fixture.classicTileTitle
import com.app.pegsolitaire.ui.home.fixture.levelLabel
import com.app.pegsolitaire.ui.home.fixture.pillDescriptions
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.starsLabel
import com.app.pegsolitaire.ui.home.fixture.string
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Home's main flow on a Turkish phone (docs/ux/localisation-tr.md §7.6, criteria H-1 to H-3): the
 * mode tiles, the Challenge chip, both Continue tiles and the ribbon read in Turkish, and each still
 * leads where it did. Nodes are found by the strings this configuration resolves, never by typed
 * copy; [englishString] shows those strings are not the English ones.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK], qualifiers = TURKISH_DEVICE)
class HomeScreenTurkishTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theModeTilesReadInTurkishAndTheChallengeChipEntersThePersistedLevel() {
        var enteredLevel: Int? = null
        composeRule.renderHome(
            state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL),
            onPlayChallenge = { level -> enteredLevel = level },
        )
        val chip = string(R.string.challenge_tile_continue, PERSISTED_LEVEL)

        assertThat(challengeTileTitle).isNotEqualTo(englishString(R.string.challenge_tile_title))
        assertThat(chip).isNotEqualTo(englishString(R.string.challenge_tile_continue, PERSISTED_LEVEL))
        composeRule.onNodeWithText(classicTileTitle).assertExists()
        composeRule.onNodeWithText(challengeTileTitle).assertExists()

        composeRule.onNodeWithText(chip).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(enteredLevel).isEqualTo(PERSISTED_LEVEL)
    }

    @Test
    fun bothContinueTilesReadInTurkishAndResumeTheirOwnGames() {
        var resumedBoard: String? = null
        var resumedLevel: Int? = null
        composeRule.renderHome(
            state =
                HomeUiState(
                    challengeCurrentLevel = PERSISTED_LEVEL,
                    classicResumeLevelId = RESUMABLE_LEVEL_ID,
                    classicResumeBoard = Boards.european37Initial,
                    challengeResumeLevel = STAMPED_RESUME_LEVEL,
                ),
            onResumeClassic = { levelId -> resumedBoard = levelId },
            onResumeChallenge = { level -> resumedLevel = level },
        )
        val classicTitle = string(R.string.resume_classic_title)
        val challengeTitle = string(R.string.resume_challenge_title)

        assertThat(classicTitle).isNotEqualTo(englishString(R.string.resume_classic_title))
        assertThat(challengeTitle).isNotEqualTo(englishString(R.string.resume_challenge_title))
        composeRule.onNodeWithText(string(R.string.resume_classic_supporting)).assertExists()
        composeRule.onNodeWithText(string(R.string.resume_challenge_supporting, STAMPED_RESUME_LEVEL)).assertExists()

        composeRule.onNodeWithText(classicTitle).performScrollTo().performClick()
        composeRule.onNodeWithText(challengeTitle).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(resumedBoard).isEqualTo(RESUMABLE_LEVEL_ID)
        assertThat(resumedLevel).isEqualTo(STAMPED_RESUME_LEVEL)
    }

    @Test
    fun eachRibbonPillSpeaksItsTurkishLabelThenItsValue() {
        composeRule.renderHome(
            state =
                HomeUiState(
                    totalStars = EARNED_STARS,
                    classicBoardsCleared = CLEARED_BOARDS,
                    challengeCurrentLevel = PERSISTED_LEVEL,
                ),
        )

        assertThat(starsLabel).isNotEqualTo(englishString(R.string.home_stat_stars_label))
        assertThat(composeRule.pillDescriptions(STARS_PILL))
            .containsExactly(announcement(starsLabel, EARNED_STARS.toString()))
        assertThat(composeRule.pillDescriptions(LEVEL_PILL))
            .containsExactly(announcement(levelLabel, PERSISTED_LEVEL.toString()))
    }

    @Test
    fun aPillWithNoValueYetIsSpokenInTurkishAndNeverAsTheDash() {
        // Criterion H-3: "Yıldız, henüz yok", the placeholder's spoken form, never the em dash.
        composeRule.renderHome(state = HomeUiState())
        val notYet = string(R.string.stat_value_unknown_desc)

        assertThat(notYet).isNotEqualTo(englishString(R.string.stat_value_unknown_desc))
        assertThat(composeRule.pillDescriptions(STARS_PILL)).containsExactly(announcement(starsLabel, notYet))
    }
}
