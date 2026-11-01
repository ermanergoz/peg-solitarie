package com.app.pegsolitaire.platform.games

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * The Slice-1 [GamesService] stub: [signIn] reports "not signed in" (`false`), [session]
 * is permanently [GamesSignInState.SignedOut], every submission reports `false`, and both
 * stock-UI hooks discard the call silently.
 *
 * Unlike [com.app.pegsolitaire.platform.ads.NoOpAdGateway], which auto-grants
 * ([com.app.pegsolitaire.platform.ads.RewardOutcome.Granted]) so hint/undo stay usable
 * without an ad SDK, this stub deliberately *declines*. Play Games genuinely is not
 * wired up yet, so reporting a connected player would let a future leaderboard or
 * achievement screen render as though the player were ranked. Declining keeps every
 * Play-Games-dependent affordance correctly hidden until Phase 11 replaces this binding
 * in `PlatformModule`. The ad seam needs a third, fail-open answer because a missing ad
 * would otherwise cost the player an assist (ADS-03); a missing leaderboard costs them
 * nothing, so this seam needs no equivalent.
 *
 * **That same reasoning is why the submissions return `false` rather than `true`.** `false`
 * is simply the truth for a stub — nothing was submitted — and it is also the answer that
 * keeps a caller's retry logic correct: a caller that records the D-10 pending-submissions
 * flag on `false` ends up with an accurate "still owed to the server" record, whereas a
 * phantom `true` would have it discard progress that never left the device. Fail-open costs
 * nothing on the ad seam and costs a lost submission here.
 *
 * [session] reports [GamesSignInState.SignedOut] rather than [GamesSignInState.Unknown]
 * on purpose: `Unknown` means "the first check has not answered yet", and a stub that
 * never answers would leave a future surface waiting forever.
 */
class NoOpGamesService
    @Inject
    constructor() : GamesService {
        private val signedOutSession = MutableStateFlow<GamesSignInState>(GamesSignInState.SignedOut)

        override val session: StateFlow<GamesSignInState> = signedOutSession.asStateFlow()

        /**
         * A no-op, for the same reason [bootstrapIfOptedIn] is: no persisted opt-in flag
         * to read and no SDK to initialize, so the stub has only the D-01 gate's "never
         * opted in" branch. Nothing is published either — the process-start hook never
         * writes [session] in any implementation.
         */
        override fun initializeIfOptedIn() = Unit

        /**
         * A no-op: the stub has no persisted opt-in flag to read and no SDK to
         * initialize, so the D-01 gate's "never opted in" branch is the only branch it
         * has. [session] is already resolved to [GamesSignInState.SignedOut], which is
         * exactly what that branch is specified to leave behind.
         */
        override fun bootstrapIfOptedIn() = Unit

        /**
         * Always the silent-decline branch of the D-10 contract: returns `false` and
         * never throws. A stub has no SDK outcome to classify, so it can never report
         * the error branch that would raise a snackbar the player cannot act on.
         */
        override suspend fun signIn(): Boolean = false

        /** `false`: nothing was submitted, and a caller's retry bookkeeping should say so. */
        override suspend fun submitScore(
            leaderboardId: String,
            score: Long,
        ): Boolean = false

        /** `false`, for the same reason as [submitScore]. */
        override suspend fun unlockAchievement(achievementId: String): Boolean = false

        /** `false`, for the same reason as [submitScore]. */
        override suspend fun setAchievementSteps(
            achievementId: String,
            steps: Int,
        ): Boolean = false

        override suspend fun showLeaderboards() = Unit

        override suspend fun showAchievements() = Unit
    }
