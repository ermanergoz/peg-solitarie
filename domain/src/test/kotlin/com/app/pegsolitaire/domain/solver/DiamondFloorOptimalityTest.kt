package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.random.Random

/**
 * The **optimality half** of Diamond-41's fewest-pegs floor (DEBT-02, D-05b): one peg
 * is unreachable from the canonical centre start.
 *
 * What the two halves together establish:
 *  - **one peg is unreachable** — this file, exhaustively over all 41 single-peg
 *    positions, none of which carries the colour signature the centre start pins;
 *  - **two pegs IS reachable** — the committed `diamond41.txt` line, replayed move by
 *    move through the unchanged `RulesEngine` in `SolutionTest`;
 *  - therefore **F = 2 exactly**. Not a best-known result, not a search plateau: a
 *    fact with a witness on one side and an invariant on the other.
 *
 * **Why an invariant and not a search.** Proving a floor normally means exhausting the
 * state space. For a 41-hole board that is out of reach — a comparable 41-hole
 * exhaustion is reported at roughly 128 GB. It is also unnecessary here, because
 * F - 1 = 1 and a single peg is precisely the case the colour invariant decides for
 * free. The expensive instrument is not merely inconvenient, it is the wrong one.
 *
 * **The argument.** Colour every cell by `(row + col) mod 3` and, independently, by
 * `(row - col) mod 3`. The three cells of an orthogonal jump are colinear and
 * consecutive, so each jump touches all three colours of a colouring exactly once and
 * therefore flips the parity of ALL three class counts. The parity vector of a
 * position, advanced by the number of moves still to play, is thus forced — so two
 * positions can only be connected if their forward projections to a common peg count
 * agree. Diamond-41's centre start projects to all-even under both colourings; every
 * single-peg position has exactly one odd class. They cannot meet.
 *
 * **This proof is deliberately independent of the code under test.** The signature is
 * re-derived here rather than delegated to the production pruner, which this phase
 * modifies: a "proof" that restates the implementation it is meant to justify proves
 * nothing. For the same reason it carries two controls — a positive one (the committed
 * two-peg terminal DOES match the start, so the test is not rejecting everything) and
 * a negative one (English-33 DOES admit single-peg candidates, so the test
 * discriminates between boards rather than between nothing).
 *
 * [BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR] and Diamond-41's star bands rest on
 * exactly this. **A failure here means that constant is wrong, not that this test is
 * wrong** — D-05's escalation clause fires and F reverts to unknown.
 */
class DiamondFloorOptimalityTest {
    @Test
    fun noneOfTheFortyOneSinglePegPositionsIsReachableFromTheCentreStart() {
        val startSignature = projectedSignature(Boards.diamond41Initial, targetPegs = ONE_PEG)
        var examined = 0
        val survivors = mutableListOf<Position>()
        for (position in Boards.Diamond41.inPlay) {
            examined++
            if (projectedSignature(singlePeg(Boards.Diamond41, position), ONE_PEG) == startSignature) {
                survivors.add(position)
            }
        }

        // Asserting the count examined as well as the count surviving: a footprint that
        // silently shrank would otherwise make "0 survived" true and meaningless.
        assertWithMessage("every in-play Diamond-41 hole must be examined")
            .that(examined)
            .isEqualTo(DIAMOND_41_HOLES)
        assertWithMessage("no single-peg Diamond-41 position may carry the centre start's colour signature")
            .that(survivors)
            .isEmpty()
    }

    @Test
    fun theCentreStartIsAllEvenUnderBothColourings() {
        val parities = colourings.map { colouring -> classParities(Boards.diamond41Initial, colouring) }
        assertWithMessage("the Diamond-41 centre start's class parities must all be even")
            .that(parities)
            .isEqualTo(listOf(ALL_EVEN, ALL_EVEN))
    }

    @Test
    fun everyLegalMoveConservesTheColourSignature() {
        val random = Random(PLAYOUT_SEED)
        val reference = projectedSignature(Boards.diamond41Initial, targetPegs = ONE_PEG)
        repeat(PLAYOUT_COUNT) { playout ->
            var board = Boards.diamond41Initial
            var moves = RulesEngine.legalMoves(board)
            while (moves.isNotEmpty()) {
                board = RulesEngine.applyMove(board, moves[random.nextInt(moves.size)])
                assertWithMessage("playout %s: a legal move changed the colour signature", playout)
                    .that(projectedSignature(board, targetPegs = ONE_PEG))
                    .isEqualTo(reference)
                moves = RulesEngine.legalMoves(board)
            }
        }
    }

    /**
     * Positive control. The committed floor line ends on a position that IS compatible
     * with the start — so the machinery above accepts something, and its verdict on one
     * peg is a real exclusion rather than a blanket rejection.
     */
    @Test
    fun theCommittedTwoPegTerminalIsCompatibleWithTheCentreStart() {
        val terminal =
            RecordedSolutions
                .load(DIAMOND_FIXTURE)
                .fold(Boards.diamond41Initial) { board, move -> RulesEngine.applyMove(board, move) }

        assertThat(terminal.pegCount).isEqualTo(BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR)
        assertWithMessage("the committed two-peg terminal must carry the centre start's colour signature")
            .that(projectedSignature(terminal, targetPegs = terminal.pegCount))
            .isEqualTo(projectedSignature(Boards.diamond41Initial, targetPegs = terminal.pegCount))
    }

    /**
     * Negative control. Run against English-33 — a board whose one-peg finish is a
     * committed fixture — the identical machinery must find surviving candidates. Without
     * this, a bug that rejected every candidate on every board would pass silently.
     */
    @Test
    fun english33AdmitsSinglePegCandidatesUnderTheSameMachinery() {
        val startSignature = projectedSignature(Boards.english33Initial, targetPegs = ONE_PEG)
        val survivors =
            Boards.English33.inPlay.filter { position ->
                projectedSignature(singlePeg(Boards.English33, position), ONE_PEG) == startSignature
            }

        assertWithMessage("English-33 has a committed one-peg solution, so candidates MUST survive")
            .that(survivors)
            .isNotEmpty()
    }

    /**
     * The exclusion is exactly at one peg, not a general rejection of small Diamond
     * finishes: at two pegs the invariant admits candidates, which is why F = 2 needed a
     * witness line and F = 1 did not.
     */
    @Test
    fun theDiamondExclusionStopsAtOnePeg() {
        val startSignature =
            projectedSignature(Boards.diamond41Initial, targetPegs = BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR)
        val holes = Boards.Diamond41.inPlay
        val pairs =
            holes.indices.flatMap { first ->
                (first + 1 until holes.size).map { second -> holes[first] to holes[second] }
            }
        val survivors =
            pairs.filter { (first, second) ->
                val board = singlePeg(Boards.Diamond41, first).withPeg(second)
                projectedSignature(board, targetPegs = board.pegCount) == startSignature
            }

        assertWithMessage("the colour invariant must admit two-peg Diamond-41 finishes")
            .that(survivors)
            .isNotEmpty()
    }

    /**
     * The colour signature of [board] advanced to [targetPegs] pegs: for each of the two
     * colourings, the parity of the peg count in each of the three residue classes,
     * flipped when an odd number of moves still has to be played. Two positions are
     * mutually reachable-compatible exactly when their projections agree.
     */
    private fun projectedSignature(
        board: Board,
        targetPegs: Int,
    ): List<Boolean> {
        val movesRemaining = board.pegCount - targetPegs
        // Every jump flips all three class parities, so an odd number of remaining
        // moves inverts the whole vector and an even number leaves it alone.
        val flips = (movesRemaining and 1) == 1
        return colourings.flatMap { colouring ->
            classParities(board, colouring).map { parity -> parity xor flips }
        }
    }

    /** Per-residue-class peg-count parity of [board] under [colouring]. */
    private fun classParities(
        board: Board,
        colouring: (Int, Int) -> Int,
    ): List<Boolean> {
        val parity = BooleanArray(COLOURING_MODULUS)
        for (position in board.spec.inPlay) {
            if (board.cellAt(position) != Cell.Peg) continue
            val residue = Math.floorMod(colouring(position.row, position.col), COLOURING_MODULUS)
            parity[residue] = !parity[residue]
        }
        return parity.toList()
    }

    /** The board holding exactly one peg, at [position], on [spec]'s footprint. */
    private fun singlePeg(
        spec: BoardSpec,
        position: Position,
    ): Board = Board(spec = spec, pegs = 0L).withPeg(position)

    private companion object {
        /** The two independent colourings whose class parities the jump rule conserves. */
        val colourings: List<(Int, Int) -> Int> =
            listOf(
                { row, col -> row + col },
                { row, col -> row - col },
            )

        /** Residue classes per colouring — the invariant is taken mod 3. */
        const val COLOURING_MODULUS = 3

        /** The finish this test proves unreachable from the Diamond-41 centre start. */
        const val ONE_PEG = 1

        /** Diamond-41's hole count, asserted so a footprint change cannot shrink the search. */
        const val DIAMOND_41_HOLES = 41

        /** Seeded random legal play-outs used to observe the invariant being conserved. */
        const val PLAYOUT_COUNT = 200

        const val PLAYOUT_SEED = 0x0D1A_5106L

        const val DIAMOND_FIXTURE = "diamond41"

        /** The all-even parity vector the Diamond-41 centre start carries. */
        val ALL_EVEN = listOf(false, false, false)
    }
}
