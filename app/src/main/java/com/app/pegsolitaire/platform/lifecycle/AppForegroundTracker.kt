package com.app.pegsolitaire.platform.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.app.pegsolitaire.data.InterstitialCadenceStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns real whole-app foreground/background into the interstitial session boundary
 * (D-04, D-16): `ON_STOP` stamps when the app left, `ON_START` asks whether enough time has
 * passed since that stamp to call this a new session.
 *
 * ## Why NOT `DefaultForegroundActivityProvider` (D-16)
 *
 * The obvious choice is the wrong one, so it is written down. `DefaultForegroundActivityProvider`
 * is the app's only other lifecycle hook, and reaching for it here would look like reuse — but it
 * clears its Activity on `onActivityPaused`, and **showing a fullscreen ad pauses the host
 * Activity**. Sourcing the session boundary from it would therefore forge a "backgrounded" event
 * on every single interstitial, reset the D-04 per-session cap on every show, and leave a cap that
 * can never bind: the one thing in the phase that limits how many ads a long sitting can produce
 * would be silently disabled by the very ads it exists to limit (T-10-11).
 *
 * `ProcessLifecycleOwner` is immune to that by construction: it dispatches `ON_STOP` only after
 * every Activity in the process has stopped, and it debounces same-process transitions, so a
 * same-process ad Activity is not a background. **[ASSUMED]** — that last claim is the framework's
 * documented behaviour rather than something a JVM test can prove (RESEARCH assumption A1); the
 * device-UAT step in plan 10-06 is what confirms it against a real ad.
 *
 * ## Wall-clock, deliberately
 *
 * The stamp is `System.currentTimeMillis()`, not `SystemClock.elapsedRealtime()`, because it is
 * persisted and compared **after process death** and no monotonic timebase survives that: an
 * uptime value read back after a reboot is meaningless (RESEARCH pitfall 10). This is the exact
 * opposite of `RewardedAdSlot`'s freshness bound, which uses `elapsedRealtime` precisely because it
 * is never persisted and must not be movable by the user's clock. A moved clock here can only
 * mis-classify one session boundary, and
 * [com.app.pegsolitaire.domain.ads.InterstitialCadenceState.isNewSession] resolves a negative delta
 * toward keeping the session — the direction that keeps the cap binding.
 *
 * ## Why `@Singleton` is on the CLASS
 *
 * The reason [com.app.pegsolitaire.platform.activity.DefaultForegroundActivityProvider] documents,
 * one step removed: this class holds no state itself, but the [InterstitialCadenceStore] it drives
 * does, and [com.app.pegsolitaire.App] injects the CONCRETE type here — there is no interface and
 * none is needed. `@Singleton` on a `@Binds` scopes the *binding*, so with no binding to scope, the
 * annotation on the class is the only thing that guarantees one instance and therefore one
 * registration against [ProcessLifecycleOwner].
 *
 * Nothing here logs (ASVS V7 — ad-adjacent state).
 */
@Singleton
class AppForegroundTracker
    @Inject
    constructor(
        private val store: InterstitialCadenceStore,
    ) : DefaultLifecycleObserver {
        /**
         * Starts observing the process lifecycle. Called once, from `App.onCreate` — the observer
         * is app-lifetime by definition and is never removed.
         */
        fun attach() {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }

        /** The app came to the foreground: a long enough absence makes this a new session. */
        override fun onStart(owner: LifecycleOwner) {
            store.startSessionIfGapElapsed(System.currentTimeMillis())
        }

        /** The app left the foreground: stamp when, so the next [onStart] can measure the gap. */
        override fun onStop(owner: LifecycleOwner) {
            store.recordForegroundLeft(System.currentTimeMillis())
        }
    }
