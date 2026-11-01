package com.app.pegsolitaire.platform.ads

import android.os.SystemClock
import com.app.pegsolitaire.di.ApplicationScope
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds at most **one** rewarded ad: load it, keep it until it is shown or goes stale,
 * hand it over exactly once, then throw it away (D-09, D-10, Pitfall 11).
 *
 * **Why a hand-rolled slot rather than the SDK's `RewardedAdPreloader`.** The preloader
 * gives managed retry and a maintained buffer for free, and on any other project it would
 * be the right answer. It loses here on one property: `pollAd()` "returns the next
 * available ad and loads another ad in the background", and `start()` keeps that buffer
 * full for the rest of the session. D-09 says the opposite — **no ad request may be in
 * flight while free allowance remains** — so every successful show would have to be
 * followed by a `destroy(preloadId)` to stop the refill, and a later exhaustion by another
 * `start()`. That is the preloader's own lifecycle fought at every step, to reach the
 * behaviour a single slot has by construction. The trade-off accepted in exchange is that
 * retry is ours to write, and it is deliberately one attempt (see [loadIntoSlot]). If
 * device testing ever shows load latency hurting the accept path, the preloader is the
 * sanctioned fallback — but it arrives with that `destroy`/`start` bookkeeping attached.
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
 * mutation of the three fields below happens there too: [preload] and [invalidate] are
 * called from the ad gateway, which is driven from `viewModelScope` and from the SDK's own
 * main-thread callbacks. That confinement is what makes plain `var`s safe here; there is
 * no lock because there is no second thread, and adding a background dispatcher would
 * break the SDK contract rather than improve concurrency.
 *
 * No ad-unit literal appears here or anywhere else under `src/main` — [AdIds] behind
 * `BuildConfig` is the only sanctioned route, and the `adIdLiteralCheck` build gate fails
 * the build over any literal (Phase 6 D-06/D-08).
 */
@Singleton
class RewardedAdSlot
    @Inject
    constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        /** The one ad, or `null` when the slot is empty. */
        private var held: RewardedAd? = null

        /**
         * When [held] was loaded, on the monotonic
         * [SystemClock.elapsedRealtime] timebase — deliberately not wall-clock, which a
         * timezone change or an NTP correction can move backwards under a long session.
         */
        private var loadedAtMs: Long = 0L

        /** The load currently in flight, so two callers share one request rather than racing. */
        private var inFlight: Deferred<RewardedAd?>? = null

        /**
         * Starts warming an ad if the slot is empty and nothing is already loading.
         * Fire-and-forget and **idempotent** — calling it with an ad already held, or with
         * a load in flight, does nothing at all.
         *
         * Called at the moment a free assist counter reaches zero (D-09) and again after
         * each terminal ad callback, so the next offer has something to show.
         *
         * **Takes no [AdReason], deliberately.** The seam above it does — `AdGateway`'s
         * members are expressed per reason — but this class serves both assists from the
         * single rewarded unit, so the reason cannot change anything it does. Carrying it
         * anyway would document a distinction the code does not make, and detekt's
         * `UnusedParameter` says so at build time. If a second ad unit ever appears, the
         * parameter comes back here as a real one.
         */
        fun preload() {
            if (freshOrDiscard() == null) startLoad()
        }

        /**
         * Returns the held ad if one is loaded and not over-age, otherwise starts a load
         * and suspends until it finishes.
         *
         * **The caller owns the timeout.** [AdConstants.ACCEPT_WAIT_MS] is applied by
         * `AdMobAdGateway.requestRewarded`, and burying a second bound here would turn
         * D-10's single dialog deadline into two deadlines that can disagree — the dialog
         * would either give up while this one still waits, or spin past its own limit.
         *
         * Cancelling the wait does **not** cancel the load: the request lives on the
         * application scope, so an accept that times out still leaves a warm ad behind for
         * the next offer rather than throwing the work away.
         *
         * Takes no [AdReason] for the same reason [preload] does not — see there.
         */
        suspend fun await(): RewardedAd? = freshOrDiscard() ?: startLoad().await()

        /**
         * Empties the slot, destroying the ad it held.
         *
         * Called after every terminal show callback because **a rewarded ad may be shown
         * exactly once** (Pitfall 11) — reusing the instance for a second show is the
         * anti-pattern this method exists to make impossible — and by [freshOrDiscard] when
         * a slot has aged out. `destroy()` releases the SDK-side resources the ad holds;
         * dropping the reference alone would leak them.
         */
        fun invalidate() {
            held?.destroy()
            held = null
            loadedAtMs = 0L
        }

        /**
         * The held ad if it is still young enough to be worth showing, otherwise `null`
         * with the slot emptied as a side effect.
         *
         * An over-age ad is treated exactly like an empty slot rather than shown and
         * hoped for: see [AdConstants.REWARDED_AD_MAX_AGE_MS] for why that bound is a
         * defensive heuristic rather than a documented guarantee.
         */
        private fun freshOrDiscard(): RewardedAd? {
            val age = SystemClock.elapsedRealtime() - loadedAtMs
            if (held != null && age >= AdConstants.REWARDED_AD_MAX_AGE_MS) invalidate()
            return held
        }

        /**
         * The in-flight load, starting one on the application scope if none is running.
         *
         * Deliberately started on [scope] rather than on the caller's coroutine: a caller
         * that gives up waiting (the D-10 accept deadline) must not cancel the request it
         * was waiting for.
         */
        private fun startLoad(): Deferred<RewardedAd?> {
            val running = inFlight
            if (running != null && running.isActive) return running
            // Dispatchers.Main because every GMA call other than MobileAds.initialize must
            // be made from the main thread, and because it is the confinement that keeps
            // the three fields above lock-free. See the class KDoc.
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
         * Because [AdConstants.PRELOAD_RETRY_DELAY_MS] is far longer than the accept
         * deadline, the retry is in practice a preload-only affordance — an accepting
         * player falls open to a free assist rather than waiting for it (ADS-03).
         */
        private suspend fun loadIntoSlot(): RewardedAd? {
            val ad =
                loadOnce() ?: run {
                    delay(AdConstants.PRELOAD_RETRY_DELAY_MS)
                    loadOnce()
                }
            if (ad != null) {
                held = ad
                loadedAtMs = SystemClock.elapsedRealtime()
            }
            inFlight = null
            return ad
        }

        /**
         * One load attempt, reduced to "an ad, or nothing".
         *
         * A [AdLoadResult.Failure] — no fill, no network, a bad request — is **not** an
         * error path here. It becomes `null`, which the gateway maps to
         * [RewardOutcome.Unavailable], which grants the assist anyway (ADS-03): a player
         * is never charged for an ad the network could not supply.
         *
         * The `runCatching` is the same fail-open stance applied to a *thrown* failure.
         * GMA Next-Gen makes initialization mandatory and throws on an ad call against an
         * uninitialized SDK; the gateway's readiness gate should make that unreachable, but
         * this load can be awaited from `viewModelScope`, where an escaping exception is an
         * app crash during gameplay rather than a lost ad. [ensureActive] immediately after
         * restores the one thing `runCatching` must not swallow — coroutine cancellation.
         *
         * The request is identical for `HINT` and `UNDO` — one rewarded unit serves both.
         */
        private suspend fun loadOnce(): RewardedAd? {
            val outcome = runCatching { RewardedAd.load(AdRequest.Builder(AdIds.rewardedAdUnitId).build()) }
            currentCoroutineContext().ensureActive()
            return when (val result = outcome.getOrNull()) {
                is AdLoadResult.Success -> result.ad
                is AdLoadResult.Failure -> null
                null -> null
            }
        }
    }
