package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
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
import com.app.pegsolitaire.ui.home.fixture.RESUME_CHALLENGE_LABEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CLASSIC_LABEL
import com.app.pegsolitaire.ui.home.fixture.appName
import com.app.pegsolitaire.ui.home.fixture.challengeTileTitle
import com.app.pegsolitaire.ui.home.fixture.chipsState
import com.app.pegsolitaire.ui.home.fixture.classicTileTitle
import com.app.pegsolitaire.ui.home.fixture.columnBoundsOf
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * When the Continue chips arrive (ux-spec §4.2.5, D-03n): criteria 15s, 15t and 15u.
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
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

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

    // ---- Criterion 15u: the room opens, and nothing jumps -----------------------

    /**
     * Criterion 15u with OS animations on, on the 6.7-inch window, where the stage can give the
     * whole room: the Classic chip arriving, then both chips leaving, read frame by frame on a
     * paused clock once Home's entrance cascade has settled (a band's test bounds include its
     * entrance's drawn offset, §4.2.5).
     *
     * In every frame from the emission until the layout settles the hero row and both tiles are
     * where they were, the ribbon's top edge moves monotonically from its old place to its new one
     * and never past it, and no frame shows a chip's room — its gap included — without the chip.
     * The room opens over `ClayAnim.ENTRANCE_MS`: it has started moving by [START_FRAMES] frames
     * after the emission and settles no sooner than the entrance's own duration and no later than
     * [START_FRAMES] frames past it.
     */
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun theRoomOpensOverTheEntranceDurationAndNothingJumps() {
        val state = renderWithThePausedClock()
        val noChip = readFrame()

        val arriving = framesAfter(state, classic = true, challenge = false)
        assertTheRoomMovesSmoothly(arriving, from = noChip, what = "the Classic chip arriving")

        settle(state, classic = true, challenge = true)
        val bothChips = readFrame()
        val leaving = framesAfter(state, classic = false, challenge = false)
        assertTheRoomMovesSmoothly(leaving, from = bothChips, what = "both chips leaving")
    }

    /**
     * Criterion 15u with OS animations off (`rememberAnimationsEnabled()` false), the setting a
     * reduced-motion player has: the frame after the emission is the settled layout, on arrival and
     * on leaving, and nothing a player can tap moves in it either.
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
            // The footer pays what the stage cannot, down to 0 (code-auditor loop 1, P5).
            assertWithMessage("$chips: the footer against min(S₀/5, max(0, S₀ − R)), R = $room, S₀ = $slack")
                .that(reading.footer.value)
                .isWithin(TOLERANCE_DP)
                .of(min(slack.value / FOOTER_SHARE_DIVISOR, max(0f, slack.value - room.value)))

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

    /**
     * A chip that arrives after first paint takes no stagger delay (§4.2.5, "The motion"): its fade
     * starts in the frame its room starts to open, so no empty room shows ahead of it. With the
     * cascade order a chip present at first paint takes, the third band's 80ms, the room would open
     * blank for five frames.
     *
     * Read in the flat render, in every frame whose room is open by at least [INKED_ROOM_DP] — past
     * the chip's 16dp entrance slide, so its fading top edge is inside its room: between the ribbon
     * and the first tile something is drawn besides the background.
     */
    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun aChipThatArrivesAfterFirstPaintFadesInWithItsRoomWithNoStaggerDelay() {
        val state = renderWithThePausedClock()
        val noChip = readFrame()
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
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)
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
        // Where the closing chip is drawn now: it rides down with the ribbon as the stage grows back.
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
     * The stage art at the end of its range (§4.2.5, "The stage art at the end of its range"; §10):
     * on the 360x640dp baseline both chips take the stage from 186dp to 10dp, under the 48dp the
     * art needs. The art must not shrink to 48dp and vanish in one frame: it stays drawn while the
     * room opens, shrinking and fading, and is gone once the room has opened. Both chips leaving run
     * it in reverse: the art is back from the room's first frame, faded in, never popped in whole.
     *
     * The fade is read in the flat render: an opaque slab paints pixels of exactly its
     * `surfaceContainerHigh`, and a slab drawn at any alpha under 1 paints none, since the
     * background behind it is another colour.
     */
    @Test
    @Config(qualifiers = "w360dp-h640dp")
    fun theStageArtFadesAtItsFloorRatherThanVanishingOrPoppingWhenBothChipsCome() {
        val state = renderWithThePausedClock()
        assertWithMessage("no chip: the stage art drawn opaque").that(readArt().opaquePixels).isGreaterThan(0)

        val arriving = artFramesAfter(state, classic = true, challenge = true)
        val opened = arriving.indexOfFirst { it.ribbonTop == arriving.last().ribbonTop }
        arriving.take(opened).forEachIndexed { index, frame ->
            assertWithMessage("both chips arriving, frame ${index + 1}: the stage art, the room still opening")
                .that(frame.present)
                .isTrue()
        }
        val midway = arriving[opened / 2]
        assertWithMessage("both chips arriving, frame ${opened / 2 + 1}: opaque slab px, the art half faded")
            .that(midway.opaquePixels)
            .isEqualTo(0)
        assertWithMessage("both chips arriving, settled: the stage art").that(arriving.last().present).isFalse()

        settle(state, classic = true, challenge = true)
        val leaving = artFramesAfter(state, classic = false, challenge = false)
        val back = leaving.indexOfFirst { it.present }
        assertWithMessage("both chips leaving: the frame the art is back in").that(back + 1).isIn(1..START_FRAMES)
        assertWithMessage("both chips leaving, frame ${back + 1}: opaque slab px as the art comes back")
            .that(leaving[back].opaquePixels)
            .isEqualTo(0)
        assertWithMessage("both chips leaving, settled: opaque slab px")
            .that(leaving.last().opaquePixels)
            .isGreaterThan(0)
    }

    /**
     * 15u's motion read over [frames], the first being the frame after the emission and the last
     * settled: see [theRoomOpensOverTheEntranceDurationAndNothingJumps].
     */
    private fun assertTheRoomMovesSmoothly(
        frames: List<FrameReading>,
        from: FrameReading,
        what: String,
    ) {
        val settled = frames.last()
        assertWithMessage("$what: the last frames, settled").that(frames.takeLast(SETTLED_RUN).distinct()).hasSize(1)
        val rising = settled.ribbonTop < from.ribbonTop
        frames.forEachIndexed { index, frame ->
            val label = "$what, frame ${index + 1} after the emission"
            assertSameBounds("$label: the title", frame.title, from.title)
            assertSameBounds("$label: the gear", frame.gear, from.gear)
            assertSameBounds("$label: the Classic tile", frame.classicTile, from.classicTile)
            assertSameBounds("$label: the Challenge tile", frame.challengeTile, from.challengeTile)
            val previous = if (index == 0) from.ribbonTop else frames[index - 1].ribbonTop
            val monotonic =
                if (rising) {
                    frame.ribbonTop.value <= previous.value + TOLERANCE_DP
                } else {
                    frame.ribbonTop.value >= previous.value - TOLERANCE_DP
                }
            assertWithMessage("$label: the ribbon's top edge, ${frame.ribbonTop}, against the frame before, $previous")
                .that(monotonic)
                .isTrue()
            assertWithMessage("$label: the ribbon's top edge between its old place and its new one, in dp")
                .that(frame.ribbonTop.value)
                .isIn(rangeOf(from.ribbonTop.value, settled.ribbonTop.value))
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

    /** The closed range between two values, whichever is the larger, widened by the tolerance. */
    private fun rangeOf(
        one: Float,
        other: Float,
    ): Range<Float> = Range.closed(min(one, other) - TOLERANCE_DP, max(one, other) + TOLERANCE_DP)

    /** 15u with animations off: the first frame after the emission is already the settled one. */
    private fun assertSettledInTheFirstFrame(
        frames: List<FrameReading>,
        from: FrameReading,
        what: String,
    ) {
        assertWithMessage("$what: every frame after the emission, the first included")
            .that(frames.distinct())
            .hasSize(1)
        val settled = frames.first()
        assertSameBounds("$what: the title", settled.title, from.title)
        assertSameBounds("$what: the gear", settled.gear, from.gear)
        assertSameBounds("$what: the Classic tile", settled.classicTile, from.classicTile)
        assertSameBounds("$what: the Challenge tile", settled.challengeTile, from.challengeTile)
        assertWithMessage("$what: the ribbon moved in the first frame")
            .that(settled.ribbonTop)
            .isNotEqualTo(from.ribbonTop)
    }

    // ---- Harness ---------------------------------------------------------------

    /**
     * Home with no chip, its clock paused, the insets supplied and the entrance cascade run out:
     * from here on, a frame happens only when a test advances one.
     */
    private fun renderWithThePausedClock(): MutableState<HomeUiState> {
        composeRule.mainClock.autoAdvance = false
        val state = mutableStateOf(chipsState(classic = false, challenge = false))
        renderWithInsets(state, DEFAULT_FONT_SCALE)
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
        return List(FRAMES_READ) {
            composeRule.mainClock.advanceTimeByFrame()
            readFrame()
        }
    }

    /** [framesAfter] for the stage art: whether it is drawn, and how opaque, frame by frame. */
    private fun artFramesAfter(
        holder: MutableState<HomeUiState>,
        classic: Boolean,
        challenge: Boolean,
    ): List<ArtFrame> {
        composeRule.runOnUiThread {
            holder.value = chipsState(classic = classic, challenge = challenge)
            Snapshot.sendApplyNotifications()
        }
        return List(FRAMES_READ) {
            composeRule.mainClock.advanceTimeByFrame()
            readArt()
        }
    }

    /** The stage art in the current frame: drawn or not, its opaque slab pixels, and the ribbon's top. */
    private fun readArt(): ArtFrame {
        val ribbonTop = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG)).top
        val art = composeRule.onAllNodesWithTag(HOME_STAGE_ART_TEST_TAG, useUnmergedTree = true).fetchSemanticsNodes()
        if (art.isEmpty()) return ArtFrame(present = false, opaquePixels = 0, ribbonTop = ribbonTop)
        val window = composeRule.renderedWindow()
        val slab = LightColors.surfaceContainerHigh.toArgb()
        val bounds = art.single().boundsInRoot
        val opaque =
            (ceil(bounds.top).toInt() until floor(bounds.bottom).toInt()).sumOf { y ->
                (ceil(bounds.left).toInt() until floor(bounds.right).toInt()).count { x ->
                    window.getPixel(x, y) == slab
                }
            }
        return ArtFrame(present = true, opaquePixels = opaque, ribbonTop = ribbonTop)
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

    /** What 15u reads in one frame, read along the column with the clock paused. */
    private fun readFrame(): FrameReading {
        val classicTile = bounds(composeRule.onNodeWithText(classicTileTitle))
        val ribbon = bounds(composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG))
        val gap = bounds(composeRule.onNodeWithText(challengeTileTitle)).top - classicTile.bottom
        return FrameReading(
            title = bounds(composeRule.onNode(hasText(appName), useUnmergedTree = true)),
            gear = gearChip(),
            classicTile = classicTile,
            challengeTile = bounds(composeRule.onNodeWithText(challengeTileTitle)),
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

    /**
     * One frame's geometry for 15u: [room] is the space between the ribbon's band gap and the first
     * tile — the chips' room, gaps included — and [chips] how many Continue chips are composed.
     */
    private data class FrameReading(
        val title: DpRect,
        val gear: DpRect,
        val classicTile: DpRect,
        val challengeTile: DpRect,
        val ribbonTop: Dp,
        val room: Dp,
        val chips: Int,
    )

    /** The stage art in one frame. */
    private data class ArtFrame(
        val present: Boolean,
        val opaquePixels: Int,
        val ribbonTop: Dp,
    )

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

/** D-03n's split of S₀: the footer is a fifth, so the stage is four fifths less the chips' room. */
private const val FOOTER_SHARE_DIVISOR = 5f
private const val STAGE_SHARE_OF_SLACK = 0.8f
