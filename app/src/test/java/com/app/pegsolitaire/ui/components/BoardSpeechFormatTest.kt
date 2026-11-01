package com.app.pegsolitaire.ui.components

import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.ui.game.WrongTapKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The words (D-TB, `docs/ux-spec.md` §11.1 and §8): the model's parts rendered through the string
 * resources, with every row and column spoken from 1. Each expectation is the spec's own example,
 * written out by hand.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [FORMAT_SDK])
class BoardSpeechFormatTest {
    private val format = BoardSpeechFormat(RuntimeEnvironment.getApplication().resources)

    // ---- What a cell says: §11.1's six examples ------------------------------------------------

    @Test
    fun theSixExampleCellsSayExactlyWhatTheSpecWritesWithRowsAndColumnsFromOne() {
        val start = Boards.english33Initial
        val idle =
            BoardSpeechModel.of(
                start,
                selected = null,
                legalTargets = emptyList(),
                hintSource = null,
                hintTarget = null,
            )
        val holding =
            BoardSpeechModel.of(
                board = start,
                selected = ABOVE_CENTRE,
                legalTargets = listOf(CENTRE),
                hintSource = null,
                hintTarget = null,
            )
        val hinted =
            BoardSpeechModel.of(
                start,
                selected = null,
                legalTargets = emptyList(),
                hintSource = BELOW_CENTRE,
                hintTarget = CENTRE,
            )

        assertThat(format.cellDescription(idle.at(Position(0, 2)))).isEqualTo("Row 1, column 3, peg, no jumps")
        assertThat(format.cellDescription(idle.at(CENTRE))).isEqualTo("Row 4, column 4, empty")
        assertThat(format.cellDescription(idle.at(ABOVE_CENTRE))).isEqualTo("Row 2, column 4, peg, 1 jump")
        assertThat(format.cellDescription(holding.at(ABOVE_CENTRE))).isEqualTo("Row 2, column 4, peg, selected, 1 jump")
        assertThat(format.cellDescription(holding.at(CENTRE))).isEqualTo("Row 4, column 4, empty, jump target")
        assertThat(format.cellDescription(hinted.at(BELOW_CENTRE)))
            .isEqualTo("Row 6, column 4, peg, 1 jump, hint, move this peg")
    }

    @Test
    fun aHintedDestinationThatIsAlsoAJumpTargetSaysBothInTheSpecsOrder() {
        val speech =
            BoardSpeechModel.of(
                board = Boards.english33Initial,
                selected = ABOVE_CENTRE,
                legalTargets = listOf(CENTRE),
                hintSource = ABOVE_CENTRE,
                hintTarget = CENTRE,
            )

        assertThat(
            format.cellDescription(speech.at(CENTRE)),
        ).isEqualTo("Row 4, column 4, empty, jump target, hint, land here")
    }

    @Test
    fun aPegWithSeveralJumpsCountsThemInThePlural() {
        // The centre peg can jump up over (2, 3) and down over (4, 3); nothing else is on the board.
        val star =
            listOf(CENTRE, Position(2, 3), Position(4, 3))
                .fold(Boards.english33Initial.copy(pegs = 0L)) { board, peg -> board.withPeg(peg) }
        val speech =
            BoardSpeechModel.of(
                star,
                selected = null,
                legalTargets = emptyList(),
                hintSource = null,
                hintTarget = null,
            )

        assertThat(RulesEngine.legalMoves(star).count { it.from == CENTRE }).isEqualTo(2)
        assertThat(format.cellDescription(speech.at(CENTRE))).isEqualTo("Row 4, column 4, peg, 2 jumps")
    }

    // ---- The tap's label and the jump actions --------------------------------------------------

    @Test
    fun theThreeTapLabelsAreSelectPutDownAndJumpHere() {
        assertThat(format.tapLabel(TapLabel.Select)).isEqualTo("select")
        assertThat(format.tapLabel(TapLabel.PutDown)).isEqualTo("put down")
        assertThat(format.tapLabel(TapLabel.JumpHere)).isEqualTo("jump here")
    }

    @Test
    fun aJumpActionNamesItsDirectionAndItsDestinationFromOne() {
        assertThat(format.jumpActionLabel(JumpAction(JumpDirection.Up, CENTRE))).isEqualTo("Jump up to row 4, column 4")
        assertThat(
            format.jumpActionLabel(JumpAction(JumpDirection.Down, Position(5, 3))),
        ).isEqualTo("Jump down to row 6, column 4")
        assertThat(
            format.jumpActionLabel(JumpAction(JumpDirection.Left, Position(3, 1))),
        ).isEqualTo("Jump left to row 4, column 2")
        assertThat(
            format.jumpActionLabel(JumpAction(JumpDirection.Right, Position(3, 5))),
        ).isEqualTo("Jump right to row 4, column 6")
    }

    // ---- The board stop ----------------------------------------------------------------------

    @Test
    fun theBoardStopSaysBoardAndThePegsLeftInTheSingularAndThePlural() {
        assertThat(format.boardLabel(pegsLeft = 32)).isEqualTo("Board, 32 pegs left")
        assertThat(format.boardLabel(pegsLeft = 1)).isEqualTo("Board, 1 peg left")
    }

    // ---- One of each announcement (§11.1's table) ----------------------------------------------

    @Test
    fun eachBoardEventIsSpokenInItsOwnLine() {
        assertThat(format.announcement(BoardEvent.Selected(cell = ABOVE_CENTRE, jumps = 1)))
            .isEqualTo("Selected row 2, column 4, 1 jump.")
        assertThat(format.announcement(BoardEvent.Selected(cell = Position(0, 2), jumps = 0)))
            .isEqualTo("Selected row 1, column 3, no jumps.")
        assertThat(format.announcement(BoardEvent.PutDown(cell = ABOVE_CENTRE)))
            .isEqualTo("Peg put down, row 2, column 4.")
        assertThat(
            format.announcement(
                BoardEvent.Jumped(from = ABOVE_CENTRE, to = CENTRE, removed = Position(2, 3), pegsLeft = 31),
            ),
        ).isEqualTo("Row 2, column 4 to row 4, column 4. Removed row 3, column 4. 31 pegs left.")
        assertThat(format.announcement(BoardEvent.Undone(from = ABOVE_CENTRE, to = CENTRE, pegsLeft = 32)))
            .isEqualTo("Undid row 2, column 4 to row 4, column 4. 32 pegs left.")
        assertThat(format.announcement(BoardEvent.Hinted(from = ABOVE_CENTRE, to = CENTRE)))
            .isEqualTo("Hint: row 2, column 4, jump down to row 4, column 4.")
        assertThat(format.announcement(BoardEvent.Restarted(pegsLeft = 32))).isEqualTo("Board restarted. 32 pegs left.")
        assertThat(
            format.announcement(
                BoardEvent.Jumped(from = Position(3, 5), to = CENTRE, removed = Position(3, 4), pegsLeft = 1),
            ),
        ).isEqualTo("Row 4, column 6 to row 4, column 4. Removed row 4, column 5. 1 peg left.")
    }

    @Test
    fun eachRejectedTapIsSpokenInItsNeutralLine() {
        assertThat(format.announcement(BoardEvent.Rejected(WrongTapKind.EmptyHole))).isEqualTo("No peg here.")
        assertThat(
            format.announcement(BoardEvent.Rejected(WrongTapKind.NotADestination)),
        ).isEqualTo("Not a jump target.")
        assertThat(
            format.announcement(BoardEvent.Rejected(WrongTapKind.OtherPeg)),
        ).isEqualTo("Put down the selected peg first.")
    }

    @Test
    fun aTutorialStepIsSpokenAsItsProgressThenItsCaption() {
        assertThat(format.stepLine(stepNumber = 2, stepCount = 3, captionRes = R.string.tutorial_step_2_caption))
            .isEqualTo("Step 2 of 3. Jumps work up, down, left and right — never diagonally.")
    }

    @Test
    fun theResultsSummaryIsTheHeadlineTheStarsAndThePegsLeft() {
        assertThat(
            format.resultsSummary(
                headlineRes = R.string.results_headline_solved,
                stars = 2,
                totalStars = 3,
                pegsLeft = 2,
            ),
        ).isEqualTo("Solved! — 2 of 3 stars, 2 pegs left.")
    }

    private fun BoardSpeech.at(position: Position): CellSpeech = cells.single { it.position == position }

    private companion object {
        val CENTRE = Position(row = 3, col = 3)
        val ABOVE_CENTRE = Position(row = 1, col = 3)
        val BELOW_CENTRE = Position(row = 5, col = 3)
    }
}

/** Robolectric has no android-all jar for SDK 36 yet; the suite emulates 34. */
private const val FORMAT_SDK = 34
