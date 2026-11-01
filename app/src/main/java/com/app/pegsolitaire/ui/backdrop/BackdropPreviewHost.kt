package com.app.pegsolitaire.ui.backdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier

/**
 * The host's shape for previews (BD-25): the window-filling `Surface`, [SceneBackdrop] at [density]
 * with its scene ready, and the clock fixed at [timeMs] so a preview shows one exact pose, with the
 * screen's [content] above it.
 *
 * The scene fills its parent, so a preview drawn through this host states its window
 * (`widthDp` and `heightDp`): [BACKDROP_PREVIEW_WIDTH_DP] by [BACKDROP_PREVIEW_HEIGHT_DP] unless
 * the preview is about a narrower one.
 */
@Composable
internal fun BackdropPreviewHost(
    density: BackdropDensity,
    timeMs: Long = PREVIEW_TIME_MS,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxSize(),
    ) {
        CompositionLocalProvider(LocalBackdropClock provides FixedBackdropClock(timeMs)) {
            Box(modifier = Modifier.fillMaxSize()) {
                SceneBackdrop(density = density)
                content()
            }
        }
    }
}

/** A pose mid-way through the waves' swing, with most lights in view. */
private const val PREVIEW_TIME_MS = 6_000L

/** The 6.7-inch baseline window a preview through [BackdropPreviewHost] is drawn in. */
internal const val BACKDROP_PREVIEW_WIDTH_DP = 412
internal const val BACKDROP_PREVIEW_HEIGHT_DP = 915
