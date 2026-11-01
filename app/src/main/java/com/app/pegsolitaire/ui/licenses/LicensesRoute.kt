package com.app.pegsolitaire.ui.licenses

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The Licenses destination (spec §3; tech design §3.6): the stateless [LicensesScreen] fed the
 * static [LicenseCatalog]. No ViewModel — there is nothing to hold (LS-09). The callbacks come from
 * `AppNavHost`, so the screen never sees a controller.
 *
 * @param onOpenEntry opens an entry's text by its catalogue id.
 * @param onOpenGoogleNotices opens the Google notices list.
 * @param onBack leaves the screen.
 */
@Composable
fun LicensesRoute(
    onOpenEntry: (String) -> Unit,
    onOpenGoogleNotices: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LicensesScreen(
        sections = LicenseCatalog.sections,
        onOpenEntry = onOpenEntry,
        onOpenGoogleNotices = onOpenGoogleNotices,
        onBack = onBack,
        modifier = modifier,
    )
}
