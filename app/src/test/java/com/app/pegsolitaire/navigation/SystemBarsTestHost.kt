package com.app.pegsolitaire.navigation

import android.view.Window
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import com.app.pegsolitaire.ui.window.LocalSystemBars
import com.app.pegsolitaire.ui.window.LocalSystemBarsState
import com.app.pegsolitaire.ui.window.SystemBars
import com.app.pegsolitaire.ui.window.SystemBarsState

/**
 * The real `AppNavHost` with every destination a stand-in, for the window-bar tests (docs/ux-spec.md
 * criteria 30a and 30b): [bars] is provided as the host's seam and [hostWindow] handed to the host,
 * as `MainActivity` does. A stand-in is an undrawn, tagged region; the tests drive the graph with
 * `navigate`, so no stand-in needs a control. [onBoardReadsBarsState] receives, after each
 * composition of the board stand-in, the bar state the host provides to what it hosts.
 */
@Composable
internal fun AppNavHostWithBarStandIns(
    navController: NavHostController,
    hostWindow: Window?,
    bars: SystemBars,
    onBoardReadsBarsState: (SystemBarsState) -> Unit = {},
) {
    CompositionLocalProvider(LocalSystemBars provides bars) {
        AppNavHost(
            hostWindow = hostWindow,
            navController = navController,
            homeContent = { _, _, _, _, _, _ -> StandIn(BarStandInTags.HOME) },
            catalogContent = { _, _ -> StandIn(BarStandInTags.CATALOG) },
            gameplayContent = { _, _ ->
                val provided = LocalSystemBarsState.current.value
                SideEffect { onBoardReadsBarsState(provided) }
                StandIn(BarStandInTags.GAMEPLAY)
            },
            resultsContent = { _, _, _, _, _, _ -> StandIn(BarStandInTags.RESULTS) },
            settingsContent = { _, _, _ -> StandIn(BarStandInTags.SETTINGS) },
            tutorialContent = { _ -> StandIn(BarStandInTags.TUTORIAL) },
            licensesContent = { _, _, _ -> StandIn(BarStandInTags.LICENSES) },
            licenseTextContent = { _ -> StandIn(BarStandInTags.LICENSE_TEXT) },
            googleNoticesContent = { _, _ -> StandIn(BarStandInTags.GOOGLE_NOTICES) },
            googleNoticeTextContent = { _ -> StandIn(BarStandInTags.GOOGLE_NOTICE_TEXT) },
        )
    }
}

/** The test tags of [AppNavHostWithBarStandIns]' destinations. */
internal object BarStandInTags {
    const val HOME = "bars_home"
    const val CATALOG = "bars_catalog"
    const val GAMEPLAY = "bars_gameplay"
    const val RESULTS = "bars_results"
    const val SETTINGS = "bars_settings"
    const val TUTORIAL = "bars_tutorial"
    const val LICENSES = "bars_licenses"
    const val LICENSE_TEXT = "bars_license_text"
    const val GOOGLE_NOTICES = "bars_google_notices"
    const val GOOGLE_NOTICE_TEXT = "bars_google_notice_text"
}

@Composable
private fun StandIn(tag: String) {
    Box(Modifier.fillMaxSize().testTag(tag))
}
