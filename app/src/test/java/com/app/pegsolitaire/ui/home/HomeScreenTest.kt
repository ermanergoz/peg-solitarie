package com.app.pegsolitaire.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Render proof that the Challenge tile actually *shows and uses* the persisted level
 * (CHAL-05) — the half a ViewModel assertion cannot cover.
 *
 * Without this, `HomeUiState.challengeCurrentLevel` could be collected correctly and
 * still never reach the screen (the "invisible feature" trap): the tile would read
 * "Level 1" forever and enter level 1 forever.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun challengeTileShowsThePersistedLevelAndEntersAtIt() {
        var enteredLevel: Int? = null
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL),
                onPlayClassic = {},
                onPlayChallenge = { level -> enteredLevel = level },
                onShowHowToPlay = {},
                onDismissOnboarding = {},
                onOpenSettings = {},
            )
        }
        composeRule.waitForIdle()

        // The tile is labelled with the persisted level, verbatim from strings.xml.
        composeRule.onNodeWithText(CONTINUE_LABEL).assertExists()

        // ...and tapping it enters Challenge at that same level, not a hardcoded 1.
        composeRule.onNodeWithText(CONTINUE_LABEL).performClick()
        composeRule.waitForIdle()
        assertThat(enteredLevel).isEqualTo(PERSISTED_LEVEL)
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val HOME_ROBOLECTRIC_SDK = 34

/** A persisted level past the on-ramp, so a hardcoded 1 cannot pass by accident. */
private const val PERSISTED_LEVEL = 7

/** The rendered tile copy for [PERSISTED_LEVEL] (verbatim from `strings.xml`). */
private const val CONTINUE_LABEL = "Continue — Level 7"
