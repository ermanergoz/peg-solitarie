package com.app.pegsolitaire.platform.ads

import android.os.SystemClock
import com.app.pegsolitaire.di.ApplicationScope
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds at most **one** interstitial ad: load it, keep it until it is shown or goes stale,
 * hand it over exactly once, then throw it away (INT-01, D-10, D-11, Pitfall 9).
 *
 * **This class is [RewardedAdSlot] retyped, and the symmetry is a design requirement.**
 * The dominant risk of this phase is not that anything here is hard — it is that a second,
 * subtly different ad-holding class drifts away from the first, so that a fix applied to
 * one silently misses the other. Everything below therefore keeps the shipped rewarded
 * shape: the same three confined fields, the same idempotent [preload], the same
 * one-attempt retry, the same "a no-fill is not an error path" mapping. The two deliberate
 * deltas — there is no `await()`, and readiness is observable — each carry a KDoc
 * paragraph saying so, precisely so that the *rest* of the file reads as a clone.
 *
 * **Why a hand-rolled slot rather than the SDK's `InterstitialAdPreloader`.** The same
 * argument [RewardedAdSlot] makes: the preloader's `pollAd()` "returns the next available
 * ad and loads another ad in the background" and `start()` keeps that buffer full for the
 * rest of the session, whereas D-12 wants a request issued only at the moment a win makes
 * the *next* continue-tap ad-eligible — no request while first-session suppression, the
 * session cap or the level/time gates make an ad impossible. Reaching that behaviour
 * through the preloader means a `destroy(preloadId)` after every show and a `start()` at
 * every re-eligibility: its lifecycle fought at every step, to arrive where a single slot
 * begins. The trade-off accepted in exchange is that retry is ours to write, and it is
 * deliberately one attempt (see [loadIntoSlot]).
 *
 * **`@Singleton` is on the class, not merely on a binding.** This object *is* state — the
 * held ad, its load time and the in-flight load. A second, unscoped instance would hold a
 * second ad, request a second impression, and leak the first. The precedent and full
 * reasoning are in
 * [com.app.pegsolitaire.platform.activity.DefaultForegroundActivityProvider], annotated
 * this way for exactly the same reason.
 *
 * **Main-thread confined.** Every Google Mobile Ads call except `MobileAds.initialize`
 * must happen on the main thread, so the load runs on [Dispatchers.Main] and every
 * mutation of the three fields below happens there too: [preload], [heldFresh] and
 * [invalidate] are called from the ad gateway, which is driven from `viewModelScope` and
 * from the SDK's own main-thread callbacks. That confinement is what makes plain `var`s
 * safe here; there is no lock because there is no second thread, and adding a background
 * dispatcher would break the SDK contract rather than improve concurrency.
 *
 * **Nothing here logs**, for the reason [AdMobAdGateway] states: ad response info is
 * personal-data-adjacent (ASVS V7), so no diagnostic call may name it.
 *
 * No ad-unit literal appears here or anywhere else under `src/main` — [AdIds] behind
 * `BuildConfig` is the only sanctioned route, and the `adIdLiteralCheck` build gate fails
 * the build over any literal (Phase 6 D-06/D-08).
 */
@Singleton
class InterstitialAdSlot
    @Inject
    constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        /** The one ad, or `null` when the slot is empty. */
        private var held: InterstitialAd? = null

        /**
         * When [held] was loaded, on the monotonic
         * [SystemClock.elapsedRealtime] timebase — deliberately not wall-clock, which a
         * timezone change or an NTP correction can move backwards under a long session.
         */
        private var loadedAtMs: Long = 0L

        /** The load currently in flight, so two callers share one request rather than racing. */
        private var inFlight: Deferred<InterstitialAd?>? = null

        /** Backing state for [ready]; mutated only on the main thread, like the fields above. */
        private val readyState = MutableStateFlow(false)

        /**
         * Whether an interstitial is held right now — **the first of two deliberate
         * departures from [RewardedAdSlot]**, which exposes no such signal.
         *
         * Observable rather than a plain getter, borrowing [AdGateway.rewardedOfferable]'s
         * argument: consent resolves asynchronously after launch and a load lands later
         * still, so a surface must be able to *react* to readiness arriving rather than
         * hold a stale answer captured at composition time. The rewarded path never needed
         * this because its readiness question is answered by consent alone; the
         * interstitial path additionally needs "and one is actually loaded", because D-10
         * makes that the difference between a dimmed pause and an instant navigation.
         *
         * It carries a `Boolean` and never the ad, so no Google Mobile Ads type crosses
         * into anything Hilt or a ViewModel can see — the containment rule written up in
         * [com.app.pegsolitaire.platform.ads.di.AdsModule].
         *
         * **Its honest limit:** `true` means *an ad is held*, not *an ad will present*. A
         * held ad can still fail at `show()`, and that failure is indistinguishable from a
         * no-fill — both land on D-11's silent-skip path, where the tap navigates
         * immediately and the cadence counters keep running. Treat this as a useful
         * predicate, never as a guarantee.
         */
        val ready: StateFlow<Boolean> = readyState.asStateFlow()

        /**
         * Starts warming an ad if the slot is empty and nothing is already loading.
         * Fire-and-forget and **idempotent** — calling it with an ad already held, or with
         * a load in flight, does nothing at all.
         *
         * Called at the moment a win makes the next continue-tap ad-eligible (D-12) and
         * again after each terminal ad callback, so the next eligible tap has something to
         * show.
         */
        fun preload() {
            if (freshOrDiscard() == null) startLoad()
        }

        /**
         * The held ad if one is loaded and young enough to be worth showing, otherwise
         * `null` — **the second deliberate departure from [RewardedAdSlot]**, which
         * instead offers a suspending wait for a load.
         *
         * The rewarded path can wait, because by then the player has accepted an offer and
         * is watching a spinner they asked for. An interstitial is **never** awaited: D-10
         * says a continue tap with no ad loaded must navigate instantly with zero added
         * latency, so the gateway only ever peeks. This never starts a load; [preload] is
         * the only thing that does.
         *
         * A peek at an over-age slot empties it as a side effect, exactly as
         * [freshOrDiscard] does — of which this is simply the public form.
         */
        fun heldFresh(): InterstitialAd? = freshOrDiscard()

        /**
         * Empties the slot, destroying the ad it held.
         *
         * Called after every terminal show callback because **an interstitial may be shown
         * exactly once** (Pitfall 9) — reusing the instance for a second show is the
         * anti-pattern this method exists to make impossible — and by [freshOrDiscard]
         * when a slot has aged out. `destroy()` releases the SDK-side resources the ad
         * holds; dropping the reference alone would leak them.
         */
        fun invalidate() {
            held?.destroy()
            held = null
            loadedAtMs = 0L
            readyState.value = false
        }

        /**
         * The held ad if it is still young enough to be worth showing, otherwise `null`
         * with the slot emptied as a side effect.
         *
         * An over-age ad is treated exactly like an empty slot rather than shown and
         * hoped for: see [AdConstants.INTERSTITIAL_AD_MAX_AGE_MS] for why that bound is a
         * defensive heuristic rather than a documented guarantee. The age-out path runs
         * through [invalidate], which is what also drops [ready] back to `false`.
         */
        private fun freshOrDiscard(): InterstitialAd? {
            val age = SystemClock.elapsedRealtime() - loadedAtMs
            if (held != null && age >= AdConstants.INTERSTITIAL_AD_MAX_AGE_MS) invalidate()
            return held
        }

        /**
         * The in-flight load, starting one on the application scope if none is running.
         *
         * Deliberately started on [scope] rather than on the caller's coroutine: nothing
         * ever waits on this load, so tying it to a ViewModel's lifetime would let a
         * Results screen leaving composition cancel the warm-up it just asked for.
         */
        private fun startLoad(): Deferred<InterstitialAd?> {
            val running = inFlight
            if (running != null && running.isActive) return running
            // Dispatchers.Main because every GMA call other than MobileAds.initialize must
            // be made from the main thread, and because it is the confinement that keeps
            // the fields above lock-free. See the class KDoc.
            return scope.async(Dispatchers.Main) { loadIntoSlot() }.also { inFlight = it }
        }

        /**
         * Loads an ad into the slot, with **one** bounded retry, and never more.
         *
         * The retry is a single delayed second attempt rather than a loop, and that is a
         * decision rather than a simplification: an unbounded or exponential retry against
         * a no-fill response burns battery and request quota for a player who has asked
         * for nothing, and sustained retrying is what ad networks read as abusive traffic.
         * One attempt, one retry, then stop until something asks again.
         *
         * Nothing is waiting on either attempt — D-10 forbids a continue tap waiting on an
         * ad — so a slow retry costs the player nothing. At worst the tap that arrives
         * mid-retry navigates instantly and the ad it missed is still warm for the next
         * eligible win (D-11: counters keep running, the skip is silent).
         */
        private suspend fun loadIntoSlot(): InterstitialAd? {
            val ad =
                loadOnce() ?: run {
                    delay(AdConstants.PRELOAD_RETRY_DELAY_MS)
                    loadOnce()
                }
            if (ad != null) {
                held = ad
                loadedAtMs = SystemClock.elapsedRealtime()
                readyState.value = true
            }
            inFlight = null
            return ad
        }

        /**
         * One load attempt, reduced to "an ad, or nothing".
         *
         * A [AdLoadResult.Failure] — no fill, no network, a bad request — is **not** an
         * error path here. It becomes `null`, the slot stays empty, [ready] stays `false`,
         * and the next eligible continue tap navigates immediately with no pause and no
         * scrim (D-10, D-11). A player who could not be served an ad is simply a player
         * who was not interrupted.
         *
         * The `runCatching` is the same fail-open stance applied to a *thrown* failure.
         * GMA Next-Gen makes initialization mandatory and throws on an ad call against an
         * uninitialized SDK; the gateway's readiness gate should make that unreachable,
         * but an escaping exception on the application scope is an app crash during
         * gameplay rather than a lost ad. [ensureActive] immediately after restores the one
         * thing `runCatching` must not swallow — coroutine cancellation.
         */
        private suspend fun loadOnce(): InterstitialAd? {
            val outcome = runCatching { InterstitialAd.load(AdRequest.Builder(AdIds.interstitialAdUnitId).build()) }
            currentCoroutineContext().ensureActive()
            return when (val result = outcome.getOrNull()) {
                is AdLoadResult.Success -> result.ad
                is AdLoadResult.Failure -> null
                null -> null
            }
        }
    }
