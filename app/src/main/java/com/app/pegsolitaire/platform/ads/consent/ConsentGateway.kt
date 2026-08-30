package com.app.pegsolitaire.platform.ads.consent

import kotlinx.coroutines.flow.StateFlow

/**
 * The platform seam through which the app runs the GDPR/consent flow and publishes
 * whether ads may be requested at all (CONS-01..CONS-04).
 *
 * **This interface names no consent-SDK type and no `Activity` type**, in the same way
 * [com.app.pegsolitaire.platform.games.GamesService] names no Play Games type. That is
 * the whole point of the seam: `MainActivity` and `SettingsViewModel` both live outside
 * `..platform..`, and `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` reads bytecode
 * and fails the build if either of them acquires an SDK dependency. Depending on this
 * interface is safe; depending on the implementation behind it would not be.
 *
 * The implementation owns the entire consent dance and reduces it to two observable
 * answers — [readiness] and [privacyOptionsRequired] — plus two commands. Callers never
 * see a consent form, a requirement status, or an initialization callback.
 */
interface ConsentGateway {
    /**
     * Whether ads may currently be requested, [AdsReadiness.Unknown] until the consent
     * flow answers for the first time.
     *
     * Observable rather than a plain getter because consent resolves asynchronously after
     * launch: a surface must react to the answer arriving instead of holding whatever was
     * true at composition time. This is the signal that feeds
     * `AdGateway.rewardedOfferable`, and therefore D-12 — see [AdsReadiness.Unknown] for
     * the rule that governs the pre-answer window.
     */
    val readiness: StateFlow<AdsReadiness>

    /**
     * Whether the privacy-options entry point must be offered to this player (CONS-03).
     *
     * `true` only while the consent framework reports the requirement as required — in
     * other regions and for other players there is nothing to show, and a settings screen
     * MUST hide the affordance rather than present one that opens nothing. Observable for
     * the same reason as [readiness]: the requirement is unknown until the flow answers.
     */
    val privacyOptionsRequired: StateFlow<Boolean>

    /**
     * Starts the consent sequence: refresh consent information, show any required form,
     * then initialize the ads SDK only if consent permits it (CONS-01/CONS-02).
     *
     * **Idempotent**, and called once per Activity creation — a configuration change must
     * not re-run the form or double-initialize the SDK. It runs the full sequence on
     * **every launch**, not only first install: consent can be revoked or expire, and the
     * consent framework's own "can we request ads?" answer is `false` until the refresh
     * has been asked for at least once this session.
     *
     * Non-suspending and fire-and-forget: the caller does not wait for consent. Progress
     * is reported through [readiness], and the app remains fully playable throughout —
     * a consent flow that fails serves no ads (CONS-04) rather than blocking play.
     */
    fun bootstrap()

    /**
     * Opens the privacy-options form so the player can change their consent choices
     * (CONS-03). A silent no-op when [privacyOptionsRequired] is `false`.
     *
     * **The Activity is deliberately absent from this signature.** The implementation
     * resolves it internally through
     * [com.app.pegsolitaire.platform.activity.ForegroundActivityProvider], bounded by
     * [com.app.pegsolitaire.platform.ads.AdConstants.ACTIVITY_WAIT_MS]. Taking an
     * `Activity` parameter would put the framework Activity type in a signature a
     * `SettingsViewModel` must call, which
     * `PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity` fails the build over — the
     * gate is the reason for the shape, not an obstacle to it.
     *
     * `suspend` because awaiting that Activity and the form's dismissal are both
     * genuinely asynchronous.
     */
    suspend fun showPrivacyOptions()
}
