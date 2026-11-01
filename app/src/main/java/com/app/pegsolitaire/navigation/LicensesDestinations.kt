package com.app.pegsolitaire.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

/** The Licenses destination body's three callbacks, hoisted so `AppNavHost` stays test-injectable. */
internal typealias LicensesContent =
    @Composable (onOpenEntry: (String) -> Unit, onOpenGoogleNotices: () -> Unit, onBack: () -> Unit) -> Unit

/**
 * The licences spine (spec LS-02 as superseded by LS-09; tech design §3.6, §13.1 LN-4): Settings →
 * [Licenses] → [LicenseText]; Licenses → [GoogleNotices] → [GoogleNoticeText].
 *
 * Its own `NavGraphBuilder` extension rather than four more blocks in `AppNavHost`'s body (brief
 * §0.4, T2), which already sits near detekt's `LongMethod` threshold; the four slots stay on
 * `AppNavHost`, so tests inject stand-ins exactly as `NavFlowTest` does. Every edge is a plain push
 * whose only way out is `popBackStack()`, so back and system back return one step (LS-AC02), and the
 * text routes carry an entry id or an index key, never a path.
 */
internal fun NavGraphBuilder.licensesGraph(
    navController: NavHostController,
    licensesContent: LicensesContent,
    licenseTextContent: @Composable (onBack: () -> Unit) -> Unit,
    googleNoticesContent: @Composable (onOpenNotice: (String) -> Unit, onBack: () -> Unit) -> Unit,
    googleNoticeTextContent: @Composable (onBack: () -> Unit) -> Unit,
) {
    composable<Licenses> {
        licensesContent(
            { entryId -> navController.toLicenseText(entryId) },
            { navController.toGoogleNotices() },
            { navController.popBackStack() },
        )
    }
    composable<LicenseText> { licenseTextContent { navController.popBackStack() } }
    composable<GoogleNotices> {
        googleNoticesContent(
            { key -> navController.toGoogleNoticeText(key) },
            { navController.popBackStack() },
        )
    }
    composable<GoogleNoticeText> { googleNoticeTextContent { navController.popBackStack() } }
}

/**
 * Pushes [Licenses] — Settings › About › Licenses. `launchSingleTop`, defensively, as on
 * `toTutorial()`: a squishing 72dp card can take a double tap, and two pushed Licenses entries would
 * make back return to the same screen once. No `popUpTo`: it pops back to Settings.
 */
internal fun NavHostController.toLicenses() {
    navigate(Licenses) { launchSingleTop = true }
}

/** Pushes one entry's text by its catalogue id (never a path). */
private fun NavHostController.toLicenseText(entryId: String) {
    navigate(LicenseText(entryId)) { launchSingleTop = true }
}

/** Pushes the Google notices list. */
private fun NavHostController.toGoogleNotices() {
    navigate(GoogleNotices) { launchSingleTop = true }
}

/** Pushes one notice's text by its index key (never a stem or a path). */
private fun NavHostController.toGoogleNoticeText(key: String) {
    navigate(GoogleNoticeText(key)) { launchSingleTop = true }
}
