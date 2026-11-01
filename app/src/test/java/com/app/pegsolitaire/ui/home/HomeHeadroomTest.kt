package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.home.fixture.BOARDS_PILL
import com.app.pegsolitaire.ui.home.fixture.CLEARED_BOARDS
import com.app.pegsolitaire.ui.home.fixture.DEFAULT_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.DOUBLE_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.EARNED_STARS
import com.app.pegsolitaire.ui.home.fixture.EVERY_CHIP_STATE
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.HomeSlack
import com.app.pegsolitaire.ui.home.fixture.LEVEL_PILL
import com.app.pegsolitaire.ui.home.fixture.PERSISTED_LEVEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CHALLENGE_LABEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CLASSIC_LABEL
import com.app.pegsolitaire.ui.home.fixture.STARS_PILL
import com.app.pegsolitaire.ui.home.fixture.appName
import com.app.pegsolitaire.ui.home.fixture.bothResumesState
import com.app.pegsolitaire.ui.home.fixture.challengeTileTitle
import com.app.pegsolitaire.ui.home.fixture.classicTileTitle
import com.app.pegsolitaire.ui.home.fixture.columnBoundsOf
import com.app.pegsolitaire.ui.home.fixture.homeSlack
import com.app.pegsolitaire.ui.home.fixture.pillDescriptions
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.ribbon
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
 * Home's slack as **headroom above the hero row** (ux-spec §4.2.7, D-03p) and the title bound that
 * stops it (D-03q): criteria 15x, 15y and 15ac part 1, and 15ab part 3. The stage D-03l put beneath
 * the hero row is withdrawn on the founder's note; the slack it held now sits above the title as space
 * with nothing in it, so the title, the ribbon, the chips and the tiles are one block in the lower
 * part of the screen with the backdrop's sky above them, and the hero row never goes below the line the
 * backdrop keeps clear of its waves, `TITLE_REACH` of the window less the entrance slide.
 *
 * Renamed from `HomeStageTest` (ux-spec §13's D-03p table): its gap reads became 15y part 3, its
 * traversal case 15ab part 3, its "renders identically" case the hero row's and the ribbon's bounds
 * across the progress emission, and its semantics case — the stage carried nothing but test tags —
 * the headroom carrying no node at all (15x); the ≥ 120dp art went with its subject.
 *
 * ## How it is measured
 *
 * With the device's system-bar insets supplied (§13 forbids a zero-inset reading of a layout property),
 * under NATIVE graphics in the app theme, distances taken **along the column** from each node's
 * unclipped position, which with the insets supplied are the window's coordinates. Every expected
 * quantity is [HomeSlack]'s, computed from the window and the bands' own heights and never from where
 * the column put the hero row, so the reading can fail.
 *
 * ## The windows
 *
 * 15p part 2's six, with the design baseline's 24dp status bar and 48dp three-button bar, and 15ac's
 * two gesture-navigation windows: `w412dp-h915dp` with a 24dp status bar and a 24dp gesture bar, and the
 * Pixel_9, `w411dp-h923dp`, with a 54dp status bar and a 24dp gesture bar. Every window is read against
 * H₀ = min(⅘ S₀, C) and the bound alike, with no class of window exempt: by §4.2.7's eight-window table,
 * and measured here, the bound bites on `w360dp-h720dp`, `w360dp-h800dp` and `w412dp-h915dp` with the
 * three-button bar as well as on the two gesture-navigation windows, and on none of them at font scale
 * 2.0, where the slack is smaller. Every expected figure is derived from the formula, never copied
 * from a table.
 *
 * ## Between visible elements (D-03m)
 *
 * Every gap runs from one element's visible bottom edge to the next one's visible top edge: the hero
 * row is the span of its title and its gear chip, the ribbon the span of its three pills, each chip and
 * each tile its own surface. The headroom above the first element and the footer below the last are not
 * gaps between two elements.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeHeadroomTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---- Criteria 15y and 15ac part 1 at default font scale ----------------------

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun onA360x640dpPhoneTheSlackIsHeadroomAboveTheHeroRowInEveryChipState() =
        assertTheSlackIsHeadroomAboveTheHeroRow(THREE_BUTTON)

    @Test
    @Config(qualifiers = "w360dp-h720dp")
    fun onA360x720dpPhoneTheSlackIsHeadroomAboveTheHeroRowInEveryChipState() =
        assertTheSlackIsHeadroomAboveTheHeroRow(THREE_BUTTON)

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun onA360x800dpPhoneTheSlackIsHeadroomAboveTheHeroRowInEveryChipState() =
        assertTheSlackIsHeadroomAboveTheHeroRow(THREE_BUTTON)

    /** 780 − 72 = 708dp usable, under the 720dp threshold: this window takes the compact set. */
    @Test
    @Config(qualifiers = "w412dp-h780dp")
    fun onA412x780dpPhoneTheSlackIsHeadroomAboveTheHeroRowInEveryChipState() =
        assertTheSlackIsHeadroomAboveTheHeroRow(THREE_BUTTON)

    @Test
    @Config(qualifiers = "w412dp-h850dp")
    fun onA412x850dpPhoneTheSlackIsHeadroomAboveTheHeroRowInEveryChipState() =
        assertTheSlackIsHeadroomAboveTheHeroRow(THREE_BUTTON)

    /**
     * The 6.7-inch reference phone (15y part 4, 15g): the first mode tile's top edge is at or below the
     * viewport's midpoint with no chip and with both; and no single gap on the screen is larger than the
     * space the tiles occupy — 15g's fourth clause as §4.2.7 "15g's fourth clause under the bound" rules
     * it: that space is both tiles with their band gap, and a gap is a distance between two consecutive
     * visible elements, so neither the headroom above the first element nor the footer below the last is
     * one. (15y part 3 holds every such gap to the band gap, which this clause can only follow.) The
     * footer's third of S₀ is read on every window (point 5 of [assertTheSlackIsHeadroomAboveTheHeroRow]).
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun onA412x915dpPhoneTheSlackIsHeadroomAboveTheHeroRowAndTheTilesStayLow() {
        assertTheSlackIsHeadroomAboveTheHeroRow(THREE_BUTTON)
        listOf(chipsOf(classic = false, challenge = false), bothResumesState()).forEach { state ->
            show(state)
            val viewport = viewportOf(THREE_BUTTON)
            assertWithMessage("${chipsNamed(state)}: the first tile's top against the viewport's midpoint, in dp")
                .that(boundsOf(composeRule.onNodeWithText(classicTileTitle)).top.value)
                .isAtLeast(((viewport.top + viewport.bottom) / 2).value)
            assertWithMessage("${chipsNamed(state)}: the widest gap between two elements against the tiles' span")
                .that(widestGap().value)
                .isAtMost(tilesSpan().value)
        }
    }

    /** 15ac part 1 on the tall phone in gesture navigation, where the bound holds the no-chip headroom at C. */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun onA412x915dpPhoneInGestureNavigationTheTitleBoundHoldsTheHeroRowAboveTheWaves() =
        assertTheSlackIsHeadroomAboveTheHeroRow(GESTURE_TALL)

    /**
     * 15ac part 1 on the Pixel_9, QA's device: with no save the hero row's bottom edge sits exactly on the
     * bound, 0.47 × 923 − 16 = 417.8dp, and the headroom is C = 417.8 − 54 − 16 − 48 = 299.8dp — §4.2.7's
     * D-03q table, by hand — against ⅘ S₀ = 339.1dp; the excess lies below the tiles.
     */
    @Test
    @Config(qualifiers = PIXEL_9_WINDOW)
    fun onThePixel9WithNoSaveTheHeroRowEndsExactlyOnTheTitleBound() {
        assertTheSlackIsHeadroomAboveTheHeroRow(PIXEL_9_BARS)
        show(chipsOf(classic = false, challenge = false))

        assertWithMessage("the hero row's visible bottom edge against the bound, in dp")
            .that(heroVisibleBottom().value)
            .isWithin(TOLERANCE_DP)
            .of(PIXEL_9_TITLE_BOUND_DP)
        assertWithMessage("the headroom, in dp")
            .that(headroom(PIXEL_9_BARS).value)
            .isWithin(TOLERANCE_DP)
            .of(PIXEL_9_C_DP)
    }

    // ---- Criterion 15ac part 1 at font scale 2.0: the bound, wherever there is headroom --

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun atDoubleFontScaleOnA360x640dpPhoneTheHeroRowNeverPassesTheTitleBound() =
        assertTheHeroRowNeverPassesTheTitleBound(THREE_BUTTON)

    @Test
    @Config(qualifiers = "w360dp-h720dp")
    fun atDoubleFontScaleOnA360x720dpPhoneTheHeroRowNeverPassesTheTitleBound() =
        assertTheHeroRowNeverPassesTheTitleBound(THREE_BUTTON)

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun atDoubleFontScaleOnA360x800dpPhoneTheHeroRowNeverPassesTheTitleBound() =
        assertTheHeroRowNeverPassesTheTitleBound(THREE_BUTTON)

    @Test
    @Config(qualifiers = "w412dp-h780dp")
    fun atDoubleFontScaleOnA412x780dpPhoneTheHeroRowNeverPassesTheTitleBound() =
        assertTheHeroRowNeverPassesTheTitleBound(THREE_BUTTON)

    @Test
    @Config(qualifiers = "w412dp-h850dp")
    fun atDoubleFontScaleOnA412x850dpPhoneTheHeroRowNeverPassesTheTitleBound() =
        assertTheHeroRowNeverPassesTheTitleBound(THREE_BUTTON)

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun atDoubleFontScaleOnA412x915dpPhoneTheHeroRowNeverPassesTheTitleBound() =
        assertTheHeroRowNeverPassesTheTitleBound(THREE_BUTTON)

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun atDoubleFontScaleOnA412x915dpPhoneInGestureNavigationTheHeroRowNeverPassesTheTitleBound() =
        assertTheHeroRowNeverPassesTheTitleBound(GESTURE_TALL)

    @Test
    @Config(qualifiers = PIXEL_9_WINDOW)
    fun atDoubleFontScaleOnThePixel9TheHeroRowNeverPassesTheTitleBound() =
        assertTheHeroRowNeverPassesTheTitleBound(PIXEL_9_BARS)

    // ---- Criterion 15x: no stage, and nothing in the headroom --------------------

    /**
     * In every chip state: no node carries the withdrawn stage's tags; nothing in Home's column is
     * composed above the title — the headroom is space, not a child, so it has no node to find; and the
     * one `BoardCanvas` on Home is the Classic Continue chip's thumbnail, composed only while a Classic
     * save exists. Today's code fails it: the stage composed a board in every state.
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun nothingIsComposedInTheHeadroomAndTheOnlyBoardOnHomeIsTheClassicThumbnail() {
        renderWithInsets(mutableStateOf(EVERY_CHIP_STATE.first()), THREE_BUTTON)
        EVERY_CHIP_STATE.forEach { state ->
            show(state)
            val chips = chipsNamed(state)
            WITHDRAWN_STAGE_TAGS.forEach { tag ->
                assertWithMessage("$chips: nodes tagged $tag")
                    .that(composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes())
                    .isEmpty()
            }
            assertWithMessage("$chips: boards composed on Home")
                .that(boardsComposed())
                .hasSize(if (state.classicResumeAvailable) 1 else 0)
            val heroTop = boundsOf(heroRow()).top
            val above = columnNodes().filter { it.size.height > 0 && composeRule.columnBoundsOf(it).bottom <= heroTop }
            assertWithMessage("$chips: nodes composed wholly above the hero row").that(above.map { it.id }).isEmpty()
        }
    }

    // ---- Criterion 15ab part 3: the traversal is unchanged -----------------------

    /**
     * Home's TalkBack traversal is unchanged by the headroom: title, gear, the three pills, both
     * Continue chips, both tiles — and no stop before the title, because the headroom has no node.
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun homesTalkBackTraversalRunsTitleGearPillsChipsTilesAndNothingElse() {
        renderWithInsets(mutableStateOf(bothResumesState()), THREE_BUTTON)

        assertThat(spokenInTraversalOrder())
            .containsExactly(
                appName,
                settingsLabel,
                composeRule.pillDescriptions(STARS_PILL).single(),
                composeRule.pillDescriptions(BOARDS_PILL).single(),
                composeRule.pillDescriptions(LEVEL_PILL).single(),
                RESUME_CLASSIC_LABEL,
                RESUME_CHALLENGE_LABEL,
                classicTileTitle,
                challengeTileTitle,
            ).inOrder()
    }

    // ---- Criterion 15y part 1: the progress emission moves nothing ---------------

    /**
     * The hero row and the ribbon hold still across `HomeUiState`'s first progress emission: the ribbon
     * fills its values in place and the headroom does not depend on them. The retarget of the stage's
     * "renders identically before and after the first progress emission" (15r).
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun theHeroRowAndTheRibbonHoldStillAcrossTheFirstProgressEmission() {
        val state = mutableStateOf(HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL))
        renderWithInsets(state, THREE_BUTTON)
        val heroBefore = boundsOf(heroRow())
        val ribbonBefore = boundsOf(composeRule.ribbon())

        show(state.value.copy(totalStars = EARNED_STARS, classicBoardsCleared = CLEARED_BOARDS))

        assertThat(boundsOf(heroRow())).isEqualTo(heroBefore)
        assertThat(boundsOf(composeRule.ribbon())).isEqualTo(ribbonBefore)
    }

    // ---- Assertions --------------------------------------------------------------

    /**
     * Criteria 15y parts 1–3 and 15ac part 1 at default font scale on the current window with [bars], in
     * every chip state:
     * 1. the hero row's top edge is the column's top padding plus the headroom, max(0, H₀ − R), below
     *    the viewport's top — H₀ = min(⅘ S₀, C) — and the padding exactly wherever the column overflows;
     * 2. the footer, from the second tile's bottom edge to the content's bottom less one band gap and
     *    the bottom padding, is min(S₀ − H₀, max(0, S₀ − R));
     * 3. no distance between two consecutive visible elements exceeds the band gap;
     * 4. the hero row's visible bottom edge is at or above the title bound, wherever there is headroom;
     * 5. the footer is at most a third of S₀ (15g's ruled reading, the footer being S₀ − H₀ under
     *    D-03q) — on every window, the bound biting or not.
     */
    private fun assertTheSlackIsHeadroomAboveTheHeroRow(bars: Bars) {
        val holder = mutableStateOf(EVERY_CHIP_STATE.first())
        renderWithInsets(holder, bars)
        val slack = composeRule.homeSlack(statusBar = bars.status, navigationBar = bars.navigation)

        EVERY_CHIP_STATE.forEach { state ->
            show(state)
            val chips = chipsNamed(state)
            val room = roomOf(state)
            assertWithMessage("$chips: the headroom against max(0, H₀ − R), H₀ = ${slack.noChipHeadroom}, R = $room")
                .that(headroom(bars).value)
                .isWithin(TOLERANCE_DP)
                .of(slack.headroomWith(room).value)
            assertWithMessage("$chips: the footer against min(S₀ − H₀, max(0, S₀ − R)), S₀ = ${slack.slack}, in dp")
                .that(footer(bars).value)
                .isWithin(TOLERANCE_DP)
                .of(slack.footerWith(room).value)
            assertWithMessage("$chips: the widest gap between two visible elements, in dp")
                .that(widestGap().value)
                .isAtMost((bandGap() + TOLERANCE_DP.dp).value)
            assertTheHeroRowIsAboveTheTitleBound(slack, chips)
            assertWithMessage("$chips: the footer against a third of S₀ = ${slack.slack}, in dp")
                .that(footer(bars).value)
                .isAtMost(slack.slack.value / THIRDS + TOLERANCE_DP)
        }
    }

    /**
     * Criterion 15ac part 1 at font scale 2.0 on the current window with [bars], in every chip state:
     * the headroom is still max(0, H₀ − R); wherever it is above 0 the hero row's visible bottom edge is
     * at or above the title bound; the footer is the formula's, min(S₀ − H₀, max(0, S₀ − R)) — S₀ − H₀
     * wherever the headroom can give the room, and what is left of S₀ once a room larger than H₀ has
     * taken its share (15z part 3) — and at most a third of S₀. At 2.0 the slack is small, so the room
     * often outgrows the headroom and the row sits at the top padding, which is where the bound does
     * not apply: the wrapped title's case, which B-21 already covers.
     */
    private fun assertTheHeroRowNeverPassesTheTitleBound(bars: Bars) {
        val holder = mutableStateOf(EVERY_CHIP_STATE.first())
        renderWithInsets(holder, bars, DOUBLE_FONT_SCALE)
        val slack = composeRule.homeSlack(statusBar = bars.status, navigationBar = bars.navigation)

        EVERY_CHIP_STATE.forEach { state ->
            show(state)
            val chips = chipsNamed(state)
            val room = roomOf(state)
            assertWithMessage("$chips, at 2.0: the headroom against max(0, H₀ − R), in dp")
                .that(headroom(bars).value)
                .isWithin(TOLERANCE_DP)
                .of(slack.headroomWith(room).value)
            assertTheHeroRowIsAboveTheTitleBound(slack, "$chips, at 2.0")
            assertWithMessage("$chips, at 2.0: the footer against min(S₀ − H₀, max(0, S₀ − R)), in dp")
                .that(footer(bars).value)
                .isWithin(TOLERANCE_DP)
                .of(slack.footerWith(room).value)
            assertWithMessage("$chips, at 2.0: the footer against a third of S₀ = ${slack.slack}, in dp")
                .that(footer(bars).value)
                .isAtMost(slack.slack.value / THIRDS + TOLERANCE_DP)
        }
    }

    /** Wherever the hero row sits below its top padding, its visible bottom edge is at or above the bound. */
    private fun assertTheHeroRowIsAboveTheTitleBound(
        slack: HomeSlack,
        situation: String,
    ) {
        if (headroomIsOpen()) {
            assertWithMessage("$situation: the hero row's visible bottom edge against the title bound, in dp")
                .that(heroVisibleBottom().value)
                .isAtMost(slack.titleBound.value + TOLERANCE_DP)
        }
    }

    // ---- Readings ----------------------------------------------------------------

    /** The headroom as laid out: the hero row's top edge less the viewport's top and the column's padding. */
    private fun headroom(bars: Bars): Dp = boundsOf(heroRow()).top - bars.status - bandGap()

    /** Whether the hero row sits below the column's top padding by more than the tolerance. */
    private fun headroomIsOpen(): Boolean {
        val viewport = boundsOf(composeRule.onNode(hasScrollAction(), useUnmergedTree = true))
        return boundsOf(heroRow()).top - viewport.top - bandGap() > TOLERANCE_DP.dp
    }

    /**
     * The footer as laid out: from the second tile's bottom edge to the content's bottom, less one band
     * gap and the bottom padding. The content's bottom is the viewport's plus the column's scrollable
     * overflow, so the reading holds where the column scrolls.
     */
    private fun footer(bars: Bars): Dp {
        val contentBottom = viewportOf(bars).bottom + scrollableOverflow()
        return contentBottom - boundsOf(composeRule.onNodeWithText(challengeTileTitle)).bottom - bandGap() * 2
    }

    /** The viewport the column scrolls in: the window less the bars. */
    private fun viewportOf(bars: Bars): DpRect {
        val window = boundsOf(composeRule.onRoot())
        return DpRect(
            left = window.left,
            top = window.top + bars.status,
            right = window.right,
            bottom = window.bottom - bars.navigation,
        )
    }

    /** 15g's "space the tiles occupy" (§4.2.7, F-2's ruling): both mode tiles with their band gap. */
    private fun tilesSpan(): Dp {
        val classicTile = boundsOf(composeRule.onNodeWithText(classicTileTitle))
        return boundsOf(composeRule.onNodeWithText(challengeTileTitle)).bottom - classicTile.top
    }

    /** The band gap, read where nothing else can sit: between the two mode tiles. */
    private fun bandGap(): Dp {
        val classicTile = boundsOf(composeRule.onNodeWithText(classicTileTitle))
        return boundsOf(composeRule.onNodeWithText(challengeTileTitle)).top - classicTile.bottom
    }

    /** R: each Continue chip present, its own height plus one band gap. */
    private fun roomOf(state: HomeUiState): Dp =
        listOfNotNull(
            RESUME_CLASSIC_LABEL.takeIf { state.classicResumeAvailable },
            RESUME_CHALLENGE_LABEL.takeIf { state.challengeResumeAvailable },
        ).fold(0.dp) { room, chip ->
            val bounds = boundsOf(composeRule.onNodeWithText(chip))
            room + (bounds.bottom - bounds.top) + bandGap()
        }

    /** The hero row's visible bottom edge (D-03m): the lower of its title's and its gear chip's. */
    private fun heroVisibleBottom(): Dp = maxOf(boundsOf(heroTitle()).bottom, gearChip().bottom)

    /**
     * The widest distance between two consecutive visible elements, top to bottom: the hero row (its
     * title and its gear chip), the ribbon (its three pills), each Continue chip, the first mode tile
     * and the second.
     */
    private fun widestGap(): Dp {
        val elements =
            buildList {
                add(spanning(listOf(boundsOf(heroTitle()), gearChip())))
                add(spanning(ribbonPills()))
                listOf(RESUME_CLASSIC_LABEL, RESUME_CHALLENGE_LABEL).forEach { chip ->
                    if (composeRule.onAllNodesWithText(chip).fetchSemanticsNodes().isNotEmpty()) {
                        add(boundsOf(composeRule.onNodeWithText(chip)))
                    }
                }
                add(boundsOf(composeRule.onNodeWithText(classicTileTitle)))
                add(boundsOf(composeRule.onNodeWithText(challengeTileTitle)))
            }.sortedBy { it.top.value }
        return elements.zipWithNext { upper, lower -> lower.top - upper.bottom }.maxOf { it }
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

    // ---- Harness -----------------------------------------------------------------

    /** The state Home was rendered from, so each test can walk its emissions on one composition. */
    private lateinit var holder: MutableState<HomeUiState>

    private fun renderWithInsets(
        state: MutableState<HomeUiState>,
        bars: Bars,
        fontScale: Float = DEFAULT_FONT_SCALE,
    ) {
        holder = state
        composeRule.renderHome(state, fontScale = fontScale)
        composeRule.applySystemBarInsets(top = bars.status, bottom = bars.navigation)
    }

    private fun show(state: HomeUiState) {
        composeRule.runOnUiThread { holder.value = state }
        composeRule.waitForIdle()
    }

    private fun chipsOf(
        classic: Boolean,
        challenge: Boolean,
    ): HomeUiState =
        EVERY_CHIP_STATE.single { it.classicResumeAvailable == classic && it.challengeResumeAvailable == challenge }

    private fun chipsNamed(state: HomeUiState): String =
        listOfNotNull(
            "Classic".takeIf { state.classicResumeAvailable },
            "Challenge".takeIf { state.challengeResumeAvailable },
        ).joinToString(" and ").ifEmpty { "no chip" } + " saved"

    private fun heroRow(): SemanticsNodeInteraction =
        composeRule.onNodeWithTag(HOME_HERO_TEST_TAG, useUnmergedTree = true)

    /** The hero's title: a laid-out text, and the row's visible content with the gear chip. */
    private fun heroTitle(): SemanticsNodeInteraction = composeRule.onNode(hasText(appName), useUnmergedTree = true)

    /**
     * The gear **chip**, the 48dp clay circle the row draws: the parent of the button node that
     * carries the gear's description.
     */
    private fun gearChip(): DpRect =
        composeRule.columnBoundsOf(
            checkNotNull(composeRule.onNodeWithContentDescription(settingsLabel).fetchSemanticsNode().parent),
        )

    /** The ribbon's three pills: the surfaces the ribbon draws. */
    private fun ribbonPills(): List<DpRect> =
        composeRule
            .ribbon()
            .onChildren()
            .fetchSemanticsNodes()
            .map(composeRule::columnBoundsOf)

    /** Every board canvas Home composes, from the unmerged tree: the Classic chip merges its thumbnail's. */
    private fun boardsComposed(): List<SemanticsNode> =
        composeRule.onAllNodesWithTag(BOARD_CANVAS_TEST_TAG, useUnmergedTree = true).fetchSemanticsNodes()

    /** Every node inside Home's scrolling column — where the bands are, and not the backdrop behind it. */
    private fun columnNodes(): List<SemanticsNode> =
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNode().descendants()

    /** The band a set of visible elements makes: from the highest top edge to the lowest bottom edge. */
    private fun spanning(parts: List<DpRect>): DpRect =
        DpRect(
            left = parts.minOf { it.left },
            top = parts.minOf { it.top },
            right = parts.maxOf { it.right },
            bottom = parts.maxOf { it.bottom },
        )

    private fun boundsOf(node: SemanticsNodeInteraction): DpRect = composeRule.columnBoundsOf(node)

    /**
     * What TalkBack would say, stop by stop: a walk of the merged tree in order, taking each
     * node's first label and not descending past a node that speaks — its children were merged
     * into it.
     */
    private fun spokenInTraversalOrder(): List<String> = composeRule.onRoot().fetchSemanticsNode().spoken()

    private fun SemanticsNode.spoken(): List<String> {
        val label =
            config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                ?: config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
        return if (label != null) listOf(label) else children.flatMap { it.spoken() }
    }

    private fun SemanticsNode.descendants(): List<SemanticsNode> = children.flatMap { listOf(it) + it.descendants() }

    /** A window's system bars. */
    private data class Bars(
        val status: Dp,
        val navigation: Dp,
    )

    private companion object {
        /** The design baseline's system bars: a 24dp status bar and a 48dp three-button navigation bar. */
        val THREE_BUTTON = Bars(status = 24.dp, navigation = 48.dp)

        /** A tall phone in gesture navigation: a 24dp status bar and a 24dp gesture bar (15ac). */
        val GESTURE_TALL = Bars(status = 24.dp, navigation = 24.dp)

        /** The Pixel_9 in gesture navigation, QA's device: a 54dp status bar and a 24dp gesture bar (15ac). */
        val PIXEL_9_BARS = Bars(status = 54.dp, navigation = 24.dp)
    }
}

/** The 6.7-inch reference phone (criteria 15g, 15l, 15p, 15y). */
private const val TALL_PHONE = "w412dp-h915dp"

/** The Pixel_9's native window, QA's device (§4.2.5, §4.2.7). */
private const val PIXEL_9_WINDOW = "w411dp-h923dp"

/** §4.2.7's D-03q table on the Pixel_9, by hand: the bound 0.47 × 923 − 16, and C = bound − 54 − 16 − 48. */
private const val PIXEL_9_TITLE_BOUND_DP = 417.8f
private const val PIXEL_9_C_DP = 299.8f

/** The withdrawn stage's two tags (D-03l), which no node may carry (15x). */
private val WITHDRAWN_STAGE_TAGS = listOf("home_stage", "home_stage_art")

/** 15y, 15ac's ±0.5dp. */
private const val TOLERANCE_DP = 0.5f

/** 15g's bound on the footer: a third of S₀. */
private const val THIRDS = 3f
