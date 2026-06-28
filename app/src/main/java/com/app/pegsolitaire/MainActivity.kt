package com.app.pegsolitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.app.pegsolitaire.ui.game.GameRoute
import com.app.pegsolitaire.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single host activity for the game.
 *
 * `@AndroidEntryPoint` registers this activity with Hilt's generated component
 * graph so the [GameRoute] can inject the `@HiltViewModel` `GameViewModel` via
 * `hiltViewModel()`. Hosts the one playable English-33 game route inside the
 * Material 3 [AppTheme] — the whole walking skeleton end-to-end.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                GameRoute()
            }
        }
    }
}
