package com.app.pegsolitaire.ui.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.app.pegsolitaire.testing.TURKISH_DEVICE
import com.app.pegsolitaire.testing.englishString
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Settings' main flow on a Turkish phone (docs/ux/localisation-tr.md §7.6, criterion S-1): the four
 * section headings in order, the rows with their supporting lines, and the rows that lead somewhere
 * still do. Nodes are found by the strings this configuration resolves, never by typed copy;
 * [englishString] shows those strings are not the English ones. "Play Games" is a proper name and
 * reads the same in both languages (spec D-4).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SETTINGS_TURKISH_SDK], qualifiers = TURKISH_DEVICE)
class SettingsScreenTurkishTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    private var tutorialOpenings = 0
    private var licencesOpenings = 0
    private val colorblindFlips = mutableListOf<Boolean>()

    @Test
    fun theFourSectionHeadingsReadInTurkishInTheirOrder() {
        render(SettingsUiState(gamesSignInState = GamesSignInState.SignedIn(playerId = PLAYER, displayName = null)))

        val headings =
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
                .fetchSemanticsNodes()
                .map { node -> node.config[SemanticsProperties.Text].joinToString { it.text } }

        assertThat(app.getString(R.string.settings_section_general))
            .isNotEqualTo(englishString(R.string.settings_section_general))
        assertThat(headings)
            .containsExactly(
                app.getString(R.string.settings_section_general),
                app.getString(R.string.settings_section_play_games),
                app.getString(R.string.settings_section_about),
                app.getString(R.string.settings_section_privacy),
            ).inOrder()
    }

    @Test
    fun everyGeneralRowShowsItsTurkishLabelAndSupportingLine() {
        render(SettingsUiState())

        listOf(
            R.string.settings_how_to_play_label,
            R.string.settings_how_to_play_supporting,
            R.string.settings_sound_label,
            R.string.settings_sound_supporting,
            R.string.settings_haptics_label,
            R.string.settings_haptics_supporting,
            R.string.settings_colorblind_label,
            R.string.settings_colorblind_supporting,
        ).forEach { id ->
            assertThat(app.getString(id)).isNotEqualTo(englishString(id))
            composeRule.onNodeWithText(app.getString(id)).performScrollTo().assertIsDisplayed()
        }
    }

    @Test
    fun theHowToPlayAndLicensesRowsStillOpenTheirScreens() {
        render(SettingsUiState())

        assertThat(app.getString(R.string.settings_licenses_label))
            .isNotEqualTo(englishString(R.string.settings_licenses_label))
        composeRule.onNodeWithText(app.getString(R.string.settings_how_to_play_label)).performScrollTo().performClick()
        composeRule.onNodeWithText(app.getString(R.string.settings_licenses_label)).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(tutorialOpenings).isEqualTo(1)
        assertThat(licencesOpenings).isEqualTo(1)
    }

    @Test
    fun theColorblindSwitchCarriesItsTurkishLabelAndReportsTheNewValue() {
        render(SettingsUiState())

        assertThat(app.getString(R.string.settings_colorblind_label))
            .isNotEqualTo(englishString(R.string.settings_colorblind_label))
        composeRule
            .onNodeWithContentDescription(app.getString(R.string.settings_colorblind_label))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertThat(colorblindFlips).containsExactly(true)
    }

    private fun render(state: SettingsUiState) {
        composeRule.setContent {
            AppTheme {
                SettingsScreen(
                    state = state,
                    onToggleSound = {},
                    onToggleHaptics = {},
                    onToggleColorblindPalette = { colorblindFlips += it },
                    onShowHowToPlay = { tutorialOpenings++ },
                    onOpenLicenses = { licencesOpenings++ },
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
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val SETTINGS_TURKISH_SDK = 34

/** A signed-in player id, so the Play Games section is on the screen. */
private const val PLAYER = "player"
