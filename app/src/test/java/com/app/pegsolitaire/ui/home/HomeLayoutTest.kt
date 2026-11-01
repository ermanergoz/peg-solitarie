package com.app.pegsolitaire.ui.home

import android.graphics.Bitmap
import androidx.annotation.DimenRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import com.app.pegsolitaire.R
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.backdrop.BackdropMetrics
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
import com.app.pegsolitaire.ui.home.fixture.columnBoundsOf
import com.app.pegsolitaire.ui.home.fixture.levelLabel
import com.app.pegsolitaire.ui.home.fixture.pillClickAction
import com.app.pegsolitaire.ui.home.fixture.pillDescriptions
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.renderedValues
import com.app.pegsolitaire.ui.home.fixture.ribbonPillCount
import com.app.pegsolitaire.ui.home.fixture.settingsLabel
import com.app.pegsolitaire.ui.home.fixture.starsLabel
import com.app.pegsolitaire.ui.theme.DarkColors
import com.app.pegsolitaire.ui.theme.FinishedContainer
import com.app.pegsolitaire.ui.theme.LightColors
import com.app.pegsolitaire.ui.theme.finishGlow
import com.app.pegsolitaire.ui.theme.glossFinish
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Home's **geometry**: the vertical slack (D-03i, §4.2.2, criteria 15g–15j) and the narrow window
 * class (D-03k, §4.2.3, criteria 15k–15m). The hero row (§4.2.4, criteria 15n and 15o) is measured in
 * `HomeHeroRowTest`, and the headroom D-03p put above it in place of the withdrawn stage, with its title
 * bound (§4.2.7, criteria 15x, 15y and 15ac), in `HomeHeadroomTest`.
 *
 * Split out of [HomeScreenTest], which proves that what the state carries reaches the screen. This
 * class proves *where* it lands, which is a different question measured with different tools —
 * window qualifiers, supplied insets, and bounds read past `getBoundsInRoot()`'s clipping. Both
 * classes render through the same [renderHome] fixture, so they cannot disagree about the screen
 * they are describing.
 *
 * ## Measured on NATIVE graphics, in the app's own faces
 *
 * Every band here wraps and measures as the bundled Fredoka and Nunito make it on a device: the
 * class runs under NATIVE graphics and [renderHome] draws Home in the app's own theme, light unless
 * a test states dark.
 * Robolectric's default LEGACY mode fakes text metrics, and it over-wraps: on 15k's own window it
 * measured 44dp of overflow, with the Challenge tile cut by the fold, where NATIVE and the device
 * both measure **0** (the H-1 audit's F-1). A geometry claim read off fake metrics is MB-1's class
 * of defect — a JVM reading the device contradicts.
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
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    /**
     * The pill rects that frame 2 of criterion 15m's pixel read lays the finish over
     * ([ReferenceFinishes]). Empty while frame 1 is taken, so frame 1 is Home alone.
     */
    private val referenceFinishes = mutableStateOf(emptyList<IntRect>())

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
     * keeps the full band set.
     *
     * **"Full" is read from the band spacing**, and from nothing drawn (15l's D-03p pointer: "full, by
     * its band spacing"). The two sets differ at this width in one thing only: `spacing_md` between the
     * bands and around the column, where the compact set has `spacing_sm`. The pill shape cannot tell
     * them apart here, since the compact pill needs the narrow class as well as the compact set, and
     * since D-03p nothing is drawn in the slack to tell them apart either. So the gap between the two
     * mode tiles, one band gap with nothing inside it, must be the full set's. A set forced compact at
     * or above 400dp halves it (the H-1 audit's M5).
     *
     * Retargeted twice, never deleted (ux-spec §13): this asserted the hero's decorative art, which
     * D-03l withdrew on every window, then the stage art D-03l put in the slack, which D-03p withdrew
     * in its turn; the band-gap read is the half that stands.
     *
     * The qualifier is 15l's own `w412dp-h915dp` rather than [TALL_PHONE_QUALIFIER]'s 411dp — the
     * same side of the 400dp boundary, named as the criterion names it so the width term's
     * "nothing at or above 400dp changes" half is measured on the window it was written for.
     */
    @Test
    @Config(qualifiers = WIDE_PHONE_QUALIFIER)
    fun aWidePhoneKeepsTheFullBandSetByItsBandSpacing() {
        composeRule.renderHome(state = bothResumesState())

        assertWithMessage("the band gap between the two mode tiles, in dp")
            .that(gapBetweenTheModeTiles().value)
            .isWithin(BAND_GAP_TOLERANCE_DP)
            .of(dimen(R.dimen.spacing_md).value)
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
    //
    // Criterion 15k is written for a 360x640dp phone, and on real text metrics that phone's
    // column FITS: every band whole, 30dp of clear space below the Challenge tile, 0dp of overflow
    // (NATIVE here, and the device in QA's loop 3). Parts 2 and 3 are stated conditionally on the
    // column overflowing, so on that window their overflow halves cannot fail. Each 15k test
    // therefore runs twice: on the criterion's own window, where the conditional is read as it
    // falls, and on OVERFLOWING_PHONE_QUALIFIER, where the column must overflow into a band, so
    // the overflow halves are the ones being read.

    /**
     * Criterion 15k parts 1 and 4 on the criterion's own window — and the replacement for the
     * assertion the re-audit blocked on (MB-1).
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
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun theChipsAndTheFirstTileAreCompleteInTheFirstViewportOnTheNarrowBaseline() {
        renderTheNarrowClassWithInsets()

        assertTheFirstViewportHoldsEverythingAboveTheSecondTile()
    }

    /**
     * Criterion 15k parts 1 and 4 where the column overflows, which is where part 1 can fail on
     * real metrics: a band grown taller pushes the first tile across the fold here long before it
     * does on the criterion's own window.
     */
    @Test
    @Config(qualifiers = OVERFLOWING_PHONE_QUALIFIER)
    fun theChipsAndTheFirstTileAreCompleteInTheFirstViewportOnANarrowPhoneThatOverflows() {
        renderTheOverflowingNarrowWindow()

        assertTheFirstViewportHoldsEverythingAboveTheSecondTile()
    }

    /**
     * Criterion 15k part 2 on the criterion's own window, read **conditionally**, as the criterion
     * states it: if the column overflows into a band, the fold cuts through that band and not
     * through a gap; if every band fits, there is no fold to place and nothing may hide below it.
     * On real metrics this window fits, so the second branch is the one read here — and it is a
     * real assertion, not a pass by default (see [assertTheFoldLeavesNoFalseBottom]).
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun theFoldLeavesNoFalseBottomOnTheNarrowBaseline() {
        renderTheNarrowClassWithInsets()

        assertTheFoldLeavesNoFalseBottom()
    }

    /**
     * Criterion 15k part 2's overflow branch, kept live: on a narrow phone whose column does
     * overflow, the fold cuts through the second mode tile — its top edge above the fold, its
     * bottom edge below it — so Home presents no false bottom.
     */
    @Test
    @Config(qualifiers = OVERFLOWING_PHONE_QUALIFIER)
    fun theFoldCutsThroughTheSecondModeTileOnANarrowPhoneThatOverflows() {
        renderTheOverflowingNarrowWindow()

        assertTheFoldLeavesNoFalseBottom()
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
     *
     * Both branches of 15k hold here at once: where the column fits, the overflow is 0 and the
     * scroll moves nothing, which is this window on real metrics; where it overflows, the bound
     * and the scroll are read for real, which is [OVERFLOWING_PHONE_QUALIFIER].
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun oneShortScrollCompletesHomeOnTheNarrowBaseline() {
        renderTheNarrowClassWithInsets()

        assertOneShortScrollCompletesHome()
    }

    /** Criterion 15k part 3 where the scroll has something to do: the bound and the reveal, read live. */
    @Test
    @Config(qualifiers = OVERFLOWING_PHONE_QUALIFIER)
    fun oneShortScrollCompletesHomeOnANarrowPhoneThatOverflows() {
        renderTheOverflowingNarrowWindow()

        assertOneShortScrollCompletesHome()
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
     * which only the compact set on the narrow class draws.
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
     * The shape is read on the Boards pill, whose label and value are both unique strings on the
     * screen — the Stars total and the ladder level are the same number in [bothResumesState], and
     * a locator that matched two nodes would prove nothing. Label and value share a line (their
     * vertical extents overlap, which stacked text cannot do), the label first, and the whole band
     * measures no more than [COMPACT_RIBBON_MAX_HEIGHT_DP].
     *
     * **None of that sees a glyph.** Put back inline before the label, a 20dp glyph changes neither
     * the line nor the band's height (the H-1 audit's M3b, under which every Home suite stayed
     * green), and as `contentDescription = null` decoration it leaves no description and no text in
     * either semantics tree. What a glyph cannot avoid is being drawn, so the absence is read off
     * the pixels ([assertEachPillPaintsOnlyItsSurfaceAndItsTexts]), on all three pills: here in
     * light, and in dark in [theNarrowClassPillComposesNoGlyphInTheDarkThemeEither].
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun theNarrowClassPillPutsItsLabelBesideItsValueAndComposesNoGlyph() {
        renderHomeUnderItsReferenceFinishes(darkTheme = false)

        val label = unmergedBounds(boardsLabel)
        val value = unmergedBounds(boardsValue)

        // One line: two stacked texts cannot overlap vertically, and these must.
        assertThat(value.top).isLessThan(label.bottom)
        assertThat(label.top).isLessThan(value.bottom)
        // Label first, which is also the order the pill speaks them in.
        assertThat(label.right).isAtMost(value.left)
        assertThat(ribbonHeight()).isAtMost(COMPACT_RIBBON_MAX_HEIGHT_DP.dp)
        // No glyph: inside its padding, each pill paints its finished surface and its two texts,
        // nothing else.
        assertEachPillPaintsOnlyItsSurfaceAndItsTexts(darkTheme = false)
    }

    /**
     * Criterion 15m's pixel half in the dark theme (B-19a, property 1). B-19's gloss differs by
     * theme, 6% white in dark against 70% in light, and the M3b glyph is lighter than the dark
     * fill: the case a loose tolerance would miss (B-19a's red 3). The one-line shape and the band's
     * height do not depend on the theme, so they are read in light only.
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun theNarrowClassPillComposesNoGlyphInTheDarkThemeEither() {
        renderHomeUnderItsReferenceFinishes(darkTheme = true)

        assertEachPillPaintsOnlyItsSurfaceAndItsTexts(darkTheme = true)
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
     * Criterion 15m read from the other side, at or above 400dp (the close report's INFO row 3; the
     * loop-5 audit's M5c): the **full** pill paints its decorative glyph beside its label and its
     * value, on all three pills.
     *
     * Until this test, the full pill was guarded only by accident: with the compact pill forced onto
     * every window (M5c) every `HomeLayoutTest` case stayed green, and one `HomeStageTest` (now
     * `HomeHeadroomTest`) gap caught it by a single dp. This is
     * [assertEachPillPaintsOnlyItsSurfaceAndItsTexts]'s pixel read turned
     * round: inside each pill's padding and outside its two texts, the pill must paint something
     * besides its finished surface — the glyph. "Besides" is read against frame 2's reference finish
     * on the same pixels ([inkBesideTheTextsOfEachPill]), never against the flat fill: B-19's gloss
     * differs from the flat fill across the top of every pill, so a flat read counts the gloss as ink
     * and passes with the glyph deleted (the code-auditor's M1). The floor is far under what the
     * glyphs measure here (the H-1 probe read 196 px and 85 px on the Boards and Level pills), so it
     * fails on an absent glyph and on nothing else. Here in light, and in dark in
     * [onAWidePhoneEveryFullPillPaintsItsGlyphInTheDarkThemeToo].
     */
    @Test
    @Config(qualifiers = WIDE_PHONE_QUALIFIER)
    fun onAWidePhoneEveryFullPillPaintsItsGlyphBesideItsTexts() {
        renderHomeUnderItsReferenceFinishes(darkTheme = false)

        assertEachPillPaintsItsGlyph(darkTheme = false)
    }

    /**
     * The full pill's glyph in the dark theme, read the same way: B-19's gloss is 6% white there
     * against 70% in light, so the read must see the glyph under both (B-19a, property 1).
     */
    @Test
    @Config(qualifiers = WIDE_PHONE_QUALIFIER)
    fun onAWidePhoneEveryFullPillPaintsItsGlyphInTheDarkThemeToo() {
        renderHomeUnderItsReferenceFinishes(darkTheme = true)

        assertEachPillPaintsItsGlyph(darkTheme = true)
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

    // ---- Criterion 15k, asserted once and read on two windows ------------------

    /** Home's busiest state on the current window, with the design baseline's system bars supplied. */
    private fun renderTheNarrowClassWithInsets() {
        composeRule.renderHome(state = bothResumesState())
        composeRule.applySystemBarInsets(top = statusBarInset, bottom = navigationBarInset)
    }

    /**
     * [renderTheNarrowClassWithInsets] on [OVERFLOWING_PHONE_QUALIFIER], with the precondition that
     * gives that window its job: the column overflows past its own trailing space, so the fold
     * cuts into a band and 15k's overflow halves are the ones read. If a layout change ever lets
     * the column fit here, this fails rather than letting those halves pass unread — pick a
     * shorter window then.
     */
    private fun renderTheOverflowingNarrowWindow() {
        renderTheNarrowClassWithInsets()
        assertWithMessage(
            "$OVERFLOWING_PHONE_QUALIFIER keeps 15k's overflow branch live, so its column must overflow past its " +
                "trailing space into a band",
        ).that(scrollableOverflow()).isGreaterThan(compactTrailingSpace)
    }

    /**
     * Criterion 15k part 1, and part 4's halves that belong with it, on the current window.
     *
     * **Part 1:** the hero row, the whole ribbon, both Continue chips and the first mode tile are
     * complete in the first viewport, with no scrolling — top to bottom, and the geometry FIRST, so
     * a node that does move cannot be masked by an earlier scroll and no cheaper assertion can
     * return before the geometry is read (the re-audit's probe 3d found an older shape of this
     * test reporting on its art assertion and never reaching its layout one at all).
     *
     * **Part 4:** all three ribbon readouts are rendered rather than dropped or truncated to fit,
     * and an overflowing column has no headroom: its hero row's top edge is at the column's top padding
     * (§13's D-03p row retargets "no stage art on an overflowing column", itself D-03l's retarget of
     * "the hero shows no decorative art", to this). That half is conditional on the column overflowing
     * at all, trailing space included: the headroom is the slack's, so any overflow leaves it nothing,
     * while a column that fits may hand it slack above the hero row, which is D-03p's intent rather than
     * a defect.
     */
    private fun assertTheFirstViewportHoldsEverythingAboveTheSecondTile() {
        assertAScrollDoesNotMove(composeRule.onNodeWithText(appName))
        assertAScrollDoesNotMove(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG))
        assertAScrollDoesNotMove(composeRule.onNodeWithText(RESUME_CLASSIC_LABEL))
        assertAScrollDoesNotMove(composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL))
        assertAScrollDoesNotMove(composeRule.onNodeWithText(classicTileTitle))

        assertThat(composeRule.renderedValues())
            .containsAtLeast(EARNED_STARS.toString(), boardsValue, PERSISTED_LEVEL.toString())
        if (scrollableOverflow() > 0.dp) {
            val hero = composeRule.onNodeWithTag(HOME_HERO_TEST_TAG, useUnmergedTree = true)
            val heroTop = composeRule.columnBoundsOf(hero).top
            assertWithMessage("an overflowing column has no headroom: the hero row's top against its padding, in dp")
                .that((heroTop - statusBarInset).value)
                .isWithin(BAND_GAP_TOLERANCE_DP)
                .of(gapBetweenTheModeTiles().value)
        }
    }

    /**
     * Criterion 15k part 2, as the criterion states it — **conditionally**: if the column overflows
     * into a band, the fold cuts through that band and not through a gap.
     *
     * It is read on the second mode tile, the last band, because part 1 holds every band above it
     * complete. In **both** branches the tile's top edge is inside the first viewport — it is
     * displayed, and its clipped top lies between the status bar and the fold — because a fold
     * above that edge is a fold through the gap above the tile, which is a false bottom whether or
     * not anything else overflows. A scroll then decides the branch:
     *
     * - **It moves the tile:** part of the tile was past the fold, so the fold cut through it — top
     *   above the fold, bottom below it. A false bottom is how a readout becomes hidden without
     *   anyone collapsing it, and this is the branch that rules one out.
     * - **It does not:** every band is whole, and the only thing that may lie past the fold is the
     *   column's own trailing space ([compactTrailingSpace]). Anything more is something hidden
     *   below a gap, which this branch fails.
     *
     * ## The trailing space is not a band (the H-1 audit's LEGACY ruling, item 5)
     *
     * The column's scrollable overflow includes the gap after its last band and its bottom padding.
     * So a column whose last band ends within that distance of the fold overflows with nothing
     * hidden, and read literally, "if the column overflows at all, the fold cuts through a band"
     * fails it for a fold that hides nothing. That reading is rejected on purpose: §4.2.3 defines a
     * false bottom as a fold in a gap **between two bands**, which hides the lower one, and below
     * the trailing space there is no band. So "overflows" here means "overflows into a band", and a
     * column that overflows by its trailing space alone is read as fitting — every band whole,
     * nothing below the fold but space. (S9's full geometry measured exactly that on NATIVE: 12dp
     * of overflow below a wholly visible tile.)
     */
    private fun assertTheFoldLeavesNoFalseBottom() {
        val fold = composeRule.onRoot().getBoundsInRoot().bottom - navigationBarInset
        val lastBand = composeRule.onNodeWithText(challengeTileTitle)

        lastBand.assertIsDisplayed()
        val unscrolled = lastBand.getBoundsInRoot()
        assertThat(unscrolled.top).isAtLeast(statusBarInset)
        assertThat(unscrolled.top).isLessThan(fold)

        val scrolledTop = lastBand.performScrollTo().getBoundsInRoot().top
        if (scrolledTop == unscrolled.top) {
            assertWithMessage("every band is whole, so only the column's trailing space may lie past the fold")
                .that(scrollableOverflow())
                .isAtMost(compactTrailingSpace)
        } else {
            assertWithMessage("the scroll that completes the cut tile moves it up")
                .that(scrolledTop)
                .isLessThan(unscrolled.top)
        }
    }

    /** Criterion 15k part 3 on the current window: see [oneShortScrollCompletesHomeOnTheNarrowBaseline]. */
    private fun assertOneShortScrollCompletesHome() {
        assertThat(scrollableOverflow()).isAtMost(NARROW_MAX_OVERFLOW_DP.dp)

        composeRule.onNodeWithText(challengeTileTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(CONTINUE_LABEL).assertIsDisplayed()
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

    /**
     * Home's busiest state on the current window, in [darkTheme], with frame 2's reference finishes
     * composed over it ([ReferenceFinishes]): none until [inkBesideTheTextsOfEachPill] asks for them,
     * so until then the screen is exactly what [renderHome] draws.
     */
    private fun renderHomeUnderItsReferenceFinishes(darkTheme: Boolean) {
        composeRule.renderHome(
            state = bothResumesState(),
            darkTheme = darkTheme,
            overlay = { ReferenceFinishes(referenceFinishes.value) },
        )
    }

    /**
     * Criterion 15m's "no decorative glyph", read where a glyph cannot hide: in what the pill paints
     * (B-19a; the tech-lead's 15m oracle, O1).
     *
     * **First, what the pill lays out as text** (the close report's INFO row 1; the loop-5 audit's
     * M3d), checked once G1 has found the three pills and ahead of the rest of G1–G5. The pixel read
     * skips every node with text semantics, so a glyph drawn as text — a `Text("★")` before the label
     * — is skipped with the label and passes it. Each pill must therefore lay out exactly two texts,
     * its label and then its value, before its pixels count.
     *
     * Then each pill is read inside its own padding against its own finished surface, in the theme
     * Home is drawn in ([darkTheme]): [inkBesideTheTextsOfEachPill], which holds guards G1–G4. Every
     * pixel read there must be that surface; any other is something else the pill paints.
     *
     * **G5** is the comparison. The strays of all three pills are collected before anything is
     * asserted, so a red names every pill it reaches.
     */
    private fun assertEachPillPaintsOnlyItsSurfaceAndItsTexts(darkTheme: Boolean) {
        val expectedTexts =
            listOf(
                listOf(starsLabel, EARNED_STARS.toString()),
                listOf(boardsLabel, boardsValue),
                listOf(levelLabel, PERSISTED_LEVEL.toString()),
            )
        ribbonPills().forEachIndexed { index, pill ->
            assertWithMessage("pill $index: the texts it lays out, in order")
                .that(pill.textsWithin().map(::textOf))
                .containsExactlyElementsIn(expectedTexts[index])
                .inOrder()
        }

        val strays = inkBesideTheTextsOfEachPill(darkTheme)
        assertWithMessage(
            "G5: pixels each pill paints besides its finished surface and its texts: " +
                strays.withIndex().joinToString("; ") { (index, pill) ->
                    "pill $index ${pill.ink.size} px, ${spanOf(pill.ink)}${rowsBelowTheTop(pill.ink, pill.rect)}"
                },
        ).that(strays.withIndex().associate { (index, pill) -> index to pill.ink.size })
            .isEqualTo((0 until RIBBON_PILLS).associateWith { 0 })
    }

    /**
     * Each full pill paints at least [GLYPH_INK_FLOOR_PX] of ink beside its texts, in [darkTheme]: its
     * glyph. Every pill's ink is read before anything is asserted, so a red names every pill it reaches.
     */
    private fun assertEachPillPaintsItsGlyph(darkTheme: Boolean) {
        val pills = inkBesideTheTextsOfEachPill(darkTheme)
        assertWithMessage(
            "pills painting under $GLYPH_INK_FLOOR_PX px besides their finished surface and their texts: " +
                pills.withIndex().joinToString("; ") { (index, pill) ->
                    "pill $index ${pill.ink.size} px, ${spanOf(pill.ink)}"
                },
        ).that(pills.withIndex().filter { (_, pill) -> pill.ink.size < GLYPH_INK_FLOOR_PX }.map { it.index })
            .isEmpty()
    }

    /**
     * For each pill, left to right, the pixels it paints besides its finished surface and its texts
     * (B-19a; the tech-lead's 15m oracle, O1), in the theme Home is drawn in ([darkTheme]).
     *
     * Each pill is read inside its own padding (`spacing_sm`, the box its content is laid out in),
     * skipping its label's and its value's own bounds, each widened by [TEXT_INK_MARGIN_DP] for the
     * text's anti-aliased edge. The texts are found by their text semantics alone, so a glyph that
     * did carry a node would still be read.
     *
     * A pixel read is ink when it is not **the pill's finished surface at that pixel**: its
     * `surfaceContainerHigh` fill with B-19's gloss over it, and the rim and lip wherever the read
     * meets them (at an 8dp inset it meets neither). That surface is drawn, not modelled. Frame 1 is
     * Home. Frame 2 is the same composition with [ReferenceFinishes] laid over each pill on that
     * pill's own pixels: the pill's finish modifiers and nothing inside them. Same modifiers, same
     * size, same pixels, so Skia's ordered dither and the edge anti-aliasing fall in the same phase in
     * both frames, and a read pixel is ink when frame 1 differs from frame 2 by more than
     * [PAINT_TOLERANCE]. Both frames are the flat render ([renderedWindow]), where no elevation shadow
     * reaches into a pill. No golden is stored (D4). Against the flat fill instead, B-19's gloss would
     * count as ink across the top band of every pill, with or without a glyph (the code-auditor's M1).
     *
     * The guards, in this order, each with its own message:
     * - **G1** the ribbon has [RIBBON_PILLS] pills, and every edge of each lies on a whole pixel, so a
     *   reference can sit on the same pixels. A fractional edge fails; it is never rounded silently.
     * - **G2** in frame 2 each reference is placed once, on its pill's rect exactly.
     * - **G3** frame 2 shows the finish alone: across each pill's whole padded box, texts included,
     *   every row is flat within [PAINT_TOLERANCE], as a flat fill under a vertical gradient is. A
     *   frame 2 that did not draw, still shows the texts, or carries a glyph of its own breaks a row;
     *   without this guard such a frame 2 would equal frame 1, and every glyph would pass.
     * - **G4** below the gloss band ([BackdropMetrics.FINISH_GLOSS_FADE] of the pill's height) frame 2
     *   is the theme's `surfaceContainerHigh`, so there the read still checks against the token.
     */
    private fun inkBesideTheTextsOfEachPill(darkTheme: Boolean): List<PillInk> {
        val padding = with(composeRule.density) { dimen(R.dimen.spacing_sm).toPx() }
        val inkMargin = with(composeRule.density) { TEXT_INK_MARGIN_DP.dp.toPx() }
        val reads =
            ribbonPills().mapIndexed { index, pill ->
                val texts = pill.textsWithin().map { it.boundsInRoot.inflate(inkMargin) }
                val box = pill.boundsInRoot.deflate(padding)
                PillRead(
                    rect = pill.boundsInRoot.onWholePixels("pill $index"),
                    box = box,
                    read = box.wholePixels().filter { pixel -> texts.none { it.contains(pixel.centre()) } },
                ).also { assertWithMessage("pill $index: pixels read outside its texts").that(it.read).isNotEmpty() }
            }
        val home = composeRule.renderedWindow()

        referenceFinishes.value = reads.map { it.rect }
        composeRule.waitForIdle()
        reads.forEachIndexed { index, pill -> assertTheReferenceLiesOn(index, pill.rect) }
        val finish = composeRule.renderedWindow()
        reads.forEachIndexed { index, pill ->
            assertTheReferenceIsFlatAlongEveryRow(index, pill, finish)
            assertTheReferenceIsTheTokenBelowTheGloss(index, pill, finish, darkTheme)
        }

        return reads.map { pill ->
            PillInk(
                rect = pill.rect,
                ink = pill.read.filterNot { isPaintedIn(home.getPixel(it.x, it.y), finish.getPixel(it.x, it.y)) },
            )
        }
    }

    /** The ribbon's three pills, left to right, in the unmerged tree where their texts are children (G1). */
    private fun ribbonPills(): List<SemanticsNode> {
        val pills =
            composeRule
                .onNode(hasTestTag(HOME_PROGRESS_RIBBON_TEST_TAG), useUnmergedTree = true)
                .fetchSemanticsNode()
                .children
        assertWithMessage("G1: the ribbon's pills").that(pills).hasSize(RIBBON_PILLS)
        return pills
    }

    /** G2: frame 2 composes reference [index] once, and on exactly [pill]'s pixels. */
    private fun assertTheReferenceLiesOn(
        index: Int,
        pill: IntRect,
    ) {
        val placed =
            composeRule
                .onAllNodes(hasTestTag(REFERENCE_FINISH_TAG_PREFIX + index), useUnmergedTree = true)
                .fetchSemanticsNodes()
        assertWithMessage("G2: frame 2 composes reference $index once").that(placed).hasSize(1)
        assertWithMessage("G2: reference $index lies on pill $index's pixels (px)")
            .that(placed.single().boundsInRoot)
            .isEqualTo(pill.toRect())
    }

    /**
     * G3: in frame 2, every row of [pill]'s padded box, texts included, is flat within
     * [PAINT_TOLERANCE] in every channel: the finish alone, with nothing of the pill showing through.
     */
    private fun assertTheReferenceIsFlatAlongEveryRow(
        index: Int,
        pill: PillRead,
        finish: Bitmap,
    ) {
        val spreads =
            pill.box.wholePixels().groupBy { it.y }.mapValues { (_, row) ->
                COLOUR_CHANNEL_SHIFTS.maxOf { shift ->
                    row.maxOf { finish.channel(it, shift) } - row.minOf { finish.channel(it, shift) }
                }
            }
        val widest = spreads.entries.maxBy { it.value }
        assertWithMessage(
            "G3: frame 2 must show pill $index's finish alone, flat along every row, but row y ${widest.key} " +
                "(px) spreads ${widest.value} levels in one channel: frame 2 still shows its texts, or its " +
                "reference was not drawn or carries a glyph",
        ).that(widest.value)
            .isAtMost(PAINT_TOLERANCE)
    }

    /**
     * G4: below the gloss band, frame 2 paints [pill]'s padded box in the theme's own
     * `surfaceContainerHigh`: the token the flat-fill read was anchored to, kept where the gloss is gone.
     */
    private fun assertTheReferenceIsTheTokenBelowTheGloss(
        index: Int,
        pill: PillRead,
        finish: Bitmap,
        darkTheme: Boolean,
    ) {
        val token = (if (darkTheme) DarkColors else LightColors).surfaceContainerHigh.toArgb()
        val glossEnd = pill.rect.top + BackdropMetrics.FINISH_GLOSS_FADE * pill.rect.height
        val belowTheGloss = pill.box.wholePixels().filter { it.centre().y >= glossEnd }
        val off = belowTheGloss.filterNot { isPaintedIn(finish.getPixel(it.x, it.y), token) }

        assertWithMessage("G4: pill $index: rows read below its gloss band").that(belowTheGloss).isNotEmpty()
        assertWithMessage(
            "G4: below its gloss band frame 2 must paint pill $index in the theme's surfaceContainerHigh, " +
                "but ${off.size} px differ, ${spanOf(off)}",
        ).that(off.size)
            .isEqualTo(0)
    }

    /**
     * This rect as whole pixels, which G1 requires: within [WHOLE_PIXEL_SLACK] of a whole pixel at
     * every edge, so a reference sized and placed in px covers exactly the same pixels.
     */
    private fun Rect.onWholePixels(what: String): IntRect {
        assertWithMessage(
            "G1: every edge of $what lies on a whole pixel, so its reference can sit on the same pixels: $this",
        ).that(listOf(left, top, right, bottom).all { abs(it - it.roundToInt()) <= WHOLE_PIXEL_SLACK })
            .isTrue()
        return IntRect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
    }

    /** Where strays lie against their pill's top, for G5's message: ", rows 8..16 below its top". */
    private fun rowsBelowTheTop(
        pixels: List<IntOffset>,
        pill: IntRect,
    ): String =
        if (pixels.isEmpty()) {
            ""
        } else {
            ", rows ${pixels.minOf { it.y } - pill.top}..${pixels.maxOf { it.y } - pill.top} below its top"
        }

    /** One channel of [pixel] in this frame, picked by its bit shift ([COLOUR_CHANNEL_SHIFTS]). */
    private fun Bitmap.channel(
        pixel: IntOffset,
        shift: Int,
    ): Int = getPixel(pixel.x, pixel.y) shr shift and CHANNEL_MASK

    /** Every descendant carrying text: the pill's label and its value. */
    private fun SemanticsNode.textsWithin(): List<SemanticsNode> =
        children.flatMap { child ->
            listOfNotNull(child.takeIf { SemanticsProperties.Text in it.config }) + child.textsWithin()
        }

    /** The text a node lays out. */
    private fun textOf(node: SemanticsNode): String = node.config[SemanticsProperties.Text].joinToString("") { it.text }

    /** The pixels lying wholly inside this rect, in px. */
    private fun Rect.wholePixels(): List<IntOffset> =
        (ceil(top).toInt() until floor(bottom).toInt()).flatMap { y ->
            (ceil(left).toInt() until floor(right).toInt()).map { x -> IntOffset(x, y) }
        }

    private fun IntOffset.centre(): Offset = Offset(x + HALF_PIXEL, y + HALF_PIXEL)

    /** Where a set of pixels lies, for a failure message: "x 199..214, y 323..336 (px)". */
    private fun spanOf(pixels: List<IntOffset>): String =
        if (pixels.isEmpty()) {
            "none"
        } else {
            "x ${pixels.minOf { it.x }}..${pixels.maxOf { it.x }}, " +
                "y ${pixels.minOf { it.y }}..${pixels.maxOf { it.y }} (px)"
        }

    /** Whether [pixel] is [colour] to within [PAINT_TOLERANCE] in each of its red, green and blue channels. */
    private fun isPaintedIn(
        pixel: Int,
        colour: Int,
    ): Boolean =
        COLOUR_CHANNEL_SHIFTS.all { shift ->
            abs((pixel shr shift and CHANNEL_MASK) - (colour shr shift and CHANNEL_MASK)) <= PAINT_TOLERANCE
        }

    /**
     * One band gap of whichever set Home took: from the Classic tile's bottom edge to the Challenge
     * tile's top edge, both read along the column so neither edge is the viewport's.
     */
    private fun gapBetweenTheModeTiles(): Dp {
        val first = composeRule.columnBoundsOf(composeRule.onNodeWithText(classicTileTitle))
        val second = composeRule.columnBoundsOf(composeRule.onNodeWithText(challengeTileTitle))
        return second.top - first.bottom
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

    /** A `dimens.xml` token in dp, read from the resources rather than retyped. */
    private fun dimen(
        @DimenRes id: Int,
    ): Dp {
        val px = RuntimeEnvironment.getApplication().resources.getDimension(id)
        return with(composeRule.density) { px.toDp() }
    }

    private val contentMaxWidthPx: Int
        get() = RuntimeEnvironment.getApplication().resources.getDimensionPixelSize(R.dimen.content_max_width)

    /**
     * The compact column's own trailing space: the band gap after its last band (before the empty
     * footer, which measures 0 once the column overflows) and the column's bottom padding, both
     * `spacing_sm` in the compact set (§4.2.2). Space, not a band — see [assertTheFoldLeavesNoFalseBottom].
     */
    private val compactTrailingSpace: Dp
        get() {
            val gapAfterTheLastBand = dimen(R.dimen.spacing_sm)
            val bottomPadding = dimen(R.dimen.spacing_sm)
            return gapAfterTheLastBand + bottomPadding
        }

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
 * The window that keeps criterion 15k's overflow halves live: a 360dp phone 88dp shorter than
 * [SHORT_PHONE_QUALIFIER], with the same system bars, so 480dp of usable height. On real text
 * metrics its compact column overflows past its trailing space into the Challenge tile, which the
 * fold cuts — what the criterion's own window, fitting with room to spare, never shows. It is no
 * device the criterion names; it is the window on which those halves can fail
 * ([renderTheOverflowingNarrowWindow] holds it to that).
 *
 * It was `w360dp-h560dp` until D-03p took the stage's band gap out of the column (§4.2.7): 8dp less
 * column there left an overflow of exactly the 16dp trailing space, no band past the fold, and the
 * precondition below failed as written — so the window moved 8dp shorter, as that precondition asks.
 */
private const val OVERFLOWING_PHONE_QUALIFIER = "w360dp-h552dp"

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

/**
 * How far a text's ink may stray past its layout bounds on the anti-aliased edge, in dp. Measured
 * clean at 1dp; 2dp is still half the `spacing_xs` a glyph beside a label would sit at.
 */
private const val TEXT_INK_MARGIN_DP = 2

/**
 * The least glyph ink a full pill may paint beside its texts, in px: far under the 196 px and 85 px
 * the H-1 probe read on the Boards and Level pills at `w412dp-h915dp`, and far over the anti-aliased
 * fringe a text could leave past its widened bounds, which reads 0 on every compact pill.
 */
private const val GLYPH_INK_FLOOR_PX = 20

/**
 * Rounding the band-gap read allows, in dp: far below the 8dp between the full set's `spacing_md`
 * and the compact set's `spacing_sm`.
 */
private const val BAND_GAP_TOLERANCE_DP = 0.5f

/** A pixel's centre, from its top-left corner. */
private const val HALF_PIXEL = 0.5f

/** The red, green and blue channels of an ARGB pixel, by their bit shift; alpha is not read. */
private val COLOUR_CHANNEL_SHIFTS = listOf(0, 8, 16)
private const val CHANNEL_MASK = 0xFF

/**
 * One level a channel: the reference is the pill's own finish drawn by the same modifiers on the
 * same pixels, dither phase included, so it renders exact; the level is the dither's, and every
 * glyph red fails with it.
 */
private const val PAINT_TOLERANCE = 1

/**
 * How far a pill's edge may lie from a whole pixel, in px, for its reference to sit on the same
 * pixels (G1): float noise, never a fraction a layout could produce.
 */
private const val WHOLE_PIXEL_SLACK = 0.01f

/** The test tag of reference finish *i*, over pill *i* ([ReferenceFinishes]). */
private const val REFERENCE_FINISH_TAG_PREFIX = "home_layout_test_reference_finish_"

/**
 * One pill as criterion 15m's pixel read sees it: its [rect] on whole pixels (G1), the [box] inside
 * its `spacing_sm` padding, and [read], the pixels of that box outside its texts.
 */
private data class PillRead(
    val rect: IntRect,
    val box: Rect,
    val read: List<IntOffset>,
)

/** What one pill paints besides its finished surface and its texts: its [rect] (G1) and that [ink]. */
private data class PillInk(
    val rect: IntRect,
    val ink: List<IntOffset>,
)

/**
 * Frame 2 of criterion 15m's pixel read (B-19a; the tech-lead's 15m oracle, O1): over each of
 * [references], the ribbon pill's own finish, drawn by the production modifiers in `StatPill`'s order
 * and with its arguments, on exactly that rect's pixels, and nothing inside: no content, no text,
 * no glyph. Empty, it emits nothing.
 *
 * It is composed inside the theme `renderHome` provides, so it reads the same finish colours,
 * shapes, scheme and density as the pills. Its recipe is [FinishedContainer.RibbonPill]'s, so a
 * recipe change reaches the reference by itself. A change to the pill's fill token or to its
 * modifier chain does not, and fails the read, which is what criterion 15m is there to catch.
 *
 * Placed by `offset` and sized by a `layout` that measures with fixed constraints, both in px, so
 * no px-to-dp round trip can move an edge (G2 checks both).
 */
@Composable
private fun ReferenceFinishes(references: List<IntRect>) {
    val glow = dimensionResource(R.dimen.elevation_card)
    val shape = MaterialTheme.shapes.medium
    val fill = MaterialTheme.colorScheme.surfaceContainerHigh
    references.forEachIndexed { index, rect ->
        Box(
            modifier =
                Modifier
                    .offset { rect.topLeft }
                    .layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixed(rect.width, rect.height))
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }.finishGlow(glow, shape)
                    .glossFinish(FinishedContainer.RibbonPill, shape, fill = fill)
                    .testTag(REFERENCE_FINISH_TAG_PREFIX + index),
        )
    }
}

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
