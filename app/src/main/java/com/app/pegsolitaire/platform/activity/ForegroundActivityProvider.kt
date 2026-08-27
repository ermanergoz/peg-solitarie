package com.app.pegsolitaire.platform.activity

import android.app.Activity
import android.app.Application

/**
 * The single source of the foreground [Activity] for platform-layer SDK calls
 * (rewarded-ad show, UMP consent form, Play Games client getters) — PLAT-01.
 *
 * Every real Play/AdMob SDK call needs a foreground Activity, and every naive way of
 * getting one either leaks it or drags `android.app.Activity` into a ViewModel. This
 * seam does it once: the implementation holds the Activity **weakly** and is fed by
 * `Application.registerActivityLifecycleCallbacks`.
 *
 * **This type is reachable ONLY from `com.app.pegsolitaire.platform..`.**
 * `PlatformBoundaryTest` reads bytecode and fails the build if any ViewModel, UI or
 * `:domain` class depends on it. [com.app.pegsolitaire.App] is the one class exempted
 * from that rule (via its `haveNameNotMatching(".*\\.App")` clause), because it is the
 * registrar: it calls [attachTo] from `onCreate`. The
 * [com.app.pegsolitaire.platform.ads.AdGateway] and
 * [com.app.pegsolitaire.platform.games.GamesService] interfaces deliberately expose no
 * Activity in any signature, so no caller outside platform can obtain one transitively.
 */
interface ForegroundActivityProvider {
    /**
     * The currently-resumed Activity, or `null` while the app is backgrounded.
     *
     * **Deliberately `null` while a fullscreen ad is on screen.** Showing an ad pauses
     * the host Activity, and the implementation clears the reference on pause — that is
     * the safer default, and callers honour it by capturing the Activity *before*
     * calling `show()`. Do not "fix" this by holding a strong reference across pause:
     * that reintroduces the configuration-change leak this seam exists to prevent.
     */
    val current: Activity?

    /**
     * Suspends until an Activity is resumed, then returns it.
     *
     * Callers MUST apply their own timeout: a backgrounded app may never resume, and an
     * ad request that waits forever leaves the assist control spinning with no way out.
     */
    suspend fun awaitActivity(): Activity

    /**
     * Registers the lifecycle callbacks that feed this provider. Called exactly once,
     * from [com.app.pegsolitaire.App]'s `onCreate`, after `super.onCreate()`.
     */
    fun attachTo(application: Application)
}
