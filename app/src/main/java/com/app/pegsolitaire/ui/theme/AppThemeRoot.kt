package com.app.pegsolitaire.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

/**
 * The app's composition root theme: collects the palette setting once and hands it to [AppTheme]
 * as a value (docs/ux/colour-vision.md §3.3). No composable below reads the repository.
 *
 * `collectAsStateWithLifecycle` seeds the first composition with the `StateFlow`'s current value,
 * which is what keeps a recreated Activity from drawing one frame in the standard palette; each
 * later value recomposes this, [AppTheme] re-provides [LocalBoardMarks], and only that local's
 * readers recompose.
 *
 * @param palette the process-wide palette, `BoardMarkPaletteState.palette`.
 * @param content the app's navigation host.
 */
@Composable
internal fun AppThemeRoot(
    palette: StateFlow<BoardMarkPalette>,
    content: @Composable () -> Unit,
) {
    val boardMarkPalette by palette.collectAsStateWithLifecycle()
    AppTheme(boardMarkPalette = boardMarkPalette, content = content)
}
