package com.app.pegsolitaire.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled

/**
 * Two slow-drifting, low-alpha clay blobs floating behind the Home content
 * (DESIGN-SYSTEM.md Home: 2 blobs, ±16dp drift over ~8s). Purely decorative —
 * no semantics — and skipped entirely when OS animations are off.
 */
@Composable
internal fun HomeBlobs() {
    if (!rememberAnimationsEnabled()) return
    val drift = rememberInfiniteTransition(label = "homeBlobDrift")
    val progress by
        drift.animateFloat(
            initialValue = -DRIFT_EXTENT,
            targetValue = DRIFT_EXTENT,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(ClayAnim.BLOB_DRIFT_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "homeBlobOffset",
        )
    val blobSize = dimensionResource(R.dimen.blob_size)
    val driftRange = dimensionResource(R.dimen.blob_drift_range)
    Box(modifier = Modifier.fillMaxSize()) {
        Blob(
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = blobSize * BLOB_PEEK_FRACTION + driftRange * progress,
                        y = -blobSize * BLOB_PEEK_FRACTION - driftRange * progress,
                    ),
        )
        Blob(
            color = MaterialTheme.colorScheme.secondary,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = -blobSize * BLOB_PEEK_FRACTION - driftRange * progress,
                        y = blobSize * BLOB_PEEK_FRACTION + driftRange * progress,
                    ),
        )
    }
}

/** One soft circular background blob at [ClayAnim.BLOB_ALPHA]. */
@Composable
private fun Blob(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(dimensionResource(R.dimen.blob_size))
                .background(color = color.copy(alpha = ClayAnim.BLOB_ALPHA), shape = CircleShape),
    )
}

/** How far a background blob's center peeks past its screen corner. */
private const val BLOB_PEEK_FRACTION = 0.3f

/** Blob drift progress runs -1...+1 of the `blob_drift_range` (16dp). */
private const val DRIFT_EXTENT = 1f
