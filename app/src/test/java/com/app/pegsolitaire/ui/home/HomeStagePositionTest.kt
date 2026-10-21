package com.app.pegsolitaire.ui.home

import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The Home stage shows a real game, and holds still (ux-spec §4.2.4, D-03l, criterion 15q).
 *
 * The stage is decoration, but it is decoration that *claims* something — "this is the game you are
 * about to play, in progress" — and a position no game could reach would be a quiet lie on the
 * first screen. So its peg set is proved reachable here, by replaying a recorded line of legal
 * jumps from the English-33 start through the shipped [RulesEngine]. Nothing is replayed at
 * start-up: the stage draws a constant, and this test is the only place the line exists (D-03g —
 * the stage costs Home's first frame one board draw and nothing else).
 *
 * A pure JVM test: the claim is about a board and a rules engine, not about any frame.
 */
class HomeStagePositionTest {
    /**
     * Criterion 15q, first half: every recorded jump is legal where it is played, and the line
     * arrives exactly at the stage's pegs — 14 of them, inside the 12–16 the criterion allows, a
     * scattered mid-game rather than a near-win.
     */
    @Test
    fun aRecordedLineOfLegalJumpsFromTheEnglishStartArrivesExactlyAtTheStage() {
        var board = Boards.english33Initial

        RECORDED_LINE.forEachIndexed { index, (from, to) ->
            val jump = RulesEngine.legalMoves(board).firstOrNull { it.from == from && it.to == to }
            assertWithMessage("jump ${index + 1}, $from to $to, is legal where it is played").that(jump).isNotNull()
            board = RulesEngine.applyMove(board, checkNotNull(jump))
        }

        assertThat(board).isEqualTo(HomeStageBoardState.board)
        assertThat(board.pegCount).isIn(STAGE_PEG_RANGE)
    }

    /**
     * Criterion 15q, second half: the state the stage draws has no selected peg, no legal targets,
     * no hint and no jump in flight — so nothing on it pulses, pops or slides on any frame, with
     * animations on or off (§10).
     */
    @Test
    fun theStageDrawsABoardWithNothingSelectedTargetedHintedOrMoving() {
        val stage = HomeStageBoardState

        assertThat(stage.selected).isNull()
        assertThat(stage.legalTargets).isEmpty()
        assertThat(stage.hintSource).isNull()
        assertThat(stage.hintTarget).isNull()
        assertThat(stage.animating).isNull()
    }

    /** A game *in progress*: the stage is not a finished or dead position — a jump remains. */
    @Test
    fun theStagePositionStillHasAJumpToPlay() {
        assertThat(RulesEngine.legalMoves(HomeStageBoardState.board)).isNotEmpty()
    }

    private companion object {
        /** 15q's bounds on the stage's peg count. */
        val STAGE_PEG_RANGE = 12..16

        /**
         * The first 18 jumps of a winning English-33 line, from the standard start (centre empty),
         * written as `from to` pairs — the jumped peg is implied by the rules.
         */
        val RECORDED_LINE: List<Pair<Position, Position>> =
            listOf(
                jump(5, 3, 3, 3),
                jump(2, 3, 4, 3),
                jump(2, 1, 2, 3),
                jump(3, 5, 3, 3),
                jump(4, 2, 2, 2),
                jump(2, 3, 2, 1),
                jump(1, 4, 3, 4),
                jump(4, 4, 2, 4),
                jump(0, 3, 2, 3),
                jump(3, 0, 3, 2),
                jump(3, 3, 1, 3),
                jump(4, 0, 4, 2),
                jump(0, 2, 2, 2),
                jump(4, 3, 4, 1),
                jump(6, 2, 4, 2),
                jump(6, 4, 6, 2),
                jump(4, 6, 4, 4),
                jump(3, 2, 1, 2),
            )

        fun jump(
            fromRow: Int,
            fromCol: Int,
            toRow: Int,
            toCol: Int,
        ): Pair<Position, Position> = Position(row = fromRow, col = fromCol) to Position(row = toRow, col = toCol)
    }
}
