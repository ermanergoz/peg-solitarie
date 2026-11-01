package com.app.pegsolitaire.ui.licenses

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The Licenses screen's main flow (spec §3, LS-AC10; tech design §3.6): the stateless screen fed
 * the static catalogue shows its four sections, a tappable entry opens its text by id, the notices
 * entry opens the notices list, an inert entry opens nothing, and back leaves.
 *
 * A tall window composes the whole list, so every entry is on screen without scrolling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [LICENSES_SCREEN_ROBOLECTRIC_SDK], qualifiers = TALL_WINDOW)
class LicensesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()
    private val openedEntries = mutableListOf<String>()
    private var noticesOpened = 0
    private var backs = 0

    @Test
    fun theScreenShowsItsFourSectionsAndOpensFredoka() {
        setContent()

        listOf(
            R.string.licenses_section_fonts,
            R.string.licenses_section_sounds,
            R.string.licenses_section_software,
            R.string.licenses_section_google,
        ).forEach { composeRule.onNodeWithText(app.getString(it)).assertIsDisplayed() }
        composeRule.onNodeWithText(app.getString(R.string.licenses_fredoka_title)).performClick()

        assertThat(openedEntries).containsExactly("fredoka")
        assertThat(noticesOpened).isEqualTo(0)
    }

    @Test
    fun theNoticesEntryOpensTheNoticesList() {
        setContent()

        composeRule.onNodeWithText(app.getString(R.string.licenses_google_notices_title)).performClick()

        assertThat(noticesOpened).isEqualTo(1)
        assertThat(openedEntries).isEmpty()
    }

    @Test
    fun aTwoTextEntryOpensByItsId() {
        setContent()

        composeRule.onNodeWithText(app.getString(R.string.licenses_jakarta_title)).performClick()

        assertThat(openedEntries).containsExactly("jakarta-inject")
    }

    @Test
    fun anInertEntryOpensNothing() {
        setContent()

        composeRule.onNodeWithText(app.getString(R.string.licenses_kenney_title)).performClick()
        composeRule.onNodeWithText(app.getString(R.string.licenses_google_sdk_title)).performClick()

        assertThat(openedEntries).isEmpty()
        assertThat(noticesOpened).isEqualTo(0)
    }

    @Test
    fun theBackButtonLeaves() {
        setContent()

        composeRule.onNodeWithContentDescription(app.getString(R.string.navigate_back)).performClick()

        assertThat(backs).isEqualTo(1)
    }

    private fun setContent() {
        composeRule.setContent {
            AppTheme {
                LicensesScreen(
                    sections = LicenseCatalog.sections,
                    onOpenEntry = { openedEntries += it },
                    onOpenGoogleNotices = { noticesOpened++ },
                    onBack = { backs++ },
                )
            }
        }
        composeRule.waitForIdle()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val LICENSES_SCREEN_ROBOLECTRIC_SDK = 34

/** Tall enough that the lazy list composes every one of its items at once. */
private const val TALL_WINDOW = "w411dp-h4000dp"
