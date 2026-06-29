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
 * `hiltViewModel()`. Hosts the gameplay route inside the Material 3 [AppTheme].
 *
 * The `onFinished`/`onNavigateBack` hand-offs and the `Gameplay(levelId)` route arg
 * are stubbed here only as an interim seam: the real `AppNavHost` (02-08) replaces
 * this direct host with a typed back stack that supplies the selected `levelId` and
 * wires finish→Results / back navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                GameRoute(
                    onFinished = {},
                    onNavigateBack = {},
                )
            }
        }
    }
}
