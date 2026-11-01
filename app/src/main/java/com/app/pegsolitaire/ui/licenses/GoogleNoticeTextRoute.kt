package com.app.pegsolitaire.ui.licenses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The Google notice text destination: obtains the [GoogleNoticeTextViewModel] (which reads the
 * route's key itself), collects its state lifecycle-aware, and renders [GoogleNoticeTextScreen].
 *
 * @param onBack leaves the screen (supplied by `AppNavHost`).
 */
@Composable
fun GoogleNoticeTextRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: GoogleNoticeTextViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GoogleNoticeTextScreen(state = state, onBack = onBack, modifier = modifier)
}
