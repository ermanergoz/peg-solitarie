package com.app.pegsolitaire.ui.home.fixture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.ui.home.HOME_PROGRESS_RIBBON_TEST_TAG
import com.app.pegsolitaire.ui.home.HomeScreen
import com.app.pegsolitaire.ui.home.HomeUiState
import com.app.pegsolitaire.ui.theme.AppTheme
import org.robolectric.RuntimeEnvironment

/*
 * The fixture `HomeScreenTest` and `HomeLayoutTest` share: one way to render Home (in the app's own
 * light theme, so a suite under NATIVE graphics measures the bundled faces), one definition of its
 * busiest legitimate state, and one set of accessors for the progress ribbon.
 *
 * It exists because the two classes must agree. `HomeLayoutTest` measures the same screen in the
 * same state that `HomeScreenTest` interrogates, and criterion 15m's whole claim is that the
 * compact pill and the full pill speak BYTE-IDENTICAL sentences — a claim that means nothing if
 * the two classes build the expected string two different ways. One `announcement()`, one
 * `bothResumesState()`, one `renderHome()`.
 *
 * It lives in its own package rather than beside the two classes because the `ui.home` test
 * package already carries file-private `PERSISTED_LEVEL` and `RESUMABLE_LEVEL_ID` constants of
 * `HomeViewModelTest`'s own, and a shared `internal` copy of either name in that package makes
 * both ambiguous inside that file. Explicit imports are the better outcome anyway: a reader of
 * `HomeLayoutTest` can see exactly which shared oracle criterion 15m's byte-identity claim rests
 * on.
 */

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
internal const val HOME_ROBOLECTRIC_SDK = 34

/** A persisted level past the on-ramp, so a hardcoded 1 cannot pass by accident. */
internal const val PERSISTED_LEVEL = 7

/** The rendered tile copy for [PERSISTED_LEVEL] (verbatim from `strings.xml`). */
internal const val CONTINUE_LABEL = "Continue — Level 7"

/** The Classic board a saved game sits on — carried into the resume by the tile. */
internal val RESUMABLE_LEVEL_ID = ClassicLevelIds.EUROPEAN37

/** The rendered Continue-affordance titles (verbatim from `strings.xml`). */
internal const val RESUME_CLASSIC_LABEL = "Continue Classic"
internal const val RESUME_CHALLENGE_LABEL = "Continue Challenge"

/** The snapshot's stamped level — deliberately NOT the ladder level (WR-03). */
internal const val STAMPED_RESUME_LEVEL = 9

/** Ribbon pill positions, left to right (§4.2 band 2). */
internal const val STARS_PILL = 0
internal const val BOARDS_PILL = 1
internal const val LEVEL_PILL = 2

/** Three pills, and exactly three: the ribbon is not a place to add a fourth thing. */
internal const val RIBBON_PILLS = 3

/** A stars total past zero, so a placeholder cannot pass for a real value. */
internal const val EARNED_STARS = 7

/** Cleared Classic boards — deliberately neither 0 nor the catalogue size. */
internal const val CLEARED_BOARDS = 3

/** The system font-size setting at its maximum (criteria 14, 15i). */
internal const val DOUBLE_FONT_SCALE = 2f

/** The system font-size setting as shipped. */
internal const val DEFAULT_FONT_SCALE = 1f

/** The busiest legitimate Home: a filled ribbon, both Continue chips, both mode tiles. */
internal fun bothResumesState(): HomeUiState = chipsState(classic = true, challenge = true)

/**
 * A filled ribbon and the Continue chips asked for — the four chip states criterion 15p walks
 * (none, Classic only, Challenge only, both), from one builder so they differ in the chips alone.
 */
internal fun chipsState(
    classic: Boolean,
    challenge: Boolean,
): HomeUiState =
    HomeUiState(
        totalStars = EARNED_STARS,
        classicBoardsCleared = CLEARED_BOARDS,
        challengeCurrentLevel = PERSISTED_LEVEL,
        classicResumeLevelId = RESUMABLE_LEVEL_ID.takeIf { classic },
        classicResumeBoard = Boards.european37Initial.takeIf { classic },
        challengeResumeLevel = STAMPED_RESUME_LEVEL.takeIf { challenge },
    )

/** Every chip state, in 15p's order: none, Classic only, Challenge only, both. */
internal val EVERY_CHIP_STATE: List<HomeUiState>
    get() =
        listOf(
            chipsState(classic = false, challenge = false),
            chipsState(classic = true, challenge = false),
            chipsState(classic = false, challenge = true),
            chipsState(classic = true, challenge = true),
        )

/**
 * Home in [state], in the app's own theme.
 *
 * @param darkTheme the scheme Home is drawn in: light unless a test states dark, and never read
 *   from the configuration.
 * @param overlay drawn over Home in the same composition and the same theme, after it, so it lies
 *   on top. Empty by default, so every caller that passes none composes exactly the tree it always
 *   did. `HomeLayoutTest` draws criterion 15m's reference finishes here (B-19a).
 * @param underlay drawn under Home in the same composition and theme, before it. Empty by default,
 *   so every caller that passes none composes exactly the tree it always did. `HomeChipArrivalTest`
 *   draws the flat background Home's opaque `Scaffold` drew before BD-14 here.
 */
internal fun ComposeContentTestRule.renderHome(
    state: HomeUiState,
    onPlayChallenge: (Int) -> Unit = {},
    onResumeClassic: (String) -> Unit = {},
    onResumeChallenge: (Int) -> Unit = {},
    onKeepDeviceProgress: () -> Unit = {},
    onUseCloudProgress: () -> Unit = {},
    fontScale: Float = DEFAULT_FONT_SCALE,
    darkTheme: Boolean = false,
    overlay: @Composable () -> Unit = {},
    underlay: @Composable () -> Unit = {},
) = renderHome(
    state = mutableStateOf(state),
    onPlayChallenge = onPlayChallenge,
    onResumeClassic = onResumeClassic,
    onResumeChallenge = onResumeChallenge,
    onKeepDeviceProgress = onKeepDeviceProgress,
    onUseCloudProgress = onUseCloudProgress,
    fontScale = fontScale,
    darkTheme = darkTheme,
    overlay = overlay,
    underlay = underlay,
)

/**
 * The settable overload, for the tests that need a live recomposition — Home fills the ribbon
 * *after* first paint, so "shows the placeholder, then the value" is only honest when it is
 * asserted across one state change rather than across two compositions. [darkTheme], [overlay]
 * and [underlay] are the other overload's.
 */
internal fun ComposeContentTestRule.renderHome(
    state: MutableState<HomeUiState>,
    onPlayChallenge: (Int) -> Unit = {},
    onResumeClassic: (String) -> Unit = {},
    onResumeChallenge: (Int) -> Unit = {},
    onKeepDeviceProgress: () -> Unit = {},
    onUseCloudProgress: () -> Unit = {},
    fontScale: Float = DEFAULT_FONT_SCALE,
    darkTheme: Boolean = false,
    overlay: @Composable () -> Unit = {},
    underlay: @Composable () -> Unit = {},
) {
    setContent {
        // The font scale is provided rather than set through a resource qualifier because there
        // is no qualifier for it: it is a property of `Density`, which is where every `sp` on the
        // screen reads it from.
        CompositionLocalProvider(
            LocalDensity provides Density(density = LocalDensity.current.density, fontScale = fontScale),
        ) {
            // In the app's own theme, as `MainActivity` draws it: without it every role falls back
            // to Material 3's baseline faces, weights, shapes and colours, so a suite under NATIVE
            // graphics would measure text a player never sees. Light unless a test states dark;
            // never read from the configuration, so no test depends on the host's night mode.
            AppTheme(darkTheme = darkTheme) {
                underlay()
                HomeScreen(
                    state = state.value,
                    onPlayClassic = {},
                    onPlayChallenge = onPlayChallenge,
                    onResumeClassic = onResumeClassic,
                    onResumeChallenge = onResumeChallenge,
                    onAcceptPlayGamesPrompt = {},
                    onDeclinePlayGamesPrompt = {},
                    onKeepDeviceProgress = onKeepDeviceProgress,
                    onUseCloudProgress = onUseCloudProgress,
                    onPlayGamesSignInErrorShown = {},
                    onOpenSettings = {},
                )
                overlay()
            }
        }
    }
    waitForIdle()
}

// ---- Geometry ----------------------------------------------------------------

/**
 * A node's bounds **along the column**: its unclipped position and size, so a band below the fold
 * reports where it is rather than the viewport edge `getBoundsInRoot()` clips it to (§13's first
 * forbidden idiom is containment read off clipped bounds).
 */
internal fun ComposeContentTestRule.columnBoundsOf(node: SemanticsNodeInteraction): DpRect =
    columnBoundsOf(node.fetchSemanticsNode())

/** [columnBoundsOf] for a node already fetched, such as a parent reached through the tree. */
internal fun ComposeContentTestRule.columnBoundsOf(node: SemanticsNode): DpRect {
    val origin = node.positionInRoot
    return with(density) {
        DpRect(
            left = origin.x.toDp(),
            top = origin.y.toDp(),
            right = (origin.x + node.size.width).toDp(),
            bottom = (origin.y + node.size.height).toDp(),
        )
    }
}

// ---- Ribbon accessors --------------------------------------------------------

/**
 * The three pills are the ribbon node's children, located through
 * [HOME_PROGRESS_RIBBON_TEST_TAG] rather than by their copy — the rule §13 sets after the retired
 * overlay was found by its "Got it" label.
 */
internal fun ComposeContentTestRule.pill(index: Int) = ribbon().onChildren()[index]

internal fun ComposeContentTestRule.ribbonPillCount(): Int = ribbon().fetchSemanticsNode().children.size

internal fun ComposeContentTestRule.ribbon() = onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG)

/**
 * Every value string the ribbon actually **renders**, read from the unmerged tree.
 *
 * The pills clear and set one description each, so their child `Text`s are invisible to the merged
 * tree by design — which is the point of the announcement assertions. What a player *sees*
 * therefore has to be read past that, and this is where "—" versus "0" is checked.
 */
internal fun ComposeContentTestRule.renderedValues(): List<String> =
    onNode(hasTestTag(HOME_PROGRESS_RIBBON_TEST_TAG), useUnmergedTree = true)
        .fetchSemanticsNode()
        .textsIn()

private fun SemanticsNode.textsIn(): List<String> =
    config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
        children.flatMap { it.textsIn() }

/** Every `contentDescription` on a pill's node — the whole announcement, and only one. */
internal fun ComposeContentTestRule.pillDescriptions(index: Int): List<String> =
    pill(index)
        .fetchSemanticsNode()
        .config
        .getOrNull(SemanticsProperties.ContentDescription)
        .orEmpty()

/** A pill's click action, which must be absent: the pills are status, not navigation. */
internal fun ComposeContentTestRule.pillClickAction(index: Int) =
    pill(index).fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)

// ---- Strings, read from `strings.xml` rather than retyped --------------------

/** The exact string a pill announces — "Stars, 7" — built from the shipped format. */
internal fun announcement(
    label: String,
    value: String,
): String = string(R.string.home_stat_pill_desc, label, value)

internal fun string(
    id: Int,
    vararg args: Any,
): String = RuntimeEnvironment.getApplication().getString(id, *args)

internal val starsLabel: String get() = string(R.string.home_stat_stars_label)

internal val boardsLabel: String get() = string(R.string.home_stat_boards_label)

internal val levelLabel: String get() = string(R.string.home_stat_level_label)

/**
 * The Boards pill's rendered value for [CLEARED_BOARDS] — built from the catalogue rather than
 * retyped, and the one pill whose label *and* value are unique strings on the screen.
 */
internal val boardsValue: String
    get() = string(R.string.home_stat_boards_value, CLEARED_BOARDS, Boards.CLASSIC_SPECS.size)

internal val classicTileTitle: String get() = string(R.string.classic_tile_title)

internal val challengeTileTitle: String get() = string(R.string.challenge_tile_title)

/** The hero's gradient title — band 1's own content, and the compact hero's whole width. */
internal val appName: String get() = string(R.string.app_name)

/** The gear's unchanged description (D-05b): `ButtonSoundTest` locates it by this string. */
internal val settingsLabel: String get() = string(R.string.settings_action)
