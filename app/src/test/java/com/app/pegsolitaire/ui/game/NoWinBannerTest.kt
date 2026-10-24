package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
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

    /**
     * Criterion 23k, its Compose half (D-04f): the banner's message is a polite live region, so
     * TalkBack speaks a proven dead end — and a stuck board — as it speaks the Undecided notice;
     * and the three actions are outside that region, neither carrying one nor inside one, so
     * they are read when they are focused and never announced with the message.
     */
    @Test
    fun theBannersMessageIsAPoliteLiveRegionAndItsActionsAreOutsideIt() {
        val messages = listOf(app.getString(R.string.nowin_message), app.getString(R.string.stuck_message))
        val shown = mutableStateOf(messages.first())
        composeRule.setContent {
            AppTheme { NoWinBanner(message = shown.value, onUndo = {}, onRestart = {}, onHome = {}) }
        }

        messages.forEach { message ->
            composeRule.runOnUiThread { shown.value = message }
            composeRule.waitForIdle()

            val messageNode = composeRule.onNodeWithText(message, useUnmergedTree = true).fetchSemanticsNode()
            assertWithMessage("the live region on \"%s\"", message)
                .that(messageNode.config.getOrNull(SemanticsProperties.LiveRegion))
                .isEqualTo(LiveRegionMode.Polite)
            RESCUE_ACTIONS.map(app::getString).forEach { action ->
                val actionNode = composeRule.onNodeWithText(action, useUnmergedTree = true).fetchSemanticsNode()
                assertWithMessage("the live regions on and above the %s action", action)
                    .that(actionNode.liveRegionsOnAndAbove())
                    .isEmpty()
            }
        }
    }
}

/** Every live region on this node and on each of its ancestors, the node's own first. */
private fun SemanticsNode.liveRegionsOnAndAbove(): List<LiveRegionMode> =
    generateSequence(this) { it.parent }
        .mapNotNull { it.config.getOrNull(SemanticsProperties.LiveRegion) }
        .toList()

/** The banner's three actions, in their on-screen order (D-09). */
private val RESCUE_ACTIONS = listOf(R.string.undo_action, R.string.rescue_restart, R.string.action_home)

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34
