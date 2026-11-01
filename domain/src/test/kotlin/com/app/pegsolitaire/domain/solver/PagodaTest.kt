package com.app.pegsolitaire.domain.solver

import com.app.pegsolitaire.domain.golden.solutions.RecordedSolutions
import com.app.pegsolitaire.domain.golden.solutions.SolutionFixtures
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.random.Random

/**
 * Behaviour spec for [Pagoda.mayBeSolvable] — the necessary-condition fast-reject that
 * lets the Solver drop provably-dead positions without search.
 *
 * The contract is one-directional: it may only return `false` when a position is
 * genuinely unable to reach its board's target, and MUST never reject a reachable one
 * (a false reject would make the game tell a player that a winnable board is dead).
 *
 * The first two tests predate the per-board-target generalisation and are deliberately
 * untouched, so the non-regression claim for the four one-peg boards is visible rather
 * than asserted. The rest pin the generalised behaviour: that Diamond-41 is no longer
 * rejected out of hand, that the condition is driven by the TARGET rather than by the
 * footprint, and — honestly — exactly how much prune survives above one peg.
 *
 * Magic numbers (board coordinates, corpus sizes) are intentional in test code: they
 * ARE the specification of the sampled positions.
 */
class PagodaTest {
    @Test
    fun rejectsAKnownDeadPosition() {
        // Two pegs on the same mod-3 colour class with no peg between them: the
        // colour-parity necessary condition proves this cannot reach a single peg.
        assertThat(Pagoda.mayBeSolvable(SolverSamples.deadBoard)).isFalse()
    }

    @Test
    fun admitsTheDiamondStartNowThatItsTargetIsTwoPegs() {
        // The DEBT-01 defect this generalisation exists to remove. While the condition
        // asserted a ONE-peg terminal it returned `false` for EVERY Diamond-41 position,
        // so the Solver returned no move in O(cells) with zero search and the Diamond
        // hint was structurally incapable of saying anything but "no winning move".
        // Diamond-41's target is its proven floor of two pegs, not one.
        assertThat(Pagoda.mayBeSolvable(Boards.diamond41Initial)).isTrue()
    }

    @Test
    fun neverRejectsASolvablePosition() {
        // ONE_PEG, not ALL: `mayBeSolvable` asks "can this still reach a SINGLE peg",
        // and Diamond-41 correctly answers no from every position (its floor is two).
        // Widening this to per-board targets is plan 08-04's job.
        SolutionFixtures.ONE_PEG.forEach { fixture ->
            val line = RecordedSolutions.load(fixture.name)
            val start = SolverSamples.startBoard(fixture)
            // Every prefix of a real winning line is, by construction, still solvable —
            // a necessary condition must accept all of them.
            for (prefixLength in 0 until line.size) {
                val board = SolverSamples.replay(start, line.take(prefixLength))
                assertThat(Pagoda.mayBeSolvable(board)).isTrue()
            }
        }
    }

    @Test
    fun everyReachableDiamondPositionIsClassifiedStably() {
        // The colour-class masks are memoised per footprint behind a bounded cache, so
        // this also pins that repeated calls are not order-dependent: whatever verdict a
        // position gets, it must get the same one every time it is asked.
        reachableDiamondPositions().forEach { board ->
            val first = Pagoda.mayBeSolvable(board)
            val second = Pagoda.mayBeSolvable(board)
            assertWithMessage("Diamond-41 verdict flipped for pegs=${board.pegs.toHex()}")
                .that(second)
                .isEqualTo(first)
        }
    }

    /**
     * The honest measure of what the generalisation costs, asserted as a fact so that a
     * future reader does not go hunting for a bug that is not there (RESEARCH Pitfall 1).
     *
     * The parity half of the condition is automatic. The number of odd colour classes is
     * congruent mod 2 to the peg count `P`; the forward projection flips every class iff
     * `P - T` is odd, which flips the count's parity to match `T`. So `oddClasses` always
     * has `T`'s parity, and the only surviving constraint is `oddClasses <= T`. With
     * three colour classes:
     *
     * - `T = 1` — `oddClasses` is odd, so it is 1 or 3, and the test bites on 3. A real cut.
     * - `T = 2` — `oddClasses` is even, so it is 0 or 2, both `<= 2`. Nothing is ever cut.
     *
     * Diamond-41's target is 2, so on Diamond-41 the condition is not merely weaker — it
     * is vacuous. That is the mathematics of a three-class invariant applied to a
     * multi-peg terminal, not a defect, and it is exactly why the opening book (plans
     * 08-08/08-10) has to be the PRIMARY Diamond hint path: nothing prunes the fallback
     * search for it (threat T-08-11).
     */
    @Test
    fun theConditionIsProvablyVacuousAtTheDiamondsTwoPegTarget() {
        reachableDiamondPositions().forEach { board ->
            assertWithMessage("Diamond-41 rejected pegs=${board.pegs.toHex()} at a two-peg target")
                .that(Pagoda.mayBeSolvable(board))
                .isTrue()
        }
    }

    /**
     * The generalised condition still prunes — verified by construction rather than by
     * search, on a position whose projected parity vector needs all three classes odd.
     *
     * Pegs at `(4,1)` and `(4,4)` on the Diamond footprint. `(4+1) mod 3 == 2` and
     * `(4+4) mod 3 == 2`, so both pegs sit in the SAME `(row + col)` class: that class
     * holds 2 pegs (even) and the other two hold 0 (even), giving a base vector of
     * weight 0. Reaching one peg from two takes one move, an odd count, so every class
     * flips and the projected vector has weight 3 — more odd classes than a single peg
     * could ever account for, so the position must be REJECTED. (`(4-1) mod 3 == 0` and
     * `(4-4) mod 3 == 0`, so the `(row - col)` colouring says the same thing.)
     *
     * It is genuinely dead too, not merely a formula artifact: the two pegs are three
     * cells apart with an empty cell between them, so no jump exists at all.
     *
     * If this ever passes [Pagoda.mayBeSolvable], the prune has degenerated to "always
     * true" even at a one-peg target and the guard is worthless — which is the point.
     */
    @Test
    fun rejectsASyntheticPositionWhoseProjectedParityNeedsAllThreeClasses() {
        val board = twoPegsOnOneColourClass(diamondFootprintWithAOnePegTarget())
        assertThat(RulesEngine.legalMoves(board)).isEmpty()
        assertThat(Pagoda.mayBeSolvable(board)).isFalse()
    }

    /**
     * The GF(4) impossibility result restated as a unit test: the Diamond-41 centre start
     * cannot reach ONE peg, which is precisely why the shipped board's target is two.
     *
     * The sum of `omega^((r+c) mod 3)` and of `omega^((r-c) mod 3)` over the occupied
     * cells are both conserved by every jump. The centre start pins both to 0, while any
     * single-peg position has exactly one odd class and so a non-zero value — verified
     * exhaustively over all 41 single-peg positions, zero of which pass
     * ([BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR]).
     *
     * The verdict therefore has to be driven by the board's TARGET, not by its footprint:
     * the same 41-hole footprint carrying a one-peg target must reject the very start
     * that [admitsTheDiamondStartNowThatItsTargetIsTwoPegs] admits at a two-peg target.
     */
    @Test
    fun rejectsTheDiamondStartWhenTheFootprintCarriesAOnePegTarget() {
        val onePegTargetSpec = diamondFootprintWithAOnePegTarget()
        assertThat(onePegTargetSpec.footprint).isEqualTo(Boards.Diamond41.footprint)
        val start = startBoardOn(onePegTargetSpec)
        assertThat(start.pegs).isEqualTo(Boards.diamond41Initial.pegs)
        assertThat(Pagoda.mayBeSolvable(start)).isFalse()
    }

    /** The Diamond-41 footprint carrying the DEFAULT one-peg target instead of its proven floor. */
    private fun diamondFootprintWithAOnePegTarget(): BoardSpec =
        BoardSpec(
            inPlay = Boards.Diamond41.footprint,
            target = Boards.Diamond41.target,
            minimumPegs = BoardConstants.MINIMUM_PEGS_DEFAULT,
        )

    /** Every hole of [spec] filled except its start vacancy — the board's canonical start. */
    private fun startBoardOn(spec: BoardSpec): Board {
        val full = spec.inPlay.fold(Board(spec = spec, pegs = 0L)) { board, position -> board.withPeg(position) }
        return full.withoutPeg(requireNotNull(spec.target) { "the Diamond spec carries its start vacancy" })
    }

    /** See [rejectsASyntheticPositionWhoseProjectedParityNeedsAllThreeClasses] for the construction. */
    private fun twoPegsOnOneColourClass(spec: BoardSpec): Board =
        Board(spec = spec, pegs = 0L)
            .withPeg(Position(row = 4, col = 1))
            .withPeg(Position(row = 4, col = 4))

    /**
     * At least [DIAMOND_POSITIONS] genuinely reachable Diamond-41 positions, collected by
     * replaying random legal-move prefixes from the canonical centre start under the fixed
     * [CORPUS_SEED] — so a failure is reproducible from the message alone and never flakes.
     */
    private fun reachableDiamondPositions(): List<Board> {
        val random = Random(CORPUS_SEED)
        val corpus = mutableListOf<Board>()
        while (corpus.size < DIAMOND_POSITIONS) {
            var board = Boards.diamond41Initial
            var ply = 0
            while (ply < WALK_DEPTH) {
                corpus += board
                val moves = RulesEngine.legalMoves(board)
                if (moves.isEmpty()) break
                board = RulesEngine.applyMove(board, moves[random.nextInt(moves.size)])
                ply++
            }
        }
        return corpus
    }

    private fun Long.toHex(): String = "0x" + toULong().toString(HEX_RADIX)

    private companion object {
        /** Fixed seed for the Diamond corpus — reproducible failures, no flake. */
        const val CORPUS_SEED = 0x5150_2602L

        /** Reachable Diamond-41 positions sampled (the plan's floor is 200). */
        const val DIAMOND_POSITIONS = 200

        /** Plies walked per random line before restarting from the centre start. */
        const val WALK_DEPTH = 25

        const val HEX_RADIX = 16
    }
}
