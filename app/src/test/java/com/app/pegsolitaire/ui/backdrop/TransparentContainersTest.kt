package com.app.pegsolitaire.ui.backdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.navigation.AppNavHost
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.classic.ClassicCatalogScreen
import com.app.pegsolitaire.ui.classic.ClassicCatalogUiState
import com.app.pegsolitaire.ui.game.GameScreen
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.home.HomeScreen
import com.app.pegsolitaire.ui.home.HomeUiState
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
import com.app.pegsolitaire.ui.theme.DarkColors
import com.app.pegsolitaire.ui.theme.LightColors
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * BD-14 (the tech-lead's D1): every screen's `Scaffold`, the four `TopAppBar`s, the licences
 * screens' shared top bar and the tutorial's root `Surface` paint no colour, so the backdrop shows
 * through them — read as pixels over a sentinel fill no theme colour can equal — and the host's
 * window-filling `Surface` gives text that names no colour `onBackground` in both themes
 * (rules/compose.md, F-32). The two licences screens are E7's rows, owed here because the licences
 * branch landed first (the tech-lead's merge ruling R-3.3).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [CONTAINERS_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TransparentContainersTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun everyScaffoldTopBarAndTheTutorialRootPaintNoColourInLightAndDark() {
        var screen by mutableStateOf(SCREENS.keys.first())
        var dark by mutableStateOf(false)
        rule.setContent {
            AppTheme(darkTheme = dark) {
                Box(Modifier.fillMaxSize()) {
                    Spacer(Modifier.fillMaxSize().background(SENTINEL))
                    SCREENS.getValue(screen)()
                }
            }
        }
        listOf(false, true).forEach { theme ->
            SCREENS.keys.forEach { name ->
                dark = theme
                screen = name
                rule.waitForIdle()
                val pixel = rule.renderedWindow().getPixel(PROBE_PX, PROBE_PX)
                assertWithMessage("$name, dark = $theme").that(pixel).isEqualTo(SENTINEL.toArgb())
            }
        }
    }

    @Test
    fun unlabelledTextInTheHostIsOnBackgroundInBothThemes() {
        val seen = mutableMapOf<Boolean, Color>()
        var dark by mutableStateOf(false)
        rule.setContent {
            AppTheme(darkTheme = dark) {
                val theme = dark
                AppNavHost(
                    navController = rememberNavController(),
                    homeContent = { _, _, _, _, _, _ -> seen[theme] = LocalContentColor.current },
                )
            }
        }
        rule.waitForIdle()
        dark = true
        rule.waitForIdle()
        assertWithMessage("light").that(seen[false]).isEqualTo(LightColors.onBackground)
        assertWithMessage("dark").that(seen[true]).isEqualTo(DarkColors.onBackground)
    }

    private companion object {
        /** A green no theme token is: an opaque container would show its own colour here instead. */
        val SENTINEL = Color(0xFF00FF00)

        /** Inside each screen's outer padding and its top bar, clear of every icon and text. */
        const val PROBE_PX = 2

        val SCREENS: Map<String, @Composable () -> Unit> =
            linkedMapOf(
                "Home" to {
                    HomeScreen(
                        state = HomeUiState(),
                        onPlayClassic = {},
                        onPlayChallenge = {},
                        onResumeClassic = {},
                        onResumeChallenge = {},
                        onAcceptPlayGamesPrompt = {},
                        onDeclinePlayGamesPrompt = {},
                        onKeepDeviceProgress = {},
                        onUseCloudProgress = {},
                        onPlayGamesSignInErrorShown = {},
                        onOpenSettings = {},
                    )
                },
                "the game screen" to {
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
                },
                "the catalogue" to {
                    ClassicCatalogScreen(state = ClassicCatalogUiState.Empty, onSelect = {}, onNavigateBack = {})
                },
                "Results" to {
                    ResultsScreen(
                        state = RESULTS,
                        onReplay = {},
                        onCatalog = {},
                        onHome = {},
                        onNextLevel = {},
                        onRetry = {},
                        onNavigateBack = {},
                    )
                },
                "Settings" to {
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
                },
                "the tutorial" to {
                    TutorialScreen(
                        state = TutorialUiState.initial(),
                        onCellTap = {},
                        onAnimationFinished = {},
                        onSkip = {},
                        onDone = {},
                    )
                },
                "the licences list" to {
                    LicensesScreen(
                        sections = LicenseCatalog.sections,
                        onOpenEntry = {},
                        onBack = {},
                    )
                },
                "a licence text" to {
                    LicenseTextScreen(state = LicenseTextUiState.Failed(header = null), onBack = {})
                },
            )

        val RESULTS =
            ResultsUiState(
                stars = 2,
                pegsRemaining = 1,
                elapsedSeconds = 90,
                score = 1_200,
                headlineTier = ResultsHeadlineTier.SOLVED,
                replayId = "english33",
            )
    }
}

private const val CONTAINERS_ROBOLECTRIC_SDK = 34
