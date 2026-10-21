package com.app.pegsolitaire.ui.game

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * [UndoControl] leaf (ASST-02): when `canUndo` is true the icon-only control is enabled and
 * a tap invokes the hoisted handler; when false it is disabled (greyed) and taps are
 * dropped. The control is located by its `undo_action` content description (the icon is
 * decorative), so the assertions are deterministic rather than pixel-based. The Robolectric
 * runtime SDK is pinned independently of the app's `compileSdk 36`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class UndoControlTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val undoLabel get() = RuntimeEnvironment.getApplication().getString(R.string.undo_action)

    @Test
    fun enabledWhenCanUndoAndAClickInvokesTheHandler() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                UndoControl(canUndo = true, onUndo = { clicks++ })
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(undoLabel).assertIsEnabled()
        composeRule.onNodeWithContentDescription(undoLabel).performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun disabledWhenNothingToUndoAndTapsAreDropped() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                UndoControl(canUndo = false, onUndo = { clicks++ })
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(undoLabel).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(undoLabel).performClick()

        assertThat(clicks).isEqualTo(0)
    }

    /**
     * Criterion 22 after the D-04 move: the control keeps its label in **both** states, so the
     * disabled case is announced rather than merely drawn.
     *
     * Added with Phase 15 rather than rewritten: the control moved from the top bar into the
     * action dock and grew to `game_action_button_size` from the retired top-bar token, but what it
     * promises the player did not change, so the two tests above stand unedited and this one
     * covers the channel the reskin could silently have dropped.
     */
    @Test
    fun theUndoActionKeepsItsLabelInBothStatesSoDisabledIsAnnouncedAndNotOnlyDrawn() {
        composeRule.setContent {
            AppTheme {
                UndoControl(canUndo = false, onUndo = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(undoLabel).assertExists()
        composeRule.onNodeWithContentDescription(undoLabel).assertIsNotEnabled()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34
