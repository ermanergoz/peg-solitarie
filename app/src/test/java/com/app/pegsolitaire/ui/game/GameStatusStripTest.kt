package com.app.pegsolitaire.ui.game

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.testing.shippedFaceFile
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.withTabularFigures
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * The status strip, re-sized, and each system bar applied once (ux-spec §4.3.1, D-05d, criteria
 * 18a to 18e; 18d is also a device check, which governs).
 *
 * The founder's note was that "elapsed time and the title seem too small", and the ruling has two
 * halves that pay for each other: the title and the timer grow, and the space they need comes back
 * from an inset the screen was applying twice — once in the `Scaffold`'s content padding and again
 * in the strip and the dock.
 *
 * ## Why NATIVE graphics
 *
 * An ellipsis is only a fact if the text was measured. Robolectric's default LEGACY mode measures
 * every character one dp wide — "Peg Solitaire" at 22sp comes out 13dp across, against 126dp under
 * NATIVE — so a no-ellipsis assertion under LEGACY could not fail. NATIVE runs the real text stack
 * (with the platform font, since the downloadable Fredoka never resolves off-device).
 *
 * ## Why the insets are supplied
 *
 * §13 forbids measuring a short-window property on a zero-inset window, and 18b *is* the insets:
 * with none supplied, applying them once and applying them twice are the same zero, and the test
 * could not tell the defect from the fix.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [STRIP_ROBOLECTRIC_SDK], qualifiers = DESIGN_BASELINE_QUALIFIER)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameStatusStripTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val fontFolder = TemporaryFolder()

    private val challengeLevel = mutableStateOf<Int?>(null)

    /** The slab colour of the theme the screen was last drawn in. */
    private var slabColour = Color.Unspecified

    /**
     * Criterion 18a, the type roles — read from the rendered text layout, not assumed from the
     * code: the title is `titleLarge` at 22sp, the timer `headlineSmall` at 24sp with tabular
     * figures requested, and the timer's pill is at least its 40dp minimum.
     *
     * The timer is set one character to a cell ([TimerDigits]), so its rendered text layouts are
     * its cells'. Every cell is read, and together they must spell the clock the timer's node
     * carries, so no cell can be missed.
     */
    @Test
    fun theTitleIsTwentyTwoSpAndTheTimerTwentyFourSpWithTabularFiguresInAFortyDpPill() {
        renderGame()

        assertThat(layoutOf(title()).layoutInput.style.fontSize).isEqualTo(TITLE_SP.sp)
        val cells = timerCellLayouts()
        assertThat(cells.joinToString(separator = "") { it.layoutInput.text.text }).isEqualTo(timerText())
        cells.forEach { cell ->
            assertThat(cell.layoutInput.style.fontSize).isEqualTo(TIMER_SP.sp)
            assertThat(cell.layoutInput.style.fontFeatureSettings).isEqualTo(TABULAR_FIGURES)
        }
        assertThat(heightOf(composeRule.onNodeWithTag(GAME_TIMER_PILL_TEST_TAG))).isAtLeast(PILL_MIN_DP.dp)
    }

    /**
     * Criterion 18a, the fit: on a 360dp strip at default scale both titles the strip can show —
     * the app name for Classic and a four-digit Challenge level — render whole, with no ellipsis.
     */
    @Test
    fun bothTitlesRenderWholeOnA360dpStripAtDefaultScale() {
        renderGame()
        assertNotEllipsised(layoutOf(title()), "the Classic title")

        setChallengeLevel(FOUR_DIGIT_LEVEL)
        assertNotEllipsised(layoutOf(title()), "the Challenge title")
    }

    /**
     * Criterion 18b — each system bar once. With a 24dp status bar and a 48dp navigation bar
     * supplied, the strip starts where the status bar ends, and the dock slab ends one
     * `spacing_md` margin above the navigation bar: 640 − 48 − 16 = 576dp. Applied twice, the strip
     * starts at 48dp and the dock ends at 528dp.
     *
     * The tagged containers are read, and so is what they draw. A second inset applied *inside* a
     * tagged container pushes the back chip down and pulls the slab's paint up, yet leaves the
     * container's own bounds exactly where they were — so the tags alone cannot tell once from
     * twice. The back chip's top edge must be within 8dp of the status bar's bottom edge (18b's
     * device clause, read here with the insets supplied), and the slab must be painted down to
     * 576dp.
     */
    @Test
    fun eachSystemBarIsAppliedToTheGameplayScreenOnce() {
        renderGame()
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)

        val strip = composeRule.onNodeWithTag(GAME_STATUS_STRIP_TEST_TAG).getBoundsInRoot()
        val dock = composeRule.onNodeWithTag(GAME_ACTION_DOCK_TEST_TAG).getBoundsInRoot()
        assertThat(strip.top.value).isWithin(EDGE_TOLERANCE_DP).of(STATUS_BAR_DP)
        assertThat(dock.bottom.value).isWithin(EDGE_TOLERANCE_DP).of(DOCK_BOTTOM_DP)

        val backChip = composeRule.onNodeWithContentDescription(string(R.string.navigate_back)).getBoundsInRoot()
        assertWithMessage("the back chip's top edge, in dp").that(backChip.top.value).isAtLeast(STATUS_BAR_DP)
        assertWithMessage("the back chip's top edge, in dp")
            .that(backChip.top.value)
            .isAtMost(STATUS_BAR_DP + BACK_CHIP_MAX_GAP_DP)
        assertWithMessage("where the dock slab's paint ends, in dp")
            .that(dockSlabPaintBottomDp())
            .isWithin(EDGE_TOLERANCE_DP)
            .of(DOCK_BOTTOM_DP)
    }

    /**
     * Criterion 18c at default scale — the board keeps its full width: its slab's edge is the
     * window less two `board_screen_margin`s, 360 − 2 × 16 = 328dp. With the insets applied twice
     * the board region is 72dp shorter and the slab is height-limited to 304dp.
     */
    @Test
    fun theBoardSlabKeepsItsFullWidthAtDefaultFontScale() {
        renderGame()
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)

        assertSlabIsFullWidth()
    }

    /** Criterion 18c at 200% — the larger title and timer still cost the board nothing. */
    @Test
    fun theBoardSlabKeepsItsFullWidthAtDoubleFontScale() {
        renderGame(fontScale = DOUBLE_FONT_SCALE)
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)

        assertSlabIsFullWidth()
    }

    /**
     * Criterion 18d, on the JVM, in the shipped face: the timer pill keeps one width as the clock
     * ticks. At 00:00, 08:08, 11:11 and 59:59 it is the same width to within ±0.5dp.
     *
     * The shipped face is Fredoka Medium. It is a downloadable Google Font that never resolves
     * under Robolectric, and Robolectric's fallback face has tabular digits of its own, so on the
     * fallback this would pass whatever the timer does. That is how a proportional clock reached
     * a device. Here the timer's role is set in the Fredoka binary that Google Fonts serves (a
     * test-only copy, `fixtures/fonts/Fredoka-Variable.ttf`, OFL). Its digits are proportional
     * ("1" is 388 units wide, "2" is 587), and it has no `tnum` feature for `withTabularFigures()`
     * to switch on. The first assertion proves the digits really are proportional in this render,
     * so the second cannot pass on a fallback.
     */
    @Test
    fun theTimerPillKeepsOneWidthAsItTicksInTheShippedFace() {
        val shippedFace = fredokaMedium()
        val elapsed = mutableStateOf(JITTER_READINGS_SECONDS.first())
        lateinit var timerStyle: TextStyle
        lateinit var measurer: TextMeasurer
        composeRule.setContent {
            AppTheme {
                val appTypography = MaterialTheme.typography
                MaterialTheme(
                    typography =
                        appTypography.copy(headlineSmall = appTypography.headlineSmall.copy(fontFamily = shippedFace)),
                ) {
                    timerStyle = MaterialTheme.typography.headlineSmall.withTabularFigures()
                    measurer = rememberTextMeasurer()
                    GameStatusStrip(elapsedSeconds = elapsed.value, challengeLevel = null, onNavigateBack = {})
                }
            }
        }
        composeRule.waitForIdle()

        val setWidths =
            JITTER_READINGS_SECONDS.map { seconds ->
                measurer.measure(GameTimerFormat.format(seconds), timerStyle).size.width
            }
        assertWithMessage("how far apart the four readings are set in the face under test, in dp")
            .that(with(composeRule.density) { (setWidths.max() - setWidths.min()).toDp().value })
            .isGreaterThan(JITTER_TOLERANCE_DP)

        val pillWidths =
            JITTER_READINGS_SECONDS.map { seconds ->
                composeRule.runOnUiThread { elapsed.value = seconds }
                composeRule.waitForIdle()
                val pill = composeRule.onNodeWithTag(GAME_TIMER_PILL_TEST_TAG).getBoundsInRoot()
                (pill.right - pill.left).value
            }
        pillWidths.forEachIndexed { index, width ->
            assertWithMessage("the pill at ${GameTimerFormat.format(JITTER_READINGS_SECONDS[index])}, in dp")
                .that(width)
                .isWithin(JITTER_TOLERANCE_DP)
                .of(pillWidths.first())
        }
    }

    /**
     * Criterion 18e — at 200% the timer wins. The timer's text is whole — every cell of it, not
     * ellipsised, not overflowing — inside a pill that has grown past its 40dp minimum to hold it;
     * only the title may give way; and TalkBack still reads the title's full text, because the
     * ellipsis is drawn and never spoken.
     */
    @Test
    fun atDoubleFontScaleTheTimerIsWholeInAGrownPillAndTheTitleStillSpeaksInFull() {
        renderGame(fontScale = DOUBLE_FONT_SCALE)

        val cells = timerCellLayouts()
        assertThat(cells.joinToString(separator = "") { it.layoutInput.text.text }).isEqualTo(timerText())
        cells.forEach(::assertTimerWhole)
        val pill = composeRule.onNodeWithTag(GAME_TIMER_PILL_TEST_TAG).getBoundsInRoot()
        val digits = timer().getBoundsInRoot()
        assertThat(pill.bottom - pill.top).isGreaterThan(PILL_MIN_DP.dp)
        assertThat(digits.top).isAtLeast(pill.top)
        assertThat(digits.bottom).isAtMost(pill.bottom)
        assertThat(
            title()
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Text)
                ?.map { it.text },
        ).containsExactly(string(R.string.app_name))

        setChallengeLevel(FOUR_DIGIT_LEVEL)
        val challengeName =
            RuntimeEnvironment.getApplication().getString(R.string.challenge_level_indicator_desc, FOUR_DIGIT_LEVEL)
        assertThat(challengeTitle().fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription))
            .containsExactly(challengeName)
    }

    // ---- Harness ---------------------------------------------------------------

    private fun renderGame(fontScale: Float = DEFAULT_FONT_SCALE) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = LocalDensity.current.density, fontScale = fontScale),
            ) {
                AppTheme {
                    slabColour = MaterialTheme.colorScheme.surfaceContainerHigh
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
                        challengeLevel = challengeLevel.value,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** The shipped face, Fredoka, at the Medium weight the timer's role asks for. */
    private fun fredokaMedium(): FontFamily =
        FontFamily(Font(file = fontFolder.shippedFaceFile(), weight = FontWeight.Medium))

    private fun setChallengeLevel(level: Int) {
        composeRule.runOnUiThread { challengeLevel.value = level }
        composeRule.waitForIdle()
    }

    /** The strip's title as rendered — the app name for Classic, "Level N" for Challenge. */
    private fun title(): SemanticsNodeInteraction =
        if (challengeLevel.value == null) {
            composeRule.onNode(hasText(string(R.string.app_name)), useUnmergedTree = true)
        } else {
            challengeTitle()
        }

    private fun challengeTitle(): SemanticsNodeInteraction =
        composeRule.onNode(
            hasText(
                RuntimeEnvironment.getApplication().getString(R.string.challenge_level_indicator, challengeLevel.value),
            ),
            useUnmergedTree = true,
        )

    /** The timer's own node, by its unchanged `elapsed_time` description (`GameFlowTest.kt:129`). */
    private fun timer(): SemanticsNodeInteraction =
        composeRule.onNodeWithContentDescription(string(R.string.elapsed_time), useUnmergedTree = true)

    /** The clock the timer's node carries as its text, e.g. "00:00". */
    private fun timerText(): String =
        timer()
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .joinToString(separator = "") { it.text }

    /** The text layouts of the timer's cells, one per character of the clock, in order. */
    private fun timerCellLayouts(): List<TextLayoutResult> = timer().fetchSemanticsNode().children.map(::layoutOf)

    private fun layoutOf(node: SemanticsNodeInteraction): TextLayoutResult = layoutOf(node.fetchSemanticsNode())

    private fun layoutOf(node: SemanticsNode): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.single()
    }

    /**
     * A title renders whole: its one line is not cut short with an ellipsis, and it needed no more
     * lines than the one it may have. The title is weighted, so its constraints are exact and the
     * layout read back is the one on screen.
     */
    private fun assertNotEllipsised(
        layout: TextLayoutResult,
        what: String,
    ) {
        assertWithMessage("$what is ellipsised").that(layout.isLineEllipsized(0)).isFalse()
        assertWithMessage("$what needs more lines than it may have")
            .that(layout.multiParagraph.didExceedMaxLines)
            .isFalse()
    }

    /**
     * The timer renders whole: one line, its full height inside the height it was given, and its
     * line no wider than the width it was given.
     *
     * `hasVisualOverflow` cannot judge this, and the reason is Compose's, not the timer's: for a
     * wrap-content `Text`, the result `GetTextLayoutResult` hands back is laid out again at its
     * constraints' **maximum** width, so its width always reads as overflowing whenever the digits
     * are narrower than the room they had. The height half is sound — the old fixed 32dp pill cut
     * 48dp of digits to 32 and read `didOverflowHeight` — and the width half is asked directly.
     */
    private fun assertTimerWhole(layout: TextLayoutResult) {
        assertWithMessage("the timer wraps").that(layout.lineCount).isEqualTo(1)
        assertWithMessage("the timer is ellipsised").that(layout.isLineEllipsized(0)).isFalse()
        assertWithMessage("the timer is cut short vertically").that(layout.didOverflowHeight).isFalse()
        assertWithMessage("the timer is cut short horizontally")
            .that(layout.getLineRight(0))
            .isAtMost(layout.size.width.toFloat())
    }

    /**
     * Where the dock slab's paint ends, in dp: down the column between the dock's first two
     * controls, from their centre line, the first row no longer painted in the slab's colour. Read
     * from the flat render, where the slab's edge is crisp and no elevation shadow blurs it into the
     * background below — which is only a few levels away from the slab to begin with.
     */
    private fun dockSlabPaintBottomDp(): Float {
        val controls =
            composeRule
                .onNodeWithTag(GAME_ACTION_DOCK_TEST_TAG)
                .onChildren()
                .fetchSemanticsNodes()
                .map { it.boundsInRoot }
                .sortedBy { it.left }
        val column = ((controls[0].right + controls[1].left) / 2).toInt()
        val window = composeRule.renderedWindow()
        val slab = slabColour.toArgb()
        var row = controls[0].center.y.toInt()
        while (row < window.height && isPaintedIn(window.getPixel(column, row), slab)) row++
        return with(composeRule.density) { row.toDp().value }
    }

    /** Whether [pixel] is [colour] to within one level a channel: a flat region renders exact. */
    private fun isPaintedIn(
        pixel: Int,
        colour: Int,
    ): Boolean =
        COLOUR_CHANNELS.all { shift ->
            abs((pixel shr shift and CHANNEL) - (colour shr shift and CHANNEL)) <= PAINT_TOLERANCE
        }

    private fun assertSlabIsFullWidth() {
        val slab = composeRule.onNodeWithTag(GAME_BOARD_SLAB_TEST_TAG).getBoundsInRoot()
        assertThat((slab.right - slab.left).value).isWithin(EDGE_TOLERANCE_DP).of(FULL_SLAB_DP)
        assertThat((slab.bottom - slab.top).value).isWithin(EDGE_TOLERANCE_DP).of(FULL_SLAB_DP)
    }

    private fun heightOf(node: SemanticsNodeInteraction): Dp {
        val bounds = node.getBoundsInRoot()
        return bounds.bottom - bounds.top
    }

    private fun string(
        @StringRes id: Int,
    ): String = RuntimeEnvironment.getApplication().getString(id)
}

/** §4.3.1's design baseline: a 360x640dp phone. */
private const val DESIGN_BASELINE_QUALIFIER = "w360dp-h640dp"

/** The baseline's system bars: a 24dp status bar and a 48dp three-button navigation bar. */
private const val STATUS_BAR_DP = 24f
private const val NAVIGATION_BAR_DP = 48f

/** 640 − the 48dp navigation bar − the dock's 16dp margin (criterion 18b). */
private const val DOCK_BOTTOM_DP = 576f

/** 360 − 2 × the 16dp `board_screen_margin` (criterion 18c). */
private const val FULL_SLAB_DP = 328f

/** Criterion 18b's ±1dp. */
private const val EDGE_TOLERANCE_DP = 1f

/** Criterion 18b's device clause: the back chip's top edge within 8dp of the status bar's bottom edge. */
private const val BACK_CHIP_MAX_GAP_DP = 8f

/** Blue, green and red, by bit offset in an ARGB int. */
private val COLOUR_CHANNELS = listOf(0, 8, 16)
private const val CHANNEL = 0xFF

/** Per-channel slack for reading a flat fill, which renders exact: rounding and nothing more. */
private const val PAINT_TOLERANCE = 1

/** §4.3.1: `titleLarge` and `headlineSmall`, and the pill's 40dp minimum. */
private const val TITLE_SP = 22
private const val TIMER_SP = 24
private const val PILL_MIN_DP = 40

/** The OpenType feature `withTabularFigures()` requests. */
private const val TABULAR_FIGURES = "tnum"

/** Criterion 18d's four readings: 00:00, 08:08, 11:11 and 59:59, in seconds. */
private val JITTER_READINGS_SECONDS = listOf(0, 488, 671, 3599)

/** Criterion 18d's ±0.5dp. */
private const val JITTER_TOLERANCE_DP = 0.5f

/** Criterion 18a's widest Challenge title: "Level 1234". */
private const val FOUR_DIGIT_LEVEL = 1234

private const val DEFAULT_FONT_SCALE = 1f
private const val DOUBLE_FONT_SCALE = 2f

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val STRIP_ROBOLECTRIC_SDK = 34
