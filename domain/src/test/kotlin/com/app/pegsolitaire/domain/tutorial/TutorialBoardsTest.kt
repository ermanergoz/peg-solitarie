package com.app.pegsolitaire.domain.tutorial

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The permanent guard on the three tutorial footprints (ux-spec §4.1, D-01c).
 *
 * The load-bearing test is [everyLegalLineOfEveryTutorialBoardEndsWithExactlyOnePeg]:
 * "a wrong tap is never a failure" is only true if it is a property of the LAYOUTS
 * rather than of the UI refusing taps, and the layouts have that property exactly
 * when no reachable position has zero legal moves and more than one peg. The
 * game-designer's own hand analysis of a candidate board was confidently wrong and
 * was caught by exactly this enumeration, which is why it lives in `check` and not
 * in a report.
 *
 * Magic numbers are intentional in test code (detekt's `MagicNumber` rule excludes
 * the test source set) — the coordinates here ARE the specification of the three
 * footprints.
 */
class TutorialBoardsTest {
    @Test
    fun everyLegalLineOfEveryTutorialBoardEndsWithExactlyOnePeg() {
        TutorialBoards.ALL.forEachIndexed { index, start ->
            val terminalPegCounts = terminalPegCountsOf(start)
            val worstTerminalPegs = terminalPegCounts.max()

            assertWithMessage(
                "T%s: worstTerminalPegs=%s over %s terminal position(s) — a board is " +
                    "dead-end-free to one peg exactly when this is 1",
                index + 1,
                worstTerminalPegs,
                terminalPegCounts.size,
            ).that(worstTerminalPegs)
                .isEqualTo(BoardConstants.MINIMUM_PEGS_DEFAULT)
        }
    }

    @Test
    fun theFirstTutorialStepHasExactlyOneLegalMoveAndItIsHorizontal() {
        val moves = RulesEngine.legalMoves(TutorialBoards.T1)

        assertThat(moves).hasSize(1)
        val only = moves.single()
        assertWithMessage("T1's single move must be horizontal: %s", only)
            .that(only.from.row)
            .isEqualTo(only.to.row)
    }

    @Test
    fun theSecondTutorialStepHasExactlyOneLegalMoveAndItIsVertical() {
        val moves = RulesEngine.legalMoves(TutorialBoards.T2)

        assertThat(moves).hasSize(1)
        val only = moves.single()
        assertWithMessage("T2's single move must be vertical: %s", only)
            .that(only.from.col)
            .isEqualTo(only.to.col)
    }

    @Test
    fun noTutorialBoardAppearsInTheClassicCatalogue() {
        // The catalogue is the denominator of the Home "Boards" pill; a teaching
        // miniature joining it would change a number the player reads on Home.
        assertThat(Boards.CLASSIC_SPECS).hasSize(CLASSIC_CATALOGUE_SIZE)

        val catalogueFootprints = Boards.CLASSIC_SPECS.map { it.footprint }.toSet()
        TutorialBoards.ALL.forEachIndexed { index, board ->
            assertWithMessage("T%s's footprint must not be a Classic footprint", index + 1)
                .that(catalogueFootprints)
                .doesNotContain(board.spec.footprint)
        }
    }

    @Test
    fun everyTutorialBoardStartsAboveItsTargetAndUsesTheDefaultMinimumPegs() {
        TutorialBoards.ALL.forEachIndexed { index, board ->
            assertWithMessage("T%s must start with something to do", index + 1)
                .that(board.pegCount)
                .isGreaterThan(BoardConstants.MINIMUM_PEGS_DEFAULT)
            assertWithMessage("T%s is a teaching board with no designated finishing hole", index + 1)
                .that(board.spec.target)
                .isNull()
            assertWithMessage("T%s finishes on one peg", index + 1)
                .that(board.spec.minimumPegs)
                .isEqualTo(BoardConstants.MINIMUM_PEGS_DEFAULT)
        }
    }

    @Test
    fun theThreeStepsAreExposedInTheOrderTheTutorialPlaysThem() {
        assertThat(TutorialBoards.ALL)
            .isEqualTo(listOf(TutorialBoards.T1, TutorialBoards.T2, TutorialBoards.T3))
    }

    /**
     * The peg count of every terminal position (no legal move) reachable from
     * [start] by any legal sequence of jumps, memoised on [Board.pegs] — every
     * board in a run shares one spec, so the bitmask is a complete state key.
     */
    private fun terminalPegCountsOf(start: Board): List<Int> {
        val seen = mutableSetOf(start.pegs)
        val frontier = ArrayDeque(listOf(start))
        val terminals = mutableListOf<Int>()

        while (frontier.isNotEmpty()) {
            val board = frontier.removeFirst()
            val moves = RulesEngine.legalMoves(board)
            if (moves.isEmpty()) {
                terminals += board.pegCount
                continue
            }
            moves.forEach { move ->
                val next = RulesEngine.applyMove(board, move)
                if (seen.add(next.pegs)) frontier.addLast(next)
            }
        }
        return terminals
    }

    private companion object {
        /** The five shipped Classic footprints (`Boards.CLASSIC_SPECS`). */
        const val CLASSIC_CATALOGUE_SIZE = 5
    }
}
