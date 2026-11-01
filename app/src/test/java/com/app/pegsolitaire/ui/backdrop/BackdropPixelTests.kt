package com.app.pegsolitaire.ui.backdrop

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.testing.BackdropTestHost
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.game.GAME_BOARD_SLAB_TEST_TAG
import com.app.pegsolitaire.ui.game.GAME_STATE_TEST_TAG
import com.app.pegsolitaire.ui.game.GameScreen
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.home.HOME_RESUME_THUMB_TEST_TAG
import com.app.pegsolitaire.ui.home.HomeScreen
import com.app.pegsolitaire.ui.home.HomeUiState
import com.app.pegsolitaire.ui.home.fixture.MID_GAME_ENGLISH_BOARD
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.app.pegsolitaire.ui.theme.BoardMarkPaletteKey
import com.app.pegsolitaire.ui.theme.DarkColors
import com.app.pegsolitaire.ui.theme.LightColors
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.hypot

/**
 * BD-17 and BD-18 (the tech-lead's D4: two renders compared inside one run, nothing stored): the
 * board slab's interior, inset 2dp from its edge, is pixel-identical with and without the backdrop
 * behind it, mid-game with a selection, a legal target and a hint, in light and dark, and once per
 * board-mark palette (docs/ux/colour-vision.md §3.7: "BD-17 … runs once per palette"); so are the
 * interiors of Home's gel tiles, its Continue chips and the Classic chip's thumbnail; and the slab's
 * interior, like the thumbnail's, is its opaque `surfaceContainerHigh`, with nothing over it.
 * Animations are off, so the selection pulse is still.
 *
 * The thumbnail took the Home stage art's place in this list when D-03p withdrew the stage (the
 * tech-lead's ruling 4 in `.planning/reports/home-continue-live-boards/tech-lead-brief.md`; the backdrop
 * spec's §8.1 pointer): it is the only board left on Home, under Home's own backdrop density and inside
 * the chip whose interior this test already reads. Its slab is a plain `surfaceContainerHigh` square,
 * `shapes.extraSmall`, not a finished container, so what proves no gloss lies over it is the sample of
 * its blocked corner against the token, as the game slab's is proved.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PIXEL_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoardInteriorIdentityTest {
    @get:Rule
    val rule = createComposeRule(effectContext = SystemMotionScale(SystemMotionScale.OFF))

    @Test
    fun theSlabsInteriorIsIdenticalWithAndWithoutTheBackdrop() {
        var withBackdrop by mutableStateOf(true)
        var dark by mutableStateOf(false)
        var palette by mutableStateOf(BoardMarkPalette.Standard)
        rule.setContent {
            AppTheme(darkTheme = dark, boardMarkPalette = palette) {
                Hosted(withBackdrop, BackdropDensity.Quiet) { MidGame() }
            }
        }
        BoardMarkPalette.entries.forEach { marks ->
            palette = marks
            listOf(false, true).forEach { theme ->
                dark = theme
                val case = "palette = $marks, dark = $theme"
                assertWithMessage("the board draws its marks in the palette under test, $case")
                    .that(rule.onNodeWithTag(GAME_STATE_TEST_TAG).fetchSemanticsNode().config[BoardMarkPaletteKey])
                    .isEqualTo(marks)
                val slabCorner = cornerPx(R.dimen.shape_corner_extra_large)
                assertInteriorsMatch(rule, listOf(slab() to slabCorner), "board slab, $case") { withBackdrop = it }
                val window = rule.renderedWindow()
                val fill = (if (theme) DarkColors else LightColors).surfaceContainerHigh.toArgb()
                val slab = slab()
                val corner =
                    window.getPixel(
                        (slab.left + slab.width * BLOCKED_CORNER).toInt(),
                        (slab.top + slab.height * BLOCKED_CORNER).toInt(),
                    )
                assertWithMessage("the slab's interior is its opaque fill, $case").that(corner).isEqualTo(fill)
            }
        }
    }

    @Test
    @Config(qualifiers = BASELINE_WINDOW)
    fun homesGelTilesContinueChipsAndClassicThumbnailAreIdenticalWithAndWithoutTheBackdrop() {
        var withBackdrop by mutableStateOf(true)
        var dark by mutableStateOf(false)
        rule.setContent {
            AppTheme(darkTheme = dark) { Hosted(withBackdrop, BackdropDensity.Landscape) { BusyHome() } }
        }
        listOf(false, true).forEach { theme ->
            dark = theme
            val large = cornerPx(R.dimen.shape_corner_large)
            val regions =
                HOME_HEROES.map { bounds(it) to large } +
                    (thumbnail() to cornerPx(R.dimen.shape_corner_extra_small))
            assertInteriorsMatch(rule, regions, "Home heroes, dark = $theme") { withBackdrop = it }
            val window = rule.renderedWindow()
            val fill = (if (theme) DarkColors else LightColors).surfaceContainerHigh.toArgb()
            val thumbnail = thumbnail()
            val corner =
                window.getPixel(
                    (thumbnail.left + thumbnail.width * BLOCKED_CORNER).toInt(),
                    (thumbnail.top + thumbnail.height * BLOCKED_CORNER).toInt(),
                )
            assertWithMessage("the Classic thumbnail's slab is its opaque fill, dark = $theme")
                .that(corner)
                .isEqualTo(fill)
        }
    }

    private fun slab(): Rect = bounds(hasTestTag(GAME_BOARD_SLAB_TEST_TAG))

    /** The Classic chip's thumbnail slab, from the unmerged tree: the chip merges it into its own node. */
    private fun thumbnail(): Rect =
        rule.onNode(hasTestTag(HOME_RESUME_THUMB_TEST_TAG), useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun cornerPx(id: Int): Float =
        ApplicationProvider.getApplicationContext<Context>().resources.getDimension(id)

    private fun bounds(matcher: SemanticsMatcher): Rect = rule.onNode(matcher).fetchSemanticsNode().boundsInRoot

    private companion object {
        /**
         * A point in the English cross's blocked top-start corner: the slab's fill and nothing else —
         * on the game slab, and on the thumbnail's 56dp slab, whose board sits `spacing_xs` inside it.
         */
        const val BLOCKED_CORNER = 0.12f

        /** The 6.7-inch baseline, where Home draws every hero container whole. */
        const val BASELINE_WINDOW = "w412dp-h915dp"
        val HOME_HEROES =
            listOf(
                hasText("Classic") and hasClickAction(),
                hasText("Challenge") and hasClickAction(),
                hasText("Continue Classic") and hasClickAction(),
            )
    }
}

/** BD-20: the backdrop reads no font size — identical at font scale 1.0 and 2.0 at one clock time. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PIXEL_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackdropFontScaleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theBackdropIsIdenticalAtOneAndTwiceTheFontScale() {
        var scale by mutableStateOf(1f)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = scale)) {
                AppTheme { BackdropTestHost(BackdropDensity.Landscape) {} }
            }
        }
        val normal = rule.renderedWindow()
        scale = 2f
        rule.waitForIdle()
        assertThat(normal.sameAs(rule.renderedWindow())).isTrue()
    }
}

/**
 * BD-22: in right to left the scene is the mirror of left to right at one clock time, under a stated
 * tolerance; and the same comparison fails on a deliberately unmirrored scene, so it can catch one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PIXEL_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackdropMirrorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theSceneInRightToLeftIsTheMirrorOfLeftToRight() {
        var direction by mutableStateOf(LayoutDirection.Ltr)
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                AppTheme { BackdropTestHost(BackdropDensity.Landscape) {} }
            }
        }
        val ltr = rule.renderedWindow()
        direction = LayoutDirection.Rtl
        rule.waitForIdle()
        val rtl = rule.renderedWindow()
        val mirror = worstDifference(mirrored(ltr), rtl)
        assertWithMessage("RTL against mirrored LTR").that(mirror).isAtMost(MIRROR_TOLERANCE)
        val unmirrored = worstDifference(ltr, rtl)
        assertWithMessage("an unmirrored scene is caught").that(unmirrored).isGreaterThan(MIRROR_TOLERANCE)
    }

    private fun mirrored(source: Bitmap): Bitmap =
        Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).also { out ->
            for (y in 0 until source.height) {
                for (x in 0 until source.width) out.setPixel(source.width - 1 - x, y, source.getPixel(x, y))
            }
        }

    private fun worstDifference(
        a: Bitmap,
        b: Bitmap,
    ): Int {
        var worst = 0
        for (y in 0 until a.height) {
            for (x in 0 until a.width) {
                val p = a.getPixel(x, y)
                val q = b.getPixel(x, y)
                for (shift in CHANNEL_SHIFTS) {
                    worst = maxOf(worst, abs(((p shr shift) and BYTE) - ((q shr shift) and BYTE)))
                }
            }
        }
        return worst
    }

    private companion object {
        /** Per-channel slack for anti-aliasing at mirrored sub-pixel edges. */
        const val MIRROR_TOLERANCE = 3
        val CHANNEL_SHIFTS = intArrayOf(16, 8, 0)
        const val BYTE = 0xFF
    }
}

/** BD-09: the status-bar band of every density is the theme's background, pixel for pixel, in light and dark. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PIXEL_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackdropStatusBandTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theStatusBarBandIsTheBackgroundOnEveryDensityInBothThemes() {
        var density by mutableStateOf(BackdropDensity.Landscape)
        var dark by mutableStateOf(false)
        rule.setContent { AppTheme(darkTheme = dark) { BackdropTestHost(density) {} } }
        val band = with(rule.density) { STATUS_BAND.dp.roundToPx() }
        listOf(false, true).forEach { theme ->
            BackdropDensity.entries.forEach { d ->
                dark = theme
                density = d
                rule.waitForIdle()
                val window = rule.renderedWindow()
                val background = (if (theme) DarkColors else LightColors).background.toArgb()
                val strays =
                    (0 until band).sumOf { y ->
                        (0 until window.width).count { x -> window.getPixel(x, y) != background }
                    }
                assertWithMessage("$d, dark = $theme: off-background status-band pixels").that(strays).isEqualTo(0)
            }
        }
    }

    private companion object {
        /** The design baseline's status bar (backdrop spec §3.3). */
        const val STATUS_BAND = 24
    }
}

/** With the backdrop at [density] behind [content], or the plain window-filling surface alone. */
@Composable
private fun Hosted(
    withBackdrop: Boolean,
    density: BackdropDensity,
    content: @Composable () -> Unit,
) {
    if (withBackdrop) {
        BackdropTestHost(density, content = content)
    } else {
        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxSize(),
        ) { Box(Modifier.fillMaxSize()) { content() } }
    }
}

/** English-33 mid-game: a selected peg, its legal target and a hint showing. */
@Composable
private fun MidGame() {
    GameScreen(
        state =
            GameUiState.initial().copy(
                selected = Position(row = 1, col = 3),
                legalTargets = listOf(Position(row = 3, col = 3)),
                hintSource = Position(row = 5, col = 3),
                hintTarget = Position(row = 3, col = 3),
            ),
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

/**
 * Home with the Classic Continue chip holding a saved position partway through an English game, so
 * every hero container, and the thumbnail's board, is on screen.
 */
@Composable
private fun BusyHome() {
    HomeScreen(
        state = HomeUiState(classicResumeLevelId = "english33", classicResumeBoard = MID_GAME_ENGLISH_BOARD),
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
}

/**
 * Renders with the backdrop and without, and asserts each region's interior — its rounded outline
 * (the region's bounds with that corner radius) inset 2dp — is unchanged.
 */
private fun assertInteriorsMatch(
    rule: ComposeContentTestRule,
    regions: List<Pair<Rect, Float>>,
    where: String,
    setBackdrop: (Boolean) -> Unit,
) {
    setBackdrop(true)
    rule.waitForIdle()
    val withBackdrop = rule.renderedWindow()
    setBackdrop(false)
    rule.waitForIdle()
    val without = rule.renderedWindow()
    val inset = with(rule.density) { INTERIOR_INSET.dp.toPx() }
    regions.forEach { (region, corner) ->
        var differing = 0
        for (y in region.top.toInt() until region.bottom.toInt()) {
            for (x in region.left.toInt() until region.right.toInt()) {
                val inside = insideRoundedInset(x + HALF_PIXEL, y + HALF_PIXEL, region, corner, inset)
                if (inside && withBackdrop.getPixel(x, y) != without.getPixel(x, y)) differing++
            }
        }
        assertWithMessage("$where: differing pixels inside $region").that(differing).isEqualTo(0)
    }
}

/** Whether ([x], [y]) lies inside [rect] rounded by [corner] and then inset by [inset] all round. */
private fun insideRoundedInset(
    x: Float,
    y: Float,
    rect: Rect,
    corner: Float,
    inset: Float,
): Boolean {
    val inner = rect.deflate(inset)
    if (!inner.contains(Offset(x, y))) return false
    val r = corner - inset
    val cx = x.coerceIn(rect.left + corner, rect.right - corner)
    val cy = y.coerceIn(rect.top + corner, rect.bottom - corner)
    return hypot(x - cx, y - cy) <= r
}

private const val PIXEL_ROBOLECTRIC_SDK = 34
private const val HALF_PIXEL = 0.5f
private const val INTERIOR_INSET = 2
