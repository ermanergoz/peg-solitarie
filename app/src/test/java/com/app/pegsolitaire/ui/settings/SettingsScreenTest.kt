package com.app.pegsolitaire.ui.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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

    private fun setContent(
        onShowHowToPlay: () -> Unit = {},
        state: SettingsUiState = SettingsUiState(),
        onOpenLicenses: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                SettingsScreen(
                    state = state,
                    onToggleSound = {},
                    onToggleHaptics = {},
                    onShowHowToPlay = onShowHowToPlay,
                    onOpenLicenses = onOpenLicenses,
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

    /**
     * Spec LS-AC01 as amended (loop 1, F-09; brief A1.4): every Settings section header is a
     * heading, so TalkBack's heading navigation moves section to section — and nothing else on
     * the screen is one. One change to `SettingsSectionHeader`, never to a single header.
     */
    @Test
    fun everySectionHeaderIsAHeading() {
        val signedIn = GamesSignInState.SignedIn(playerId = "player", displayName = null)
        setContent(state = SettingsUiState(gamesSignInState = signedIn))

        val headings =
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
                .fetchSemanticsNodes()
                .map { node -> node.config[SemanticsProperties.Text].joinToString { it.text } }

        assertThat(headings)
            .containsExactly(
                app.getString(R.string.settings_section_general),
                app.getString(R.string.settings_section_play_games),
                app.getString(R.string.settings_section_about),
                app.getString(R.string.settings_section_privacy),
            ).inOrder()
    }

    /** Spec LS-AC01: About and its Licenses row sit after Play Games and before Privacy. */
    @Test
    fun theAboutHeaderAndLicensesRowSitBetweenPlayGamesAndPrivacy() {
        val signedIn = GamesSignInState.SignedIn(playerId = "player", displayName = null)
        setContent(state = SettingsUiState(gamesSignInState = signedIn))

        val playGames = topOf(app.getString(R.string.settings_section_play_games))
        val about = topOf(app.getString(R.string.settings_section_about))
        val licenses = topOf(app.getString(R.string.settings_licenses_label))
        val privacy = topOf(app.getString(R.string.settings_section_privacy))
        assertThat(playGames).isLessThan(about)
        assertThat(about).isLessThan(licenses)
        assertThat(licenses).isLessThan(privacy)
    }

    /** With no Play Games section (D-11), About simply follows General. */
    @Test
    fun theAboutSectionFollowsGeneralWhenPlayGamesIsAbsent() {
        setContent()

        val haptics = topOf(app.getString(R.string.settings_haptics_label))
        val about = topOf(app.getString(R.string.settings_section_about))
        val privacy = topOf(app.getString(R.string.settings_section_privacy))
        assertThat(haptics).isLessThan(about)
        assertThat(about).isLessThan(privacy)
    }

    @Test
    fun tappingTheLicensesRowOpensLicenses() {
        var opened = 0
        setContent(onOpenLicenses = { opened++ })

        composeRule.onNodeWithText(app.getString(R.string.settings_licenses_label)).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(opened).isEqualTo(1)
    }

    /**
     * Device re-run F-3 (the tech-lead's re-run ruling): on a window wider than `content_max_width`
     * the Settings column is capped at it and centred, as its KDoc promises and as the Licenses
     * screens it opens are. The cap is read from the resources, never written as a number.
     */
    @Test
    @Config(qualifiers = SETTINGS_TABLET_WINDOW)
    fun theSettingsColumnIsCappedAtContentMaxWidthAndCentredOnAWideWindow() {
        setContent()

        val window =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .size.width
        val cap = app.resources.getDimension(R.dimen.content_max_width)
        val column = composeRule.onNodeWithTag(SETTINGS_CONTENT_TAG, useUnmergedTree = true).fetchSemanticsNode()
        assertWithMessage("the Settings column's width on a ${window}px window, against the cap")
            .that(column.size.width.toFloat())
            .isWithin(WIDTH_TOLERANCE_PX)
            .of(cap)
        assertWithMessage("the Settings column's left edge, against equal margins")
            .that(column.positionInRoot.x)
            .isWithin(WIDTH_TOLERANCE_PX)
            .of((window - cap) / 2)
    }

    /** The guard on the phone: the order that caps the column must still let it fill a narrow window. */
    @Test
    @Config(qualifiers = SETTINGS_NARROW_WINDOW)
    fun theColumnFillsAWindowNarrowerThanTheCap() {
        setContent()

        val window =
            composeRule
                .onRoot()
                .fetchSemanticsNode()
                .size.width
        val column = composeRule.onNodeWithTag(SETTINGS_CONTENT_TAG, useUnmergedTree = true).fetchSemanticsNode()
        assertWithMessage("the Settings column's width, against the window's").that(column.size.width).isEqualTo(window)
        assertWithMessage("the Settings column's left edge")
            .that(column.positionInRoot.x)
            .isWithin(WIDTH_TOLERANCE_PX)
            .of(0f)
    }

    private fun topOf(text: String): Float =
        composeRule
            .onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
            .positionInRoot.y
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val SETTINGS_SCREEN_ROBOLECTRIC_SDK = 34

/** The tablet window QA measured F-3 on: wider than the 480 dp cap. */
private const val SETTINGS_TABLET_WINDOW = "w800dp-h1280dp"

/** A small phone's window: narrower than the cap, so the column fills it. */
private const val SETTINGS_NARROW_WINDOW = "w360dp-h640dp"

/** How far the measured column may sit from its expected bounds: one pixel of rounding. */
private const val WIDTH_TOLERANCE_PX = 1f
