package com.app.pegsolitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.app.pegsolitaire.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single host activity for the game.
 *
 * `@AndroidEntryPoint` registers this activity with Hilt's generated component
 * graph, so a later plan can inject `@HiltViewModel` view models via
 * `hiltViewModel()` with no further build setup. Hosts an empty themed surface;
 * the real game route lands in a later plan.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
            }
        }
    }
}
