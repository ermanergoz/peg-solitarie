package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.home.fixture.DEFAULT_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.DOUBLE_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.RESUME_CHALLENGE_LABEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CLASSIC_LABEL
import com.app.pegsolitaire.ui.home.fixture.appName
import com.app.pegsolitaire.ui.home.fixture.challengeTileTitle
import com.app.pegsolitaire.ui.home.fixture.chipsState
import com.app.pegsolitaire.ui.home.fixture.classicTileTitle
import com.app.pegsolitaire.ui.home.fixture.columnBoundsOf
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.settingsLabel
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.max

/**
 * When the Continue chips arrive (ux-spec §4.2.5, D-03n): criteria 15s and 15t.
 *
 * The chips are known only once `HomeViewModel` has decoded both resume snapshots, so they land
 * after first paint, ≈0.6s on QA's AVD. Under D-03i's 3 : 1 split of the *current* slack every
 * chip's arrival moved the mode tiles 22–24dp, under a thumb that was already heading for them
 * (QA's loop-3 F-1). D-03n moves the room out of the stage above the ribbon instead: the footer is
 * one fifth of the slack Home has with **no** chip, S₀, fixed for the window, so the tiles hold
 * still wherever the stage can give the chips' room, and move by exactly what it cannot give.
 *
 * ## How it is read
 *
 * Every emission goes through the fixture's settable overload, so it is one composition across
 * the walk, as on a device: no chip, the Classic chip, both, none again; then the Challenge chip
 * and both. Bounds are read **along the column**, from each node's unclipped position
 * ([columnBoundsOf]), under NATIVE graphics in the app theme, with the design baseline's 24dp
 * status bar and 48dp navigation bar supplied (§13's two idioms). Every quantity the assertions
 * compare against is read from the no-chip emission on the same window, never retyped from the
 * spec's projected table, so the ±0.5dp tolerance is rounding and nothing else.
 *
 * - **S₀** is the no-chip stage slot plus the no-chip footer (§4.2.5, "How 15g's unused space is
 *   read"): the two things the slack becomes.
 * - **The footer** runs from the second tile's bottom edge to the bottom of the column's content,
 *   less one band gap and the column's bottom padding. The content's bottom is the viewport's plus
 *   the column's scrollable overflow, so the same reading holds where the column scrolls (15t).
 * - **R** is, for each chip present, its own height plus one band gap.
 * - **The band gap** is read where nothing else can sit, between the two mode tiles; the column's
 *   padding is the same token (§4.2.2).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeChipArrivalTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---- Criterion 15s: nothing a player can tap moves -------------------------

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun nothingAPlayerCanTapMovesWhenAChipArrivesOnA360x640dpPhone() = assertNothingAPlayerCanTapMoves()

    @Test
    @Config(qualifiers = "w360dp-h720dp")
    fun nothingAPlayerCanTapMovesWhenAChipArrivesOnA360x720dpPhone() = assertNothingAPlayerCanTapMoves()

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun nothingAPlayerCanTapMovesWhenAChipArrivesOnA360x800dpPhone() = assertNothingAPlayerCanTapMoves()

    @Test
    @Config(qualifiers = "w412dp-h780dp")
    fun nothingAPlayerCanTapMovesWhenAChipArrivesOnA412x780dpPhone() = assertNothingAPlayerCanTapMoves()

    @Test
    @Config(qualifiers = "w412dp-h850dp")
    fun nothingAPlayerCanTapMovesWhenAChipArrivesOnA412x850dpPhone() = assertNothingAPlayerCanTapMoves()

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun nothingAPlayerCanTapMovesWhenAChipArrivesOnA412x915dpPhone() = assertNothingAPlayerCanTapMoves()

    // ---- Criterion 15t: the shortfall, and no more -----------------------------

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun whereTheStageCannotGiveTheRoomTheTilesMoveByTheShortfallAndNoMoreOnA360x640dpPhone() =
        assertTheTilesMoveByTheShortfallAndNoMore()

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun whereTheStageCannotGiveTheRoomTheTilesMoveByTheShortfallAndNoMoreOnA412x915dpPhone() =
        assertTheTilesMoveByTheShortfallAndNoMore()

    /**
     * Criterion 15s on the current window, at default font scale, over both of its walks.
     *
     * 1. The title's, the gear's and both mode tiles' bounds equal their no-chip bounds.
     * 2. The footer is the same in every emission, and one fifth of S₀.
     * 3. The stage is max(0, ⅘S₀ − R), and wherever it is above 0 the ribbon rides up by exactly R.
     * 4. Every chip present is complete in the first viewport, and the column's scrollable overflow
     *    is what it is with no chip.
     *
     * If the stage cannot give the room on one of these windows (R > ⅘S₀), part 1 fails, and that
     * is a finding for the ux-designer with the numbers, never something to close by letting the
     * tiles move (§4.2.5).
     */
    private fun assertNothingAPlayerCanTapMoves() {
        val state = mutableStateOf(chipsState(classic = false, challenge = false))
        renderWithInsets(state, DEFAULT_FONT_SCALE)
        val noChip = readHome()
        val slack = noChip.stage + noChip.footer

        WALK.forEach { (classic, challenge) ->
            show(state, classic, challenge)
            val chips = chipsNamed(classic, challenge)
            val reading = readHome()
            val room = roomOf(reading, classic, challenge)

            assertSameBounds("$chips: the title", reading.title, noChip.title)
            assertSameBounds("$chips: the gear", reading.gear, noChip.gear)
            assertSameBounds("$chips: the Classic tile", reading.classicTile, noChip.classicTile)
            assertSameBounds("$chips: the Challenge tile", reading.challengeTile, noChip.challengeTile)
            assertWithMessage("$chips: the footer against a fifth of the no-chip slack S₀ = $slack, in dp")
                .that(reading.footer.value)
                .isWithin(TOLERANCE_DP)
                .of(slack.value / FOOTER_SHARE_DIVISOR)
            assertWithMessage("$chips: the footer against the no-chip footer, in dp")
                .that(reading.footer.value)
                .isWithin(TOLERANCE_DP)
                .of(noChip.footer.value)
            val stage = max(0f, slack.value * STAGE_SHARE_OF_SLACK - room.value)
            assertWithMessage("$chips: the stage against max(0, ⅘S₀ − R), R = $room, in dp")
                .that(reading.stage.value)
                .isWithin(TOLERANCE_DP)
                .of(stage)
            if (reading.stage > 0.dp) {
                assertWithMessage("$chips: the ribbon's top edge, risen by R = $room, in dp")
                    .that(reading.ribbonTop.value)
                    .isWithin(TOLERANCE_DP)
                    .of((noChip.ribbonTop - room).value)
            }
            chipNodes(classic, challenge).forEach(::assertAScrollDoesNotMove)
            assertWithMessage("$chips: the column's scrollable overflow, in dp")
                .that(reading.overflow.value)
                .isWithin(TOLERANCE_DP)
                .of(noChip.overflow.value)
        }
    }

    /**
     * Criterion 15t on the current window, at font scale 2.0: between any two emissions the mode
     * tiles sit exactly max(0, R − ⅘S₀) below their no-chip position — never above it — the hero
     * row does not move, no band is dropped, and where the column overflows it scrolls.
     */
    private fun assertTheTilesMoveByTheShortfallAndNoMore() {
        val state = mutableStateOf(chipsState(classic = false, challenge = false))
        renderWithInsets(state, DOUBLE_FONT_SCALE)
        val noChip = readHome()
        val slack = noChip.stage + noChip.footer

        WALK.forEach { (classic, challenge) ->
            show(state, classic, challenge)
            val chips = chipsNamed(classic, challenge)
            val reading = readHome()
            val room = roomOf(reading, classic, challenge)
            val shortfall = max(0f, room.value - slack.value * STAGE_SHARE_OF_SLACK)

            assertSameBounds("$chips: the title", reading.title, noChip.title)
            assertSameBounds("$chips: the gear", reading.gear, noChip.gear)
            listOf(
                "the Classic tile" to (reading.classicTile.top - noChip.classicTile.top),
                "the Challenge tile" to (reading.challengeTile.top - noChip.challengeTile.top),
            ).forEach { (tile, moved) ->
                assertWithMessage("$chips: how far $tile moved down, R = $room, S₀ = $slack, in dp")
                    .that(moved.value)
                    .isWithin(TOLERANCE_DP)
                    .of(shortfall)
            }
        }
        assertEveryBandIsReachable()
    }

    // ---- Harness ---------------------------------------------------------------

    private fun renderWithInsets(
        state: MutableState<HomeUiState>,
        fontScale: Float,
    ) {
        composeRule.renderHome(state, fontScale = fontScale)
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)
    }

    private fun show(
        holder: MutableState<HomeUiState>,
        classic: Boolean,
        challenge: Boolean,
    ) {
        composeRule.runOnUiThread { holder.value = chipsState(classic = classic, challenge = challenge) }
        composeRule.waitForIdle()
    }

    /** Everything one emission is judged on, read along the column with the column unscrolled. */
    private fun readHome(): HomeReading {
        val classicTile = bounds(composeRule.onNodeWithText(classicTileTitle))
        val challengeTile = bounds(composeRule.onNodeWithText(challengeTileTitle))
        val gap = challengeTile.top - classicTile.bottom
        val viewportBottom = composeRule.onRoot().getBoundsInRoot().bottom - NAVIGATION_BAR_DP.dp
        val overflow = scrollableOverflow()
        val contentBottom = viewportBottom + overflow
        val stage = bounds(composeRule.onNodeWithTag(HOME_STAGE_TEST_TAG, useUnmergedTree = true))
        return HomeReading(
            title = bounds(composeRule.onNode(hasText(appName), useUnmergedTree = true)),
            gear = gearChip(),
            classicTile = classicTile,
            challengeTile = challengeTile,
            ribbonTop = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG)).top,
            stage = stage.bottom - stage.top,
            // The column's padding is the band spacing (§4.2.2), so one gap and the padding both.
            footer = contentBottom - challengeTile.bottom - gap - gap,
            gap = gap,
            overflow = overflow,
        )
    }

    /** R: each chip present, its own height plus one band gap. */
    private fun roomOf(
        reading: HomeReading,
        classic: Boolean,
        challenge: Boolean,
    ): Dp =
        chipNodes(classic, challenge)
            .map { node -> heightOf(bounds(node)) + reading.gap }
            .fold(0.dp) { sum, chip -> sum + chip }

    private fun chipNodes(
        classic: Boolean,
        challenge: Boolean,
    ): List<SemanticsNodeInteraction> =
        listOfNotNull(
            composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).takeIf { classic },
            composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).takeIf { challenge },
        )

    private fun chipsNamed(
        classic: Boolean,
        challenge: Boolean,
    ): String =
        listOfNotNull("Classic".takeIf { classic }, "Challenge".takeIf { challenge })
            .joinToString(" and ")
            .ifEmpty { "no" } + " chip"

    /**
     * The gear **chip**, the 48dp clay circle the row draws: the parent of the button node that
     * carries the gear's description (as `HomeStageTest` reads it).
     */
    private fun gearChip(): DpRect =
        composeRule.columnBoundsOf(
            checkNotNull(composeRule.onNodeWithContentDescription(settingsLabel).fetchSemanticsNode().parent),
        )

    private fun bounds(node: SemanticsNodeInteraction): DpRect = composeRule.columnBoundsOf(node)

    private fun heightOf(bounds: DpRect): Dp = bounds.bottom - bounds.top

    private fun assertSameBounds(
        what: String,
        actual: DpRect,
        expected: DpRect,
    ) {
        listOf(
            "left" to (actual.left to expected.left),
            "top" to (actual.top to expected.top),
            "right" to (actual.right to expected.right),
            "bottom" to (actual.bottom to expected.bottom),
        ).forEach { (edge, pair) ->
            assertWithMessage("$what's $edge edge against its no-chip edge, in dp")
                .that(pair.first.value)
                .isWithin(TOLERANCE_DP)
                .of(pair.second.value)
        }
    }

    /**
     * "Complete in the first viewport", asserted as the only form of it that can fail: a scroll
     * does not move the node (§13). Bounds clipped to the root cannot show containment.
     */
    private fun assertAScrollDoesNotMove(node: SemanticsNodeInteraction) {
        val before = node.getBoundsInRoot()
        val after = node.performScrollTo().getBoundsInRoot()

        assertWithMessage("a chip's top edge before and after a scroll to it").that(after.top).isEqualTo(before.top)
        assertWithMessage("a chip's bottom edge before and after a scroll to it")
            .that(after.bottom)
            .isEqualTo(before.bottom)
    }

    /** No band is dropped, and the last one is one plain scroll away (criteria 14, 15i). */
    private fun assertEveryBandIsReachable() {
        listOf(appName, RESUME_CLASSIC_LABEL, RESUME_CHALLENGE_LABEL, classicTileTitle, challengeTileTitle)
            .forEach { text ->
                assertWithMessage("how many nodes read '$text'")
                    .that(composeRule.onAllNodesWithText(text).fetchSemanticsNodes())
                    .hasSize(1)
                composeRule.onNodeWithText(text).performScrollTo().assertIsDisplayed()
            }
        composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG).performScrollTo().assertIsDisplayed()
    }

    /** The column's total scrollable overflow, from the scroll container's own axis range. */
    private fun scrollableOverflow(): Dp {
        val range =
            composeRule
                .onNode(hasScrollAction())
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
        return with(composeRule.density) { range.maxValue().toDp() }
    }

    /** One emission's geometry, in dp, the column unscrolled. */
    private data class HomeReading(
        val title: DpRect,
        val gear: DpRect,
        val classicTile: DpRect,
        val challengeTile: DpRect,
        val ribbonTop: Dp,
        val stage: Dp,
        val footer: Dp,
        val gap: Dp,
        val overflow: Dp,
    )
}

/**
 * Both of 15s's walks as one: no chip → the Classic chip → both → none, then → the Challenge chip →
 * both. Each pair is (Classic present, Challenge present).
 */
private val WALK =
    listOf(
        true to false,
        true to true,
        false to false,
        false to true,
        true to true,
    )

/** The design baseline's system bars: a 24dp status bar and a 48dp three-button navigation bar. */
private const val STATUS_BAR_DP = 24
private const val NAVIGATION_BAR_DP = 48

/** Criteria 15s and 15t's ±0.5dp. */
private const val TOLERANCE_DP = 0.5f

/** D-03n's split of S₀: the footer is a fifth, so the stage is four fifths less the chips' room. */
private const val FOOTER_SHARE_DIVISOR = 5f
private const val STAGE_SHARE_OF_SLACK = 0.8f
