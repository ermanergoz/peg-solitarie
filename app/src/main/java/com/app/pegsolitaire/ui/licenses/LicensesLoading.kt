package com.app.pegsolitaire.ui.licenses

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay

/**
 * The three loading screens' one spinner (spec §4, §4A, §4B; brief §3.4): nothing for the first
 * [LicensesUi.SPINNER_DELAY_MS], so a text that arrives sooner never flashes a spinner; then a
 * centred `primary` progress indicator whose [description] sits in a polite live region, announced
 * once when it appears (spec §5.1). The delay runs on the composition's clock, so the Compose test
 * clock drives it.
 */
@Composable
internal fun LoadingAfterDelay(
    description: String,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LicensesUi.SPINNER_DELAY_MS)
        shown = true
    }
    if (shown) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier.semantics {
                        contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                    },
            )
        }
    }
}

/** A screen's error line (`bodyMedium`, `onSurfaceVariant`), a polite live region (spec §5.1). */
@Composable
internal fun LicensesErrorText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
