package com.app.pegsolitaire.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.home.fixture.DEFAULT_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.DOUBLE_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.HomeSlack
import com.app.pegsolitaire.ui.home.fixture.RESUME_CHALLENGE_LABEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CLASSIC_LABEL
import com.app.pegsolitaire.ui.home.fixture.appName
import com.app.pegsolitaire.ui.home.fixture.challengeTileTitle
import com.app.pegsolitaire.ui.home.fixture.chipsState
import com.app.pegsolitaire.ui.home.fixture.classicTileTitle
import com.app.pegsolitaire.ui.home.fixture.columnBoundsOf
import com.app.pegsolitaire.ui.home.fixture.homeSlack
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.settingsLabel
import com.app.pegsolitaire.ui.theme.LightColors
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.max
import kotlin.math.min

/**
 * When the Continue chips arrive (ux-spec §4.2.5, D-03n; read since D-03p as criterion 15z, and since
 * D-03q with the title bound's no-chip headroom H₀ — criterion 15ac part 2).
 *
 * The chips are known only once `HomeViewModel` has decoded both resume snapshots, so they land
 * after first paint, ≈0.6s on QA's AVD. Under D-03i's 3 : 1 split of the *current* slack every
 * chip's arrival moved the mode tiles 22–24dp, under a thumb that was already heading for them
 * (QA's loop-3 F-1). D-03n fixed the footer to a share of the slack Home has with **no** chip, S₀, so
 * the tiles hold still wherever the room above them can give the chips' room, and move by exactly what
 * it cannot give. Since D-03p that room above is the **headroom above the hero row** (the stage is
 * withdrawn): the hero row and the ribbon ride up on it together while the tiles hold still. Since
 * D-03q the no-chip headroom is H₀ = min(⅘ S₀, C), C the headroom at which the hero row's bottom edge
 * meets the title bound, the footer S₀ − H₀, and the hero row never passes the bound in any emission or
 * any frame of the room's motion — it only ever rides up from H₀.
 *
 * ## How it is read
 *
 * Every emission goes through the fixture's settable overload, so it is one composition across
 * the walk, as on a device: no chip, the Classic chip, both, none again; then the Challenge chip
 * and both. Bounds are read **along the column**, from each node's unclipped position
 * ([columnBoundsOf]), under NATIVE graphics in the app theme, with the window's system bars supplied
 * (§13's two idioms): the design baseline's 24dp status bar and 48dp three-button bar on 15s's six
 * windows, and 15ac's two gesture-navigation windows. Every expected quantity is [HomeSlack]'s,
 * computed from the window and the bands' own heights — never from where the column put the hero row
 * — so the ±0.5dp tolerance is rounding and nothing else.
 *
 * - **The headroom** is the hero row's top edge less the viewport's top and the column's padding.
 * - **The footer** runs from the second tile's bottom edge to the bottom of the column's content,
 *   less one band gap and the column's bottom padding. The content's bottom is the viewport's plus
 *   the column's scrollable overflow, so the same reading holds where the column scrolls (15t).
 * - **R** is, for each chip present, its own height plus one band gap.
 * - **The band gap** is read where nothing else can sit, between the two mode tiles; the column's
 *   padding is the same token (§4.2.2).
 *
 * The stage art's fade at its floor (D-03n's "the stage art at the end of its range") was deleted with
 * its subject: there is no stage art to fade (ux-spec §13's D-03p table, a ruled deletion).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeChipArrivalTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    // ---- Criterion 15z parts 1–4 (15s): the room comes from the headroom ---------

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun theRoomComesFromTheHeadroomAndTheTilesHoldStillOnA360x640dpPhone() = assertTheRoomComesFromTheHeadroom()

    @Test
    @Config(qualifiers = "w360dp-h720dp")
    fun theRoomComesFromTheHeadroomAndTheTilesHoldStillOnA360x720dpPhone() = assertTheRoomComesFromTheHeadroom()

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun theRoomComesFromTheHeadroomAndTheTilesHoldStillOnA360x800dpPhone() = assertTheRoomComesFromTheHeadroom()

    @Test
    @Config(qualifiers = "w412dp-h780dp")
    fun theRoomComesFromTheHeadroomAndTheTilesHoldStillOnA412x780dpPhone() = assertTheRoomComesFromTheHeadroom()

    @Test
    @Config(qualifiers = "w412dp-h850dp")
    fun theRoomComesFromTheHeadroomAndTheTilesHoldStillOnA412x850dpPhone() = assertTheRoomComesFromTheHeadroom()

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun theRoomComesFromTheHeadroomAndTheTilesHoldStillOnA412x915dpPhone() = assertTheRoomComesFromTheHeadroom()

    // ---- Criterion 15ac part 2: the chips' room under the title bound -------------

    /** On the tall phone in gesture navigation, where the bound holds the no-chip headroom at C. */
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun underTheTitleBoundTheRoomStillComesFromTheHeadroomOnATallPhoneInGestureNavigation() =
        assertTheRoomComesFromTheHeadroom(GESTURE_TALL)

    /** On the Pixel_9, QA's device, where the bound bites by 39.3dp (§4.2.7's D-03q table). */
    @Test
    @Config(qualifiers = "w411dp-h923dp")
    fun underTheTitleBoundTheRoomStillComesFromTheHeadroomOnThePixel9() =
        assertTheRoomComesFromTheHeadroom(PIXEL_9_BARS)

    // ---- Criterion 15z part 1 at font scale 2.0 (15t): the shortfall, and no more -----

    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun whereTheHeadroomCannotGiveTheRoomTheTilesFallByTheShortfallAndNoMoreOnA360x640dpPhone() =
        assertTheTilesFallByTheShortfallAndNoMore()

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun whereTheHeadroomCannotGiveTheRoomTheTilesFallByTheShortfallAndNoMoreOnA412x915dpPhone() =
        assertTheTilesFallByTheShortfallAndNoMore()

    // ---- Criterion 15z part 5 (15u): the room opens, and nothing jumps ---------------

    /**
     * Criterion 15z part 5 with OS animations on, on the 6.7-inch window, where the headroom can give
     * the whole room: the Classic chip arriving, then both chips leaving, read frame by frame on a
     * paused clock once Home's entrance cascade has settled (a band's test bounds include its
     * entrance's drawn offset, §4.2.5).
     *
     * In every frame from the emission until the layout settles both tiles are where they were; the
     * hero row's and the ribbon's top edges move monotonically from their old places to their new ones,
     * together, and never past them; the hero row never passes the title bound; and no frame shows a
     * chip's room — its gap included — without the chip. The room opens over `ClayAnim.ENTRANCE_MS`: it
     * has started moving by [START_FRAMES] frames after the emission and settles no sooner than the
     * entrance's own duration and no later than [START_FRAMES] frames past it.
     */
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun theRoomOpensOverTheEntranceDurationAndNothingJumps() {
        val state = renderWithThePausedClock()
        val slack = composeRule.homeSlack(statusBar = THREE_BUTTON.status, navigationBar = THREE_BUTTON.navigation)
        val noChip = readFrame()

        val arriving = framesAfter(state, classic = true, challenge = false)
        assertTheRoomMovesSmoothly(arriving, from = noChip, slack = slack, what = "the Classic chip arriving")

        settle(state, classic = true, challenge = true)
        val bothChips = readFrame()
        val leaving = framesAfter(state, classic = false, challenge = false)
        assertTheRoomMovesSmoothly(leaving, from = bothChips, slack = slack, what = "both chips leaving")
    }

    /**
     * Criterion 15z part 5 with OS animations off (`rememberAnimationsEnabled()` false), the setting a
     * reduced-motion player has: the frame after the emission is the settled layout, on arrival and on
     * leaving, the tiles where they were and the hero row and the ribbon at their new place together.
     */
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun withAnimationsOffTheRoomIsOpenInTheNextFrame() {
        motion.turnAnimationsOff()
        val state = renderWithThePausedClock()
        val noChip = readFrame()

        val arriving = framesAfter(state, classic = true, challenge = false)
        assertSettledInTheFirstFrame(arriving, from = noChip, what = "the Classic chip arriving")

        settle(state, classic = true, challenge = true)
        val bothChips = readFrame()
        val leaving = framesAfter(state, classic = false, challenge = false)
        assertSettledInTheFirstFrame(leaving, from = bothChips, what = "both chips leaving")
    }

    // ---- The chip room under a live switch of Remove animations (§14.3 item 1) -------

    /**
     * P3 part 1 and the code-auditor's step 1b (`home-chip-arrival/tech-lead-c11-ruling.md` §2;
     * `code-auditor-loop3.md` I1): Remove animations switched **on** while the Classic chip's room is
     * opening, then off again. From the second frame after the first switch every frame is the settled
     * one-chip layout, and after the switch back every frame still is: a room left part-way, which the
     * gate's off branch would hide until animations come back, shows there. The tiles hold their no-chip
     * bounds throughout.
     */
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun switchingAnimationsOffMidOpeningSettlesTheRoomAndSwitchingThemBackLeavesItSettled() {
        val state = renderWithThePausedClock()
        val noChip = readFrame()
        composeRule.runOnUiThread {
            state.value = chipsState(classic = true, challenge = false)
            Snapshot.sendApplyNotifications()
        }
        val opening = nextFrames(START_FRAMES + 2)
        assertWithMessage("the room, part-way open when animations go off")
            .that(opening.last().ribbonTop.value)
            .isLessThan(noChip.ribbonTop.value - TOLERANCE_DP)

        switchAnimations(on = false)
        val off = nextFrames(FRAMES_READ)
        composeRule.mainClock.advanceTimeBy(CASCADE_SETTLED_MS)
        val settled = readFrame()
        switchAnimations(on = true)
        val backOn = nextFrames(FRAMES_READ)

        off.drop(1).forEachIndexed { index, frame ->
            assertWithMessage("frame ${index + 2} after the switch off, against the settled one-chip frame")
                .that(frame)
                .isEqualTo(settled)
        }
        backOn.forEachIndexed { index, frame ->
            assertWithMessage("frame ${index + 1} after the switch back on, against the settled one-chip frame")
                .that(frame)
                .isEqualTo(settled)
        }
        (opening + off + backOn).forEachIndexed { index, frame ->
            assertSameBounds("walk frame ${index + 1}: the Classic tile", frame.classicTile, noChip.classicTile)
            assertSameBounds("walk frame ${index + 1}: the Challenge tile", frame.challengeTile, noChip.challengeTile)
        }
    }

    /**
     * P3 part 2 (`home-chip-arrival/tech-lead-c11-ruling.md` §2): with Remove animations on, the chip
     * arrives and its room is open in the next frame; switching animations back on then changes nothing
     * — the frame after the switch is the frame before it — and the chip leaving afterwards closes its
     * room smoothly over the entrance duration, as 15z part 5 reads it.
     */
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun switchingAnimationsOnAfterAnInstantArrivalMovesNothingAndTheLeavingStillGlides() {
        motion.turnAnimationsOff()
        val state = renderWithThePausedClock()
        val slack = composeRule.homeSlack(statusBar = THREE_BUTTON.status, navigationBar = THREE_BUTTON.navigation)
        val arrived = framesAfter(state, classic = true, challenge = false)
        val before = arrived.last()
        assertWithMessage("the room open in the first frame after the arrival").that(arrived.first()).isEqualTo(before)

        switchAnimations(on = true)
        composeRule.mainClock.advanceTimeByFrame()
        assertWithMessage("the frame after the switch on, against the frame before it")
            .that(readFrame())
            .isEqualTo(before)

        val leaving = framesAfter(state, classic = false, challenge = false)
        assertTheRoomMovesSmoothly(leaving, from = before, slack = slack, what = "the chip leaving after the switch on")
    }

    /**
     * Turns Remove animations [on] or off while Home stays composed, as a live switch in Settings reaches
     * a resident app: the scale is written on the UI thread and its apply notification sent at once, as
     * [framesAfter] writes state, so the next frame reads it.
     */
    private fun switchAnimations(on: Boolean) {
        composeRule.runOnUiThread {
            if (on) motion.turnAnimationsOn() else motion.turnAnimationsOff()
            Snapshot.sendApplyNotifications()
        }
    }

    /**
     * Criteria 15z parts 1–4 and 15ac part 2 on the current window with [bars], at default font scale,
     * over both of 15s's walks. In the no-chip emission the headroom is H₀ = min(⅘ S₀, C); then in every
     * emission:
     * 1. both mode tiles' bounds equal their no-chip bounds wherever R ≤ H₀, and otherwise sit exactly
     *    R − H₀ lower;
     * 2. the hero row's and the ribbon's top edges sit higher than their no-chip tops by exactly
     *    min(R, H₀), the ribbon one band gap below the hero row's bottom;
     * 3. the footer is min(S₀ − H₀, max(0, S₀ − R));
     * 4. every chip present is complete in the first viewport, and the column's scrollable overflow is
     *    what it is with no chip;
     * 5. the hero row's visible bottom edge is at or above the title bound.
     */
    private fun assertTheRoomComesFromTheHeadroom(bars: Bars = THREE_BUTTON) {
        val state = mutableStateOf(chipsState(classic = false, challenge = false))
        renderWithInsets(state, DEFAULT_FONT_SCALE, bars)
        val slack = composeRule.homeSlack(statusBar = bars.status, navigationBar = bars.navigation)
        val noChip = readHome(bars)
        assertWithMessage("no chip: the headroom against H₀ = min(⅘ S₀, C), S₀ = ${slack.slack}, in dp")
            .that(noChip.headroom.value)
            .isWithin(TOLERANCE_DP)
            .of(slack.noChipHeadroom.value)

        WALK.forEach { (classic, challenge) ->
            show(state, classic, challenge)
            val chips = chipsNamed(classic, challenge)
            val reading = readHome(bars)
            val room = roomOf(reading, classic, challenge)
            assertTheRoomCameFromTheHeadroom(chips, reading, noChip, slack, room, boundInEveryEmission = true)
            chipNodes(classic, challenge).forEach(::assertAScrollDoesNotMove)
            assertWithMessage("$chips: the column's scrollable overflow, in dp")
                .that(reading.overflow.value)
                .isWithin(TOLERANCE_DP)
                .of(noChip.overflow.value)
        }
    }

    /**
     * Criterion 15z part 1 at font scale 2.0 (15t) on the current window: between any two emissions the
     * mode tiles sit exactly max(0, R − H₀) below their no-chip position — never above it — the hero row
     * and the ribbon sit higher by exactly min(R, H₀), the footer pays what the headroom cannot, down to
     * 0, no band is dropped, and where the column overflows it scrolls.
     */
    private fun assertTheTilesFallByTheShortfallAndNoMore() {
        val state = mutableStateOf(chipsState(classic = false, challenge = false))
        renderWithInsets(state, DOUBLE_FONT_SCALE, THREE_BUTTON)
        val slack = composeRule.homeSlack(statusBar = THREE_BUTTON.status, navigationBar = THREE_BUTTON.navigation)
        val noChip = readHome(THREE_BUTTON)

        WALK.forEach { (classic, challenge) ->
            show(state, classic, challenge)
            val chips = chipsNamed(classic, challenge)
            val reading = readHome(THREE_BUTTON)
            val room = roomOf(reading, classic, challenge)
            assertTheRoomCameFromTheHeadroom(chips, reading, noChip, slack, room, boundInEveryEmission = false)
        }
        assertEveryBandIsReachable()
    }

    /**
     * One emission against the no-chip one: the tiles, the hero row, the ribbon, the footer and the
     * bound. With [boundInEveryEmission] the hero row's bottom edge is held to the bound in every
     * emission (15ac part 2, at default font scale); without it, only where the headroom is above 0 (15ac
     * part 1's reading at 2.0, where a row taller than the bound allows sits at the top padding: the
     * wrapped title's case, which B-21 covers).
     */
    private fun assertTheRoomCameFromTheHeadroom(
        chips: String,
        reading: HomeReading,
        noChip: HomeReading,
        slack: HomeSlack,
        room: Dp,
        boundInEveryEmission: Boolean,
    ) {
        val fall = slack.tilesFallWith(room)
        val rise = slack.riseWith(room)
        assertMovedDown("$chips: the Classic tile", reading.classicTile, noChip.classicTile, by = fall)
        assertMovedDown("$chips: the Challenge tile", reading.challengeTile, noChip.challengeTile, by = fall)
        assertWithMessage("$chips: the hero row's top edge, risen by min(R, H₀), R = $room, in dp")
            .that(reading.heroTop.value)
            .isWithin(TOLERANCE_DP)
            .of((noChip.heroTop - rise).value)
        assertWithMessage("$chips: the ribbon's top edge, risen by min(R, H₀), R = $room, in dp")
            .that(reading.ribbonTop.value)
            .isWithin(TOLERANCE_DP)
            .of((noChip.ribbonTop - rise).value)
        assertWithMessage("$chips: the ribbon's top edge, one band gap below the hero row's bottom, in dp")
            .that(reading.ribbonTop.value)
            .isWithin(TOLERANCE_DP)
            .of((reading.heroBottom + reading.gap).value)
        assertWithMessage("$chips: the footer against min(S₀ − H₀, max(0, S₀ − R)), S₀ = ${slack.slack}, in dp")
            .that(reading.footer.value)
            .isWithin(TOLERANCE_DP)
            .of(slack.footerWith(room).value)
        if (boundInEveryEmission || reading.headroom > TOLERANCE_DP.dp) {
            assertWithMessage("$chips: the hero row's visible bottom edge against the title bound, in dp")
                .that(reading.heroVisibleBottom.value)
                .isAtMost(slack.titleBound.value + TOLERANCE_DP)
        }
    }

    /**
     * A chip that arrives after first paint takes no stagger delay (§4.2.5, "The motion"): its fade
     * starts in the frame its room starts to open, so no empty room shows ahead of it. With the
     * cascade order a chip present at first paint takes, the third band's 80ms, the room would open
     * blank for five frames.
     *
     * Read in the flat render, in every frame whose room is open by at least [INKED_ROOM_DP] — past
     * the chip's 16dp entrance slide, so its fading top edge is inside its room: between the ribbon
     * and the first tile something is drawn besides the background — on a window-filling
     * `background` surface drawn under Home, as its opaque `Scaffold` drew before BD-14, and with the
     * gap checked empty before the chip comes, so the read cannot pass on an empty room.
     */
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun aChipThatArrivesAfterFirstPaintFadesInWithItsRoomWithNoStaggerDelay() {
        val state = renderWithThePausedClock(underlay = { FlatBackground() })
        val noChip = readFrame()
        val ribbonBottom = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG)).bottom
        assertWithMessage("no chip yet: px drawn between the ribbon and the first tile")
            .that(inkBetween(ribbonBottom, noChip.classicTile))
            .isEqualTo(0)
        composeRule.runOnUiThread {
            state.value = chipsState(classic = true, challenge = false)
            Snapshot.sendApplyNotifications()
        }

        repeat(FRAMES_READ) { index ->
            composeRule.mainClock.advanceTimeByFrame()
            val ribbon = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG))
            val room = noChip.ribbonTop - ribbon.top
            if (room >= INKED_ROOM_DP.dp) {
                assertWithMessage("the Classic chip arriving, frame ${index + 1}, room $room: px drawn in it")
                    .that(inkBetween(ribbon.bottom, readFrame().classicTile))
                    .isGreaterThan(0)
            }
        }
    }

    /**
     * A chip whose save has gone is drawn while its room closes, and is nothing else (code-auditor
     * loop 1, finding 1): no click, no semantics action and no TalkBack stop, so neither a tap nor a
     * TalkBack click can resume a save that no longer exists — in Challenge, the replaced ladder's
     * level (the phantom Continue WR-02 and CR-01 removed). Read four frames after the save goes, the
     * room still part open, as the audit's probe read it.
     */
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun aChipWhoseSaveIsGoneIsDrawnButInertWhileItsRoomCloses() {
        var resumed: Int? = null
        composeRule.mainClock.autoAdvance = false
        val state = mutableStateOf(chipsState(classic = false, challenge = true))
        composeRule.renderHome(state, onResumeChallenge = { level -> resumed = level })
        composeRule.applySystemBarInsets(top = THREE_BUTTON.status, bottom = THREE_BUTTON.navigation)
        composeRule.mainClock.advanceTimeBy(CASCADE_SETTLED_MS)
        composeRule.runOnUiThread {
            state.value = chipsState(classic = false, challenge = false)
            Snapshot.sendApplyNotifications()
        }
        repeat(CLOSING_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        assertWithMessage("frame $CLOSING_FRAMES after the save went: the room, still closing")
            .that(readFrame().room)
            .isGreaterThan(TOLERANCE_DP.dp)
        // The merged tree is what an accessibility service is handed.
        assertWithMessage("nodes speaking the closing chip's title")
            .that(composeRule.onAllNodesWithText(RESUME_CHALLENGE_LABEL).fetchSemanticsNodes())
            .isEmpty()
        assertWithMessage("controls on Home with no save: the gear and the two tiles")
            .that(composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes())
            .hasSize(CONTROLS_WITH_NO_CHIP)
        // Where the closing chip is drawn now: it rides down with the ribbon as the headroom grows back.
        val chip = bounds(composeRule.onNodeWithTag(CONTINUE_CHIP_ROOM_TEST_TAG, useUnmergedTree = true))
        val tap =
            with(composeRule.density) {
                Offset(((chip.left + chip.right) / 2).toPx(), (chip.top + TAP_DEPTH_DP.dp).toPx())
            }
        composeRule.onRoot().performTouchInput { click(tap) }
        composeRule.mainClock.advanceTimeByFrame()
        assertWithMessage("the level a tap on the closing chip resumed").that(resumed).isNull()
    }

    /** Pixels between [top] and the first tile, across the tile's width, that are not the background. */
    private fun inkBetween(
        top: Dp,
        firstTile: DpRect,
    ): Int {
        val window = composeRule.renderedWindow()
        val background = LightColors.background.toArgb()
        return with(composeRule.density) {
            (top.roundToPx() until firstTile.top.roundToPx()).sumOf { y ->
                (firstTile.left.roundToPx() until firstTile.right.roundToPx()).count { x ->
                    window.getPixel(x, y) != background
                }
            }
        }
    }

    /**
     * 15z part 5's motion read over [frames], the first being the frame after the emission and the last
     * settled: see [theRoomOpensOverTheEntranceDurationAndNothingJumps].
     */
    private fun assertTheRoomMovesSmoothly(
        frames: List<FrameReading>,
        from: FrameReading,
        slack: HomeSlack,
        what: String,
    ) {
        val settled = frames.last()
        assertWithMessage("$what: the last frames, settled").that(frames.takeLast(SETTLED_RUN).distinct()).hasSize(1)
        val rising = settled.ribbonTop < from.ribbonTop
        frames.forEachIndexed { index, frame ->
            val label = "$what, frame ${index + 1} after the emission"
            assertSameBounds("$label: the Classic tile", frame.classicTile, from.classicTile)
            assertSameBounds("$label: the Challenge tile", frame.challengeTile, from.challengeTile)
            val previous = if (index == 0) from else frames[index - 1]
            assertMonotonic("$label: the ribbon's top edge", frame.ribbonTop, previous.ribbonTop, rising)
            assertMonotonic("$label: the hero row's top edge", frame.heroTop, previous.heroTop, rising)
            assertWithMessage("$label: the ribbon's top edge between its old place and its new one, in dp")
                .that(frame.ribbonTop.value)
                .isIn(rangeOf(from.ribbonTop.value, settled.ribbonTop.value))
            assertWithMessage("$label: the hero row's top edge between its old place and its new one, in dp")
                .that(frame.heroTop.value)
                .isIn(rangeOf(from.heroTop.value, settled.heroTop.value))
            assertWithMessage("$label: the ribbon riding with the hero row — the distance between their tops, in dp")
                .that((frame.ribbonTop - frame.heroTop).value)
                .isWithin(TOLERANCE_DP)
                .of((from.ribbonTop - from.heroTop).value)
            assertWithMessage("$label: the hero row's visible bottom edge against the title bound, in dp")
                .that(frame.heroVisibleBottom.value)
                .isAtMost(slack.titleBound.value + TOLERANCE_DP)
            if (frame.room > TOLERANCE_DP.dp) {
                assertWithMessage("$label: the chips composed while the room is ${frame.room} open")
                    .that(frame.chips)
                    .isAtLeast(1)
            }
        }
        val firstMove = frames.indexOfFirst { it.ribbonTop != from.ribbonTop } + 1
        val settledAt = frames.indexOfFirst { it == settled } + 1
        assertWithMessage("$what: the frame the room starts to move in").that(firstMove).isIn(1..START_FRAMES)
        assertWithMessage("$what: the frame the room settles in")
            .that(settledAt)
            .isIn(ENTRANCE_FRAMES..ENTRANCE_FRAMES + START_FRAMES)
    }

    /** One edge's step: monotonic toward where it is going, never back by more than the tolerance. */
    private fun assertMonotonic(
        what: String,
        edge: Dp,
        previous: Dp,
        rising: Boolean,
    ) {
        val monotonic =
            if (rising) edge.value <= previous.value + TOLERANCE_DP else edge.value >= previous.value - TOLERANCE_DP
        assertWithMessage("$what, $edge, against the frame before, $previous").that(monotonic).isTrue()
    }

    /** The closed range between two values, whichever is the larger, widened by the tolerance. */
    private fun rangeOf(
        one: Float,
        other: Float,
    ): Range<Float> = Range.closed(min(one, other) - TOLERANCE_DP, max(one, other) + TOLERANCE_DP)

    /** 15z part 5 with animations off: the first frame after the emission is already the settled one. */
    private fun assertSettledInTheFirstFrame(
        frames: List<FrameReading>,
        from: FrameReading,
        what: String,
    ) {
        assertWithMessage("$what: every frame after the emission, the first included")
            .that(frames.distinct())
            .hasSize(1)
        val settled = frames.first()
        assertSameBounds("$what: the Classic tile", settled.classicTile, from.classicTile)
        assertSameBounds("$what: the Challenge tile", settled.challengeTile, from.challengeTile)
        assertWithMessage("$what: the ribbon moved in the first frame")
            .that(settled.ribbonTop)
            .isNotEqualTo(from.ribbonTop)
        assertWithMessage("$what: the hero row moved with the ribbon in the first frame, in dp")
            .that((settled.ribbonTop - settled.heroTop).value)
            .isWithin(TOLERANCE_DP)
            .of((from.ribbonTop - from.heroTop).value)
    }

    // ---- Harness ---------------------------------------------------------------

    /**
     * Home with no chip, its clock paused, the design baseline's insets supplied and the entrance
     * cascade run out: from here on, a frame happens only when a test advances one.
     */
    private fun renderWithThePausedClock(underlay: @Composable () -> Unit = {}): MutableState<HomeUiState> {
        composeRule.mainClock.autoAdvance = false
        val state = mutableStateOf(chipsState(classic = false, challenge = false))
        renderWithInsets(state, DEFAULT_FONT_SCALE, THREE_BUTTON, underlay)
        composeRule.mainClock.advanceTimeBy(CASCADE_SETTLED_MS)
        return state
    }

    /**
     * Emits the chips asked for, then reads each of the [FRAMES_READ] frames that follow.
     *
     * The write's apply notification is sent at once, as a `StateFlow` collected on the main thread
     * has it sent before the next frame on a device; left to the paused clock's own dispatch, the
     * first frame after the write would not yet see it, and "the frame after the emission" would be
     * one frame later than the frame it names.
     */
    private fun framesAfter(
        holder: MutableState<HomeUiState>,
        classic: Boolean,
        challenge: Boolean,
    ): List<FrameReading> {
        composeRule.runOnUiThread {
            holder.value = chipsState(classic = classic, challenge = challenge)
            Snapshot.sendApplyNotifications()
        }
        return nextFrames(FRAMES_READ)
    }

    /** Advances the paused clock [count] frames, reading each one. */
    private fun nextFrames(count: Int): List<FrameReading> =
        List(count) {
            composeRule.mainClock.advanceTimeByFrame()
            readFrame()
        }

    /** Emits the chips asked for and lets the clock run until everything has settled. */
    private fun settle(
        holder: MutableState<HomeUiState>,
        classic: Boolean,
        challenge: Boolean,
    ) {
        composeRule.runOnUiThread { holder.value = chipsState(classic = classic, challenge = challenge) }
        composeRule.mainClock.advanceTimeBy(CASCADE_SETTLED_MS)
    }

    /** What 15z part 5 reads in one frame, read along the column with the clock paused. */
    private fun readFrame(): FrameReading {
        val classicTile = bounds(composeRule.onNodeWithText(classicTileTitle))
        val ribbon = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG))
        val gap = bounds(composeRule.onNodeWithText(challengeTileTitle)).top - classicTile.bottom
        return FrameReading(
            classicTile = classicTile,
            challengeTile = bounds(composeRule.onNodeWithText(challengeTileTitle)),
            heroTop = bounds(composeRule.onNodeWithTag(HOME_HERO_TEST_TAG, useUnmergedTree = true)).top,
            heroVisibleBottom = heroVisibleBottom(),
            ribbonTop = ribbon.top,
            room = classicTile.top - ribbon.bottom - gap,
            // Counted by their rooms: a chip whose save has gone is drawn but carries no text (loop 1).
            chips =
                composeRule
                    .onAllNodesWithTag(CONTINUE_CHIP_ROOM_TEST_TAG, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .size,
        )
    }

    private fun renderWithInsets(
        state: MutableState<HomeUiState>,
        fontScale: Float,
        bars: Bars,
        underlay: @Composable () -> Unit = {},
    ) {
        composeRule.renderHome(state, fontScale = fontScale, underlay = underlay)
        composeRule.applySystemBarInsets(top = bars.status, bottom = bars.navigation)
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
    private fun readHome(bars: Bars): HomeReading {
        val classicTile = bounds(composeRule.onNodeWithText(classicTileTitle))
        val challengeTile = bounds(composeRule.onNodeWithText(challengeTileTitle))
        val gap = challengeTile.top - classicTile.bottom
        val viewportBottom = composeRule.onRoot().getBoundsInRoot().bottom - bars.navigation
        val overflow = scrollableOverflow()
        val contentBottom = viewportBottom + overflow
        val hero = bounds(composeRule.onNodeWithTag(HOME_HERO_TEST_TAG, useUnmergedTree = true))
        return HomeReading(
            classicTile = classicTile,
            challengeTile = challengeTile,
            heroTop = hero.top,
            heroBottom = hero.bottom,
            heroVisibleBottom = heroVisibleBottom(),
            ribbonTop = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG)).top,
            // The column's padding is the band spacing (§4.2.2), so the padding and one gap both.
            headroom = hero.top - bars.status - gap,
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

    /** The hero row's visible bottom edge (D-03m): the lower of its title's and its gear chip's. */
    private fun heroVisibleBottom(): Dp =
        maxOf(bounds(composeRule.onNode(hasText(appName), useUnmergedTree = true)).bottom, gearChip().bottom)

    /**
     * The gear **chip**, the 48dp clay circle the row draws: the parent of the button node that
     * carries the gear's description (as `HomeHeadroomTest` reads it).
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
    ) = assertMovedDown(what, actual, expected, by = 0.dp)

    /** [actual] is [expected] moved down by exactly [by], edge for edge (a move of 0 is "the same bounds"). */
    private fun assertMovedDown(
        what: String,
        actual: DpRect,
        expected: DpRect,
        by: Dp,
    ) {
        listOf(
            "left" to (actual.left to expected.left),
            "top" to (actual.top to expected.top + by),
            "right" to (actual.right to expected.right),
            "bottom" to (actual.bottom to expected.bottom + by),
        ).forEach { (edge, pair) ->
            assertWithMessage("$what's $edge edge against its no-chip edge moved down by $by, in dp")
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

    /**
     * One frame's geometry for 15z part 5: [room] is the space between the ribbon's band gap and the
     * first tile — the chips' room, gaps included — and [chips] how many Continue chips are composed.
     */
    private data class FrameReading(
        val classicTile: DpRect,
        val challengeTile: DpRect,
        val heroTop: Dp,
        val heroVisibleBottom: Dp,
        val ribbonTop: Dp,
        val room: Dp,
        val chips: Int,
    )

    /** One emission's geometry, in dp, the column unscrolled. */
    private data class HomeReading(
        val classicTile: DpRect,
        val challengeTile: DpRect,
        val heroTop: Dp,
        val heroBottom: Dp,
        val heroVisibleBottom: Dp,
        val ribbonTop: Dp,
        val headroom: Dp,
        val footer: Dp,
        val gap: Dp,
        val overflow: Dp,
    )

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

/** The flat field Home's opaque `Scaffold` drew before BD-14: the background, filling the window. */
@Composable
private fun FlatBackground() =
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {}

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

/** Criteria 15s, 15t and 15z's ±0.5dp. */
private const val TOLERANCE_DP = 0.5f

/**
 * Past Home's entrance cascade before an emission (15u): the last band's stagger plus
 * `ClayAnim.ENTRANCE_MS` is 3 × 40 + 240 = 360ms with no chip, 440ms with both; 15u asks for at
 * least 500ms.
 */
private const val CASCADE_SETTLED_MS = 600L

/** `ClayAnim.ENTRANCE_MS` in frames of the test clock's 16ms: 240 ÷ 16. */
private const val ENTRANCE_FRAMES = 15

/**
 * How many frames after the emission the room may take to start moving: the emission's own
 * recomposition, then the frame in which the animation takes its start time, as every Compose
 * animation does, so the room's first opening shows in the frame after that at the latest.
 */
private const val START_FRAMES = 3

/** Frames read after each emission: the entrance's own and a margin to show it has settled. */
private const val FRAMES_READ = ENTRANCE_FRAMES + START_FRAMES + 3

/** Consecutive identical frames that show the layout has settled. */
private const val SETTLED_RUN = 3

/** A room open past the chip's 16dp entrance slide by a margin, so a fading chip shows in it. */
private const val INKED_ROOM_DP = 40

/** The audit's probe frame: four frames after the save goes, the room part shut. */
private const val CLOSING_FRAMES = 4

/** Home's controls with no Continue chip: the Settings gear and the two mode tiles. */
private const val CONTROLS_WITH_NO_CHIP = 3

/** How far below a closing chip's top edge the tap lands: inside the part still drawn. */
private const val TAP_DEPTH_DP = 8
