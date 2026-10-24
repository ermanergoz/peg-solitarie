package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.home.fixture.BOARDS_PILL
import com.app.pegsolitaire.ui.home.fixture.DEFAULT_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.DOUBLE_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.LEVEL_PILL
import com.app.pegsolitaire.ui.home.fixture.RIBBON_PILLS
import com.app.pegsolitaire.ui.home.fixture.STARS_PILL
import com.app.pegsolitaire.ui.home.fixture.announcement
import com.app.pegsolitaire.ui.home.fixture.boardsLabel
import com.app.pegsolitaire.ui.home.fixture.columnBoundsOf
import com.app.pegsolitaire.ui.home.fixture.levelLabel
import com.app.pegsolitaire.ui.home.fixture.pillDescriptions
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.starsLabel
import com.app.pegsolitaire.ui.home.fixture.string
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.max

/**
 * The progress pill at a large font scale (ux-spec §4.2.6, D-03o): criterion 15w.
 *
 * At 360dp and font scale 2.0 the compact pills read "Sta/rs", "Bo/ard/s" and "Leve/l": D-03k's
 * shape weighted the label and measured the value first, so the label got what was left and broke
 * wherever it fell. D-03o decides one shape per ribbon from the labels: side by side while every
 * label fits half its pill, stacked — label over value, no glyph — once any one does not, and a
 * label or a value never breaks inside a word.
 *
 * Read on NATIVE graphics in the app theme, so the labels are Nunito and the values Fredoka, with
 * the design baseline's system bars supplied. Texts are read in the unmerged tree, where the pills'
 * one description each does not hide them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK], qualifiers = NARROW_BASELINE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeRibbonShapeTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---- Part 1: no break inside a word or a number, at 1.0, 1.3 and 2.0 -------

    @Test
    fun noLabelOrValueBreaksInsideAWordAtDefaultFontScale() = assertNothingBreaks(DEFAULT_FONT_SCALE)

    @Test
    fun noLabelOrValueBreaksInsideAWordAtOnePointThreeTimesFontScale() = assertNothingBreaks(LARGE_FONT_SCALE)

    @Test
    fun noLabelOrValueBreaksInsideAWordAtDoubleFontScale() = assertNothingBreaks(DOUBLE_FONT_SCALE)

    // ---- Part 2: at 1.0, D-03k's one-line pill and the 56dp band -----------------

    @Test
    fun atDefaultScaleTheNarrowRibbonKeepsItsOneLinePills() {
        render(VALUES, DEFAULT_FONT_SCALE)

        pills().forEachIndexed { index, pill ->
            assertWithMessage("pill $index's shape").that(shapeOf(pill)).isEqualTo(PillShape.OneLine)
        }
        val band = composeRule.columnBoundsOf(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG))
        assertWithMessage("the ribbon band's height at default scale, in dp")
            .that((band.bottom - band.top).value)
            .isAtMost(COMPACT_RIBBON_MAX_HEIGHT_DP)
    }

    // ---- Part 3: at 2.0, all three stacked, at one height and one width --------

    /**
     * Part 3. The widths are compared to within one pixel when that is more than 0.5dp: the
     * ribbon's three equal weights share its whole pixels, so on Robolectric's mdpi window, where a
     * pixel is a dp, one pill can be a pixel wider than the others (328px is 110 + 109 + 109). That
     * is the row's rounding, in every shape and before D-03o alike; on a phone a pixel is under 0.4dp.
     */
    @Test
    fun atDoubleScaleAllThreePillsStackAtOneHeightAndWidth() {
        render(VALUES, DOUBLE_FONT_SCALE)

        val pills = pills()
        pills.forEachIndexed { index, pill ->
            assertWithMessage("pill $index's shape").that(shapeOf(pill)).isEqualTo(PillShape.Stacked)
        }
        val bounds = pills.map(composeRule::columnBoundsOf)
        bounds.forEachIndexed { index, pill ->
            assertWithMessage("pill $index's height against pill 0's, in dp")
                .that(heightOf(pill).value)
                .isWithin(TOLERANCE_DP)
                .of(heightOf(bounds.first()).value)
            assertWithMessage("pill $index's width against pill 0's, in dp")
                .that(widthOf(pill).value)
                .isWithin(max(TOLERANCE_DP, with(composeRule.density) { 1.toDp() }.value))
                .of(widthOf(bounds.first()).value)
        }
    }

    // ---- Part 4: one shape per ribbon, and the progress emission changes none --

    @Test
    fun theProgressEmissionChangesNoPillsShapeOrHeightAtDefaultFontScale() =
        assertTheProgressEmissionChangesNoShape(DEFAULT_FONT_SCALE)

    @Test
    fun theProgressEmissionChangesNoPillsShapeOrHeightAtOnePointThreeTimesFontScale() =
        assertTheProgressEmissionChangesNoShape(LARGE_FONT_SCALE)

    @Test
    fun theProgressEmissionChangesNoPillsShapeOrHeightAtDoubleFontScale() =
        assertTheProgressEmissionChangesNoShape(DOUBLE_FONT_SCALE)

    // ---- Part 5: one sentence per pill in every shape --------------------------

    @Test
    fun everyPillSpeaksTheSameSentenceInTheOneLineShape() = assertEveryPillSpeaksItsSentence(DEFAULT_FONT_SCALE)

    @Test
    fun everyPillSpeaksTheSameSentenceInTheStackedShape() = assertEveryPillSpeaksItsSentence(DOUBLE_FONT_SCALE)

    @Test
    @Config(qualifiers = WIDE_PHONE)
    fun everyPillSpeaksTheSameSentenceInTheFullShape() = assertEveryPillSpeaksItsSentence(DOUBLE_FONT_SCALE)

    // ---- The wide phone: the full pill binds only as "no break" ----------------

    @Test
    @Config(qualifiers = WIDE_PHONE)
    fun onAWidePhoneAtDoubleScaleEveryFullPillLaysOutOnOneLineEach() {
        render(VALUES, DOUBLE_FONT_SCALE)

        assertEveryTextIsOneLineAndWholeInItsPill("the full pills at 2.0 on $WIDE_PHONE")
    }

    // ---- Assertions --------------------------------------------------------------

    /**
     * Part 1 on the current scale: with Stars 401, Boards 0/5 and Level 111; again with Stars and
     * Boards at "—", before the progress emission; and again with a five-digit level, every label
     * and every value lays out on exactly one line, inside its pill.
     */
    private fun assertNothingBreaks(fontScale: Float) {
        val state = render(VALUES, fontScale)
        assertEveryTextIsOneLineAndWholeInItsPill("401 · 0/5 · 111 at $fontScale")

        show(state, PLACEHOLDERS)
        assertEveryTextIsOneLineAndWholeInItsPill("— · — · 111 at $fontScale")

        show(state, FIVE_DIGIT_LEVEL)
        assertEveryTextIsOneLineAndWholeInItsPill("401 · 0/5 · 12345 at $fontScale")
    }

    /**
     * Part 4 on the current scale: the three pills share one shape, and the shape and the height
     * with "—" — before the progress emission — are those with the values after it.
     */
    private fun assertTheProgressEmissionChangesNoShape(fontScale: Float) {
        val state = render(PLACEHOLDERS, fontScale)
        val before = pills().map { shapeOf(it) to heightOf(composeRule.columnBoundsOf(it)) }
        assertWithMessage("the pills' shapes before the progress emission at $fontScale")
            .that(before.map { it.first }.distinct())
            .hasSize(1)

        show(state, VALUES)
        val after = pills().map { shapeOf(it) to heightOf(composeRule.columnBoundsOf(it)) }
        after.forEachIndexed { index, (shape, height) ->
            assertWithMessage("pill $index's shape after the progress emission at $fontScale")
                .that(shape)
                .isEqualTo(before[index].first)
            assertWithMessage("pill $index's height after the progress emission at $fontScale, in dp")
                .that(height.value)
                .isWithin(TOLERANCE_DP)
                .of(before[index].second.value)
        }
    }

    /** Part 5: each pill's one description is the full set's, byte for byte ("Stars, 401"). */
    private fun assertEveryPillSpeaksItsSentence(fontScale: Float) {
        render(VALUES, fontScale)

        assertThat(composeRule.pillDescriptions(STARS_PILL)).containsExactly(announcement(starsLabel, STARS.toString()))
        assertThat(composeRule.pillDescriptions(BOARDS_PILL)).containsExactly(announcement(boardsLabel, boardsValue()))
        assertThat(composeRule.pillDescriptions(LEVEL_PILL)).containsExactly(announcement(levelLabel, LEVEL.toString()))
    }

    /**
     * Every label and every value lays out on exactly one line — so nothing is broken inside a word
     * or a number — and sits wholly inside its pill, so nothing is cut instead.
     */
    private fun assertEveryTextIsOneLineAndWholeInItsPill(situation: String) {
        val pills = pills()
        assertThat(pills).hasSize(RIBBON_PILLS)
        pills.forEachIndexed { index, pill ->
            val pillBounds = composeRule.columnBoundsOf(pill)
            pill.textsWithin().forEach { text ->
                val words = textOf(text)
                assertWithMessage("$situation: pill $index's '$words', in lines")
                    .that(layoutOf(text).lineCount)
                    .isEqualTo(1)
                // A one-line text that does not fit is cut, not broken (`softWrap = false` clips by
                // default), so the line count alone cannot see it (code-auditor loop 1, P13). Read as
                // `GameStatusStripTest` reads its timer: `hasVisualOverflow` is not usable on a text
                // read back through `GetTextLayoutResult`, so the line is held against the width it
                // was laid out in, and the height half is read directly.
                val layout = layoutOf(text)
                assertWithMessage("$situation: pill $index's '$words', its line against its laid-out width")
                    .that(layout.getLineRight(0))
                    .isAtMost(layout.size.width.toFloat())
                assertWithMessage("$situation: pill $index's '$words' cut short vertically")
                    .that(layout.didOverflowHeight)
                    .isFalse()
                val bounds = composeRule.columnBoundsOf(text)
                assertWithMessage("$situation: pill $index's '$words' inside its pill $pillBounds, at $bounds")
                    .that(
                        bounds.left >= pillBounds.left &&
                            bounds.right <= pillBounds.right &&
                            bounds.top >= pillBounds.top &&
                            bounds.bottom <= pillBounds.bottom,
                    ).isTrue()
            }
        }
    }

    // ---- Harness -----------------------------------------------------------------

    private fun render(
        state: HomeUiState,
        fontScale: Float,
    ): MutableState<HomeUiState> {
        val holder = mutableStateOf(state)
        composeRule.renderHome(holder, fontScale = fontScale)
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)
        return holder
    }

    private fun show(
        holder: MutableState<HomeUiState>,
        state: HomeUiState,
    ) {
        composeRule.runOnUiThread { holder.value = state }
        composeRule.waitForIdle()
    }

    /** The three pills, left to right, in the unmerged tree where their texts are children. */
    private fun pills(): List<SemanticsNode> =
        composeRule
            .onNode(hasTestTag(HOME_PROGRESS_RIBBON_TEST_TAG), useUnmergedTree = true)
            .fetchSemanticsNode()
            .children

    /** A pill's shape, read off where its label and its value are. */
    private fun shapeOf(pill: SemanticsNode): PillShape {
        val (label, value) = pill.textsWithin().map(composeRule::columnBoundsOf)
        return when {
            value.top >= label.bottom -> PillShape.Stacked
            value.top < label.bottom && label.top < value.bottom && label.right <= value.left -> PillShape.OneLine
            else -> PillShape.Other
        }
    }

    private fun SemanticsNode.textsWithin(): List<SemanticsNode> =
        children.flatMap { child ->
            listOfNotNull(child.takeIf { SemanticsProperties.Text in it.config }) + child.textsWithin()
        }

    private fun textOf(node: SemanticsNode): String = node.config[SemanticsProperties.Text].joinToString("") { it.text }

    private fun layoutOf(node: SemanticsNode): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private fun heightOf(bounds: DpRect): Dp = bounds.bottom - bounds.top

    private fun widthOf(bounds: DpRect): Dp = bounds.right - bounds.left

    private fun boardsValue(): String = string(R.string.home_stat_boards_value, BOARDS, Boards.CLASSIC_SPECS.size)

    /** Where a pill's label and value sit relative to each other. */
    private enum class PillShape { OneLine, Stacked, Other }
}

/** 15w's window: the narrow baseline, where the compact pill lives. */
private const val NARROW_BASELINE = "w360dp-h640dp"

/** 15w's wide phone, where the full pill lives. */
private const val WIDE_PHONE = "w412dp-h915dp"

/** The largest font scale API 33 and below offer, and one 15w names. */
private const val LARGE_FONT_SCALE = 1.3f

/** 15w's values: Stars 401, Boards 0 of the catalogue, Level 111. */
private const val STARS = 401
private const val BOARDS = 0
private const val LEVEL = 111
private const val FIVE_DIGIT = 12345

private val VALUES = HomeUiState(totalStars = STARS, classicBoardsCleared = BOARDS, challengeCurrentLevel = LEVEL)
private val PLACEHOLDERS = HomeUiState(totalStars = null, classicBoardsCleared = null, challengeCurrentLevel = LEVEL)
private val FIVE_DIGIT_LEVEL = VALUES.copy(challengeCurrentLevel = FIVE_DIGIT)

/** The design baseline's system bars: a 24dp status bar and a 48dp navigation bar. */
private const val STATUS_BAR_DP = 24
private const val NAVIGATION_BAR_DP = 48

/** Criterion 15m: the compact-narrow ribbon band's height at default font scale. */
private const val COMPACT_RIBBON_MAX_HEIGHT_DP = 56f

/** 15w's ±0.5dp. */
private const val TOLERANCE_DP = 0.5f
