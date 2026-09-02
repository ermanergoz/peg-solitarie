package com.app.pegsolitaire.platform.ads

import android.app.Activity
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.ads.consent.AdsReadiness
import com.app.pegsolitaire.platform.ads.consent.ConsentGateway
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * The real, ad-backed [AdGateway] — Google Mobile Ads Next-Gen rewarded and interstitial
 * ads, loaded behind the consent gate (ADS-01, ADS-03, ADS-05, CONS-02, INT-01, INT-02).
 *
 * **Two ad kinds, two independent slots, one shared discipline.** [RewardedAdSlot] and
 * [InterstitialAdSlot] hold at most one ad each and never share inventory: a rewarded ad
 * is *offered* — the player accepts it in a dialog before anything plays — while an
 * interstitial is not, which is exactly why [interstitialReady] additionally requires that
 * an ad is already held (D-10: no ad, no pause, no scrim, instant navigation). What both
 * paths obey without exception: no request is issued while consent is unresolved or
 * refused, no Google Mobile Ads type appears in any signature Hilt can see, the shown
 * instance is destroyed on every terminal callback, and nothing logs.
 *
 * **This class is the last step of a deliberate ordering, not the first.** Every opt-in
 * behaviour above it — the dialog, the allowance, the fail-open free grant, the D-11
 * snackbar, the clock freeze — was built and tested against [NoOpAdGateway] before this
 * existed, precisely so that binding this class changes *which ad plays*, never *whether
 * the player is asked*. Binding a real gateway earlier would have shipped an AdMob policy
 * violation: rewarded inventory may only be served after an explicit opt-in.
 *
 * **Every wait here is bounded, and each bound answers a different question.**
 * [AdConstants.ACCEPT_WAIT_MS] caps the wait for an ad to *load* and is the only bound on
 * the dialog's loading state — the ViewModel deliberately applies none, because from there
 * "still loading" and "on screen" are indistinguishable and a blanket cap would abort an ad
 * the player is legitimately watching. [AdConstants.ACTIVITY_WAIT_MS] caps the wait for a
 * foreground Activity. Neither may be removed and no third bound may be added around the
 * presentation itself.
 *
 * **Threading: `requestRewarded` runs on the main thread and must stay there.** It is
 * called from `viewModelScope`, which is `Dispatchers.Main` by default, and every Google
 * Mobile Ads call except `MobileAds.initialize` must be made from the main thread.
 * `initialize` is the sole exception and is already handled off-thread inside
 * [com.app.pegsolitaire.platform.ads.consent.UmpConsentGateway]. Moving this work to an
 * IO dispatcher would look like an improvement and would be a bug; there is no blocking
 * work here to move.
 *
 * **Nothing here logs.** Ad response info, consent status and the TCF string are all
 * personal-data-adjacent (ASVS V7), so no diagnostic call may name them — which is why
 * this paragraph, like [com.app.pegsolitaire.platform.ads.consent.UmpConsentGateway]'s,
 * avoids spelling any logging API out. Every failure degrades to
 * [RewardOutcome.Unavailable], which grants the assist anyway.
 */
@Singleton
class AdMobAdGateway
    @Inject
    constructor(
        private val consentGateway: ConsentGateway,
        private val slot: RewardedAdSlot,
        private val interstitialSlot: InterstitialAdSlot,
        private val activityProvider: ForegroundActivityProvider,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : AdGateway {
        /**
         * `true` only while consent has actually resolved and the ads SDK reports itself
         * initialized.
         *
         * [AdsReadiness.Unknown] maps to **false**, not to "offerable, let's find out"
         * (Pitfall 16). A fast player on a slow network can exhaust an allowance before the
         * consent bootstrap answers; offering "watch a short ad?" and then discovering
         * nothing can be served produces exactly the instant-free-grant absurdity D-12
         * exists to rule out. `false` sends the caller down D-12's silent free path
         * instead, which is indistinguishable from v1.0 behaviour.
         *
         * Derived rather than mirrored into a second mutable flow: one source of truth,
         * and nothing mutable escapes.
         */
        override val rewardedOfferable: StateFlow<Boolean> =
            consentGateway.readiness
                .map { it == AdsReadiness.Ready }
                .stateIn(scope, SharingStarted.Eagerly, false)

        /**
         * Warms an ad, but only when one could legally be served.
         *
         * D-09 keeps this stingy from above — the ViewModel calls it only on the spend that
         * takes a free counter to zero, so most levels issue no ad request at all. The
         * readiness check adds the other half: there is nothing to gain from loading an ad
         * the consent state forbids showing, and issuing that request is itself the GDPR
         * exposure (T-09-44).
         */
        override fun preloadRewarded(reason: AdReason) {
            if (rewardedOfferable.value) slot.preload()
        }

        /**
         * Loads and shows a rewarded ad, answering with the three-way [RewardOutcome].
         *
         * The consent check is repeated here rather than trusted from [rewardedOfferable]
         * because an offer and its acceptance are separated by a dialog the player may sit
         * on: readiness can change in between, and this is the SDK-side half of the CONS-02
         * gate.
         *
         * Load failure, no fill, no foreground Activity and a failed presentation all
         * resolve to [RewardOutcome.Unavailable], which the caller treats as "proceed
         * anyway" — ADS-03 grants an assist that could not be paid for rather than charging
         * the player for the network's bad day.
         */
        override suspend fun requestRewarded(reason: AdReason): RewardOutcome {
            if (consentGateway.readiness.value != AdsReadiness.Ready) return RewardOutcome.Unavailable

            // D-10's bounded wait — this is the spinner the dialog is showing.
            val ad = withTimeoutOrNull(AdConstants.ACCEPT_WAIT_MS) { slot.await() }

            // Captured BEFORE show(), and never re-awaited inside a callback: showing a
            // fullscreen ad pauses the host, so ForegroundActivityProvider.current is
            // deliberately null for the whole presentation and the failure mode of asking
            // again from a callback is a hang, not a crash (Pitfall 13, T-09-45).
            val activity =
                ad?.let {
                    withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) { activityProvider.awaitActivity() }
                }

            return if (ad != null && activity != null) {
                showForOutcome(ad, activity, reason)
            } else {
                RewardOutcome.Unavailable
            }
        }

        /**
         * `true` only while consent has resolved **and** an interstitial is actually held.
         *
         * Both halves are load-bearing and neither is redundant. Consent is the serving
         * gate (Phase 9 D-20): without it nothing may be shown, whatever is loaded. A held
         * ad is what D-10 means by "an ad will actually show", and it is the difference
         * between a dimmed 500 ms pause and an instant navigation — offering the pause
         * without the ad is the interstitial form of the instant-free-grant absurdity D-12
         * rules out on the rewarded side.
         *
         * Derived rather than mirrored into a second mutable flow, like
         * [rewardedOfferable]: one source of truth per half, and nothing mutable escapes.
         */
        override val interstitialReady: StateFlow<Boolean> =
            combine(consentGateway.readiness, interstitialSlot.ready) { readiness, held ->
                readiness == AdsReadiness.Ready && held
            }.stateIn(scope, SharingStarted.Eagerly, false)

        /**
         * Warms an interstitial, but only when one could legally be served.
         *
         * D-12 keeps this stingy from above — the caller asks only at the moment a win
         * makes the next continue tap ad-eligible — and the readiness check adds the other
         * half: issuing the request is itself the GDPR exposure (T-09-44), so there is
         * nothing to gain from loading an ad the consent state forbids showing.
         *
         * The gate is deliberately [ConsentGateway.readiness] and **not**
         * [interstitialReady], which would be self-defeating: that flow is `false`
         * precisely when nothing is loaded yet, which is exactly when a warm-up is wanted.
         */
        override fun preloadInterstitial() {
            if (consentGateway.readiness.value == AdsReadiness.Ready) interstitialSlot.preload()
        }

        /**
         * Shows a held interstitial, answering whether one was actually presented (D-09).
         *
         * Three guards return `false` with no side effect at all — consent not resolved,
         * no fresh ad held, no foreground Activity — and every one of them means the same
         * thing to the caller: navigate immediately, advance no counters (D-11). A silent
         * skip is the whole degradation story here; there is no snackbar and no retry,
         * because the player asked to continue, not to see an ad.
         *
         * **The held ad is peeked, never awaited.** This is the deliberate divergence from
         * [requestRewarded], which spends [AdConstants.ACCEPT_WAIT_MS] waiting on a load:
         * D-10 forbids adding any latency to a continue tap, so a slot that is empty at
         * this instant is simply a tap that navigates now. The ad that was still loading
         * stays warm for the next eligible win.
         *
         * The consent-and-slot pair is factored into [servableInterstitial] rather than
         * written as two guard clauses, for the reason `GameViewModel.resumeTickerIfNeeded`
         * folds its two guards into one: detekt's `ReturnCount` caps a function at two
         * exits. Neither condition changed in the folding, and the two are one question
         * anyway — "is there an ad I am allowed to show right now?".
         */
        override suspend fun showInterstitial(): Boolean {
            val ad = servableInterstitial() ?: return false

            // Captured BEFORE show(), and never re-awaited inside a callback: showing a
            // fullscreen ad pauses the host, so ForegroundActivityProvider.current is
            // deliberately null for the whole presentation and the failure mode of asking
            // again from a callback is a hang, not a crash (Pitfall 13, T-09-45).
            val activity =
                withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) { activityProvider.awaitActivity() }

            return if (activity != null) showToDismissal(ad, activity) else false
        }

        /**
         * The interstitial that may be presented at this instant, or `null`.
         *
         * Two conditions, one question. The consent check is repeated here rather than
         * trusted from [interstitialReady] for the reason [requestRewarded] repeats it:
         * readiness can change between the flow being sampled by a surface and the show
         * being attempted, and this is the SDK-side half of the CONS-02 gate. The slot
         * peek never starts a load (D-10).
         */
        private fun servableInterstitial(): InterstitialAd? =
            if (consentGateway.readiness.value == AdsReadiness.Ready) interstitialSlot.heldFresh() else null

        /**
         * Bridges the SDK's callback-shaped presentation into the suspend contract.
         *
         * **The reward flag is set in exactly one place: inside the
         * `OnUserEarnedRewardListener` passed to `show()`.** It is never set from a
         * dismissal and never inferred from one. ADS-05 and the AdMob rewarded-inventory
         * policy both require the reward come only from that callback — granting on
         * dismissal is fraud against the advertiser (T-09-43), and it is the single most
         * consequential line in this file. A player who closes the ad early therefore gets
         * [RewardOutcome.Declined], which costs them nothing and refills nothing.
         *
         * Both terminal callbacks discard the ad first: a rewarded ad may be shown exactly
         * once (Pitfall 11). Both resumes are guarded on `isActive`, because a cancelled
         * caller must not be resumed twice, and the callback is detached on cancellation so
         * a dead continuation cannot be reached through a retained SDK object.
         */
        private suspend fun showForOutcome(
            ad: RewardedAd,
            activity: Activity,
            reason: AdReason,
        ): RewardOutcome =
            suspendCancellableCoroutine { continuation ->
                var earned = false

                ad.adEventCallback =
                    object : RewardedAdEventCallback {
                        override fun onAdDismissedFullScreenContent() {
                            recycleSlot(reason)
                            if (continuation.isActive) {
                                continuation.resume(
                                    if (earned) RewardOutcome.Granted else RewardOutcome.Declined,
                                )
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                            // The error is deliberately not surfaced or recorded: the honest
                            // reaction is to grant the assist anyway (ADS-03). This branch is
                            // also the net under REWARDED_AD_MAX_AGE_MS's assumed expiry —
                            // a stale ad that slipped through lands here.
                            recycleSlot(reason)
                            if (continuation.isActive) continuation.resume(RewardOutcome.Unavailable)
                        }
                    }

                // ADS-05: the one and only place the reward becomes true.
                ad.show(activity) { _: RewardItem -> earned = true }

                continuation.invokeOnCancellation { ad.adEventCallback = null }
            }

        /**
         * Discards the just-shown ad and warms the next one.
         *
         * The discard is mandatory — the instance cannot be shown again. The re-warm is a
         * judgement call worth naming: D-09 says no ad request should be in flight while
         * free allowance remains, and after a granted reward the allowance has just been
         * refilled. This is nonetheless **one** bounded request, not a maintained buffer,
         * which is precisely the distinction that made a hand-rolled slot preferable to the
         * SDK's `RewardedAdPreloader` (Pitfall 12): that one keeps refilling for the rest of
         * the session, this one loads once and then stops until something asks again. It
         * still passes through the readiness gate, so a request is never issued against a
         * consent state that forbids it.
         */
        private fun recycleSlot(reason: AdReason) {
            slot.invalidate()
            preloadRewarded(reason)
        }

        /**
         * Bridges the interstitial's callback-shaped presentation into the suspend
         * contract — [showForOutcome] with the reward plumbing deleted.
         *
         * The outcome is binary because there is nothing to earn: dismissal means the ad
         * was presented and watched to whatever end the player chose (`true`), a failure to
         * show means it never appeared (`false`). Nothing distinguishes an early close from
         * a full view here, and nothing should: the player was not asked for anything, so
         * there is nothing to withhold. This is also the branch under
         * [AdConstants.INTERSTITIAL_AD_MAX_AGE_MS]'s assumed expiry — a stale ad that
         * slipped through fails here and becomes D-11's silent skip.
         *
         * Both terminal callbacks discard the ad first: an interstitial may be shown
         * exactly once (Pitfall 9). Both resumes are guarded on `isActive`, because a
         * cancelled caller must not be resumed twice, and the callback is detached on
         * cancellation so a dead continuation cannot be reached through a retained SDK
         * object.
         */
        private suspend fun showToDismissal(
            ad: InterstitialAd,
            activity: Activity,
        ): Boolean =
            suspendCancellableCoroutine { continuation ->
                ad.adEventCallback =
                    object : InterstitialAdEventCallback {
                        override fun onAdDismissedFullScreenContent() {
                            recycleInterstitial()
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                            // The error is deliberately not surfaced or recorded: the honest
                            // reaction is to navigate immediately and leave the cadence
                            // counters running, so the next eligible win can show one (D-11).
                            recycleInterstitial()
                            if (continuation.isActive) continuation.resume(false)
                        }
                    }

                ad.show(activity)

                continuation.invokeOnCancellation { ad.adEventCallback = null }
            }

        /**
         * Discards the just-shown interstitial and warms the next one — [recycleSlot]'s
         * twin.
         *
         * The discard is mandatory: the instance cannot be shown again. The re-warm is
         * one bounded request rather than a maintained buffer, and it still passes through
         * [preloadInterstitial]'s consent gate, so a request is never issued against a
         * consent state that forbids it. Re-warming here rather than waiting for the next
         * eligibility signal is deliberate: the earliest moment an interstitial is
         * certainly *not* imminent is the instant one has just finished, so this is the
         * cheapest possible time to pay for the next load.
         */
        private fun recycleInterstitial() {
            interstitialSlot.invalidate()
            preloadInterstitial()
        }
    }
