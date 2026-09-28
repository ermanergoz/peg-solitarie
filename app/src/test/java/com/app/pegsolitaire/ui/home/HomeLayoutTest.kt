package com.app.pegsolitaire.ui.home

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.home.fixture.BOARDS_PILL
import com.app.pegsolitaire.ui.home.fixture.CONTINUE_LABEL
import com.app.pegsolitaire.ui.home.fixture.DOUBLE_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.EARNED_STARS
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.LEVEL_PILL
import com.app.pegsolitaire.ui.home.fixture.PERSISTED_LEVEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CHALLENGE_LABEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CLASSIC_LABEL
import com.app.pegsolitaire.ui.home.fixture.RIBBON_PILLS
import com.app.pegsolitaire.ui.home.fixture.STARS_PILL
import com.app.pegsolitaire.ui.home.fixture.announcement
import com.app.pegsolitaire.ui.home.fixture.appName
import com.app.pegsolitaire.ui.home.fixture.boardsLabel
import com.app.pegsolitaire.ui.home.fixture.boardsValue
import com.app.pegsolitaire.ui.home.fixture.bothResumesState
import com.app.pegsolitaire.ui.home.fixture.challengeTileTitle
import com.app.pegsolitaire.ui.home.fixture.classicTileTitle
import com.app.pegsolitaire.ui.home.fixture.levelLabel
import com.app.pegsolitaire.ui.home.fixture.pillClickAction
import com.app.pegsolitaire.ui.home.fixture.pillDescriptions
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.renderedValues
import com.app.pegsolitaire.ui.home.fixture.ribbonPillCount
import com.app.pegsolitaire.ui.home.fixture.settingsLabel
import com.app.pegsolitaire.ui.home.fixture.starsLabel
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Home's **geometry**: the vertical slack (D-03i, §4.2.2, criteria 15g–15j) and the narrow window
 * class (D-03k, §4.2.3, criteria 15k–15m). The hero row and the stage that D-03l put in the slack
 * (§4.2.4, criteria 15n–15r) are measured in `HomeHeroRowTest` and `HomeStageTest`.
 *
 * Split out of [HomeScreenTest], which proves that what the state carries reaches the screen. This
 * class proves *where* it lands, which is a different question measured with different tools —
 * window qualifiers, supplied insets, and bounds read past `getBoundsInRoot()`'s clipping. Both
 * classes render through the same [renderHome] fixture, so they cannot disagree about the screen
 * they are describing.
 *
 * ## The two idioms this file must never reach for (§13, from the re-audit's MB-1)
 *
 * 1. **Never assert containment as `bottom <= contentBottom()`.** `getBoundsInRoot()` clips to the
 *    root, so that comparison holds by construction and a node pushed entirely off screen reports
 *    `0.0.dp..0.0.dp` and passes at zero. Use [assertAScrollDoesNotMove].
 * 2. **Never measure a short-window property on a zero-inset window.** The narrow class's design
 *    baseline is 568dp of usable height at 360dp width, not 640dp, and the compact column lands
 *    within a few dp of 640 — so a zero-inset reading says "fits" on exactly the window where the
 *    device does not. Supply the insets ([applySystemBarInsets]).
 *
 * The 15g tests below predate both rules and are deliberately left on a zero-inset window: their
 * claim is a *ratio* (a tile's top edge against the midpoint of the same measured area), which the
 * insets scale rather than invalidate, and the root's height there really is the available height.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
class HomeLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---- Home's vertical slack (D-03i, §4.2.2, criteria 15g-15j) -------------

    /**
     * Criterion 15g, the fresh-install state — the player the refresh is for, and the state
     * the device session failed on.
     *
     * With a fixed `Spacer` in a top-aligned scroll column all the slack falls to the *bottom*:
     * QA measured the Classic tile's centre at 1173 px against a 1212 px midline with the
     * bottom 34% of the screen empty, so criterion 15 held only for a player who happened to
     * have both Continue chips. The weighted pair puts the slack above band 3 instead, and the
     * assertion is on the tile's **top** edge, because a tile whose top is below the midpoint
     * is wholly in the lower half however tall it grows.
     *
     * The window is the reference 6.7-inch phone. Robolectric reports zero window insets, so
     * the root's height *is* the available content height the `Scaffold` hands `HomeBands` —
     * which is what "half the Home content height" means in §4.2.2's owed tests.
     */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theFirstModeTileStartsBelowTheMidpointOnAFreshInstall() {
        composeRule.renderHome(state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL))

        assertThat(topOf(classicTileTitle)).isAtLeast(midpointOfContent())
    }

    /** Criterion 15g's other half: the busiest legitimate Home must hold the same line. */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theFirstModeTileStartsBelowTheMidpointWithBothContinueChips() {
        composeRule.renderHome(state = bothResumesState())

        assertThat(topOf(classicTileTitle)).isAtLeast(midpointOfContent())
    }

    /**
     * Criterion 15l's third window, and the other side of the compact band set: at or above
     * `home_compact_threshold_width` nothing D-03k added changes behaviour, so a 6.7-inch phone
     * keeps the full band set — and its slack holds the stage's board.
     *
     * Retargeted, not deleted (ux-spec §13's founder-feedback table): this asserted the hero's
     * decorative art, which D-03l withdrew on every window. 15l's "with the hero's decorative art
     * present" now reads "with the stage art present" (§14), and the full/compact distinction is
     * spacing and pill shape alone.
     *
     * The qualifier is 15l's own `w412dp-h915dp` rather than [TALL_PHONE_QUALIFIER]'s 411dp — the
     * same side of the 400dp boundary, named as the criterion names it so the width term's
     * "nothing at or above 400dp changes" half is measured on the window it was written for.
     */
    @Test
    @Config(qualifiers = WIDE_PHONE_QUALIFIER)
    fun theStageArtIsPresentOnAPhoneAboveTheCompactThreshold() {
        composeRule.renderHome(state = bothResumesState())

        composeRule.onNodeWithTag(HOME_STAGE_ART_TEST_TAG).assertExists()
    }

    /**
     * Criterion 15i (with 14) — at 200% font scale the **slack** is what disappears, not the
     * content: every band is still composed and still reachable, and the column scrolls.
     *
     * This is the case §4.2.2 clause 4 forbids solving with a weighted content band: a weighted
     * child with `fill = true` is measured at exactly its share, so a weighted hero would measure
     * **0** the moment the fixed bands exceed the viewport — clipped to nothing at exactly this
     * font scale.
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun everyBandSurvivesAndStaysReachableAtDoubleFontScale() {
        composeRule.renderHome(state = bothResumesState(), fontScale = DOUBLE_FONT_SCALE)

        listOf(
            appName,
            RESUME_CLASSIC_LABEL,
            RESUME_CHALLENGE_LABEL,
            classicTileTitle,
            challengeTileTitle,
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG).performScrollTo().assertIsDisplayed()
    }

    /**
     * Criterion 15j, first half: the slack is empty space and not a control.
     *
     * The two `Spacer`s carry no semantics at all, so there is no node to interrogate — the honest
     * form of "neither spacer is clickable or focusable" is therefore an exhaustive count of what
     * *is* clickable on the busiest Home. Five, and these five: the Settings gear, two Continue
     * chips, two mode tiles. A control added into the slack fails here.
     */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theOnlyClickableThingsOnHomeAreTheGearTheTwoChipsAndTheTwoTiles() {
        composeRule.renderHome(state = bothResumesState())

        assertThat(composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()).hasSize(HOME_CONTROL_COUNT)
        listOf(RESUME_CLASSIC_LABEL, RESUME_CHALLENGE_LABEL, classicTileTitle, challengeTileTitle)
            .forEach { composeRule.onNodeWithText(it).assertHasClickAction() }
        composeRule.onNodeWithContentDescription(settingsLabel).assertHasClickAction()
    }

    /**
     * Criterion 15j, second half: on a window wider than `content_max_width` the content column
     * is still capped **and centred**, not pushed against one edge.
     *
     * §12 has claimed this since Phase 2 and it stopped being true when `fillMaxSize()` went
     * ahead of `widthIn(max = …)` in the column's chain: ahead of `verticalScroll` it fixes the
     * content's width to the window's, which makes the cap that follows it a no-op (§4.2.2
     * clause 2).
     */
    @Test
    @Config(qualifiers = WIDE_WINDOW_QUALIFIER)
    fun theContentColumnIsCappedAndCentredOnAWindowWiderThanTheCap() {
        composeRule.renderHome(state = bothResumesState())

        val root = composeRule.onRoot().getBoundsInRoot()
        val tile = composeRule.onNodeWithText(classicTileTitle).getBoundsInRoot()
        val cap = with(composeRule.density) { contentMaxWidthPx.toDp() }

        assertThat(tile.right - tile.left).isAtMost(cap)
        // Centred: the two margins agree. A column hugging the leading edge fails the first
        // assertion of the pair, whatever the second says.
        assertThat((tile.left - root.left).value).isWithin(CENTRING_TOLERANCE_DP).of((root.right - tile.right).value)
        assertThat(tile.left).isGreaterThan(root.left)
    }

    // ---- Home on the narrow class (D-03k, §4.2.3, criteria 15k-15m) ----------

    /**
     * Criterion 15k parts 1 and 4 — and the replacement for the assertion the re-audit blocked
     * on (MB-1).
     *
     * ## What was wrong with the assertion this replaces
     *
     * It read `bottomOf(challengeTileTitle) <= contentBottom()` on a zero-inset window, and it
     * was **arithmetically incapable of failing**: `getBoundsInRoot()` returns bounds *clipped to
     * the root*, so `bottom <= root.bottom` holds by construction and a node pushed entirely off
     * screen reports `0.0.dp..0.0.dp` and passes at zero. `assertIsDisplayed()` does not close the
     * gap either — Compose satisfies it on **partial** visibility. Criterion 15h, which it
     * certified, is retired as false (§4.2.3); the tautology would still be a defect if it were
     * not, which is why it is replaced rather than deleted along with the criterion.
     *
     * ## The two idioms §13 now requires instead
     *
     * **Insets are supplied**, so the window is the 568dp a 360x640dp phone with a 24dp status bar
     * and a 48dp three-button navigation bar actually offers, not the 640dp no player holds. And
     * "complete in the first viewport" is asserted as **"a scroll does not move this node"**
     * ([assertAScrollDoesNotMove]), which is a property of the layout rather than of the clipping.
     *
     * Part 4's half that belongs here: no decorative art on the overflowing column — the stage
     * measures 0 and draws nothing (retargeted from "the hero composes no art", which D-03l made
     * true of every window) — and all three ribbon readouts are rendered rather than dropped or
     * truncated to fit.
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun theChipsAndTheFirstTileAreCompleteInTheFirstViewportOnTheNarrowBaseline() {
        composeRule.renderHome(state = bothResumesState())
        composeRule.applySystemBarInsets(top = statusBarInset, bottom = navigationBarInset)

        // The geometry FIRST, and top to bottom: a node that does move cannot then be masked by
        // an earlier scroll, and no cheaper assertion can return before the geometry is read.
        // (The re-audit's probe 3d found the old shape of this test reporting on its hero-art
        // assertion and never reaching its layout one at all.)
        assertAScrollDoesNotMove(composeRule.onNodeWithText(appName))
        assertAScrollDoesNotMove(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG))
        assertAScrollDoesNotMove(composeRule.onNodeWithText(RESUME_CLASSIC_LABEL))
        assertAScrollDoesNotMove(composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL))
        assertAScrollDoesNotMove(composeRule.onNodeWithText(classicTileTitle))

        composeRule.onNodeWithTag(HOME_STAGE_ART_TEST_TAG).assertDoesNotExist()
        assertThat(composeRule.renderedValues())
            .containsAtLeast(EARNED_STARS.toString(), boardsValue, PERSISTED_LEVEL.toString())
    }

    /**
     * Criterion 15k part 2 — the fold cuts through a band and not through a gap.
     *
     * A fold landing in a gap is a **false bottom**, and a false bottom is how a readout becomes
     * hidden without anyone collapsing it. §4.2.3 requires the cut rather than merely permitting
     * it, so this test fails as surely on a tile that sits exactly at the fold as on one pushed
     * entirely below it.
     *
     * Both halves are asserted past `getBoundsInRoot()`'s clipping, which is what makes them
     * decidable. **Part of the tile shows:** it is displayed, and its top edge is inside the
     * viewport. **Part of it does not:** a scroll moves it — the honest form of "its bottom edge
     * is below the fold" once the clipping has replaced that edge with the fold itself.
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun theFoldCutsThroughTheSecondModeTileAndNotThroughAGap() {
        composeRule.renderHome(state = bothResumesState())
        composeRule.applySystemBarInsets(top = statusBarInset, bottom = navigationBarInset)

        val fold = composeRule.onRoot().getBoundsInRoot().bottom - navigationBarInset
        composeRule.onNodeWithText(challengeTileTitle).assertIsDisplayed()
        val unscrolled = composeRule.onNodeWithText(challengeTileTitle).getBoundsInRoot()
        assertThat(unscrolled.top).isLessThan(fold)
        assertThat(unscrolled.top).isAtLeast(statusBarInset)

        val scrolledTop =
            composeRule
                .onNodeWithText(challengeTileTitle)
                .performScrollTo()
                .getBoundsInRoot()
                .top
        assertThat(scrolledTop).isLessThan(unscrolled.top)
    }

    /**
     * Criterion 15k part 3 — one plain scroll completes the screen, and it is a short one.
     *
     * The bound is the criterion's own [NARROW_MAX_OVERFLOW_DP] of total scrollable overflow,
     * under one mode tile, one flick. It is read from the scroll container's own
     * `VerticalScrollAxisRange`, so it is the whole column's overflow rather than one band's edge
     * — §4.2.2 records that the two differ by the column's trailing gap and bottom padding, and
     * that the layout must clear the larger figure.
     *
     * The scroll involves no tap, no expander and no state change: `performScrollTo` is a scroll
     * of the existing column, and the Challenge tile's level chip — oracle 1's readout — is
     * already composed, asserted here by reading it after the scroll.
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun oneShortScrollCompletesHomeOnTheNarrowBaseline() {
        composeRule.renderHome(state = bothResumesState())
        composeRule.applySystemBarInsets(top = statusBarInset, bottom = navigationBarInset)

        assertThat(scrollableOverflow()).isAtMost(NARROW_MAX_OVERFLOW_DP.dp)

        composeRule.onNodeWithText(challengeTileTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(CONTINUE_LABEL).assertIsDisplayed()
    }

    /**
     * Criterion 15l, the width term's whole point: the ~156dp arithmetic error D-03k corrects was
     * **wrap at narrow width, not height**, so a threshold that reads height alone measures the
     * wrong axis.
     *
     * A 360x800dp phone is comfortably above `home_compact_threshold_height` and would have kept
     * the full band set, which at this width is ~872dp against ~728dp of usable height — 144dp
     * over, scrolling. Below `home_compact_threshold_width` the threshold is
     * `home_compact_threshold_height_narrow` instead, so it takes the compact set and fits with
     * slack to spare.
     *
     * Retargeted, not deleted (ux-spec §13's founder-feedback table): the compact set used to be
     * recognisable by its missing hero art, and D-03l withdrew that art on every window, so the
     * full/compact distinction is now spacing and pill shape alone. The pill shape is the one read
     * here — label **beside** value on one line, which the full set's stacked pill cannot do and
     * which only the compact set on the narrow class draws. (This window's slack now draws a small
     * stage board, which is §4.2.4's intent for a taller narrow phone and not a sign of the full
     * set.)
     */
    @Test
    @Config(qualifiers = NARROW_TALL_PHONE_QUALIFIER)
    fun aNarrowTallPhoneIsCompactEvenThoughTheWideThresholdWouldKeepItFull() {
        composeRule.renderHome(state = bothResumesState())

        val label = unmergedBounds(boardsLabel)
        val value = unmergedBounds(boardsValue)
        assertThat(value.top).isLessThan(label.bottom)
        assertThat(label.top).isLessThan(value.bottom)
        assertThat(label.right).isAtMost(value.left)
    }

    /**
     * Criterion 15m — the last yield costs no readout, half one: the pill's label sits **beside**
     * its value on one line, and the decorative glyph is not composed.
     *
     * The glyph carries `contentDescription = null` inside a `clearAndSetSemantics` node, so it
     * has no semantics node in either tree and cannot be asserted absent directly. What is
     * observable is the consequence, and it is the stronger claim anyway: label and value share a
     * line (their vertical extents overlap, which stacked text cannot do) with the label before
     * the value, and the whole band measures no more than [COMPACT_RIBBON_MAX_HEIGHT_DP]. A
     * stacked pill with a 20dp glyph over two text lines fails both.
     *
     * The Boards pill is the one interrogated because its label and its value are both unique
     * strings on the screen — the Stars total and the ladder level are the same number in
     * [bothResumesState], and a locator that matched two nodes would prove nothing.
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun theNarrowClassPillPutsItsLabelBesideItsValueAndComposesNoGlyph() {
        composeRule.renderHome(state = bothResumesState())

        val label = unmergedBounds(boardsLabel)
        val value = unmergedBounds(boardsValue)

        // One line: two stacked texts cannot overlap vertically, and these must.
        assertThat(value.top).isLessThan(label.bottom)
        assertThat(label.top).isLessThan(value.bottom)
        // Label first, which is also the order the pill speaks them in.
        assertThat(label.right).isAtMost(value.left)
        assertThat(ribbonHeight()).isAtMost(COMPACT_RIBBON_MAX_HEIGHT_DP.dp)
    }

    /**
     * Criterion 15m, half two — the half that makes "costs no readout" checkable rather than
     * asserted: the compact pill speaks **exactly** what the full pill speaks.
     *
     * The expected strings are built from the shipped `home_stat_pill_desc` format by the shared
     * `announcement()` helper, so this and [theFullPillSpeaksTheSameSentenceTheCompactPillDoes]
     * assert **byte-identical** oracles on the two window classes. TalkBack cannot tell the two
     * pill shapes apart, and that pair is the proof. The em-dash placeholder rule and the "never
     * 0" rule are unchanged and are pinned in [HomeScreenTest], which runs on Robolectric's
     * default 320x470dp window — narrow and short, so those tests now read the compact pill too.
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun theCompactPillSpeaksItsLabelThenItsValueExactlyOnce() {
        composeRule.renderHome(state = bothResumesState())

        assertEveryPillSpeaksOnce()
    }

    /** The other side of the pair above, on the full pill: the same three sentences, verbatim. */
    @Test
    @Config(qualifiers = WIDE_PHONE_QUALIFIER)
    fun theFullPillSpeaksTheSameSentenceTheCompactPillDoes() {
        composeRule.renderHome(state = bothResumesState())

        assertEveryPillSpeaksOnce()
    }

    /**
     * The three sentences, and the absence of a click action, asserted identically on both window
     * classes — one body so the two shapes cannot be checked against two different expectations,
     * which is the whole content of "the yield costs no readout".
     */
    private fun assertEveryPillSpeaksOnce() {
        assertThat(composeRule.ribbonPillCount()).isEqualTo(RIBBON_PILLS)
        assertThat(composeRule.pillDescriptions(STARS_PILL))
            .containsExactly(announcement(starsLabel, EARNED_STARS.toString()))
        assertThat(composeRule.pillDescriptions(BOARDS_PILL))
            .containsExactly(announcement(boardsLabel, boardsValue))
        assertThat(composeRule.pillDescriptions(LEVEL_PILL))
            .containsExactly(announcement(levelLabel, PERSISTED_LEVEL.toString()))
        repeat(RIBBON_PILLS) { index -> assertThat(composeRule.pillClickAction(index)).isNull() }
    }

    // ---- Measurement helpers -------------------------------------------------

    private fun topOf(text: String): Dp = composeRule.onNodeWithText(text).getBoundsInRoot().top

    /**
     * "This node is complete in the first viewport", asserted as the only form of it that can
     * fail: **a scroll does not move it** (§13, and `ux-spec.md:1512-1514`).
     *
     * `performScrollTo` on a node already wholly inside its scroll container's viewport is a
     * no-op, so its bounds are unchanged; on a node that is clipped — even by one dp — the
     * container scrolls it into view and the bounds move. Containment cannot be asserted
     * directly, because `getBoundsInRoot()` clips to the root and therefore reports the viewport's
     * own edge for any node that crosses it, and zero for one entirely past it.
     */
    private fun assertAScrollDoesNotMove(node: SemanticsNodeInteraction) {
        val before = node.getBoundsInRoot()
        val after = node.performScrollTo().getBoundsInRoot()

        assertThat(after.top).isEqualTo(before.top)
        assertThat(after.bottom).isEqualTo(before.bottom)
    }

    /**
     * The column's **total** scrollable overflow, read from the scroll container's own
     * `VerticalScrollAxisRange` rather than from any band's edge.
     *
     * §4.2.2 records why the distinction matters: the last band's bottom edge against usable
     * height and the column's own requirement differ by the trailing gap and the bottom padding,
     * and the layout must clear the larger of the two. This is the larger of the two.
     */
    private fun scrollableOverflow(): Dp {
        val range =
            composeRule
                .onNode(hasScrollAction())
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
        return with(composeRule.density) { range.maxValue().toDp() }
    }

    /** The progress ribbon band's measured height — criterion 15m's ceiling is on this. */
    private fun ribbonHeight(): Dp {
        val bounds = composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG).getBoundsInRoot()
        return bounds.bottom - bounds.top
    }

    /**
     * A rendered `Text`'s own bounds, read from the **unmerged** tree: a pill clears and sets one
     * description, so its label and its value are invisible to the merged tree by design.
     */
    private fun unmergedBounds(text: String): DpRect =
        composeRule.onNode(hasText(text), useUnmergedTree = true).getBoundsInRoot()

    /**
     * Half the height of Home's content area.
     *
     * Robolectric delivers no window insets, so the `Scaffold`'s content padding is zero and the
     * root's height is exactly the available height `HomeBands` measures its slack against — which
     * is what §4.2.2 means by "the available height". On a device the insets are subtracted first,
     * by the `Scaffold`, before the threshold or the weights see it.
     */
    private fun midpointOfContent(): Dp {
        val root = composeRule.onRoot().getBoundsInRoot()
        return (root.bottom - root.top) / HALVES
    }

    private val contentMaxWidthPx: Int
        get() = RuntimeEnvironment.getApplication().resources.getDimensionPixelSize(R.dimen.content_max_width)

    /** The design baseline's own insets (`ux-spec.md:1170-1178`): 24dp status, 48dp navigation. */
    private val statusBarInset: Dp get() = STATUS_BAR_INSET_DP.dp

    private val navigationBarInset: Dp get() = NAVIGATION_BAR_INSET_DP.dp
}

/**
 * The reference 6.7-inch phone (criterion 15/15g), and the narrow baseline the first-paint rule is
 * written against (criteria 15a/15k). The short one is below both compact thresholds; the tall one
 * is above `home_compact_threshold_height` and at or above `home_compact_threshold_width`, so it
 * takes the full band set.
 */
private const val TALL_PHONE_QUALIFIER = "w411dp-h915dp"
private const val SHORT_PHONE_QUALIFIER = "w360dp-h640dp"

/**
 * Criterion 15l's own three windows. `w412dp-h915dp` is the wide class the criterion names, and
 * `w360dp-h800dp` is the window the width term exists for: above `home_compact_threshold_height`
 * and therefore full under D-03i, compact under D-03k because the bands wrap at 360dp whatever the
 * height (§4.2.3). The third is [SHORT_PHONE_QUALIFIER].
 */
private const val WIDE_PHONE_QUALIFIER = "w412dp-h915dp"
private const val NARROW_TALL_PHONE_QUALIFIER = "w360dp-h800dp"

/** Wider than `content_max_width` 480dp, which is the only window where the cap is visible. */
private const val WIDE_WINDOW_QUALIFIER = "w800dp-h915dp"

/**
 * The design baseline's system-bar insets (`ux-spec.md:1170-1178`, and §4.2.2's 568dp table): a
 * 24dp status bar and a 48dp three-button navigation bar, which is the **deeper** of the two
 * bottom cases and so the honest one to design against. Robolectric reports neither, which is why
 * criterion 15k requires them to be supplied and makes a zero-inset reading void.
 */
private const val STATUS_BAR_INSET_DP = 24
private const val NAVIGATION_BAR_INSET_DP = 48

/** Criterion 15k part 3: under one mode tile, one flick. */
private const val NARROW_MAX_OVERFLOW_DP = 120

/** Criterion 15m: the compact-narrow ribbon band's design target at default font scale. */
private const val COMPACT_RIBBON_MAX_HEIGHT_DP = 56

/** Two halves of a screen — the midpoint criterion 15g is asserted against. */
private const val HALVES = 2f

/**
 * Everything on the busiest Home a player can press: the Settings gear, two Continue chips, two
 * mode tiles. The ribbon's three pills are deliberately not among them (§5), and neither spacer
 * may ever be (criterion 15j).
 */
private const val HOME_CONTROL_COUNT = 5

/**
 * Slack the centring assertion allows, in dp: a cap of an odd number of pixels cannot be centred
 * exactly, and rounding may put one extra pixel on either side.
 */
private const val CENTRING_TOLERANCE_DP = 1f
