package com.app.pegsolitaire.platform.games

import javax.inject.Inject

/**
 * The Slice-1 [GamesService] stub: [signIn] reports "not signed in" (`false`) and
 * [submitScore] discards the submission.
 *
 * Unlike [com.app.pegsolitaire.platform.ads.NoOpAdGateway], which auto-*grants* so
 * hint/undo stay usable without an ad SDK, this stub deliberately *declines*. Play
 * Games genuinely is not wired up yet, so reporting `true` would let a future
 * leaderboard screen render as though the player were connected. Declining keeps
 * every Play-Games-dependent affordance correctly hidden until Slice 2 replaces this
 * binding in `PlatformModule`.
 */
class NoOpGamesService
    @Inject
    constructor() : GamesService {
        override suspend fun signIn(): Boolean = false

        override fun submitScore(
            leaderboardId: String,
            score: Long,
        ) = Unit
    }
