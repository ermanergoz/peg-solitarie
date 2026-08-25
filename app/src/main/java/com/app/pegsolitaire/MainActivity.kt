package com.app.pegsolitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.navigation.AppNavHost
import com.app.pegsolitaire.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single host activity for the game.
 *
 * `@AndroidEntryPoint` registers this activity with Hilt's generated component graph
 * so each route's `@HiltViewModel` can be injected via `hiltViewModel()`. Hosts the
 * type-safe [AppNavHost] inside the Material 3 [AppTheme]: it owns the five-screen back
 * stack (Home → ClassicCatalog → Gameplay → Results), replacing the Phase-1 direct
 * `GameRoute()` host (NAV-02). The activity adds no logic beyond hosting.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AppNavHost(navController = rememberNavController())
            }
        }
    }
}
