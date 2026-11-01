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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.navigation.NavHostController
import com.app.pegsolitaire.navigation.AppNavHost
import com.app.pegsolitaire.navigation.Results
import com.app.pegsolitaire.ui.backdrop.BACKDROP_TEST_TAG
import com.app.pegsolitaire.ui.backdrop.BackdropClock
import com.app.pegsolitaire.ui.backdrop.BackdropDensity
import com.app.pegsolitaire.ui.backdrop.BackdropFrames
import com.app.pegsolitaire.ui.backdrop.BackdropMotion
import com.app.pegsolitaire.ui.backdrop.BackdropSceneAlphaKey
import com.app.pegsolitaire.ui.backdrop.FixedBackdropClock
import com.app.pegsolitaire.ui.backdrop.LocalBackdropClock
import com.app.pegsolitaire.ui.backdrop.LocalBackdropSceneReady
import com.app.pegsolitaire.ui.backdrop.SceneBackdrop
import kotlin.math.ceil

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

/**
 * Hands the host's clock to [onClock] once composed, so a test reads its time between frames
 * outside composition: the clock's own snapshot state, never a recomposition per frame.
 */
@Composable
internal fun BackdropClockProbe(onClock: (BackdropClock) -> Unit) {
    val clock = LocalBackdropClock.current
    SideEffect { onClock(clock) }
}

/** The backdrop root's semantics: its tag, density, layers and motion. */
internal fun SemanticsNodeInteractionsProvider.backdropSemantics(): SemanticsConfiguration =
    onNode(hasTestTag(BACKDROP_TEST_TAG), useUnmergedTree = true).fetchSemanticsNode().config

/**
 * Every scene node's alpha, read through the reader its layer draws with (BD-23 (a), the tech-lead's
 * J-4b A3): one entry per density slot that shows a scene, none while no scene is composed.
 */
internal fun SemanticsNodeInteractionsProvider.sceneAlphas(): List<Float> =
    onAllNodes(SemanticsMatcher.keyIsDefined(BackdropSceneAlphaKey), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { it.config[BackdropSceneAlphaKey]() }

/** Steps the test clock one frame and returns that frame's length in ms, the frame time a test counts in. */
internal fun MainTestClock.advanceOneFrame(): Long {
    val before = currentTime
    advanceTimeByFrame()
    return currentTime - before
}

/**
 * Frames of [frameMs] from the gate's first awaited frame (the host's second) to the scene's open at
 * the animator [scale]: the first frame at or past `BACKDROP_SCENE_OPEN_MS` of scaled time (B-11b).
 */
internal fun framesToSceneOpen(
    frameMs: Long,
    scale: Float = SystemMotionScale.ON,
): Int = ceil(BackdropMotion.BACKDROP_SCENE_OPEN_MS * scale / frameMs).toInt()

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
    const val SETTINGS_LICENSES = "standin_settings_licenses"
    const val TUTORIAL_EXIT = "standin_tutorial_exit"
    const val LICENSES_ENTRY = "standin_licenses_entry"
    const val LICENSES_GOOGLE_NOTICES = "standin_licenses_google_notices"
    const val LICENSE_TEXT_BACK = "standin_license_text_back"
    const val GOOGLE_NOTICE = "standin_google_notice"
    const val GOOGLE_NOTICE_TEXT_BACK = "standin_google_notice_text_back"
}

/**
 * The real `AppNavHost` with transparent stand-in destinations, in `NavFlowTest`'s idiom: every
 * stand-in is a tappable, undrawn region that fires one navigation callback, so the host's real
 * lambdas run and nothing a stand-in draws covers the backdrop. [onBoardTap] records the board
 * region's taps. [home], when given, is composed as the Home destination instead of its three
 * stand-ins, inside the host's providers, so it can read the host's live clock or be the real Home.
 */
@Composable
internal fun AppNavHostWithStandIns(
    navController: NavHostController,
    frames: BackdropFrames,
    onBoardTap: () -> Unit = {},
    home: (@Composable () -> Unit)? = null,
) {
    AppNavHost(
        navController = navController,
        homeContent = { onPlayClassic, onPlayChallenge, _, _, onOpenSettings, _ ->
            if (home != null) {
                home()
            } else {
                Column(Modifier.fillMaxSize()) {
                    StandIn(StandInTags.HOME_CATALOG, Modifier.weight(1f), onPlayClassic)
                    StandIn(StandInTags.HOME_CHALLENGE, Modifier.weight(1f)) { onPlayChallenge(STAND_IN_LEVEL) }
                    StandIn(StandInTags.HOME_SETTINGS, Modifier.weight(1f), onOpenSettings)
                }
            }
        },
        catalogContent = { onSelect, _ ->
            StandIn(StandInTags.CATALOG, Modifier.fillMaxSize()) { onSelect(STAND_IN_BOARD) }
        },
        gameplayContent = { onFinished, _ ->
            Column(Modifier.fillMaxSize()) {
                StandIn(StandInTags.BOARD, Modifier.weight(1f), onBoardTap)
                StandIn(StandInTags.BOARD_FINISH, Modifier.weight(1f)) { onFinished(STAND_IN_RESULTS) }
            }
        },
        resultsContent = { _, _, onHome, _, _, _ ->
            StandIn(StandInTags.RESULTS_HOME, Modifier.fillMaxSize(), onClick = onHome)
        },
        settingsContent = { _, onShowHowToPlay, onOpenLicenses ->
            Column(Modifier.fillMaxSize()) {
                StandIn(StandInTags.SETTINGS_TUTORIAL, Modifier.weight(1f), onShowHowToPlay)
                StandIn(StandInTags.SETTINGS_LICENSES, Modifier.weight(1f), onOpenLicenses)
            }
        },
        tutorialContent = { onExit -> StandIn(StandInTags.TUTORIAL_EXIT, Modifier.fillMaxSize(), onClick = onExit) },
        licensesContent = { onOpenEntry, onOpenGoogleNotices, _ ->
            Column(Modifier.fillMaxSize()) {
                StandIn(StandInTags.LICENSES_ENTRY, Modifier.weight(1f)) { onOpenEntry(STAND_IN_LICENCE_ENTRY) }
                StandIn(StandInTags.LICENSES_GOOGLE_NOTICES, Modifier.weight(1f), onOpenGoogleNotices)
            }
        },
        licenseTextContent = { onBack ->
            StandIn(StandInTags.LICENSE_TEXT_BACK, Modifier.fillMaxSize(), onClick = onBack)
        },
        googleNoticesContent = { onOpenNotice, _ ->
            StandIn(StandInTags.GOOGLE_NOTICE, Modifier.fillMaxSize()) { onOpenNotice(STAND_IN_NOTICE_KEY) }
        },
        googleNoticeTextContent = { onBack ->
            StandIn(StandInTags.GOOGLE_NOTICE_TEXT_BACK, Modifier.fillMaxSize(), onClick = onBack)
        },
        backdropFrames = frames,
    )
}

@Composable
private fun StandIn(
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(modifier.fillMaxWidth().testTag(tag).clickable(onClick = onClick))
}

/** A pose with the waves part-way through their swing. */
internal const val TEST_POSE_MS = 4_000L

private const val STAND_IN_LEVEL = 3
private const val STAND_IN_BOARD = "english33"
private const val STAND_IN_LICENCE_ENTRY = "stand_in_licence_entry"
private const val STAND_IN_NOTICE_KEY = "stand_in_notice_key"
private val STAND_IN_RESULTS =
    Results(levelId = STAND_IN_BOARD, stars = 1, pegsRemaining = 3, elapsedSeconds = 60, score = 10)
