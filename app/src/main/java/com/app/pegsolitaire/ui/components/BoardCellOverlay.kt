package com.app.pegsolitaire.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Test tag of the layer that carries the cells' stops over the board's canvas. */
const val BOARD_CELLS_TEST_TAG = "board_cells"

/**
 * Semantics: the cell a stop speaks for. Test-only, like `PegCountKey`: TalkBack never reads it, and
 * no stop's spoken content depends on it.
 */
val BoardCellPositionKey = SemanticsPropertyKey<Position>("BoardCellPosition")

private var SemanticsPropertyReceiver.boardCellPosition by BoardCellPositionKey

/**
 * One TalkBack stop per in-play cell (D-TB as amended by D-TBa, `docs/ux-spec.md` §11.1), laid over
 * the board's canvas by the same [BoardGeometry] that draws the cells and hit-tests a finger, so
 * explore-by-touch finds the cell under the finger.
 *
 * - **No pointer input.** The layer attaches no pointer or gesture handler of any kind, so a finger
 *   tap passes through it to the canvas exactly as before (TB-11). A cell's activation is a
 *   semantics action that calls the same [onCellTap] a finger reaches (TB-4).
 * - **The geometry is measured, never remembered from the canvas.** It is built here from this
 *   layer's own constraints, which are the canvas's size: a size read back from the canvas would be
 *   a frame late, and TalkBack caches the bounds it is given.
 * - **A cell keeps its node.** Every cell is composed for the whole footprint and keyed on its
 *   position, so a cell TalkBack has focused keeps focus while the board changes under it (TB-7).
 * - **Input focus only while touch exploration is on** ([touchExploration], D-TBa item 5 and its
 *   loop-5 line). With it off, Tab and D-pad traversal are what they were, and no focus is ever
 *   requested. With it on, a jump taken by action moves focus to the destination cell.
 *
 * Interactive boards only: `BoardCanvas` composes this under its interactive branch, so a read-only
 * board has no stop (D-TBa item 9).
 *
 * @param spec the footprint being drawn; the cells' places and focus requesters follow it.
 * @param speech what each cell says and offers, in stop order.
 * @param touchExploration whether the platform's screen-reader signal is on.
 * @param onCellTap the board's tap, hoisted: every activation and every jump action ends here.
 */
@Composable
internal fun BoardCellOverlay(
    spec: BoardSpec,
    speech: BoardSpeech,
    touchExploration: Boolean,
    onCellTap: (Position) -> Unit,
    modifier: Modifier = Modifier,
) {
    val format = rememberBoardSpeechFormat()
    val requesters = remember(spec) { spec.inPlay.associateWith { FocusRequester() } }
    val measurePolicy = remember(spec) { cellsMeasurePolicy(spec) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val onJumpTaken: (Position) -> Unit = { to ->
        val target = requesters[to]
        if (touchExploration && target != null) scope.launch { focusManager.moveInputFocusTo(target) }
    }
    Layout(
        content = {
            speech.cells.forEachIndexed { stopIndex, cell ->
                key(cell.position) {
                    BoardCellStop(
                        cell = cell,
                        stopIndex = stopIndex,
                        format = format,
                        focusRequester = requesters.getValue(cell.position),
                        touchExploration = touchExploration,
                        onCellTap = onCellTap,
                        onJumpTaken = onJumpTaken,
                    )
                }
            }
        },
        modifier = modifier,
        measurePolicy = measurePolicy,
    )
}

/**
 * The board stop (§11.1, stop 1), applied by each host to its board's slab so a touch on the slab's
 * rim finds the board: [label] ("Board, 32 pegs left"), no role, and a click action that does
 * nothing (D-TBa item 8), so a double-tap on it is consumed and never falls through to a cell. The
 * slab is the cells' traversal group, so every cell is read before anything that overlaps the slab.
 *
 * @param focusRequester where a host moves focus after a step change or a banner's Undo or Restart.
 * @param focusEnabled whether the stop can take input focus — only while touch exploration is on.
 */
internal fun Modifier.boardStop(
    label: String,
    focusRequester: FocusRequester,
    focusEnabled: Boolean,
): Modifier =
    this
        .focusRequester(focusRequester)
        .focusable(enabled = focusEnabled)
        .semantics {
            isTraversalGroup = true
            contentDescription = label
            onClick(label = null) { true }
        }

/** The board stop's words for [pegsLeft]: "Board, 32 pegs left". */
@Composable
internal fun boardStopLabel(pegsLeft: Int): String = rememberBoardSpeechFormat().boardLabel(pegsLeft)

/** The board's words, read from the composition's resources so a locale change re-renders them. */
@Composable
internal fun rememberBoardSpeechFormat(): BoardSpeechFormat {
    val resources = LocalResources.current
    return remember(resources) { BoardSpeechFormat(resources) }
}

/**
 * One cell's stop: its description, its tap as an activation with §11.1's label, and its jumps as
 * custom actions, each the two taps the jump is ([BoardSpeechModel.tapsForJump]). No role, no
 * `selected` and no state description: the text is the cell's only spoken content.
 */
@Composable
private fun BoardCellStop(
    cell: CellSpeech,
    stopIndex: Int,
    format: BoardSpeechFormat,
    focusRequester: FocusRequester,
    touchExploration: Boolean,
    onCellTap: (Position) -> Unit,
    onJumpTaken: (Position) -> Unit,
) {
    val description = remember(cell, format) { format.cellDescription(cell) }
    val tapLabel = remember(cell.tap, format) { cell.tap?.let(format::tapLabel) }
    val actions =
        cell.actions.map { action ->
            CustomAccessibilityAction(format.jumpActionLabel(action)) {
                val held = cell.position.takeIf { cell.selected }
                BoardSpeechModel.tapsForJump(held, cell.position, action.to).forEach(onCellTap)
                onJumpTaken(action.to)
                true
            }
        }
    Box(
        modifier =
            Modifier
                .layoutId(cell.position)
                .semantics {
                    contentDescription = description
                    onClick(label = tapLabel) {
                        onCellTap(cell.position)
                        true
                    }
                    if (actions.isNotEmpty()) customActions = actions
                    traversalIndex = stopIndex.toFloat()
                    boardCellPosition = cell.position
                }.focusRequester(focusRequester)
                .focusable(enabled = touchExploration),
    )
}

/** Places each cell's stop on its own square of the board, as [BoardGeometry] draws it. */
private fun cellsMeasurePolicy(spec: BoardSpec): MeasurePolicy =
    MeasurePolicy { measurables, constraints ->
        val geometry =
            BoardGeometry.forCanvas(
                spec,
                Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat()),
            )
        val side = geometry.cellSize.roundToInt()
        val square = Constraints.fixed(side, side)
        val placed =
            measurables.mapNotNull { measurable ->
                (measurable.layoutId as? Position)?.let {
                    measurable.measure(square) to
                        it
                }
            }
        layout(constraints.maxWidth, constraints.maxHeight) {
            for ((placeable, position) in placed) {
                val center = geometry.center(position)
                placeable.place(
                    x = (center.x - side / HALVES).roundToInt(),
                    y = (center.y - side / HALVES).roundToInt(),
                )
            }
        }
    }

/** A cell's centre is half its side from its top-left corner. */
private const val HALVES = 2f
