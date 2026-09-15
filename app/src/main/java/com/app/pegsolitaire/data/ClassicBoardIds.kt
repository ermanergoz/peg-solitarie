package com.app.pegsolitaire.data

import com.app.pegsolitaire.domain.achievement.ClassicBoard

/**
 * The single data-boundary translation between `:app`'s on-disk level-id strings
 * ([ClassicLevelIds]) and `:domain`'s [ClassicBoard] identity.
 *
 * It exists because `:domain` must never learn an id string — the rule
 * `Boards.CLASSIC_SPECS`' KDoc states for footprints (QUAL-01), applied to achievements
 * and leaderboards. Both directions are needed: the aggregate queries read `levelId`
 * text out of `level_result` and must key the result by [ClassicBoard], while
 * `platform/games` (`GamesIds.forClassic()`) needs the reverse to name a per-board
 * leaderboard. `internal` is module-wide, so `platform/games` reaches it without this
 * mapping leaking out of the app module.
 *
 * The two directions are deliberately asymmetric in their failure mode — see each
 * function.
 */
internal object ClassicBoardIds {
    /**
     * The [ClassicBoard] a stored `levelId` names, or `null` when it names none.
     *
     * `null` is the correct answer, not an error: a Challenge row's numeric id and a
     * legacy/forged id are both "not a Classic board", and an aggregate read must DROP
     * such a row rather than crash a leaderboard submission over it. The `else` branch
     * is what makes that total.
     */
    fun boardFor(levelId: String): ClassicBoard? =
        when (levelId) {
            ClassicLevelIds.ENGLISH33 -> ClassicBoard.ENGLISH33
            ClassicLevelIds.EUROPEAN37 -> ClassicBoard.EUROPEAN37
            ClassicLevelIds.ASYMMETRIC39 -> ClassicBoard.ASYMMETRIC39
            ClassicLevelIds.DIAMOND41 -> ClassicBoard.DIAMOND41
            ClassicLevelIds.WIEGLEB45 -> ClassicBoard.WIEGLEB45
            else -> null
        }

    /**
     * The stored `levelId` for [board].
     *
     * Deliberately has **no** `else` branch: a sixth Classic board must fail the build
     * here rather than silently vanish from the roster the way an `else` would let it.
     * That is the same exhaustiveness argument `AchievementRules`' board→unlock `when`
     * makes.
     */
    fun levelIdFor(board: ClassicBoard): String =
        when (board) {
            ClassicBoard.ENGLISH33 -> ClassicLevelIds.ENGLISH33
            ClassicBoard.EUROPEAN37 -> ClassicLevelIds.EUROPEAN37
            ClassicBoard.ASYMMETRIC39 -> ClassicLevelIds.ASYMMETRIC39
            ClassicBoard.DIAMOND41 -> ClassicLevelIds.DIAMOND41
            ClassicBoard.WIEGLEB45 -> ClassicLevelIds.WIEGLEB45
        }
}
