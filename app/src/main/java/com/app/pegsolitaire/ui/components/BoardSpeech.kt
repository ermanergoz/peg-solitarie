package com.app.pegsolitaire.ui.components

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.ui.game.WrongTapKind

/**
 * What the board says with TalkBack on (D-TB as amended by D-TBa, `docs/ux-spec.md` §11.1): one
 * [CellSpeech] per in-play cell, in stop order, and the pegs left the board stop speaks. Words only,
 * no strings — [BoardSpeechFormat] renders them — and no Android, so the model is tested on the JVM.
 *
 * Built by [BoardSpeechModel.of] from the board, the selection, `legalTargets` and the revealed hint,
 * and from nothing else (TB-12): the rules engine is read to *describe* the legal jumps a sighted
 * player sees at a glance, never to decide anything, and no solver is in reach
 * (`BoardSpeechPurityTest`).
 *
 * @property pegsLeft the pegs on the board, for the board stop's "Board, 32 pegs left".
 * @property cells every in-play cell, row by row from the top and left to right within a row.
 */
data class BoardSpeech(
    val pegsLeft: Int,
    val cells: List<CellSpeech>,
)

/**
 * One cell's spoken parts and what it offers, §11.1's "What a cell says" and its two action tables.
 *
 * @property position the cell, 0-based like every [Position] in code; spoken from 1.
 * @property content a peg or an empty hole — a blocked cell is never a stop.
 * @property selected whether this is the selected peg.
 * @property jumps on a peg, its legal jumps as the rules engine counts them, spoken with Show-moves on
 *   and off alike (GD-TB-2); `null` on a hole.
 * @property jumpTarget while a peg is selected, whether this empty hole is one of its legal targets.
 * @property hint which end of the revealed hint this cell is, while one is shown.
 * @property tap what a tap here does, as TalkBack's "double-tap to …" names it; `null` where the tap
 *   is rejected or the spec gives no label (TalkBack's default).
 * @property actions one per legal jump this cell offers, up, down, left, right.
 */
data class CellSpeech(
    val position: Position,
    val content: CellContent,
    val selected: Boolean,
    val jumps: Int?,
    val jumpTarget: Boolean,
    val hint: HintMark?,
    val tap: TapLabel?,
    val actions: List<JumpAction>,
)

/** A cell's content as spoken: "peg" or "empty". */
enum class CellContent { Peg, Empty }

/** The revealed hint's two ends: the peg to move and the hole it lands in. */
enum class HintMark { Source, Target }

/** The tap's action label, §11.1's table: "select", "put down", "jump here". */
enum class TapLabel { Select, PutDown, JumpHere }

/** One jump a peg offers as a TalkBack custom action: "Jump up to row 4, column 4". */
data class JumpAction(
    val direction: JumpDirection,
    val to: Position,
)

/**
 * The four directions, declared in the order the actions are listed (§11.1): up, down, left, right.
 * Up is towards row 1, left towards column 1. The board is never mirrored, so they mean the same in
 * every locale.
 */
enum class JumpDirection {
    Up,
    Down,
    Left,
    Right,
    ;

    companion object {
        /** The direction of an orthogonal jump from [from] to [to]. */
        fun of(
            from: Position,
            to: Position,
        ): JumpDirection =
            when {
                to.row < from.row -> Up
                to.row > from.row -> Down
                to.col < from.col -> Left
                else -> Right
            }
    }
}

/**
 * A board event the board's one polite channel speaks (§11.1, "Every change is spoken once"). Each
 * names only what the cells then say, so an announcement is never the only record.
 */
sealed interface BoardEvent {
    /** A peg picked up, with its legal jumps: "Selected row 2, column 4, 1 jump." */
    data class Selected(
        val cell: Position,
        val jumps: Int,
    ) : BoardEvent

    /** The selected peg put back down: "Peg put down, row 2, column 4." */
    data class PutDown(
        val cell: Position,
    ) : BoardEvent

    /** A jump, by tap or by action, with the peg it removed and the pegs left. */
    data class Jumped(
        val from: Position,
        val to: Position,
        val removed: Position,
        val pegsLeft: Int,
    ) : BoardEvent

    /** A tap the board refused, in D-TB's neutral words for its [kind]. */
    data class Rejected(
        val kind: WrongTapKind,
    ) : BoardEvent

    /** An undo, naming the jump it took back and the pegs left. */
    data class Undone(
        val from: Position,
        val to: Position,
        val pegsLeft: Int,
    ) : BoardEvent

    /** A revealed hint on gameplay, or a coach escalation in the tutorial. */
    data class Hinted(
        val from: Position,
        val to: Position,
    ) : BoardEvent

    /** A restart, with the pegs the fresh board has. */
    data class Restarted(
        val pegsLeft: Int,
    ) : BoardEvent
}

/** Builds [BoardSpeech] from the five inputs TB-12 allows, and names the taps a jump action makes. */
object BoardSpeechModel {
    /**
     * The board as spoken. [legalTargets] are the selected peg's, as the state holder computed them;
     * [hintSource] and [hintTarget] mark cells only as a pair, because only a pair is drawn.
     */
    fun of(
        board: Board,
        selected: Position?,
        legalTargets: List<Position>,
        hintSource: Position?,
        hintTarget: Position?,
    ): BoardSpeech {
        val jumpsFrom = RulesEngine.legalMoves(board).groupBy { it.from }
        val hint = if (hintSource != null && hintTarget != null) hintSource to hintTarget else null
        val cells =
            board.spec.inPlay
                .sortedWith(compareBy({ it.row }, { it.col }))
                .map { position ->
                    val isPeg = board.cellAt(position) == Cell.Peg
                    cellSpeech(
                        position = position,
                        isPeg = isPeg,
                        selected = selected,
                        jumpTarget = selected != null && !isPeg && position in legalTargets,
                        moves = if (isPeg) jumpsFrom[position].orEmpty() else emptyList(),
                        hint = hint,
                    )
                }
        return BoardSpeech(pegsLeft = board.pegCount, cells = cells)
    }

    /** The legal jumps from [peg] as the rules engine counts them — the number its cell speaks. */
    fun jumpsFrom(
        board: Board,
        peg: Position,
    ): Int = RulesEngine.legalMoves(board).count { it.from == peg }

    /**
     * The taps a jump action is (GD-TB-5): a selection of [from], unless it is already [selected],
     * then the tap on [to]. Run in order through the same `onCellTap` a finger reaches.
     */
    fun tapsForJump(
        selected: Position?,
        from: Position,
        to: Position,
    ): List<Position> = if (selected == from) listOf(to) else listOf(from, to)

    private fun cellSpeech(
        position: Position,
        isPeg: Boolean,
        selected: Position?,
        jumpTarget: Boolean,
        moves: List<Move>,
        hint: Pair<Position, Position>?,
    ): CellSpeech {
        val isSelected = position == selected
        // §11.1: the actions are on every peg with a jump while nothing is held, and on the held peg
        // alone while one is — never where two taps would not make the jump.
        val offersActions = isPeg && (selected == null || isSelected)
        return CellSpeech(
            position = position,
            content = if (isPeg) CellContent.Peg else CellContent.Empty,
            selected = isSelected,
            jumps = if (isPeg) moves.size else null,
            jumpTarget = jumpTarget,
            hint =
                when (position) {
                    hint?.first -> HintMark.Source
                    hint?.second -> HintMark.Target
                    else -> null
                },
            tap =
                when {
                    selected == null && isPeg -> TapLabel.Select
                    isSelected -> TapLabel.PutDown
                    jumpTarget -> TapLabel.JumpHere
                    else -> null
                },
            actions =
                if (offersActions) {
                    moves.map { JumpAction(JumpDirection.of(it.from, it.to), it.to) }.sortedBy { it.direction }
                } else {
                    emptyList()
                },
        )
    }
}
