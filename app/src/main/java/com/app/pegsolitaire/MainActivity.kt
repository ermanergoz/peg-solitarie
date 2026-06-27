package com.app.pegsolitaire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.app.pegsolitaire.ui.theme.AppTheme

/**
 * Single host activity for the game.
 *
 * Plain [ComponentActivity] (no `@AndroidEntryPoint`) in Plan 01 — see [App] for
 * why Hilt is deferred. Hosts an empty themed surface; the real game route lands
 * in a later plan.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
            }
        }
    }
}
