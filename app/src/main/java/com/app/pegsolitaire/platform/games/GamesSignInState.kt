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
 * - [SignedIn]: the player is connected, identified by [SignedIn.playerId] and
 *   optionally named by [SignedIn.displayName].
 *
 * Sealed rather than an enum because [SignedIn] carries a payload the other two members
 * have no meaning for; [com.app.pegsolitaire.domain.model.GameStatus] stays an `enum`
 * for exactly the opposite reason. `playerId` and `displayName` are plain `String`s so
 * the seam leaks no Play Games SDK type into anything that observes it.
 *
 * A player who has never opted in (D-17/D-19) is never [Unknown]: with the SDK left
 * untouched there is no check to wait on, so the state resolves straight to [SignedOut].
 * [Unknown] belongs solely to an opted-in player's first, still-pending silent check.
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

    /**
     * The player is connected to Play Games as [playerId].
     *
     * [displayName] is fetched **once, at sign-in** (via the Play Games `PlayersClient`,
     * inside the platform implementation) and carried here rather than re-queried by
     * each observer — one source of truth in the flow beats a second seam method that
     * Settings would re-ask on every open (D-07).
     *
     * ## `displayName = null` is not a failure (D-14)
     *
     * When authentication succeeds but the display-name lookup **fails or stalls**, the
     * state is `SignedIn(playerId, displayName = null)`. That degraded name is:
     *
     * - **never** reported as a sign-in failure — the player *is* signed in, and every
     *   Play-Games-dependent affordance must behave as such;
     * - **never** a reason to keep or return the state to [Unknown] — a surface must not
     *   hide itself waiting for a name that will never arrive;
     * - rendered by the caller as the neutral fallback copy ("Signed in to Play Games")
     *   instead of the named form ("Signed in as <display name>").
     *
     * A name lookup is therefore always best-effort: it widens what a surface can *say*
     * about the player, never whether the player is signed in at all.
     *
     * @property playerId the Play Games player identifier; always present when signed in.
     * @property displayName the player's Play Games display name, or `null` when that
     *   lookup failed or stalled (D-14) — a degraded name, never an error state.
     */
    data class SignedIn(
        val playerId: String,
        val displayName: String? = null,
    ) : GamesSignInState
}
