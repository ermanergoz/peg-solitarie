package com.app.pegsolitaire.domain.golden

import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Golden-master determinism snapshots over `RulesEngine` + English-33 (QUAL-04).
 *
 * These lock the [BoardCodec] encoding and the [GoldenMasterAssert] contract that
 * Phase 3 plugs the generator into unchanged. All inputs are deterministic, so the
 * committed fixtures must match byte-for-byte on every run.
 */
class GoldenMasterTest {
    @Test
    fun canonicalEnglish33InitialBoardMatchesFixture() {
        GoldenMasterAssert.assertMatches(
            fixtureName = "english33_initial.txt",
            actual = BoardCodec.encodeBoard(Boards.english33Initial),
        )
    }

    @Test
    fun sortedLegalMovesFromInitialPositionMatchFixture() {
        val moves = RulesEngine.legalMoves(Boards.english33Initial)

        GoldenMasterAssert.assertMatches(
            fixtureName = "english33_initial_moves.txt",
            actual = BoardCodec.encodeMoves(moves),
        )
    }

    @Test
    fun scriptedLineProducesDeterministicBoardAfterEachStep() {
        val encoded = encodeScriptedLine(steps = SCRIPTED_LINE_STEPS)

        GoldenMasterAssert.assertMatches(
            fixtureName = "english33_scripted_line.txt",
            actual = encoded,
        )
    }

    @Test
    fun playedOutTerminalBoardMatchesFixtureAndIsGameOver() {
        var board = Boards.english33Initial
        while (true) {
            val moves = RulesEngine.legalMoves(board)
            if (moves.isEmpty()) break
            board = RulesEngine.applyMove(board, moves.first())
        }

        // Sanity: the terminal really is terminal before we snapshot it.
        assertThat(RulesEngine.isGameOver(board)).isTrue()

        val encoded =
            BoardCodec.encodeBoard(board) +
                "\ngameOver=${RulesEngine.isGameOver(board)}" +
                "\npegCount=${RulesEngine.pegCount(board)}"

        GoldenMasterAssert.assertMatches(
            fixtureName = "english33_terminal.txt",
            actual = encoded,
        )
    }

    @Test
    fun reEncodingIsStableAcrossRepeatedRuns() {
        val once = BoardCodec.encodeBoard(Boards.english33Initial)
        val twice = BoardCodec.encodeBoard(Boards.english33Initial)

        assertThat(once).isEqualTo(twice)
    }

    private fun encodeScriptedLine(steps: Int): String {
        val builder = StringBuilder()
        var board = Boards.english33Initial
        repeat(steps) { index ->
            val move = RulesEngine.legalMoves(board).first()
            board = RulesEngine.applyMove(board, move)
            builder.append("step=").append(index + 1).append('\n')
            builder.append("move=").append(BoardCodec.encodeMoves(listOf(move))).append('\n')
            builder.append(BoardCodec.encodeBoard(board))
            if (index + 1 < steps) builder.append("\n\n")
        }
        return builder.toString()
    }

    private companion object {
        /** A fixed-length deterministic solution-prefix snapshotted step-by-step. */
        const val SCRIPTED_LINE_STEPS = 5
    }
}
