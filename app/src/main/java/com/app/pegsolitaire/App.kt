package com.app.pegsolitaire

import android.app.Application
import com.app.pegsolitaire.audio.SoundPlayer
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.lifecycle.AppForegroundTracker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point and Hilt's `SingletonComponent` root.
 *
 * `@HiltAndroidApp` triggers Hilt's code generation: the processor (running under
 * KSP — no KAPT) emits the component graph so `@AndroidEntryPoint` activities and,
 * later, `@HiltViewModel` view models can be injected with zero further build
 * setup. The Kotlin pin (2.3.20) has a matching KSP build (2.3.9), unblocking DI.
 *
 * It is the registrar for **two** platform observers — [ForegroundActivityProvider] (PLAT-01)
 * and [AppForegroundTracker] (D-16) — and the place the sound samples start decoding. The two
 * are not redundant: the first tracks which Activity is resumed and clears on
 * `onActivityPaused`, which a fullscreen ad triggers; the second tracks whether the *process*
 * is foregrounded, which a same-process ad does not disturb. Only the second may source the
 * interstitial session boundary; see [AppForegroundTracker]'s KDoc for why.
 *
 * The ordering in [onCreate] is **not optional**: Hilt performs field injection into an
 * `Application` as part of `super.onCreate()`, so [activityProvider], [gamesService],
 * [foregroundTracker], [soundPlayer] and [appScope] are only non-null once that call has
 * returned — touching any of them before it would throw
 * `UninitializedPropertyAccessException`.
 *
 * It is also where the Play Games SDK is initialized for an already-opted-in player, through
 * [GamesService.initializeIfOptedIn] (PGS-01). That call site is load-bearing in **both**
 * directions: it must be after `super.onCreate()` because that is where Hilt injects, and it
 * must be inside `Application.onCreate` because the SDK's automatic sign-in attempt fires only
 * for an Activity created *after* initialization — from `MainActivity.onCreate` it is already
 * too late, and an opted-in player's cold relaunch silently resolves to signed-out (Phase 11
 * UAT test 1). The opt-in gate itself lives behind the seam and is unchanged: a player who
 * never opted in is still never reached by the SDK (D-07/D-17).
 *
 * That is also why this class imports **nothing** from `com.google.android.gms`: the SDK call
 * happens inside `platform/games`, and [PlatformBoundaryTest]'s exemption for this class
 * covers only [ForegroundActivityProvider] — it grants no licence to name an SDK type here.
 *
 * The preload lives here rather than in `MainActivity` because SOUND-01 requires the first
 * tap of a session to be audible: `SoundPool` decodes asynchronously, so the decode has to
 * be requested at the earliest point in the process, not when a board is already on
 * screen. It runs on the application scope so a cold start never blocks on file IO.
 *
 * Every type declared here that HAS an interface is declared as the interface, never as the
 * implementation. `@Binds @Singleton` scopes the binding of the interface, not of the concrete
 * class, so naming a concrete type would hand this class a second, unscoped instance: one that
 * never receives a lifecycle event (`awaitActivity()` would hang forever with no exception), or
 * one owning a second `SoundPool` that the sound-off switch could never silence.
 * [AppForegroundTracker] is the one exception and is not an inconsistency: it has no interface,
 * so there is no binding to scope, and `@Singleton` sits on that class itself — which is what
 * makes the concrete type safe to inject here. See its KDoc.
 *
 * `App` is consequently the one class outside `com.app.pegsolitaire.platform..` allowed
 * to depend on [ForegroundActivityProvider]: [PlatformBoundaryTest] exempts it by name
 * with a `haveNameNotMatching(".*\\.App")` clause. That exemption is deliberately
 * documented at both ends — if this registration ever moves, remove the exemption too.
 * It is **not** being widened here: the exemption names
 * `com.app.pegsolitaire.platform.activity.ForegroundActivityProvider` by fully-qualified name,
 * and [AppForegroundTracker] is a different type that no boundary rule names at all.
 */
@HiltAndroidApp
class App : Application() {
    @Inject
    lateinit var activityProvider: ForegroundActivityProvider

    // The interface, never `PlayGamesService` — see the class KDoc's rule; a concrete type
    // here would be a second, unscoped instance whose session nothing ever updates.
    @Inject
    lateinit var gamesService: GamesService

    @Inject
    lateinit var foregroundTracker: AppForegroundTracker

    @Inject
    lateinit var soundPlayer: SoundPlayer

    // `@field:` is required, not stylistic: a qualifier with no declared `@Target` lands on
    // the Kotlin *property* by default, where Dagger's field injection cannot see it, and
    // the graph would then fail to resolve an unqualified CoroutineScope.
    @Inject
    @field:ApplicationScope
    lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        activityProvider.attachTo(this)
        // Position is load-bearing in both directions: after super.onCreate() because Hilt
        // injects there, and inside Application.onCreate because the SDK's automatic sign-in
        // attempt only fires for an Activity created AFTER initialize (Phase 11 UAT test 1).
        // Opt-in-gated behind the seam, so a player who never opted in is untouched.
        gamesService.initializeIfOptedIn()
        foregroundTracker.attach()
        appScope.launch { soundPlayer.preload() }
    }
}
