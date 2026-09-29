package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.home.fixture.DOUBLE_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.appName
import com.app.pegsolitaire.ui.home.fixture.chipsState
import com.app.pegsolitaire.ui.home.fixture.columnBoundsOf
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.settingsLabel
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Home's hero, re-seated (ux-spec §4.2.4, D-03l): **one row** — the gradient title start-aligned,
 * the Settings gear at the end — on every window, and no logo (criteria 15n and 15o).
 *
 * The founder named the peg-cross logo and "how the title sits" in the same breath, and they had
 * one cause: a centred emblem wants a centred word under it, and without the emblem that word
 * floats between the screen's edge and the ribbon, belonging to neither. Start-aligned in the
 * gear's row it reads as the screen's heading — the geometry the 360dp-wide class has shown since
 * D-03i, now the only hero Home has.
 *
 * Measured with the device's system-bar insets supplied (§13) and under NATIVE graphics, because
 * "neither truncated nor scaled" is only decidable on text that was really measured: LEGACY
 * graphics sets every character one dp wide, and a title that can never be too wide can never be
 * shown not to be.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeHeroRowTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---- Criterion 15n: no logo -------------------------------------------------

    /**
     * On a phone with room for decoration — where the withdrawn peg cluster used to stand over the
     * title — nothing is composed above or beside the title but the gear: the hero row holds
     * exactly two things, the title and the gear chip, and no node outside the row reaches up into
     * it or above it (the stage included). Both chip extremes, since the chips decide how much room
     * the stage gets.
     */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun nothingButTheGearIsComposedBesideOrAboveTheTitleOnATallPhone() = assertOnlyTheTitleAndTheGearInTheHero()

    /** The same on the narrow baseline, where the hero was already a row. */
    @Test
    @Config(qualifiers = NARROW_BASELINE_QUALIFIER)
    fun nothingButTheGearIsComposedBesideOrAboveTheTitleOnTheNarrowBaseline() = assertOnlyTheTitleAndTheGearInTheHero()

    // ---- Criterion 15o: one row with the gear ----------------------------------

    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theTitleSitsInOneRowWithTheGearOnATallPhone() = assertTitleSitsInOneRowWithTheGear()

    @Test
    @Config(qualifiers = NARROW_BASELINE_QUALIFIER)
    fun theTitleSitsInOneRowWithTheGearOnTheNarrowBaseline() = assertTitleSitsInOneRowWithTheGear()

    /**
     * Criterion 15o at 200%: the title may wrap and the row grows, but it is never truncated and
     * never scaled, and the gear stays in the row at the column's end edge.
     */
    @Test
    @Config(qualifiers = NARROW_BASELINE_QUALIFIER)
    fun atDoubleFontScaleTheTitleWrapsRatherThanShrinksAndTheGearStaysInTheRow() {
        composeRule.renderHome(state = chipsState(classic = true, challenge = true), fontScale = DOUBLE_FONT_SCALE)
        supplyInsets()

        assertTitleWholeAtDisplayScale()
        val title = bounds(title())
        val gear = gearChip()
        val content = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG))
        assertThat(gear.top).isLessThan(title.bottom)
        assertThat(title.top).isLessThan(gear.bottom)
        assertThat(gear.right.value).isWithin(EDGE_TOLERANCE_DP).of(content.right.value)
        assertNothingElseTakesRoomBesideOrAboveTheTitle()
    }

    // ---- Assertions ------------------------------------------------------------

    /**
     * The hero row's children are exactly the title and the gear chip, and every other node in
     * Home's column that has a height begins at or below the row's bottom — unless it is a
     * container the row sits inside. Those two halves catch anything that has a node; decorative
     * art has none, so a third half asks the layout instead
     * ([assertNothingElseTakesRoomBesideOrAboveTheTitle]). A logo, an emblem or a stray board
     * anywhere in or above the row fails one of the three. The two ambient `HomeBlobs` drift
     * *behind* the column, not in it, and §4.2.4 names them as the one thing that may be drawn
     * above the title.
     *
     * Asked of the tree rather than of `BOARD_CANVAS_TEST_TAG`s, because a second test tag on the
     * same node overrides that one: the withdrawn cluster carried its own tag and was invisible to
     * a canvas-tag query, which is exactly how an assertion stops being able to fail.
     */
    private fun assertOnlyTheTitleAndTheGearInTheHero() {
        val holder = mutableStateOf(chipsState(classic = false, challenge = false))
        composeRule.renderHome(holder)
        supplyInsets()

        listOf(chipsState(classic = false, challenge = false), chipsState(classic = true, challenge = true))
            .forEach { state ->
                composeRule.runOnUiThread { holder.value = state }
                composeRule.waitForIdle()

                val row = composeRule.onNodeWithTag(HOME_HERO_TEST_TAG, useUnmergedTree = true).fetchSemanticsNode()
                assertThat(row.children.map { describe(it) }).containsExactly(TITLE, GEAR)

                val rowBounds = boundsOf(row)
                val inRow = setOf(row.id) + row.descendants().map { it.id }
                val intruders =
                    columnNodes()
                        .filter { it.id !in inRow && it.size.height > 0 }
                        .filter { boundsOf(it).top < rowBounds.bottom && !boundsOf(it).contains(rowBounds) }
                assertThat(intruders.map { describe(it) }).isEmpty()
                assertNothingElseTakesRoomBesideOrAboveTheTitle()
            }
    }

    /**
     * Nothing takes room beside the title or above the row — asked of the **layout**, because
     * decorative art is exactly what the semantics tree cannot show: an `Icon` with a null
     * description (§11's idiom for decoration) has no node for any query above to find, and it
     * still takes room.
     *
     * Beside: the title and the gear chip tile the row — the title starts at the row's start, ends
     * where the chip starts, and the chip ends at the row's end — so nothing is laid out before,
     * between or after them. Above: the row is the first thing in Home's column — its top is the
     * column's content top, one column inset below the viewport's top edge (the inset read off the
     * ribbon, which fills the content width, since the column pads all four sides alike) — so
     * nothing is laid out above it.
     */
    private fun assertNothingElseTakesRoomBesideOrAboveTheTitle() {
        val row = boundsOf(composeRule.onNodeWithTag(HOME_HERO_TEST_TAG, useUnmergedTree = true).fetchSemanticsNode())
        val title = bounds(title())
        val gear = gearChip()
        assertWithMessage("the title's start edge against the row's, in dp")
            .that(title.left.value)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(row.left.value)
        assertWithMessage("the title's end edge against the gear chip's start edge, in dp")
            .that(title.right.value)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(gear.left.value)
        assertWithMessage("the gear chip's end edge against the row's, in dp")
            .that(gear.right.value)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(row.right.value)

        val viewport = boundsOf(composeRule.onNode(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNode())
        val columnInset = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG)).left - viewport.left
        assertWithMessage("how far below the column's top edge the row starts, in dp")
            .that((row.top - viewport.top).value)
            .isWithin(EDGE_TOLERANCE_DP)
            .of(columnInset.value)
    }

    /** What a node is, in a word a failure message can use. */
    private fun describe(node: SemanticsNode): String =
        when {
            node.config.getOrNull(SemanticsProperties.Text)?.any { it.text == appName } == true -> TITLE
            (listOf(node) + node.descendants()).any { settingsLabel in it.descriptions() } -> GEAR
            else -> node.config.getOrNull(SemanticsProperties.TestTag) ?: node.config.toString()
        }

    private fun SemanticsNode.descriptions(): List<String> =
        config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()

    private fun DpRect.contains(other: DpRect): Boolean =
        left <= other.left && top <= other.top && right >= other.right && bottom >= other.bottom

    /**
     * The row, on the current window: the title starts at the column's content start and the gear
     * ends at its content end (±1dp) — read off the ribbon, which fills that width exactly — their
     * vertical ranges overlap, nothing is composed above the title, and the title is whole at
     * `displaySmall`.
     */
    private fun assertTitleSitsInOneRowWithTheGear() {
        composeRule.renderHome(state = chipsState(classic = true, challenge = true))
        supplyInsets()

        val title = bounds(title())
        val gear = gearChip()
        val content = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG))
        assertThat(title.left.value).isWithin(EDGE_TOLERANCE_DP).of(content.left.value)
        // The node spans its weighted width however the text sits inside it, so the start edge of
        // the *rendered* line is read too: a title centred in its box would pass the line above.
        assertThat(firstLineInsetDp()).isWithin(EDGE_TOLERANCE_DP).of(0f)
        assertThat(gear.right.value).isWithin(EDGE_TOLERANCE_DP).of(content.right.value)
        assertThat(gear.top).isLessThan(title.bottom)
        assertThat(title.top).isLessThan(gear.bottom)
        assertThat(nodesWhollyAbove(title.top)).isEmpty()
        assertNothingElseTakesRoomBesideOrAboveTheTitle()
        assertTitleWholeAtDisplayScale()
    }

    /** `displaySmall` is 36sp; the whole string is laid out, on as many lines as it needs, and none is cut. */
    private fun assertTitleWholeAtDisplayScale() {
        val layout = layoutOf(title())
        assertThat(layout.layoutInput.style.fontSize).isEqualTo(DISPLAY_SMALL_SP.sp)
        assertThat(layout.layoutInput.text.text).isEqualTo(appName)
        assertThat(layout.multiParagraph.didExceedMaxLines).isFalse()
        (0 until layout.lineCount).forEach { line ->
            assertWithMessage("the title is ellipsised on line $line").that(layout.isLineEllipsized(line)).isFalse()
        }
    }

    // ---- Harness ---------------------------------------------------------------

    private fun supplyInsets() = composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)

    private fun title(): SemanticsNodeInteraction = composeRule.onNode(hasText(appName), useUnmergedTree = true)

    /**
     * The gear **chip** — the 48dp clay circle, not the 40dp button M3 centres inside it: the
     * button's node is the one carrying the description, and the chip is its parent.
     */
    private fun gearChip(): DpRect =
        boundsOf(checkNotNull(composeRule.onNodeWithContentDescription(settingsLabel).fetchSemanticsNode().parent))

    private fun bounds(node: SemanticsNodeInteraction): DpRect = composeRule.columnBoundsOf(node)

    private fun boundsOf(node: SemanticsNode): DpRect =
        with(composeRule.density) {
            DpRect(
                left = node.positionInRoot.x.toDp(),
                top = node.positionInRoot.y.toDp(),
                right = (node.positionInRoot.x + node.size.width).toDp(),
                bottom = (node.positionInRoot.y + node.size.height).toDp(),
            )
        }

    /** Every node in Home's column with a height that ends at or above [top] — composed wholly above it. */
    private fun nodesWhollyAbove(top: Dp): List<String> =
        columnNodes()
            .filter { it.size.height > 0 && boundsOf(it).bottom <= top }
            .map { describe(it) }

    /** Every node inside Home's scrolling column — where the bands are, and not the blobs behind it. */
    private fun columnNodes(): List<SemanticsNode> =
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNode().descendants()

    private fun SemanticsNode.descendants(): List<SemanticsNode> = children.flatMap { listOf(it) + it.descendants() }

    /** How far the title's first rendered line starts inside its own node, in dp. */
    private fun firstLineInsetDp(): Float = with(composeRule.density) { layoutOf(title()).getLineLeft(0).toDp().value }

    private fun layoutOf(node: SemanticsNodeInteraction): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.single()
    }
}

/** The 6.7-inch reference phone, and the 360x640dp design baseline. */
private const val TALL_PHONE_QUALIFIER = "w412dp-h915dp"
private const val NARROW_BASELINE_QUALIFIER = "w360dp-h640dp"

/** The design baseline's system bars: a 24dp status bar and a 48dp three-button navigation bar. */
private const val STATUS_BAR_DP = 24f
private const val NAVIGATION_BAR_DP = 48f

/** Criterion 15o's ±1dp. */
private const val EDGE_TOLERANCE_DP = 1f

/** The hero title's type role, `displaySmall`. */
private const val DISPLAY_SMALL_SP = 36

/** The two things the hero row may hold. */
private const val TITLE = "the title"
private const val GEAR = "the gear"
