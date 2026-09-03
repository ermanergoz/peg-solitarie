package com.app.pegsolitaire.ui.results

import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.platform.ads.RewardOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An [AdGateway] whose interstitial presentation is held open on a gate until the test
 * releases it — the harness the whole Results trigger surface is pinned with.
 *
 * Shared as a top-level double rather than kept private to one test class, following
 * [com.app.pegsolitaire.ui.settings.FakeConsentGateway]'s promotion: `ResultsViewModelTest`
 * drives it as the subject, and `ResultsScreenTest` will need the same shape once the route
 * is wired (plan 10-05). Modelled on `GameViewModelAdClockTest`'s `AdClockGatedAdGateway`,
 * which does the same job for the rewarded path.
 *
 * The four decisions it exists to make assertable:
 *
 *  - **D-08 (quiescence timing)** — [showCount] increments BEFORE the gate is awaited, so a
 *    test can distinguish "called and waiting" from "not yet called". That distinction is
 *    the only way to assert that nothing was shown until
 *    `AdConstants.INTERSTITIAL_QUIESCENCE_MS` of virtual time had elapsed.
 *  - **D-09 (ad first, then navigate)** — the span between [showInterstitial] being entered
 *    and [releaseShow] being called *is* the presentation. Navigation asserted to be absent
 *    inside that span and present after it is exactly D-09.
 *  - **D-10 (the zero-latency fast path)** — [interstitialReady] starting `false` makes the
 *    "no ad held" branch reachable without any timing at all.
 *  - **D-11 (the silent skip)** — [showResult] `false` reproduces a held ad that fails at
 *    `show()`, which the caller must treat as "navigate, record nothing".
 *
 * The rewarded members are inert: this fake exists for the Results screen, which never
 * offers a rewarded ad.
 *
 * @param interstitialReadyInitially the starting [interstitialReady] value. Defaults to
 *   `true` because the ad path is what this double is for; the *test factory's* default is
 *   deliberately the opposite, so a test that says nothing about ads gets none.
 * @param showResult what [showInterstitial] answers once released — `true` for "an ad
 *   actually presented", `false` for the no-fill / failed-show path.
 */
class FakeAdGateway(
    interstitialReadyInitially: Boolean = true,
    private val showResult: Boolean = true,
) : AdGateway {
    private val readyState = MutableStateFlow(interstitialReadyInitially)

    private val showGate = CompletableDeferred<Unit>()

    override val interstitialReady: StateFlow<Boolean> = readyState.asStateFlow()

    /** How many times [preloadInterstitial] was called — the D-12 warm-up, counted. */
    var preloadCount = 0
        private set

    /** How many times [showInterstitial] was ENTERED, counted before the gate is awaited. */
    var showCount = 0
        private set

    override fun preloadInterstitial() {
        preloadCount++
    }

    override suspend fun showInterstitial(): Boolean {
        showCount++
        showGate.await()
        return showResult
    }

    /** Ends the presentation, resuming whoever is parked inside [showInterstitial]. */
    fun releaseShow() {
        showGate.complete(Unit)
    }

    /** Simulates a load landing (or being invalidated) after construction. */
    fun setInterstitialReady(ready: Boolean) {
        readyState.value = ready
    }

    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun preloadRewarded(reason: AdReason) = Unit

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome = RewardOutcome.Granted
}
