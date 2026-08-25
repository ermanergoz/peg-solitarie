package com.app.pegsolitaire.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.pegsolitaire.data.ClassicLevelIds
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Render proof that Home actually *shows and uses* what its state carries — the half a
 * ViewModel assertion cannot cover.
 *
 * Without this, `HomeUiState.challengeCurrentLevel` and the PERS-03 resume flags could be
 * collected correctly and still never reach the screen (the "invisible feature" trap): the
 * Challenge tile would read "Level 1" forever, and a saved game would offer no way back
 * into it however faithfully the snapshot was stored.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun challengeTileShowsThePersistedLevelAndEntersAtIt() {
        var enteredLevel: Int? = null
        renderHome(
            state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL),
            onPlayChallenge = { level -> enteredLevel = level },
        )

        // The tile is labelled with the persisted level, verbatim from strings.xml.
        composeRule.onNodeWithText(CONTINUE_LABEL).assertExists()

        // ...and tapping it enters Challenge at that same level, not a hardcoded 1.
        composeRule.onNodeWithText(CONTINUE_LABEL).performClick()
        composeRule.waitForIdle()
        assertThat(enteredLevel).isEqualTo(PERSISTED_LEVEL)
    }

    @Test
    fun noContinueAffordanceRendersWhenNothingIsSaved() {
        renderHome(state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL))

        composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).assertDoesNotExist()
        composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).assertDoesNotExist()
    }

    @Test
    fun aSavedClassicGameOffersAContinueThatResumesItsOwnBoard() {
        var resumedBoard: String? = null
        renderHome(
            state = HomeUiState(classicResumeLevelId = RESUMABLE_LEVEL_ID),
            onResumeClassic = { levelId -> resumedBoard = levelId },
        )

        composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).performClick()
        composeRule.waitForIdle()

        assertThat(resumedBoard).isEqualTo(RESUMABLE_LEVEL_ID)
    }

    @Test
    fun bothContinueAffordancesRenderTogetherAndResumeTheirOwnMode() {
        // D-03: a Classic and a Challenge game coexist, so Home shows both entries.
        // The Challenge Continue deliberately diverges from the ladder level here: the
        // tile must label and resume at the SNAPSHOT's stamped level (WR-03), never at
        // challengeCurrentLevel (which can lag at a cold start).
        var resumedLevel: Int? = null
        renderHome(
            state =
                HomeUiState(
                    challengeCurrentLevel = PERSISTED_LEVEL,
                    classicResumeLevelId = RESUMABLE_LEVEL_ID,
                    challengeResumeLevel = STAMPED_RESUME_LEVEL,
                ),
            onResumeChallenge = { level -> resumedLevel = level },
        )

        composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).assertExists()
        composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).assertExists()
        composeRule.onNodeWithText(STAMPED_RESUME_SUPPORTING).assertExists()

        composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).performClick()
        composeRule.waitForIdle()
        assertThat(resumedLevel).isEqualTo(STAMPED_RESUME_LEVEL)
    }

    private fun renderHome(
        state: HomeUiState,
        onPlayChallenge: (Int) -> Unit = {},
        onResumeClassic: (String) -> Unit = {},
        onResumeChallenge: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            HomeScreen(
                state = state,
                onPlayClassic = {},
                onPlayChallenge = onPlayChallenge,
                onResumeClassic = onResumeClassic,
                onResumeChallenge = onResumeChallenge,
                onShowHowToPlay = {},
                onDismissOnboarding = {},
                onOpenSettings = {},
            )
        }
        composeRule.waitForIdle()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val HOME_ROBOLECTRIC_SDK = 34

/** A persisted level past the on-ramp, so a hardcoded 1 cannot pass by accident. */
private const val PERSISTED_LEVEL = 7

/** The rendered tile copy for [PERSISTED_LEVEL] (verbatim from `strings.xml`). */
private const val CONTINUE_LABEL = "Continue — Level 7"

/** The Classic board a saved game sits on — carried into the resume by the tile. */
private val RESUMABLE_LEVEL_ID = ClassicLevelIds.EUROPEAN37

/** The rendered Continue-affordance titles (verbatim from `strings.xml`). */
private const val RESUME_CLASSIC_LABEL = "Continue Classic"
private const val RESUME_CHALLENGE_LABEL = "Continue Challenge"

/** The snapshot's stamped level — deliberately NOT the ladder level (WR-03). */
private const val STAMPED_RESUME_LEVEL = 9

/** The rendered supporting copy for [STAMPED_RESUME_LEVEL] (verbatim from `strings.xml`). */
private const val STAMPED_RESUME_SUPPORTING = "Pick up Level 9 where you left off"
