package com.app.pegsolitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundPlayer
import com.app.pegsolitaire.navigation.AppNavHost
import com.app.pegsolitaire.platform.ads.consent.ConsentGateway
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
 * It is also where the consent flow starts (CONS-01, D-16) — see [consentGateway].
 *
 * Every declared type here is the **interface**, for the same reason `App` documents:
 * naming a concrete implementation instead would build a second, unscoped instance — a
 * second player owning a second `SoundPool` that the sound-off switch could never silence,
 * or a second consent gateway whose readiness flow nothing ever updates.
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // After super.onCreate() is not stylistic: Hilt performs field injection as part of
        // that call, so touching an injected field earlier throws
        // UninitializedPropertyAccessException. Fire-and-forget — the app is fully playable
        // while consent resolves, and CONS-04 keeps it that way if consent never does.
        consentGateway.bootstrap()
        setContent {
            CompositionLocalProvider(LocalSoundPlayer provides soundPlayer) {
                AppTheme {
                    AppNavHost(navController = rememberNavController())
                }
            }
        }
    }
}
