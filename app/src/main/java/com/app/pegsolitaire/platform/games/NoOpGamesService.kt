package com.app.pegsolitaire.platform.games

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * The Slice-1 [GamesService] stub: [signIn] reports "not signed in" (`false`), [session]
 * is permanently [GamesSignInState.SignedOut], and every submission and stock-UI hook
 * discards the call silently.
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
 * [session] reports [GamesSignInState.SignedOut] rather than [GamesSignInState.Unknown]
 * on purpose: `Unknown` means "the first check has not answered yet", and a stub that
 * never answers would leave a future surface waiting forever.
 */
class NoOpGamesService
    @Inject
    constructor() : GamesService {
        private val signedOutSession = MutableStateFlow<GamesSignInState>(GamesSignInState.SignedOut)

        override val session: StateFlow<GamesSignInState> = signedOutSession.asStateFlow()

        override suspend fun signIn(): Boolean = false

        override fun submitScore(
            leaderboardId: String,
            score: Long,
        ) = Unit

        override fun unlockAchievement(achievementId: String) = Unit

        override fun incrementAchievement(
            achievementId: String,
            steps: Int,
        ) = Unit

        override suspend fun showLeaderboard(leaderboardId: String) = Unit

        override suspend fun showAchievements() = Unit
    }
