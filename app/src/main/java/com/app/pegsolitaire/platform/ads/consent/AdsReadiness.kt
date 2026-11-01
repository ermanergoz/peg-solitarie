package com.app.pegsolitaire.platform.ads.consent

/**
 * Whether the ads stack is in a state where an ad may legitimately be requested, observed
 * through [ConsentGateway.readiness] (CONS-02 / CONS-04).
 *
 * An `enum class` rather than a `sealed interface`, deliberately. All three members are
 * payload-free and no future member has any data to carry — readiness is a tri-state
 * answer, not a result type.
 * [com.app.pegsolitaire.platform.games.GamesSignInState] is sealed *because* one of its
 * members carries a `playerId`; that reasoning does not transfer here, and repeating it
 * would be false. The in-repo precedent for a payload-free set is
 * [com.app.pegsolitaire.platform.ads.AdReason] and
 * [com.app.pegsolitaire.domain.model.GameStatus], both `enum class`. Being an enum also
 * makes `when` exhaustiveness free and keeps the type trivially comparable in tests.
 *
 * This type names no UMP and no Google Mobile Ads type, so anything observing it — a
 * ViewModel included — stays clear of the SDK boundary `PlatformBoundaryRules` enforces.
 */
enum class AdsReadiness {
    /**
     * Pre-bootstrap: no consent answer has arrived yet.
     *
     * **Observers MUST treat this as _not offerable_** and fall through to D-12's silent
     * free grant — never as "offerable, let's find out" (Pitfall 16). A fast player on a
     * slow network can exhaust a free allowance before
     * [ConsentGateway.bootstrap] completes; showing the opt-in dialog then and only
     * afterwards discovering the SDK is uninitialized produces exactly the "watch an ad?
     * → instant free grant" absurdity D-12 exists to rule out.
     *
     * This is the state *before* an answer, not a failed one — a failure is
     * [Unavailable]. The two behave identically at the offer site on purpose; they are
     * kept distinct so a later retry can tell "not yet" from "no".
     */
    Unknown,

    /**
     * Consent permits ad requests and the ads SDK reports itself initialized.
     *
     * The only state in which a rewarded offer may be put in front of the player.
     */
    Ready,

    /**
     * Ads cannot be served: consent was refused, the consent flow failed and the app fell
     * back to the previous session's status (CONS-04), or SDK initialization failed.
     *
     * Never blocks play. It is the direct input to D-12: the opt-in dialog is skipped and
     * the assist is granted for free, silently, exactly like v1.0 NoOp behaviour.
     */
    Unavailable,
}
