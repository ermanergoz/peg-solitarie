package com.app.pegsolitaire.platform.ads

import android.app.Activity
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.ads.consent.AdsReadiness
import com.app.pegsolitaire.platform.ads.consent.ConsentGateway
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardItem
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * The real, ad-backed [AdGateway] — a Google Mobile Ads Next-Gen rewarded ad, loaded
 * behind the consent gate and shown behind the opt-in dialog (ADS-01, ADS-03, ADS-05,
 * CONS-02).
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
         * Deliberately still a no-op. Interstitials between levels are Phase 10's work,
         * and adding one here would put an unrequested fullscreen ad in a phase whose
         * contract is "the player is always asked first".
         */
        override fun showInterstitial() = Unit

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
    }
