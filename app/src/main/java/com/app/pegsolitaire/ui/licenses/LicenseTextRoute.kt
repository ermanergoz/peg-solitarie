package com.app.pegsolitaire.ui.licenses

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The License text destination: obtains the [LicenseTextViewModel] (which reads the route's entry
 * id itself), collects its state lifecycle-aware, and renders the stateless [LicenseTextScreen].
 *
 * @param onBack leaves the screen (supplied by `AppNavHost`).
 */
@Composable
fun LicenseTextRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LicenseTextViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LicenseTextScreen(state = state, onBack = onBack, modifier = modifier)
}
