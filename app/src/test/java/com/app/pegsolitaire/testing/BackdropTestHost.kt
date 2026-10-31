package com.app.pegsolitaire.testing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.navigation.NavHostController
import com.app.pegsolitaire.navigation.AppNavHost
import com.app.pegsolitaire.navigation.Results
import com.app.pegsolitaire.ui.backdrop.BACKDROP_TEST_TAG
import com.app.pegsolitaire.ui.backdrop.BackdropClock
import com.app.pegsolitaire.ui.backdrop.BackdropDensity
import com.app.pegsolitaire.ui.backdrop.BackdropFrames
import com.app.pegsolitaire.ui.backdrop.FixedBackdropClock
import com.app.pegsolitaire.ui.backdrop.LocalBackdropClock
import com.app.pegsolitaire.ui.backdrop.LocalBackdropSceneReady
import com.app.pegsolitaire.ui.backdrop.SceneBackdrop

/**
 * The production backdrop host's shape for tests (the tech-lead's file table): the window-filling
 * `Surface`, [SceneBackdrop] at [density] with the scene-ready flag and the clock under the test's
 * control, and the screen's [content] above it — so a screen suite reads what ships.
 */
@Composable
internal fun BackdropTestHost(
    density: BackdropDensity,
    sceneReady: State<Boolean> = mutableStateOf(true),
    clock: BackdropClock = FixedBackdropClock(TEST_POSE_MS),
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize(),
    ) {
        CompositionLocalProvider(LocalBackdropSceneReady provides sceneReady, LocalBackdropClock provides clock) {
            Box(modifier = Modifier.fillMaxSize()) {
                SceneBackdrop(density = density)
                content()
            }
        }
    }
}

/** Frames from the Compose test clock, counted, so "requests no frames" is an exact assertion. */
internal class CountingFrames : BackdropFrames {
    var awaited: Int = 0
        private set

    override suspend fun awaitFrameNanos(): Long {
        awaited++
        return withFrameNanos { it }
    }
}

/** The backdrop root's semantics: its tag, density, layers and motion. */
internal fun SemanticsNodeInteractionsProvider.backdropSemantics(): SemanticsConfiguration =
    onNode(hasTestTag(BACKDROP_TEST_TAG), useUnmergedTree = true).fetchSemanticsNode().config

/** The test tags of [AppNavHostWithStandIns]' destinations. */
internal object StandInTags {
    const val HOME_CATALOG = "standin_home_catalog"
    const val HOME_CHALLENGE = "standin_home_challenge"
    const val HOME_SETTINGS = "standin_home_settings"
    const val CATALOG = "standin_catalog"
    const val BOARD = "standin_board"
    const val BOARD_FINISH = "standin_board_finish"
    const val RESULTS_HOME = "standin_results_home"
    const val SETTINGS_TUTORIAL = "standin_settings_tutorial"
    const val TUTORIAL_EXIT = "standin_tutorial_exit"
}

/**
 * The real `AppNavHost` with transparent stand-in destinations, in `NavFlowTest`'s idiom: every
 * stand-in is a tappable, undrawn region that fires one navigation callback, so the host's real
 * lambdas run and nothing a stand-in draws covers the backdrop. [onBoardTap] records the board
 * region's taps.
 */
@Composable
internal fun AppNavHostWithStandIns(
    navController: NavHostController,
    frames: BackdropFrames,
    onBoardTap: () -> Unit = {},
) {
    AppNavHost(
        navController = navController,
        homeContent = { onPlayClassic, onPlayChallenge, _, _, onOpenSettings, _ ->
            Column(Modifier.fillMaxSize()) {
                StandIn(StandInTags.HOME_CATALOG, Modifier.weight(1f), onPlayClassic)
                StandIn(StandInTags.HOME_CHALLENGE, Modifier.weight(1f)) { onPlayChallenge(STAND_IN_LEVEL) }
                StandIn(StandInTags.HOME_SETTINGS, Modifier.weight(1f), onOpenSettings)
            }
        },
        catalogContent = { onSelect, _ -> StandIn(StandInTags.CATALOG) { onSelect(STAND_IN_BOARD) } },
        gameplayContent = { onFinished, _ ->
            Column(Modifier.fillMaxSize()) {
                StandIn(StandInTags.BOARD, Modifier.weight(1f), onBoardTap)
                StandIn(StandInTags.BOARD_FINISH, Modifier.weight(1f)) { onFinished(STAND_IN_RESULTS) }
            }
        },
        resultsContent = { _, _, onHome, _, _, _ -> StandIn(StandInTags.RESULTS_HOME, onClick = onHome) },
        settingsContent = { _, onShowHowToPlay -> StandIn(StandInTags.SETTINGS_TUTORIAL, onClick = onShowHowToPlay) },
        tutorialContent = { onExit -> StandIn(StandInTags.TUTORIAL_EXIT, onClick = onExit) },
        backdropFrames = frames,
    )
}

@Composable
private fun StandIn(
    tag: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    onClick: () -> Unit,
) {
    Box(modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick))
}

/** A pose with the waves part-way through their swing. */
internal const val TEST_POSE_MS = 4_000L

private const val STAND_IN_LEVEL = 3
private const val STAND_IN_BOARD = "english33"
private val STAND_IN_RESULTS =
    Results(levelId = STAND_IN_BOARD, stars = 1, pegsRemaining = 3, elapsedSeconds = 60, score = 10)
