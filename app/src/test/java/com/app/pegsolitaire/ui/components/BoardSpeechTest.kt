package com.app.pegsolitaire.ui.components

import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.random.Random

/**
 * The board's spoken model (D-TB, `docs/ux-spec.md` §11.1): what each in-play cell says, which tap
 * label and which jump actions it offers, read from the board, the selection, `legalTargets` and the
 * revealed hint only (TB-12). Plain JVM: the model has no Android in it.
 *
 * TB-2 over every Classic board and Challenge levels 1 to 1,000; TB-3 as §11.1's six example rows and
 * a property over 1,000 seeded reachable states; TB-5's offering rule and the two taps an action is.
 * The rules engine is the oracle the criteria name ("the spoken k equals the rules engine's count").
 */
class BoardSpeechTest {
    // ---- TB-2: one stop per playable cell, row by row ---------------------------------------

    @Test
    fun everyClassicBoardSpeaksExactlyItsInPlayCellsRowByRowFromTheTop() {
        for (spec in Boards.CLASSIC_SPECS) {
            assertOneStopPerCellInRowMajorOrder(spec, label = "Classic ${spec.size}")
        }
    }

    @Test
    fun everyChallengeLevelFromOneToAThousandSpeaksExactlyItsInPlayCellsRowByRow() {
        for (level in LevelGenerator.generateLadder(CHALLENGE_LEVELS)) {
            assertOneStopPerCellInRowMajorOrder(level.startingBoard.spec, label = "level ${level.levelNumber}")
        }
    }

    @Test
    fun theStopsFollowTheGridEvenWhenTheFootprintListsItsCellsInAnotherOrder() {
        val shuffled = BoardSpec(inPlay = Boards.English33.inPlay.shuffled(Random(SEED)), target = null)
        val speech = speak(Board(spec = shuffled, pegs = 0L))

        assertThat(speech.cells.map { it.position }).isEqualTo(Boards.English33.footprint)
    }

    // ---- TB-3: §11.1's example rows -----------------------------------------------------------

    @Test
    fun theEnglishStartsFirstStopIsAPegWithNoJumps() {
        assertThat(cellAt(speak(Boards.english33Initial), FIRST_STOP)).isEqualTo(
            CellSpeech(
                position = FIRST_STOP,
                content = CellContent.Peg,
                selected = false,
                jumps = 0,
                jumpTarget = false,
                hint = null,
                tap = TapLabel.Select,
                actions = emptyList(),
            ),
        )
    }

    @Test
    fun theEnglishStartsCentreIsAnEmptyHoleWithNoTapLabelAndNoAction() {
        assertThat(cellAt(speak(Boards.english33Initial), CENTRE)).isEqualTo(
            CellSpeech(
                position = CENTRE,
                content = CellContent.Empty,
                selected = false,
                jumps = null,
                jumpTarget = false,
                hint = null,
                tap = null,
                actions = emptyList(),
            ),
        )
    }

    @Test
    fun aPegThatCanJumpIntoTheCentreHasOneJumpAndOffersIt() {
        val cell = cellAt(speak(Boards.english33Initial), ABOVE_CENTRE)

        assertThat(cell.jumps).isEqualTo(1)
        assertThat(cell.tap).isEqualTo(TapLabel.Select)
        assertThat(cell.actions).containsExactly(JumpAction(JumpDirection.Down, CENTRE))
    }

    @Test
    fun theSelectedPegSaysSelectedAndOffersPutDownAndItsJump() {
        val cell = cellAt(speakSelected(Boards.english33Initial, ABOVE_CENTRE), ABOVE_CENTRE)

        assertThat(cell.selected).isTrue()
        assertThat(cell.jumps).isEqualTo(1)
        assertThat(cell.tap).isEqualTo(TapLabel.PutDown)
        assertThat(cell.actions).containsExactly(JumpAction(JumpDirection.Down, CENTRE))
    }

    @Test
    fun theCentreWhileThatPegIsSelectedIsAJumpTargetLabelledJumpHere() {
        val cell = cellAt(speakSelected(Boards.english33Initial, ABOVE_CENTRE), CENTRE)

        assertThat(cell.jumpTarget).isTrue()
        assertThat(cell.tap).isEqualTo(TapLabel.JumpHere)
        assertThat(cell.actions).isEmpty()
    }

    @Test
    fun aHintedPegSaysHintSourceAndItsDestinationSaysHintTarget() {
        val speech =
            BoardSpeechModel.of(
                board = Boards.english33Initial,
                selected = null,
                legalTargets = emptyList(),
                hintSource = BELOW_CENTRE,
                hintTarget = CENTRE,
            )

        assertThat(cellAt(speech, BELOW_CENTRE).hint).isEqualTo(HintMark.Source)
        assertThat(cellAt(speech, BELOW_CENTRE).jumps).isEqualTo(1)
        assertThat(cellAt(speech, CENTRE).hint).isEqualTo(HintMark.Target)
        assertThat(speech.cells.count { it.hint != null }).isEqualTo(2)
    }

    @Test
    fun halfAHintPairMarksNoCellBecauseNothingIsDrawnForIt() {
        val speech =
            BoardSpeechModel.of(
                board = Boards.english33Initial,
                selected = null,
                legalTargets = emptyList(),
                hintSource = BELOW_CENTRE,
                hintTarget = null,
            )

        assertThat(speech.cells).hasSize(Boards.English33.size)
        assertThat(speech.cells.count { it.hint != null }).isEqualTo(0)
    }

    @Test
    fun whileAPegIsSelectedNoOtherPegOffersAnActionOrATapLabel() {
        val speech = speakSelected(Boards.english33Initial, ABOVE_CENTRE)
        val others = speech.cells.filter { it.content == CellContent.Peg && it.position != ABOVE_CENTRE }
        assertThat(others).hasSize(ENGLISH_START_PEGS - 1)
        assertThat(others.flatMap { it.actions }).isEmpty()
        assertThat(others.mapNotNull { it.tap }).isEmpty()
    }

    @Test
    fun theBoardCountsItsPegs() {
        assertThat(speak(Boards.english33Initial).pegsLeft).isEqualTo(ENGLISH_START_PEGS)
    }

    // ---- TB-5: the actions' order and the two taps ------------------------------------------

    @Test
    fun aPegWithAJumpEveryWayListsThemUpDownLeftRightNotInTheEnginesOrder() {
        // The centre peg with its four neighbours and nothing beyond them: one jump every way.
        val star =
            listOf(CENTRE, Position(2, 3), Position(4, 3), Position(3, 2), Position(3, 4))
                .fold(Board(spec = Boards.English33, pegs = 0L)) { board, peg -> board.withPeg(peg) }

        assertThat(cellAt(speak(star), CENTRE).actions.map { it.direction })
            .containsExactly(JumpDirection.Up, JumpDirection.Down, JumpDirection.Left, JumpDirection.Right)
            .inOrder()
        assertThat(RulesEngine.legalMoves(star).filter { it.from == CENTRE }.map { it.to })
            .containsExactly(Position(1, 3), Position(3, 1), Position(3, 5), Position(5, 3))
            .inOrder()
    }

    @Test
    fun aSelectionLineCountsTheJumpsTheSelectedPegsCellSpeaks() {
        val star =
            listOf(CENTRE, Position(2, 3), Position(4, 3))
                .fold(Board(spec = Boards.English33, pegs = 0L)) { board, peg -> board.withPeg(peg) }

        assertThat(BoardSpeechModel.jumpsFrom(Boards.english33Initial, ABOVE_CENTRE)).isEqualTo(1)
        assertThat(BoardSpeechModel.jumpsFrom(Boards.english33Initial, FIRST_STOP)).isEqualTo(0)
        assertThat(BoardSpeechModel.jumpsFrom(star, CENTRE)).isEqualTo(cellAt(speak(star), CENTRE).jumps)
        assertThat(BoardSpeechModel.jumpsFrom(star, CENTRE)).isEqualTo(2)
    }

    @Test
    fun upIsTowardsRowOneAndLeftIsTowardsColumnOne() {
        assertThat(JumpDirection.of(from = CENTRE, to = Position(1, 3))).isEqualTo(JumpDirection.Up)
        assertThat(JumpDirection.of(from = CENTRE, to = Position(5, 3))).isEqualTo(JumpDirection.Down)
        assertThat(JumpDirection.of(from = CENTRE, to = Position(3, 1))).isEqualTo(JumpDirection.Left)
        assertThat(JumpDirection.of(from = CENTRE, to = Position(3, 5))).isEqualTo(JumpDirection.Right)
    }

    @Test
    fun anActionIsASelectionThenTheDestinationOrOnlyTheDestinationWhenAlreadySelected() {
        assertThat(BoardSpeechModel.tapsForJump(selected = null, from = ABOVE_CENTRE, to = CENTRE))
            .containsExactly(ABOVE_CENTRE, CENTRE)
            .inOrder()
        assertThat(BoardSpeechModel.tapsForJump(selected = ABOVE_CENTRE, from = ABOVE_CENTRE, to = CENTRE))
            .containsExactly(CENTRE)
    }

    // ---- TB-3 and TB-5 as properties over 1,000 seeded reachable states -----------------------

    @Test
    fun overAThousandSeededStatesEveryCellAgreesWithTheRulesEngine() {
        repeat(PROPERTY_STATES) { seed ->
            val state = reachableState(seed)
            val speech =
                BoardSpeechModel.of(
                    board = state.board,
                    selected = state.selected,
                    legalTargets = state.legalTargets,
                    hintSource = state.hint?.from,
                    hintTarget = state.hint?.to,
                )
            assertWithMessage("seed $seed: pegs left").that(speech.pegsLeft).isEqualTo(state.board.pegCount)
            for (cell in speech.cells) assertCellAgreesWithTheEngine(cell, state, seed)
        }
    }

    // ---- Helpers ------------------------------------------------------------------------------

    private fun assertOneStopPerCellInRowMajorOrder(
        spec: BoardSpec,
        label: String,
    ) {
        val board = spec.inPlay.fold(Board(spec = spec, pegs = 0L)) { b, p -> b.withPeg(p) }
        val positions = speak(board).cells.map { it.position }

        assertWithMessage("$label: one stop per in-play cell").that(positions).hasSize(spec.inPlay.size)
        assertWithMessage("$label: each in-play cell once").that(positions).containsExactlyElementsIn(spec.inPlay)
        assertWithMessage("$label: row by row, left to right")
            .that(positions)
            .isInOrder(compareBy<Position>({ it.row }, { it.col }))
    }

    private fun assertCellAgreesWithTheEngine(
        cell: CellSpeech,
        state: SeededState,
        seed: Int,
    ) {
        val at = "seed $seed, ${cell.position}"
        val isPeg = state.board.cellAt(cell.position) == Cell.Peg
        val moves = RulesEngine.legalMoves(state.board).filter { it.from == cell.position }
        val selectedHere = state.selected == cell.position
        val offersActions = isPeg && (state.selected == null || selectedHere)

        assertWithMessage(
            "$at: content",
        ).that(cell.content).isEqualTo(if (isPeg) CellContent.Peg else CellContent.Empty)
        assertWithMessage("$at: jumps").that(cell.jumps).isEqualTo(if (isPeg) moves.size else null)
        assertWithMessage("$at: selected").that(cell.selected).isEqualTo(selectedHere)
        assertWithMessage("$at: jump target")
            .that(cell.jumpTarget)
            .isEqualTo(state.selected != null && !isPeg && cell.position in state.legalTargets)
        assertWithMessage("$at: hint").that(cell.hint).isEqualTo(expectedHint(cell.position, state.hint))
        assertWithMessage("$at: tap").that(cell.tap).isEqualTo(expectedTap(cell, state, isPeg))
        assertWithMessage("$at: actions")
            .that(cell.actions.map { it.to })
            .containsExactlyElementsIn(if (offersActions) moves.map { it.to } else emptyList())
        assertWithMessage("$at: actions up, down, left, right")
            .that(cell.actions.map { it.direction })
            .isInOrder()
        for (action in cell.actions) {
            val taps = BoardSpeechModel.tapsForJump(state.selected, cell.position, action.to)
            assertWithMessage("$at: taps for ${action.to}")
                .that(taps)
                .isEqualTo(if (selectedHere) listOf(action.to) else listOf(cell.position, action.to))
        }
    }

    private fun expectedHint(
        position: Position,
        hint: Move?,
    ): HintMark? =
        when (position) {
            hint?.from -> HintMark.Source
            hint?.to -> HintMark.Target
            else -> null
        }

    private fun expectedTap(
        cell: CellSpeech,
        state: SeededState,
        isPeg: Boolean,
    ): TapLabel? =
        when {
            state.selected == null && isPeg -> TapLabel.Select
            state.selected == cell.position -> TapLabel.PutDown
            state.selected != null && !isPeg && cell.position in state.legalTargets -> TapLabel.JumpHere
            else -> null
        }

    /**
     * A reachable state built by seeded legal play from one of the five Classic starts, then a
     * seeded selection (with the engine's targets for it, as the ViewModels compute them) and a
     * seeded revealed hint, each present in about half of the states.
     */
    private fun reachableState(seed: Int): SeededState {
        val random = Random(seed)
        val start = CLASSIC_STARTS[seed % CLASSIC_STARTS.size]
        var board = start
        repeat(random.nextInt(MAX_PLY)) {
            val moves = RulesEngine.legalMoves(board)
            if (moves.isNotEmpty()) board = RulesEngine.applyMove(board, moves[random.nextInt(moves.size)])
        }
        val pegs = board.spec.inPlay.filter { board.cellAt(it) == Cell.Peg }
        val selected = pegs.takeIf { it.isNotEmpty() && random.nextBoolean() }?.let { it[random.nextInt(it.size)] }
        val targets = RulesEngine.legalMoves(board).filter { it.from == selected }.map { it.to }
        val moves = RulesEngine.legalMoves(board)
        val hint = moves.takeIf { it.isNotEmpty() && random.nextBoolean() }?.let { it[random.nextInt(it.size)] }
        return SeededState(board = board, selected = selected, legalTargets = targets, hint = hint)
    }

    private fun speak(board: Board): BoardSpeech =
        BoardSpeechModel.of(
            board = board,
            selected = null,
            legalTargets = emptyList(),
            hintSource = null,
            hintTarget = null,
        )

    private fun speakSelected(
        board: Board,
        selected: Position,
    ): BoardSpeech =
        BoardSpeechModel.of(
            board = board,
            selected = selected,
            legalTargets = RulesEngine.legalMoves(board).filter { it.from == selected }.map { it.to },
            hintSource = null,
            hintTarget = null,
        )

    private fun cellAt(
        speech: BoardSpeech,
        position: Position,
    ): CellSpeech = speech.cells.single { it.position == position }

    private data class SeededState(
        val board: Board,
        val selected: Position?,
        val legalTargets: List<Position>,
        val hint: Move?,
    )

    private companion object {
        /** English-33's first in-play cell, row 1, column 3 when spoken. */
        val FIRST_STOP = Position(row = 0, col = 2)
        val CENTRE = Position(row = 3, col = 3)
        val ABOVE_CENTRE = Position(row = 1, col = 3)
        val BELOW_CENTRE = Position(row = 5, col = 3)

        const val ENGLISH_START_PEGS = 32
        const val CHALLENGE_LEVELS = 1_000
        const val PROPERTY_STATES = 1_000
        const val MAX_PLY = 30
        const val SEED = 7

        val CLASSIC_STARTS =
            listOf(
                Boards.english33Initial,
                Boards.european37Initial,
                Boards.asymmetric39Initial,
                Boards.diamond41Initial,
                Boards.wiegleb45Initial,
            )
    }
}
