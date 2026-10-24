package com.app.pegsolitaire.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.ui.classic.ClassicBoardUiState
import com.app.pegsolitaire.ui.classic.ClassicCatalogScreen
import com.app.pegsolitaire.ui.classic.ClassicCatalogUiState
import com.app.pegsolitaire.ui.game.GameScreen
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.home.HomeScreen
import com.app.pegsolitaire.ui.home.HomeUiState
import com.app.pegsolitaire.ui.results.ResultsHeadlineTier
import com.app.pegsolitaire.ui.results.ResultsScreen
import com.app.pegsolitaire.ui.results.ResultsUiState
import com.app.pegsolitaire.ui.settings.SettingsScreen
import com.app.pegsolitaire.ui.settings.SettingsUiState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * What the app's buttons sound like — and, just as deliberately, what they do NOT (SOUND-04,
 * D-09/D-11).
 *
 * D-09 is a *selective* coverage rule: navigational CTAs click, while back-navigation icons,
 * the hint toggle and the Settings switches stay silent. Selectivity is only a real property
 * if both halves are asserted, so the negative cases here are first-class tests rather than
 * afterthoughts — each one clicks an excluded control, proves its callback still fired, and
 * proves the recording player heard nothing. Without them a future "simplification" that folds
 * the cue into the shared `Modifier.claySquish()` would pass every positive test while making
 * exactly the excluded controls noisy.
 *
 * Each screen is composed stateless with hoisted callbacks (the `HomeScreenTest` shell) and a
 * recording `FakeSoundPlayer` is installed through
 * `CompositionLocalProvider(LocalSoundPlayer provides …)`, the same trick `GameScreenSoundTest`
 * uses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BUTTON_SOUND_ROBOLECTRIC_SDK])
class ButtonSoundTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    /** Records every cue a click fires instead of playing it. */
    private val sounds = FakeSoundPlayer()

    // ---- Home mode tiles + Continue (D-09 covered set) -----------------------

    @Test
    fun theClassicTilePlaysOneForwardClickAndStillNavigates() {
        var played = false
        renderHome(onPlayClassic = { played = true })

        clickHomeText(R.string.classic_tile_title)

        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
        assertThat(played).isTrue()
    }

    @Test
    fun theChallengeTilePlaysOneForwardClickAndEntersAtThePersistedLevel() {
        var enteredLevel: Int? = null
        renderHome(
            state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL),
            onPlayChallenge = { level -> enteredLevel = level },
        )

        clickHomeText(R.string.challenge_tile_title)

        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
        assertThat(enteredLevel).isEqualTo(PERSISTED_LEVEL)
    }

    @Test
    fun theResumeTilePlaysOneForwardClickAndStillResumesItsOwnBoard() {
        var resumedBoard: String? = null
        renderHome(
            state = HomeUiState(classicResumeLevelId = ClassicLevelIds.EUROPEAN37),
            onResumeClassic = { levelId -> resumedBoard = levelId },
        )

        clickHomeText(R.string.resume_classic_title)

        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
        assertThat(resumedBoard).isEqualTo(ClassicLevelIds.EUROPEAN37)
    }

    // ---- NEGATIVE: the Home Settings gear stays silent (D-09) -----------------

    /**
     * The surviving half of the retired `theHomeTopBarHowToPlayAndSettingsIconsMakeNoSound`
     * (ux-spec §13): Home's How-to-play icon and its `how_to_play` string are both deleted by
     * D-02, so the assertion is **narrowed**, not re-aimed. It is deliberately not pointed at
     * the new Settings row, which is a navigational CTA and therefore *plays* — see
     * [theSettingsHowToPlayRowPlaysTheForwardCueAndOpensTheTutorial] for that half.
     *
     * `settings_action` is preserved on purpose, so this keeps passing after the gear moves
     * from the bar into the Home hero.
     */
    @Test
    fun theHomeSettingsGearMakesNoSound() {
        var settingsOpened = false
        renderHome(onOpenSettings = { settingsOpened = true })

        clickIcon(R.string.settings_action)

        // A back-nav-class affordance under D-09: it acts, and it says nothing.
        assertThat(settingsOpened).isTrue()
        assertThat(sounds.cues).isEmpty()
    }

    // ---- The Settings "How to play" row is a navigational CTA (D-09, §4.5) ----

    /**
     * The positive half the retarget owes (§4.5, D-09, SOUND-04): the tutorial's entry point
     * moved from a silent Home icon to a Settings row, and a row that navigates speaks the
     * forward press cue like every other navigational CTA in the app.
     */
    @Test
    fun theSettingsHowToPlayRowPlaysTheForwardCueAndOpensTheTutorial() {
        var tutorialOpened = false
        renderSettings(onShowHowToPlay = { tutorialOpened = true })

        clickSettingsRow(R.string.settings_how_to_play_label)

        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
        assertThat(tutorialOpened).isTrue()
    }

    /** The Settings toggles stay silent (D-09) — the row above them is the only speaking one. */
    @Test
    fun theSettingsSoundToggleMakesNoSound() {
        var toggled: Boolean? = null
        renderSettings(onToggleSound = { toggled = it })

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.settings_sound_label))
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertThat(toggled).isNotNull()
        assertThat(sounds.cues).isEmpty()
    }

    // ---- Classic catalog board cards -----------------------------------------

    @Test
    fun aCatalogBoardCardPlaysOneForwardClickAndStillSelectsItsBoard() {
        var selectedId: String? = null
        renderCatalog(onSelect = { id -> selectedId = id })

        composeRule.onNodeWithText(CATALOG_BOARD_NAME).performClick()
        composeRule.waitForIdle()

        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
        assertThat(selectedId).isEqualTo(ClassicLevelIds.ENGLISH33)
    }

    // ---- NEGATIVE: the catalog back icon stays silent (D-09) -----------------

    @Test
    fun theCatalogBackIconMakesNoSound() {
        var navigatedBack = false
        renderCatalog(onNavigateBack = { navigatedBack = true })

        clickIcon(R.string.navigate_back)

        assertThat(navigatedBack).isTrue()
        assertThat(sounds.cues).isEmpty()
    }

    // ---- Results actions (every CTA is forward, D-11) ------------------------

    @Test
    fun theResultsPrimaryActionPlaysOneForwardClickAndStillReplays() {
        var replayedId: String? = null
        renderResults(onReplay = { id -> replayedId = id })

        clickResultsAction(R.string.action_replay)

        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
        assertThat(replayedId).isEqualTo(ClassicLevelIds.ENGLISH33)
    }

    @Test
    fun theResultsHomeActionPlaysOneForwardClickAndStillNavigatesHome() {
        var wentHome = false
        renderResults(onHome = { wentHome = true })

        clickResultsAction(R.string.action_home)

        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
        assertThat(wentHome).isTrue()
    }

    @Test
    fun theResultsCatalogActionPlaysOneForwardClickAndStillOpensTheCatalog() {
        var openedCatalog = false
        renderResults(onCatalog = { openedCatalog = true })

        clickResultsAction(R.string.action_catalog)

        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
        assertThat(openedCatalog).isTrue()
    }

    // ---- NEGATIVE: the Results back icon stays silent (D-09) -----------------

    @Test
    fun theResultsBackIconMakesNoSound() {
        var navigatedBack = false
        renderResults(onNavigateBack = { navigatedBack = true })

        clickIcon(R.string.navigate_back)

        assertThat(navigatedBack).isTrue()
        assertThat(sounds.cues).isEmpty()
    }

    // ---- The restart dialog is the forward/dismiss pair (D-11) ---------------

    @Test
    fun theRestartDialogConfirmPlaysTheForwardClick() {
        var confirmed = false
        renderRestartDialog(onConfirmRestart = { confirmed = true })

        clickText(R.string.restart_board)

        assertThat(sounds.cues).containsExactly(SoundCue.ButtonForward)
        assertThat(confirmed).isTrue()
    }

    @Test
    fun theRestartDialogDismissPlaysTheSofterDismissClickAndNeverTheForwardOne() {
        var dismissed = false
        renderRestartDialog(onDismissRestart = { dismissed = true })

        clickText(R.string.keep_playing)

        // D-11: cancel/dismiss is audibly its own thing, not the commit click.
        assertThat(sounds.cues).containsExactly(SoundCue.ButtonDismiss)
        assertThat(sounds.cues).doesNotContain(SoundCue.ButtonForward)
        assertThat(dismissed).isTrue()
    }

    // ---- NEGATIVE: the gameplay back icon and hint toggle stay silent --------

    @Test
    fun theGameTopBarBackIconAndHintToggleMakeNoSound() {
        var navigatedBack = false
        var toggledHints = false
        renderGame(
            onNavigateBack = { navigatedBack = true },
            onToggleHints = { toggledHints = true },
        )

        clickIcon(R.string.navigate_back)
        clickIcon(R.string.show_moves_on)

        // The hint toggle is a render gate, not a navigational CTA — D-09 keeps it quiet.
        assertThat(navigatedBack).isTrue()
        assertThat(toggledHints).isTrue()
        assertThat(sounds.cues).isEmpty()
    }

    // ---- Helpers -------------------------------------------------------------

    private fun clickText(resId: Int) {
        composeRule.onNodeWithText(context.getString(resId)).performClick()
        composeRule.waitForIdle()
    }

    /**
     * Home scrolls since Phase 15 (§4.2), and this rule's viewport is 320x470px, so its lower
     * bands sit below the fold here — scroll, then click. On a real 360x640dp phone at default
     * font scale they are all above it, which is criterion 15a and a device check.
     */
    private fun clickHomeText(resId: Int) = clickSettingsRow(resId)

    /** Settings scrolls too, and the default Robolectric viewport is short — scroll, then click. */
    private fun clickSettingsRow(resId: Int) {
        composeRule.onNodeWithText(context.getString(resId)).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    /** Results is the one screen whose content column scrolls, so its CTAs are scrolled to first. */
    private fun clickResultsAction(resId: Int) {
        composeRule.onNodeWithText(context.getString(resId)).performScrollTo().performClick()
        composeRule.waitForIdle()
    }

    private fun clickIcon(resId: Int) {
        composeRule.onNodeWithContentDescription(context.getString(resId)).performClick()
        composeRule.waitForIdle()
    }

    private fun renderHome(
        state: HomeUiState = HomeUiState(),
        onPlayClassic: () -> Unit = {},
        onPlayChallenge: (Int) -> Unit = {},
        onResumeClassic: (String) -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) = render {
        HomeScreen(
            state = state,
            onPlayClassic = onPlayClassic,
            onPlayChallenge = onPlayChallenge,
            onResumeClassic = onResumeClassic,
            onResumeChallenge = {},
            // Not exercised here: the opt-in prompt's own cues are asserted where the
            // dialog is (PlayGamesPromptDialogTest); this helper renders Home's body.
            onAcceptPlayGamesPrompt = {},
            onDeclinePlayGamesPrompt = {},
            // Not exercised here either: the swap dialog's own cues are asserted in
            // ProfileSwapDialogTest, and this helper renders Home's body.
            onKeepDeviceProgress = {},
            onUseCloudProgress = {},
            onPlayGamesSignInErrorShown = {},
            onOpenSettings = onOpenSettings,
        )
    }

    /** The Settings screen, stateless with hoisted handlers — the D-02a tutorial entry point. */
    private fun renderSettings(
        onShowHowToPlay: () -> Unit = {},
        onToggleSound: (Boolean) -> Unit = {},
    ) = render {
        SettingsScreen(
            state = SettingsUiState(),
            onToggleSound = onToggleSound,
            onToggleHaptics = {},
            onToggleColorblindPalette = {},
            onShowHowToPlay = onShowHowToPlay,
            onSignInToPlayGames = {},
            onShowLeaderboards = {},
            onShowAchievements = {},
            onPlayGamesSignInErrorShown = {},
            onManagePrivacyChoices = {},
            onBack = {},
        )
    }

    private fun renderCatalog(
        onSelect: (String) -> Unit = {},
        onNavigateBack: () -> Unit = {},
    ) = render {
        ClassicCatalogScreen(
            state = CATALOG_STATE,
            onSelect = onSelect,
            onNavigateBack = onNavigateBack,
        )
    }

    private fun renderResults(
        onReplay: (String) -> Unit = {},
        onCatalog: () -> Unit = {},
        onHome: () -> Unit = {},
        onNavigateBack: () -> Unit = {},
    ) = render {
        ResultsScreen(
            state = RESULTS_STATE,
            onReplay = onReplay,
            onCatalog = onCatalog,
            onHome = onHome,
            onNextLevel = {},
            onRetry = {},
            onNavigateBack = onNavigateBack,
        )
    }

    private fun renderRestartDialog(
        onConfirmRestart: () -> Unit = {},
        onDismissRestart: () -> Unit = {},
    ) = renderGame(
        state = GameUiState.initial().copy(confirmRestart = true),
        onConfirmRestart = onConfirmRestart,
        onDismissRestart = onDismissRestart,
    )

    private fun renderGame(
        state: GameUiState = GameUiState.initial(),
        onConfirmRestart: () -> Unit = {},
        onDismissRestart: () -> Unit = {},
        onToggleHints: () -> Unit = {},
        onNavigateBack: () -> Unit = {},
    ) = render {
        GameScreen(
            state = state,
            onCellTap = {},
            onRestart = {},
            onConfirmRestart = onConfirmRestart,
            onDismissRestart = onDismissRestart,
            onAnimationFinished = {},
            onToggleHints = onToggleHints,
            onUndo = {},
            onHint = {},
            onAcceptAdOffer = {},
            onDismissAdOffer = {},
            onHome = {},
            onNavigateBack = onNavigateBack,
        )
    }

    private fun render(screen: @Composable () -> Unit) {
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalSoundPlayer provides sounds) {
                    screen()
                }
            }
        }
        composeRule.waitForIdle()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val BUTTON_SOUND_ROBOLECTRIC_SDK = 34

/** A persisted level past the on-ramp, so a hardcoded 1 cannot pass by accident. */
private const val PERSISTED_LEVEL = 7

/** The single catalog card's rendered name — distinctive so no other node matches it. */
private const val CATALOG_BOARD_NAME = "Sound Test Board"

/** A one-card catalog: enough to prove the card's own cue without grid ambiguity. */
private val CATALOG_STATE =
    ClassicCatalogUiState(
        boards =
            listOf(
                ClassicBoardUiState(
                    descriptor =
                        ClassicLevelDescriptor(
                            id = ClassicLevelIds.ENGLISH33,
                            displayName = CATALOG_BOARD_NAME,
                            holeCount = SAMPLE_HOLE_COUNT,
                            previewBoard = Boards.english33Initial,
                        ),
                ),
            ),
    )

/** The English-33 footprint's hole count — the card's subtitle, never asserted on. */
private const val SAMPLE_HOLE_COUNT = 33

/** A Classic 2-star result: the Replay / Catalog / Home trio, all three forward CTAs. */
private val RESULTS_STATE =
    ResultsUiState(
        stars = SAMPLE_STARS,
        pegsRemaining = SAMPLE_PEGS_REMAINING,
        elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
        score = SAMPLE_SCORE,
        headlineTier = ResultsHeadlineTier.SOLVED,
        replayId = ClassicLevelIds.ENGLISH33,
    )

private const val SAMPLE_STARS = 2
private const val SAMPLE_PEGS_REMAINING = 5
private const val SAMPLE_ELAPSED_SECONDS = 75
private const val SAMPLE_SCORE = 1280
