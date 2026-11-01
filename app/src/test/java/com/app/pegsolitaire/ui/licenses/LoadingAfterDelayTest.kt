package com.app.pegsolitaire.ui.licenses

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * LS-AC16 (spec §4, §5.1; brief §3.4): a loading screen shows nothing for its first
 * [LicensesUi.SPINNER_DELAY_MS], so a text that arrives sooner never flashes a spinner; after it,
 * the spinner carries its description in a polite live region, announced once. The error text is a
 * polite live region too. The delay runs on the composition clock, so the paused test clock drives
 * it with no wall-clock wait.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [LOADING_ROBOLECTRIC_SDK])
class LoadingAfterDelayTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noSpinnerIsComposedWhenTheStateLeavesLoadingBeforeTheDelay() {
        val loading = mutableStateOf(true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme { if (loading.value) LoadingAfterDelay(description = DESCRIPTION) else Text(LOADED) }
        }

        composeRule.mainClock.advanceTimeBy(JUST_BEFORE_THE_DELAY_MS)
        composeRule.onAllNodesWithContentDescription(DESCRIPTION).assertCountEquals(0)
        loading.value = false
        composeRule.mainClock.advanceTimeBy(WELL_PAST_THE_DELAY_MS)

        composeRule.onAllNodesWithContentDescription(DESCRIPTION).assertCountEquals(0)
        composeRule.onNodeWithText(LOADED).assertIsDisplayed()
    }

    @Test
    fun theSpinnerAppearsAfterTheDelayWithItsDescriptionAndAPoliteLiveRegion() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { AppTheme { LoadingAfterDelay(description = DESCRIPTION) } }

        composeRule.mainClock.advanceTimeBy(JUST_BEFORE_THE_DELAY_MS)
        composeRule.onAllNodesWithContentDescription(DESCRIPTION).assertCountEquals(0)
        composeRule.mainClock.advanceTimeBy(PAST_THE_DELAY_STEP_MS)

        val spinner = composeRule.onNodeWithContentDescription(DESCRIPTION).fetchSemanticsNode()
        assertThat(spinner.config[SemanticsProperties.LiveRegion]).isEqualTo(LiveRegionMode.Polite)
    }

    @Test
    fun theErrorTextIsAPoliteLiveRegion() {
        composeRule.setContent { AppTheme { LicensesErrorText(text = ERROR) } }

        val error = composeRule.onNodeWithText(ERROR).fetchSemanticsNode()
        assertThat(error.config[SemanticsProperties.LiveRegion]).isEqualTo(LiveRegionMode.Polite)
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val LOADING_ROBOLECTRIC_SDK = 34

private const val DESCRIPTION = "Loading license"
private const val LOADED = "Loaded text"
private const val ERROR = "This license text could not be shown."
private const val JUST_BEFORE_THE_DELAY_MS = 299L
private const val PAST_THE_DELAY_STEP_MS = 20L
private const val WELL_PAST_THE_DELAY_MS = 1_000L
