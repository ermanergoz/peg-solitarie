package com.app.pegsolitaire

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundPlayer
import com.app.pegsolitaire.data.ProgressSubmitter
import com.app.pegsolitaire.navigation.AppNavHost
import com.app.pegsolitaire.platform.ads.consent.ConsentGateway
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.save.DebugSnapshotPickerEntry
import com.app.pegsolitaire.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single host activity for the game.
 *
 * `@AndroidEntryPoint` registers this activity with Hilt's generated component graph
 * so each route's `@HiltViewModel` can be injected via `hiltViewModel()`. Hosts the
 * type-safe [AppNavHost] inside the Material 3 [AppTheme]: it owns the five-screen back
 * stack (Home → ClassicCatalog → Gameplay → Results), replacing the Phase-1 direct
 * `GameRoute()` host (NAV-02). The activity adds no logic beyond hosting — publishing the
 * injected [SoundPlayer] into [LocalSoundPlayer] is hosting too: it hands the composition
 * a dependency it cannot inject for itself and does nothing with it. The player's
 * lifecycle stays entirely in `App`/Hilt; in particular the startup preload is **not**
 * here, because SOUND-01 needs the decode to start before an activity exists.
 *
 * It is also where the three platform bootstraps start: the consent flow (CONS-01, D-16) —
 * see [consentGateway] — the opt-in-gated Play Games sign-in (PGS-01, D-01) — see
 * [gamesService] — and the Play Games submission collector (PGSL-05, D-10) — see
 * [progressSubmitter]. Each is one undecorated, fire-and-forget line; none branches here.
 *
 * Every seam declared here is the **interface**, for the same reason `App` documents:
 * naming a concrete implementation instead would build a second, unscoped instance — a
 * second player owning a second `SoundPool` that the sound-off switch could never silence,
 * or a second consent gateway whose readiness flow nothing ever updates. [progressSubmitter]
 * is the one field that names a class, because it *is* a class rather than a seam with an
 * implementation; its KDoc explains why the convention's purpose is still served.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var soundPlayer: SoundPlayer

    /**
     * The consent seam, bootstrapped from [onCreate].
     *
     * **This deliberately does not live in `App`, which is where a reader will look for
     * it**, next to the `ForegroundActivityProvider` registration and the sound preload.
     * `App` is the `@HiltAndroidApp` class named in the manifest, so Robolectric
     * instantiates it and runs `onCreate()` for every Robolectric test in the suite —
     * bootstrapping consent there would drag the consent SDK onto the execution path of
     * the whole JVM suite and make `./gradlew check --offline` fragile, breaking the
     * Phase-6 D-18 offline invariant. No test references `MainActivity` and there is no
     * `androidTest` source set, so this class has exactly zero JVM-test exposure. D-16's
     * "gather and show on the first Activity at cold start" points here anyway.
     */
    @Inject
    lateinit var consentGateway: ConsentGateway

    /**
     * The Play Games seam, bootstrapped from [onCreate] (PGS-01, D-01).
     *
     * **This lives here and not in `App` for exactly the reason [consentGateway] does**, and
     * the reason is load-bearing rather than stylistic. `App` is the `@HiltAndroidApp` class
     * named in the manifest, so Robolectric instantiates it and runs `onCreate()` for every
     * Robolectric test in the suite; bootstrapping Play Games there would drag the SDK onto
     * the execution path of the whole JVM suite and make `./gradlew check --offline` fragile,
     * breaking the Phase-6 D-18 offline invariant. No test references `MainActivity` and
     * there is no `androidTest` source set, so this class has exactly zero JVM-test exposure.
     *
     * Declared as the **interface**, like every other injected field here: naming
     * `PlayGamesService` instead would build a second, unscoped instance whose session flow
     * nothing ever updates, leaving every observer permanently at
     * [com.app.pegsolitaire.platform.games.GamesSignInState.Unknown].
     */
    @Inject
    lateinit var gamesService: GamesService

    /**
     * The Play Games submission orchestrator, started from [onCreate] (PGSL-05, D-10).
     *
     * **Why this line exists at all (RESEARCH Pitfall 5).** Hilt constructs a `@Singleton`
     * lazily, on first injection. [ProgressSubmitter] is otherwise injected only by
     * `ResultsViewModel`, so without this call its session collector would not exist until
     * the player first reached a Results screen — and the D-10/PGSL-05 backfill, whose
     * entire purpose is the player who signed in from Settings without finishing a level,
     * would never run. It would fail silently, in production only, while passing every JVM
     * test. [ProgressSubmitter.start] is `AtomicBoolean`-guarded, so the repeat call every
     * configuration change makes is free.
     *
     * **This is the one field here declared as a concrete class, and the convention above
     * still holds.** That convention exists because naming an implementation would build a
     * second, unscoped instance beside the bound one. [ProgressSubmitter] has no interface
     * to name — it is a `@Singleton` with an `@Inject` constructor, like
     * [com.app.pegsolitaire.data.ChallengeProgress] — so `@Singleton` alone already
     * guarantees the single instance the convention was protecting, and there is nothing
     * else it could be declared as.
     *
     * Like everything else in this class it has **zero JVM-test coverage by construction**:
     * no test references [MainActivity] and there is no `androidTest` source set. It is
     * verified by plan 12-07's grep gate and by the device checklist plan 12-08 extends.
     */
    @Inject
    lateinit var progressSubmitter: ProgressSubmitter

    /**
     * The QA-only snapshot picker's gate (CLOUD-03 verification).
     *
     * **Inert in release, twice over**: the entry returns before it reads the intent unless
     * `BuildConfig.DEBUG` is set, *and* the picker it would launch lives in the `debug` source
     * set, so the release variant binds a no-op `show` and compiles no picker at all. Nothing
     * player-facing is added and D-03's silent cloud save is unchanged. The decision lives
     * behind the entry, not here, which keeps this an undecorated one-liner like the three
     * bootstraps — and keeps it testable, since this class has no JVM-test exposure. See
     * [DebugSnapshotPickerEntry] for the adb command.
     *
     * The one thing this class must supply is the **scope**: the entry's wait belongs to the
     * Activity that was launched with the extra, so both call sites below hand it
     * [lifecycleScope]. On the process-lifetime scope it used to hold, a 30-second sign-in wait
     * could outlive this Activity and surface the picker over a later one.
     */
    @Inject
    lateinit var debugSnapshotPickerEntry: DebugSnapshotPickerEntry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // After super.onCreate() is not stylistic: Hilt performs field injection as part of
        // that call, so touching an injected field earlier throws
        // UninitializedPropertyAccessException. All three calls below are fire-and-forget —
        // the app is fully playable while consent resolves, and CONS-04 keeps it that way if
        // consent never does; PGS-01 makes the same promise for sign-in ("no wall, no pause
        // before they can start a level"), so none may be awaited before setContent.
        // The Play Games call is a no-op unless the player already opted in (D-01): that
        // branch lives behind the seam, which is what keeps this an undecorated one-liner
        // and keeps the persisted-flag read where PlatformBoundaryRules can see it.
        // The third bootstrap only attaches a collector to the session the second one
        // resolves; it issues no submission until that session reports SignedIn AND the
        // persisted pending flag is set (D-10), so a player who never opted in pays nothing.
        consentGateway.bootstrap()
        gamesService.bootstrapIfOptedIn()
        progressSubmitter.start()
        debugSnapshotPickerEntry.onIntent(intent, lifecycleScope)
        setContent {
            CompositionLocalProvider(LocalSoundPlayer provides soundPlayer) {
                AppTheme {
                    AppNavHost(navController = rememberNavController())
                }
            }
        }
    }

    /** A debug launch that reaches an already-running instance carries the extra here instead. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        debugSnapshotPickerEntry.onIntent(intent, lifecycleScope)
    }
}
