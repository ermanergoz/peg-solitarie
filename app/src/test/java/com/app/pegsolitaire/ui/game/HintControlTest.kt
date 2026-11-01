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
 * [HintControl] leaf (ASST-01/ASST-03, D-01/D-10). At rest the icon-only control is enabled
 * and a tap invokes the hoisted handler; while `solverThinking` it shows the "Finding a move"
 * thinking node and is disabled so a double-tap can never launch an overlapping Solver search
 * (T-04-3B-DT). The control is located by its content description (the icon/spinner is
 * decorative), so the assertions are deterministic rather than pixel-based.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class HintControlTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val hintLabel get() = RuntimeEnvironment.getApplication().getString(R.string.hint_action)
    private val thinkingLabel get() = RuntimeEnvironment.getApplication().getString(R.string.hint_thinking)

    @Test
    fun restingIsEnabledAndAClickRequestsAHint() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                HintControl(solverThinking = false, onHint = { clicks++ })
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(hintLabel).assertIsEnabled()
        composeRule.onNodeWithContentDescription(hintLabel).performClick()

        assertThat(clicks).isEqualTo(1)
    }

    @Test
    fun thinkingShowsTheSpinnerAndIsDisabled() {
        composeRule.setContent {
            AppTheme {
                HintControl(solverThinking = true, onHint = {})
            }
        }
        composeRule.waitForIdle()

        // The disabled button carries the "Finding a move" description (spinner replaces the
        // icon); being disabled is what blocks a double-tap launching an overlapping search.
        composeRule.onNodeWithContentDescription(thinkingLabel).assertIsNotEnabled()
        // The resting Hint icon is gone while thinking, so the two states never coexist.
        composeRule.onNodeWithContentDescription(hintLabel).assertDoesNotExist()
    }

    /**
     * Criterion 23's second clause, asserted as behaviour rather than as appearance: a tap while
     * the Solver is already searching **reaches nothing**, so it cannot start a second,
     * overlapping search (T-04-3B-DT).
     *
     * Added with Phase 15 rather than rewritten. The control moved into the action dock and its
     * glyph changed from a magnifying glass to a lightbulb (D-04a), but the thinking contract is
     * the one it already had — this is the half a disabled-state assertion does not prove.
     */
    @Test
    fun aSecondTapWhileThinkingCannotStartASecondSearch() {
        var clicks = 0
        composeRule.setContent {
            AppTheme {
                HintControl(solverThinking = true, onHint = { clicks++ })
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(thinkingLabel).performClick()
        composeRule.waitForIdle()

        assertThat(clicks).isEqualTo(0)
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34
