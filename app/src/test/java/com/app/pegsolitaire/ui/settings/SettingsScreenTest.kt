package com.app.pegsolitaire.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The Settings › General "How to play" row (D-02a, §4.5) — the tutorial's one permanent
 * entry point after D-02 deletes Home's info icon.
 *
 * Its position is asserted, not just its presence: the section's entrance-order constants are
 * positional, so a row inserted without renumbering the cascade fades in alongside the row
 * below it instead of above it — a defect no other test in the suite can see. Position is read
 * from the rendered top coordinates rather than from the constants, so the assertion survives a
 * renumbering and fails a half-renumbering.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SETTINGS_SCREEN_ROBOLECTRIC_SDK])
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    private val generalHeader get() = app.getString(R.string.settings_section_general)
    private val howToPlayLabel get() = app.getString(R.string.settings_how_to_play_label)
    private val howToPlaySupporting get() = app.getString(R.string.settings_how_to_play_supporting)
    private val soundLabel get() = app.getString(R.string.settings_sound_label)

    private fun setContent(onShowHowToPlay: () -> Unit = {}) {
        composeRule.setContent {
            AppTheme {
                SettingsScreen(
                    state = SettingsUiState(),
                    onToggleSound = {},
                    onToggleHaptics = {},
                    onShowHowToPlay = onShowHowToPlay,
                    onSignInToPlayGames = {},
                    onShowLeaderboards = {},
                    onShowAchievements = {},
                    onPlayGamesSignInErrorShown = {},
                    onManagePrivacyChoices = {},
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theGeneralSectionOpensWithAHowToPlayRowAboveTheSoundToggle() {
        setContent()

        val header = composeRule.onNodeWithText(generalHeader).fetchSemanticsNode()
        val howToPlay = composeRule.onNodeWithText(howToPlayLabel).fetchSemanticsNode()
        val sound = composeRule.onNodeWithText(soundLabel).fetchSemanticsNode()

        assertThat(header.positionInRoot.y).isLessThan(howToPlay.positionInRoot.y)
        assertThat(howToPlay.positionInRoot.y).isLessThan(sound.positionInRoot.y)
    }

    @Test
    fun theHowToPlayRowCarriesItsOwnSupportingLine() {
        setContent()

        composeRule.onNodeWithText(howToPlaySupporting).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tappingTheHowToPlayRowOpensTheTutorial() {
        // The invisible-feature trap: the row can render perfectly and forward nowhere, and a
        // player who has already finished the tutorial would then have no way back into it.
        var opened = 0
        setContent(onShowHowToPlay = { opened++ })

        composeRule.onNodeWithText(howToPlayLabel).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(opened).isEqualTo(1)
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val SETTINGS_SCREEN_ROBOLECTRIC_SDK = 34
