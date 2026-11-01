package com.app.pegsolitaire.ui.game

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.scoring.ChallengeClearRule
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Undecided notice on the real gameplay screen (ux-spec §4.4.2: D-04f as amended by D-04g;
 * criteria 23a to 23e and 23g to 23i, and the game-designer's AC-16 and AC-17). Criterion 23f is
 * the ViewModel's — which taps clear the flag — and lives in `GameViewModelHintStalenessTest`.
 *
 * The screen is driven from a hand-built [GameUiState]: the notice is raised and lowered by
 * swapping `hintUndecided`, exactly as the ViewModel does, and every other surface on the screen
 * is left as a fresh English-33 board shows it. What the notice does once raised — how long it
 * stays, what a tap on it does, what it says to TalkBack, whether it comes back — belongs to the
 * screen, and that is what is asserted.
 *
 * The test clock never advances on its own. Every frame is advanced by hand, so "present at
 * 4.9 s" and "gone after its exit" are readings of a clock this test controls, not a race
 * against the one the framework would otherwise run. The spec's durations (5 000 ms and 160 ms,
 * §6.4) are written out here rather than read from `GameAnim`, so that a wrong constant fails.
 *
 * Text is laid out in the real faces (`GraphicsMode.NATIVE`): where a notice wraps and how tall it
 * is are the properties under test. The screen gets a phone's system-bar insets, as §13 asks of
 * every layout reading on the baseline window, and edges are read before any ancestor clips them.
 *
 * The class runs on the `junit4.v2` rule, the exception the tech design's §8.1 allows, for the two
 * behaviours its helpers document: a recreation whose snapshot writes only a running clock
 * delivers ([recreateTheScreen]), and a tap's writes, which reach the screen on the paused clock
 * only once their apply notification is sent ([deliverTheTapsWrites]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HINT_NOTICE_ROBOLECTRIC_SDK], qualifiers = DESIGN_BASELINE_QUALIFIER)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameScreenHintNoticeTest {
    /** The system's animator scale, as the screen's effects and its reduced-motion gate read it. */
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    /** The state the screen renders. A test raises and lowers the notice by swapping it. */
    private val screenState = mutableStateOf(GameUiState.initial())

    /** The font scale the screen is composed at; a test may change it while the screen is up. */
    private val fontScale = mutableFloatStateOf(DEFAULT_FONT_SCALE)

    /** Every cell the screen handed to `onCellTap`, in order. */
    private val cellTaps = mutableListOf<Position>()

    // ---- 23a: its own text, never the banner (AC-16, AC-17) -------------------

    /**
     * Criterion 23a, with the game-designer's AC-16 and AC-17: an Undecided answer shows the
     * notice with exactly its own words, and nothing of the rescue banner — no banner node, no
     * `nowin_message`, no `stuck_message`, and neither of the Challenge banner's two messages
     * (ux-spec §13, D-SB-3), resolved at this board's peg count and the clear threshold — and
     * nothing inside the notice can be clicked.
     */
    @Test
    fun theUndecidedNoticeShowsItsOwnTextAndNeverTheBanner() {
        showScreen()
        raiseTheNotice()

        composeRule
            .onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG)
            .assertIsDisplayed()
            .assertTextEquals(string(R.string.hint_undecided_message))
        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.nowin_message)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.stuck_message)).assertDoesNotExist()
        val pegs = screenState.value.board.pegCount
        val threshold = ChallengeClearRule.CLEAR_MAX_PEGS
        composeRule
            .onNodeWithText(plural(R.plurals.stuck_message_challenge, pegs, pegs, threshold))
            .assertDoesNotExist()
        composeRule.onNodeWithText(plural(R.plurals.nowin_message_challenge, threshold, threshold)).assertDoesNotExist()
        composeRule
            .onAllNodes(
                insideTheNotice() and SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick),
                useUnmergedTree = true,
            ).assertCountEquals(0)
    }

    // ---- 23b: its words ---------------------------------------------------------

    /** Criterion 23b: the words are the spec's, and none of the ruling's forbidden ones. */
    @Test
    fun theNoticeSaysWhatTheSpecSaysAndNothingTheRulingForbids() {
        val words = string(R.string.hint_undecided_message)

        assertThat(words).isEqualTo("Hint couldn't work this position out in time. It may still be winnable.")
        FORBIDDEN_PHRASES.forEach { phrase ->
            assertWithMessage("the notice must not say \"%s\"", phrase).that(words.lowercase()).doesNotContain(phrase)
        }
    }

    // ---- 23c: a tap puts it away and reaches nothing else -----------------------

    /** Criterion 23c, first clause: one node, its text, one Dismiss, no click, role or toggle. */
    @Test
    fun theNoticeIsOneNodeWithItsTextADismissActionAndNoClickRoleOrToggle() {
        showScreen()
        raiseTheNotice()

        val notice = composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).fetchSemanticsNode()
        assertThat(notice.config.getOrNull(SemanticsProperties.Text)?.map { it.text })
            .containsExactly(string(R.string.hint_undecided_message))
        assertThat(notice.config.getOrNull(SemanticsActions.Dismiss)).isNotNull()
        assertThat(notice.config.getOrNull(SemanticsActions.OnClick)).isNull()
        assertThat(notice.config.getOrNull(SemanticsProperties.Role)).isNull()
        assertThat(notice.config.getOrNull(SemanticsProperties.ToggleableState)).isNull()
        val nodesInside =
            composeRule.onAllNodes(hasAnyAncestor(hasTestTag(HINT_UNDECIDED_NOTICE_TEST_TAG))).fetchSemanticsNodes()
        assertWithMessage("nothing inside the notice may be a stop of its own").that(nodesInside).isEmpty()
    }

    /**
     * Criterion 23c, second clause, with its control (the check's F-3): a tap at the centre of a
     * cell that lies under the notice puts the notice away and reaches nothing on the board — while
     * the same point, with the notice absent, plays that very cell. Without the control the
     * negative could pass on a point the board ignores anyway (`positionAt` answers nothing outside
     * a cell's hit radius); a cell's centre is inside it.
     *
     * At 200% on the 360x640dp baseline the notice runs to several lines and covers the board's
     * lower rows, so a covered cell exists; the test fails if none does.
     */
    @Test
    fun aTapOnTheNoticePutsItAwayAndNeverReachesTheCellBeneathIt() {
        showScreen(fontScale = DOUBLE_FONT_SCALE)
        raiseTheNotice()
        settleTheEntrance()
        val (cell, point) = aCellCentreUnderTheNotice()
        lowerTheNotice()
        runTheExit()

        tapAt(point)
        assertWithMessage("with the notice absent, the point plays the cell beneath")
            .that(cellTaps)
            .containsExactly(cell)

        cellTaps.clear()
        raiseTheNotice()
        settleTheEntrance()
        tapAt(point)
        assertWithMessage("a tap on the notice must reach nothing on the board").that(cellTaps).isEmpty()
        runTheExit()
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()

        tapAt(point)
        assertWithMessage("once it is put away, the next tap there plays").that(cellTaps).containsExactly(cell)
    }

    /** Criterion 23c, third clause: a tap on the board outside the notice plays and leaves it up. */
    @Test
    fun aTapOnTheBoardOutsideTheNoticePlaysAndLeavesTheNoticeUp() {
        showScreen()
        raiseTheNotice()
        settleTheEntrance()
        val notice = noticeBounds()
        val (cell, point) =
            cellCentres().first { (_, centre) -> centre !in notice }

        tapAt(point)
        advanceFrames(SETTLE_FRAMES)

        assertThat(cellTaps).containsExactly(cell)
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertIsDisplayed()
    }

    /** Criterion 23c, fourth clause: TalkBack's Dismiss puts the notice away too. */
    @Test
    fun theDismissActionPutsTheNoticeAway() {
        showScreen()
        raiseTheNotice()

        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).performSemanticsAction(SemanticsActions.Dismiss)
        runTheExit()

        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()
    }

    /**
     * Criterion 23c, fifth clause, where the property lives (the check's F-2): the spec's message
     * always wraps to two lines on the 480dp-capped layout, so the 48dp floor never binds on the
     * screen. A short message, one line of `bodyMedium` and its padding, is about 36dp — less than
     * the floor — so on the leaf the floor is what holds it at 48dp.
     */
    @Test
    fun theNoticeIsAtLeast48dpTallEvenWhenItsTextIsOneShortLine() {
        composeRule.setContent { AppTheme { HintUndecidedNotice(message = SHORT_MESSAGE, onDismiss = {}) } }

        assertThat(heightOf(noticeBounds()).value).isAtLeast(NOTICE_MIN_HEIGHT_DP)
    }

    /** Criterion 23c, fifth clause, as the spec words it: on a `w600dp` window at font scale 1.0. */
    @Test
    @Config(qualifiers = WIDE_WINDOW_QUALIFIER)
    fun theNoticeIsAtLeast48dpTallOnAWideWindow() {
        showScreen()
        raiseTheNotice()
        settleTheEntrance()

        assertThat(heightOf(noticeBounds()).value).isAtLeast(NOTICE_MIN_HEIGHT_DP)
    }

    /**
     * Criterion 23c, last clause: the notice ends above the dock slab, at default scale and at
     * 200% alike, so it covers no dock control — the edge that also puts it before the dock in
     * TalkBack's order. And by as much as §4.4.2's "Touch targets" says (criterion 26a's note): at
     * least `board_screen_margin` plus the dock's `spacing_md`, 32dp, well beyond the 8dp rule.
     */
    @Test
    fun theNoticesBottomEdgeIsAboveTheDockAtEveryScale() {
        showScreen()
        raiseTheNotice()
        settleTheEntrance()

        listOf(DEFAULT_FONT_SCALE, DOUBLE_FONT_SCALE).forEach { scale ->
            composeRule.runOnUiThread { Snapshot.withMutableSnapshot { fontScale.floatValue = scale } }
            advanceFrames(SETTLE_FRAMES)

            val dock = unclippedBoundsOf(GAME_ACTION_DOCK_TEST_TAG)
            assertWithMessage("at font scale %s, the notice's bottom against the dock's top", scale)
                .that(noticeBounds().bottom)
                .isLessThan(dock.top)
            assertWithMessage("at font scale %s, the room between the notice and the dock", scale)
                .that((dock.top - noticeBounds().bottom).value)
                .isAtLeast(ROOM_ABOVE_THE_DOCK_DP)
        }
    }

    // ---- 23d: spoken politely, every time ---------------------------------------

    /** Criterion 23d, first sentence: a polite live region, the visible text, nothing assertive. */
    @Test
    fun theNoticeIsAPoliteLiveRegionAndNothingOnTheScreenIsAssertive() {
        showScreen()
        raiseTheNotice()

        val notice = composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).fetchSemanticsNode()
        assertThat(notice.config.getOrNull(SemanticsProperties.LiveRegion)).isEqualTo(LiveRegionMode.Polite)
        assertThat(notice.config[SemanticsProperties.Text].map { it.text })
            .containsExactly(string(R.string.hint_undecided_message))
        composeRule
            .onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive),
                useUnmergedTree = true,
            ).assertCountEquals(0)
    }

    /**
     * Criterion 23d, the rest: a live region speaks when it appears, so every raise must be a node
     * of its own. Raised, cleared and raised again with the same words, the notice is absent in the
     * middle once its exit has run and present after it; and a raise that lands while the last
     * notice is still fading out is a new node, not the fading one brought back.
     */
    @Test
    fun everyRaiseOfTheNoticeIsANewNodeEvenWithTheSameWords() {
        showScreen()
        raiseTheNotice()
        val first = noticeId()
        lowerTheNotice()
        runTheExit()
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()

        raiseTheNotice()
        val second = noticeId()
        lowerTheNotice()
        advanceFrames(MID_EXIT_FRAMES)
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertExists()
        raiseTheNotice()
        val third = noticeId()

        assertThat(listOf(first, second, third).toSet()).hasSize(THREE_NODES)
    }

    // ---- 23e: it leaves on time --------------------------------------------------

    /** Criterion 23e: up for its 5 s, gone after its 160 ms exit, on the platform's manager. */
    @Test
    fun theNoticeLeavesAfterItsDisplayTimeAndItsExit() {
        showScreen()
        raiseTheNotice()

        assertTheNoticeLastsFor(SPEC_DISPLAY_MS, exitMillis = SPEC_EXIT_MS)
    }

    /**
     * Criterion 23e, the accessibility timeout: a manager that recommends 60 s keeps the notice up
     * for 60 s. The call is the one the spec names, `SnackbarHost`'s own: the 5 s display time, an
     * icon and text, and no controls.
     */
    @Test
    fun aLongerAccessibilityTimeoutKeepsTheNoticeUpForThatLong() {
        val manager = RecordingAccessibilityManager(recommendedMillis = LENGTHENED_DISPLAY_MS)
        showScreen(accessibility = Accessibility.Provided(manager))
        raiseTheNotice()

        assertTheNoticeLastsFor(LENGTHENED_DISPLAY_MS, exitMillis = SPEC_EXIT_MS)
        assertThat(manager.asked.toSet())
            .containsExactly(TimeoutQuestion(SPEC_DISPLAY_MS, icons = true, text = true, controls = false))
    }

    /** Criterion 23e, loop 1's I-4: with no manager at all, the display time is the default 5 s. */
    @Test
    fun withNoAccessibilityManagerTheNoticeStaysForTheDefaultTime() {
        showScreen(accessibility = Accessibility.Provided(manager = null))
        raiseTheNotice()

        assertTheNoticeLastsFor(SPEC_DISPLAY_MS, exitMillis = SPEC_EXIT_MS)
    }

    /**
     * Criterion 23e, loop 1's I-7: with the OS animator scale at 0, the notice keeps its whole
     * reading time and goes in one frame — reduced motion drops the fade, never the time.
     */
    @Test
    fun withAnimationsOffTheNoticeKeepsItsTimeAndGoesInOneFrame() {
        motion.turnAnimationsOff()
        showScreen()
        raiseTheNotice()

        assertTheNoticeLastsFor(SPEC_DISPLAY_MS, exitMillis = NO_EXIT_MS)
    }

    /** Criterion 23e with D-04g item 4: a tap ends even an accessibility-lengthened display at once. */
    @Test
    fun aTapEndsAnAccessibilityLengthenedDisplayAtOnce() {
        showScreen(accessibility = Accessibility.Provided(RecordingAccessibilityManager(LENGTHENED_DISPLAY_MS)))
        raiseTheNotice()
        composeRule.mainClock.advanceTimeBy(TAP_AFTER_MS)
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertExists()

        tapTheNotice()
        runTheExit()

        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()
    }

    // ---- 23g: once gone, it stays gone -------------------------------------------

    /** Criterion 23g: a notice that timed out does not come back when the screen is recreated. */
    @Test
    fun aNoticeThatTimedOutStaysGoneAcrossARecreation() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { ScreenUnderTest(Accessibility.Platform, sounds = null) }
        composeRule.mainClock.autoAdvance = false
        advanceFrames(SETTLE_FRAMES)
        raiseTheNotice()
        composeRule.mainClock.advanceTimeBy(SPEC_DISPLAY_MS)
        runTheExit()
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()

        recreateTheScreen(restoration)

        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()
    }

    /** Criterion 23g with D-04g: a notice the player tapped away does not come back either. */
    @Test
    fun aNoticeTappedAwayStaysGoneAcrossARecreation() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { ScreenUnderTest(Accessibility.Platform, sounds = null) }
        composeRule.mainClock.autoAdvance = false
        advanceFrames(SETTLE_FRAMES)
        raiseTheNotice()
        settleTheEntrance()
        tapTheNotice()
        runTheExit()
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()

        recreateTheScreen(restoration)

        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()
    }

    // ---- 23h: one slot, one occupant -----------------------------------------------

    /** Criterion 23h: if both are ever raised, the proof wins and the notice is not drawn at all. */
    @Test
    fun theRescueBannerOutranksTheNoticeInTheirSharedSlot() {
        showScreen()
        setState(screenState.value.copy(hintUndecided = true, noWinningMove = true))
        settleTheEntrance()

        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()
    }

    // ---- 23i: it is silent ----------------------------------------------------------

    /**
     * Criterion 23i, in `GameScreenSoundTest`'s idiom: raising the notice records no cue at all —
     * in particular not the Hint cue — while a hint that reveals a move still records the Hint
     * cue, exactly once.
     */
    @Test
    fun theNoticeIsSilentWhileARevealedHintStillPlaysTheHintCueOnce() {
        val sounds = FakeSoundPlayer()
        showScreen(sounds = sounds)
        raiseTheNotice()
        settleTheEntrance()

        assertWithMessage("the cues the notice played").that(sounds.cues).isEmpty()

        setState(screenState.value.copy(hintUndecided = false, hintSource = HINT_SOURCE, hintTarget = HINT_TARGET))
        advanceFrames(SETTLE_FRAMES)

        assertThat(sounds.cues).containsExactly(SoundCue.Hint)
    }

    // ---- Harness ------------------------------------------------------------------

    /** Composes the gameplay screen over [screenState], with the clock under the test's control. */
    private fun showScreen(
        fontScale: Float = DEFAULT_FONT_SCALE,
        accessibility: Accessibility = Accessibility.Platform,
        sounds: FakeSoundPlayer? = null,
    ) {
        this.fontScale.floatValue = fontScale
        composeRule.setContent { ScreenUnderTest(accessibility, sounds) }
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)
        composeRule.mainClock.autoAdvance = false
        advanceFrames(SETTLE_FRAMES)
    }

    /**
     * The real [GameScreen] over [screenState], at [fontScale], with the accessibility manager
     * and sound player a test supplies (the platform's own when it supplies none).
     */
    @Composable
    private fun ScreenUnderTest(
        accessibility: Accessibility,
        sounds: FakeSoundPlayer?,
    ) {
        val density = LocalDensity.current
        val locals =
            buildList {
                add(LocalDensity provides Density(density = density.density, fontScale = fontScale.floatValue))
                if (accessibility is Accessibility.Provided) {
                    add(LocalAccessibilityManager provides accessibility.manager)
                }
                if (sounds != null) add(LocalSoundPlayer provides sounds)
            }
        CompositionLocalProvider(*locals.toTypedArray()) {
            AppTheme {
                GameScreen(
                    state = screenState.value,
                    onCellTap = { cellTaps += it },
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
        }
    }

    /** What the ViewModel does when a search ends Undecided, then one frame for it to compose. */
    private fun raiseTheNotice() = setState(screenState.value.copy(hintUndecided = true))

    /** What the ViewModel does at a jump, an undo, a restart or a Hint request. */
    private fun lowerTheNotice() = setState(screenState.value.copy(hintUndecided = false))

    /**
     * Writes [state] in a snapshot of its own, so the screen is invalidated the moment it applies,
     * then advances one frame for the screen to compose it.
     */
    private fun setState(state: GameUiState) {
        composeRule.runOnUiThread { Snapshot.withMutableSnapshot { screenState.value = state } }
        composeRule.mainClock.advanceTimeByFrame()
    }

    /**
     * Saves the screen's saveable state, disposes it and composes it again from that state, as a
     * configuration change does. The tester does it through snapshot writes that only a running
     * clock delivers — paused, the dispose and the re-emit cancel out and nothing is recreated — so
     * the clock runs for the recreation alone. A notice wrongly brought back would still be up after
     * it: the framework's idling advances the frames its entrance asks for, never its 5 s display.
     */
    private fun recreateTheScreen(restoration: StateRestorationTester) {
        composeRule.mainClock.autoAdvance = true
        restoration.emulateSavedInstanceStateRestore()
        composeRule.mainClock.autoAdvance = false
        advanceFrames(SETTLE_FRAMES)
    }

    /** Lets the notice's 240 ms entrance finish, so it sits where it will stay. */
    private fun settleTheEntrance() = advanceFrames(ENTRANCE_FRAMES)

    /** Lets a notice that is leaving run its 160 ms exit to the end. */
    private fun runTheExit() = advanceFrames(EXIT_FRAMES)

    private fun advanceFrames(frames: Int) {
        repeat(frames) { composeRule.mainClock.advanceTimeByFrame() }
    }

    /**
     * From the frame after the raise: present 100 ms before [displayMillis] is up, and gone one
     * frame after [displayMillis] plus [exitMillis].
     */
    private fun assertTheNoticeLastsFor(
        displayMillis: Long,
        exitMillis: Long,
    ) {
        composeRule.mainClock.advanceTimeBy(displayMillis - BEFORE_THE_END_MS)
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertExists()
        composeRule.mainClock.advanceTimeBy(BEFORE_THE_END_MS + exitMillis + FRAME_MS)
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).assertDoesNotExist()
    }

    private fun noticeBounds(): DpRect = unclippedBoundsOf(HINT_UNDECIDED_NOTICE_TEST_TAG)

    /**
     * The tagged node's bounds in the root, read before any ancestor clips them: clipped bounds
     * shrink to what shows, which can make an edge comparison hold by construction (§13).
     */
    private fun unclippedBoundsOf(tag: String): DpRect {
        val node = composeRule.onNodeWithTag(tag).fetchSemanticsNode()
        return with(composeRule.density) {
            DpRect(
                left = node.positionInRoot.x.toDp(),
                top = node.positionInRoot.y.toDp(),
                right = (node.positionInRoot.x + node.size.width).toDp(),
                bottom = (node.positionInRoot.y + node.size.height).toDp(),
            )
        }
    }

    private fun noticeId(): Int = composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).fetchSemanticsNode().id

    private fun heightOf(bounds: DpRect): Dp = bounds.bottom - bounds.top

    /** Every in-play English-33 cell with its centre on screen, in the root's coordinates. */
    private fun cellCentres(): List<Pair<Position, DpOffset>> {
        val canvas = unclippedBoundsOf(BOARD_CANVAS_TEST_TAG)
        val cell = (canvas.right - canvas.left) / ENGLISH_33_GRID
        return Boards.English33.inPlay.map { position ->
            position to
                DpOffset(
                    x = canvas.left + cell * (position.col + BoardMetrics.CELL_CENTER_FRACTION),
                    y = canvas.top + cell * (position.row + BoardMetrics.CELL_CENTER_FRACTION),
                )
        }
    }

    /** A cell whose centre lies inside the notice — failing the test if this window has none. */
    private fun aCellCentreUnderTheNotice(): Pair<Position, DpOffset> {
        val notice = noticeBounds()
        val covered = cellCentres().firstOrNull { (_, centre) -> centre in notice }
        assertWithMessage("no in-play cell's centre lies under the notice on this window").that(covered).isNotNull()
        return requireNotNull(covered)
    }

    /** Whether [this] point lies strictly inside [bounds]. */
    private operator fun DpRect.contains(point: DpOffset): Boolean =
        point.x > left && point.x < right && point.y > top && point.y < bottom

    /** A tap at [point], given in the root's coordinates. */
    private fun tapAt(point: DpOffset) {
        composeRule.onRoot().performTouchInput { click(Offset(point.x.toPx(), point.y.toPx())) }
        deliverTheTapsWrites()
    }

    /** A tap at the notice's centre. */
    private fun tapTheNotice() {
        composeRule.onNodeWithTag(HINT_UNDECIDED_NOTICE_TEST_TAG).performTouchInput { click() }
        deliverTheTapsWrites()
    }

    /**
     * Delivers what a tap handler wrote, as the platform's global snapshot manager does on a
     * device, then advances one frame. A write made during input dispatch is outside any
     * composition, and with the clock paused nothing else would send its apply notification.
     */
    private fun deliverTheTapsWrites() {
        composeRule.runOnUiThread { Snapshot.sendApplyNotifications() }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun insideTheNotice(): SemanticsMatcher =
        hasTestTag(HINT_UNDECIDED_NOTICE_TEST_TAG) or hasAnyAncestor(hasTestTag(HINT_UNDECIDED_NOTICE_TEST_TAG))

    private fun string(
        @StringRes id: Int,
    ): String = RuntimeEnvironment.getApplication().getString(id)

    private fun plural(
        @PluralsRes id: Int,
        quantity: Int,
        vararg args: Any,
    ): String = RuntimeEnvironment.getApplication().resources.getQuantityString(id, quantity, *args)
}

/** Which accessibility manager the screen sees: the platform's own, or one the test provides. */
private sealed interface Accessibility {
    data object Platform : Accessibility

    data class Provided(
        val manager: AccessibilityManager?,
    ) : Accessibility
}

/** One call to [AccessibilityManager.calculateRecommendedTimeoutMillis], as it was asked. */
private data class TimeoutQuestion(
    val originalMillis: Long,
    val icons: Boolean,
    val text: Boolean,
    val controls: Boolean,
)

/** An accessibility manager that recommends [recommendedMillis] and records what it was asked. */
private class RecordingAccessibilityManager(
    private val recommendedMillis: Long,
) : AccessibilityManager {
    val asked = mutableListOf<TimeoutQuestion>()

    override fun calculateRecommendedTimeoutMillis(
        originalTimeoutMillis: Long,
        containsIcons: Boolean,
        containsText: Boolean,
        containsControls: Boolean,
    ): Long {
        asked += TimeoutQuestion(originalTimeoutMillis, containsIcons, containsText, containsControls)
        return recommendedMillis
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val HINT_NOTICE_ROBOLECTRIC_SDK = 34

/** §4.3.1's design baseline: a 360x640dp phone. */
private const val DESIGN_BASELINE_QUALIFIER = "w360dp-h640dp"

/** The baseline's system bars (§13): a 24dp status bar and a 48dp three-button navigation bar. */
private const val STATUS_BAR_DP = 24
private const val NAVIGATION_BAR_DP = 48

/** Criterion 23c's wide window: `w600dp`, where the board slab reaches its 480dp cap. */
private const val WIDE_WINDOW_QUALIFIER = "w600dp-h960dp"

private const val DEFAULT_FONT_SCALE = 1f
private const val DOUBLE_FONT_SCALE = 2f

/** The spec's durations (§6.4), written out so that a wrong constant in `GameAnim` fails. */
private const val SPEC_DISPLAY_MS = 5_000L
private const val SPEC_EXIT_MS = 160L

/** With animations off there is no exit at all. */
private const val NO_EXIT_MS = 0L

/** Criterion 23e's fake: "Time to take action" at one minute. */
private const val LENGTHENED_DISPLAY_MS = 60_000L

/** How far into a one-minute display the player taps the notice away. */
private const val TAP_AFTER_MS = 10_000L

/** Criterion 23e reads the notice 100 ms before its time is up. */
private const val BEFORE_THE_END_MS = 100L

/** One frame of the test clock. */
private const val FRAME_MS = 16L

/** Enough frames for the 240 ms entrance, the 160 ms exit, or a state change, to settle. */
private const val ENTRANCE_FRAMES = 20
private const val EXIT_FRAMES = 15
private const val SETTLE_FRAMES = 5

/** Three frames into the 160 ms exit: the leaving notice is still on screen. */
private const val MID_EXIT_FRAMES = 3

private const val THREE_NODES = 3

/** The criterion's 48dp floor. */
private const val NOTICE_MIN_HEIGHT_DP = 48f

/** §4.4.2's room between the notice and the dock: `board_screen_margin` plus `spacing_md`. */
private const val ROOM_ABOVE_THE_DOCK_DP = 32f

/** One short line, far below the floor once padded (the check's F-2). */
private const val SHORT_MESSAGE = "Short"

/** The English-33 footprint occupies a 7x7 lattice. */
private const val ENGLISH_33_GRID = 7

/** Criterion 23b: the ruling's forbidden words, matched case-insensitively. */
private val FORBIDDEN_PHRASES = listOf("winning move", "undo", "restart", "try again", "stuck", "dead end", "lost")

/** Any in-play cell of the English-33 start will do as a revealed hint's source and target. */
private val HINT_SOURCE = Position(row = 3, col = 1)
private val HINT_TARGET = Position(row = 3, col = 3)
