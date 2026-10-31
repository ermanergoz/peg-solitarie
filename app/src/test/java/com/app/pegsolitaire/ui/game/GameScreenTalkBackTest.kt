package com.app.pegsolitaire.ui.game

import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.domain.assist.AssistAllowanceLimits
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import com.app.pegsolitaire.domain.usecase.HintOutcome
import com.app.pegsolitaire.platform.ads.NoOpAdGateway
import com.app.pegsolitaire.testing.RecordingHapticFeedback
import com.app.pegsolitaire.testing.assertInTouchMode
import com.app.pegsolitaire.testing.liveRegionsHandedOut
import com.app.pegsolitaire.testing.traversalBeforeOf
import com.app.pegsolitaire.testing.turnOnATouchExplorationService
import com.app.pegsolitaire.ui.components.ANNOUNCEMENT_ARM_GAP_MS
import com.app.pegsolitaire.ui.components.ANNOUNCEMENT_REGION_TEST_TAG
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.components.BOARD_STOP_TEST_TAG
import com.app.pegsolitaire.ui.components.BoardCellPositionKey
import com.app.pegsolitaire.ui.components.FOCUS_MOVE_GAP_MS
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.TestTimeSource

/**
 * The gameplay board with TalkBack (D-TB as amended by D-TBa, `docs/ux-spec.md` §11.1), through the
 * real [GameScreen] over a real [GameViewModel]: TB-1 on the slab, TB-4 and TB-5 as the double-tap
 * and the jump action against a finger's taps, TB-6 as the canvas's one polite channel, and 26b's
 * pinned order (D-TBa loop 5, F-38) on a window that limits the board by height.
 */
@RunWith(RobolectricTestRunner::class)
// A phone's state: Robolectric starts a window in touch mode only with native graphics (TouchMode.kt).
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [TALKBACK_SCREEN_SDK], qualifiers = TALKBACK_SCREEN_QUALIFIERS)
class GameScreenTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val haptics = RecordingHapticFeedback()
    private val sounds = FakeSoundPlayer()
    private var viewModel by mutableStateOf<GameViewModel?>(null)
    private var shown by mutableStateOf(true)

    // ---- TB-1: the board stop on the real slab ---------------------------------------------

    @Test
    fun theBoardStopIsItsOwnStopAndActivatingItChangesNothing() {
        val game = startGame()
        val before = game.uiState.value

        val stop = composeRule.onNodeWithTag(BOARD_STOP_TEST_TAG)
        assertThat(stop.fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription))
            .containsExactly("Board, 32 pegs left")
        stop.performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertThat(game.uiState.value).isEqualTo(before)
        assertThat(sounds.cues).isEmpty()
        assertThat(haptics.events).isEmpty()
        assertThat(channel()).isNull()
    }

    // ---- TB-4: a double-tap is a tap ------------------------------------------------------------

    @Test
    fun theScriptGivesTheSameStatesCuesAndHapticsByDoubleTapAndByFinger() {
        val byFinger = record(startGame()) { tapCell(it) }
        val fingerCues = sounds.cues.toList()
        val fingerHaptics = haptics.events.toList()
        sounds.clear()
        haptics.events.clear()

        val byDoubleTap = record(swapInANewGame()) { activate(it) }

        assertThat(byDoubleTap).isEqualTo(byFinger)
        assertThat(sounds.cues).isEqualTo(fingerCues)
        assertThat(haptics.events).isEqualTo(fingerHaptics)
        assertThat(byFinger.last().rejectedTapTick).isEqualTo(2)
    }

    // ---- TB-5: a jump action is the two taps -------------------------------------------------

    @Test
    fun aJumpActionFromNothingSelectedLeavesWhatTheTwoTapsLeave() {
        val tapped = startGame()
        tapCell(ABOVE_CENTRE)
        tapCell(CENTRE)
        val byTaps = settled(tapped)

        val acted = swapInANewGame()
        takeAction(ABOVE_CENTRE, "Jump down to row 4, column 4")

        assertThat(settled(acted)).isEqualTo(byTaps)
        assertThat(byTaps.selectionTick).isEqualTo(1)
        assertThat(byTaps.canUndo).isTrue()
    }

    @Test
    fun aJumpActionFromThePegSelectedLeavesWhatTheDestinationTapLeaves() {
        val tapped = startGame()
        tapCell(ABOVE_CENTRE)
        tapCell(CENTRE)
        val byTaps = settled(tapped)

        val acted = swapInANewGame()
        tapCell(ABOVE_CENTRE)
        takeAction(ABOVE_CENTRE, "Jump down to row 4, column 4")

        assertThat(settled(acted)).isEqualTo(byTaps)
    }

    // ---- TB-6: every change is spoken once, on the canvas's one channel -------------------------

    @Test
    fun eachBoardEventPutsItsOwnLineOnTheChannel() {
        val game = startGame()

        tapCell(ABOVE_CENTRE)
        assertChannel("Selected row 2, column 4, 1 jump.")
        tapCell(ABOVE_CENTRE)
        assertChannel("Peg put down, row 2, column 4.")
        tapCell(ABOVE_CENTRE)
        tapCell(CENTRE)
        assertChannel("Row 2, column 4 to row 4, column 4. Removed row 3, column 4. 31 pegs left.")
        tapCell(ABOVE_CENTRE)
        assertChannel("No peg here.")
        tapCell(BELOW_CENTRE_TWO)
        tapCell(TOP_LEFT_PEG)
        assertChannel("Put down the selected peg first.")
        tapCell(ABOVE_CENTRE)
        assertChannel("Not a jump target.")
        tapCell(BELOW_CENTRE_TWO)
        composeRule.runOnIdle { game.onUndo() }
        composeRule.waitForIdle()
        assertChannel("Undid row 2, column 4 to row 4, column 4. 32 pegs left.")
        composeRule.runOnIdle { game.onRestart() }
        composeRule.waitForIdle()
        assertChannel("Board restarted. 32 pegs left.")
    }

    @Test
    fun aRevealedHintIsSpokenAsItsLine() {
        val game = startGame(hint = fixedHint(HintOutcome.Found(DOWN_INTO_THE_CENTRE)))
        // Loop 3: the channel speaks once armed (defect ruling §1, mechanism 5).
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS)

        composeRule.runOnIdle { game.onHintRequested() }
        composeRule.waitForIdle()

        assertChannel("Hint: row 2, column 4, jump down to row 4, column 4.")
    }

    @Test
    fun nothingIsSpokenOnFirstCompositionNorWhenTheScreenIsComposedAgainOverTheSameGame() {
        val game = startGame()
        assertThat(channel()).isNull()
        tapCell(ABOVE_CENTRE)
        tapCell(CENTRE)
        composeRule.runOnIdle { game.onUndo() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { game.onRestart() }
        tapCell(CENTRE)
        composeRule.mainClock.advanceTimeBy(PAST_THE_HOLD_MS)

        shown = false
        composeRule.waitForIdle()
        shown = true
        composeRule.waitForIdle()

        assertThat(channel()).isNull()
    }

    @Test
    fun theChannelNeverCarriesTheBannersOrTheNoticesText() {
        val stranding = startGame(board = STRANDING_BOARD, hint = fixedHint(HintOutcome.Undecided))
        tapCell(Position(3, 1))
        tapCell(CENTRE)
        assertThat(stranding.uiState.value.stuck).isTrue()
        assertChannel("Row 4, column 2 to row 4, column 4. Removed row 4, column 3. 2 pegs left.")
        composeRule.onNodeWithText(string(R.string.stuck_message)).assertExists()

        composeRule.runOnIdle { stranding.onUndo() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { stranding.onHintRequested() }
        composeRule.waitForIdle()

        assertThat(stranding.uiState.value.hintUndecided).isTrue()
        val banned =
            listOf(
                R.string.stuck_message,
                R.string.nowin_message,
                R.string.hint_undecided_message,
            ).map(::string)
        assertThat(banned).doesNotContain(channel())
    }

    @Test
    fun twoIdenticalRejectedTapsEachLetSettleChangeTheChannelTwice() {
        startGame()

        tapCell(CENTRE)
        assertChannel("No peg here.")
        composeRule.mainClock.advanceTimeBy(PAST_THE_HOLD_MS)
        composeRule.waitForIdle()
        assertThat(channel()).isNull()
        tapCell(CENTRE)

        assertChannel("No peg here.")
    }

    // ---- The banner's Undo and Restart move focus to the board stop ---------------------------

    @Test
    fun withTouchExplorationOnTheBannersUndoMovesFocusToTheBoardStop() {
        startGame(board = STRANDING_BOARD, touchExploration = true)
        tapCell(Position(3, 1))
        tapCell(CENTRE)

        bannerButton(R.string.undo_action).performClick()
        settleFocusMove()

        composeRule.onNodeWithTag(BOARD_STOP_TEST_TAG).assertIsFocused()
    }

    @Test
    fun withTouchExplorationOffTheBannersRestartMovesNoFocus() {
        startGame(board = STRANDING_BOARD, touchExploration = false)
        tapCell(Position(3, 1))
        tapCell(CENTRE)

        bannerButton(R.string.rescue_restart).performClick()
        composeRule.waitForIdle()

        val stop = composeRule.onNodeWithTag(BOARD_STOP_TEST_TAG).fetchSemanticsNode().config
        assertThat(stop.getOrNull(SemanticsProperties.Focused)).isNull()
    }

    // ---- Loop 1, F-2: a focus move is always a change, even onto the focused target -------------

    @Test
    fun aSecondBannerUndoMovesFocusToTheBoardStopAgain() {
        startGame(board = STRANDING_BOARD, touchExploration = true)
        strand()
        bannerButton(R.string.undo_action).performClick()
        settleFocusMove()
        composeRule.onNodeWithTag(BOARD_STOP_TEST_TAG).assertIsFocused()
        strand()

        composeRule.mainClock.autoAdvance = false
        bannerButton(R.string.undo_action).performClick()

        assertLostThenRegained(focusTrace { composeRule.onNodeWithTag(BOARD_STOP_TEST_TAG) })
    }

    @Test
    fun aRetriedJumpActionMovesFocusToItsDestinationAgain() {
        val game = startGame(touchExploration = true)
        takeAction(ABOVE_CENTRE, "Jump down to row 4, column 4")
        settleFocusMove()
        cellNode(CENTRE).assertIsFocused()
        composeRule.runOnIdle { game.onUndo() }
        composeRule.waitForIdle()

        composeRule.mainClock.autoAdvance = false
        val again = cellNode(ABOVE_CENTRE).fetchSemanticsNode().config[SemanticsActions.CustomActions].single()
        composeRule.runOnIdle { again.action() }

        assertLostThenRegained(focusTrace { cellNode(CENTRE) })
    }

    // ---- Loop 1, F-3: the banner moves focus only when its action happened ----------------------

    @Test
    fun aBannerUndoThatOpensTheAdOfferMovesNoFocus() {
        val game = startGame(board = STRANDING_BOARD, touchExploration = true)
        repeat(AssistAllowanceLimits.FREE_UNDOS) {
            strand()
            composeRule.runOnIdle { game.onUndo() }
            composeRule.waitForIdle()
        }
        strand()

        bannerButton(R.string.undo_action).performClick()
        settleFocusMove()

        assertThat(game.uiState.value.pendingAdOffer).isNotNull()
        assertThat(boardStopFocused()).isFalse()
    }

    @Test
    fun aBannerRestartThatOpensTheConfirmMovesNoFocus() {
        val game = startGame(hint = fixedHint(HintOutcome.ProvenDead), touchExploration = true)
        tapCell(ABOVE_CENTRE)
        tapCell(CENTRE)
        composeRule.runOnIdle { game.onHintRequested() }
        composeRule.waitForIdle()

        bannerButton(R.string.rescue_restart).performClick()
        settleFocusMove()

        assertThat(game.uiState.value.confirmRestart).isTrue()
        assertThat(boardStopFocused()).isFalse()
    }

    // ---- Loop 2, L1-1 and L1-3: a banner Restart moves focus once the restart has happened -------

    @Test
    fun aBannerRestartOnAStuckBoardMovesFocusToTheBoardStop() {
        startGame(board = STRANDING_BOARD, touchExploration = true)
        strand()

        composeRule.mainClock.autoAdvance = false
        bannerButton(R.string.rescue_restart).performClick()

        assertLostThenRegained(focusTrace { composeRule.onNodeWithTag(BOARD_STOP_TEST_TAG) })
    }

    @Test
    fun aBannerRestartConfirmedInItsDialogMovesFocusToTheBoardStop() {
        val game = noWinningMoveBannerUp()
        bannerButton(R.string.rescue_restart).performClick()
        settleFocusMove()
        assertThat(game.uiState.value.confirmRestart).isTrue()
        assertThat(boardStopFocused()).isFalse()

        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { game.onConfirmRestart() }

        assertThat(game.uiState.value.restartTick).isEqualTo(1)
        assertLostThenRegained(focusTrace { composeRule.onNodeWithTag(BOARD_STOP_TEST_TAG) })
    }

    @Test
    fun aBannerRestartDeclinedInItsDialogMovesNoFocusThenOrOnALaterRestart() {
        val game = noWinningMoveBannerUp()
        bannerButton(R.string.rescue_restart).performClick()
        composeRule.runOnIdle { game.onDismissRestart() }
        settleFocusMove()
        assertThat(boardStopFocused()).isFalse()

        // The dock's Restart, confirmed: no longer the banner's, so it moves nothing (§11.1).
        composeRule.runOnIdle { game.onRestart() }
        composeRule.runOnIdle { game.onConfirmRestart() }
        settleFocusMove()

        assertThat(game.uiState.value.restartTick).isEqualTo(1)
        assertThat(boardStopFocused()).isFalse()
    }

    // ---- 26b's pin (D-TBa loop 5, F-38): the slab before the banner on every window -----------

    @Test
    @Config(qualifiers = HEIGHT_LIMITED_QUALIFIERS)
    fun onAWindowThatLimitsTheBoardByHeightTheSlabIsOrderedBeforeTheBanner() {
        showState(GameUiState.initial(Boards.english33Initial).copy(stuck = true))

        val region = composeRule.onNodeWithTag(GAME_STATE_TEST_TAG).fetchSemanticsNode()
        val slab = composeRule.onNodeWithTag(GAME_BOARD_SLAB_TEST_TAG).fetchSemanticsNode()
        val message = composeRule.onNodeWithText(string(R.string.stuck_message)).fetchSemanticsNode()
        val banner = composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).fetchSemanticsNode()

        assertThat(region.config.getOrNull(SemanticsProperties.IsTraversalGroup)).isTrue()
        assertThat(slab.parent?.id).isEqualTo(region.id)
        assertThat(ancestorsOf(message).map { it.id }).contains(region.id)
        assertThat(slab.config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f)
            .isLessThan(banner.config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f)
        assertThat(message.config.getOrNull(SemanticsProperties.TraversalIndex)).isNull()
        assertThat(banner.boundsInRoot.left).isLessThan(slab.boundsInRoot.left)
    }

    /** Defect ruling §1: a service is handed the board's channel, the window's one live region, a leaf. */
    @Test
    fun aServiceIsHandedTheBoardsChannelAsTheWindowsOnlyLiveRegionAndALeaf() {
        turnOnATouchExplorationService()
        showState(GameUiState.initial(Boards.english33Initial))

        val regions = composeRule.liveRegionsHandedOut()
        assertThat(regions).hasSize(1)
        assertThat(regions.single().childCount).isEqualTo(0)
    }

    /**
     * Defect ruling §9 (B-9): with the banner up over a board limited by height, the cells, the
     * banner and the dock cover what shows of the canvas; the channel is still handed out, a leaf,
     * beside the banner's own region (23k).
     */
    @Test
    @Config(qualifiers = HEIGHT_LIMITED_QUALIFIERS)
    fun withTheBannerUpOverAHeightLimitedBoardAServiceIsStillHandedTheBoardsChannel() {
        turnOnATouchExplorationService()
        showState(GameUiState.initial(Boards.english33Initial).copy(stuck = true))

        val regions = composeRule.liveRegionsHandedOut()
        val board = regions.filterNot { it.text?.toString() == string(R.string.stuck_message) }
        assertThat(regions).hasSize(2)
        assertThat(board).hasSize(1)
        assertThat(board.single().childCount).isEqualTo(0)
    }

    /**
     * Defect ruling §3 (B-3): on gameplay the board stop follows the strip's last stop in the order —
     * the title, until home-chip-arrival's timer fix reaches this branch — and precedes row 1's first cell.
     */
    @Test
    fun theBoardStopFollowsTheTitleAndPrecedesTheFirstCell() {
        turnOnATouchExplorationService()
        startGame(touchExploration = true)
        val stop = composeRule.onNodeWithContentDescription("Board, ", substring = true).fetchSemanticsNode()
        val title = composeRule.onNodeWithText(string(R.string.app_name)).fetchSemanticsNode()

        assertThat(composeRule.traversalBeforeOf(title)).isEqualTo(stop.id)
        assertThat(
            composeRule.traversalBeforeOf(stop),
        ).isEqualTo(cellNode(Position(row = 0, col = 2)).fetchSemanticsNode().id)
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    private fun startGame(
        board: Board = Boards.english33Initial,
        hint: GetHintUseCase = GetHintUseCase(),
        touchExploration: Boolean = false,
    ): GameViewModel {
        val manager = RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setTouchExplorationEnabled(touchExploration)
        val game = newGame(board, hint)
        viewModel = game
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalHapticFeedback provides haptics, LocalSoundPlayer provides sounds) {
                    val current = viewModel
                    if (shown && current != null) key(current) { GameScreenOver(current) }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.assertInTouchMode()
        return game
    }

    private fun swapInANewGame(): GameViewModel {
        val game = newGame(Boards.english33Initial, GetHintUseCase())
        composeRule.runOnIdle { viewModel = game }
        composeRule.waitForIdle()
        return game
    }

    private fun newGame(
        board: Board,
        hint: GetHintUseCase,
    ): GameViewModel =
        GameViewModel(
            rulesEngine = RulesEngine,
            levelRepository = OneClassicBoard(board),
            scoreCalculator = ScoreCalculator,
            settingsRepository = TalkBackSettings,
            challengeProgress = mockk(relaxed = true),
            progressRepository = mockk(relaxed = true),
            timeSource = TestTimeSource(),
            defaultDispatcher = Dispatchers.Unconfined,
            adGateway = NoOpAdGateway(),
            getHintUseCase = hint,
            savedStateHandle = SavedStateHandle(mapOf(LEVEL_ID_ARG to ClassicLevelIds.ENGLISH33)),
        )

    private fun showState(state: GameUiState) {
        composeRule.setContent {
            AppTheme {
                GameScreen(
                    state = state,
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
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.assertInTouchMode()
    }

    /** The script of TB-4: select, re-tap, select, jump, a hole with nothing held, another peg held. */
    private fun record(
        game: GameViewModel,
        tap: (Position) -> Unit,
    ): List<GameUiState> =
        listOf(ABOVE_CENTRE, ABOVE_CENTRE, ABOVE_CENTRE, CENTRE, ABOVE_CENTRE, BELOW_CENTRE_TWO, TOP_LEFT_PEG).map {
            tap(it)
            settled(game)
        }

    /** The game's state once the screen is idle, minus the clock, which no tap decides. */
    private fun settled(game: GameViewModel): GameUiState {
        composeRule.waitForIdle()
        return game.uiState.value.copy(elapsedSeconds = 0)
    }

    private fun tapCell(cell: Position) {
        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(
                percentOffset(
                    x = (cell.col + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH_GRID,
                    y = (cell.row + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH_GRID,
                ),
            )
        }
        composeRule.waitForIdle()
    }

    private fun activate(cell: Position) {
        cellNode(cell).performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
    }

    private fun takeAction(
        cell: Position,
        label: String,
    ) {
        val action =
            cellNode(cell).fetchSemanticsNode().config[SemanticsActions.CustomActions].single {
                it.label ==
                    label
            }
        composeRule.runOnIdle { action.action() }
        composeRule.waitForIdle()
    }

    private fun cellNode(cell: Position): SemanticsNodeInteraction =
        composeRule.onNode(SemanticsMatcher.expectValue(BoardCellPositionKey, cell))

    private fun bannerButton(label: Int): SemanticsNodeInteraction =
        composeRule.onNode(
            hasClickAction() and hasText(string(label)) and hasAnyAncestor(hasTestTag(NOWIN_BANNER_TEST_TAG)),
        )

    /** The board's channel as a speech engine reads it: the repeat mark is unspoken. */
    private fun channel(): String? =
        composeRule
            .onNodeWithTag(ANNOUNCEMENT_REGION_TEST_TAG)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
            ?.single()
            ?.replace(ZERO_WIDTH_SPACE, "")

    private fun assertChannel(line: String) {
        val region = composeRule.onNodeWithTag(ANNOUNCEMENT_REGION_TEST_TAG).fetchSemanticsNode().config
        assertThat(region.getOrNull(SemanticsProperties.LiveRegion)).isNotNull()
        assertThat(channel()).isEqualTo(line)
    }

    /** One move made, then a Hint that proves the position lost: the banner over a game in play. */
    private fun noWinningMoveBannerUp(): GameViewModel {
        val game = startGame(hint = fixedHint(HintOutcome.ProvenDead), touchExploration = true)
        tapCell(ABOVE_CENTRE)
        tapCell(CENTRE)
        composeRule.runOnIdle { game.onHintRequested() }
        composeRule.waitForIdle()
        check(game.uiState.value.noWinningMove) { "the banner must be up" }
        return game
    }

    /** (3, 1) jumps into the centre on [STRANDING_BOARD], leaving it stuck under the banner. */
    private fun strand() {
        tapCell(Position(3, 1))
        tapCell(CENTRE)
    }

    /** Lets a focus move finish: its gap, then a frame. */
    private fun settleFocusMove() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(FOCUS_MOVE_GAP_MS + FRAME_MS)
        composeRule.waitForIdle()
    }

    private fun boardStopFocused(): Boolean =
        composeRule
            .onNodeWithTag(
                BOARD_STOP_TEST_TAG,
            ).fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Focused) ==
            true

    /** The node's input focus, frame by frame on the paused clock, across a whole focus move. */
    private fun focusTrace(node: () -> SemanticsNodeInteraction): List<Boolean> =
        List(TRACE_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            node().fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused) == true
        }

    /** The target lost focus and then had it again: a change TalkBack is sent and follows. */
    private fun assertLostThenRegained(trace: List<Boolean>) {
        assertThat(trace).contains(false)
        assertThat(trace.last()).isTrue()
        assertThat(trace.indexOf(false)).isLessThan(trace.lastIndexOf(true))
    }

    private fun ancestorsOf(node: SemanticsNode): List<SemanticsNode> =
        generateSequence(node.parent) { it.parent }.toList()

    private fun string(id: Int): String = RuntimeEnvironment.getApplication().getString(id)

    private fun fixedHint(outcome: HintOutcome): GetHintUseCase =
        mockk { every { this@mockk.invoke(any(), any(), any(), any(), any()) } returns outcome }

    private companion object {
        val CENTRE = Position(row = 3, col = 3)
        val ABOVE_CENTRE = Position(row = 1, col = 3)

        /** After the opening jump, (4, 3)'s only jump lands on (2, 3). */
        val BELOW_CENTRE_TWO = Position(row = 4, col = 3)
        val TOP_LEFT_PEG = Position(row = 0, col = 2)
        val DOWN_INTO_THE_CENTRE = Move(from = ABOVE_CENTRE, over = Position(2, 3), to = CENTRE)

        /** Three pegs: (3, 1) jumps (3, 2) into the centre, and (0, 2) is left stranded with it. */
        val STRANDING_BOARD =
            listOf(Position(3, 1), Position(3, 2), Position(0, 2))
                .fold(Board(spec = Boards.English33, pegs = 0L)) { board, peg -> board.withPeg(peg) }

        const val PAST_THE_HOLD_MS = 2_000L
        const val FRAME_MS = 16L

        /** Frames enough for a focus move's clear, its gap and its request (about half a second). */
        const val TRACE_FRAMES = 30
        const val ZERO_WIDTH_SPACE = "​"
        const val ENGLISH_GRID = 7
        const val LEVEL_ID_ARG = "levelId"
    }
}

/** The screen over [game], every callback the ViewModel's own, as `GameRoute` wires it. */
@Composable
private fun GameScreenOver(game: GameViewModel) {
    val state by game.uiState.collectAsStateWithLifecycle()
    GameScreen(
        state = state,
        onCellTap = game::onCellTap,
        onRestart = game::onRestart,
        onConfirmRestart = game::onConfirmRestart,
        onDismissRestart = game::onDismissRestart,
        onAnimationFinished = game::onAnimationFinished,
        onToggleHints = game::onToggleHints,
        onUndo = game::onUndo,
        onHint = game::onHintRequested,
        onAcceptAdOffer = game::onAcceptAdOffer,
        onDismissAdOffer = game::onDismissAdOffer,
        onHome = {},
        onNavigateBack = {},
    )
}

private const val TALKBACK_SCREEN_SDK = 34
private const val TALKBACK_SCREEN_QUALIFIERS = "w400dp-h800dp-hdpi"

/** Landscape on a phone: the board is limited by height and the banner spans wider than the slab. */
private const val HEIGHT_LIMITED_QUALIFIERS = "w640dp-h360dp"

/** Serves one board through the Classic path. */
private class OneClassicBoard(
    private val board: Board,
) : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> = emptyList()

    override fun loadClassic(levelId: String): Level =
        Level(
            id = levelId,
            displayName = levelId,
            spec = board.spec,
            initialBoard = board,
            holeCount = board.spec.inPlay.size,
        )

    override fun loadChallenge(levelNumber: Int): Level = error("Classic only")
}

/** Show-moves, Sound and Haptics on. */
private object TalkBackSettings : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)
    override val soundOn: Flow<Boolean> = flowOf(true)
    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}
