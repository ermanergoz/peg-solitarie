package com.app.pegsolitaire.domain.scoring

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Behaviour spec for [ChallengeClearRule], the one function that decides a Challenge finish
 * (docs/gdd.md R5–R7, §6.1, GD-SB-02, GD-SB-14).
 *
 * The band is absolute: one peg left is three stars, two pegs two, three pegs one, and four or
 * more is not a clear. The expected values are the founder's table written out by hand, never
 * computed from the rule's own constants, so a wrong constant or a wrong formula fails here.
 *
 * Boards are built on the English-33 footprint from hand-placed pegs. A "terminal" fixture keeps
 * every peg orthogonally isolated, so no jump exists; the "playing" fixture adds exactly one
 * adjacent pair, `(0,2)`–`(0,3)`, whose only jump lands on the empty `(0,4)` (`(0,1)` is off the
 * cross). Magic numbers are intentional and acceptable in test code — the table IS the spec.
 */
class ChallengeClearRuleTest {
    @Test
    fun onePegLeftIsClearedWithThreeStars() {
        assertThat(ChallengeClearRule.finish(pegsRemaining = 1)).isEqualTo(ChallengeFinish.Cleared(stars = 3))
        assertThat(ChallengeClearRule.isCleared(pegsRemaining = 1)).isTrue()
    }

    @Test
    fun twoPegsLeftAreClearedWithTwoStars() {
        assertThat(ChallengeClearRule.finish(pegsRemaining = 2)).isEqualTo(ChallengeFinish.Cleared(stars = 2))
        assertThat(ChallengeClearRule.isCleared(pegsRemaining = 2)).isTrue()
    }

    @Test
    fun threePegsLeftAreClearedWithOneStar() {
        assertThat(ChallengeClearRule.finish(pegsRemaining = 3)).isEqualTo(ChallengeFinish.Cleared(stars = 1))
        assertThat(ChallengeClearRule.isCleared(pegsRemaining = 3)).isTrue()
    }

    @Test
    fun fourOrMorePegsLeftAreNotCleared() {
        // 64 is the largest footprint a Long peg mask can hold, so this is the whole domain above the band.
        for (pegsRemaining in 4..64) {
            assertWithMessage("finish(%s)", pegsRemaining)
                .that(ChallengeClearRule.finish(pegsRemaining))
                .isEqualTo(ChallengeFinish.NotCleared)
            assertWithMessage("isCleared(%s)", pegsRemaining)
                .that(ChallengeClearRule.isCleared(pegsRemaining))
                .isFalse()
        }
    }

    @Test
    fun aPegCountBelowOneIsNotCleared() {
        // Only a forged Results route can carry these; they must never read as four or five stars.
        listOf(0, -1).forEach { pegsRemaining ->
            assertWithMessage("finish(%s)", pegsRemaining)
                .that(ChallengeClearRule.finish(pegsRemaining))
                .isEqualTo(ChallengeFinish.NotCleared)
            assertWithMessage("isCleared(%s)", pegsRemaining)
                .that(ChallengeClearRule.isCleared(pegsRemaining))
                .isFalse()
        }
    }

    @Test
    fun aBoardWithALegalMoveIsStillPlayingWhateverItsPegCount() {
        val twoPegs = english33With(ADJACENT_PAIR)
        val threePegs = english33With(ADJACENT_PAIR + ISOLATED.take(1))
        val tenPegs = english33With(ADJACENT_PAIR + ISOLATED.take(8))

        listOf(twoPegs, threePegs, tenPegs).forEach { board ->
            // The fixture's own precondition: a jump exists, so the board is not terminal.
            assertThat(RulesEngine.legalMoves(board)).isNotEmpty()
            assertWithMessage("status at %s pegs", board.pegCount)
                .that(ChallengeClearRule.status(board))
                .isEqualTo(GameStatus.Playing)
        }
        assertThat(listOf(twoPegs, threePegs, tenPegs).map { it.pegCount }).isEqualTo(listOf(2, 3, 10))
    }

    @Test
    fun aTerminalBoardIsWonUpToThreePegsAndStuckFromFour() {
        val expected =
            mapOf(
                1 to GameStatus.Won,
                2 to GameStatus.Won,
                3 to GameStatus.Won,
                4 to GameStatus.Stuck,
                8 to GameStatus.Stuck,
            )

        expected.forEach { (pegs, status) ->
            val board = english33With(ISOLATED.take(pegs))
            assertThat(RulesEngine.isGameOver(board)).isTrue()
            assertWithMessage("status of a terminal board with %s pegs", pegs)
                .that(ChallengeClearRule.status(board))
                .isEqualTo(status)
        }
    }

    @Test
    fun theClearRuleIgnoresTheFootprintsMinimumPegs() {
        // A footprint declaring a two-peg floor, as the Diamond-41 Classic spec does: the Classic
        // bands would call two pegs perfect (three stars). The Challenge rule does not read the floor.
        val spec = BoardSpec(inPlay = Boards.Diamond41.inPlay, target = null, minimumPegs = 2)
        // The diamond's top and bottom tips: two isolated pegs, a terminal two-peg finish.
        val board = Board(spec = spec, pegs = 0L).withPeg(Position(0, 4)).withPeg(Position(8, 4))

        assertThat(RulesEngine.isGameOver(board)).isTrue()
        assertThat(ScoreCalculator.stars(pegsRemaining = 2, minimumPegs = 2)).isEqualTo(3)
        assertThat(ChallengeClearRule.finish(board.pegCount)).isEqualTo(ChallengeFinish.Cleared(stars = 2))
        assertThat(ChallengeClearRule.status(board)).isEqualTo(GameStatus.Won)
    }

    @Test
    fun clearMaxPegsEqualsThePerfectStarCountSoTheBandIsOneStarPerPeg() {
        // GDD §6.1: "two named constants; everything else derives" is true only while the clear
        // threshold and the perfect star count coincide, so that coincidence is pinned here.
        assertThat(ChallengeClearRule.CLEAR_MAX_PEGS).isEqualTo(ChallengeClearRule.PERFECT_STARS)
        assertThat(ChallengeClearRule.PERFECT_STARS).isEqualTo(ScoreParams.THREE_STARS)
        assertThat(ChallengeClearRule.CLEAR_MAX_PEGS).isEqualTo(3)
    }

    private fun english33With(pegs: List<Position>): Board =
        pegs.fold(Board(spec = Boards.English33, pegs = 0L)) { board, position -> board.withPeg(position) }

    private companion object {
        /** One adjacent pair with exactly one jump: `(0,2)` over `(0,3)` into the empty `(0,4)`. */
        val ADJACENT_PAIR = listOf(Position(0, 2), Position(0, 3))

        /**
         * English-33 cells no two of which are orthogonal neighbours, and none of which neighbours
         * [ADJACENT_PAIR] — any subset is a terminal board, and adding any subset to the pair adds no jump.
         */
        val ISOLATED =
            listOf(
                Position(2, 0),
                Position(4, 0),
                Position(2, 6),
                Position(4, 6),
                Position(6, 2),
                Position(6, 4),
                Position(3, 3),
                Position(4, 2),
            )
    }
}
