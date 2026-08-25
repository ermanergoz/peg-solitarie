package com.app.pegsolitaire.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
 * Robolectric + Compose UI test (`createComposeRule`, JVM, no emulator) for the stateless
 * [NoWinBanner] leaf (ASST-03, D-06/D-08/D-09). It proves the hoisted [message] is rendered
 * and that each of the three actions (Undo · Restart · Home) invokes its handler exactly
 * once. The banner is a pure render of hoisted state/handlers, so the assertions are
 * deterministic (located by text) rather than pixel-based.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class NoWinBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    @Test
    fun rendersTheMessageAndInvokesEachActionExactlyOnce() {
        var undoCount = 0
        var restartCount = 0
        var homeCount = 0
        val message = app.getString(R.string.nowin_message)

        composeRule.setContent {
            AppTheme {
                NoWinBanner(
                    message = message,
                    onUndo = { undoCount++ },
                    onRestart = { restartCount++ },
                    onHome = { homeCount++ },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(message).assertIsDisplayed()

        composeRule.onNodeWithText(app.getString(R.string.undo_action)).performClick()
        composeRule.onNodeWithText(app.getString(R.string.rescue_restart)).performClick()
        composeRule.onNodeWithText(app.getString(R.string.action_home)).performClick()

        assertThat(undoCount).isEqualTo(1)
        assertThat(restartCount).isEqualTo(1)
        assertThat(homeCount).isEqualTo(1)
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34
