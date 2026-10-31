package com.app.pegsolitaire.ui.components

import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.testing.nodeInfoHandedOut
import com.app.pegsolitaire.testing.turnOnATouchExplorationService
import com.app.pegsolitaire.ui.components.BOARD_STOP_TEST_TAG
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The board's stops (D-TB as amended by D-TBa, `docs/ux-spec.md` §11.1): a [BoardCanvas] hosted on a
 * slab that carries [boardStop], over hand-built states, with a recording `onCellTap`.
 *
 * TB-1 (the board stop and its no-op click), TB-2 (one node per in-play cell, row by row), TB-4's
 * labels and activation, TB-5 (one action per legal jump, run as the two taps), TB-7's structural
 * proxy (a cell keeps its node), and TB-11 (a finger still reaches the canvas, a read-only board has
 * no stop, and nothing takes input focus unless touch exploration is on — set here on Robolectric's
 * accessibility manager before the content is composed).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SEMANTICS_SDK], qualifiers = SEMANTICS_QUALIFIERS)
class BoardSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val taps = mutableListOf<Position>()

    // ---- TB-1: the board stop -------------------------------------------------------------------

    @Test
    fun theBoardStopSpeaksTheBoardAndItsPegsLeftWithNoRole() {
        show(START)

        val stop = boardStop().fetchSemanticsNode().config
        assertThat(stop.getOrNull(SemanticsProperties.ContentDescription)).containsExactly("Board, 32 pegs left")
        assertThat(stop.getOrNull(SemanticsProperties.Role)).isNull()
        // Loop 3 (defect ruling §3): the cells' group is the slab's own, and the stop is read first in it.
        val slab = composeRule.onNodeWithTag(SLAB_TAG).fetchSemanticsNode().config
        assertThat(slab.getOrNull(SemanticsProperties.IsTraversalGroup)).isTrue()
        assertThat(stop.getOrNull(SemanticsProperties.TraversalIndex)).isEqualTo(BoardTraversal.STOP_BEFORE_CELLS)
    }

    @Test
    fun theBoardStopsClickIsUnlabelledAndActivatingItReachesNoCell() {
        show(START)

        val click = boardStop().fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)
        assertThat(click).isNotNull()
        assertThat(click?.label).isNull()
        boardStop().performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        assertThat(taps).isEmpty()
    }

    // ---- TB-2: one node per in-play cell, row by row ------------------------------------------

    @Test
    fun englishHasThirtyThreeCellNodesInRowMajorTraversalOrder() {
        show(START)

        val nodes = cellNodes()
        val positions = nodes.map { it.config[BoardCellPositionKey] }
        assertThat(positions).containsExactlyElementsIn(Boards.English33.footprint).inOrder()
        assertThat(nodes.map { it.config[SemanticsProperties.TraversalIndex] })
            .isEqualTo(List(nodes.size) { it.toFloat() })
    }

    @Test
    fun diamondHasFortyOneCellNodes() {
        show(GameUiState.initial(Boards.diamond41Initial))

        assertThat(cellNodes()).hasSize(DIAMOND_CELLS)
    }

    @Test
    fun eachCellNodeSpeaksItsOwnTextAndNothingElse() {
        show(START)

        val first = cell(Position(0, 2)).fetchSemanticsNode().config
        assertThat(
            first.getOrNull(SemanticsProperties.ContentDescription),
        ).containsExactly("Row 1, column 3, peg, no jumps")
        assertThat(first.getOrNull(SemanticsProperties.Role)).isNull()
        assertThat(first.getOrNull(SemanticsProperties.StateDescription)).isNull()
        assertThat(first.getOrNull(SemanticsProperties.Selected)).isNull()
        assertThat(description(CENTRE)).isEqualTo("Row 4, column 4, empty")
    }

    // ---- TB-4: the tap's label, and a double-tap is the tap ------------------------------------

    @Test
    fun withNothingSelectedEveryPegIsLabelledSelectAndAHoleHasNoLabel() {
        show(START)

        assertThat(clickLabel(ABOVE_CENTRE)).isEqualTo("select")
        assertThat(clickLabel(Position(0, 2))).isEqualTo("select")
        assertThat(clickLabel(CENTRE)).isNull()
    }

    @Test
    fun withAPegSelectedItSaysPutDownItsTargetSaysJumpHereAndEveryOtherCellHasNoLabel() {
        show(selecting(ABOVE_CENTRE))

        assertThat(clickLabel(ABOVE_CENTRE)).isEqualTo("put down")
        assertThat(clickLabel(CENTRE)).isEqualTo("jump here")
        assertThat(clickLabel(BELOW_CENTRE)).isNull()
        assertThat(description(ABOVE_CENTRE)).isEqualTo("Row 2, column 4, peg, selected, 1 jump")
        assertThat(description(CENTRE)).isEqualTo("Row 4, column 4, empty, jump target")
    }

    @Test
    fun activatingAnyCellIsATapOnThatCellEvenWhereTheTapWillBeRejected() {
        show(START)

        cell(ABOVE_CENTRE).performSemanticsAction(SemanticsActions.OnClick)
        cell(CENTRE).performSemanticsAction(SemanticsActions.OnClick)

        assertThat(taps).containsExactly(ABOVE_CENTRE, CENTRE).inOrder()
    }

    // ---- TB-5: one action per legal jump, run as the two taps ----------------------------------

    @Test
    fun withNothingSelectedAPegOffersOneActionPerLegalJumpAndTakingItSelectsThenJumps() {
        show(START)

        assertThat(actionLabels(ABOVE_CENTRE)).containsExactly("Jump down to row 4, column 4")
        assertThat(actionLabels(CENTRE)).isEmpty()
        assertThat(actionLabels(Position(0, 2))).isEmpty()
        val withActions = cellNodes().count { !it.config.getOrNull(SemanticsActions.CustomActions).isNullOrEmpty() }
        assertThat(
            withActions,
        ).isEqualTo(
            RulesEngine
                .legalMoves(Boards.english33Initial)
                .map { it.from }
                .distinct()
                .size,
        )

        takeAction(ABOVE_CENTRE, "Jump down to row 4, column 4")
        assertThat(taps).containsExactly(ABOVE_CENTRE, CENTRE).inOrder()
    }

    @Test
    fun withAPegSelectedOnlyItOffersItsActionsAndTakingOneIsTheDestinationTap() {
        show(selecting(ABOVE_CENTRE))

        assertThat(actionLabels(BELOW_CENTRE)).isEmpty()
        assertThat(actionLabels(Position(3, 1))).isEmpty()
        takeAction(ABOVE_CENTRE, "Jump down to row 4, column 4")

        assertThat(taps).containsExactly(CENTRE)
    }

    // ---- TB-3 (Compose half): the words do not follow Show-moves -----------------------------

    @Test
    fun everyCellSaysTheSameWithShowMovesOnAndOff() {
        var state by mutableStateOf(selecting(ABOVE_CENTRE).copy(showHints = true))
        showLive { state }
        val withDots = cellNodes().map { it.config[SemanticsProperties.ContentDescription] }

        state = state.copy(showHints = false)
        composeRule.waitForIdle()
        val withoutDots = cellNodes().map { it.config[SemanticsProperties.ContentDescription] }

        assertThat(withDots).hasSize(Boards.English33.size)
        assertThat(withoutDots).isEqualTo(withDots)
    }

    // ---- TB-7's structural proxy: a cell keeps its node -----------------------------------------

    @Test
    fun aCellKeepsItsNodeWhileTheBoardChangesUnderIt() {
        var state by mutableStateOf(START)
        showLive { state }
        val before = cell(CENTRE).fetchSemanticsNode().id

        state = afterJumpingDownIntoTheCentre()
        composeRule.waitForIdle()

        assertThat(cell(CENTRE).fetchSemanticsNode().id).isEqualTo(before)
        assertThat(description(CENTRE)).isEqualTo("Row 4, column 4, peg, no jumps")
    }

    // ---- TB-11: sighted play is untouched --------------------------------------------------------

    @Test
    fun aFingerTapStillReachesTheCanvasThroughTheCellLayer() {
        show(START)

        composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).performTouchInput {
            click(percentOffset(cellX(ABOVE_CENTRE), cellY(ABOVE_CENTRE)))
        }
        composeRule.waitForIdle()

        assertThat(taps).containsExactly(ABOVE_CENTRE)
    }

    @Test
    fun aReadOnlyBoardHasNoCellNodeNoStopAndNoAction() {
        show(START, interactive = false)

        assertThat(cellNodes()).isEmpty()
        val nodes =
            composeRule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick),
                ).fetchSemanticsNodes()
        assertThat(nodes).isEmpty()
        val canvas = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).fetchSemanticsNode().config
        assertThat(canvas.getOrNull(SemanticsProperties.ContentDescription)).isNull()
    }

    @Test
    fun withTouchExplorationOffNoCellAndNoBoardStopCanTakeInputFocus() {
        show(START, touchExploration = false)

        assertThat(cellNodes()).hasSize(Boards.English33.size)
        for (node in cellNodes() + boardStop().fetchSemanticsNode()) {
            assertWithMessage("${node.config.getOrNull(BoardCellPositionKey) ?: "the board stop"}")
                .that(
                    node.config.getOrNull(SemanticsActions.RequestFocus) == null &&
                        node.config.getOrNull(SemanticsProperties.Focused) == null,
                ).isTrue()
        }
    }

    @Test
    fun withTouchExplorationOnEveryCellAndTheBoardStopCanTakeInputFocus() {
        show(START, touchExploration = true)

        for (node in cellNodes() + boardStop().fetchSemanticsNode()) {
            assertWithMessage("${node.config.getOrNull(BoardCellPositionKey) ?: "the board stop"}")
                .that(
                    node.config.getOrNull(SemanticsActions.RequestFocus) != null &&
                        node.config.getOrNull(SemanticsProperties.Focused) != null,
                ).isTrue()
        }
    }

    @Test
    fun withTouchExplorationOnAJumpActionMovesFocusToTheDestination() {
        show(START, touchExploration = true)

        takeAction(ABOVE_CENTRE, "Jump down to row 4, column 4")
        composeRule.mainClock.advanceTimeBy(FOCUS_MOVE_GAP_MS + FRAME_MS)
        composeRule.waitForIdle()

        cell(CENTRE).assertIsFocused()
    }

    @Test
    fun withTouchExplorationOffAJumpActionRequestsNoFocus() {
        show(START, touchExploration = false)

        takeAction(ABOVE_CENTRE, "Jump down to row 4, column 4")

        assertThat(taps).containsExactly(ABOVE_CENTRE, CENTRE).inOrder()
        assertThat(cell(CENTRE).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Focused)).isNull()
    }

    @Test
    fun turningTouchExplorationOnAfterTheBoardIsShownLetsTheCellsTakeFocus() {
        show(START, touchExploration = false)

        setTouchExploration(true)
        composeRule.waitForIdle()

        cell(CENTRE).assertIsNotFocused()
        assertThat(cell(CENTRE).fetchSemanticsNode().config.getOrNull(SemanticsActions.RequestFocus)).isNotNull()
    }

    // ---- Helpers ---------------------------------------------------------------------------------

    /**
     * Defect ruling §3 (B-3): a service is handed the board stop as a screen-reader-focusable leaf;
     * on the slab, which has the board as its children and merges nothing, Compose counts it as neither.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // Compose prunes with android.graphics.Region
    fun aServiceIsHandedTheBoardStopAsAScreenReaderFocusableLeaf() {
        turnOnATouchExplorationService()
        show(START, touchExploration = true)
        val stop = composeRule.onNodeWithContentDescription("Board, ", substring = true).fetchSemanticsNode()
        val info = checkNotNull(composeRule.nodeInfoHandedOut(stop)) { "the board stop is not handed out" }

        assertThat(info.isScreenReaderFocusable).isTrue()
        assertThat(info.childCount).isEqualTo(0)
    }

    private fun show(
        state: GameUiState,
        interactive: Boolean = true,
        touchExploration: Boolean = false,
    ) {
        setTouchExploration(touchExploration)
        showLive(interactive) { state }
    }

    private fun showLive(
        interactive: Boolean = true,
        state: () -> GameUiState,
    ) {
        composeRule.setContent {
            AppTheme {
                val current = state()
                val stopFocus = remember { FocusRequester() }
                // As the hosts compose it: the slab is the cells' group, the board stop its first child.
                val group = if (interactive) Modifier.semantics { isTraversalGroup = true } else Modifier
                Surface(modifier = Modifier.size(SLAB_SIZE.dp).testTag(SLAB_TAG).then(group)) {
                    if (interactive) {
                        BoardStop(
                            label = boardStopLabel(current.board.pegCount),
                            focusRequester = stopFocus,
                            focusEnabled = rememberTouchExplorationEnabled(),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    BoardCanvas(
                        state = current,
                        onCellTap = { taps += it },
                        onAnimationFinished = {},
                        modifier = Modifier.padding(SLAB_PADDING.dp),
                        interactive = interactive,
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun setTouchExploration(enabled: Boolean) {
        val manager = RuntimeEnvironment.getApplication().getSystemService(AccessibilityManager::class.java)
        shadowOf(manager).setTouchExplorationEnabled(enabled)
    }

    private fun boardStop(): SemanticsNodeInteraction = composeRule.onNodeWithTag(BOARD_STOP_TEST_TAG)

    private fun cell(position: Position): SemanticsNodeInteraction =
        composeRule.onNode(SemanticsMatcher.expectValue(BoardCellPositionKey, position))

    private fun cellNodes(): List<SemanticsNode> =
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(BoardCellPositionKey)).fetchSemanticsNodes()

    private fun description(position: Position): String? =
        cell(position)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
            ?.single()

    private fun clickLabel(position: Position): String? =
        cell(position)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.OnClick)
            ?.label

    private fun actionLabels(position: Position): List<String> =
        cell(position)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            .orEmpty()
            .map { it.label }

    private fun takeAction(
        position: Position,
        label: String,
    ) {
        val action =
            cell(position).fetchSemanticsNode().config[SemanticsActions.CustomActions].single { it.label == label }
        composeRule.runOnIdle { action.action() }
        composeRule.waitForIdle()
    }

    private fun selecting(peg: Position): GameUiState =
        START.copy(
            selected = peg,
            legalTargets = RulesEngine.legalMoves(Boards.english33Initial).filter { it.from == peg }.map { it.to },
        )

    private fun afterJumpingDownIntoTheCentre(): GameUiState {
        val move = RulesEngine.legalMoves(Boards.english33Initial).single { it.from == ABOVE_CENTRE }
        return START.copy(board = RulesEngine.applyMove(Boards.english33Initial, move))
    }

    private fun cellX(position: Position): Float = (position.col + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH_GRID

    private fun cellY(position: Position): Float = (position.row + BoardMetrics.CELL_CENTER_FRACTION) / ENGLISH_GRID

    private companion object {
        val START = GameUiState.initial(Boards.english33Initial)
        val CENTRE = Position(row = 3, col = 3)
        val ABOVE_CENTRE = Position(row = 1, col = 3)
        val BELOW_CENTRE = Position(row = 5, col = 3)

        const val SLAB_TAG = "test_slab"
        const val SLAB_SIZE = 360
        const val SLAB_PADDING = 16
        const val ENGLISH_GRID = 7
        const val DIAMOND_CELLS = 41
        const val FRAME_MS = 16L
    }
}

/** Robolectric has no android-all jar for SDK 36 yet; the suite emulates 34. */
private const val SEMANTICS_SDK = 34

/** A tall phone, like the wrong-tap suites. */
private const val SEMANTICS_QUALIFIERS = "w400dp-h800dp-hdpi"
