package com.app.pegsolitaire.domain.scoring

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.rules.RulesEngine

/**
 * How a terminal Challenge board finished, as [ChallengeClearRule.finish] grades it (docs/gdd.md R5, R6).
 *
 * A closed pair rather than a nullable star count, so a caller cannot read stars off a finish that
 * did not clear: the stars exist only inside [Cleared].
 */
sealed interface ChallengeFinish {
    /**
     * The level is cleared and advances the ladder (R7), with [stars] from 1 to
     * [ChallengeClearRule.PERFECT_STARS]: one peg left is three, two pegs two, three pegs one.
     */
    data class Cleared(
        val stars: Int,
    ) : ChallengeFinish

    /**
     * Too many pegs were left: no stars, no result, no advance (R6). Also the answer for a count
     * below one peg, which no real board can report and only a forged Results route can carry.
     */
    data object NotCleared : ChallengeFinish
}

/**
 * The Challenge clear rule: the **one** function that decides whether a Challenge finish counts, and
 * for how many stars (docs/gdd.md R5–R7, §6.1, GD-SB-02, GD-SB-14; tech-design §7.4).
 *
 * The count is **absolute**. It reads no `BoardSpec.minimumPegs`, no footprint, no level number and
 * no mode: every generated level is built backwards from one peg (GDD I1), so one peg is reachable
 * on every board and the band is the same on a six-peg level 1 as on a thirty-one-peg level 300.
 * Classic does not come here; it keeps its own per-board floor (R4, `ScoreCalculator.stars`).
 *
 * `GameViewModel`, `ResultsUiState` and the skill-achievement query all read this object rather
 * than restating a peg count, so the Challenge cleared test exists exactly once. Three scattered
 * `pegsRemaining == 1` comparisons are how the Diamond-41 record gap survived to phase 10 (CR-01).
 *
 * Pure Kotlin with no Android dependency (QUAL-01). [status] reads [RulesEngine], so this package
 * depends on `rules/`, as `usecase/` already does; `rules/` never depends back.
 */
object ChallengeClearRule {
    /**
     * The most pegs a terminal Challenge board may hold and still clear its level: three
     * (R5, GD-SB-02, the founder's 2026-11-01 directive). Four or more is a stuck board (R6).
     *
     * The generator reads this one constant too, for the fail-proof opening of levels 1–4 (G6),
     * rather than restating it: a copy would drift.
     */
    const val CLEAR_MAX_PEGS = 3

    /**
     * The stars a perfect, one-peg finish earns — the top of the band, and the star count the
     * three skill achievements require (GD-SB-05).
     *
     * Equal by design to [CLEAR_MAX_PEGS]: the band is one star per peg, so a clear at
     * [CLEAR_MAX_PEGS] pegs earns exactly one. `ChallengeClearRuleTest` pins the coincidence,
     * because "everything else derives" (GDD §6.1) holds only while it does. The same number as
     * [ScoreParams.THREE_STARS], viewed from the Challenge side.
     */
    const val PERFECT_STARS = ScoreParams.THREE_STARS

    /**
     * The fewest pegs any finish can leave — one, since every jump lands a peg — and so the bottom
     * of the clear band and the perfect finish. A count below it is not a board, only a forged arg.
     */
    private const val PERFECT_FINISH_PEGS = 1

    /**
     * Grades a terminal Challenge board by the [pegsRemaining] on it: [ChallengeFinish.Cleared] with
     * `CLEAR_MAX_PEGS + 1 − pegsRemaining` stars for one to [CLEAR_MAX_PEGS] pegs, otherwise
     * [ChallengeFinish.NotCleared] — including any count below one, which must never read as four
     * or more stars.
     */
    fun finish(pegsRemaining: Int): ChallengeFinish =
        if (pegsRemaining in PERFECT_FINISH_PEGS..CLEAR_MAX_PEGS) {
            ChallengeFinish.Cleared(stars = CLEAR_MAX_PEGS + PERFECT_FINISH_PEGS - pegsRemaining)
        } else {
            ChallengeFinish.NotCleared
        }

    /** Whether a terminal board with [pegsRemaining] pegs clears its level — [finish] is a clear. */
    fun isCleared(pegsRemaining: Int): Boolean = finish(pegsRemaining) is ChallengeFinish.Cleared

    /**
     * The status of a Challenge [board]: [GameStatus.Playing] while a legal move remains (R3: a finish
     * cannot be banked early), else [GameStatus.Won] when its peg count clears the level, otherwise
     * [GameStatus.Stuck]. `Won` keeps its name and here means "cleared", with any star count.
     */
    fun status(board: Board): GameStatus =
        when {
            !RulesEngine.isGameOver(board) -> GameStatus.Playing
            isCleared(board.pegCount) -> GameStatus.Won
            else -> GameStatus.Stuck
        }
}
