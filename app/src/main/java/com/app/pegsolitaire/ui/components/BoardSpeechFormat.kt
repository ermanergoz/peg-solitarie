package com.app.pegsolitaire.ui.components

import android.content.res.Resources
import androidx.annotation.StringRes
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.game.WrongTapKind

/**
 * Renders the board's spoken model into D-TB's strings (`docs/ux-spec.md` §11.1 and §8): a cell's
 * description, its tap label and jump actions, the board stop's label and the board channel's
 * lines, plus the tutorial's step line and Results' summary.
 *
 * **The 1-based offset lives here and nowhere else.** Every [Position] in code and tests is 0-based;
 * a row or column becomes a spoken number only through this file's `spokenRow` and `spokenColumn`. The model
 * never sees a string, so a wording change touches `strings.xml` and this file only.
 *
 * @param resources the resources to read the strings from — a screen passes `LocalResources.current`,
 *   so a locale change re-renders the words.
 */
internal class BoardSpeechFormat(
    private val resources: Resources,
) {
    /** §11.1's parts in order, only those that apply, joined by `board_a11y_separator`. */
    fun cellDescription(cell: CellSpeech): String {
        val parts =
            buildList {
                add(
                    resources.getString(
                        R.string.board_a11y_position,
                        spokenRow(cell.position),
                        spokenColumn(cell.position),
                    ),
                )
                add(
                    resources.getString(
                        if (cell.content ==
                            CellContent.Peg
                        ) {
                            R.string.board_a11y_peg
                        } else {
                            R.string.board_a11y_empty
                        },
                    ),
                )
                if (cell.selected) add(resources.getString(R.string.board_a11y_selected))
                cell.jumps?.let { add(jumpsPart(it)) }
                if (cell.jumpTarget) add(resources.getString(R.string.board_a11y_jump_target))
                when (cell.hint) {
                    HintMark.Source -> add(resources.getString(R.string.board_a11y_hint_source))
                    HintMark.Target -> add(resources.getString(R.string.board_a11y_hint_target))
                    null -> Unit
                }
            }
        return parts.joinToString(separator = resources.getString(R.string.board_a11y_separator))
    }

    /** The label TalkBack speaks after "double-tap to". */
    fun tapLabel(tap: TapLabel): String = resources.getString(tapRes(tap))

    /** "Jump up to row 4, column 4". */
    fun jumpActionLabel(action: JumpAction): String =
        resources.getString(
            R.string.board_a11y_action_jump,
            resources.getString(directionRes(action.direction)),
            spokenRow(action.to),
            spokenColumn(action.to),
        )

    /** The board stop: "Board, 32 pegs left". */
    fun boardLabel(pegsLeft: Int): String = resources.getString(R.string.board_a11y_label, pegsLeftPart(pegsLeft))

    /** The board channel's line for [event] (§11.1's announcement table). */
    fun announcement(event: BoardEvent): String =
        when (event) {
            is BoardEvent.Selected ->
                resources.getString(
                    R.string.board_a11y_announce_selected,
                    spokenRow(event.cell),
                    spokenColumn(event.cell),
                    jumpsPart(event.jumps),
                )
            is BoardEvent.PutDown ->
                resources.getString(
                    R.string.board_a11y_announce_put_down,
                    spokenRow(event.cell),
                    spokenColumn(event.cell),
                )
            is BoardEvent.Jumped ->
                resources.getString(
                    R.string.board_a11y_announce_jump,
                    spokenRow(event.from),
                    spokenColumn(event.from),
                    spokenRow(event.to),
                    spokenColumn(event.to),
                    spokenRow(event.removed),
                    spokenColumn(event.removed),
                    pegsLeftPart(event.pegsLeft),
                )
            is BoardEvent.Rejected -> resources.getString(rejectedRes(event.kind))
            is BoardEvent.Undone ->
                resources.getString(
                    R.string.board_a11y_announce_undo,
                    spokenRow(event.from),
                    spokenColumn(event.from),
                    spokenRow(event.to),
                    spokenColumn(event.to),
                    pegsLeftPart(event.pegsLeft),
                )
            is BoardEvent.Hinted ->
                resources.getString(
                    R.string.board_a11y_announce_hint,
                    spokenRow(event.from),
                    spokenColumn(event.from),
                    resources.getString(directionRes(JumpDirection.of(event.from, event.to))),
                    spokenRow(event.to),
                    spokenColumn(event.to),
                )
            is BoardEvent.Restarted ->
                resources.getString(
                    R.string.board_a11y_announce_restart,
                    pegsLeftPart(event.pegsLeft),
                )
        }

    /** A tutorial step's line: `tutorial_step_progress`, then the step's caption. */
    fun stepLine(
        stepNumber: Int,
        stepCount: Int,
        @StringRes captionRes: Int,
    ): String =
        resources.getString(
            R.string.board_a11y_announce_step,
            resources.getString(R.string.tutorial_step_progress, stepNumber, stepCount),
            resources.getString(captionRes),
        )

    /** Results' summary: the headline, `catalog_stars_earned` and the pegs left. */
    fun resultsSummary(
        @StringRes headlineRes: Int,
        stars: Int,
        totalStars: Int,
        pegsLeft: Int,
    ): String =
        resources.getString(
            R.string.results_a11y_summary,
            resources.getString(headlineRes),
            resources.getString(R.string.catalog_stars_earned, stars, totalStars),
            pegsLeftPart(pegsLeft),
        )

    private fun jumpsPart(jumps: Int): String =
        if (jumps == NO_JUMPS) {
            resources.getString(R.string.board_a11y_no_jumps)
        } else {
            resources.getQuantityString(R.plurals.board_a11y_jumps, jumps, jumps)
        }

    private fun pegsLeftPart(pegsLeft: Int): String =
        resources.getQuantityString(R.plurals.board_a11y_pegs_left, pegsLeft, pegsLeft)
}

/** Rows and columns are spoken from 1 (§11.1, "Coordinates"); every `Position` is 0-based. */
private const val SPOKEN_FROM_ONE = 1

/** A peg with this many jumps says "no jumps" rather than "0 jumps". */
private const val NO_JUMPS = 0

private fun spokenRow(cell: Position): Int = cell.row + SPOKEN_FROM_ONE

private fun spokenColumn(cell: Position): Int = cell.col + SPOKEN_FROM_ONE

@StringRes
private fun tapRes(tap: TapLabel): Int =
    when (tap) {
        TapLabel.Select -> R.string.board_a11y_action_select
        TapLabel.PutDown -> R.string.board_a11y_action_put_down
        TapLabel.JumpHere -> R.string.board_a11y_action_jump_here
    }

@StringRes
private fun directionRes(direction: JumpDirection): Int =
    when (direction) {
        JumpDirection.Up -> R.string.board_a11y_direction_up
        JumpDirection.Down -> R.string.board_a11y_direction_down
        JumpDirection.Left -> R.string.board_a11y_direction_left
        JumpDirection.Right -> R.string.board_a11y_direction_right
    }

/** D-TB's three neutral lines, which replace D-06c's sentences on both boards (D-TBa item 2). */
@StringRes
private fun rejectedRes(kind: WrongTapKind): Int =
    when (kind) {
        WrongTapKind.EmptyHole -> R.string.board_a11y_rejected_no_peg
        WrongTapKind.OtherPeg -> R.string.board_a11y_rejected_put_down_first
        WrongTapKind.NotADestination -> R.string.board_a11y_rejected_not_target
    }
