package com.app.pegsolitaire.ui.licenses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The Google notices destination: obtains the [GoogleNoticesViewModel], collects its state
 * lifecycle-aware, and renders the stateless [GoogleNoticesScreen].
 *
 * @param onOpenNotice opens one notice's text by its index key (supplied by `AppNavHost`).
 * @param onBack leaves the screen.
 */
@Composable
fun GoogleNoticesRoute(
    onOpenNotice: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: GoogleNoticesViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GoogleNoticesScreen(state = state, onOpenNotice = onOpenNotice, onBack = onBack, modifier = modifier)
}
