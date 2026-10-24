package com.app.pegsolitaire.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.licenses.GOOGLE_NOTICES_LIST_TAG
import com.app.pegsolitaire.ui.licenses.GoogleNoticeRow
import com.app.pegsolitaire.ui.licenses.GoogleNoticesScreen
import com.app.pegsolitaire.ui.licenses.GoogleNoticesUiState
import com.app.pegsolitaire.ui.licenses.LICENSES_LIST_TAG
import com.app.pegsolitaire.ui.licenses.LicenseCatalog
import com.app.pegsolitaire.ui.licenses.LicensesScreen
import com.app.pegsolitaire.ui.licenses.NoticeOrdinal
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The licences spine of the nav graph (spec LS-AC02, LS-AC13; brief §3.2): Licenses → License text,
 * Licenses → Google notices → Google notice text, every edge a plain push whose only way out is
 * `popBackStack()` — back and system back return one step — with each list's position surviving
 * its round trip. Routes carry an entry id or an index key, never a path.
 *
 * `AppNavHost` runs with the real stateless list screens where a position matters and stand-ins
 * for the ViewModel-backed text screens; the Settings edge into Licenses is `NavFlowTest`'s and
 * `SettingsScreenTest`'s (commit 15).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [LICENSES_NAV_ROBOLECTRIC_SDK])
class LicensesNavFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: NavHostController
    private val app get() = composeRule.activity

    @Test
    fun aTextEntryRoundTripKeepsTheListsPosition() {
        host()
        val checker = app.getString(R.string.licenses_checker_title)

        composeRule.onNodeWithTag(LICENSES_LIST_TAG).performScrollToNode(hasText(checker))
        composeRule.onNodeWithText(checker).performClick()
        composeRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.toRoute<LicenseText>()?.entryId).isEqualTo("checker-qual")
        composeRule.onNodeWithTag(TEXT_STAND_IN).performClick()
        composeRule.waitForIdle()

        assertThat(currentIs<Licenses>()).isTrue()
        composeRule.onNodeWithText(checker).assertIsDisplayed()
    }

    @Test
    fun aTwoTextEntryOpensAndReturnsInOneStep() {
        host()
        val jakarta = app.getString(R.string.licenses_jakarta_title)

        composeRule.onNodeWithTag(LICENSES_LIST_TAG).performScrollToNode(hasText(jakarta))
        composeRule.onNodeWithText(jakarta).performClick()
        composeRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.toRoute<LicenseText>()?.entryId).isEqualTo("jakarta-inject")
        systemBack()

        assertThat(currentIs<Licenses>()).isTrue()
    }

    @Test
    fun theNoticesRoundTripKeepsTheListsPositionAndRowsSharingANameOpenDifferentKeys() {
        host()
        val notices = app.getString(R.string.licenses_google_notices_title)
        composeRule.onNodeWithTag(LICENSES_LIST_TAG).performScrollToNode(hasText(notices))
        composeRule.onNodeWithText(notices).performClick()
        composeRule.waitForIdle()
        assertThat(currentIs<GoogleNotices>()).isTrue()

        val second = app.getString(R.string.google_notices_ordinal, 2, 2)
        composeRule.onNodeWithTag(GOOGLE_NOTICES_LIST_TAG).performScrollToNode(hasText(second))
        composeRule.onNodeWithText(second).performClick()
        composeRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.toRoute<GoogleNoticeText>()?.key).isEqualTo(LAST_ROWS[1].key)
        composeRule.onNodeWithTag(NOTICE_STAND_IN).performClick()
        composeRule.waitForIdle()

        assertThat(currentIs<GoogleNotices>()).isTrue()
        composeRule.onNodeWithText(second).assertIsDisplayed()
        val first = app.getString(R.string.google_notices_ordinal, 1, 2)
        composeRule.onNodeWithText(first).performClick()
        composeRule.waitForIdle()
        assertThat(navController.currentBackStackEntry?.toRoute<GoogleNoticeText>()?.key).isEqualTo(LAST_ROWS[0].key)
    }

    @Test
    fun systemBackEqualsTheBackButtonOnEveryLicencesScreen() {
        host()
        composeRule.runOnIdle { navController.navigate(GoogleNotices) }
        composeRule.runOnIdle { navController.navigate(GoogleNoticeText(LAST_ROWS[0].key)) }
        composeRule.waitForIdle()

        systemBack()
        assertThat(currentIs<GoogleNotices>()).isTrue()
        systemBack()
        assertThat(currentIs<Licenses>()).isTrue()
        composeRule.runOnIdle { navController.navigate(LicenseText("fredoka")) }
        systemBack()
        assertThat(currentIs<Licenses>()).isTrue()
        systemBack()
        assertThat(currentIs<Home>()).isTrue()
    }

    /** `AppNavHost` with the licences graph's real list screens, opened on Licenses above Home. */
    private fun host() {
        composeRule.setContent {
            navController = rememberNavController()
            AppNavHost(
                navController = navController,
                homeContent = { _, _, _, _, _, _ -> StandIn(HOME_STAND_IN) {} },
                licensesContent = { onOpenEntry, onOpenGoogleNotices, onBack ->
                    LicensesScreen(LicenseCatalog.sections, onOpenEntry, onOpenGoogleNotices, onBack)
                },
                licenseTextContent = { onBack -> StandIn(TEXT_STAND_IN, onBack) },
                googleNoticesContent = { onOpenNotice, onBack ->
                    GoogleNoticesScreen(GoogleNoticesUiState.Loaded(NOTICE_ROWS), onOpenNotice, onBack)
                },
                googleNoticeTextContent = { onBack -> StandIn(NOTICE_STAND_IN, onBack) },
            )
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { navController.navigate(Licenses) }
        composeRule.waitForIdle()
    }

    private fun systemBack() {
        composeRule.runOnIdle { app.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private inline fun <reified T : Any> currentIs(): Boolean = navController.currentDestination?.hasRoute<T>() == true

    @Composable
    private fun StandIn(
        tag: String,
        onClick: () -> Unit,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .testTag(tag)
                .clickable(onClick = onClick),
        )
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val LICENSES_NAV_ROBOLECTRIC_SDK = 34

private const val HOME_STAND_IN = "stand_in_home"
private const val TEXT_STAND_IN = "stand_in_license_text"
private const val NOTICE_STAND_IN = "stand_in_notice_text"
private const val LEADING_ROWS = 20

/** The last two rows share a name and sources: only their ordinals tell them apart (LS-AC13). */
private val LAST_ROWS =
    listOf(
        GoogleNoticeRow("00000000000000f1", "Kotlin", "ads-mobile-sdk, hsdp", NoticeOrdinal(1, 2)),
        GoogleNoticeRow("00000000000000f2", "Kotlin", "ads-mobile-sdk, hsdp", NoticeOrdinal(2, 2)),
    )

/** Enough rows ahead of them that the last two sit below the fold. */
private val NOTICE_ROWS =
    (0 until LEADING_ROWS).map { index ->
        GoogleNoticeRow("%016x".format(index), "Component $index", "ads-mobile-sdk", ordinal = null)
    } + LAST_ROWS
