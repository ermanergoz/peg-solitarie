package com.app.pegsolitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundPlayer
import com.app.pegsolitaire.navigation.AppNavHost
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
 * The declared type is the **interface**, for the same reason `App` documents: naming the
 * concrete implementation instead would build a second, unscoped player owning a second
 * `SoundPool`, which the sound-off switch could never silence.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var soundPlayer: SoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalSoundPlayer provides soundPlayer) {
                AppTheme {
                    AppNavHost(navController = rememberNavController())
                }
            }
        }
    }
}
