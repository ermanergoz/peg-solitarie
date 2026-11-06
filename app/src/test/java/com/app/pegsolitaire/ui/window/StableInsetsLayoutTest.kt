package com.app.pegsolitaire.ui.window

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.classic.ClassicCatalogScreen
import com.app.pegsolitaire.ui.classic.ClassicCatalogUiState
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.game.GAME_ACTION_DOCK_TEST_TAG
import com.app.pegsolitaire.ui.game.GAME_BOARD_SLAB_TEST_TAG
import com.app.pegsolitaire.ui.game.GAME_STATUS_STRIP_TEST_TAG
import com.app.pegsolitaire.ui.game.GAME_TIMER_PILL_TEST_TAG
import com.app.pegsolitaire.ui.game.GameScreen
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.home.HOME_HERO_TEST_TAG
import com.app.pegsolitaire.ui.home.HOME_PROGRESS_RIBBON_TEST_TAG
import com.app.pegsolitaire.ui.home.fixture.RESUME_CHALLENGE_LABEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CLASSIC_LABEL
import com.app.pegsolitaire.ui.home.fixture.bothResumesState
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.licenses.LicenseCatalog
import com.app.pegsolitaire.ui.licenses.LicenseTextScreen
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState
import com.app.pegsolitaire.ui.licenses.LicensesScreen
import com.app.pegsolitaire.ui.onboarding.TutorialScreen
import com.app.pegsolitaire.ui.onboarding.TutorialUiState
import com.app.pegsolitaire.ui.results.ResultsHeadlineTier
import com.app.pegsolitaire.ui.results.ResultsScreen
import com.app.pegsolitaire.ui.results.ResultsUiState
import com.app.pegsolitaire.ui.settings.SettingsScreen
import com.app.pegsolitaire.ui.settings.SettingsUiState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Criterion 30d (docs/ux-spec.md D-05g part 3, §6.2 "Layout: the bars come and go as pixels, never
 * as layout"; docs/tech-design.md §3.8 (d)): every screen pads off the bars' **stable** insets and
 * the cutout, so no node moves when a bar is shown, hidden, or reported at any height in between.
 *
 * Each screen is composed once; then, for a 24dp status bar over a 48dp navigation bar and again for
 * a 54dp status bar over a 24dp gesture bar, the insets are dispatched in three states — bars
 * visible; bars hidden with the visible values still reported; bars hidden and reported at zero — and
 * every named node's bounds, unclipped and in px, must be the same in all three, to the pixel. A
 * screen that padded off a visible inset (a `Scaffold`'s default content insets, a default
 * `TopAppBar`, `safeDrawingPadding()`) moves by the bar's height between the first state and the
 * third.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [STABLE_LAYOUT_ROBOLECTRIC_SDK], qualifiers = DESIGN_BASELINE_QUALIFIER)
class StableInsetsLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theGameplayScreenStaysPutWhenTheBarsComeOrGo() {
        show {
            GameScreen(
                state = GameUiState.initial(),
                onCellTap = {},
                onRestart = {},
                onConfirmRestart = {},
                onDismissRestart = {},
                onAnimationFinished = {},
                onToggleHints = {},
                onUndo = {},
                onHint = {},
                onAcceptAdOffer = {},
                onDismissAdOffer = {},
                onHome = {},
                onNavigateBack = {},
            )
        }

        assertStaysPut(
            "gameplay",
            mapOf(
                "the status strip" to { tag(GAME_STATUS_STRIP_TEST_TAG) },
                "the back chip" to { description(R.string.navigate_back) },
                "the title" to { text(R.string.app_name) },
                "the timer pill" to { tag(GAME_TIMER_PILL_TEST_TAG) },
                "the board slab" to { tag(GAME_BOARD_SLAB_TEST_TAG) },
                "the action dock" to { tag(GAME_ACTION_DOCK_TEST_TAG) },
            ),
        )
    }

    @Test
    fun homeStaysPutWhenTheBarsComeOrGo() {
        composeRule.renderHome(bothResumesState())

        assertStaysPut(
            "Home",
            mapOf(
                "the title row" to { tag(HOME_HERO_TEST_TAG) },
                "the settings gear" to { description(R.string.settings_action) },
                "the progress ribbon" to { tag(HOME_PROGRESS_RIBBON_TEST_TAG) },
                "the Classic Continue chip" to { composeRule.onNodeWithText(RESUME_CLASSIC_LABEL) },
                "the Challenge Continue chip" to { composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL) },
                "the Classic tile" to { text(R.string.classic_tile_title) },
                "the Challenge tile" to { text(R.string.challenge_tile_title) },
            ),
        )
    }

    @Test
    fun resultsStaysPutWhenTheBarsComeOrGo() {
        show {
            ResultsScreen(
                state = CLASSIC_RESULTS,
                onReplay = {},
                onCatalog = {},
                onHome = {},
                onNextLevel = {},
                onRetry = {},
                onNavigateBack = {},
            )
        }

        assertStaysPut(
            "Results",
            mapOf(
                "the top bar's title" to { text(R.string.results_title) },
                "the top bar's back" to { description(R.string.navigate_back) },
                "Replay" to { text(R.string.action_replay) },
                "Catalog" to { text(R.string.action_catalog) },
                "Home" to { text(R.string.action_home) },
            ),
        )
    }

    @Test
    fun theTutorialStaysPutWhenTheBarsComeOrGo() {
        val state = TutorialUiState.initial()
        show {
            TutorialScreen(state = state, onCellTap = {}, onAnimationFinished = {}, onSkip = {}, onDone = {})
        }

        assertStaysPut(
            "the tutorial",
            mapOf(
                "the board" to { tag(BOARD_CANVAS_TEST_TAG) },
                "the caption" to { text(state.captionRes) },
                "Skip" to { text(R.string.onboarding_skip) },
            ),
        )
    }

    @Test
    fun theCatalogueStaysPutWhenTheBarsComeOrGo() {
        show { ClassicCatalogScreen(state = ClassicCatalogUiState.Empty, onSelect = {}, onNavigateBack = {}) }

        assertTopBarStaysPut("the catalogue", R.string.catalog_title)
    }

    @Test
    fun settingsStaysPutWhenTheBarsComeOrGo() {
        show {
            SettingsScreen(
                state = SettingsUiState(),
                onToggleSound = {},
                onToggleHaptics = {},
                onToggleColorblindPalette = {},
                onShowHowToPlay = {},
                onOpenLicenses = {},
                onSignInToPlayGames = {},
                onShowLeaderboards = {},
                onShowAchievements = {},
                onPlayGamesSignInErrorShown = {},
                onManagePrivacyChoices = {},
                onBack = {},
            )
        }

        assertTopBarStaysPut("Settings", R.string.settings_title)
    }

    @Test
    fun theLicencesListStaysPutWhenTheBarsComeOrGo() {
        show { LicensesScreen(sections = LicenseCatalog.sections, onOpenEntry = {}, onBack = {}) }

        assertTopBarStaysPut("the licences list", R.string.licenses_title)
    }

    @Test
    fun aLicenceTextStaysPutWhenTheBarsComeOrGo() {
        show { LicenseTextScreen(state = LicenseTextUiState.Failed(header = null), onBack = {}) }

        assertTopBarStaysPut("a licence text", R.string.licenses_title)
    }

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent { AppTheme(darkTheme = false) { content() } }
        composeRule.waitForIdle()
    }

    private fun assertTopBarStaysPut(
        screen: String,
        @StringRes title: Int,
    ) = assertStaysPut(
        screen,
        mapOf(
            "the top bar's title" to { text(title) },
            "the top bar's back" to { description(R.string.navigate_back) },
        ),
    )

    /**
     * For each inset pair, the [nodes]' bounds in each [BarsReport] equal their bounds with the bars
     * visible, to the pixel.
     */
    private fun assertStaysPut(
        screen: String,
        nodes: Map<String, () -> SemanticsNodeInteraction>,
    ) {
        INSET_PAIRS.forEach { (status, navigation) ->
            val readings =
                BarsReport.entries.associateWith { report ->
                    report.dispatch(status, navigation)
                    nodes.mapValues { (_, node) -> boundsOf(node()) }
                }
            val visible = readings.getValue(BarsReport.Visible)
            readings.forEach { (report, bounds) ->
                nodes.keys.forEach { name ->
                    assertWithMessage(
                        "$screen, $name, in px, with a ${status.value}dp status bar and a ${navigation.value}dp " +
                            "navigation bar: ${report.label}, against bars visible",
                    ).that(bounds.getValue(name)).isEqualTo(visible.getValue(name))
                }
            }
        }
    }

    private fun BarsReport.dispatch(
        status: Dp,
        navigation: Dp,
    ) = when (this) {
        BarsReport.Visible -> composeRule.applySystemBarInsets(top = status, bottom = navigation)
        BarsReport.HiddenStillReported ->
            composeRule.applySystemBarInsets(
                top = status,
                bottom = navigation,
                visible = false,
                reportedTop = status,
                reportedBottom = navigation,
            )
        BarsReport.Hidden -> composeRule.applySystemBarInsets(top = status, bottom = navigation, visible = false)
    }

    /** A node's bounds in the root, in px, before any ancestor clips it. */
    private fun boundsOf(node: SemanticsNodeInteraction): Rect {
        val semantics = node.fetchSemanticsNode()
        val origin = semantics.positionInRoot
        return Rect(origin.x, origin.y, origin.x + semantics.size.width, origin.y + semantics.size.height)
    }

    private fun tag(tag: String): SemanticsNodeInteraction = composeRule.onNodeWithTag(tag)

    private fun text(
        @StringRes id: Int,
    ): SemanticsNodeInteraction = composeRule.onNodeWithText(composeRule.activity.getString(id))

    private fun description(
        @StringRes id: Int,
    ): SemanticsNodeInteraction = composeRule.onNodeWithContentDescription(composeRule.activity.getString(id))

    /** The three ways a device can report the bars (§3.8 (g)). */
    private enum class BarsReport(
        val label: String,
    ) {
        Visible("bars visible"),
        HiddenStillReported("bars hidden, their visible insets still reported"),
        Hidden("bars hidden, their visible insets zero"),
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) on this machine; emulate SDK 34. */
private const val STABLE_LAYOUT_ROBOLECTRIC_SDK = 34

/** §4.3.1's design baseline: a 360x640dp phone. */
private const val DESIGN_BASELINE_QUALIFIER = "w360dp-h640dp"

/** Criterion 30d's two windows: 24dp over three-button navigation, and a hole-punch bar over a gesture bar. */
private val INSET_PAIRS = listOf(24.dp to 48.dp, 54.dp to 24.dp)

/** A finished Classic board: Replay, Catalog and Home. */
private val CLASSIC_RESULTS =
    ResultsUiState(
        stars = 2,
        pegsRemaining = 2,
        elapsedSeconds = 75,
        score = 1280,
        headlineTier = ResultsHeadlineTier.SOLVED,
        replayId = ClassicLevelIds.ENGLISH33,
    )
