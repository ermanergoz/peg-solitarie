package com.app.pegsolitaire.platform.games

/**
 * The player's Play Games sign-in state, observed through [GamesService.session]
 * (PLAT-03).
 *
 * - [Unknown]: no sign-in check has completed yet. The state *before* the first answer
 *   arrives, not a failed one — a surface seeing it should commit to neither a
 *   signed-in nor a signed-out affordance.
 * - [SignedOut]: the player is not connected, or Play Games is unavailable on this
 *   device. Every Play-Games-dependent affordance stays hidden.
 * - [SignedIn]: the player is connected, identified by [SignedIn.playerId].
 *
 * Sealed rather than an enum because [SignedIn] carries a payload the other two members
 * have no meaning for; [com.app.pegsolitaire.domain.model.GameStatus] stays an `enum`
 * for exactly the opposite reason. `playerId` is a plain `String` so the seam leaks no
 * Play Games SDK type into anything that observes it.
 *
 * The Slice-1 [NoOpGamesService] stub is permanently [SignedOut]. Never [Unknown],
 * which would leave a future surface waiting on an answer that never comes; never
 * [SignedIn], which would let that surface render as though the player were ranked when
 * Play Games is not wired up at all.
 */
sealed interface GamesSignInState {
    /** No sign-in check has completed yet — the pre-first-check state, not a failure. */
    data object Unknown : GamesSignInState

    /** The player is not connected; hide every Play-Games-dependent affordance. */
    data object SignedOut : GamesSignInState

    /** The player is connected to Play Games as [playerId]. */
    data class SignedIn(
        val playerId: String,
    ) : GamesSignInState
}
